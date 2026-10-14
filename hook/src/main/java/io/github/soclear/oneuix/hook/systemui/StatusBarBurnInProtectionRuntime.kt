package io.github.soclear.oneuix.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import kotlinx.serialization.decodeFromString
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Micro-shift only views/overlays owned by OneuiXP. Samsung-native status-bar views are never
 * registered: their built-in burn-in behavior cannot be inferred safely from a One UI version.
 */
internal object StatusBarBurnInProtectionRuntime {
    private const val TAG = "OneuiXPBurnIn"
    private const val PHONE_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBarView"
    private const val MIUI_PHONE_BAR = "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"
    private const val TICK_MS = 250L

    private data class Target(
        val owner: WeakReference<Any>,
        val apply: (Any, Float, Float) -> Unit,
    )

    private data class ViewBaseline(
        var x: Float,
        var y: Float,
        var lastOffsetX: Float = 0f,
        var lastOffsetY: Float = 0f,
        var lastWrittenX: Float = x,
        var lastWrittenY: Float = y,
    )

    private val installed = AtomicBoolean(false)
    private val receiverReady = AtomicBoolean(false)
    private val targets = Collections.synchronizedList(mutableListOf<Target>())
    private val viewBaselines = Collections.synchronizedMap(WeakHashMap<View, ViewBaseline>())
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var enabled = false
    @Volatile private var amplitudeDp = 0.50f
    @Volatile private var periodSeconds = 60
    @Volatile private var startedAtMs = 0L
    @Volatile private var shadeBusy = false
    private var tickerScheduled = false
    private var receiver: BroadcastReceiver? = null

    private val applyToView: (Any, Float, Float) -> Unit = { owner, x, y ->
        applyViewOffset(owner as View, x, y)
    }

    private val ticker = object : Runnable {
        override fun run() {
            tickerScheduled = false
            if (!isMotionActive()) {
                applyOffsetToTargets(0f, 0f)
                return
            }
            val elapsed = (SystemClock.elapsedRealtime() - startedAtMs).coerceAtLeast(0L).toDouble()
            val periodMs = periodSeconds.coerceIn(10, 300) * 1000.0
            // Keep total displacement within the selected amplitude while avoiding a short
            // repeating X/Y cycle.
            val axisAmplitude = amplitudeDp.coerceIn(0f, 2f).toDouble() / sqrt(2.0)
            val x = (axisAmplitude * sin(2.0 * PI * elapsed / periodMs)).toFloat()
            val y = (axisAmplitude * sin(2.0 * PI * elapsed / (periodMs * sqrt(2.0)))).toFloat()
            applyOffsetToTargets(x, y)
            tickerScheduled = true
            main.postDelayed(this, TICK_MS)
        }
    }

    fun install(module: XposedModule, param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != Package.SYSTEMUI || !installed.compareAndSet(false, true)) return
        with(module) { PreferenceProvider.loadPreference() }
            ?.systemUI?.statusBar?.let(::configure)

