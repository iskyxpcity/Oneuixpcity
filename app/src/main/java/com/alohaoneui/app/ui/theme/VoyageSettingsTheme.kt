package com.alohaoneui.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.alohaoneui.app.ui.screen.settings.SettingsUiState
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * Voyage settings are rendered with Voyage's MaterialKolor expressive theme engine.
 *
 * This is deliberately a local theme boundary: the host screen's Miuix/Material choices and
 * Aloha-specific typography, shape and display wrappers do not style these imported pages.
 */
@Composable
fun VoyageSettingsTheme(
    uiState: SettingsUiState,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val systemDensity = context.resources.displayMetrics.density
    val systemDark = isSystemInDarkTheme()
    val colorMode = ColorMode.fromValue(uiState.themeMode)
    val dark = colorMode.isDark || (colorMode.isSystem && systemDark)
    val amoled = colorMode.isAmoled
    val paletteStyle = runCatching { PaletteStyle.valueOf(uiState.colorStyle) }
        .getOrDefault(PaletteStyle.Expressive)
    val colorSpec = runCatching { ColorSpec.SpecVersion.valueOf(uiState.colorSpec) }
        .getOrDefault(ColorSpec.SpecVersion.SPEC_2025)
    val seed = when {
        uiState.keyColor != 0 -> Color(uiState.keyColor)
        uiState.materialDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
        else -> Color(0xFF4A658D)
    }
    val fontScale = uiState.materialFontScale.coerceIn(0.8f, 1.5f) *
        if (uiState.materialLargeTextEnabled) 1.12f else 1f
    val displayScale = uiState.materialDisplayScale.coerceIn(0.85f, 1.25f)
    val selectedDensity = Density(
        density = systemDensity * displayScale,
        fontScale = configuration.fontScale * fontScale,
    )

    CompositionLocalProvider(LocalDensity provides selectedDensity) {
        DynamicMaterialExpressiveTheme(
            seedColor = seed,
            motionScheme = if (uiState.materialNavigationMotionMode == 1) {
                MotionScheme.standard()
            } else {
                MotionScheme.expressive()
            },
            isDark = dark,
            isAmoled = amoled,
            style = paletteStyle,
            contrastLevel = uiState.materialContrastLevel.toDouble(),
            specVersion = colorSpec,
            shapes = voyageShapes(uiState.materialShapeStyle, uiState.materialCornerScale),
            typography = Typography.withMaterialFontFamily(
                uiState.materialFontFamily,
                uiState.materialBoldTextEnabled,
            ),
            animate = true,
            content = content,
        )
    }
}
