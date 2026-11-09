package com.alohaoneui.app.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.alohaoneui.app.ui.UiMode

/** AlohaOneUI appearance and application preferences. */
@Immutable
data class SettingsUiState(
    val uiMode: String = UiMode.DEFAULT_VALUE,
    val checkUpdate: Boolean = true,
    val appLanguage: String = "system",
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = PaletteStyle.TonalSpot.name,
    val colorSpec: String = ColorSpec.SpecVersion.SPEC_2025.name,
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val pageScale: Float = 1.0f,
    val materialCornerScale: Float = 1.0f,
    val materialShapeStyle: Int = 0,
    val materialSwitchStyle: Int = 0,
    val materialSliderStyle: Int = 0,
    val materialIconShapeStyle: Int = -1,
    val materialOutlineWidth: Float = 0.0f,
    val materialShadowContainers: Boolean = true,
    val materialShadowTopBars: Boolean = true,
    val materialShadowButtons: Boolean = true,
    val materialShadowFloatingButtons: Boolean = true,
    val materialShadowSwitches: Boolean = true,
    val materialShadowSliders: Boolean = true,
    val enableSwipeBack: Boolean = false,
    val pagerGestureMode: Int = 0,
)

@Immutable
data class SettingsScreenActions(
    val onSetCheckUpdate: (Boolean) -> Unit,
    val onSetAppLanguage: (String) -> Unit,
    val onOpenTheme: () -> Unit,
    val onSetUiModeIndex: (Int) -> Unit,
    val onOpenAbout: () -> Unit,
)
