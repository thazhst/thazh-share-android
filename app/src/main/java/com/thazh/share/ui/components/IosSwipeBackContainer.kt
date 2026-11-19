package com.thazh.share.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * - Zero-recomposition frame updates (all drag/settle progress read strictly in Draw/Layer phases)
 * - Touch-down underlay pre-warming so heavy screens never stutter on the first drag frame
 * - Hold-at-1f commit state so the outgoing screen never snaps back or double-animates on pop
 * - Automatic exclusion of horizontal controls via [LocalGlasmioSwipeBackExclusions]
 */
@Composable
fun IosSwipeBackContainer(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentKey: Any? = Unit,
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

    // Single Animatable drives both finger tracking and spring settle without recomposing the tree.
    val progressAnimatable = remember(contentKey) { Animatable(0f) }
    var isGestureActive by remember(contentKey) { mutableStateOf(false) }
    var isSettling by remember(contentKey) { mutableStateOf(false) }
    var isCommitted by remember(contentKey) { mutableStateOf(false) }

    LaunchedEffect(enabled, contentKey) {
        if (!enabled) {
            isGestureActive = false
            isSettling = false
            isCommitted = false
            progressAnimatable.snapTo(0f)
        }
    }

    val currentProgress: () -> Float = remember(progressAnimatable) {
        {
            if (isCommitted) 1f else progressAnimatable.value.coerceIn(0f, 1f)
        }
    }

    val draggedCornerShape = remember { ContinuousRoundedRectangle(24.dp) }
    val edgeShadowBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.16f),
            ),
        )
    }

    val gestureModifier = if (!enabled) {
        Modifier
    } else {
        Modifier.pointerInput(layoutDirection, exclusions, contentKey) {
            val sign = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
            val edgePriorityWidthPx = 32.dp.toPx()
            val edgePrewarmWidthPx = 56.dp.toPx()
            val slopPx = max(viewConfiguration.touchSlop, 10.dp.toPx())

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                if (!currentEnabled || isSettling || isCommitted) return@awaitEachGesture
                val root = rootCoordinates[0]?.takeIf { it.isAttached } ?: return@awaitEachGesture

                val isLeftEdgeStart = if (layoutDirection == LayoutDirection.Ltr) {
                    down.position.x <= edgePriorityWidthPx
                } else {
                    down.position.x >= size.width - edgePriorityWidthPx
                }
                val isNearLeftEdge = if (layoutDirection == LayoutDirection.Ltr) {
                    down.position.x <= edgePrewarmWidthPx
                } else {
                    down.position.x >= size.width - edgePrewarmWidthPx
                }

                if (!isLeftEdgeStart && exclusions.hitTest(root.localToWindow(down.position))) {
                    return@awaitEachGesture
                }

                // Pre-warm underlay composition on left-edge touch-down while finger is still at 0f
                // so the first drag movement frame never drops frames composing previousContent.
                if (isNearLeftEdge && previousContent != null && !isGestureActive) {
                    isGestureActive = true
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
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed || event.changes.count { it.pressed } != 1 || !currentEnabled) {
                        if (!isSettling && !isCommitted) isGestureActive = false
                        return@awaitEachGesture
                    }
                    if (change.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis) {
                        if (!isSettling && !isCommitted) isGestureActive = false
                        return@awaitEachGesture
                    }

                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val delta = change.positionChangeIgnoreConsumed()
                    val signedDx = sign * delta.x
                    totalDx += signedDx
                    totalDy += delta.y

                    if (!isLeftEdgeStart && change.isConsumed) {
                        if (!isSettling && !isCommitted) isGestureActive = false
                        return@awaitEachGesture
                    }
                    if (totalDx < -slopPx || (abs(totalDy) > slopPx && abs(totalDy) > abs(totalDx))) {
                        if (!isSettling && !isCommitted) isGestureActive = false
                        return@awaitEachGesture
                    }
                    if (totalDx > slopPx && totalDx > abs(totalDy) * 1.12f) {
                        val initialTravel = (totalDx - slopPx).coerceAtLeast(0f)
                        totalDx = initialTravel
                        val initialProgress = (initialTravel / widthPx).coerceIn(0f, 1f)
                        isGestureActive = true
                        scope.launch { progressAnimatable.snapTo(initialProgress) }
                        val vx = sign * velocityTracker.calculateVelocity().x / widthPx
                        releaseProjector.sample(initialProgress, change.uptimeMillis, vx)
                        change.consume()
                        claimed = true
                    }
                }

                var cancelled = false
                var latestProgress = (totalDx / widthPx).coerceIn(0f, 1f)
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
                            latestProgress = (totalDx / widthPx).coerceIn(0f, 1f)
                            scope.launch { progressAnimatable.snapTo(latestProgress) }
                            val vx = sign * velocityTracker.calculateVelocity().x / widthPx
                            releaseProjector.sample(latestProgress, change.uptimeMillis, vx)
                        }
                        break
                    }
                    if (cancelled || change == null) continue
                    totalDx = (totalDx + sign * change.positionChangeIgnoreConsumed().x).coerceAtLeast(0f)
                    latestProgress = (totalDx / widthPx).coerceIn(0f, 1f)
                    scope.launch { progressAnimatable.snapTo(latestProgress) }
                    val vx = sign * velocityTracker.calculateVelocity().x / widthPx
                    releaseProjector.sample(latestProgress, change.uptimeMillis, vx)
                }

                val releaseVelocity = if (cancelled) 0f else sign * velocityTracker.calculateVelocity().x / widthPx
                val shouldCommit = !cancelled && releaseProjector.shouldCommit(latestProgress, releaseVelocity)

                isSettling = true
                scope.launch {
                    progressAnimatable.snapTo(latestProgress)
                    val target = if (shouldCommit) 1f else 0f
                    progressAnimatable.animateTo(
                        targetValue = target,
                        initialVelocity = releaseVelocity,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 560f,
                            visibilityThreshold = 0.004f,
                        ),
                    )
                    if (shouldCommit) {
                        // Lock visual progress at 1f before invoking onBack() so the outgoing page
                        // never snaps back to 0f before the parent navigation state updates.
                        isCommitted = true
                        isSettling = false
                        currentOnBack()
                    } else {
                        progressAnimatable.snapTo(0f)
                        isSettling = false
                        isGestureActive = false
                    }
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
        if (isGestureActive && previousContent != null) {
            val underlayExclusions = remember { GlasmioSwipeBackExclusionRegistry() }
            CompositionLocalProvider(LocalGlasmioSwipeBackExclusions provides underlayExclusions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = currentProgress()
                            val sign = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                            translationX = -sign * size.width * IOS_PARALLAX_FACTOR * (1f - p)
                            alpha = if (p > 0.0005f) 1f else 0f
                        }
                        .drawWithContent {
                            drawContent()
                            val p = currentProgress()
                            val dimAlpha = IOS_DIM_MAX_ALPHA * (1f - p)
                            if (dimAlpha > 0.001f) {
                                drawRect(color = Color.Black, alpha = dimAlpha)
                            }
                        },
                ) {
                    previousContent()
                }
            }
        }

        if (isGestureActive) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        val p = currentProgress()
                        val parentW = (rootCoordinates[0]?.size?.width ?: 0).toFloat()
                        val sign = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                        translationX = sign * (parentW * p - size.width)
                        alpha = if (p > 0.002f && p < 0.998f) (1f - p).coerceIn(0f, 1f) else 0f
                    }
                    .background(edgeShadowBrush),
            )
        }

        CompositionLocalProvider(LocalGlasmioSwipeBackExclusions provides exclusions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = currentProgress()
                        val sign = if (layoutDirection == LayoutDirection.Ltr) 1f else -1f
                        translationX = sign * size.width * p
                        if (p > 0.002f && p < 0.999f) {
                            shape = draggedCornerShape
                            clip = true
                        } else {
                            clip = false
                        }
                    },
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
    private val threshold = minOf(165f / widthPoints.coerceAtLeast(1f), 0.35f)
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
        if (progress >= 0.34f || releaseVelocity >= 0.70f) return true
        val samples = minOf(count, 3)
        if (samples == 0) return false
        val avgVelocity = velocities.sum() / samples
        if (count < 3) return avgVelocity > 0.28f
        val avgAcceleration = accelerations.sum() / samples
        return progress + 0.35f * avgVelocity + 0.5f * 0.35f * 0.35f * avgAcceleration > threshold
    }
}
