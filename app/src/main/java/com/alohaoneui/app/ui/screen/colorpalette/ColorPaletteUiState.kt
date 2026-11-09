package com.alohaoneui.app.ui.screen.colorpalette

import androidx.compose.runtime.Immutable
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.alohaoneui.app.ui.screen.settings.SettingsUiState
import com.alohaoneui.app.ui.theme.ColorMode

@Immutable
data class ColorPaletteUiState(
    val uiState: SettingsUiState,
    val currentColorMode: ColorMode,
    val currentPaletteStyle: PaletteStyle,
    val currentColorSpec: ColorSpec.SpecVersion,
)

@Immutable
data class ColorPaletteScreenActions(
    val onBack: () -> Unit,
    val onSetThemeMode: (Int) -> Unit,
    val onSetMiuixMonet: (Boolean) -> Unit,
    val onSetKeyColor: (Int) -> Unit,
    val onSetColorMode: (ColorMode) -> Unit,
    val onSetColorStyle: (String) -> Unit,
    val onSetColorSpec: (String) -> Unit,
    val onSetEnableBlur: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBar: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBarBlur: (Boolean) -> Unit,
    val onSetEnablePredictiveBack: (Boolean) -> Unit,
    val onSetEnableSwipeBack: (Boolean) -> Unit,
    val onSetPagerGestureMode: (Int) -> Unit,
    val onSetPageScale: (Float) -> Unit,
    val onSetMaterialCornerScale: (Float) -> Unit,
    val onSetMaterialShapeStyle: (Int) -> Unit,
    val onSetMaterialSwitchStyle: (Int) -> Unit,
    val onSetMaterialSliderStyle: (Int) -> Unit,
    val onSetMaterialIconShapeStyle: (Int) -> Unit,
    val onSetMaterialOutlineWidth: (Float) -> Unit,
    val onSetMaterialShadowContainers: (Boolean) -> Unit,
    val onSetMaterialShadowTopBars: (Boolean) -> Unit,
    val onSetMaterialShadowButtons: (Boolean) -> Unit,
    val onSetMaterialShadowFloatingButtons: (Boolean) -> Unit,
    val onSetMaterialShadowSwitches: (Boolean) -> Unit,
    val onSetMaterialShadowSliders: (Boolean) -> Unit,
)
