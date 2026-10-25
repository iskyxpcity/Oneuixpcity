package com.alohaoneui.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.alohaoneui.app.data.repository.SettingsRepository
import com.alohaoneui.app.data.repository.SettingsRepositoryImpl
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.ThemeId
import com.alohaoneui.app.ui.ThemeRegistry
import com.alohaoneui.app.ui.UiMode

enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    MONET_SYSTEM(3),
    MONET_LIGHT(4),
    MONET_DARK(5),
    DARK_AMOLED(6);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }

    val isSystem: Boolean get() = value == 0 || value == 3
    val isDark: Boolean get() = value == 2 || value == 5 || value == 6
    val isAmoled: Boolean get() = value == 6
    val isMonet: Boolean get() = value >= 3

    fun toNonMonetMode(): Int = when (this) {
        MONET_SYSTEM -> 0
        MONET_LIGHT -> 1
        MONET_DARK, DARK_AMOLED -> 2
        else -> value
    }

    fun toMonetMode(): Int = when (this) {
        SYSTEM -> 3
        LIGHT -> 4
        DARK -> 5
        else -> value
    }
}

data class AppSettings(
    val colorMode: ColorMode,
    val keyColor: Int,
    val paletteStyle: PaletteStyle,
    val colorSpec: ColorSpec.SpecVersion,
    val materialFontScale: Float = 1.0f,
    val materialContrastMode: Int = -1,
    val materialCornerScale: Float = 1.0f,
    val materialBorderWidth: Float = 0.0f,
    val materialFontFamily: String = "Default",
    val materialColorVisionMode: Int = 0,
    val materialDynamicColorEnabled: Boolean = true,
    val materialInvertColors: Boolean = false,
    val materialShapeStyle: Int = 2,
    val materialSwitchStyle: Int = 0,
    val materialSliderStyle: Int = 0,
    val materialShadowContainers: Boolean = true,
    val materialShadowAppBars: Boolean = true,
    val materialShadowButtons: Boolean = true,
    val materialShadowSwitches: Boolean = true,
    val materialShadowSliders: Boolean = true,
    val materialNavigationMotionMode: Int = 0,
)

fun Int.toMaterialContrastLevelOverride(): Double? = when (this) {
    -1 -> null
    0 -> -1.0
    1 -> 0.0
    2 -> 1.0
    else -> null
}

val PaletteStyle.supportsSpec2025: Boolean
    get() = this == PaletteStyle.TonalSpot ||
        this == PaletteStyle.Neutral ||
        this == PaletteStyle.Vibrant ||
        this == PaletteStyle.Expressive

fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && !style.supportsSpec2025) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

object ThemeController {
    fun getAppSettings(repo: SettingsRepository = SettingsRepositoryImpl()): AppSettings {
        val uiMode = repo.uiMode
        var colorModeValue = repo.themeMode

        if (uiMode == "miuix") {
            val miuixMonet = repo.miuixMonet
            val colorMode = ColorMode.fromValue(colorModeValue)
            colorModeValue = if (!miuixMonet && colorMode.isMonet) {
                colorMode.toNonMonetMode()
            } else if (miuixMonet && !colorMode.isMonet) {
                colorMode.toMonetMode()
            } else {
                colorModeValue
            }
        }

        val colorMode = ColorMode.fromValue(colorModeValue)
        val keyColor = repo.keyColor
        val paletteStyleStr = repo.colorStyle
        val paletteStyle = try {
            PaletteStyle.valueOf(paletteStyleStr)
        } catch (_: Exception) {
            PaletteStyle.TonalSpot
        }
        val colorSpecStr = repo.colorSpec
        val colorSpec = try {
            ColorSpec.SpecVersion.valueOf(colorSpecStr)
        } catch (_: Exception) {
            ColorSpec.SpecVersion.SPEC_2025
        }

        return AppSettings(colorMode, keyColor, paletteStyle, colorSpec, repo.materialFontScale.coerceIn(0.8f, 1.5f), repo.materialContrastMode.coerceIn(-1, 2), repo.materialCornerScale.coerceIn(0.5f, 1.5f), repo.materialBorderWidth.coerceIn(0.0f, 3.0f), repo.materialFontFamily, repo.materialColorVisionMode.coerceIn(0, 4), repo.materialDynamicColorEnabled, repo.materialInvertColors, repo.materialShapeStyle, repo.materialSwitchStyle, repo.materialSliderStyle, repo.materialShadowContainers, repo.materialShadowAppBars, repo.materialShadowButtons, repo.materialShadowSwitches, repo.materialShadowSliders, repo.materialNavigationMotionMode.coerceIn(0, 1))
    }
}

@Composable
fun AlohaOneUITheme(
    appSettings: AppSettings = ThemeController.getAppSettings(),
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit
) {
    // Theme selection is resolved through the registry; feature screens stay theme-agnostic.
    ThemeRegistry.definition(ThemeId.fromPersistedValue(uiMode.value))
        .adapter
        .Apply(appSettings = appSettings, content = content)
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    return when (LocalColorMode.current) {
        1, 4 -> false  // Force light mode
        2, 5, 6 -> true   // Force dark mode
        else -> isSystemInDarkTheme()  // Follow system (0 or default)
    }
}

val LocalColorMode = staticCompositionLocalOf { 0 }

val LocalEnableBlur = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }

val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { false }
