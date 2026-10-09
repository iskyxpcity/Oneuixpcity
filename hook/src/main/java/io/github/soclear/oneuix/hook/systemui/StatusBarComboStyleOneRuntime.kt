package io.github.soclear.oneuix.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyDisplayInfo
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.decodeFromString
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.roundToInt

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
    private const val ICON_MANAGER = "com.android.systemui.statusbar.phone.ui.IconManager"
    private const val BATTERY_METER = "com.android.systemui.battery.BatteryMeterView"
    private const val MIUI_BATTERY_METER = "com.android.systemui.statusbar.views.MiuiBatteryMeterView"

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
    private val displayNetworkTypes = ConcurrentHashMap<Int, String>()
    private val nativeChildSnapshots = Collections.synchronizedMap(WeakHashMap<View, LayoutSnapshot>())
    private val knownIconSlots = Collections.synchronizedMap(WeakHashMap<View, String>())
    private val rowTintSnapshots = Collections.synchronizedMap(WeakHashMap<ViewGroup, Int>())
    // Prefer the exact foreground tint applied to the native battery meter; the
    // generic status-icon tint remains a fallback for non-MIUI implementations.
    private val batteryTintSnapshots = Collections.synchronizedMap(WeakHashMap<View, Int>())
    private val lastContainerFingerprints = Collections.synchronizedMap(WeakHashMap<ViewGroup, Int>())
    private val scheduledContainerPasses = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<ViewGroup, Boolean>())
    )
    private val scheduledBatteryChildPasses = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<ViewGroup, Boolean>())
    )
    private val glyphInvalidationPending = AtomicBoolean(false)

    private data class LayoutSnapshot(
        val width: Int, val height: Int,
        val leftMargin: Int, val topMargin: Int, val rightMargin: Int, val bottomMargin: Int,
        val visibility: Int,
    )

    private class Host(val root: ViewGroup) {
        val original = IdentityHashMap<View, Int>()
        val layouts = IdentityHashMap<View, LayoutSnapshot>()
        var battery: View? = null
        var inline: GlyphView? = null
        var overlay: GlyphOverlay? = null
        var overlayFailed = false
        var applying = false
        var retired = false
        var rowTint: Int? = null
        // Cache the centre of the status-bar row at first attachment. The row's
        // height animates while the shade opens; recentering the overlay on every
        // layout frame causes repeated WindowManager relayouts and panel jank.
        var overlayCenterOffsetPx: Int? = null
        val delayedApply = Runnable {
            if (!retired && !applying) StatusBarComboStyleOneRuntime.apply(this)
        }
        val delayedOverlayUpdate = Runnable {
            if (!retired) overlay?.let { win -> battery?.let { anchor -> win.update(anchor, root) } }
        }
    }

    private class GlyphView(context: Context) : View(context) {
        private var tintOverride: Int? = null
        init {
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        fun setTintOverride(color: Int?) {
            if (tintOverride == color) return
            tintOverride = color
            invalidate()
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val currentState = StatusBarComboStyleOneRuntime.state
            val localTint = tintOverride
            val drawState = if (localTint == null) currentState else currentState.copy(tintColor = localTint)
            StatusComboStyleOneGlyphRenderer.draw(
                canvas, width, height, StatusBarComboStyleOneRuntime.config, drawState,
            )
        }
    }

    private class GlyphOverlay(context: Context) {
        private val app = context.applicationContext ?: context
        private val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        private val glyph = GlyphView(app)
        private var params: WindowManager.LayoutParams? = null

        fun attach(anchor: View, root: ViewGroup): Boolean {
            if (params != null) { update(anchor, root); return true }
            val type = runCatching {
                WindowManager.LayoutParams::class.java.getDeclaredField("TYPE_STATUS_BAR_ADDITIONAL")
                    .apply { isAccessible = true }.getInt(null)
            }.getOrElse {
                if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE
            }
            val cfg = StatusBarComboStyleOneRuntime.config
            val lp = WindowManager.LayoutParams(
                StatusBarComboStyleOneRuntime.dp(app, StatusComboStyleOneGlyphRenderer.glyphWidthDp(cfg)),
                StatusBarComboStyleOneRuntime.dp(app, StatusComboStyleOneGlyphRenderer.glyphHeightDp(cfg)),
                type,
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
                update(anchor, root)
                true
            } catch (e: Throwable) {
                Log.w(TAG, "Status-bar glyph window failed", e)
                if (type != WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY && Build.VERSION.SDK_INT >= 26) {
                    try {
                        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        lp.token = null
                        wm.addView(glyph, lp)
                        params = lp
                        update(anchor, root)
                        true
                    } catch (second: Throwable) {
                        Log.w(TAG, "Fallback glyph window failed", second)
                        false
                    }
                } else false
            }
        }

        fun update(anchor: View, root: ViewGroup) {
            val lp = params ?: return
            val oldX = lp.x
            val oldY = lp.y
            val oldWidth = lp.width
            val oldHeight = lp.height
            val cfg = StatusBarComboStyleOneRuntime.config
            lp.width = StatusBarComboStyleOneRuntime.dp(app, StatusComboStyleOneGlyphRenderer.glyphWidthDp(cfg))
            lp.height = StatusBarComboStyleOneRuntime.dp(app, StatusComboStyleOneGlyphRenderer.glyphHeightDp(cfg))
            val cutout = StatusBarComboStyleOneRuntime.cutoutBounds(root)
            if (cutout != null) {
                val centreRatio = if (StatusComboStyleOneGlyphRenderer.usesWideLayout(cfg)) 204f / 260f else 59.5f / 120f
                lp.x = (cutout.centerX() - lp.width * centreRatio).roundToInt().coerceAtLeast(0)
                val rootPos = IntArray(2)
                root.getLocationOnScreen(rootPos)
                // Use the initial row centre rather than its animated height.
                val host = StatusBarComboStyleOneRuntime.hosts[root]
                val centreOffset = host?.overlayCenterOffsetPx ?: (root.height / 2).also {
                    if (host != null) host.overlayCenterOffsetPx = it
                }
                lp.y = (rootPos[1] + centreOffset - lp.height / 2).coerceAtLeast(0)
            } else {
                val pos = IntArray(2)
                anchor.getLocationOnScreen(pos)
                lp.x = (pos[0] + anchor.width - lp.width).coerceAtLeast(0)
                lp.y = (pos[1] + (anchor.height - lp.height) / 2).coerceAtLeast(0)
            }
            // updateViewLayout invalidates/remeasures the overlay when its bounds
            // actually changed. Do not force an extra draw for every status-bar
            // layout callback during shade expansion.
            if (oldX != lp.x || oldY != lp.y || oldWidth != lp.width || oldHeight != lp.height) {
                runCatching { wm.updateViewLayout(glyph, lp) }
            }
        }
        fun setTintOverride(color: Int?) { glyph.setTintOverride(color) }
        fun invalidate() { glyph.invalidate() }

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
            if (next != tint) {
                tint = next
                state = state.copy(tintColor = next)
                invalidateGlyphs()
            }
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
            }.onFailure { diagnostic("Status-bar host unavailable: $name: $it") }
        }
        listOf(MIUI_ICONS, AOSP_ICONS).forEach { name ->
            runCatching {
                val cls = param.classLoader.loadClass(name)
                // onMeasure is hot during shade expansion and is not needed for
                // folding icons. Only inspect a container after layout and only
                // when it belongs to our active status-bar host.
                cls.declaredMethods.filter {
                    it.name == "onLayout" && it.parameterTypes.size == 5
                }.forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        val container = chain.thisObject as? ViewGroup
                        if (container != null && hostFor(container) != null) {
                            scheduleContainerSuppression(container)
                        }
                        result
                    }
                }
            }.onFailure { diagnostic("Status icon container unavailable: $name: $it") }
        }
        hookIconManager(module, param.classLoader)
        hookBatteryMeters(module, param.classLoader)
        hookBatteryTint(module, param.classLoader)
        hookNativeBatteryTint(module, param.classLoader)
        hookIconTint(module, param.classLoader)
        if (!hookedHost) Log.w(TAG, "No status-bar host constructor was hooked")
    }

    fun isEnabled(): Boolean = config.enabled
    fun isMobileNativeSuppressed(): Boolean =
        config.enabled && config.showMobile &&
            (config.signalMode == 0 || (config.signalMode == 1 && config.stackedSignal))

    private fun cutoutBounds(root: View): Rect? {
        if (Build.VERSION.SDK_INT < 28) return null
        return runCatching {
            val cutout = root.rootWindowInsets?.displayCutout ?: return@runCatching null
            val bounds = cutout.boundingRects.filter { it.width() > 0 && it.height() > 0 }
            val midpoint = root.resources.displayMetrics.widthPixels / 2
            bounds.minByOrNull { abs(it.centerX() - midpoint) }?.let { Rect(it) }
        }.getOrNull()
    }

    private fun scheduleRegister(root: ViewGroup) {
        root.post { register(root) }
        root.postDelayed({ register(root) }, 250L)
        root.postDelayed({ register(root) }, 1000L)
    }

    private fun register(root: ViewGroup) {
        // Prefer PhoneStatusBarView when it is present; StatusBarWindowView can
        // contain the same battery icon and drawing both roots would double-render.
        if (root.javaClass.name == WINDOW_BAR) {
            val phoneRoot = descendants(root).firstOrNull {
                it !== root && it.javaClass.name == PHONE_BAR
            } as? ViewGroup
            if (phoneRoot != null) {
                register(phoneRoot)
                return
            }
        }
        if (root.javaClass.name == PHONE_BAR) {
            val ancestorHosts = synchronized(hosts) {
                hosts.entries.map { it.key to it.value }.filter { (existingRoot, _) ->
                    existingRoot !== root &&
                        existingRoot.javaClass.name == WINDOW_BAR &&
                        isWithin(existingRoot, root)
                }
            }
            ancestorHosts.forEach { (oldRoot, oldHost) ->
                oldHost.retired = true
                oldHost.overlay?.detach()
                oldHost.overlay = null
                removeInline(oldHost)
                restore(oldHost)
                oldHost.battery = null
                synchronized(hosts) { hosts.remove(oldRoot) }
            }
        }
        val host = synchronized(hosts) {
            hosts[root] ?: Host(root).also {
                it.rowTint = rowTintSnapshots[root]
                hosts[root] = it
            }
        }
        if (host.battery == null) {
            root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // Defer overlay geometry reads until the expansion/collapse layout
                // burst has settled. The glyph position itself is pinned to the
                // initial row centre, so an animation frame must not move its window.
                if (host.overlay != null && host.battery != null) {
                    main.removeCallbacks(host.delayedOverlayUpdate)
                    main.postDelayed(host.delayedOverlayUpdate, 140L)
                }
                // Debounce full tree scans until that layout burst settles.
                if (!host.applying) {
                    main.removeCallbacks(host.delayedApply)
                    main.postDelayed(host.delayedApply, 140L)
                }
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

    private fun requiresHostRefresh(
        old: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        new: Preference.SystemUI.StatusBarComboStyleOneRuntime,
    ): Boolean =
        old.enabled != new.enabled ||
            old.overlayGlyph != new.overlayGlyph ||
            old.showWifi != new.showWifi ||
            old.showMobile != new.showMobile ||
            old.signalMode != new.signalMode ||
            old.stackedSignal != new.stackedSignal ||
            old.mobileTypeMode != new.mobileTypeMode ||
            old.glyphScalePercent != new.glyphScalePercent

    /** Refresh just the glyph drawing when telemetry changes; avoid a status-bar relayout. */
    private fun invalidateGlyphs() {
        if (!glyphInvalidationPending.compareAndSet(false, true)) return
        main.post {
            glyphInvalidationPending.set(false)
            synchronized(hosts) { hosts.values.toList() }.forEach { host ->
                host.inline?.invalidate()
                host.overlay?.invalidate()
            }
        }
    }

    private fun apply(host: Host) {
        if (host.retired || host.applying) return
        host.applying = true
        try {
            val root = host.root
            if (!root.isAttachedToWindow) { host.overlay?.detach(); return }
            val views = descendants(root)
            val battery = views.firstOrNull(::isBatteryMeterView)
                ?: views.firstOrNull(::isBatteryIcon)

            if (!config.enabled || battery == null) {
                host.overlay?.detach()
                host.overlay = null
                removeInline(host)
                restore(host)
                if (!config.enabled) restoreNativeBatteryViews(root)
                host.battery = null
                return
            }

            val previousBattery = host.battery
            if (previousBattery != null && previousBattery !== battery) {
                host.overlay?.detach()
                host.overlay = null
                removeInline(host)
                restoreHostView(host, previousBattery)
                restoreNativeBatteryViews(previousBattery)
            }
            host.battery = battery
            // BatteryMeterView receives the tint intended for the battery glyph.
            // Prefer it over the last colour sent to another status icon.
            val meterTint = batteryTintSnapshots[battery]
            val rowTint = meterTint ?: rowTintSnapshots[root]
            if (rowTint != null && host.rowTint != rowTint) {
                host.rowTint = rowTint
                host.inline?.setTintOverride(rowTint)
                host.overlay?.setTintOverride(rowTint)
            }
            val foldMobile = isMobileNativeSuppressed()

            views.forEach { view ->
                if (view === battery || view is GlyphView) return@forEach
                val hide = (config.showWifi && isWifiIcon(view)) ||
                    (foldMobile && isMobileIcon(view)) ||
                    (config.showMobile && config.mobileTypeMode == 2 && isMobileTypeView(view))
                if (hide) collapseHostView(host, view) else restoreHostView(host, view)
            }

            if (isBatteryMeterView(battery)) collapseNativeBatteryChildren(battery as ViewGroup)
            if (config.overlayGlyph && !host.overlayFailed) {
                removeInline(host)
                val overlay = host.overlay ?: GlyphOverlay(root.context).also { host.overlay = it }
                overlay.setTintOverride(host.rowTint)
                if (!overlay.attach(battery, root)) {
                    host.overlayFailed = true
                    overlay.detach()
                    host.overlay = null
                    collapseHostView(host, battery)
                    insertInline(host, battery)
                } else {
                    if (cutoutBounds(root) != null) collapseHostView(host, battery)
                    else reserveHostSlot(host, battery, StatusComboStyleOneGlyphRenderer.glyphWidthDp(config))
                    overlay.update(battery, root)
                }
            } else {
                host.overlay?.detach()
                host.overlay = null
                collapseHostView(host, battery)
                insertInline(host, battery)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot apply Style One", t)
        } finally {
            host.applying = false
        }
    }

    private fun insertInline(host: Host, anchor: View) {
        val parent = anchor.parent as? ViewGroup ?: return
        val width = dp(parent.context, StatusComboStyleOneGlyphRenderer.glyphWidthDp(config))
        val height = dp(parent.context, StatusComboStyleOneGlyphRenderer.glyphHeightDp(config))
        host.inline?.takeIf { it.parent === parent }?.let { glyph ->
            val lp = glyph.layoutParams
            if (lp != null && (lp.width != width || lp.height != height)) {
                lp.width = width
                lp.height = height
                glyph.layoutParams = lp
            }
            glyph.invalidate()
            return
        }
        removeInline(host)
        val glyph = GlyphView(parent.context).apply { setTintOverride(host.rowTint) }
        val old = anchor.layoutParams
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
        host.layouts.entries.toList().forEach { (view, layout) -> runCatching { restoreLayout(view, layout) } }
        host.layouts.clear()
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
    private fun viewSignature(v: View): String =
        (v.javaClass.name + " " + resourceName(v) + " " + v.tag + " " + v.contentDescription).lowercase()

    private fun isBatteryMeterView(v: View): Boolean {
        val name = viewSignature(v)
        return v is ViewGroup && (name.contains("batterymeterview") || name.contains("miuibatterymeterview"))
    }

    private fun isBatteryIcon(v: View): Boolean {
        if (v is GlyphView) return false
        val name = viewSignature(v)
        return name.contains("batterymetericonview") || name.contains("batteryiconview") ||
            (name.contains("battery") && (name.contains("status") || name.contains("meter") || name.contains("icon")))
    }

    private fun slotOf(view: View): String? {
        var cls: Class<*>? = view.javaClass
        while (cls != null) {
            val current = cls
            for (name in listOf("getSlot", "getSlotName", "getIconSlot")) {
                val method = current.declaredMethods.firstOrNull { it.name == name && it.parameterTypes.isEmpty() } ?: continue
                val normalized = slotText(runCatching { method.isAccessible = true; method.invoke(view) }.getOrNull())
                if (!normalized.isNullOrBlank()) return normalized
            }
            for (name in listOf("mSlot", "slot", "mSlotName", "slotName")) {
                val field = runCatching { current.getDeclaredField(name).apply { isAccessible = true } }.getOrNull() ?: continue
                val normalized = slotText(runCatching { field.get(view) }.getOrNull())
                if (!normalized.isNullOrBlank()) return normalized
            }
            cls = current.superclass
        }
        return null
    }

    private fun slotText(raw: Any?): String? {
        if (raw is String) return raw.lowercase()
        if (raw == null) return null
        var cls: Class<*>? = raw.javaClass
        while (cls != null) {
            val current = cls
            for (name in listOf("mName", "name", "mSlot", "slot")) {
                val field = runCatching { current.getDeclaredField(name).apply { isAccessible = true } }.getOrNull() ?: continue
                (runCatching { field.get(raw) }.getOrNull() as? String)?.let { return it.lowercase() }
            }
            cls = current.superclass
        }
        return null
    }

    private fun isMobileTypeView(v: View): Boolean {
        val name = viewSignature(v)
        return name.contains("mobile_type_single") || name.contains("mobile_type") || name.contains("mobiletypedrawable")
    }

    private fun isManagedMobileSlot(slot: String): Boolean =
        slot == "mobile" || slot == "stacked_mobile" || slot.startsWith("mobile") ||
            slot.contains("cellular") || slot.contains("signal_strength")

    private fun shouldSuppressSlot(slot: String): Boolean {
        val normalized = slot.lowercase()
        return (config.enabled && config.showWifi && normalized == "wifi") ||
            (isMobileNativeSuppressed() && isManagedMobileSlot(normalized))
    }

    private fun hostFor(view: View): Host? = synchronized(hosts) {
        hosts.values.firstOrNull { !it.retired && isWithin(it.root, view) }
    }

    private fun isWifiIcon(v: View): Boolean {
        if (v is GlyphView) return false
        val slot = slotOf(v) ?: knownIconSlots[v]
        if (slot == "wifi" || slot?.contains("wlan") == true) return true
        val name = viewSignature(v)
        if (name.contains("battery") || name.contains("bluetooth") || name.contains("airplane")) return false
        return name.contains("wifi") || name.contains("wlan") || name.contains("slot_wifi")
    }

    private fun isMobileIcon(v: View): Boolean {
        if (v is GlyphView || isMobileTypeView(v)) return false
        (slotOf(v) ?: knownIconSlots[v])?.let { if (isManagedMobileSlot(it)) return true }
        val name = viewSignature(v)
        if (name.contains("battery") || name.contains("bluetooth") || name.contains("airplane") || name.contains("wifi")) return false
        return name.contains("mobile") || name.contains("cellular") || name.contains("signal_mobile") ||
            name.contains("slot_mobile") || name.contains("signal_strength") || name.contains("signalcluster")
    }

    private fun isWithin(root: View, candidate: View): Boolean {
        var node: View? = candidate
        while (node != null) {
            if (node === root) return true
            node = node.parent as? View
        }
        return false
    }

    private fun containerSuppressionFingerprint(container: ViewGroup): Int {
        var key = 17
        key = 31 * key + if (config.enabled && config.showWifi) 1 else 0
        key = 31 * key + if (isMobileNativeSuppressed()) 1 else 0
        key = 31 * key + if (config.showMobile && config.mobileTypeMode == 2) 1 else 0
        key = 31 * key + container.childCount
        for (index in 0 until container.childCount) {
            key = 31 * key + System.identityHashCode(container.getChildAt(index))
        }
        return key
    }

    private fun scheduleContainerSuppression(container: ViewGroup) {
        // Mark the pass before fingerprinting. onLayout may run repeatedly during
        // shade expansion; all callbacks in the same frame should share one scan.
        synchronized(scheduledContainerPasses) {
            if (!scheduledContainerPasses.add(container)) return
        }
        container.post {
            try {
                if (hostFor(container) == null) return@post
                val fingerprint = containerSuppressionFingerprint(container)
                synchronized(lastContainerFingerprints) {
                    if (lastContainerFingerprints[container] == fingerprint) return@post
                }
                suppressInContainer(container)
            } finally {
                scheduledContainerPasses.remove(container)
            }
        }
    }

    private fun suppressInContainer(container: ViewGroup) {
        val host = hostFor(container) ?: return
        descendants(container).forEach { view ->
            if (view is GlyphView) return@forEach
            val hide = config.enabled && (
                (config.showWifi && isWifiIcon(view)) ||
                    (isMobileNativeSuppressed() && isMobileIcon(view)) ||
                    (config.showMobile && config.mobileTypeMode == 2 && isMobileTypeView(view))
            )
            if (hide) collapseHostView(host, view) else restoreHostView(host, view)
        }
        lastContainerFingerprints[container] = containerSuppressionFingerprint(container)
    }

    private fun snapshotLayout(view: View): LayoutSnapshot? {
        val lp = view.layoutParams ?: return null
        val margin = lp as? ViewGroup.MarginLayoutParams
        return LayoutSnapshot(
            lp.width, lp.height, margin?.leftMargin ?: 0, margin?.topMargin ?: 0,
            margin?.rightMargin ?: 0, margin?.bottomMargin ?: 0, view.visibility,
        )
    }

    private fun restoreLayout(view: View, snapshot: LayoutSnapshot) {
        val lp = view.layoutParams
        if (lp != null) {
            lp.width = snapshot.width
            lp.height = snapshot.height
            (lp as? ViewGroup.MarginLayoutParams)?.setMargins(
                snapshot.leftMargin, snapshot.topMargin, snapshot.rightMargin, snapshot.bottomMargin,
            )
            view.layoutParams = lp
        }
        view.visibility = snapshot.visibility
    }

    private fun collapseViewLayout(view: View) {
        val lp = view.layoutParams
        if (lp != null) {
            val margins = lp as? ViewGroup.MarginLayoutParams
            val changed = lp.width != 0 || lp.height != 0 ||
                margins?.let { it.leftMargin != 0 || it.topMargin != 0 || it.rightMargin != 0 || it.bottomMargin != 0 } == true
            if (changed) {
                lp.width = 0
                lp.height = 0
                margins?.setMargins(0, 0, 0, 0)
                view.layoutParams = lp
            }
        }
        if (view.width != 0 || view.height != 0) runCatching { view.layout(0, 0, 0, 0) }
        if (view.visibility != View.GONE) view.visibility = View.GONE
    }

    private fun collapseHostView(host: Host, view: View) {
        host.original.putIfAbsent(view, view.visibility)
        if (!host.layouts.containsKey(view)) snapshotLayout(view)?.let { host.layouts[view] = it }
        collapseViewLayout(view)
    }

    private fun reserveHostSlot(host: Host, view: View, widthDp: Int) {
        host.original.putIfAbsent(view, view.visibility)
        if (!host.layouts.containsKey(view)) snapshotLayout(view)?.let { host.layouts[view] = it }
        val lp = view.layoutParams ?: return
        val margins = lp as? ViewGroup.MarginLayoutParams
        val width = dp(view.context, widthDp)
        val changed = lp.width != width ||
            margins?.let { it.leftMargin != 0 || it.topMargin != 0 || it.rightMargin != 0 || it.bottomMargin != 0 } == true
        if (changed) {
            lp.width = width
            margins?.setMargins(0, 0, 0, 0)
            view.layoutParams = lp
        }
        if (view.visibility != View.INVISIBLE) view.visibility = View.INVISIBLE
    }

    private fun restoreHostView(host: Host, view: View) {
        host.layouts.remove(view)?.let { runCatching { restoreLayout(view, it) } }
        host.original.remove(view)?.let { old -> runCatching { view.visibility = old } }
    }

    private fun collapseNativeChild(view: View) {
        if (!nativeChildSnapshots.containsKey(view)) snapshotLayout(view)?.let { nativeChildSnapshots[view] = it }
        collapseViewLayout(view)
    }

    private fun restoreNativeBatteryViews(root: View) {
        nativeChildSnapshots.entries.toList().forEach { (view, snapshot) ->
            if (isWithin(root, view)) {
                runCatching { restoreLayout(view, snapshot) }
                nativeChildSnapshots.remove(view)
            }
        }
    }

    private fun fieldView(instance: Any, names: List<String>): View? {
        var cls: Class<*>? = instance.javaClass
        while (cls != null) {
            val current = cls
            for (name in names) {
                val field = runCatching { current.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()
                val view = field?.let { runCatching { it.get(instance) as? View }.getOrNull() }
                if (view != null) return view
            }
            cls = current.superclass
        }
        return null
    }

    private fun collapseNativeBatteryChildren(meter: ViewGroup) {
        listOf(
            "mBatteryIconView", "batteryIconView", "mHollowBatteryIconView", "hollowBatteryIconView",
            "mBatteryChargingView", "mBatteryChargingIconView", "batteryChargingView",
            "mBatteryPercentContainer", "batteryPercentContainer", "mBatteryPercentView", "batteryPercentView",
        ).forEach { name ->
            fieldView(meter, listOf(name))?.takeIf { it !== meter }?.let(::collapseNativeChild)
        }
    }

    private fun hookBatteryMeters(module: XposedModule, classLoader: ClassLoader) {
        listOf(BATTERY_METER, MIUI_BATTERY_METER).forEach { className ->
            runCatching {
                val cls = classLoader.loadClass(className)
                val callback = io.github.libxposed.api.XposedInterface.Hooker { chain ->
                    val result = chain.proceed()
                    val meter = chain.thisObject as? ViewGroup
                    if (meter != null) scheduleBatteryChildCollapse(meter)
                    result
                }
                cls.declaredConstructors.forEach { module.hook(it).intercept(callback) }
                // Avoid onMeasure/onLayout: both are hot paths during notification
                // shade expansion. Child suppression is handled on inflation and
                // on the meter's actual battery/percent update methods instead.
                cls.declaredMethods.filter {
                    it.name in setOf(
                        "onFinishInflate", "onAttachedToWindow", "onBatteryLevelChanged",
                        "updateChargeAndText", "updatePercentText", "updateShowPercent",
                        "scaleBatteryMeterViewsLegacy",
                    )
                }.forEach { module.hook(it).intercept(callback) }
            }.onFailure { diagnostic("Battery meter hook unavailable: $className: $it") }
        }
    }

    private fun scheduleBatteryChildCollapse(meter: ViewGroup) {
        if (!config.enabled || hostFor(meter) == null) return
        synchronized(scheduledBatteryChildPasses) {
            if (!scheduledBatteryChildPasses.add(meter)) return
        }
        meter.post {
            try {
                if (config.enabled && hostFor(meter) != null) collapseNativeBatteryChildren(meter)
            } finally {
                scheduledBatteryChildPasses.remove(meter)
            }
        }
    }

    /**
     * MIUI's battery meter is the most accurate source for the colour actually
     * used to paint the native battery icon. Capture the resolved tint per meter,
     * rather than deriving it from the clock or whichever icon tinted last.
     */
    private fun hookBatteryTint(module: XposedModule, classLoader: ClassLoader) {
        runCatching {
            val cls = classLoader.loadClass(MIUI_BATTERY_METER)
            cls.declaredMethods.filter {
                it.name == "updateLightDarkTint" &&
                    it.parameterTypes.size == 6 &&
                    it.parameterTypes[1] == Float::class.javaPrimitiveType &&
                    it.parameterTypes[2] == Int::class.javaPrimitiveType &&
                    it.parameterTypes[3] == Int::class.javaPrimitiveType &&
                    it.parameterTypes[4] == Int::class.javaPrimitiveType &&
                    it.parameterTypes[5] == Boolean::class.javaPrimitiveType
            }.forEach { method ->
                module.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    val meter = chain.thisObject as? View
                    if (meter != null) {
                        val intensity = runCatching { chain.getArg(1) as? Float }.getOrNull() ?: 0f
                        val tintColor = runCatching { chain.getArg(2) as? Int }.getOrNull() ?: 0
                        val lightColor = runCatching { chain.getArg(3) as? Int }.getOrNull() ?: 0
                        val darkColor = runCatching { chain.getArg(4) as? Int }.getOrNull() ?: 0
                        val useTint = runCatching { chain.getArg(5) as? Boolean }.getOrNull() ?: false
                        val resolved = (if (useTint) tintColor else if (intensity > 0f) darkColor else lightColor)
                            .takeIf { it != Color.TRANSPARENT } ?: Color.WHITE
                        saveBatteryTint(meter, resolved)
                    }
                    result
                }
            }
        }.onFailure { diagnostic("Battery meter tint hook unavailable: $it") }
    }

    /**
     * AOSP/Samsung BatteryMeterView commonly receives its tint through
     * DarkReceiver.onDarkChanged instead of MIUI's updateLightDarkTint method.
     * Track that colour per meter as well; it is the foreground used for the
     * battery itself, not the last tint observed on a neighbouring status icon.
     */
    private fun hookNativeBatteryTint(module: XposedModule, classLoader: ClassLoader) {
        listOf(BATTERY_METER, MIUI_BATTERY_METER).forEach classLoop@{ className ->
            runCatching {
                var current: Class<*>? = classLoader.loadClass(className)
                val methods = mutableListOf<java.lang.reflect.Method>()
                while (current != null) {
                    methods += current.declaredMethods.filter { method ->
                        method.name == "onDarkChanged" &&
                            method.parameterTypes.any { it == Int::class.javaPrimitiveType }
                    }
                    current = current.superclass
                }
                methods.distinctBy { it.name + it.parameterTypes.joinToString { type -> type.name } }
                    .forEach methodLoop@{ method ->
                        val tintIndex = method.parameterTypes.indices.lastOrNull {
                            method.parameterTypes[it] == Int::class.javaPrimitiveType
                        } ?: return@methodLoop
                        module.hook(method).intercept { chain ->
                            val result = chain.proceed()
                            val meter = chain.thisObject as? View
                            val color = runCatching { chain.getArg(tintIndex) as? Int }.getOrNull()
                            if (meter != null && color != null && color != Color.TRANSPARENT) {
                                saveBatteryTint(meter, color)
                            }
                            result
                        }
                    }
            }.onFailure { diagnostic("Native battery tint hook unavailable: $className: $it") }
        }
    }

    private fun saveBatteryTint(meter: View, color: Int) {
        batteryTintSnapshots[meter] = color
        val host = hostFor(meter)
        if (host != null && host.battery === meter && host.rowTint != color) {
            host.rowTint = color
            host.inline?.setTintOverride(color)
            host.overlay?.setTintOverride(color)
        }
    }

    private fun statusBarRootOf(view: View): ViewGroup? {
        var node: View? = view
        while (node != null) {
            if (node is ViewGroup && node.javaClass.name == PHONE_BAR) return node
            node = node.parent as? View
        }
        return null
    }

    /**
     * Capture the colour SystemUI actually applies to each status-bar icon.
     * The QS clock is only a useful fallback: the shade and collapsed status
     * bar may be tinted differently in the same frame.
     */
    private fun hookIconTint(module: XposedModule, classLoader: ClassLoader) {
        runCatching {
            var cls: Class<*>? = classLoader.loadClass("com.android.systemui.statusbar.StatusBarIconView")
            val methods = mutableListOf<java.lang.reflect.Method>()
            while (cls != null) {
                methods += cls.declaredMethods.filter {
                    (it.name == "onDarkChanged" || it.name == "setIconColor") &&
                        it.parameterTypes.any { type -> type == Int::class.javaPrimitiveType }
                }
                cls = cls.superclass
            }
            methods.distinctBy { it.name + it.parameterTypes.joinToString { type -> type.name } }
                .forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        var color: Int? = null
                        method.parameterTypes.forEachIndexed { index, type ->
                            if (color == null && type == Int::class.javaPrimitiveType) {
                                val value = runCatching { chain.getArg(index) as? Int }.getOrNull()
                                if (value != null && value != Color.TRANSPARENT) color = value
                            }
                        }
                        val resolvedColor = color
                        val icon = chain.thisObject as? View
                        val row = icon?.let(::statusBarRootOf)
                        if (resolvedColor != null && row != null) {
                            rowTintSnapshots[row] = resolvedColor
                            val host = synchronized(hosts) { hosts[row] }
                            // A battery-meter tint hook is more specific. Keep the
                            // generic icon tint as fallback only when that source is
                            // not available on this SystemUI build.
                            if (host != null && host.battery?.let { batteryTintSnapshots[it] } == null &&
                                host.rowTint != resolvedColor
                            ) {
                                val applyTint = Runnable {
                                    if (host.battery?.let { batteryTintSnapshots[it] } == null &&
                                        host.rowTint != resolvedColor
                                    ) {
                                        host.rowTint = resolvedColor
                                        host.inline?.setTintOverride(resolvedColor)
                                        host.overlay?.setTintOverride(resolvedColor)
                                    }
                                }
                                if (Looper.myLooper() == Looper.getMainLooper()) applyTint.run()
                                else main.post(applyTint)
                            }
                        }
                        result
                    }
                }
        }.onFailure { diagnostic("StatusBarIconView tint hook unavailable: $it") }
    }

    private fun hookIconManager(module: XposedModule, classLoader: ClassLoader) {
        runCatching {
            val cls = classLoader.loadClass(ICON_MANAGER)
            cls.declaredMethods.filter { it.name == "addHolder" && it.parameterTypes.size == 4 }.forEach { method ->
                module.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    val slot = chain.getArg(1) as? String
                    val view = result as? View
                    if (slot != null && view != null) {
                        knownIconSlots[view] = slot.lowercase()
                        view.post {
                            val host = hostFor(view) ?: return@post
                            if (shouldSuppressSlot(slot)) {
                                if (view.parent is ViewGroup) collapseHostView(host, view)
                            } else {
                                restoreHostView(host, view)
                            }
                        }
                    }
                    result
                }
            }
        }.onFailure { diagnostic("IconManager slot hook unavailable: $it") }
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
                            val previous = config
                            val next = sanitize(status.comboStyleOneRuntime)
                            config = next
                            diagnostic("Settings updated: enabled=${config.enabled}, overlay=${config.overlayGlyph}, style=${config.trioStyle}, signalMode=${config.signalMode}")
                            if (requiresHostRefresh(previous, next)) refreshHosts() else invalidateGlyphs()
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
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(WifiManager.RSSI_CHANGED_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")
                addAction("android.intent.action.SUBSCRIPTION_PHONE_STATE_CHANGED")
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        Intent.ACTION_BATTERY_CHANGED -> updateBattery(intent)
                        Intent.ACTION_BATTERY_LOW -> state = state.copy(lowBatteryWarning = true)
                        Intent.ACTION_BATTERY_OKAY -> state = state.copy(lowBatteryWarning = false)
                        Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED ->
                            updateBattery(app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
                        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> updatePowerSave(app)
                        WifiManager.RSSI_CHANGED_ACTION, WifiManager.NETWORK_STATE_CHANGED_ACTION -> updateWifi(app)
                        else -> refreshTelephony(app)
                    }
                    invalidateGlyphs()
                }
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                else app.registerReceiver(receiver, filter)
                updateBattery(app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
                updatePowerSave(app)
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
        diagnostic("Battery: level=${state.batteryLevel}, charging=${state.charging}, quick=${state.quickCharge}")
    }
    private fun updatePowerSave(context: Context) {
        val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val enabled = runCatching { manager?.isPowerSaveMode == true }.getOrDefault(state.powerSave)
        if (state.powerSave != enabled) {
            state = state.copy(powerSave = enabled)
            diagnostic("Power-save mode changed: $enabled")
        }
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
            val level = if (info == null || info.rssi == -127) -1
                else WifiManager.calculateSignalLevel(info.rssi, 5)
            state = state.copy(wifiLevel = level.coerceIn(-1, 4))
            diagnostic("Wi-Fi: level=${state.wifiLevel}")
        }
    }
    private fun ensureTelephony(context: Context) {
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager ?: return
        if (subscriptionsListener == null) {
            val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    refreshTelephony(context)
                    installPhoneListeners(context, sm)
                    invalidateGlyphs()
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
            displayNetworkTypes.remove(id)
        }
        active.forEach { info ->
            if (phoneListeners.containsKey(info.subscriptionId)) return@forEach
            runCatching {
                val sub = base.createForSubscriptionId(info.subscriptionId)
                val listener = object : PhoneStateListener() {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        refreshTelephony(context)
                        invalidateGlyphs()
                    }
                    override fun onServiceStateChanged(serviceState: ServiceState?) {
                        refreshTelephony(context)
                        invalidateGlyphs()
                    }
                    override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                        // Cache each SIM's display type so switching the default data SIM
                        // immediately uses the new subscription's network label.
                        val label = displayNetworkType(displayInfo)
                        if (label == null) displayNetworkTypes.remove(info.subscriptionId)
                        else displayNetworkTypes[info.subscriptionId] = label
                        refreshTelephony(context)
                        invalidateGlyphs()
                    }
                }
                var events = PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    events = events or PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED
                }
                sub.listen(listener, events)
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
                label = displayNetworkTypes[info.subscriptionId] ?: networkType(sub.dataNetworkType)
            }
        }
        if (dataLevel < 0) dataLevel = if (simOne >= 0) simOne else simTwo
        if (dataLevel < 0) dataLevel = runCatching { base.signalStrength?.level ?: state.mobileLevel }.getOrDefault(state.mobileLevel)
        state = state.copy(mobileLevel = dataLevel.coerceIn(0, 4), simOneLevel = simOne, simTwoLevel = simTwo, networkType = label)
        diagnostic("Telephony refreshed: dataSub=$dataSub, dataLevel=${state.mobileLevel}, type=${state.networkType}")
    }

    private fun displayNetworkType(info: TelephonyDisplayInfo): String? = when (info.overrideNetworkType) {
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5GA"
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G"
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA,
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "4GA"
        else -> null
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
        signalMode = v.signalMode.coerceIn(0, 1), lowThreshold = v.lowThreshold.coerceIn(5, 50),
        criticalThreshold = v.criticalThreshold.coerceIn(1, 20),
        glyphScalePercent = v.glyphScalePercent.coerceIn(70, 150),
        ringStroke = v.ringStroke.coerceIn(4, 16), arcStroke = v.arcStroke.coerceIn(3, 16),
        valueSize = v.valueSize.coerceIn(16, 44), valueWeight = v.valueWeight.coerceIn(100, 900),
        typeSize = v.typeSize.coerceIn(16, 44), typeWeight = v.typeWeight.coerceIn(100, 900),
        outTypeSizeDp = v.outTypeSizeDp.coerceIn(8, 26), outSignalSizeDp = v.outSignalSizeDp.coerceIn(6, 20),
        outTypeMarginLeftDp = v.outTypeMarginLeftDp.coerceIn(0, 16),
        outTypeMarginRightDp = v.outTypeMarginRightDp.coerceIn(0, 16),
        outSignalMarginDp = v.outSignalMarginDp.coerceIn(0, 16),
        typeSuffixScale = v.typeSuffixScale.coerceIn(50, 100), trackAlpha = v.trackAlpha.coerceIn(0, 255),
    )

    private fun diagnostic(message: String) {
        if (config.debugLog) Log.d(TAG, message)
    }
    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt().coerceAtLeast(1)
}
