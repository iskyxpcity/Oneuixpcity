package io.github.soclear.oneuix.hook

import android.content.Context
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.hook.util.xlog
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/** Requested two-pane Settings hooks ported from App20261018Aloha. */
object SettingsTwoPane {
    private const val LARGE_SCREEN_OPT_PROPERTY = "persist.settings.large_screen_opt.enabled"
    private const val LARGE_SCREEN_OPT_FOR_DP_PROPERTY = "persist.settings.large_screen_opt_for_dp.enabled"
    private const val SUPPORT_LARGE_SCREEN_FLAG = "settings_support_large_screen"
    private const val FORCE_SINGLE_VIEW_FLAG = "flag_force_single_view"
    private val topLevelFeatureKeys = setOf(
        "flag_settings_support_large_screen", "feature_is_embedding_activity_enabled",
        "isSettingsSplitEnabled", "flag_user_setup_complete", FORCE_SINGLE_VIEW_FLAG,
    )

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun install(enabled: Boolean) {
        if (!enabled || param.packageName != Package.SETTINGS) return
        val loader = param.classLoader
        hookSystemProperties(xposedModule, loader, "android.os.SystemProperties")
        hookSystemProperties(xposedModule, loader, "android.os.SemSystemProperties")
        hookFeatureFlagUtils(xposedModule, loader)
        hookTopLevelFeature(xposedModule, loader)
        hookSettingsApplication(xposedModule, loader)
        hookActivityEmbeddingUtils(xposedModule, loader)
        hookSplitRuleConstructors(xposedModule, loader)
    }

    context(xposedModule: XposedModule)
    private fun hookSystemProperties(module: XposedModule, loader: ClassLoader, className: String) {
        runCatching {
            loader.loadClass(className).declaredMethods
                .filter { it.name == "getBoolean" && it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == String::class.java && it.parameterTypes[1] == Boolean::class.javaPrimitiveType }
                .forEach { method ->
                    module.hook(method).intercept { chain ->
                        val key = chain.args.getOrNull(0) as? String
                        if (key == LARGE_SCREEN_OPT_PROPERTY || key == LARGE_SCREEN_OPT_FOR_DP_PROPERTY) true else chain.proceed()
                    }
                }
        }.onFailure { xlog(it) }
    }

    context(xposedModule: XposedModule)
    private fun hookFeatureFlagUtils(module: XposedModule, loader: ClassLoader) {
        runCatching {
            loader.loadClass("android.util.FeatureFlagUtils").declaredMethods
                .filter { it.name == "isEnabled" && it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] == Context::class.java && it.parameterTypes[1] == String::class.java }
                .forEach { method ->
                    module.hook(method).intercept { chain ->
                        if (chain.args.getOrNull(1) == SUPPORT_LARGE_SCREEN_FLAG) true else chain.proceed()
                    }
                }
        }.onFailure { xlog(it) }
    }

    context(xposedModule: XposedModule)
    private fun hookTopLevelFeature(module: XposedModule, loader: ClassLoader) {
        runCatching {
            loader.loadClass("com.samsung.android.settings.homepage.SecTopLevelFeature").declaredMethods
                .filter { it.name == "getBoolean" && it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java }
                .forEach { method ->
                    module.hook(method).intercept { chain ->
                        val key = chain.args.getOrNull(0) as? String
                        if (key != null && key in topLevelFeatureKeys) key != FORCE_SINGLE_VIEW_FLAG else chain.proceed()
                    }
                }
        }.onFailure { xlog(it) }
    }

    context(xposedModule: XposedModule)
    private fun hookSettingsApplication(module: XposedModule, loader: ClassLoader) {
        runCatching {
            loader.loadClass("com.android.settings.SettingsApplication").declaredMethods
                .filter { it.name == "onCreate" && it.parameterCount == 0 }
                .forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        runCatching {
                            loader.loadClass("com.android.settings.activityembedding.ActivityEmbeddingUtils")
                                .getDeclaredField("SHOULD_ENABLE_LARGE_SCREEN_OPTIMIZATION").apply {
                                    isAccessible = true
                                    setBoolean(null, true)
                                }
                        }.onFailure { xlog(it) }
                        result
                    }
                }
        }.onFailure { xlog(it) }
    }

    context(xposedModule: XposedModule)
    private fun hookActivityEmbeddingUtils(module: XposedModule, loader: ClassLoader) {
        runCatching {
            loader.loadClass("com.android.settings.activityembedding.ActivityEmbeddingUtils").declaredMethods
                .filter { it.name == "isSettingsSplitEnabled" || it.name == "isEmbeddingActivityEnabled" }
                .forEach { method: Method -> module.hook(method).intercept { true } }
        }.onFailure { xlog(it) }
    }

    context(xposedModule: XposedModule)
    private fun hookSplitRuleConstructors(module: XposedModule, loader: ClassLoader) {
        runCatching {
            loader.loadClass("androidx.window.embedding.SplitRule").declaredConstructors
                .filter { constructor: Constructor<*> ->
                    val types=constructor.parameterTypes
                    types.size == 3 && types[0] == Int::class.javaPrimitiveType &&
                        types[1] == Int::class.javaPrimitiveType && types[2].name == "androidx.window.embedding.SplitAttributes"
                }
                .forEach { constructor ->
                    module.hook(constructor).intercept { chain ->
                        chain.args[0]=0
                        chain.args[1]=0
                        chain.proceed()
                    }
                }
        }.onFailure { xlog(it) }
    }
}
