package com.thazh.share.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.thazh.share.R
import com.thazh.share.data.ConnectionQuality
import io.github.neoruaa.glasmio.core.GlasmioTheme

/**
 * Maps a Kalman-filtered RSSI (dBm) value to a 1..4 signal bar count.
 */
fun rssiToSignalBars(rssiDbm: Int?): Int = when {
    rssiDbm == null -> 4
    rssiDbm >= -58 -> 4
    rssiDbm >= -70 -> 3
    rssiDbm >= -82 -> 2
    else -> 1
}

@Composable
fun SignalStrengthMiniBars(
    barsFilled: Int,
    activeColor: Color,
    modifier: Modifier = Modifier,
    inactiveColor: Color = GlasmioTheme.colors.tertiaryContent.copy(alpha = 0.38f),
    width: Dp = 15.dp,
    height: Dp = 11.dp,
) {
    val clampedBars = barsFilled.coerceIn(0, 4)
    Canvas(modifier = modifier.size(width = width, height = height)) {
        val barCount = 4
        val gap = size.width * 0.13f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val heightFraction = 0.34f + (i * 0.22f)
            val barHeight = size.height * heightFraction
            val left = i * (barWidth + gap)
            val top = size.height - barHeight
            drawRoundRect(
                color = if (i < clampedBars) activeColor else inactiveColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
fun ConnectionQualityIndicator(
    quality: ConnectionQuality,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val colors = GlasmioTheme.colors
    val indicatorColor = quality.color(colors.isDark)
    val labelText = when (quality) {
        ConnectionQuality.STRONG -> stringResource(R.string.quality_strong)
        ConnectionQuality.MEDIUM -> stringResource(R.string.quality_medium)
        ConnectionQuality.WEAK -> stringResource(R.string.quality_weak)
        ConnectionQuality.POOR -> stringResource(R.string.quality_poor)
        ConnectionQuality.DISCONNECTED -> stringResource(R.string.quality_disconnected)
    }
    val inactiveColor = colors.tertiaryContent.copy(alpha = 0.38f)

    Row(
        modifier = modifier
            .clip(ContinuousCapsule)
            .background(indicatorColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = labelText },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        SignalStrengthMiniBars(
            barsFilled = quality.barsFilled,
            activeColor = indicatorColor,
            inactiveColor = inactiveColor,
            width = 16.dp,
            height = 12.dp,
        )
        if (showLabel) {
            GlasmioText(
                text = labelText,
                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = indicatorColor,
                maxLines = 1,
            )
        }
    }
}
