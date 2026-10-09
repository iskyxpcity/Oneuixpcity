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
        val level: Int = 0,
        val inService: Boolean = false,
        val is5g: Boolean = false,
    )

    private data class HiddenState(
        val alpha: Float,
        val visibility: Int,
    )

    private data class SuppressionKey(
        val enabled: Boolean,
        val showWifi: Boolean,
        val showMobile: Boolean,
        val placementMode: Int,
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
            val density = resources.displayMetrics.density
            val side = metrics.side
            val ringRadius = side * 0.37f
            val ringCenterX = metrics.ringCenterX
            val centerY = height / 2f
            val stroke = runtime.ringStrokeDp.coerceIn(1f, 4f) * density *
                (runtime.sizePercent.coerceIn(60f, 180f) / 100f)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.FILL
                textSize = runtime.batteryTextSizeSp.coerceIn(5f, 14f) *
                    resources.displayMetrics.scaledDensity
                typeface = Typeface.create(
                    "sans-serif",
                    runtime.batteryTextFontWeight.coerceIn(100, 900),
                    false,
                )
            }
            val percentText = StatusBarComboStyleOneRuntime.batteryLevel.coerceIn(0, 100).toString()
            val textWidth = if (runtime.showBatteryPercent) textPaint.measureText(percentText) else 0f
            val boltActive = runtime.showChargingBolt && StatusBarComboStyleOneRuntime.charging
            val boltNotchWidth = if (boltActive) 5.5f * density *
                (runtime.chargingBoltSizePercent.coerceIn(50f, 180f) / 100f) else 0f
            val notchWidth = max(
                if (runtime.showBatteryPercent) textWidth + 2.5f * density else 0f,
                boltNotchWidth,
            )
            val gapDegrees = if (notchWidth <= 0f) 0f else {
                val ratio = (notchWidth / (2f * ringRadius)).coerceIn(0f, 0.94f)
                Math.toDegrees((2f * asin(ratio)).toDouble()).toFloat().coerceIn(0f, 110f)
            }
            val bounds = RectF(
                ringCenterX - ringRadius,
                centerY - ringRadius,
                ringCenterX + ringRadius,
                centerY + ringRadius,
            )

            val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                alpha = runtime.trackAlpha.coerceIn(0, 180)
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
            }
            if (gapDegrees <= 0.01f) {
                canvas.drawCircle(ringCenterX, centerY, ringRadius, trackPaint)
            } else {
                canvas.drawArc(
                    bounds,
                    -90f + gapDegrees / 2f,
                    360f - gapDegrees,
                    false,
                    trackPaint,
                )
            }

            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
            }
            val sweep = (360f - gapDegrees) *
                StatusBarComboStyleOneRuntime.batteryLevel.coerceIn(0, 100) / 100f
            if (sweep > 0f) {
                canvas.drawArc(
                    bounds,
                    -90f + gapDegrees / 2f,
                    sweep,
                    false,
                    progressPaint,
                )
            }

            if (runtime.showBatteryPercent) {
                val fm = textPaint.fontMetrics
                val baseline = centerY - (fm.ascent + fm.descent) / 2f + side * 0.08f
                canvas.drawText(
                    percentText,
                    ringCenterX - textPaint.measureText(percentText) / 2f,
                    baseline,
                    textPaint,
                )
            }

            if (boltActive) {
                val boltScale = runtime.chargingBoltSizePercent.coerceIn(50f, 180f) / 100f
                val bx = ringCenterX + runtime.chargingBoltXDp.coerceIn(-8f, 8f) * density
                val by = centerY - ringRadius * 0.58f +
                    runtime.chargingBoltYDp.coerceIn(-8f, 8f) * density
                val boltW = side * 0.20f * boltScale
                val boltH = side * 0.30f * boltScale
                val path = Path().apply {
                    moveTo(bx + boltW * 0.15f, by - boltH * 0.55f)
                    lineTo(bx - boltW * 0.48f, by + boltH * 0.10f)
                    lineTo(bx - boltW * 0.03f, by + boltH * 0.10f)
                    lineTo(bx - boltW * 0.20f, by + boltH * 0.62f)
                    lineTo(bx + boltW * 0.48f, by - boltH * 0.12f)
                    lineTo(bx + boltW * 0.04f, by - boltH * 0.12f)
                    close()
                }
                canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = tint
                    style = Paint.Style.FILL
                })
            }

            var segmentX = if (runtime.placementMode == 0) 0f else side
            if (runtime.placementMode == 0) {
                if (runtime.showWifi) {
                    drawWifi(
                        canvas, segmentX, centerY, metrics.wifiWidth, side, density, tint,
                        StatusBarComboStyleOneRuntime.wifiConnected,
                        StatusBarComboStyleOneRuntime.wifiLevel, runtime.trackAlpha,
                    )
                    segmentX += metrics.wifiWidth + metrics.gap
                }
                if (runtime.showMobile) {
                    drawSignal(
                        canvas, segmentX, centerY, metrics.signalWidth, side, density, tint,
                        StatusBarComboStyleOneRuntime.currentSignalLevel(), runtime.dotsGapDp,
                        runtime.dotsSizePercent, runtime.trackAlpha,
                    )
                    segmentX += metrics.signalWidth + metrics.gap
                }
                if (metrics.typeWidth > 0f && StatusBarComboStyleOneRuntime.network5g) {
                    drawNetworkType(canvas, segmentX, centerY, metrics.typeWidth, side, tint, density)
                }
            } else {
                if (metrics.typeWidth > 0f && StatusBarComboStyleOneRuntime.network5g) {
                    drawNetworkType(canvas, segmentX, centerY, metrics.typeWidth, side, tint, density)
                    segmentX += metrics.typeWidth + metrics.gap
                }
                if (runtime.showMobile) {
                    drawSignal(
                        canvas, segmentX, centerY, metrics.signalWidth, side, density, tint,
                        StatusBarComboStyleOneRuntime.currentSignalLevel(), runtime.dotsGapDp,
                        runtime.dotsSizePercent, runtime.trackAlpha,
                    )
                    segmentX += metrics.signalWidth + metrics.gap
                }
                if (runtime.showWifi) {
                    drawWifi(
                        canvas, segmentX, centerY, metrics.wifiWidth, side, density, tint,
                        StatusBarComboStyleOneRuntime.wifiConnected,
                        StatusBarComboStyleOneRuntime.wifiLevel, runtime.trackAlpha,
                    )
                }
            }
        }

        private fun drawWifi(
            canvas: Canvas,
            left: Float,
            centerY: Float,
            width: Float,
            side: Float,
            density: Float,
            tint: Int,
            connected: Boolean,
            level: Int,
            trackAlpha: Int,
        ) {
            if (width <= 0f) return
            val scale = side / (24f * density)
            val cx = left + width / 2f
            val cy = centerY + side * 0.04f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.STROKE
                strokeWidth = 1.35f * density * scale
                strokeCap = Paint.Cap.ROUND
            }
            val radii = floatArrayOf(0.21f, 0.32f, 0.43f)
            radii.forEachIndexed { index, factor ->
                paint.alpha = if (connected && index < level.coerceIn(0, 4)) {
                    255
                } else {
                    trackAlpha.coerceIn(0, 180)
                }
                val radius = side * factor
                canvas.drawArc(
                    RectF(cx - radius, cy - radius, cx + radius, cy + radius),
                    225f,
                    90f,
                    false,
                    paint,
                )
            }
            paint.style = Paint.Style.FILL
            paint.alpha = if (connected && level > 0) 255 else trackAlpha.coerceIn(0, 180)
            canvas.drawCircle(cx, cy + side * 0.34f, max(1f, side * 0.045f), paint)
        }

        private fun drawSignal(
            canvas: Canvas,
            left: Float,
            centerY: Float,
            width: Float,
            side: Float,
            density: Float,
            tint: Int,
            level: Int,
            gapDp: Float,
            sizePercent: Float,
            trackAlpha: Int,
        ) {
            if (width <= 0f) return
            val scale = (sizePercent.coerceIn(50f, 180f) / 100f) * (side / (24f * density))
            val radius = max(0.65f * density, 1.1f * density * scale)
            val gap = gapDp.coerceIn(0f, 6f) * density
            val total = radius * 2f * 4f + gap * 3f
            var x = left + (width - total) / 2f + radius
            val y = centerY + side * 0.14f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                style = Paint.Style.FILL
            }
            for (index in 0..3) {
                paint.alpha = if (index < level.coerceIn(0, 4)) 255 else trackAlpha.coerceIn(0, 180)
                canvas.drawCircle(x, y, radius, paint)
                x += radius * 2f + gap
            }
        }

        private fun drawNetworkType(
            canvas: Canvas,
            left: Float,
            centerY: Float,
            width: Float,
            side: Float,
            tint: Int,
            density: Float,
        ) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tint
                textSize = 6.5f * density * (side / (24f * density))
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val fm = paint.fontMetrics
            canvas.drawText("5G", left + width / 2f, centerY - (fm.ascent + fm.descent) / 2f, paint)
        }
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
            if (cfg.placementMode == 1) {
                val cutout = runCatching {
                    host.rootWindowInsets?.displayCutout?.boundingRects
                        ?.filter { it.width() > 0 }
                        ?.maxByOrNull { it.width() }
                }.getOrNull()
                val centerX = if (cutout != null) hostLocation[0] + cutout.centerX()
                    else context.resources.displayMetrics.widthPixels / 2
                val centerY = if (cutout != null) hostLocation[1] + cutout.centerY()
                    else hostLocation[1] + host.height / 2
                x = (centerX - metrics.ringCenterX +
                    cfg.offsetXDp * context.resources.displayMetrics.density).roundToInt()
                y = (centerY - metrics.height / 2f +
                    cfg.offsetYDp * context.resources.displayMetrics.density).roundToInt()
            } else {
                val anchor = holder.batteryAnchor?.takeIf { it.isAttachedToWindow }
                val anchorLocation = IntArray(2)
                if (anchor != null) anchor.getLocationOnScreen(anchorLocation)
                val centerX = if (anchor != null) anchorLocation[0] + anchor.width / 2f
                    else hostLocation[0] + host.width - metrics.side / 2f
                val centerY = if (anchor != null) anchorLocation[1] + anchor.height / 2f
                    else hostLocation[1] + host.height / 2f
                x = (centerX - metrics.ringCenterX +
                    cfg.offsetXDp * context.resources.displayMetrics.density).roundToInt()
                y = (centerY - metrics.height / 2f +
                    cfg.offsetYDp * context.resources.displayMetrics.density).roundToInt()
            }

            val widthChanged = params.width != metrics.width || params.height != metrics.height
            val positionChanged = params.x != x || params.y != y
            if (!widthChanged && !positionChanged) return
            params.width = metrics.width
            params.height = metrics.height
            params.x = x
            params.y = y
            runCatching { windowManager.updateViewLayout(view, params) }
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
    private var subscriptionListener: SubscriptionManager.OnSubscriptionsChangedListener? = null

    fun isEnabled(): Boolean = config.enabled && hasAttachedRenderer()

    fun isMobileNativeSuppressed(): Boolean =
        config.enabled && config.showMobile && hasAttachedRenderer()

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
        placementMode = source.placementMode.coerceIn(0, 1),
        sizePercent = source.sizePercent.coerceIn(60f, 180f),
        offsetXDp = source.offsetXDp.coerceIn(-24f, 24f),
        offsetYDp = source.offsetYDp.coerceIn(-15f, 15f),
        ringStrokeDp = source.ringStrokeDp.coerceIn(1f, 4f),
        trackAlpha = source.trackAlpha.coerceIn(0, 180),
        dotsGapDp = source.dotsGapDp.coerceIn(0f, 6f),
        dotsSizePercent = source.dotsSizePercent.coerceIn(50f, 180f),
        batteryTextSizeSp = source.batteryTextSizeSp.coerceIn(5f, 14f),
        batteryTextFontWeight = source.batteryTextFontWeight.coerceIn(100, 900),
        chargingBoltSizePercent = source.chargingBoltSizePercent.coerceIn(50f, 180f),
        chargingBoltXDp = source.chargingBoltXDp.coerceIn(-8f, 8f),
        chargingBoltYDp = source.chargingBoltYDp.coerceIn(-8f, 8f),
        lowBatteryThreshold = source.lowBatteryThreshold.coerceIn(5, 50),
    )

    private fun calculateMetrics(
        context: Context,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
    ): Metrics {
        val density = context.resources.displayMetrics.density
        val scale = cfg.sizePercent.coerceIn(60f, 180f) / 100f
        val side = 24f * density * scale
        val wifiWidth = if (cfg.showWifi) 11f * density * scale else 0f
        val signalWidth = if (cfg.showMobile) 11f * density * scale else 0f
        val typeWidth = if (cfg.showMobile && cfg.show5gNetworkType && network5g) {
            9f * density * scale
        } else 0f
        var segments = 0
        if (wifiWidth > 0f) segments++
        if (signalWidth > 0f) segments++
        if (typeWidth > 0f) segments++
        val gap = if (segments > 1) 1.5f * density * scale else 0f
        val width = (side + wifiWidth + signalWidth + typeWidth +
            max(0, segments - 1) * gap).roundToInt().coerceAtLeast(1)
        val ringCenterX = if (cfg.placementMode == 0) width - side / 2f else side / 2f
        return Metrics(
            width, side.roundToInt().coerceAtLeast(1), side, ringCenterX,
            wifiWidth, signalWidth, typeWidth, gap,
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
            if (config.enabled && holder.batteryAnchor?.isAttachedToWindow != true) {
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

        val key = SuppressionKey(cfg.enabled, cfg.showWifi, cfg.showMobile, cfg.placementMode)
        if (holder.suppressionKey != key || holder.batteryAnchor?.isAttachedToWindow != true) {
            restoreNative(holder)
            holder.batteryAnchor = findBatteryAnchor(holder.statusBarView)
            suppressNative(holder, cfg)
            holder.suppressionKey = key
        }
        overlay.syncGeometry(cfg)
    }

    private fun suppressNative(holder: Holder, cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        val statusBar = holder.statusBarView
        val views = collectDescendants(statusBar)
        val anchor = holder.batteryAnchor

        if (anchor != null) {
            rememberAndSet(holder, anchor, if (cfg.placementMode == 0) View.INVISIBLE else View.GONE)
        }

        val candidates = views.filter { view ->
            view !== anchor && (
                (cfg.showWifi && isNativeWifi(view)) ||
                (cfg.showMobile && isNativeMobile(view))
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
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                batteryLevel = (level * 100 / scale).coerceIn(0, 100)
                val status = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN,
                )
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                refreshGlyphs()
            }
        }
        batteryReceiver = receiver
        runCatching {
            appContext.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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
                if (!signalStates.containsKey(info.subscriptionId)) {
                    signalStates[info.subscriptionId] = SignalState(info.subscriptionId)
                }
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
            updateSignal(subscriptionId, inService = serviceState.state == ServiceState.STATE_IN_SERVICE)
        }

        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            val value = telephonyDisplayInfo.overrideNetworkType ==
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
                telephonyDisplayInfo.overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
            updateSignal(subscriptionId, is5g = value)
        }
    }

    private fun updateSignal(
        subscriptionId: Int,
        level: Int? = null,
        inService: Boolean? = null,
        is5g: Boolean? = null,
    ) {
        synchronized(signalStates) {
            val previous = signalStates[subscriptionId] ?: SignalState(subscriptionId)
            signalStates[subscriptionId] = previous.copy(
                level = level ?: previous.level,
                inService = inService ?: previous.inService,
                is5g = is5g ?: previous.is5g,
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
