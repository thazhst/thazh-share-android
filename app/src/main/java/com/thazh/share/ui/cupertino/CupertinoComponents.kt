package com.thazh.share.ui.cupertino

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.thazh.share.ui.theme.CupertinoTheme
import com.thazh.share.ui.theme.LocalAppBackdrop
import com.thazh.share.ui.theme.LocalOverlayBackdrop
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

val LocalCupertinoContentColor = staticCompositionLocalOf { Color.Unspecified }

/**
 * Crisp stroke border matching Shapes-1.2.1's `Border.kt` for continuous-curvature shapes.
 */
fun Modifier.cupertinoBorder(
    width: Dp,
    color: Color,
    shape: Shape
): Modifier = this then CupertinoBorderElement(width, color, shape)

private data class CupertinoBorderElement(
    val width: Dp,
    val color: Color,
    val shape: Shape
) : ModifierNodeElement<CupertinoBorderNode>() {
    override fun create() = CupertinoBorderNode(width, color, shape)
    override fun update(node: CupertinoBorderNode) {
        node.width = width
        node.color = color
        node.shape = shape
        node.invalidateDrawCache()
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "cupertinoBorder"
        properties["width"] = width
        properties["color"] = color
        properties["shape"] = shape
    }
}

private class CupertinoBorderNode(
    var width: Dp,
    var color: Color,
    var shape: Shape
) : DelegatingNode() {
    override val shouldAutoInvalidate: Boolean = false

    private val drawNode = delegate(CacheDrawModifierNode {
        val minDimension = size.minDimension
        if (width.value <= 0f || color.isUnspecified || color.alpha <= 0f || minDimension <= 0f) {
            return@CacheDrawModifierNode onDrawWithContent { drawContent() }
        }
        val strokeWidthPx = if (width == Dp.Hairline) {
            1f
        } else {
            ceil(width.toPx().fastCoerceAtMost(minDimension * 0.5f))
        }
        val borderSize = Size(
            (size.width - strokeWidthPx).coerceAtLeast(0f),
            (size.height - strokeWidthPx).coerceAtLeast(0f)
        )
        val outline = shape.createOutline(borderSize, layoutDirection, this)
        val strokeStyle = Stroke(strokeWidthPx)

        onDrawWithContent {
            drawContent()
            translate(strokeWidthPx * 0.5f, strokeWidthPx * 0.5f) {
                drawOutline(
                    outline = outline,
                    color = color,
                    style = strokeStyle
                )
            }
        }
    })

    fun invalidateDrawCache() {
        drawNode.invalidateDrawCache()
    }
}

@Composable
fun CupertinoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalCupertinoContentColor.current.let {
        if (it == Color.Unspecified) CupertinoTheme.colors.label else it
    },
    style: TextStyle = CupertinoTheme.typography.body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null
) {
    val mergedStyle = if (textAlign != null) {
        style.copy(color = color, textAlign = textAlign)
    } else {
        style.copy(color = color)
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun CupertinoIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalCupertinoContentColor.current.let {
        if (it == Color.Unspecified) CupertinoTheme.colors.label else it
    }
) {
    val painter = rememberVectorPainter(imageVector)
    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = semanticsModifier.size(22.dp),
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null
    )
}

@Composable
fun CupertinoDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(0.5.dp)
            .background(CupertinoTheme.colors.separator)
    )
}

