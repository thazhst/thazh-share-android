package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
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
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.segmentedControlBackground
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioCard

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

    val accentColor = when (mode) {
        DeviceVisibilityMode.HIDDEN -> colors.secondaryContent
        DeviceVisibilityMode.TEMPORARY_10_MIN -> semantic.accentBlue
        DeviceVisibilityMode.ALWAYS_VISIBLE -> semantic.accentGreen
    }

    GlasmioCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    Text(
                        text = stringResource(R.string.visibility_card_title),
                        style = GlasmioTheme.textStyles.headline,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (mode == DeviceVisibilityMode.TEMPORARY_10_MIN) {
                    Box(
                        modifier = Modifier
                            .clip(ContinuousCapsule)
                            .background(semantic.accentBlue.copy(alpha = 0.14f))
                            .border(1.dp, semantic.accentBlue.copy(alpha = 0.3f), ContinuousCapsule)
                            .clickable(role = Role.Button) {
                                onSelectMode(DeviceVisibilityMode.TEMPORARY_10_MIN)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("visibility_timer_badge"),
                    ) {
                        Text(
                            text = stringResource(R.string.visibility_timer_badge, formattedTimer),
                            style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                            color = semantic.accentBlue,
                            maxLines = 1,
                        )
                    }
                } else if (mode == DeviceVisibilityMode.ALWAYS_VISIBLE) {
                    Box(
                        modifier = Modifier
                            .clip(ContinuousCapsule)
                            .background(semantic.accentGreen.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.visibility_mode_always),
                            style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                            color = semantic.accentGreen,
                            maxLines = 1,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(14.dp))
                    .background(colors.segmentedControlBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                VisibilityOptionChip(
                    label = stringResource(R.string.visibility_mode_hidden),
                    symbol = ThazhSymbols.VisibilityOff,
                    selected = mode == DeviceVisibilityMode.HIDDEN,
                    selectedColor = colors.secondaryContent,
                    onClick = { onSelectMode(DeviceVisibilityMode.HIDDEN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("visibility_option_hidden"),
                )
                VisibilityOptionChip(
                    label = if (mode == DeviceVisibilityMode.TEMPORARY_10_MIN) {
                        formattedTimer
                    } else {
                        stringResource(R.string.visibility_mode_10min)
                    },
                    symbol = ThazhSymbols.Timer,
                    selected = mode == DeviceVisibilityMode.TEMPORARY_10_MIN,
                    selectedColor = semantic.accentBlue,
                    onClick = { onSelectMode(DeviceVisibilityMode.TEMPORARY_10_MIN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("visibility_option_10min"),
                )
                VisibilityOptionChip(
                    label = stringResource(R.string.visibility_mode_always),
                    symbol = ThazhSymbols.AllInclusive,
                    selected = mode == DeviceVisibilityMode.ALWAYS_VISIBLE,
                    selectedColor = semantic.accentGreen,
                    onClick = { onSelectMode(DeviceVisibilityMode.ALWAYS_VISIBLE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("visibility_option_always"),
                )
            }

            Text(
                text = when (mode) {
                    DeviceVisibilityMode.HIDDEN -> stringResource(R.string.visibility_desc_hidden)
                    DeviceVisibilityMode.TEMPORARY_10_MIN -> stringResource(R.string.visibility_desc_10min, formattedTimer)
                    DeviceVisibilityMode.ALWAYS_VISIBLE -> stringResource(R.string.visibility_desc_always)
                },
                style = GlasmioTheme.textStyles.footnote,
                color = colors.secondaryContent,
            )
        }
    }
}

@Composable
private fun VisibilityOptionChip(
    label: String,
    symbol: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val shape = ContinuousRoundedRectangle(11.dp)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SfIcon(
            systemName = symbol,
            contentDescription = null,
            tint = if (selected) Color.White else colors.secondaryContent,
            size = 15.dp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = GlasmioTheme.textStyles.caption1.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) Color.White else colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
