package com.thazh.share.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * Reads/writes [Frame]s as length-prefixed JSON over a raw socket stream,
 * and provides raw length-prefixed chunk read/write for file bytes so large
 * transfers don't pay JSON/base64 overhead.
 *
 * Not thread-safe for concurrent writes from multiple threads onto the same
 * stream — callers must serialize writes (see `PeerConnection`'s single
 * writer coroutine + Mutex).
 */
class SocketFrameCodec(
    inputStream: InputStream,
    outputStream: OutputStream
) {
    private val dataIn = DataInputStream(inputStream)
    private val dataOut = DataOutputStream(outputStream)

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "frameType"
        encodeDefaults = true
    }

    /** Max single-frame JSON size (control frames only, not file bytes). 1MB is generous. */
    private val maxJsonFrameBytes = 1_000_000

    /** Max raw chunk size accepted at once, to bound memory. 512KB chunks. */
    private val maxRawChunkBytes = 512 * 1024

    @Synchronized
    @Throws(Exception::class)
    fun writeFrame(frame: Frame) {
        val payload = json.encodeToString(frame).toByteArray(Charsets.UTF_8)
        dataOut.writeInt(payload.size)
        dataOut.write(payload)
        dataOut.flush()
    }

    @Throws(Exception::class)
    fun readFrame(): Frame {
        val length = dataIn.readInt()
        if (length <= 0 || length > maxJsonFrameBytes) {
            throw IllegalStateException("Invalid frame length: $length")
        }
        val buffer = ByteArray(length)
        dataIn.readFully(buffer)
        val text = String(buffer, Charsets.UTF_8)
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

    fun close() {
        runCatching { dataIn.close() }
        runCatching { dataOut.close() }
    }
}

/** Thrown when the peer closes the connection cleanly mid-read. */
class PeerDisconnectedException(cause: Throwable? = null) : EOFException(cause?.message)
