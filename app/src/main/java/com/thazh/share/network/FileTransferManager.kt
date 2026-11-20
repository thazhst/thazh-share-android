package com.thazh.share.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.thazh.share.util.FileNameUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_CHUNK_SIZE_BYTES = 256 * 1024 // 256KB default high-speed P2P chunk size
private const val DISK_IO_BUFFER_BYTES = 256 * 1024

/** Extra room required on top of a file's size before accepting it, so the disk is never filled to the last byte. */
private const val FREE_SPACE_MARGIN_BYTES = 16L * 1024 * 1024

data class IncomingTransfer(
    val messageId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val bytesReceived: Long,
    val destinationFile: File
)

sealed class TransferProgressEvent {
    data class Progress(val messageId: String, val bytesTransferred: Long, val totalBytes: Long) : TransferProgressEvent()
    data class Completed(val messageId: String, val file: File) : TransferProgressEvent()
    data class Failed(val messageId: String, val reason: String) : TransferProgressEvent()
}

/**
 * High-throughput file transfer engine driving outgoing file sends and incoming file assembly.
 *
 * Optimizations:
 * - Adaptive chunk sizing (`64 KB .. 512 KB`) computed via [NativeCoreEngine.recommendChunkSize].
 * - Zero-allocation double-buffering backed by [ChunkBufferPool] slabs.
 * - Direct IO-thread chunk reception via [PeerConnection.directChunkHandler] bypassing Main-thread
 *   dispatch and using 256 KB buffered sequential disk output streams.
 */