        listOf(PHONE_BAR, MIUI_PHONE_BAR).forEach { className ->
            runCatching {
                val hostClass = param.classLoader.loadClass(className)
                hostClass.declaredConstructors.forEach { constructor ->
                    module.hook(constructor).intercept { chain ->
                        val result = chain.proceed()
                        (chain.thisObject as? ViewGroup)?.let { root ->
                            root.post { ensureReceiver(module, root.context) }
                        }
                        result
                    }
                }
            }.onFailure { Log.i(TAG, "Status-bar host unavailable: $className") }
        }
        hookShadeExpansion(module, param.classLoader)
    }

    fun registerView(view: View) = registerOffsetTarget(view, applyToView)

    fun registerOffsetTarget(owner: Any, apply: (Any, Float, Float) -> Unit) {
        runOnMain {
            synchronized(targets) {
                targets.removeAll { it.owner.get() == null }
                if (targets.none { it.owner.get() === owner }) {
                    targets += Target(WeakReference(owner), apply)
                }
            }
            val offset = currentOffset()
            synchronized(targets) { targets.firstOrNull { it.owner.get() === owner } }
                ?.let { target ->
                    runCatching { target.apply(owner, offset.first, offset.second) }
                        .onFailure { Log.d(TAG, "Initial custom target offset failed", it) }
                }
            syncTicker()
        }
    }

    fun unregisterOffsetTarget(owner: Any) {
        runOnMain {
            val removed = synchronized(targets) {
                val matching = targets.filter { it.owner.get() === owner }
                targets.removeAll { it.owner.get() == null || it.owner.get() === owner }
                matching
            }
            removed.forEach { target ->
                runCatching { target.apply(owner, 0f, 0f) }
                    .onFailure { Log.d(TAG, "Cannot restore custom target", it) }
            }
            if (owner is View) synchronized(viewBaselines) { viewBaselines.remove(owner) }
            syncTicker()
        }
    }

    private fun ensureReceiver(module: XposedModule, context: Context) {
        if (!receiverReady.compareAndSet(false, true)) return
        val app = context.applicationContext ?: context
        val nextReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != StatusBarRuntimeBridge.ACTION) return
                val payload = intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON) ?: return
                runCatching {
                    val status = IgnoreUnknownKeysJson.decodeFromString(
                        Preference.SystemUI.StatusBar.serializer(), payload,
                    )
                    configure(status)
                }.onFailure { Log.w(TAG, "Cannot read burn-in settings broadcast", it) }
            }
        }
        receiver = nextReceiver
        runCatching {
            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION)
            if (Build.VERSION.SDK_INT >= 33) {
                app.registerReceiver(nextReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                app.registerReceiver(nextReceiver, filter)
            }
        }.onFailure {
            receiverReady.set(false)
            Log.w(TAG, "Cannot register burn-in settings receiver", it)
        }
    }

    private fun configure(status: Preference.SystemUI.StatusBar) {
        val wasEnabled = enabled
        enabled = status.statusBarBurnInProtectionEnabled
        amplitudeDp = status.statusBarBurnInProtectionAmplitudeDp.coerceIn(0f, 2f)
        periodSeconds = status.statusBarBurnInProtectionPeriodSeconds.coerceIn(10, 300)
        if (!wasEnabled && enabled) startedAtMs = SystemClock.elapsedRealtime()
        runOnMain { syncTicker() }
    }

    private fun hookShadeExpansion(module: XposedModule, classLoader: ClassLoader) {
        listOf(
            "com.android.systemui.shade.NotificationPanelViewControllerInjector" to "setExpandedHeightInternal",
            "com.android.systemui.shade.NotificationPanelViewController" to "setExpandedHeight",
        ).forEach { (className, methodName) ->
            runCatching {
                var current: Class<*>? = classLoader.loadClass(className)
                val candidates = mutableListOf<java.lang.reflect.Method>()
                while (current != null) {
                    candidates += current.declaredMethods.filter { method ->
                        method.name == methodName && method.parameterTypes.size == 1 &&
                            method.parameterTypes[0] == Float::class.javaPrimitiveType
                    }
                    current = current.superclass
                }
                candidates.distinctBy { it.toGenericString() }.forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        val height = chain.args.firstOrNull() as? Float
                        if (height != null) {
                            val wasBusy = shadeBusy
                            shadeBusy = height > 0.5f
                            if (wasBusy && !shadeBusy) startedAtMs = SystemClock.elapsedRealtime()
                            runOnMain { syncTicker() }
                        }
                        result
                    }
                }
            }.onFailure { Log.i(TAG, "Shade-state hook unavailable: $className.$methodName") }
        }
    }

    private fun isMotionActive(): Boolean =
        enabled && amplitudeDp > 0f && !shadeBusy && synchronized(targets) {
            targets.any { it.owner.get() != null }
        }

    private fun syncTicker() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { syncTicker() }
            return
        }
        if (!isMotionActive()) {
            main.removeCallbacks(ticker)
            tickerScheduled = false
            applyOffsetToTargets(0f, 0f)
        } else if (!tickerScheduled) {
            tickerScheduled = true
            main.post(ticker)
        }
    }

    private fun currentOffset(): Pair<Float, Float> {
        if (!enabled || amplitudeDp <= 0f || shadeBusy) return 0f to 0f
        val elapsed = (SystemClock.elapsedRealtime() - startedAtMs).coerceAtLeast(0L).toDouble()
        val periodMs = periodSeconds.coerceIn(10, 300) * 1000.0
        val axisAmplitude = amplitudeDp.coerceIn(0f, 2f).toDouble() / sqrt(2.0)
        return (axisAmplitude * sin(2.0 * PI * elapsed / periodMs)).toFloat() to
            (axisAmplitude * sin(2.0 * PI * elapsed / (periodMs * sqrt(2.0)))).toFloat()
    }

    private fun applyOffsetToTargets(x: Float, y: Float) {
        val snapshot = synchronized(targets) {
            targets.removeAll { it.owner.get() == null }
            targets.toList()
        }
        snapshot.forEach { target ->
            target.owner.get()?.let { owner ->
                runCatching { target.apply(owner, x, y) }
                    .onFailure { Log.d(TAG, "Cannot move OneuiXP status-bar element", it) }
            }
        }
    }

    private fun applyViewOffset(view: View, xDp: Float, yDp: Float) {
        val base = synchronized(viewBaselines) {
            viewBaselines.getOrPut(view) {
                ViewBaseline(view.translationX, view.translationY)
            }
        }
        // If another OneuiXP setting writes a new translation, reconstruct its unshifted
        // baseline by removing our previous contribution before applying the new offset.
        if (abs(view.translationX - base.lastWrittenX) > 0.01f) {
            base.x = view.translationX - base.lastOffsetX
        }
        if (abs(view.translationY - base.lastWrittenY) > 0.01f) {
            base.y = view.translationY - base.lastOffsetY
        }
        val density = view.resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        val offsetX = xDp * density
        val offsetY = yDp * density
        val targetX = base.x + offsetX
        val targetY = base.y + offsetY
        if (abs(view.translationX - targetX) > 0.01f) view.translationX = targetX
        if (abs(view.translationY - targetY) > 0.01f) view.translationY = targetY
        base.lastOffsetX = offsetX
        base.lastOffsetY = offsetY
        base.lastWrittenX = targetX
        base.lastWrittenY = targetY
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }
}
