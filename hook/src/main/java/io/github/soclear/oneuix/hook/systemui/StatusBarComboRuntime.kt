package io.github.soclear.oneuix.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * OneuiXP-owned three-in-one status-bar renderer.
 *
 * It combines battery ring, Wi-Fi / network type and default-data-SIM signal
 * into one independently drawn View. No external app code, resource or
 * preference namespace is used at runtime.
 */
internal object StatusBarComboRuntime {
    private const val PREFERENCE_CHANGED_URI =
        "content://com.oneuixp.app.statusbridge/preferences"
    private const val PHONE_STATUS_BAR_VIEW =
        "com.android.systemui.statusbar.phone.PhoneStatusBarView"

    private data class SignalState(
        val subscriptionId: Int,
        val slotIndex: Int,
        val level: Int = 0,
        val inService: Boolean = false,
        val is5g: Boolean = false,
    )

    private data class HiddenState(
        val alpha: Float,
        val visibility: Int,
    )

    private data class Holder(
        val statusBarView: ViewGroup,
        val parent: ViewGroup,
        val root: LinearLayout,
        val icon: ComboIconView,
        val hiddenNative: MutableMap<View, HiddenState> = LinkedHashMap(),
        var mounted: Boolean = false,
        var nativeSuppressed: Boolean = false,
        var overlay: ComboOverlay? = null,
    )

    private class ComboIconView(context: Context) : View(context) {
        private var runtime = Preference.SystemUI.StatusBarComboRuntime()
        fun updateFromRuntime(next: Preference.SystemUI.StatusBarComboRuntime) {
            runtime = next
            visibility = if (next.enabled) VISIBLE else GONE
            invalidate()
        }

