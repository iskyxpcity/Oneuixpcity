package com.alohaoneui.hook.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.TrafficStats
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.alohaoneui.common.IgnoreUnknownKeysJson
import com.alohaoneui.common.Package
import com.alohaoneui.common.Preference
import com.alohaoneui.common.StatusBarRuntimeBridge
import com.alohaoneui.hook.util.PreferenceProvider
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/** Samsung SystemUI runtime for the AlohaOneUI Wi-Fi icon editor. */
internal object StatusBarWifiRuntime {
    private const val PREFERENCE_CHANGED_URI =
        "content://com.alohaoneui.app.statusbridge/preferences"

    private data class NativeState(
        val alpha: Float,
        val visibility: Int,
        val scaleX: Float,
        val scaleY: Float,
    )

    private data class Holder(
        val statusBarView: ViewGroup,
        val wifiHost: ViewGroup,
        val glyph: WifiGlyphView,
        val nativeStates: Map<View, NativeState>,
        val originalHostScaleX: Float,
        val originalHostScaleY: Float,
        val originalHostTranslationY: Float,
        val originalHostVisibility: Int,
        val originalMarginStart: Int?,
        val originalMarginEnd: Int?,
    )

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val wifiReceiverStarted = AtomicBoolean(false)
    private val holders = WeakHashMap<View, Holder>()
    private val retryCounts = WeakHashMap<View, Int>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var config = Preference.SystemUI.StatusBarWifiRuntime()

    @Volatile
    private var activityUp = false

    @Volatile
    private var activityDown = false

    private var preferenceThread: HandlerThread? = null
    private var activityThread: HandlerThread? = null
    private var activityHandler: Handler? = null
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null
    private var wifiReceiver: BroadcastReceiver? = null
    private var lastTxBytes = 0L
    private var lastRxBytes = 0L

    private val activityRunnable = object : Runnable {
        override fun run() {
            val current = config
            if (!current.enabled || current.hideActivity) return

            val tx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
            val rx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
            if (lastTxBytes > 0L && lastRxBytes > 0L) {
                activityUp = tx - lastTxBytes > 1024L
                activityDown = rx - lastRxBytes > 1024L
                invalidateGlyphs()
            }
            lastTxBytes = tx
            lastRxBytes = rx
            activityHandler?.postDelayed(this, 1000L)
        }
    }

