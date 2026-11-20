package com.thazh.share.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.thazh.share.data.PickedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object FileActions {

    private const val TAG = "FileActions"

    const val EXPORT_FOLDER = "Thazh Share"

    private const val SIZE_PROBE_BUFFER_BYTES = 64 * 1024

    fun uriFor(context: Context, file: File): Uri? {
        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    fun openFile(context: Context, file: File, mimeType: String?): Boolean {
        if (!file.exists()) return false
        val uri = uriFor(context, file) ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType?.takeIf { it.isNotBlank() } ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun shareFiles(context: Context, files: List<Pair<File, String?>>, chooserTitle: String): Boolean {
        val existing = files.filter { it.first.exists() }
        val uris = existing.mapNotNull { uriFor(context, it.first) }
        if (uris.isEmpty()) return false

        val mimeTypes = existing
            .map { it.second?.takeIf { type -> type.isNotBlank() } ?: "*/*" }
            .distinct()
        val shareType = when {
            mimeTypes.size == 1 -> mimeTypes.first()
            mimeTypes.map { it.substringBefore('/') }.distinct().size == 1 -> mimeTypes.first().substringBefore('/') + "/*"
            else -> "*/*"
        }

        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
        send.type = shareType
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val chooser = Intent.createChooser(send, chooserTitle).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    suspend fun exportToDownloads(context: Context, source: File, displayName: String, mimeType: String?): Boolean =
        withContext(Dispatchers.IO) {
            if (!source.isFile) return@withContext false
            val name = FileNameUtils.sanitize(displayName)
            val mime = mimeType?.takeIf { it.isNotBlank() } ?: FileNameUtils.guessMimeType(name)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    exportViaMediaStore(context, source, name, mime)
                } else {
                    exportLegacy(context, source, name, mime)
                }
            } catch (e: Exception) {
                Log.w(TAG, "exportToDownloads failed: ${e.message}", e)
                false
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportViaMediaStore(context: Context, source: File, name: String, mime: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            val output = resolver.openOutputStream(target) ?: throw IOException("Không mở được tệp đích")
            output.use { out ->
                val outChannel = (out as? java.io.FileOutputStream)?.channel
                if (outChannel != null) {
                    java.io.FileInputStream(source).channel.use { inChannel ->
                        var position = 0L
                        val size = inChannel.size()
                        while (position < size) {
                            val transferred = inChannel.transferTo(position, size - position, outChannel)
                            if (transferred <= 0L) break
                            position += transferred
                        }
                    }
                } else {
                    source.inputStream().buffered(SIZE_PROBE_BUFFER_BYTES).use { input ->
                        input.copyTo(out, SIZE_PROBE_BUFFER_BYTES)
                    }
                }
            }

            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(target, done, null, null)
            true
        } catch (e: Exception) {
            runCatching { resolver.delete(target, null, null) }
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun exportLegacy(context: Context, source: File, name: String, mime: String): Boolean {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(downloads, EXPORT_FOLDER)
        if (!folder.exists() && !folder.mkdirs()) return false
        val target = FileNameUtils.uniqueFile(folder, name)
        java.io.FileInputStream(source).channel.use { inChannel ->
            java.io.FileOutputStream(target).channel.use { outChannel ->
                var position = 0L
                val size = inChannel.size()
                while (position < size) {
                    val transferred = inChannel.transferTo(position, size - position, outChannel)
                    if (transferred <= 0L) break
                    position += transferred
                }
            }
        }
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mime), null)
        return true
    }

    fun queryPickedFile(context: Context, uri: Uri): PickedFile {
        // Fast-path O(1) resolution for direct file:// URIs from InAppFileMediaPickerSheet
        if (uri.scheme == "file") {
            val filePath = uri.path
            if (!filePath.isNullOrBlank()) {
                val localFile = File(filePath)
                if (localFile.exists()) {
                    val name = FileNameUtils.sanitize(localFile.name)
                    val mime = FileNameUtils.guessMimeType(name)
                    return PickedFile(
                        uri = uri,
                        name = name,
                        sizeBytes = localFile.length().coerceAtLeast(0L),
                        mimeType = mime,
                    )
                }
            }
        }

        val resolver = context.contentResolver
        var displayName: String? = null
        var reportedSize = 0L

        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) displayName = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) reportedSize = cursor.getLong(sizeIndex)
                }
            }
        }

        val name = FileNameUtils.sanitize(displayName ?: uri.lastPathSegment)
        val mime = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: FileNameUtils.guessMimeType(name)
        val size = if (reportedSize > 0L) reportedSize else measureSize(resolver, uri)
        return PickedFile(uri = uri, name = name, sizeBytes = size, mimeType = mime)
    }

    private fun measureSize(resolver: ContentResolver, uri: Uri): Long {
        val descriptorLength = runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }.getOrDefault(-1L)
        if (descriptorLength > 0L) return descriptorLength

        return runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(SIZE_PROBE_BUFFER_BYTES)
                var total = 0L
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                }
                total
            } ?: 0L
        }.getOrDefault(0L)
    }
}
