package com.thazh.share.data

import com.thazh.share.util.FileNameUtils
import kotlinx.serialization.Serializable

/** Coarse file kind, used for the icon and the filter chips of the received-files manager. */
enum class FileCategory {
    IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, OTHER;

    companion object {
        private val documentExtensions = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf",
            "odt", "ods", "odp", "csv", "md", "epub", "json", "xml", "html", "htm"
        )
        private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz")
        private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp")
        private val videoExtensions = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v")
        private val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg", "opus", "flac", "amr")

        /** Decides from the MIME type first and falls back to the file extension when the MIME type is generic. */
        fun from(mimeType: String?, fileName: String): FileCategory {
            val mime = mimeType.orEmpty().lowercase()
            val ext = FileNameUtils.extensionOf(fileName)
            return when {
                mime.startsWith("image/") || ext in imageExtensions -> IMAGE
                mime.startsWith("video/") || ext in videoExtensions -> VIDEO
                mime.startsWith("audio/") || ext in audioExtensions -> AUDIO
                ext == "apk" || mime == "application/vnd.android.package-archive" -> APK
                ext in archiveExtensions || mime == "application/zip" || mime == "application/x-rar-compressed" -> ARCHIVE
                mime.startsWith("text/") || mime == "application/pdf" || ext in documentExtensions -> DOCUMENT
                else -> OTHER
            }
        }
    }
}

/**
 * A file that arrived through the "Truyền tệp" tab and now lives in the
 * app's private received-files folder. Persisted (as a JSON index, see
 * [ReceivedFilesRepository]) so the manager screen survives restarts.
 *
 * [seen] is false until the manager screen has been shown once after the
 * file arrived; it drives the "Mới" badge and the unread counter on the tab.
 */
@Serializable
data class ReceivedFile(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val filePath: String,
    val peerName: String = "",
    val peerDeviceAddress: String = "",
    val receivedAtEpochMs: Long,
    val seen: Boolean = false
) {
    val category: FileCategory get() = FileCategory.from(mimeType, fileName)
}
