package com.alohaoneui.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.DynamicMaterialExpressiveTheme

/**
 * Material 3 theme entry point aligned with Voyage's MaterialKolor expressive theme engine.
 *
 * Keep Aloha preferences as the source of truth for mode, seed color and palette settings;
 * let MaterialKolor provide the actual expressive Material 3 color, shape and motion system
 * instead of layering a second, Aloha-specific color-scheme renderer over it.
 */
@Composable
fun MaterialAlohaOneUITheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark ||
        (appSettings.colorMode.isSystem && systemDarkTheme)
    val amoledMode = appSettings.colorMode.isAmoled
    val useSystemDynamicColor = appSettings.keyColor == 0

    // Voyage resolves the platform's wallpaper-derived color before handing the seed to
    // MaterialKolor. Preserve that behavior for Aloha's existing "system dynamic color" mode.
    val seedColor = if (useSystemDynamicColor) {
        (if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    } else {
        Color(appSettings.keyColor)
    }

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    DynamicMaterialExpressiveTheme(
        seedColor = seedColor,
        motionScheme = MotionScheme.expressive(),
        isDark = darkTheme,
        isAmoled = amoledMode,
        style = appSettings.paletteStyle,
        specVersion = appSettings.colorSpec.effectiveFor(appSettings.paletteStyle),
        animate = true,
        content = content,
    )
}
