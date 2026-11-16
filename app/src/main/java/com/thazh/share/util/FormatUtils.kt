package com.thazh.share.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object FormatUtils {

    // Built once and reused rather than constructing a SimpleDateFormat on
    // every call: SimpleDateFormat re-parses its pattern string on every
    // instantiation (measurably expensive when it happens once per chat
    // bubble on every recomposition) and isn't thread-safe to share even if
    // it were cached. DateTimeFormatter is both cheaper to reuse and
    // genuinely thread-safe, and java.time is available unconditionally
    // from API 26 — this project's minSdk — with no desugaring needed.
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val fullDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${value.toInt()} ${units[unitIndex]}"
        } else {
            String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    fun formatTimestamp(epochMs: Long): String {
        return timeFormatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
    }

    fun formatFullDateTime(epochMs: Long): String {
        return fullDateTimeFormatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
    }

    fun formatRelativeTime(epochMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMs
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Vừa xong"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} phút trước"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} giờ trước"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} ngày trước"
            else -> formatFullDateTime(epochMs)
        }
    }

    fun formatProgressPercent(progress: Float): String {
        return "${(progress * 100).roundToInt()}%"
    }

    /** Formats a rough RSSI-based distance estimate, e.g. "~27m" or "~1.2km". */
    fun formatDistance(meters: Int): String {
        return if (meters < 1000) {
            "~${meters}m"
        } else {
            String.format(Locale.getDefault(), "~%.1fkm", meters / 1000.0)
        }
    }

    /** Transfer speed, e.g. "3.4 MB/s". */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond.coerceAtLeast(0L))}/s"
    }

    /** Remaining-time text for a transfer, e.g. "25 giây", "2 phút 05 giây", "1 giờ 10 phút". */
    fun formatEta(seconds: Long): String {
        val total = seconds.coerceAtLeast(1L)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60
        return when {
            hours > 0 -> "$hours giờ ${String.format(Locale.getDefault(), "%02d", minutes)} phút"
            minutes > 0 -> "$minutes phút ${String.format(Locale.getDefault(), "%02d", secs)} giây"
            else -> "$secs giây"
        }
    }
}
