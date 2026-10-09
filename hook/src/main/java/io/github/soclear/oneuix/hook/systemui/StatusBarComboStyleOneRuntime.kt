package io.github.soclear.oneuix.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import java.util.LinkedHashMap
import java.util.WeakHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * OneuiXP Three-in-One Icon Style One.
 *
 * A single, independently positioned status-bar window renders the battery ring,
 * Wi-Fi arcs and active-data-SIM signal. Updates are event driven; there is no
 * periodic visibility or layout polling.
 */
internal object StatusBarComboStyleOneRuntime {
    private const val PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.PhoneStatusBarView"
    private const val PREFERENCE_CHANGED_URI =
        "content://com.oneuixp.app.statusbridge/preferences"
    private const val TAG = "OneuiXP.StyleOne"

    private data class SignalState(
        val subscriptionId: Int,
        val slotIndex: Int = -1,
        val level: Int = 0,
        val inService: Boolean = false,
        val is5g: Boolean = false,
        val networkType: String = "",
    )

    private data class HiddenState(
        val alpha: Float,
        val visibility: Int,
    )

    private data class SuppressionKey(
        val enabled: Boolean,
        val showWifi: Boolean,
        val showMobile: Boolean,
        val showBattery: Boolean,
        val signalMode: Int,
        val stackedSignal: Boolean,
        val mobileTypeMode: Int,
    )

    private data class Metrics(
        val width: Int,
        val height: Int,
        val side: Float,
        val ringCenterX: Float,
        val wifiWidth: Float,
        val signalWidth: Float,
        val typeWidth: Float,
        val gap: Float,
    )

    private class Holder(val statusBarView: ViewGroup) {
        val hiddenNative = LinkedHashMap<View, HiddenState>()
        var batteryAnchor: View? = null
        var overlay: OverlayWindow? = null
        var suppressionKey: SuppressionKey? = null
    }

    private class StyleOneIconView(context: Context) : View(context) {
        private var runtime = Preference.SystemUI.StatusBarComboStyleOneRuntime()
        private var tint = Color.WHITE

        fun update(next: Preference.SystemUI.StatusBarComboStyleOneRuntime, nextTint: Int) {
            runtime = next
            tint = nextTint
            val metrics = calculateMetrics(context, next)
            val params = layoutParams
            if (params != null && (params.width != metrics.width || params.height != metrics.height)) {
                params.width = metrics.width
                params.height = metrics.height
                layoutParams = params
            }
            invalidate()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val metrics = calculateMetrics(context, runtime)
            setMeasuredDimension(metrics.width, metrics.height)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!runtime.enabled) return

            val metrics = calculateMetrics(context, runtime)
            val save = canvas.save()
            StatusBarComboStyleOneRenderer.draw(
                canvas,
                width,
                height,
                runtime,
                StatusBarComboStyleOneRenderer.State(
                    batteryLevel = StatusBarComboStyleOneRuntime.batteryLevel,
                    charging = StatusBarComboStyleOneRuntime.charging,
                    quickCharging = StatusBarComboStyleOneRuntime.quickCharging,
                    powerSave = StatusBarComboStyleOneRuntime.powerSave,
                    lowBattery = StatusBarComboStyleOneRuntime.batteryLevel in
                        0..runtime.lowBatteryThreshold,
                    wifiLevel = if (StatusBarComboStyleOneRuntime.wifiConnected) {
                        StatusBarComboStyleOneRuntime.wifiLevel
                    } else {
                        0
                    },
                    mobileLevel = StatusBarComboStyleOneRuntime.currentSignalLevel(),
                    slotLevels = StatusBarComboStyleOneRuntime.currentSlotLevels(),
                    dataSlot = StatusBarComboStyleOneRuntime.dataSlotIndex(),
                    networkType = StatusBarComboStyleOneRuntime.currentNetworkType(),
                    foreground = tint,
                ),
            )
            canvas.restoreToCount(save)
        }

    private class OverlayWindow(private val holder: Holder) {
        private val host: ViewGroup = holder.statusBarView
        private val context: Context = host.context
        private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        private val view = StyleOneIconView(context)
        private val params = WindowManager.LayoutParams()
        private var attached = false

