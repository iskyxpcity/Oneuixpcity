package com.alohaoneui.app.ui.screen.colorpalette

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.alohaoneui.app.AlohaOneUIApplication
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.theme.ColorMode
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel

@Composable
fun ColorPaletteScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPaletteStyle = try {
        PaletteStyle.valueOf(uiState.colorStyle)
    } catch (_: Exception) {
        PaletteStyle.TonalSpot
    }
    val currentColorSpec = try {
        ColorSpec.SpecVersion.valueOf(uiState.colorSpec)
    } catch (_: Exception) {
        ColorSpec.SpecVersion.Default
    }
    val state = ColorPaletteUiState(
        uiState = uiState,
        currentColorMode = ColorMode.fromValue(uiState.themeMode),
        currentPaletteStyle = currentPaletteStyle,
        currentColorSpec = currentColorSpec,
    )
    val actions = ColorPaletteScreenActions(
        onBack = { if (navigator.backStackSize() > 1) navigator.pop() },
        onSetThemeMode = viewModel::setThemeMode,
        onSetMiuixMonet = viewModel::setMiuixMonet,
        onSetKeyColor = viewModel::setKeyColor,
        onSetColorMode = viewModel::setColorMode,
        onSetMaterialDynamicColorEnabled = viewModel::setMaterialDynamicColorEnabled,
        onSetColorStyle = viewModel::setColorStyle,
        onSetColorSpec = viewModel::setColorSpec,
        onSetMaterialFontScale = viewModel::setMaterialFontScale,
        onSetMaterialContrastMode = viewModel::setMaterialContrastMode,
        onSetMaterialContrastLevel = viewModel::setMaterialContrastLevel,
        onSetMaterialCornerScale = viewModel::setMaterialCornerScale,
        onSetMaterialBorderWidth = viewModel::setMaterialBorderWidth,
        onSetMaterialFontFamily = viewModel::setMaterialFontFamily,
        onSetMaterialShadowContainers = viewModel::setMaterialShadowContainers,
        onSetMaterialShadowAppBars = viewModel::setMaterialShadowAppBars,
        onSetMaterialShadowButtons = viewModel::setMaterialShadowButtons,
        onSetMaterialShadowSwitches = viewModel::setMaterialShadowSwitches,
        onSetMaterialShadowSliders = viewModel::setMaterialShadowSliders,
        onSetMaterialColorVisionMode = viewModel::setMaterialColorVisionMode,
        onSetMaterialNavigationMotionMode = viewModel::setMaterialNavigationMotionMode,
        onSaveMaterialColorScheme = viewModel::saveMaterialColorScheme,
        onApplyMaterialColorScheme = viewModel::applyMaterialColorScheme,
        onDeleteMaterialColorScheme = viewModel::deleteMaterialColorScheme,
        onSetEnableBlur = viewModel::setEnableBlur,
        onSetEnableFloatingBottomBar = viewModel::setEnableFloatingBottomBar,
        onSetEnableFloatingBottomBarBlur = viewModel::setEnableFloatingBottomBarBlur,
        onSetEnablePredictiveBack = { enabled ->
            viewModel.setEnablePredictiveBack(enabled)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                AlohaOneUIApplication.applyEnableOnBackInvokedCallback(context, enabled)
                activity?.recreate()
            }
        },
        onSetEnableSwipeBack = viewModel::setEnableSwipeBack,
        onSetPagerGestureMode = viewModel::setPagerGestureMode,
        onSetPageScale = viewModel::setPageScale,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> ColorPaletteScreenMiuix(state, actions)
        UiMode.Material -> ColorPaletteScreenMaterial(state, actions)
    }
}
