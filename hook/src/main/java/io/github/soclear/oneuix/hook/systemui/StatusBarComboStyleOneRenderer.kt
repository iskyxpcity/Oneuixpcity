package io.github.soclear.oneuix.hook.systemui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import io.github.soclear.oneuix.common.Preference
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * OneuiXP Three-in-One Icon Style One renderer.
 *
 * Draws circular and rectangular styles, battery state, Wi-Fi arcs,
 * dual-SIM/mobile signal layouts, network type, status colours and typography.
 */
internal object StatusBarComboStyleOneRenderer {
    private const val B_CX = 59.5f
    private const val B_CY = 61.4871526f
    private const val B_R = 51.5f
    private const val B_START = 148.6900869f
    private const val B_SWEEP = 242.6198262f
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
    private const val GAP_WIFI_SCALE = 0.8f

    private const val DOT_R = 5.5f
    private const val DUAL_DOT_R = 7f
    private val DOTS = arrayOf(
        33f to 104.2f,
        50.5f to 111.2f,
        68.5f to 111.7f,
        86f to 105.8f,
    )

    private const val STACK_BAR_W = 50f
    private const val STACK_PITCH = 67f
    private val STACK_BAR_H = floatArrayOf(75f, 100f, 125f, 150f)
    private const val STACK_DOT_D = 50f
    private const val STACK_DOT_GAP = 9f
    private const val STACK_INK_W = 251f

    private const val RECT_DOT_R = 8.6f
    private const val RECT_DOT_LX = 9.6f
    private const val RECT_DOT_RX = 110.4f
    private const val RECT_DOT_TOP = 15.6f
    private const val RECT_DOT_STEP = 22.5f
    private const val RECT_BAR_LEFT = 1f
    private const val RECT_BAR_RIGHT = 119f
    private const val RECT_BAR_CY = 107.3f
    private const val RECT_TYPE_BASELINE = 42.3f
    private const val RECT_TYPE_SCALE = 1.26f
    private const val RECT_VALUE_BASELINE = 92.7f
    private const val RECT_VALUE_SCALE = 1.38f
    private const val RECT_WIFI_SCALE = 0.95f

    data class State(
        val batteryLevel: Int,
        val charging: Boolean,
        val quickCharging: Boolean,
        val powerSave: Boolean,
        val lowBattery: Boolean,
        val wifiLevel: Int,
        val mobileLevel: Int,
        val slotLevels: IntArray,
        val dataSlot: Int,
        val networkType: String,
        val foreground: Int,
    )

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()
    private val bolt = Path().apply {
        moveTo(62.1f, 2.2f)
        quadTo(62.8f, 2.5f, 62.6f, 3.3f)
        lineTo(61.2f, 7.8f)
        lineTo(65.9f, 7.8f)
        quadTo(66.9f, 8.6f, 67.3f, 8.6f)
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
    private val wifiDot = Path().apply {
        moveTo(59.5f, 69.9f)
        cubicTo(61f, 69.9f, 65.2f, 70.8f, 66.5f, 73f)
        cubicTo(66.7f, 73.8f, 66.7f, 74.3f, 66.5f, 75f)
        cubicTo(63.8f, 78.8f, 61.15f, 80.95f, 59.5f, 80.95f)
        cubicTo(57.85f, 80.95f, 55.2f, 78.8f, 52.5f, 75f)
        cubicTo(52.3f, 74.3f, 52.3f, 73.8f, 52.5f, 73f)
        cubicTo(53.8f, 70.8f, 58f, 69.9f, 59.5f, 69.9f)
        close()
    }

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        state: State,
    ) {
        if (!cfg.enabled || width <= 0 || height <= 0) return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val isRect = cfg.trioStyle == 1
        val inkW = if (isRect) 118f else 117f
        val inkH = if (isRect) 114.4f else 117.5f
        val scale = min(width / inkW, height / inkH)
        if (scale <= 0f) return

        canvas.save()
        canvas.translate((width - inkW * scale) * 0.5f, (height - inkH * scale) * 0.5f)
        canvas.scale(scale, scale)
        if (isRect) drawRect(canvas, cfg, state) else drawRing(canvas, cfg, state)
        canvas.restore()
    }

