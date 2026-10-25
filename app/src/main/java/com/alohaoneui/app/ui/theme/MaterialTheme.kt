package com.alohaoneui.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.alohaoneui.app.ui.theme.toMaterialContrastLevelOverride
import androidx.core.view.WindowInsetsControllerCompat

val LocalMaterialBorderWidth = staticCompositionLocalOf { 0.0f }

/**
 * Voyage-aligned Material 3 defaults for shapes and typography, while retaining Aloha's
 * expressive motion, palette preferences, AMOLED handling, and existing screen behavior.
 */
@Composable
fun MaterialAlohaOneUITheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark ||
        (appSettings.colorMode.isSystem && systemDarkTheme)
    val amoledMode = appSettings.colorMode.isAmoled
    val dynamicColor = appSettings.keyColor == 0

    val colorScheme = rememberAlohaOneUIColorScheme(
        seedColor = if (dynamicColor) Color.Unspecified else Color(appSettings.keyColor),
        isDark = darkTheme,
        isAmoled = amoledMode,
        paletteStyle = appSettings.paletteStyle,
        colorSpec = appSettings.colorSpec,
        contrastLevelOverride = appSettings.materialContrastMode.toMaterialContrastLevelOverride(),
    )
    val animatedColorScheme = colorScheme.animateAsState()

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalMaterialBorderWidth provides appSettings.materialBorderWidth.coerceIn(0.0f, 3.0f),
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * appSettings.materialFontScale.coerceIn(0.8f, 1.3f),
        )
    ) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            shapes = Shapes(
                small = RoundedCornerShape(4.dp * appSettings.materialCornerScale.coerceIn(0.5f, 1.5f)),
                medium = RoundedCornerShape(8.dp * appSettings.materialCornerScale.coerceIn(0.5f, 1.5f)),
                large = RoundedCornerShape(16.dp * appSettings.materialCornerScale.coerceIn(0.5f, 1.5f)),
            ),
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            content = content,
        )
    }
}
