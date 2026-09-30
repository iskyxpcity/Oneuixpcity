package io.github.soclear.oneuix.hook.systemui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.hook.util.PreferenceProvider
import io.github.soclear.oneuix.hook.util.afterAttach
import io.github.soclear.oneuix.hook.util.HookInstallTracker
import io.github.soclear.oneuix.hook.util.xlog
import java.util.WeakHashMap
import kotlin.math.roundToInt

/** Main-derived MOD behavior; upstream StatusBar owns only upstream hooks. */
@SuppressLint("PrivateApi", "DiscouragedApi")
object OfficialMainStatusBarMod {
    private data class DoubleLineClockStyle(
        val timeScale: Float,
        val dateScale: Float,
        val lineSpacing: Float,
        val opticalTranslationYDp: Float,
    )

    private data class SingleLineClockLayout(
        val height: Int?,
        val parentGravity: Int?,
        val gravity: Int,
        val includeFontPadding: Boolean,
        val paddingTop: Int,
        val paddingBottom: Int,
    )

    private val singleLineClockLayouts = WeakHashMap<TextView, SingleLineClockLayout>()
    private val doubleLineClockViews = WeakHashMap<TextView, Unit>()

    private data class DoubleLineClockRuntimeConfig(
        val style: DoubleLineClockStyle,
        val extraLineGapDp: Float,
    )

    @Volatile
    private var doubleLineClockRuntimeConfig: DoubleLineClockRuntimeConfig? = null

    private val statusBarOriginalTranslations = WeakHashMap<View, Float>()
    private val statusBarViews = WeakHashMap<View, Unit>()

    private data class StatusBarVerticalOffset(
        val topDp: Float,
        val bottomDp: Float,
    )

    @Volatile
    private var statusBarVerticalOffset = StatusBarVerticalOffset(0f, 0f)

    private fun View.findStatusBarArea(vararg resourceNames: String): View? {
        resourceNames.forEach { resourceName ->
            val id = resources.getIdentifier(resourceName, "id", Package.SYSTEMUI)
            if (id != 0) {
                findViewById<View>(id)?.let { return it }
            }
        }
        return null
    }

    private fun updateStatusBarVerticalOffset(topDp: Float, bottomDp: Float) {
        statusBarVerticalOffset = StatusBarVerticalOffset(topDp, bottomDp)
        val activeStatusBarViews = synchronized(statusBarViews) {
            statusBarViews.keys.toList()
        }
        activeStatusBarViews.forEach { statusBarView ->
            statusBarView.post {
                applyStatusBarVerticalOffset(statusBarView)
            }
        }
    }