    private fun drawRing(
        canvas: Canvas,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        s: State,
    ) {
        cfgColorForeground = s.foreground
        val role = roleColor(cfg, s)
        val hasBattery = cfg.showBattery
        val hasValue = hasBattery && (cfg.showValue || cfg.showBatteryLevelText)
        val displayType = if (!cfg.show5gNetworkType && s.networkType.startsWith("5G")) {
            ""
        } else {
            s.networkType
        }
        val wifiInk = cfg.showWifi && s.wifiLevel >= 1
        val dual = cfg.dualSim && !cfg.dataSimOnly &&
            cfg.showMobile && s.slotLevels.count { it >= 0 } >= 2 &&
            !wifiInk && !s.charging

        val gapMode = when {
            hasBattery && s.charging && cfg.showChargingBolt -> 1
            dual -> 3
            hasValue && !cfg.valueCentred -> 2
            else -> 0
        }
        val gapWidthScale = when (gapMode) {
            1 -> cfg.chargingBoltSizePercent.coerceIn(50f, 180f) / 100f
            2 -> (
                (cfg.valueSize.coerceIn(16, 44) / 36f) *
                    (cfg.batteryTextSizeSp.coerceIn(5f, 18f) / 12f)
                ).coerceIn(0.7f, 1.8f)
            else -> 1f
        }
        val baseHalfGap = when (gapMode) {
            1 -> (GAP_CHARGE_END - GAP_CHARGE_START) * 0.5f
            2 -> (GAP_IDLE_END - GAP_IDLE_START) * 0.5f
            3 -> (GAP_DUAL_END - GAP_DUAL_START) * 0.5f
            else -> 0f
        }
        val halfGap = when (gapMode) {
            1, 2 -> (baseHalfGap * gapWidthScale).coerceIn(0.08f, 0.24f)
            3 -> baseHalfGap
            else -> 0f
        }
        val gs = if (gapMode == 0) 0f else 0.5f - halfGap
        val ge = if (gapMode == 0) 0f else 0.5f + halfGap

        val ringWidth = cfg.ringStroke.coerceIn(4, 16).toFloat()
        val arcWidth = (
            cfg.arcStroke.coerceIn(3, 16) *
                cfg.arcStrokePercent.coerceIn(50f, 200f) / 100f
            )

        if (hasBattery) {
            drawBatteryRing(
                canvas,
                s.batteryLevel,
                role,
                s.foreground,
                ringWidth,
                cfg.trackAlpha,
                cfg.batteryBlackDepth,
                gs,
                ge,
            )
        }

        if (cfg.showWifi && s.wifiLevel >= 1) {
            drawWifi(
                canvas,
                s.wifiLevel.coerceIn(0, 4),
                s.foreground,
                arcWidth,
                cfg.valueCentred && hasValue,
                cfg.innerSizePercent,
                cfg.innerYDp,
                cfg.wifiTwoArcs,
            )
        }

        if (cfg.showMobile && cfg.signalMode == 0) {
            drawRingDots(canvas, cfg, s, dual)
        }

        if (hasValue) {
            drawValue(canvas, s.batteryLevel, s.foreground, cfg, cfg.valueCentred)
        }

        if (cfg.showMobile && cfg.mobileTypeMode == 1 && displayType.isNotEmpty()) {
            if (!cfg.valueCentred || !hasValue) {
                drawType(canvas, displayType, cfg, 59.5f, 64f, cfg.typeSize.toFloat())
            }
        }

        if (hasBattery && s.charging && cfg.showChargingBolt) {
            drawBolt(
                canvas,
                if (s.quickCharging) cfg.quickChargeColorArgb else role,
                cfg.chargingBoltSizePercent,
                cfg.chargingBoltXDp,
                cfg.chargingBoltYDp,
            )
        }

        if (cfg.showMobile && cfg.mobileTypeMode == 2 && displayType.isNotEmpty()) {
            drawType(
                canvas,
                displayType,
                cfg,
                95f + cfg.outTypeMarginRightDp - cfg.outTypeMarginLeftDp * 0.5f,
                59f,
                cfg.outTypeSizeDp.coerceIn(8, 26).toFloat(),
            )
        }

        if (cfg.showMobile && cfg.signalMode == 1) {
            drawStackedSignal(canvas, cfg, s)
        }
    }

