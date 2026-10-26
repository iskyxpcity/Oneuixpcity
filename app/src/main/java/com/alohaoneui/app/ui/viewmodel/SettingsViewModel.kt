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
            val materialCornerScale = repo.materialCornerScale
            val materialOutlineWidth = repo.materialOutlineWidth
            val materialShadowContainers = repo.materialShadowContainers
            val materialShadowTopBars = repo.materialShadowTopBars
            val materialShadowButtons = repo.materialShadowButtons
            val materialShadowFloatingButtons = repo.materialShadowFloatingButtons
            val materialShadowSwitches = repo.materialShadowSwitches
            val materialShadowSliders = repo.materialShadowSliders
            val enableSwipeBack = repo.enableSwipeBack
            val pagerGestureMode = repo.pagerGestureMode
            val colorStyle = repo.colorStyle
            val colorSpec = repo.colorSpec
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
                    materialCornerScale = materialCornerScale,
                    materialOutlineWidth = materialOutlineWidth,
                    materialShadowContainers = materialShadowContainers,
                    materialShadowTopBars = materialShadowTopBars,
                    materialShadowButtons = materialShadowButtons,
                    materialShadowFloatingButtons = materialShadowFloatingButtons,
                    materialShadowSwitches = materialShadowSwitches,
                    materialShadowSliders = materialShadowSliders,
                    enableSwipeBack = enableSwipeBack,
                    pagerGestureMode = pagerGestureMode,
                    colorStyle = colorStyle,
                    colorSpec = colorSpec,
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
        _uiState.update { it.copy(keyColor = color) }
    }

    fun setColorStyle(style: String) {
        repo.colorStyle = style
        _uiState.update { it.copy(colorStyle = style) }
    }

    fun setColorSpec(spec: String) {
        repo.colorSpec = spec
        _uiState.update { it.copy(colorSpec = spec) }
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

    fun setMaterialCornerScale(scale: Float) {
        val safe = scale.coerceIn(0.0f, 2.0f)
        repo.materialCornerScale = safe
        _uiState.update { it.copy(materialCornerScale = safe) }
    }

    fun setMaterialOutlineWidth(width: Float) {
        val safe = width.coerceIn(0.0f, 1.5f)
        repo.materialOutlineWidth = safe
        _uiState.update { it.copy(materialOutlineWidth = safe) }
    }

    fun setMaterialShadowContainers(enabled: Boolean) {
        repo.materialShadowContainers = enabled
        _uiState.update { it.copy(materialShadowContainers = enabled) }
    }

    fun setMaterialShadowTopBars(enabled: Boolean) {
        repo.materialShadowTopBars = enabled
        _uiState.update { it.copy(materialShadowTopBars = enabled) }
    }

    fun setMaterialShadowButtons(enabled: Boolean) {
        repo.materialShadowButtons = enabled
        _uiState.update { it.copy(materialShadowButtons = enabled) }
    }

    fun setMaterialShadowFloatingButtons(enabled: Boolean) {
        repo.materialShadowFloatingButtons = enabled
        _uiState.update { it.copy(materialShadowFloatingButtons = enabled) }
    }

    fun setMaterialShadowSwitches(enabled: Boolean) {
        repo.materialShadowSwitches = enabled
        _uiState.update { it.copy(materialShadowSwitches = enabled) }
    }

    fun setMaterialShadowSliders(enabled: Boolean) {
        repo.materialShadowSliders = enabled
        _uiState.update { it.copy(materialShadowSliders = enabled) }
    }


}
