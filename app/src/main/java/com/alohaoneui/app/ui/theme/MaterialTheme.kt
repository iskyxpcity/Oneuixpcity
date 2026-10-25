package com.alohaoneui.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
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
val LocalMaterialSwitchStyle = staticCompositionLocalOf { 0 }
val LocalMaterialSliderStyle = staticCompositionLocalOf { 3 }
val LocalMaterialShadowContainers = staticCompositionLocalOf { true }
val LocalMaterialShadowAppBars = staticCompositionLocalOf { true }
val LocalMaterialShadowButtons = staticCompositionLocalOf { true }
val LocalMaterialShadowFab = staticCompositionLocalOf { true }
val LocalMaterialShadowSwitches = staticCompositionLocalOf { true }
val LocalMaterialShadowSliders = staticCompositionLocalOf { true }

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
    val dynamicColor = appSettings.materialDynamicColorEnabled && appSettings.keyColor == 0
    val seedColor = when {
        dynamicColor -> Color.Unspecified
        appSettings.keyColor != 0 -> Color(appSettings.keyColor)
        else -> Color(0xFF6750A4)
    }

    val colorScheme = rememberAlohaOneUIColorScheme(
        seedColor = seedColor,
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
        LocalMaterialSwitchStyle provides appSettings.materialSwitchStyle.coerceIn(0, 7),
        LocalMaterialSliderStyle provides appSettings.materialSliderStyle.coerceIn(0, 3),
        LocalMaterialShadowContainers provides appSettings.materialShadowContainers,
        LocalMaterialShadowAppBars provides appSettings.materialShadowAppBars,
        LocalMaterialShadowButtons provides appSettings.materialShadowButtons,
        LocalMaterialShadowFab provides appSettings.materialShadowFab,
        LocalMaterialShadowSwitches provides appSettings.materialShadowSwitches,
        LocalMaterialShadowSliders provides appSettings.materialShadowSliders,
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * appSettings.materialFontScale.coerceIn(0.8f, 1.3f),
        )
    ) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            shapes = Shapes(
                small = materialShape(appSettings.materialShapeStyle, 4.dp * appSettings.materialCornerScale.coerceIn(0.5f, 1.5f)),
                medium = materialShape(appSettings.materialShapeStyle, 8.dp * appSettings.materialCornerScale.coerceIn(0.5f, 1.5f)),
                large = materialShape(appSettings.materialShapeStyle, 16.dp * appSettings.materialCornerScale.coerceIn(0.5f, 1.5f)),
            ),
            motionScheme = MotionScheme.expressive(),
            typography = Typography.withMaterialFontFamily(appSettings.materialFontFamily),
            content = {
                val colorMatrix = materialColorVisionMatrix(appSettings.materialColorVisionMode, appSettings.materialInvertColors)
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


private fun materialShape(style: Int, size: androidx.compose.ui.unit.Dp): CornerBasedShape = when (style.coerceIn(0, 6)) {
    0 -> CutCornerShape(size)
    1 -> CutCornerShape(topStart = size * 1.5f, topEnd = size, bottomEnd = size * 1.5f, bottomStart = size)
    2 -> RoundedCornerShape(size)
    3 -> RoundedCornerShape(percent = 50)
    4 -> RoundedCornerShape(size * 1.4f)
    5 -> RoundedCornerShape(size * 2.0f)
    else -> RoundedCornerShape(size * 1.2f)
}

private fun materialColorVisionMatrix(mode: Int, invert: Boolean): ColorMatrix? {
    val vision = when (mode) {
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
    if (!invert) return vision
    val inversion = ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    ))
    if (vision == null) return inversion
    return multiplyColorMatrices(inversion, vision)
}

private fun multiplyColorMatrices(first: ColorMatrix, second: ColorMatrix): ColorMatrix {
    val a = first.values
    val b = second.values
    val out = FloatArray(20)
    for (row in 0..3) {
        for (column in 0..3) {
            var value = 0f
            for (k in 0..3) value += a[row * 5 + k] * b[k * 5 + column]
            out[row * 5 + column] = value
        }
        var offset = a[row * 5 + 4]
        for (k in 0..3) offset += a[row * 5 + k] * b[k * 5 + 4]
        out[row * 5 + 4] = offset
    }
    return ColorMatrix(out)
}
