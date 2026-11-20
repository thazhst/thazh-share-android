package com.thazh.share.network

import android.content.Context
import android.os.SystemClock
import com.thazh.share.data.PickedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.coroutines.coroutineContext

enum class CatboxExpiry(
    val code: String,
    val litterboxTimeParam: String?,
    val durationMs: Long,
) {
    PERMANENT(code = "PERMANENT", litterboxTimeParam = null, durationMs = 0L),
    ONE_HOUR(code = "1H", litterboxTimeParam = "1h", durationMs = 3_600_000L),
    TWELVE_HOURS(code = "12H", litterboxTimeParam = "12h", durationMs = 43_200_000L),
    ONE_DAY(code = "24H", litterboxTimeParam = "24h", durationMs = 86_400_000L),
    THREE_DAYS(code = "72H", litterboxTimeParam = "72h", durationMs = 259_200_000L);

    val isTemporary: Boolean get() = litterboxTimeParam != null
}

data class CatboxUploadProgress(
    val fileName: String,
    val bytesUploaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
) {
    val fraction: Float
        get() = if (totalBytes > 0L) {
            (bytesUploaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

data class CatboxUploadResult(
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val url: String,
    val expiry: CatboxExpiry,
    val expiresAtEpochMs: Long,
    val note: String,
    val uploadedAtEpochMs: Long,
)

/**
 * High-speed HTTP multipart/form-data uploader to Catbox.moe and Litterbox.
 * Uses 128 KB buffered I/O slabs from [ChunkBufferPool] and [NativeCoreEngine.computeEwmaSpeed].
 */
object CatboxUploader {
    private const val CATBOX_API_URL = "https://catbox.moe/user/api.php"
    private const val LITTERBOX_API_URL = "https://litterbox.catbox.moe/resources/internals/api.php"
    private const val UPLOAD_CHUNK_SIZE = 128 * 1024
    private const val CRLF = "\r\n"

    suspend fun uploadFile(
        context: Context,
        file: PickedFile,
        expiry: CatboxExpiry,
        note: String = "",
        userHash: String? = null,
        onProgress: (CatboxUploadProgress) -> Unit = {},
    ): Result<CatboxUploadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary = "----ThazhShareCatboxBoundary" + UUID.randomUUID().toString().replace("-", "")
            val targetEndpoint = if (expiry.isTemporary) LITTERBOX_API_URL else CATBOX_API_URL

            val safeFileName = file.name
                .replace("\"", "_")
                .replace("\r", "")
                .replace("\n", "")
                .ifBlank { "file.bin" }
            val safeMime = file.mimeType.ifBlank { "application/octet-stream" }

            val preambleBuilder = StringBuilder(256)
            fun appendFormField(name: String, value: String) {
                preambleBuilder.append("--").append(boundary).append(CRLF)
                preambleBuilder.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(CRLF)
                preambleBuilder.append(CRLF)
                preambleBuilder.append(value).append(CRLF)
            }

            appendFormField("reqtype", "fileupload")
            if (expiry.isTemporary && expiry.litterboxTimeParam != null) {
                appendFormField("time", expiry.litterboxTimeParam)
            } else if (!userHash.isNullOrBlank()) {
                appendFormField("userhash", userHash.trim())
            }

            preambleBuilder.append("--").append(boundary).append(CRLF)
            preambleBuilder.append(
                "Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"$safeFileName\"$CRLF"
            )
            preambleBuilder.append("Content-Type: $safeMime$CRLF")
            preambleBuilder.append(CRLF)

            val preambleBytes = preambleBuilder.toString().toByteArray(StandardCharsets.UTF_8)
            val closingBytes = "$CRLF--$boundary--$CRLF".toByteArray(StandardCharsets.UTF_8)

            val connection = (URL(targetEndpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                useCaches = false
                connectTimeout = 20_000
                readTimeout = 90_000
                setRequestProperty("User-Agent", "ThazhShareAndroid/1.2 (+https://catbox.moe)")
                setRequestProperty("Accept", "text/plain, */*")
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                if (file.sizeBytes > 0L) {
                    val totalFixedLength = preambleBytes.size.toLong() + file.sizeBytes + closingBytes.size.toLong()
                    setFixedLengthStreamingMode(totalFixedLength)
                } else {
                    setChunkedStreamingMode(UPLOAD_CHUNK_SIZE)
                }
            }

            var pooledSlab: ByteArray? = null
            try {
                val rawStream = context.contentResolver.openInputStream(file.uri)
                    ?: throw IllegalStateException("Không thể mở tệp nguồn để tải lên")

                pooledSlab = ChunkBufferPool.acquire(UPLOAD_CHUNK_SIZE)
                val buffer = pooledSlab

                BufferedInputStream(rawStream, UPLOAD_CHUNK_SIZE).use { input ->
                    BufferedOutputStream(connection.outputStream, UPLOAD_CHUNK_SIZE).use { output ->
                        output.write(preambleBytes)

                        var uploaded = 0L
                        val total = file.sizeBytes.coerceAtLeast(0L)
                        var lastUiTime = SystemClock.elapsedRealtime()
                        var lastBytesForSpeed = 0L
                        var smoothedSpeed = 0L

                        onProgress(
                            CatboxUploadProgress(
                                fileName = file.name,
                                bytesUploaded = 0L,
                                totalBytes = total,
                                speedBytesPerSec = 0L,
                            )
                        )

                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer, 0, minOf(UPLOAD_CHUNK_SIZE, buffer.size))
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            uploaded += read

                            val now = SystemClock.elapsedRealtime()
                            val elapsed = now - lastUiTime
                            if (elapsed >= 110L || (total > 0L && uploaded >= total)) {
                                if (elapsed > 0L) {
                                    smoothedSpeed = NativeCoreEngine.computeEwmaSpeed(
                                        previousSpeedBytesPerSec = smoothedSpeed,
                                        deltaBytes = uploaded - lastBytesForSpeed,
                                        elapsedMs = elapsed,
                                    )
                                    lastBytesForSpeed = uploaded
                                    lastUiTime = now
                                }
                                onProgress(
                                    CatboxUploadProgress(
                                        fileName = file.name,
                                        bytesUploaded = uploaded,
                                        totalBytes = total.coerceAtLeast(uploaded),
                                        speedBytesPerSec = smoothedSpeed,
                                    )
                                )
                            }
                        }

                        output.write(closingBytes)
                        output.flush()
                    }
                }

                val statusCode = connection.responseCode
                val responseStream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
                val responseBody = responseStream?.use { stream ->
                    BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText().trim()
                }.orEmpty()

                if (statusCode !in 200..299 || !responseBody.startsWith("https://")) {
                    val detail = responseBody.ifBlank { "HTTP $statusCode" }
                    throw IllegalStateException("Máy chủ phản hồi lỗi: $detail")
                }

                val nowEpoch = System.currentTimeMillis()
                val expiresAt = if (expiry.isTemporary) nowEpoch + expiry.durationMs else 0L
                CatboxUploadResult(
                    fileName = file.name,
                    fileSizeBytes = file.sizeBytes,
                    mimeType = safeMime,
                    url = responseBody,
                    expiry = expiry,
                    expiresAtEpochMs = expiresAt,
                    note = note.trim(),
                    uploadedAtEpochMs = nowEpoch,
                )
            } finally {
                ChunkBufferPool.release(pooledSlab)
                connection.disconnect()
            }
        }
    }
}
