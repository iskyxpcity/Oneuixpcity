package com.alohaoneui.hook

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.alohaoneui.common.Package
import com.alohaoneui.hook.util.xlog

object Calendar {
    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun enableChineseHolidayDisplay() {
        if (param.packageName != Package.CALENDAR) return

        try {
            val semCscFeatureClass =
                param.classLoader.loadClass("com.samsung.android.feature.SemCscFeature")

            xposedModule.hook(
                semCscFeatureClass.getDeclaredMethod("getString", String::class.java, String::class.java)
            ).intercept { chain ->
                val result = chain.proceed()
                if (chain.args[0] == "CscFeature_Calendar_EnableLocalHolidayDisplay") {
                    "CHINA"
                } else {
                    result
                }
            }

            xposedModule.hook(
                semCscFeatureClass.getDeclaredMethod("getString", String::class.java)
            ).intercept { chain ->
                val result = chain.proceed()
                if (chain.args[0] == "CscFeature_Calendar_EnableLocalHolidayDisplay") {
                    "CHINA"
                } else {
                    result
                }
            }
        } catch (t: Throwable) {
            xlog(t)
        }
    }
}
