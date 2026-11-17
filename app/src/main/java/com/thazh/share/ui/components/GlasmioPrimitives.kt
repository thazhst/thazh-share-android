package com.thazh.share.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.thazh.share.R
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.icons.SfIcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Glasmio typography primitive built directly on Compose Foundation [BasicText].
 * Automatically resolves colors and text styles from [GlasmioTheme] without Material 3 dependencies.
 */
@Composable
fun GlasmioText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = GlasmioTheme.textStyles.body,
) {
    val resolvedColor = color.takeOrElse {
        style.color.takeOrElse { GlasmioTheme.colors.content }
    }
    val mergedStyle = style.merge(
        TextStyle(
            color = resolvedColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
        ),
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
    )
}

/**
 * iOS / Glasmio style spoke activity spinner rendered via Compose Foundation [Canvas].
 */
@Composable
fun GlasmioActivityIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = GlasmioTheme.colors.accent,
    strokeWidth: Dp = 2.2.dp,
) {
    val transition = rememberInfiniteTransition(label = "glasmio_activity_spinner")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glasmio_spinner_phase",
    )

    Canvas(modifier = modifier.size(size)) {
        val spokeCount = 8
        val radius = this.size.minDimension / 2f
        val innerRadius = radius * 0.46f
        val outerRadius = radius * 0.90f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val strokePx = strokeWidth.toPx()

        for (i in 0 until spokeCount) {
            val angleRad = (i * 45.0) * (PI / 180.0)
            val distance = ((i - phase.toInt() + spokeCount) % spokeCount).toFloat()
            val spokeAlpha = (1f - (distance / spokeCount) * 0.78f).coerceIn(0.18f, 1f)
            val start = Offset(
                x = center.x + (cos(angleRad) * innerRadius).toFloat(),
                y = center.y + (sin(angleRad) * innerRadius).toFloat(),
            )
            val end = Offset(
                x = center.x + (cos(angleRad) * outerRadius).toFloat(),
                y = center.y + (sin(angleRad) * outerRadius).toFloat(),
            )
            drawLine(
                color = color.copy(alpha = spokeAlpha),
                start = start,
                end = end,
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Liquid Glass continuous capsule progress bar.
 */
@Composable
fun GlasmioLinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = GlasmioTheme.colors.accent,
    trackColor: Color = color.copy(alpha = 0.18f),
    height: Dp = 6.dp,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glasmio_linear_progress",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(ContinuousCapsule)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = animatedProgress)
                .clip(ContinuousCapsule)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.85f),
                            color,
                        ),
                    ),
                ),
        )
    }
}

/**
 * Apple HIG / iOS 18 Settings-style tinted squircle icon badge.
 * Gives every setting row, card header, and action item a cohesive Liquid Glass depth.
 */
@Composable
fun ThazhSquircleIconBox(
    symbol: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 34.dp,
    iconSize: Dp = 18.dp,
    cornerRadius: Dp = 10.dp,
    contentDescription: String? = null,
) {
    val isDark = GlasmioTheme.colors.isDark
    val shape = ContinuousRoundedRectangle(cornerRadius)
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isDark) 0.28f else 0.18f),
                        accentColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                    ),
                ),
            )
            .border(
                width = 0.8.dp,
                color = accentColor.copy(alpha = if (isDark) 0.36f else 0.22f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SfIcon(
            systemName = symbol,
            contentDescription = contentDescription,
            tint = accentColor,
            size = iconSize,
        )
    }
}

/**
 * Renders the Apple Human Interface Guidelines (HIG) App Icon in a continuous squircle
 * with multi-layered specular rim lighting and ambient elevation shadow.
 */
@Composable
fun ThazhAppIconBadge(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    cornerRadius: Dp = (size.value * 0.225f).dp,
    contentDescription: String? = null,
) {
    val shape = ContinuousRoundedRectangle(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = (size.value * 0.14f).dp,
                shape = shape,
                ambientColor = Color(0xFF0A84FF).copy(alpha = 0.35f),
                spotColor = Color(0xFF00D4FF).copy(alpha = 0.45f),
            )
            .clip(shape)
            .background(Color(0xFF001F55))
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color(0xFF5AC8FA).copy(alpha = 0.35f),
                        Color(0xFF0A84FF).copy(alpha = 0.20f),
                    ),
                ),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_hig_art),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
