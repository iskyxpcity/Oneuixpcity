package io.github.soclear.oneuix.hook

import android.view.View
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import io.github.soclear.oneuix.hook.util.xlog

/**
 * Hides Samsung's post-install recommendation panel in the Samsung installer.
 * The toggle is read from the shared preference file at call time.
 */
object PackageInstallerHooks {
    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun hideRecommendations() {
        if (param.packageName != Package.PACKAGE_INSTALLER &&
            param.packageName != Package.SAMSUNG_PACKAGE_INSTALLER
        ) return

        val recommendClass = runCatching {
            param.classLoader.loadClass("com.samsung.android.packageinstaller.chinafeature.ui.RecommendAdView")
        }.onFailure { xlog(it) }.getOrNull() ?: return

        fun enabled(): Boolean = with(xposedModule) {
            PreferenceProvider.loadPreference()?.other?.hidePackageInstallerRecommendationEnabled == true
        }

        recommendClass.declaredConstructors.forEach { constructor ->
            runCatching {
                xposedModule.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    if (enabled()) (chain.thisObject as? View)?.visibility = View.GONE
                    result
                }
            }.onFailure { xlog(it) }
        }

        runCatching {
            val setData = recommendClass.getDeclaredMethod("setData", List::class.java)
            xposedModule.hook(setData).intercept { chain ->
                if (!enabled()) return@intercept chain.proceed()
                val args = chain.args.toMutableList()
                if (args.isNotEmpty()) args[0] = emptyList<Any>()
                val result = chain.proceed(args.toTypedArray())
                (chain.thisObject as? View)?.visibility = View.GONE
                result
            }
        }.onFailure { xlog(it) }
    }
}