    fun install(
        module: XposedModule,
        param: XposedModuleInterface.PackageReadyParam,
    ) {
        if (param.packageName != Package.SYSTEMUI) return
        if (!installed.compareAndSet(false, true)) return

        runCatching {
            val clazz = param.classLoader.loadClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView"
            )
            clazz.declaredConstructors.forEach { constructor ->
                module.hook(constructor).intercept { chain ->
                    val result = chain.proceed()
                    (chain.thisObject as? View)?.let { statusBarView ->
                        statusBarView.post {
                            registerStatusBarView(statusBarView)
                            ensurePreferenceWatcher(module, statusBarView.context)
                            ensureWifiReceiver(statusBarView.context)
                        }
                    }
                    result
                }
            }
        }
    }

    private fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (!watcherStarted.compareAndSet(false, true)) return
        val worker = HandlerThread("AlohaOneUIStatusWifiPrefs").apply { start() }
        preferenceThread = worker
        val background = Handler(worker.looper)
        var previous: Preference.SystemUI.StatusBarWifiRuntime? = null
        // The Wi-Fi host must be re-applied when Style One changes ownership,
        // even when the standalone Wi-Fi category settings are unchanged.
        var previousComboOwnsWifi: Boolean? = null

        fun refreshOnce() {
            background.post {
                val statusBar = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }?.systemUI?.statusBar
                }.getOrNull() ?: return@post
                val current = statusBar.wifiRuntime
                val comboOwnsWifi = statusBar.comboStyleOneRuntime.enabled &&
                    statusBar.comboStyleOneRuntime.showWifi
                if (current == previous && comboOwnsWifi == previousComboOwnsWifi) return@post
                previous = current
                previousComboOwnsWifi = comboOwnsWifi
                mainHandler.post { updateRuntime(current) }
            }
        }

        val observer = object : ContentObserver(background) {
            override fun onChange(selfChange: Boolean) = refreshOnce()
            override fun onChange(selfChange: Boolean, uri: Uri?) = refreshOnce()
        }
        preferenceObserver = observer
        runCatching {
            context.contentResolver.registerContentObserver(
                Uri.parse(PREFERENCE_CHANGED_URI),
                false,
                observer,
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
                            Preference.SystemUI.StatusBar.serializer(),
                            payload,
                        )
                    }.getOrNull() ?: return@post
                    val current = statusBar.wifiRuntime
                    val comboOwnsWifi = statusBar.comboStyleOneRuntime.enabled &&
                        statusBar.comboStyleOneRuntime.showWifi
                    val ownershipChanged = comboOwnsWifi != previousComboOwnsWifi
                    previous = current
                    previousComboOwnsWifi = comboOwnsWifi
                    if (current != config || ownershipChanged) {
                        mainHandler.post { updateRuntime(current) }
                    }
                }
            }
        }
        runtimeReceiver = receiver
        runCatching {
            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
        }

        refreshOnce()
    }

    @Suppress("DEPRECATION")
    private fun ensureWifiReceiver(context: Context) {
        if (!wifiReceiverStarted.compareAndSet(false, true)) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                invalidateGlyphs()
            }
        }
        wifiReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }
    }

    private fun registerStatusBarView(statusBarView: View) {
        if (!statusBarView.isLaidOut) {
            statusBarView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int,
                ) {
                    if (!v.isLaidOut) return
                    v.removeOnLayoutChangeListener(this)
                    registerStatusBarView(v)
                }
            })
            return
        }

        val existing = synchronized(holders) { holders[statusBarView] }
        if (existing != null) {
            applyConfig(existing, config)
            return
        }

        val holder = createHolder(statusBarView)
        if (holder == null) {
            val attempts = synchronized(retryCounts) {
                val next = (retryCounts[statusBarView] ?: 0) + 1
                retryCounts[statusBarView] = next
                next
            }
            if (attempts <= 12) {
                statusBarView.postDelayed({ registerStatusBarView(statusBarView) }, 250L)
            }
            return
        }

        synchronized(holders) {
            holders[statusBarView] = holder
            retryCounts.remove(statusBarView)
        }
        applyConfig(holder, config)
        refreshActivitySampling()
    }

    private fun updateRuntime(newConfig: Preference.SystemUI.StatusBarWifiRuntime) {
        config = sanitize(newConfig)
        val active = synchronized(holders) { holders.values.toList() }
        active.forEach { holder -> holder.statusBarView.post { applyConfig(holder, config) } }
        refreshActivitySampling()
    }

    private fun sanitize(
        source: Preference.SystemUI.StatusBarWifiRuntime,
    ): Preference.SystemUI.StatusBarWifiRuntime = source.copy(
        arcCount = source.arcCount.coerceIn(3, 4),
        overallSizeDp = source.overallSizeDp.coerceIn(8f, 32f),
        blackDepth = source.blackDepth.coerceIn(0.25f, 1f),
        verticalOffsetDp = source.verticalOffsetDp.coerceIn(-24f, 24f),
        startMarginDp = source.startMarginDp.coerceIn(-12f, 36f),
        endMarginDp = source.endMarginDp.coerceIn(-12f, 36f),
        arc1 = sanitizeArc(source.arc1),
        arc2 = sanitizeArc(source.arc2),
        arc3 = sanitizeArc(source.arc3),
        arc4 = sanitizeArc(source.arc4),
    )

    private fun sanitizeArc(
        source: Preference.SystemUI.StatusBarWifiArcStyle,
    ) = source.copy(
        length = source.length.coerceIn(0.35f, 1.2f),
        strokeWidthDp = source.strokeWidthDp.coerceIn(0.5f, 4f),
        leftRadius = source.leftRadius.coerceIn(0.5f, 1.5f),
        rightRadius = source.rightRadius.coerceIn(0.5f, 1.5f),
        leftTiltDeg = source.leftTiltDeg.coerceIn(-35f, 35f),
        rightTiltDeg = source.rightTiltDeg.coerceIn(-35f, 35f),
        bend = source.bend.coerceIn(-1f, 1f),
        yOffsetDp = source.yOffsetDp.coerceIn(-10f, 10f),
    )

    private fun createHolder(statusBarView: View): Holder? {
        val statusHost = statusBarView as? ViewGroup ?: return null
        val wifiHost = findWifiHost(statusHost) ?: return null
        val nativeViews = collectDescendants(wifiHost)
        if (nativeViews.isEmpty()) return null

        val nativeStates = nativeViews.associateWith { view ->
            NativeState(
                alpha = view.alpha,
                visibility = view.visibility,
                scaleX = view.scaleX,
                scaleY = view.scaleY,
            )
        }
        val marginParams = wifiHost.layoutParams as? ViewGroup.MarginLayoutParams
        val glyph = WifiGlyphView(statusHost.context)
        statusHost.overlay.add(glyph)
        StatusBarBurnInProtectionRuntime.registerView(glyph)

        val holder = Holder(
            statusBarView = statusHost,
            wifiHost = wifiHost,
            glyph = glyph,
            nativeStates = nativeStates,
            originalHostScaleX = wifiHost.scaleX,
            originalHostScaleY = wifiHost.scaleY,
            originalHostTranslationY = wifiHost.translationY,
            originalHostVisibility = wifiHost.visibility,
            originalMarginStart = marginParams?.marginStart,
            originalMarginEnd = marginParams?.marginEnd,
        )

        wifiHost.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            positionGlyph(holder, config)
        }
        statusHost.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            positionGlyph(holder, config)
        }
        return holder
    }

    private fun findWifiHost(root: ViewGroup): ViewGroup? {
        val resources = root.resources
        listOf(
            "wifi_group",
            "wifi_combo",
            "status_bar_wifi",
            "status_bar_wifi_group",
            "wifi_container",
            "modern_status_bar_wifi",
        ).forEach { name ->
            val id = resources.getIdentifier(name, "id", Package.SYSTEMUI)
            if (id != 0) {
                val found = root.findViewById<View>(id)
                when (found) {
                    is ViewGroup -> return found
                    null -> Unit
                    else -> (found.parent as? ViewGroup)?.let { return it }
                }
            }
        }

        var best: ViewGroup? = null
        var bestScore = Int.MIN_VALUE
        fun visit(view: View, depth: Int) {
            if (depth > 8) return
            if (view is ViewGroup) {
                val name = resourceEntryName(view)
                val className = view.javaClass.simpleName.lowercase()
                val nameLower = name.lowercase()
                var score = 0
                if (nameLower.contains("wifi")) score += 70
                if (className.contains("wifi")) score += 55
                if (nameLower.contains("group") || nameLower.contains("combo") || nameLower.contains("container")) score += 20
                if (view.childCount > 0) score += min(view.childCount, 6)
                score -= depth * 2
                if (score > bestScore && score >= 55) {
                    best = view
                    bestScore = score
                }
                for (index in 0 until view.childCount) {
                    visit(view.getChildAt(index), depth + 1)
                }
            }
        }
        visit(root, 0)
        return best
    }

    private fun resourceEntryName(view: View): String = runCatching {
        if (view.id == View.NO_ID || view.id == 0) ""
        else view.resources.getResourceEntryName(view.id)
    }.getOrDefault("")

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

    private fun restoreNative(holder: Holder) {
        holder.nativeStates.forEach { (view, state) ->
            if (view.parent != null) {
                view.alpha = state.alpha
                view.visibility = state.visibility
                view.scaleX = state.scaleX
                view.scaleY = state.scaleY
            }
        }
        holder.wifiHost.scaleX = holder.originalHostScaleX
        holder.wifiHost.scaleY = holder.originalHostScaleY
        holder.wifiHost.translationY = holder.originalHostTranslationY
        holder.wifiHost.visibility = holder.originalHostVisibility
    }

    private fun applyMargins(
        holder: Holder,
        current: Preference.SystemUI.StatusBarWifiRuntime,
    ) {
        val params = holder.wifiHost.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val density = holder.wifiHost.resources.displayMetrics.density
        val startBase = holder.originalMarginStart ?: params.marginStart
        val endBase = holder.originalMarginEnd ?: params.marginEnd
        val targetStart = startBase + (current.startMarginDp * density).toInt()
        val targetEnd = endBase + (current.endMarginDp * density).toInt()
        if (params.marginStart != targetStart || params.marginEnd != targetEnd) {
            params.marginStart = targetStart
            params.marginEnd = targetEnd
            holder.wifiHost.layoutParams = params
        }
    }

    private fun applyConfig(
        holder: Holder,
        current: Preference.SystemUI.StatusBarWifiRuntime,
    ) {
        restoreNative(holder)
        val density = holder.wifiHost.resources.displayMetrics.density

        // Only hide this independent Wi-Fi category when the combo actually draws
        // Wi-Fi arcs. If its Wi-Fi option is off, leave the native icon or this
        // category's own replacement available.
        if (StatusBarComboStyleOneRuntime.isNativeWifiSuppressed()) {
            holder.glyph.visibility = View.GONE
            holder.wifiHost.visibility = View.GONE
            holder.statusBarView.requestLayout()
            return
        }

        applyMargins(holder, current)

        if (!current.enabled) {
            holder.glyph.visibility = View.GONE
            positionGlyph(holder, current)
            return
        }

        holder.wifiHost.translationY =
            holder.originalHostTranslationY + current.verticalOffsetDp * density

        if (current.editorEnabled) {
            holder.nativeStates.keys.forEach { view ->
                if (view.parent != null) view.alpha = 0f
            }
            holder.wifiHost.scaleX = holder.originalHostScaleX
            holder.wifiHost.scaleY = holder.originalHostScaleY
            holder.glyph.runtime = current
            holder.glyph.activityUp = activityUp
            holder.glyph.activityDown = activityDown
            holder.glyph.tintSource = holder.nativeStates.keys.filterIsInstance<ImageView>().firstOrNull()
            holder.glyph.visibility = if (holder.wifiHost.visibility == View.VISIBLE) View.VISIBLE else View.GONE
            positionGlyph(holder, current)
            holder.glyph.invalidate()
        } else {
            holder.glyph.visibility = View.GONE
            val scale = (current.overallSizeDp / 16f).coerceIn(0.5f, 2f)
            holder.wifiHost.scaleX = holder.originalHostScaleX * scale
            holder.wifiHost.scaleY = holder.originalHostScaleY * scale

            holder.nativeStates.forEach { (view, state) ->
                if (view.parent == null) return@forEach
                val name = resourceEntryName(view).lowercase()
                val isActivity =
                    name.contains("activity") || name.contains("inout") ||
                        name.endsWith("_in") || name.endsWith("_out") ||
                        name.contains("wifi_in") || name.contains("wifi_out")
                val isStandard =
                    name.contains("standard") || name.contains("wifi6") ||
                        name.contains("wifi_6") || name.contains("wifi_type") ||
                        name.contains("badge")

                if (isActivity && current.hideActivity) {
                    view.visibility = View.GONE
                } else if (isStandard && current.hideStandardBadge) {
                    view.visibility = View.GONE
                } else if (isStandard && current.extendStandardBadge) {
                    view.scaleX = state.scaleX * 1.15f
                    view.scaleY = state.scaleY * 1.15f
                }

                if (view is ImageView && view.visibility == View.VISIBLE) {
                    view.alpha = state.alpha * current.blackDepth
                }
            }
        }
    }

    private fun positionGlyph(
        holder: Holder,
        current: Preference.SystemUI.StatusBarWifiRuntime,
    ) {
        if (!current.enabled || !current.editorEnabled) {
            holder.glyph.visibility = View.GONE
            return
        }
        if (holder.statusBarView.width <= 0 || holder.statusBarView.height <= 0) return
        if (holder.wifiHost.width <= 0 || holder.wifiHost.height <= 0) return

        val density = holder.wifiHost.resources.displayMetrics.density
        val size = max(1, (current.overallSizeDp * density).toInt())
        val extra = if (!current.hideStandardBadge) {
            ((if (current.extendStandardBadge) 14f else 8f) * density).toInt()
        } else 0
        val hostLocation = IntArray(2)
        val barLocation = IntArray(2)
        holder.wifiHost.getLocationOnScreen(hostLocation)
        holder.statusBarView.getLocationOnScreen(barLocation)

        val hostLeft = hostLocation[0] - barLocation[0]
        val hostTop = hostLocation[1] - barLocation[1]
        val left = hostLeft + holder.wifiHost.width / 2 - size / 2
        val top = hostTop + holder.wifiHost.height / 2 - size / 2

        holder.glyph.runtime = current
        holder.glyph.activityUp = activityUp
        holder.glyph.activityDown = activityDown
        holder.glyph.visibility = if (holder.wifiHost.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        holder.glyph.measure(
            View.MeasureSpec.makeMeasureSpec(size + extra, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
        )
        holder.glyph.layout(left, top, left + size + extra, top + size)
        holder.glyph.invalidate()
    }

    private fun invalidateGlyphs() {
        mainHandler.post {
            val active = synchronized(holders) { holders.values.toList() }
            active.forEach { holder ->
                if (config.enabled) {
                    holder.glyph.activityUp = activityUp
                    holder.glyph.activityDown = activityDown
                    positionGlyph(holder, config)
                }
            }
        }
    }

    private fun refreshActivitySampling() {
        activityHandler?.removeCallbacks(activityRunnable)
        if (!config.enabled || config.hideActivity) {
            activityUp = false
            activityDown = false
            lastTxBytes = 0L
            lastRxBytes = 0L
            invalidateGlyphs()
            return
        }
        if (activityThread == null) {
            activityThread = HandlerThread("AlohaOneUIStatusWifiActivity").apply { start() }
            activityHandler = Handler(activityThread!!.looper)
        }
        lastTxBytes = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
        lastRxBytes = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        activityHandler?.postDelayed(activityRunnable, 1000L)
    }

    private class WifiGlyphView(context: Context) : View(context) {
        var runtime = Preference.SystemUI.StatusBarWifiRuntime()
        var tintSource: ImageView? = null
        var activityUp: Boolean = false
        var activityDown: Boolean = false

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.LEFT
        }
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val current = runtime
            val density = resources.displayMetrics.density
            val iconSize = min(height.toFloat(), current.overallSizeDp * density)
            if (iconSize <= 1f) return

            val tint = resolveTintColor()
            val baseAlpha = (Color.alpha(tint) * current.blackDepth).toInt().coerceIn(20, 255)
            val rgb = Color.rgb(Color.red(tint), Color.green(tint), Color.blue(tint))
            val centerX = iconSize * 0.5f
            val baseY = iconSize * 0.78f
            val styles = listOf(current.arc1, current.arc2, current.arc3, current.arc4)
                .take(current.arcCount.coerceIn(3, 4))
            val activeLevel = resolveSignalLevel(styles.size)

            styles.forEachIndexed { index, style ->
                val active = index >= styles.size - activeLevel
                val alpha = if (active) baseAlpha else (baseAlpha * 0.24f).toInt().coerceAtLeast(12)
                paint.color = rgb
                paint.alpha = alpha
                paint.strokeWidth = style.strokeWidthDp * density

                val halfWidth = iconSize * 0.43f * style.length
                val leftHalf = halfWidth * (0.65f + 0.35f * style.leftRadius)
                val rightHalf = halfWidth * (0.65f + 0.35f * style.rightRadius)
                val depth = if (styles.size <= 1) 0f else index.toFloat() / (styles.size - 1).toFloat()
                val rowY = baseY - (styles.size - 1 - index) * iconSize * 0.045f + style.yOffsetDp * density
                val leftTilt = tan(Math.toRadians(style.leftTiltDeg.toDouble())).toFloat() * halfWidth * 0.18f
                val rightTilt = tan(Math.toRadians(style.rightTiltDeg.toDouble())).toFloat() * halfWidth * 0.18f
                val peakLift = halfWidth * (0.72f - depth * 0.10f + style.bend * 0.22f)

                val leftX = centerX - leftHalf
                val rightX = centerX + rightHalf
                val leftY = rowY + leftTilt
                val rightY = rowY - rightTilt
                val peakY = rowY - peakLift
                val controlSpan = (rightX - leftX) * 0.28f

                path.reset()
                path.moveTo(leftX, leftY)
                path.cubicTo(
                    leftX + controlSpan,
                    peakY,
                    rightX - controlSpan,
                    peakY,
                    rightX,
                    rightY,
                )
                canvas.drawPath(path, paint)
            }

            paint.style = Paint.Style.FILL
            paint.color = rgb
            paint.alpha = baseAlpha
            canvas.drawCircle(centerX, iconSize * 0.82f, max(0.8f * density, iconSize * 0.052f), paint)
            paint.style = Paint.Style.STROKE

            if (!current.hideStandardBadge) {
                resolveStandardLabel()?.let { label ->
                    textPaint.color = rgb
                    textPaint.alpha = baseAlpha
                    textPaint.textSize = (if (current.extendStandardBadge) 4.2f else 3.8f) * density
                    textPaint.isFakeBoldText = current.extendStandardBadge
                    val display = if (current.extendStandardBadge) "WiFi $label" else label
                    canvas.drawText(display, iconSize * 0.80f, iconSize * 0.38f, textPaint)
                }
            }

            if (!current.hideActivity) {
                paint.color = rgb
                paint.alpha = baseAlpha
                paint.strokeWidth = max(0.75f * density, iconSize * 0.045f)
                val arrowX = iconSize * 0.82f
                val arrowY = iconSize * 0.66f
                val step = 2.6f * density
                if (activityUp) {
                    canvas.drawLine(arrowX, arrowY + step, arrowX, arrowY - step, paint)
                    canvas.drawLine(arrowX, arrowY - step, arrowX - step * 0.45f, arrowY - step * 0.45f, paint)
                    canvas.drawLine(arrowX, arrowY - step, arrowX + step * 0.45f, arrowY - step * 0.45f, paint)
                }
                if (activityDown) {
                    val x = arrowX + 3.4f * density
                    canvas.drawLine(x, arrowY - step, x, arrowY + step, paint)
                    canvas.drawLine(x, arrowY + step, x - step * 0.45f, arrowY + step * 0.45f, paint)
                    canvas.drawLine(x, arrowY + step, x + step * 0.45f, arrowY + step * 0.45f, paint)
                }
            }
        }

        private fun resolveTintColor(): Int {
            val source = tintSource
            val tintList = source?.imageTintList
            if (source != null && tintList != null) {
                return tintList.getColorForState(source.drawableState, tintList.defaultColor)
            }
            return Color.WHITE
        }

        @Suppress("DEPRECATION")
        private fun resolveWifiInfo() = runCatching {
            val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            manager.connectionInfo
        }.getOrNull()

        private fun resolveSignalLevel(count: Int): Int {
            val info = resolveWifiInfo() ?: return count
            return runCatching {
                WifiManager.calculateSignalLevel(info.rssi, count + 1).coerceIn(1, count)
            }.getOrDefault(count)
        }

        private fun resolveStandardLabel(): String? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
            val info = resolveWifiInfo() ?: return null
            return when (info.wifiStandard) {
                6 -> if (info.frequency >= 5925) "6E" else "6"
                8 -> "7"
                else -> null
            }
        }
    }
}
