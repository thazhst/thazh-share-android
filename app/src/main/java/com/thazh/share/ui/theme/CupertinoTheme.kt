package com.thazh.share.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle

@Immutable
data class CupertinoColors(
    val isLight: Boolean,
    val accentBlue: Color,
    val accentCyan: Color,
    val accentGreen: Color,
    val warningOrange: Color,
    val destructiveRed: Color,
    val systemBackground: Color,
    val secondaryBackground: Color,
    val glassSurface: Color,
    val glassProminentSurface: Color,
    val fillTertiary: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val dimOverlay: Color
)

private val LightCupertinoColors = CupertinoColors(
    isLight = true,
    accentBlue = Color(0xFF007AFF),
    accentCyan = Color(0xFF32ADE6),
    accentGreen = Color(0xFF34C759),
    warningOrange = Color(0xFFFF9500),
    destructiveRed = Color(0xFFFF3B30),
    systemBackground = Color(0xFFF2F4F8),
    secondaryBackground = Color(0xFFFFFFFF),
    glassSurface = Color(0xFFFFFFFF).copy(alpha = 0.58f),
    glassProminentSurface = Color(0xFFFAFAFC).copy(alpha = 0.72f),
    fillTertiary = Color(0xFF767680).copy(alpha = 0.12f),
    label = Color(0xFF0B0D12),
    secondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.68f),
    tertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.40f),
    separator = Color(0xFF3C3C43).copy(alpha = 0.16f),
    dimOverlay = Color(0xFF1D2433).copy(alpha = 0.24f)
)

private val DarkCupertinoColors = CupertinoColors(
    isLight = false,
    accentBlue = Color(0xFF0A84FF),
    accentCyan = Color(0xFF64D2FF),
    accentGreen = Color(0xFF30D158),
    warningOrange = Color(0xFFFF9F0A),
    destructiveRed = Color(0xFFFF453A),
    systemBackground = Color(0xFF090C12),
    secondaryBackground = Color(0xFF1C1C1E),
    glassSurface = Color(0xFF1C2029).copy(alpha = 0.54f),
    glassProminentSurface = Color(0xFF1E232E).copy(alpha = 0.68f),
    fillTertiary = Color(0xFF767680).copy(alpha = 0.24f),
    label = Color(0xFFF5F7FA),
    secondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.65f),
    tertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.36f),
    separator = Color(0xFF545458).copy(alpha = 0.45f),
    dimOverlay = Color(0xFF05070B).copy(alpha = 0.56f)
)

@Immutable
data class CupertinoTypography(
    val largeTitle: TextStyle = TextStyle(
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    val title1: TextStyle = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    val title2: TextStyle = TextStyle(
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    val headline: TextStyle = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    val body: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    ),
    val subheadline: TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal
    ),
    val footnote: TextStyle = TextStyle(
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal
    ),
    val caption: TextStyle = TextStyle(
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium
    )
)

@Immutable
data class CupertinoShapes(
    val iconBadge: Shape = RoundedRectangle(12.dp, RoundedCornerStyle.Continuous),
    val input: Shape = RoundedRectangle(14.dp, RoundedCornerStyle.Continuous),
    val card: Shape = RoundedRectangle(24.dp, RoundedCornerStyle.Continuous),
    val banner: Shape = RoundedRectangle(20.dp, RoundedCornerStyle.Continuous),
    val sheet: Shape = RoundedRectangle(28.dp, RoundedCornerStyle.Continuous),
    val dialog: Shape = RoundedRectangle(36.dp, RoundedCornerStyle.Continuous),
    val pill: Shape = Capsule()
)

val LocalCupertinoColors = staticCompositionLocalOf { LightCupertinoColors }
val LocalCupertinoTypography = staticCompositionLocalOf { CupertinoTypography() }
val LocalCupertinoShapes = staticCompositionLocalOf { CupertinoShapes() }
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }
val LocalOverlayBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

object CupertinoTheme {
    val colors: CupertinoColors
        @Composable get() = LocalCupertinoColors.current

    val typography: CupertinoTypography
        @Composable get() = LocalCupertinoTypography.current

    val shapes: CupertinoShapes
        @Composable get() = LocalCupertinoShapes.current
}

@Composable
fun ThazhShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkCupertinoColors else LightCupertinoColors
    val typography = CupertinoTypography()
    val shapes = CupertinoShapes()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalCupertinoColors provides colors,
        LocalCupertinoTypography provides typography,
        LocalCupertinoShapes provides shapes,
        content = content
    )
}