@Composable
fun LiquidCard(
    modifier: Modifier = Modifier,
    shape: Shape = CupertinoTheme.shapes.card,
    surfaceColor: Color = CupertinoTheme.colors.glassSurface,
    blurRadius: Dp = 18.dp,
    borderColor: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val backdrop = LocalAppBackdrop.current
    val isLight = CupertinoTheme.colors.isLight
    val highlightAlpha = if (isLight) 0.78f else 0.50f
    val shadowAlpha = if (isLight) 0.07f else 0.24f
    val interactionSource = remember { MutableInteractionSource() }
    val borderMod = if (borderColor != Color.Unspecified && borderColor.alpha > 0f) {
        Modifier.cupertinoBorder(0.6.dp, borderColor, shape)
    } else {
        Modifier
    }
    val clickMod = if (onClick != null) {
        Modifier
            .elasticHold(enabled = true, pressedScale = 0.97f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
    } else {
        Modifier
    }
    val cardModifier = modifier
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                lens(8f.dp.toPx(), 16f.dp.toPx())
            },
            highlight = {
                Highlight(
                    width = 0.6.dp,
                    alpha = highlightAlpha,
                    style = HighlightStyle.Default
                )
            },
            shadow = {
                Shadow(
                    radius = 16.dp,
                    offset = DpOffset(0.dp, 6.dp),
                    color = Color.Black.copy(alpha = shadowAlpha)
                )
            },
            onDrawSurface = {
                drawRect(surfaceColor)
            }
        )
        .then(borderMod)
        .then(clickMod)

    Column(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = CupertinoTheme.colors.accentBlue,
    surfaceColor: Color = Color.Unspecified,
    contentColor: Color = Color.White,
    shape: Shape = CupertinoTheme.shapes.pill,
    isInteractive: Boolean = true,
    height: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = LocalAppBackdrop.current
    val colors = CupertinoTheme.colors
    val effectiveContentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.55f)
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = height.coerceAtLeast(48.dp))
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                highlight = {
                    Highlight(
                        width = 0.5.dp,
                        alpha = if (enabled) 0.90f else 0.35f,
                        style = HighlightStyle.Default
                    )
                },
                shadow = {
                    Shadow(
                        radius = 12.dp,
                        offset = DpOffset(0.dp, 4.dp),
                        color = if (tint != Color.Unspecified && enabled) {
                            tint.copy(alpha = 0.22f)
                        } else {
                            Color.Black.copy(alpha = 0.08f)
                        }
                    )
                },
                layerBlock = if (isInteractive && enabled) {
                    {
                        if (size.isSpecified && size.width > 1f && size.height > 1f && size.maxDimension > 1f) {
                            val width = size.width
                            val heightPx = size.height
                            val progress = interactiveHighlight.pressProgress.coerceIn(-0.25f, 1.35f)
                            val scale = lerp(1f, 1f + 6f.dp.toPx() / heightPx, progress)

                            val maxOffset = size.minDimension.coerceAtLeast(1f)
                            val initialDerivative = 0.06f
                            val offset = interactiveHighlight.offset
                            if (offset.isSpecified) {
                                translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                                translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                                val maxDragScale = 6f.dp.toPx() / heightPx
                                val offsetAngle = atan2(offset.y, offset.x)
                                scaleX = scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / heightPx).fastCoerceAtMost(1f)
                                scaleY = scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (heightPx / width).fastCoerceAtMost(1f)
                            } else {
                                scaleX = scale
                                scaleY = scale
                            }
                        }
                    }
                } else null,
                onDrawSurface = {
                    if (tint != Color.Unspecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = if (enabled) 0.72f else 0.32f))
                    }
                    if (surfaceColor != Color.Unspecified) {
                        drawRect(surfaceColor)
                    } else if (tint == Color.Unspecified) {
                        drawRect(colors.glassSurface)
                    }
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive && enabled) {
                    interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalCupertinoContentColor provides effectiveContentColor) {
            content()
        }
    }
}