    private fun drawRect(
        canvas: Canvas,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        s: State,
    ) {
        val role = roleColor(cfg, s)
        val displayType = if (!cfg.show5gNetworkType && s.networkType.startsWith("5G")) {
            ""
        } else {
            s.networkType
        }
        val dataLevel = if (s.dataSlot in 0..1) {
            s.slotLevels.getOrNull(s.dataSlot)?.coerceIn(0, 4) ?: s.mobileLevel
        } else {
            s.mobileLevel
        }
        val leftLevel =
            if (cfg.dualSim && !cfg.dataSimOnly) {
                s.slotLevels.getOrNull(0)?.coerceIn(0, 4) ?: dataLevel
            } else dataLevel
        val rightLevel =
            if (cfg.dualSim && !cfg.dataSimOnly) {
                s.slotLevels.getOrNull(1)?.coerceIn(0, 4) ?: dataLevel
            } else dataLevel

        if (cfg.showMobile) {
            val inactive = withAlpha(s.foreground, cfg.trackAlpha)
            for (i in 0..3) {
                val y = RECT_DOT_TOP + i * RECT_DOT_STEP
                fill.color = if (i < leftLevel.coerceIn(0, 4)) s.foreground else inactive
                canvas.drawCircle(RECT_DOT_LX, y, RECT_DOT_R, fill)
                fill.color = if (i < rightLevel.coerceIn(0, 4)) s.foreground else inactive
                canvas.drawCircle(RECT_DOT_RX, y, RECT_DOT_R, fill)
            }
        }

        val thickness = cfg.ringStroke.coerceIn(4, 16).toFloat()
        val radius = thickness * 0.5f
        if (cfg.showBattery) {
            fill.color = withAlpha(s.foreground, cfg.trackAlpha)
            canvas.drawRoundRect(
                RECT_BAR_LEFT,
                RECT_BAR_CY - radius,
                RECT_BAR_RIGHT,
                RECT_BAR_CY + radius,
                radius,
                radius,
                fill,
            )
        }
        if (cfg.showBattery && s.batteryLevel > 0) {
            val right = RECT_BAR_LEFT +
                (RECT_BAR_RIGHT - RECT_BAR_LEFT) * (s.batteryLevel / 100f).coerceIn(0f, 1f)
            fill.color = role
            canvas.drawRoundRect(
                RECT_BAR_LEFT,
                RECT_BAR_CY - radius,
                right,
                RECT_BAR_CY + radius,
                radius,
                radius,
                fill,
            )
        }

        if (cfg.showBattery && s.charging && cfg.showChargingBolt) {
            drawBolt(
                canvas,
                if (s.quickCharging) cfg.quickChargeColorArgb else role,
                cfg.chargingBoltSizePercent,
                cfg.chargingBoltXDp,
                cfg.chargingBoltYDp,
            )
        } else if (cfg.showWifi && s.wifiLevel >= 1) {
            drawRectWifi(canvas, s.wifiLevel, s.foreground, arcWidth(cfg))
        } else if (cfg.showMobile && cfg.mobileTypeMode != 0 && displayType.isNotEmpty()) {
            drawType(
                canvas,
                displayType,
                cfg,
                60f,
                RECT_TYPE_BASELINE,
                cfg.typeSize * RECT_TYPE_SCALE,
            )
        }

        if (cfg.showBattery && (cfg.showValue || cfg.showBatteryLevelText)) {
            drawValueRect(canvas, s.batteryLevel, s.foreground, cfg)
        }
    }

    private fun arcWidth(cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime): Float =
        cfg.arcStroke.coerceIn(3, 16) *
            cfg.arcStrokePercent.coerceIn(50f, 200f) / 100f

    private fun drawBatteryRing(
        canvas: Canvas,
        level: Int,
        role: Int,
        foreground: Int,
        width: Float,
        trackAlpha: Int,
        blackDepth: Float,
        gapStart: Float,
        gapEnd: Float,
    ) {
        stroke.strokeWidth = width
        stroke.color = withAlpha(
            foreground,
            (trackAlpha.coerceIn(0, 255) * blackDepth.coerceIn(0.15f, 1f)).toInt(),
        )
        batteryArc(canvas, 0f, 1f, gapStart, gapEnd)
        if (level > 0) {
            stroke.color = role
            batteryArc(canvas, 0f, min(1f, level / 100f), gapStart, gapEnd)
        }
    }

