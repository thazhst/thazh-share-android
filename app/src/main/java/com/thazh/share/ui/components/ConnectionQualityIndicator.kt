package com.thazh.share.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.thazh.share.R
import com.thazh.share.data.ConnectionQuality
import io.github.neoruaa.glasmio.core.GlasmioTheme

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
    val inactiveColor = colors.tertiaryContent

    Row(
        modifier = modifier
            .clip(ContinuousCapsule)
            .background(indicatorColor.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = labelText },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Canvas(modifier = Modifier.size(width = 16.dp, height = 12.dp)) {
            val barCount = 4
            val gap = size.width * 0.12f
            val barWidth = (size.width - gap * (barCount - 1)) / barCount
            for (i in 0 until barCount) {
                val heightFraction = 0.35f + (i * 0.21f)
                val barHeight = size.height * heightFraction
                val left = i * (barWidth + gap)
                val top = size.height - barHeight
                drawRoundRect(
                    color = if (i < quality.barsFilled) indicatorColor else inactiveColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        if (showLabel) {
            Text(
                text = labelText,
                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = indicatorColor,
                maxLines = 1,
            )
        }
    }
}
