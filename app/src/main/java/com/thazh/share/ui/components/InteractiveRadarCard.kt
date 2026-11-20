package com.thazh.share.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import com.thazh.share.data.ConnectionQuality
import com.thazh.share.data.DeviceVisibilityMode
import com.thazh.share.data.DiscoveredPeer
import com.thazh.share.data.PeerDiscoveryTransport
import com.thazh.share.data.PeerStatus
import com.thazh.share.service.WifiDirectService
import com.thazh.share.ui.theme.ThazhSymbols
import com.thazh.share.ui.theme.ThazhTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val PENDING_CONNECTION_WARNING_THRESHOLD_SEC = 10

enum class RadarMode {
    SEND,
    RECEIVE
}

/**
 * Precomputed trigonometry table for the 36-tick azimuth bezel around the spatial radar.
 * Avoids 72 sin/cos evaluations per 120Hz frame inside Canvas DrawScope.
 */
private object RadarAzimuthTable {
    const val TICK_COUNT = 36
    val cosTable = FloatArray(TICK_COUNT)
    val sinTable = FloatArray(TICK_COUNT)
    val isCardinal = BooleanArray(TICK_COUNT)
    val isMajor = BooleanArray(TICK_COUNT)

    init {
        for (i in 0 until TICK_COUNT) {
            val deg = i * 10
            val rad = deg * (PI / 180.0)
            cosTable[i] = cos(rad).toFloat()
            sinTable[i] = sin(rad).toFloat()
            isCardinal[i] = (deg % 90 == 0)
            isMajor[i] = (deg % 30 == 0)
        }
    }
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
    connectionQuality: ConnectionQuality = ConnectionQuality.STRONG,
    peers: List<DiscoveredPeer> = emptyList(),
    localBatteryPercent: Int? = null,
    localBatteryCharging: Boolean = false,
    connectedPeerBatteryPercent: Int? = null,
    connectedPeerCharging: Boolean = false,
    peerBatteryLevels: Map<String, Pair<Int, Boolean>> = emptyMap(),
    pendingConnectPeerName: String? = null,
    hapticsEnabled: Boolean = true,
    isBleSupported: Boolean = true,
    isBleEnabled: Boolean = true,
    isNfcSupported: Boolean = false,
    isNfcEnabled: Boolean = false,
    nfcPairingPeerName: String? = null,
    errorMessage: String? = null,
    onCenterClick: () -> Unit = {},
    onPeerNodeClick: (DiscoveredPeer) -> Unit = {},
    onCancelPendingConnection: (() -> Unit)? = null,
    onRetryPendingConnection: (() -> Unit)? = null,
    onToggleBleClick: (() -> Unit)? = null,
    onNfcPillClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val density = LocalDensity.current
    val view = LocalView.current

    // 1. Subtle haptic feedback when a new device is discovered on the radar screen
    val knownPeerAddresses = remember { HashSet<String>() }
    LaunchedEffect(peers, hapticsEnabled) {
        val currentAddresses = peers.mapTo(HashSet()) { it.deviceAddress.uppercase() }
        val hasNewDevice = currentAddresses.any { it !in knownPeerAddresses }
        knownPeerAddresses.retainAll(currentAddresses)
        if (hasNewDevice) {
            knownPeerAddresses.addAll(currentAddresses)
            if (hapticsEnabled) {
                runCatching {
                    val performed = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    if (!performed) {
                        view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                    }
                }
            }
        }
    }

    // 2. 10-second pending connection visual warning overlay tracker
    val invitedPeer = remember(peers) {
        peers.firstOrNull { it.status == PeerStatus.INVITED }
    }
    val isPendingConnection = !isConnected && (
        isConnecting ||
            invitedPeer != null ||
            !nfcPairingPeerName.isNullOrBlank()
        )
    val pendingPeerDisplayName = pendingConnectPeerName?.takeIf { it.isNotBlank() }
        ?: invitedPeer?.deviceName?.takeIf { it.isNotBlank() }
        ?: nfcPairingPeerName?.takeIf { it.isNotBlank() }
    val pendingSessionKey = remember(isPendingConnection, invitedPeer?.deviceAddress, pendingPeerDisplayName) {
        if (!isPendingConnection) null else "${invitedPeer?.deviceAddress.orEmpty()}_${pendingPeerDisplayName.orEmpty()}"
    }

