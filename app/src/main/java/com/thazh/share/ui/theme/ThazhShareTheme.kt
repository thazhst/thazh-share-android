package com.thazh.share.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.neoruaa.glasmio.core.GlassAppearance
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.defaultGlassColors
import io.github.neoruaa.glasmio.core.glasmioColorScheme

@Immutable
data class ThazhSemanticColors(
    val accentBlue: Color,
    val accentCyan: Color,
    val accentGreen: Color,
    val warningOrange: Color,
    val categoryVideo: Color,
    val categoryAudio: Color,
    val categoryDocument: Color,
)

private val LightSemanticColors = ThazhSemanticColors(
    accentBlue = Color(0xFF007AFF),
    accentCyan = Color(0xFF32ADE6),
    accentGreen = Color(0xFF34C759),
    warningOrange = Color(0xFFFF9500),
    categoryVideo = Color(0xFFAF52DE),
    categoryAudio = Color(0xFFFF2D55),
    categoryDocument = Color(0xFF5856D6),
)

private val DarkSemanticColors = ThazhSemanticColors(
    accentBlue = Color(0xFF0A84FF),
    accentCyan = Color(0xFF64D2FF),
    accentGreen = Color(0xFF30D158),
    warningOrange = Color(0xFFFF9F0A),
    categoryVideo = Color(0xFFBF5AF2),
    categoryAudio = Color(0xFFFF375F),
    categoryDocument = Color(0xFF5E5CE6),
)

val LocalThazhSemanticColors = staticCompositionLocalOf { LightSemanticColors }

object ThazhTheme {
    val semantic: ThazhSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalThazhSemanticColors.current
}

@Composable
fun ThazhShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceTransparency: Boolean = false,
    liquidGlassEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val semantic = if (darkTheme) DarkSemanticColors else LightSemanticColors
    val glassColors = defaultGlassColors(
        isDark = darkTheme,
        accent = semantic.accentBlue,
    )
    val colorScheme = glasmioColorScheme(
        isDark = darkTheme,
        accent = semantic.accentBlue,
    )
    val appearance = GlassAppearance(
        liquidGlassEnabled = liquidGlassEnabled,
        reduceTransparency = reduceTransparency,
        adaptiveColorScheme = false,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.isNavigationBarContrastEnforced = false
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalThazhSemanticColors provides semantic) {
        GlasmioTheme(
            isDark = darkTheme,
            colorScheme = colorScheme,
            colors = glassColors,
            appearance = appearance,
            content = content,
        )
    }
}