    private fun batteryArc(
        canvas: Canvas,
        from: Float,
        to: Float,
        gapStart: Float,
        gapEnd: Float,
    ) {
        if (to <= from) return
        if (gapStart == 0f && gapEnd == 0f) {
            arc(canvas, B_CX, B_CY, B_R, B_START, B_SWEEP, stroke)
            return
        }
        val drawn = gapStart + (1f - gapEnd)
        if (drawn <= 0f) return
        val visibleFrom = from * drawn
        val visibleTo = to * drawn
        val firstEnd = min(visibleTo, gapStart)
        if (firstEnd > visibleFrom) {
            arc(
                canvas, B_CX, B_CY, B_R,
                B_START + visibleFrom * B_SWEEP,
                (firstEnd - visibleFrom) * B_SWEEP,
                stroke,
            )
        }
        val secondFrom = max(visibleFrom, gapStart)
        if (visibleTo > secondFrom) {
            arc(
                canvas, B_CX, B_CY, B_R,
                B_START + (gapEnd + secondFrom - gapStart) * B_SWEEP,
                (visibleTo - secondFrom) * B_SWEEP,
                stroke,
            )
        }
    }

    private fun drawWifi(
        canvas: Canvas,
        level: Int,
        foreground: Int,
        width: Float,
        inGap: Boolean,
        sizePercent: Float,
        yDp: Float,
        forceTwoArcs: Boolean,
    ) {
        val save = canvas.save()
        if (inGap) {
            canvas.translate(
                W_CX * (1f - GAP_WIFI_SCALE),
                2.5f - (W1_CY - W1_R - width * 0.5f) * GAP_WIFI_SCALE,
            )
            canvas.scale(GAP_WIFI_SCALE, GAP_WIFI_SCALE)
        }
        val scale = sizePercent.coerceIn(50f, 180f) / 100f
        canvas.translate(
            (1f - scale) * W_CX,
            (1f - scale) * (W1_CY - W1_R),
        )
        canvas.translate(0f, yDp.coerceIn(-8f, 8f))
        canvas.scale(scale, scale)

        stroke.strokeWidth = width.coerceIn(3f, 16f)
        stroke.color = foreground
        when {
            level >= 3 || (forceTwoArcs && level >= 2) -> {
                arc(canvas, W_CX, W1_CY, W1_R, W1_START, W1_SWEEP, stroke)
                arc(canvas, W_CX, W2_CY, W2_R, W2_START, W2_SWEEP, stroke)
            }
            level == 2 -> {
                arc(canvas, W_CX, W2_CY, W2_R, W2_START, W2_SWEEP, stroke)
            }
        }
        if (level >= 1) {
            fill.color = foreground
            canvas.drawPath(wifiDot, fill)
        }
        canvas.restoreToCount(save)
    }

    private fun drawRectWifi(
        canvas: Canvas,
        level: Int,
        foreground: Int,
        width: Float,
    ) {
        val save = canvas.save()
        canvas.translate(
            60f - W_CX * RECT_WIFI_SCALE,
            8.5f - (W1_CY - W1_R - width * 0.5f) * RECT_WIFI_SCALE,
        )
        canvas.scale(RECT_WIFI_SCALE, RECT_WIFI_SCALE)
        drawWifi(canvas, level, foreground, width, false, 100f, 0f, false)
        canvas.restoreToCount(save)
    }

    private fun drawRingDots(
        canvas: Canvas,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        s: State,
        dual: Boolean,
    ) {
        val inactive = withAlpha(s.foreground, cfg.trackAlpha)
        val radius = DOT_R * cfg.dotsSizePercent.coerceIn(50f, 180f) / 100f
        val gap = cfg.dotsGapDp.coerceIn(0f, 8f)
        if (!dual) {
            val level = s.mobileLevel.coerceIn(0, 4)
            DOTS.forEachIndexed { index, point ->
                val dx = when (index) {
                    0 -> -gap * 0.75f
                    3 -> gap * 0.75f
                    else -> 0f
                }
                fill.color = if (index < level) s.foreground else inactive
                canvas.drawCircle(point.first + dx, point.second, radius, fill)
            }
            return
        }

        val top = DOTS.map { it.first to 2f * B_CY - it.second }
        val level0 = s.slotLevels.getOrNull(0)?.coerceIn(0, 4) ?: 0
        val level1 = s.slotLevels.getOrNull(1)?.coerceIn(0, 4) ?: 0
        DOTS.forEachIndexed { index, point ->
            fill.color = if (index < level0) s.foreground else inactive
            canvas.drawCircle(top[index].first, top[index].second, DUAL_DOT_R, fill)
            fill.color = if (index < level1) s.foreground else inactive
            canvas.drawCircle(point.first, point.second, DUAL_DOT_R, fill)
        }
    }

