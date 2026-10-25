package com.alohaoneui.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.alohaoneui.app.data.repository.SettingsRepository
import com.alohaoneui.app.data.repository.SettingsRepositoryImpl
import com.alohaoneui.app.ui.screen.settings.SettingsUiState
import com.alohaoneui.app.ui.theme.ColorMode

class SettingsViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val checkUpdate = repo.checkUpdate
            val themeMode = repo.themeMode
            val miuixMonet = repo.miuixMonet
            val keyColor = repo.keyColor
            val enablePredictiveBack = repo.enablePredictiveBack
            val enableBlur = repo.enableBlur
            val enableFloatingBottomBar = repo.enableFloatingBottomBar
            val enableFloatingBottomBarBlur = repo.enableFloatingBottomBarBlur
            val pageScale = repo.pageScale
            val enableSwipeBack = repo.enableSwipeBack
            val pagerGestureMode = repo.pagerGestureMode
            val colorStyle = repo.colorStyle
            val colorSpec = repo.colorSpec
            val materialFontScale = repo.materialFontScale
            val materialContrastMode = repo.materialContrastMode
            val materialCornerScale = repo.materialCornerScale
            val materialColorSchemes = repo.materialColorSchemes
            val materialBorderWidth = repo.materialBorderWidth
            val materialFontFamily = repo.materialFontFamily
            val materialColorVisionMode = repo.materialColorVisionMode
            val materialNavigationMotionMode = repo.materialNavigationMotionMode
            val materialDynamicColorEnabled = repo.materialDynamicColorEnabled
            val materialInvertColors = repo.materialInvertColors
            val materialShapeStyle = repo.materialShapeStyle
            val materialSwitchStyle = repo.materialSwitchStyle
            val materialSliderStyle = repo.materialSliderStyle
            val materialLaunchFollowsTheme = repo.materialLaunchFollowsTheme
            val materialShadowContainers = repo.materialShadowContainers
            val materialShadowAppBars = repo.materialShadowAppBars
            val materialShadowButtons = repo.materialShadowButtons
            val materialShadowSwitches = repo.materialShadowSwitches
            val materialShadowSliders = repo.materialShadowSliders
            val uiMode = repo.uiMode

            _uiState.update {
                it.copy(
                    uiMode = uiMode,
                    checkUpdate = checkUpdate,
                    themeMode = themeMode,
                    miuixMonet = miuixMonet,
                    keyColor = keyColor,
                    enablePredictiveBack = enablePredictiveBack,
                    enableBlur = enableBlur,
                    enableFloatingBottomBar = enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                    pageScale = pageScale,
                    enableSwipeBack = enableSwipeBack,
                    pagerGestureMode = pagerGestureMode,
                    colorStyle = colorStyle,
                    colorSpec = colorSpec,
                    materialFontScale = materialFontScale,
                    materialContrastMode = materialContrastMode,
                    materialCornerScale = materialCornerScale,
                    materialColorSchemes = materialColorSchemes,
                    materialBorderWidth = materialBorderWidth,
                    materialFontFamily = materialFontFamily,
                    materialColorVisionMode = materialColorVisionMode,
                    materialNavigationMotionMode = materialNavigationMotionMode,
                    materialDynamicColorEnabled = materialDynamicColorEnabled,
                    materialInvertColors = materialInvertColors,
                    materialShapeStyle = materialShapeStyle,
                    materialSwitchStyle = materialSwitchStyle,
                    materialSliderStyle = materialSliderStyle,
                    materialLaunchFollowsTheme = materialLaunchFollowsTheme,
                    materialShadowContainers = materialShadowContainers,
                    materialShadowAppBars = materialShadowAppBars,
                    materialShadowButtons = materialShadowButtons,
                    materialShadowSwitches = materialShadowSwitches,
                    materialShadowSliders = materialShadowSliders,
                )
            }
        }
    }

    fun setCheckUpdate(enabled: Boolean) {
        repo.checkUpdate = enabled
        _uiState.update { it.copy(checkUpdate = enabled) }
    }

    fun setUiMode(mode: String) {
        val oldMode = repo.uiMode
        val currentThemeMode = repo.themeMode

        val newThemeMode = when (oldMode) {
            "material" if mode == "miuix" -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                val baseMode = if (colorMode == ColorMode.DARK_AMOLED) 2 else currentThemeMode
                if (repo.miuixMonet && !colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toMonetMode()
                } else if (!repo.miuixMonet && colorMode.isMonet) {
                    ColorMode.fromValue(baseMode).toNonMonetMode()
                } else baseMode
            }

            "miuix" if mode == "material" -> {
                val colorMode = ColorMode.fromValue(currentThemeMode)
                if (colorMode.isMonet) {
                    colorMode.toNonMonetMode()
                } else currentThemeMode
            }

            else -> currentThemeMode
        }

        repo.uiMode = mode
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(uiMode = mode, themeMode = newThemeMode) }
    }

    fun setThemeMode(mode: Int) {
        val currentUiMode = repo.uiMode
        val effectiveMode = if (currentUiMode == "miuix" && _uiState.value.miuixMonet) {
            mode + 3
        } else {
            mode
        }
        repo.themeMode = effectiveMode
        _uiState.update { it.copy(themeMode = effectiveMode) }
    }

    fun setColorMode(mode: ColorMode) {
        repo.themeMode = mode.value
        _uiState.update { it.copy(themeMode = mode.value) }
    }

    fun setMiuixMonet(enabled: Boolean) {
        val currentThemeMode = repo.themeMode
        val colorMode = ColorMode.fromValue(currentThemeMode)
        val newThemeMode = if (enabled) {
            if (!colorMode.isMonet) colorMode.toMonetMode() else currentThemeMode
        } else {
            if (colorMode.isMonet) colorMode.toNonMonetMode() else currentThemeMode
        }
        repo.miuixMonet = enabled
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(miuixMonet = enabled, themeMode = newThemeMode) }
    }

    fun setKeyColor(color: Int) {
        repo.keyColor = color
        repo.materialDynamicColorEnabled = color == 0
        _uiState.update { it.copy(keyColor = color, materialDynamicColorEnabled = repo.materialDynamicColorEnabled) }
    }

    fun setColorStyle(style: String) {
        repo.colorStyle = style
        _uiState.update { it.copy(colorStyle = style) }
    }

    fun setColorSpec(spec: String) {
        repo.colorSpec = spec
        _uiState.update { it.copy(colorSpec = spec) }
    }

    fun setMaterialFontScale(scale: Float) {
        val safeScale = scale.coerceIn(0.8f, 1.5f)
        repo.materialFontScale = safeScale
        _uiState.update { it.copy(materialFontScale = safeScale) }
    }

    fun setMaterialContrastMode(mode: Int) {
        val safeMode = mode.coerceIn(-1, 2)
        repo.materialContrastMode = safeMode
        _uiState.update { it.copy(materialContrastMode = safeMode) }
    }

    fun setMaterialCornerScale(scale: Float) {
        val safeScale = scale.coerceIn(0.5f, 1.5f)
        repo.materialCornerScale = safeScale
        _uiState.update { it.copy(materialCornerScale = safeScale) }
    }

    fun saveMaterialColorScheme(name: String) {
        val safeName = name.trim().take(40)
        if (safeName.isBlank()) return
        val scheme = com.alohaoneui.app.data.repository.MaterialColorScheme(
            name = safeName,
            keyColor = repo.keyColor,
            colorStyle = repo.colorStyle,
            colorSpec = repo.colorSpec,
            contrastMode = repo.materialContrastMode,
        )
        val updated = repo.materialColorSchemes.filterNot { it.name.equals(safeName, ignoreCase = true) } + scheme
        repo.materialColorSchemes = updated
        _uiState.update { it.copy(materialColorSchemes = updated) }
    }

    fun applyMaterialColorScheme(scheme: com.alohaoneui.app.data.repository.MaterialColorScheme) {
        repo.keyColor = scheme.keyColor
        repo.colorStyle = runCatching { com.materialkolor.PaletteStyle.valueOf(scheme.colorStyle).name }
            .getOrDefault(com.materialkolor.PaletteStyle.TonalSpot.name)
        repo.colorSpec = runCatching { com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.valueOf(scheme.colorSpec).name }
            .getOrDefault(com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.SPEC_2025.name)
        repo.materialContrastMode = scheme.contrastMode.coerceIn(-1, 2)
        _uiState.update {
            it.copy(
                keyColor = repo.keyColor,
                colorStyle = repo.colorStyle,
                colorSpec = repo.colorSpec,
                materialContrastMode = repo.materialContrastMode,
            )
        }
    }

    fun deleteMaterialColorScheme(name: String) {
        val updated = repo.materialColorSchemes.filterNot { it.name == name }
        repo.materialColorSchemes = updated
        _uiState.update { it.copy(materialColorSchemes = updated) }
    }

    fun setMaterialBorderWidth(width: Float) {
        val safeWidth = width.coerceIn(0.0f, 3.0f)
        repo.materialBorderWidth = safeWidth
        _uiState.update { it.copy(materialBorderWidth = safeWidth) }
    }

    fun setMaterialFontFamily(family: String) {
        val safeFamily = family.takeIf { it in setOf("Default", "SansSerif", "Serif", "Monospace", "Cursive") } ?: "Default"
        repo.materialFontFamily = safeFamily
        _uiState.update { it.copy(materialFontFamily = safeFamily) }
    }

    fun setMaterialColorVisionMode(mode: Int) {
        val safeMode = mode.coerceIn(0, 8)
        repo.materialColorVisionMode = safeMode
        _uiState.update { it.copy(materialColorVisionMode = safeMode) }
    }

    fun setMaterialNavigationMotionMode(mode: Int) {
        val safeMode = mode.coerceIn(0, 1)
        repo.materialNavigationMotionMode = safeMode
        _uiState.update { it.copy(materialNavigationMotionMode = safeMode) }
    }

    fun setMaterialDynamicColorEnabled(enabled: Boolean) {
        repo.materialDynamicColorEnabled = enabled
        if (enabled) repo.keyColor = 0 else if (repo.keyColor == 0) repo.keyColor = 0xFF6750A4.toInt()
        _uiState.update { it.copy(materialDynamicColorEnabled = enabled, keyColor = repo.keyColor) }
    }

    fun setMaterialInvertColors(enabled: Boolean) {
        repo.materialInvertColors = enabled
        _uiState.update { it.copy(materialInvertColors = enabled) }
    }

    fun setMaterialShapeStyle(style: Int) {
        val safeStyle = style.coerceIn(0, 6)
        repo.materialShapeStyle = safeStyle
        _uiState.update { it.copy(materialShapeStyle = safeStyle) }
    }

    fun setMaterialSwitchStyle(style: Int) {
        val safeStyle = style.coerceIn(0, 7)
        repo.materialSwitchStyle = safeStyle
        _uiState.update { it.copy(materialSwitchStyle = safeStyle) }
    }

    fun setMaterialSliderStyle(style: Int) {
        val safeStyle = style.coerceIn(0, 3)
        repo.materialSliderStyle = safeStyle
        _uiState.update { it.copy(materialSliderStyle = safeStyle) }
    }

    fun setMaterialLaunchFollowsTheme(enabled: Boolean) {
        repo.materialLaunchFollowsTheme = enabled
        _uiState.update { it.copy(materialLaunchFollowsTheme = enabled) }
    }

    fun setMaterialShadowContainers(enabled: Boolean) {
        repo.materialShadowContainers = enabled
        _uiState.update { it.copy(materialShadowContainers = enabled) }
    }

    fun setMaterialShadowAppBars(enabled: Boolean) {
        repo.materialShadowAppBars = enabled
        _uiState.update { it.copy(materialShadowAppBars = enabled) }
    }

    fun setMaterialShadowButtons(enabled: Boolean) {
        repo.materialShadowButtons = enabled
        _uiState.update { it.copy(materialShadowButtons = enabled) }
    }

    fun setMaterialShadowSwitches(enabled: Boolean) {
        repo.materialShadowSwitches = enabled
        _uiState.update { it.copy(materialShadowSwitches = enabled) }
    }

    fun setMaterialShadowSliders(enabled: Boolean) {
        repo.materialShadowSliders = enabled
        _uiState.update { it.copy(materialShadowSliders = enabled) }
    }


    fun setEnablePredictiveBack(enabled: Boolean) {
        repo.enablePredictiveBack = enabled
        _uiState.update { it.copy(enablePredictiveBack = enabled) }
    }

    fun setEnableSwipeBack(enabled: Boolean) {
        repo.enableSwipeBack = enabled
        _uiState.update { it.copy(enableSwipeBack = enabled) }
    }

    fun setPagerGestureMode(mode: Int) {
        val safeMode = mode.coerceIn(0, 2)
        repo.pagerGestureMode = safeMode
        _uiState.update { it.copy(pagerGestureMode = safeMode) }
    }

    fun setEnableBlur(enabled: Boolean) {
        repo.enableBlur = enabled
        _uiState.update { it.copy(enableBlur = enabled) }
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        repo.enableFloatingBottomBar = enabled
        _uiState.update { it.copy(enableFloatingBottomBar = enabled) }
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        repo.enableFloatingBottomBarBlur = enabled
        _uiState.update { it.copy(enableFloatingBottomBarBlur = enabled) }
    }

    fun setPageScale(scale: Float) {
        repo.pageScale = scale
        _uiState.update { it.copy(pageScale = scale) }
    }


}
