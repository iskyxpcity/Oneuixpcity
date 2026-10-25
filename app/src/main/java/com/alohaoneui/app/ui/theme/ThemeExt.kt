package com.alohaoneui.app.ui.theme

import android.app.UiModeManager
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.ktx.animateColorScheme

/**
 * Adapted from the KernelSU v3.3.1 theme layer under GPL-3.0.
 * Kept as a AlohaOneUI-local theme helper; no KernelSU functionality is included.
 */
fun ColorScheme.amoledBackground(amoled: Boolean): ColorScheme =
    if (!amoled) this
    else copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerHigh = Color.Black,
        surfaceContainerHighest = Color.Black,
    )

@Composable
fun rememberAlohaOneUIColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    contrastLevelOverride: Double? = null,
): ColorScheme {
    val context = LocalContext.current
    // Match Voyage's Material 3 theme: honor Android's system contrast preference on API 34+.
    // Keep neutral contrast on older Android versions or when the system service is unavailable.
    val contrastLevel = contrastLevelOverride ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(UiModeManager::class.java)
            ?.contrast
            ?.toDouble()
            ?.coerceIn(-1.0, 1.0)
            ?: 0.0
    } else {
        0.0
    }
    val effectiveSpec = colorSpec.effectiveFor(paletteStyle)

    // Voyage uses Android's native dynamic Material 3 scheme for its default
    // expressive palette when the system supplies one. Keep Aloha's custom-seed,
    // alternate-palette, spec-version, and AMOLED paths intact.
    val usePlatformDynamicScheme =
        seedColor == Color.Unspecified &&
            !isAmoled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            paletteStyle == PaletteStyle.Expressive &&
            effectiveSpec == ColorSpec.SpecVersion.SPEC_2025

    if (usePlatformDynamicScheme) {
        return (if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }).amoledBackground(isAmoled)
    }

    val seed = if (seedColor == Color.Unspecified) {
        (if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    } else {
        seedColor
    }
    return rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        isAmoled = isAmoled,
        style = paletteStyle,
        contrastLevel = contrastLevel,
        specVersion = effectiveSpec,
    ).amoledBackground(isAmoled)
}

@Composable
fun ColorScheme.animateAsState(): ColorScheme =
    animateColorScheme(colorScheme = this, label = "oneuixp_theme_color_animation")
