package io.github.soclear.oneuix.hook.systemui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import io.github.soclear.oneuix.hook.util.StatusBarClockDrawPaintBridge
import io.github.soclear.oneuix.hook.util.reflect
import java.net.NetworkInterface
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Event-driven presentation and data layer for Samsung's native NetspeedView.
 *
 * Samsung still supplies the stock aggregate network-speed value and owns the native
 * view lifecycle. OneuiXP is the only SystemUI presentation owner once this runtime is
 * installed. Separate upload/download mode uses a dedicated data-source hook here so
 * the legacy OneUIX Network.kt NetspeedView hooks can remain uninstalled and untouched.
 */
internal object StatusBarNetworkSpeedRuntime {
    private const val PREFERENCE_CHANGED_URI =
        "content://com.oneuixp.app.statusbridge/preferences"
    private const val NETSPEED_VIEW_CLASS =
        "com.android.systemui.statusbar.policy.NetspeedView"
    private const val NETWORK_SPEED_INTERVAL_MS = 3000L
    private const val MESSAGE_INITIAL = 1
    private const val MESSAGE_UPDATE = 2

    // Use real SystemUI layout spacing instead of trailing text whitespace.
    private const val NETWORK_SPEED_TRAILING_GAP_DP = 3f

    private data class RuntimeConfig(
        val display: Preference.SystemUI.StatusBarNetworkSpeedRuntime =
            Preference.SystemUI.StatusBarNetworkSpeedRuntime(),
        val showSeparateUpDown: Boolean = false,
        val thresholdKb: Int = 0,
        val modifyLineGap: Boolean = false,
        val lineGapDp: Float = 0f,
        val setUpperScale: Boolean = false,
        val upperScale: Float = 1f,
        val setLowerScale: Boolean = false,
        val lowerScale: Float = 1f,
    )

    private data class ViewState(
        var host: View? = null,
        var nativeHostMarginStart: Int? = null,
        var nativeHostMarginEnd: Int? = null,
        var hostMarginsCaptured: Boolean = false,
        var nativeTextSizePx: Float = 0f,
        var nativeTypeface: Typeface? = null,
        var nativeFontVariationSettings: String? = null,
        var nativeGravity: Int = Gravity.NO_GRAVITY,
        var nativeTextAlignment: Int = View.TEXT_ALIGNMENT_INHERIT,
        var nativePaddingStart: Int = 0,
        var nativePaddingTop: Int = 0,
        var nativePaddingEnd: Int = 0,
        var nativePaddingBottom: Int = 0,
        var nativeLineSpacingExtra: Float = 0f,
        var nativeLineSpacingMultiplier: Float = 1f,
        var nativeTranslationX: Float = 0f,
        var nativeTranslationY: Float = 0f,
        var nativeAlpha: Float = 1f,
        var sourceText: CharSequence? = null,
        var captured: Boolean = false,
    )

    private data class NetworkStats(
        val totalTx: Long,
        val totalRx: Long,
        val interfaces: Set<String>,
    )

    /**
     * Start from the exact Paint delivered to the hardware-proven status-bar
     * clock ReplacementSpan, then apply the same Typeface.create() weight path.
     * Do not force OpenType wght, alpha, color, bitmap morphology, stroke,
     * shadow or fake-bold synthesis.
     */
    private class NetworkSpeedLineSpan(
        private val weight: Int,
        private val sizeScale: Float,
    ) : ReplacementSpan() {
        private fun styledPaint(base: Paint): Paint {
            val clockDrawPaint = StatusBarClockDrawPaintBridge.currentPaint()
            return Paint(clockDrawPaint ?: base).apply {
                color = base.color
                alpha = base.alpha
                textSize = base.textSize * sizeScale.coerceIn(0.5f, 2f)
                shader = base.shader
                colorFilter = base.colorFilter
                val resolvedWeight = amplifiedNetworkWeight(weight)
                typeface = Typeface.create(
                    typeface ?: base.typeface ?: Typeface.DEFAULT,
                    resolvedWeight,
                    false,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Typeface.create() can collapse a static family to only
                    // regular/bold. A variable font exposes its real weight
                    // continuum through the OpenType wght axis. Keep the
                    // Typeface path as fallback and never synthesize weight.
                    val applied = runCatching {
                        setFontVariationSettings("'wght' $resolvedWeight")
                    }.getOrDefault(false)
                    if (!applied && resolvedWeight > 900) {
                        // Many variable fonts publish 100..900 even though
                        // Typeface accepts 1..1000. Preserve a useful maximum
                        // instead of silently falling back to the 500-ish face.
                        runCatching { setFontVariationSettings("'wght' 900") }
                    }
                }
            }
        }

        override fun getSize(
            paint: Paint,
            text: CharSequence,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?,
        ): Int {
            val styled = styledPaint(paint)
            fm?.let {
                val baseMetrics = paint.fontMetricsInt
                val styledMetrics = styled.fontMetricsInt
                it.top = minOf(baseMetrics.top, styledMetrics.top)
                it.ascent = minOf(baseMetrics.ascent, styledMetrics.ascent)
                it.descent = maxOf(baseMetrics.descent, styledMetrics.descent)
                it.bottom = maxOf(baseMetrics.bottom, styledMetrics.bottom)
                it.leading = maxOf(baseMetrics.leading, styledMetrics.leading)
            }
            return ceil(styled.measureText(text, start, end).toDouble()).toInt()
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint,
        ) {
            canvas.drawText(
                text,
                start,
                end,
                x,
                y.toFloat(),
                styledPaint(paint),
            )
        }
    }

