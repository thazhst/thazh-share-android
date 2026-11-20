package com.thazh.share.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

private const val STREAM_BUFFER_SIZE_BYTES = 256 * 1024

/**
 * High-throughput length-prefixed frame & raw chunk codec over TCP sockets.
 *
 * Optimizations:
 * - Wraps raw socket streams in 256 KB [BufferedInputStream] / [BufferedOutputStream]
 *   to eliminate per-field kernel `read`/`write` syscalls.
 * - Combines [Frame.FileChunkHeader] and raw payload bytes into a single coalesced
 *   write via [writeChunkWithHeader] so TCP sends full MSS segments without double-flushing.
 * - Supports zero-allocation pooled chunk reads via [readRawChunkPooled] and [ChunkBufferPool].
 */
class SocketFrameCodec(
    inputStream: InputStream,
    outputStream: OutputStream
) {
    private val bufferedIn = inputStream as? BufferedInputStream
        ?: BufferedInputStream(inputStream, STREAM_BUFFER_SIZE_BYTES)
    private val bufferedOut = outputStream as? BufferedOutputStream
        ?: BufferedOutputStream(outputStream, STREAM_BUFFER_SIZE_BYTES)

    private val dataIn = DataInputStream(bufferedIn)
    private val dataOut = DataOutputStream(bufferedOut)

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "frameType"
        encodeDefaults = true
    }

    /** Reusable scratch buffer for decoding small control JSON frames without per-frame heap churn. */
    private var jsonReadScratch = ByteArray(4096)

    /** Max single-frame JSON size (control frames only, not file bytes). 1MB is generous. */
    private val maxJsonFrameBytes = 1_000_000

    /** Max raw chunk size accepted at once (supports adaptive 64KB..512KB+ chunks up to 2MB). */
    private val maxRawChunkBytes = 2 * 1024 * 1024

    @Synchronized
    @Throws(Exception::class)
    fun writeFrame(frame: Frame) {
        val payload = json.encodeToString(frame).toByteArray(Charsets.UTF_8)
        dataOut.writeInt(payload.size)
        dataOut.write(payload)
        dataOut.flush()
    }

    /**
     * Coalesces the [Frame.FileChunkHeader] and its raw file chunk payload into a single
     * synchronized buffered write, flushing only when [flushImmediately] or [Frame.FileChunkHeader.isLastChunk]
     * is true. Avoids splitting header and payload across separate TCP packets.
     */
    @Synchronized
    @Throws(Exception::class)
    fun writeChunkWithHeader(
        header: Frame.FileChunkHeader,
        bytes: ByteArray,
        length: Int,
        flushImmediately: Boolean = true,
    ) {
        val headerPayload = json.encodeToString<Frame>(header).toByteArray(Charsets.UTF_8)
        dataOut.writeInt(headerPayload.size)
        dataOut.write(headerPayload)
        dataOut.writeInt(length)
        dataOut.write(bytes, 0, length)
        if (flushImmediately || header.isLastChunk) {
            dataOut.flush()
        }
    }

    @Throws(Exception::class)
    fun readFrame(): Frame {
        val length = dataIn.readInt()
        if (length <= 0 || length > maxJsonFrameBytes) {
            throw IllegalStateException("Invalid frame length: $length")
        }
        val buffer = if (length <= jsonReadScratch.size) {
            jsonReadScratch
        } else {
            ByteArray(length).also { if (length <= 32 * 1024) jsonReadScratch = it }
        }
        dataIn.readFully(buffer, 0, length)
        val text = String(buffer, 0, length, Charsets.UTF_8)
        return json.decodeFromString(Frame.serializer(), text)
    }

    /** Writes a raw length-prefixed chunk of file bytes (no JSON wrapping). */
    @Synchronized
    @Throws(Exception::class)
    fun writeRawChunk(bytes: ByteArray, length: Int) {
        dataOut.writeInt(length)
        dataOut.write(bytes, 0, length)
        dataOut.flush()
    }

    /** Reads a raw length-prefixed chunk previously written by [writeRawChunk]. */
    @Throws(Exception::class)
    fun readRawChunk(): ByteArray {
        val length = dataIn.readInt()
        if (length <= 0 || length > maxRawChunkBytes) {
            throw IllegalStateException("Invalid raw chunk length: $length")
        }
        val buffer = ByteArray(length)
        dataIn.readFully(buffer)
        return buffer
    }

    /**
     * Reads a raw length-prefixed chunk into a reusable slab from [ChunkBufferPool].
     * Caller MUST return the slab via [ChunkBufferPool.release] after writing it to disk.
     */
    @Throws(Exception::class)
    fun readRawChunkPooled(): Pair<ByteArray, Int> {
        val length = dataIn.readInt()
        if (length <= 0 || length > maxRawChunkBytes) {
            throw IllegalStateException("Invalid raw chunk length: $length")
        }
        val slab = ChunkBufferPool.acquire(length)
        try {
            dataIn.readFully(slab, 0, length)
            return slab to length
        } catch (t: Throwable) {
            ChunkBufferPool.release(slab)
            throw t
        }
    }

    fun close() {
        runCatching { dataIn.close() }
        runCatching { dataOut.close() }
    }
}

/** Thrown when the peer closes the connection cleanly mid-read. */
class PeerDisconnectedException(cause: Throwable? = null) : EOFException(cause?.message)
