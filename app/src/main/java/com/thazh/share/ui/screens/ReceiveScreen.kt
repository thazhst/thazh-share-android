package com.thazh.share.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.thazh.share.R
import com.thazh.share.data.ConnectionQuality
import com.thazh.share.data.TransferItem
import com.thazh.share.ui.ConnectScreenState
import com.thazh.share.ui.components.ConnectionQualityIndicator
import com.thazh.share.ui.components.NfcTapCard
import com.thazh.share.ui.cupertino.CupertinoActivityIndicator
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.CupertinoTopBar
import com.thazh.share.ui.cupertino.LiquidAlertDialog
import com.thazh.share.ui.cupertino.LiquidButton
import com.thazh.share.ui.cupertino.LiquidCard
import com.thazh.share.ui.cupertino.LiquidIconButton
import com.thazh.share.ui.cupertino.elasticHold
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.ui.theme.LocalAppBackdrop
import com.thazh.share.util.FileActions
import com.thazh.share.util.FormatUtils
import java.io.File

@Composable
fun ReceiveScreen(
    displayName: String,
    isDiscovering: Boolean,
    screenState: ConnectScreenState,
    connectedPeerName: String?,
    isSocketConnected: Boolean,
    connectionQuality: ConnectionQuality,
    estimatedDistanceMeters: Int?,
    transfers: List<TransferItem>,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    nfcPairingPeerName: String?,
    onStartDiscovery: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onCancelTransfer: (String) -> Unit,
    onRetryTransfer: (String) -> Unit,
    onDismissTransfer: (String) -> Unit,
    onClearFinished: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CupertinoTheme.colors
    var showDisconnectDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topPadding = if (topBarHeightPx > 0) {
        with(density) { topBarHeightPx.toDp() } + 16.dp
    } else {
        112.dp
    }

    val contentBackdrop = rememberLayerBackdrop()
    val barBackdrop = rememberCombinedBackdrop(LocalAppBackdrop.current, contentBackdrop)

    val isConnected = screenState is ConnectScreenState.Connected
    val peerDisplay = connectedPeerName
        ?: (screenState as? ConnectScreenState.Connected)?.peerName
        ?: stringResource(R.string.transfer_unknown_peer)

    val topSubtitle = if (isConnected) {
        buildList {
            add(
                if (isSocketConnected) stringResource(R.string.transfer_send_subtitle, peerDisplay)
                else stringResource(R.string.transfer_connecting)
            )
            if (estimatedDistanceMeters != null) {
                add(FormatUtils.formatDistance(estimatedDistanceMeters))
            }
        }.joinToString(" • ")
    } else {
        stringResource(R.string.receive_subtitle_ready)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "receive_hero") {
                    ReceiveHeroCard(
                        displayName = displayName,
                        isDiscovering = isDiscovering,
                        isConnected = isConnected,
                        isConnecting = screenState is ConnectScreenState.Connecting || !nfcPairingPeerName.isNullOrBlank(),
                        peerDisplay = peerDisplay,
                        onStartDiscovery = onStartDiscovery
                    )
                }

                if (isNfcSupported && !isConnected) {
                    item(key = "receive_nfc_card") {
                        NfcTapCard(
                            isNfcEnabled = isNfcEnabled,
                            nfcPairingPeerName = nfcPairingPeerName,
                            onOpenNfcSettings = onOpenNfcSettings
                        )
                    }
                }

                item(key = "receive_activity_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CupertinoText(
                            text = stringResource(R.string.transfer_activity_title).uppercase(),
                            style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.secondaryLabel
                        )
                        if (transfers.any { it.isFinished }) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .elasticHold(pressedScale = 0.92f)
                                    .clip(Capsule())
                                    .clickable(role = Role.Button, onClick = onClearFinished)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("receive_clear_finished_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                CupertinoText(
                                    text = stringResource(R.string.transfer_clear_finished),
                                    style = CupertinoTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.accentBlue
                                )
                            }
                        }
                    }
                }

                if (transfers.isEmpty()) {
                    item(key = "receive_empty") {
                        LiquidCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CupertinoIcon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = colors.tertiaryLabel,
                                    modifier = Modifier.size(34.dp)
                                )
                                CupertinoText(
                                    text = stringResource(R.string.receive_empty_title),
                                    style = CupertinoTheme.typography.headline,
                                    textAlign = TextAlign.Center
                                )
                                CupertinoText(
                                    text = stringResource(R.string.receive_empty_body),
                                    style = CupertinoTheme.typography.subheadline,
                                    color = colors.secondaryLabel,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(transfers, key = { it.id }) { item ->
                        TransferActivityCard(
                            item = item,
                            onOpenFile = {
                                val path = item.localPath ?: return@TransferActivityCard
                                if (!FileActions.openFile(context, File(path), item.mimeType)) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.transfer_cannot_open_file),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onCancel = { onCancelTransfer(item.id) },
                            onRetry = { onRetryTransfer(item.id) },
                            onDismiss = { onDismissTransfer(item.id) }
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(LocalAppBackdrop provides barBackdrop) {
            CupertinoTopBar(
                title = stringResource(R.string.receive_title),
                subtitle = topSubtitle,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { topBarHeightPx = it.height },
                actions = {
                    if (isConnected) {
                        ConnectionQualityIndicator(
                            quality = connectionQuality,
                            showLabel = false
                        )
                        LiquidIconButton(
                            onClick = { showDisconnectDialog = true },
                            icon = Icons.Rounded.LinkOff,
                            contentDescription = stringResource(R.string.transfer_disconnect),
                            tint = colors.destructiveRed,
                            modifier = Modifier.testTag("receive_disconnect_button")
                        )
                    }
                    LiquidIconButton(
                        onClick = onOpenSettings,
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        modifier = Modifier.testTag("receive_open_settings_button")
                    )
                }
            )
        }
    }

    if (showDisconnectDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = stringResource(R.string.transfer_disconnect),
            confirmText = stringResource(R.string.confirm),
            confirmDestructive = true,
            onConfirm = {
                showDisconnectDialog = false
                onDisconnect()
            },
            dismissText = stringResource(R.string.cancel)
        ) {
            CupertinoText(
                text = stringResource(R.string.transfer_send_subtitle, peerDisplay),
                style = CupertinoTheme.typography.subheadline,
                color = colors.secondaryLabel
            )
        }
    }
}

@Composable
private fun ReceiveHeroCard(
    displayName: String,
    isDiscovering: Boolean,
    isConnected: Boolean,
    isConnecting: Boolean,
    peerDisplay: String,
    onStartDiscovery: () -> Unit
) {
    val colors = CupertinoTheme.colors
    val ringColor = if (isConnected) colors.accentGreen else colors.accentBlue
    val transition = rememberInfiniteTransition(label = "receive_wave")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "receive_wave_phase"
    )

    LiquidCard(
        surfaceColor = if (isConnected) colors.accentGreen.copy(alpha = 0.14f) else colors.glassSurface,
        borderColor = if (isConnected) colors.accentGreen.copy(alpha = 0.35f) else Color.Unspecified,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxR = size.minDimension / 2f
                    if (isDiscovering || isConnected) {
                        for (ring in 0..2) {
                            val phase = (wave + ring / 3f) % 1f
                            val radius = maxR * (0.36f + 0.64f * phase)
                            val alpha = (1f - phase) * 0.40f
                            drawCircle(
                                color = ringColor.copy(alpha = alpha),
                                radius = radius,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    } else {
                        drawCircle(
                            color = ringColor.copy(alpha = 0.18f),
                            radius = maxR * 0.72f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = ringColor.copy(alpha = 0.16f),
                        radius = maxR * 0.38f
                    )
                }

                if (isConnecting) {
                    CupertinoActivityIndicator(size = 24.dp, color = colors.accentBlue)
                } else {
                    CupertinoIcon(
                        imageVector = if (isConnected) Icons.Rounded.Download else Icons.Rounded.WifiTethering,
                        contentDescription = null,
                        tint = ringColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isConnected) {
                CupertinoText(
                    text = stringResource(R.string.receive_connected_title, peerDisplay),
                    style = CupertinoTheme.typography.title2,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                CupertinoText(
                    text = stringResource(R.string.receive_connected_body),
                    style = CupertinoTheme.typography.subheadline,
                    color = colors.secondaryLabel,
                    textAlign = TextAlign.Center
                )
            } else {
                val safeName = displayName.ifBlank { stringResource(R.string.app_name) }
                Box(
                    modifier = Modifier
                        .clip(CupertinoTheme.shapes.pill)
                        .background(colors.accentBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    CupertinoText(
                        text = stringResource(R.string.receive_ready_title, safeName),
                        style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                CupertinoText(
                    text = stringResource(R.string.receive_ready_body),
                    style = CupertinoTheme.typography.subheadline,
                    color = colors.secondaryLabel,
                    textAlign = TextAlign.Center
                )
                if (!isDiscovering) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LiquidButton(
                        onClick = onStartDiscovery,
                        modifier = Modifier.testTag("receive_enable_visibility_button")
                    ) {
                        CupertinoIcon(
                            imageVector = Icons.Rounded.WifiTethering,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CupertinoText(
                            text = stringResource(R.string.discover_start_scan),
                            style = CupertinoTheme.typography.headline,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