    /**
     * Keep the UI at 100..900 in 100-point steps while widening the real
     * Typeface response exactly as hardware tuning requested. The visible 900
     * tier maps to Typeface weight 1000; there is no visible 1000 tier.
     *
     * Anchors: 100->100, 200->220, 300->330, 400->450, 500->560,
     * 600->670, 700->780, 800->890, 900->1000.
     *
     * This remains a pure numeric Typeface.create() mapping. Do not add fake
     * bold, stroke, alpha/color tricks, bitmap morphology or a forced wght axis.
     */
    private fun amplifiedNetworkWeight(weight: Int): Int {
        val nominal = weight.coerceIn(100, 900)
        return when (nominal) {
            in 100 until 200 -> 100 + ((nominal - 100) * 1.20f).roundToInt()
            in 200 until 300 -> 220 + ((nominal - 200) * 1.10f).roundToInt()
            in 300 until 400 -> 330 + ((nominal - 300) * 1.20f).roundToInt()
            in 400 until 500 -> 450 + ((nominal - 400) * 1.10f).roundToInt()
            in 500 until 600 -> 560 + ((nominal - 500) * 1.10f).roundToInt()
            in 600 until 700 -> 670 + ((nominal - 600) * 1.10f).roundToInt()
            in 700 until 800 -> 780 + ((nominal - 700) * 1.10f).roundToInt()
            in 800 until 900 -> 890 + ((nominal - 800) * 1.10f).roundToInt()
            else -> 1000
        }.coerceIn(100, 1000)
    }

    private val installed = AtomicBoolean(false)
    private val watcherStarted = AtomicBoolean(false)
    private val views = WeakHashMap<TextView, ViewState>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var config = RuntimeConfig()

    @Volatile
    private var lastNetworkStats = NetworkStats(0L, 0L, emptySet())

    @Volatile
    private var lastNetworkUpdateTime = 0L

    private var preferenceThread: HandlerThread? = null
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null
    private var keyguardManager: KeyguardManager? = null

    fun install(
        module: XposedModule,
        param: XposedModuleInterface.PackageReadyParam,
    ) {
        if (param.packageName != Package.SYSTEMUI) return
        if (!installed.compareAndSet(false, true)) return

        runCatching {
            with(module) { PreferenceProvider.loadPreference() }
                ?.systemUI?.statusBar
                ?.let { config = configFrom(it) }
        }


        installSeparateSpeedSource(module, param)

        runCatching {
            val clazz = param.classLoader.loadClass(NETSPEED_VIEW_CLASS)

            runCatching {
                val onFinishInflate = clazz.getDeclaredMethod("onFinishInflate")
                module.hook(onFinishInflate).intercept { chain ->
                    val result = chain.proceed()
                    contentView(chain.thisObject)?.let { view ->
                        val state = register(module, chain.thisObject as? View, view)
                        captureNative(view, state)
                        state.sourceText = normalizeSamsungText(view.text)
                        applyConfig(view, state)
                    }
                    result
                }
            }

            runCatching {
                val scaleView = clazz.getDeclaredMethod(
                    "scaleView",
                    Float::class.javaPrimitiveType,
                )
                module.hook(scaleView).intercept { chain ->
                    val before = contentView(chain.thisObject)
                    if (before != null) {
                        synchronized(views) { views[before] }?.let { state ->
                            restoreNativeVisual(before, state)
                        }
                    }

                    val result = chain.proceed()

                    contentView(chain.thisObject)?.let { view ->
                        val state = register(module, chain.thisObject as? View, view)
                        captureNative(view, state)
                        applyConfig(view, state)
                    }
                    result
                }
            }

            val update = clazz.getDeclaredMethod(
                "update",
                java.util.Observable::class.java,
                Any::class.java,
            )
            module.hook(update).intercept { chain ->
                val sourceFromObserver = chain.args.getOrNull(1) as? CharSequence
                val result = chain.proceed()
                contentView(chain.thisObject)?.let { view ->
                    val state = register(module, chain.thisObject as? View, view)
                    if (!state.captured) {
                        captureNative(view, state)
                    }
                    state.sourceText = normalizeSamsungText(
                        sourceFromObserver?.takeIf { it.isNotEmpty() } ?: view.text
                    )
                    applyConfig(view, state)
                }
                result
            }
        }
    }

