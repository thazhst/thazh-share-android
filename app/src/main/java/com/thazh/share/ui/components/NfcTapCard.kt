package com.thazh.share.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thazh.share.R
import com.thazh.share.ui.cupertino.CupertinoActivityIndicator
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.LiquidButton
import com.thazh.share.ui.cupertino.LiquidCard
import com.thazh.share.ui.theme.CupertinoTheme

@Composable
fun NfcTapCard(
    isNfcEnabled: Boolean,
    nfcPairingPeerName: String?,
    onOpenNfcSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CupertinoTheme.colors
    val isPairing = !nfcPairingPeerName.isNullOrBlank()
    val tintColor = when {
        isPairing -> colors.accentBlue
        isNfcEnabled -> colors.accentCyan
        else -> colors.warningOrange
    }

    LiquidCard(
        surfaceColor = tintColor.copy(alpha = if (colors.isLight) 0.12f else 0.16f),
        borderColor = tintColor.copy(alpha = 0.32f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("nfc_tap_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CupertinoTheme.shapes.iconBadge)
                    .background(tintColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPairing) {
                    CupertinoActivityIndicator(size = 20.dp, color = colors.accentBlue)
                } else {
                    CupertinoIcon(
                        imageVector = Icons.Rounded.Nfc,
                        contentDescription = stringResource(R.string.nfc_card_title),
                        tint = tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CupertinoText(
                        text = stringResource(R.string.nfc_card_title),
                        style = CupertinoTheme.typography.headline
                    )
                    if (isNfcEnabled && !isPairing) {
                        Box(
                            modifier = Modifier
                                .clip(CupertinoTheme.shapes.pill)
                                .background(colors.accentGreen.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            CupertinoText(
                                text = stringResource(R.string.nfc_status_ready),
                                style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accentGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                CupertinoText(
                    text = when {
                        isPairing -> stringResource(R.string.nfc_card_connecting, nfcPairingPeerName!!)
                        isNfcEnabled -> stringResource(R.string.nfc_card_ready)
                        else -> stringResource(R.string.nfc_card_disabled)
                    },
                    style = CupertinoTheme.typography.footnote,
                    color = colors.secondaryLabel
                )
            }

            if (!isNfcEnabled && !isPairing) {
                Spacer(modifier = Modifier.width(4.dp))
                LiquidButton(
                    onClick = onOpenNfcSettings,
                    tint = colors.warningOrange,
                    contentColor = Color.White,
                    height = 48.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("enable_nfc_button")
                ) {
                    CupertinoText(
                        text = stringResource(R.string.nfc_open_settings),
                        style = CupertinoTheme.typography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