    var pendingElapsedSeconds by remember { mutableIntStateOf(0) }
    var showPendingTimeoutOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(isPendingConnection, pendingSessionKey) {
        pendingElapsedSeconds = 0
        showPendingTimeoutOverlay = false
        if (isPendingConnection) {
            while (true) {
                delay(1_000L)
                pendingElapsedSeconds += 1
                if (pendingElapsedSeconds >= PENDING_CONNECTION_WARNING_THRESHOLD_SEC && !showPendingTimeoutOverlay) {
                    showPendingTimeoutOverlay = true
                    if (hapticsEnabled) {
                        runCatching {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                    }
                }
            }
        }
    }

    val isHidden = !isConnected && visibilityMode == DeviceVisibilityMode.HIDDEN
    val primaryAccent = when {
        isConnected -> semantic.accentGreen
        mode == RadarMode.RECEIVE && visibilityMode == DeviceVisibilityMode.ALWAYS_VISIBLE -> semantic.accentGreen
        isHidden -> colors.secondaryContent
        else -> semantic.accentBlue
    }
    val secondaryAccent = when {
        isConnected -> semantic.accentCyan
        isHidden -> colors.tertiaryContent
        else -> semantic.accentCyan
    }

    val activeRadar = isDiscovering || isConnected || (!isHidden && mode == RadarMode.RECEIVE)

    val infiniteTransition = if (activeRadar) {
        rememberInfiniteTransition(label = "spatial_radar")
    } else {
        null
    }
    val sweepAngleState = infiniteTransition?.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar_sweep_angle",
    )
    val pulsePhaseState = infiniteTransition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar_pulse_phase",
    )
    val cachedDashEffect = remember {
        PathEffect.dashPathEffect(floatArrayOf(7f, 9f), 0f)
    }

    // Borderless open spatial Radar section on both Send and Receive screens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top header bar: Local device badge (+ live local battery) + Live Signal Quality / Connected Peer Battery / Visibility badge
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
                    .weight(1f, fill = false)
                    .clip(ContinuousCapsule)
                    .background(primaryAccent.copy(alpha = if (colors.isDark) 0.22f else 0.12f))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryAccent),
                )
                GlasmioText(
                    text = displayName.ifBlank { stringResource(R.string.app_name) },
                    style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                    color = primaryAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (localBatteryPercent != null && localBatteryPercent in 0..100) {
                    RadarBatteryMiniBadge(
                        percent = localBatteryPercent,
                        isCharging = localBatteryCharging,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isConnected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (connectedPeerBatteryPercent != null && connectedPeerBatteryPercent in 0..100) {
                        RadarBatteryPill(
                            percent = connectedPeerBatteryPercent,
                            isCharging = connectedPeerCharging,
                            modifier = Modifier.testTag("radar_connected_peer_battery_pill"),
                        )
                    }
                    ConnectionQualityIndicator(
                        quality = connectionQuality,
                        showLabel = true,
                    )
                }
            } else {
                val visibilityBadgeText = when (visibilityMode) {
                    DeviceVisibilityMode.HIDDEN -> stringResource(R.string.visibility_mode_hidden)
                    DeviceVisibilityMode.TEMPORARY_10_MIN -> stringResource(
                        R.string.visibility_timer_badge,
                        WifiDirectService.formatCountdown(visibilityRemainingSeconds ?: 600),
                    )
                    else -> stringResource(R.string.visibility_active_always_badge)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(ContinuousCapsule)
                        .background(colors.secondaryContent.copy(alpha = if (colors.isDark) 0.18f else 0.10f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    SignalStrengthMiniBars(
                        barsFilled = if (isHidden) 0 else 4,
                        activeColor = primaryAccent,
                        width = 13.dp,
                        height = 10.dp,
                    )
                    GlasmioText(
                        text = visibilityBadgeText,
                        style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.Medium),
                        color = colors.secondaryContent,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2D Spatial Signal Radar Canvas + Interactive Peer Nodes + 10s Pending Timeout Visual Warning Overlay
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp),
            contentAlignment = Alignment.Center,
        ) {
            val radarSizeDp = minOf(maxWidth, maxHeight)
            val maxRadiusPx = with(density) { (radarSizeDp / 2f).toPx() }

            Canvas(
                modifier = Modifier.size(radarSizeDp),
            ) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f * 0.94f
                val outerOrbitR = maxR * 0.91f

                // 1. Multi-stop ambient radial field glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = if (colors.isDark) 0.20f else 0.12f),
                            secondaryAccent.copy(alpha = if (colors.isDark) 0.08f else 0.05f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = maxR,
                    ),
                    radius = maxR,
                    center = centerOffset,
                )

                // 2. Reticle crosshairs with clean center aperture gap
                val gapR = maxR * 0.26f
                val crossAlpha = if (colors.isDark) 0.16f else 0.12f
                drawLine(
                    color = primaryAccent.copy(alpha = crossAlpha),
                    start = Offset(centerOffset.x - outerOrbitR, centerOffset.y),
                    end = Offset(centerOffset.x - gapR, centerOffset.y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = cachedDashEffect,
                )
                drawLine(
                    color = primaryAccent.copy(alpha = crossAlpha),
                    start = Offset(centerOffset.x + gapR, centerOffset.y),
                    end = Offset(centerOffset.x + outerOrbitR, centerOffset.y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = cachedDashEffect,
                )
                drawLine(
                    color = primaryAccent.copy(alpha = crossAlpha),
                    start = Offset(centerOffset.x, centerOffset.y - outerOrbitR),
                    end = Offset(centerOffset.x, centerOffset.y - gapR),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = cachedDashEffect,
                )
                drawLine(
                    color = primaryAccent.copy(alpha = crossAlpha),
                    start = Offset(centerOffset.x, centerOffset.y + gapR),
                    end = Offset(centerOffset.x, centerOffset.y + outerOrbitR),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = cachedDashEffect,
                )

                // 3. Four Concentric Signal Quality Orbit Rings (no distance text)
                val ringFractions = floatArrayOf(0.29f, 0.50f, 0.71f, 0.91f)
                for (i in ringFractions.indices) {
                    val r = maxR * ringFractions[i]
                    val isOuter = i == ringFractions.lastIndex
                    val ringAlpha = when (i) {
                        0 -> 0.18f
                        1 -> 0.15f
                        2 -> 0.16f
                        else -> 0.24f
                    }
                    drawCircle(
                        color = primaryAccent.copy(alpha = ringAlpha),
                        radius = r,
                        center = centerOffset,
                        style = Stroke(width = if (isOuter) 1.4.dp.toPx() else 1.05.dp.toPx()),
                    )
                }

                // 4. Precision 36-Tick Azimuth Bezel around outer ring (precomputed trigonometry)
                val bezelOuterR = maxR * 0.985f
                for (i in 0 until RadarAzimuthTable.TICK_COUNT) {
                    val c = RadarAzimuthTable.cosTable[i]
                    val s = RadarAzimuthTable.sinTable[i]
                    val tickLen = when {
                        RadarAzimuthTable.isCardinal[i] -> 6.5.dp.toPx()
                        RadarAzimuthTable.isMajor[i] -> 4.5.dp.toPx()
                        else -> 2.8.dp.toPx()
                    }
                    val tickAlpha = when {
                        RadarAzimuthTable.isCardinal[i] -> 0.48f
                        RadarAzimuthTable.isMajor[i] -> 0.28f
                        else -> 0.15f
                    }
                    val tickWidth = if (RadarAzimuthTable.isCardinal[i]) 1.6.dp.toPx() else 1.0.dp.toPx()
                    val innerR = bezelOuterR - tickLen
                    drawLine(
                        color = primaryAccent.copy(alpha = tickAlpha),
                        start = Offset(centerOffset.x + c * innerR, centerOffset.y + s * innerR),
                        end = Offset(centerOffset.x + c * bezelOuterR, centerOffset.y + s * bezelOuterR),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round,
                    )
                }

                // 5. Dynamic Expanding Signal Waves & Dual-Gradient Sweep Beam (read strictly in DrawScope)
                if (activeRadar) {
                    val currentPulse = pulsePhaseState?.value ?: 0f
                    val currentSweep = sweepAngleState?.value ?: 0f

                    // Dual harmonic expanding signal ripples
                    for (ring in 0..1) {
                        val phase = (currentPulse + ring * 0.5f) % 1f
                        val radius = maxR * (0.24f + 0.67f * phase)
                        val alpha = (1f - phase) * (1f - phase) * 0.38f
                        drawCircle(
                            color = primaryAccent.copy(alpha = alpha),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = (2.2f - phase * 1.1f).dp.toPx()),
                        )
                    }

                    // Rotating luminous sweep fan + laser beam + outer beacon tip
                    rotate(degrees = currentSweep, pivot = centerOffset) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to Color.Transparent,
                                0.72f to Color.Transparent,
                                0.86f to secondaryAccent.copy(alpha = 0.09f),
                                0.95f to primaryAccent.copy(alpha = 0.22f),
                                1.0f to primaryAccent.copy(alpha = 0.38f),
                                center = centerOffset,
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = true,
                            topLeft = Offset(centerOffset.x - outerOrbitR, centerOffset.y - outerOrbitR),
                            size = Size(outerOrbitR * 2f, outerOrbitR * 2f),
                        )
                        // Leading laser beam
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = 0.15f),
                                    primaryAccent.copy(alpha = 0.85f),
                                ),
                                start = centerOffset,
                                end = Offset(centerOffset.x + outerOrbitR, centerOffset.y),
                            ),
                            start = Offset(centerOffset.x + gapR * 0.7f, centerOffset.y),
                            end = Offset(centerOffset.x + outerOrbitR, centerOffset.y),
                            strokeWidth = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        // Glowing beacon tip on the outer orbit ring
                        val tipCenter = Offset(centerOffset.x + outerOrbitR, centerOffset.y)
                        drawCircle(
                            color = primaryAccent.copy(alpha = 0.32f),
                            radius = 6.5.dp.toPx(),
                            center = tipCenter,
                        )
                        drawCircle(
                            color = primaryAccent,
                            radius = 3.2.dp.toPx(),
                            center = tipCenter,
                        )
                    }
                }

                // 6. Center Core Aura Halo
                val breathPhase = if (activeRadar) {
                    val p = pulsePhaseState?.value ?: 0f
                    0.88f + 0.24f * sin(p * PI.toFloat())
                } else {
                    1f
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = 0.36f),
                            secondaryAccent.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        center = centerOffset,
                        radius = maxR * 0.36f * breathPhase,
                    ),
                    radius = maxR * 0.36f * breathPhase,
                    center = centerOffset,
                )
            }

            // Connected Peer Node (shows Connection Signal Quality + Live Connected Peer Battery Level)
            if (isConnected && !connectedPeerName.isNullOrBlank()) {
                val qualityBars = connectionQuality.barsFilled.coerceAtLeast(1)
                val signalLabel = when (connectionQuality) {
                    ConnectionQuality.STRONG -> stringResource(R.string.quality_strong)
                    ConnectionQuality.MEDIUM -> stringResource(R.string.quality_medium)
                    ConnectionQuality.WEAK -> stringResource(R.string.quality_weak)
                    ConnectionQuality.POOR -> stringResource(R.string.quality_poor)
                    ConnectionQuality.DISCONNECTED -> stringResource(R.string.quality_disconnected)
                }
                val orbitFraction = when (qualityBars) {
                    4 -> 0.52f
                    3 -> 0.64f
                    2 -> 0.74f
                    else -> 0.82f
                }
                val angleRad = -PI / 4.0
                val nodeRadius = maxRadiusPx * 0.86f * orbitFraction
                val offsetX = (cos(angleRad) * nodeRadius).roundToInt()
                val offsetY = (sin(angleRad) * nodeRadius).roundToInt()

                RadarPeerChip(
                    name = connectedPeerName,
                    subtitle = signalLabel,
                    signalBars = qualityBars,
                    batteryPercent = connectedPeerBatteryPercent,
                    isCharging = connectedPeerCharging,
                    accentColor = semantic.accentGreen,
                    iconName = ThazhSymbols.CheckCircle,
                    onClick = onCenterClick,
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .testTag("radar_connected_peer_chip"),
                )
            } else {
                // Orbiting Discovered Peers (positioned by signal strength tier, showing signal bars + known battery)
                val plottedPeers = peers.take(6)
                plottedPeers.forEachIndexed { index, peer ->
                    val hashSeed = abs(peer.deviceAddress.hashCode())
                    val baseAngleDeg = (index * (360f / plottedPeers.size.coerceAtLeast(1)) + (hashSeed % 24) - 12f) - 65f
                    val angleRad = baseAngleDeg * (PI / 180.0)

                    val signalBars = rssiToSignalBars(peer.rssiDbm)
                    val radiusFraction = when (signalBars) {
                        4 -> 0.48f + (index % 2) * 0.08f
                        3 -> 0.62f + (index % 2) * 0.06f
                        2 -> 0.74f
                        else -> 0.84f
                    }

                    val orbitPx = maxRadiusPx * 0.86f * radiusFraction
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
                        peer.rssiDbm != null -> when (signalBars) {
                            4 -> stringResource(R.string.quality_strong)
                            3 -> stringResource(R.string.quality_medium)
                            2 -> stringResource(R.string.quality_weak)
                            else -> stringResource(R.string.quality_poor)
                        }
                        peer.discoveryTransport == PeerDiscoveryTransport.BLE -> "BLE"
                        peer.discoveryTransport == PeerDiscoveryTransport.WIFI_AND_BLE -> "Wi-Fi+BLE"
                        else -> stringResource(R.string.quality_strong)
                    }

                    val mappedBattery = peerBatteryLevels[peer.deviceAddress.uppercase()]
                    val resolvedBatteryPercent = peer.batteryPercent ?: mappedBattery?.first
                    val resolvedCharging = if (peer.batteryPercent != null) {
                        peer.isCharging
                    } else {
                        mappedBattery?.second ?: false
                    }

                    RadarPeerChip(
                        name = peer.deviceName.ifBlank { peer.deviceAddress },
                        subtitle = subLabel,
                        signalBars = signalBars,
                        batteryPercent = resolvedBatteryPercent,
                        isCharging = resolvedCharging,
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

            // Center Device Core Node (Tap to start/stop scan or toggle visibility)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryAccent,
                                primaryAccent.copy(alpha = 0.82f),
                            )
                        )
                    )
                    .border(
                        width = 1.6.dp,
                        color = Color.White.copy(alpha = 0.42f),
                        shape = CircleShape,
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = onCenterClick,
                    )
                    .testTag("radar_center_button"),
                contentAlignment = Alignment.Center,
            ) {
                if (isConnecting || !nfcPairingPeerName.isNullOrBlank()) {
                    GlasmioActivityIndicator(
                        size = 26.dp,
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    SfIcon(
                        systemName = when {
                            isConnected -> if (mode == RadarMode.RECEIVE) ThazhSymbols.Download else ThazhSymbols.SendPaperPlane
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

            // Visual Warning Overlay when a connection attempt stays in 'pending' state for > 10 seconds
            androidx.compose.animation.AnimatedVisibility(
                visible = showPendingTimeoutOverlay && isPendingConnection,
                enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.92f),
                exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.94f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp),
            ) {
                PendingConnectionTimeoutOverlay(
                    peerName = pendingPeerDisplayName,
                    elapsedSeconds = pendingElapsedSeconds,
                    onRetry = {
                        showPendingTimeoutOverlay = false
                        pendingElapsedSeconds = 0
                        onRetryPendingConnection?.invoke()
                    },
                    onCancel = {
                        showPendingTimeoutOverlay = false
                        pendingElapsedSeconds = 0
                        onCancelPendingConnection?.invoke()
                    },
                )
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

        GlasmioText(
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

        GlasmioText(
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
private fun PendingConnectionTimeoutOverlay(
    peerName: String?,
    elapsedSeconds: Int,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val overlayShape = ContinuousRoundedRectangle(22.dp)

    Column(
        modifier = modifier
            .widthIn(max = 330.dp)
            .fillMaxWidth()
            .clip(overlayShape)
            .background(
                color = colors.elevatedBackground.copy(alpha = if (colors.isDark) 0.96f else 0.97f),
            )
            .border(
                width = 1.4.dp,
                color = semantic.warningOrange.copy(alpha = 0.72f),
                shape = overlayShape,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("radar_pending_timeout_overlay"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(semantic.warningOrange.copy(alpha = if (colors.isDark) 0.24f else 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                SfIcon(
                    systemName = ThazhSymbols.Timer,
                    contentDescription = null,
                    tint = semantic.warningOrange,
                    size = 18.dp,
                )
            }
            GlasmioText(
                text = stringResource(R.string.radar_pending_timeout_title),
                style = GlasmioTheme.textStyles.subheadline.copy(fontWeight = FontWeight.Bold),
                color = colors.content,
            )
        }

        GlasmioText(
            text = if (!peerName.isNullOrBlank()) {
                stringResource(R.string.radar_pending_timeout_body, peerName, elapsedSeconds)
            } else {
                stringResource(R.string.radar_pending_timeout_body_generic, elapsedSeconds)
            },
            style = GlasmioTheme.textStyles.caption1,
            color = colors.secondaryContent,
            textAlign = TextAlign.Center,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            GlasmioButton(
                onClick = onCancel,
                emphasis = GlassEmphasis.Regular,
                destructive = true,
                size = GlasmioButtonSize.Small,
                modifier = Modifier
                    .weight(1f)
                    .testTag("radar_pending_timeout_cancel_button"),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.Close,
                    contentDescription = null,
                    tint = colors.destructive,
                    size = 14.dp,
                )
                Spacer(modifier = Modifier.width(5.dp))
                GlasmioText(
                    text = stringResource(R.string.radar_pending_timeout_cancel),
                    style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.destructive,
                )
            }

            GlasmioButton(
                onClick = onRetry,
                emphasis = GlassEmphasis.Prominent,
                navigationSurfaceColor = semantic.warningOrange,
                size = GlasmioButtonSize.Small,
                modifier = Modifier
                    .weight(1f)
                    .testTag("radar_pending_timeout_retry_button"),
            ) {
                SfIcon(
                    systemName = ThazhSymbols.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    size = 14.dp,
                )
                Spacer(modifier = Modifier.width(5.dp))
                GlasmioText(
                    text = stringResource(R.string.radar_pending_timeout_retry),
                    style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun RadarBatteryMiniBadge(
    percent: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    val semantic = ThazhTheme.semantic
    val tint = when {
        isCharging -> semantic.accentGreen
        percent <= 20 -> GlasmioTheme.colors.destructive
        percent <= 40 -> semantic.warningOrange
        else -> semantic.accentGreen
    }
    Row(
        modifier = modifier
            .clip(ContinuousCapsule)
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SfIcon(
            systemName = ThazhSymbols.BatterySaver,
            contentDescription = null,
            tint = tint,
            size = 11.dp,
        )
        GlasmioText(
            text = stringResource(
                if (isCharging) R.string.radar_battery_charging else R.string.radar_battery_percent,
                percent,
            ),
            style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.Bold),
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
private fun RadarBatteryPill(
    percent: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val semantic = ThazhTheme.semantic
    val tint = when {
        isCharging -> semantic.accentGreen
        percent <= 20 -> colors.destructive
        percent <= 40 -> semantic.warningOrange
        else -> semantic.accentGreen
    }
    Row(
        modifier = modifier
            .clip(ContinuousCapsule)
            .background(tint.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
            .border(0.9.dp, tint.copy(alpha = 0.45f), ContinuousCapsule)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SfIcon(
            systemName = ThazhSymbols.BatterySaver,
            contentDescription = null,
            tint = tint,
            size = 13.dp,
        )
        GlasmioText(
            text = stringResource(
                if (isCharging) R.string.radar_battery_charging else R.string.radar_battery_percent,
                percent,
            ),
            style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
private fun RadarPeerChip(
    name: String,
    subtitle: String,
    signalBars: Int,
    batteryPercent: Int? = null,
    isCharging: Boolean = false,
    accentColor: Color,
    iconName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .widthIn(max = 178.dp)
            .clip(ContinuousCapsule)
            .background(
                color = colors.elevatedBackground.copy(alpha = if (colors.isDark) 0.93f else 0.96f),
            )
            .border(1.2.dp, accentColor.copy(alpha = 0.68f), ContinuousCapsule)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GlasmioText(
                text = name,
                style = GlasmioTheme.textStyles.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SignalStrengthMiniBars(
                    barsFilled = signalBars,
                    activeColor = accentColor,
                    width = 12.dp,
                    height = 9.dp,
                )
                GlasmioText(
                    text = subtitle,
                    style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.Medium),
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (batteryPercent != null && batteryPercent in 0..100) {
                    RadarBatteryMiniBadge(
                        percent = batteryPercent,
                        isCharging = isCharging,
                    )
                }
            }
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
        GlasmioText(
            text = label,
            style = GlasmioTheme.textStyles.caption2.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
        )
    }
}
