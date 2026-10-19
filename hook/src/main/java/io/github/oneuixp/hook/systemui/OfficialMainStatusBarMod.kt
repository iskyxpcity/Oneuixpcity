package io.github.oneuixp.hook.systemui

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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.oneuixp.common.IgnoreUnknownKeysJson
import io.github.oneuixp.common.Package
import io.github.oneuixp.common.Preference
import io.github.oneuixp.common.StatusBarRuntimeBridge
import io.github.oneuixp.hook.util.PreferenceProvider
import io.github.oneuixp.hook.util.afterAttach
import io.github.oneuixp.hook.util.HookInstallTracker
import io.github.oneuixp.hook.util.xlog
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
        val textAlignment: Int,
        val layoutGravity: Int?,
        val includeFontPadding: Boolean,
        val paddingTop: Int,
        val paddingBottom: Int,
    )

    private val singleLineClockLayouts = WeakHashMap<TextView, SingleLineClockLayout>()
    private val doubleLineClockViews = WeakHashMap<TextView, Unit>()
    private val doubleLineClockSlotLayouts = WeakHashMap<ViewGroup, Pair<Int, Int>>()
    private val doubleLineClockHostLayouts = WeakHashMap<ViewGroup, Pair<Int, Int>>()

    private data class DoubleLineClockRuntimeConfig(
        val style: DoubleLineClockStyle,
        val extraLineGapDp: Float,
    )

    @Volatile
    private var doubleLineClockRuntimeConfig: DoubleLineClockRuntimeConfig? = null

    private val statusBarOriginalTranslations = WeakHashMap<View, Float>()
    private val statusBarViews = WeakHashMap<View, Unit>()

    private data class StatusBarClockPositionRuntimeConfig(
        val enabled: Boolean,
        val offsetXDp: Float,
        val offsetYDp: Float,
        val leftMarginDp: Float,
        val rightMarginDp: Float,
        val alignment: String,
        val fixedContentWidthDp: Float,
    )

    @Volatile
    private var statusBarClockPositionRuntimeConfig =
        StatusBarClockPositionRuntimeConfig(false, 0f, 0f, 0f, 0f, "native", 0f)

    private val statusBarClockOriginalTranslations =
        WeakHashMap<TextView, Pair<Float, Float>>()
    private val statusBarClockAppliedMargins =
        WeakHashMap<TextView, Pair<Int, Int>>()
    private val statusBarClockOriginalAlignments =
        WeakHashMap<TextView, Int>()
    private val statusBarClockOriginalGravities =
        WeakHashMap<TextView, Int>()
    private val statusBarClockOriginalWidths =
        WeakHashMap<TextView, Int>()
    private val statusBarClockOriginalClipChildren =
        WeakHashMap<ViewGroup, Boolean>()
    private val statusBarClockOriginalClipToPadding =
        WeakHashMap<ViewGroup, Boolean>()
    private val statusBarClockPositionViews =
        WeakHashMap<TextView, Unit>()
    private val statusBarClockPositionListeners =
        WeakHashMap<TextView, Unit>()

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

    private fun rememberStatusBarClockPositionView(clockTextView: TextView) {
        synchronized(statusBarClockPositionViews) {
            statusBarClockPositionViews[clockTextView] = Unit
        }
        val installListener = synchronized(statusBarClockPositionListeners) {
            statusBarClockPositionListeners.put(clockTextView, Unit) == null
        }
        if (installListener) {
            clockTextView.addOnLayoutChangeListener(
                View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    if (view is TextView) {
                        applyStatusBarClockPosition(view)
                    }
                }
            )
        }
    }

    private fun relaxStatusBarClockPositionClipping(
        clockTextView: TextView,
        enabled: Boolean,
    ) {
        var parent = clockTextView.parent as? ViewGroup
        while (parent != null) {
            synchronized(statusBarClockOriginalClipChildren) {
                statusBarClockOriginalClipChildren.getOrPut(parent) {
                    parent.clipChildren
                }
            }
            synchronized(statusBarClockOriginalClipToPadding) {
                statusBarClockOriginalClipToPadding.getOrPut(parent) {
                    parent.clipToPadding
                }
            }

            if (enabled) {
                parent.clipChildren = false
                parent.clipToPadding = false
            } else {
                val originalChildren = synchronized(statusBarClockOriginalClipChildren) {
                    statusBarClockOriginalClipChildren[parent]
                }
                val originalPadding = synchronized(statusBarClockOriginalClipToPadding) {
                    statusBarClockOriginalClipToPadding[parent]
                }
                if (originalChildren != null) {
                    parent.clipChildren = originalChildren
                }
                if (originalPadding != null) {
                    parent.clipToPadding = originalPadding
                }
            }

            if (parent.javaClass.name ==
                "com.android.systemui.statusbar.phone.PhoneStatusBarView"
            ) {
                break
            }
            parent = parent.parent as? ViewGroup
        }
    }

    private fun applyStatusBarClockPosition(
        clockTextView: TextView,
        config: StatusBarClockPositionRuntimeConfig =
            statusBarClockPositionRuntimeConfig,
        forceAlignmentRefresh: Boolean = false,
    ) {
        relaxStatusBarClockPositionClipping(clockTextView, config.enabled)

        val originalTranslation = synchronized(statusBarClockOriginalTranslations) {
            statusBarClockOriginalTranslations.getOrPut(clockTextView) {
                clockTextView.translationX to clockTextView.translationY
            }
        }

        val density = clockTextView.resources.displayMetrics.density
        val nextX = if (config.enabled) {
            config.offsetXDp.coerceIn(-20f, 20f) * density
        } else 0f
        val nextY = if (config.enabled) {
            config.offsetYDp.coerceIn(-20f, 20f) * density
        } else 0f

        // Always apply the offset from the original Samsung translation. Reconstructing
        // the base from the current translation loses the offset if Samsung resets
        // translation during a native layout pass, causing the clock to jump back.
        val desiredTranslationX = originalTranslation.first + nextX
        val desiredTranslationY = originalTranslation.second + nextY
        if (kotlin.math.abs(clockTextView.translationX - desiredTranslationX) > 0.01f) {
            clockTextView.translationX = desiredTranslationX
        }
        if (kotlin.math.abs(clockTextView.translationY - desiredTranslationY) > 0.01f) {
            clockTextView.translationY = desiredTranslationY
        }

        val originalAlignment = synchronized(statusBarClockOriginalAlignments) {
            statusBarClockOriginalAlignments.getOrPut(clockTextView) {
                clockTextView.textAlignment
            }
        }
        val originalGravity = synchronized(statusBarClockOriginalGravities) {
            statusBarClockOriginalGravities.getOrPut(clockTextView) {
                clockTextView.gravity
            }
        }
        val selectedAlignment = if (config.enabled) config.alignment else "native"
        val desiredAlignment = when (selectedAlignment) {
            "start" -> View.TEXT_ALIGNMENT_TEXT_START
            "center" -> View.TEXT_ALIGNMENT_CENTER
            "end" -> View.TEXT_ALIGNMENT_TEXT_END
            else -> originalAlignment
        }
        val desiredGravity = when (selectedAlignment) {
            "start" ->
                (originalGravity and Gravity.VERTICAL_GRAVITY_MASK) or Gravity.START
            "center" ->
                (originalGravity and Gravity.VERTICAL_GRAVITY_MASK) or Gravity.CENTER_HORIZONTAL
            "end" ->
                (originalGravity and Gravity.VERTICAL_GRAVITY_MASK) or Gravity.END
            else -> originalGravity
        }
        var alignmentChanged = false
        if (clockTextView.textAlignment != desiredAlignment) {
            clockTextView.textAlignment = desiredAlignment
            alignmentChanged = true
        }
        // On Samsung SystemUI the clock can be measured by a native host whose
        // own gravity governs positioning. Set both the TextView's text alignment
        // and horizontal gravity so a newly selected option is visible immediately.
        if (clockTextView.gravity != desiredGravity) {
            clockTextView.gravity = desiredGravity
            alignmentChanged = true
        }
        val params = clockTextView.layoutParams
        if (params != null) {
            val originalWidth = synchronized(statusBarClockOriginalWidths) {
                statusBarClockOriginalWidths.getOrPut(clockTextView) {
                    params.width
                }
            }
            val requestedWidth = if (config.enabled && config.fixedContentWidthDp > 0f) {
                (config.fixedContentWidthDp.coerceIn(1f, 200f) * density).roundToInt()
            } else {
                originalWidth
            }
            if (params.width != requestedWidth) {
                params.width = requestedWidth
                clockTextView.layoutParams = params
            }
        }
        if (params is ViewGroup.MarginLayoutParams) {
            val previousMargins = synchronized(statusBarClockAppliedMargins) {
                statusBarClockAppliedMargins[clockTextView] ?: (0 to 0)
            }
            val baseLeft = params.leftMargin - previousMargins.first
            val baseRight = params.rightMargin - previousMargins.second
            val nextLeft = if (config.enabled) {
                (config.leftMarginDp.coerceIn(0f, 40f) * density).roundToInt()
            } else 0
            val nextRight = if (config.enabled) {
                (config.rightMarginDp.coerceIn(0f, 40f) * density).roundToInt()
            } else 0
            val desiredLeft = baseLeft + nextLeft
            val desiredRight = baseRight + nextRight
            var changed = false
            if (params.leftMargin != desiredLeft) {
                params.leftMargin = desiredLeft
                changed = true
            }
            if (params.rightMargin != desiredRight) {
                params.rightMargin = desiredRight
                changed = true
            }
            if (changed) {
                clockTextView.layoutParams = params
            }
            synchronized(statusBarClockAppliedMargins) {
                if (config.enabled) {
                    statusBarClockAppliedMargins[clockTextView] = nextLeft to nextRight
                } else {
                    statusBarClockAppliedMargins.remove(clockTextView)
                }
            }
        }
        if (alignmentChanged || forceAlignmentRefresh) {
            // Ask the TextView and its native host to remeasure on this frame.
            clockTextView.requestLayout()
            (clockTextView.parent as? View)?.requestLayout()
            clockTextView.invalidate()

            // Samsung's clock host can finish a pending native layout after the
            // preference broadcast was handled. Re-apply the latest selection once
            // on the next animation frame so that host layout cannot leave the
            // previous alignment visible until another settings interaction.
            clockTextView.postOnAnimation {
                if (clockTextView.isAttachedToWindow) {
                    applyStatusBarClockPosition(
                        clockTextView,
                        statusBarClockPositionRuntimeConfig,
                    )
                    clockTextView.requestLayout()
                    (clockTextView.parent as? View)?.requestLayout()
                    clockTextView.invalidate()
                }
            }
        }
    }

    fun applyStatusBarClockPosition(clockTextView: TextView) {
        rememberStatusBarClockPositionView(clockTextView)
        applyStatusBarClockPosition(
            clockTextView,
            statusBarClockPositionRuntimeConfig,
        )
    }

    fun updateStatusBarClockPositionRuntime(
        enabled: Boolean,
        offsetXDp: Float,
        offsetYDp: Float,
        leftMarginDp: Float = 0f,
        rightMarginDp: Float = 0f,
        alignment: String = "native",
        fixedContentWidthDp: Float = 0f,
    ) {
        val previousConfig = statusBarClockPositionRuntimeConfig
        val nextConfig = StatusBarClockPositionRuntimeConfig(
            enabled = enabled,
            offsetXDp = offsetXDp,
            offsetYDp = offsetYDp,
            leftMarginDp = leftMarginDp,
            rightMarginDp = rightMarginDp,
            alignment = alignment,
            fixedContentWidthDp = fixedContentWidthDp,
        )
        val forceAlignmentRefresh =
            previousConfig.enabled != nextConfig.enabled ||
                previousConfig.alignment != nextConfig.alignment
        statusBarClockPositionRuntimeConfig = nextConfig
        val activeClockViews = synchronized(statusBarClockPositionViews) {
            statusBarClockPositionViews.keys.toList()
        }
        activeClockViews.forEach { clockTextView ->
            val apply = {
                applyStatusBarClockPosition(
                    clockTextView,
                    nextConfig,
                    forceAlignmentRefresh,
                )
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                apply()
            } else {
                Handler(Looper.getMainLooper()).post { apply() }
            }
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
            extraLineGapDp = extraLineGapDp.coerceIn(-3f, 3f),
        )
        // Position previews arrive much more frequently than typography changes.
        // A position-only update must not replace the clock text or request another
        // layout, otherwise the Samsung clock visibly blinks while the slider moves.
        if (doubleLineClockRuntimeConfig == runtimeConfig) return
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

    private val doubleLineClockAlignmentListeners = WeakHashMap<TextView, Unit>()
    private val doubleLineClockSlotLayoutListeners = WeakHashMap<ViewGroup, Unit>()

    private fun findFullStatusBarView(view: View): View? {
        var current: View? = view
        while (current != null) {
            if (current.javaClass.name == "com.android.systemui.statusbar.phone.PhoneStatusBarView") {
                return current
            }
            current = current.parent as? View
        }
        return null
    }

    /**
     * Samsung exposes dedicated clock slots on different One UI layouts.
     * Prefer those concrete containers over a guessed ancestor so only the
     * clock's own slot becomes full-height, exactly like the combo's root slot.
     */
    private fun isDescendantOf(view: View, ancestor: View): Boolean {
        var current = view.parent as? View
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent as? View
        }
        return false
    }

    /** The real vertical space host is the first ancestor directly under PhoneStatusBarView. */
    fun findDoubleLineClockSpatialHost(clockTextView: TextView): ViewGroup? {
        val statusBarView = findFullStatusBarView(clockTextView) as? ViewGroup ?: return null
        return generateSequence(clockTextView.parent as? View) { it.parent as? View }
            .filterIsInstance<ViewGroup>()
            .firstOrNull { it.parent === statusBarView }
    }

    private fun findDoubleLineClockSlot(
        clockTextView: TextView,
        statusBarView: View,
    ): ViewGroup? {
        val resourceNames = listOf(
            "left_clock_container",
            "clock",
        )
        resourceNames.forEach { resourceName ->
            val id = statusBarView.resources.getIdentifier(
                resourceName,
                "id",
                Package.SYSTEMUI,
            )
            if (id == 0) return@forEach

            val candidate = statusBarView.findViewById<View>(id) ?: return@forEach
            if (candidate === clockTextView) {
                (candidate.parent as? ViewGroup)?.let { return it }
            }
            if (candidate is ViewGroup) {
                if (candidate === clockTextView ||
                    isDescendantOf(clockTextView, candidate)
                ) {
                    return candidate
                }
            }
        }

        return generateSequence(clockTextView.parent as? View) { it.parent as? View }
            .filterIsInstance<ViewGroup>()
            .firstOrNull { ancestor ->
                ancestor.parent === statusBarView
            }
    }

    /**
     * Use the same two-layer space model as the three-in-one icon: the existing
     * Samsung clock slot is the spatial host, while the clock TextView remains
     * WRAP_CONTENT and grows with the real two-line text height.
     */
    /**
     * The Samsung clock TextView lives inside left_clock_container, whose stock
     * measured height is only the normal one-line status-bar band. The combo icon
     * avoids this problem by having a full-height host and a wrap-content child.
     * Reproduce that space model on Samsung's existing clock slot without
     * reparenting the native clock: the slot is measured/layouted at the real
     * PhoneStatusBarView height, while the clock remains WRAP_CONTENT.
     */
    private fun ensureDoubleLineClockFullHeightSlot(
        clockTextView: TextView,
    ) {
        val statusBarView = findFullStatusBarView(clockTextView) ?: return
        val slot = findDoubleLineClockSlot(clockTextView, statusBarView) ?: return
        val fullHeight = doubleLineClockSlotHeight(clockTextView)
        if (fullHeight <= 0) return

        // The Samsung status-bar content row may be only 58-70px high. Do not
        // allow that row to clip the expanded clock slot or any of its ancestors.
        var ancestor: View? = slot.parent as? View
        while (ancestor != null && ancestor !== statusBarView) {
            (ancestor as? ViewGroup)?.let {
                it.clipChildren = false
                it.clipToPadding = false
            }
            ancestor = ancestor.parent as? View
        }
        slot.clipChildren = false
        slot.clipToPadding = false
        (statusBarView as? ViewGroup)?.let {
            it.clipChildren = false
            it.clipToPadding = false
        }

        // The native Samsung clock slot is the full-height spatial box.
        // The TextView fills that slot, while glyphs remain naturally sized.
        // No overlay and no reparenting are used.
        synchronized(doubleLineClockSlotLayouts) {
            if (!doubleLineClockSlotLayouts.containsKey(slot)) {
                val originalParams = slot.layoutParams
                val originalHeight = originalParams?.height
                    ?: ViewGroup.LayoutParams.WRAP_CONTENT
                val originalGravity = when (originalParams) {
                    is LinearLayout.LayoutParams -> originalParams.gravity
                    is FrameLayout.LayoutParams -> originalParams.gravity
                    else -> Gravity.NO_GRAVITY
                }
                doubleLineClockSlotLayouts[slot] = Pair(originalHeight, originalGravity)
            }
        }
        // One UI 8.5's punch-hole IndicatorGarden can give the clock slot's
        // immediate host a cutout-sized measured height. Expanding only the child
        // slot cannot escape that parent measurement box. Promote the native clock
        // host one level as well, without reparenting or creating an Overlay.
        val host = slot.parent as? ViewGroup
        if (host != null && host !== statusBarView) {
            synchronized(doubleLineClockHostLayouts) {
                if (!doubleLineClockHostLayouts.containsKey(host)) {
                    val originalParams = host.layoutParams
                    val originalHeight = originalParams?.height
                        ?: ViewGroup.LayoutParams.WRAP_CONTENT
                    val originalGravity = when (originalParams) {
                        is LinearLayout.LayoutParams -> originalParams.gravity
                        is FrameLayout.LayoutParams -> originalParams.gravity
                        else -> Gravity.NO_GRAVITY
                    }
                    doubleLineClockHostLayouts[host] = Pair(originalHeight, originalGravity)
                }
            }
            host.layoutParams?.let { params ->
                var changed = false
                if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    changed = true
                }
                when (params) {
                    is LinearLayout.LayoutParams -> {
                        val desiredGravity = params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                        if (params.gravity != desiredGravity) {
                            params.gravity = desiredGravity
                            changed = true
                        }
                    }
                    is FrameLayout.LayoutParams -> {
                        val desiredGravity = params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                        if (params.gravity != desiredGravity) {
                            params.gravity = desiredGravity
                            changed = true
                        }
                    }
                }
                if (changed) host.layoutParams = params
            }
            host.clipChildren = false
            host.clipToPadding = false
        }

        slot.layoutParams?.let { params ->
            var changed = false
            if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                changed = true
            }
            when (params) {
                is LinearLayout.LayoutParams -> {
                    val desiredGravity = params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                    if (params.gravity != desiredGravity) {
                        params.gravity = desiredGravity
                        changed = true
                    }
                }
                is FrameLayout.LayoutParams -> {
                    val desiredGravity = params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                    if (params.gravity != desiredGravity) {
                        params.gravity = desiredGravity
                        changed = true
                    }
                }
            }
            if (changed) slot.layoutParams = params
        }

        clockTextView.layoutParams?.let { params ->
            val positionConfig = statusBarClockPositionRuntimeConfig
            val desiredWidth = if (
                positionConfig.enabled && positionConfig.fixedContentWidthDp > 0f
            ) {
                (positionConfig.fixedContentWidthDp.coerceIn(1f, 200f) *
                    clockTextView.resources.displayMetrics.density).roundToInt()
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
            var changed = false
            if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                changed = true
            }
            if (params.width != desiredWidth) {
                params.width = desiredWidth
                changed = true
            }
            if (changed) clockTextView.layoutParams = params
        }

        // Keep Samsung's native vertical gravity unchanged. The full-height slot
        // lets the native TextView lay out the two lines naturally.
        applyStatusBarClockPosition(clockTextView)

        slot.requestLayout()
        statusBarView.requestLayout()
        // End of the outer positioning post{}; the function itself closes below.
    }

    fun getDoubleLineClockSlot(
        clockTextView: TextView,
    ): ViewGroup? {
        val statusBarView = findFullStatusBarView(clockTextView) ?: return null
        return findDoubleLineClockSlot(clockTextView, statusBarView)
    }

    fun getDoubleLineClockHost(
        clockTextView: TextView,
    ): ViewGroup? =
        getDoubleLineClockSlot(clockTextView)?.parent as? ViewGroup


    fun isDoubleLineClockSlotHost(
        view: View,
        expectedId: Int = View.NO_ID,
    ): Boolean {
        val id = view.id
        if (expectedId != View.NO_ID) {
            return id == expectedId
        }
        if (id == View.NO_ID) return false
        val name = runCatching {
            view.resources.getResourceEntryName(id)
        }.getOrNull()
        return name == "left_clock_container" ||
            name == "middle_clock_container" ||
            name == "right_clock_container"
    }

    fun findDoubleLineClockDescendant(slot: View): TextView? {
        val group = slot as? ViewGroup ?: return null

        fun visit(parent: ViewGroup): TextView? {
            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)
                if (
                    child is TextView &&
                    child.text?.contains('\n') == true &&
                    child.maxLines == 2
                ) {
                    return child
                }
                if (child is ViewGroup) {
                    visit(child)?.let { return it }
                }
            }
            return null
        }

        return visit(group)
    }

    fun doubleLineClockSlotHeight(
        clockTextView: TextView,
    ): Int {
        val statusBarView = findFullStatusBarView(clockTextView) ?: return 0
        val slot = findDoubleLineClockSlot(clockTextView, statusBarView) ?: return 0
        val slotParentHeight = (slot.parent as? View)?.let {
            maxOf(it.height, it.measuredHeight)
        } ?: 0
        val resourceHeight = runCatching {
            val id = statusBarView.resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android",
            )
            if (id != 0) statusBarView.resources.getDimensionPixelSize(id) else 0
        }.getOrDefault(0)
        return maxOf(
            statusBarView.height,
            statusBarView.measuredHeight,
            slotParentHeight,
            resourceHeight,
        )
    }


    /**
     * Samsung can pass an EXACTLY one-line height to QSClockIndicatorView even
     * when the same TextView contains two lines. Convert that height constraint
     * to an AT_MOST constraint based on the real PhoneStatusBarView height so
     * TextView can measure the full two-line content naturally.
     */
    fun expandDoubleLineClockHeightMeasureSpec(
        clockTextView: TextView,
        originalHeightSpec: Int,
    ): Int {
        if (clockTextView.text?.contains('\n') != true || clockTextView.maxLines != 2) {
            return originalHeightSpec
        }
        val availableHeight = doubleLineClockSlotHeight(clockTextView)
        if (availableHeight <= 0) return originalHeightSpec

        val originalSize = View.MeasureSpec.getSize(originalHeightSpec)
        val allowedHeight = maxOf(availableHeight, originalSize)
        return View.MeasureSpec.makeMeasureSpec(
            allowedHeight,
            View.MeasureSpec.AT_MOST,
        )
    }

    private fun registerDoubleLineClockView(clockTextView: TextView) {
        synchronized(doubleLineClockViews) {
            doubleLineClockViews[clockTextView] = Unit
        }
        rememberStatusBarClockPositionView(clockTextView)
        val installListener = synchronized(doubleLineClockAlignmentListeners) {
            doubleLineClockAlignmentListeners.put(clockTextView, Unit) == null
        }
        if (installListener) {
            clockTextView.addOnLayoutChangeListener(
                View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                    val config = doubleLineClockRuntimeConfig
                    if (
                        view is TextView &&
                        config != null &&
                        view.text?.contains('\n') == true
                    ) {
                        ensureDoubleLineClockFullHeightSlot(view)
                    }
                }
            )
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
        updateStatusBarClockPositionRuntime(
            current.modifyStatusBarClockPosition,
            current.statusBarClockOffsetXDp,
            current.statusBarClockOffsetYDp,
            current.statusBarClockLeftMarginDp,
            current.statusBarClockRightMarginDp,
            current.statusBarClockAlignment,
            current.statusBarClockFixedContentWidthDp,
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

        val worker = HandlerThread("OneUIXPModStatusBarPrefs").apply { start() }
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
            private var pendingPayload: String? = null
            private var previewRefreshScheduled = false

            private val applyPreviewRunnable = Runnable {
                val latest = synchronized(this) {
                    previewRefreshScheduled = false
                    pendingPayload.also { pendingPayload = null }
                } ?: return@Runnable

                background.post {
                    val current = runCatching {
                        IgnoreUnknownKeysJson.decodeFromString(
                            Preference.SystemUI.StatusBar.serializer(),
                            latest,
                        )
                    }.getOrNull() ?: return@post

                    previous = current
                    main.post { applyRuntimeStatusBarPreference(current) }
                }
            }

            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != StatusBarRuntimeBridge.ACTION) return
                val payload =
                    intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON)
                        ?: return

                // Slider previews can emit dozens of values per second. Coalesce
                // them into one latest-value render frame so SystemUI never queues
                // a backlog of text re-layouts while the finger is moving.
                synchronized(this) {
                    pendingPayload = payload
                    if (previewRefreshScheduled) return
                    previewRefreshScheduled = true
                }
                background.postDelayed(applyPreviewRunnable, 16L)
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
            preset,
            legacyScale,
            gapDp,
            fold7Custom,
            upperScale,
            lowerScale,
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
                textAlignment = clockTextView.textAlignment,
                layoutGravity = when (layoutParams) {
                    is LinearLayout.LayoutParams -> layoutParams.gravity
                    is FrameLayout.LayoutParams -> layoutParams.gravity
                    else -> null
                },
                includeFontPadding = clockTextView.includeFontPadding,
                paddingTop = clockTextView.paddingTop,
                paddingBottom = clockTextView.paddingBottom,
            )
        }

        clockTextView.layoutParams?.let { params ->
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            clockTextView.layoutParams = params
        }
        clockTextView.isSingleLine = false
        clockTextView.maxLines = 2
        // Do not force a fixed two-line minimum box.
        clockTextView.minLines = 1
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
        clockTextView.post {
            ensureDoubleLineClockFullHeightSlot(clockTextView)
        }
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
        val slot = findFullStatusBarView(clockTextView)?.let { statusBar ->
            findDoubleLineClockSlot(clockTextView, statusBar)
        }
        slot?.let { clockSlot ->
            synchronized(doubleLineClockSlotLayouts) {
                doubleLineClockSlotLayouts.remove(clockSlot)?.let { original ->
                    clockSlot.layoutParams?.let { params ->
                        params.height = original.first
                        when (params) {
                            is LinearLayout.LayoutParams -> params.gravity = original.second
                            is FrameLayout.LayoutParams -> params.gravity = original.second
                        }
                        clockSlot.layoutParams = params
                    }
                }
            }
        }
        getDoubleLineClockHost(clockTextView)?.let { host ->
            synchronized(doubleLineClockHostLayouts) {
                doubleLineClockHostLayouts.remove(host)?.let { original ->
                    host.layoutParams?.let { params ->
                        params.height = original.first
                        when (params) {
                            is LinearLayout.LayoutParams -> params.gravity = original.second
                            is FrameLayout.LayoutParams -> params.gravity = original.second
                        }
                        host.layoutParams = params
                    }
                }
            }
        }
        singleLineClockLayouts.remove(clockTextView)?.let { original ->
            clockTextView.layoutParams?.let { params ->
                original.height?.let { params.height = it }
                original.layoutGravity?.let { gravity ->
                    when (params) {
                        is LinearLayout.LayoutParams -> params.gravity = gravity
                        is FrameLayout.LayoutParams -> params.gravity = gravity
                    }
                }
                clockTextView.layoutParams = params
            }
            (clockTextView.parent as? LinearLayout)?.let { parent ->
                original.parentGravity?.let { parent.gravity = it }
            }
            clockTextView.gravity = original.gravity
            clockTextView.textAlignment = original.textAlignment
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
        rememberStatusBarClockPositionView(clockTextView)
        applyStatusBarClockPosition(clockTextView)
        clockTextView.text = dateTime
        clockTextView.contentDescription = dateTime.toString().replace('\n', ' ')
    }
}