        init {
            params.type = resolveWindowType()
            params.gravity = Gravity.TOP or Gravity.LEFT
            params.format = PixelFormat.TRANSLUCENT
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            params.windowAnimations = 0
            params.setTitle("OneuiXP Three-in-One Icon Style One")
            view.isClickable = false
            view.isFocusable = false
            view.setWillNotDraw(false)
        }

        fun isAttached(): Boolean = attached

        fun attach(cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime): Boolean {
            if (attached) {
                update(cfg)
                return true
            }
            val wm = windowManager ?: return false
            val metrics = calculateMetrics(context, cfg)
            params.width = metrics.width
            params.height = metrics.height
            view.update(cfg, indicatorColor)
            return runCatching {
                wm.addView(view, params)
                attached = true
                syncGeometry(cfg)
                true
            }.getOrElse { error ->
                Log.w(TAG, "Unable to attach Style One window", error)
                false
            }
        }

        fun update(cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
            if (!attached) return
            view.update(cfg, indicatorColor)
            syncGeometry(cfg)
        }

        fun refreshGlyphs() {
            if (attached) update(config)
        }

        fun syncGeometry(cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime = config) {
            if (!attached || !host.isAttachedToWindow || windowManager == null) return
            val metrics = calculateMetrics(context, cfg)
            val hostLocation = IntArray(2)
            host.getLocationOnScreen(hostLocation)

            val x: Int
            val y: Int
            val anchor = holder.batteryAnchor?.takeIf { it.isAttachedToWindow }
            val anchorLocation = IntArray(2)
            if (anchor != null) anchor.getLocationOnScreen(anchorLocation)
            val centerX = if (anchor != null) anchorLocation[0] + anchor.width / 2f
                else hostLocation[0] + host.width - metrics.side / 2f
            val centerY = if (anchor != null) anchorLocation[1] + anchor.height / 2f
                else hostLocation[1] + host.height / 2f
            x = (centerX - metrics.width / 2f +
                cfg.offsetXDp * context.resources.displayMetrics.density).roundToInt()
            y = (centerY - metrics.height / 2f +
                cfg.offsetYDp * context.resources.displayMetrics.density).roundToInt()

            val widthChanged = params.width != metrics.width || params.height != metrics.height
            val positionChanged = params.x != x || params.y != y
            if (!widthChanged && !positionChanged) return
            params.width = metrics.width
            params.height = metrics.height
            params.x = x
            params.y = y
            runCatching { windowManager?.updateViewLayout(view, params) }
        }

        fun detach() {
            if (!attached) return
            attached = false
            runCatching { windowManager?.removeViewImmediate(view) }
        }

        private fun resolveWindowType(): Int {
            return runCatching {
                WindowManager.LayoutParams::class.java
                    .getField("TYPE_STATUS_BAR_ADDITIONAL")
                    .getInt(null)
            }.getOrElse { WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY }
        }
    }

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val batteryReceiverStarted = AtomicBoolean(false)
    private val wifiReceiverStarted = AtomicBoolean(false)
    private val telephonyStarted = AtomicBoolean(false)
    private val previewQueued = AtomicBoolean(false)
    @Volatile private var pendingPreview: Preference.SystemUI.StatusBarComboStyleOneRuntime? = null
    private val holders = WeakHashMap<View, Holder>()
    private val signalStates = LinkedHashMap<Int, SignalState>()
    private val callbacks = LinkedHashMap<Int, Pair<TelephonyManager, TelephonyCallback>>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val indicatorListeners = CopyOnWriteArrayList<(Int) -> Unit>()

    @Volatile private var config = Preference.SystemUI.StatusBarComboStyleOneRuntime()
    @Volatile private var indicatorColor = Color.WHITE
    @Volatile var batteryLevel: Int = 0
        private set
    @Volatile var charging: Boolean = false
        private set
    @Volatile var quickCharging: Boolean = false
        private set
    @Volatile var powerSave: Boolean = false
        private set
    @Volatile var wifiConnected: Boolean = false
        private set
    @Volatile var wifiLevel: Int = 0
        private set
    @Volatile var network5g: Boolean = false
        private set
    @Volatile private var defaultDataSubscriptionId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    private var preferenceThread: HandlerThread? = null
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var wifiReceiver: BroadcastReceiver? = null
    private var subscriptionManager: SubscriptionManager? = null
    private var telephonyContext: Context? = null
    private var subscriptionListener: SubscriptionManager.OnSubscriptionsChangedListener? = null

