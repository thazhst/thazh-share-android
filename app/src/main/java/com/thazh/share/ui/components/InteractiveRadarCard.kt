package com.thazh.share.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.thazh.share.R
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.PeerDiscoveryTransport
import com.thazh.share.data.PeerStatus
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import com.thazh.share.util.FormatUtils
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.icons.SfIcon
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class RadarMode {
    SEND,
    RECEIVE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveRadarCard(
    mode: RadarMode,
    displayName: String,
    visibilityMode: DeviceVisibilityMode,
    visibilityRemainingSeconds: Int?,
    isDiscovering: Boolean,
    isConnecting: Boolean = false,
    isConnected: Boolean = false,
    connectedPeerName: String? = null,
    connectedDistanceMeters: Int? = null,
    peers: List<DiscoveredPeer> = emptyList(),
    isBleSupported: Boolean = true,
    isBleEnabled: Boolean = true,
    isNfcSupported: Boolean = false,
    isNfcEnabled: Boolean = false,
    nfcPairingPeerName: String? = null,
    errorMessage: String? = null,
    onCenterClick: () -> Unit = {},
    onPeerNodeClick: (DiscoveredPeer) -> Unit = {},
    onToggleBleClick: (() -> Unit)? = null,
    onNfcPillClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val density = LocalDensity.current

    val isHidden = !isConnected && visibilityMode == DeviceVisibilityMode.HIDDEN
    val primaryAccent = when {
        isConnected -> semantic.accentGreen
        mode == RadarMode.RECEIVE && visibilityMode == DeviceVisibilityMode.ALWAYS_VISIBLE -> semantic.accentGreen
        isHidden -> colors.secondaryContent
        else -> semantic.accentBlue
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spatial_radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar_sweep_angle",
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar_pulse_phase",
    )

    val activeRadar = isDiscovering || isConnected || (!isHidden && mode == RadarMode.RECEIVE)

    // Borderless open spatial Radar section on both Send and Receive screens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top header bar: Local device badge + live visibility badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(ContinuousCapsule)
                    .background(primaryAccent.copy(alpha = if (colors.isDark) 0.22f else 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryAccent),
                )
                Text(
                    text = displayName.ifBlank { stringResource(R.string.app_name) },
                    style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                    color = primaryAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val visibilityBadgeText = when {
                isConnected -> stringResource(R.string.quality_strong)
                visibilityMode == DeviceVisibilityMode.HIDDEN -> stringResource(R.string.visibility_mode_hidden)
                visibilityMode == DeviceVisibilityMode.TEMPORARY_10_MIN -> stringResource(
                    R.string.visibility_timer_badge,
                    WifiDirectService.formatCountdown(visibilityRemainingSeconds ?: 600),
                )
                else -> stringResource(R.string.visibility_active_always_badge)
            }
            Box(
                modifier = Modifier
                    .clip(ContinuousCapsule)
                    .background(colors.secondaryContent.copy(alpha = if (colors.isDark) 0.18f else 0.10f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = visibilityBadgeText,
                    style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.Medium),
                    color = colors.secondaryContent,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2D Spatial Radar Canvas + Interactive Peer Nodes (No outer frame/border)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(244.dp),
            contentAlignment = Alignment.Center,
        ) {
            val radarSizeDp = minOf(maxWidth, maxHeight)
            val maxRadiusPx = with(density) { (radarSizeDp / 2f).toPx() }

            Canvas(
                modifier = Modifier.size(radarSizeDp),
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f * 0.92f
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // Soft ambient radial glow behind the radar rings (no hard border)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = if (colors.isDark) 0.16f else 0.10f),
                            primaryAccent.copy(alpha = if (colors.isDark) 0.05f else 0.03f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = maxR,
                    ),
                    radius = maxR,
                    center = centerOffset,
                )

                // Subtle crosshairs
                drawLine(
                    color = primaryAccent.copy(alpha = 0.12f),
                    start = Offset(centerOffset.x - maxR, centerOffset.y),
                    end = Offset(centerOffset.x + maxR, centerOffset.y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect,
                )
                drawLine(
                    color = primaryAccent.copy(alpha = 0.12f),
                    start = Offset(centerOffset.x, centerOffset.y - maxR),
                    end = Offset(centerOffset.x, centerOffset.y + maxR),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect,
                )

                // 3 Concentric Distance Rings (1m = 0.36, 3m = 0.66, 10m = 0.95)
                val ringFractions = listOf(0.36f, 0.66f, 0.95f)
                ringFractions.forEachIndexed { idx, frac ->
                    drawCircle(
                        color = primaryAccent.copy(alpha = if (idx == 2) 0.18f else 0.14f),
                        radius = maxR * frac,
                        center = centerOffset,
                        style = Stroke(width = 1.1.dp.toPx()),
                    )
                }

                // Expanding pulse wave when active
                if (activeRadar) {
                    for (ring in 0..1) {
                        val phase = (pulsePhase + ring * 0.5f) % 1f
                        val radius = maxR * (0.22f + 0.73f * phase)
                        val alpha = (1f - phase) * 0.34f
                        drawCircle(
                            color = primaryAccent.copy(alpha = alpha),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }

                    // Rotating Radar Sweep Sector
                    rotate(degrees = sweepAngle, pivot = centerOffset) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to Color.Transparent,
                                0.78f to Color.Transparent,
                                0.92f to primaryAccent.copy(alpha = 0.12f),
                                1.0f to primaryAccent.copy(alpha = 0.34f),
                                center = centerOffset,
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = true,
                            topLeft = Offset(centerOffset.x - maxR * 0.95f, centerOffset.y - maxR * 0.95f),
                            size = Size(maxR * 1.90f, maxR * 1.90f),
                        )
                        drawLine(
                            color = primaryAccent.copy(alpha = 0.62f),
                            start = centerOffset,
                            end = Offset(centerOffset.x + maxR * 0.95f, centerOffset.y),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }

                // Center glow aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = 0.32f),
                            primaryAccent.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = maxR * 0.38f,
                    ),
                    radius = maxR * 0.38f,
                    center = centerOffset,
                )
            }

            // Distance ring labels (1m, 3m, 10m)
            val ringLabels = listOf(0.36f to "1m", 0.66f to "3m", 0.95f to "10m")
            ringLabels.forEach { (fraction, label) ->
                val yOffsetPx = -(maxRadiusPx * 0.92f * fraction).roundToInt()
                Text(
                    text = label,
                    style = GlasmioTheme.textStyles.caption2,
                    color = colors.tertiaryContent.copy(alpha = 0.78f),
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffsetPx + 8) },
                )
            }

            // Connected Peer Node (if in active session)
            if (isConnected && !connectedPeerName.isNullOrBlank()) {
                val distM = connectedDistanceMeters ?: 2
                val normalizedDist = when {
                    distM <= 1 -> 0.40f
                    distM <= 3 -> 0.58f
                    distM <= 7 -> 0.74f
                    else -> 0.84f
                }
                val angleRad = -PI / 4.0
                val nodeRadius = maxRadiusPx * 0.85f * normalizedDist
                val offsetX = (cos(angleRad) * nodeRadius).roundToInt()
                val offsetY = (sin(angleRad) * nodeRadius).roundToInt()

                RadarPeerChip(
                    name = connectedPeerName,
                    subtitle = FormatUtils.formatDistance(distM),
                    accentColor = semantic.accentGreen,
                    iconName = ThazhSymbols.CheckCircle,
                    onClick = onCenterClick,
                    modifier = Modifier.offset { IntOffset(offsetX, offsetY) },
                )
            } else {
                // Orbiting Discovered Peers (up to 6 plotted around the radar)
                val plottedPeers = peers.take(6)
                plottedPeers.forEachIndexed { index, peer ->
                    val hashSeed = abs(peer.deviceAddress.hashCode())
                    val baseAngleDeg = (index * (360f / plottedPeers.size.coerceAtLeast(1)) + (hashSeed % 28) - 14f) - 65f
                    val angleRad = baseAngleDeg * (PI / 180.0)

                    val distM = peer.estimatedDistanceMeters
                    val radiusFraction = when {
                        distM == null -> 0.56f + (index % 3) * 0.12f
                        distM <= 1 -> 0.38f
                        distM <= 3 -> 0.56f
                        distM <= 7 -> 0.72f
                        else -> 0.84f
                    }

                    val orbitPx = maxRadiusPx * 0.85f * radiusFraction
                    val offsetX = (cos(angleRad) * orbitPx).roundToInt()
                    val offsetY = (sin(angleRad) * orbitPx).roundToInt()

                    val nodeColor = when (peer.status) {
                        PeerStatus.CONNECTED -> semantic.accentGreen
                        PeerStatus.INVITED -> semantic.warningOrange
                        else -> when (peer.discoveryTransport) {
                            PeerDiscoveryTransport.BLE -> semantic.accentCyan
                            PeerDiscoveryTransport.WIFI_AND_BLE -> semantic.accentGreen
                            else -> semantic.accentBlue
                        }
                    }

                    val subLabel = when {
                        peer.status == PeerStatus.INVITED -> stringResource(R.string.discover_connecting)
                        distM != null -> FormatUtils.formatDistance(distM)
                        peer.discoveryTransport == PeerDiscoveryTransport.BLE -> "BLE"
                        peer.discoveryTransport == PeerDiscoveryTransport.WIFI_AND_BLE -> "Wi-Fi+BLE"
                        else -> stringResource(R.string.discover_connect)
                    }

                    RadarPeerChip(
                        name = peer.deviceName.ifBlank { peer.deviceAddress },
                        subtitle = subLabel,
                        accentColor = nodeColor,
                        iconName = when (peer.discoveryTransport) {
                            PeerDiscoveryTransport.BLE -> ThazhSymbols.Bluetooth
                            PeerDiscoveryTransport.NFC -> ThazhSymbols.Nfc
                            else -> ThazhSymbols.Devices
                        },
                        onClick = { onPeerNodeClick(peer) },
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .testTag("radar_peer_node_${peer.deviceAddress}"),
                    )
                }
            }

            // Center Device Avatar Node (Tap to start/stop scan or toggle visibility)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryAccent,
                                primaryAccent.copy(alpha = 0.78f),
                            )
                        )
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = onCenterClick,
                    )
                    .testTag("radar_center_button"),
                contentAlignment = Alignment.Center,
            ) {
                if (isConnecting || !nfcPairingPeerName.isNullOrBlank()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    SfIcon(
                        systemName = when {
                            isConnected -> if (mode == RadarMode.RECEIVE) ThazhSymbols.Download else ThazhSymbols.Share
                            isHidden -> ThazhSymbols.VisibilityOff
                            mode == RadarMode.RECEIVE -> ThazhSymbols.Download
                            else -> ThazhSymbols.WifiTethering
                        },
                        contentDescription = stringResource(R.string.discover_start_scan),
                        tint = Color.White,
                        size = 28.dp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Headline & guidance text
        val statusHeadline = when {
            !nfcPairingPeerName.isNullOrBlank() -> stringResource(R.string.nfc_card_connecting, nfcPairingPeerName)
            isConnected && !connectedPeerName.isNullOrBlank() -> stringResource(R.string.receive_connected_title, connectedPeerName)
            isConnecting -> stringResource(R.string.discover_connecting)
            peers.isNotEmpty() -> stringResource(R.string.radar_found_peers_count, peers.size)
            isHidden -> stringResource(R.string.receive_ready_hidden_title, displayName.ifBlank { stringResource(R.string.app_name) })
            isDiscovering -> stringResource(R.string.radar_scanning_active_title)
            else -> stringResource(R.string.radar_idle_title)
        }

        Text(
            text = statusHeadline,
            style = GlasmioTheme.textStyles.headline.copy(fontWeight = FontWeight.Bold),
            color = colors.content,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        val statusSubtitle = when {
            errorMessage != null -> errorMessage
            isConnected -> stringResource(R.string.receive_connected_body)
            peers.isNotEmpty() -> stringResource(R.string.radar_tap_peer_hint)
            isHidden -> stringResource(R.string.receive_ready_hidden_body)
            else -> stringResource(R.string.discover_intro)
        }

        Text(
            text = statusSubtitle,
            style = GlasmioTheme.textStyles.footnote,
            color = if (errorMessage != null) colors.destructive else colors.secondaryContent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Protocol Status Strip (Wi-Fi Direct • BLE Beacon • NFC Tap)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProtocolStatusPill(
                icon = ThazhSymbols.WifiTethering,
                label = if (isDiscovering || isConnected) {
                    stringResource(R.string.radar_pill_wifi_active)
                } else {
                    stringResource(R.string.radar_pill_wifi_idle)
                },
                active = isDiscovering || isConnected,
                accentColor = semantic.accentBlue,
                onClick = onCenterClick,
            )

            if (isBleSupported) {
                ProtocolStatusPill(
                    icon = ThazhSymbols.Bluetooth,
                    label = if (isBleEnabled && (isDiscovering || !isHidden)) {
                        stringResource(R.string.radar_pill_ble_active)
                    } else {
                        stringResource(R.string.radar_pill_ble_idle)
                    },
                    active = isBleEnabled && (isDiscovering || !isHidden),
                    accentColor = semantic.accentCyan,
                    onClick = onToggleBleClick,
                )
            }

            if (isNfcSupported) {
                ProtocolStatusPill(
                    icon = ThazhSymbols.Nfc,
                    label = if (isNfcEnabled) {
                        stringResource(R.string.radar_pill_nfc_ready)
                    } else {
                        stringResource(R.string.radar_pill_nfc_off)
                    },
                    active = isNfcEnabled,
                    accentColor = if (isNfcEnabled) semantic.accentGreen else semantic.warningOrange,
                    onClick = if (!isNfcEnabled) onNfcPillClick else null,
                )
            }
        }
    }
}

@Composable
private fun RadarPeerChip(
    name: String,
    subtitle: String,
    accentColor: Color,
    iconName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .widthIn(max = 148.dp)
            .clip(ContinuousCapsule)
            .background(
                color = colors.elevatedBackground.copy(alpha = if (colors.isDark) 0.92f else 0.95f),
            )
            .border(1.2.dp, accentColor.copy(alpha = 0.65f), ContinuousCapsule)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = if (colors.isDark) 0.26f else 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            SfIcon(
                systemName = iconName,
                contentDescription = null,
                tint = accentColor,
                size = 14.dp,
            )
        }
        Column {
            Text(
                text = name,
                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = GlasmioTheme.textStyles.caption2,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProtocolStatusPill(
    icon: String,
    label: String,
    active: Boolean,
    accentColor: Color,
    onClick: (() -> Unit)?,
) {
    val colors = GlasmioTheme.colors
    val tint = if (active) accentColor else colors.secondaryContent
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 34.dp)
            .clip(ContinuousCapsule)
            .background(tint.copy(alpha = if (active) (if (colors.isDark) 0.22f else 0.12f) else 0.08f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        SfIcon(
            systemName = icon,
            contentDescription = null,
            tint = tint,
            size = 14.dp,
        )
        Text(
            text = label,
            style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
        )
    }
}
