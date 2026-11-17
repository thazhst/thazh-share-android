package com.thazh.share.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import com.thazh.share.ui.components.GlasmioText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thazh.share.R
import com.thazh.share.data.ConnectionQuality
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.TransferItem
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.ConnectScreenState
import com.thazh.share.ui.components.ConnectionQualityIndicator
import com.thazh.share.ui.components.InteractiveRadarCard
import com.thazh.share.ui.components.NfcTapCard
import com.thazh.share.ui.components.RadarMode
import com.thazh.share.ui.components.VisibilitySelectorCard
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.FileActions
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle
import java.io.File

@Composable
fun ReceiveScreen(
    displayName: String,
    visibilityMode: DeviceVisibilityMode,
    visibilityRemainingSeconds: Int?,
    isDiscovering: Boolean,
    screenState: ConnectScreenState,
    connectedPeerName: String?,
    isSocketConnected: Boolean,
    connectionQuality: ConnectionQuality,
    estimatedDistanceMeters: Int?,
    peers: List<DiscoveredPeer> = emptyList(),
    transfers: List<TransferItem>,
    isBleSupported: Boolean = true,
    isBleEnabled: Boolean = true,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    nfcPairingPeerName: String?,
    bottomBarPadding: Dp = 96.dp,
    onSelectVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onStartDiscovery: () -> Unit,
    onConnectPeer: (DiscoveredPeer) -> Unit = {},
    onToggleBleFastDiscovery: (() -> Unit)? = null,
    onOpenNfcSettings: () -> Unit,
    onCancelTransfer: (String) -> Unit,
    onRetryTransfer: (String) -> Unit,
    onDismissTransfer: (String) -> Unit,
    onClearFinished: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic
    var showDisconnectDialog by remember { mutableStateOf(false) }

    val isConnected = screenState is ConnectScreenState.Connected
    val peerDisplay = connectedPeerName
        ?: (screenState as? ConnectScreenState.Connected)?.peerName
        ?: stringResource(R.string.transfer_unknown_peer)

    val topSubtitle = if (isConnected) {
        buildList {
            add(
                if (isSocketConnected) stringResource(R.string.transfer_send_subtitle, peerDisplay)
                else stringResource(R.string.transfer_connecting),
            )
            if (estimatedDistanceMeters != null) {
                add(FormatUtils.formatDistance(estimatedDistanceMeters))
            }
        }.joinToString(" • ")
    } else {
        when (visibilityMode) {
            DeviceVisibilityMode.HIDDEN -> stringResource(R.string.receive_subtitle_hidden)
            DeviceVisibilityMode.TEMPORARY_10_MIN -> stringResource(
                R.string.receive_subtitle_10min,
                WifiDirectService.formatCountdown(visibilityRemainingSeconds ?: 600),
            )
            DeviceVisibilityMode.ALWAYS_VISIBLE -> stringResource(R.string.receive_subtitle_always)
        }
    }

    GlasmioPinnedListPage(
        title = stringResource(R.string.receive_title),
        subtitle = topSubtitle,
        bottomPadding = bottomBarPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        backgroundColor = colors.groupedBackground.copy(
            alpha = if (appearance.reduceTransparency) 1f else 0.78f,
        ),
        modifier = modifier.fillMaxSize(),
        actions = {
            if (isConnected) {
                ConnectionQualityIndicator(
                    quality = connectionQuality,
                    showLabel = false,
                    modifier = Modifier.padding(end = 2.dp),
                )
                GlasmioIconButton(
                    onClick = { showDisconnectDialog = true },
                    modifier = Modifier.testTag("receive_disconnect_button"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.LinkOff,
                        contentDescription = stringResource(R.string.transfer_disconnect),
                        tint = colors.destructive,
                    )
                }
            }
            GlasmioIconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("receive_open_settings_button"),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                )
            }
        },
    ) {
        item(key = "receive_hero") {
            CenteredReceiveContent {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    InteractiveRadarCard(
                        mode = RadarMode.RECEIVE,
                        displayName = displayName,
                        visibilityMode = visibilityMode,
                        visibilityRemainingSeconds = visibilityRemainingSeconds,
                        isDiscovering = isDiscovering,
                        isConnecting = screenState is ConnectScreenState.Connecting || !nfcPairingPeerName.isNullOrBlank(),
                        isConnected = isConnected,
                        connectedPeerName = if (isConnected) peerDisplay else null,
                        connectedDistanceMeters = estimatedDistanceMeters,
                        peers = peers,
                        isBleSupported = isBleSupported,
                        isBleEnabled = isBleEnabled,
                        isNfcSupported = isNfcSupported,
                        isNfcEnabled = isNfcEnabled,
                        nfcPairingPeerName = nfcPairingPeerName,
                        onCenterClick = {
                            if (!isConnected && visibilityMode == DeviceVisibilityMode.HIDDEN) {
                                onStartDiscovery()
                            } else if (!isConnected) {
                                onStartDiscovery()
                            }
                        },
                        onPeerNodeClick = { peer -> onConnectPeer(peer) },
                        onToggleBleClick = onToggleBleFastDiscovery,
                        onNfcPillClick = onOpenNfcSettings,
                    )

                    if (!isConnected && visibilityMode == DeviceVisibilityMode.HIDDEN) {
                        GlasmioButton(
                            onClick = onStartDiscovery,
                            emphasis = GlassEmphasis.Prominent,
                            navigationSurfaceColor = semantic.accentBlue,
                            size = GlasmioButtonSize.Medium,
                            modifier = Modifier.testTag("receive_enable_visibility_button"),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                size = 18.dp,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            GlasmioText(
                                text = stringResource(R.string.visibility_enable_prompt),
                                style = GlasmioTheme.textStyles.headline,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        if (!isConnected) {
            item(key = "receive_visibility_card") {
                CenteredReceiveContent {
                    VisibilitySelectorCard(
                        mode = visibilityMode,
                        remainingSeconds = visibilityRemainingSeconds,
                        onSelectMode = onSelectVisibilityMode,
                    )
                }
            }
        }

        if (isNfcSupported && !isConnected) {
            item(key = "receive_nfc_card") {
                CenteredReceiveContent {
                    NfcTapCard(
                        isNfcEnabled = isNfcEnabled,
                        nfcPairingPeerName = nfcPairingPeerName,
                        onOpenNfcSettings = onOpenNfcSettings,
                    )
                }
            }
        }

        item(key = "receive_activity_header") {
            CenteredReceiveContent {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    GlasmioSectionTitle(
                        title = stringResource(R.string.transfer_activity_title),
                        modifier = Modifier.weight(1f),
                    )
                    if (transfers.any { it.isFinished }) {
                        GlasmioButton(
                            onClick = onClearFinished,
                            emphasis = GlassEmphasis.Regular,
                            size = GlasmioButtonSize.Small,
                            modifier = Modifier.testTag("receive_clear_finished_button"),
                        ) {
                            GlasmioText(
                                text = stringResource(R.string.transfer_clear_finished),
                                style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.accent,
                            )
                        }
                    }
                }
            }
        }

        if (transfers.isEmpty()) {
            item(key = "receive_empty") {
                CenteredReceiveContent {
                    GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SfIcon(
                                systemName = ThazhSymbols.Download,
                                contentDescription = null,
                                tint = colors.tertiaryContent,
                                size = 34.dp,
                            )
                            GlasmioText(
                                text = stringResource(R.string.receive_empty_title),
                                style = GlasmioTheme.textStyles.headline,
                                color = colors.content,
                                textAlign = TextAlign.Center,
                            )
                            GlasmioText(
                                text = stringResource(R.string.receive_empty_body),
                                style = GlasmioTheme.textStyles.subheadline,
                                color = colors.secondaryContent,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        } else {
            items(transfers, key = { it.id }) { item ->
                CenteredReceiveContent {
                    TransferActivityCard(
                        item = item,
                        onOpenFile = {
                            val path = item.localPath ?: return@TransferActivityCard
                            if (!FileActions.openFile(context, File(path), item.mimeType)) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.transfer_cannot_open_file),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onCancel = { onCancelTransfer(item.id) },
                        onRetry = { onRetryTransfer(item.id) },
                        onDismiss = { onDismissTransfer(item.id) },
                    )
                }
            }
        }
    }

    GlasmioAlertDialog(
        visible = showDisconnectDialog,
        onDismissRequest = { showDisconnectDialog = false },
        title = stringResource(R.string.transfer_disconnect),
        message = stringResource(R.string.transfer_send_subtitle, peerDisplay),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.cancel),
                onClick = { showDisconnectDialog = false },
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.confirm),
                onClick = {
                    showDisconnectDialog = false
                    onDisconnect()
                },
                role = GlasmioAlertButtonRole.Destructive,
            ),
        ),
    )
}

@Composable
private fun CenteredReceiveContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}
