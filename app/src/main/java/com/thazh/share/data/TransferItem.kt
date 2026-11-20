package com.thazh.share.data

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.util.UUID

enum class TransferDirection { SENT, RECEIVED }

enum class TransferState { PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED }

/** A file picked by the user to send, with metadata resolved from its content [uri]. */
@Immutable
data class PickedFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String
)

/**
 * One row of the active session's transfer list: a file being sent or received.
 * Held in memory by [com.thazh.share.service.WifiDirectService]; once a received
 * file finishes it is persisted in [ReceivedFilesRepository].
 */
@Immutable
data class TransferItem(
    val id: String = UUID.randomUUID().toString(),
    val direction: TransferDirection,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val state: TransferState = TransferState.PENDING,
    val bytesTransferred: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val peerName: String = "",
    val peerDeviceAddress: String = "",
    /** Outgoing only: content Uri of the source file, kept so a failed send can be retried. */
    val sourceUri: String? = null,
    /** Incoming only: path of the finished file inside the received-files folder. */
    val localPath: String? = null,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = when {
            fileSizeBytes > 0 -> (bytesTransferred.toFloat() / fileSizeBytes.toFloat()).coerceIn(0f, 1f)
            state == TransferState.COMPLETED -> 1f
            else -> 0f
        }

    /** Still waiting for its turn or actively moving bytes. */
    val isActive: Boolean
        get() = state == TransferState.PENDING || state == TransferState.IN_PROGRESS

    val isFinished: Boolean get() = !isActive

    val category: FileCategory get() = FileCategory.from(mimeType, fileName)

    /** Estimated seconds left, or null while the speed is still unknown. */
    val etaSeconds: Long?
        get() = if (state == TransferState.IN_PROGRESS && speedBytesPerSec > 0 && fileSizeBytes > bytesTransferred) {
            (fileSizeBytes - bytesTransferred) / speedBytesPerSec
        } else {
            null
        }
}
