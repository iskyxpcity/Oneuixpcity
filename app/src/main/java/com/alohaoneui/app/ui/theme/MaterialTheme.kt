package com.alohaoneui.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.alohaoneui.app.ui.component.material.LocalMaterialSwitchBackdrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Aloha OneUI Material 3 theme foundation aligned with Voyage's standard Material 3 shapes.
 * Voyage-specific settings and component behavior are integrated separately from this theme root.
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

    MaterialTheme(
        colorScheme = animatedColorScheme,
        shapes = Shapes(),
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
    ) {
        // Capture the Material screen behind transient Liquid Glass controls.
        // This is a rendering backdrop only; it does not import Miuix theme
        // components, colors, or switch behavior into the Material theme.
        val materialSwitchBackdrop = rememberLayerBackdrop()
        CompositionLocalProvider(
            LocalMaterialSwitchBackdrop provides materialSwitchBackdrop,
        ) {
            Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .layerBackdrop(materialSwitchBackdrop),
            ) {
                content()
            }
        }
    }
}