class FileTransferManager(
    private val context: Context,
    private val connection: PeerConnection,
    private val outputDir: File
) {
    private val _progressEvents = MutableSharedFlow<TransferProgressEvent>(extraBufferCapacity = 64)
    val progressEvents: SharedFlow<TransferProgressEvent> = _progressEvents

    private val activeIncoming = ConcurrentHashMap<String, IncomingWriteState>()
    private val cancelledOutgoing: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private class IncomingWriteState(
        val file: File,
        val outputStream: BufferedOutputStream,
        val totalBytes: Long,
        val finalDir: File? = null,
        val finalName: String? = null,
        var bytesWritten: Long = 0L,
        var nextExpectedChunkIndex: Int = 0,
        var rollingHashFnv64: Long = 0L,
    ) {
        fun closeQuietly() {
            runCatching { outputStream.flush() }
            runCatching { outputStream.close() }
        }
    }

    private val destinationLock = Any()

    @Volatile
    private var pendingHeader: Frame.FileChunkHeader? = null

    init {
        connection.directChunkHandler = { header, pooledBuffer, length ->
            writeIncomingChunkInternal(header, pooledBuffer, length)
        }
    }

    suspend fun sendFile(
        messageId: String,
        sourceUri: Uri,
        fileName: String,
        fileSizeBytes: Long
    ): Boolean = withContext(Dispatchers.IO) {
        var currentSlab: ByteArray? = null
        var nextSlab: ByteArray? = null
        try {
            val rawInput = if (sourceUri.scheme == "file") {
                sourceUri.path?.let { path -> File(path).takeIf { it.isFile }?.inputStream() }
            } else {
                context.contentResolver.openInputStream(sourceUri)
            }
            if (rawInput == null) {
                _progressEvents.emit(TransferProgressEvent.Failed(messageId, "Không thể mở tệp"))
                return@withContext false
            }

            val adaptiveChunkSize = NativeCoreEngine.recommendChunkSize(fileSizeBytes)
                .coerceIn(64 * 1024, ChunkBufferPool.DEFAULT_SLAB_BYTES)
            currentSlab = ChunkBufferPool.acquire(adaptiveChunkSize)
            nextSlab = ChunkBufferPool.acquire(adaptiveChunkSize)

            BufferedInputStream(rawInput, DISK_IO_BUFFER_BYTES).use { stream ->
                var current = currentSlab!!
                var next = nextSlab!!
                var currentLength = stream.readUpTo(current, adaptiveChunkSize)
                var chunkIndex = 0
                var totalSent = 0L

                while (currentLength > 0) {
                    if (cancelledOutgoing.remove(messageId)) {
                        Log.d(TAG, "sendFile $messageId stopped: cancelled")
                        return@withContext false
                    }

                    val nextLength = stream.readUpTo(next, adaptiveChunkSize)
                    val isLast = nextLength <= 0

                    val header = Frame.FileChunkHeader(
                        messageId = messageId,
                        chunkIndex = chunkIndex,
                        chunkSizeBytes = currentLength,
                        isLastChunk = isLast
                    )
                    val ok = connection.sendFileChunk(header, current, currentLength)
                    if (!ok) {
                        _progressEvents.emit(TransferProgressEvent.Failed(messageId, "Mất kết nối trong khi gửi"))
                        return@withContext false
                    }

                    totalSent += currentLength
                    chunkIndex++
                    _progressEvents.tryEmit(TransferProgressEvent.Progress(messageId, totalSent, fileSizeBytes))

                    val swap = current
                    current = next
                    next = swap
                    currentLength = nextLength
                }

                _progressEvents.emit(TransferProgressEvent.Completed(messageId, File(fileName)))
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "sendFile failed: ${e.message}", e)
            _progressEvents.emit(TransferProgressEvent.Failed(messageId, e.message ?: "Lỗi không xác định"))
            false
        } finally {
            ChunkBufferPool.release(currentSlab)
            ChunkBufferPool.release(nextSlab)
            cancelledOutgoing.remove(messageId)
        }
    }

    fun cancelOutgoing(messageId: String) {
        cancelledOutgoing.add(messageId)
    }

    fun clearOutgoingCancel(messageId: String) {
        cancelledOutgoing.remove(messageId)
    }

    @Throws(IOException::class)
    fun onFileOfferReceived(
        offer: Frame.FileOffer,
        destinationDir: File = outputDir,
        stagingDir: File? = null,
        existingNames: Set<String> = emptySet()
    ): String {
        activeIncoming.remove(offer.messageId)?.let {
            it.closeQuietly()
            runCatching { it.file.delete() }
        }

        val safeName = FileNameUtils.sanitize(offer.fileName)
        val size = offer.fileSizeBytes.coerceAtLeast(0L)

        if (!destinationDir.exists()) destinationDir.mkdirs()
        if (destinationDir.usableSpace < size + FREE_SPACE_MARGIN_BYTES) {
            throw IOException("Không đủ dung lượng trống")
        }

        val resolvedName: String
        val state: IncomingWriteState = synchronized(destinationLock) {
            val inFlightNames = activeIncoming.values.mapNotNullTo(HashSet()) { it.finalName ?: it.file.name }
            val reserved = existingNames + inFlightNames

            if (stagingDir != null) {
                if (!stagingDir.exists()) stagingDir.mkdirs()
                resolvedName = FileNameUtils.uniqueFileName(destinationDir, safeName, reserved)
                val partial = File(stagingDir, FileNameUtils.sanitizeId(offer.messageId) + ".part")
                IncomingWriteState(
                    file = partial,
                    outputStream = BufferedOutputStream(FileOutputStream(partial, false), DISK_IO_BUFFER_BYTES),
                    totalBytes = size,
                    finalDir = destinationDir,
                    finalName = resolvedName
                )
            } else {
                val target = FileNameUtils.uniqueFile(destinationDir, safeName, reserved)
                resolvedName = target.name
                IncomingWriteState(
                    file = target,
                    outputStream = BufferedOutputStream(FileOutputStream(target, false), DISK_IO_BUFFER_BYTES),
                    totalBytes = size,
                    finalName = resolvedName
                )
            }
        }
        Log.d(TAG, "Priming incoming file $safeName -> $resolvedName (${state.file.absolutePath})")

        if (size == 0L) {
            try {
                val finished = finishIncoming(state)
                _progressEvents.tryEmit(TransferProgressEvent.Completed(offer.messageId, finished))
            } catch (e: Exception) {
                runCatching { state.file.delete() }
                _progressEvents.tryEmit(TransferProgressEvent.Failed(offer.messageId, e.message ?: "Lỗi ghi tệp"))
            }
            return resolvedName
        }

        activeIncoming[offer.messageId] = state
        return resolvedName
    }

    /** Fallback header setter when not using directChunkHandler. */
    fun onChunkHeaderReceived(header: Frame.FileChunkHeader) {
        pendingHeader = header
    }

    /** Fallback raw chunk writer when not using directChunkHandler. */
    suspend fun onChunkBytesReceived(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val header = pendingHeader
        pendingHeader = null
        if (header == null) {
            Log.w(TAG, "Received raw chunk with no pending header; dropping ${bytes.size} bytes")
            return@withContext
        }
        writeIncomingChunkInternal(header, bytes, header.chunkSizeBytes)
    }

    private suspend fun writeIncomingChunkInternal(
        header: Frame.FileChunkHeader,
        bytes: ByteArray,
        length: Int,
    ) {
        val state = activeIncoming[header.messageId]
        if (state == null) {
            return
        }
        val bytesToWrite = minOf(header.chunkSizeBytes, length, bytes.size)
        if (bytesToWrite <= 0) return

        try {
            synchronized(state) {
                state.outputStream.write(bytes, 0, bytesToWrite)
                state.bytesWritten += bytesToWrite
                state.nextExpectedChunkIndex = header.chunkIndex + 1
                state.rollingHashFnv64 = NativeCoreEngine.computeFnv1a64(
                    buffer = bytes,
                    offset = 0,
                    length = bytesToWrite,
                    seed = state.rollingHashFnv64,
                )
            }

            _progressEvents.tryEmit(
                TransferProgressEvent.Progress(header.messageId, state.bytesWritten, state.totalBytes)
            )

            if (header.isLastChunk) {
                activeIncoming.remove(header.messageId)
                val finished = finishIncoming(state)
                _progressEvents.emit(TransferProgressEvent.Completed(header.messageId, finished))
                Log.d(
                    TAG,
                    "Completed incoming file ${finished.name} (${state.bytesWritten} bytes, fnv64=${java.lang.Long.toUnsignedString(state.rollingHashFnv64, 16)})"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing chunk: ${e.message}", e)
            state.closeQuietly()
            activeIncoming.remove(header.messageId)
            runCatching { state.file.delete() }
            _progressEvents.emit(TransferProgressEvent.Failed(header.messageId, e.message ?: "Lỗi ghi tệp"))
        }
    }

    fun cancelTransfer(messageId: String) {
        activeIncoming.remove(messageId)?.let {
            it.closeQuietly()
            runCatching { it.file.delete() }
        }
    }

    fun abortAll() {
        activeIncoming.keys.toList().forEach { cancelTransfer(it) }
        pendingHeader = null
    }

    private fun finishIncoming(state: IncomingWriteState): File {
        state.closeQuietly()
        val finalDir = state.finalDir ?: return state.file

        synchronized(destinationLock) {
            val target = FileNameUtils.uniqueFile(finalDir, state.finalName ?: state.file.name)
            if (!state.file.renameTo(target)) {
                state.file.copyTo(target, overwrite = false)
                state.file.delete()
            }
            return target
        }
    }

    private fun InputStream.readUpTo(buffer: ByteArray, targetBytes: Int = buffer.size): Int {
        val maxLen = minOf(targetBytes, buffer.size)
        var total = 0
        while (total < maxLen) {
            val read = read(buffer, total, maxLen - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    companion object {
        private const val TAG = "FileTransferManager"
        const val CHUNK_SIZE = DEFAULT_CHUNK_SIZE_BYTES
    }
}
