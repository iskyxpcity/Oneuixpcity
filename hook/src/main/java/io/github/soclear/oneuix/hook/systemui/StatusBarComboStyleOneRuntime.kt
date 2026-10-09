package io.github.soclear.oneuix.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.common.StatusComboStyleOneGlyphRenderer
import io.github.soclear.oneuix.common.StatusComboStyleOneGlyphState
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import java.util.Collections
import java.util.IdentityHashMap
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.decodeFromString

/**
 * Independent runtime for the Style One icon: state polling, native icon folding,
 * layout replacement, standalone rendering window and live preference updates.
 */
@Suppress("DEPRECATION")
object StatusBarComboStyleOneRuntime {
    private const val TAG = "OneuiXPStyleOne"
    private const val PHONE_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBarView"
    private const val WINDOW_BAR = "com.android.systemui.statusbar.phone.StatusBarWindowView"
    private const val MIUI_ICONS = "com.android.systemui.statusbar.phone.MiuiStatusIconContainer"
    private const val AOSP_ICONS = "com.android.systemui.statusbar.phone.StatusBarIconContainer"
    private const val GLYPH_WIDTH_DP = 78
    private const val GLYPH_HEIGHT_DP = 36

    private val installed = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val hosts = Collections.synchronizedMap(WeakHashMap<ViewGroup, Host>())
    @Volatile private var config = Preference.SystemUI.StatusBarComboStyleOneRuntime()
    @Volatile private var state = StatusComboStyleOneGlyphState()
    @Volatile private var preferenceReceiverReady = false
    @Volatile private var stateReceiverReady = false
    @Volatile private var tint = Color.WHITE
    @Volatile private var subscriptionsListener: SubscriptionManager.OnSubscriptionsChangedListener? = null
    private val phoneListeners = mutableMapOf<Int, Pair<TelephonyManager, PhoneStateListener>>()

    private class Host(val root: ViewGroup) {
        val original = IdentityHashMap<View, Int>()
        var battery: View? = null
        var inline: GlyphView? = null
        var overlay: GlyphOverlay? = null
        var overlayFailed = false
        var applying = false
    }

    private class GlyphView(context: Context) : View(context) {
        init {
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            StatusComboStyleOneGlyphRenderer.draw(canvas, width, height, StatusBarComboStyleOneRuntime.config, StatusBarComboStyleOneRuntime.state)
        }
    }

    private class GlyphOverlay(context: Context) {
        private val app = context.applicationContext ?: context
        private val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        private val glyph = GlyphView(app)
        private var params: WindowManager.LayoutParams? = null

        fun attach(anchor: View): Boolean {
            if (params != null) { update(anchor); return true }
            val type = runCatching {
                WindowManager.LayoutParams::class.java.getDeclaredField("TYPE_STATUS_BAR_ADDITIONAL")
                    .apply { isAccessible = true }.getInt(null)
            }.getOrElse {
                if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE
            }
            val lp = WindowManager.LayoutParams(
                StatusBarComboStyleOneRuntime.dp(app, StatusBarComboStyleOneRuntime.GLYPH_WIDTH_DP), StatusBarComboStyleOneRuntime.dp(app, StatusBarComboStyleOneRuntime.GLYPH_HEIGHT_DP), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.LEFT
                title = "OneuiXP Style One"
                if (type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY &&
                    type != WindowManager.LayoutParams.TYPE_PHONE
                ) token = anchor.applicationWindowToken
            }
            return try {
                wm.addView(glyph, lp)
                params = lp
                update(anchor)
                true
            } catch (e: Throwable) {
                Log.w(TAG, "Status-bar glyph window failed", e)
                if (type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY && Build.VERSION.SDK_INT >= 26) {
                    try {
                        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        lp.token = null
                        wm.addView(glyph, lp)
                        params = lp
                        update(anchor)
                        true
                    } catch (second: Throwable) {
                        Log.w(TAG, "Fallback glyph window failed", second)
                        false
                    }
                } else false
            }
        }

