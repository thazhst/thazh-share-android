package com.thazh.share.network

import kotlinx.serialization.Serializable

/**
 * Application-layer protocol spoken over the raw TCP socket that WiFi
 * Direct's group owner/client connection provides for direct file sharing.
 */
@Serializable
sealed class Frame {

    @Serializable
    data class Hello(
        val displayName: String,
        val deviceAddress: String,
        val protocolVersion: Int = PROTOCOL_VERSION
    ) : Frame()

    @Serializable
    data class FileOffer(
        val messageId: String,
        val fileName: String,
        val fileSizeBytes: Long,
        val mimeType: String,
        val messageType: String = "FILE",
        val timestampEpochMs: Long,
        val durationMs: Long? = null,
        val channel: String = CHANNEL_TRANSFER
    ) : Frame()

    @Serializable
    data class FileChunkHeader(
        val messageId: String,
        val chunkIndex: Int,
        val chunkSizeBytes: Int,
        val isLastChunk: Boolean
    ) : Frame()

    @Serializable
    data class FileAck(
        val messageId: String,
        val success: Boolean,
        val cancelled: Boolean = false
    ) : Frame()

    @Serializable
    data class NameChanged(
        val newDisplayName: String
    ) : Frame()

    /** Lightweight ping/pong used to measure link RTT + loss. */
    @Serializable
    data class Ping(val sequence: Long, val sentAtEpochMs: Long) : Frame()

    @Serializable
    data class Pong(val sequence: Long, val echoedSentAtEpochMs: Long) : Frame()

    companion object {
        const val PROTOCOL_VERSION = 2
        const val CHANNEL_TRANSFER = "TRANSFER"
    }
}
