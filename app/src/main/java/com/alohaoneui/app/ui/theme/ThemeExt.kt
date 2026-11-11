package com.alohaoneui.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

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
): ColorScheme {
    val context = LocalContext.current
    val seed = remember(seedColor, isDark, context) {
        if (seedColor == Color.Unspecified) {
            (if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
        } else {
            seedColor
        }
    }
    return rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        isAmoled = isAmoled,
        style = paletteStyle,
        specVersion = colorSpec.effectiveFor(paletteStyle),
    ).amoledBackground(isAmoled)
}

@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    val targetScheme = this
    val progress = remember { Animatable(1f) }
    var startScheme by remember { mutableStateOf(targetScheme) }
    var endScheme by remember { mutableStateOf(targetScheme) }

    LaunchedEffect(targetScheme) {
        if (targetScheme == endScheme) return@LaunchedEffect

        // Continue from the currently displayed palette if another mode is
        // selected before the previous color transition has finished.
        startScheme = lerpColorScheme(startScheme, endScheme, progress.value)
        endScheme = targetScheme
        progress.snapTo(0f)
        progress.animateTo(1f, spring())
    }

    return lerpColorScheme(startScheme, endScheme, progress.value)
}

private fun lerpColorScheme(
    start: ColorScheme,
    end: ColorScheme,
    fraction: Float,
): ColorScheme = ColorScheme(
    primary = lerp(start.primary, end.primary, fraction),
    onPrimary = lerp(start.onPrimary, end.onPrimary, fraction),
    primaryContainer = lerp(start.primaryContainer, end.primaryContainer, fraction),
    onPrimaryContainer = lerp(start.onPrimaryContainer, end.onPrimaryContainer, fraction),
    inversePrimary = lerp(start.inversePrimary, end.inversePrimary, fraction),
    secondary = lerp(start.secondary, end.secondary, fraction),
    onSecondary = lerp(start.onSecondary, end.onSecondary, fraction),
    secondaryContainer = lerp(start.secondaryContainer, end.secondaryContainer, fraction),
    onSecondaryContainer = lerp(start.onSecondaryContainer, end.onSecondaryContainer, fraction),
    tertiary = lerp(start.tertiary, end.tertiary, fraction),
    onTertiary = lerp(start.onTertiary, end.onTertiary, fraction),
    tertiaryContainer = lerp(start.tertiaryContainer, end.tertiaryContainer, fraction),
    onTertiaryContainer = lerp(start.onTertiaryContainer, end.onTertiaryContainer, fraction),
    background = lerp(start.background, end.background, fraction),
    onBackground = lerp(start.onBackground, end.onBackground, fraction),
    surface = lerp(start.surface, end.surface, fraction),
    onSurface = lerp(start.onSurface, end.onSurface, fraction),
    surfaceVariant = lerp(start.surfaceVariant, end.surfaceVariant, fraction),
    onSurfaceVariant = lerp(start.onSurfaceVariant, end.onSurfaceVariant, fraction),
    surfaceTint = lerp(start.surfaceTint, end.surfaceTint, fraction),
    inverseSurface = lerp(start.inverseSurface, end.inverseSurface, fraction),
    inverseOnSurface = lerp(start.inverseOnSurface, end.inverseOnSurface, fraction),
    error = lerp(start.error, end.error, fraction),
    onError = lerp(start.onError, end.onError, fraction),
    errorContainer = lerp(start.errorContainer, end.errorContainer, fraction),
    onErrorContainer = lerp(start.onErrorContainer, end.onErrorContainer, fraction),
    outline = lerp(start.outline, end.outline, fraction),
    outlineVariant = lerp(start.outlineVariant, end.outlineVariant, fraction),
    scrim = lerp(start.scrim, end.scrim, fraction),
    surfaceBright = lerp(start.surfaceBright, end.surfaceBright, fraction),
    surfaceDim = lerp(start.surfaceDim, end.surfaceDim, fraction),
    surfaceContainer = lerp(start.surfaceContainer, end.surfaceContainer, fraction),
    surfaceContainerHigh = lerp(start.surfaceContainerHigh, end.surfaceContainerHigh, fraction),
    surfaceContainerHighest = lerp(start.surfaceContainerHighest, end.surfaceContainerHighest, fraction),
    surfaceContainerLow = lerp(start.surfaceContainerLow, end.surfaceContainerLow, fraction),
    surfaceContainerLowest = lerp(start.surfaceContainerLowest, end.surfaceContainerLowest, fraction),
    primaryFixed = lerp(start.primaryFixed, end.primaryFixed, fraction),
    primaryFixedDim = lerp(start.primaryFixedDim, end.primaryFixedDim, fraction),
    onPrimaryFixed = lerp(start.onPrimaryFixed, end.onPrimaryFixed, fraction),
    onPrimaryFixedVariant = lerp(start.onPrimaryFixedVariant, end.onPrimaryFixedVariant, fraction),
    secondaryFixed = lerp(start.secondaryFixed, end.secondaryFixed, fraction),
    secondaryFixedDim = lerp(start.secondaryFixedDim, end.secondaryFixedDim, fraction),
    onSecondaryFixed = lerp(start.onSecondaryFixed, end.onSecondaryFixed, fraction),
    onSecondaryFixedVariant = lerp(start.onSecondaryFixedVariant, end.onSecondaryFixedVariant, fraction),
    tertiaryFixed = lerp(start.tertiaryFixed, end.tertiaryFixed, fraction),
    tertiaryFixedDim = lerp(start.tertiaryFixedDim, end.tertiaryFixedDim, fraction),
    onTertiaryFixed = lerp(start.onTertiaryFixed, end.onTertiaryFixed, fraction),
    onTertiaryFixedVariant = lerp(start.onTertiaryFixedVariant, end.onTertiaryFixedVariant, fraction),
)
