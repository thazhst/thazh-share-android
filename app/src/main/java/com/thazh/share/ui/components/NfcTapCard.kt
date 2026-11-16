package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard

@Composable
fun NfcTapCard(
    isNfcEnabled: Boolean,
    nfcPairingPeerName: String?,
    onOpenNfcSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val isPairing = !nfcPairingPeerName.isNullOrBlank()
    val tintColor = when {
        isPairing -> semantic.accentBlue
        isNfcEnabled -> semantic.accentCyan
        else -> semantic.warningOrange
    }
    val cardShape = ContinuousRoundedRectangle(GlasmioTheme.dimensions.regularCornerRadius)

    GlasmioCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nfc_tap_card"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(tintColor.copy(alpha = if (colors.isDark) 0.14f else 0.08f), cardShape)
                .border(1.dp, tintColor.copy(alpha = 0.28f), cardShape)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(ContinuousRoundedRectangle(12.dp))
                        .background(tintColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isPairing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = semantic.accentBlue,
                            strokeWidth = 2.2.dp,
                        )
                    } else {
                        SfIcon(
                            systemName = ThazhSymbols.Nfc,
                            contentDescription = stringResource(R.string.nfc_card_title),
                            tint = tintColor,
                            size = 24.dp,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.nfc_card_title),
                            style = GlasmioTheme.textStyles.headline,
                            color = colors.content,
                        )
                        if (isNfcEnabled && !isPairing) {
                            Box(
                                modifier = Modifier
                                    .clip(ContinuousCapsule)
                                    .background(semantic.accentGreen.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.nfc_status_ready),
                                    style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                                    color = semantic.accentGreen,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = when {
                            isPairing -> stringResource(R.string.nfc_card_connecting, nfcPairingPeerName!!)
                            isNfcEnabled -> stringResource(R.string.nfc_card_ready)
                            else -> stringResource(R.string.nfc_card_disabled)
                        },
                        style = GlasmioTheme.textStyles.footnote,
                        color = colors.secondaryContent,
                    )
                }

                if (!isNfcEnabled && !isPairing) {
                    Spacer(modifier = Modifier.width(4.dp))
                    GlasmioButton(
                        onClick = onOpenNfcSettings,
                        emphasis = GlassEmphasis.Prominent,
                        navigationSurfaceColor = semantic.warningOrange,
                        size = GlasmioButtonSize.Medium,
                        modifier = Modifier.testTag("enable_nfc_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.nfc_open_settings),
                            style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
