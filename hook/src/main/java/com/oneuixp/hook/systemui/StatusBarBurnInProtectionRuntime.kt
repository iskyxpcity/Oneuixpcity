package com.oneuixp.hook.systemui

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
import com.oneuixp.common.IgnoreUnknownKeysJson
import com.oneuixp.common.Package
import com.oneuixp.common.Preference
import com.oneuixp.common.StatusBarRuntimeBridge
import com.oneuixp.hook.util.PreferenceProvider
import com.oneuixp.hook.util.OneuiXPDiagnosticLog
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

    private data class Target(
        val owner: WeakReference<Any>,
        val apply: (Any, Float, Float) -> Unit,
    )

    private data class ViewBaseline(
        var x: Float,
        var y: Float,
        var lastWrittenX: Float = x,
        var lastWrittenY: Float = y,
    )

    /**
     * Watches native layout/attachment lifecycle for translated OneuiXP TextViews.
     * All View references reachable from this WeakHashMap's values are weak.
     */
    private data class ViewGeometryWatch(
        var parent: WeakReference<View>? = null,
        var viewLayoutListener: View.OnLayoutChangeListener? = null,
        var parentLayoutListener: View.OnLayoutChangeListener? = null,
        var attachListener: View.OnAttachStateChangeListener? = null,
    )

    private val installed = AtomicBoolean(false)
    private val receiverReady = AtomicBoolean(false)
    private val targets = Collections.synchronizedList(mutableListOf<Target>())
    private val viewBaselines = Collections.synchronizedMap(WeakHashMap<View, ViewBaseline>())
    private val viewGeometryWatches =
        Collections.synchronizedMap(WeakHashMap<View, ViewGeometryWatch>())
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
            main.postDelayed(this, nextTickDelayMs())
        }
    }

    /**
     * Sample four positions across the selected horizontal micro-shift cycle instead of
     * continuously waking the main thread every 250 ms. The existing period setting controls
     * the cadence; no separate fixed refresh interval is introduced.
     */
    private fun nextTickDelayMs(): Long =
        periodSeconds.coerceIn(10, 300).toLong() * 1000L / 4L

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

    /**
     * Network speed is placed immediately before the three-in-one glyph. It may micro-shift
     * left with the glyph, but must not independently drift right into the glyph's draw area.
     * Vertical motion remains synchronized; the glyph can still complete the rightward half
     * of its cycle while the speed text stays clear.
     */
    fun registerView(view: View) {
        // Keep the network-speed text from drifting right into the three-in-one glyph.
        registerOffsetTarget(view) { owner, xDp, yDp ->
            applyViewOffset(owner as View, minOf(xDp, 0f), yDp)
        }
        // Fold/unfold can reset View.translationX/Y without delivering a preference or
        // text update. Re-apply from lifecycle events rather than waiting for the next
        // quarter-period ticker sample (up to 75 seconds for a 300-second cycle).
        runOnMain { ensureViewGeometryWatch(view) }
    }

    fun registerOffsetTarget(owner: Any, apply: (Any, Float, Float) -> Unit) {
        runOnMain {
            val target = synchronized(targets) {
                targets.removeAll { it.owner.get() == null }
                targets.firstOrNull { it.owner.get() === owner }
                    ?: Target(WeakReference(owner), apply).also { targets += it }
            }
            val offset = currentOffset()
            runCatching { target.apply(owner, offset.first, offset.second) }
                .onFailure { Log.d(TAG, "Initial custom target offset failed", it) }
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
            if (removed.isEmpty()) return@runOnMain
            removed.forEach { target ->
                runCatching { target.apply(owner, 0f, 0f) }
                    .onFailure { Log.d(TAG, "Cannot restore custom target", it) }
            }
            if (owner is View) {
                synchronized(viewBaselines) { viewBaselines.remove(owner) }
                removeViewGeometryWatch(owner)
            }
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

    private fun ensureViewGeometryWatch(view: View) {
        val watch = synchronized(viewGeometryWatches) {
            viewGeometryWatches[view] ?: ViewGeometryWatch().also {
                viewGeometryWatches[view] = it
            }
        }

        if (watch.viewLayoutListener == null) {
            val weakView = WeakReference(view)
            val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                weakView.get()?.let { target ->
                    ensureParentGeometryWatch(target, watch)
                    reapplyViewOffsetPreservingBaseline(target, "view-layout")
                }
            }
            watch.viewLayoutListener = listener
            view.addOnLayoutChangeListener(listener)
        }

        ensureParentGeometryWatch(view, watch)

        if (watch.attachListener == null) {
            val weakView = WeakReference(view)
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(attached: View) {
                    weakView.get()?.let { target ->
                        ensureParentGeometryWatch(target, watch)
                        reapplyViewOffsetPreservingBaseline(target, "view-attach")
                    }
                    main.post {
                        weakView.get()?.let { target ->
                            ensureParentGeometryWatch(target, watch)
                            reapplyViewOffsetPreservingBaseline(target, "view-attach-post")
                        }
                    }
                    main.postDelayed({
                        weakView.get()?.let { target ->
                            ensureParentGeometryWatch(target, watch)
                            reapplyViewOffsetPreservingBaseline(target, "view-attach-settled")
                        }
                    }, 100L)
                }

                override fun onViewDetachedFromWindow(detached: View) = Unit
            }
            watch.attachListener = listener
            view.addOnAttachStateChangeListener(listener)
        }
    }

    private fun ensureParentGeometryWatch(view: View, watch: ViewGeometryWatch) {
        val parent = view.parent as? View
        val oldParent = watch.parent?.get()
        if (parent === oldParent &&
            (parent == null || watch.parentLayoutListener != null)
        ) {
            return
        }

        watch.parentLayoutListener?.let { listener ->
            oldParent?.removeOnLayoutChangeListener(listener)
        }
        watch.parent = parent?.let(::WeakReference)
        watch.parentLayoutListener = null

        if (parent != null) {
            val weakView = WeakReference(view)
            val weakParent = WeakReference(parent)
            val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                val target = weakView.get()
                val sourceParent = weakParent.get()
                if (target != null && sourceParent != null && target.parent === sourceParent) {
                    reapplyViewOffsetPreservingBaseline(target, "parent-layout")
                }
            }
            watch.parentLayoutListener = listener
            parent.addOnLayoutChangeListener(listener)
        }
    }

    private fun removeViewGeometryWatch(view: View) {
        val watch = synchronized(viewGeometryWatches) { viewGeometryWatches.remove(view) }
            ?: return
        watch.viewLayoutListener?.let(view::removeOnLayoutChangeListener)
        watch.parentLayoutListener?.let { listener ->
            watch.parent?.get()?.removeOnLayoutChangeListener(listener)
        }
        watch.attachListener?.let(view::removeOnAttachStateChangeListener)
    }

    /**
     * Unlike applyViewOffset(), this deliberately does not infer a new baseline from the
     * current translation. On fold/unfold, SystemUI may reset the native View translation
     * to zero; treating that reset as an intentional OneuiXP position change makes the
     * element jump back to its original location. The last known custom baseline is retained.
     */
    private fun reapplyViewOffsetPreservingBaseline(view: View, source: String) {
        val baseline = synchronized(viewBaselines) { viewBaselines[view] } ?: return
        val registered = synchronized(targets) {
            targets.any { it.owner.get() === view }
        }
        if (!registered) return

        val (xDp, yDp) = currentOffset()
        val density = view.resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        val targetX = baseline.x + minOf(xDp, 0f) * density
        val targetY = baseline.y + yDp * density
        val beforeX = view.translationX
        val beforeY = view.translationY
        if (abs(beforeX - targetX) > 0.01f) view.translationX = targetX
        if (abs(beforeY - targetY) > 0.01f) view.translationY = targetY
        baseline.lastWrittenX = targetX
        baseline.lastWrittenY = targetY

        if (abs(beforeX - targetX) > 0.5f || abs(beforeY - targetY) > 0.5f) {
            OneuiXPDiagnosticLog.record(
                TAG,
                "Restored custom View micro-offset after $source: " +
                    "before=($beforeX,$beforeY),target=($targetX,$targetY)," +
                    "baseline=(${baseline.x},${baseline.y}),density=$density"
            )
        }
    }

    private fun applyViewOffset(view: View, xDp: Float, yDp: Float) {
        val base = synchronized(viewBaselines) {
            viewBaselines.getOrPut(view) {
                ViewBaseline(view.translationX, view.translationY)
            }
        }
        // Custom OneuiXP layout code writes its intended translation without our micro-shift.
        // Detect that change and accept it as the new base before applying the offset again.
        if (abs(view.translationX - base.lastWrittenX) > 0.01f) base.x = view.translationX
        if (abs(view.translationY - base.lastWrittenY) > 0.01f) base.y = view.translationY
        val density = view.resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        val targetX = base.x + xDp * density
        val targetY = base.y + yDp * density
        if (abs(view.translationX - targetX) > 0.01f) view.translationX = targetX
        if (abs(view.translationY - targetY) > 0.01f) view.translationY = targetY
        base.lastWrittenX = targetX
        base.lastWrittenY = targetY
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }
}