    fun isEnabled(): Boolean = config.enabled && hasAttachedRenderer()

    fun isMobileNativeSuppressed(): Boolean =
        config.enabled && config.showMobile &&
            (config.signalMode == 0 || config.stackedSignal) && hasAttachedRenderer()

    private fun hasAttachedRenderer(): Boolean = synchronized(holders) {
        holders.values.any { it.overlay?.isAttached() == true }
    }

    fun install(module: XposedModule, param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != Package.SYSTEMUI) return
        if (!installed.compareAndSet(false, true)) return

        runCatching {
            with(module) { PreferenceProvider.loadPreference() }
                ?.systemUI?.statusBar?.comboStyleOneRuntime
                ?.let { config = sanitize(it) }
        }

        StatusBarSignalIndependentRuntime.addIndicatorColorListener { nextColor ->
            if (indicatorColor != nextColor) {
                indicatorColor = nextColor
                refreshGlyphs()
            }
        }

        runCatching {
            val clazz = param.classLoader.loadClass(PHONE_STATUS_BAR_VIEW)
            clazz.declaredConstructors.forEach { constructor ->
                module.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    (chain.thisObject as? ViewGroup)?.let { statusBar ->
                        statusBar.post {
                            registerStatusBar(module, statusBar)
                            ensurePreferenceWatcher(module, statusBar.context)
                            ensureBatteryReceiver(statusBar.context)
                            ensureWifiReceiver(statusBar.context)
                            ensureTelephony(statusBar.context)
                        }
                    }
                    result
                }
            }
        }.onFailure { Log.w(TAG, "Unable to hook status bar", it) }
    }

    private fun sanitize(source: Preference.SystemUI.StatusBarComboStyleOneRuntime) = source.copy(
        placementMode = 0,
        trioStyle = source.trioStyle.coerceIn(0, 1),
        mobileTypeMode = source.mobileTypeMode.coerceIn(0, 2),
        signalMode = source.signalMode.coerceIn(0, 1),
        sizePercent = source.sizePercent.coerceIn(50f, 250f),
        offsetXDp = source.offsetXDp.coerceIn(-24f, 24f),
        offsetYDp = source.offsetYDp.coerceIn(-15f, 15f),
        arcStrokePercent = source.arcStrokePercent.coerceIn(50f, 200f),
        ringStrokeDp = source.ringStrokeDp.coerceIn(1f, 4f),
        ringStroke = source.ringStroke.coerceIn(4, 16),
        arcStroke = source.arcStroke.coerceIn(3, 16),
        batteryBlackDepth = source.batteryBlackDepth.coerceIn(0.15f, 1f),
        trackAlpha = source.trackAlpha.coerceIn(0, 255),
        dotsGapDp = source.dotsGapDp.coerceIn(0f, 8f),
        dotsSizePercent = source.dotsSizePercent.coerceIn(50f, 180f),
        innerSizePercent = source.innerSizePercent.coerceIn(50f, 180f),
        innerYDp = source.innerYDp.coerceIn(-8f, 8f),
        batteryTextSizeSp = source.batteryTextSizeSp.coerceIn(5f, 18f),
        batteryTextFontWeight = source.batteryTextFontWeight.coerceIn(100, 900),
        chargingBoltSizePercent = source.chargingBoltSizePercent.coerceIn(50f, 180f),
        chargingBoltXDp = source.chargingBoltXDp.coerceIn(-8f, 8f),
        chargingBoltYDp = source.chargingBoltYDp.coerceIn(-8f, 8f),
        lowBatteryThreshold = source.lowBatteryThreshold.coerceIn(5, 50),
        valueSize = source.valueSize.coerceIn(16, 44),
        valueWeight = source.valueWeight.coerceIn(100, 900),
        typeSize = source.typeSize.coerceIn(16, 44),
        typeWeight = source.typeWeight.coerceIn(100, 900),
        typeSuffixScale = source.typeSuffixScale.coerceIn(50, 100),
        outTypeSizeDp = source.outTypeSizeDp.coerceIn(8, 26),
        outSignalSizeDp = source.outSignalSizeDp.coerceIn(6, 20),
        outTypeMarginLeftDp = source.outTypeMarginLeftDp.coerceIn(0, 16),
        outTypeMarginRightDp = source.outTypeMarginRightDp.coerceIn(0, 16),
        outSignalMarginDp = source.outSignalMarginDp.coerceIn(0, 16),
    )

    private fun calculateMetrics(
        context: Context,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
    ): Metrics {
        val density = context.resources.displayMetrics.density
        val scale = cfg.sizePercent.coerceIn(50f, 250f) / 100f
        val side = (24f * density * scale).coerceIn(12f * density, 72f * density)
        val extra = when {
            cfg.signalMode == 1 && cfg.showMobile -> 34f * density
            cfg.mobileTypeMode == 2 && cfg.showMobile && currentNetworkType().isNotEmpty() ->
                24f * density
            else -> 0f
        }
        val width = (side + extra).roundToInt().coerceAtLeast(1)
        val height = side.roundToInt().coerceAtLeast(1)
        return Metrics(
            width = width,
            height = height,
            side = side,
            ringCenterX = width / 2f,
            wifiWidth = 0f,
            signalWidth = 0f,
            typeWidth = extra,
            gap = 0f,
        )
    }

    private fun registerStatusBar(module: XposedModule, statusBar: ViewGroup) {
        val existing = synchronized(holders) { holders[statusBar] }
        if (existing != null) {
            applyHolder(existing)
            return
        }

        val holder = Holder(statusBar)
        synchronized(holders) { holders[statusBar] = holder }
        statusBar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            holder.overlay?.takeIf { it.isAttached() }?.syncGeometry(config)
            if (config.enabled && holder.batteryAnchor != null && holder.batteryAnchor?.isAttachedToWindow != true) {
                holder.suppressionKey = null
                mainHandler.post { applyHolder(holder) }
            }
        }
        statusBar.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                mainHandler.post { applyHolder(holder) }
            }

            override fun onViewDetachedFromWindow(v: View) {
                mainHandler.post {
                    holder.overlay?.detach()
                    restoreNative(holder)
                    holder.suppressionKey = null
                }
            }
        })
        ensurePreferenceWatcher(module, statusBar.context)
        ensureBatteryReceiver(statusBar.context)
        ensureWifiReceiver(statusBar.context)
        ensureTelephony(statusBar.context)
        applyHolder(holder)
    }

    private fun applyHolder(holder: Holder) {
        val cfg = config
        if (!cfg.enabled || !holder.statusBarView.isAttachedToWindow) {
            holder.overlay?.detach()
            holder.overlay = null
            restoreNative(holder)
            holder.suppressionKey = null
            return
        }

        val overlay = holder.overlay ?: OverlayWindow(holder).also { holder.overlay = it }
        if (!overlay.attach(cfg)) {
            restoreNative(holder)
            holder.suppressionKey = null
            return
        }
        overlay.update(cfg)

        val key = SuppressionKey(
            cfg.enabled, cfg.showWifi, cfg.showMobile, cfg.showBattery,
            cfg.signalMode, cfg.stackedSignal, cfg.mobileTypeMode,
        )
        if (holder.suppressionKey != key || (holder.batteryAnchor != null && holder.batteryAnchor?.isAttachedToWindow != true)) {
            restoreNative(holder)
            holder.batteryAnchor = findBatteryAnchor(holder.statusBarView)
            suppressNative(holder, cfg)
            holder.suppressionKey = key
            refreshNativeChargingFlags()
        }
        overlay.syncGeometry(cfg)
    }

    private fun suppressNative(holder: Holder, cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        val statusBar = holder.statusBarView
        val views = collectDescendants(statusBar)
        val anchor = holder.batteryAnchor

        if (anchor != null && cfg.showBattery) {
            // Preserve the native battery width as the replacement slot.
            rememberAndSet(holder, anchor, View.INVISIBLE)
        }

        val candidates = views.filter { view ->
            view !== anchor && (
                (cfg.showWifi && isNativeWifi(view)) ||
                (cfg.showMobile && (cfg.signalMode == 0 || cfg.stackedSignal) && isNativeMobile(view))
            )
        }
        val candidateSet = candidates.toSet()
        candidates.forEach { view ->
            var parent = view.parent as? View
            var hasCandidateAncestor = false
            while (parent != null && parent !== statusBar) {
                if (candidateSet.contains(parent)) {
                    hasCandidateAncestor = true
                    break
                }
                parent = parent.parent as? View
            }
            if (!hasCandidateAncestor) rememberAndSet(holder, view, View.GONE)
        }
    }

    private fun rememberAndSet(holder: Holder, view: View, visibility: Int) {
        if (!holder.hiddenNative.containsKey(view)) {
            holder.hiddenNative[view] = HiddenState(view.alpha, view.visibility)
        }
        if (view.visibility != visibility) view.visibility = visibility
    }

    private fun restoreNative(holder: Holder) {
        holder.hiddenNative.forEach { (view, state) ->
            if (view.parent != null) {
                view.alpha = state.alpha
                view.visibility = state.visibility
            }
        }
        holder.hiddenNative.clear()
        holder.batteryAnchor = null
    }

    private fun findBatteryAnchor(root: View): View? {
        val all = collectDescendants(root as? ViewGroup ?: return null)
        return all.firstOrNull { it.javaClass.name.contains("BatteryMeterView", ignoreCase = true) }
            ?: all.firstOrNull { view ->
                val name = resourceName(view).lowercase()
                name == "battery" || name.contains("battery_meter") ||
                    name.contains("status_bar_battery") || name == "battery_icon"
            }
    }

    private fun isNativeWifi(view: View): Boolean {
        val value = descriptor(view)
        if (value.contains("battery") || value.contains("bluetooth") ||
            value.contains("network_speed") || value.contains("netspeed") ||
            value.contains("wifi_link_speed") || value.contains("wifilinkspeed")
        ) return false
        return value.contains("wifi") || value.contains("wlan")
    }

    private fun isNativeMobile(view: View): Boolean {
        val value = descriptor(view)
        if (value.contains("battery") || value.contains("wifi") ||
            value.contains("bluetooth") || value.contains("network_speed") ||
            value.contains("netspeed") || value.contains("carrier") ||
            value.contains("airplane") || value.contains("clock")
        ) return false
        return value.contains("mobile") || value.contains("cellular") ||
            value.contains("signal_cluster") || value.contains("statusbar_signal") ||
            value.contains("signal_strength") || value.contains("cell_signal") ||
            value.contains("mobile_data")
    }

    private fun descriptor(view: View): String =
        (resourceName(view) + " " + view.javaClass.name + " " +
            (view.contentDescription?.toString() ?: "")).lowercase()

    private fun resourceName(view: View): String {
        if (view.id == View.NO_ID) return ""
        return runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault("")
    }

    private fun collectDescendants(root: ViewGroup): List<View> {
        val result = ArrayList<View>()
        fun visit(group: ViewGroup) {
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                result += child
                if (child is ViewGroup) visit(child)
            }
        }
        visit(root)
        return result
    }

    private fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (!watcherStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val worker = HandlerThread("OneuiXPStyleOnePrefs").apply { start() }
        preferenceThread = worker
        val background = Handler(worker.looper)

        fun refreshStored() {
            background.post {
                val next = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }
                        ?.systemUI?.statusBar?.comboStyleOneRuntime
                }.getOrNull() ?: return@post
                val safe = sanitize(next)
                if (safe == config) return@post
                mainHandler.post {
                    config = safe
                    applyAll()
                }
            }
        }

        val observer = object : ContentObserver(background) {
            override fun onChange(selfChange: Boolean) = refreshStored()
            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) = refreshStored()
        }
        preferenceObserver = observer
        runCatching {
            appContext.contentResolver.registerContentObserver(
                android.net.Uri.parse(PREFERENCE_CHANGED_URI), false, observer,
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != StatusBarRuntimeBridge.ACTION) return
                val payload = intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON) ?: return
                background.post {
                    val bar = runCatching {
                        IgnoreUnknownKeysJson.decodeFromString(
                            Preference.SystemUI.StatusBar.serializer(), payload,
                        )
                    }.getOrNull() ?: return@post
                    val next = sanitize(bar.comboStyleOneRuntime)
                    pendingPreview = next
                    if (previewQueued.compareAndSet(false, true)) {
                        mainHandler.postDelayed({
                            val latest = pendingPreview
                            pendingPreview = null
                            previewQueued.set(false)
                            if (latest != null && latest != config) {
                                config = latest
                                applyAll()
                            }
                        }, 16L)
                    }
                }
            }
        }
        runtimeReceiver = receiver
        runCatching {
            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(receiver, filter)
            }
        }
        refreshStored()
    }

    private fun ensureBatteryReceiver(context: Context) {
        if (!batteryReceiverStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                    batteryLevel = (level * 100 / scale).coerceIn(0, 100)
                    val status = intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN,
                    )
                    charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                } else if (intent?.action != "android.os.action.POWER_SAVE_MODE_CHANGED") {
                    return
                }
                val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                powerSave = runCatching { powerManager?.isPowerSaveMode == true }.getOrDefault(false)
                refreshNativeChargingFlags()
                refreshGlyphs()
            }
        }
        batteryReceiver = receiver
        runCatching {
            appContext.registerReceiver(
                receiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction("android.os.action.POWER_SAVE_MODE_CHANGED")
                },
            )
        }
    }

    private fun ensureWifiReceiver(context: Context) {
        if (!wifiReceiverStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                updateWifi(wifi)
            }
        }
        wifiReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(receiver, filter)
            }
        }
        updateWifi(wifi)
    }

    private fun updateWifi(wifi: WifiManager?) {
        val info = runCatching { wifi?.connectionInfo }.getOrNull()
        val connected = info != null && info.networkId >= 0 &&
            info.supplicantState == SupplicantState.COMPLETED
        wifiConnected = connected
        wifiLevel = if (connected) {
            WifiManager.calculateSignalLevel(info!!.rssi, 4).coerceIn(0, 3) + 1
        } else {
            0
        }
        refreshGlyphs()
    }

    private fun ensureTelephony(context: Context) {
        if (!telephonyStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        telephonyContext = appContext
        subscriptionManager = appContext.getSystemService(SubscriptionManager::class.java)
        val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
            override fun onSubscriptionsChanged() {
                refreshSubscriptions(appContext)
            }
        }
        subscriptionListener = listener
        runCatching { subscriptionManager?.addOnSubscriptionsChangedListener(listener) }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED") {
                    refreshSubscriptions(appContext)
                }
            }
        }
        val filter = IntentFilter("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(receiver, filter)
            }
        }
        refreshSubscriptions(appContext)
    }

    private fun refreshSubscriptions(context: Context) {
        defaultDataSubscriptionId = runCatching {
            SubscriptionManager.getDefaultDataSubscriptionId()
        }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        val active: List<SubscriptionInfo> = runCatching {
            subscriptionManager?.activeSubscriptionInfoList.orEmpty()
        }.getOrDefault(emptyList())
        val wanted = active.map { it.subscriptionId }.toSet()
        val stale = synchronized(callbacks) { callbacks.keys.filter { it !in wanted } }
        stale.forEach { subId ->
            val old = synchronized(callbacks) { callbacks.remove(subId) }
            old?.let { (manager, callback) ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { manager.unregisterTelephonyCallback(callback) }
                }
            }
            synchronized(signalStates) { signalStates.remove(subId) }
        }

        active.forEach { info ->
            synchronized(signalStates) {
                val previous = signalStates[info.subscriptionId]
                signalStates[info.subscriptionId] = (previous ?: SignalState(info.subscriptionId)).copy(
                    slotIndex = info.simSlotIndex,
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alreadyRegistered = synchronized(callbacks) { callbacks.containsKey(info.subscriptionId) }
                if (!alreadyRegistered) {
                    val base = context.getSystemService(TelephonyManager::class.java) ?: return@forEach
                    val manager = runCatching { base.createForSubscriptionId(info.subscriptionId) }.getOrNull()
                        ?: return@forEach
                    val callback = SubscriptionCallback(info.subscriptionId)
                    runCatching {
                        manager.registerTelephonyCallback(context.mainExecutor, callback)
                        synchronized(callbacks) { callbacks[info.subscriptionId] = manager to callback }
                    }
                }
            }
        }
        updateDefaultDataSignal()
        refreshGlyphs()
    }

    private class SubscriptionCallback(private val subscriptionId: Int) :
        TelephonyCallback(),
        TelephonyCallback.SignalStrengthsListener,
        TelephonyCallback.ServiceStateListener,
        TelephonyCallback.DisplayInfoListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            updateSignal(subscriptionId, level = signalStrength.level.coerceIn(0, 4))
        }

        override fun onServiceStateChanged(serviceState: ServiceState) {
            updateSignal(
                subscriptionId,
                inService = serviceState.state == ServiceState.STATE_IN_SERVICE,
                networkType = baseNetworkType(subscriptionId),
            )
        }

        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            val override = telephonyDisplayInfo.overrideNetworkType
            val value = override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
            val label = when (override) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5GA"
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G"
                else -> baseNetworkType(subscriptionId)
            }
            updateSignal(subscriptionId, is5g = value, networkType = label)
        }
    }

    private fun updateSignal(
        subscriptionId: Int,
        level: Int? = null,
        inService: Boolean? = null,
        is5g: Boolean? = null,
        networkType: String? = null,
    ) {
        synchronized(signalStates) {
            val previous = signalStates[subscriptionId] ?: SignalState(subscriptionId)
            signalStates[subscriptionId] = previous.copy(
                level = level ?: previous.level,
                inService = inService ?: previous.inService,
                is5g = is5g ?: previous.is5g,
                networkType = networkType ?: previous.networkType,
            )
        }
        updateDefaultDataSignal()
        refreshGlyphs()
    }

    private fun updateDefaultDataSignal() {
        network5g = synchronized(signalStates) {
            signalStates[defaultDataSubscriptionId]?.let { it.inService && it.is5g } ?: false
        }
    }

    private fun currentSignalLevel(): Int {
        if (defaultDataSubscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return 0
        return synchronized(signalStates) {
            signalStates[defaultDataSubscriptionId]
                ?.takeIf { it.inService }
                ?.level?.coerceIn(0, 4) ?: 0
        }
    }

    private fun currentSlotLevels(): IntArray {
        val result = intArrayOf(-1, -1)
        synchronized(signalStates) {
            signalStates.values.forEach { state ->
                if (state.slotIndex in 0..1 && state.inService) {
                    result[state.slotIndex] = state.level.coerceIn(0, 4)
                }
            }
        }
        return result
    }

    private fun dataSlotIndex(): Int {
        if (defaultDataSubscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return -1
        val info = runCatching {
            subscriptionManager?.activeSubscriptionInfoList
                ?.firstOrNull { it.subscriptionId == defaultDataSubscriptionId }
        }.getOrNull()
        return info?.simSlotIndex ?: -1
    }

    private fun currentNetworkType(): String {
        if (defaultDataSubscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return ""
        return synchronized(signalStates) {
            signalStates[defaultDataSubscriptionId]?.networkType?.takeIf { it.isNotBlank() }
                ?: if (signalStates[defaultDataSubscriptionId]?.is5g == true) "5G" else ""
        }
    }

    private fun baseNetworkType(subscriptionId: Int): String {
        val manager = runCatching {
            telephonyContext?.getSystemService(TelephonyManager::class.java)
                ?.createForSubscriptionId(subscriptionId)
        }.getOrNull() ?: return ""
        return when (runCatching { manager.dataNetworkType }.getOrDefault(TelephonyManager.NETWORK_TYPE_UNKNOWN)) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_CDMA,
            -> "2G"
            else -> ""
        }
    }

    private fun refreshNativeChargingFlags() {
        val anchors = synchronized(holders) { holders.values.mapNotNull { it.batteryAnchor } }
        val target = anchors.firstOrNull() ?: return
        quickCharging = readBooleanField(
            target,
            "mQuickCharging",
            "mFastCharging",
            "mIsFastCharging",
            "mQuickCharge",
        ) ?: quickCharging
    }

    private fun readBooleanField(target: Any, vararg names: String): Boolean? {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null && clazz != Any::class.java) {
            val currentClass = clazz
            for (name in names) {
                val field = runCatching { currentClass.getDeclaredField(name) }.getOrNull() ?: continue
                val value = runCatching {
                    field.isAccessible = true
                    field.get(target)
                }.getOrNull()
                when (value) {
                    is Boolean -> return value
                    is Number -> return value.toInt() != 0
                }
            }
            clazz = currentClass.superclass
        }
        return null
    }

    private fun applyAll() {
        val active = synchronized(holders) { holders.values.toList() }
        active.forEach(::applyHolder)
        refreshGlyphs()
    }

    private fun refreshGlyphs() {
        mainHandler.post {
            val active = synchronized(holders) { holders.values.toList() }
            active.forEach { it.overlay?.refreshGlyphs() }
        }
    }
}