@Composable
fun LiquidIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = CupertinoTheme.colors.label,
    surfaceColor: Color = CupertinoTheme.colors.glassProminentSurface,
    size: Dp = 48.dp,
    badgeCount: Int = 0
) {
    val backdrop = LocalAppBackdrop.current
    val shape = Capsule()
    val colors = CupertinoTheme.colors
    val animationScope = rememberCoroutineScope()
    val highlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val touchSize = size.coerceAtLeast(48.dp)
    val glassCircleSize = (touchSize - 4.dp).coerceAtLeast(42.dp)

    Box(
        modifier = modifier
            .size(touchSize)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (enabled) highlight.gestureModifier else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(glassCircleSize)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    },
                    highlight = {
                        Highlight(
                            width = 0.5.dp,
                            alpha = if (colors.isLight) 0.85f else 0.60f,
                            style = HighlightStyle.Default
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 10.dp,
                            offset = DpOffset(0.dp, 3.dp),
                            color = Color.Black.copy(alpha = if (colors.isLight) 0.08f else 0.24f)
                        )
                    },
                    layerBlock = if (enabled) {
                        {
                            val s = lerp(1f, 1.14f, highlight.pressProgress.coerceIn(-0.25f, 1.35f))
                            scaleX = s
                            scaleY = s
                        }
                    } else null,
                    onDrawSurface = {
                        drawRect(surfaceColor)
                    }
                )
                .then(if (enabled) highlight.modifier else Modifier),
            contentAlignment = Alignment.Center
        ) {
            CupertinoIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .background(colors.destructiveRed, Capsule())
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                CupertinoText(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val backdrop = LocalAppBackdrop.current
    val colors = CupertinoTheme.colors
    val accentColor = colors.accentGreen
    val trackColor = if (colors.isLight) Color(0xFF787878).copy(0.20f) else Color(0xFF787880).copy(0.36f)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }

    val dampedDragAnimation = remember(animationScope, dragWidth) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (!enabled) return@DampedDragAnimation
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onCheckedChange(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (checked) 0f else 1f
                    onCheckedChange(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!enabled) return@DampedDragAnimation
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction = if (isLtr) {
                    (fraction + delta).fastCoerceIn(0f, 1f)
                } else {
                    (fraction - delta).fastCoerceIn(0f, 1f)
                }
            }
        )
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { value ->
                dampedDragAnimation.updateValue(value)
            }
    }

    LaunchedEffect(checked) {
        snapshotFlow { checked }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberLayerBackdrop()

    // Minimum 48.dp touch target around the compact 64.dp x 28.dp LiquidGlass switch
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                if (enabled) {
                    onClick {
                        onCheckedChange(!checked)
                        true
                    }
                }
            }
            .then(if (enabled) dampedDragAnimation.modifier else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    val f = dampedDragAnimation.value.fastCoerceIn(0f, 1f)
                    drawRect(lerp(trackColor, accentColor, f))
                }
                .size(64f.dp, 28f.dp)
        )

        Box(
            modifier = Modifier
                .graphicsLayer {
                    val f = dampedDragAnimation.value.fastCoerceIn(0f, 1f)
                    val paddingPx = 2f.dp.toPx()
                    translationX = if (isLtr) {
                        lerp(paddingPx, paddingPx + dragWidth, f)
                    } else {
                        lerp(-paddingPx, -(paddingPx + dragWidth), f)
                    }
                    alpha = if (enabled) 1f else 0.6f
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress.fastCoerceIn(0f, 1f)
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            if (scaleY > 0.01f) {
                                scale(scaleX, scaleY) {
                                    drawBackdrop()
                                }
                            }
                        }
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress.fastCoerceIn(0f, 1f)
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress.fastCoerceIn(0f, 1f)
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.08f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress.fastCoerceIn(0f, 1f)
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress.fastCoerceIn(0f, 1f)
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}

@Composable
fun CupertinoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backTestTag: String = "top_bar_back_button",
    actions: @Composable RowScope.() -> Unit = {}
) {
    val backdrop = LocalAppBackdrop.current
    val colors = CupertinoTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    vibrancy()
                    blur(22f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(colors.glassProminentSurface)
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .elasticHold(pressedScale = 0.88f)
                        .clip(Capsule())
                        .testTag(backTestTag)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CupertinoIcon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                        contentDescription = "Quay lại",
                        tint = colors.accentBlue,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                CupertinoText(
                    text = title,
                    style = CupertinoTheme.typography.headline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    CupertinoText(
                        text = subtitle,
                        style = CupertinoTheme.typography.caption,
                        color = colors.secondaryLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                content = actions
            )
        }
        CupertinoDivider()
    }
}

@Composable
fun CupertinoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CupertinoTheme.colors.accentBlue,
    trackColor: Color = CupertinoTheme.colors.fillTertiary,
    height: Dp = 6.dp
) {
    val clamped by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 400f),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(Capsule())
            .drawBehind {
                drawRect(trackColor)
                val fraction = clamped.coerceIn(0f, 1f)
                if (fraction > 0f) {
                    val fillWidth = (size.width * fraction).coerceIn(size.height.coerceAtMost(size.width), size.width)
                    val corner = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(
                        color = color,
                        size = Size(fillWidth, size.height),
                        cornerRadius = corner
                    )
                }
            }
    )
}

