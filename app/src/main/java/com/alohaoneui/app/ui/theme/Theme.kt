package com.alohaoneui.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.alohaoneui.app.data.repository.SettingsRepository
import com.alohaoneui.app.data.repository.SettingsRepositoryImpl
import com.alohaoneui.app.ui.LocalUiMode
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
    val materialCornerScale: Float = 1.0f,
    val materialOutlineWidth: Float = 0.0f,
    val materialShadowContainers: Boolean = true,
    val materialShadowTopBars: Boolean = true,
    val materialShadowButtons: Boolean = true,
    val materialShadowFloatingButtons: Boolean = true,
    val materialShadowSwitches: Boolean = true,
    val materialShadowSliders: Boolean = true,
)

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

        return AppSettings(
            colorMode = colorMode,
            keyColor = keyColor,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            materialCornerScale = repo.materialCornerScale,
            materialOutlineWidth = repo.materialOutlineWidth,
            materialShadowContainers = repo.materialShadowContainers,
            materialShadowTopBars = repo.materialShadowTopBars,
            materialShadowButtons = repo.materialShadowButtons,
            materialShadowFloatingButtons = repo.materialShadowFloatingButtons,
            materialShadowSwitches = repo.materialShadowSwitches,
            materialShadowSliders = repo.materialShadowSliders,
        )
    }
}

@Composable
fun AlohaOneUITheme(
    appSettings: AppSettings = ThemeController.getAppSettings(),
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit
) {
    when (uiMode) {
        UiMode.Miuix -> MiuixAlohaOneUITheme(
            appSettings = appSettings,
            content = content
        )

        UiMode.Material -> MiuixAlohaOneUITheme(
            appSettings = appSettings,
            content = {
                CompositionLocalProvider(
                    LocalMaterialCornerScale provides appSettings.materialCornerScale,
                    LocalMaterialOutlineWidth provides appSettings.materialOutlineWidth,
                    LocalMaterialShadowContainers provides appSettings.materialShadowContainers,
                    LocalMaterialShadowTopBars provides appSettings.materialShadowTopBars,
                    LocalMaterialShadowButtons provides appSettings.materialShadowButtons,
                    LocalMaterialShadowFloatingButtons provides appSettings.materialShadowFloatingButtons,
                    LocalMaterialShadowSwitches provides appSettings.materialShadowSwitches,
                    LocalMaterialShadowSliders provides appSettings.materialShadowSliders,
                ) {
                    MaterialAlohaOneUITheme(
                        appSettings = appSettings,
                        content = content
                    )
                }
            }
        )
    }
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
val LocalMaterialCornerScale = staticCompositionLocalOf { 1.0f }
val LocalMaterialOutlineWidth = staticCompositionLocalOf { 0.0f }
val LocalMaterialShadowContainers = staticCompositionLocalOf { true }
val LocalMaterialShadowTopBars = staticCompositionLocalOf { true }
val LocalMaterialShadowButtons = staticCompositionLocalOf { true }
val LocalMaterialShadowFloatingButtons = staticCompositionLocalOf { true }
val LocalMaterialShadowSwitches = staticCompositionLocalOf { true }
val LocalMaterialShadowSliders = staticCompositionLocalOf { true }
