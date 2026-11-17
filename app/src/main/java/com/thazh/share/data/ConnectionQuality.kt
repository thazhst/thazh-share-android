package com.thazh.share.data

import androidx.compose.ui.graphics.Color

/**
 * Represents the perceived quality of the active WiFi Direct link between
 * the two connected devices, derived from ping RTT + packet success rate.
 */
enum class ConnectionQuality(
    val label: String,
    val colorValue: Long,
    val darkColorValue: Long,
    val barsFilled: Int
) {
    STRONG(label = "Mạnh", colorValue = 0xFF34C759, darkColorValue = 0xFF30D158, barsFilled = 4),
    MEDIUM(label = "Trung bình", colorValue = 0xFFFF9500, darkColorValue = 0xFFFF9F0A, barsFilled = 3),
    WEAK(label = "Yếu", colorValue = 0xFFFF6B00, darkColorValue = 0xFFFF7A00, barsFilled = 2),
    POOR(label = "Kém", colorValue = 0xFFFF3B30, darkColorValue = 0xFFFF453A, barsFilled = 1),
    DISCONNECTED(label = "Mất kết nối", colorValue = 0xFF8E8E93, darkColorValue = 0xFF98989D, barsFilled = 0);

    val color: Color get() = Color(colorValue)

    fun color(isDark: Boolean): Color = Color(if (isDark) darkColorValue else colorValue)

    companion object {
        fun fromMetrics(avgLatencyMs: Long?, successRatio: Float, isConnected: Boolean): ConnectionQuality {
            if (!isConnected || avgLatencyMs == null) return DISCONNECTED
            if (successRatio < 0.4f) return POOR

            return when {
                avgLatencyMs <= 60L && successRatio >= 0.95f -> STRONG
                avgLatencyMs <= 150L && successRatio >= 0.85f -> MEDIUM
                avgLatencyMs <= 400L && successRatio >= 0.6f -> WEAK
                else -> POOR
            }
        }
    }
}
