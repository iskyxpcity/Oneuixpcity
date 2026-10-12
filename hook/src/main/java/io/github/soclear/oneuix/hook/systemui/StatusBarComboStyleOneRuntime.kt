package io.github.soclear.oneuix.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
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
import io.github.soclear.oneuix.hook.util.OneuiXPDiagnosticLog
import java.lang.ref.WeakReference
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
    private const val MIUI_PHONE_BAR = "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView"
    private const val WINDOW_BAR = "com.android.systemui.statusbar.phone.StatusBarWindowView"
    private const val MIUI_ICONS = "com.android.systemui.statusbar.phone.MiuiStatusIconContainer"
    private const val AOSP_ICONS = "com.android.systemui.statusbar.phone.StatusBarIconContainer"
    private const val GLYPH_WIDTH_DP = 78
    private const val GLYPH_HEIGHT_DP = 36
    private const val ICON_MANAGER = "com.android.systemui.statusbar.phone.ui.IconManager"
    private const val BATTERY_METER = "com.android.systemui.battery.BatteryMeterView"
    private const val MIUI_BATTERY_METER = "com.android.systemui.statusbar.views.MiuiBatteryMeterView"
    private const val MIUI_BATTERY_CONTAINER = "com.android.systemui.statusbar.views.MiuiStatusBatteryContainer"
    private const val ICON_HIDE_LIST_KEY = "icon_blacklist"
    // The outer display's camera needs the established +4dp vertical calibration.
    // The foldable inner display places the camera near the right edge; this separate
    // calibration is based on the reported cutout position and does not alter outer-screen alignment.
    private const val PUNCH_HOLE_VERTICAL_CALIBRATION_DP = 4f
    private const val INNER_PUNCH_HOLE_VERTICAL_CALIBRATION_DP = 0.5f
    private const val INNER_PUNCH_HOLE_HORIZONTAL_CALIBRATION_DP = 1.5f
    private const val RIGHT_SIDE_CUTOUT_THRESHOLD = 0.65f

    private val installed = AtomicBoolean(false)
    @Volatile private var moduleForPreferenceReload: XposedModule? = null
    @Volatile private var systemUiClassLoader: ClassLoader? = null
    private val main = Handler(Looper.getMainLooper())
    private val hosts = Collections.synchronizedMap(WeakHashMap<ViewGroup, Host>())
    // Keep exactly one lifecycle listener per root across fold/unfold reattachment.
    // Listener values do not capture their weak-map keys, so destroyed roots remain collectible.
    private val hostLifecycleListeners =
        Collections.synchronizedMap(WeakHashMap<ViewGroup, View.OnAttachStateChangeListener>())
    @Volatile private var config = Preference.SystemUI.StatusBarComboStyleOneRuntime()

    internal fun shouldHideNativeBatteryIcon(): Boolean = config.enabled
    @Volatile private var state = StatusComboStyleOneGlyphState()
    @Volatile private var preferenceReceiverReady = false
    private val chargingRotationTicker = object : Runnable {
        override fun run() {
            if (!shouldRotateChargingDisplay()) {
                if (state.chargingAlternatePhase) {
                    state = state.copy(chargingAlternatePhase = false)
                    invalidateGlyphs()
                }
                return
            }
            state = state.copy(chargingAlternatePhase = !state.chargingAlternatePhase)
            invalidateGlyphs()
            main.postDelayed(this, 5_000L)
        }
    }
    @Volatile private var stateReceiverReady = false
    @Volatile private var tint = Color.WHITE
    @Volatile private var defaultDataSubscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    @Volatile private var subscriptionsListener: SubscriptionManager.OnSubscriptionsChangedListener? = null
    private val phoneListeners = mutableMapOf<Int, Pair<TelephonyManager, PhoneStateListener>>()
    private val displayNetworkTypes = ConcurrentHashMap<Int, String>()
    // Prefer the exact collapsed status-bar signal icon level; use Quick Settings
    // and TelephonyManager as fallbacks when an OEM icon resource cannot be decoded.
    private val quickPanelSignalLevels = ConcurrentHashMap<Int, Int>()
    private val nativeSignalLevels = ConcurrentHashMap<Int, Int>()
    // The level decoded from the SignalDrawable inside the collapsed status-bar icon is the
    // highest-confidence source. Keep it per subscription so TelephonyManager cannot override
    // what SystemUI is actually drawing (notably on Samsung dual-SIM/eSIM devices).
    private val renderedSignalLevels = ConcurrentHashMap<Int, Int>()
    private val modernMobileViewModels = ConcurrentHashMap<Int, WeakReference<Any>>()
    private val modernMobileViewSubscriptionIds = Collections.synchronizedMap(WeakHashMap<View, Int>())
    private val lastModernSignalSamplingDiagnostics = ConcurrentHashMap<String, String>()
    private data class NativeDrawableOrigin(
        val resourceId: Int,
        val resourcePackage: String,
        val resourceName: String,
    )
    private val drawableOrigins = Collections.synchronizedMap(WeakHashMap<Drawable, NativeDrawableOrigin>())
    private val drawableStateOrigins =
        Collections.synchronizedMap(WeakHashMap<Drawable.ConstantState, NativeDrawableOrigin>())
    @Volatile private var modernMobileViewClass: Class<*>? = null
    private val lastModernPipelineSignalDiagnostics = ConcurrentHashMap<Int, String>()
    private val lastModernPipelineLevelDiagnostics = ConcurrentHashMap<Int, String>()
    private val lastNativeWifiVisibilityRequests = Collections.synchronizedMap(WeakHashMap<View, String>())
    // Persist the pre-Combo native visibility separately from transient GONE/restore passes,
    // so forcing a connected Wi-Fi icon visible does not override an already-hidden native slot.
    private val nativeWifiBaselineVisibility = Collections.synchronizedMap(WeakHashMap<View, Int>())
    @Volatile private var modernWifiViewClass: Class<*>? = null
    // Track each native Wi-Fi view's own status-bar pipeline model. This preserves
    // SystemUI's true Visible/Hidden state when the custom arc is switched off.
    private val modernWifiViewModels = Collections.synchronizedMap(WeakHashMap<View, WeakReference<Any>>())
    private val lastNativeWifiDiagnostics = Collections.synchronizedMap(WeakHashMap<View, String>())
    private val lastNativeWifiSlotRequests = ConcurrentHashMap<String, String>()
    private val lastSubscriptionSignalLevels = ConcurrentHashMap<Int, Int>()
    private val subscriptionSlots = ConcurrentHashMap<Int, Int>()
    private val nativeChildSnapshots = Collections.synchronizedMap(WeakHashMap<View, LayoutSnapshot>())
    private val knownIconSlots = Collections.synchronizedMap(WeakHashMap<View, String>())
    private val iconVisibilityControllers: MutableSet<Any> =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<Any, Boolean>()))
    private val nativeBatterySlotVisibility =
        Collections.synchronizedMap(WeakHashMap<Any, MutableMap<String, Boolean>>())
    // Native-slot visibility control is battery-only. Wi-Fi suppression stays on
    // the established view/layout path; changing its holder visibility directly
    // can race with SystemUI's rebinding and leave the native icon hidden.
    private val knownBatterySlotNames: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf("battery"))
    private val enforcingBatterySlotVisibility = ThreadLocal<Boolean>()
    private val rowTintSnapshots = Collections.synchronizedMap(WeakHashMap<ViewGroup, Int>())
    // Prefer the exact foreground tint applied to the native battery meter; the
    // generic status-icon tint remains a fallback for non-MIUI implementations.
    private val batteryTintSnapshots = Collections.synchronizedMap(WeakHashMap<View, Int>())
    private val batteryDrawHookClasses = java.util.concurrent.CopyOnWriteArrayList<Class<*>>()
    // Original island requests are kept so toggling the feature during an island
    // replays the correct MIUI state immediately instead of waiting for the next event.
    private val batteryIslandRequests =
        Collections.synchronizedMap(WeakHashMap<View, Pair<Boolean, Boolean>>())
    private val batteryContainerIslandRequests =
        Collections.synchronizedMap(WeakHashMap<View, Boolean>())
    private data class TintIconClassification(
        val isCollapsedStatusBar: Boolean,
        val root: WeakReference<ViewGroup>? = null,
        val tintOwner: WeakReference<ViewGroup>? = null,
    )
    private val tintIconClassifications =
        Collections.synchronizedMap(WeakHashMap<View, TintIconClassification>())
    @Volatile private var statusBarIconViewClass: Class<*>? = null
    @Volatile private var miuiIslandMeterHooked = false
    @Volatile private var shadeBusy = false
    private val lastContainerFingerprints = Collections.synchronizedMap(WeakHashMap<ViewGroup, Int>())
    private val scheduledContainerPasses = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<ViewGroup, Boolean>())
    )
    private val scheduledBatteryChildPasses = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<ViewGroup, Boolean>())
    )
    // Preserve the SystemUI battery meter's native visibility/layout before the
    // icon_hide_list callback can suppress it for Style One.
    private val nativeBatteryLayouts = Collections.synchronizedMap(WeakHashMap<View, LayoutSnapshot>())
    private val glyphInvalidationPending = AtomicBoolean(false)
    private val telephonyRefreshPending = AtomicBoolean(false)

    private data class LayoutSnapshot(
        val width: Int, val height: Int,
        val leftMargin: Int, val topMargin: Int, val rightMargin: Int, val bottomMargin: Int,
        val visibility: Int, val alpha: Float,
    )

    private class Host(val root: ViewGroup) {
        val original = IdentityHashMap<View, Int>()
        val layouts = IdentityHashMap<View, LayoutSnapshot>()
        var battery: View? = null
        var shadeDrawTarget: View? = null
        var batteryWillNotDraw: Boolean? = null
        var batteryWillNotDrawCaptured: Boolean = false
        var inline: GlyphView? = null
        var overlay: GlyphOverlay? = null
        var overlayFailed = false
        var applying = false
        var retired = false
        var rowTint: Int? = null
        var activeDisplayId: Int = -1
        var activeDisplayWidth: Int = -1
        var activeDisplayHeight: Int = -1
        var geometryRetryPending = false
        var geometryRetryAttempts = 0
        // Foldable display transitions can temporarily report no cutout. Retry geometry
        // without inserting a wide inline glyph into the status-bar row.
        val geometryRetry = Runnable {
            geometryRetryPending = false
            if (!retired && !applying &&
                StatusBarComboStyleOneRuntime.config.enabled &&
                StatusBarComboStyleOneRuntime.config.overlayGlyph &&
                geometryRetryAttempts < 1
            ) {
                geometryRetryAttempts++
                StatusBarComboStyleOneRuntime.apply(this)
            }
        }
        // MIUI can host multiple status-bar rows at once. Keep the icon ink tied
        // to the battery's own container, not the whole PhoneStatusBarView.
        var tintRow: ViewGroup? = null
        val delayedApply = Runnable {
            if (!retired && !applying) StatusBarComboStyleOneRuntime.apply(this)
        }
    }

    private class GlyphView(context: Context) : View(context) {
        private var tintOverride: Int? = null
        private var statusBarRootForTypeBounds: View? = null
        init {
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        fun setStatusBarRootForTypeBounds(root: View?) {
            if (statusBarRootForTypeBounds === root) return
            statusBarRootForTypeBounds = root
            invalidate()
        }
        fun minimumOutsideTypeAdvanceLeftPx(): Float? =
            StatusBarComboStyleOneRuntime.minimumOutsideTypeAdvanceLeftPx(
                this, statusBarRootForTypeBounds,
            )
        fun setTintOverride(color: Int?) {
            if (tintOverride == color) return
            tintOverride = color
            invalidate()
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val currentState = StatusBarComboStyleOneRuntime.state
            val currentConfig = StatusBarComboStyleOneRuntime.config
            val localTint = tintOverride
            val drawState = if (localTint == null) currentState else currentState.copy(tintColor = localTint)
            StatusComboStyleOneGlyphRenderer.draw(
                canvas, width, height, currentConfig, drawState,
                minimumOutsideTypeAdvanceLeftPx(),
            )
        }
    }

    private class GlyphOverlay(rootContext: Context, private val display: android.view.Display?) {
        // Bind the overlay WindowManager/resources to the status-bar root's actual display.
        // applicationContext can resolve to the cover/default display even when the inner
        // status-bar root is active; re-create this object when the root display geometry changes.
        private val context = display?.let { target ->
            runCatching { rootContext.createDisplayContext(target) }.getOrNull()
        } ?: rootContext
        private var wm: WindowManager? = null
        private val glyph = GlyphView(context)
        private var params: WindowManager.LayoutParams? = null

        fun attach(anchor: View, root: ViewGroup, cameraBounds: Rect): Boolean {
            if (params != null) return update(anchor, root, cameraBounds)
            val cfg = StatusBarComboStyleOneRuntime.config
            val width = StatusBarComboStyleOneRuntime.dp(
                context, StatusComboStyleOneGlyphRenderer.glyphWidthDp(cfg, StatusBarComboStyleOneRuntime.state.networkType),
            )
            val height = StatusBarComboStyleOneRuntime.dp(
                context, StatusComboStyleOneGlyphRenderer.glyphHeightDp(cfg),
            )
            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                // Without NO_LIMITS, Samsung may inset an independent overlay below
                // the status bar. The camera/punch-hole is inside that bar.
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            for (type in windowTypeCandidates()) {
                val lp = WindowManager.LayoutParams(
                    width, height, type, flags, PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.LEFT
                    title = "OneuiXP Style One"
                    // Status-bar-owned window types are system windows. Do not pass
                    // the context-window token: Samsung may reject the type/token pair
                    // and the old fallback then silently shifted the glyph down.
                    token = null
                }
                val targetDisplay = display
                val windowContext = if (Build.VERSION.SDK_INT >= 30 && targetDisplay != null) {
                    runCatching { context.createWindowContext(targetDisplay, type, null) }.getOrElse { context }
                } else context
                val candidateWindowManager = runCatching {
                    windowContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                }.getOrNull() ?: continue
                try {
                    candidateWindowManager.addView(glyph, lp)
                    wm = candidateWindowManager
                    params = lp
                    diagnostic(
                        "Glyph overlay attached: type=$type displayId=${display?.displayId} " +
                            "size=${width}x${height} flags=$flags cameraBounds=$cameraBounds"
                    )
                    if (!update(anchor, root, cameraBounds)) {
                        detach()
                        return false
                    }
                    return true
                } catch (error: Throwable) {
                    Log.w(TAG, "Glyph overlay attach failed for type=$type", error)
                }
            }
            Log.w(TAG, "No supported status-bar glyph window type; using inline renderer")
            return false
        }

        private fun windowTypeCandidates(): List<Int> {
            val result = linkedSetOf<Int>()
            // A sub-panel sits above the ordinary status-bar window. Prefer it so
            // the glyph cannot be painted underneath the status bar on newer builds.
            listOf("TYPE_STATUS_BAR_SUB_PANEL", "TYPE_STATUS_BAR_ADDITIONAL").forEach { name ->
                runCatching {
                    val field = WindowManager.LayoutParams::class.java.fields.firstOrNull {
                        it.name == name
                    } ?: WindowManager.LayoutParams::class.java.getDeclaredField(name).apply {
                        isAccessible = true
                    }
                    result += field.getInt(null)
                }.onFailure { diagnostic("Window type unavailable: $name: $it") }
            }
            if (Build.VERSION.SDK_INT >= 26) {
                result += WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                result += WindowManager.LayoutParams.TYPE_PHONE
            }
            return result.toList()
        }

        fun update(anchor: View, root: ViewGroup, cameraBounds: Rect): Boolean {
            val lp = params ?: return false
            glyph.setStatusBarRootForTypeBounds(root)
            val cutout = Rect(cameraBounds)
            val oldX = lp.x
            val oldY = lp.y
            val oldWidth = lp.width
            val oldHeight = lp.height
            val cfg = StatusBarComboStyleOneRuntime.config
            lp.width = StatusBarComboStyleOneRuntime.dp(
                context, StatusComboStyleOneGlyphRenderer.glyphWidthDp(cfg, StatusBarComboStyleOneRuntime.state.networkType),
            )
            lp.height = StatusBarComboStyleOneRuntime.dp(context, StatusComboStyleOneGlyphRenderer.glyphHeightDp(cfg))
            val networkType = StatusBarComboStyleOneRuntime.state.networkType
            val designWidthUnits = StatusComboStyleOneGlyphRenderer.glyphDesignWidth(cfg, networkType)
            val designHeightUnits = StatusComboStyleOneGlyphRenderer.glyphDesignHeight(cfg)
            val ringCenterXUnits = StatusComboStyleOneGlyphRenderer.glyphCenterX(cfg, networkType)
            val centreRatioY = StatusComboStyleOneGlyphRenderer.glyphVerticalCenterRatio(cfg)
            // Use the active status bar/display's reported cutout rectangle. Fold inner
            // screens can place the camera well to the right of the geometric centre.
            val cameraCenterX = cutout.centerX()
            val cameraCenterY = cutout.centerY()
            val displaySize = activeDisplaySize(root)
            // On this foldable, the inner-screen camera is on the right side while
            // the cover-screen camera is near the centre. Detect from cutout geometry
            // instead of applying an inner-screen correction to every display.
            val rightSideCutout = displaySize.x > 0 &&
                cameraCenterX >= displaySize.x * RIGHT_SIDE_CUTOUT_THRESHOLD
            val horizontalCalibrationPx = if (rightSideCutout) {
                (INNER_PUNCH_HOLE_HORIZONTAL_CALIBRATION_DP *
                    context.resources.displayMetrics.density).roundToInt()
            } else 0
            val verticalCalibrationDp = if (rightSideCutout) {
                INNER_PUNCH_HOLE_VERTICAL_CALIBRATION_DP
            } else {
                PUNCH_HOLE_VERTICAL_CALIBRATION_DP
            }
            // Anchor the actual drawn ring centre, not a ratio of the window width.
            // The renderer centres its canvas inside the integer-sized WindowManager bounds;
            // compensating for that exact inset prevents label-margin width changes from moving
            // the ring by even a rounded pixel.
            val renderScale = lp.height.toFloat() / designHeightUnits
            val canvasLeftInsetPx = (lp.width - designWidthUnits * renderScale) / 2f
            val ringCenterOffsetPx = ringCenterXUnits * renderScale + canvasLeftInsetPx
            lp.x = (
                cameraCenterX - ringCenterOffsetPx +
                    StatusBarComboStyleOneRuntime.dpExact(
                        context, cfg.glyphOffsetXDp + cfg.glyphLeftMarginDp - cfg.glyphRightMarginDp,
                    ) + horizontalCalibrationPx
                ).roundToInt()
            val verticalOffsetPx = (
                (cfg.glyphOffsetYDp.toFloat() + verticalCalibrationDp) *
                    context.resources.displayMetrics.density
                ).roundToInt()
            lp.y = (
                cameraCenterY - lp.height * centreRatioY + verticalOffsetPx
                ).roundToInt()
            diagnostic(
                "Glyph overlay coordinates: type=${lp.type} cameraCenter=(${cameraCenterX},${cameraCenterY}) " +
                    "window=(${lp.x},${lp.y}) size=${lp.width}x${lp.height} cutout=$cutout " +
                    "rightSideCutout=$rightSideCutout correctionDp=(" +
                    "${if (rightSideCutout) INNER_PUNCH_HOLE_HORIZONTAL_CALIBRATION_DP else 0f}," +
                    "${verticalCalibrationDp})"
            )
            if (oldX != lp.x || oldY != lp.y || oldWidth != lp.width || oldHeight != lp.height) {
                runCatching { wm?.updateViewLayout(glyph, lp) }
                    .onFailure { Log.w(TAG, "Cannot update camera-anchored glyph window", it) }
            }
            return true
        }

        fun diagnosticSnapshot(): String {
            val lp = params ?: return "overlayWindow=not-attached (no LayoutParams)"
            val actual = IntArray(2)
            val hasScreenOrigin = glyph.isAttachedToWindow &&
                runCatching { glyph.getLocationOnScreen(actual) }.isSuccess
            val screenX = if (hasScreenOrigin) actual[0] else lp.x
            val screenY = if (hasScreenOrigin) actual[1] else lp.y
            val density = context.resources.displayMetrics.density
            return buildString {
                appendLine(
                    "windowType=${lp.type},title=${lp.title},flags=${lp.flags},gravity=${lp.gravity}," +
                        "lpOrigin=(${lp.x},${lp.y}),actualViewScreenOrigin=(${if (hasScreenOrigin) actual[0] else "unknown"}," +
                        "${if (hasScreenOrigin) actual[1] else "unknown"}),lpSizePx=${lp.width}x${lp.height}," +
                        "actualViewSize=${glyph.width}x${glyph.height},attached=${glyph.isAttachedToWindow}," +
                        "windowTokenPresent=${lp.token != null},density=${density}"
                )
                appendLine(
                    StatusComboStyleOneGlyphRenderer.diagnosticGeometrySnapshot(
                        StatusBarComboStyleOneRuntime.config,
                        StatusBarComboStyleOneRuntime.state.networkType,
                        lp.width,
                        lp.height,
                        density,
                        screenX,
                        screenY,
                        glyph.minimumOutsideTypeAdvanceLeftPx(),
                    )
                )
            }
        }

        fun setTintOverride(color: Int?) { glyph.setTintOverride(color) }
        fun invalidate() { glyph.invalidate() }

        // Keep the WindowManager window attached while shade animation runs.
        // Hiding the only view with INVISIBLE can make the window stop reporting
        // visible; alpha handoff lets the native battery view paint on that frame.
        fun setShadeVisible(visible: Boolean) {
            val wanted = if (visible) 1f else 0f
            if (glyph.alpha != wanted) glyph.alpha = wanted
        }

        fun detach() {
            if (params == null) {
                wm = null
                return
            }
            runCatching { wm?.removeView(glyph) }
            params = null
            wm = null
        }
    }

    fun install(module: XposedModule, param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != Package.SYSTEMUI || !installed.compareAndSet(false, true)) return
        moduleForPreferenceReload = module
        systemUiClassLoader = param.classLoader
        runCatching {
            with(module) { PreferenceProvider.loadPreference() }
                ?.systemUI?.statusBar?.comboStyleOneRuntime?.let { config = sanitize(it) }
        }.onFailure { Log.w(TAG, "Cannot read Style One settings", it) }

        hookNativeMobileState(module, param.classLoader)
        hookNativeSignalPolicy(module, param.classLoader)
        hookVectorSignalResourceOrigins(module)
        hookModernMobileViewConstruction(module, param.classLoader)
        hookModernSignalDrawable(module)
        hookModernWifiVisibility(module, param.classLoader)

        // Style One tint is sourced per collapsed status-bar row by the native
        // icon/battery tint hooks below, never from the Quick Panel clock.
        var hookedHost = false
        listOf(PHONE_BAR, MIUI_PHONE_BAR).forEach { name ->
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
        // Native slots are handed off when IconManager creates a holder and when
        // the relevant icon state changes. Do not hook container onLayout here:
        // it runs repeatedly during shade expansion and causes view-tree scans.
        hookShadeExpansion(module, param.classLoader)
        hookIconManager(module, param.classLoader)
        hookBatterySlotVisibility(module, param.classLoader)
        hookBatteryMeters(module, param.classLoader)
        hookBatteryDrawFallback(module, param.classLoader)
        hookMiuiBatteryIsland(module, param.classLoader)
        hookBatteryTint(module, param.classLoader)
        hookNativeBatteryTint(module, param.classLoader)
        hookIconTint(module, param.classLoader)
        if (!hookedHost) Log.w(TAG, "No status-bar host constructor was hooked")
    }

    private fun hookShadeExpansion(module: XposedModule, classLoader: ClassLoader) {
        val targets = listOf(
            "com.android.systemui.shade.NotificationPanelViewControllerInjector" to "setExpandedHeightInternal",
            "com.android.systemui.shade.NotificationPanelViewController" to "setExpandedHeight",
        )
        targets.forEach { (className, methodName) ->
            runCatching {
                var current: Class<*>? = classLoader.loadClass(className)
                val candidates = mutableListOf<java.lang.reflect.Method>()
                while (current != null) {
                    candidates += current.declaredMethods.filter { method ->
                        method.name == methodName &&
                            method.parameterTypes.size == 1 &&
                            method.parameterTypes[0] == Float::class.javaPrimitiveType
                    }
                    current = current.superclass
                }
                candidates.distinctBy { it.toGenericString() }.forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        val height = runCatching { chain.getArg(0) as? Float }.getOrNull()
                        if (height != null) updateShadeBusy(height > 0.5f)
                        result
                    }
                }
            }.onFailure { diagnostic("Shade expansion hook unavailable: $className.$methodName: $it") }
        }
    }

    private fun updateShadeBusy(busy: Boolean) {
        if (shadeBusy == busy) return
        shadeBusy = busy
        val update = Runnable {
            val snapshot = synchronized(hosts) { hosts.values.toList() }
            snapshot.forEach { host ->
                if (host.retired || !config.enabled || !config.overlayGlyph || host.overlayFailed) return@forEach
                val battery = host.battery ?: return@forEach
                val overlay = host.overlay ?: return@forEach

                // QuickStar-style native visibility remains authoritative even while the
                // shade expands. Re-resolve camera geometry because the active foldable
                // display may have changed while the same SystemUI host was reused.
                val cameraBounds = cutoutBounds(host.root)
                if (cameraBounds == null) {
                    apply(host)
                    return@forEach
                }
                collapseHostView(host, battery)
                if (overlay.update(battery, host.root, cameraBounds)) {
                    overlay.setShadeVisible(true)
                    overlay.invalidate()
                } else {
                    apply(host)
                }
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) update.run() else main.post(update)
    }

    private fun showNativeGlyphForShade(host: Host, battery: View) {
        val slotAlpha = host.layouts[battery]?.alpha ?: 1f
        battery.alpha = slotAlpha
        val target = host.shadeDrawTarget ?: battery
        if (target !== battery) {
            if (!nativeChildSnapshots.containsKey(target)) {
                snapshotLayout(target)?.let { nativeChildSnapshots[target] = it }
            }
            target.alpha = nativeChildSnapshots[target]?.alpha ?: 1f
            if (target is ViewGroup) enableOnDrawIfNeeded(host, target)
            target.invalidate()
        } else if (battery is ViewGroup) {
            enableOnDrawIfNeeded(host, battery)
        }
        battery.invalidate()
    }

    private fun hideNativeGlyphAfterShade(host: Host, battery: View) {
        val target = host.shadeDrawTarget ?: battery
        if (target !== battery) {
            if (!nativeChildSnapshots.containsKey(target)) {
                snapshotLayout(target)?.let { nativeChildSnapshots[target] = it }
            }
            target.alpha = 0f
            target.invalidate()
        }
        battery.alpha = 0f
        restoreOnDrawFlag(host, target)
        battery.invalidate()
    }

    private fun enableOnDrawIfNeeded(host: Host, view: ViewGroup) {
        if (!batteryDrawHookClasses.any { it.isInstance(view) }) return
        if (!host.batteryWillNotDrawCaptured) {
            // If this ROM does not expose willNotDraw(), don't mutate a draw flag
            // that we cannot later restore.
            val original = readWillNotDraw(view) ?: return
            host.batteryWillNotDraw = original
            host.batteryWillNotDrawCaptured = true
        }
        view.setWillNotDraw(false)
    }

    private fun restoreOnDrawFlag(host: Host, battery: View) {
        val original = host.batteryWillNotDraw
        if (host.batteryWillNotDrawCaptured && battery is ViewGroup && original != null) {
            battery.setWillNotDraw(original)
        }
        host.batteryWillNotDraw = null
        host.batteryWillNotDrawCaptured = false
    }

    private fun readWillNotDraw(view: ViewGroup): Boolean? {
        var type: Class<*>? = view.javaClass
        while (type != null) {
            val current = type
            val method = runCatching {
                current.getDeclaredMethod("willNotDraw").apply { isAccessible = true }
            }.getOrNull()
            if (method != null) {
                return runCatching { method.invoke(view) as? Boolean }.getOrNull()
            }
            type = current.superclass
        }
        return null
    }

    fun isEnabled(): Boolean = config.enabled
    fun isNativeWifiSuppressed(): Boolean = config.enabled && config.showWifi
    fun isMobileNativeSuppressed(): Boolean =
        config.enabled && config.showMobile &&
            (config.signalMode == 0 || (config.signalMode == 1 && config.stackedSignal))

    /**
     * Prefer DisplayInfo's display-space cutout for the independent WindowManager
     * window. WindowInsets can be relative to the status-bar window on Samsung,
     * while TYPE_STATUS_BAR_ADDITIONAL / TYPE_APPLICATION_OVERLAY need display
     * coordinates. Only map WindowInsets coordinates as a fallback.
     */
    private fun activeDisplaySize(root: View): android.graphics.Point {
        val point = android.graphics.Point()
        val display = root.display ?: runCatching {
            (root.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }.getOrNull()
        if (display != null) runCatching { display.getRealSize(point) }
        if (point.x <= 0) point.x = root.resources.displayMetrics.widthPixels
        if (point.y <= 0) point.y = root.resources.displayMetrics.heightPixels
        return point
    }

    /**
     * Resolve the actual camera cutout for the currently attached SystemUI root.
     *
     * Do not assume the punch-hole sits near the horizontal centre. Samsung foldable
     * inner displays can put the camera well to the right. A reported rectangle is
     * accepted only when it fits the active display and touches the top edge where
     * the status bar is hosted. If no such rectangle is reported, return null so the
     * caller uses the inline renderer instead of fabricating centre coordinates.
     */
    private fun cutoutBounds(root: View): Rect? {
        if (Build.VERSION.SDK_INT < 28) return null

        val display = root.display ?: runCatching {
            (root.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }.getOrNull()
        val displaySize = activeDisplaySize(root)
        val screenWidth = displaySize.x
        val screenHeight = displaySize.y
        if (screenWidth <= 0 || screenHeight <= 0) return null

        val horizontalTolerance = dpExact(root.context, 8).coerceAtLeast(1)
        val edgeTolerance = dpExact(root.context, 8).coerceAtLeast(1)
        val topBand = max(dpExact(root.context, 64), (screenHeight * 0.12f).roundToInt())

        fun selectTopCutout(bounds: List<Rect>, source: String): Rect? {
            val candidates = bounds.filter { rect ->
                rect.width() > 0 &&
                    rect.height() > 0 &&
                    rect.left >= -horizontalTolerance &&
                    rect.right <= screenWidth + horizontalTolerance &&
                    rect.top >= -edgeTolerance &&
                    rect.top <= edgeTolerance &&
                    rect.bottom <= topBand &&
                    rect.bottom <= screenHeight + edgeTolerance
            }
            val selected = candidates.maxByOrNull { it.width().toLong() * it.height().toLong() }
                ?: return null
            val result = Rect(selected)
            diagnostic(
                "Punch-hole source=$source bounds=$result displayId=${display?.displayId} " +
                    "activeDisplay=${screenWidth}x${screenHeight}; horizontal position from reported geometry"
            )
            return result
        }

        // WindowInsets belongs to the current status-bar root and may be fresher
        // than reflected DisplayInfo while a fold/unfold transition is in flight.
        // Samsung builds vary in coordinate space, so try mapping from the attached
        // root to screen coordinates first, then try raw coordinates if already valid.
        val windowCutout = readWindowCutout(root)
        if (windowCutout != null) {
            val windowRoot = root.rootView
            val location = IntArray(2)
            windowRoot.getLocationOnScreen(location)
            val localBounds = windowCutout.boundingRects.filter { it.width() > 0 && it.height() > 0 }
            val mappedBounds = localBounds.map { Rect(it).apply { offset(location[0], location[1]) } }
            selectTopCutout(mappedBounds, "WindowInsets-mapped root=${location.contentToString()}")?.let {
                return it
            }
            selectTopCutout(localBounds, "WindowInsets-raw")?.let {
                diagnostic("WindowInsets bounds already matched active display coordinate space")
                return it
            }
            diagnostic(
                "WindowInsets did not yield a valid top-edge cutout: local=$localBounds " +
                    "root=${location.contentToString()} activeDisplay=${screenWidth}x${screenHeight}"
            )
        }

        // DisplayInfo gives display-space geometry and is the next source. A camera
        // rectangle is not rejected just because its X coordinate is far from centre.
        val displayCutout = readDisplayCutout(display)
        if (displayCutout != null) {
            selectTopCutout(displayCutout.boundingRects, "DisplayInfo")?.let { return it }
            diagnostic(
                "DisplayInfo did not provide a valid active-display top-edge cutout: " +
                    "bounds=${displayCutout.boundingRects} safeTop=${displayCutout.safeInsetTop} " +
                    "activeDisplay=${screenWidth}x${screenHeight}"
            )
        } else {
            diagnostic(
                "No DisplayInfo cutout available: displayId=${display?.displayId} " +
                    "activeDisplay=${screenWidth}x${screenHeight}"
            )
        }

        // safeInsetTop describes avoidance depth, not the camera's horizontal location.
        // It is insufficient for positioning an independent window, so never synthesize
        // a fake cutout around the display centre.
        diagnostic("No verified camera rectangle for active display; independent overlay must not use screen centre")
        return null
    }

    private fun readWindowCutout(root: View): android.view.DisplayCutout? =
        runCatching {
            (root.rootWindowInsets ?: root.rootView.rootWindowInsets)?.displayCutout
        }.getOrNull()

    private fun readDisplayCutout(display: android.view.Display?): android.view.DisplayCutout? {
        if (display == null) return null
        val getter = display.javaClass.methods.firstOrNull {
            it.name == "getCutout" && it.parameterTypes.isEmpty()
        } ?: display.javaClass.declaredMethods.firstOrNull {
            it.name == "getCutout" && it.parameterTypes.isEmpty()
        }
        val direct = getter?.let { method ->
            runCatching { method.isAccessible = true; method.invoke(display) as? android.view.DisplayCutout }.getOrNull()
        }
        if (direct != null) return direct

        var current: Class<*>? = display.javaClass
        var displayInfo: Any? = null
        while (current != null && displayInfo == null) {
            val field = current.declaredFields.firstOrNull {
                it.name.equals("mDisplayInfo", ignoreCase = true) ||
                    it.name.equals("displayInfo", ignoreCase = true)
            }
            if (field != null) {
                displayInfo = runCatching {
                    field.isAccessible = true
                    field.get(display)
                }.getOrNull()
            }
            current = current.superclass
        }
        if (displayInfo == null) return null

        current = displayInfo.javaClass
        while (current != null) {
            val field = current.declaredFields.firstOrNull {
                it.name.equals("displayCutout", ignoreCase = true)
            }
            if (field != null) {
                val cutout = runCatching {
                    field.isAccessible = true
                    field.get(displayInfo) as? android.view.DisplayCutout
                }.getOrNull()
                if (cutout != null) return cutout
            }
            current = current.superclass
        }
        return null
    }


    /**
     * Captures status-bar/window geometry only when Settings → Send logs is requested.
     * This is read-only: it never changes layout, visibility, or the foldable lifecycle.
     */
    private fun buildLiveDiagnosticSnapshot(): String {
        val hostList = synchronized(hosts) { hosts.values.toList().filter { !it.retired } }
        val cfg = config
        val currentState = state
        return buildString {
            appendLine("SnapshotTimeMs=${System.currentTimeMillis()}")
            appendLine(
                "Config enabled=${cfg.enabled},overlayGlyph=${cfg.overlayGlyph},showMobile=${cfg.showMobile}," +
                    "showWifi=${cfg.showWifi},signalMode=${cfg.signalMode},mobileTypeMode=${cfg.mobileTypeMode}," +
                    "stackedSignal=${cfg.stackedSignal},dataSimOnly=${cfg.dataSimOnly},dualSim=${cfg.dualSim}," +
                    "dualSimMode=${cfg.dualSimMode},trioStyle=${cfg.trioStyle}"
            )
            appendLine(
                "Config glyphScalePercent=${cfg.glyphScalePercent},glyphOffsetXDp=${cfg.glyphOffsetXDp}," +
                    "glyphOffsetYDp=${cfg.glyphOffsetYDp},glyphLeftMarginDp=${cfg.glyphLeftMarginDp}," +
                    "glyphRightMarginDp=${cfg.glyphRightMarginDp},outTypeSizeDp=${cfg.outTypeSizeDp}," +
                    "outTypeMarginLeftDp=${cfg.outTypeMarginLeftDp},outTypeMarginRightDp=${cfg.outTypeMarginRightDp}," +
                    "outSignalSizeDp=${cfg.outSignalSizeDp},outSignalMarginDp=${cfg.outSignalMarginDp}," +
                    "ringStroke=${cfg.ringStroke},typeWeight=${cfg.typeWeight},typeSuffixScale=${cfg.typeSuffixScale}"
            )
            appendLine(
                "State batteryLevel=${currentState.batteryLevel},charging=${currentState.charging}," +
                    "quickCharge=${currentState.quickCharge},wifiLevel=${currentState.wifiLevel}," +
                    "mobileLevel=${currentState.mobileLevel},simOneLevel=${currentState.simOneLevel}," +
                    "simTwoLevel=${currentState.simTwoLevel},dataSimSlotIndex=${currentState.dataSimSlotIndex}," +
                    "networkType=${currentState.networkType},powerSave=${currentState.powerSave}," +
                    "lowBatteryWarning=${currentState.lowBatteryWarning}"
            )
            appendLine("Hosts collected=${hostList.size}; attached=${hostList.count { it.root.isAttachedToWindow }}")
            if (hostList.isEmpty()) {
                appendLine("No registered StatusBarComboStyleOneRuntime hosts are currently available.")
            }
            hostList.take(6).forEachIndexed { hostIndex, host ->
                val root = host.root
                val metrics = root.resources.displayMetrics
                val displaySize = activeDisplaySize(root)
                val rootScreen = IntArray(2)
                val rootWindow = IntArray(2)
                runCatching { root.getLocationOnScreen(rootScreen) }
                runCatching { root.getLocationInWindow(rootWindow) }
                val camera = runCatching { cutoutBounds(root) }.getOrNull()
                appendLine("[Host ${hostIndex}] retired=${host.retired},applying=${host.applying}," +
                    "overlayFailed=${host.overlayFailed},rootAttached=${root.isAttachedToWindow}")
                appendLine(
                    "Display id=${root.display?.displayId},realSizePx=${displaySize.x}x${displaySize.y}," +
                        "metricsPx=${metrics.widthPixels}x${metrics.heightPixels},density=${metrics.density}," +
                        "densityDpi=${metrics.densityDpi},cameraBounds=${camera}"
                )
                appendLine(
                    "Root class=${root.javaClass.name},resource=${resourceName(root)},screenOrigin=${rootScreen.contentToString()}," +
                        "windowOrigin=${rootWindow.contentToString()},localBounds=[${root.left},${root.top},${root.right},${root.bottom}]," +
                        "size=${root.width}x${root.height},measured=${root.measuredWidth}x${root.measuredHeight}," +
                        "padding=[${root.paddingLeft},${root.paddingTop},${root.paddingRight},${root.paddingBottom}]," +
                        "clipChildren=${root.clipChildren},clipToPadding=${root.clipToPadding}"
                )
                appendLine(
                    "Host battery=${host.battery?.javaClass?.name},shadeDrawTarget=${host.shadeDrawTarget?.javaClass?.name}," +
                        "inlinePresent=${host.inline != null},overlayPresent=${host.overlay != null}," +
                        "savedLayouts=${host.layouts.size},savedVisibility=${host.original.size}," +
                        "batteryWillNotDraw=${host.batteryWillNotDraw},activeDisplay=${host.activeDisplayId}:" +
                        "${host.activeDisplayWidth}x${host.activeDisplayHeight}"
                )
                if (host.overlay != null) {
                    appendLine("[Independent overlay geometry]")
                    appendLine(host.overlay!!.diagnosticSnapshot())
                }
                host.inline?.let { glyph ->
                    val pos = IntArray(2)
                    runCatching { glyph.getLocationOnScreen(pos) }
                    appendLine("[Inline glyph geometry]")
                    appendLine(
                        StatusComboStyleOneGlyphRenderer.diagnosticGeometrySnapshot(
                            cfg, currentState.networkType, glyph.width, glyph.height, metrics.density,
                            pos[0], pos[1], glyph.minimumOutsideTypeAdvanceLeftPx(),
                        )
                    )
                }

                val allViews = descendants(root)
                val candidates = linkedSetOf<View>()
                candidates += root
                val clusters = allViews.filter(::isStatusIconsCluster)
                clusters.forEach { cluster ->
                    candidates += cluster
                    val group = cluster as? ViewGroup ?: return@forEach
                    for (i in 0 until group.childCount) {
                        val child = group.getChildAt(i)
                        candidates += child
                        val nested = child as? ViewGroup
                        if (nested != null) {
                            for (j in 0 until nested.childCount) candidates += nested.getChildAt(j)
                        }
                    }
                }
                listOfNotNull(host.battery, host.shadeDrawTarget, host.inline).forEach { anchor ->
                    candidates += anchor
                    val parent = anchor.parent as? ViewGroup ?: return@forEach
                    candidates += parent
                    for (i in 0 until parent.childCount) candidates += parent.getChildAt(i)
                }
                allViews.forEach { view ->
                    val className = view.javaClass.name.lowercase()
                    val resource = resourceName(view).lowercase()
                    val slot = (knownIconSlots[view] ?: slotOf(view)).orEmpty()
                    val looksLikeStatusIcon = className.contains("statusbariconview") ||
                        className.contains("statusicon") || className.contains("iconcontainer") ||
                        className.contains("batterymeter") || className.contains("batteryicon") ||
                        className.contains("mobileview") || className.contains("wifi") ||
                        resource.contains("status_icon") || resource.contains("statusicon") ||
                        resource.contains("battery") || resource.contains("mobile") ||
                        resource.contains("wifi") || slot.isNotBlank()
                    if (looksLikeStatusIcon || view === host.battery || view === host.inline ||
                        view === host.shadeDrawTarget || isWifiIcon(view) || isMobileIcon(view) ||
                        isMobileTypeView(view) || isBatteryIcon(view)
                    ) candidates += view
                }
                appendLine("[Status-bar view geometry] candidates=${candidates.size}; outputLimit=100")
                candidates.take(100).forEach { view -> appendLine(diagnosticViewSummary(host, view)) }
                if (candidates.size > 100) {
                    appendLine("View geometry truncated: ${candidates.size - 100} additional views omitted.")
                }
            }
        }.take(48 * 1024)
    }

    private fun diagnosticViewSummary(host: Host, view: View): String {
        val screen = IntArray(2)
        val window = IntArray(2)
        runCatching { view.getLocationOnScreen(screen) }
        runCatching { view.getLocationInWindow(window) }
        val parent = view.parent as? View
        val parentGroup = parent as? ViewGroup
        val childIndex = parentGroup?.indexOfChild(view) ?: -1
        val lp = view.layoutParams
        val margins = lp as? ViewGroup.MarginLayoutParams
        val saved = host.layouts[view]
        val slot = (knownIconSlots[view] ?: slotOf(view)).orEmpty()
        val visibility = when (view.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> view.visibility.toString()
        }
        val savedText = saved?.let {
            "${it.width}x${it.height}/m=${it.leftMargin},${it.topMargin},${it.rightMargin},${it.bottomMargin}/vis=${it.visibility}/alpha=${it.alpha}"
        } ?: "-"
        return "View class=${view.javaClass.name},id=${resourceName(view).ifBlank { "-" }},slot=${slot.ifBlank { "-" }}," +
            "visibility=${visibility},alpha=${view.alpha},screenOrigin=${screen.contentToString()}," +
            "windowOrigin=${window.contentToString()},localBounds=[${view.left},${view.top},${view.right},${view.bottom}]," +
            "size=${view.width}x${view.height},measured=${view.measuredWidth}x${view.measuredHeight}," +
            "translation=[${view.translationX},${view.translationY}],padding=[${view.paddingLeft},${view.paddingTop}," +
            "${view.paddingRight},${view.paddingBottom}],lp=${lp?.width}x${lp?.height}," +
            "margins=[${margins?.leftMargin ?: 0},${margins?.topMargin ?: 0},${margins?.rightMargin ?: 0},${margins?.bottomMargin ?: 0}]," +
            "clipBounds=${view.clipBounds},parent=${parent?.javaClass?.name}:${resourceName(parent ?: view).ifBlank { "-" }}," +
            "childIndex=${childIndex},parentChildCount=${parentGroup?.childCount ?: -1}," +
            "parentClipChildren=${parentGroup?.clipChildren},parentClipToPadding=${parentGroup?.clipToPadding}," +
            "savedOriginal=${savedText},originalVisibility=${host.original[view] ?: "-"}"
    }

    private fun scheduleRegister(root: ViewGroup) {
        // Register the shared, on-demand diagnostics reply channel with the first live SystemUI context.
        OneuiXPDiagnosticLog.setLiveSnapshotProvider { buildLiveDiagnosticSnapshot() }
        OneuiXPDiagnosticLog.install(root.context)
        root.post { register(root) }
        // One bounded retry for SystemUI's deferred icon inflation. Do not keep
        // repeating whole-root discovery during status-bar/Quick Panel animation.
        root.postDelayed({
            val needsRetry = synchronized(hosts) { hosts[root]?.battery == null }
            if (needsRetry && root.isAttachedToWindow) register(root)
        }, 400L)
    }

    private fun ensureHostLifecycleListener(root: ViewGroup) {
        synchronized(hostLifecycleListeners) {
            if (hostLifecycleListeners.containsKey(root)) return
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    (v as? ViewGroup)?.let(::scheduleRegister)
                }

                override fun onViewDetachedFromWindow(v: View) {
                    val detachedRoot = v as? ViewGroup ?: return
                    main.post { cleanupDetachedHost(detachedRoot) }
                }
            }
            hostLifecycleListeners[root] = listener
            root.addOnAttachStateChangeListener(listener)
        }
    }

    private fun cleanupDetachedHost(root: ViewGroup) {
        val host = synchronized(hosts) { hosts.remove(root) } ?: return
        host.retired = true
        main.removeCallbacks(host.geometryRetry)
        main.removeCallbacks(host.delayedApply)
        host.geometryRetryPending = false
        host.geometryRetryAttempts = 0
        host.battery?.let { restoreOnDrawFlag(host, host.shadeDrawTarget ?: it) }
        host.overlay?.detach()
        host.overlay = null
        removeInline(host)
        restore(host)
        restoreNativeBatteryViews(root)
        host.battery = null
        host.shadeDrawTarget = null
        host.tintRow = null
        host.rowTint = null
        diagnostic("Status-bar host detached; restored native views and released overlay")
    }

    private fun register(root: ViewGroup) {
        // Prefer PhoneStatusBarView when it is present; StatusBarWindowView can
        // contain the same battery icon and drawing both roots would double-render.
        if (root.javaClass.name == WINDOW_BAR) {
            val phoneRoot = descendants(root).firstOrNull {
                it !== root && (it.javaClass.name == PHONE_BAR || it.javaClass.name == MIUI_PHONE_BAR)
            } as? ViewGroup
            if (phoneRoot != null) {
                register(phoneRoot)
                return
            }
            // Never treat the entire status-bar window as the combo host: it also
            // owns shade/Quick Panel content on several ROMs. The dedicated phone
            // bar constructor will register the right row as soon as it inflates.
            return
        }
        if (root.javaClass.name == PHONE_BAR || root.javaClass.name == MIUI_PHONE_BAR) {
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
        ensureHostLifecycleListener(root)
        if (host.activeDisplayId < 0) {
            host.activeDisplayId = root.display?.displayId ?: -1
        }
        if (!preferenceReceiverReady) reloadConfigBeforeReceiver()
        ensureReceivers(root.context)
        apply(host)
    }

    private fun refreshHosts() {
        main.post { synchronized(hosts) { hosts.values.toList() }.forEach(::apply) }
    }

    /**
     * Size changes are frequent while the user drags the scale slider. Update only
     * the glyph/anchor bounds instead of rescanning and suppressing the full view
     * tree on every preview frame.
     */
    private fun resizeGlyphs() {
        main.post {
            synchronized(hosts) { hosts.values.toList() }.forEach { host ->
                if (host.retired) return@forEach
                val battery = host.battery ?: return@forEach
                host.inline?.let { glyph ->
                    glyph.setStatusBarRootForTypeBounds(host.root)
                    val width = dp(glyph.context, StatusComboStyleOneGlyphRenderer.glyphWidthDp(config, state.networkType))
                    val height = dp(glyph.context, StatusComboStyleOneGlyphRenderer.glyphHeightDp(config))
                    val lp = glyph.layoutParams
                    if (lp != null) {
                        val margins = lp as? ViewGroup.MarginLayoutParams
                        // Extra label/signal space grows the canvas to the left. Cancel its added
                        // row occupancy with a negative start margin so native icons stay fixed.
                        val leadingExpansionPx = glyphLeadingExpansionPx(glyph.context, state.networkType)
                        val left = dpExact(glyph.context, config.glyphLeftMarginDp) - leadingExpansionPx
                        val right = dpExact(glyph.context, config.glyphRightMarginDp)
                        val changed = lp.width != width || lp.height != height || margins?.let {
                            it.leftMargin != left || it.rightMargin != right || it.topMargin != 0 || it.bottomMargin != 0
                        } == true
                        if (changed) {
                            lp.width = width
                            lp.height = height
                            margins?.setMargins(left, 0, right, 0)
                            glyph.layoutParams = lp
                        }
                    }
                    glyph.translationX = dpExact(glyph.context, config.glyphOffsetXDp).toFloat()
                    glyph.translationY = dpExact(glyph.context, config.glyphOffsetYDp).toFloat()
                    glyph.invalidate()
                }
                host.overlay?.let { overlay ->
                    val cameraBounds = cutoutBounds(host.root)
                    if (cameraBounds == null) {
                        apply(host)
                    } else {
                        collapseHostView(host, battery)
                        if (overlay.update(battery, host.root, cameraBounds)) {
                            overlay.invalidate()
                        } else {
                            apply(host)
                        }
                    }
                }
            }
        }
    }

    private fun reloadConfigBeforeReceiver() {
        if (preferenceReceiverReady) return
        val module = moduleForPreferenceReload ?: return
        val latest = runCatching {
            with(module) { PreferenceProvider.loadPreference() }?.systemUI?.statusBar?.comboStyleOneRuntime
        }.onFailure { Log.w(TAG, "Cannot reload Style One settings before receiver setup", it) }
            .getOrNull() ?: return
        config = sanitize(latest)
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
            old.mobileTypeMode != new.mobileTypeMode

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

    private fun shouldRotateChargingDisplay(): Boolean {
        val centredBatteryValue = config.trioStyle == 0 && config.valueCentred
        val wifiInk = config.showWifi && state.wifiLevel >= 1
        val innerType = config.showMobile && config.mobileTypeMode == 1 &&
            state.networkType.isNotBlank()
        val batteryValueInOpening = config.trioStyle == 0 && !config.valueCentred &&
            (wifiInk || innerType != null)
        return config.enabled && config.chargingDisplayMode == 1 &&
            config.showBolt && config.showValue && state.charging &&
            ((!centredBatteryValue && config.mobileTypeMode == 1) || batteryValueInOpening)
    }

    private fun syncChargingRotationTicker(resetPhase: Boolean = true) {
        main.removeCallbacks(chargingRotationTicker)
        if (shouldRotateChargingDisplay()) {
            if (resetPhase && state.chargingAlternatePhase) {
                state = state.copy(chargingAlternatePhase = false)
                invalidateGlyphs()
            }
            main.postDelayed(chargingRotationTicker, 5_000L)
        } else if (state.chargingAlternatePhase) {
            state = state.copy(chargingAlternatePhase = false)
            invalidateGlyphs()
        }
    }

    private fun apply(host: Host) {
        if (host.retired || host.applying) return
        host.applying = true
        try {
            val root = host.root
            if (!root.isAttachedToWindow) {
                main.removeCallbacks(host.geometryRetry)
                host.geometryRetryPending = false
                host.geometryRetryAttempts = 0
                host.overlay?.detach()
                return
            }
            val currentDisplayId = root.display?.displayId ?: -1
            val currentDisplaySize = activeDisplaySize(root)
            val displayIdentityChanged =
                (currentDisplayId >= 0 && host.activeDisplayId >= 0 &&
                    currentDisplayId != host.activeDisplayId) ||
                (host.activeDisplayWidth > 0 && host.activeDisplayHeight > 0 &&
                    (currentDisplaySize.x != host.activeDisplayWidth ||
                        currentDisplaySize.y != host.activeDisplayHeight))
            if (displayIdentityChanged) {
                diagnostic(
                    "Status-bar display geometry changed: id=${host.activeDisplayId}->${currentDisplayId}, " +
                        "size=${host.activeDisplayWidth}x${host.activeDisplayHeight}->" +
                        "${currentDisplaySize.x}x${currentDisplaySize.y}; resetting overlay only"
                )
                main.removeCallbacks(host.geometryRetry)
                host.geometryRetryPending = false
                host.geometryRetryAttempts = 0
                host.battery?.let { restoreOnDrawFlag(host, host.shadeDrawTarget ?: it) }
                host.overlay?.detach()
                host.overlay = null
                removeInline(host)
                restore(host)
                restoreNativeBatteryViews(root)
                host.battery = null
                host.shadeDrawTarget = null
                host.tintRow = null
                host.rowTint = rowTintSnapshots[root]
            }
            if (currentDisplayId >= 0) host.activeDisplayId = currentDisplayId
            if (currentDisplaySize.x > 0 && currentDisplaySize.y > 0) {
                host.activeDisplayWidth = currentDisplaySize.x
                host.activeDisplayHeight = currentDisplaySize.y
            }
            val views = descendants(root)
            rememberStatusBarTintViews(root, views)
            // One UI 8.5 / Android 16+ can render the battery as a ComposeView
            // immediately after the classic statusIcons cluster. Prefer that
            // active surface over an obsolete/inactive BatteryMeterView anchor (Android 16/17).
            val battery = findModernBatteryComposeView(root)
                ?: views.firstOrNull(::isBatteryMeterView)
                ?: views.firstOrNull(::isBatteryIcon)

            if (!config.enabled || battery == null) {
                main.removeCallbacks(host.geometryRetry)
                host.geometryRetryPending = false
                host.geometryRetryAttempts = 0
                host.battery?.let { restoreOnDrawFlag(host, host.shadeDrawTarget ?: it) }
                host.overlay?.detach()
                host.overlay = null
                removeInline(host)
                restore(host)
                if (!config.enabled) restoreNativeBatteryViews(root)
                host.battery = null
                host.shadeDrawTarget = null
                host.tintRow = null
                host.rowTint = null
                return
            }

            val previousBattery = host.battery
            if (previousBattery != null && previousBattery !== battery) {
                restoreOnDrawFlag(host, host.shadeDrawTarget ?: previousBattery)
                host.overlay?.detach()
                host.overlay = null
                removeInline(host)
                restoreHostView(host, previousBattery)
                restoreNativeBatteryViews(previousBattery)
                host.shadeDrawTarget = null
            }
            host.battery = battery
            syncBatteryStateFromMeter(battery)
            host.shadeDrawTarget = views.firstOrNull { candidate ->
                candidate !== battery &&
                    isWithin(battery, candidate) &&
                    candidate.javaClass.simpleName.contains("MiuiBatteryMeterIconView", ignoreCase = true)
            } ?: battery
            val nextTintRow = batteryTintContainerFor(battery) ?: root
            if (host.tintRow !== nextTintRow) {
                host.tintRow = nextTintRow
                // A replacement battery/container must not inherit the previous
                // row's last foreground while its own tint is being initialized.
                host.rowTint = null
            }
            readNativeBatteryTint(battery)?.let { batteryTintSnapshots[battery] = it }
            val meterTint = batteryTintSnapshots[battery] ?: views.firstNotNullOfOrNull { candidate ->
                val isBatteryTintOwner = candidate === battery ||
                    isWithin(battery, candidate) || isWithin(candidate, battery)
                if (isBatteryTintOwner) batteryTintSnapshots[candidate] else null
            }
            // The upstream tint source is a value saved for the MIUI battery
            // container. The phone-bar value is only a fallback for AOSP layouts.
            val tintRow = host.tintRow
            val rowTint = tintRow?.let { rowTintSnapshots[it] }
                ?: meterTint
                // Only AOSP layouts use the whole status-bar root as their tint
                // owner. On MIUI, falling back to another icon container's ink
                // would reintroduce the previously identified cross-row colour leak.
                ?: if (tintRow === root) rowTintSnapshots[root] else null
                ?: host.rowTint
            if (rowTint != null && host.rowTint != rowTint) {
                host.rowTint = rowTint
                host.inline?.setTintOverride(rowTint)
                host.overlay?.setTintOverride(rowTint)
            }
            // Resolve the active display's camera before touching native status-bar slots.
            // If Samsung has not supplied valid geometry yet, leave the whole native icon row alone
            // and perform at most one delayed retry; never collapse/restore it on a retry loop.
            val wantsOverlay = config.overlayGlyph && !host.overlayFailed
            val overlayCameraBounds = if (wantsOverlay) cutoutBounds(root) else null
            if (wantsOverlay && overlayCameraBounds == null) {
                if (!host.geometryRetryPending && host.geometryRetryAttempts == 0) {
                    diagnostic("No stable cutout geometry yet; leaving native status-bar layout unchanged")
                }
                scheduleGeometryRetry(host)
                return
            }

            val foldMobile = isMobileNativeSuppressed()

            views.forEach { view ->
                val nativeBatteryIndicator = isBatteryIcon(view) ||
                    (slotOf(view) ?: knownIconSlots[view])?.contains("battery", ignoreCase = true) == true
                if (view === battery || view is GlyphView || nativeBatteryIndicator) return@forEach
                // Keep Wi-Fi on the established view/layout path for now. The
                // experimental native Wi-Fi slot interception is disabled after a
                // SystemUI/LSPosed stability regression.
                val wifiCandidate = isWifiIcon(view)
                if (wifiCandidate && !nativeWifiBaselineVisibility.containsKey(view)) {
                    val nativeVisibility = host.layouts[view]?.visibility ?: view.visibility
                    nativeWifiBaselineVisibility[view] = nativeVisibility
                }
                val hide =
                    (config.showWifi && wifiCandidate) ||
                    (foldMobile && isMobileIcon(view)) ||
                    (config.showMobile && config.mobileTypeMode == 2 && isMobileTypeView(view))
                if (hide) collapseHostView(host, view) else restoreHostView(host, view)
                if (wifiCandidate) diagnoseNativeWifiView(view, if (hide) "apply-collapse" else "apply-restore")
            }

            if (wantsOverlay) {
                // Geometry was checked before suppressing any native slot. Reuse that one
                // snapshot throughout this apply pass so all icon/window work is atomic.
                val cameraBounds = requireNotNull(overlayCameraBounds)
                removeInline(host)
                val overlay = host.overlay ?: GlyphOverlay(root.context, root.display).also { host.overlay = it }
                overlay.setTintOverride(host.rowTint)
                if (!overlay.attach(battery, root, cameraBounds)) {
                    // Failed attachment must restore every native view changed above.
                    // Never leave the row collapsed while a retry waits.
                    diagnostic("Overlay attach failed; restoring native icon row and retrying once")
                    overlay.detach()
                    host.overlay = null
                    restore(host)
                    scheduleGeometryRetry(host)
                } else {
                    main.removeCallbacks(host.geometryRetry)
                    host.geometryRetryPending = false
                    host.geometryRetryAttempts = 0
                    // Anchor the independent renderer to the reported camera rectangle,
                    // never a presumed centre. Collapse the native meter to avoid overdraw.
                    collapseHostView(host, battery)
                    overlay.setShadeVisible(true)
                    overlay.update(battery, root, cameraBounds)
                    overlay.invalidate()
                }
            } else {
                main.removeCallbacks(host.geometryRetry)
                host.geometryRetryPending = false
                host.geometryRetryAttempts = 0
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

    private fun insertInline(host: Host, anchor: View, compactToAnchor: Boolean = false) {
        val parent = anchor.parent as? ViewGroup ?: return
        val designWidth = StatusComboStyleOneGlyphRenderer.glyphWidthDp(config, state.networkType)
        val designHeight = StatusComboStyleOneGlyphRenderer.glyphHeightDp(config)
        val originalHeight = host.layouts[anchor]?.height?.takeIf { it > 0 }
            ?: anchor.height.takeIf { it > 0 }
            ?: dp(parent.context, 24)
        val width = if (compactToAnchor) {
            (originalHeight * designWidth.toFloat() / designHeight).roundToInt().coerceAtLeast(1)
        } else {
            dp(parent.context, designWidth)
        }
        val height = if (compactToAnchor) originalHeight else dp(parent.context, designHeight)
        host.inline?.takeIf { it.parent === parent }?.let { glyph ->
            glyph.setStatusBarRootForTypeBounds(host.root)
            val lp = glyph.layoutParams
            if (lp != null) {
                val margins = lp as? ViewGroup.MarginLayoutParams
                val leadingExpansionPx = glyphLeadingExpansionPx(parent.context, state.networkType)
                val left = dpExact(parent.context, config.glyphLeftMarginDp) - leadingExpansionPx
                val right = dpExact(parent.context, config.glyphRightMarginDp)
                val changed = lp.width != width || lp.height != height || margins?.let {
                    it.leftMargin != left || it.rightMargin != right ||
                        it.topMargin != 0 || it.bottomMargin != 0
                } == true
                if (changed) {
                    lp.width = width
                    lp.height = height
                    margins?.setMargins(left, 0, right, 0)
                    glyph.layoutParams = lp
                }
            }
            glyph.invalidate()
            return
        }
        removeInline(host)
        val glyph = GlyphView(parent.context).apply {
            setTintOverride(host.rowTint)
            setStatusBarRootForTypeBounds(host.root)
        }
        val old = anchor.layoutParams
        val lp = when (old) {
            is LinearLayout.LayoutParams -> LinearLayout.LayoutParams(old).apply { this.width = width; this.height = height }
            is FrameLayout.LayoutParams -> FrameLayout.LayoutParams(old).apply { this.width = width; this.height = height }
            is RelativeLayout.LayoutParams -> RelativeLayout.LayoutParams(old).apply { this.width = width; this.height = height }
            is ViewGroup.MarginLayoutParams -> ViewGroup.MarginLayoutParams(old).apply { this.width = width; this.height = height }
            else -> ViewGroup.LayoutParams(width, height)
        }
        val margins = lp as? ViewGroup.MarginLayoutParams
        val leadingExpansionPx = glyphLeadingExpansionPx(parent.context, state.networkType)
        margins?.setMargins(
            dpExact(parent.context, config.glyphLeftMarginDp) - leadingExpansionPx,
            0,
            dpExact(parent.context, config.glyphRightMarginDp),
            0,
        )
        glyph.translationX = dpExact(parent.context, config.glyphOffsetXDp).toFloat()
        glyph.translationY = dpExact(parent.context, config.glyphOffsetYDp).toFloat()
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

    /**
     * Rightmost visible native icon edge within the active status-icons cluster. Only leaves are
     * measured: the StatusIconContainer's own bounds include unused end padding, not visible ink.
     */
    private fun lastVisibleStatusIconRightScreenPx(root: View?): Int? {
        root ?: return null
        val cluster = descendants(root).firstOrNull(::isStatusIconsCluster) ?: return null
        val leaves = descendants(cluster).drop(1).filter { candidate ->
            val isLeaf = candidate !is ViewGroup || candidate.childCount == 0
            if (!isLeaf || candidate.width <= 0 || candidate.height <= 0) return@filter false
            var current: View? = candidate
            var visible = true
            while (current != null) {
                if (current.visibility != View.VISIBLE || current.alpha <= 0f) visible = false
                if (current === cluster) break
                current = current.parent as? View
            }
            visible
        }
        return leaves.mapNotNull { candidate ->
            runCatching {
                val location = IntArray(2)
                candidate.getLocationOnScreen(location)
                location[0] + candidate.width
            }.getOrNull()
        }.maxOrNull()
    }

    private fun minimumOutsideTypeAdvanceLeftPx(view: View, statusBarRoot: View?): Float? {
        val root = statusBarRoot ?: view.rootView
        val iconRight = lastVisibleStatusIconRightScreenPx(root) ?: return null
        val viewLocation = IntArray(2)
        if (runCatching { view.getLocationOnScreen(viewLocation) }.isFailure) return null
        // Keep a small, real gap between the custom label and the last visible native icon.
        return (iconRight + dpExact(view.context, 2) - viewLocation[0]).toFloat()
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

    private fun isComposeHost(view: View): Boolean {
        val name = view.javaClass.name.lowercase()
        return name.contains("androidx.compose.ui.platform.composeview") ||
            name.contains("androidcomposeview") ||
            name.endsWith(".composeview")
    }

    private fun isStatusIconsCluster(view: View): Boolean {
        val resource = resourceName(view)
        val name = view.javaClass.name
        return resource.equals("statusIcons", ignoreCase = true) ||
            resource.equals("status_icons", ignoreCase = true) ||
            name.contains("StatusIconContainer", ignoreCase = true) ||
            viewSignature(view).contains("statusicons")
    }

    /**
     * Android 16's newStatusBarIcons path may put the battery in Compose after
     * the classic statusIcons cluster, with no R.id.battery / BatteryMeterView.
     * Resolve that sibling structurally; do not guess by hiding arbitrary
     * ComposeViews elsewhere in SystemUI.
     */
    private fun findModernBatteryComposeView(root: View): View? {
        val clusters = descendants(root).filter(::isStatusIconsCluster)
        for (cluster in clusters) {
            val parent = cluster.parent as? ViewGroup ?: continue
            val index = parent.indexOfChild(cluster)
            if (index < 0) continue
            val end = minOf(parent.childCount, index + 4)
            for (childIndex in (index + 1) until end) {
                val candidate = parent.getChildAt(childIndex)
                if (isComposeHost(candidate)) return candidate
                // Some builds wrap the battery's ComposeView in a single-child
                // container. Only accept this narrow wrapper shape.
                val wrapper = candidate as? ViewGroup ?: continue
                if (wrapper.childCount == 1 && isComposeHost(wrapper.getChildAt(0))) {
                    return wrapper
                }
            }
        }
        return null
    }

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

    private fun isManagedWifiSlot(slot: String): Boolean {
        val normalized = slot.lowercase()
        return normalized == "wifi" || normalized.contains("wlan") ||
            normalized.startsWith("wifi_") || normalized.startsWith("wifi.")
    }

    private fun shouldSuppressSlot(slot: String): Boolean {
        val normalized = slot.lowercase()
        return (config.enabled && normalized.contains("battery")) ||
            (config.enabled && config.showWifi && isManagedWifiSlot(normalized)) ||
            (isMobileNativeSuppressed() && isManagedMobileSlot(normalized))
    }

    private fun hostFor(view: View): Host? = synchronized(hosts) {
        hosts.values.firstOrNull { !it.retired && isWithin(it.root, view) }
    }

    private fun compactIconDescriptor(v: View): String =
        viewSignature(v).filter { it.isLetterOrDigit() }

    private fun isWifiIcon(v: View): Boolean {
        if (v is GlyphView) return false
        val slot = (slotOf(v) ?: knownIconSlots[v]).orEmpty().lowercase()
        if (isManagedWifiSlot(slot)) return true
        val name = compactIconDescriptor(v)
        if (name.contains("battery") || name.contains("bluetooth") || name.contains("airplane")) return false
        // Normalize separators so "Wi-Fi" and "Wi Fi" cannot evade Wi-Fi detection.
        return name.contains("wifi") || name.contains("wlan") || name.contains("wireless") ||
            name.contains("slotwifi")
    }

    private fun diagnoseNativeWifiView(view: View, action: String) {
        val slot = (slotOf(view) ?: knownIconSlots[view]).orEmpty()
        val lp = view.layoutParams
        val descriptor = viewSignature(view).take(180)
        val snapshot = "$action|showWifi=${config.showWifi}|wifiLevel=${state.wifiLevel}|" +
            "vis=${view.visibility}|alpha=${view.alpha}|" +
            "size=${lp?.width}x${lp?.height}|slot=$slot"
        if (lastNativeWifiDiagnostics.put(view, snapshot) != snapshot) {
            diagnostic("Native Wi-Fi view: $snapshot class=${view.javaClass.name} desc=$descriptor")
        }
    }

    private fun diagnoseNativeWifiSlotRequest(controller: Any?, slot: String, requested: Boolean) {
        val owner = controller?.let { System.identityHashCode(it).toString() } ?: "null"
        val key = "$owner|${slot.lowercase()}"
        val snapshot = "visible=$requested|showWifi=${config.showWifi}|wifiLevel=${state.wifiLevel}"
        if (lastNativeWifiSlotRequests.put(key, snapshot) != snapshot) {
            diagnostic("Native Wi-Fi slot request: slot=$slot $snapshot owner=$owner")
        }
    }

    private fun isMobileIcon(v: View): Boolean {
        if (v is GlyphView || isMobileTypeView(v) || isWifiIcon(v)) return false
        val slot = (slotOf(v) ?: knownIconSlots[v]).orEmpty().lowercase()
        if (isManagedWifiSlot(slot)) return false
        if (isManagedMobileSlot(slot)) return true
        val name = compactIconDescriptor(v)
        if (name.contains("battery") || name.contains("bluetooth") || name.contains("airplane") ||
            name.contains("wifi") || name.contains("wlan") || name.contains("wireless")
        ) return false
        // SignalClusterView is commonly a parent containing Wi-Fi, mobile,
        // airplane and other icons. Never collapse that whole group as a signal icon.
        return name.contains("mobile") || name.contains("cellular") || name.contains("signalmobile") ||
            name.contains("slotmobile") || name.contains("signalstrength") ||
            (name.contains("signalcluster") && v !is ViewGroup)
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
            // Roll back the experimental Wi-Fi native-slot interception.
            // Use the established per-view/layout suppression path until the
            // SystemUI crash has been isolated and a safer slot API is verified.
            val wifiCandidate = isWifiIcon(view)
            val hide = config.enabled && (
                (config.showWifi && wifiCandidate) ||
                    (isMobileNativeSuppressed() && isMobileIcon(view)) ||
                    (config.showMobile && config.mobileTypeMode == 2 && isMobileTypeView(view))
            )
            if (hide) collapseHostView(host, view) else restoreHostView(host, view)
            if (wifiCandidate) diagnoseNativeWifiView(view, if (hide) "container-collapse" else "container-restore")
        }
        lastContainerFingerprints[container] = containerSuppressionFingerprint(container)
    }

    private fun snapshotLayout(view: View): LayoutSnapshot? {
        val lp = view.layoutParams ?: return null
        val margin = lp as? ViewGroup.MarginLayoutParams
        return LayoutSnapshot(
            lp.width, lp.height, margin?.leftMargin ?: 0, margin?.topMargin ?: 0,
            margin?.rightMargin ?: 0, margin?.bottomMargin ?: 0, view.visibility, view.alpha,
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
        view.alpha = snapshot.alpha
        // Modern status-bar icon containers can cache child placement until the
        // parent is asked to lay out again after GONE/0x0 folding is reversed.
        (view.parent as? View)?.requestLayout()
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
        val nativeBatterySnapshot = nativeBatteryLayouts[view]
        host.original.putIfAbsent(view, nativeBatterySnapshot?.visibility ?: view.visibility)
        if (!host.layouts.containsKey(view)) {
            (nativeBatterySnapshot ?: snapshotLayout(view))?.let { host.layouts[view] = it }
        }
        collapseViewLayout(view)
    }

    private fun scheduleGeometryRetry(host: Host) {
        // Exactly one delayed retry per failed attach/display transition. Re-running apply
        // in a rapid loop repeatedly scans and collapses the entire icon row.
        if (host.geometryRetryPending || host.geometryRetryAttempts >= 1) return
        host.geometryRetryPending = true
        main.postDelayed(host.geometryRetry, 320L)
    }

    private fun reserveHostSlot(host: Host, view: View, widthDp: Int) {
        host.original.putIfAbsent(view, view.visibility)
        if (!host.layouts.containsKey(view)) snapshotLayout(view)?.let { host.layouts[view] = it }
        val lp = view.layoutParams ?: return
        val margins = lp as? ViewGroup.MarginLayoutParams
        val width = dp(view.context, widthDp)
        val left = dpExact(view.context, config.glyphLeftMarginDp)
        val right = dpExact(view.context, config.glyphRightMarginDp)
        val changed = lp.width != width || margins?.let {
            it.leftMargin != left || it.topMargin != 0 || it.rightMargin != right || it.bottomMargin != 0
        } == true
        if (changed) {
            lp.width = width
            margins?.setMargins(left, 0, right, 0)
            view.layoutParams = lp
        }
        val originalVisibility = host.original[view] ?: View.VISIBLE
        if (view.visibility != originalVisibility) view.visibility = originalVisibility
        if (view.alpha != 0f) view.alpha = 0f
    }
    private fun restoreHostView(host: Host, view: View) {
        host.layouts.remove(view)?.let { runCatching { restoreLayout(view, it) } }
        host.original.remove(view)?.let { old -> runCatching { view.visibility = old } }
    }

    private fun collapseNativeChild(view: View, hideLayout: Boolean) {
        if (!nativeChildSnapshots.containsKey(view)) {
            snapshotLayout(view)?.let { nativeChildSnapshots[view] = it }
        }
        if (hideLayout && nativeChildSnapshots.containsKey(view)) {
            // Samsung's tint callbacks can reset native battery alpha. GONE plus zero
            // dimensions suppresses the real icon instead of merely making it transparent.
            collapseViewLayout(view)
        } else {
            // Keep MIUI children measured to avoid layout churn around its charging island.
            if (view.alpha != 0f) view.alpha = 0f
        }
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
        // Some Samsung builds draw the stock battery from a nested icon view whose
        // field name differs between One UI versions. Walk the actual meter subtree
        // and collapse every battery-specific view, rather than relying on alpha:
        // tint callbacks can restore alpha and make the native icon reappear.
        descendants(meter).drop(1).forEach { child ->
            val slot = slotOf(child) ?: knownIconSlots[child]
            val signature = viewSignature(child)
            val isBatteryDescendant = signature.contains("battery") &&
                !signature.contains("island")
            if (isBatteryDescendant || slot?.contains("battery", ignoreCase = true) == true) {
                collapseNativeChild(child, hideLayout = true)
            }
        }
        // Preserve explicit Samsung/MIUI field fallbacks even when resource names
        // are obfuscated or absent. Charging-island views are handled separately.
        listOf(
            "mBatteryIconView", "batteryIconView", "mHollowBatteryIconView", "hollowBatteryIconView",
            "mBatteryChargingView", "mBatteryChargingIconView", "batteryChargingView",
            "mBatteryPercentContainer", "batteryPercentContainer", "mBatteryPercentView", "batteryPercentView",
        ).forEach { name ->
            fieldView(meter, listOf(name))?.takeIf { it !== meter }
                ?.let { collapseNativeChild(it, hideLayout = true) }
        }
    }

    private fun hasNativeField(target: Any, vararg names: String): Boolean {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            val current = type
            val found = runCatching {
                current.declaredFields.any { field ->
                    names.any { name -> name.equals(field.name, ignoreCase = true) }
                }
            }.getOrDefault(false)
            if (found) return true
            type = current.superclass
        }
        return false
    }

    /**
     * MIUI/HyperOS may remove the battery meter's reserved width for its charging
     * island. The combo glyph is anchored to that meter, so doing so would leave
     * the glyph floating over the island. While Style One owns the status icon,
     * decline only the layout replacement request; keep the true request in the
     * MIUI field and replay it if Style One is disabled during the animation.
     */
    private fun hookMiuiBatteryIsland(module: XposedModule, classLoader: ClassLoader) {
        runCatching {
            val meterClass = classLoader.loadClass(MIUI_BATTERY_METER)
            meterClass.declaredMethods.filter { method ->
                method.name == "updateIslandChanged" &&
                    method.parameterTypes.size == 2 &&
                    method.parameterTypes.all { it == Boolean::class.javaPrimitiveType }
            }.forEach { method ->
                module.hook(method).intercept { chain ->
                    val meter = chain.thisObject as? View
                    val requestedHide = chain.getArg(0) as? Boolean ?: false
                    val trigger = chain.getArg(1) as? Boolean ?: false
                    if (meter != null) batteryIslandRequests[meter] = requestedHide to trigger
                    if (!config.enabled) {
                        chain.proceed()
                    } else {
                        // Let the charging island itself continue unchanged while
                        // retaining the battery anchor's width for the combined glyph.
                        val result = chain.proceed(arrayOf(false, trigger))
                        if (meter != null) {
                            setBooleanField(meter, "mStoreIsAddBatteryIsland", requestedHide)
                        }
                        result
                    }
                }
                miuiIslandMeterHooked = true
            }
        }.onFailure { diagnostic("MIUI battery island meter hook unavailable: $it") }

        // Older/newer HyperOS builds may route the visibility request through the
        // container without this meter callback. Preserve the request and suppress
        // only the width-removal while Style One owns the anchor.
        if (!miuiIslandMeterHooked) runCatching {
            val containerClass = classLoader.loadClass(MIUI_BATTERY_CONTAINER)
            containerClass.declaredMethods.filter { method ->
                method.name == "setIsHideBattery" &&
                    method.parameterTypes.size == 1 &&
                    (method.parameterTypes[0] == Boolean::class.javaPrimitiveType ||
                        method.parameterTypes[0] == Boolean::class.javaObjectType)
            }.forEach { method ->
                module.hook(method).intercept { chain ->
                    val container = chain.thisObject as? View
                    val requestedHide = chain.getArg(0) as? Boolean ?: false
                    if (container != null) batteryContainerIslandRequests[container] = requestedHide
                    if (config.enabled) chain.proceed(arrayOf(false)) else chain.proceed()
                }
            }
        }.onFailure { diagnostic("MIUI battery island container fallback unavailable: $it") }
    }

    private fun setBooleanField(instance: Any, fieldName: String, value: Boolean) {
        var type: Class<*>? = instance.javaClass
        while (type != null) {
            val current = type
            val field = runCatching {
                current.getDeclaredField(fieldName).apply { isAccessible = true }
            }.getOrNull()
            if (field != null) {
                runCatching {
                    if (field.type == Boolean::class.javaPrimitiveType) field.setBoolean(instance, value)
                    else field.set(instance, value)
                }
                return
            }
            type = current.superclass
        }
    }

    private fun replayMiuiBatteryIslandRequests() {
        main.post {
            if (miuiIslandMeterHooked) {
                val requests = synchronized(batteryIslandRequests) {
                    batteryIslandRequests.entries.map { it.key to it.value }
                }
                requests.forEach { (meter, request) ->
                    runCatching {
                        var type: Class<*>? = meter.javaClass
                        var method: java.lang.reflect.Method? = null
                        while (type != null && method == null) {
                            method = type.declaredMethods.firstOrNull { candidate ->
                                candidate.name == "updateIslandChanged" &&
                                    candidate.parameterTypes.size == 2 &&
                                    candidate.parameterTypes.all { it == Boolean::class.javaPrimitiveType }
                            }
                            type = type.superclass
                        }
                        method?.apply { isAccessible = true }?.invoke(meter, request.first, request.second)
                        setBooleanField(meter, "mStoreIsAddBatteryIsland", request.first)
                    }.onFailure { diagnostic("Cannot replay MIUI island state: $it") }
                }
            } else {
                val requests = synchronized(batteryContainerIslandRequests) {
                    batteryContainerIslandRequests.entries.map { it.key to it.value }
                }
                requests.forEach { (container, requestedHide) ->
                    runCatching {
                        var type: Class<*>? = container.javaClass
                        var method: java.lang.reflect.Method? = null
                        while (type != null && method == null) {
                            method = type.declaredMethods.firstOrNull { candidate ->
                                candidate.name == "setIsHideBattery" &&
                                    candidate.parameterTypes.size == 1 &&
                                    (candidate.parameterTypes[0] == Boolean::class.javaPrimitiveType ||
                                        candidate.parameterTypes[0] == Boolean::class.javaObjectType)
                            }
                            type = type.superclass
                        }
                        method?.apply { isAccessible = true }?.invoke(container, requestedHide)
                    }.onFailure { diagnostic("Cannot replay MIUI island container state: $it") }
                }
            }
        }
    }

    private fun hookBatteryDrawFallback(module: XposedModule, classLoader: ClassLoader) {
        // The upstream renderer paints into MiuiBatteryMeterIconView.onDraw when its separate
        // window is not covering the row. Use the same event path only while the
        // shade is transforming the status bar; never poll or rescan the tree.
        val candidates = listOf(
            "com.android.systemui.statusbar.views.MiuiBatteryMeterIconView",
            MIUI_BATTERY_METER,
            "com.android.systemui.statusbar.phone.MiuiBatteryMeterView",
            "com.android.systemui.statusbar.phone.MiuiBatteryMeterIconView",
            BATTERY_METER,
        )
        val methods = LinkedHashMap<String, java.lang.reflect.Method>()
        candidates.forEach { className ->
            runCatching {
                var current: Class<*>? = classLoader.loadClass(className)
                var found: java.lang.reflect.Method? = null
                while (current != null &&
                    current.name != "android.view.View" &&
                    current.name != "android.view.ViewGroup" &&
                    found == null
                ) {
                    found = current.declaredMethods.firstOrNull { method ->
                        method.name == "onDraw" &&
                            method.parameterTypes.size == 1 &&
                            method.parameterTypes[0] == Canvas::class.java
                    }
                    current = current.superclass
                }
                found?.let { method ->
                    methods.putIfAbsent(method.toGenericString(), method)
                    if (!batteryDrawHookClasses.contains(method.declaringClass)) {
                        batteryDrawHookClasses.add(method.declaringClass)
                    }
                }
            }.onFailure { diagnostic("Battery draw hook unavailable: $className: $it") }
        }
        methods.values.forEach { method ->
            module.hook(method).intercept { chain ->
                val result = chain.proceed()
                if (!config.enabled) return@intercept result
                val view = chain.thisObject as? View ?: return@intercept result
                val host = synchronized(hosts) {
                    hosts.values.firstOrNull { candidate ->
                        !candidate.retired &&
                            (candidate.battery === view || candidate.shadeDrawTarget === view)
                    }
                } ?: return@intercept result
                if (view.width <= 0 || view.height <= 0) return@intercept result
                val canvas = runCatching { chain.getArg(0) as? Canvas }.getOrNull()
                    ?: return@intercept result

                // Clear stock pixels every time this OEM battery surface draws. Visibility
                // and alpha alone are not sufficient on builds that reapply icon tint.
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                // During shade expansion, the native surface is temporarily the glyph
                // renderer's canvas. Paint OneuiXP there only when the overlay is hidden.
                if (shadeBusy && config.overlayGlyph && host.overlay != null &&
                    host.shadeDrawTarget === view && cutoutBounds(host.root) == null
                ) {
                    val currentState = state
                    StatusComboStyleOneGlyphRenderer.draw(
                        canvas,
                        view.width,
                        view.height,
                        config,
                        currentState.copy(tintColor = host.rowTint ?: currentState.tintColor),
                    )
                }
                result
            }
        }
        if (methods.isEmpty()) diagnostic("No battery onDraw hook was found for shade fallback")
    }

    private fun hookBatteryMeters(module: XposedModule, classLoader: ClassLoader) {
        val hookedHideListMethods = mutableSetOf<String>()
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
                // shade expansion. State is read from the native meter's own callbacks.
                cls.declaredMethods.filter {
                    it.name in setOf(
                        "onFinishInflate", "onAttachedToWindow", "onBatteryLevelChanged",
                        "updateChargeAndText", "updatePercentText", "updateShowPercent",
                        "scaleBatteryMeterViewsLegacy", "scaleBatteryMeterViews", "updateColors", "onDarkChanged",
                    )
                }.forEach { module.hook(it).intercept(callback) }

                // Match SystemUI/QuickStar's native icon-hide-list contract. BatteryMeterView
                // consumes "icon_blacklist" and applies View.GONE itself; pass a temporary
                // list with the battery slot hidden only while Style One is enabled. Do not
                // write Settings.Secure, so user/QuickStar preferences remain untouched.
                var current: Class<*>? = cls
                val hideListMethods = mutableListOf<java.lang.reflect.Method>()
                while (current != null) {
                    hideListMethods += current.declaredMethods.filter { method ->
                        method.name == "onTuningChanged" &&
                            method.parameterTypes.size == 2 &&
                            method.parameterTypes[0] == String::class.java &&
                            method.parameterTypes[1] == String::class.java
                    }
                    current = current.superclass
                }
                hideListMethods.distinctBy { it.toGenericString() }.forEach { method ->
                    if (!hookedHideListMethods.add(method.toGenericString())) return@forEach
                    module.hook(method).setPriority(100).intercept { chain ->
                        val args = chain.getArgs().toTypedArray()
                        val key = args.getOrNull(0) as? String
                        val meter = chain.thisObject as? ViewGroup
                        val target = key == ICON_HIDE_LIST_KEY && meter != null &&
                            isStatusBarBatteryMeter(meter)
                        val nativeList = args.getOrNull(1) as? String
                        if (target && meter != null) {
                            snapshotLayout(meter)?.let { snapshot ->
                                val userHidesBattery = nativeList?.split(',')?.any {
                                    it.trim().equals("battery", ignoreCase = true)
                                } == true
                                val baseline = if (userHidesBattery) snapshot.copy(visibility = View.GONE) else snapshot
                                nativeBatteryLayouts.putIfAbsent(meter, baseline)
                            }
                            if (config.enabled) {
                                // A null tuner value means the blacklist is empty. Still pass
                                // an explicit battery entry so SystemUI's own visibility path
                                // can hide the stock indicator.
                                args[1] = appendHiddenSlot(nativeList.orEmpty(), "battery")
                            }
                        }
                        val result = chain.proceed(args)
                        if (target && config.enabled && meter != null) {
                            // Also cover Samsung variants that consume the callback but do
                            // not use AOSP's helper to update the BatteryMeterView visibility.
                            if (meter.visibility != View.GONE) meter.visibility = View.GONE
                        }
                        result
                    }
                }
                diagnostic("Battery icon-hide-list hook installed for $className (${hideListMethods.size} candidate methods)")
            }.onFailure { diagnostic("Battery meter hook unavailable: $className: $it") }
        }
    }

    private fun isStatusBarBatteryMeter(view: View): Boolean {
        if (hostFor(view) != null) return true
        var current: View? = view
        while (current != null) {
            val name = current.javaClass.name
            if (name == PHONE_BAR || name == MIUI_PHONE_BAR) return true
            current = current.parent as? View
        }
        return false
    }

    private fun appendHiddenSlot(rawList: String, slot: String): String {
        val entries = rawList.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return if (entries.any { it.equals(slot, ignoreCase = true) }) rawList
        else (entries + slot).joinToString(",")
    }

    private fun scheduleBatteryChildCollapse(meter: ViewGroup) {
        if (!config.enabled || hostFor(meter) == null) return
        synchronized(scheduledBatteryChildPasses) {
            if (!scheduledBatteryChildPasses.add(meter)) return
        }
        meter.post {
            try {
                if (config.enabled && hostFor(meter) != null) {
                    // Keep reading Samsung's resolved charging state, but delegate stock
                    // icon visibility to the native icon-hide-list/slot visibility path.
                    syncBatteryStateFromMeter(meter)
                    // Some Samsung battery callbacks reset VISIBLE after the tuner update.
                    // Re-assert only on existing battery-state callbacks, never on measure/layout.
                    if (config.enabled && meter.visibility != View.GONE) {
                        meter.visibility = View.GONE
                    }
                }
            } finally {
                scheduledBatteryChildPasses.remove(meter)
            }
        }
    }

    private fun syncBatteryStateFromMeter(meter: View) {
        val nextLevel = readSignalIntField(meter, "mLevel", "level")
            ?.takeIf { it in 0..100 }
        val nextCharging = readNativeBoolean(meter, "mCharging", "charging")
        val nextQuickCharging = readSamsungFastCharging(meter)
            ?: readNativeBoolean(meter, "mQuickCharging", "quickCharging", "mFastCharging", "fastCharging", "mIsFastCharging", "isFastCharging")
        val nextLow = readNativeBoolean(meter, "mLow", "low")
        val nextPowerSave = readNativeBoolean(meter, "mPowerSave", "powerSave")
        val previous = state
        val charging = nextCharging ?: previous.charging
        val next = previous.copy(
            batteryLevel = nextLevel ?: previous.batteryLevel,
            charging = charging,
            quickCharge = charging && (nextQuickCharging == true || isSamsungFastChargeTint(meter) || previous.quickCharge),
            lowBatteryWarning = nextLow ?: previous.lowBatteryWarning,
            powerSave = nextPowerSave ?: previous.powerSave,
        )
        if (next != previous) {
            state = next
            invalidateGlyphs()
        }
        if (previous.charging != state.charging) {
            syncChargingRotationTicker()
        }
    }

    /**
     * MIUI's battery meter is the most accurate source for the colour actually
     * used to paint the native battery icon. Capture the resolved tint per meter,
     * rather than deriving it from the clock or whichever icon tinted last.
     */
    private fun hookBatteryTint(module: XposedModule, classLoader: ClassLoader) {
        val methods = LinkedHashMap<String, java.lang.reflect.Method>()
        listOf(
            MIUI_BATTERY_METER,
            "com.android.systemui.statusbar.views.MiuiBatteryMeterIconView",
            "com.android.systemui.statusbar.phone.MiuiBatteryMeterView",
            "com.android.systemui.statusbar.phone.MiuiBatteryMeterIconView",
        ).forEach { className ->
            runCatching {
                var current: Class<*>? = classLoader.loadClass(className)
                while (current != null) {
                    current.declaredMethods.filter { method ->
                        method.name == "updateLightDarkTint" &&
                            method.parameterTypes.size == 6 &&
                            method.parameterTypes[1] == Float::class.javaPrimitiveType &&
                            method.parameterTypes[2] == Int::class.javaPrimitiveType &&
                            method.parameterTypes[3] == Int::class.javaPrimitiveType &&
                            method.parameterTypes[4] == Int::class.javaPrimitiveType &&
                            method.parameterTypes[5] == Boolean::class.javaPrimitiveType
                    }.forEach { methods.putIfAbsent(it.toGenericString(), it) }
                    current = current.superclass
                }
            }.onFailure { diagnostic("MIUI battery tint class unavailable: $className: $it") }
        }
        methods.values.forEach { method ->
            module.hook(method).intercept { chain ->
                val result = chain.proceed()
                val meter = chain.thisObject as? View
                if (meter != null) {
                    val intensity = runCatching { chain.getArg(1) as? Float }.getOrNull() ?: 0f
                    val tintColor = runCatching { chain.getArg(2) as? Int }.getOrNull() ?: Color.TRANSPARENT
                    val lightColor = runCatching { chain.getArg(3) as? Int }.getOrNull() ?: Color.TRANSPARENT
                    val darkColor = runCatching { chain.getArg(4) as? Int }.getOrNull() ?: Color.TRANSPARENT
                    val useTint = runCatching { chain.getArg(5) as? Boolean }.getOrNull() ?: false
                    // The current callback arguments are the colour SystemUI is
                    // applying for this exact light/dark transition. MIUI's backing
                    // fields can lag one transition, so use them only as fallback.
                    val currentTint = if (useTint) tintColor else if (intensity > 0f) darkColor else lightColor
                    val fieldTint = readNativeBatteryTint(meter)
                    val resolved = currentTint.takeIf { it != Color.TRANSPARENT } ?: fieldTint
                    if (resolved != null && resolved != Color.TRANSPARENT) {
                        saveBatteryTint(meter, resolved)
                    }
                }
                result
            }
        }
        if (methods.isEmpty()) diagnostic("No MIUI updateLightDarkTint method was found")
    }

    /**
     * AOSP/Samsung BatteryMeterView commonly receives its tint through
     * DarkReceiver.onDarkChanged instead of MIUI's updateLightDarkTint method.
     * Track that colour per meter as well; it is the foreground used for the
     * battery itself, not the last tint observed on a neighbouring status icon.
     */
    private fun hookNativeBatteryTint(module: XposedModule, classLoader: ClassLoader) {
        listOf(BATTERY_METER).forEach classLoop@{ className ->
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

    private fun readNativeBatteryTint(meter: View): Int? {
        val useTint = readNativeBoolean(meter, "mUseTint", "useTint")
        val tintColor = readSignalIntField(meter, "mTintColor", "tintColor")
        val lightColor = readSignalIntField(meter, "mLightColor", "lightColor")
        val darkColor = readSignalIntField(meter, "mDarkColor", "darkColor")
        val intensity = readNativeFloat(meter, "mDarkIntensity", "darkIntensity") ?: 0f
        val available = useTint != null || tintColor != null || lightColor != null || darkColor != null
        if (!available) return null
        val resolved = if (useTint == true) tintColor
            else if (intensity > 0f) darkColor ?: lightColor
            else lightColor ?: darkColor
        return resolved?.takeIf { it != Color.TRANSPARENT }
    }

    private fun readSamsungFastCharging(meter: View): Boolean? {
        val drawable = readNativeObject(meter, "mSamsungDrawable", "samsungDrawable") ?: return null
        val batteryState = readNativeObject(drawable, "batteryState", "mBatteryState", "state") ?: drawable
        return readNativeBoolean(
            batteryState,
            "mQuickCharging", "quickCharging", "isQuickCharging",
            "mFastCharging", "fastCharging", "isFastCharging",
            "mSuperFastCharging", "superFastCharging", "isSuperFastCharging",
            "isFastCharger", "fastCharge", "mFastCharge",
        )
    }

    private fun isSamsungFastChargeTint(meter: View): Boolean {
        val drawable = readNativeObject(meter, "mSamsungDrawable", "samsungDrawable") ?: return false
        val tint = readSignalIntField(drawable, "iconTint", "mIconTint") ?: return false
        val red = Color.red(tint)
        val green = Color.green(tint)
        val blue = Color.blue(tint)
        // Samsung's fast-charge icon tint is blue; ordinary charging remains status-bar tinted.
        return blue >= 120 && blue > red * 1.25 && blue >= green * 0.90
    }

    private fun readNativeObject(target: Any, vararg names: String): Any? {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            val current = type
            for (name in names) {
                val field = runCatching {
                    current.declaredFields.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?.apply { isAccessible = true }
                }.getOrNull() ?: continue
                val value = runCatching { field.get(target) }.getOrNull()
                if (value != null) return value
            }
            type = current.superclass
        }
        return null
    }

    private fun readNativeBoolean(target: Any, vararg names: String): Boolean? {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            val current = type
            for (name in names) {
                val field = runCatching {
                    current.declaredFields.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?.apply { isAccessible = true }
                }.getOrNull() ?: continue
                val value = runCatching { field.get(target) as? Boolean }.getOrNull()
                if (value != null) return value
            }
            type = current.superclass
        }
        return null
    }

    private fun readNativeFloat(target: Any, vararg names: String): Float? {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            val current = type
            for (name in names) {
                val field = runCatching {
                    current.declaredFields.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?.apply { isAccessible = true }
                }.getOrNull() ?: continue
                val value = runCatching { (field.get(target) as? Number)?.toFloat() }.getOrNull()
                if (value != null) return value
            }
            type = current.superclass
        }
        return null
    }

    private fun saveBatteryTint(meter: View, color: Int) {
        batteryTintSnapshots[meter] = color
        val host = hostFor(meter)
        val anchor = host?.battery
        val belongsToAnchor = anchor != null &&
            (anchor === meter || isWithin(anchor, meter) || isWithin(meter, anchor))
        if (host != null && belongsToAnchor) {
            val tintRow = host.tintRow ?: batteryTintContainerFor(anchor!!)
            val owner = tintRow ?: host.root
            // This callback carries the colour SystemUI is painting now. Replace
            // the previous value instead of freezing the first light/dark tint
            // observed after SystemUI started.
            rowTintSnapshots[owner] = color
            if (owner === host.root) rowTintSnapshots[host.root] = color
            if (host.rowTint != color) {
                host.rowTint = color
                host.inline?.setTintOverride(color)
                host.overlay?.setTintOverride(color)
            }
        }
    }

    /**
     * Returns the closest MIUI battery row that owns this icon. This is the same
     * boundary used to keep launcher, status-bar, and control-center tints consistent
     * from overwriting one another. AOSP has no such container, so callers fall
     * back to the collapsed PhoneStatusBarView.
     */
    private fun batteryTintContainerFor(view: View): ViewGroup? {
        var node: View? = view
        while (node != null) {
            if (node is ViewGroup) {
                val name = node.javaClass.name
                if (name == MIUI_BATTERY_CONTAINER ||
                    node.javaClass.simpleName.contains("StatusBatteryContainer", ignoreCase = true)
                ) return node
            }
            node = node.parent as? View
        }
        return null
    }

    private fun classifyTintIcon(icon: View): TintIconClassification {
        val row = statusBarRootOf(icon) ?: return TintIconClassification(false)
        val tintOwner = batteryTintContainerFor(icon) ?: row
        return TintIconClassification(
            isCollapsedStatusBar = true,
            root = WeakReference(row),
            tintOwner = WeakReference(tintOwner),
        )
    }

    private fun tintOwnerFor(icon: View): Pair<ViewGroup, ViewGroup>? {
        val cached = tintIconClassifications[icon]
        if (cached != null) {
            if (!cached.isCollapsedStatusBar) return null
            val root = cached.root?.get()
            val owner = cached.tintOwner?.get()
            if (root != null && owner != null) return root to owner
        }
        // Classify each icon once. Expanded shade/QS icons cache a negative
        // result and are skipped on subsequent tint-animation frames.
        val resolved = classifyTintIcon(icon)
        tintIconClassifications[icon] = resolved
        if (!resolved.isCollapsedStatusBar) return null
        val root = resolved.root?.get() ?: return null
        val owner = resolved.tintOwner?.get() ?: return null
        return root to owner
    }

    private fun rememberStatusBarTintViews(root: ViewGroup, views: Iterable<View>) {
        val iconClass = statusBarIconViewClass ?: return
        synchronized(tintIconClassifications) {
            views.forEach { view ->
                if (iconClass.isInstance(view)) {
                    val tintOwner = batteryTintContainerFor(view) ?: root
                    tintIconClassifications[view] = TintIconClassification(
                        isCollapsedStatusBar = true,
                        root = WeakReference(root),
                        tintOwner = WeakReference(tintOwner),
                    )
                }
            }
        }
    }

    private fun statusBarRootOf(view: View): ViewGroup? {
        var node: View? = view
        while (node != null) {
            if (node is ViewGroup &&
                (node.javaClass.name == PHONE_BAR || node.javaClass.name == MIUI_PHONE_BAR)
            ) return node
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
            val iconClass = classLoader.loadClass("com.android.systemui.statusbar.StatusBarIconView")
            statusBarIconViewClass = iconClass
            var cls: Class<*>? = iconClass
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
                        val icon = chain.thisObject as? View ?: return@intercept result
                        val tintOwner = tintOwnerFor(icon) ?: return@intercept result
                        var color: Int? = null
                        method.parameterTypes.forEachIndexed { index, type ->
                            if (color == null && type == Int::class.javaPrimitiveType) {
                                val value = runCatching { chain.getArg(index) as? Int }.getOrNull()
                                if (value != null && value != Color.TRANSPARENT) color = value
                            }
                        }
                        val resolvedColor = color
                        val row = tintOwner.first
                        val owner = tintOwner.second
                        if (resolvedColor != null) {
                            // Cache by MIUI's battery container so each status-bar row keeps its own tint.
                            // Also keep the root value solely as the AOSP fallback.
                            rowTintSnapshots[owner] = resolvedColor
                            if (owner === row) rowTintSnapshots[row] = resolvedColor
                            val host = synchronized(hosts) { hosts[row] }
                            if (host != null && (host.tintRow ?: row) === owner &&
                                host.rowTint != resolvedColor
                            ) {
                                val applyTint = Runnable {
                                    if (!host.retired && (host.tintRow ?: row) === owner &&
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
                            if (statusBarIconViewClass?.isInstance(view) == true) {
                                tintIconClassifications[view] = classifyTintIcon(view)
                            }
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

    /**
     * QuickStar-style visibility is managed at the status-bar icon slot, not just
     * by hiding BatteryMeterView child ImageViews. Keep the battery slot suppressed
     * while Style One is enabled and restore SystemUI-requested visibility on disable.
     */
    private fun hookBatterySlotVisibility(module: XposedModule, classLoader: ClassLoader) {
        val targets = listOf(
            "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
            "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl",
        )
        val methods = mutableListOf<java.lang.reflect.Method>()
        targets.forEach { className ->
            runCatching { classLoader.loadClass(className) }.getOrNull()?.let { cls ->
                var current: Class<*>? = cls
                while (current != null) {
                    methods += current.declaredMethods.filter { method ->
                        method.name == "setIconVisibility" &&
                            method.parameterTypes[0] == String::class.java &&
                            ((method.parameterTypes.size == 2 &&
                                method.parameterTypes[1] == Boolean::class.javaPrimitiveType) ||
                                (method.parameterTypes.size == 3 &&
                                    ((method.parameterTypes[1] == Boolean::class.javaPrimitiveType &&
                                        method.parameterTypes[2] == Int::class.javaPrimitiveType) ||
                                     (method.parameterTypes[1] == Int::class.javaPrimitiveType &&
                                        method.parameterTypes[2] == Boolean::class.javaPrimitiveType))))
                    }
                    current = current.superclass
                }
                cls.declaredConstructors.forEach { constructor ->
                    runCatching {
                        module.hook(constructor).intercept { chain ->
                            val result = chain.proceed()
                            val controller = chain.thisObject
                            if (controller != null) {
                                iconVisibilityControllers.add(controller)
                                main.postDelayed({ applyBatterySlotVisibility() }, 500L)
                            }
                            result
                        }
                    }.onFailure { diagnostic("Battery slot controller constructor hook failed: $it") }
                }
            }
        }

        methods.distinctBy { it.toGenericString() }.forEach { method ->
            runCatching {
                module.hook(method).setPriority(100).intercept { chain ->
                    val controller = chain.thisObject
                    if (controller != null) iconVisibilityControllers.add(controller)
                    val args = chain.getArgs().toTypedArray()
                    val slot = args.getOrNull(0) as? String
                    // Keep the icon controller's semantic visibility in sync with
                    // the view tree. New status-bar icons may ignore View.VISIBLE if
                    // their holder/binding still says the slot is hidden.
                    // Keep this native-slot hook battery-only. Extending it to Wi-Fi
                    // was the latest high-risk change preceding the LSPosed safe mode.
                    val batterySlot = slot?.contains("battery", ignoreCase = true) == true
                    val managedSlot = batterySlot
                    val visibleIndex = when {
                        args.getOrNull(1) is Boolean -> 1
                        args.size >= 3 && args.getOrNull(1) is Int && args.getOrNull(2) is Boolean -> 2
                        else -> -1
                    }
                    if (slot != null && isManagedWifiSlot(slot) && visibleIndex >= 0) {
                        diagnoseNativeWifiSlotRequest(
                            controller, slot, args[visibleIndex] as? Boolean ?: true,
                        )
                    }
                    val tag = when {
                        args.size >= 3 && args.getOrNull(1) is Int && visibleIndex == 2 -> args[1] as Int
                        args.size >= 3 && args.getOrNull(2) is Int && visibleIndex == 1 -> args[2] as Int
                        else -> 0
                    }
                    if (managedSlot && slot != null && visibleIndex >= 0) {
                        knownBatterySlotNames.add(slot)
                        val alreadyEnforcing = enforcingBatterySlotVisibility.get() == true
                        val requestedVisible = args[visibleIndex] as? Boolean ?: true
                        val styleSuppressesSlot = shouldSuppressSlot(slot)
                        if (!alreadyEnforcing && controller != null) {
                            val key = batterySlotVisibilityKey(slot, tag)
                            val previousNativeState = synchronized(nativeBatterySlotVisibility) {
                                nativeBatterySlotVisibility[controller]?.get(key)
                            }
                            // While OneuiXP owns a slot, SystemUI can repeatedly submit
                            // false because the slot is currently hidden. Do not mistake
                            // that enforced state for QuickStar's saved preference. Still
                            // accept true updates so a newly-active Wi-Fi holder is cached.
                            // Once OneuiXP stops suppressing it, accept both true and false
                            // from SystemUI so the user's QuickStar preference is respected.
                            if (!styleSuppressesSlot || requestedVisible || previousNativeState == null) {
                                rememberNativeBatterySlotVisibility(
                                    controller, slot, tag, requestedVisible,
                                )
                            }
                        }
                        if (styleSuppressesSlot) args[visibleIndex] = false
                    }

                    val previousEnforcement = enforcingBatterySlotVisibility.get() == true
                    if (managedSlot) enforcingBatterySlotVisibility.set(true)
                    try {
                        chain.proceed(args)
                    } finally {
                        if (managedSlot) {
                            if (previousEnforcement) enforcingBatterySlotVisibility.set(true)
                            else enforcingBatterySlotVisibility.remove()
                        }
                    }
                }
            }.onFailure { diagnostic("Battery slot visibility hook failed for ${method.declaringClass.name}: $it") }
        }
        diagnostic("Battery slot visibility hook installed for ${methods.distinctBy { it.toGenericString() }.size} methods")
    }

    private fun batterySlotVisibilityKey(slot: String, tag: Int): String = "$slot|$tag"

    private fun parseBatterySlotVisibilityKey(key: String): Pair<String, Int>? {
        val separator = key.lastIndexOf('|')
        if (separator <= 0) return null
        val tag = key.substring(separator + 1).toIntOrNull() ?: return null
        return key.substring(0, separator) to tag
    }

    private fun rememberNativeBatterySlotVisibility(
        controller: Any, slot: String, tag: Int, visible: Boolean,
    ) {
        synchronized(nativeBatterySlotVisibility) {
            val snapshot = nativeBatterySlotVisibility[controller]
                ?: mutableMapOf<String, Boolean>().also { nativeBatterySlotVisibility[controller] = it }
            snapshot[batterySlotVisibilityKey(slot, tag)] = visible
        }
    }

    private fun applyBatterySlotVisibility() {
        main.post {
            val controllers = synchronized(iconVisibilityControllers) { iconVisibilityControllers.toList() }
            controllers.forEach { controller ->
                val snapshot = synchronized(nativeBatterySlotVisibility) {
                    nativeBatterySlotVisibility[controller]?.toMap().orEmpty()
                }
                val targets = (snapshot.keys.mapNotNull(::parseBatterySlotVisibilityKey) +
                    knownBatterySlotNames.map { it to 0 }).distinct()
                // Never mutate the Wi-Fi holder's native visibility. A previous
                // implementation could hide the holder while the view briefly
                // reappeared, then SystemUI's next bind hid it again. Wi-Fi now
                // uses collapseHostView()/restoreHostView() exclusively.
                targets.filterNot { (slot, _) -> isManagedWifiSlot(slot) }
                    .forEach { (slot, tag) ->
                        val key = batterySlotVisibilityKey(slot, tag)
                        if (shouldSuppressSlot(slot)) {
                            if (!snapshot.containsKey(key)) {
                                val currentVisibility = readNativeBatterySlotVisibility(controller, slot, tag) ?: true
                                rememberNativeBatterySlotVisibility(controller, slot, tag, currentVisibility)
                            }
                            setNativeBatterySlotVisibility(controller, slot, tag, false)
                        } else {
                            val original = synchronized(nativeBatterySlotVisibility) {
                                nativeBatterySlotVisibility[controller]?.get(key)
                            }
                            if (original != null) {
                                setNativeBatterySlotVisibility(controller, slot, tag, original)
                            }
                        }
                    }
            }
            // A native icon may also have been collapsed in the view tree. Reassert
            // its original measured layout after the controller has restored its slot.
            synchronized(hosts) { hosts.values.toList() }.forEach { host ->
                if (host.retired || (config.enabled && config.showWifi) || state.wifiLevel < 0) return@forEach
                host.layouts.keys.toList().filter { view ->
                    isWifiIcon(view) || (slotOf(view) ?: knownIconSlots[view])?.let(::isManagedWifiSlot) == true
                }.forEach { view ->
                    restoreHostView(host, view)
                    (view.parent as? ViewGroup)?.requestLayout()
                    view.invalidate()
                }
            }
        }
    }

    private fun setNativeBatterySlotVisibility(controller: Any, slot: String, tag: Int, visible: Boolean) {
        val methods = mutableListOf<java.lang.reflect.Method>()
        var current: Class<*>? = controller.javaClass
        while (current != null) {
            methods += current.declaredMethods.filter { method ->
                method.name == "setIconVisibility" &&
                    method.parameterTypes[0] == String::class.java &&
                    ((method.parameterTypes.size == 2 &&
                        method.parameterTypes[1] == Boolean::class.javaPrimitiveType) ||
                        (method.parameterTypes.size == 3 &&
                            ((method.parameterTypes[1] == Boolean::class.javaPrimitiveType &&
                                method.parameterTypes[2] == Int::class.javaPrimitiveType) ||
                             (method.parameterTypes[1] == Int::class.javaPrimitiveType &&
                                method.parameterTypes[2] == Boolean::class.javaPrimitiveType))))
            }
            current = current.superclass
        }
        val method = methods.firstOrNull { candidate ->
            candidate.parameterTypes.size == 2 &&
                candidate.parameterTypes[1] == Boolean::class.javaPrimitiveType
        } ?: methods.firstOrNull { candidate ->
            candidate.parameterTypes.size == 3 &&
                ((candidate.parameterTypes[1] == Boolean::class.javaPrimitiveType &&
                    candidate.parameterTypes[2] == Int::class.javaPrimitiveType) ||
                 (candidate.parameterTypes[1] == Int::class.javaPrimitiveType &&
                    candidate.parameterTypes[2] == Boolean::class.javaPrimitiveType))
        } ?: return

        val wasEnforcing = enforcingBatterySlotVisibility.get() == true
        enforcingBatterySlotVisibility.set(true)
        try {
            method.isAccessible = true
            when {
                method.parameterTypes.size == 2 -> method.invoke(controller, slot, visible)
                method.parameterTypes[1] == Boolean::class.javaPrimitiveType ->
                    method.invoke(controller, slot, visible, tag)
                else -> method.invoke(controller, slot, tag, visible)
            }
        } catch (t: Throwable) {
            diagnostic("Cannot update native battery slot visibility for $slot: $t")
        } finally {
            if (wasEnforcing) enforcingBatterySlotVisibility.set(true)
            else enforcingBatterySlotVisibility.remove()
        }
    }

    private fun readNativeBatterySlotVisibility(controller: Any, slot: String, tag: Int): Boolean? {
        val iconList = readObjectField(controller, "mStatusBarIconList", "mIconList", "statusBarIconList")
            ?: return null
        val slotObject = invokeObjectMethod(iconList, listOf("getSlot"), slot) ?: return null
        val holder = invokeObjectMethod(slotObject, listOf("getHolderForTag", "getIconHolder"), tag)
            ?: return null
        val visibleMethod = findNoArgMethod(holder.javaClass, "isVisible", "getVisible")
        val visible = visibleMethod?.let { runCatching { it.invoke(holder) as? Boolean }.getOrNull() }
        if (visible != null) return visible
        return readBooleanField(holder, "mVisible", "visible", "isVisible")
    }

    private fun readObjectField(target: Any, vararg names: String): Any? {
        var current: Class<*>? = target.javaClass
        while (current != null) {
            for (field in current.declaredFields) {
                if (!names.any { it.equals(field.name, ignoreCase = true) }) continue
                val value = runCatching { field.isAccessible = true; field.get(target) }.getOrNull()
                if (value != null) return value
            }
            current = current.superclass
        }
        return null
    }

    private fun invokeObjectMethod(target: Any, names: List<String>, argument: Any): Any? {
        var current: Class<*>? = target.javaClass
        while (current != null) {
            val method = current.declaredMethods.firstOrNull { candidate ->
                candidate.name in names && candidate.parameterTypes.size == 1 &&
                    (candidate.parameterTypes[0].isInstance(argument) ||
                        (candidate.parameterTypes[0] == Int::class.javaPrimitiveType && argument is Int))
            }
            if (method != null) {
                val result = runCatching { method.isAccessible = true; method.invoke(target, argument) }.getOrNull()
                if (result != null) return result
            }
            current = current.superclass
        }
        return null
    }

    private fun findNoArgMethod(type: Class<*>, vararg names: String): java.lang.reflect.Method? {
        var current: Class<*>? = type
        while (current != null) {
            val method = current.declaredMethods.firstOrNull {
                it.name in names && it.parameterTypes.isEmpty()
            }
            if (method != null) return runCatching { method.isAccessible = true; method }.getOrNull()
            current = current.superclass
        }
        return null
    }

    private fun readBooleanField(target: Any, vararg names: String): Boolean? {
        var current: Class<*>? = target.javaClass
        while (current != null) {
            for (field in current.declaredFields) {
                if (!names.any { it.equals(field.name, ignoreCase = true) }) continue
                val value = runCatching { field.isAccessible = true; field.get(target) as? Boolean }.getOrNull()
                if (value != null) return value
            }
            current = current.superclass
        }
        return null
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
                            if (previous.enabled != next.enabled || previous.showWifi != next.showWifi) {
                                applyBatterySlotVisibility()
                            }
                            if (previous.enabled != next.enabled) replayMiuiBatteryIslandRequests()
                            if (previous.enabled != next.enabled ||
                                previous.chargingDisplayMode != next.chargingDisplayMode ||
                                previous.showBolt != next.showBolt ||
                                previous.showValue != next.showValue ||
                                previous.valueCentred != next.valueCentred ||
                                previous.trioStyle != next.trioStyle ||
                                previous.mobileTypeMode != next.mobileTypeMode
                            ) syncChargingRotationTicker()
                            if (requiresHostRefresh(previous, next)) {
                                refreshHosts()
                            } else {
                                if (
                                    previous.glyphScalePercent != next.glyphScalePercent ||
                                    previous.glyphOffsetXDp != next.glyphOffsetXDp ||
                                    previous.glyphOffsetYDp != next.glyphOffsetYDp ||
                                    previous.glyphLeftMarginDp != next.glyphLeftMarginDp ||
                                    previous.glyphRightMarginDp != next.glyphRightMarginDp ||
                                    previous.outSignalMarginDp != next.outSignalMarginDp ||
                                    previous.outSignalSizeDp != next.outSignalSizeDp ||
                                    previous.outTypeSizeDp != next.outTypeSizeDp ||
                                    previous.typeWeight != next.typeWeight ||
                                    previous.typeSuffixScale != next.typeSuffixScale ||
                                    previous.dualSimMode != next.dualSimMode ||
                                    previous.dualSim != next.dualSim ||
                                    previous.trioStyle != next.trioStyle
                                ) resizeGlyphs()
                                invalidateGlyphs()
                            }
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
        // Samsung SystemUI reads "charger_type" from this BATTERY_CHANGED intent.
        // Types 3/4/5 map to Samsung's Super Fast charging indicator.
        val samsungChargerType = intent?.takeIf { it.hasExtra("charger_type") }
            ?.getIntExtra("charger_type", 0)
        val samsungSuperFast = samsungChargerType != null && samsungChargerType in 3..5
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val wasCharging = state.charging
        state = state.copy(
            batteryLevel = level.coerceIn(0, 100),
            charging = charging,
            quickCharge = charging && (
                samsungSuperFast || quickCharge() || (samsungChargerType == null && state.quickCharge)
            ),
            chargingAlternatePhase = if (charging) state.chargingAlternatePhase else false,
        )
        if (wasCharging != charging) syncChargingRotationTicker()
        diagnostic("Battery: level=${state.batteryLevel}, charging=${state.charging}, quick=${state.quickCharge}, chargerType=${samsungChargerType ?: -1}")
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
        listOf(
            "sys.miui.quickcharge", "persist.vendor.quickcharge.status", "ro.boot.quickcharge",
            "sys.fastcharging", "persist.sys.fastcharge", "persist.vendor.battery.fast_charge",
            "sys.super_fast_charging",
        ).any { key ->
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
            val nextLevel = level.coerceIn(-1, 4)
            val wifiLevelChanged = state.wifiLevel != nextLevel
            state = state.copy(wifiLevel = nextLevel)
            if (wifiLevelChanged) syncChargingRotationTicker(resetPhase = false)
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
        val dataSub = runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }
            .getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        defaultDataSubscriptionId = dataSub
        subscriptionSlots.clear()
        active.forEach { info -> subscriptionSlots[info.subscriptionId] = info.simSlotIndex }
        val activeIds = active.map { it.subscriptionId }.toSet()
        quickPanelSignalLevels.keys.filter { it !in activeIds }.forEach { quickPanelSignalLevels.remove(it) }
        nativeSignalLevels.keys.filter { it !in activeIds }.forEach { nativeSignalLevels.remove(it) }
        renderedSignalLevels.keys.filter { it !in activeIds }.forEach { renderedSignalLevels.remove(it) }
        lastSubscriptionSignalLevels.keys.filter { it !in activeIds }.forEach { lastSubscriptionSignalLevels.remove(it) }
        // Read the live collapsed-status-bar SignalDrawable before falling back to TelephonyManager.
        sampleModernStatusBarSignalDrawables()
        var dataLevel = -1
        var simOne = -1
        var simTwo = -1
        var dataSlot = -1
        var label = state.networkType
        active.forEach { info ->
            val sub = runCatching { base.createForSubscriptionId(info.subscriptionId) }.getOrNull() ?: return@forEach
            val telephonyLevel = runCatching { sub.signalStrength?.level ?: -1 }
                .getOrDefault(-1).coerceIn(-1, 4)
            val drawnStatusBarLevel = renderedSignalLevels[info.subscriptionId]?.coerceIn(0, 4)
            val modernPipelineLevel = readModernPipelineSignalLevel(info.subscriptionId)
            // The actual SignalDrawable level wins over both the reflected model and telephony.
            // The model remains useful as a fallback if this firmware uses a nonstandard drawable.
            when {
                drawnStatusBarLevel != null -> nativeSignalLevels[info.subscriptionId] = drawnStatusBarLevel
                modernPipelineLevel != null -> nativeSignalLevels[info.subscriptionId] = modernPipelineLevel
            }
            if (drawnStatusBarLevel != null || modernPipelineLevel != null) {
                val snapshot = "drawn=${drawnStatusBarLevel ?: -1}|model=${modernPipelineLevel ?: -1}" +
                    "|dataSub=${info.subscriptionId == dataSub}|telephony=$telephonyLevel"
                if (lastModernPipelineLevelDiagnostics.put(info.subscriptionId, snapshot) != snapshot) {
                    diagnostic("Modern status-bar signal sample: subId=${info.subscriptionId} $snapshot")
                }
            }
            val quickPanelLevel = quickPanelSignalLevels[info.subscriptionId]?.coerceIn(0, 4)
            val nativeLevel = nativeSignalLevels[info.subscriptionId]?.coerceIn(0, 4)
            // Match the collapsed status-bar icon first. Quick Settings is a fallback
            // for Samsung builds where the status-bar icon state doesn't expose a
            // decodable level; TelephonyManager comes after both SystemUI sources.
            val level = nativeLevel ?: quickPanelLevel ?: telephonyLevel.takeIf { it in 0..4 }
                ?: lastSubscriptionSignalLevels[info.subscriptionId] ?: -1
            if (level in 0..4) lastSubscriptionSignalLevels[info.subscriptionId] = level
            val embedded = if (Build.VERSION.SDK_INT >= 28) {
                runCatching { info.isEmbedded }.getOrDefault(false)
            } else false
            diagnostic(
                "SIM signal: subId=${info.subscriptionId} slot=${info.simSlotIndex} embedded=$embedded " +
                    "defaultData=${info.subscriptionId == dataSub} qs=${quickPanelLevel ?: -1} " +
                    "native=${nativeLevel ?: -1} telephony=$telephonyLevel selected=$level"
            )
            if (info.simSlotIndex == 0) simOne = level
            if (info.simSlotIndex == 1) simTwo = level
            if (info.subscriptionId == dataSub) {
                dataLevel = level
                dataSlot = info.simSlotIndex
                label = displayNetworkTypes[info.subscriptionId] ?: networkType(sub.dataNetworkType)
            }
        }
        val hasActiveDataSubscription = dataSub >= 0 && active.any { it.subscriptionId == dataSub }
        if (dataLevel < 0) {
            // Prefer this subscription's own cached state. Never reuse the prior
            // default-data SIM's level after the user switches the data subscription.
            dataLevel = if (hasActiveDataSubscription) -1 else if (simOne >= 0) simOne else simTwo
        }
        if (dataLevel < 0 && !hasActiveDataSubscription) {
            dataLevel = runCatching { base.signalStrength?.level ?: state.mobileLevel }
                .getOrDefault(state.mobileLevel)
        }
        if (dataSlot !in 0..1) {
            dataSlot = subscriptionSlots[dataSub]?.takeIf { it in 0..1 }
                ?: if (!hasActiveDataSubscription) {
                    if (simOne >= 0) 0 else if (simTwo >= 0) 1 else 0
                } else {
                    state.dataSimSlotIndex.takeIf { it in 0..1 } ?: 0
                }
        }
        val networkTypeChanged = state.networkType != label
        state = state.copy(
            mobileLevel = dataLevel.coerceIn(0, 4),
            simOneLevel = simOne,
            simTwoLevel = simTwo,
            dataSimSlotIndex = dataSlot,
            networkType = label,
        )
        if (networkTypeChanged) {
            syncChargingRotationTicker(resetPhase = false)
            resizeGlyphs()
        }
        diagnostic("Telephony refreshed: dataSub=$dataSub, dataLevel=${state.mobileLevel}, type=${state.networkType}")
    }

    private fun hookNativeMobileState(module: XposedModule, classLoader: ClassLoader) {
        val candidates = listOf(
            "com.android.systemui.statusbar.StatusBarMobileView",
            "com.android.systemui.statusbar.phone.StatusBarMobileView",
            "com.android.systemui.statusbar.views.StatusBarMobileView",
            "com.android.systemui.statusbar.phone.ModernStatusBarMobileView",
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView",
        )
        candidates.forEach { name ->
            runCatching {
                val clazz = classLoader.loadClass(name)
                clazz.declaredMethods.filter {
                    it.name in setOf("applyMobileState", "updateState") && it.parameterTypes.size == 1
                }.distinctBy { it.toGenericString() }.forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        if (!config.enabled) return@intercept result
                        val view = chain.thisObject as? View ?: return@intercept result
                        val mobileState = chain.args.firstOrNull() ?: return@intercept result
                        val strength = readSignalIntField(mobileState, "strengthId", "mStrengthId")
                            ?: return@intercept result
                        val subId = readSignalIntField(mobileState, "subId", "mSubId")
                            ?: defaultDataSubscriptionId
                        val resourceName = runCatching { view.resources.getResourceEntryName(strength) }
                            .getOrNull().orEmpty()
                        val level = normalizeNativeSignalLevel(strength)
                            ?: if (resourceName.isNotBlank()) {
                                resolveSignalResourceName(resourceName).takeIf { it in 0..4 }
                            } else {
                                // Legacy status-bar state may carry SignalDrawable's packed state,
                                // not a resource ID. Decode it only when resource lookup failed.
                                decodePackedSignalDrawableLevel(strength).takeIf { it in 0..4 }
                            }
                        diagnostic(
                            "StatusBarMobileView state: subId=$subId strengthId=$strength " +
                                "resource=$resourceName level=$level"
                        )
                        if (level != null && level in 0..4 && subId >= 0) updateNativeSignalLevel(subId, level)
                        result
                    }
                }
            }.onFailure { diagnostic("Native mobile state hook unavailable: $name: $it") }
        }
    }

    /**
     * Read each SIM's modern status-bar model and map the returned view to its subscription.
     * OEM Android revisions vary the logger/slot parameter order and may expose a wrapper view
     * model, so identify the model by type/name and subscription ID instead of a fixed index.
     */
    private fun hookModernMobileViewConstruction(module: XposedModule, classLoader: ClassLoader) {
        val className = "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
        runCatching {
            val clazz = classLoader.loadClass(className)
            modernMobileViewClass = clazz
            val methods = clazz.declaredMethods.filter {
                it.name == "constructAndBind" && it.parameterTypes.any { type ->
                    type.name.contains("MobileIconViewModel") ||
                        type.name.contains("LocationBasedMobileViewModel") ||
                        type.name.contains("MobileIconViewModelCommon")
                }
            }
            if (methods.isEmpty()) {
                diagnostic("Modern mobile pipeline hook unavailable: no constructAndBind method with view-model parameter")
                return@runCatching
            }
            methods.distinctBy { it.toGenericString() }.forEach { method ->
                module.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    val viewModel = chain.args.firstNotNullOfOrNull { argument ->
                        val candidate = argument ?: return@firstNotNullOfOrNull null
                        val typeName = candidate.javaClass.name
                        val modelType = typeName.contains("MobileIconViewModel") ||
                            typeName.contains("LocationBasedMobileViewModel")
                        val subId = readSignalIntValue(candidate, "subscriptionId", "subId")
                        if (subId != null && subId >= 0 &&
                            (modelType || readObjectMember(candidate, "icon", "signalIcon", "signalLevelIcon") != null)
                        ) candidate else null
                    }
                    if (viewModel != null) {
                        val subId = readSignalIntValue(viewModel, "subscriptionId", "subId")
                        if (subId != null && subId >= 0) {
                            // Keep the collapsed status-bar HOME model as the canonical
                            // signal source; later QS/Keyguard view construction must not
                            // replace it for the same subscription.
                            val modelSimpleName = viewModel.javaClass.simpleName
                            if (modelSimpleName.contains("HomeMobileIconViewModel")) {
                                modernMobileViewModels[subId] = WeakReference(viewModel)
                            }
                            val resultView = result as? View
                            if (resultView != null) {
                                modernMobileViewSubscriptionIds[resultView] = subId
                                knownIconSlots[resultView] = "mobile"
                            }
                            val iconModel = readModernIconModel(viewModel)
                            val level = iconModel?.let(::modernPipelineSignalLevel)
                            val levels = iconModel?.let {
                                readSignalIntValue(it, "numberOfLevels", "numLevels", "levelCount")
                            } ?: -1
                            val inspect = if (iconModel == null) "|inspect=${describeModernSignalModel(viewModel)}" else ""
                            val snapshot = "viewModel=${viewModel.javaClass.name}|iconModel=${iconModel?.javaClass?.name}" +
                                "|level=${level ?: -1}|levels=$levels|drawn=${renderedSignalLevels[subId] ?: -1}$inspect"
                            if (lastModernPipelineSignalDiagnostics.put(subId, snapshot) != snapshot) {
                                diagnostic("Modern status-bar mobile pipeline: subId=$subId $snapshot")
                            }
                            if (level != null && renderedSignalLevels[subId] == null) {
                                nativeSignalLevels[subId] = level
                            } else if (level == null) {
                                diagnostic("Modern status-bar mobile pipeline: current icon level unavailable for subId=$subId")
                            }
                            scheduleTelephonyRefresh()
                            // Binding can finish before the mobile view is attached; sample again on main.
                            main.post { sampleModernStatusBarSignalDrawables() }
                        }
                    } else {
                        val signature = chain.args.mapIndexed { index, argument ->
                            "$index:${argument?.javaClass?.name ?: "null"}"
                        }.joinToString(",")
                        diagnostic("Modern mobile pipeline view-model parameter not identified: $signature")
                    }
                    result
                }
            }
            diagnostic("Modern mobile pipeline hook installed: $className")
        }.onFailure { diagnostic("Modern mobile pipeline hook unavailable: $it") }
    }

    /**
     * Observe the SignalDrawable attached to the real status-bar mobile ImageView. This is
     * intentionally per-view-model/subscription; it never samples QS icons or the other SIM.
     */
    /**
     * Keep resource identities attached to drawable instances. Samsung One UI uses
     * VectorDrawable resources for mobile_signal, unlike AOSP SignalDrawable.
     */
    private fun hookVectorSignalResourceOrigins(module: XposedModule) {
        var hookedResourceMethods = 0
        Resources::class.java.methods.filter { method ->
            method.name in setOf("getDrawable", "getDrawableForDensity") &&
                method.parameterTypes.firstOrNull() == Int::class.javaPrimitiveType &&
                Drawable::class.java.isAssignableFrom(method.returnType)
        }.distinctBy { it.toGenericString() }.forEach { method ->
            runCatching {
                module.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    val resources = chain.thisObject as? Resources
                    val resourceId = chain.args.firstOrNull() as? Int
                    val drawable = result as? Drawable
                    if (resources != null && resourceId != null && drawable != null) {
                        rememberDrawableOrigin(resources, resourceId, drawable)
                    }
                    result
                }
                hookedResourceMethods++
            }.onFailure { diagnostic("Signal resource origin hook unavailable: ${method.name}: $it") }
        }

        // Icon.loadDrawable() is a common SystemUI path. It exposes the original Icon
        // resource even when the ImageView only receives a VectorDrawable instance.
        runCatching {
            val iconClass = android.graphics.drawable.Icon::class.java
            val methods = iconClass.methods.filter { method ->
                method.name.startsWith("loadDrawable") &&
                    method.parameterTypes.firstOrNull()?.let { Context::class.java.isAssignableFrom(it) } == true &&
                    Drawable::class.java.isAssignableFrom(method.returnType)
            }.distinctBy { it.toGenericString() }
            methods.forEach { method ->
                module.hook(method).intercept { chain ->
                    val result = chain.proceed()
                    val icon = chain.thisObject
                    val drawable = result as? Drawable
                    val context = chain.args.firstOrNull() as? Context
                    val resourceId = readSignalIntValue(icon, "resId", "mInt1", "int1")
                    if (drawable != null && context != null && resourceId != null && resourceId != 0) {
                        rememberDrawableOrigin(context.resources, resourceId, drawable)
                    }
                    result
                }
            }
            diagnostic("Signal resource origin hooks installed: Resources=$hookedResourceMethods Icon.loadDrawable=${methods.size}")
        }.onFailure {
            diagnostic("Signal Icon.loadDrawable origin hook unavailable: $it; Resources=$hookedResourceMethods")
        }
    }

    private fun rememberDrawableOrigin(resources: Resources, resourceId: Int, drawable: Drawable) {
        val name = runCatching { resources.getResourceEntryName(resourceId) }.getOrNull() ?: return
        val normalized = name.lowercase()
        // Keep the cache focused on likely status signal resources, not every drawable
        // loaded by SystemUI. We can resolve the named level later against the live view.
        if (!normalized.contains("signal") || normalized.contains("wifi") ||
            normalized.contains("wlan") || normalized.contains("bluetooth")
        ) return
        val pkg = runCatching { resources.getResourcePackageName(resourceId) }.getOrDefault("unknown")
        val origin = NativeDrawableOrigin(resourceId, pkg, name)
        drawableOrigins[drawable] = origin
        drawable.constantState?.let { drawableStateOrigins[it] = origin }
    }

    private fun drawableOrigin(drawable: Drawable): NativeDrawableOrigin? =
        drawableOrigins[drawable] ?: drawable.constantState?.let { drawableStateOrigins[it] }

    private fun hookModernSignalDrawable(module: XposedModule) {
        runCatching {
            val setImageLevel = android.widget.ImageView::class.java.getDeclaredMethod(
                "setImageLevel", Int::class.javaPrimitiveType!!,
            )
            module.hook(setImageLevel).intercept { chain ->
                val result = chain.proceed()
                val image = chain.thisObject as? android.widget.ImageView
                val packedLevel = chain.args.getOrNull(0) as? Int
                if (image != null && packedLevel != null && config.enabled) {
                    captureModernSignalDrawable(image, packedLevel)
                }
                result
            }
        }.onFailure { diagnostic("Modern status-bar signal image-level hook unavailable: $it") }

        runCatching {
            val setImageDrawable = android.widget.ImageView::class.java.getDeclaredMethod(
                "setImageDrawable", Drawable::class.java,
            )
            module.hook(setImageDrawable).intercept { chain ->
                val result = chain.proceed()
                val image = chain.thisObject as? android.widget.ImageView
                if (image != null && config.enabled) captureModernMobileSignalImage(image)
                result
            }
        }.onFailure { diagnostic("Modern status-bar signal drawable hook unavailable: $it") }

        runCatching {
            val setImageResource = android.widget.ImageView::class.java.getDeclaredMethod(
                "setImageResource", Int::class.javaPrimitiveType!!,
            )
            module.hook(setImageResource).intercept { chain ->
                val result = chain.proceed()
                val image = chain.thisObject as? android.widget.ImageView
                val resourceId = chain.args.getOrNull(0) as? Int
                if (image != null && resourceId != null && config.enabled) {
                    val root = findModernMobileRoot(image)
                    val subId = root?.let { modernMobileViewSubscriptionIds[it] }
                    if (root != null && subId != null && isMobileSignalImage(image)) {
                        val name = runCatching { image.resources.getResourceEntryName(resourceId) }
                            .getOrNull().orEmpty()
                        val level = resolveSignalResourceName(name)
                        if (level in 0..4) {
                            recordModernStatusBarSignal(
                                subId, level, "image-resource",
                                "resource=$name id=$resourceId image=${resourceName(image)}",
                            )
                        } else {
                            diagnoseVectorSignalOrigin(
                                image, subId, "image-resource=$name id=$resourceId level=$level",
                            )
                        }
                    }
                }
                result
            }
        }.onFailure { diagnostic("Modern status-bar signal image-resource hook unavailable: $it") }

        runCatching {
            // Some status-bar implementations update Drawable.level directly after binding.
            val setDrawableLevel = Drawable::class.java.getDeclaredMethod(
                "setLevel", Int::class.javaPrimitiveType!!,
            )
            module.hook(setDrawableLevel).intercept { chain ->
                val result = chain.proceed()
                val drawable = chain.thisObject as? Drawable
                val image = drawable?.callback as? android.widget.ImageView
                if (image != null && config.enabled &&
                    drawable.javaClass.name.contains("SignalDrawable", ignoreCase = true)
                ) {
                    captureModernSignalDrawable(image, drawable.level)
                }
                result
            }
        }.onFailure { diagnostic("Modern status-bar drawable-level hook unavailable: $it") }
        diagnostic("Modern status-bar rendered signal observer installed")
    }

    private fun isMobileSignalImage(image: android.widget.ImageView): Boolean =
        resourceName(image).equals("mobile_signal", ignoreCase = true) ||
            resourceName(image).equals("signal_strength", ignoreCase = true)

    private fun findModernMobileRoot(image: View): View? {
        val mobileClass = modernMobileViewClass ?: return null
        var cursor: View? = image
        while (cursor != null) {
            if (mobileClass.isInstance(cursor)) return cursor
            cursor = cursor.parent as? View
        }
        return null
    }

    private fun captureModernMobileSignalImage(image: android.widget.ImageView) {
        val drawable = image.drawable ?: return
        if (drawable.javaClass.name.contains("SignalDrawable", ignoreCase = true)) {
            captureModernSignalDrawable(image, drawable.level)
            return
        }
        if (!isMobileSignalImage(image)) return
        val root = findModernMobileRoot(image) ?: return
        if (hostFor(root) == null) return
        val subId = modernMobileViewSubscriptionIds[root] ?: return
        val origin = drawableOrigin(drawable)
        val level = origin?.let { resolveSignalResourceName(it.resourceName) } ?: -1
        if (level in 0..4) {
            recordModernStatusBarSignal(
                subId, level, "vector-resource",
                "resource=${origin!!.resourcePackage}:${origin.resourceName} id=${origin.resourceId} drawable=${drawable.javaClass.name}",
            )
        } else {
            diagnoseVectorSignalOrigin(
                image, subId,
                "drawable=${drawable.javaClass.name}|origin=${origin?.resourcePackage ?: "unknown"}:${origin?.resourceName ?: "unknown"}",
            )
        }
    }

    private fun diagnoseVectorSignalOrigin(image: android.widget.ImageView, subId: Int, details: String) {
        val key = "vector-origin:$subId:${System.identityHashCode(image)}"
        val snapshot = "subId=$subId|image=${resourceName(image)}|$details"
        if (lastModernSignalSamplingDiagnostics.put(key, snapshot) != snapshot) {
            diagnostic("Modern status-bar signal vector origin unresolved: $snapshot")
        }
    }

    private fun recordModernStatusBarSignal(subId: Int, level: Int, source: String, details: String) {
        if (subId < 0 || level !in 0..4) return
        val old = renderedSignalLevels.put(subId, level)
        nativeSignalLevels[subId] = level
        val key = "captured:$subId"
        val snapshot = "level=$level|source=$source|$details"
        if (old != level || lastModernSignalSamplingDiagnostics.put(key, snapshot) != snapshot) {
            lastModernSignalSamplingDiagnostics[key] = snapshot
            diagnostic(
                "Modern status-bar drawn signal: subId=$subId level=$level source=$source " +
                    "$details defaultData=${subId == defaultDataSubscriptionId}"
            )
            scheduleTelephonyRefresh()
            invalidateGlyphs()
        }
    }

    private fun captureModernSignalDrawable(image: android.widget.ImageView, packedLevel: Int?) {
        val drawable = image.drawable ?: return
        if (!drawable.javaClass.name.contains("SignalDrawable", ignoreCase = true)) return
        val root = findModernMobileRoot(image) ?: return
        // Ignore demo/keyguard/Quick Settings views: only sample the registered collapsed host.
        if (hostFor(root) == null) return
        val subId = modernMobileViewSubscriptionIds[root] ?: return
        val fullState = packedLevel ?: drawable.level
        val level = decodePackedSignalDrawableLevel(fullState)
        if (level !in 0..4) return
        recordModernStatusBarSignal(
            subId, level, "signal-drawable",
            "packed=$fullState drawable=${drawable.javaClass.name} image=${resourceName(image)}",
        )
    }

    /**
     * Sample the actual SignalDrawable instances hosted by the collapsed status bar.
     * Samsung's mobile view-model exposes a cold ChannelFlowTransformLatest rather than
     * a directly readable StateFlow, so reading the model property alone cannot return
     * its latest emission. The attached native drawable remains the authoritative state.
     */
    private fun sampleModernStatusBarSignalDrawables() {
        val mobileClass = modernMobileViewClass ?: return
        val hostSnapshot = synchronized(hosts) { hosts.values.filter { !it.retired }.toList() }
        if (hostSnapshot.isEmpty()) return
        var anyMobileView = false
        var anySignalDrawable = false
        hostSnapshot.forEach { host ->
            val mobileViews = descendants(host.root).filter { mobileClass.isInstance(it) }
            if (mobileViews.isNotEmpty()) anyMobileView = true
            mobileViews.forEach { mobileRoot ->
                val subId = modernMobileViewSubscriptionIds[mobileRoot]
                if (subId == null || subId < 0) {
                    val key = "unmapped:${System.identityHashCode(mobileRoot)}"
                    val snapshot = "root=${mobileRoot.javaClass.name}|attached=${mobileRoot.isAttachedToWindow}|" +
                        "children=${(mobileRoot as? ViewGroup)?.childCount ?: 0}"
                    if (lastModernSignalSamplingDiagnostics.put(key, snapshot) != snapshot) {
                        diagnostic("Modern status-bar signal live sample unmapped: $snapshot")
                    }
                    return@forEach
                }
                val imageViews = descendants(mobileRoot).filterIsInstance<android.widget.ImageView>()
                val signals = imageViews.mapNotNull { image ->
                    val drawable = image.drawable ?: return@mapNotNull null
                    if (drawable.javaClass.name.contains("SignalDrawable", ignoreCase = true)) {
                        val decoded = decodePackedSignalDrawableLevel(drawable.level)
                        if (decoded in 0..4) {
                            Triple(image, drawable, decoded to ("signal-drawable packed=${drawable.level}"))
                        } else null
                    } else if (isMobileSignalImage(image)) {
                        val origin = drawableOrigin(drawable) ?: return@mapNotNull null
                        val decoded = resolveSignalResourceName(origin.resourceName)
                        if (decoded in 0..4) {
                            Triple(
                                image, drawable,
                                decoded to ("vector-resource ${origin.resourcePackage}:${origin.resourceName} id=${origin.resourceId}"),
                            )
                        } else null
                    } else null
                }
                if (signals.isEmpty()) {
                    val key = "no-drawable:${System.identityHashCode(mobileRoot)}"
                    val details = imageViews.take(12).joinToString(",") { image ->
                        val id = resourceName(image)
                        "${image.javaClass.simpleName}:${id.ifBlank { "no-id" }}:${image.drawable?.javaClass?.simpleName ?: "no-drawable"}"
                    }
                    val snapshot = "subId=$subId|root=${mobileRoot.javaClass.simpleName}|" +
                        "attached=${mobileRoot.isAttachedToWindow}|images=${imageViews.size}|$details"
                    if (lastModernSignalSamplingDiagnostics.put(key, snapshot) != snapshot) {
                        diagnostic("Modern status-bar signal live sample has no SignalDrawable: $snapshot")
                    }
                    return@forEach
                }
                anySignalDrawable = true
                signals.forEach { (image, drawable, decoded) ->
                    recordModernStatusBarSignal(
                        subId,
                        decoded.first,
                        "live-view-tree",
                        "${decoded.second}|drawable=${drawable.javaClass.name}|image=${resourceName(image)}",
                    )
                    lastModernSignalSamplingDiagnostics.remove("no-drawable:${System.identityHashCode(mobileRoot)}")
                    lastModernSignalSamplingDiagnostics.remove("undecoded:${System.identityHashCode(image)}")
                }
            }
        }
        val summary = "hosts=${hostSnapshot.size}|mobileViews=${if (anyMobileView) "present" else "none"}|" +
            "signalDrawables=${if (anySignalDrawable) "present" else "none"}"
        if (lastModernSignalSamplingDiagnostics.put("summary", summary) != summary) {
            diagnostic("Modern status-bar signal live sampling: $summary")
        }
    }
    private fun readModernIconModel(viewModel: Any): Any? {
        // Samsung One UI may wrap the common mobile model more than once. Walk only
        // known delegation links instead of relying on a single commonImpl property.
        val wrapperMembers = listOf(
            "commonImpl", "delegate", "delegateImpl", "mobileIconViewModel",
            "iconViewModel", "viewModel", "model", "implementation", "impl",
            "mobileIconViewModelCommon", "common", "wrapped", "wrappedModel",
        )
        val iconMembers = listOf("iconId", "signalIconId", "icon", "signalIcon", "signalLevelIcon")
        val modelMembers = listOf("iconModel", "signalIconModel", "signalStrengthModel")
        val pending = java.util.ArrayDeque<Pair<Any, Int>>()
        val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Any, Boolean>())
        pending.addLast(viewModel to 0)
        while (pending.isNotEmpty()) {
            val (candidate, depth) = pending.removeFirst()
            if (!visited.add(candidate)) continue
            if (candidate is Number) return candidate

            for (member in iconMembers) {
                val icon = readObjectMember(candidate, member) ?: continue
                if (icon is Number) return icon
                readFlowSnapshot(icon)?.let { return it }
                if (readSignalIntValue(icon, "level", "signalLevel", "strengthLevel", "resId", "resourceId") != null) {
                    return icon
                }
            }
            for (member in modelMembers) {
                val model = readObjectMember(candidate, member) ?: continue
                if (model is Number) return model
                readFlowSnapshot(model)?.let { return it }
                if (readSignalIntValue(model, "level", "signalLevel", "strengthLevel", "resId", "resourceId") != null) {
                    return model
                }
            }

            if (depth >= 3) continue
            wrapperMembers.forEach { member ->
                val nested = readObjectMember(candidate, member) ?: return@forEach
                if (nested !== candidate && nested !is String && nested !is Number &&
                    nested !is Boolean && nested !is Char
                ) {
                    pending.addLast(nested to depth + 1)
                }
            }
        }
        return null
    }

    /** Read the latest value from StateFlow/SharedFlow even when its getter is inherited. */
    private fun readFlowSnapshot(flow: Any): Any? {
        if (flow is Number) return flow
        invokeZeroArgumentMember(flow, "value", "getValue")?.let { return it }

        val rawValue = readObjectField(flow, "value")
        when (rawValue) {
            is Number, is String, is Boolean, is Char -> return rawValue
            null -> Unit
            else -> {
                if (rawValue.javaClass.name.contains("Atomic")) {
                    invokeZeroArgumentMember(rawValue, "get")?.let { return it }
                }
            }
        }

        val replay = (invokeZeroArgumentMember(flow, "replayCache", "getReplayCache")
            ?: readObjectMember(flow, "replayCache")) as? List<*>
        return replay?.lastOrNull()
    }

    private fun invokeZeroArgumentMember(target: Any, vararg names: String): Any? {
        fun matches(methodName: String): Boolean = names.any { name ->
            methodName.equals(name, ignoreCase = true) ||
                methodName == "get" + name.replaceFirstChar { it.uppercase() }
        }
        target.javaClass.methods.firstOrNull { it.parameterTypes.isEmpty() && matches(it.name) }?.let { method ->
            runCatching { method.isAccessible = true; method.invoke(target) }.getOrNull()?.let { return it }
        }
        var type: Class<*>? = target.javaClass
        while (type != null) {
            type.declaredMethods.firstOrNull { it.parameterTypes.isEmpty() && matches(it.name) }?.let { method ->
                runCatching { method.isAccessible = true; method.invoke(target) }.getOrNull()?.let { return it }
            }
            type = type.superclass
        }
        return null
    }

    private fun describeModernSignalModel(viewModel: Any): String {
        val wrapperMembers = listOf(
            "commonImpl", "delegate", "delegateImpl", "mobileIconViewModel",
            "iconViewModel", "viewModel", "model", "implementation", "impl",
            "mobileIconViewModelCommon", "common", "wrapped", "wrappedModel",
        )
        val iconMembers = listOf("iconId", "signalIconId", "icon", "signalIcon", "signalLevelIcon")
        val pending = java.util.ArrayDeque<Pair<Any, Int>>()
        val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Any, Boolean>())
        val descriptions = mutableListOf<String>()
        pending.addLast(viewModel to 0)
        while (pending.isNotEmpty() && descriptions.size < 6) {
            val (candidate, depth) = pending.removeFirst()
            if (!visited.add(candidate)) continue
            val members = mutableListOf<String>()
            iconMembers.forEach { name ->
                val icon = readObjectMember(candidate, name) ?: return@forEach
                val value = readFlowSnapshot(icon)
                val detail = when (value) {
                    null -> ""
                    is Number, is String, is Boolean, is Char -> "=${value}"
                    else -> "->${value.javaClass.simpleName}"
                }
                members += "$name:${icon.javaClass.simpleName}$detail"
            }
            descriptions += candidate.javaClass.simpleName +
                (if (members.isEmpty()) "" else "[" + members.joinToString(",") + "]")
            if (depth >= 2) continue
            wrapperMembers.forEach { name ->
                val nested = readObjectMember(candidate, name) ?: return@forEach
                if (nested !== candidate && nested !is String && nested !is Number &&
                    nested !is Boolean && nested !is Char
                ) pending.addLast(nested to depth + 1)
            }
        }
        return descriptions.joinToString(";").take(700)
    }
    /**
     * Return true/false only when Android's network tracker has a usable answer.
     * WifiManager RSSI may be -127 during a transient modern-pipeline update, even while
     * the native status-bar Wi-Fi view was visibly connected.
     */
    private fun isWifiTransportConnected(context: Context): Boolean? {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager ?: return null
        val networks = runCatching { connectivity.allNetworks }.getOrNull() ?: return null
        var queriedAny = false
        if (networks.any { network ->
                val capabilities = runCatching {
                    connectivity.getNetworkCapabilities(network)
                }.getOrNull()
                if (capabilities != null) queriedAny = true
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        ) return true
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val info = runCatching { wifi?.connectionInfo }.getOrNull()
        if (wifi?.isWifiEnabled == true && info != null &&
            info.networkId >= 0 && info.rssi != -127
        ) return true
        if (wifi?.isWifiEnabled == true) {
            // Radio enabled but NetworkCapabilities/RSSI does not yet reveal whether the
            // connection is ready: do not call this definitively disconnected.
            return null
        }
        if (wifi?.isWifiEnabled == false) return false
        return if (queriedAny) false else null
    }

    private fun readModernWifiModelVisibility(view: View): Boolean? {
        val model = modernWifiViewModels[view]?.get() ?: return null
        val flow = readObjectMember(model, "wifiIcon", "wifiIconState", "icon")
            ?: return null
        val icon = readObjectMember(flow, "value") ?: return null
        val name = icon.javaClass.name.lowercase()
        return when {
            name.contains("invisible") || name.contains("hidden") || name.contains("notvisible") -> false
            name.contains("visible") -> true
            else -> null
        }
    }

    private fun hookModernWifiVisibility(module: XposedModule, classLoader: ClassLoader) {
        val className = "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView"
        runCatching {
            modernWifiViewClass = classLoader.loadClass(className)
            // Resolve the most specific public method so an OEM override on the modern
            // Wi-Fi view/parent cannot bypass a hook attached only to android.view.View.
            val method = runCatching {
                modernWifiViewClass!!.getMethod("setVisibility", Int::class.javaPrimitiveType!!)
            }.getOrElse {
                View::class.java.getDeclaredMethod(
                    "setVisibility",
                    Int::class.javaPrimitiveType!!,
                )
            }
            module.hook(method).intercept { chain ->
                val view = chain.thisObject as? View
                val wifiClass = modernWifiViewClass
                // ModernStatusBarWifiView is also used by demo/secondary SystemUI
                // icon hosts. Only modify the instance inside the PhoneStatusBarView
                // host that OneuiXP has explicitly registered for the collapsed bar.
                val host = view?.let(::hostFor)
                if (view != null && wifiClass != null && wifiClass.isInstance(view) &&
                    config.enabled && host != null
                ) {
                    val args = chain.args.toTypedArray()
                    val requested = args.getOrNull(0) as? Int ?: View.VISIBLE
                    if (!nativeWifiBaselineVisibility.containsKey(view)) {
                        // The incoming argument may be a stale asynchronous Binder request.
                        // Recover the pre-Combo visibility snapshot first; only then fall back to
                        // the current pre-call view state. Never treat the first requested GONE
                        // value itself as proof that the native slot was originally hidden.
                        val savedVisibility = host.original[view] ?: host.layouts[view]?.visibility
                        nativeWifiBaselineVisibility[view] = savedVisibility ?: view.visibility
                    }
                    val nativeWasVisible = nativeWifiBaselineVisibility[view] == View.VISIBLE
                    val modelVisible = readModernWifiModelVisibility(view)
                    val wifiConnected = isWifiTransportConnected(view.context)
                    val desiredVisible = modelVisible
                        ?: (state.wifiLevel >= 0 || wifiConnected == true)
                    val forced = when {
                        // The combined glyph owns Wi-Fi drawing; prevent a later binder update
                        // from making this collapsed status-bar instance flash over it.
                        config.showWifi -> View.GONE
                        // When the arc is disabled, follow this exact view-model's Visible/Hidden
                        // state. Network APIs are fallback-only on Samsung Android 17 because
                        // WifiManager RSSI/connectionInfo may be temporarily unavailable.
                        nativeWasVisible && desiredVisible -> View.VISIBLE
                        else -> requested
                    }
                    val connectedLabel = wifiConnected?.toString() ?: "unknown"
                    val modelLabel = modelVisible?.toString() ?: "unknown"
                    val snapshot = "$requested->$forced|showWifi=${config.showWifi}|" +
                        "wifiLevel=${state.wifiLevel}|wifiConnected=$connectedLabel|modelVisible=$modelLabel"
                    if (lastNativeWifiVisibilityRequests.put(view, snapshot) != snapshot) {
                        diagnostic(
                            "Modern Wi-Fi visibility decision: requested=$requested forced=$forced " +
                                "baseline=${nativeWifiBaselineVisibility[view] ?: -1} " +
                                "showWifi=${config.showWifi} wifiLevel=${state.wifiLevel} " +
                                "wifiConnected=$connectedLabel modelVisible=$modelLabel"
                        )
                    }
                    if (forced != requested) {
                        args[0] = forced
                        chain.proceed(args)
                    } else {
                        chain.proceed()
                    }
                } else {
                    chain.proceed()
                }
            }
            // Observe the per-view model without changing visibility here. SystemUI also
            // constructs ModernStatusBarWifiView for demo/secondary surfaces, so actual
            // hiding remains gated by hostFor(view) in the setter above.
            val constructMethods = modernWifiViewClass?.declaredMethods.orEmpty().filter {
                it.name == "constructAndBind" && java.lang.reflect.Modifier.isStatic(it.modifiers)
            }
            constructMethods.distinctBy { it.toGenericString() }.forEach { constructMethod ->
                module.hook(constructMethod).intercept { chain ->
                    val result = chain.proceed()
                    val view = result as? View
                    val model = chain.args.firstNotNullOfOrNull { argument ->
                        val candidate = argument ?: return@firstNotNullOfOrNull null
                        val typeName = candidate.javaClass.name
                        if ((typeName.contains("LocationBasedWifiViewModel") ||
                                readObjectMember(candidate, "wifiIcon", "wifiIconState") != null) &&
                            readObjectMember(candidate, "wifiIcon", "wifiIconState") != null
                        ) candidate else null
                    }
                    if (view != null && model != null) {
                        modernWifiViewModels[view] = WeakReference(model)
                        diagnostic("Modern Wi-Fi model mapped: view=${view.javaClass.name} model=${model.javaClass.name}")
                    }
                    result
                }
            }
            diagnostic(
                "Modern Wi-Fi visibility guard installed: $className; " +
                    "setterOwner=${method.declaringClass.name}; scope=registered-host-only; " +
                    "modelHooks=${constructMethods.size}"
            )
        }.onFailure { diagnostic("Modern Wi-Fi visibility guard unavailable: $it") }
    }

    private fun readModernPipelineSignalLevel(subscriptionId: Int): Int? {
        val viewModel = modernMobileViewModels[subscriptionId]?.get() ?: return null
        val iconModel = readModernIconModel(viewModel) ?: return null
        return modernPipelineSignalLevel(iconModel)
    }

    private fun modernPipelineSignalLevel(iconModel: Any): Int? {
        // Legacy Android/Samsung models expose SignalDrawable's packed state as an Int,
        // not as a model with "level" and "numberOfLevels" fields.
        if (iconModel is Number) {
            val raw = iconModel.toInt()
            val resources = synchronized(hosts) { hosts.values.firstOrNull()?.root?.resources }
            val resourceName = resources?.let {
                runCatching { it.getResourceEntryName(raw) }.getOrNull()
            }
            if (resourceName != null) {
                // Numeric models can contain either resource IDs or packed states.
                // If Android resolves it as a resource, never decode that ID as bit fields.
                return resolveSignalResourceName(resourceName).takeIf { it in 0..4 }
            }
            val decoded = decodePackedSignalDrawableLevel(raw)
            if (decoded in 0..4) return decoded
        }
        val rawLevel = readSignalIntValue(iconModel, "level", "signalLevel", "strengthLevel")
        if (rawLevel != null) {
            val numberOfLevels = readSignalIntValue(
                iconModel, "numberOfLevels", "numLevels", "levelCount", "levels"
            ) ?: 5
            if (numberOfLevels > 1 && rawLevel in 0..numberOfLevels) {
                return (rawLevel.toFloat() * 4f / (numberOfLevels - 1).toFloat())
                    .roundToInt().coerceIn(0, 4)
            }
        }
        val resource = readSignalIntValue(iconModel, "resId", "resourceId", "iconId", "icon")
            ?: return null
        val resources = synchronized(hosts) { hosts.values.firstOrNull()?.root?.resources } ?: return null
        // A resolved resource ID must be interpreted by its name. Do not treat an
        // unrelated drawable resource ID as a packed SignalDrawable state; that can
        // leave the rendered signal stuck at a false level.
        val resourceName = runCatching { resources.getResourceEntryName(resource) }.getOrNull()
        return if (resourceName != null) {
            resolveSignalResourceName(resourceName).takeIf { it in 0..4 }
        } else {
            decodePackedSignalDrawableLevel(resource).takeIf { it in 0..4 }
        }
    }

    private fun readSignalIntValue(target: Any, vararg names: String): Int? {
        readSignalIntField(target, *names)?.let { return it }
        var type: Class<*>? = target.javaClass
        while (type != null) {
            for (name in names) {
                val cap = name.replaceFirstChar { it.uppercase() }
                val method = type.declaredMethods.firstOrNull {
                    it.parameterTypes.isEmpty() &&
                        (it.name.equals(name, true) || it.name == "get$cap")
                } ?: continue
                val value = runCatching {
                    method.isAccessible = true
                    method.invoke(target)
                }.getOrNull()
                if (value is Number) return value.toInt()
            }
            type = type.superclass
        }
        return null
    }

    private fun readObjectMember(target: Any, vararg names: String): Any? {
        readObjectField(target, *names)?.let { return it }
        var type: Class<*>? = target.javaClass
        while (type != null) {
            for (name in names) {
                val cap = name.replaceFirstChar { it.uppercase() }
                val method = type.declaredMethods.firstOrNull {
                    it.parameterTypes.isEmpty() &&
                        (it.name.equals(name, true) || it.name == "get$cap")
                } ?: continue
                val value = runCatching {
                    method.isAccessible = true
                    method.invoke(target)
                }.getOrNull()
                if (value != null) return value
            }
            type = type.superclass
        }
        return null
    }

    private fun hookNativeSignalPolicy(module: XposedModule, classLoader: ClassLoader) {
        val candidates = listOf(
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicyImpl",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicyKt",
        )
        candidates.forEach { name ->
            runCatching {
                val clazz = classLoader.loadClass(name)
                clazz.declaredMethods.filter {
                    it.name == "setMobileDataIndicators" || it.name == "setMobileIcons"
                }.distinctBy { it.toGenericString() }.forEach { method ->
                    module.hook(method).intercept { chain ->
                        val result = chain.proceed()
                        if (config.enabled) chain.args.take(8).forEach(::extractNativeMobileStates)
                        result
                    }
                }
            }.onFailure { diagnostic("Native signal policy hook unavailable: $name: $it") }
        }
    }

    private fun extractNativeMobileStates(value: Any?) {
        when (value) {
            null -> return
            is Iterable<*> -> { value.take(8).forEach(::extractNativeMobileStates); return }
            is Array<*> -> { value.take(8).forEach(::extractNativeMobileStates); return }
        }
        val simpleName = value.javaClass.simpleName.lowercase()
        val resources = synchronized(hosts) { hosts.values.firstOrNull()?.root?.resources } ?: return

        // NetworkController's MobileDataIndicators carries separate status-bar and
        // Quick Settings icon states keyed by subscription ID. Read both so each SIM
        // keeps its own SystemUI-computed level, including eSIM + physical SIM pairs.
        if (simpleName.contains("mobiledataindicators")) {
            val subId = readSignalIntField(value, "subId", "subscriptionId", "mSubId", "mSubscriptionId")
                ?: return
            val qsIcon = readObjectField(value, "qsIcon", "mQsIcon")
            val statusIcon = readObjectField(value, "statusIcon", "mStatusIcon")
            val qsLevel = signalLevelFromIconState(qsIcon, resources)
            val statusLevel = signalLevelFromIconState(statusIcon, resources)
            val qsName = iconResourceName(qsIcon, resources)
            val statusName = iconResourceName(statusIcon, resources)
            diagnostic(
                "SystemUI MobileDataIndicators: subId=$subId " +
                    "statusIcon=${iconValue(statusIcon)}:$statusName level=$statusLevel " +
                    "qsIcon=${iconValue(qsIcon)}:$qsName level=$qsLevel"
            )
            if (qsLevel in 0..4) {
                quickPanelSignalLevels[subId] = qsLevel
            } else {
                // A null/unsupported QS icon means this subscription has no current
                // Quick Settings signal state. Do not keep an old data-SIM level.
                quickPanelSignalLevels.remove(subId)
            }
            if (statusLevel in 0..4) updateNativeSignalLevel(subId, statusLevel)
            scheduleTelephonyRefresh()
            return
        }

        if (!simpleName.contains("mobileiconstate") &&
            !simpleName.contains("mobileicon") &&
            !simpleName.contains("iconstate")
        ) return
        val subId = readSignalIntField(value, "subId", "subscriptionId", "mSubId", "mSubscriptionId")
            ?: return
        val strength = readSignalIntField(value, "strengthId", "mStrengthId", "icon", "mIcon")
            ?: return
        val resourceName = runCatching { resources.getResourceEntryName(strength) }.getOrNull().orEmpty()
        val level = normalizeNativeSignalLevel(strength)
            ?: if (resourceName.isNotBlank()) {
                resolveSignalResourceName(resourceName).takeIf { it in 0..4 }
            } else {
                // Only decode as packed bits when this integer is not a real resource ID.
                decodePackedSignalDrawableLevel(strength).takeIf { it in 0..4 }
            }
        diagnostic(
            "Native mobile icon state: class=${value.javaClass.simpleName} subId=$subId " +
                "strength=$strength resource=$resourceName level=$level"
        )
        if (level != null && level in 0..4 && subId >= 0) updateNativeSignalLevel(subId, level)
    }

    private fun iconValue(iconState: Any?): Int =
        iconState?.let { readSignalIntField(it, "icon", "mIcon", "iconId", "resId") } ?: -1

    private fun iconResourceName(
        iconState: Any?,
        resources: android.content.res.Resources,
    ): String {
        val value = iconValue(iconState)
        if (value < 0) return "none"
        return runCatching { resources.getResourceEntryName(value) }.getOrNull()
            ?: "packed-or-unresolved"
    }

    private fun normalizeNativeSignalLevel(value: Int): Int? = when (value) {
        in 0..4 -> value
        5 -> 4 // Some Samsung signal packs number the full-strength icon as level 5.
        else -> null
    }

    private fun signalLevelFromIconState(
        iconState: Any?,
        resources: android.content.res.Resources,
    ): Int {
        if (iconState == null) return -1
        val value = iconValue(iconState)
        if (value < 0) return -1
        val resourceName = runCatching { resources.getResourceEntryName(value) }
            .getOrNull()?.lowercase()
        if (resourceName != null) {
            val namedLevel = resolveSignalResourceName(resourceName)
            return namedLevel.takeIf { it in 0..4 } ?: -1
        }
        return normalizeNativeSignalLevel(value) ?: decodePackedSignalDrawableLevel(value)
    }

    /**
     * Android's SignalDrawable stores QS icon state as a packed integer rather than
     * a drawable resource ID. Decode it through the SystemUI/SettingsLib helper when
     * present; normalize devices with 4 or 5 level buckets to the four dots we draw.
     */
    private fun decodePackedSignalDrawableLevel(fullState: Int): Int {
        val loader = systemUiClassLoader ?: javaClass.classLoader
        val classNames = listOf(
            "com.android.systemui.statusbar.phone.SignalDrawable",
            "com.android.settingslib.graph.SignalDrawable",
        )
        for (className in classNames) {
            val drawableClass = runCatching { Class.forName(className, false, loader) }.getOrNull() ?: continue
            val getLevel = drawableClass.methods.firstOrNull {
                it.name == "getLevel" && java.lang.reflect.Modifier.isStatic(it.modifiers) &&
                    it.parameterTypes.size == 1 && it.parameterTypes[0] == Int::class.javaPrimitiveType
            } ?: continue
            val getNumLevels = drawableClass.methods.firstOrNull {
                it.name == "getNumLevels" && java.lang.reflect.Modifier.isStatic(it.modifiers) &&
                    it.parameterTypes.size == 1 && it.parameterTypes[0] == Int::class.javaPrimitiveType
            } ?: continue
            val packedLevel = runCatching { (getLevel.invoke(null, fullState) as? Number)?.toInt() }.getOrNull()
                ?: continue
            val numLevels = runCatching { (getNumLevels.invoke(null, fullState) as? Number)?.toInt() }.getOrNull()
                ?: continue
            if (numLevels in 4..8 && packedLevel in 0..numLevels) {
                val normalizedLevel = packedLevel.coerceAtMost(numLevels - 1)
                return (normalizedLevel * 4f / (numLevels - 1)).roundToInt().coerceIn(0, 4)
            }
        }
        return fullState.takeIf { it in 0..4 } ?: -1
    }

    private fun updateNativeSignalLevel(subscriptionId: Int, level: Int) {
        if (subscriptionId < 0) return
        val safeLevel = level.coerceIn(0, 4)
        nativeSignalLevels[subscriptionId] = safeLevel
        // Cache native SystemUI levels for fallback/diagnostics, but do not interpret
        // resource IDs as levels. Re-read subscriptions so the newest QS/status icon
        // level takes effect on the very same renderer update.
        diagnostic(
            "Native SIM signal observed: subId=$subscriptionId slot=${subscriptionSlots[subscriptionId] ?: -1} " +
                "level=$safeLevel defaultData=${subscriptionId == defaultDataSubscriptionId}"
        )
        scheduleTelephonyRefresh()
        invalidateGlyphs()
    }

    private fun scheduleTelephonyRefresh() {
        if (!telephonyRefreshPending.compareAndSet(false, true)) return
        main.post {
            telephonyRefreshPending.set(false)
            val context = synchronized(hosts) { hosts.values.firstOrNull()?.root?.context } ?: return@post
            refreshTelephony(context)
            invalidateGlyphs()
        }
    }

    private fun readSignalIntField(target: Any, vararg names: String): Int? {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            val current = type
            for (name in names) {
                val field = runCatching {
                    current.declaredFields.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?.apply { isAccessible = true }
                }.getOrNull() ?: continue
                val result = runCatching { (field.get(target) as? Number)?.toInt() }.getOrNull()
                if (result != null) return result
            }
            type = current.superclass
        }
        return null
    }

    private fun resolveSignalResourceLevel(view: View, resourceId: Int): Int {
        val resourceName = runCatching { view.resources.getResourceEntryName(resourceId) }
            .getOrNull().orEmpty().lowercase()
        return resolveSignalResourceName(resourceName)
    }

    private fun resolveSignalResourceLevelFromResources(resources: android.content.res.Resources, resourceId: Int): Int {
        val resourceName = runCatching { resources.getResourceEntryName(resourceId) }
            .getOrNull().orEmpty().lowercase()
        return resolveSignalResourceName(resourceName)
    }

    private fun resolveSignalResourceName(name: String): Int {
        val lower = name.lowercase()
        val compact = lower.filter { it.isLetterOrDigit() }
        if (compact.contains("wifi") || compact.contains("wlan") ||
            compact.contains("bluetooth") || compact.contains("airplane")
        ) return -1
        val signalIndex = lower.indexOf("signal")
        if (signalIndex < 0) return -1
        val suffix = lower.substring(signalIndex + "signal".length)
        // Match an exact numeric token. Do not mistake names such as "signal_4g"
        // for signal-strength level 4.
        val namedLevel = suffix.split(Regex("[^a-z0-9]+"))
            .firstNotNullOfOrNull { it.toIntOrNull()?.takeIf { level -> level in 0..5 } }
            ?: return when {
                suffix.contains("full") || suffix.contains("excellent") || suffix.contains("strong") -> 4
                else -> -1
            }
        // Samsung resource packs may expose five visible strength steps (0..5),
        // while the OneuiXP renderer uses four dots (0..4).
        return namedLevel.coerceAtMost(4)
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
        // The upstream critical cutoff is a constant, not a user setting.
        criticalThreshold = 20,
        glyphScalePercent = v.glyphScalePercent.coerceIn(70, 150),
        glyphOffsetXDp = v.glyphOffsetXDp.coerceIn(-16, 16),
        glyphOffsetYDp = v.glyphOffsetYDp.coerceIn(-16, 16),
        glyphLeftMarginDp = v.glyphLeftMarginDp.coerceIn(0, 16),
        glyphRightMarginDp = v.glyphRightMarginDp.coerceIn(0, 16),
        signalDotSizePercent = v.signalDotSizePercent.coerceIn(50, 150),
        signalDotSpacingDp = v.signalDotSpacingDp.coerceIn(3, 8),
        signalDotOffsetXDp = v.signalDotOffsetXDp.coerceIn(-8, 8),
        signalDotOffsetYDp = v.signalDotOffsetYDp.coerceIn(-8, 8),
        chargingBoltSizePercent = v.chargingBoltSizePercent.coerceIn(50, 180),
        chargingBoltXDp = v.chargingBoltXDp.coerceIn(-8, 8),
        chargingBoltYDp = v.chargingBoltYDp.coerceIn(-8, 8),
        ringStroke = v.ringStroke.coerceIn(4, 16), arcStroke = v.arcStroke.coerceIn(3, 16),
        valueSize = v.valueSize.coerceIn(6, 18), valueWeight = v.valueWeight.coerceIn(100, 900),
        typeSize = v.typeSize.coerceIn(16, 44), typeWeight = v.typeWeight.coerceIn(100, 900),
        outTypeSizeDp = v.outTypeSizeDp.coerceIn(8, 26), outSignalSizeDp = v.outSignalSizeDp.coerceIn(6, 20),
        outTypeMarginLeftDp = v.outTypeMarginLeftDp.coerceIn(0, 16),
        outTypeMarginRightDp = v.outTypeMarginRightDp.coerceIn(0, 16),
        outSignalMarginDp = v.outSignalMarginDp.coerceIn(0, 16),
        typeSuffixScale = v.typeSuffixScale.coerceIn(50, 100), trackAlpha = v.trackAlpha.coerceIn(0, 255),
    )

    private fun diagnostic(message: String) {
        // Bounded in-memory history for Settings → Send logs, without a feature-owned switch or Logcat flood.
        OneuiXPDiagnosticLog.record(TAG, message)
    }
    private fun glyphLeadingExpansionPx(context: Context, networkType: String): Int {
        val expandedWidthPx = dp(
            context,
            StatusComboStyleOneGlyphRenderer.glyphWidthDp(config, networkType),
        )
        val baseWidthPx = dp(
            context,
            StatusComboStyleOneGlyphRenderer.glyphWidthDpWithoutLeadingExpansion(config, networkType),
        )
        return (expandedWidthPx - baseWidthPx).coerceAtLeast(0)
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt().coerceAtLeast(1)

    private fun dpExact(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()
}
