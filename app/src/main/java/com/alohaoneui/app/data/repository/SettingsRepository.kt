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
    var materialCornerScale: Float
    var materialColorSchemes: List<MaterialColorScheme>
    var materialBorderWidth: Float
    var enablePredictiveBack: Boolean
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var navigationRailExpanded: Boolean
    var pageScale: Float
    var enableSwipeBack: Boolean
    var pagerGestureMode: Int

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
    val materialCornerScale: Float = 1.0f,
    val materialColorSchemes: List<MaterialColorScheme> = emptyList(),
    val materialBorderWidth: Float = 0.0f,
    val enablePredictiveBack: Boolean,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val navigationRailExpanded: Boolean,
    val pageScale: Float,
    val enableSwipeBack: Boolean,
    val pagerGestureMode: Int,
)
