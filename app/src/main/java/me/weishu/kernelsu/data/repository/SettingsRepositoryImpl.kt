package me.weishu.kernelsu.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.screen.modulerepo.RepoSort
import java.security.SecureRandom

private const val SETTINGS_PREFS = "settings"
private const val KEY_USE_SOFT_REBOOT = "soft_reboot"

/** Prefer soft reboot only when the user enables the setting. */
fun isSoftRebootPreferred(): Boolean =
    ksuApp.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_USE_SOFT_REBOOT, false)

class SettingsRepositoryImpl : SettingsRepository {

    private companion object {
        private const val INTENT_TOKEN_KEY = "intent_token"
        private val secureRandom = SecureRandom()
    }

    private val prefs by lazy {
        ksuApp.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString("ui_mode", value) }

    override var checkUpdate: Boolean
        get() = prefs.getBoolean("check_update", true)
        set(value) = prefs.edit { putBoolean("check_update", value) }

    override var checkModuleUpdate: Boolean
        get() = prefs.getBoolean("module_check_update", true)
        set(value) = prefs.edit { putBoolean("module_check_update", value) }

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
        get() = prefs.getString("color_spec", ColorSpec.SpecVersion.Default.name) ?: ColorSpec.SpecVersion.Default.name
        set(value) = prefs.edit { putString("color_spec", value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", false)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var enableSwipeDismiss: Boolean
        get() = prefs.getBoolean("enable_swipe_dismiss", true)
        set(value) = prefs.edit { putBoolean("enable_swipe_dismiss", value) }

    override var pagerInterceptionMode: Int
        get() = prefs.getInt("pager_interception_mode", 1)
        set(value) = prefs.edit { putInt("pager_interception_mode", value.coerceIn(0, 2)) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean("enable_blur", false)
        set(value) = prefs.edit { putBoolean("enable_blur", value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar", value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar_blur", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar_blur", value) }

    override var enableNavigationBadge: Boolean
        get() = prefs.getBoolean("enable_navigation_badge", true)
        set(value) = prefs.edit { putBoolean("enable_navigation_badge", value) }

    override var navigationRailExpanded: Boolean
        get() = prefs.getBoolean("nav_rail_expanded", false)
        set(value) = prefs.edit { putBoolean("nav_rail_expanded", value) }

    override var pageScale: Float
        get() = prefs.getFloat("page_scale", 1.0f)
        set(value) = prefs.edit { putFloat("page_scale", value) }

    override var moduleDescriptionMaxLines: Int
        get() = prefs.getInt("module_description_max_lines", 4)
        set(value) = prefs.edit { putInt("module_description_max_lines", value) }

    override var enableWebDebugging: Boolean
        get() = prefs.getBoolean("enable_web_debugging", false)
        set(value) = prefs.edit { putBoolean("enable_web_debugging", value) }

    override var moduleSortEnabledFirst: Boolean
        get() = prefs.getBoolean("module_sort_enabled_first", false)
        set(value) = prefs.edit { putBoolean("module_sort_enabled_first", value) }

    override var moduleSortActionFirst: Boolean
        get() = prefs.getBoolean("module_sort_action_first", false)
        set(value) = prefs.edit { putBoolean("module_sort_action_first", value) }

    override var moduleRepoSortOrder: Int
        get() = prefs.getInt("module_repo_sort_order", RepoSort.UPDATED.ordinal)
        set(value) = prefs.edit { putInt("module_repo_sort_order", value) }

    override var superuserShowSystemApps: Boolean
        get() = prefs.getBoolean("show_system_apps", false)
        set(value) = prefs.edit { putBoolean("show_system_apps", value) }

    override var superuserShowOnlyPrimaryUserApps: Boolean
        get() = prefs.getBoolean("show_only_primary_user_apps", false)
        set(value) = prefs.edit { putBoolean("show_only_primary_user_apps", value) }

    override var superuserSortOption: Int
        get() = prefs.getInt("superuser_sort_option", 0)
        set(value) = prefs.edit { putInt("superuser_sort_option", value) }

    override var suLogFilters: Set<String>?
        get() = prefs.getStringSet("sulog_filters", null)?.toSet()
        set(filters) = prefs.edit { putStringSet("sulog_filters", filters) }

    // KernelSU auto-jailbreak is unsupported in OneuiXP; never enable boot hooks.
    override var autoJailbreak: Boolean
        get() = false
        set(value) { prefs.edit { putBoolean("auto_jailbreak", false) } }

    override var useSoftReboot: Boolean
        get() = prefs.getBoolean(KEY_USE_SOFT_REBOOT, false)
        set(value) = prefs.edit { putBoolean(KEY_USE_SOFT_REBOOT, value) }

    override val intentToken: String
        get() {
        val existing = prefs.getString(INTENT_TOKEN_KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val token = ByteArray(32).also(secureRandom::nextBytes)
            .joinToString(separator = "") { "%02x".format(it) }
        prefs.edit { putString(INTENT_TOKEN_KEY, token) }
        return token
    }

    override suspend fun getSuCompatStatus(): String = "unsupported"

    override suspend fun getSuCompatPersistValue(): Long? = null

    override fun isSuEnabled(): Boolean = false

    override fun setSuEnabled(enabled: Boolean): Boolean = false

    override fun setSuCompatModePref(mode: Int) = prefs.edit { putInt("su_compat_mode", mode) }

    override fun getSuCompatModePref(): Int = prefs.getInt("su_compat_mode", 0)

    override suspend fun getKernelUmountStatus(): String = "unsupported"

    override fun isKernelUmountEnabled(): Boolean = false

    override fun setKernelUmountEnabled(enabled: Boolean): Boolean = false

    override suspend fun getWebViewZygoteUmountStatus(): String = "unsupported"

    override fun isWebViewZygoteUmountEnabled(): Boolean = false

    override fun setWebViewZygoteUmountEnabled(enabled: Boolean): Boolean =
        false

    override suspend fun getSelinuxHideStatus(): String = "unsupported"

    override fun isSelinuxHideEnabled(): Boolean = false

    override fun setSelinuxHideEnabled(enabled: Boolean): Int = -1

    override suspend fun getSulogStatus(): String = "unsupported"

    override suspend fun getSulogPersistValue(): Long? = null

    override fun setSulogEnabled(enabled: Boolean): Boolean = false

    override suspend fun getAdbRootStatus(): String = "unsupported"

    override suspend fun getAdbRootPersistValue(): Long? = null

    override fun setAdbRootEnabled(enabled: Boolean): Boolean = false

    override fun isDefaultUmountModules(): Boolean = false

    override fun setDefaultUmountModules(enabled: Boolean): Boolean = false

    override fun isLkmMode(): Boolean = false

    override fun execKsudFeatureSave() {
        // OneuiXP intentionally has no KernelSU feature backend.
    }
}
