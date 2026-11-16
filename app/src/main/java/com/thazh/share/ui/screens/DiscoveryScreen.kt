package com.thazh.share.ui.screens

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WifiOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.thazh.share.R
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.SavedDevice
import com.thazh.share.ui.ConnectScreenState
import com.thazh.share.ui.components.DiscoveredPeerRow
import com.thazh.share.ui.components.NfcTapCard
import com.thazh.share.ui.components.SavedDeviceRow
import com.thazh.share.ui.cupertino.CupertinoActivityIndicator
import com.thazh.share.ui.cupertino.CupertinoDivider
import com.thazh.share.ui.cupertino.CupertinoIcon
import com.thazh.share.ui.cupertino.CupertinoText
import com.thazh.share.ui.cupertino.CupertinoTextField
import com.thazh.share.ui.cupertino.CupertinoTopBar
import com.thazh.share.ui.cupertino.LiquidAlertDialog
import com.thazh.share.ui.cupertino.LiquidButton
import com.thazh.share.ui.cupertino.LiquidCard
import com.thazh.share.ui.cupertino.LiquidIconButton
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.ui.theme.LocalAppBackdrop

@Composable
fun DiscoveryScreen(
    displayName: String,
    peers: List<DiscoveredPeer>,
    savedDevices: List<SavedDevice>,
    isDiscovering: Boolean,
    screenState: ConnectScreenState,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    nfcPairingPeerName: String?,
    pendingWifiDirectFilesCount: Int = 0,
    onCancelPendingFiles: () -> Unit = {},
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectPeer: (DiscoveredPeer) -> Unit,
    onConnectSavedDevice: (SavedDevice) -> Unit,
    onRenameSavedDevice: (String, String) -> Unit,
    onRemoveSavedDevice: (String) -> Unit,
    onOpenActiveSession: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CupertinoTheme.colors
    val wifiManager = remember(context) {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }
    val isWifiEnabled = remember(wifiManager) {
        runCatching { wifiManager?.isWifiEnabled }.getOrNull() ?: true
    }
    val isConnecting = screenState is ConnectScreenState.Connecting

    var deviceToRename by remember { mutableStateOf<SavedDevice?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topPadding = if (topBarHeightPx > 0) {
        with(density) { topBarHeightPx.toDp() } + 16.dp
    } else {
        112.dp
    }

    val contentBackdrop = rememberLayerBackdrop()
    val barBackdrop = rememberCombinedBackdrop(LocalAppBackdrop.current, contentBackdrop)

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
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding,
                    bottom = 176.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                    if (screenState is ConnectScreenState.Connected) {
                        item(key = "active_session_banner") {
                            LiquidCard(
                                onClick = onOpenActiveSession,
                                surfaceColor = colors.accentGreen.copy(alpha = 0.16f),
                                borderColor = colors.accentGreen.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("active_session_banner")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CupertinoTheme.shapes.iconBadge)
                                            .background(colors.accentGreen.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CupertinoIcon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = null,
                                            tint = colors.accentGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        CupertinoText(
                                            text = stringResource(R.string.discover_active_session, screenState.peerName),
                                            style = CupertinoTheme.typography.headline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        CupertinoText(
                                            text = stringResource(R.string.discover_active_session_action),
                                            style = CupertinoTheme.typography.footnote,
                                            color = colors.secondaryLabel
                                        )
                                    }
                                    CupertinoIcon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = colors.secondaryLabel
                                    )
                                }
                            }
                        }
                    }

                    if (pendingWifiDirectFilesCount > 0) {
                        item(key = "pending_wifi_direct_files_banner") {
                            LiquidCard(
                                surfaceColor = colors.accentBlue.copy(alpha = 0.14f),
                                borderColor = colors.accentBlue.copy(alpha = 0.38f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pending_wifi_direct_files_banner")
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
                                            .background(colors.accentBlue.copy(alpha = 0.20f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CupertinoIcon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = null,
                                            tint = colors.accentBlue
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        CupertinoText(
                                            text = stringResource(
                                                R.string.discover_pending_files_title,
                                                pendingWifiDirectFilesCount
                                            ),
                                            style = CupertinoTheme.typography.headline
                                        )
                                        CupertinoText(
                                            text = stringResource(R.string.discover_pending_files_subtitle),
                                            style = CupertinoTheme.typography.footnote,
                                            color = colors.secondaryLabel
                                        )
                                    }
                                    LiquidIconButton(
                                        onClick = onCancelPendingFiles,
                                        icon = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.discover_pending_files_cancel),
                                        tint = colors.destructiveRed,
                                        modifier = Modifier.testTag("cancel_pending_wifi_direct_files_button")
                                    )
                                }
                            }
                        }
                    }

                    if (!isWifiEnabled) {
                        item(key = "wifi_off_warning") {
                            LiquidCard(
                                surfaceColor = colors.warningOrange.copy(alpha = 0.14f),
                                borderColor = colors.warningOrange.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CupertinoIcon(
                                        imageVector = Icons.Rounded.WifiOff,
                                        contentDescription = null,
                                        tint = colors.warningOrange
                                    )
                                    CupertinoText(
                                        text = stringResource(R.string.discover_wifi_off),
                                        style = CupertinoTheme.typography.subheadline,
                                        color = colors.label,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "radar_hero") {
                        DiscoveryRadarCard(
                            displayName = displayName,
                            isDiscovering = isDiscovering,
                            errorMessage = (screenState as? ConnectScreenState.Error)?.message
                        )
                    }

                    if (isNfcSupported) {
                        item(key = "nfc_tap_card") {
                            NfcTapCard(
                                isNfcEnabled = isNfcEnabled,
                                nfcPairingPeerName = nfcPairingPeerName,
                                onOpenNfcSettings = onOpenNfcSettings
                            )
                        }
                    }

                    if (savedDevices.isNotEmpty()) {
                        item(key = "saved_header") {
                            SectionHeader(title = stringResource(R.string.discover_saved_devices))
                        }
                        item(key = "saved_list") {
                            LiquidCard(
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                savedDevices.forEachIndexed { index, device ->
                                    SavedDeviceRow(
                                        device = device,
                                        isConnecting = isConnecting,
                                        onConnect = { onConnectSavedDevice(device) },
                                        onRename = {
                                            deviceToRename = device
                                            renameDraft = device.nickname.orEmpty().ifBlank { device.displayName }
                                        },
                                        onRemove = { onRemoveSavedDevice(device.deviceAddress) }
                                    )
                                    if (index < savedDevices.lastIndex) {
                                        CupertinoDivider(startIndent = 72.dp)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "nearby_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SectionHeader(title = stringResource(R.string.discover_available_devices))
                            if (isDiscovering) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CupertinoActivityIndicator(size = 15.dp, color = colors.accentBlue)
                                    CupertinoText(
                                        text = stringResource(R.string.discover_scanning),
                                        style = CupertinoTheme.typography.caption,
                                        color = colors.accentBlue
                                    )
                                }
                            }
                        }
                    }

                    item(key = "nearby_list") {
                        LiquidCard(
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (peers.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CupertinoIcon(
                                        imageVector = Icons.Rounded.Radar,
                                        contentDescription = null,
                                        tint = colors.tertiaryLabel,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    CupertinoText(
                                        text = stringResource(R.string.discover_no_peers),
                                        style = CupertinoTheme.typography.subheadline,
                                        color = colors.secondaryLabel,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                peers.forEachIndexed { index, peer ->
                                    DiscoveredPeerRow(
                                        peer = peer,
                                        isConnecting = isConnecting,
                                        onConnect = { onConnectPeer(peer) }
                                    )
                                    if (index < peers.lastIndex) {
                                        CupertinoDivider(startIndent = 72.dp)
                                    }
                                }
                            }
                        }
                    }
                }
        }

        // Floating LiquidGlass TopBar + Scan Pill refracting live scrollable content beneath them
        CompositionLocalProvider(LocalAppBackdrop provides barBackdrop) {
            CupertinoTopBar(
                title = stringResource(R.string.discover_title),
                subtitle = stringResource(R.string.discover_subtitle),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { topBarHeightPx = it.height },
                actions = {
                    LiquidIconButton(
                        onClick = onOpenSettings,
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        modifier = Modifier.testTag("open_settings_button")
                    )
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 92.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidButton(
                    onClick = { if (isDiscovering) onStopDiscovery() else onStartDiscovery() },
                    modifier = Modifier
                        .widthIn(min = 210.dp, max = 320.dp)
                        .fillMaxWidth(0.76f)
                        .testTag("toggle_scan_button"),
                    tint = if (isDiscovering) colors.destructiveRed else colors.accentBlue,
                    height = 54.dp
                ) {
                    if (isDiscovering) {
                        CupertinoIcon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CupertinoText(
                            text = stringResource(R.string.discover_stop_scan),
                            style = CupertinoTheme.typography.headline,
                            color = Color.White
                        )
                    } else {
                        CupertinoIcon(
                            imageVector = Icons.Rounded.WifiTethering,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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

    deviceToRename?.let { target ->
        LiquidAlertDialog(
            onDismissRequest = { deviceToRename = null },
            title = stringResource(R.string.saved_device_rename),
            confirmText = stringResource(R.string.save),
            onConfirm = {
                onRenameSavedDevice(target.deviceAddress, renameDraft.trim())
                deviceToRename = null
            },
            dismissText = stringResource(R.string.cancel)
        ) {
            CupertinoTextField(
                value = renameDraft,
                onValueChange = { renameDraft = it },
                placeholder = target.displayName,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onRenameSavedDevice(target.deviceAddress, renameDraft.trim())
                    deviceToRename = null
                }),
                modifier = Modifier.testTag("rename_saved_device_input")
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    CupertinoText(
        text = title.uppercase(),
        style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
        color = CupertinoTheme.colors.secondaryLabel,
        modifier = Modifier.padding(horizontal = 6.dp)
    )
}

@Composable
private fun DiscoveryRadarCard(
    displayName: String,
    isDiscovering: Boolean,
    errorMessage: String?
) {
    val colors = CupertinoTheme.colors
    val transition = rememberInfiniteTransition(label = "radar_wave")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    LiquidCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxR = size.minDimension / 2f
                    if (isDiscovering) {
                        for (ring in 0..2) {
                            val phase = (wave + ring / 3f) % 1f
                            val radius = maxR * (0.36f + 0.64f * phase)
                            val alpha = (1f - phase) * 0.42f
                            drawCircle(
                                color = colors.accentBlue.copy(alpha = alpha),
                                radius = radius,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    } else {
                        drawCircle(
                            color = colors.accentBlue.copy(alpha = 0.16f),
                            radius = maxR * 0.72f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = colors.accentBlue.copy(alpha = 0.16f),
                        radius = maxR * 0.38f
                    )
                }
                CupertinoIcon(
                    imageVector = Icons.Rounded.WifiTethering,
                    contentDescription = null,
                    tint = colors.accentBlue,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (displayName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(CupertinoTheme.shapes.pill)
                        .background(colors.accentBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    CupertinoText(
                        text = displayName,
                        style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            CupertinoText(
                text = errorMessage ?: stringResource(R.string.discover_intro),
                style = CupertinoTheme.typography.subheadline,
                color = if (errorMessage != null) colors.destructiveRed else colors.secondaryLabel,
                textAlign = TextAlign.Center
            )
        }
    }
}
