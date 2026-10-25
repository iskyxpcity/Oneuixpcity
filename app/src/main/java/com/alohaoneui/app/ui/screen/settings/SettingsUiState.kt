package com.alohaoneui.app.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.data.repository.MaterialColorScheme

/** AlohaOneUI appearance and application preferences. */
@Immutable
data class SettingsUiState(
    val uiMode: String = UiMode.DEFAULT_VALUE,
    val appLanguage: String = "system",
    val checkUpdate: Boolean = true,
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = PaletteStyle.Expressive.name,
    val colorSpec: String = ColorSpec.SpecVersion.SPEC_2025.name,
    val materialFontScale: Float = 1.0f,
    val materialContrastMode: Int = -1,
    val materialContrastLevel: Float = 0.0f,
    val materialCornerScale: Float = 1.0f,
    val materialColorSchemes: List<MaterialColorScheme> = emptyList(),
    val materialBorderWidth: Float = 0.0f,
    val materialFontFamily: String = "Default",
    val materialColorVisionMode: Int = 0,
    val materialNavigationMotionMode: Int = 0,
    val materialDynamicColorEnabled: Boolean = true,
    val materialInvertColors: Boolean = false,
    val materialShapeStyle: Int = 0,
    val materialSwitchStyle: Int = 0,
    val materialSliderStyle: Int = 0,
    val materialLaunchFollowsTheme: Boolean = true,
    val materialShadowContainers: Boolean = true,
    val materialShadowAppBars: Boolean = true,
    val materialShadowButtons: Boolean = true,
    val materialShadowFab: Boolean = true,
    val materialShadowSwitches: Boolean = true,
    val materialShadowSliders: Boolean = true,
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val pageScale: Float = 1.0f,
    val enableSwipeBack: Boolean = false,
    val enableHapticFeedback: Boolean = true,
    val materialLargeTextEnabled: Boolean = false,
    val materialBoldTextEnabled: Boolean = false,
    val pagerGestureMode: Int = 0,
)

@Immutable
data class SettingsScreenActions(
    val onSetCheckUpdate: (Boolean) -> Unit,
    val onOpenTheme: () -> Unit,
    val onSetUiModeIndex: (Int) -> Unit,
    val onOpenAbout: () -> Unit,
    val onSetAppLanguage: (Int) -> Unit,
    val onSetMaterialFontScale: (Float) -> Unit,
    val onSetMaterialLargeText: (Boolean) -> Unit,
    val onSetMaterialBoldText: (Boolean) -> Unit,
    val onSetNavigationMotion: (Int) -> Unit,
    val onSetHapticFeedback: (Boolean) -> Unit,
    val onResetDisplay: () -> Unit,
)
