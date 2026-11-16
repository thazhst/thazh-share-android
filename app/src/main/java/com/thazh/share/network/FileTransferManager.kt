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
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

private const val CHUNK_SIZE_BYTES = 64 * 1024 // 64KB chunks: good balance of throughput vs latency over P2P

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
 * Drives outgoing file sends (reading a content [Uri] or [File] in
 * [CHUNK_SIZE_BYTES] pieces and pushing them through [PeerConnection]) and
 * assembles incoming files chunk-by-chunk as [Frame.FileChunkHeader] +
 * raw-byte events arrive from the connection's read loop.
 *
 * The caller (WifiDirectService) is responsible for routing
 * [SocketConnectionEvent.FrameReceived]/[RawChunkReceived] pairs for
 * file-related frames into [onFileOfferReceived]/[onChunkBytesReceived]; this
 * class does not read the socket directly, keeping a single read-loop owner.
 *
 * The same instance serves both the chat attachments and the dedicated file
 * transfer tab; the only difference is where an incoming file is written
 * (see [onFileOfferReceived]).
 */
class FileTransferManager(
    private val context: Context,
    private val connection: PeerConnection,
    private val outputDir: File
) {
    // Terminal events (Completed/Failed) are sent with the suspending emit() so a
    // burst of Progress events can never push them out of the buffer; Progress
    // itself uses tryEmit(), where dropping an update is harmless.
    private val _progressEvents = MutableSharedFlow<TransferProgressEvent>(extraBufferCapacity = 64)
    val progressEvents: SharedFlow<TransferProgressEvent> = _progressEvents

    // Written by the service's event collector (offers) and by the user's cancel
    // button, while chunks are being written from an IO thread — hence concurrent.
    private val activeIncoming = ConcurrentHashMap<String, IncomingWriteState>()

    /** Ids of outgoing transfers the sender loop should stop at its next chunk. */
    private val cancelledOutgoing: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private class IncomingWriteState(
        /** The file bytes are written to: either the final file, or a staging file that is moved on completion. */
        val file: File,
        val raf: RandomAccessFile,
        val totalBytes: Long,
        /** Non-null when [file] is a staging file that must be moved into this folder, under [finalName], once complete. */
        val finalDir: File? = null,
        val finalName: String? = null,
        var bytesWritten: Long = 0L,
        var nextExpectedChunkIndex: Int = 0
    )

    /** Synchronizes destination filename reservation and final staging move so concurrent transfers never collide. */
    private val destinationLock = Any()

    /** True while a header has been read and we're awaiting its raw chunk on the next event. */
    private var pendingHeader: Frame.FileChunkHeader? = null

    suspend fun sendFile(
        messageId: String,
        sourceUri: Uri,
        fileName: String,
        fileSizeBytes: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val input = if (sourceUri.scheme == "file") {
                sourceUri.path?.let { path -> File(path).takeIf { it.isFile }?.inputStream() }
            } else {
                context.contentResolver.openInputStream(sourceUri)
            }
            if (input == null) {
                _progressEvents.emit(TransferProgressEvent.Failed(messageId, "Không thể mở tệp"))
                return@withContext false
            }
            input.use { stream ->
                // Read one chunk ahead so "is this the last chunk?" is decided by actually hitting
                // the end of the stream rather than by trusting the size reported up front: some
                // content providers report 0 or a wrong size, which would otherwise truncate the
                // file or leave the receiver waiting for a final chunk that never comes.
                var current = ByteArray(CHUNK_SIZE_BYTES)
                var next = ByteArray(CHUNK_SIZE_BYTES)
                var currentLength = stream.readUpTo(current)
                var chunkIndex = 0
                var totalSent = 0L

                while (currentLength > 0) {
                    if (cancelledOutgoing.remove(messageId)) {
                        Log.d(TAG, "sendFile $messageId stopped: cancelled")
                        return@withContext false
                    }

                    val nextLength = stream.readUpTo(next)
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
            cancelledOutgoing.remove(messageId)
        }
    }

    /** Asks the sender loop of [messageId] to stop at its next chunk. */
    fun cancelOutgoing(messageId: String) {
        cancelledOutgoing.add(messageId)
    }

    /** Forgets a pending cancel request, so a retry of the same transfer id starts clean. */
    fun clearOutgoingCancel(messageId: String) {
        cancelledOutgoing.remove(messageId)
    }

    /**
     * Call when a [Frame.FileOffer] arrives, to prime the destination file for the upcoming chunk stream.
     *
     * Automatically resolves a unique file name (adding suffix ` (1)`, ` (2)`, etc.) if a file with the
     * same name already exists in [destinationDir], in [existingNames], or among other active incoming transfers.
     *
     * @return The resolved (potentially auto-renamed) file name.
     * @throws IOException if the file can't be created or there isn't enough free space.
     */
    @Throws(IOException::class)
    fun onFileOfferReceived(
        offer: Frame.FileOffer,
        destinationDir: File = outputDir,
        stagingDir: File? = null,
        existingNames: Set<String> = emptySet()
    ): String {
        // A repeated offer for the same id (e.g. a retry) replaces whatever was left of the earlier attempt.
        activeIncoming.remove(offer.messageId)?.let {
            runCatching { it.raf.close() }
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
                    raf = RandomAccessFile(partial, "rw").also { it.setLength(0L) },
                    totalBytes = size,
                    finalDir = destinationDir,
                    finalName = resolvedName
                )
            } else {
                val target = FileNameUtils.uniqueFile(destinationDir, safeName, reserved)
                resolvedName = target.name
                IncomingWriteState(
                    file = target,
                    raf = RandomAccessFile(target, "rw").also { it.setLength(0L) },
                    totalBytes = size,
                    finalName = resolvedName
                )
            }
        }
        Log.d(TAG, "Priming incoming file $safeName -> $resolvedName (${state.file.absolutePath})")

        if (size == 0L) {
            // An empty file has no chunks to wait for (the sender can't send a zero-length chunk),
            // so it is complete as soon as it is announced.
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

    /** Call for every [Frame.FileChunkHeader] read from the socket. */
    fun onChunkHeaderReceived(header: Frame.FileChunkHeader) {
        pendingHeader = header
    }

    /** Call for the raw bytes event that immediately follows a chunk header. */
    suspend fun onChunkBytesReceived(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val header = pendingHeader
        pendingHeader = null
        if (header == null) {
            Log.w(TAG, "Received raw chunk with no pending header; dropping ${bytes.size} bytes")
            return@withContext
        }
        val state = activeIncoming[header.messageId]
        if (state == null) {
            // Normal after a cancel: the sender may still have chunks in flight.
            Log.w(TAG, "Received chunk for unknown transfer ${header.messageId}")
            return@withContext
        }

        try {
            state.raf.seek(state.bytesWritten)
            state.raf.write(bytes, 0, header.chunkSizeBytes)
            state.bytesWritten += header.chunkSizeBytes
            state.nextExpectedChunkIndex = header.chunkIndex + 1

            _progressEvents.tryEmit(
                TransferProgressEvent.Progress(header.messageId, state.bytesWritten, state.totalBytes)
            )

            if (header.isLastChunk) {
                activeIncoming.remove(header.messageId)
                val finished = finishIncoming(state)
                _progressEvents.emit(TransferProgressEvent.Completed(header.messageId, finished))
                Log.d(TAG, "Completed incoming file ${finished.name} (${state.bytesWritten} bytes)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing chunk: ${e.message}", e)
            runCatching { state.raf.close() }
            activeIncoming.remove(header.messageId)
            runCatching { state.file.delete() }
            _progressEvents.emit(TransferProgressEvent.Failed(header.messageId, e.message ?: "Lỗi ghi tệp"))
        }
    }

    /** Drops an incoming transfer (the person cancelled, or the sender did) and deletes what was written so far. */
    fun cancelTransfer(messageId: String) {
        activeIncoming.remove(messageId)?.let {
            runCatching { it.raf.close() }
            runCatching { it.file.delete() }
        }
    }

    /** Called when the connection is torn down: discards every unfinished incoming file. */
    fun abortAll() {
        activeIncoming.keys.toList().forEach { cancelTransfer(it) }
        pendingHeader = null
    }

    /** Closes the written file and, for staged transfers, moves it into its final folder under a unique name. */
    private fun finishIncoming(state: IncomingWriteState): File {
        runCatching { state.raf.close() }
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

    /** Reads until [buffer] is full or the stream ends; returns the number of bytes read (0 at end of stream). */
    private fun InputStream.readUpTo(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = read(buffer, total, buffer.size - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    companion object {
        private const val TAG = "FileTransferManager"
        const val CHUNK_SIZE = CHUNK_SIZE_BYTES
    }
}