        private fun sizePx(): Float {
            return 24f * resources.displayMetrics.density *
                (runtime.sizePercent.coerceIn(50f, 250f) / 100f)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            // Do not accept SystemUI's 24dp EXACTLY constraint: the combo is an
            // optical replacement for the battery slot and must be allowed to
            // grow/shrink with its own sizePercent.
            val side = sizePx().coerceIn(
                12f * resources.displayMetrics.density,
                72f * resources.displayMetrics.density,
            )
            // Out-of-ring mobile/type content is part of the combo's own measured
            // bounds even when stackedSignal is disabled; otherwise Android may clip the
            // right-side renderer while the native mobile slot is already hidden.
            val extra = if (runtime.signalMode == 1 && runtime.showMobile) {
                34f * resources.displayMetrics.density
            } else if (runtime.mobileTypeMode == 2 && runtime.showMobile &&
                    StatusBarComboRuntime.networkTypeLabel.isNotEmpty()) {
                24f * resources.displayMetrics.density
            } else 0f
            setMeasuredDimension((side + extra).roundToInt(), side.roundToInt())
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!runtime.enabled) return
            StatusComboFullRenderer.draw(
                canvas,
                width,
                height,
                runtime,
                StatusComboFullRenderer.State(
                    batteryLevel = StatusBarComboRuntime.batteryLevel,
                    charging = StatusBarComboRuntime.charging,
                    quickCharging = StatusBarComboRuntime.quickCharging,
                    powerSave = StatusBarComboRuntime.powerSave,
                    lowBattery = StatusBarComboRuntime.batteryLevel in 0..runtime.lowBatteryThreshold,
                    wifiLevel = if (StatusBarComboRuntime.wifiConnected) {
                        StatusBarComboRuntime.wifiLevel
                    } else 0,
                    mobileLevel = StatusBarComboRuntime.currentSignalLevel(),
                    slotLevels = StatusBarComboRuntime.currentSlotLevels(),
                    dataSlot = StatusBarComboRuntime.dataSlotIndex(),
                    networkType = StatusBarComboRuntime.networkTypeLabel,
                    foreground = StatusBarComboRuntime.indicatorColor,
                ),
            )
        }

    }

    private class ComboOverlay(
        private val host: View,
        private val context: Context,
    ) {
        private val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
        private val view = ComboIconView(context)
        private val params = android.view.WindowManager.LayoutParams()
        private var attached = false
        private val watchHandler = Handler(Looper.getMainLooper())
        private val visibilityWatch = object : Runnable {
            override fun run() {
                if (!attached) return
                syncVisibility()
                watchHandler.postDelayed(this, 200L)
            }
        }

        init {
            params.type = resolveWindowType()
            params.gravity = Gravity.TOP or Gravity.LEFT
            params.format = android.graphics.PixelFormat.TRANSLUCENT
            params.flags = android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            params.windowAnimations = 0
            params.setTitle("OneuiXP Combo")
            view.isClickable = false
            view.isFocusable = false
            view.setWillNotDraw(false)
        }

        fun updateFrom(cfg: Preference.SystemUI.StatusBarComboRuntime) {
            view.updateFromRuntime(cfg)
            syncGeometry(cfg)
        }

        private fun syncGeometry(cfg: Preference.SystemUI.StatusBarComboRuntime) {
            if (!attached || windowManager == null || !host.isAttachedToWindow) return
            val density = context.resources.displayMetrics.density
            val side = (24f * density * (cfg.sizePercent.coerceIn(50f, 250f) / 100f))
                .coerceIn(12f * density, 72f * density)
            val extra = if (cfg.signalMode == 1 && cfg.showMobile) {
                34f * density
            } else if (cfg.mobileTypeMode == 2 && network5g) {
                24f * density
            } else 0f
            val width = (side + extra).roundToInt().coerceAtLeast(1)
            val height = side.roundToInt().coerceAtLeast(1)

            val location = IntArray(2)
            host.getLocationOnScreen(location)
            val x = location[0] + ((host.width - side) * 0.5f).roundToInt() +
                (cfg.offsetXDp * density).roundToInt()
            val y = location[1] + ((host.height - side) * 0.5f).roundToInt() +
                (cfg.offsetYDp * density).roundToInt()

            if (params.width != width || params.height != height ||
                params.x != x || params.y != y
            ) {
                params.width = width
                params.height = height
                params.x = x
                params.y = y
                runCatching { windowManager?.updateViewLayout(view, params) }
            }
            view.invalidate()
        }

        fun attach(cfg: Preference.SystemUI.StatusBarComboRuntime): Boolean {
            if (attached) {
                syncGeometry(cfg)
                return true
            }
            val wm = windowManager ?: return false
            return runCatching {
                val density = context.resources.displayMetrics.density
                val side = (24f * density * (cfg.sizePercent.coerceIn(50f, 250f) / 100f))
                    .coerceIn(12f * density, 72f * density)
                params.width = side.roundToInt()
                params.height = side.roundToInt()
                wm.addView(view, params)
                attached = true
                syncVisibility()
                syncGeometry(cfg)
                watchHandler.removeCallbacks(visibilityWatch)
                watchHandler.postDelayed(visibilityWatch, 200L)
                true
            }.getOrElse {
                false
            }
        }

        fun detach() {
            if (!attached) return
            attached = false
            watchHandler.removeCallbacks(visibilityWatch)
            runCatching { windowManager?.removeViewImmediate(view) }
        }

        private fun syncVisibility() {
            if (!attached) return
            val visible = host.isShown &&
                host.getWindowVisibility() == View.VISIBLE &&
                host.alpha > 0f &&
                host.getGlobalVisibleRect(android.graphics.Rect())
            val target = if (visible && StatusBarComboRuntime.config.enabled) 1f else 0f
            if (view.alpha != target) {
                view.alpha = target
            }
        }

        private fun resolveWindowType(): Int {
            return runCatching {
                android.view.WindowManager.LayoutParams::class.java
                    .getField("TYPE_STATUS_BAR_ADDITIONAL")
                    .getInt(null)
            }.getOrElse { 2038 }
        }
    }

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val batteryReceiverStarted = AtomicBoolean(false)
    private val previewQueued = AtomicBoolean(false)
    @Volatile private var pendingPreview: Preference.SystemUI.StatusBarComboRuntime? = null
    private val wifiReceiverStarted = AtomicBoolean(false)
    private val defaultDataReceiverStarted = AtomicBoolean(false)

    private val holders = WeakHashMap<View, Holder>()
    private val knownStatusBars = WeakHashMap<View, Boolean>()
    private val comboIgnoredSlots = WeakHashMap<ViewGroup, MutableSet<String>>()
    private val signalStates = LinkedHashMap<Int, SignalState>()
    private val callbacks = LinkedHashMap<Int, Pair<TelephonyManager, TelephonyCallback>>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var config = Preference.SystemUI.StatusBarComboRuntime()

    @Volatile
    private var indicatorColor = Color.WHITE

    @Volatile
    private var batteryLevel = 0

    @Volatile
    private var charging = false

    @Volatile
    private var wifiConnected = false

    @Volatile
    private var wifiLevel = 0

    @Volatile
    private var network5g = false

    @Volatile
    private var networkTypeLabel = ""

    @Volatile
    private var quickCharging = false

    @Volatile
    private var powerSave = false

    @Volatile
    private var defaultDataSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    private var preferenceThread: HandlerThread? = null
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var wifiReceiver: BroadcastReceiver? = null
    private var subscriptionManager: SubscriptionManager? = null
    private var subscriptionListener: SubscriptionManager.OnSubscriptionsChangedListener? = null

    fun isEnabled(): Boolean = config.enabled

    fun isMobileNativeSuppressed(): Boolean {
        val c = config
        return c.enabled && c.showMobile
    }

    private fun debugLog(message: String) {
        if (config.debugLog) {
            runCatching { Log.d("OneuiXP.Combo", message) }
        }
    }


    fun install(
        module: XposedModule,
        param: XposedModuleInterface.PackageReadyParam,
    ) {
        if (param.packageName != Package.SYSTEMUI) return
        // Read Samsung's resolved mobile strength only from mobile-state events;
        // never hook the global StatusBarIconView update path.
        installNativeMobileStateHook(module, param.classLoader)
        installNativeSignalPolicyHook(module, param.classLoader)
        installStatusIconContainerChildHook(module, param.classLoader)
        installSystemUIApplicationReceiverHook(module, param.classLoader)
        installBatteryStateHook(module, param.classLoader)
        if (!installed.compareAndSet(false, true)) return

        runCatching {
            with(module) { PreferenceProvider.loadPreference() }
                ?.systemUI?.statusBar?.comboRuntime
                ?.let { config = sanitize(it) }
        }

        runCatching {
            val clazz = param.classLoader.loadClass(PHONE_STATUS_BAR_VIEW)
            clazz.declaredConstructors.forEach { constructor ->
                module.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    (chain.thisObject as? View)?.let { view ->
                        synchronized(knownStatusBars) {
                            knownStatusBars[view] = true
                        }
                        view.post {
                            registerStatusBar(module, view)
                            ensurePreferenceWatcher(module, view.context)
                            ensureBatteryReceiver(view.context)
                            ensureWifiReceiver(view.context)
                            ensureTelephony(view.context)
                        }
                        view.postDelayed({ registerStatusBar(module, view) }, 250L)
                        view.postDelayed({ registerStatusBar(module, view) }, 1200L)
                    }
                    result
                }
            }
        }

    }

    private fun installSystemUIApplicationReceiverHook(
        module: XposedModule,
        classLoader: ClassLoader,
    ) {
        runCatching {
            val clazz = classLoader.loadClass("com.android.systemui.SystemUIApplication")
            clazz.declaredMethods.firstOrNull {
                it.name == "onCreate" && it.parameterTypes.isEmpty()
            }?.let { method ->
                module.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    (chain.thisObject as? Context)?.let {
                        ensurePreferenceWatcher(module, it)
                    }
                    result
                }
            }
        }
    }

    private fun installBatteryStateHook(
        module: XposedModule,
        classLoader: ClassLoader,
    ) {
        val candidates = listOf(
            "com.android.systemui.statusbar.phone.MiuiBatteryMeterView",
            "com.android.systemui.statusbar.views.MiuiBatteryMeterView",
            "com.android.systemui.statusbar.views.MiuiBatteryMeterIconView",
        )
        candidates.forEach { name ->
            runCatching {
                val clazz = classLoader.loadClass(name)
                clazz.declaredMethods
                    .filter {
                        (it.name == "updateChargeAndText" ||
                            it.name == "updateBatteryState" ||
                            it.name == "updateState") &&
                            !it.isSynthetic
                    }
                    .distinctBy { it.toGenericString() }
                    .forEach { method ->
                        module.hook(method).intercept { chain ->
                            val result = chain.proceed()
                            val self = chain.thisObject ?: return@intercept result
                            var changed = false
                            readBooleanField(self, "mQuickCharging", "quickCharging")?.let {
                                if (it != quickCharging) {
                                    quickCharging = it
                                    changed = true
                                }
                            }
                            readBooleanField(self, "mPowerSave", "powerSave")?.let {
                                if (it != powerSave) {
                                    powerSave = it
                                    changed = true
                                }
                            }
                            if (changed) mainHandler.post { applyAll() }
                            result
                        }
                    }
            }
        }
    }

    private fun readBooleanField(target: Any, vararg names: String): Boolean? {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            names.forEach { wanted ->
                type.declaredFields.firstOrNull { it.name.equals(wanted, true) }?.let { field ->
                    return runCatching {
                        field.isAccessible = true
                        field.get(target) as? Boolean
                    }.getOrNull()
                }
            }
            type = type.superclass
        }
        return null
    }

    private fun installNativeSignalPolicyHook(
        module: XposedModule,
        classLoader: ClassLoader,
    ) {
        val candidates = listOf(
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicyImpl",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicyKt",
        )
        candidates.forEach { name ->
            runCatching {
                val clazz = classLoader.loadClass(name)
                clazz.declaredMethods
                    .filter {
                        it.name == "setMobileDataIndicators" ||
                            it.name == "setMobileIcons"
                    }
                    .distinctBy { it.toGenericString() }
                    .forEach { method ->
                        module.hook(method).intercept { chain ->
                            val result = chain.proceed()
                            if (!config.enabled) return@intercept result
                            chain.args.forEach { arg ->
                                extractNativeMobileStates(arg)
                            }
                            result
                        }
                    }
            }
        }
    }

    private fun extractNativeMobileStates(arg: Any?) {
        when (arg) {
            null -> Unit
            is Iterable<*> -> arg.take(8).forEach { extractNativeMobileStates(it) }
            is Array<*> -> arg.take(8).forEach { extractNativeMobileStates(it) }
            else -> {
                val simple = arg.javaClass.simpleName.lowercase()
                if (!simple.contains("mobileiconstate") &&
                    !simple.contains("mobileicon") &&
                    !simple.contains("iconstate")
                ) return

                val subId = readIntField(
                    arg,
                    "subId", "subscriptionId", "mSubId", "mSubscriptionId",
                ) ?: return
                val strength = readIntField(
                    arg,
                    "strengthId", "mStrengthId", "icon", "mIcon",
                ) ?: return

                val resources = synchronized(holders) {
                    holders.values.firstOrNull()?.statusBarView?.resources
                } ?: return
                val level = if (strength in 0..4) {
                    strength
                } else {
                    resolveSignalResourceLevelFromResources(resources, strength)
                }
                if (level >= 0) {
                    updateSignal(subId, level = level)
                }
            }
        }
    }

    private fun resolveSignalResourceLevelFromResources(
        resources: android.content.res.Resources,
        resId: Int,
    ): Int {
        val name = runCatching { resources.getResourceEntryName(resId) }
            .getOrNull().orEmpty().lowercase()
        if (!name.startsWith("stat_sys_signal") || name.contains("wifi")) return -1
        val signalIndex = name.indexOf("signal")
        if (signalIndex < 0) return -1
        val tail = name.substring(signalIndex + 6).trimStart('_')
        val digit = tail.firstOrNull { it in '0'..'4' } ?: return -1
        return digit - '0'
    }

    private fun installStatusIconContainerChildHook(
        module: XposedModule,
        classLoader: ClassLoader,
    ) {
        val candidates = listOf(
            "com.android.systemui.statusbar.StatusIconContainer",
            "com.android.systemui.statusbar.phone.StatusIconContainer",
            "com.android.systemui.statusbar.StatusBarIconContainer",
            "com.android.systemui.statusbar.phone.StatusBarIconContainer",
        )
        candidates.forEach { name ->
            runCatching {
                val clazz = classLoader.loadClass(name)
                clazz.declaredMethods
                    .filter {
                        it.name == "onViewAdded" &&
                            it.parameterTypes.size == 1 &&
                            View::class.java.isAssignableFrom(it.parameterTypes[0])
                    }
                    .distinctBy { it.toGenericString() }
                    .forEach { method ->
                        module.hook(method).intercept { chain ->
                            val result = chain.proceed()
                            if (!config.enabled) return@intercept result
                            val child = chain.args.firstOrNull() as? View
                                ?: return@intercept result
                            val container = chain.thisObject as? ViewGroup
                                ?: return@intercept result

                            val statusBar = findOwningStatusBar(container) ?: return@intercept result
                            val holder = synchronized(holders) { holders[statusBar] }
                                ?: return@intercept result
                            syncIgnoredSlots(container, true)
                            result
                        }
                    }
            }
        }
    }

    private fun findOwningStatusBar(view: View): ViewGroup? {
        var current: View? = view
        while (current != null) {
            synchronized(knownStatusBars) {
                if (knownStatusBars.containsKey(current)) {
                    return current as? ViewGroup
                }
            }
            current = current.parent as? View
        }
        return null
    }


    private fun installNativeMobileStateHook(
        module: XposedModule,
        classLoader: ClassLoader,
    ) {
        val candidates = listOf(
            "com.android.systemui.statusbar.StatusBarMobileView",
            "com.android.systemui.statusbar.phone.StatusBarMobileView",
            "com.android.systemui.statusbar.views.StatusBarMobileView",
            "com.android.systemui.statusbar.phone.ModernStatusBarMobileView",
        )
        val methodsToHook = setOf("applyMobileState", "updateState")
        candidates.forEach { name ->
            runCatching {
                val clazz = classLoader.loadClass(name)
                clazz.declaredMethods
                    .filter { it.name in methodsToHook && it.parameterTypes.size == 1 }
                    .distinctBy { it.toGenericString() }
                    .forEach { method ->
                        module.hook(method).intercept { chain ->
                            val result = chain.proceed()
                            if (!config.enabled) return@intercept result

                            val view = chain.thisObject as? View
                                ?: return@intercept result
                            val state = chain.args.firstOrNull()
                                ?: return@intercept result
                            val strength = readIntField(state, "strengthId", "mStrengthId")
                                ?: return@intercept result
                            val subId = readIntField(state, "subId", "mSubId")
                            val level = when {
                                strength in 0..4 -> strength
                                else -> resolveSignalResourceLevel(view, strength)
                            }
                            if (level >= 0) {
                                updateSignal(
                                    subId ?: defaultDataSubscriptionId,
                                    level = level,
                                )
                            }
                            result
                        }
                    }
            }
        }
    }

    private fun readIntField(target: Any, vararg names: String): Int? {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            names.forEach { wanted ->
                type.declaredFields.firstOrNull { it.name.equals(wanted, true) }?.let { field ->
                    return runCatching {
                        field.isAccessible = true
                        (field.get(target) as? Number)?.toInt()
                    }.getOrNull()
                }
            }
            type = type.superclass
        }
        return null
    }

    private fun resolveSignalResourceLevel(view: View, resId: Int): Int {
        val name = runCatching {
            view.resources.getResourceEntryName(resId)
        }.getOrNull().orEmpty().lowercase()
        if (!name.startsWith("stat_sys_signal") || name.contains("wifi")) return -1
        val index = name.indexOf("signal")
        if (index < 0) return -1
        val tail = name.substring(index + 6).trimStart('_')
        val digit = tail.firstOrNull { it in '0'..'4' } ?: return -1
        return digit - '0'
    }

    private fun slotOf(view: View): String {
        return runCatching {
            view.javaClass.methods.firstOrNull {
                it.name == "getSlot" && it.parameterTypes.isEmpty()
            }?.let { method ->
                method.isAccessible = true
                (method.invoke(view) as? String).orEmpty()
            }.orEmpty()
        }.getOrDefault("")
    }

    private fun findIgnoredSlotsField(container: Any): java.lang.reflect.Field? {
        var type: Class<*>? = container.javaClass
        while (type != null) {
            type.declaredFields.firstOrNull {
                it.name.equals("mIgnoredSlots", true) ||
                    it.name.equals("ignoredSlots", true)
            }?.let {
                it.isAccessible = true
                return it
            }
            type = type.superclass
        }
        return null
    }

    private fun desiredIgnoredSlots(): Set<String> {
        val c = config
        if (!c.enabled) return emptySet()
        val wanted = LinkedHashSet<String>()
        if (c.showWifi) wanted += "wifi"
        if (c.showMobile) {
            wanted += "mobile"
            wanted += "stacked_mobile"
        }
        if (c.showBattery) {
            wanted += "battery"
        }
        return wanted
    }

    private fun currentIgnoredSlots(container: ViewGroup): MutableList<String>? {
        return runCatching {
            findIgnoredSlotsField(container)?.get(container) as? MutableList<String>
        }.getOrNull()
    }

    private fun statusSlots(container: ViewGroup): Set<String> {
        val slots = LinkedHashSet<String>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) ?: continue
            when (slotOf(child).lowercase()) {
                "wifi", "mobile", "stacked_mobile", "battery" -> slots += slotOf(child).lowercase()
            }
        }
        return slots
    }

    private fun syncIgnoredSlots(container: ViewGroup, enabled: Boolean) {
        val ignored = currentIgnoredSlots(container) ?: return
        val added = synchronized(comboIgnoredSlots) {
            comboIgnoredSlots[container] ?: LinkedHashSet<String>().also {
                comboIgnoredSlots[container] = it
            }
        }
        val wanted = if (enabled) desiredIgnoredSlots() else emptySet()
        var changed = false

        // Remove only slots previously managed by OneuiXP.
        added.toList().forEach { slot ->
            if (!wanted.contains(slot) && ignored.remove(slot)) {
                changed = true
            }
            if (!wanted.contains(slot)) {
                added.remove(slot)
            }
        }
        // Add newly requested slots.
        wanted.forEach { slot ->
            if (!ignored.contains(slot)) {
                ignored.add(slot)
                added.add(slot)
                changed = true
            }
        }

        if (added.isEmpty()) {
            synchronized(comboIgnoredSlots) { comboIgnoredSlots.remove(container) }
        }
        if (changed) container.requestLayout()
    }


    private fun currentSlotLevels(): IntArray {
        val out = IntArray(2) { -1 }
        synchronized(signalStates) {
            signalStates.values.sortedBy { it.slotIndex }.forEach { state ->
                if (state.slotIndex in 0..1) out[state.slotIndex] = state.level.coerceIn(0, 4)
            }
        }
        return out
    }

    private fun dataSlotIndex(): Int =
        synchronized(signalStates) {
            signalStates.values.firstOrNull {
                it.subscriptionId == defaultDataSubscriptionId
            }?.slotIndex ?: -1
        }

    private fun sanitize(source: Preference.SystemUI.StatusBarComboRuntime) = source.copy(
        trioStyle = source.trioStyle.coerceIn(0, 1),
        mobileTypeMode = source.mobileTypeMode.coerceIn(0, 2),
        signalMode = source.signalMode.coerceIn(0, 1),
        ringStroke = source.ringStroke.coerceIn(4, 16),
        arcStroke = source.arcStroke.coerceIn(3, 16),
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
        lowBatteryThreshold = source.lowBatteryThreshold.coerceIn(5, 50),
        trackAlpha = source.trackAlpha.coerceIn(0, 255),
        sizePercent = source.sizePercent.coerceIn(50f, 250f),
        offsetXDp = source.offsetXDp.coerceIn(-24f, 24f),
        offsetYDp = source.offsetYDp.coerceIn(-15f, 15f),
        arcStrokePercent = source.arcStrokePercent.coerceIn(50f, 200f),
        batteryBlackDepth = source.batteryBlackDepth.coerceIn(0.15f, 1f),
        dotsGapDp = source.dotsGapDp.coerceIn(0f, 8f),
        dotsSizePercent = source.dotsSizePercent.coerceIn(50f, 180f),
        innerSizePercent = source.innerSizePercent.coerceIn(50f, 180f),
        innerYDp = source.innerYDp.coerceIn(-8f, 8f),
        batteryTextSizeSp = source.batteryTextSizeSp.coerceIn(5f, 18f),
        batteryTextFontWeight = source.batteryTextFontWeight.coerceIn(100, 900),
        chargingBoltSizePercent = source.chargingBoltSizePercent.coerceIn(50f, 180f),
        chargingBoltXDp = source.chargingBoltXDp.coerceIn(-8f, 8f),
        chargingBoltYDp = source.chargingBoltYDp.coerceIn(-8f, 8f),
    )

    private fun registerStatusBar(module: XposedModule, raw: View) {
        val statusBar = raw as? ViewGroup ?: return
        synchronized(knownStatusBars) {
            knownStatusBars[statusBar] = true
        }

        val existing = synchronized(holders) { holders[statusBar] }
        if (existing != null) {
            applyHolder(existing)
            return
        }

        val parent = findRightIndicatorArea(statusBar) ?: run {
            // The view exists but its icon area may be inflated a little later.
            // Keep the candidate so the next live preference change can retry.
            statusBar.post { registerStatusBar(module, statusBar) }
            return
        }

        val context = statusBar.context
        val icon = ComboIconView(context).apply {
            isClickable = false
            isFocusable = false
            setWillNotDraw(false)
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            isClickable = false
            isFocusable = false
            addView(
                icon,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        parent.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val holder = Holder(statusBar, parent, root, icon)
        synchronized(holders) { holders[statusBar] = holder }

        statusBar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (config.enabled && config.overlayGlyph) {
                holder.overlay?.updateFrom(config)
            }
        }

        ensurePreferenceWatcher(module, context)
        ensureBatteryReceiver(context)
        ensureWifiReceiver(context)
        ensureTelephony(context)
        applyHolder(holder)
    }

    private fun findRightIndicatorArea(statusBar: View): ViewGroup? {
        val resources = statusBar.resources
        listOf(
            "system_icon_area",
            "status_bar_right_side",
            "status_bar_end_side_content",
            "status_icon_area",
            "status_bar_end_side",
        ).forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                (statusBar.findViewById<View>(id) as? ViewGroup)?.let { return it }
            }
        }
        return null
    }

    /**
     * The combo must replace the real Samsung battery child, not merely be
     * inserted into a guessed right-side container. This gives us the exact
     * sibling index Samsung assigned to BatteryMeterView on that firmware.
     */
    private fun findBatteryContainer(statusBar: View): ViewGroup? {
        val resources = statusBar.resources
        val candidateNames = listOf(
            "battery",
            "battery_icon",
            "battery_meter_view",
            "battery_meter",
            "status_bar_battery",
            "status_bar_battery_icon",
        )

        fun batteryLike(view: View): Boolean {
            val descriptor = buildString {
                if (view.id != View.NO_ID) {
                    runCatching { resources.getResourceEntryName(view.id) }
                        .getOrNull()?.let { append(it).append(' ') }
                }
                append(view.javaClass.name)
            }
            val value = descriptor.lowercase()
            return value.contains("batterymeterview") ||
                (value.contains("battery") && value.contains("meter"))
        }

        fun visit(group: ViewGroup): ViewGroup? {
            for (index in 0 until group.childCount) {
                val child = group.getChildAt(index)
                if (candidateNames.any { name ->
                        val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
                        id != 0 && child.id == id
                    } || batteryLike(child)
                ) {
                    return child.parent as? ViewGroup
                }
                if (child is ViewGroup) {
                    visit(child)?.let { return it }
                }
            }
            return null
        }
        return visit(statusBar as? ViewGroup ?: return null)
    }

    private fun mountAtBatterySlot(holder: Holder) {
        val parent = holder.parent
        val anchor = findBatteryAnchor(holder.statusBarView, parent)
        if (anchor != null) {
            val anchorIndex = parent.indexOfChild(anchor)
            val currentIndex = parent.indexOfChild(holder.root)
            if (anchorIndex >= 0 && currentIndex != anchorIndex) {
                if (holder.root.parent === parent) parent.removeView(holder.root)
                val refreshed = findBatteryAnchor(holder.statusBarView, parent)
                val index = refreshed?.let(parent::indexOfChild)?.takeIf { it >= 0 }
                    ?: parent.childCount
                parent.addView(
                    holder.root,
                    index,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
            return
        }

        // Battery child can be recreated by SystemUI. During that short window,
        // keep the combo as the final sibling and re-check after layout.
        if (holder.root.parent !== parent) {
            parent.addView(
                holder.root,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        if (parent.indexOfChild(holder.root) != parent.childCount - 1) {
            parent.removeView(holder.root)
            parent.addView(holder.root)
        }
    }

    private fun findBatteryAnchor(statusBar: ViewGroup, parent: ViewGroup): View? {
        val resources = statusBar.resources
        val names = listOf(
            "battery",
            "battery_icon",
            "battery_meter_view",
            "battery_meter",
            "status_bar_battery",
            "status_bar_battery_icon",
        )
        fun visit(group: ViewGroup): View? {
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                val descriptor = buildString {
                    if (child.id != View.NO_ID) {
                        runCatching { child.resources.getResourceEntryName(child.id) }
                            .getOrNull()?.let { append(it).append(' ') }
                    }
                    append(child.javaClass.name).append(' ')
                }.lowercase()
                if (names.any { it == descriptor.trim() } ||
                    descriptor.contains("batterymeterview") ||
                    (descriptor.contains("battery") && descriptor.contains("meter"))
                ) {
                    directChildForParent(child, parent)?.let { return it }
                }
                if (child is ViewGroup) visit(child)?.let { return it }
            }
            return null
        }
        return visit(statusBar)
    }

    private fun directChildForParent(view: View?, parent: ViewGroup): View? {
        var current = view ?: return null
        var owner = current.parent as? ViewGroup
        while (owner != null && owner !== parent) {
            current = owner
            owner = current.parent as? ViewGroup
        }
        return current.takeIf { owner === parent }
    }

    private fun applyVisualOnly(next: Preference.SystemUI.StatusBarComboRuntime) {
        val old = config
        config = next

        val pending = synchronized(knownStatusBars) { knownStatusBars.keys.toList() }
        pending.forEach { statusBar ->
            registerStatusBarFromPreview(statusBar)
        }

        val active = synchronized(holders) {
            holders.values.filter { it.statusBarView.isAttachedToWindow }.toList()
        }
        val ownershipChanged =
            old.enabled != next.enabled ||
            old.showWifi != next.showWifi ||
            old.showMobile != next.showMobile ||
            old.showBattery != next.showBattery ||
            old.signalMode != next.signalMode ||
            old.stackedSignal != next.stackedSignal

        active.forEach { holder ->
            if (ownershipChanged) {
                if (!next.enabled) {
                    restoreHidden(holder)
                    holder.nativeSuppressed = false
                } else {
                    restoreDisabledComponents(holder, next)
                    holder.nativeSuppressed = false
                }
            }
            applyHolder(holder)
            if (next.overlayGlyph) {
                holder.overlay?.updateFrom(next)
            }
            if (old.sizePercent != next.sizePercent ||
                old.signalMode != next.signalMode ||
                old.stackedSignal != next.stackedSignal ||
                old.trioStyle != next.trioStyle
            ) {
                holder.icon.requestLayout()
            } else {
                holder.icon.invalidate()
            }
        }
    }

    private fun restoreDisabledComponents(
        holder: Holder,
        next: Preference.SystemUI.StatusBarComboRuntime,
    ) {
        var restored = false
        val entries = holder.hiddenNative.toList()
        entries.forEach { (view, state) ->
            val slot = slotOf(view).lowercase()
            val descriptor = describeView(view)
            val wifiItem = slot == "wifi" || descriptor.contains("statusbarwif")
            val mobileItem = slot == "mobile" || slot == "stacked_mobile" ||
                descriptor.contains("statusbarmobile") ||
                descriptor.contains("modernstatusbarmobile") ||
                descriptor.contains("mobilesignal") ||
                descriptor.contains("signalcluster") ||
                descriptor.contains("mobileicon")
            val batteryItem = slot == "battery" ||
                descriptor.contains("batterymeterview") ||
                descriptor.contains("statusbarbattery") ||
                (descriptor.contains("battery") && descriptor.contains("meter"))
            val mobileSuppressed = next.showMobile
            val shouldRestore =
                (!next.showWifi && wifiItem) ||
                ((!mobileSuppressed) && mobileItem) ||
                ((!next.showBattery) && batteryItem)

            if (shouldRestore && view.parent != null) {
                if (view.alpha != state.alpha || view.visibility != state.visibility) {
                    view.alpha = state.alpha
                    view.visibility = state.visibility
                    restored = true
                }
                holder.hiddenNative.remove(view)
            }
        }
        if (restored) {
            holder.parent.requestLayout()
            holder.statusBarView.requestLayout()
        }
    }

    private fun registerStatusBarFromPreview(statusBar: View) {
        if (!statusBar.isAttachedToWindow) return
        val existing = synchronized(holders) { holders[statusBar] }
        if (existing != null) {
            applyHolder(existing)
            return
        }
        // A no-op self-registration is enough to reuse all guarded initialization
        // from the lifecycle hook without adding a second global callback.
        val parent = findRightIndicatorArea(statusBar)
        if (parent == null) {
            statusBar.post { registerStatusBarFromPreview(statusBar) }
            return
        }
        val context = statusBar.context
        val icon = ComboIconView(context).apply {
            isClickable = false
            isFocusable = false
            setWillNotDraw(false)
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            isClickable = false
            isFocusable = false
            addView(
                icon,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        parent.addView(root)
        val holder = Holder(statusBar as ViewGroup, parent, root, icon)
        synchronized(holders) { holders[statusBar] = holder }
        ensureBatteryReceiver(context)
        ensureWifiReceiver(context)
        ensureTelephony(context)
        applyHolder(holder)
    }

    private fun currentSignalLevel(): Int {
        val preferredSubId = defaultDataSubscriptionId
        synchronized(signalStates) {
            val preferred = signalStates[preferredSubId]
            if (preferred != null) {
                return preferred.level.coerceIn(0, 4)
            }
            return signalStates.values
                .sortedBy { it.slotIndex }
                .firstOrNull()
                ?.level
                ?.coerceIn(0, 4)
                ?: 0
        }
    }


    private fun applyAll() {
        val active = synchronized(holders) {
            holders.values.filter { it.statusBarView.isAttachedToWindow }.toList()
        }
        active.forEach(::applyHolder)
    }

    private fun applyHolder(holder: Holder) {
        val current = config
        val density = holder.root.resources.displayMetrics.density

        if (current.debugLog) {
            debugLog(
                "combo enable=" + current.enabled +
                    " wifi=" + current.showWifi +
                    " mobile=" + current.showMobile +
                    " style=" + current.trioStyle +
                    " signalMode=" + current.signalMode +
                    " stacked=" + current.stackedSignal +
                    " dataOnly=" + current.dataSimOnly
            )
        }

        var overlayReady = false
        if (current.enabled && current.overlayGlyph) {
            if (holder.overlay == null) {
                holder.overlay = ComboOverlay(
                    holder.statusBarView,
                    holder.statusBarView.context,
                )
            }
            overlayReady = holder.overlay?.attach(current) == true
            if (overlayReady) {
                holder.overlay?.updateFrom(current)
            }
        } else {
            holder.overlay?.detach()
            holder.overlay = null
        }

        val wantedVisibility = if (current.enabled && !overlayReady) {
            View.VISIBLE
        } else {
            View.GONE
        }
        if (holder.root.visibility != wantedVisibility) {
            holder.root.visibility = wantedVisibility
        }

        val targetX = current.offsetXDp.coerceIn(-24f, 24f) * density
        val targetY = current.offsetYDp.coerceIn(-15f, 15f) * density
        if (holder.root.translationX != targetX) holder.root.translationX = targetX
        if (holder.root.translationY != targetY) holder.root.translationY = targetY

        holder.icon.updateFromRuntime(current)

        if (current.enabled) {
            if (!holder.mounted) {
                mountAtBatterySlot(holder)
                holder.mounted = true
            }
            if (!holder.nativeSuppressed) {
                hideNativeAndCustomIcons(holder)
                holder.nativeSuppressed = true
            }

            if (current.overlayGlyph) {
                if (holder.overlay == null) {
                    holder.overlay = ComboOverlay(holder.statusBarView, holder.statusBarView.context)
                }
                val attached = holder.overlay?.attach(current) == true
                if (attached) {
                    holder.overlay?.updateFrom(current)
                }
            } else {
                holder.overlay?.detach()
                holder.overlay = null
            }
        } else {
            holder.overlay?.detach()
            holder.overlay = null
            if (holder.nativeSuppressed || holder.hiddenNative.isNotEmpty()) {
                restoreHidden(holder)
                holder.nativeSuppressed = false
                holder.mounted = false
            }
        }
    }


    private fun hideNativeAndCustomIcons(holder: Holder) {
        var changed = false
        val container = findStatusIconContainer(holder.statusBarView)
        if (container != null && currentIgnoredSlots(container) != null) {
            syncIgnoredSlots(container, true)
        }

        // Battery is often outside StatusIconContainer. Collapse only that anchor;
        // Wi-Fi/mobile are handled by the slot ignore list so they leave no gap.
        val batteryParent = findBatteryContainer(holder.statusBarView)
        val batteryAnchor = batteryParent?.let {
            findBatteryAnchor(holder.statusBarView, it)
        }
        if (config.showBattery && batteryAnchor != null &&
            batteryAnchor !== holder.root &&
            !isDescendantOf(batteryAnchor, holder.root)
        ) {
            if (!holder.hiddenNative.containsKey(batteryAnchor)) {
                holder.hiddenNative[batteryAnchor] =
                    HiddenState(batteryAnchor.alpha, batteryAnchor.visibility)
            }
            if (batteryAnchor.visibility != View.GONE) {
                batteryAnchor.visibility = View.GONE
                changed = true
            }
        }

        // Fallback only for Samsung layouts without an ignored-slot list.
        if (container == null || currentIgnoredSlots(container) == null) {
            collectDescendants(holder.statusBarView).forEach { view ->
                if (view === holder.root || isDescendantOf(view, holder.root)) return@forEach
                if (!isComboNativeStatusItem(view)) return@forEach
                if (!holder.hiddenNative.containsKey(view)) {
                    holder.hiddenNative[view] = HiddenState(view.alpha, view.visibility)
                }
                if (view.visibility != View.GONE) {
                    view.visibility = View.GONE
                    changed = true
                }
            }
        }

        if (changed) {
            holder.parent.requestLayout()
            holder.statusBarView.requestLayout()
        }
    }


    private fun isComboNativeStatusItem(view: View): Boolean {
        val slot = slotOf(view).lowercase()
        val descriptor = describeView(view)

        if (descriptor.contains("statusbarsignalindependentruntime") ||
            descriptor.contains("independentsignalview") ||
            descriptor.contains("oneuixp")
        ) return false

        val isWifi = slot == "wifi" ||
            descriptor.contains("statusbarwifiview") ||
            descriptor.contains("modernstatusbarwifiview") ||
            descriptor.contains("wifiglyphview") ||
            descriptor.contains("wifi_group")

        val isMobile = slot == "mobile" || slot == "stacked_mobile" ||
            descriptor.contains("statusbarmobileview") ||
            descriptor.contains("modernstatusbarmobileview") ||
            descriptor.contains("mobilesignalview") ||
            descriptor.contains("mobileicon") ||
            descriptor.contains("singlebindablestatusbarcomposeiconview") ||
            descriptor.contains("signalcluster") ||
            descriptor.contains("mobile_group")

        val isBattery = config.showBattery && (
            descriptor.contains("batterymeterview") ||
                descriptor.contains("statusbarbattery") ||
                (descriptor.contains("battery") && descriptor.contains("meter"))
            )

        val mobileSuppressed = config.showMobile

        return (config.showWifi && isWifi) ||
            (mobileSuppressed && isMobile) ||
            isBattery
    }


    private fun collapseNativeBattery(statusBar: ViewGroup?) {
        if (statusBar == null) return
        val parent = findBatteryContainer(statusBar) ?: return
        val anchor = findBatteryAnchor(statusBar, parent) ?: return
        val holder = synchronized(holders) {
            holders[statusBar]
        } ?: return
        if (anchor === holder.root || isDescendantOf(anchor, holder.root)) return
        var changed = false
        if (!holder.hiddenNative.containsKey(anchor)) {
            holder.hiddenNative[anchor] = HiddenState(anchor.alpha, anchor.visibility)
            changed = true
        }
        if (anchor.visibility != View.GONE) {
            anchor.visibility = View.GONE
            changed = true
        }
        if (changed) {
            parent.requestLayout()
        }
    }

    private fun findStatusIconContainer(statusBar: ViewGroup): ViewGroup? {
        var found: ViewGroup? = null
        fun visit(group: ViewGroup) {
            if (found != null) return
            val name = group.javaClass.name.lowercase()
            if (name.contains("statusiconcontainer") ||
                name.contains("statusbariconcontainer")
            ) {
                found = group
                return
            }
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is ViewGroup) visit(child)
                if (found != null) return
            }
        }
        visit(statusBar)
        return found
    }

    private fun restoreHidden(holder: Holder) {
        val containers = synchronized(comboIgnoredSlots) {
            comboIgnoredSlots.keys.toList()
        }
        containers.forEach { container ->
            syncIgnoredSlots(container, false)
        }
        var restored = false
        holder.hiddenNative.forEach { (view, state) ->
            if (view.parent != null) {
                if (view.alpha != state.alpha || view.visibility != state.visibility) {
                    view.alpha = state.alpha
                    view.visibility = state.visibility
                    restored = true
                }
            }
        }
        holder.hiddenNative.clear()
        if (restored) {
            holder.parent.requestLayout()
            holder.statusBarView.requestLayout()
        }
        holder.parent.invalidate()
    }

    private fun describeView(view: View): String {
        val resourceName = if (view.id != View.NO_ID) {
            runCatching { view.resources.getResourceEntryName(view.id) }
                .getOrNull()
                .orEmpty()
        } else ""
        return buildString {
            append(resourceName).append(' ')
            append(view.javaClass.name).append(' ')
            append(view.contentDescription?.toString().orEmpty())
        }.lowercase()
    }

    private fun isTargetStatusItem(descriptor: String): Boolean {
        val positive = descriptor.contains("battery") ||
            descriptor.contains("wifi") ||
            descriptor.contains("mobile") ||
            descriptor.contains("signal") ||
            descriptor.contains("cellular") ||
            descriptor.contains("cell signal") ||
            descriptor.contains("network_type") ||
            descriptor.contains("networktype") ||
            descriptor.contains("statusbarmobileview") ||
            descriptor.contains("modernstatusbarmobileview") ||
            descriptor.contains("mobileicon") ||
            descriptor.contains("signalcluster") ||
            descriptor.contains("mobile_signal") ||
            descriptor.contains("mobile_combo") ||
            descriptor.contains("mobile_group") ||
            descriptor.contains("statusbarsignalindependentruntime") ||
            descriptor.contains("independentsignalview") ||
            descriptor.contains("statusbarwifiruntime") ||
            descriptor.contains("wifiglyphview")
        val negative = descriptor.contains("bluetooth") ||
            descriptor.contains("netspeed") ||
            descriptor.contains("network_speed") ||
            descriptor.contains("clock")
        return positive && !negative
    }

    private fun isCollapsibleStatusItem(view: View): Boolean {
        if (view.id == View.NO_ID) return false
        val descriptor = describeView(view)
        if (descriptor.contains("bluetooth") ||
            descriptor.contains("netspeed") ||
            descriptor.contains("network_speed") ||
            descriptor.contains("clock")
        ) return false

        if (view is ViewGroup) {
            val descendants = collectDescendants(view)
            val targeted = descendants.count { isTargetStatusItem(describeView(it)) }
            val protected = descendants.any {
                val d = describeView(it)
                d.contains("bluetooth") ||
                    d.contains("netspeed") ||
                    d.contains("network_speed") ||
                    d.contains("clock")
            }
            return targeted > 0 && !protected
        }
        return isTargetStatusItem(descriptor)
    }

    private fun collectDescendants(root: ViewGroup): List<View> {
        val out = ArrayList<View>()
        fun visit(group: ViewGroup) {
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                out += child
                if (child is ViewGroup) visit(child)
            }
        }
        visit(root)
        return out
    }

    private fun isDescendantOf(view: View, ancestor: View): Boolean {
        var parent = view.parent
        while (parent is View) {
            if (parent === ancestor) return true
            parent = parent.parent
        }
        return false
    }

    private fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (!watcherStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val worker = HandlerThread("OneuiXPComboPrefs").apply { start() }
        preferenceThread = worker
        val background = Handler(worker.looper)

        fun refreshOnce() {
            background.post {
                val next = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }
                        ?.systemUI?.statusBar?.comboRuntime
                }.getOrNull() ?: return@post
                val sanitized = sanitize(next)
                if (sanitized == config) return@post
                config = sanitized
                mainHandler.post { applyAll() }
            }
        }

        val observer = object : ContentObserver(background) {
            override fun onChange(selfChange: Boolean) = refreshOnce()
            override fun onChange(selfChange: Boolean, uri: Uri?) = refreshOnce()
        }
        preferenceObserver = observer
        runCatching {
            appContext.contentResolver.registerContentObserver(
                Uri.parse(PREFERENCE_CHANGED_URI), false, observer,
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != StatusBarRuntimeBridge.ACTION) return
                val payload = intent.getStringExtra(
                    StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON
                ) ?: return
                background.post {
                    val statusBar = runCatching {
                        IgnoreUnknownKeysJson.decodeFromString(
                            Preference.SystemUI.StatusBar.serializer(), payload,
                        )
                    }.getOrNull() ?: return@post
                    val next = sanitize(statusBar.comboRuntime)
                    pendingPreview = next
                    if (previewQueued.compareAndSet(false, true)) {
                        mainHandler.postDelayed({
                            val latest = pendingPreview
                            pendingPreview = null
                            previewQueued.set(false)
                            if (latest != null) applyVisualOnly(latest)
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
        refreshOnce()
    }

    private fun ensureBatteryReceiver(context: Context) {
        if (!batteryReceiverStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val nextLevel = (rawLevel * 100f / scale).roundToInt().coerceIn(0, 100)
                val status = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN,
                )
                val nextCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                val changed = nextLevel != batteryLevel || nextCharging != charging
                batteryLevel = nextLevel
                charging = nextCharging
                if (changed) mainHandler.post { applyAll() }
            }
        }
        batteryReceiver = receiver
        runCatching {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            @Suppress("DEPRECATION")
            val sticky = appContext.registerReceiver(receiver, filter)
            sticky?.let { receiver.onReceive(appContext, it) }
        }
    }

    private fun ensureWifiReceiver(context: Context) {
        if (!wifiReceiverStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.NETWORK_STATE_CHANGED_ACTION ||
                    intent?.action == WifiManager.WIFI_STATE_CHANGED_ACTION
                ) {
                    refreshWifi(appContext)
                }
            }
        }
        wifiReceiver = receiver
        runCatching {
            val filter = IntentFilter().apply {
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(receiver, filter)
            }
        }
        refreshWifi(appContext)
    }

    @Suppress("DEPRECATION")
    private fun refreshWifi(context: Context) {
        val info = runCatching {
            context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo
        }.getOrNull()
        val nextConnected = info != null && info.networkId >= 0 && info.ipAddress != 0
        val nextLevel = if (nextConnected && info != null) {
            WifiManager.calculateSignalLevel(info.rssi, 5).coerceIn(0, 4)
        } else 0
        val changed = nextConnected != wifiConnected || nextLevel != wifiLevel
        wifiConnected = nextConnected
        wifiLevel = nextLevel
        if (changed) mainHandler.post { applyAll() }
    }

    private fun ensureDefaultDataReceiver(context: Context) {
        if (!defaultDataReceiverStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != "android.telephony.action.DEFAULT_DATA_SUBSCRIPTION_CHANGED") return
                defaultDataSubscriptionId = runCatching {
                    SubscriptionManager.getDefaultDataSubscriptionId()
                }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                mainHandler.post { applyAll() }
            }
        }
        runCatching {
            val filter = IntentFilter("android.telephony.action.DEFAULT_DATA_SUBSCRIPTION_CHANGED")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(receiver, filter)
            }
        }
        defaultDataSubscriptionId = runCatching {
            SubscriptionManager.getDefaultDataSubscriptionId()
        }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
    }

    private fun ensureTelephony(context: Context) {
        if (subscriptionManager != null) return
        val appContext = context.applicationContext ?: context
        val manager = appContext.getSystemService(SubscriptionManager::class.java) ?: return
        subscriptionManager = manager
        ensureDefaultDataReceiver(appContext)
        val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
            override fun onSubscriptionsChanged() {
                refreshSubscriptions(appContext)
            }
        }
        subscriptionListener = listener
        runCatching { manager.addOnSubscriptionsChangedListener(listener) }
        refreshSubscriptions(appContext)
    }

    private fun refreshSubscriptions(context: Context) {
        val active = runCatching {
            subscriptionManager?.activeSubscriptionInfoList.orEmpty()
                .sortedBy { it.simSlotIndex }
                .take(2)
        }.getOrDefault(emptyList())

        defaultDataSubscriptionId = runCatching {
            SubscriptionManager.getDefaultDataSubscriptionId()
        }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)

        val wanted = active.map { it.subscriptionId }.toSet()
        val stale = callbacks.keys.filter { it !in wanted }
        stale.forEach { subId ->
            callbacks.remove(subId)?.let { (manager, callback) ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { manager.unregisterTelephonyCallback(callback) }
                }
            }
            synchronized(signalStates) { signalStates.remove(subId) }
        }

        active.forEach { info ->
            synchronized(signalStates) {
                val old = signalStates[info.subscriptionId]
                signalStates[info.subscriptionId] = (old ?: SignalState(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                )).copy(slotIndex = info.simSlotIndex)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !callbacks.containsKey(info.subscriptionId)
            ) {
                val manager = context.getSystemService(TelephonyManager::class.java)
                    .createForSubscriptionId(info.subscriptionId)
                val callback = MobileCallback(info.subscriptionId)
                runCatching {
                    manager.registerTelephonyCallback(context.mainExecutor, callback)
                    callbacks[info.subscriptionId] = manager to callback
                    manager.signalStrength?.let { strength ->
                        val resolvedLevel = resolveSignalLevel(strength)
                        synchronized(signalStates) {
                            val old = signalStates[info.subscriptionId]
                            if (old != null) {
                                signalStates[info.subscriptionId] =
                                    old.copy(level = resolvedLevel)
                            }
                        }
                    }
                    manager.serviceState?.let { service ->
                        synchronized(signalStates) {
                            val old = signalStates[info.subscriptionId]
                            if (old != null) {
                                signalStates[info.subscriptionId] =
                                    old.copy(
                                        inService = service.state == ServiceState.STATE_IN_SERVICE,
                                    )
                            }
                        }
                    }
                }
            }
        }

        refreshNetworkType(context)
        mainHandler.post { applyAll() }
    }

    private class MobileCallback(
        private val subscriptionId: Int,
    ) : TelephonyCallback(),
        TelephonyCallback.SignalStrengthsListener,
        TelephonyCallback.ServiceStateListener,
        TelephonyCallback.DisplayInfoListener {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            updateSignal(
                subscriptionId,
                level = resolveSignalLevel(signalStrength),
            )
        }

        override fun onServiceStateChanged(serviceState: ServiceState) {
            updateSignal(
                subscriptionId,
                inService = serviceState.state == ServiceState.STATE_IN_SERVICE,
            )
        }

        override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
            val advanced = displayInfo.overrideNetworkType ==
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
            val nsa = displayInfo.overrideNetworkType ==
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
            updateSignal(subscriptionId, is5g = advanced || nsa)
        }
    }

    private fun resolveSignalLevel(strength: SignalStrength): Int {
        val aggregate = strength.level.coerceIn(0, 4)
        val cellLevels = runCatching {
            strength.cellSignalStrengths.map { it.level.coerceIn(0, 4) }
        }.getOrDefault(emptyList())
        var resolved = max(aggregate, cellLevels.maxOrNull() ?: 0).coerceIn(0, 4)

        // Samsung can report the Android logical level one step below the glyph
        // while the physical serving cell is already in the strongest range.
        val bestDbm = runCatching {
            strength.cellSignalStrengths
                .mapNotNull { cell ->
                    val dbm = cell.dbm
                    if (dbm == Int.MAX_VALUE || dbm >= 0) null else dbm
                }
                .maxOrNull()
        }.getOrNull()
        val physical = when {
            bestDbm == null -> 0
            bestDbm >= -95 -> 4
            bestDbm >= -105 -> 3
            bestDbm >= -115 -> 2
            bestDbm >= -125 -> 1
            else -> 0
        }
        return max(resolved, physical).coerceIn(0, 4)
    }

    private fun updateSignal(
        subscriptionId: Int,
        level: Int? = null,
        inService: Boolean? = null,
        is5g: Boolean? = null,
    ) {
        var changed = false
        synchronized(signalStates) {
            val old = signalStates[subscriptionId] ?: return
            val next = old.copy(
                level = level ?: old.level,
                inService = inService ?: old.inService,
                is5g = is5g ?: old.is5g,
            )
            if (next != old) {
                signalStates[subscriptionId] = next
                changed = true
            }
        }
        if (changed) {
            mainHandler.post { applyAll() }
        }
    }

    private fun refreshNetworkType(context: Context? = null): Boolean {
        val appContext = context ?: synchronized(holders) {
            holders.values.firstOrNull()?.statusBarView?.context
        } ?: return false

        val preferred = defaultDataSubscriptionId
        val preferredState = synchronized(signalStates) { signalStates[preferred] }

        val nextType = runCatching {
            val manager = appContext.getSystemService(TelephonyManager::class.java)
            val type = if (preferred != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                manager.createForSubscriptionId(preferred).dataNetworkType
            } else {
                manager.dataNetworkType
            }
            when (type) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_GSM,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                else -> if (preferredState?.is5g == true) "5G" else ""
            }
        }.getOrDefault(if (preferredState?.is5g == true) "5G" else "")

        val next5g = nextType == "5G"
        val changed = network5g != next5g || networkTypeLabel != nextType
        network5g = next5g
        networkTypeLabel = nextType
        return changed
    }

}
