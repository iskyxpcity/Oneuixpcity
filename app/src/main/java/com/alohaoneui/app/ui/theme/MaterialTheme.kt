package com.alohaoneui.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.component.material.alohaCornerShape
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Aloha OneUI Material 3 theme foundation aligned with Aloha's standard Material 3 shapes.
 * Aloha-specific settings and component behavior are integrated separately from this theme root.
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
    val dynamicColor = appSettings.keyColor == 0

    val colorScheme = rememberAlohaOneUIColorScheme(
        seedColor = if (dynamicColor) Color.Unspecified else Color(appSettings.keyColor),
        isDark = darkTheme,
        isAmoled = amoledMode,
        paletteStyle = appSettings.paletteStyle,
        colorSpec = appSettings.colorSpec,
    )
    val animatedColorScheme = colorScheme.animateAsState()

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Use the same Aloha CornerBasedShape factory as shared controls. This
    // keeps all seven shape styles and the live corner scale consistent even
    // for Material 3 components that do not receive an explicit shape.
    val materialShapes = Shapes(
        extraSmall = alohaCornerShape(4.dp),
        small = alohaCornerShape(8.dp),
        medium = alohaCornerShape(12.dp),
        large = alohaCornerShape(16.dp),
        extraLarge = alohaCornerShape(28.dp),
    )

    MaterialTheme(
        colorScheme = animatedColorScheme,
        shapes = materialShapes,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
    ) {
        // Keep the Material theme root free of a global backdrop layer.
        // The Liquid Glass switch owns its local track backdrop, avoiding
        // root-level render-layer initialization during app startup.
        content()
    }
}