    private fun drawStackedSignal(
        canvas: Canvas,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        s: State,
    ) {
        val scale = cfg.outSignalSizeDp.coerceIn(6, 20) / 15f
        val margin = cfg.outSignalMarginDp.coerceIn(0, 16)
        val dataSlot = s.dataSlot.takeIf { it in 0..1 } ?: -1
        val showSecondary = cfg.dualSim && cfg.stackedSignal && !cfg.dataSimOnly
        val primary = if (showSecondary) {
            s.slotLevels.getOrNull(0)?.coerceIn(0, 4) ?: s.mobileLevel
        } else {
            if (dataSlot >= 0) {
                s.slotLevels.getOrNull(dataSlot)?.coerceIn(0, 4) ?: s.mobileLevel
            } else {
                s.mobileLevel
            }
        }
        val secondary = if (showSecondary) {
            s.slotLevels.getOrNull(1)?.coerceIn(0, 4) ?: -1
        } else {
            -1
        }

        val inactive = withAlpha(s.foreground, cfg.trackAlpha)
        val barWidth = STACK_BAR_W * 0.18f * scale
        val pitch = STACK_PITCH * 0.18f * scale
        val x0 = 60f - STACK_INK_W * 0.09f * scale + margin
        val baseline = 105f
        for (index in 0..3) {
            val x = x0 + index * pitch
            fill.color = if (index < primary) s.foreground else inactive
            canvas.drawRoundRect(
                x,
                baseline - STACK_BAR_H[index] * 0.18f * scale,
                x + barWidth,
                baseline,
                barWidth * 0.5f,
                barWidth * 0.5f,
                fill,
            )
            if (secondary >= 0) {
                fill.color = if (index < secondary) s.foreground else inactive
                canvas.drawCircle(
                    x + barWidth * 0.5f,
                    baseline + STACK_DOT_GAP * 0.18f * scale,
                    STACK_DOT_D * 0.09f * scale,
                    fill,
                )
            }
        }
    }

    private fun drawValue(
        canvas: Canvas,
        level: Int,
        foreground: Int,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        centered: Boolean,
    ) {
        val configured = if (centered) {
            cfg.valueSize.coerceIn(16, 44).toFloat()
        } else {
            cfg.batteryTextSizeSp.coerceIn(5f, 18f) * 3.2f
        }
        val requested = if (centered) min(configured * 1.4f, 52f) else configured
        val clear = if (centered) {
            centerClearWidth(requested, cfg.ringStroke.toFloat())
        } else {
            gapClearWidth(cfg.ringStroke.toFloat())
        }
        applyWeight(
            if (centered) cfg.valueWeight else cfg.batteryTextFontWeight
        )
        val size = fitText(level.coerceIn(0, 100).toString(), requested, clear)
        text.textAlign = Paint.Align.CENTER
        text.textSize = size
        text.color = foreground
        canvas.drawText(
            level.coerceIn(0, 100).toString(),
            B_CX,
            if (centered) 64f + size * 0.355f else max(24f, 2.5f + size * 0.71f),
            text,
        )
    }

    private fun drawValueRect(
        canvas: Canvas,
        level: Int,
        foreground: Int,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
    ) {
        applyWeight(cfg.valueWeight)
        val size = fitText(
            level.coerceIn(0, 100).toString(),
            cfg.valueSize.coerceIn(16, 44) * RECT_VALUE_SCALE,
            (RECT_DOT_RX - RECT_DOT_R) - (RECT_DOT_LX + RECT_DOT_R),
        )
        text.textAlign = Paint.Align.CENTER
        text.textSize = size
        text.color = foreground
        canvas.drawText(level.coerceIn(0, 100).toString(), 60f, RECT_VALUE_BASELINE, text)
    }

    private var cfgColorForeground: Int = Color.WHITE