    private fun installSeparateSpeedSource(
        module: XposedModule,
        param: XposedModuleInterface.PackageReadyParam,
    ) {
        runCatching {
            val controllerClass = "${NETSPEED_VIEW_CLASS}Controller"
            val handlerClass = param.classLoader.loadClass(
                "${controllerClass}\$NetworkSpeedManager\$1"
            )
            val handleMessage = handlerClass.getDeclaredMethod(
                "handleMessage",
                Message::class.java,
            )

            module.hook(handleMessage).intercept { chain ->
                if (!config.showSeparateUpDown) {
                    chain.proceed()
                } else {
                    runCatching {
                        val message = chain.args[0] as Message
                        val handler = chain.thisObject as Handler
                        val observable =
                            chain.thisObject.reflect["this$0"] as? java.util.Observable

                        if (observable == null || observable.countObservers() <= 0) {
                            return@runCatching null
                        }

                        when (message.what) {
                            MESSAGE_INITIAL -> {
                                resetSeparateSpeedBaseline()
                                lastNetworkStats = getCurrentNetworkStats()
                                lastNetworkUpdateTime = SystemClock.elapsedRealtime()
                                handler.scheduleNextSeparateUpdate()
                            }

                            MESSAGE_UPDATE -> {
                                val current = getCurrentNetworkStats()
                                val now = SystemClock.elapsedRealtime()
                                val previous = lastNetworkStats
                                val previousTime = lastNetworkUpdateTime

                                if (previousTime <= 0L) {
                                    lastNetworkStats = current
                                    lastNetworkUpdateTime = now
                                    handler.scheduleNextSeparateUpdate()
                                    return@runCatching null
                                }

                                if (current.interfaces == previous.interfaces &&
                                    current.totalTx >= previous.totalTx &&
                                    current.totalRx >= previous.totalRx &&
                                    now > previousTime
                                ) {
                                    val intervalSeconds = (now - previousTime) / 1000f
                                    val txBytesPerSecond =
                                        (current.totalTx - previous.totalTx) / intervalSeconds
                                    val rxBytesPerSecond =
                                        (current.totalRx - previous.totalRx) / intervalSeconds
                                    val speedString = buildString {
                                        append(formatSpeed(txBytesPerSecond))
                                        append('\n')
                                        append(formatSpeed(rxBytesPerSecond))
                                    }
                                    observable.reflect.call("setChanged")
                                    observable.notifyObservers(speedString)
                                }

                                lastNetworkStats = current
                                lastNetworkUpdateTime = now
                                handler.scheduleNextSeparateUpdate()
                            }

                            else -> return@runCatching chain.proceed()
                        }

                        null
                    }.getOrElse {
                        chain.proceed()
                    }
                }
            }
        }
    }

    private fun Handler.scheduleNextSeparateUpdate() {
        removeMessages(MESSAGE_UPDATE)
        sendEmptyMessageDelayed(MESSAGE_UPDATE, NETWORK_SPEED_INTERVAL_MS)
    }

    private fun resetSeparateSpeedBaseline() {
        lastNetworkStats = NetworkStats(0L, 0L, emptySet())
        lastNetworkUpdateTime = 0L
    }

