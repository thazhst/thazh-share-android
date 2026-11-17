package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.thazh.share.R
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl

@Composable
fun VisibilitySelectorCard(
    mode: DeviceVisibilityMode,
    remainingSeconds: Int?,
    onSelectMode: (DeviceVisibilityMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val formattedTimer = remainingSeconds?.let { WifiDirectService.formatCountdown(it) } ?: "10:00"
    val badgeBgAlpha = if (colors.isDark) 0.22f else 0.14f

    val accentColor = when (mode) {
        DeviceVisibilityMode.HIDDEN -> colors.secondaryContent
        DeviceVisibilityMode.TEMPORARY_10_MIN -> semantic.accentBlue
        DeviceVisibilityMode.ALWAYS_VISIBLE -> semantic.accentGreen
    }

    val hiddenLabel = stringResource(R.string.visibility_mode_hidden)
    val tenMinLabel = stringResource(R.string.visibility_mode_10min)
    val alwaysLabel = stringResource(R.string.visibility_mode_always)

    val segmentItems = listOf(
        DeviceVisibilityMode.HIDDEN to hiddenLabel,
        DeviceVisibilityMode.TEMPORARY_10_MIN to tenMinLabel,
        DeviceVisibilityMode.ALWAYS_VISIBLE to alwaysLabel,
    )

    GlasmioCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    SfIcon(
                        systemName = when (mode) {
                            DeviceVisibilityMode.HIDDEN -> ThazhSymbols.VisibilityOff
                            DeviceVisibilityMode.TEMPORARY_10_MIN -> ThazhSymbols.Timer
                            DeviceVisibilityMode.ALWAYS_VISIBLE -> ThazhSymbols.AllInclusive
                        },
                        contentDescription = null,
                        tint = accentColor,
                        size = 20.dp,
                    )
                    GlasmioText(
                        text = stringResource(R.string.visibility_card_title),
                        style = GlasmioTheme.textStyles.headline,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                when (mode) {
                    DeviceVisibilityMode.TEMPORARY_10_MIN -> {
                        Row(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(semantic.accentBlue.copy(alpha = badgeBgAlpha))
                                .border(1.dp, semantic.accentBlue.copy(alpha = 0.34f), ContinuousCapsule)
                                .clickable(role = Role.Button) {
                                    onSelectMode(DeviceVisibilityMode.TEMPORARY_10_MIN)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("visibility_timer_badge"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            GlasmioText(
                                text = stringResource(R.string.visibility_timer_badge, formattedTimer),
                                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                color = semantic.accentBlue,
                                maxLines = 1,
                            )
                            SfIcon(
                                systemName = ThazhSymbols.Refresh,
                                contentDescription = stringResource(R.string.visibility_timer_reset),
                                tint = semantic.accentBlue,
                                size = 12.dp,
                            )
                        }
                    }

                    DeviceVisibilityMode.ALWAYS_VISIBLE -> {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(semantic.accentGreen.copy(alpha = badgeBgAlpha))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("visibility_always_badge"),
                        ) {
                            GlasmioText(
                                text = stringResource(R.string.visibility_active_always_badge),
                                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                color = semantic.accentGreen,
                                maxLines = 1,
                            )
                        }
                    }

                    DeviceVisibilityMode.HIDDEN -> {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(colors.secondaryContent.copy(alpha = if (colors.isDark) 0.16f else 0.10f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("visibility_hidden_badge"),
                        ) {
                            GlasmioText(
                                text = hiddenLabel,
                                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.Medium),
                                color = colors.secondaryContent,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            GlasmioSegmentedControl(
                items = segmentItems,
                selected = mode,
                onSelected = onSelectMode,
                height = 44.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visibility_segmented_control"),
            ) { value, isSelected ->
                val symbol = when (value) {
                    DeviceVisibilityMode.HIDDEN -> ThazhSymbols.VisibilityOff
                    DeviceVisibilityMode.TEMPORARY_10_MIN -> ThazhSymbols.Timer
                    DeviceVisibilityMode.ALWAYS_VISIBLE -> ThazhSymbols.AllInclusive
                }
                val label = when (value) {
                    DeviceVisibilityMode.HIDDEN -> hiddenLabel
                    DeviceVisibilityMode.TEMPORARY_10_MIN -> tenMinLabel
                    DeviceVisibilityMode.ALWAYS_VISIBLE -> alwaysLabel
                }
                val itemTint = when {
                    isSelected && value == DeviceVisibilityMode.TEMPORARY_10_MIN ->
                        if (colors.isDark) Color.White else semantic.accentBlue
                    isSelected && value == DeviceVisibilityMode.ALWAYS_VISIBLE ->
                        if (colors.isDark) Color.White else semantic.accentGreen
                    isSelected -> colors.content
                    else -> colors.secondaryContent
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp),
                ) {
                    SfIcon(
                        systemName = symbol,
                        contentDescription = null,
                        tint = itemTint,
                        size = 15.dp,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    GlasmioText(
                        text = label,
                        style = GlasmioTheme.textStyles.caption1.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (isSelected) colors.content else colors.secondaryContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
