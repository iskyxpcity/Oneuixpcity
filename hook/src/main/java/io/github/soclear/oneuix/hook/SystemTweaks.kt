package io.github.soclear.oneuix.hook

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.hook.util.xlog

/** Requested system_server tweaks ported from App20261018Aloha. */
object SystemTweaks {
    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun hookAllowAppDowngrade(enabled: Boolean) {
        if (!enabled || param.packageName != Package.ANDROID) return
        runCatching {
            param.classLoader.loadClass("com.android.server.pm.PackageManagerServiceUtils")
                .declaredMethods.filter { it.name == "checkDowngrade" }.forEach { method ->
                    xposedModule.hook(method).intercept {
                        // The source hook bypasses this void check to suppress downgrade exceptions.
                        null
                    }
                }
        }.onFailure { xlog(it) }
    }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun hookSecureScreenshotRestriction(enabled: Boolean) {
        if (!enabled || param.packageName != Package.ANDROID) return
        runCatching {
            param.classLoader.loadClass("com.android.server.wm.WindowState").declaredMethods
                .filter { it.name == "isSecureLocked" && it.parameterCount == 0 }
                .forEach { method -> xposedModule.hook(method).intercept { false } }
        }.onFailure { xlog(it) }
    }
}