    private fun getCurrentNetworkStats(): NetworkStats {
        var totalTx = 0L
        var totalRx = 0L
        val validInterfaces = mutableSetOf<String>()
        runCatching {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (networkInterface.isUp &&
                    !networkInterface.isVirtual &&
                    !networkInterface.isLoopback &&
                    !networkInterface.name.startsWith("tun") &&
                    !networkInterface.name.startsWith("dummy")
                ) {
                    totalTx += TrafficStats.getTxBytes(networkInterface.name)
                    totalRx += TrafficStats.getRxBytes(networkInterface.name)
                    validInterfaces.add(networkInterface.name)
                }
            }
        }
        return NetworkStats(totalTx, totalRx, validInterfaces)
    }

    private fun formatSpeed(bytesPerSecond: Float): String {
        if (bytesPerSecond <= 0f) return "0B"
        if (bytesPerSecond < 1024f) return "${bytesPerSecond.roundToInt()}B"
        val kib = bytesPerSecond / 1024f
        if (kib < 100f) return "%.2fKB".format(kib)
        if (kib < 1000f) return "%.1fKB".format(kib)
        val mib = kib / 1024f
        if (mib < 100f) return "%.2fMB".format(mib)
        return "%.1fMB".format(mib)
    }

    private fun contentView(owner: Any?): TextView? = runCatching {
        owner?.reflect?.get("mContentView") as? TextView
    }.getOrNull()

    private fun register(
        module: XposedModule,
        host: View?,
        view: TextView,
    ): ViewState {
        val state = synchronized(views) {
            views[view] ?: ViewState().also { views[view] = it }
        }
        if (state.host == null && host != null) state.host = host
        ensurePreferenceWatcher(module, view.context)
        return state
    }

    private fun captureNative(
        view: TextView,
        state: ViewState,
    ) {
        state.nativeTextSizePx = view.textSize
        state.nativeTypeface = view.typeface
        state.nativeFontVariationSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.fontVariationSettings
        } else {
            null
        }
        state.nativeGravity = view.gravity
        state.nativeTextAlignment = view.textAlignment
        state.nativePaddingStart = view.paddingStart
        state.nativePaddingTop = view.paddingTop
        state.nativePaddingEnd = view.paddingEnd
        state.nativePaddingBottom = view.paddingBottom
        state.nativeLineSpacingExtra = view.lineSpacingExtra
        state.nativeLineSpacingMultiplier = view.lineSpacingMultiplier
        state.nativeTranslationX = view.translationX
        state.nativeTranslationY = view.translationY
        state.nativeAlpha = view.alpha
        state.captured = true
    }

    private fun restoreNativeVisual(view: TextView, state: ViewState) {
        if (!state.captured) return
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, state.nativeTextSizePx)
        view.typeface = state.nativeTypeface
        view.paint.typeface = state.nativeTypeface
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { view.fontVariationSettings = state.nativeFontVariationSettings }
        }
        view.gravity = state.nativeGravity
        view.textAlignment = state.nativeTextAlignment
        view.setPaddingRelative(
            state.nativePaddingStart,
            state.nativePaddingTop,
            state.nativePaddingEnd,
            state.nativePaddingBottom,
        )
        view.setLineSpacing(
            state.nativeLineSpacingExtra,
            state.nativeLineSpacingMultiplier,
        )
        view.translationX = state.nativeTranslationX
        view.translationY = state.nativeTranslationY
        view.alpha = state.nativeAlpha
    }

    /**
     * Keep the original NetspeedView margins and add a real trailing layout gap.
     * This replaces the old non-breaking-space suffix used to fake inter-icon
     * spacing in the displayed string. The text and line-spacing logic stay
     * independent from the host's layout spacing.
     */
    private fun applyHostTrailingGap(state: ViewState, showGap: Boolean) {
        val host = state.host ?: return
        val params = host.layoutParams as? ViewGroup.MarginLayoutParams ?: return

        if (!state.hostMarginsCaptured) {
            state.nativeHostMarginStart = params.marginStart
            state.nativeHostMarginEnd = params.marginEnd
            state.hostMarginsCaptured = true
        }

        val baseStart = state.nativeHostMarginStart ?: params.marginStart
        val baseEnd = state.nativeHostMarginEnd ?: params.marginEnd
        val density = host.resources.displayMetrics.density
        val gapPx = if (showGap) {
            (NETWORK_SPEED_TRAILING_GAP_DP * density).roundToInt()
        } else {
            0
        }
        val targetEnd = baseEnd + gapPx

        if (params.marginStart != baseStart || params.marginEnd != targetEnd) {
            params.marginStart = baseStart
            params.marginEnd = targetEnd
            host.layoutParams = params
            host.requestLayout()
            (host.parent as? View)?.requestLayout()
        }
    }

    private fun applyConfig(view: TextView, state: ViewState) {
        if (!state.captured) {
            captureNative(view, state)
        }

        val current = config
        val display = current.display
        restoreNativeVisual(view, state)

        val density = view.resources.displayMetrics.density
        val lineGapPx = if (current.modifyLineGap) {
            current.lineGapDp.coerceIn(-4f, 4f) * density
        } else {
            0f
        }
        view.setLineSpacing(
            state.nativeLineSpacingExtra + lineGapPx,
            state.nativeLineSpacingMultiplier,
        )

        if (display.enabled) {
            view.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                display.fontSizeSp.coerceIn(5f, 18f),
            )
            view.translationX =
                state.nativeTranslationX + display.horizontalOffsetDp.coerceIn(-40f, 40f) * density
            view.translationY =
                state.nativeTranslationY + display.verticalOffsetDp.coerceIn(-20f, 20f) * density

            if (display.alignRight) {
                val vertical = state.nativeGravity and Gravity.VERTICAL_GRAVITY_MASK
                view.gravity = vertical or Gravity.END
                view.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            }

            val locked = if (display.hideOnLockscreen) {
                runCatching { keyguardManager?.isKeyguardLocked == true }.getOrDefault(false)
            } else {
                false
            }
            view.alpha = if (locked) 0f else state.nativeAlpha
        }

        val source = state.sourceText ?: view.text
        val hiddenByThreshold = shouldHideForThreshold(source, current.thresholdKb)
        applyHostTrailingGap(
            state,
            showGap = !hiddenByThreshold && source.toString().isNotBlank(),
        )
        if (hiddenByThreshold) {
            view.text = ""
            StatusBarBurnInProtectionRuntime.unregisterOffsetTarget(view)
            return
        }

        view.text = buildDisplayText(source, current)
        if (display.enabled && view.text.isNotBlank()) {
            StatusBarBurnInProtectionRuntime.registerView(view)
        } else {
            StatusBarBurnInProtectionRuntime.unregisterOffsetTarget(view)
        }
    }

    private fun buildDisplayText(
        source: CharSequence,
        current: RuntimeConfig,
    ): CharSequence {
        val display = current.display
        val builder = SpannableStringBuilder(source.toString())

        if (display.enabled && (display.hideUnit || display.hideBs)) {
            val pattern = if (display.hideUnit) {
                Regex("(?i)[KMGT]?B(?:/s)?")
            } else {
                Regex("(?i)B(?:/s)?")
            }
            val matches = pattern.findAll(builder.toString()).toList()
            for (match in matches.asReversed()) {
                builder.delete(match.range.first, match.range.last + 1)
            }
            while (builder.isNotEmpty() && builder.last() == '\n') {
                builder.delete(builder.length - 1, builder.length)
            }
            while (builder.isNotEmpty() && builder.first() == '\n') {
                builder.delete(0, 1)
            }
        }

        if (!display.enabled) {
            val lineBreak = builder.indexOf("\n")
            if (lineBreak >= 0) {
                if (current.setUpperScale && lineBreak > 0) {
                    builder.setSpan(
                        RelativeSizeSpan(current.upperScale.coerceIn(0.5f, 2f)),
                        0,
                        lineBreak,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
                if (current.setLowerScale && lineBreak + 1 < builder.length) {
                    builder.setSpan(
                        RelativeSizeSpan(current.lowerScale.coerceIn(0.5f, 2f)),
                        lineBreak + 1,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }
            return builder
        }

        var lineStart = 0
        var lineIndex = 0
        while (lineStart <= builder.length) {
            val nextBreak = builder.indexOf("\n", lineStart)
            val lineEnd = if (nextBreak >= 0) nextBreak else builder.length
            if (lineEnd > lineStart) {
                val lineScale = when (lineIndex) {
                    0 -> if (current.setUpperScale) current.upperScale else 1f
                    else -> if (current.setLowerScale) current.lowerScale else 1f
                }
                builder.setSpan(
                    NetworkSpeedLineSpan(display.fontWeight, lineScale),
                    lineStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            if (nextBreak < 0) break
            lineStart = nextBreak + 1
            lineIndex += 1
        }

        return builder
    }

    private fun normalizeSamsungText(source: CharSequence): CharSequence {
        val raw = source.toString()
        val lines = raw.split('\n')
        if (lines.size < 2) return raw
        val unit = when (lines[1].trim()) {
            "K/s" -> "KB/s"
            "M/s" -> "MB/s"
            "G/s" -> "GB/s"
            else -> lines[1]
        }
        return buildString {
            append(lines[0])
            append('\n')
            append(unit)
            if (lines.size > 2) {
                append('\n')
                append(lines.drop(2).joinToString("\n"))
            }
        }
    }

    private fun shouldHideForThreshold(source: CharSequence, thresholdKb: Int): Boolean {
        if (thresholdKb <= 0) return false
        val lines = source.toString()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) return false

        if (lines.size >= 2) {
            val value = lines[0].toFloatOrNull()
            val unitOnly = lines[1].matches(Regex("(?i)[KMGT]?B?/s"))
            if (value != null && unitOnly) {
                val speedKb = valueToKb(value, lines[1])
                return speedKb != null && speedKb <= thresholdKb
            }
        }

        val parsed = lines.mapNotNull(::parseSpeedLineKb)
        return parsed.isNotEmpty() && parsed.all { it <= thresholdKb }
    }

    private fun parseSpeedLineKb(line: String): Float? {
        val match = Regex(
            "(?i)^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([KMGT]?B)?(?:/s)?\\s*$"
        ).matchEntire(line) ?: return null
        val value = match.groupValues[1].toFloatOrNull() ?: return null
        return valueToKb(value, match.groupValues[2])
    }

    private fun valueToKb(value: Float, unit: String): Float? {
        val normalized = unit.uppercase()
        return when {
            normalized.startsWith("T") -> value * 1024f * 1024f * 1024f
            normalized.startsWith("G") -> value * 1024f * 1024f
            normalized.startsWith("M") -> value * 1024f
            normalized.startsWith("K") -> value
            normalized.startsWith("B") || normalized.isEmpty() -> value / 1024f
            else -> null
        }
    }

    private fun configFrom(statusBar: Preference.SystemUI.StatusBar): RuntimeConfig = RuntimeConfig(
        display = statusBar.networkSpeedRuntime.copy(
            fontSizeSp = statusBar.networkSpeedRuntime.fontSizeSp.coerceIn(5f, 18f),
            fontWeight = statusBar.networkSpeedRuntime.fontWeight.coerceIn(100, 900),
            horizontalOffsetDp =
                statusBar.networkSpeedRuntime.horizontalOffsetDp.coerceIn(-40f, 40f),
            verticalOffsetDp =
                statusBar.networkSpeedRuntime.verticalOffsetDp.coerceIn(-20f, 20f),
        ),
        showSeparateUpDown = statusBar.showSeparateUpDownNetworkSpeeds,
        thresholdKb = statusBar.networkSpeedThresholdKb.coerceAtLeast(0),
        modifyLineGap = statusBar.modifyNetworkSpeedLineGap,
        lineGapDp = statusBar.networkSpeedLineGapDp.coerceIn(-4f, 4f),
        setUpperScale = statusBar.setNetworkSpeedUploadTextScale,
        upperScale = statusBar.networkSpeedUploadTextScale.coerceIn(0.5f, 2f),
        setLowerScale = statusBar.setNetworkSpeedDownloadTextScale,
        lowerScale = statusBar.networkSpeedDownloadTextScale.coerceIn(0.5f, 2f),
    )

    private fun updateRuntime(source: Preference.SystemUI.StatusBar) {
        val next = configFrom(source)
        if (config.showSeparateUpDown != next.showSeparateUpDown) {
            resetSeparateSpeedBaseline()
        }
        config = next
        mainHandler.post {
            val active = synchronized(views) { views.entries.toList() }
            active.forEach { (view, state) -> applyConfig(view, state) }
        }
    }

    private fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (!watcherStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext ?: context
        keyguardManager = appContext.getSystemService(KeyguardManager::class.java)

        val worker = HandlerThread("OneuiXPStatusNetworkSpeedPrefs").apply { start() }
        preferenceThread = worker
        val background = Handler(worker.looper)
        var previous: Preference.SystemUI.StatusBar? = null

        fun refreshOnce() {
            background.post {
                val current = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }
                        ?.systemUI?.statusBar
                }.getOrNull() ?: return@post
                if (current == previous) return@post
                previous = current
                mainHandler.post { updateRuntime(current) }
            }
        }

        val observer = object : ContentObserver(background) {
            override fun onChange(selfChange: Boolean) = refreshOnce()
            override fun onChange(selfChange: Boolean, uri: Uri?) = refreshOnce()
        }
        preferenceObserver = observer
        runCatching {
            appContext.contentResolver.registerContentObserver(
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
                    previous = statusBar
                    mainHandler.post { updateRuntime(statusBar) }
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
}
