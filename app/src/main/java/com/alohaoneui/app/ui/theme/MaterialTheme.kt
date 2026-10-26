package com.alohaoneui.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import com.materialkolor.DynamicMaterialExpressiveTheme

val LocalMaterialBorderWidth = staticCompositionLocalOf { 0.0f }
val LocalMaterialSwitchStyle = staticCompositionLocalOf { 0 }
val LocalMaterialSliderStyle = staticCompositionLocalOf { 0 }
val LocalMaterialShadowContainers = staticCompositionLocalOf { true }
val LocalMaterialShadowAppBars = staticCompositionLocalOf { true }
val LocalMaterialShadowButtons = staticCompositionLocalOf { true }
val LocalMaterialShadowFab = staticCompositionLocalOf { true }
val LocalMaterialShadowSwitches = staticCompositionLocalOf { true }
val LocalMaterialShadowSliders = staticCompositionLocalOf { true }

/**
 * Voyage's MaterialKolor expressive theme is the rendering engine. Aloha feature state remains
 * the source of persisted preferences; unsupported Aloha-only styling layers are not applied
 * over the MaterialKolor theme.
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
    val seedColor = if (appSettings.materialDynamicColorEnabled && appSettings.keyColor == 0) {
        (if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    } else if (appSettings.keyColor != 0) {
        Color(appSettings.keyColor)
    } else {
        Color(0xFF6750A4)
    }

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalMaterialBorderWidth provides appSettings.materialBorderWidth.coerceIn(0.0f, 1.5f),
        LocalMaterialSwitchStyle provides appSettings.materialSwitchStyle.coerceIn(0, 7),
        LocalMaterialSliderStyle provides appSettings.materialSliderStyle.coerceIn(0, 3),
        LocalMaterialShadowContainers provides appSettings.materialShadowContainers,
        LocalMaterialShadowAppBars provides appSettings.materialShadowAppBars,
        LocalMaterialShadowButtons provides appSettings.materialShadowButtons,
        LocalMaterialShadowFab provides appSettings.materialShadowFab,
        LocalMaterialShadowSwitches provides appSettings.materialShadowSwitches,
        LocalMaterialShadowSliders provides appSettings.materialShadowSliders,
        LocalDensity provides Density(
            density = density.density * appSettings.materialDisplayScale.coerceIn(0.85f, 1.25f) * if (appSettings.materialLargeTextEnabled) 1.12f else 1f,
            fontScale = density.fontScale * appSettings.materialFontScale.coerceIn(0.8f, 1.5f) *
                if (appSettings.materialLargeTextEnabled) 1.12f else 1f,
        ),
    ) {
        DynamicMaterialExpressiveTheme(
            seedColor = seedColor,
            motionScheme = if (appSettings.materialNavigationMotionMode == 1) MotionScheme.standard() else MotionScheme.expressive(),
            isDark = darkTheme,
            isAmoled = amoledMode,
            style = appSettings.paletteStyle,
            contrastLevel = appSettings.materialContrastLevel.toDouble(),
            specVersion = appSettings.colorSpec.effectiveFor(appSettings.paletteStyle),
            shapes = voyageShapes(appSettings.materialShapeStyle, appSettings.materialCornerScale),
            typography = Typography.withMaterialFontFamily(
                appSettings.materialFontFamily,
                appSettings.materialBoldTextEnabled,
            ),
            animate = true,
        ) {
            val colorMatrix = materialColorVisionMatrix(
                appSettings.materialColorVisionMode,
                appSettings.materialInvertColors,
            )
            if (colorMatrix == null) {
                content()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { colorFilter = ColorFilter.colorMatrix(colorMatrix) },
                ) {
                    content()
                }
            }
        }
    }
}

private fun materialColorVisionMatrix(mode: Int, invert: Boolean): ColorMatrix? {
    val grayscale = ColorMatrix(floatArrayOf(
        0.2126f, 0.7152f, 0.0722f, 0f, 0f,
        0.2126f, 0.7152f, 0.0722f, 0f, 0f,
        0.2126f, 0.7152f, 0.0722f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    val protanopia = ColorMatrix(floatArrayOf(
        0.567f, 0.433f, 0f, 0f, 0f,
        0.558f, 0.442f, 0f, 0f, 0f,
        0f, 0.242f, 0.758f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    val deuteranopia = ColorMatrix(floatArrayOf(
        0.625f, 0.375f, 0f, 0f, 0f,
        0.7f, 0.3f, 0f, 0f, 0f,
        0f, 0.3f, 0.7f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    val tritanopia = ColorMatrix(floatArrayOf(
        0.95f, 0.05f, 0f, 0f, 0f,
        0f, 0.433f, 0.567f, 0f, 0f,
        0f, 0.475f, 0.525f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    ))
    fun partial(target: ColorMatrix, amount: Float): ColorMatrix {
        val identity = ColorMatrix().values
        val values = target.values
        return ColorMatrix(FloatArray(20) { index ->
            identity[index] * (1f - amount) + values[index] * amount
        })
    }
    val vision = when (mode) {
        1 -> partial(protanopia, 0.5f)
        2 -> partial(deuteranopia, 0.5f)
        3 -> partial(tritanopia, 0.5f)
        4 -> protanopia
        5 -> deuteranopia
        6 -> tritanopia
        7 -> partial(grayscale, 0.5f)
        8 -> grayscale
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
    val a = inversion.values
    val b = vision.values
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


/**
 * Voyage's theme uses eight expressive shape tokens. Preserve the original token radii and
 * apply the user's corner scale. The rounded and cut treatments use their matching Compose
 * shape primitives.
 */
internal fun voyageShapes(style: Int, scale: Float): Shapes {
    val factor = scale.coerceIn(0.0f, 2.0f)
    fun shape(radius: Float): CornerBasedShape {
        val corner = (radius * factor).dp
        return when (style.coerceIn(0, 6)) {
            0 -> RoundedCornerShape(corner)
            1 -> CutCornerShape(corner)
            else -> VoyageExpressiveCornerShape(style.coerceIn(0, 6), corner)
        }
    }
    return Shapes(
        shape(8f),
        shape(12f),
        shape(20f),
        shape(28f),
        shape(36f),
        shape(32f),
        shape(42f),
        shape(52f),
    )
}
