package com.thazh.share.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
private const val SOCKET_CONNECT_TIMEOUT_MS = 15_000
private const val SERVER_ACCEPT_TIMEOUT_MS = 30_000

sealed class SocketConnectionEvent {
    data object Connected : SocketConnectionEvent()
    data class FrameReceived(val frame: Frame) : SocketConnectionEvent()
    data class RawChunkReceived(val bytes: ByteArray) : SocketConnectionEvent()
    data class Disconnected(val reason: String?) : SocketConnectionEvent()
}

class PeerConnection(
    private val scope: CoroutineScope
) {
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var codec: SocketFrameCodec? = null
    private var readJob: Job? = null
    private val writeMutex = Mutex()

    private val _events = MutableSharedFlow<SocketConnectionEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketConnectionEvent> = _events

    val isConnected: Boolean get() = socket?.isConnected == true && socket?.isClosed == false

    suspend fun startAsServer(groupOwnerAddress: String) {
        closeInternal(notify = false)
        withContextIO {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress(groupOwnerAddress, SHARE_SOCKET_PORT))
                server.soTimeout = SERVER_ACCEPT_TIMEOUT_MS
                serverSocket = server
                Log.d(TAG, "Server listening on $groupOwnerAddress:$SHARE_SOCKET_PORT")

                val client = server.accept()
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
            try {
                val s = Socket()
                s.connect(InetSocketAddress(groupOwnerAddress, SHARE_SOCKET_PORT), SOCKET_CONNECT_TIMEOUT_MS)
                socket = s
                codec = SocketFrameCodec(s.getInputStream(), s.getOutputStream())
                Log.d(TAG, "Client connected to $groupOwnerAddress:$SHARE_SOCKET_PORT")
                _events.tryEmit(SocketConnectionEvent.Connected)
                startReadLoop()
            } catch (e: IOException) {
                Log.e(TAG, "connectAsClient failed: ${e.message}", e)
                _events.tryEmit(SocketConnectionEvent.Disconnected(e.message))
            }
        }
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
                            _events.emit(SocketConnectionEvent.FrameReceived(frame))
                            val raw = c.readRawChunk()
                            _events.emit(SocketConnectionEvent.RawChunkReceived(raw))
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

    suspend fun sendFileChunk(header: Frame.FileChunkHeader, bytes: ByteArray, length: Int): Boolean = writeMutex.withLock {
        val c = codec ?: return@withLock false
        return@withLock try {
            withContextIO {
                c.writeFrame(header)
                c.writeRawChunk(bytes, length)
            }
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

private suspend inline fun <T> withContextIO(crossinline block: () -> T): T =
    kotlinx.coroutines.withContext(Dispatchers.IO) { block() }
