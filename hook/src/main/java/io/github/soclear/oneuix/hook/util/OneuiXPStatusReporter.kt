package io.github.soclear.oneuix.hook.util

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.os.Bundle
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.BuildConfig
import io.github.soclear.oneuix.common.Package
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OneuiXP's existing SystemUI liveness bridge, moved into the single API102 entry.
 * This code reports only; it does not mutate SystemUI views or enable OneUIX features.
 */
object OneuiXPStatusReporter {
    private val attachInstalled = AtomicBoolean(false)
    private val heartbeatStarted = AtomicBoolean(false)

    fun installIfSystemUi(
        module: XposedModule,
        param: XposedModuleInterface.PackageReadyParam,
        processName: String,
    ) {
        if (param.packageName != Package.SYSTEMUI || processName != Package.SYSTEMUI) return
        if (!attachInstalled.compareAndSet(false, true)) return

        try {
            val attach = Application::class.java.getDeclaredMethod("attach", Context::class.java)
            module.hook(attach).intercept { chain ->
                val result = chain.proceed()
                try {
                    val context = chain.args.firstOrNull() as? Context
                    if (context != null) {
                        reportLoaded(context, param.classLoader)
                        startHeartbeat(context)
                    }
                } catch (t: Throwable) {
                    module.log(Log.ERROR, null, "OneuiXP status reporter", t)
                }
                result
            }
        } catch (t: Throwable) {
            module.log(Log.ERROR, null, "OneuiXP status reporter", t)
        }
    }

    private fun bridgeUri(): Uri =
        Uri.parse("content://${BuildConfig.MODULE_APPLICATION_ID}.statusbridge")

    private fun reportLoaded(context: Context, loader: ClassLoader) {
        runCatching {
            val extras = Bundle().apply { putString("probe", probeDetails(loader)) }
            context.contentResolver.call(bridgeUri(), "loaded", null, extras)
        }
    }

    private fun startHeartbeat(context: Context) {
        if (!heartbeatStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "OneuiXP-SystemUI-heartbeat").apply { isDaemon = true }
        }
        executor.scheduleWithFixedDelay({
            runCatching {
                val response = appContext.contentResolver.call(
                    bridgeUri(),
                    "heartbeat",
                    null,
                    null,
                )
                if (response == null || !response.getBoolean("accepted", false)) {
                    val retry = Bundle().apply {
                        putString("probe", "LOAD_RETRY: original probe unavailable")
                    }
                    appContext.contentResolver.call(bridgeUri(), "loaded", null, retry)
                }
            }
        }, 0L, 5L, TimeUnit.SECONDS)
    }

    private fun probeDetails(loader: ClassLoader): String {
        val names = arrayOf(
            "com.android.systemui.statusbar.phone.PhoneStatusBarView",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy",
            "com.android.systemui.battery.BatteryMeterView",
            "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView",
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView",
        )
        val out = StringBuilder("API102 READ_ONLY\n")
        names.forEach { name ->
            out.append("\n").append(name).append("\n")
            try {
                val type = Class.forName(name, false, loader)
                out.append("FOUND parent=")
                    .append(type.superclass?.name ?: "none")
                    .append("\n")
                out.append("constructors=").append(type.declaredConstructors.size).append("\n")
                out.append("fields(").append(type.declaredFields.size).append("):")
                type.declaredFields
                    .sortedBy { it.name }
                    .asSequence()
                    .filter {
                        val n = it.name.lowercase()
                        n.contains("icon") || n.contains("view") || n.contains("container") ||
                            n.contains("battery") || n.contains("wifi") || n.contains("mobile") ||
                            n.contains("signal") || n.contains("state")
                    }
                    .take(10)
                    .forEach { out.append(" ").append(it.name).append(":").append(it.type.simpleName) }
                out.append("\n")
                out.append("methods(").append(type.declaredMethods.size).append("):")
                type.declaredMethods
                    .sortedBy { it.name }
                    .asSequence()
                    .filter {
                        val n = it.name.lowercase()
                        n.contains("attach") || n.contains("inflate") || n.contains("update") ||
                            n.contains("bind") || n.contains("icon") || n.contains("state") ||
                            n.contains("battery") || n.contains("wifi") || n.contains("mobile") ||
                            n.contains("signal") || n.contains("visible")
                    }
                    .take(12)
                    .forEach { method ->
                        out.append(" ").append(method.name).append("(")
                        method.parameterTypes.take(3).forEachIndexed { index, type ->
                            if (index > 0) out.append(",")
                            out.append(type.simpleName)
                        }
                        if (method.parameterTypes.size > 3) out.append(",...")
                        out.append(")")
                    }
                out.append("\n")
            } catch (t: Throwable) {
                out.append("NOT_FOUND_OR_REFLECTION_ERROR ")
                    .append(t.javaClass.simpleName)
                    .append("\n")
            }
        }
        return if (out.length > 12_000) out.substring(0, 12_000) + "\nTRUNCATED" else out.toString()
    }
}