    private fun registerStatusBarView(statusBarView: View) {
        val shouldAddLayoutListener = synchronized(statusBarViews) {
            statusBarViews.put(statusBarView, Unit) == null
        }
        if (shouldAddLayoutListener) {
            statusBarView.addOnLayoutChangeListener(
                View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    applyStatusBarVerticalOffset(view)
                }
            )
        }
        statusBarView.post {
            applyStatusBarVerticalOffset(statusBarView)
        }
    }

    private fun applyStatusBarVerticalOffset(statusBarView: View) {
        val offset = statusBarVerticalOffset
        val density = statusBarView.resources.displayMetrics.density
        val topPx = (offset.topDp.coerceIn(0f, 8f) * density).roundToInt()
        val bottomPx = (offset.bottomDp.coerceIn(0f, 8f) * density).roundToInt()
        // Samsung has kept PhoneStatusBarView across One UI releases, but the
        // internal left/right area IDs vary by device and firmware. Resolve one
        // container per side so nested aliases cannot receive the offset twice.
        val leftArea = statusBarView.findStatusBarArea(
            "status_bar_left_side",
            "status_bar_start_side",
            "status_bar_start_side_content",
            "status_bar_left_container",
        )
        val rightArea = statusBarView.findStatusBarArea(
            "system_icon_area",
            "status_bar_right_side",
            "status_bar_end_side",
            "status_bar_end_side_content",
            "status_icon_area",
        )
        val leftAndRightContainers = listOfNotNull(leftArea, rightArea).distinct()
        val targets = leftAndRightContainers.ifEmpty { listOf(statusBarView) }
        targets.forEach { target ->
            val originalTranslationY = statusBarOriginalTranslations.getOrPut(target) {
                target.translationY
            }
            // Padding inside fixed-height indicator containers is often ignored by
            // their child layout. Translation works across Samsung screen sizes and
            // One UI layouts: top moves both areas down, bottom moves both areas up.
            target.translationY = originalTranslationY + topPx - bottomPx
        }
    }

    private fun doubleLineClockStyle(
        persistedSize: String,
        legacyPresetScale: Float,
        useFold7CustomScale: Boolean,
        fold7TimeScale: Float,
        fold7DateScale: Float,
    ): DoubleLineClockStyle {
        // V10 stored the five choices in the old floating-point clock-scale setting.
        // Preserve that selection until the user picks a V11 string-backed choice.
        val size = when (persistedSize) {
            "small", "compact", "standard", "large", "extra_large" -> persistedSize
            else -> when {
                legacyPresetScale < 0.925f -> "small"
                legacyPresetScale < 0.975f -> "compact"
                legacyPresetScale < 1.025f -> "standard"
                legacyPresetScale < 1.075f -> "large"
                else -> "extra_large"
            }
        }
        // Fold7 keeps a one-line-height status-bar viewport, while its raw Clock
        // text size is larger than on the slab Galaxy models. The dedicated
        // default preserves the full time size and renders the date at 80%.
        // Custom values are explicit user choices, never height-based fitting.
        if (Build.MODEL.startsWith("SM-F966", ignoreCase = true)) {
            val timeScale = if (useFold7CustomScale) {
                fold7TimeScale.coerceIn(0.50f, 1.10f)
            } else 1.00f
            val dateScale = if (useFold7CustomScale) {
                fold7DateScale.coerceIn(0.50f, 1.10f)
            } else 1.00f
            return DoubleLineClockStyle(timeScale, dateScale, 0.66f, -0.85f)
        }
        return when (size) {
            "small" -> DoubleLineClockStyle(0.74f, 0.68f, 0.72f, -0.65f)
            "compact" -> DoubleLineClockStyle(0.78f, 0.72f, 0.69f, -0.65f)
            "large" -> DoubleLineClockStyle(0.86f, 0.80f, 0.63f, -0.65f)
            "extra_large" -> DoubleLineClockStyle(0.90f, 0.84f, 0.60f, -0.65f)
            else -> DoubleLineClockStyle(0.82f, 0.76f, 0.66f, -0.65f)
        }
    }

    private fun updateDoubleLineClockRuntimeConfig(
        persistedSize: String,
        legacyPresetScale: Float,
        extraLineGapDp: Float,
        useFold7CustomScale: Boolean,
        fold7TimeScale: Float,
        fold7DateScale: Float,
    ) {
        val runtimeConfig = DoubleLineClockRuntimeConfig(
            style = doubleLineClockStyle(
                persistedSize,
                legacyPresetScale,
                useFold7CustomScale,
                fold7TimeScale,
                fold7DateScale,
            ),
            extraLineGapDp = extraLineGapDp.coerceIn(0f, 2f),
        )
        doubleLineClockRuntimeConfig = runtimeConfig
        val activeClockViews = synchronized(doubleLineClockViews) {
            doubleLineClockViews.keys.toList()
        }
        activeClockViews.forEach { clockTextView ->
            clockTextView.post {
                val clockText = clockTextView.text ?: return@post
                if (clockText.contains('\n')) {
                    applyDoubleLineClockText(clockTextView, clockText, runtimeConfig)
                }
            }
        }
    }

    private fun registerDoubleLineClockView(clockTextView: TextView) {
        synchronized(doubleLineClockViews) {
            doubleLineClockViews[clockTextView] = Unit
        }
    }

    /** Event-driven preference refresh. No permanent timer runs while settings are idle. */
    private const val PREFERENCE_CHANGED_URI =
        "content://com.oneuixp.app.statusbridge/preferences"
    private var refreshStarted = false
    private var preferenceObserver: ContentObserver? = null
    private var runtimeReceiver: BroadcastReceiver? = null

    private fun applyRuntimeStatusBarPreference(current: Preference.SystemUI.StatusBar) {
        updateStatusBarVerticalOffset(
            if (current.modifyStatusBarTopPadding) current.statusBarTopPaddingDp else 0f,
            if (current.modifyStatusBarBottomPadding) current.statusBarBottomPaddingDp else 0f,
        )
        if (current.setStatusBarClockFormat && current.statusBarClockFormat.contains('\n')) {
            updateDoubleLineClockRuntimeConfig(
                current.statusBarDoubleLineClockSize,
                current.statusBarClockTextScale,
                if (current.modifyDoubleLineClockGap) current.doubleLineClockGapDp else 1f,
                current.useFold7CustomDoubleLineClockScale,
                current.fold7DoubleLineClockTimeScale,
                current.fold7DoubleLineClockDateScale,
            )
        }
        StatusBar.updateStatusBarClockRuntime(current)
        StatusBar.setStatusBarClockSecondRuntimeEnabled(
            current.updateStatusBarClockEverySecond
        )
    }

    fun ensurePreferenceWatcher(module: XposedModule, context: Context) {
        if (refreshStarted) return
        refreshStarted = true

        val worker = HandlerThread("OneUIXModStatusBarPrefs").apply { start() }
        val background = Handler(worker.looper)
        val main = Handler(Looper.getMainLooper())
        var previous: Preference.SystemUI.StatusBar? = null

        fun refreshOnce() {
            background.post {
                val current = runCatching {
                    with(module) { PreferenceProvider.loadPreference() }
                        ?.systemUI?.statusBar
                }.getOrNull() ?: return@post

                if (current == previous) return@post
                previous = current
                main.post { applyRuntimeStatusBarPreference(current) }
            }
        }

        val observer = object : ContentObserver(background) {
            override fun onChange(selfChange: Boolean) {
                refreshOnce()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                refreshOnce()
            }
        }
        preferenceObserver = observer
        runCatching {
            context.contentResolver.registerContentObserver(
                Uri.parse(PREFERENCE_CHANGED_URI),
                false,
                observer,
            )
        }.onFailure {
            with(module) { xlog(it) }
        }

        // High-frequency previews use an in-memory broadcast payload instead of
        // rewriting preference.json for every slider/eyedropper movement.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != StatusBarRuntimeBridge.ACTION) return
                val payload =
                    intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON)
                        ?: return
                background.post {
                    val current = runCatching {
                        IgnoreUnknownKeysJson.decodeFromString(
                            Preference.SystemUI.StatusBar.serializer(),
                            payload,
                        )
                    }.getOrNull() ?: return@post

                    previous = current
                    main.post { applyRuntimeStatusBarPreference(current) }
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
        }.onFailure {
            with(module) { xlog(it) }
        }

        // Apply the current persisted state once when the watcher is installed.
        refreshOnce()
    }

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun installVerticalPadding(topDp: Float, bottomDp: Float) {
        if (param.packageName != Package.SYSTEMUI) return
        updateStatusBarVerticalOffset(topDp, bottomDp)
        afterAttach {
            try {
                val clazz = classLoader.loadClass(
                    "com.android.systemui.statusbar.phone.PhoneStatusBarView"
                )
                clazz.declaredConstructors.forEach { constructor ->
                    xposedModule.hook(constructor).intercept { chain ->
                        val result = chain.proceed()
                        (chain.thisObject as? View)?.let { statusBarView ->
                            registerStatusBarView(statusBarView)
                            ensurePreferenceWatcher(xposedModule, statusBarView.context)
                        }
                        result
                    }
                }
                HookInstallTracker.markSuccessful("systemui.statusbar.vertical-runtime")
            } catch (t: Throwable) {
                xlog(t)
            }
        }
    }

    fun configureClock(
        preset: String,
        legacyScale: Float,
        gapDp: Float,
        fold7Custom: Boolean,
        upperScale: Float,
        lowerScale: Float,
    ) {
        updateDoubleLineClockRuntimeConfig(
            preset, legacyScale, gapDp, fold7Custom, upperScale, lowerScale
        )
    }

    private fun applyDoubleLineClockText(
        clockTextView: TextView,
        dateTime: CharSequence,
        runtimeConfig: DoubleLineClockRuntimeConfig,
    ) {
        val plainText = dateTime.toString()
        val firstLineEnd = plainText.indexOf('\n')
        if (firstLineEnd < 0) return
        val doubleLineClockStyle = runtimeConfig.style
        singleLineClockLayouts.getOrPut(clockTextView) {
            val layoutParams = clockTextView.layoutParams
            SingleLineClockLayout(
                height = layoutParams?.height,
                parentGravity = (clockTextView.parent as? LinearLayout)?.gravity,
                gravity = clockTextView.gravity,
                includeFontPadding = clockTextView.includeFontPadding,
                paddingTop = clockTextView.paddingTop,
                paddingBottom = clockTextView.paddingBottom,
            )
        }

        clockTextView.layoutParams?.let { params ->
            params.height = 94
            clockTextView.layoutParams = params
        }
        (clockTextView.parent as? LinearLayout)?.gravity = Gravity.CENTER_VERTICAL
        clockTextView.gravity =
            (clockTextView.gravity and Gravity.VERTICAL_GRAVITY_MASK.inv()) or
                Gravity.CENTER_VERTICAL
        clockTextView.isSingleLine = false
        clockTextView.maxLines = 2
        clockTextView.minLines = 2
        clockTextView.includeFontPadding = false
        clockTextView.ellipsize = null
        clockTextView.setHorizontallyScrolling(false)
        val density = clockTextView.resources.displayMetrics.density
        clockTextView.setPaddingRelative(
            clockTextView.paddingStart,
            0,
            clockTextView.paddingEnd,
            0
        )
        val extraLineGapPx = runtimeConfig.extraLineGapDp * density
        clockTextView.setLineSpacing(extraLineGapPx, doubleLineClockStyle.lineSpacing)
        clockTextView.text = SpannableString(dateTime).apply {
            if (firstLineEnd > 0) {
                setSpan(
                    RelativeSizeSpan(doubleLineClockStyle.timeScale),
                    0,
                    firstLineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (firstLineEnd + 1 < length) {
                setSpan(
                    RelativeSizeSpan(doubleLineClockStyle.dateScale),
                    firstLineEnd + 1,
                    length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        clockTextView.contentDescription = plainText.replace('\n', ' ')
        clockTextView.requestLayout()
        clockTextView.invalidate()
    }

    fun applyDoubleLineClockText(clockTextView: TextView, dateTime: CharSequence) {
        registerDoubleLineClockView(clockTextView)
        val config = doubleLineClockRuntimeConfig ?: return
        applyDoubleLineClockText(clockTextView, dateTime, config)
    }

    /** Exactly restore the production main's stored one-line layout state. */
    fun restoreSingleLineClock(clockTextView: TextView, dateTime: CharSequence) {
        synchronized(doubleLineClockViews) { doubleLineClockViews.remove(clockTextView) }
        singleLineClockLayouts.remove(clockTextView)?.let { original ->
            clockTextView.layoutParams?.let { params ->
                original.height?.let { params.height = it }
                clockTextView.layoutParams = params
            }
            (clockTextView.parent as? LinearLayout)?.let { parent ->
                original.parentGravity?.let { parent.gravity = it }
            }
            clockTextView.gravity = original.gravity
            clockTextView.includeFontPadding = original.includeFontPadding
            clockTextView.setPaddingRelative(
                clockTextView.paddingStart,
                original.paddingTop,
                clockTextView.paddingEnd,
                original.paddingBottom
            )
        }
        clockTextView.isSingleLine = true
        clockTextView.maxLines = 1
        clockTextView.minLines = 1
        clockTextView.setLineSpacing(0f, 1f)
        clockTextView.setHorizontallyScrolling(false)
        clockTextView.translationY = 0f
        clockTextView.text = dateTime
        clockTextView.contentDescription = dateTime.toString().replace('\n', ' ')
    }
}
