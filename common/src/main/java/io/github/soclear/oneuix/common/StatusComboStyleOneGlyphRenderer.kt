package io.github.soclear.oneuix.common

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import io.github.soclear.oneuix.common.Preference.SystemUI.StatusBarComboStyleOneRuntime
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class StatusComboStyleOneGlyphState(
    val batteryLevel: Int = 100,
    val charging: Boolean = false,
    val quickCharge: Boolean = false,
    val wifiLevel: Int = -1,
    val mobileLevel: Int = -1,
    val simOneLevel: Int = -1,
    val simTwoLevel: Int = -1,
    val dataSimSlotIndex: Int = 0,
    val networkType: String = "4G",
    val powerSave: Boolean = false,
    val lowBatteryWarning: Boolean = false,
    val chargingAlternatePhase: Boolean = false,
    val tintColor: Int = Color.WHITE,
)

object StatusComboStyleOneGlyphRenderer {
    private const val COMPACT_WIDTH = 120f
    private const val WIDE_WIDTH = 260f
    private const val HEIGHT = 120f
    private const val DUAL_BOTTOM_HEIGHT = 150f
    // Row 1 sits inside the ring's bottom opening; row 2 hugs it directly underneath.
    // The first row sits slightly inside the bottom opening instead of hanging below the ring.
    private const val DUAL_BOTTOM_ROW_ONE_Y = 106f
    private const val DUAL_BOTTOM_ROW_TWO_Y = 122.5f
    private const val DUAL_BOTTOM_DOT_RAISE_DP = 3f
    private const val WIDE_OFFSET = 144.5f
    // Reserve trailing canvas room for the complete outer ring stroke and antialiasing
    // when a network-type label is drawn out of the ring. The host's right edge is stable,
    // so any dynamic leading expansion grows left without moving the ring.
    private const val OUTSIDE_TYPE_RIGHT_CLEARANCE = 8f
    // Preserve the old label-to-ring gap while sizing the host to the real network-type text.
    private const val OUTSIDE_TYPE_REFERENCE_WIDTH = 78f
    // Without a signal lane, trim the empty leading edge by about 2.5dp.
    private const val OUTSIDE_TYPE_ONLY_REFERENCE_WIDTH = 87f
    private const val OUTSIDE_TYPE_REFERENCE_SIZE_DP = 14f
    private const val OUTSIDE_TYPE_RIGHT_GAP = 3.5f
    // Keep the label separated from the ring while reserving room for a native-sized gap on both sides.
    private const val OUTSIDE_TYPE_GAP_REDUCTION = 64f
    // Keep a minimum label-side clearance; configured margins add to the left-growing host lane.
    private const val OUTSIDE_TYPE_LEADING_CLEARANCE = 18f
    // Stable reserved leading lane; user margin changes move only the signal, not the ring or siblings.
    private const val OUTSIDE_SIGNAL_RING_CLEARANCE = 20f
    private const val OUTSIDE_COMPOSITE_HOST_EXTRA = 60f
    private const val OUTSIDE_SIGNAL_NATIVE_ICON_GAP_PX = 20f
    private const val OUTSIDE_FRAME_CENTER_Y = 46.20471f
    private const val OUTSIDE_FRAME_SIDE = 153f
    // Match the upstream HyperDuo top slot for text; keep signal bars on the native icon baseline.
    private const val OUTSIDE_TYPE_TOP_BASELINE = 42.3f
    private const val OUTSIDE_SIGNAL_BAR_BASELINE = 90f
    // Out-of-ring-type-only mode should not reserve the unused signal-pillars lane.
    private const val TYPE_ONLY_SHIFT = 60f
    private const val B_CX = 59.5f
    private const val B_CY = 61.48715f
    private const val B_R = 51.5f
    private const val B_START = 148.69009f
    private const val B_SWEEP = 242.61983f
    private const val GAP_IDLE_START = 0.325f
    private const val GAP_IDLE_END = 0.675f
    private const val GAP_CHARGE_START = 0.345f
    private const val GAP_CHARGE_END = 0.655f
    private const val GAP_DUAL_START = 1f - 180f / B_SWEEP
    private const val GAP_DUAL_END = 180f / B_SWEEP
    private const val W_CX = 59.5f
    private const val W1_CY = 78.3035085f
    private const val W1_R = 31f
    private const val W1_START = 227.3423f
    private const val W1_SWEEP = 85.3154f
    private const val W2_CY = 78.8881817f
    private const val W2_R = 18.5f
    private const val W2_START = 227.5048f
    private const val W2_SWEEP = 84.9904f
    private const val W_TOP = W1_CY - W1_R
    private const val GAP_TOP = 2.5f
    private const val GAP_WIFI_SCALE = 0.8f
    private const val DOT_R = 5.5f
    private const val DUAL_DOT_R = 7f
    private const val CENTER_CY = 64f
    private const val CAP_HEIGHT_RATIO = 0.71f
    private const val CAP_CENTER_RATIO = CAP_HEIGHT_RATIO / 2f
    private const val VALUE_X = 59.5f
    private const val VALUE_BASELINE = 24f
    private const val BOLT_CX = 59.45f
    private const val BOLT_TOP = 1.5f
    private const val BOLT_TOP_REF = 2.2f
    private const val BOLT_CX_REF = 59.45f
    private const val BOLT_SCALE = 2.0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val diagnosticPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val boltPath = buildBoltPath()
    private val wifiDotPath = buildWifiDotPath()
    private val dots = arrayOf(
        floatArrayOf(33f, 104.2f),
        floatArrayOf(50.5f, 111.2f),
        floatArrayOf(68.5f, 111.7f),
        floatArrayOf(86f, 105.8f),
    )

    fun usesWideLayout(config: StatusBarComboStyleOneRuntime): Boolean =
        config.showMobile &&
            (config.mobileTypeMode == 2 || (config.signalMode == 1 && config.stackedSignal))

    fun usesOutsideNetworkTypeLayout(config: StatusBarComboStyleOneRuntime): Boolean =
        config.showMobile && config.mobileTypeMode == 2

    /** Old configurations with dualSim=true but no dualSimMode still map to "ring top/bottom". */
    private fun effectiveDualSimMode(config: StatusBarComboStyleOneRuntime): Int =
        when {
            config.dualSimMode in 1..2 -> config.dualSimMode
            config.dualSim -> 1
            else -> 0
        }

    private fun usesDualSignalBelowRing(config: StatusBarComboStyleOneRuntime): Boolean =
        config.trioStyle == 0 && config.showMobile && config.signalMode == 0 &&
            effectiveDualSimMode(config) == 2

    private data class OutsideFrameGeometry(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val squareLeft: Float,
        val squareTop: Float,
        val squareRight: Float,
        val squareBottom: Float,
        val side: Float,
        val centerX: Float,
        val centerY: Float,
    )

    private fun outsideTypeOffsetXUnits(config: StatusBarComboStyleOneRuntime): Float =
        config.outTypeOffsetXDp.coerceIn(-16, 16) * (HEIGHT / 36f)

    private fun outsideTypeOffsetYUnits(config: StatusBarComboStyleOneRuntime): Float =
        config.outTypeOffsetYDp.coerceIn(-16, 16) * (HEIGHT / 36f)

    private fun outsideSignalOffsetXUnits(config: StatusBarComboStyleOneRuntime): Float =
        config.outSignalOffsetXDp.coerceIn(-16, 16) * (HEIGHT / 36f)

    private fun outsideSignalOffsetYUnits(config: StatusBarComboStyleOneRuntime): Float =
        config.outSignalOffsetYDp.coerceIn(-16, 16) * (HEIGHT / 36f)

    private fun outsideSignalCenterFromIconBoundary(
        config: StatusBarComboStyleOneRuntime,
        minimumVisibleIconLeftPx: Float,
        canvasLeftInsetPx: Float,
        renderScale: Float,
        leadingExpansion: Float,
        signalWidth: Float,
    ): Float {
        val safeScale = renderScale.coerceAtLeast(0.0001f)
        // Default to the same 20px separation already present between adjacent native icons.
        // Positive outer-left margin adds space from the preceding icon; positive ring-side
        // margin moves the signal left to increase separation from the battery ring.
        val targetLeftPx = minimumVisibleIconLeftPx + OUTSIDE_SIGNAL_NATIVE_ICON_GAP_PX
        val targetLeftUnits = (targetLeftPx - canvasLeftInsetPx) / safeScale
        val signedMarginUnits = (
            config.outSignalMarginLeftDp.coerceIn(-16, 16) -
                config.outSignalMarginDp.coerceIn(-16, 16)
            ) * (HEIGHT / 36f)
        return targetLeftUnits - leadingExpansion + signalWidth / 2f + signedMarginUnits
    }

    private fun outsideSignalHeight(config: StatusBarComboStyleOneRuntime): Float =
        if (usesOutsideSignalLayout(config)) 150f * outsideSignalScale(config) else 0f