        fun update(anchor: View) {
            val lp = params ?: return
            val pos = IntArray(2)
            anchor.getLocationOnScreen(pos)
            lp.x = (pos[0] + anchor.width - lp.width).coerceAtLeast(0)
            lp.y = (pos[1] + (anchor.height - lp.height) / 2).coerceAtLeast(0)
            glyph.invalidate()
            runCatching { wm.updateViewLayout(glyph, lp) }
        }
        fun detach() {
            if (params == null) return
            runCatching { wm.removeView(glyph) }
            params = null
        }
    }

    fun install(module: XposedModule, param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != Package.SYSTEMUI || !installed.compareAndSet(false, true)) return
        runCatching {
            with(module) { PreferenceProvider.loadPreference() }
                ?.systemUI?.statusBar?.comboStyleOneRuntime?.let { config = sanitize(it) }
        }.onFailure { Log.w(TAG, "Cannot read Style One settings", it) }

        StatusBarSignalIndependentRuntime.addIndicatorColorListener { next ->
            if (next != tint) { tint = next; state = state.copy(tintColor = next); refreshHosts() }
        }

        var hookedHost = false
        listOf(PHONE_BAR, WINDOW_BAR).forEach { name ->
            runCatching {
                val cls = param.classLoader.loadClass(name)
                cls.declaredConstructors.forEach { constructor ->
                    module.hook(constructor).intercept { chain ->
                        val result = chain.proceed()
                        (chain.thisObject as? ViewGroup)?.let(::scheduleRegister)
                        result
                    }
                }
                hookedHost = true
            }.onFailure { Log.d(TAG, "Status-bar host unavailable: $name", it) }
        }
        listOf(MIUI_ICONS, AOSP_ICONS).forEach { name ->
            runCatching {
                val cls = param.classLoader.loadClass(name)
                cls.declaredMethods.filter { it.name == "onMeasure" && it.parameterTypes.size == 2 }.forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        (chain.thisObject as? ViewGroup)?.takeIf { config.enabled }?.post { suppressInContainer(it) }
                        result
                    }
                }
            }.onFailure { Log.d(TAG, "Status icon container unavailable: $name", it) }
        }
        if (!hookedHost) Log.w(TAG, "No status-bar host constructor was hooked")
    }

    fun isEnabled(): Boolean = config.enabled
    fun isMobileNativeSuppressed(): Boolean =
        config.enabled && config.showMobile && (config.signalMode == 0 || config.stackedSignal)

    private fun scheduleRegister(root: ViewGroup) {
        root.post { register(root) }
        root.postDelayed({ register(root) }, 250L)
        root.postDelayed({ register(root) }, 1000L)
    }

    private fun register(root: ViewGroup) {
        val host = synchronized(hosts) {
            hosts[root] ?: Host(root).also { hosts[root] = it }
        }
        if (host.battery == null) {
            root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                host.overlay?.let { win -> host.battery?.let(win::update) }
                if (!host.applying) main.post { apply(host) }
            }
            root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) { main.post { apply(host) } }
                override fun onViewDetachedFromWindow(v: View) {
                    main.post { host.overlay?.detach(); host.overlay = null; removeInline(host) }
                }
            })
        }
        ensureReceivers(root.context)
        apply(host)
    }

    private fun refreshHosts() {
        main.post { synchronized(hosts) { hosts.values.toList() }.forEach(::apply) }
    }

    private fun apply(host: Host) {
        if (host.applying) return
        host.applying = true
        try {
            val root = host.root
            if (!root.isAttachedToWindow) { host.overlay?.detach(); return }
            val views = descendants(root)
            val battery = views.firstOrNull(::isBatteryIcon)

            if (!config.enabled) {
                host.overlay?.detach()
                host.overlay = null
                removeInline(host)
                restore(host)
                host.battery = null
                root.requestLayout()
                return
            }

            if (host.battery != null && host.battery !== battery) {
                host.overlay?.detach()
                host.overlay = null
                removeInline(host)
                host.original.remove(host.battery)?.let { v -> runCatching { host.battery?.visibility = v } }
            }
            host.battery = battery
            val foldMobile = isMobileNativeSuppressed()

            views.forEach { view ->
                if (view === battery || view is GlyphView) return@forEach
                val hide = (config.showWifi && isWifiIcon(view)) || (foldMobile && isMobileIcon(view))
                if (hide) {
                    host.original.putIfAbsent(view, view.visibility)
                    if (view.visibility != View.GONE) view.visibility = View.GONE
                } else {
                    host.original.remove(view)?.let { old -> if (view.visibility != old) view.visibility = old }
                }
            }

            if (battery != null) {
                host.original.putIfAbsent(battery, battery.visibility)
                if (config.overlayGlyph && !host.overlayFailed) {
                    removeInline(host)
                    if (battery.visibility != View.INVISIBLE) battery.visibility = View.INVISIBLE
                    val overlay = host.overlay ?: GlyphOverlay(root.context).also { host.overlay = it }
                    if (!overlay.attach(battery)) {
                        host.overlayFailed = true
                        overlay.detach()
                        host.overlay = null
                        battery.visibility = View.GONE
                        insertInline(host, battery)
                    } else overlay.update(battery)
                } else {
                    host.overlay?.detach()
                    host.overlay = null
                    if (battery.visibility != View.GONE) battery.visibility = View.GONE
                    insertInline(host, battery)
                }
            }
            root.requestLayout()
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot apply Style One", t)
        } finally {
            host.applying = false
        }
    }

    private fun insertInline(host: Host, anchor: View) {
        val parent = anchor.parent as? ViewGroup ?: return
        if (host.inline?.parent === parent) { host.inline?.invalidate(); return }
        removeInline(host)
        val glyph = GlyphView(parent.context)
        val old = anchor.layoutParams
        val width = dp(parent.context, GLYPH_WIDTH_DP)
        val height = max(dp(parent.context, 24), old?.height ?: 0)
        val lp = when (old) {
            is LinearLayout.LayoutParams -> LinearLayout.LayoutParams(old).apply { this.width = width; this.height = height }
            is FrameLayout.LayoutParams -> FrameLayout.LayoutParams(old).apply { this.width = width; this.height = height }
            is RelativeLayout.LayoutParams -> RelativeLayout.LayoutParams(old).apply { this.width = width; this.height = height }
            is ViewGroup.MarginLayoutParams -> ViewGroup.MarginLayoutParams(old).apply { this.width = width; this.height = height }
            else -> ViewGroup.LayoutParams(width, height)
        }
        runCatching { parent.addView(glyph, parent.indexOfChild(anchor).coerceAtLeast(0), lp); host.inline = glyph }
            .onFailure { Log.w(TAG, "Cannot add inline glyph", it) }
    }

    private fun removeInline(host: Host) {
        val glyph = host.inline ?: return
        (glyph.parent as? ViewGroup)?.removeView(glyph)
        host.inline = null
    }
    private fun restore(host: Host) {
        host.original.entries.toList().forEach { (view, visibility) -> runCatching { view.visibility = visibility } }
        host.original.clear()
    }

    private fun descendants(root: View): List<View> {
        val result = ArrayList<View>()
        fun visit(view: View) {
            result += view
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(root)
        return result
    }
    private fun resourceName(view: View) = runCatching {
        if (view.id == View.NO_ID) "" else view.resources.getResourceEntryName(view.id)
    }.getOrDefault("")
    private fun isBatteryIcon(v: View): Boolean {
        if (v is GlyphView) return false
        val name = (v.javaClass.name + " " + resourceName(v) + " " + v.tag).lowercase()
        return name.contains("batterymetericonview") || name.contains("batterymeter") ||
            name.contains("batteryiconview") ||
            (name.contains("battery") && (name.contains("status") || name.contains("meter") || name.contains("icon")))
    }
    private fun isWifiIcon(v: View): Boolean {
        if (v is GlyphView) return false
        val name = (v.javaClass.name + " " + resourceName(v) + " " + v.tag).lowercase()
        if (name.contains("battery") || name.contains("bluetooth") || name.contains("airplane")) return false
        return name.contains("wifi") || name.contains("wlan") || name.contains("slot_wifi")
    }
    private fun isMobileIcon(v: View): Boolean {
        if (v is GlyphView) return false
        val name = (v.javaClass.name + " " + resourceName(v) + " " + v.tag + " " + v.contentDescription).lowercase()
        if (name.contains("battery") || name.contains("bluetooth") || name.contains("airplane")) return false
        return (name.contains("mobile") || name.contains("cellular") || name.contains("signal_mobile") ||
            name.contains("slot_mobile") || name.contains("signal_strength")) &&
            (name.contains("statusbaricon") || name.contains("modernstatusbar") || name.contains("mobile") ||
                name.contains("cellular") || name.contains("signalcluster"))
    }
    private fun suppressInContainer(container: ViewGroup) {
        if (!config.enabled) return
        descendants(container).forEach { view ->
            if (view is GlyphView) return@forEach
            if ((config.showWifi && isWifiIcon(view)) || (isMobileNativeSuppressed() && isMobileIcon(view))) {
                if (view.visibility != View.GONE) view.visibility = View.GONE
            }
        }
    }

    private fun ensureReceivers(context: Context) {
        val app = context.applicationContext ?: context
        if (!preferenceReceiverReady) synchronized(this) {
            if (!preferenceReceiverReady) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        if (intent.action != StatusBarRuntimeBridge.ACTION) return
                        val payload = intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON) ?: return
                        runCatching {
                            val status = IgnoreUnknownKeysJson.decodeFromString(Preference.SystemUI.StatusBar.serializer(), payload)
                            config = sanitize(status.comboStyleOneRuntime)
                            refreshHosts()
                        }.onFailure { Log.w(TAG, "Bad Style One settings broadcast", it) }
                    }
                }
                runCatching {
                    val filter = IntentFilter(StatusBarRuntimeBridge.ACTION)
                    if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                    else app.registerReceiver(receiver, filter)
                    preferenceReceiverReady = true
                }.onFailure { Log.w(TAG, "Preference receiver registration failed", it) }
            }
        }
        ensureStateReceiver(app)
    }

    private fun ensureStateReceiver(app: Context) {
        if (stateReceiverReady) return
        synchronized(this) {
            if (stateReceiverReady) return
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(WifiManager.RSSI_CHANGED_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
                addAction("android.intent.action.SUBSCRIPTION_PHONE_STATE_CHANGED")
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        Intent.ACTION_BATTERY_CHANGED -> updateBattery(intent)
                        Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> updateBattery(null)
                        WifiManager.RSSI_CHANGED_ACTION, WifiManager.NETWORK_STATE_CHANGED_ACTION -> updateWifi(app)
                        else -> refreshTelephony(app)
                    }
                    refreshHosts()
                }
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                else app.registerReceiver(receiver, filter)
                updateBattery(app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
                updateWifi(app)
                ensureTelephony(app)
                stateReceiverReady = true
            }.onFailure { Log.w(TAG, "Battery/network receiver registration failed", it) }
        }
    }

    private fun updateBattery(intent: Intent?) {
        val raw = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = if (raw >= 0 && scale > 0) (raw * 100f / scale).toInt() else state.batteryLevel
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0
        state = state.copy(batteryLevel = level.coerceIn(0, 100), charging = charging, quickCharge = charging && quickCharge())
    }
    private fun quickCharge(): Boolean =
        listOf("sys.miui.quickcharge", "persist.vendor.quickcharge.status", "ro.boot.quickcharge").any { key ->
            runCatching {
                val c = Class.forName("android.os.SystemProperties")
                val v = c.getMethod("get", String::class.java).invoke(null, key) as? String
                v == "1" || v.equals("true", true) || v.equals("fast", true)
            }.getOrDefault(false)
        }

    private fun updateWifi(context: Context) {
        val wm = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        runCatching {
            val info = wm.connectionInfo
            val level = if (info == null || info.rssi == WifiManager.INVALID_RSSI) -1
                else WifiManager.calculateSignalLevel(info.rssi, 5)
            state = state.copy(wifiLevel = level.coerceIn(-1, 4))
        }
    }
    private fun ensureTelephony(context: Context) {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager ?: return
        if (subscriptionsListener == null) {
            val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    refreshTelephony(context)
                    installPhoneListeners(context, sm)
                    refreshHosts()
                }
            }
            subscriptionsListener = listener
            runCatching { sm.addOnSubscriptionsChangedListener(listener) }
        }
        installPhoneListeners(context, sm)
        refreshTelephony(context)
    }
    private fun installPhoneListeners(context: Context, sm: SubscriptionManager) {
        val base = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val active = runCatching { sm.activeSubscriptionInfoList.orEmpty() }.getOrDefault(emptyList())
        val ids = active.map { it.subscriptionId }.toSet()
        phoneListeners.keys.filter { it !in ids }.toList().forEach { id ->
            phoneListeners.remove(id)?.let { old -> runCatching { old.first.listen(old.second, PhoneStateListener.LISTEN_NONE) } }
        }
        active.forEach { info ->
            if (phoneListeners.containsKey(info.subscriptionId)) return@forEach
            runCatching {
                val sub = base.createForSubscriptionId(info.subscriptionId)
                val listener = object : PhoneStateListener() {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        refreshTelephony(context); refreshHosts()
                    }
                    override fun onServiceStateChanged(serviceState: ServiceState?) {
                        refreshTelephony(context); refreshHosts()
                    }
                }
                sub.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE)
                phoneListeners[info.subscriptionId] = sub to listener
            }
        }
    }
    private fun refreshTelephony(context: Context) {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager ?: return
        val base = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val active: List<SubscriptionInfo> = runCatching { sm.activeSubscriptionInfoList.orEmpty() }.getOrDefault(emptyList())
        val dataSub = runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }.getOrDefault(-1)
        var dataLevel = -1
        var simOne = -1
        var simTwo = -1
        var label = state.networkType
        active.forEach { info ->
            val sub = runCatching { base.createForSubscriptionId(info.subscriptionId) }.getOrNull() ?: return@forEach
            val level = runCatching { sub.signalStrength?.level ?: -1 }.getOrDefault(-1).coerceIn(-1, 4)
            if (info.simSlotIndex == 0) simOne = level
            if (info.simSlotIndex == 1) simTwo = level
            if (info.subscriptionId == dataSub) {
                dataLevel = level
                label = networkType(sub.dataNetworkType)
            }
        }
        if (dataLevel < 0) dataLevel = if (simOne >= 0) simOne else simTwo
        if (dataLevel < 0) dataLevel = runCatching { base.signalStrength?.level ?: state.mobileLevel }.getOrDefault(state.mobileLevel)
        state = state.copy(mobileLevel = dataLevel.coerceIn(0, 4), simOneLevel = simOne, simTwoLevel = simTwo, networkType = label)
    }
    private fun networkType(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN -> "4G"
        TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
        TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"
        else -> state.networkType.ifBlank { "4G" }
    }

    private fun sanitize(v: Preference.SystemUI.StatusBarComboStyleOneRuntime) = v.copy(
        trioStyle = v.trioStyle.coerceIn(0, 1), mobileTypeMode = v.mobileTypeMode.coerceIn(0, 2),
        signalMode = v.signalMode.coerceIn(0, 1), lowThreshold = v.lowThreshold.coerceIn(1, 100),
        ringStroke = v.ringStroke.coerceIn(3, 28), arcStroke = v.arcStroke.coerceIn(2, 20),
        valueSize = v.valueSize.coerceIn(8, 64), valueWeight = v.valueWeight.coerceIn(100, 900),
        typeSize = v.typeSize.coerceIn(8, 64), typeWeight = v.typeWeight.coerceIn(100, 900),
        outTypeSizeDp = v.outTypeSizeDp.coerceIn(6, 30), outSignalSizeDp = v.outSignalSizeDp.coerceIn(6, 36),
        outTypeMarginLeftDp = v.outTypeMarginLeftDp.coerceIn(0, 12),
        outTypeMarginRightDp = v.outTypeMarginRightDp.coerceIn(0, 12),
        outSignalMarginDp = v.outSignalMarginDp.coerceIn(0, 12),
        typeSuffixScale = v.typeSuffixScale.coerceIn(30, 100), trackAlpha = v.trackAlpha.coerceIn(0, 255),
    )
    private fun refreshHosts() { main.post { synchronized(hosts) { hosts.values.toList() }.forEach(::apply) } }
    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt().coerceAtLeast(1)
}
