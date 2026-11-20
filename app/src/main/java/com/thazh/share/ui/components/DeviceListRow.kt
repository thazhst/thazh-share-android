package com.thazh.share.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.PeerDiscoveryTransport
import com.thazh.share.data.PeerStatus
import com.thazh.share.data.SavedDevice
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.ui.glasmioGroupedListSeparator
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Row displaying a newly discovered peer inside a [io.github.neoruaa.glasmio.ui.GlasmioGroupedList],
 * enhanced with subtle Compose entry & connection establishment animations:
 * - Spring slide-in + fade + scale on new device discovery
 * - Subtle discovery glow burst on first appearance
 * - Pulsing connection sonar halo & animated status transition when connecting or connected
 */
@Composable
fun DiscoveredPeerRow(
    peer: DiscoveredPeer,
    savedCustomName: String? = null,
    isConnecting: Boolean,
    showTopSeparator: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val effectiveName = savedCustomName ?: peer.deviceName
    val isPending = isConnecting || peer.status == PeerStatus.INVITED
    val isConnected = peer.status == PeerStatus.CONNECTED

    // Subtle Compose entry transition when a new peer is discovered
    val entryProgress = remember(peer.deviceAddress) { Animatable(0f) }
    val discoveryHighlightAlpha = remember(peer.deviceAddress) { Animatable(0.32f) }

    LaunchedEffect(peer.deviceAddress) {
        launch {
            entryProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            delay(180L)
            discoveryHighlightAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1_100, easing = FastOutSlowInEasing),
            )
        }
    }

    val statusAccentColor by animateColorAsState(
        targetValue = when {
            isConnected -> semantic.accentGreen
            isPending -> semantic.accentCyan
            else -> semantic.accentBlue
        },
        animationSpec = tween(durationMillis = 320),
        label = "peer_status_accent",
    )

    val rowHighlightColor by animateColorAsState(
        targetValue = when {
            isConnected -> semantic.accentGreen.copy(alpha = if (colors.isDark) 0.14f else 0.08f)
            isPending -> semantic.accentCyan.copy(alpha = if (colors.isDark) 0.14f else 0.08f)
            else -> semantic.accentBlue.copy(alpha = discoveryHighlightAlpha.value)
        },
        animationSpec = tween(durationMillis = 280),
        label = "peer_row_highlight",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = entryProgress.value.coerceIn(0f, 1f)
                alpha = p
                translationY = (1f - p) * 24f
                val s = 0.96f + 0.04f * p
                scaleX = s
                scaleY = s
            }
            .defaultMinSize(minHeight = 68.dp)
            .background(rowHighlightColor)
            .glasmioGroupedListSeparator(
                color = colors.separator,
                visible = showTopSeparator,
                horizontalInset = 16.dp,
            )
            .clickable(
                enabled = !isPending,
                role = Role.Button,
                onClick = onConnect,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("peer_card_${peer.deviceAddress}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedDeviceSquircleIcon(
            symbol = if (isConnected) ThazhSymbols.CheckCircle else ThazhSymbols.Devices,
            accentColor = statusAccentColor,
            isConnecting = isPending,
            isConnected = isConnected,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlasmioText(
                    text = effectiveName,
                    style = GlasmioTheme.textStyles.headline,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TransportBadge(transport = peer.discoveryTransport)
                val battery = peer.batteryPercent
                if (battery != null && battery in 0..100) {
                    PeerBatteryBadge(
                        percent = battery,
                        isCharging = peer.isCharging,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            GlasmioText(
                text = peerSubtitle(peer),
                style = GlasmioTheme.textStyles.footnote,
                color = if (isConnected) semantic.accentGreen else colors.secondaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        AnimatedContent(
            targetState = when {
                isPending -> PeerActionVisualState.CONNECTING
                isConnected -> PeerActionVisualState.CONNECTED
                else -> PeerActionVisualState.IDLE
            },
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.88f)) togetherWith
                    (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.88f))
            },
            label = "peer_action_state_transition",
        ) { actionState ->
            when (actionState) {
                PeerActionVisualState.CONNECTING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(ContinuousCapsule)
                            .background(semantic.accentCyan.copy(alpha = if (colors.isDark) 0.20f else 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        GlasmioActivityIndicator(
                            size = 15.dp,
                            strokeWidth = 2.dp,
                            color = semantic.accentCyan,
                        )
                        GlasmioText(
                            text = stringResource(R.string.discover_connecting),
                            style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                            color = semantic.accentCyan,
                        )
                    }
                }

                PeerActionVisualState.CONNECTED -> {
                    GlasmioButton(
                        onClick = onConnect,
                        emphasis = GlassEmphasis.Regular,
                        size = GlasmioButtonSize.Small,
                        modifier = Modifier.testTag("connect_peer_button_${peer.deviceAddress}"),
                    ) {
                        SfIcon(
                            systemName = ThazhSymbols.CheckCircle,
                            contentDescription = null,
                            tint = semantic.accentGreen,
                            size = 15.dp,
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        GlasmioText(
                            text = stringResource(R.string.discover_connect),
                            style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                            color = semantic.accentGreen,
                        )
                    }
                }

                PeerActionVisualState.IDLE -> {
                    GlasmioButton(
                        onClick = onConnect,
                        emphasis = GlassEmphasis.Prominent,
                        navigationSurfaceColor = semantic.accentBlue,
                        size = GlasmioButtonSize.Small,
                        modifier = Modifier.testTag("connect_peer_button_${peer.deviceAddress}"),
                    ) {
                        GlasmioText(
                            text = stringResource(R.string.discover_connect),
                            style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

private enum class PeerActionVisualState {
    IDLE,
    CONNECTING,
    CONNECTED,
}

@Composable
private fun AnimatedDeviceSquircleIcon(
    symbol: String,
    accentColor: Color,
    isConnecting: Boolean,
    isConnected: Boolean,
) {
    val boxSize = 44.dp
    Box(
        modifier = Modifier.size(boxSize),
        contentAlignment = Alignment.Center,
    ) {
        if (isConnecting || isConnected) {
            val infiniteTransition = rememberInfiniteTransition(label = "peer_connection_halo")
            val pulseProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = if (isConnecting) 1_150 else 2_100,
                        easing = FastOutSlowInEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "halo_pulse_progress",
            )
            Canvas(modifier = Modifier.size(boxSize)) {
                val baseRadius = size.minDimension * 0.44f
                val maxRadius = size.minDimension * 0.56f
                val currentRadius = baseRadius + (maxRadius - baseRadius) * pulseProgress
                val ringAlpha = (1f - pulseProgress) * (if (isConnecting) 0.55f else 0.35f)
                drawCircle(
                    color = accentColor.copy(alpha = ringAlpha),
                    radius = currentRadius,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(ContinuousRoundedRectangle(12.dp))
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            SfIcon(
                systemName = symbol,
                contentDescription = null,
                tint = accentColor,
                size = 20.dp,
            )
        }
    }
}

@Composable
private fun TransportBadge(transport: PeerDiscoveryTransport) {
    val semantic = ThazhTheme.semantic
    val (label, color) = when (transport) {
        PeerDiscoveryTransport.WIFI_DIRECT -> "Wi-Fi" to semantic.accentBlue
        PeerDiscoveryTransport.BLE -> "BLE" to semantic.accentCyan
        PeerDiscoveryTransport.WIFI_AND_BLE -> "Wi-Fi + BLE" to semantic.accentGreen
        PeerDiscoveryTransport.NFC -> "NFC" to semantic.accentBlue
    }
    Box(
        modifier = Modifier
            .clip(ContinuousCapsule)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        GlasmioText(
            text = label,
            style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
private fun PeerBatteryBadge(
    percent: Int,
    isCharging: Boolean,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val color = when {
        isCharging -> semantic.accentGreen
        percent <= 20 -> colors.destructive
        percent <= 45 -> semantic.warningOrange
        else -> semantic.accentGreen
    }
    Box(
        modifier = Modifier
            .clip(ContinuousCapsule)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        GlasmioText(
            text = if (isCharging) {
                stringResource(R.string.radar_battery_charging, percent)
            } else {
                stringResource(R.string.radar_battery_percent, percent)
            },
            style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
}

@Composable
private fun peerSubtitle(peer: DiscoveredPeer): String {
    val statusPart = when (peer.status) {
        PeerStatus.CONNECTED -> stringResource(R.string.notif_connected, peer.deviceName)
        PeerStatus.INVITED -> stringResource(R.string.discover_connecting)
        else -> peer.deviceAddress
    }
    val signalLabel = when {
        peer.rssiDbm == null -> null
        peer.rssiDbm >= -58 -> stringResource(R.string.quality_strong)
        peer.rssiDbm >= -72 -> stringResource(R.string.quality_medium)
        else -> stringResource(R.string.quality_weak)
    }
    return if (signalLabel != null) "$statusPart • $signalLabel" else statusPart
}

/**
 * Row displaying a previously connected saved device inside a [io.github.neoruaa.glasmio.ui.GlasmioGroupedList].
 */
@Composable
fun SavedDeviceRow(
    device: SavedDevice,
    isNearby: Boolean = true,
    isConnecting: Boolean,
    showTopSeparator: Boolean,
    onConnect: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    var menuExpanded by remember { mutableStateOf(false) }

    val entryProgress = remember(device.deviceAddress) { Animatable(0f) }
    LaunchedEffect(device.deviceAddress) {
        entryProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    val accentColor by animateColorAsState(
        targetValue = when {
            isConnecting -> semantic.accentCyan
            isNearby -> semantic.accentGreen
            else -> colors.secondaryContent
        },
        animationSpec = tween(durationMillis = 280),
        label = "saved_device_accent",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = entryProgress.value.coerceIn(0f, 1f)
                alpha = p
                translationY = (1f - p) * 18f
            }
            .defaultMinSize(minHeight = 66.dp)
            .glasmioGroupedListSeparator(
                color = colors.separator,
                visible = showTopSeparator,
                horizontalInset = 16.dp,
            )
            .clickable(
                enabled = isNearby && !isConnecting,
                role = Role.Button,
                onClick = onConnect,
            )
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            .testTag("saved_device_card_${device.deviceAddress}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedDeviceSquircleIcon(
            symbol = ThazhSymbols.Devices,
            accentColor = accentColor,
            isConnecting = isConnecting,
            isConnected = isNearby && !isConnecting,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlasmioText(
                    text = device.effectiveName,
                    style = GlasmioTheme.textStyles.headline,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isNearby) {
                    Box(
                        modifier = Modifier
                            .clip(ContinuousCapsule)
                            .background(semantic.accentGreen.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        GlasmioText(
                            text = stringResource(R.string.discover_available_devices),
                            style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
                            color = semantic.accentGreen,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            GlasmioText(
                text = "${device.deviceAddress} • ${FormatUtils.formatRelativeTime(device.lastConnectedAtEpochMs)}",
                style = GlasmioTheme.textStyles.footnote,
                color = colors.secondaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isConnecting) {
            GlasmioActivityIndicator(
                size = 20.dp,
                strokeWidth = 2.2.dp,
                color = semantic.accentCyan,
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (isNearby) {
            GlasmioButton(
                onClick = onConnect,
                emphasis = GlassEmphasis.Regular,
                size = GlasmioButtonSize.Small,
                modifier = Modifier.testTag("connect_saved_button_${device.deviceAddress}"),
            ) {
                GlasmioText(
                    text = stringResource(R.string.discover_connect),
                    style = GlasmioTheme.textStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        GlasmioMenu(
            expanded = menuExpanded,
            onExpandedChange = { menuExpanded = it },
            itemCount = 2,
            anchor = { onAnchorClick ->
                GlasmioIconButton(
                    onClick = onAnchorClick,
                    size = 44.dp,
                    modifier = Modifier.testTag("saved_device_menu_${device.deviceAddress}"),
                ) {
                    SfIcon(
                        systemName = ThazhSymbols.MoreHoriz,
                        contentDescription = null,
                        tint = colors.secondaryContent,
                        size = 18.dp,
                    )
                }
            },
        ) { _, close ->
            GlasmioMenuItem(
                title = stringResource(R.string.saved_device_rename),
                systemName = ThazhSymbols.Edit,
                onClick = {
                    close()
                    onRename()
                },
            )
            GlasmioMenuItem(
                title = stringResource(R.string.saved_device_remove),
                systemName = ThazhSymbols.Delete,
                destructive = true,
                onClick = {
                    close()
                    onRemove()
                },
            )
        }
    }
}