    private fun outsideTypeBaseline(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float {
        // Centre the visible text bounds inside a fixed frame. Font size changes its own
        // bounds only; it never changes the frame's vertical geometry or the ring's position.
        val size = config.outTypeSizeDp.coerceIn(8, 26) * (HEIGHT / 36f)
        return OUTSIDE_FRAME_CENTER_Y + size * CAP_HEIGHT_RATIO / 2f
    }

    /** The text label has its own base anchor, independent of signal size and spacing. */
    private fun outsideTypeBaseCenter(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float = if (usesOutsideNetworkTypeLayout(config) && networkType.isNotBlank()) {
        outsideNetworkTypeCenter(config, networkType.trim().uppercase())
    } else 0f

    /**
     * The signal has its own anchor. In combined mode use a canonical type reference so changing
     * the displayed label, its font, or its margins cannot move the signal bars.
     */
    private fun outsideSignalBaseCenter(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float {
        val scale = HEIGHT / 36f
        if (usesOutsideNetworkTypeLayout(config) && networkType.isNotBlank()) {
            val defaults = StatusBarComboStyleOneRuntime()
            val canonicalType = config.copy(
                outTypeSizeDp = defaults.outTypeSizeDp,
                outTypeMarginLeftDp = 0,
                outTypeMarginRightDp = 0,
                outTypeOffsetXDp = 0,
                outTypeOffsetYDp = 0,
                typeWeight = defaults.typeWeight,
                typeSuffixScale = defaults.typeSuffixScale,
            )
            // The signal uses a stable independent anchor. Font/text-width changes never move it.
            val canonicalCenter = outsideNetworkTypeCenter(canonicalType, "5G")
            return canonicalCenter -
                config.outSignalMarginDp.coerceIn(-16, 16) * scale +
                config.outSignalMarginLeftDp.coerceIn(-16, 16) * scale
        }
        return outsideSignalWidth(config) / 2f -
            outsideSignalMargin(config) +
            outsideSignalMarginLeft(config)
    }

    /**
     * Compensate for the other lane's width changes in canvas coordinates only. This keeps each
     * visible child and the ring stable while the expanded host reflows native icons.
     */
    private fun outsideSignalCanvasCenter(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float {
        // No compensation for network-type width or the signal's own width: each child keeps
        // its own anchor. The live draw path can additionally align the signal to the last
        // visible native icon through minimumVisibleIconLeftPx.
        return outsideSignalBaseCenter(config, networkType)
    }

    /** Geometry helper; draw code uses each element's dedicated anchor instead. */
    private fun outsideBaseCenter(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float = when {
        usesOutsideNetworkTypeLayout(config) && networkType.isNotBlank() ->
            outsideTypeBaseCenter(config, networkType)
        usesOutsideSignalLayout(config) -> outsideSignalCanvasCenter(config, networkType)
        else -> 0f
    }

    /**
     * Bounds for the invisible square that contains the outside network type and signal bars.
     * The square is recomputed from actual element sizes and individual X/Y offsets.
     */
    private fun outsideFrameGeometry(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
        includeOffsets: Boolean = true,
    ): OutsideFrameGeometry? {
        val label = networkType.trim().uppercase()
        val typeActive = usesOutsideNetworkTypeLayout(config) && label.isNotBlank()
        val signalActive = usesOutsideSignalLayout(config)
        if (!typeActive && !signalActive) return null

        // Reserve a fixed square based on the supported maximum visual size. Do not derive this
        // frame from the current font, signal size, margins, offsets, or network-type string:
        // otherwise changing one child resizes the window and moves the battery ring vertically.
        val stableConfig = config.copy(
            outTypeMarginLeftDp = 0,
            outTypeMarginRightDp = 0,
            outTypeOffsetXDp = 0,
            outTypeOffsetYDp = 0,
            outSignalMarginDp = 0,
            outSignalMarginLeftDp = 0,
            outSignalOffsetXDp = 0,
            outSignalOffsetYDp = 0,
        )
        val centerX = if (typeActive) outsideTypeBaseCenter(stableConfig, label) else 126.5f
        val centerY = OUTSIDE_FRAME_CENTER_Y
        val side = OUTSIDE_FRAME_SIDE
        return OutsideFrameGeometry(
            left = centerX - side / 2f,
            top = centerY - side / 2f,
            right = centerX + side / 2f,
            bottom = centerY + side / 2f,
            squareLeft = centerX - side / 2f,
            squareTop = centerY - side / 2f,
            squareRight = centerX + side / 2f,
            squareBottom = centerY + side / 2f,
            side = side,
            centerX = centerX,
            centerY = centerY,
        )
    }

    private fun outsideFrameLeadingExpansion(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float {
        val frame = outsideFrameGeometry(config, networkType, includeOffsets = false) ?: return 0f
        // Existing type/signal expansion already moves the frame right within the enlarged host.
        // Only reserve more room if the frame would still extend beyond that translated canvas.
        val priorLeadingExpansion = outsideTypeLeadingExpansion(config) +
            outsideSignalLeadingExpansion(config)
        val translatedSquareLeft = frame.squareLeft + priorLeadingExpansion
        return if (translatedSquareLeft < 0f) -translatedSquareLeft + 1f else 0f
    }

    private fun outsideFrameTopExpansion(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float {
        if (!usesOutsideNetworkTypeLayout(config) && !usesOutsideSignalLayout(config)) return 0f
        val fixedTop = OUTSIDE_FRAME_CENTER_Y - OUTSIDE_FRAME_SIDE / 2f
        return if (fixedTop < 0f) -fixedTop + 1f else 0f
    }

    private fun outsideFrameBottomExpansion(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float {
        if (!usesOutsideNetworkTypeLayout(config) && !usesOutsideSignalLayout(config)) return 0f
        val fixedBottom = OUTSIDE_FRAME_CENTER_Y + OUTSIDE_FRAME_SIDE / 2f
        return if (fixedBottom > HEIGHT) fixedBottom - HEIGHT + 1f else 0f
    }

    private fun dualBottomExpansion(config: StatusBarComboStyleOneRuntime): Float =
        if (usesDualSignalBelowRing(config)) (DUAL_BOTTOM_HEIGHT - HEIGHT) / 2f else 0f

    private fun designHeight(
        config: StatusBarComboStyleOneRuntime,
        networkType: String = "",
    ): Float = HEIGHT +
        outsideFrameTopExpansion(config, networkType) +
        outsideFrameBottomExpansion(config, networkType) +
        2f * dualBottomExpansion(config)

    private fun usesOutsideSignalLayout(config: StatusBarComboStyleOneRuntime): Boolean =
        config.showMobile && config.signalMode == 1 && config.stackedSignal

    private fun outsideSignalScale(config: StatusBarComboStyleOneRuntime): Float =
        config.outSignalSizeDp.coerceIn(6, 20) * 1.15f * (HEIGHT / 36f) / 209f

    private fun outsideSignalWidth(config: StatusBarComboStyleOneRuntime): Float =
        if (usesOutsideSignalLayout(config)) 251f * outsideSignalScale(config) else 0f

    // Standalone mode shifts only the signal lane; composite mode applies margin deltas to text and bars together. Ring anchoring stays independent.
    private fun outsideSignalMargin(config: StatusBarComboStyleOneRuntime): Float =
        if (usesOutsideSignalLayout(config)) {
            OUTSIDE_SIGNAL_RING_CLEARANCE +
                config.outSignalMarginDp.coerceIn(-16, 16) * (HEIGHT / 36f)
        } else 0f

    // Extra outer/left margin is independent from the signal-to-ring gap.
    private fun outsideSignalMarginLeft(config: StatusBarComboStyleOneRuntime): Float =
        if (usesOutsideSignalLayout(config)) {
            config.outSignalMarginLeftDp.coerceIn(-16, 16) * (HEIGHT / 36f)
        } else 0f

    /** The row grows leftward for both signal-to-ring spacing and the outer-left margin. */
    private fun outsideSignalLeadingExpansion(config: StatusBarComboStyleOneRuntime): Float =
        if (usesOutsideSignalLayout(config)) {
            OUTSIDE_SIGNAL_RING_CLEARANCE +
                if (usesOutsideNetworkTypeLayout(config)) OUTSIDE_COMPOSITE_HOST_EXTRA else 0f
        } else 0f

    private fun outsideTypeLabelWidth(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float {
        if (!usesOutsideNetworkTypeLayout(config) || networkType.isBlank()) {
            return OUTSIDE_TYPE_REFERENCE_WIDTH
        }
        val requestedSize = config.outTypeSizeDp.coerceIn(8, 26) * (HEIGHT / 36f)
        return measuredNetworkTypeWidth(
            networkType.trim().uppercase(),
            requestedSize,
            config.typeWeight,
            config.typeSuffixScale,
        ).coerceAtLeast(1f)
    }

    private fun outsideTypeMarginLeft(config: StatusBarComboStyleOneRuntime): Float =
        if (usesOutsideNetworkTypeLayout(config)) {
            config.outTypeMarginLeftDp.coerceIn(-16, 16) * (HEIGHT / 36f)
        } else 0f

    private fun outsideTypeMarginRight(config: StatusBarComboStyleOneRuntime): Float =
        if (usesOutsideNetworkTypeLayout(config)) {
            config.outTypeMarginRightDp.coerceIn(-16, 16) * (HEIGHT / 36f)
        } else 0f

    /**
     * Leading canvas width grows with the selected type margins. The canvas translates by the same
     * amount, so its ring anchor stays fixed while the status-bar row can allocate space to the left.
     */
    private fun outsideTypeLeadingExpansion(config: StatusBarComboStyleOneRuntime): Float =
        if (usesOutsideNetworkTypeLayout(config)) OUTSIDE_TYPE_LEADING_CLEARANCE else 0f

    private fun outsideTypeWidthDelta(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float = if (usesOutsideNetworkTypeLayout(config) && networkType.isNotBlank()) {
        val referenceWidth = if (usesOutsideSignalLayout(config)) {
            OUTSIDE_TYPE_REFERENCE_WIDTH
        } else {
            OUTSIDE_TYPE_ONLY_REFERENCE_WIDTH
        }
        (outsideTypeLabelWidth(config, networkType) - referenceWidth) / 2f
    } else 0f

    /**
     * Shared horizontal centre for the out-of-ring network-type + signal composition.
     * The label and bars occupy one fixed frame instead of competing for separate lanes.
     * Changes to the locked signal margins move this whole composite relative to the ring.
     */
    private fun outsideNetworkTypeCenter(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
    ): Float {
        val outsideSignal = usesOutsideSignalLayout(config)
        val typeOnlyShift = if (outsideSignal) 0f else TYPE_ONLY_SHIFT
        val typeReferenceWidth = if (outsideSignal) {
            OUTSIDE_TYPE_REFERENCE_WIDTH
        } else {
            OUTSIDE_TYPE_ONLY_REFERENCE_WIDTH
        }
        val base = B_CX + WIDE_OFFSET - B_R - OUTSIDE_TYPE_RIGHT_GAP -
            typeReferenceWidth / 2f - typeOnlyShift
        val offset = outsideTypeWidthDelta(config, networkType) - OUTSIDE_TYPE_GAP_REDUCTION +
            outsideTypeMarginLeft(config) - outsideTypeMarginRight(config)
        var center = (base + offset).coerceIn(
            30f - typeOnlyShift + offset,
            170f - typeOnlyShift + offset,
        )
        // Network-type placement never inherits signal spacing values.
        return center
    }

    private fun designWidth(
        config: StatusBarComboStyleOneRuntime,
        networkType: String = "",
    ): Float {
        val typeLane = usesOutsideNetworkTypeLayout(config)
        val signalLane = usesOutsideSignalLayout(config)
        val signalLeadingExpansion = outsideSignalLeadingExpansion(config)
        val typeLeadingExpansion = outsideTypeLeadingExpansion(config)
        val typeWidthDelta = outsideTypeWidthDelta(config, networkType)
        val baseWidth = when {
            // Grow the host width by selected margins and any square-frame overflow to the left.
            typeLane && signalLane ->
                WIDE_WIDTH + signalLeadingExpansion + OUTSIDE_TYPE_RIGHT_CLEARANCE +
                    typeWidthDelta + typeLeadingExpansion
            typeLane ->
                WIDE_WIDTH - TYPE_ONLY_SHIFT + OUTSIDE_TYPE_RIGHT_CLEARANCE +
                    typeWidthDelta + typeLeadingExpansion
            signalLane -> outsideSignalWidth(config) + signalLeadingExpansion + COMPACT_WIDTH - 8f
            else -> COMPACT_WIDTH
        }
        return baseWidth + outsideFrameLeadingExpansion(config, networkType)
    }

    private fun baseGlyphHeightDp(config: StatusBarComboStyleOneRuntime): Int =
        (36f * 0.9f * config.glyphScalePercent.coerceIn(70, 150) / 100f)
            .roundToInt().coerceAtLeast(18)

    fun glyphWidthDp(config: StatusBarComboStyleOneRuntime, networkType: String = ""): Int =
        (designWidth(config, networkType) * baseGlyphHeightDp(config).toFloat() / HEIGHT)
            .roundToInt().coerceAtLeast(20)

    /**
     * Only user-selected increases above normal defaults occupy extra native status-row width.
     * Base drawing-lane clearances stay transparent, preserving the corrected default icon gap.
     */
    fun glyphOutsideSpacingExpansionDp(
        config: StatusBarComboStyleOneRuntime,
    ): Int {
        // Left/right margins and offsets only reposition the individual child inside the fixed
        // host envelope. Do not expand the native battery slot, which would reflow sibling icons.
        return 0
    }

    /**
     * Width before added leading space for diagnostics and layout comparisons.
     * The actual inline/overlay host uses glyphWidthDp() so selected lane spacing is allocated.
     */
    fun glyphWidthDpWithoutLeadingExpansion(
        config: StatusBarComboStyleOneRuntime,
        networkType: String = "",
    ): Int {
        val expansion = outsideTypeLeadingExpansion(config) + outsideSignalLeadingExpansion(config) +
            outsideFrameLeadingExpansion(config, networkType)
        val width = designWidth(config, networkType) - expansion
        return (width * baseGlyphHeightDp(config).toFloat() / HEIGHT)
            .roundToInt().coerceAtLeast(20)
    }

    /** Width of the canvas in the same design units used by draw(). */
    fun glyphDesignWidth(config: StatusBarComboStyleOneRuntime, networkType: String = ""): Float =
        designWidth(config, networkType)

    /** Height of the canvas in the same design units used by draw(). */
    fun glyphDesignHeight(
        config: StatusBarComboStyleOneRuntime,
        networkType: String = "",
    ): Float = designHeight(config, networkType)

    /**
     * Ring-centre X inside the canvas. When the host is right-edge anchored, the view grows left
     * by the same amount that this local coordinate grows, keeping the ring's screen X unchanged.
     */
    fun glyphCenterX(config: StatusBarComboStyleOneRuntime, networkType: String = ""): Float {
        val signalWidth = outsideSignalWidth(config)
        val outsideSignal = usesOutsideSignalLayout(config)
        val typeWidthDelta = outsideTypeWidthDelta(config, networkType)
        val leadingExpansion = outsideTypeLeadingExpansion(config) + outsideSignalLeadingExpansion(config) +
            outsideFrameLeadingExpansion(config, networkType)
        val offsetX = when {
            usesOutsideNetworkTypeLayout(config) -> {
                val typeOnlyShift = if (outsideSignal) 0f else TYPE_ONLY_SHIFT
                WIDE_OFFSET - typeOnlyShift + typeWidthDelta + leadingExpansion
            }
            outsideSignal -> signalWidth - 8f + leadingExpansion
            else -> 0f
        }
        return B_CX + offsetX
    }

    /** Horizontal anchor ratio, retained for callers that need the relative location only. */
    fun glyphCenterRatio(config: StatusBarComboStyleOneRuntime, networkType: String = ""): Float =
        glyphCenterX(config, networkType) / designWidth(config, networkType)

    /** Vertical anchor of the battery-ring centre inside the active glyph bounds. */
    fun glyphVerticalCenterRatio(
        config: StatusBarComboStyleOneRuntime,
        networkType: String = "",
    ): Float {
        val verticalContentOffset = outsideFrameTopExpansion(config, networkType) + dualBottomExpansion(config)
        return (B_CY + verticalContentOffset) / designHeight(config, networkType)
    }

    fun glyphHeightDp(
        config: StatusBarComboStyleOneRuntime,
        networkType: String = "",
    ): Int =
        (baseGlyphHeightDp(config).toFloat() * designHeight(config, networkType) / HEIGHT)
            .roundToInt().coerceAtLeast(18)


    /**
     * Export-time geometry report for the exact coordinates used by draw().
     * Design-unit bounds and screen-pixel bounds are separate, exposing clipping,
     * transparent canvas space, and window/renderer alignment differences.
     */
    fun diagnosticGeometrySnapshot(
        config: StatusBarComboStyleOneRuntime,
        networkType: String,
        windowWidthPx: Int,
        windowHeightPx: Int,
        density: Float,
        windowScreenX: Int,
        windowScreenY: Int,
        minimumVisibleIconLeftPx: Float? = null,
    ): String {
        val safeWidth = windowWidthPx.coerceAtLeast(0)
        val safeHeight = windowHeightPx.coerceAtLeast(0)
        val designWidth = designWidth(config, networkType)
        val designHeight = designHeight(config, networkType)
        val frameGeometry = outsideFrameGeometry(config, networkType)
        val frameTopExpansion = outsideFrameTopExpansion(config, networkType)
        val frameBottomExpansion = outsideFrameBottomExpansion(config, networkType)
        val verticalContentOffset = frameTopExpansion + dualBottomExpansion(config)
        val renderScale = if (designHeight > 0f) safeHeight.toFloat() / designHeight else 0f
        val canvasLeftInsetPx = (safeWidth - designWidth * renderScale) / 2f
        val outsideType = usesOutsideNetworkTypeLayout(config)
        val outsideSignal = usesOutsideSignalLayout(config)
        val signalWidth = outsideSignalWidth(config)
        val signalMargin = outsideSignalMargin(config)
        val signalMarginLeft = outsideSignalMarginLeft(config)
        val typeWidthDelta = outsideTypeWidthDelta(config, networkType)
        val leadingExpansion = outsideTypeLeadingExpansion(config) + outsideSignalLeadingExpansion(config) +
            outsideFrameLeadingExpansion(config, networkType)
        val ringOffsetX = when {
            outsideType -> WIDE_OFFSET - (if (outsideSignal) 0f else TYPE_ONLY_SHIFT) +
                typeWidthDelta + leadingExpansion
            outsideSignal -> signalWidth - 8f + leadingExpansion
            else -> 0f
        }
        val ringCenterUnits = B_CX + ringOffsetX
        val ringRadiusUnits = B_R + config.ringStroke / 2f
        val ringLeftPx = canvasLeftInsetPx + (ringCenterUnits - ringRadiusUnits) * renderScale
        val ringRightPx = canvasLeftInsetPx + (ringCenterUnits + ringRadiusUnits) * renderScale
        val ringCenterPx = canvasLeftInsetPx + ringCenterUnits * renderScale
        val screenRingLeft = windowScreenX + ringLeftPx
        val screenRingRight = windowScreenX + ringRightPx
        val screenRingCenter = windowScreenX + ringCenterPx
        val densitySafe = density.takeIf { it > 0f } ?: 1f
        val label = networkType.trim().uppercase()
        val baseTypeCenter = outsideTypeBaseCenter(config, label)
        val baseSignalCenter = outsideSignalCanvasCenter(config, label)
        val typeWidthForClamp = if (outsideType && label.isNotBlank()) {
            outsideTypeLabelWidth(config, label)
        } else 0f
        val safeTypeCenter = if (
            outsideType && minimumVisibleIconLeftPx != null && renderScale > 0f
        ) {
            max(
                baseTypeCenter,
                (minimumVisibleIconLeftPx - canvasLeftInsetPx) / renderScale -
                    leadingExpansion + typeWidthForClamp / 2f,
            )
        } else baseTypeCenter
        val safeSignalCenter = if (
            outsideSignal && minimumVisibleIconLeftPx != null && renderScale > 0f
        ) {
            outsideSignalCenterFromIconBoundary(
                config,
                minimumVisibleIconLeftPx,
                canvasLeftInsetPx,
                renderScale,
                leadingExpansion,
                signalWidth,
            )
        } else baseSignalCenter
        val labelInfo = if (outsideType && label.isNotBlank()) {
            val typeOnlyShift = if (outsideSignal) 0f else TYPE_ONLY_SHIFT
            val typeReferenceWidth = if (outsideSignal) OUTSIDE_TYPE_REFERENCE_WIDTH
                else OUTSIDE_TYPE_ONLY_REFERENCE_WIDTH
            val baseTypeCenter = B_CX + WIDE_OFFSET - B_R - OUTSIDE_TYPE_RIGHT_GAP -
                typeReferenceWidth / 2f - typeOnlyShift
            val leftMarginUnits = outsideTypeMarginLeft(config)
            val rightMarginUnits = outsideTypeMarginRight(config)
            val centerRaw = outsideNetworkTypeCenter(config, label)
            val center = safeTypeCenter + outsideTypeOffsetXUnits(config)
            val requestedTextSize = config.outTypeSizeDp.coerceIn(8, 26) * (HEIGHT / 36f)
            val safeTextSize = requestedTextSize.coerceIn(8f, 96f)
            val smallA = label.endsWith("A") && config.typeSuffixScale in 1..99
            val textBody = if (smallA) label.dropLast(1) else label
            val measure = diagnosticPaint
            measure.typeface = weightedTypeface(config.typeWeight)
            measure.textSize = safeTextSize
            val bodyAdvance = measure.measureText(textBody)
            val bodyBounds = Rect()
            if (textBody.isNotEmpty()) measure.getTextBounds(textBody, 0, textBody.length, bodyBounds)
            var advanceWidth = bodyAdvance
            var inkLeftOffset = bodyBounds.left.toFloat()
            var inkRightOffset = bodyBounds.right.toFloat()
            var suffixAdvance = 0f
            if (smallA) {
                measure.textSize = safeTextSize * config.typeSuffixScale / 100f
                suffixAdvance = measure.measureText("A")
                val suffixBounds = Rect()
                measure.getTextBounds("A", 0, 1, suffixBounds)
                val suffixInkLeft = bodyAdvance + suffixBounds.left
                val suffixInkRight = bodyAdvance + suffixBounds.right
                advanceWidth += suffixAdvance
                inkLeftOffset = min(inkLeftOffset, suffixInkLeft)
                inkRightOffset = max(inkRightOffset, suffixInkRight)
            }
            // Keep the text's measured advance to the right of the last real system icon.
            // The user margin can request more space, but must not draw over a visible icon.
            val boundaryClampedCenter = center
            // draw() translates the full glyph right while its view grows left; include that
            // translation and any icon-bound clamp in the measured screen coordinates.
            val drawnCenter = boundaryClampedCenter + leadingExpansion
            val advanceLeftUnits = drawnCenter - advanceWidth / 2f
            val advanceRightUnits = advanceLeftUnits + advanceWidth
            val inkLeftUnits = advanceLeftUnits + inkLeftOffset
            val inkRightUnits = advanceLeftUnits + inkRightOffset
            val advanceLeftPx = canvasLeftInsetPx + advanceLeftUnits * renderScale
            val advanceRightPx = canvasLeftInsetPx + advanceRightUnits * renderScale
            val inkLeftPx = canvasLeftInsetPx + inkLeftUnits * renderScale
            val inkRightPx = canvasLeftInsetPx + inkRightUnits * renderScale
            val screenAdvanceLeft = windowScreenX + advanceLeftPx
            val screenAdvanceRight = windowScreenX + advanceRightPx
            val screenInkLeft = windowScreenX + inkLeftPx
            val screenInkRight = windowScreenX + inkRightPx
            buildString {
                appendLine("networkTypeLabel=${label},outsideSignal=${outsideSignal}")
                if (frameGeometry != null) {
                    appendLine(
                        "outsideSquareFrame=side:${frameGeometry.side},boundsX=[${frameGeometry.squareLeft},${frameGeometry.squareRight}]," +
                            "boundsY=[${frameGeometry.squareTop},${frameGeometry.squareBottom}],topExpansion=${frameTopExpansion}," +
                            "bottomExpansion=${frameBottomExpansion},typeOffsetDp=(${config.outTypeOffsetXDp},${config.outTypeOffsetYDp})," +
                            "signalOffsetDp=(${config.outSignalOffsetXDp},${config.outSignalOffsetYDp})"
                    )
                }
                appendLine(
                    "typeMarginsDp=left:${config.outTypeMarginLeftDp},right:${config.outTypeMarginRightDp} " +
                        "marginUnits=left:${leftMarginUnits},right:${rightMarginUnits} " +
                        "widthDeltaUnits=${typeWidthDelta} baseCenterUnits=${baseTypeCenter} " +
                        "rawCenterUnits=${centerRaw} requestedDrawnCenterUnits=${center + leadingExpansion} " +
                        "typeBaseline=${outsideTypeBaseline(config, label) + outsideTypeOffsetYUnits(config) + verticalContentOffset} " +
                        "horizontalOffsetDp=${config.outTypeOffsetXDp},verticalOffsetDp=${config.outTypeOffsetYDp} " +
                        "drawnCenterUnits=${drawnCenter} boundaryClampUnits=${boundaryClampedCenter - center} " +
                        "minimumVisibleIconLeftPx=${minimumVisibleIconLeftPx}," +
                        "canvasLeadingExpansionUnits=${leadingExpansion}"
                )
                appendLine(
                    "typeTextSizeDp=${config.outTypeSizeDp},weight=${config.typeWeight}," +
                        "suffixScale=${config.typeSuffixScale},advanceWidthUnits=${advanceWidth}," +
                        "advanceBoundsUnits=[${advanceLeftUnits},${advanceRightUnits}]," +
                        "inkBoundsUnits=[${inkLeftUnits},${inkRightUnits}]"
                )
                appendLine(
                    "typeAdvanceLocalPx=[${advanceLeftPx},${advanceRightPx}]," +
                        "typeInkLocalPx=[${inkLeftPx},${inkRightPx}]," +
                        "typeAdvanceScreenPx=[${screenAdvanceLeft},${screenAdvanceRight}]," +
                        "typeInkScreenPx=[${screenInkLeft},${screenInkRight}]"
                )
                appendLine(
                    "typeClipPx=advanceLeft:${max(0f, -advanceLeftPx)}," +
                        "advanceRight:${max(0f, advanceRightPx - safeWidth)}," +
                        "inkLeft:${max(0f, -inkLeftPx)},inkRight:${max(0f, inkRightPx - safeWidth)}"
                )
            }
        } else {
            "networkTypeLabel=not-drawn (outsideNetworkType=${outsideType},textBlank=${label.isBlank()})"
        }
        val signalLaneInfo = if (outsideSignal && renderScale > 0f) {
            val composite = outsideType && label.isNotBlank()
            val baseSignalX = baseSignalCenter
            val requestedSignalX = baseSignalX + outsideSignalOffsetXUnits(config)
            val effectiveSignalX = safeSignalCenter + outsideSignalOffsetXUnits(config)
            val signalLeftUnits = leadingExpansion + effectiveSignalX - signalWidth / 2f
            val signalRightUnits = signalLeftUnits + signalWidth
            val signalLeftPx = canvasLeftInsetPx + signalLeftUnits * renderScale
            val signalRightPx = canvasLeftInsetPx + signalRightUnits * renderScale
            buildString {
                appendLine(
                    "outsideSignalLane=requestedCenterUnits=${requestedSignalX}," +
                        "effectiveCenterUnits=${effectiveSignalX},marginUnits=${signalMargin}," +
                        "leftMarginUnits=${signalMarginLeft}," +
                        "sharedNetworkComposite=${outsideType && label.isNotBlank()}," +
                        "barBaseline=${OUTSIDE_SIGNAL_BAR_BASELINE + outsideSignalOffsetYUnits(config)}," +
                        "horizontalOffsetDp=${config.outSignalOffsetXDp},verticalOffsetDp=${config.outSignalOffsetYDp}," +
                        "reservedExpansionUnits=${outsideSignalLeadingExpansion(config)}"
                )
                appendLine(
                    "signalLaneBoundsLocalPx=[${signalLeftPx},${signalRightPx}]," +
                        "signalLaneBoundsScreenPx=[${windowScreenX + signalLeftPx},${windowScreenX + signalRightPx}]," +
                        "signalLaneClipPx=left:${max(0f, -signalLeftPx)}," +
                        "right:${max(0f, signalRightPx - safeWidth)}," +
                        "minimumVisibleIconLeftPx=${minimumVisibleIconLeftPx}"
                )
            }
        } else {
            "outsideSignalLane=not-drawn (outsideSignal=${outsideSignal})"
        }
        return buildString {
            appendLine(
                "rendererWindowPx=${safeWidth}x${safeHeight},windowDp=${safeWidth / densitySafe}x${safeHeight / densitySafe}," +
                    "designUnits=${designWidth}x${designHeight},density=${density},renderScale=${renderScale}"
            )
            appendLine(
                "windowScreenOriginPx=(${windowScreenX},${windowScreenY}),canvasLeftInsetPx=${canvasLeftInsetPx}," +
                    "canvasRightInsetPx=${safeWidth - canvasLeftInsetPx - designWidth * renderScale}," +
                    "canvasInsetDp=${canvasLeftInsetPx / densitySafe}"
            )
            appendLine(
                "verticalContentOffset=${verticalContentOffset},glyphVerticalCenterRatio=${(B_CY + verticalContentOffset) / designHeight}," +
                    "ringCenterUnits=${ringCenterUnits},ringOuterBoundsLocalPx=[${ringLeftPx},${ringRightPx}]," +
                    "ringCenterScreenPx=${screenRingCenter},ringOuterBoundsScreenPx=[${screenRingLeft},${screenRingRight}]," +
                    "ringBoundsScreenDp=[${screenRingLeft / densitySafe},${screenRingRight / densitySafe}]"
            )
            appendLine(
                "rendererFlags=outsideType:${outsideType},outsideSignal:${outsideSignal},wide:${usesWideLayout(config)}," +
                    "ringStroke=${config.ringStroke},OUTSIDE_TYPE_GAP_REDUCTION=${OUTSIDE_TYPE_GAP_REDUCTION}," +
                    "outsideTypeTopBaseline=${OUTSIDE_TYPE_TOP_BASELINE},outsideSignalBarBaseline=${OUTSIDE_SIGNAL_BAR_BASELINE}," +
                    "OUTSIDE_TYPE_LEADING_CLEARANCE=${OUTSIDE_TYPE_LEADING_CLEARANCE}," +
                    "OUTSIDE_SIGNAL_RING_CLEARANCE=${OUTSIDE_SIGNAL_RING_CLEARANCE}," +
                    "leadingExpansionUnits=${leadingExpansion},signalMarginUnits=${signalMargin}," +
                    "typeExpansionUnits=${outsideTypeLeadingExpansion(config)}," +
                    "signalExpansionUnits=${outsideSignalLeadingExpansion(config)}," +
                    "TYPE_ONLY_SHIFT=${TYPE_ONLY_SHIFT}"
            )
            append(labelInfo)
            append(signalLaneInfo)
        }
    }

    private fun valueSizeCanvasUnits(config: StatusBarComboStyleOneRuntime): Float =
        config.valueSize.coerceIn(6, 18) * (HEIGHT / 36f)

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: StatusBarComboStyleOneRuntime,
        state: StatusComboStyleOneGlyphState,
        minimumVisibleIconLeftPx: Float? = null,
    ) {
        if (width <= 0 || height <= 0) return
        val wide = usesWideLayout(config)
        val outsideType = usesOutsideNetworkTypeLayout(config)
        val outsideSignal = usesOutsideSignalLayout(config)
        val dualBottomLayout = usesDualSignalBelowRing(config)
        val signalMargin = outsideSignalMargin(config)
        val signalMarginLeft = outsideSignalMarginLeft(config)
        val signalWidth = outsideSignalWidth(config)
        val typeWidthDelta = outsideTypeWidthDelta(config, state.networkType)
        val leadingExpansion = outsideTypeLeadingExpansion(config) + outsideSignalLeadingExpansion(config) +
            outsideFrameLeadingExpansion(config, state.networkType)
        val designWidth = designWidth(config, state.networkType)
        val designHeight = designHeight(config, state.networkType)
        val frameGeometry = outsideFrameGeometry(config, state.networkType)
        val frameTopExpansion = outsideFrameTopExpansion(config, state.networkType)
        val verticalContentOffset = frameTopExpansion + dualBottomExpansion(config)
        // Add 1dp to the bottom opening only in the dual-SIM-below-ring layout.
        // Convert dp to arc degrees using this icon's actual rendered dp height.
        val bottomOpeningExtraDegrees = if (dualBottomLayout) {
            designHeight / (glyphHeightDp(config, state.networkType).toFloat() * B_R) * (180f / PI.toFloat())
        } else 0f
        val scale = height / designHeight
        if (scale <= 0f) return
        val outsideTypeSize = config.outTypeSizeDp.coerceIn(8, 26) * (HEIGHT / 36f)
        val outsideTypeLabelWidth = if (outsideType && state.networkType.isNotBlank()) {
            measuredNetworkTypeWidth(
                state.networkType.trim().uppercase(), outsideTypeSize, config.typeWeight, config.typeSuffixScale,
            )
        } else 0f
        val outsideTypeCenter = outsideTypeBaseCenter(config, state.networkType)
        val outsideSignalCenter = outsideSignalCanvasCenter(config, state.networkType)
        val canvasLeftInsetPx = (width - designWidth * scale) / 2f
        val safeOutsideTypeCenter = if (
            outsideType && outsideTypeLabelWidth > 0f && minimumVisibleIconLeftPx != null
        ) {
            max(
                outsideTypeCenter,
                (minimumVisibleIconLeftPx - canvasLeftInsetPx) / scale -
                    leadingExpansion + outsideTypeLabelWidth / 2f,
            )
        } else outsideTypeCenter
        val safeOutsideSignalCenter = if (
            outsideSignal && signalWidth > 0f && minimumVisibleIconLeftPx != null
        ) {
            outsideSignalCenterFromIconBoundary(
                config,
                minimumVisibleIconLeftPx,
                canvasLeftInsetPx,
                scale,
                leadingExpansion,
                signalWidth,
            )
        } else outsideSignalCenter
        val save = canvas.save()
        canvas.translate((width - designWidth * scale) / 2f, (height - designHeight * scale) / 2f)
        canvas.scale(scale, scale)
        // The host grows leftward for outside content; translate the glyph by the same amount
        // to keep the ring anchored. When type and signal are both outside, their shared centre
        // keeps them stacked as one network icon.
        canvas.translate(
            leadingExpansion,
            verticalContentOffset,
        )

        val typeOnlyShift = if (outsideType && !outsideSignal) TYPE_ONLY_SHIFT else 0f
        val offsetX = when {
            // Neither user spacing slider changes the ring anchor; each moves only its own content lane.
            outsideType -> WIDE_OFFSET - typeOnlyShift + typeWidthDelta
            outsideSignal -> signalWidth - 8f
            else -> 0f
        }
        val cx = B_CX + offsetX
        val level = state.batteryLevel.coerceIn(0, 100)
        val tint = state.tintColor
        val track = withAlpha(tint, config.trackAlpha)
        val batteryColor = resolveBatteryColor(config, state, tint)
        val wifiInk = config.showWifi && state.wifiLevel >= 1
        val innerType = state.networkType.trim().uppercase().takeIf {
            config.showMobile && config.mobileTypeMode == 1 && it.isNotEmpty()
        }
        val centredBatteryValue = config.trioStyle == 0 && config.valueCentred
        // When Wi-Fi or the inner network-type label occupies the ring opening, the battery
        // number may also be placed in that opening. Charging display mode must apply there too.
        val batteryValueInOpening = config.trioStyle == 0 && !config.valueCentred &&
            (wifiInk || innerType != null)
        val shouldAlternateCharging = state.charging && config.showBolt && config.showValue &&
            ((!centredBatteryValue && config.mobileTypeMode == 1) || batteryValueInOpening) &&
            config.chargingDisplayMode == 1
        val alternateChargingPhase = shouldAlternateCharging && state.chargingAlternatePhase
        // Charging color has priority; by default the bolt replaces the percentage.
        // The optional alternate mode swaps the two glyphs every five seconds.
        val bolt = config.showBolt && state.charging && !alternateChargingPhase
        // Out-of-ring network-type mode disables the 5-second swap, not the battery value.
        val showBatteryValue = config.showValue &&
            (!state.charging || alternateChargingPhase || !config.showBolt ||
                centredBatteryValue || (config.mobileTypeMode != 1 && !batteryValueInOpening))
        val inRingSignal = config.showMobile && config.signalMode == 0
        val dualSimMode = effectiveDualSimMode(config)
        val dualLevelsAvailable = !config.dataSimOnly &&
            state.simOneLevel >= 0 && state.simTwoLevel >= 0
        val dualRows = dualSimMode == 1 && dualLevelsAvailable && inRingSignal &&
            !wifiInk && !state.charging
        val dualBottomRows = dualBottomLayout && dualLevelsAvailable
        val valueInCentre = showBatteryValue &&
            (config.valueCentred || dualRows ||
                (!wifiInk && config.trioStyle == 0 && config.mobileTypeMode != 1) ||
                (!wifiInk && innerType == null))
        val valueInGap = showBatteryValue && !valueInCentre && !bolt
        val wifiInGap = wifiInk && config.valueCentred && !bolt
        val typeInCentre = innerType != null && !wifiInk && !valueInCentre
        val typeInGap = innerType != null && valueInCentre && !wifiInk && !bolt && !dualRows
        val gapUsed = bolt || wifiInGap || valueInGap || typeInGap || dualRows
        // Size the ring opening to the glyph occupying it instead of using
        // one oversized fixed gap for every state. This keeps the top opening
        // tight around the bolt, number, Wi-Fi mark, or network label.
        val gapRange = when {
            !gapUsed -> 0f to 0f
            dualRows -> GAP_DUAL_START to GAP_DUAL_END
            bolt -> {
                val originalBoltContentWidth =
                    27f * config.chargingBoltSizePercent.coerceIn(50, 180) / 100f
                contentGapRange(
                    originalBoltContentWidth * 0.85f,
                    config.ringStroke,
                    // Offset the 15% size reduction so the opening is still 2dp wider than before.
                    extraClearance = 3f + originalBoltContentWidth * 0.15f +
                        2f * designHeight / glyphHeightDp(config, state.networkType).toFloat(),
                )
            }
            wifiInGap -> contentGapRange(43f, config.ringStroke)
            valueInGap -> contentGapRange(
                measuredTextWidth(level.toString(), valueSizeCanvasUnits(config), config.valueWeight),
                config.ringStroke,
                extraClearance = 2f * designHeight / glyphHeightDp(config, state.networkType).toFloat(),
            )
            typeInGap && innerType != null -> contentGapRange(
                measuredNetworkTypeWidth(innerType, config.typeSize.toFloat(), config.typeWeight, config.typeSuffixScale),
                config.ringStroke,
            )
            else -> GAP_IDLE_START to GAP_IDLE_END
        }
        val gapStart = gapRange.first
        val gapEnd = gapRange.second

        if (config.trioStyle == 1) {
            drawRectangleStyle(
                canvas, offsetX, config, state, level, batteryColor, tint, track,
                bolt, innerType, showBatteryValue,
            )
        } else {
            if (state.powerSave) {
                val powerSaveFillColor = if (luminance(tint) > 0.5f) {
                    config.powerSaveFillColorDarkArgb
                } else {
                    config.powerSaveFillColorLightArgb
                }
                drawPowerSaveRingFill(canvas, cx, config.ringStroke, powerSaveFillColor)
            }
            // The native ring path already leaves its opening at the bottom; keep it unrotated.
            drawBatteryRing(
                canvas, cx, level, batteryColor, track, config.ringStroke, gapStart, gapEnd,
                bottomOpeningExtraDegrees,
            )
            if (wifiInk && (!config.valueCentred || wifiInGap)) {
                drawWifi(canvas, offsetX, tint, state.wifiLevel, config.arcStroke, wifiInGap)
            }
            if (inRingSignal) {
                when {
                    dualRows -> {
                        // Existing mode: one SIM row above and one below the ring center.
                        drawDotRow(canvas, offsetX, state.simOneLevel, tint, track, DUAL_DOT_R, true, config)
                        drawDotRow(canvas, offsetX, state.simTwoLevel, tint, track, DUAL_DOT_R, false, config)
                    }
                    dualBottomRows -> {
                        // Alternative mode: reserve real canvas height and place both SIM rows
                        // underneath the ring, outside the battery stroke.
                        drawBottomDualDotRow(canvas, offsetX, state.simOneLevel, tint, track, DUAL_BOTTOM_ROW_ONE_Y, config, state.networkType)
                        drawBottomDualDotRow(canvas, offsetX, state.simTwoLevel, tint, track, DUAL_BOTTOM_ROW_TWO_Y, config, state.networkType)
                    }
                    dualBottomLayout -> {
                        // With only one SIM available, place its dots in the first row position,
                        // not the lower row used by a two-SIM layout.
                        drawBottomDualDotRow(canvas, offsetX, state.mobileLevel, tint, track, DUAL_BOTTOM_ROW_ONE_Y, config, state.networkType)
                    }
                    else -> drawSingleSignalDots(canvas, offsetX, state.mobileLevel, tint, track, config)
                }
            }
            if (typeInCentre && innerType != null) {
                val size = fitTextSize(innerType, config.typeSize.toFloat(), config.typeWeight,
                    centreClearWidth(config.typeSize.toFloat(), config.ringStroke), config.typeSuffixScale)
                drawNetworkType(canvas, innerType, cx, centerBaseline(size), size, config.typeWeight,
                    tint, config.typeSuffixScale, centreClearWidth(size, config.ringStroke))
            }
            if (typeInGap && innerType != null) {
                val size = fitTextSize(innerType, config.typeSize.toFloat(), config.typeWeight,
                    gapClearWidth(config.ringStroke, gapStart, gapEnd), config.typeSuffixScale)
                drawNetworkType(canvas, innerType, cx, gapBaseline(size), size, config.typeWeight,
                    tint, config.typeSuffixScale, gapClearWidth(config.ringStroke, gapStart, gapEnd))
            }
            if (bolt) drawBolt(
                canvas, offsetX, config,
                resolveChargingBoltColor(config, state, tint),
            )
            if (showBatteryValue) {
                val requestedSize = valueSizeCanvasUnits(config)
                val clear = if (valueInCentre) centreClearWidth(requestedSize, config.ringStroke)
                    else gapClearWidth(config.ringStroke, gapStart, gapEnd)
                val size = fitTextSize(level.toString(), requestedSize, config.valueWeight, clear)
                val baseline = if (valueInCentre) centerBaseline(size) else gapBaseline(size)
                drawTextFitted(canvas, level.toString(), cx, baseline, size, config.valueWeight, tint, clear)
            }
        }

        if (wide && config.showMobile && config.signalMode == 1 && config.stackedSignal) {
            // Independent signal offsets change only this child, while the host uses the square bounds.
            // Signal offsets and collision avoidance affect only the signal lane.
            val signalX = safeOutsideSignalCenter + outsideSignalOffsetXUnits(config)
            val primarySlotLevel = if (state.dataSimSlotIndex == 1) state.simTwoLevel else state.simOneLevel
            val secondarySlotLevel = if (state.dataSimSlotIndex == 1) state.simOneLevel else state.simTwoLevel
            val bars = if (config.dataSimOnly) state.mobileLevel
                else primarySlotLevel.takeIf { it >= 0 } ?: state.mobileLevel
            val second = if (!config.dataSimOnly && secondarySlotLevel >= 0) secondarySlotLevel else -1
            // Keep the tallest bar's lower edge on the native status-icon baseline. Optional
            // second-SIM dots follow underneath that same bar baseline.
            drawStackedSignal(
                canvas, signalX, OUTSIDE_SIGNAL_BAR_BASELINE + outsideSignalOffsetYUnits(config),
                bars, second, tint, track, config.outSignalSizeDp,
            )
        }
        if (wide && config.showMobile && config.mobileTypeMode == 2 && state.networkType.isNotBlank()) {
            // Centre text within the fixed outside frame by default. Its independent X/Y offsets
            // can move it anywhere without changing the signal lane or battery-ring geometry.
            drawNetworkType(
                canvas, state.networkType.trim().uppercase(),
                safeOutsideTypeCenter + outsideTypeOffsetXUnits(config),
                outsideTypeBaseline(config, state.networkType) + outsideTypeOffsetYUnits(config),
                outsideTypeSize, config.typeWeight, tint, config.typeSuffixScale, 0f,
            )
        }
        canvas.restoreToCount(save)
    }

    private fun drawBatteryRing(
        canvas: Canvas, cx: Float, level: Int, active: Int, track: Int,
        stroke: Int, gapStart: Float, gapEnd: Float, extraBottomOpeningDegrees: Float,
    ) {
        val openingExtra = extraBottomOpeningDegrees.coerceAtLeast(0f)
        // Keep the top of the ring anchored while splitting the extra opening evenly across both ends.
        val ringStart = B_START + openingExtra / 2f
        val ringSweep = (B_SWEEP - openingExtra).coerceAtLeast(1f)
        rect.set(cx - B_R, B_CY - B_R, cx + B_R, B_CY + B_R)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke.coerceIn(4, 16).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = track
        drawRingProgress(canvas, cx, 0f, 1f, gapStart, gapEnd, ringStart, ringSweep)
        if (level > 0) {
            paint.color = active
            drawRingProgress(canvas, cx, 0f, level / 100f, gapStart, gapEnd, ringStart, ringSweep)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawRingProgress(
        canvas: Canvas, cx: Float, from: Float, to: Float, gapStart: Float, gapEnd: Float,
        ringStart: Float, ringSweep: Float,
    ) {
        val start = gapStart.coerceIn(0f, 1f)
        val end = gapEnd.coerceIn(0f, 1f)
        val drawn = start + (1f - end)
        if (drawn <= 0f) return
        val visibleFrom = from.coerceIn(0f, 1f) * drawn
        val visibleTo = to.coerceIn(0f, 1f) * drawn
        val firstEnd = min(visibleTo, start)
        if (firstEnd > visibleFrom) drawArcByProgress(canvas, cx, visibleFrom, firstEnd, ringStart, ringSweep)
        val secondFrom = max(visibleFrom, start)
        if (visibleTo > secondFrom) {
            drawArcByProgress(
                canvas, cx, end + secondFrom - start, end + visibleTo - start, ringStart, ringSweep,
            )
        }
    }

    private fun drawArcByProgress(
        canvas: Canvas, cx: Float, from: Float, to: Float, ringStart: Float, ringSweep: Float,
    ) {
        rect.set(cx - B_R, B_CY - B_R, cx + B_R, B_CY + B_R)
        canvas.drawArc(rect, ringStart + from * ringSweep, (to - from) * ringSweep, false, paint)
    }

    private fun drawWifi(
        canvas: Canvas, offsetX: Float, color: Int, level: Int, stroke: Int, intoGap: Boolean,
    ) {
        val save = canvas.save()
        if (intoGap) {
            canvas.translate(offsetX + W_CX * (1f - GAP_WIFI_SCALE),
                GAP_TOP - (W_TOP - stroke * 0.5f) * GAP_WIFI_SCALE)
            canvas.scale(GAP_WIFI_SCALE, GAP_WIFI_SCALE)
            drawWifiArcs(canvas, 0f, color, level, stroke)
        } else {
            drawWifiArcs(canvas, offsetX, color, level, stroke)
        }
        canvas.restoreToCount(save)
    }

    private fun drawWifiArcs(canvas: Canvas, offsetX: Float, color: Int, level: Int, stroke: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke.coerceIn(3, 16).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = color
        if (level >= 3) {
            rect.set(offsetX + W_CX - W1_R, W1_CY - W1_R, offsetX + W_CX + W1_R, W1_CY + W1_R)
            canvas.drawArc(rect, W1_START, W1_SWEEP, false, paint)
            rect.set(offsetX + W_CX - W2_R, W2_CY - W2_R, offsetX + W_CX + W2_R, W2_CY + W2_R)
            canvas.drawArc(rect, W2_START, W2_SWEEP, false, paint)
        } else if (level == 2) {
            rect.set(offsetX + W_CX - W2_R, W2_CY - W2_R, offsetX + W_CX + W2_R, W2_CY + W2_R)
            canvas.drawArc(rect, W2_START, W2_SWEEP, false, paint)
        }
        paint.style = Paint.Style.FILL
        if (level >= 1) {
            val save = canvas.save()
            canvas.translate(offsetX, 0f)
            canvas.drawPath(wifiDotPath, paint)
            canvas.restoreToCount(save)
        }
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun dotCoordinateScale(config: StatusBarComboStyleOneRuntime): Float =
        HEIGHT / (36f * 0.9f) / (config.glyphScalePercent.coerceIn(70, 150) / 100f)

    private fun dotHorizontalOffset(config: StatusBarComboStyleOneRuntime): Float =
        config.signalDotOffsetXDp.coerceIn(-8, 8) * dotCoordinateScale(config)

    private fun dotVerticalOffset(config: StatusBarComboStyleOneRuntime): Float =
        config.signalDotOffsetYDp.coerceIn(-8, 8) * dotCoordinateScale(config)

    private fun dotRadius(config: StatusBarComboStyleOneRuntime, baseRadius: Float): Float =
        baseRadius * config.signalDotSizePercent.coerceIn(50, 150) / 100f

    private fun dotCenterX(config: StatusBarComboStyleOneRuntime, index: Int): Float {
        val pitch = config.signalDotSpacingDp.coerceIn(3, 8) * (HEIGHT / (36f * 0.9f))
        return B_CX + (index - 1.5f) * pitch + dotHorizontalOffset(config)
    }

    private fun drawSingleSignalDots(
        canvas: Canvas, offsetX: Float, level: Int, color: Int, track: Int,
        config: StatusBarComboStyleOneRuntime,
    ) {
        val radius = dotRadius(config, DOT_R)
        val offsetY = dotVerticalOffset(config)
        dots.forEachIndexed { index, dot ->
            paint.style = Paint.Style.FILL
            paint.color = if (index < level.coerceIn(0, 4)) color else track
            canvas.drawCircle(offsetX + dotCenterX(config, index), dot[1] + offsetY, radius, paint)
        }
    }

    private fun drawDotRow(
        canvas: Canvas, offsetX: Float, level: Int, color: Int, track: Int,
        radius: Float, topRow: Boolean, config: StatusBarComboStyleOneRuntime,
    ) {
        val resolvedRadius = dotRadius(config, radius)
        val offsetY = dotVerticalOffset(config)
        dots.forEachIndexed { index, point ->
            val y = (if (topRow) 2f * B_CY - point[1] else point[1]) + offsetY
            paint.style = Paint.Style.FILL
            paint.color = if (index < level.coerceIn(0, 4)) color else track
            canvas.drawCircle(offsetX + dotCenterX(config, index), y, resolvedRadius, paint)
        }
    }

    private fun drawBottomDualDotRow(
        canvas: Canvas, offsetX: Float, level: Int, color: Int, track: Int, y: Float,
        config: StatusBarComboStyleOneRuntime, networkType: String,
    ) {
        val radius = dotRadius(config, DOT_R)
        // Raise the selected below-ring signal layout by 3dp while preserving its user offset slider.
        val resolvedY = y + dotVerticalOffset(config) -
            DUAL_BOTTOM_DOT_RAISE_DP * DUAL_BOTTOM_HEIGHT / glyphHeightDp(config, networkType).toFloat()
        dots.forEachIndexed { index, _ ->
            paint.style = Paint.Style.FILL
            paint.color = if (index < level.coerceIn(0, 4)) color else track
            canvas.drawCircle(offsetX + dotCenterX(config, index), resolvedY, radius, paint)
        }
    }

    private fun drawRectangleStyle(
        canvas: Canvas, offsetX: Float, config: StatusBarComboStyleOneRuntime,
        state: StatusComboStyleOneGlyphState, level: Int, active: Int,
        tint: Int, track: Int, bolt: Boolean, innerType: String?, showBatteryValue: Boolean,
    ) {
        if (config.showMobile && config.signalMode == 0) {
            val dual = effectiveDualSimMode(config) != 0 && !config.dataSimOnly &&
                state.simOneLevel >= 0 && state.simTwoLevel >= 0
            drawRectDotColumn(canvas, offsetX + 9.6f,
                if (dual) state.simOneLevel else state.mobileLevel, tint, track)
            drawRectDotColumn(canvas, offsetX + 110.4f,
                if (dual) state.simTwoLevel else state.mobileLevel, tint, track)
        }
        val barHeight = config.ringStroke.coerceIn(4, 16).toFloat()
        val barTop = 107.3f - barHeight / 2f
        rect.set(offsetX + 1f, barTop, offsetX + 119f, barTop + barHeight)
        paint.style = Paint.Style.FILL
        paint.color = track
        canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, paint)
        if (level > 0) {
            rect.set(offsetX + 1f, barTop, offsetX + 1f + 118f * level / 100f, barTop + barHeight)
            paint.color = active
            canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, paint)
        }
        when {
            bolt -> drawRectBolt(
                canvas, offsetX, config,
                resolveChargingBoltColor(config, state, tint),
                config.arcStroke,
            )
            config.showWifi && state.wifiLevel >= 1 ->
                drawRectWifi(canvas, offsetX, tint, state.wifiLevel, config.arcStroke)
            innerType != null ->
                drawNetworkType(canvas, innerType, offsetX + 59.5f, 42.3f,
                    config.typeSize * 1.26f, config.typeWeight, tint, config.typeSuffixScale, 74f)
        }
        if (showBatteryValue) {
            val requestedSize = valueSizeCanvasUnits(config) * 1.38f
            val size = fitTextSize(level.toString(), requestedSize, config.valueWeight, 78f)
            drawTextFitted(canvas, level.toString(), offsetX + 59.5f, 94f,
                size, config.valueWeight, tint, 78f)
        }
    }

    private fun drawRectDotColumn(canvas: Canvas, x: Float, level: Int, color: Int, track: Int) {
        for (i in 0..3) {
            paint.style = Paint.Style.FILL
            paint.color = if (i >= 4 - level.coerceIn(0, 4)) color else track
            canvas.drawCircle(x, 15.6f + i * 22.5f, 8.6f, paint)
        }
    }

    private fun drawRectWifi(canvas: Canvas, offsetX: Float, color: Int, level: Int, stroke: Int) {
        val save = canvas.save()
        val scale = 0.95f
        canvas.translate(offsetX + 60f - W_CX * scale, 8.5f - (W_TOP - stroke * 0.5f) * scale)
        canvas.scale(scale, scale)
        drawWifiArcs(canvas, 0f, color, level, stroke)
        canvas.restoreToCount(save)
    }

    private fun drawRectBolt(
        canvas: Canvas, offsetX: Float, config: StatusBarComboStyleOneRuntime, color: Int, stroke: Int,
    ) {
        val scale = boltScale(config)
        val save = canvas.save()
        canvas.translate(offsetX + boltOffsetX(config) + 0.55f, boltOffsetY(config) + 0.24f)
        canvas.scale(scale, scale)
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(boltPath, paint)
        canvas.restoreToCount(save)
    }

    private fun drawStackedSignal(
        canvas: Canvas, cx: Float, barBaseline: Float, bars: Int, dots: Int,
        color: Int, track: Int, sizeDp: Int,
    ) {
        // Reference geometry: 251 × 209 box, 50-wide capsules at 67-pixel
        // pitch, heights 75/100/125/150, 50-diameter dots and a 9-pixel row gap.
        // Anchor the tallest bar's lower edge to the native status-icon baseline.
        val scale = sizeDp.coerceIn(6, 20) * 1.15f * (HEIGHT / 36f) / 209f
        val inkWidth = 251f * scale
        val left = cx - inkWidth / 2f
        val pitch = 67f * scale
        val barWidth = 50f * scale
        val radius = barWidth / 2f
        val heights = floatArrayOf(75f, 100f, 125f, 150f).map { it * scale }
        val baseline = barBaseline
        val dotCenterY = baseline + 34f * scale
        for (i in 0..3) {
            val barLeft = left + i * pitch
            paint.style = Paint.Style.FILL
            paint.color = if (i < bars.coerceIn(0, 4)) color else track
            rect.set(barLeft, baseline - heights[i], barLeft + barWidth, baseline)
            canvas.drawRoundRect(rect, radius, radius, paint)
            if (dots >= 0) {
                paint.color = if (i < dots.coerceIn(0, 4)) color else track
                canvas.drawCircle(barLeft + radius, dotCenterY, 25f * scale, paint)
            }
        }
    }

    /** Draws a power-save fill inset from the inner edge of the battery ring. */
    private fun drawPowerSaveRingFill(
        canvas: Canvas, cx: Float, configuredStroke: Int, colorArgb: Int,
    ) {
        val stroke = configuredStroke.coerceIn(4, 16).toFloat()
        // Preserve the existing extra inner inset so the fill never touches the ring stroke.
        val innerRadius = (B_R - stroke / 2f - 1f - HEIGHT / 36f).coerceAtLeast(0f)
        paint.style = Paint.Style.FILL
        paint.color = colorArgb
        canvas.drawCircle(cx, B_CY, innerRadius, paint)
    }

    private fun drawTextFitted(
        canvas: Canvas, text: String, cx: Float, baseline: Float,
        size: Float, weight: Int, color: Int, maxWidth: Float,
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = weightedTypeface(weight)
        paint.textSize = size.coerceIn(8f, 64f)
        val w = paint.measureText(text)
        if (maxWidth > 0f && w > maxWidth) paint.textSize *= maxWidth / w
        canvas.drawText(text, cx, baseline, paint)
    }

    private fun drawNetworkType(
        canvas: Canvas, text: String, cx: Float, baseline: Float, size: Float,
        weight: Int, color: Int, suffixScale: Int, maxWidth: Float,
    ) {
        val label = text.uppercase()
        val smallA = label.endsWith("A") && suffixScale in 1..99
        val body = if (smallA) label.dropLast(1) else label
        val safeSize = size.coerceIn(8f, 96f)
        paint.typeface = weightedTypeface(weight)
        paint.textAlign = Paint.Align.LEFT
        paint.color = color
        paint.textSize = safeSize
        val bodyWidth = paint.measureText(body)
        paint.textSize = if (smallA) safeSize * suffixScale / 100f else safeSize
        val suffixWidth = if (smallA) paint.measureText("A") else 0f
        val fullWidth = bodyWidth + suffixWidth
        val fit = if (maxWidth > 0f && fullWidth > maxWidth) maxWidth / fullWidth else 1f
        val left = cx - fullWidth * fit / 2f
        paint.textSize = safeSize * fit
        canvas.drawText(body, left, baseline, paint)
        if (smallA) {
            paint.textSize = safeSize * suffixScale / 100f * fit
            canvas.drawText("A", left + bodyWidth * fit, baseline, paint)
        }
        paint.textAlign = Paint.Align.CENTER
    }

    private fun drawBolt(
        canvas: Canvas, offsetX: Float, config: StatusBarComboStyleOneRuntime, color: Int,
    ) {
        val scale = boltScale(config)
        val save = canvas.save()
        canvas.translate(offsetX + boltOffsetX(config), boltOffsetY(config))
        canvas.scale(scale, scale)
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(boltPath, paint)
        canvas.restoreToCount(save)
    }

    private fun boltScale(config: StatusBarComboStyleOneRuntime): Float =
        0.85f * BOLT_SCALE * config.chargingBoltSizePercent.coerceIn(50, 180) / 100f

    // Convert dp offsets into normalized icon coordinates so the configured
    // offset stays optically stable when the overall icon scale changes.
    private fun boltOffsetX(config: StatusBarComboStyleOneRuntime): Float =
        BOLT_CX - BOLT_CX_REF * boltScale(config) +
            config.chargingBoltXDp.coerceIn(-8, 8) / (config.glyphScalePercent.coerceIn(70, 150) / 100f)

    private fun boltOffsetY(config: StatusBarComboStyleOneRuntime): Float =
        BOLT_TOP - BOLT_TOP_REF * boltScale(config) +
            config.chargingBoltYDp.coerceIn(-8, 8) / (config.glyphScalePercent.coerceIn(70, 150) / 100f)

    private fun buildBoltPath() = Path().apply {
        moveTo(62.1f, 2.2f)
        quadTo(62.8f, 2.5f, 62.6f, 3.3f)
        lineTo(61.2f, 7.8f)
        lineTo(65.9f, 7.8f)
        quadTo(66.9f, 7.8f, 67.3f, 8.6f)
        quadTo(67.6f, 9.3f, 67f, 10f)
        lineTo(57f, 21.3f)
        quadTo(56.4f, 22f, 55.6f, 21.6f)
        quadTo(55f, 21.3f, 55.3f, 20.5f)
        lineTo(57.4f, 14.1f)
        lineTo(52.9f, 14.1f)
        quadTo(52f, 14.1f, 51.6f, 13.3f)
        quadTo(51.3f, 12.6f, 51.9f, 12f)
        lineTo(61.1f, 2.7f)
        quadTo(61.6f, 2.1f, 62.1f, 2.2f)
        close()
    }

    private fun buildWifiDotPath() = Path().apply {
        moveTo(59.5f, 69.9f)
        cubicTo(61f, 69.9f, 65.2f, 70.8f, 66.5f, 73f)
        cubicTo(66.7f, 73.8f, 66.7f, 74.3f, 66.5f, 75f)
        cubicTo(63.8f, 78.8f, 61.15f, 80.95f, 59.5f, 80.95f)
        cubicTo(57.85f, 80.95f, 55.2f, 78.8f, 52.5f, 75f)
        cubicTo(52.3f, 74.3f, 52.3f, 73.8f, 52.5f, 73f)
        cubicTo(53.8f, 70.8f, 58f, 69.9f, 59.5f, 69.9f)
        close()
    }

    private fun fitTextSize(
        text: String, requested: Float, weight: Int, maxWidth: Float, suffixScale: Int = 100,
    ): Float {
        paint.typeface = weightedTypeface(weight)
        paint.textSize = requested.coerceIn(8f, 64f)
        val body = if (text.endsWith("A") && suffixScale < 100) text.dropLast(1) else text
        var width = paint.measureText(body)
        if (body != text) {
            paint.textSize = requested * suffixScale / 100f
            width += paint.measureText("A")
        }
        return if (maxWidth > 0f && width > maxWidth) requested * maxWidth / width else requested
    }

    private fun contentGapRange(
        contentWidth: Float, stroke: Int, extraClearance: Float = 0f,
    ): Pair<Float, Float> {
        // Include the ring stroke and an optical clearance on both sides. Charging gets
        // a tiny extra allowance so the bolt does not feel cramped in the opening.
        val chord = (contentWidth + stroke.coerceIn(4, 16) + 7f + extraClearance).coerceIn(12f, B_R * 1.65f)
        val halfAngle = Math.toDegrees(kotlin.math.asin((chord / (2f * B_R)).coerceIn(0f, 0.95f).toDouble()))
        val fraction = (2.0 * halfAngle / B_SWEEP).toFloat().coerceIn(0.06f, 0.42f)
        val start = (1f - fraction) / 2f
        return start to (1f - start)
    }

    private fun measuredTextWidth(text: String, size: Float, weight: Int): Float {
        paint.typeface = weightedTypeface(weight)
        paint.textSize = size.coerceIn(8f, 64f)
        return paint.measureText(text)
    }

    private fun measuredNetworkTypeWidth(text: String, size: Float, weight: Int, suffixScale: Int): Float {
        val label = text.uppercase()
        val body = if (label.endsWith("A") && suffixScale in 1..99) label.dropLast(1) else label
        paint.typeface = weightedTypeface(weight)
        paint.textSize = size.coerceIn(8f, 96f)
        val bodyWidth = paint.measureText(body)
        if (body == label) return bodyWidth
        paint.textSize = size.coerceIn(8f, 96f) * suffixScale / 100f
        return bodyWidth + paint.measureText("A")
    }

    private fun gapClearWidth(stroke: Int, gapStart: Float, gapEnd: Float): Float {
        val radians = Math.toRadians(((gapEnd - gapStart) * B_SWEEP * 0.5f).toDouble())
        return max(0f, (2.0 * B_R * kotlin.math.sin(radians)).toFloat() - stroke)
    }

    private fun centreClearWidth(size: Float, stroke: Int): Float {
        val innerRadius = B_R - stroke / 2f
        return max(0f, (2.0 * innerRadius * kotlin.math.sin(Math.toRadians(43.0))).toFloat())
    }

    private fun gapBaseline(size: Float): Float = max(VALUE_BASELINE, GAP_TOP + size * CAP_HEIGHT_RATIO)
    private fun centerBaseline(size: Float): Float = CENTER_CY + size * CAP_CENTER_RATIO

    private fun weightedTypeface(weight: Int): Typeface =
        Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), weight.coerceIn(100, 900), false)

    private fun resolveChargingBoltColor(
        config: StatusBarComboStyleOneRuntime,
        state: StatusComboStyleOneGlyphState,
        tint: Int,
    ): Int {
        // Bolt colours are independent of the ring's roleColors switch.
        // White status-bar tint means a dark background; black tint means a light one.
        val darkBackground = luminance(tint) > 0.5f
        return when {
            state.quickCharge && darkBackground -> config.quickChargingBoltColorDarkArgb
            state.quickCharge -> config.quickChargingBoltColorLightArgb
            darkBackground -> config.normalChargingBoltColorDarkArgb
            else -> config.normalChargingBoltColorLightArgb
        }
    }

    private fun resolveBatteryColor(
        config: StatusBarComboStyleOneRuntime, state: StatusComboStyleOneGlyphState, tint: Int,
    ): Int {
        if (!config.roleColors) return tint
        val darkBackground = luminance(tint) > 0.5f
        return when {
            // OneuiXP behavior: charging color wins over low/critical and power-save colors.
            state.charging ->
                if (darkBackground) config.chargingColorDarkArgb else config.chargingColorLightArgb
            // The critical cutoff is exclusive; its exact value belongs to the low color.
            state.batteryLevel < config.criticalThreshold.coerceIn(1, 20) ->
                if (darkBackground) config.criticalColorDarkArgb else config.criticalColorLightArgb
            state.lowBatteryWarning ||
                state.batteryLevel <= config.lowThreshold.coerceIn(5, 50) ->
                if (darkBackground) config.lowColorDarkArgb else config.lowColorLightArgb
            else -> tint
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

    private fun luminance(color: Int): Float =
        (0.2126f * Color.red(color) + 0.7152f * Color.green(color) + 0.0722f * Color.blue(color)) / 255f
}
