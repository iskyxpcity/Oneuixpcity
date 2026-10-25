package com.alohaoneui.app.data.repository

import android.content.Context
import androidx.core.content.edit
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.alohaoneui.app.alohaOneUIApp
import com.alohaoneui.app.ui.UiMode

/** Persist only the AlohaOneUI theme/navigation configuration. */
class SettingsRepositoryImpl : SettingsRepository {
    private val prefs by lazy {
        alohaOneUIApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString("ui_mode", value) }

    override var checkUpdate: Boolean
        get() = prefs.getBoolean("check_update", true)
        set(value) = prefs.edit { putBoolean("check_update", value) }

    override var themeMode: Int
        get() = prefs.getInt("color_mode", 0)
        set(value) = prefs.edit { putInt("color_mode", value) }

    override var miuixMonet: Boolean
        get() = prefs.getBoolean("miuix_monet", false)
        set(value) = prefs.edit { putBoolean("miuix_monet", value) }

    override var keyColor: Int
        get() = prefs.getInt("key_color", 0)
        set(value) = prefs.edit { putInt("key_color", value) }

    override var colorStyle: String
        get() = prefs.getString("color_style", PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name
        set(value) = prefs.edit { putString("color_style", value) }

    override var colorSpec: String
        get() = prefs.getString("color_spec", ColorSpec.SpecVersion.SPEC_2025.name) ?: ColorSpec.SpecVersion.SPEC_2025.name
        set(value) = prefs.edit { putString("color_spec", value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", false)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean("enable_blur", false)
        set(value) = prefs.edit { putBoolean("enable_blur", value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar", value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar_blur", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar_blur", value) }

    override var navigationRailExpanded: Boolean
        get() = prefs.getBoolean("nav_rail_expanded", false)
        set(value) = prefs.edit { putBoolean("nav_rail_expanded", value) }

    override var pageScale: Float
        get() = prefs.getFloat("page_scale", 1.0f)
        set(value) = prefs.edit { putFloat("page_scale", value) }

    override var enableSwipeBack: Boolean
        get() = prefs.getBoolean("enable_swipe_back", false)
        set(value) = prefs.edit { putBoolean("enable_swipe_back", value) }

    override var pagerGestureMode: Int
        get() = prefs.getInt("pager_gesture_mode", 0).coerceIn(0, 2)
        set(value) = prefs.edit { putInt("pager_gesture_mode", value.coerceIn(0, 2)) }

    override fun snapshotForBackup(): AppSettingsBackup = AppSettingsBackup(
        uiMode = uiMode,
        checkUpdate = checkUpdate,
        themeMode = themeMode,
        miuixMonet = miuixMonet,
        keyColor = keyColor,
        colorStyle = colorStyle,
        colorSpec = colorSpec,
        enablePredictiveBack = enablePredictiveBack,
        enableBlur = enableBlur,
        enableFloatingBottomBar = enableFloatingBottomBar,
        enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
        navigationRailExpanded = navigationRailExpanded,
        pageScale = pageScale,
        enableSwipeBack = enableSwipeBack,
        pagerGestureMode = pagerGestureMode,
    )

    override fun restoreFromBackup(snapshot: AppSettingsBackup) {
        val restoredColorStyle = runCatching {
            PaletteStyle.valueOf(snapshot.colorStyle).name
        }.getOrDefault(PaletteStyle.TonalSpot.name)
        val restoredColorSpec = runCatching {
            ColorSpec.SpecVersion.valueOf(snapshot.colorSpec).name
        }.getOrDefault(ColorSpec.SpecVersion.SPEC_2025.name)

        val committed = prefs.edit()
            .putString("ui_mode", UiMode.fromValue(snapshot.uiMode).value)
            .putBoolean("check_update", snapshot.checkUpdate)
            .putInt("color_mode", snapshot.themeMode)
            .putBoolean("miuix_monet", snapshot.miuixMonet)
            .putInt("key_color", snapshot.keyColor)
            .putString("color_style", restoredColorStyle)
            .putString("color_spec", restoredColorSpec)
            .putBoolean("enable_predictive_back", snapshot.enablePredictiveBack)
            .putBoolean("enable_blur", snapshot.enableBlur)
            .putBoolean("enable_floating_bottom_bar", snapshot.enableFloatingBottomBar)
            .putBoolean("enable_floating_bottom_bar_blur", snapshot.enableFloatingBottomBarBlur)
            .putBoolean("nav_rail_expanded", snapshot.navigationRailExpanded)
            .putFloat("page_scale", snapshot.pageScale)
            .putBoolean("enable_swipe_back", snapshot.enableSwipeBack)
            .putInt("pager_gesture_mode", snapshot.pagerGestureMode.coerceIn(0, 2))
            .commit()
        check(committed) { "Could not persist restored AlohaOneUI app settings." }
    }
}
