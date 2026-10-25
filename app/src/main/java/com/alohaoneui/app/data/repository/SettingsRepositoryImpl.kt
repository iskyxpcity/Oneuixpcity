package com.alohaoneui.app.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    override var materialFontScale: Float
        get() = prefs.getFloat("material_font_scale", 1.0f).coerceIn(0.8f, 1.5f)
        set(value) = prefs.edit { putFloat("material_font_scale", value.coerceIn(0.8f, 1.5f)) }

    override var materialContrastMode: Int
        get() = prefs.getInt("material_contrast_mode", -1).coerceIn(-1, 2)
        set(value) = prefs.edit { putInt("material_contrast_mode", value.coerceIn(-1, 2)) }

    override var materialCornerScale: Float
        get() = prefs.getFloat("material_corner_scale", 1.0f).coerceIn(0.5f, 1.5f)
        set(value) = prefs.edit { putFloat("material_corner_scale", value.coerceIn(0.5f, 1.5f)) }

    override var materialBorderWidth: Float
        get() = prefs.getFloat("material_border_width", 0.0f).coerceIn(0.0f, 3.0f)
        set(value) = prefs.edit { putFloat("material_border_width", value.coerceIn(0.0f, 3.0f)) }

    override var materialColorVisionMode: Int
        get() = prefs.getInt("material_color_vision_mode", 0).coerceIn(0, 4)
        set(value) = prefs.edit { putInt("material_color_vision_mode", value.coerceIn(0, 4)) }

    override var materialNavigationMotionMode: Int
        get() = prefs.getInt("material_navigation_motion_mode", 0).coerceIn(0, 1)
        set(value) = prefs.edit { putInt("material_navigation_motion_mode", value.coerceIn(0, 1)) }

    override var materialDynamicColorEnabled: Boolean
        get() = prefs.getBoolean("material_dynamic_color_enabled", true)
        set(value) = prefs.edit { putBoolean("material_dynamic_color_enabled", value) }

    override var materialInvertColors: Boolean
        get() = prefs.getBoolean("material_invert_colors", false)
        set(value) = prefs.edit { putBoolean("material_invert_colors", value) }

    override var materialShapeStyle: Int
        get() = prefs.getInt("material_shape_style", 2).coerceIn(0, 6)
        set(value) = prefs.edit { putInt("material_shape_style", value.coerceIn(0, 6)) }

    override var materialSwitchStyle: Int
        get() = prefs.getInt("material_switch_style", 0).coerceIn(0, 7)
        set(value) = prefs.edit { putInt("material_switch_style", value.coerceIn(0, 7)) }

    override var materialSliderStyle: Int
        get() = prefs.getInt("material_slider_style", 3).coerceIn(0, 3)
        set(value) = prefs.edit { putInt("material_slider_style", value.coerceIn(0, 3)) }

    override var materialLaunchFollowsTheme: Boolean
        get() = prefs.getBoolean("material_launch_follows_theme", true)
        set(value) = prefs.edit { putBoolean("material_launch_follows_theme", value) }

    override var materialShadowContainers: Boolean
        get() = prefs.getBoolean("material_shadow_containers", true)
        set(value) = prefs.edit { putBoolean("material_shadow_containers", value) }

    override var materialShadowAppBars: Boolean
        get() = prefs.getBoolean("material_shadow_appbars", true)
        set(value) = prefs.edit { putBoolean("material_shadow_appbars", value) }

    override var materialShadowButtons: Boolean
        get() = prefs.getBoolean("material_shadow_buttons", true)
        set(value) = prefs.edit { putBoolean("material_shadow_buttons", value) }

    override var materialShadowSwitches: Boolean
        get() = prefs.getBoolean("material_shadow_switches", true)
        set(value) = prefs.edit { putBoolean("material_shadow_switches", value) }

    override var materialShadowSliders: Boolean
        get() = prefs.getBoolean("material_shadow_sliders", true)
        set(value) = prefs.edit { putBoolean("material_shadow_sliders", value) }


    override var materialFontFamily: String
        get() = prefs.getString("material_font_family", "Default")
            ?.takeIf { it in setOf("Default", "SansSerif", "Serif", "Monospace", "Cursive") } ?: "Default"
        set(value) = prefs.edit {
            putString("material_font_family", value.takeIf { it in setOf("Default", "SansSerif", "Serif", "Monospace", "Cursive") } ?: "Default")
        }

    override var materialColorSchemes: List<MaterialColorScheme>
        get() = runCatching {
            Json.decodeFromString<List<MaterialColorScheme>>(
                prefs.getString("material_color_schemes", "[]") ?: "[]"
            )
        }.getOrDefault(emptyList())
        set(value) = prefs.edit {
            putString("material_color_schemes", Json.encodeToString(value))
        }

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
            .putFloat("material_font_scale", snapshot.materialFontScale.coerceIn(0.8f, 1.5f))
            .putInt("material_contrast_mode", snapshot.materialContrastMode.coerceIn(-1, 2))
            .putFloat("material_corner_scale", snapshot.materialCornerScale.coerceIn(0.5f, 1.5f))
            .putString("material_color_schemes", Json.encodeToString(snapshot.materialColorSchemes))
            .putFloat("material_border_width", snapshot.materialBorderWidth.coerceIn(0.0f, 3.0f))
            .putString("material_font_family", snapshot.materialFontFamily.takeIf { it in setOf("Default", "SansSerif", "Serif", "Monospace", "Cursive") } ?: "Default")
            .putInt("material_color_vision_mode", snapshot.materialColorVisionMode.coerceIn(0, 4))
            .putInt("material_navigation_motion_mode", snapshot.materialNavigationMotionMode.coerceIn(0, 1))
            .putBoolean("material_dynamic_color_enabled", snapshot.materialDynamicColorEnabled)
            .putBoolean("material_invert_colors", snapshot.materialInvertColors)
            .putInt("material_shape_style", snapshot.materialShapeStyle.coerceIn(0, 6))
            .putInt("material_switch_style", snapshot.materialSwitchStyle.coerceIn(0, 7))
            .putInt("material_slider_style", snapshot.materialSliderStyle.coerceIn(0, 3))
            .putBoolean("material_launch_follows_theme", snapshot.materialLaunchFollowsTheme)
            .putBoolean("material_shadow_containers", snapshot.materialShadowContainers)
            .putBoolean("material_shadow_appbars", snapshot.materialShadowAppBars)
            .putBoolean("material_shadow_buttons", snapshot.materialShadowButtons)
            .putBoolean("material_shadow_switches", snapshot.materialShadowSwitches)
            .putBoolean("material_shadow_sliders", snapshot.materialShadowSliders)
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
