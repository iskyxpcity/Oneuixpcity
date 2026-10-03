package io.github.soclear.oneuix.hook.util

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.hook.systemui.StatusBarPowerRuntime
import io.github.soclear.oneuix.hook.systemui.StatusBarWifiRuntime
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Flux-style SystemUI liveness probe.
 *
 * SystemUI never calls back into the OneuiXP app on a timer. It only exposes an
 * ordered PING receiver inside the already-injected SystemUI process. This
 * avoids keeping or resurrecting the manager app process after its task is
 * removed.
 */
object OneuiXPStatusReporter {
    private const val ACTION_PING = "com.oneuixp.app.PING.V2"
    private const val EXTRA_PROTOCOL = "com.oneuixp.app.PING_PROTOCOL"
    private const val PROTOCOL = "oneuixp-flux-v2"

    private val attachInstalled = AtomicBoolean(false)
    private val pingReceiverRegistered = AtomicBoolean(false)

    fun installIfSystemUi(
        module: XposedModule,
        param: XposedModuleInterface.PackageReadyParam,
        processName: String,
    ) {
        if (param.packageName != Package.SYSTEMUI || processName != Package.SYSTEMUI) return

        // Independent status-bar runtimes. They own their own event-driven preference
        // watchers so they do not change the locked LSPosed liveness or clock paths.
        StatusBarPowerRuntime.install(module, param)
        StatusBarWifiRuntime.install(module, param)

        if (!attachInstalled.compareAndSet(false, true)) return

        try {
            val attach = Application::class.java.getDeclaredMethod("attach", Context::class.java)
            module.hook(attach).intercept { chain ->
                val result = chain.proceed()
                runCatching {
                    val context = chain.args.firstOrNull() as? Context
                    if (context != null) registerPingReceiver(context)
                }
                result
            }
        } catch (_: Throwable) {
            // Keep SystemUI safe without writing LSPosed module logs.
        }
    }

    @Suppress("DEPRECATION")
    private fun registerPingReceiver(context: Context) {
        if (!pingReceiverRegistered.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_PING && isOrderedBroadcast) {
                    resultCode = Activity.RESULT_OK
                    val extras = getResultExtras(true) ?: Bundle()
                    extras.putString(EXTRA_PROTOCOL, PROTOCOL)
                    setResultExtras(extras)
                }
            }
        }
        val filter = IntentFilter(ACTION_PING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
    }
}
