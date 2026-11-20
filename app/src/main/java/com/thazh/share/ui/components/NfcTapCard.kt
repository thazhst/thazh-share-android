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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * Glasmio card displaying NFC Tap-to-Connect status and explicit Send vs Receive role guidance:
 * - On the **Send tab** (`isSendMode = true`): operates in NFC Reader Mode (PCD) — user touches
 *   this device to a device on the Receive tab to exchange credentials and initiate Wi-Fi Direct.
 * - On the **Receive tab** (`isSendMode = false`): operates in NFC HCE Card Emulation Mode — user
 *   waits for a device on the Send tab to touch and automatically accept the connection.
 */
@Composable
fun NfcTapCard(
    isNfcEnabled: Boolean,
    nfcPairingPeerName: String?,
    onOpenNfcSettings: () -> Unit,
    isNfcSupported: Boolean = true,
    isSendMode: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!isNfcSupported) return

    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val isPairing = !nfcPairingPeerName.isNullOrBlank()
    val cardShape = ContinuousRoundedRectangle(GlasmioTheme.dimensions.regularCornerRadius)

    val containerColor = when {
        isPairing -> semantic.accentBlue.copy(alpha = if (colors.isDark) 0.24f else 0.14f)
        else -> Color.Transparent
    }

    val iconTint = when {
        isPairing -> semantic.accentBlue
        isNfcEnabled -> if (isSendMode) semantic.accentBlue else semantic.accentGreen
        else -> colors.secondaryContent
    }

    val roleBadgeText = if (isSendMode) {
        stringResource(R.string.nfc_role_badge_send)
    } else {
        stringResource(R.string.nfc_role_badge_receive)
    }

    val subtitleText = when {
        isPairing -> stringResource(R.string.nfc_card_connecting, nfcPairingPeerName!!)
        isNfcEnabled -> if (isSendMode) {
            stringResource(R.string.nfc_card_ready_send_tab)
        } else {
            stringResource(R.string.nfc_card_ready_receive_tab)
        }
        else -> stringResource(R.string.nfc_card_disabled)
    }

    GlasmioCard(modifier = modifier.fillMaxWidth().testTag("nfc_tap_card")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor, cardShape)
                .then(
                    if (isPairing) {
                        Modifier.border(
                            width = 1.dp,
                            color = semantic.accentBlue.copy(alpha = 0.40f),
                            shape = cardShape,
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                if (isPairing) {
                    GlasmioActivityIndicator(
                        size = 20.dp,
                        strokeWidth = 2.2.dp,
                        color = iconTint,
                    )
                } else {
                    SfIcon(
                        systemName = ThazhSymbols.Nfc,
                        contentDescription = null,
                        tint = iconTint,
                        size = 20.dp,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlasmioText(
                        text = stringResource(R.string.nfc_card_title),
                        style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isNfcEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousCapsule)
                                .background(iconTint.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            GlasmioText(
                                text = roleBadgeText,
                                style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
                                color = iconTint,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                GlasmioText(
                    text = subtitleText,
                    style = GlasmioTheme.textStyles.caption1,
                    color = if (isPairing) colors.content else colors.secondaryContent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!isNfcEnabled) {
                GlasmioButton(
                    onClick = onOpenNfcSettings,
                    emphasis = GlassEmphasis.Regular,
                    size = GlasmioButtonSize.Small,
                    modifier = Modifier.testTag("nfc_enable_button"),
                ) {
                    GlasmioText(
                        text = stringResource(R.string.nfc_open_settings),
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accent,
                    )
                }
            } else if (!isPairing) {
                Row(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(semantic.accentGreen.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(semantic.accentGreen),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlasmioText(
                        text = stringResource(R.string.nfc_status_ready),
                        style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
                        color = semantic.accentGreen,
                    )
                }
            }
        }
    }
}
