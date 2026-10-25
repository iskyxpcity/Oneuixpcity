package com.alohaoneui.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
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
            typography = Typography.withMaterialFontFamily(appSettings.materialFontFamily),
            content = {
                val colorMatrix = materialColorVisionMatrix(appSettings.materialColorVisionMode)
                if (colorMatrix == null) {
                    content()
                } else {
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .graphicsLayer { colorFilter = ColorFilter.colorMatrix(colorMatrix) }
                    ) { content() }
                }
            },
        )
    }
}


private fun materialColorVisionMatrix(mode: Int): ColorMatrix? = when (mode) {
    1 -> ColorMatrix(floatArrayOf(
        0.2126f, 0.7152f, 0.0722f, 0f, 0f,
        0.2126f, 0.7152f, 0.0722f, 0f, 0f,
        0.2126f, 0.7152f, 0.0722f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    2 -> ColorMatrix(floatArrayOf(
        0.567f, 0.433f, 0f, 0f, 0f,
        0.558f, 0.442f, 0f, 0f, 0f,
        0f, 0.242f, 0.758f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    3 -> ColorMatrix(floatArrayOf(
        0.625f, 0.375f, 0f, 0f, 0f,
        0.7f, 0.3f, 0f, 0f, 0f,
        0f, 0.3f, 0.7f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    4 -> ColorMatrix(floatArrayOf(
        0.95f, 0.05f, 0f, 0f, 0f,
        0f, 0.433f, 0.567f, 0f, 0f,
        0f, 0.475f, 0.525f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    else -> null
}
