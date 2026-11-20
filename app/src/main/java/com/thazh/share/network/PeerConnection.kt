package com.thazh.share.network

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

const val SHARE_SOCKET_PORT = 8988
private const val SOCKET_CONNECT_OVERALL_TIMEOUT_MS = 15_000L
private const val SOCKET_SINGLE_ATTEMPT_TIMEOUT_MS = 2_500
private const val SOCKET_RETRY_DELAY_MS = 100L
private const val SERVER_ACCEPT_TIMEOUT_MS = 30_000
private const val SOCKET_WINDOW_BUFFER_BYTES = 1024 * 1024 // 1 MB TCP send/receive window

sealed class SocketConnectionEvent {
    data object Connected : SocketConnectionEvent()
    data class FrameReceived(val frame: Frame) : SocketConnectionEvent()
    data class RawChunkReceived(val bytes: ByteArray) : SocketConnectionEvent()
    data class Disconnected(val reason: String?) : SocketConnectionEvent()
}

class PeerConnection(
    private val scope: CoroutineScope
) {
    @Volatile
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var codec: SocketFrameCodec? = null
    private var readJob: Job? = null
    private val writeMutex = Mutex()

    /**
     * Optional fast-path handler invoked directly on the [Dispatchers.IO] socket read thread
     * using reusable [ChunkBufferPool] slabs, bypassing Main-thread event dispatching and
     * eliminating per-chunk byte array allocations.
     */
    @Volatile
    var directChunkHandler: (suspend (header: Frame.FileChunkHeader, buffer: ByteArray, length: Int) -> Unit)? = null

    private val _events = MutableSharedFlow<SocketConnectionEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketConnectionEvent> = _events

    val isConnected: Boolean get() = socket?.isConnected == true && socket?.isClosed == false

    suspend fun startAsServer(groupOwnerAddress: String) {
        closeInternal(notify = false)
        withContextIO {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                runCatching { server.receiveBufferSize = SOCKET_WINDOW_BUFFER_BYTES }
                server.bind(InetSocketAddress(groupOwnerAddress, SHARE_SOCKET_PORT))
                server.soTimeout = SERVER_ACCEPT_TIMEOUT_MS
                serverSocket = server
                Log.d(TAG, "Server listening on $groupOwnerAddress:$SHARE_SOCKET_PORT")

                val client = server.accept()
                configureHighThroughputSocket(client)
                socket = client
                codec = SocketFrameCodec(client.getInputStream(), client.getOutputStream())
                Log.d(TAG, "Server accepted client ${client.inetAddress?.hostAddress}")
                _events.tryEmit(SocketConnectionEvent.Connected)
                startReadLoop()
            } catch (e: IOException) {
                Log.e(TAG, "startAsServer failed: ${e.message}", e)
                _events.tryEmit(SocketConnectionEvent.Disconnected(e.message))
            }
        }
    }

    suspend fun connectAsClient(groupOwnerAddress: String) {
        closeInternal(notify = false)
        withContextIO {
            val deadlineElapsed = SystemClock.elapsedRealtime() + SOCKET_CONNECT_OVERALL_TIMEOUT_MS
            var lastException: IOException? = null
            var attempt = 0

            while (SystemClock.elapsedRealtime() < deadlineElapsed) {
                currentCoroutineContext().ensureActive()
                attempt++
                val candidate = Socket()
                try {
                    configureHighThroughputSocket(candidate)
                    candidate.connect(
                        InetSocketAddress(groupOwnerAddress, SHARE_SOCKET_PORT),
                        SOCKET_SINGLE_ATTEMPT_TIMEOUT_MS,
                    )
                    socket = candidate
                    codec = SocketFrameCodec(candidate.getInputStream(), candidate.getOutputStream())
                    Log.d(TAG, "Client connected to $groupOwnerAddress:$SHARE_SOCKET_PORT on attempt #$attempt")
                    _events.tryEmit(SocketConnectionEvent.Connected)
                    startReadLoop()
                    return@withContextIO
                } catch (e: IOException) {
                    lastException = e
                    runCatching { candidate.close() }
                    if (SystemClock.elapsedRealtime() + SOCKET_RETRY_DELAY_MS >= deadlineElapsed) {
                        break
                    }
                    delay(SOCKET_RETRY_DELAY_MS)
                }
            }

            Log.e(TAG, "connectAsClient failed after $attempt attempts: ${lastException?.message}", lastException)
            _events.tryEmit(SocketConnectionEvent.Disconnected(lastException?.message))
        }
    }

    private fun configureHighThroughputSocket(s: Socket) {
        runCatching { s.tcpNoDelay = true }
        runCatching { s.keepAlive = true }
        runCatching { s.sendBufferSize = SOCKET_WINDOW_BUFFER_BYTES }
        runCatching { s.receiveBufferSize = SOCKET_WINDOW_BUFFER_BYTES }
        // IPTOS_LOWDELAY (0x10) | IPTOS_THROUGHPUT (0x08) for high-speed P2P streaming
        runCatching { s.trafficClass = 0x10 or 0x08 }
    }

    private fun startReadLoop() {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val c = codec ?: return@launch
            try {
                while (isConnected) {
                    val frame = c.readFrame()
                    when (frame) {
                        is Frame.FileChunkHeader -> {
                            val fastHandler = directChunkHandler
                            if (fastHandler != null) {
                                val (slab, len) = c.readRawChunkPooled()
                                try {
                                    fastHandler(frame, slab, len)
                                } finally {
                                    ChunkBufferPool.release(slab)
                                }
                            } else {
                                _events.emit(SocketConnectionEvent.FrameReceived(frame))
                                val raw = c.readRawChunk()
                                _events.emit(SocketConnectionEvent.RawChunkReceived(raw))
                            }
                        }
                        else -> _events.emit(SocketConnectionEvent.FrameReceived(frame))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Read loop ended: ${e.message}")
                _events.emit(SocketConnectionEvent.Disconnected(e.message))
            }
        }
    }

    suspend fun send(frame: Frame): Boolean = writeMutex.withLock {
        val c = codec ?: return@withLock false
        return@withLock try {
            withContextIO { c.writeFrame(frame) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "send failed: ${e.message}", e)
            false
        }
    }

    /**
     * Writes a [Frame.FileChunkHeader] and its raw payload bytes in a single coalesced operation.
     * Flushes periodically (every 4 chunks) and always on the final chunk to keep the 1 MB TCP
     * send pipe full without per-chunk flush stalls.
     */
    suspend fun sendFileChunk(header: Frame.FileChunkHeader, bytes: ByteArray, length: Int): Boolean = writeMutex.withLock {
        val c = codec ?: return@withLock false
        return@withLock try {
            val shouldFlush = header.isLastChunk || (header.chunkIndex and 3) == 3
            c.writeChunkWithHeader(header, bytes, length, flushImmediately = shouldFlush)
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendFileChunk failed: ${e.message}", e)
            false
        }
    }

    fun close() {
        scope.launch { closeInternal(notify = true) }
    }

    private suspend fun closeInternal(notify: Boolean) {
        readJob?.cancel()
        readJob = null
        withContextIO {
            runCatching { codec?.close() }
            runCatching { socket?.close() }
            runCatching { serverSocket?.close() }
        }
        socket = null
        serverSocket = null
        codec = null
        if (notify) _events.tryEmit(SocketConnectionEvent.Disconnected("closed"))
    }

    companion object {
        private const val TAG = "PeerConnection"
    }
}

private suspend inline fun <T> withContextIO(crossinline block: suspend () -> T): T =
    kotlinx.coroutines.withContext(Dispatchers.IO) { block() }
