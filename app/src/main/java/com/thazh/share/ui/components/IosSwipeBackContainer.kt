package com.thazh.share.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioSwipeBackExclusionRegistry
import io.github.neoruaa.glasmio.core.LocalGlasmioSwipeBackExclusions
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

private const val IOS_PARALLAX_FACTOR = 0.30f
private const val IOS_DIM_MAX_ALPHA = 0.14f

/**
 * Interactive iOS-style left-to-right swipe-back container with:
 * - Full-page and left-edge horizontal gesture recognition
 * - Automatic exclusion of horizontal controls via [LocalGlasmioSwipeBackExclusions]
 * - 30% underlying page parallax, 14% dimming scrim, and left-edge drop shadow
 * - UIKit 3-sample velocity + acceleration release projection and spring completion
 */
@Composable
fun IosSwipeBackContainer(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    previousContent: (@Composable () -> Unit)? = null,
    currentContent: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val exclusions = remember { GlasmioSwipeBackExclusionRegistry() }
    val rootCoordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val currentOnBack by rememberUpdatedState(onBack)
    val currentEnabled by rememberUpdatedState(enabled)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val settleAnimatable = remember { Animatable(0f) }
    var isSettling by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            isDragging = false
            isSettling = false
            dragProgress = 0f
            settleAnimatable.snapTo(0f)
        }
    }

    val activeProgress = when {
        isDragging -> dragProgress
        isSettling -> settleAnimatable.value
        else -> 0f
    }.coerceIn(0f, 1f)

    val showUnderlay = previousContent != null && activeProgress > 0.0005f
    val cornerShape = remember(activeProgress > 0.005f) {
        if (activeProgress > 0.005f) ContinuousRoundedRectangle(24.dp) else RectangleShape
    }

    val gestureModifier = if (!enabled) {
        Modifier
    } else {
        Modifier.pointerInput(layoutDirection, exclusions) {
            val sign = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
            val edgePriorityWidthPx = 28.dp.toPx()
            val slopPx = max(viewConfiguration.touchSlop, 10.dp.toPx())

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                if (!currentEnabled || isSettling) return@awaitEachGesture
                val root = rootCoordinates[0]?.takeIf { it.isAttached } ?: return@awaitEachGesture

                val isLeftEdgeStart = if (layoutDirection == LayoutDirection.Ltr) {
                    down.position.x <= edgePriorityWidthPx
                } else {
                    down.position.x >= size.width - edgePriorityWidthPx
                }

                if (!isLeftEdgeStart && exclusions.hitTest(root.localToWindow(down.position))) {
                    return@awaitEachGesture
                }

                val widthPx = size.width.toFloat().coerceAtLeast(1f)
                val velocityTracker = VelocityTracker()
                velocityTracker.addPosition(down.uptimeMillis, down.position)
                val releaseProjector = IosReleaseProjector(widthPx / density.density)
                releaseProjector.sample(0f, down.uptimeMillis, 0f)

                var totalDx = 0f
                var totalDy = 0f
                var claimed = false

                while (!claimed) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                    if (!change.pressed || event.changes.count { it.pressed } != 1 || !currentEnabled) {
                        return@awaitEachGesture
                    }
                    if (change.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis) {
                        return@awaitEachGesture
                    }

                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val delta = change.positionChangeIgnoreConsumed()
                    val signedDx = sign * delta.x
                    totalDx += signedDx
                    totalDy += delta.y

                    if (!isLeftEdgeStart && change.isConsumed) {
                        return@awaitEachGesture
                    }
                    if (totalDx < -slopPx || (abs(totalDy) > slopPx && abs(totalDy) > abs(totalDx))) {
                        return@awaitEachGesture
                    }
                    if (totalDx > slopPx && totalDx > abs(totalDy) * 1.15f) {
                        val initialTravel = (totalDx - slopPx).coerceAtLeast(0f)
                        totalDx = initialTravel
                        dragProgress = (initialTravel / widthPx).coerceIn(0f, 1f)
                        val vx = sign * velocityTracker.calculateVelocity().x / widthPx
                        releaseProjector.sample(dragProgress, change.uptimeMillis, vx)
                        isDragging = true
                        change.consume()
                        claimed = true
                    }
                }

                var cancelled = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || event.changes.any { it.id != down.id && it.pressed } || !currentEnabled) {
                        cancelled = true
                    }
                    event.changes.forEach { it.consume() }
                    if (change != null) {
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                    }
                    if (event.changes.none { it.pressed }) {
                        if (change != null && !cancelled) {
                            totalDx = (totalDx + sign * change.positionChangeIgnoreConsumed().x).coerceAtLeast(0f)
                            dragProgress = (totalDx / widthPx).coerceIn(0f, 1f)
                            val vx = sign * velocityTracker.calculateVelocity().x / widthPx
                            releaseProjector.sample(dragProgress, change.uptimeMillis, vx)
                        }
                        break
                    }
                    if (cancelled || change == null) continue
                    totalDx = (totalDx + sign * change.positionChangeIgnoreConsumed().x).coerceAtLeast(0f)
                    dragProgress = (totalDx / widthPx).coerceIn(0f, 1f)
                    val vx = sign * velocityTracker.calculateVelocity().x / widthPx
                    releaseProjector.sample(dragProgress, change.uptimeMillis, vx)
                }

                val releaseVelocity = if (cancelled) 0f else sign * velocityTracker.calculateVelocity().x / widthPx
                val endProgress = dragProgress
                val shouldCommit = !cancelled && releaseProjector.shouldCommit(endProgress, releaseVelocity)

                isDragging = false
                isSettling = true
                scope.launch {
                    settleAnimatable.snapTo(endProgress)
                    val target = if (shouldCommit) 1f else 0f
                    settleAnimatable.animateTo(
                        targetValue = target,
                        initialVelocity = releaseVelocity,
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = 460f,
                            visibilityThreshold = 0.001f,
                        ),
                    )
                    if (shouldCommit) {
                        currentOnBack()
                    }
                    dragProgress = 0f
                    settleAnimatable.snapTo(0f)
                    isSettling = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates[0] = it }
            .then(gestureModifier),
    ) {
        if (showUnderlay && previousContent != null) {
            val underlayExclusions = remember { GlasmioSwipeBackExclusionRegistry() }
            CompositionLocalProvider(LocalGlasmioSwipeBackExclusions provides underlayExclusions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val sign = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                            translationX = -sign * size.width * IOS_PARALLAX_FACTOR * (1f - activeProgress)
                        },
                ) {
                    previousContent()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = IOS_DIM_MAX_ALPHA * (1f - activeProgress)),
                            ),
                    )
                }
            }
        }

        if (activeProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        val parentW = (rootCoordinates[0]?.size?.width ?: 0).toFloat()
                        val sign = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                        translationX = sign * (parentW * activeProgress - size.width)
                        alpha = (1f - activeProgress).coerceIn(0f, 1f)
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.14f),
                            ),
                        ),
                    ),
            )
        }

        CompositionLocalProvider(LocalGlasmioSwipeBackExclusions provides exclusions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val sign = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                        translationX = sign * size.width * activeProgress
                    }
                    .clip(cornerShape),
            ) {
                currentContent()
            }
        }
    }
}