    private fun drawType(
        canvas: Canvas,
        value: String,
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        x: Float,
        baseline: Float,
        requested: Float,
    ) {
        applyWeight(cfg.typeWeight)
        val size = fitType(
            value,
            requested,
            centerClearWidth(requested, cfg.ringStroke.toFloat()),
            cfg.typeSuffixScale,
        )
        val suffixScale = cfg.typeSuffixScale.coerceIn(50, 100)
        text.color = cfgColorForeground
        if (!value.endsWith("A") || suffixScale >= 100) {
            text.textAlign = Paint.Align.CENTER
            text.textSize = size
            canvas.drawText(value, x, baseline, text)
            return
        }
        val base = value.removeSuffix("A")
        val small = size * suffixScale / 100f
        text.textAlign = Paint.Align.LEFT
        text.textSize = size
        val baseWidth = text.measureText(base)
        text.textSize = small
        val suffixWidth = text.measureText("A")
        var left = x - (baseWidth + suffixWidth) * 0.5f
        text.textSize = size
        canvas.drawText(base, left, baseline, text)
        left += baseWidth
        text.textSize = small
        canvas.drawText("A", left, baseline, text)
        text.textAlign = Paint.Align.CENTER
    }

    private fun drawBolt(
        canvas: Canvas,
        color: Int,
        sizePercent: Float,
        xDp: Float,
        yDp: Float,
    ) {
        val save = canvas.save()
        val scale = 2.0f * sizePercent.coerceIn(50f, 180f) / 100f
        canvas.translate(
            B_CX + xDp.coerceIn(-8f, 8f),
            12.05f + yDp.coerceIn(-8f, 8f),
        )
        canvas.scale(scale, scale)
        canvas.translate(-59.45f, -12.05f)
        fill.color = color
        canvas.drawPath(bolt, fill)
        canvas.restoreToCount(save)
    }

    private fun roleColor(
        cfg: Preference.SystemUI.StatusBarComboStyleOneRuntime,
        s: State,
    ): Int {
        if (!cfg.roleColors) return s.foreground
        val dark = isDarkBackground(s.foreground)
        return when {
            s.batteryLevel in 0..5 ->
                if (dark) cfg.criticalColorDarkArgb else cfg.criticalColorLightArgb
            s.powerSave || s.lowBattery || s.batteryLevel <= cfg.lowBatteryThreshold ->
                if (dark) cfg.lowColorDarkArgb else cfg.lowColorLightArgb
            s.charging && s.quickCharging -> cfg.quickChargeColorArgb
            s.charging ->
                if (dark) cfg.chargingColorDarkArgb else cfg.chargingColorLightArgb
            else -> s.foreground
        }
    }

    private var textWeight = -1
    private fun applyWeight(weight: Int) {
        val next = weight.coerceIn(100, 900)
        if (next == textWeight) return
        textWeight = next
        text.typeface = Typeface.Builder("sans-serif")
            .setWeight(next)
            .setItalic(false)
            .build()
    }

    private fun fitText(value: String, requested: Float, clear: Float): Float {
        if (clear <= 0f) return requested
        text.textSize = requested
        val width = text.measureText(value)
        return if (width > clear && width > 0f) requested * clear / width else requested
    }

    private fun fitType(
        value: String,
        requested: Float,
        clear: Float,
        suffixScale: Int,
    ): Float {
        if (clear <= 0f) return requested
        text.textSize = requested
        val baseWidth = text.measureText(value.removeSuffix("A"))
        val suffixWidth = if (value.endsWith("A") && suffixScale < 100) {
            text.textSize = requested * suffixScale.coerceIn(50, 100) / 100f
            text.measureText("A")
        } else 0f
        val total = baseWidth + suffixWidth
        return if (total > clear && total > 0f) requested * clear / total else requested
    }

    private fun centerClearWidth(size: Float, strokeWidth: Float): Float {
        val inner = B_R - strokeWidth * 0.5f - 1f
        val dy = size * 0.71f * 0.5f
        val dx = sqrt(max(0f, inner * inner - dy * dy))
        return dx * 2f
    }

    private fun gapClearWidth(strokeWidth: Float): Float {
        val half = Math.toRadians(
            ((GAP_IDLE_END - GAP_IDLE_START) * B_SWEEP * 0.5f).toDouble()
        )
        return (2.0 * B_R.toDouble() * kotlin.math.sin(half)).toFloat() - strokeWidth
    }

    private fun arc(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        start: Float,
        sweep: Float,
        paint: Paint,
    ) {
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, start, sweep, false, paint)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

    private fun isDarkBackground(color: Int): Boolean {
        val luminance = (
            Color.red(color) * 0.2126f +
                Color.green(color) * 0.7152f +
                Color.blue(color) * 0.0722f
            ) / 255f
        return luminance > 0.5f
    }
}
