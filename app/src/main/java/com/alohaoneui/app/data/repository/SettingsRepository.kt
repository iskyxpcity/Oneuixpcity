package com.alohaoneui.app.data.repository

import kotlinx.serialization.Serializable

/** AlohaOneUI appearance, navigation and self-update preferences. */
interface SettingsRepository {
    var uiMode: String
    var checkUpdate: Boolean
    var appLanguage: String
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enablePredictiveBack: Boolean
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var navigationRailExpanded: Boolean
    var pageScale: Float
    var materialCornerScale: Float
    var materialShapeStyle: Int
    var materialOutlineWidth: Float
    var materialShadowContainers: Boolean
    var materialShadowTopBars: Boolean
    var materialShadowButtons: Boolean
    var materialShadowFloatingButtons: Boolean
    var materialShadowSwitches: Boolean
    var materialShadowSliders: Boolean
    var enableSwipeBack: Boolean
    var pagerGestureMode: Int

    fun snapshotForBackup(): AppSettingsBackup
    fun restoreFromBackup(snapshot: AppSettingsBackup)
}

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
    val enablePredictiveBack: Boolean,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val navigationRailExpanded: Boolean,
    val pageScale: Float,
    val materialCornerScale: Float = 1.0f,
    val materialShapeStyle: Int = 0,
    val materialOutlineWidth: Float = 0.0f,
    val materialShadowContainers: Boolean = true,
    val materialShadowTopBars: Boolean = true,
    val materialShadowButtons: Boolean = true,
    val materialShadowFloatingButtons: Boolean = true,
    val materialShadowSwitches: Boolean = true,
    val materialShadowSliders: Boolean = true,
    val enableSwipeBack: Boolean,
    val pagerGestureMode: Int,
    val appLanguage: String = "system",
)
