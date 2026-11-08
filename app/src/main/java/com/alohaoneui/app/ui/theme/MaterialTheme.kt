package com.alohaoneui.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    // Feed the active Aloha shape style and corner scale into Material 3's
    // native shape slots so unwrapped Material controls inherit the same theme.
    val materialCornerScale = LocalMaterialCornerScale.current.coerceIn(0f, 2f)
    val materialShapeStyle = LocalMaterialShapeStyle.current
    // Material3 Shapes accepts CornerBasedShape, while AlohaPathShape is a
    // general Shape. Keep native slots type-safe; custom Aloha components
    // continue to render the full AlohaPathShape through alohaCornerShape().
    fun nativeMaterialCorner(radius: androidx.compose.ui.unit.Dp) =
        if (materialShapeStyle == 1) CutCornerShape(radius * materialCornerScale)
        else RoundedCornerShape(radius * materialCornerScale)

    val materialShapes = Shapes(
        extraSmall = nativeMaterialCorner(4.dp),
        small = nativeMaterialCorner(8.dp),
        medium = nativeMaterialCorner(12.dp),
        large = nativeMaterialCorner(16.dp),
        extraLarge = nativeMaterialCorner(28.dp),
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
