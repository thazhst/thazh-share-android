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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.SavedDevice
import com.thazh.share.ui.ConnectScreenState
import com.thazh.share.ui.components.DiscoveredPeerRow
import com.thazh.share.ui.components.NfcTapCard
import com.thazh.share.ui.components.SavedDeviceRow
import com.thazh.share.ui.components.VisibilitySelectorCard
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioAlertFieldGroup
import io.github.neoruaa.glasmio.ui.GlasmioAlertTextField
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle

@Composable
fun DiscoveryScreen(
    displayName: String,
    visibilityMode: DeviceVisibilityMode,
    visibilityRemainingSeconds: Int?,
    peers: List<DiscoveredPeer>,
    savedDevices: List<SavedDevice>,
    isDiscovering: Boolean,
    screenState: ConnectScreenState,
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    nfcPairingPeerName: String?,
    pendingWifiDirectFilesCount: Int = 0,
    bottomBarPadding: Dp = 96.dp,
    onCancelPendingFiles: () -> Unit = {},
    onSelectVisibilityMode: (DeviceVisibilityMode) -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectPeer: (DiscoveredPeer) -> Unit,
    onConnectSavedDevice: (SavedDevice) -> Unit,
    onRenameSavedDevice: (String, String) -> Unit,
    onRemoveSavedDevice: (String) -> Unit,
    onOpenActiveSession: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = GlasmioTheme.colors
    val appearance = GlasmioTheme.appearance
    val semantic = ThazhTheme.semantic
    val wifiManager = remember(context) {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }
    val isWifiEnabled = remember(wifiManager) {
        runCatching { wifiManager?.isWifiEnabled }.getOrNull() ?: true
    }
    val isConnecting = screenState is ConnectScreenState.Connecting

    var deviceToRename by remember { mutableStateOf<SavedDevice?>(null) }
    var lastRenameTarget by remember { mutableStateOf<SavedDevice?>(null) }
    if (deviceToRename != null) {
        lastRenameTarget = deviceToRename
    }
    var renameDraft by remember { mutableStateOf(TextFieldValue("")) }

    val cardShape = ContinuousRoundedRectangle(GlasmioTheme.dimensions.regularCornerRadius)
    val badgeShape = ContinuousRoundedRectangle(12.dp)

    Box(modifier = modifier.fillMaxSize()) {
        GlasmioPinnedListPage(
            title = stringResource(R.string.discover_title),
            subtitle = stringResource(R.string.discover_subtitle),
            bottomPadding = bottomBarPadding + 72.dp,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            backgroundColor = colors.groupedBackground.copy(
                alpha = if (appearance.reduceTransparency) 1f else 0.78f,
            ),
            modifier = Modifier.fillMaxSize(),
            actions = {
                GlasmioIconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("open_settings_button"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                    )
                }
            },
        ) {
            if (screenState is ConnectScreenState.Connected) {
                item(key = "active_session_banner") {
                    CenteredDiscoveryContent {
                        GlasmioCard(
                            onClick = onOpenActiveSession,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("active_session_banner"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        semantic.accentGreen.copy(alpha = if (colors.isDark) 0.20f else 0.14f),
                                        cardShape,
                                    )
                                    .border(1.dp, semantic.accentGreen.copy(alpha = 0.35f), cardShape)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(badgeShape)
                                        .background(semantic.accentGreen.copy(alpha = if (colors.isDark) 0.26f else 0.22f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.Share,
                                        contentDescription = null,
                                        tint = semantic.accentGreen,
                                        size = 22.dp,
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.discover_active_session, screenState.peerName),
                                        style = GlasmioTheme.textStyles.headline,
                                        color = colors.content,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = stringResource(R.string.discover_active_session_action),
                                        style = GlasmioTheme.textStyles.footnote,
                                        color = colors.secondaryContent,
                                    )
                                }
                                SfIcon(
                                    systemName = ThazhSymbols.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.secondaryContent,
                                    size = 18.dp,
                                )
                            }
                        }
                    }
                }
            }

            if (pendingWifiDirectFilesCount > 0) {
                item(key = "pending_wifi_direct_files_banner") {
                    CenteredDiscoveryContent {
                        GlasmioCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pending_wifi_direct_files_banner"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        semantic.accentBlue.copy(alpha = if (colors.isDark) 0.20f else 0.14f),
                                        cardShape,
                                    )
                                    .border(1.dp, semantic.accentBlue.copy(alpha = 0.35f), cardShape)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(badgeShape)
                                        .background(semantic.accentBlue.copy(alpha = if (colors.isDark) 0.26f else 0.20f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.Share,
                                        contentDescription = null,
                                        tint = semantic.accentBlue,
                                        size = 22.dp,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(
                                            R.string.discover_pending_files_title,
                                            pendingWifiDirectFilesCount,
                                        ),
                                        style = GlasmioTheme.textStyles.headline,
                                        color = colors.content,
                                    )
                                    Text(
                                        text = stringResource(R.string.discover_pending_files_subtitle),
                                        style = GlasmioTheme.textStyles.footnote,
                                        color = colors.secondaryContent,
                                    )
                                }
                                GlasmioIconButton(
                                    onClick = onCancelPendingFiles,
                                    size = 48.dp,
                                    modifier = Modifier.testTag("cancel_pending_wifi_direct_files_button"),
                                ) {
                                    SfIcon(
                                        systemName = ThazhSymbols.Close,
                                        contentDescription = stringResource(R.string.discover_pending_files_cancel),
                                        tint = colors.destructive,
                                        size = 20.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isWifiEnabled) {
                item(key = "wifi_off_warning") {
                    CenteredDiscoveryContent {
                        GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        semantic.warningOrange.copy(alpha = if (colors.isDark) 0.20f else 0.14f),
                                        cardShape,
                                    )
                                    .border(1.dp, semantic.warningOrange.copy(alpha = 0.35f), cardShape)
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.WifiOff,
                                    contentDescription = null,
                                    tint = semantic.warningOrange,
                                    size = 22.dp,
                                )
                                Text(
                                    text = stringResource(R.string.discover_wifi_off),
                                    style = GlasmioTheme.textStyles.subheadline,
                                    color = colors.content,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "radar_hero") {
                CenteredDiscoveryContent {
                    DiscoveryRadarCard(
                        displayName = displayName,
                        isDiscovering = isDiscovering,
                        errorMessage = (screenState as? ConnectScreenState.Error)?.message,
                    )
                }
            }

            item(key = "visibility_selector") {
                CenteredDiscoveryContent {
                    VisibilitySelectorCard(
                        mode = visibilityMode,
                        remainingSeconds = visibilityRemainingSeconds,
                        onSelectMode = onSelectVisibilityMode,
                    )
                }
            }

            if (isNfcSupported) {
                item(key = "nfc_tap_card") {
                    CenteredDiscoveryContent {
                        NfcTapCard(
                            isNfcEnabled = isNfcEnabled,
                            nfcPairingPeerName = nfcPairingPeerName,
                            onOpenNfcSettings = onOpenNfcSettings,
                        )
                    }
                }
            }

            if (savedDevices.isNotEmpty()) {
                item(key = "saved_header") {
                    CenteredDiscoveryContent {
                        GlasmioSectionTitle(title = stringResource(R.string.discover_saved_devices))
                    }
                }
                item(key = "saved_list") {
                    CenteredDiscoveryContent {
                        GlasmioGroupedList(modifier = Modifier.fillMaxWidth()) {
                            savedDevices.forEachIndexed { index, device ->
                                SavedDeviceRow(
                                    device = device,
                                    isConnecting = isConnecting,
                                    onConnect = { onConnectSavedDevice(device) },
                                    onRename = {
                                        val initial = device.nickname.orEmpty().ifBlank { device.displayName }
                                        renameDraft = TextFieldValue(initial, TextRange(initial.length))
                                        deviceToRename = device
                                    },
                                    onRemove = { onRemoveSavedDevice(device.deviceAddress) },
                                    showTopSeparator = index > 0,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "nearby_header") {
                CenteredDiscoveryContent {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        GlasmioSectionTitle(
                            title = stringResource(R.string.discover_available_devices),
                            modifier = Modifier.weight(1f),
                        )
                        if (isDiscovering) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(end = 6.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    color = semantic.accentBlue,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = stringResource(R.string.discover_scanning),
                                    style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.Medium),
                                    color = semantic.accentBlue,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "nearby_list") {
                CenteredDiscoveryContent {
                    if (peers.isEmpty()) {
                        GlasmioCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SfIcon(
                                    systemName = ThazhSymbols.Radar,
                                    contentDescription = null,
                                    tint = colors.tertiaryContent,
                                    size = 34.dp,
                                )
                                Text(
                                    text = stringResource(
                                        if (visibilityMode == DeviceVisibilityMode.HIDDEN) {
                                            R.string.discover_no_peers_hidden
                                        } else {
                                            R.string.discover_no_peers
                                        },
                                    ),
                                    style = GlasmioTheme.textStyles.subheadline,
                                    color = colors.secondaryContent,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        GlasmioGroupedList(modifier = Modifier.fillMaxWidth()) {
                            peers.forEachIndexed { index, peer ->
                                DiscoveredPeerRow(
                                    peer = peer,
                                    isConnecting = isConnecting,
                                    onConnect = { onConnectPeer(peer) },
                                    showTopSeparator = index > 0,
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = bottomBarPadding),
            contentAlignment = Alignment.Center,
        ) {
            GlasmioButton(
                onClick = { if (isDiscovering) onStopDiscovery() else onStartDiscovery() },
                emphasis = GlassEmphasis.Prominent,
                destructive = isDiscovering,
                navigationSurfaceColor = if (isDiscovering) colors.destructive else semantic.accentBlue,
                size = GlasmioButtonSize.Large,
                modifier = Modifier
                    .widthIn(min = 210.dp, max = 320.dp)
                    .fillMaxWidth(0.76f)
                    .testTag("toggle_scan_button"),
            ) {
                SfIcon(
                    systemName = if (isDiscovering) ThazhSymbols.Close else ThazhSymbols.WifiTethering,
                    contentDescription = null,
                    tint = Color.White,
                    size = 20.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (isDiscovering) R.string.discover_stop_scan else R.string.discover_start_scan,
                    ),
                    style = GlasmioTheme.textStyles.headline,
                    color = Color.White,
                )
            }
        }
    }

    val renameTarget = lastRenameTarget
    GlasmioAlertDialog(
        visible = deviceToRename != null && renameTarget != null,
        onDismissRequest = { deviceToRename = null },
        title = stringResource(R.string.saved_device_rename),
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.cancel),
                onClick = { deviceToRename = null },
                role = GlasmioAlertButtonRole.Cancel,
            ),
            GlasmioAlertButtonSpec(
                label = stringResource(R.string.save),
                onClick = {
                    renameTarget?.let { onRenameSavedDevice(it.deviceAddress, renameDraft.text.trim()) }
                    deviceToRename = null
                },
                role = GlasmioAlertButtonRole.Default,
            ),
        ),
    ) {
        GlasmioAlertFieldGroup {
            GlasmioAlertTextField(
                value = renameDraft,
                onValueChange = { renameDraft = it },
                placeholder = renameTarget?.displayName.orEmpty(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        renameTarget?.let { onRenameSavedDevice(it.deviceAddress, renameDraft.text.trim()) }
                        deviceToRename = null
                    },
                ),
                modifier = Modifier.testTag("rename_saved_device_input"),
            )
        }
    }
}

@Composable
private fun CenteredDiscoveryContent(content: @Composable () -> Unit) {
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

@Composable
private fun DiscoveryRadarCard(
    displayName: String,
    isDiscovering: Boolean,
    errorMessage: String?,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val transition = rememberInfiniteTransition(label = "radar_wave")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )

    GlasmioCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxR = size.minDimension / 2f
                    if (isDiscovering) {
                        for (ring in 0..2) {
                            val phase = (wave + ring / 3f) % 1f
                            val radius = maxR * (0.36f + 0.64f * phase)
                            val alpha = (1f - phase) * 0.42f
                            drawCircle(
                                color = semantic.accentBlue.copy(alpha = alpha),
                                radius = radius,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    } else {
                        drawCircle(
                            color = semantic.accentBlue.copy(alpha = 0.16f),
                            radius = maxR * 0.72f,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                    drawCircle(
                        color = semantic.accentBlue.copy(alpha = 0.16f),
                        radius = maxR * 0.38f,
                    )
                }
                SfIcon(
                    systemName = ThazhSymbols.WifiTethering,
                    contentDescription = null,
                    tint = semantic.accentBlue,
                    size = 30.dp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (displayName.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(semantic.accentBlue.copy(alpha = if (colors.isDark) 0.22f else 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = displayName,
                        style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = semantic.accentBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = errorMessage ?: stringResource(R.string.discover_intro),
                style = GlasmioTheme.textStyles.subheadline,
                color = if (errorMessage != null) colors.destructive else colors.secondaryContent,
                textAlign = TextAlign.Center,
            )
        }
    }
}
