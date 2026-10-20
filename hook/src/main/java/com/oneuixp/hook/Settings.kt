package com.oneuixp.hook

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.view.View
import android.widget.TextView
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.oneuixp.common.Package
import com.oneuixp.hook.util.afterAttach
import com.oneuixp.hook.util.reflect
import com.oneuixp.hook.util.xlog

@SuppressLint("PrivateApi")
object Settings {

    /**
     * Enables the large-screen/activity-embedding path in Samsung Settings.
     * The hooks are installed when this feature is enabled for the Settings process.
     */
    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun enableSettingsTwoPane() {
        if (param.packageName != Package.SETTINGS) return

        val largeScreenProperties = setOf(
            "persist.settings.large_screen_opt.enabled",
            "persist.settings.large_screen_opt_for_dp.enabled",
        )

        listOf("android.os.SystemProperties", "android.os.SemSystemProperties").forEach { className ->
            runCatching {
                val clazz = param.classLoader.loadClass(className)
                val method = clazz.getDeclaredMethod(
                    "getBoolean",
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                )
                xposedModule.hook(method).intercept { chain ->
                    val key = chain.args.getOrNull(0) as? String
                    if (key != null && key in largeScreenProperties) true else chain.proceed()
                }
            }.onFailure { xlog(it) }
        }

        runCatching {
            val clazz = param.classLoader.loadClass("android.util.FeatureFlagUtils")
            val method = clazz.getDeclaredMethod("isEnabled", Context::class.java, String::class.java)
            xposedModule.hook(method).intercept { chain ->
                val key = chain.args.getOrNull(1) as? String
                if (key != null && key in setOf("settings_support_large_screen", "flag_settings_support_large_screen")) {
                    true
                } else {
                    chain.proceed()
                }
            }
        }.onFailure { xlog(it) }

        runCatching {
            val clazz = param.classLoader.loadClass("com.samsung.android.settings.homepage.SecTopLevelFeature")
            val method = clazz.getDeclaredMethod("getBoolean", String::class.java)
            val topLevelKeys = setOf(
                "flag_settings_support_large_screen",
                "feature_is_embedding_activity_enabled",
                "isSettingsSplitEnabled",
                "flag_user_setup_complete",
                "flag_force_single_view",
            )
            xposedModule.hook(method).intercept { chain ->
                val key = chain.args.getOrNull(0) as? String
                if (key != null && key in topLevelKeys) key != "flag_force_single_view" else chain.proceed()
            }
        }.onFailure { xlog(it) }

        runCatching {
            val clazz = param.classLoader.loadClass("com.android.settings.SettingsApplication")
            val method = clazz.getDeclaredMethod("onCreate")
            xposedModule.hook(method).intercept { chain ->
                val result = chain.proceed()
                runCatching {
                    val embedding = param.classLoader.loadClass(
                        "com.android.settings.activityembedding.ActivityEmbeddingUtils"
                    )
                    embedding.getDeclaredField("SHOULD_ENABLE_LARGE_SCREEN_OPTIMIZATION").apply {
                        isAccessible = true
                        setBoolean(null, true)
                    }
                }.onFailure { xlog(it) }
                result
            }
        }.onFailure { xlog(it) }

        runCatching {
            val clazz = param.classLoader.loadClass("com.android.settings.activityembedding.ActivityEmbeddingUtils")
            clazz.declaredMethods
                .filter { it.name == "isSettingsSplitEnabled" || it.name == "isEmbeddingActivityEnabled" }
                .forEach { method ->
                    xposedModule.hook(method).intercept { chain ->
                        true
                    }
                }
        }.onFailure { xlog(it) }

        runCatching {
            val clazz = param.classLoader.loadClass("androidx.window.embedding.SplitRule")
            clazz.declaredConstructors
                .filter {
                    it.parameterTypes.size == 3 &&
                        it.parameterTypes[0] == Int::class.javaPrimitiveType &&
                        it.parameterTypes[1] == Int::class.javaPrimitiveType &&
                        it.parameterTypes[2].name == "androidx.window.embedding.SplitAttributes"
                }
                .forEach { constructor ->
                    xposedModule.hook(constructor).intercept { chain ->
                        val args = chain.args.toMutableList()
                        args[0] = 0
                        args[1] = 0
                        chain.proceed(args.toTypedArray())
                    }
                }
        }.onFailure { xlog(it) }
    }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun showPackageInfo() {
        if (param.packageName != Package.SETTINGS) return
        afterAttach {
            try {
                val controllerClass = param.classLoader.loadClass(
                    "com.android.settings.applications.appinfo.AppHeaderViewPreferenceController"
                )
                val appEntryClass = param.classLoader.loadClass(
                    $$"com.android.settingslib.applications.ApplicationsState$AppEntry"
                )
                val method = controllerClass.getDeclaredMethod(
                    "setAppLabelAndIcon",
                    PackageInfo::class.java,
                    appEntryClass
                )
                xposedModule.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    try {
                        val header = chain.thisObject.reflect["mHeader"]
                        val mRootView = header?.reflect?.get("mRootView") as? View ?: return@intercept result

                        @SuppressLint("DiscouragedApi")
                        val identifier = mRootView.resources.getIdentifier(
                            "entity_header_summary", "id", Package.SETTINGS
                        )
                        val packageInfo = chain.args[0] as PackageInfo
                        val versionName = packageInfo.versionName
                        val versionCode = packageInfo.longVersionCode
                        val packageName = packageInfo.packageName
                        mRootView.findViewById<TextView>(identifier)?.apply {
                            @SuppressLint("SetTextI18n")
                            text = "$text $versionName ($versionCode)\n$packageName"
                            setTextIsSelectable(true)
                        }
                    } catch (t: Throwable) {
                        xlog(t)
                    }
                    result
                }
            } catch (t: Throwable) {
                xlog(t)
            }
        }
    }

    // 支持任意字体
    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun supportAnyFont() {
        if (param.packageName != Package.SETTINGS) return
        afterAttach {
            try {
                val clazz = param.classLoader.loadClass(
                    "com.samsung.android.settings.display.SecDisplayUtils"
                )
                val method = clazz.getDeclaredMethod("isInvalidFont", Context::class.java, String::class.java)
                xposedModule.hook(method).intercept { false }
            } catch (t: Throwable) {
                xlog(t)
            }
        }
    }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun showMoreBatteryInfo() {
        // res/xml/sec_battery_info_settings.xml
        // com.samsung.android.settings.deviceinfo.batteryinfo
        if (param.packageName != Package.SETTINGS) return
        afterAttach {
            try {
                val clazz = param.classLoader.loadClass(
                    "com.samsung.android.settings.deviceinfo.batteryinfo.BatteryRegulatoryPreferenceController"
                )
                val method = clazz.getDeclaredMethod("getAvailabilityStatus")
                xposedModule.hook(method).intercept { 0 }
            } catch (t: Throwable) {
                xlog(t)
            }
        }
    }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun showForcePeakRefreshRatePreference() {
        if (param.packageName != Package.SETTINGS) return
        afterAttach {
            try {
                val clazz = param.classLoader.loadClass(
                    "com.android.settings.development.ForcePeakRefreshRatePreferenceController"
                )
                val method = clazz.getDeclaredMethod("isAvailable")
                xposedModule.hook(method).intercept { true }
            } catch (t: Throwable) {
                xlog(t)
            }
        }
    }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun supportOutdoorMode() {
        if (param.packageName != Package.SETTINGS) return
        try {
            val controllerClass = param.classLoader.loadClass(
                "com.samsung.android.settings.display.controller.SecOutDoorModePreferenceController"
            )
            val isAvailableMethod = controllerClass.getDeclaredMethod("isAvailable")
            xposedModule.hook(isAvailableMethod).intercept { true }
        } catch (t: Throwable) {
            xlog(t)
        }

        try {
            val runeClass = runCatching {
                param.classLoader.loadClass("com.samsung.android.settings.Rune")
            }.getOrNull()
            val method = runeClass?.declaredMethods?.firstOrNull {
                it.name == "supportOutdoorMode" && it.parameterTypes.contentEquals(arrayOf(Context::class.java))
            }
            if (method != null) {
                xposedModule.hook(method).intercept { true }
            }
        } catch (t: Throwable) {
            xlog(t)
        }
    }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun supportAutoPowerOnOff() {
        if (param.packageName != Package.SETTINGS) return

        try {
            val floatingFeatureClass = param.classLoader.loadClass("com.samsung.android.feature.SemFloatingFeature")
            val getBooleanMethod = floatingFeatureClass.getDeclaredMethod("getBoolean", String::class.java)
            xposedModule.hook(getBooleanMethod).intercept { chain ->
                if (chain.args.firstOrNull() == "SEC_FLOATING_FEATURE_SETTINGS_SUPPORT_AUTO_POWER_ON_OFF") {
                    true
                } else {
                    chain.proceed()
                }
            }
        } catch (t: Throwable) {
            xlog(t)
        }

        try {
            val controllerClass = param.classLoader.loadClass(
                "com.samsung.android.settings.general.AutoPowerOnOffPreferenceController"
            )
            val isSupportMethod = controllerClass.getDeclaredMethod("isSupportAutoPowerOnOff")
            xposedModule.hook(isSupportMethod).intercept { true }
        } catch (t: Throwable) {
            xlog(t)
        }

        val shouldSpoofChinaModel = ThreadLocal<Boolean>().apply { set(false) }

        try {
            val receiverClass = param.classLoader.loadClass(
                "com.samsung.android.settings.autopoweronoff.AutoPowerOnOffReceiver"
            )
            val onReceiveMethod = receiverClass.getDeclaredMethod("onReceive", Context::class.java, Intent::class.java)
            xposedModule.hook(onReceiveMethod).intercept { chain ->
                shouldSpoofChinaModel.set(true)
                try {
                    chain.proceed()
                } finally {
                    shouldSpoofChinaModel.set(false)
                }
            }
        } catch (t: Throwable) {
            xlog(t)
        }

        try {
            val innerSettingsClass = param.classLoader.loadClass(
                $$"com.samsung.android.settings.autopoweronoff.AutoPowerOnOffSettings$2"
            )
            val resetSettingsMethod = innerSettingsClass.getDeclaredMethod("resetSettings", Context::class.java)
            xposedModule.hook(resetSettingsMethod).intercept { chain ->
                shouldSpoofChinaModel.set(true)
                try {
                    chain.proceed()
                } finally {
                    shouldSpoofChinaModel.set(false)
                }
            }
        } catch (t: Throwable) {
            xlog(t)
        }

        try {
            val runeClass = param.classLoader.loadClass("com.samsung.android.settings.Rune")
            runeClass.declaredMethods
                .filter { it.name == "isChinaModel" }
                .forEach { method ->
                    xposedModule.hook(method).intercept { chain ->
                        if (shouldSpoofChinaModel.get() == true) {
                            true
                        } else {
                            chain.proceed()
                        }
                    }
                }
        } catch (t: Throwable) {
            xlog(t)
        }
    }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun spoofPhoneStatusAsOfficial() {
        if (param.packageName != Package.SETTINGS) return
        try {
            val clazz = param.classLoader.loadClass("com.samsung.android.settings.deviceinfo.SecDeviceInfoUtils")
            val isPhoneStatusUnlockedMethod = clazz.getDeclaredMethod("isPhoneStatusUnlocked")
            xposedModule.hook(isPhoneStatusUnlockedMethod).intercept { false }

            val checkRootingConditionMethod = clazz.getDeclaredMethod("checkRootingCondition")
            xposedModule.hook(checkRootingConditionMethod).intercept { false }
        } catch (t: Throwable) {
            xlog(t)
        }
    }
}
