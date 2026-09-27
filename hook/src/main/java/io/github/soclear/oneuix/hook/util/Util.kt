package io.github.soclear.oneuix.hook.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.libxposed.api.XposedModule
import java.io.File
import java.lang.reflect.Method

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
fun getSystemContext(): Context {
    val activityThreadClass = Class.forName("android.app.ActivityThread")
    val currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
    val currentActivityThread = currentActivityThreadMethod.invoke(null)
    val getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext")
    return getSystemContextMethod.invoke(currentActivityThread) as Context
}

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
fun currentApplication(): Application? {
    val activityThreadClass = Class.forName("android.app.ActivityThread")
    val currentApplicationMethod = activityThreadClass.getDeclaredMethod("currentApplication")
    return currentApplicationMethod.invoke(null) as? Application
}

fun currentContext(): Context = currentApplication() ?: getSystemContext()

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
fun getCurrentPackageName(): String {
    val activityThreadClass = Class.forName("android.app.ActivityThread")
    val currentPackageNameMethod = activityThreadClass.getDeclaredMethod("currentPackageName")
    return (currentPackageNameMethod.invoke(null) as? String) ?: ""
}

fun getPackageVersionCode(name: String = getCurrentPackageName()): Long {
    if (name.isEmpty()) return -1L
    return getSystemContext().packageManager.getPackageInfo(name, PackageManager.PackageInfoFlags.of(0)).longVersionCode
}

val Context.longVersionCode get() = packageManager.getPackageInfo(packageName, 0).longVersionCode

@SuppressLint("DiscouragedPrivateApi")
private val attachMethod: Method = Application::class.java.getDeclaredMethod("attach", Context::class.java)

context(xposedModule: XposedModule)
fun afterAttach(action: Context.() -> Unit) {
    xposedModule.hook(attachMethod).intercept { chain ->
        val result = chain.proceed()
        action(chain.args[0] as Context)
        result
    }
}

context(xposedModule: XposedModule)
fun afterAttachTry(canRunAction: Boolean = true, action: Context.() -> Unit) {
    if (!canRunAction) return

    val caller = Throwable().stackTrace.firstOrNull { frame ->
        val name = frame.className
        name != "io.github.soclear.oneuix.hook.util.UtilKt" &&
            !name.startsWith("io.github.soclear.oneuix.hook.util.UtilKt$")
    }
    val installKey = caller?.let {
        "${it.className}#${it.methodName}:${it.lineNumber}"
    } ?: "unknown-hook-installer"

    afterAttach {
        try {
            action()
            HookInstallTracker.markSuccessful(installKey)
        } catch (t: Throwable) {
            xlog(t)
        }
    }
}

// 向宿主添加资源，路径为apk文件路径。例如添加模块的资源
context(xposedModule: XposedModule)
fun addAssetPath(modulePath: String) {
    val method = ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
    xposedModule.hook(method).intercept { chain ->
        val result = chain.proceed()
        val context = chain.thisObject as Context
        if (context is Application) {
            try {
                val moduleApk = File(modulePath)
                val parcelFileDescriptor = ParcelFileDescriptor.open(moduleApk, ParcelFileDescriptor.MODE_READ_ONLY)
                val resourcesProvider = ResourcesProvider.loadFromApk(parcelFileDescriptor)
                val resourcesLoader = ResourcesLoader()
                resourcesLoader.addProvider(resourcesProvider)
                context.resources.addLoaders(resourcesLoader)
            } catch (_: Throwable) {
                // OneuiXP does not continuously emit LSPosed module logs.
            }
        }
        result
    }
}

/*
三星 ROM 的 persist.log.semlevel = 0xFFFFFF00 会屏蔽进程名包含 .sec 、.samsung 的 VERBOSE/DEBUG
所以请使用 ASSERT/ERROR/INFO/WARN

tag 传入 null，框架会自动赋予默认 Tag（Vector 为 "VectorContext"，LSPosed 为 "LSPosedContext"）
这样才能命中 Vector/LSPosed 守护进程的 Tag 白名单，同时避免自定义 Tag 被过滤
 */
context(xposedModule: XposedModule)
fun xlog(
    message: Any?,
    throwable: Throwable? = null,
    priority: Int = Log.ERROR,
) {
    // Intentionally silent. OneuiXP diagnostics are generated on demand from Settings.
}
