package io.github.soclear.oneuix.hook

import android.view.View
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.hook.util.xlog

/** Requested post-install recommendation hook ported from App20261018Aloha. */
object PackageInstallerRecommendation {
    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun hideRecommendations(enabled: Boolean) {
        if (!enabled) return
        if (param.packageName != Package.PACKAGE_INSTALLER && param.packageName != Package.AOSP_PACKAGE_INSTALLER) return
        runCatching {
            val recommendView=param.classLoader.loadClass("com.samsung.android.packageinstaller.chinafeature.ui.RecommendAdView")
            recommendView.declaredConstructors.forEach { constructor ->
                xposedModule.hook(constructor).intercept { chain ->
                    val result=chain.proceed()
                    (chain.thisObject as? View)?.visibility=View.GONE
                    result
                }
            }
            recommendView.declaredMethods
                .filter { it.name=="setData" && it.parameterTypes.size==1 &&
                    java.util.List::class.java.isAssignableFrom(it.parameterTypes[0]) }
                .forEach { method ->
                    xposedModule.hook(method).intercept { chain ->
                        chain.args[0]=emptyList<Any>()
                        val result=chain.proceed()
                        (chain.thisObject as? View)?.visibility=View.GONE
                        result
                    }
                }
        }.onFailure { xlog(it) }
    }
}