@Composable
fun CupertinoActivityIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = CupertinoTheme.colors.secondaryLabel
) {
    val transition = rememberInfiniteTransition(label = "cupertino_spinner")
    val step by transition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_step"
    )

    Canvas(modifier = modifier.size(size)) {
        val count = 8
        val activeIndex = step.toInt() % count
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerR = this.size.minDimension * 0.45f
        val innerR = this.size.minDimension * 0.22f
        val strokeW = this.size.minDimension * 0.11f

        for (i in 0 until count) {
            val angle = (i * (360f / count)) * (PI / 180.0)
            val dist = (activeIndex - i + count) % count
            val alpha = 1f - (dist / count.toFloat()) * 0.72f
            val start = Offset(
                center.x + (innerR * cos(angle)).toFloat(),
                center.y + (innerR * sin(angle)).toFloat()
            )
            val end = Offset(
                center.x + (outerR * cos(angle)).toFloat(),
                center.y + (outerR * sin(angle)).toFloat()
            )
            drawLine(
                color = color.copy(alpha = alpha),
                start = start,
                end = end,
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun CupertinoSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colors = CupertinoTheme.colors
    val shape = CupertinoTheme.shapes.input
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(colors.fillTertiary)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CupertinoIcon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = colors.secondaryLabel,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                CupertinoText(
                    text = placeholder,
                    style = CupertinoTheme.typography.body,
                    color = colors.tertiaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = CupertinoTheme.typography.body.copy(color = colors.label),
                cursorBrush = SolidColor(colors.accentBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(Capsule())
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = { onQueryChange("") }
                    ),
                contentAlignment = Alignment.Center
            ) {
                CupertinoIcon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Xóa tìm kiếm",
                    tint = colors.secondaryLabel,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CupertinoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = CupertinoTheme.colors
    val shape = CupertinoTheme.shapes.input
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(colors.fillTertiary)
                .cupertinoBorder(
                    width = 0.8.dp,
                    color = if (errorText != null) colors.destructiveRed else colors.separator.copy(alpha = 0.35f),
                    shape = shape
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                CupertinoText(
                    text = placeholder,
                    style = CupertinoTheme.typography.body,
                    color = colors.tertiaryLabel
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = CupertinoTheme.typography.body.copy(color = colors.label),
                cursorBrush = SolidColor(colors.accentBlue),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!errorText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            CupertinoText(
                text = errorText,
                style = CupertinoTheme.typography.caption,
                color = colors.destructiveRed
            )
        }
    }
}

@Composable
fun CupertinoFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val colors = CupertinoTheme.colors
    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.accentBlue else colors.glassSurface,
        label = "pill_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else colors.label,
        label = "pill_fg"
    )
    val shape = Capsule()
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .elasticHold(pressedScale = 0.92f)
            .clip(shape)
            .background(bgColor)
            .cupertinoBorder(
                width = 0.6.dp,
                color = if (selected) colors.accentBlue else colors.separator.copy(alpha = 0.3f),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            CupertinoIcon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        CupertinoText(
            text = label,
            style = CupertinoTheme.typography.subheadline.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun LiquidAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmDestructive: Boolean = false,
    dismissText: String? = "Đóng",
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CupertinoTheme.colors
    val isLightTheme = colors.isLight
    val backdrop = LocalOverlayBackdrop.current
    val dialogShape = remember { RoundedRectangle(44.dp, RoundedCornerStyle.Continuous) }
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor = if (confirmDestructive) {
        colors.destructiveRed
    } else if (isLightTheme) {
        Color(0xFF0088FF)
    } else {
        Color(0xFF0091FF)
    }
    val containerColor = if (isLightTheme) {
        Color(0xFFFAFAFA).copy(0.6f)
    } else {
        Color(0xFF121212).copy(0.4f)
    }
    val dimColor = if (isLightTheme) {
        Color(0xFF29293A).copy(0.23f)
    } else {
        Color(0xFF121212).copy(0.56f)
    }

    val entranceScale = remember { Animatable(0.92f) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f)
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val s = entranceScale.value
                        scaleX = s
                        scaleY = s
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { dialogShape },
                        effects = {
                            colorControls(
                                brightness = if (isLightTheme) 0.2f else 0f,
                                saturation = 1.5f
                            )
                            blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                            lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                        },
                        highlight = { Highlight.Plain },
                        shadow = {
                            Shadow(
                                radius = 28.dp,
                                offset = DpOffset(0.dp, 14.dp),
                                color = Color.Black.copy(alpha = if (isLightTheme) 0.14f else 0.40f)
                            )
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                BasicText(
                    text = title,
                    modifier = Modifier.padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 12.dp),
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    content = content
                )

                Row(
                    modifier = Modifier
                        .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!dismissText.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .elasticHold(pressedScale = 0.93f)
                                .clip(Capsule())
                                .background(
                                    if (isLightTheme) containerColor.copy(0.45f)
                                    else containerColor.copy(0.35f)
                                )
                                .cupertinoBorder(
                                    width = 0.5.dp,
                                    color = contentColor.copy(alpha = if (isLightTheme) 0.10f else 0.16f),
                                    shape = Capsule()
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onDismissRequest
                                )
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = dismissText,
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    if (!confirmText.isNullOrBlank() && onConfirm != null) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .elasticHold(pressedScale = 0.93f)
                                .clip(Capsule())
                                .background(accentColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onConfirm
                                )
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = confirmText,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PopoverMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false,
    val isSelected: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun LiquidActionPopover(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<PopoverMenuItem>,
    offset: IntOffset = IntOffset(0, 12)
) {
    if (!expanded) return
    val colors = CupertinoTheme.colors
    val backdrop = LocalOverlayBackdrop.current
    val shape = CupertinoTheme.shapes.banner
    val marginPx = with(LocalDensity.current) { 12.dp.roundToPx() }
    var popupWindowOffset by remember { mutableStateOf(IntOffset.Zero) }

    val positionProvider = remember(offset, marginPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val rawX = if (layoutDirection == LayoutDirection.Ltr) {
                    anchorBounds.right - popupContentSize.width + offset.x
                } else {
                    anchorBounds.left - offset.x
                }
                val rawY = anchorBounds.bottom + offset.y
                val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
                val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
                val clampedX = rawX.coerceIn(marginPx, maxX)
                val clampedY = if (rawY + popupContentSize.height + marginPx > windowSize.height) {
                    (anchorBounds.top - popupContentSize.height - offset.y).coerceIn(marginPx, maxY)
                } else {
                    rawY.coerceIn(marginPx, maxY)
                }
                val result = IntOffset(clampedX, clampedY)
                popupWindowOffset = result
                return result
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 190.dp, max = 250.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                        lens(10f.dp.toPx(), 20f.dp.toPx(), depthEffect = true)
                    },
                    highlight = {
                        Highlight(
                            width = 0.6.dp,
                            alpha = if (colors.isLight) 0.85f else 0.55f,
                            style = HighlightStyle.Default
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 18.dp,
                            offset = DpOffset(0.dp, 8.dp),
                            color = Color.Black.copy(alpha = if (colors.isLight) 0.14f else 0.36f)
                        )
                    },
                    onDrawBackdrop = { draw ->
                        translate(
                            left = -popupWindowOffset.x.toFloat(),
                            top = -popupWindowOffset.y.toFloat()
                        ) {
                            draw()
                        }
                    },
                    onDrawSurface = {
                        drawRect(colors.glassProminentSurface)
                    }
                )
        ) {
            items.forEachIndexed { index, item ->
                val tint = when {
                    item.isDestructive -> colors.destructiveRed
                    item.isSelected -> colors.accentBlue
                    else -> colors.label
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable {
                            onDismissRequest()
                            item.onClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CupertinoText(
                        text = item.label,
                        style = CupertinoTheme.typography.body.copy(
                            fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = tint,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.icon != null) {
                        CupertinoIcon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                if (index < items.lastIndex) {
                    CupertinoDivider()
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