/**
 * UIKit three-sample velocity + acceleration projection in normalized screen-width units.
 */
private class IosReleaseProjector(widthPoints: Float) {
    private val threshold = minOf(165f / widthPoints.coerceAtLeast(1f), 0.36f)
    private val velocities = FloatArray(3)
    private val accelerations = FloatArray(3)
    private var count = 0
    private var previousProgress = 0f
    private var previousTime = 0L
    private var previousVelocity = 0f
    private var previousAcceleration = 0f

    fun sample(progress: Float, timeMillis: Long, observedVelocity: Float? = null) {
        if (count > 0 && timeMillis <= previousTime) return
        val seconds = if (count == 0) 0f else (timeMillis - previousTime) / 1000f
        val velocity = observedVelocity ?: if (seconds > 0f) (progress - previousProgress) / seconds else 0f
        val acceleration = if (seconds > 0f) {
            ((velocity - previousVelocity) / seconds).coerceIn(previousAcceleration - 5f, previousAcceleration + 5f)
        } else {
            0f
        }
        velocities[count % 3] = velocity
        accelerations[count % 3] = acceleration
        previousProgress = progress
        previousTime = timeMillis
        previousVelocity = velocity
        previousAcceleration = acceleration
        count++
    }

    fun shouldCommit(progress: Float, releaseVelocity: Float): Boolean {
        if (releaseVelocity < -0.45f) return false
        if (progress >= 0.36f || releaseVelocity >= 0.75f) return true
        val samples = minOf(count, 3)
        if (samples == 0) return false
        val avgVelocity = velocities.sum() / samples
        if (count < 3) return avgVelocity > 0.3f
        val avgAcceleration = accelerations.sum() / samples
        return progress + 0.35f * avgVelocity + 0.5f * 0.35f * 0.35f * avgAcceleration > threshold
    }
}
