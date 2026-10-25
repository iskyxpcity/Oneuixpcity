package com.alohaoneui.app.data.repository

import kotlinx.serialization.Serializable

/** AlohaOneUI appearance, navigation and self-update preferences. */
interface SettingsRepository {
    var uiMode: String
    var checkUpdate: Boolean
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var materialFontScale: Float
    var materialContrastMode: Int
    var materialContrastLevel: Float
    var materialCornerScale: Float
    var materialColorSchemes: List<MaterialColorScheme>
    var materialBorderWidth: Float
    var materialFontFamily: String
    var materialColorVisionMode: Int
    var materialNavigationMotionMode: Int
    var materialDynamicColorEnabled: Boolean
    var materialInvertColors: Boolean
    var materialShapeStyle: Int
    var materialSwitchStyle: Int
    var materialSliderStyle: Int
    var materialLaunchFollowsTheme: Boolean
    var materialShadowContainers: Boolean
    var materialShadowAppBars: Boolean
    var materialShadowButtons: Boolean
    var materialShadowFab: Boolean
    var materialShadowSwitches: Boolean
    var materialShadowSliders: Boolean
    var enablePredictiveBack: Boolean
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var navigationRailExpanded: Boolean
    var pageScale: Float
    var enableSwipeBack: Boolean
    var pagerGestureMode: Int
    var enableHapticFeedback: Boolean

    fun snapshotForBackup(): AppSettingsBackup
    fun restoreFromBackup(snapshot: AppSettingsBackup)
}

@Serializable
data class MaterialColorScheme(
    val name: String,
    val keyColor: Int,
    val colorStyle: String,
    val colorSpec: String,
    val contrastMode: Int = -1,
    val contrastLevel: Float = 0.0f,
)

/**
 * App-level settings stored separately from the LSPosed feature Preference.
 * Keep every SettingsRepository field here so manual export/import is complete.
 */
@Serializable
data class AppSettingsBackup(
    val uiMode: String,
    val checkUpdate: Boolean,
    val themeMode: Int,
    val miuixMonet: Boolean,
    val keyColor: Int,
    val colorStyle: String,
    val colorSpec: String,
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
    val enablePredictiveBack: Boolean,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val navigationRailExpanded: Boolean,
    val pageScale: Float,
    val enableSwipeBack: Boolean,
    val pagerGestureMode: Int,
    val enableHapticFeedback: Boolean = true,
)
