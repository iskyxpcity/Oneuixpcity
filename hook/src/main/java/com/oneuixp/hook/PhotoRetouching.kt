package com.oneuixp.hook

import android.content.Context
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.oneuixp.common.Package
import com.oneuixp.hook.util.HookConfig
import com.oneuixp.hook.util.afterAttach
import com.oneuixp.hook.util.getHookConfig
import com.oneuixp.hook.util.longVersionCode
import com.oneuixp.hook.util.xlog
import kotlinx.serialization.Serializable
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Modifier

object PhotoRetouching {
    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun noAIWatermark() {
        if (param.packageName != Package.PHOTO_RETOUCHING) return
        afterAttach {
            val hookConfig = getHookConfig { getHookConfigFromDexKit() }
            if (hookConfig != null) {
                try {
                    val methodInstance =
                        DexMethod(hookConfig.saveWatermarkMethod).getMethodInstance(classLoader)
                    xposedModule.hook(methodInstance).intercept { null }
                } catch (t: Throwable) {
                    xlog(t)
                }
            }
        }
    }

    @Serializable
    private data class PhotoRetouchingHookConfig(
        override val versionCode: Long,
        val saveWatermarkMethod: String,
    ) : HookConfig

    private fun Context.getHookConfigFromDexKit(): PhotoRetouchingHookConfig? {
        System.loadLibrary("dexkit")
        DexKitBridge.create(classLoader, true).use { bridge ->
            val saveWatermarkMethodUsingStrings = listOf(
                "SPE_CommonUtil",
                "getWatermarkBitmap : requiredSize = ",
                "saveWatermark : canvas shortAxis = ",
            )
            val saveWatermarkMethod = bridge.findClass {
                excludePackages(
                    "android",
                    "androidx",
                    "appfunctions_aggregated_deps",
                    "co",
                    "com",
                    "io",
                    "kotlin",
                    "org"
                )
                matcher {
                    modifiers = Modifier.PUBLIC or Modifier.FINAL
                    usingStrings = saveWatermarkMethodUsingStrings
                }
            }.findMethod {
                matcher {
                    usingStrings = saveWatermarkMethodUsingStrings
                }
            }.singleOrNull() ?: return null

            return PhotoRetouchingHookConfig(
                versionCode = longVersionCode,
                saveWatermarkMethod = saveWatermarkMethod.toDexMethod().serialize(),
            )
        }
    }
}
