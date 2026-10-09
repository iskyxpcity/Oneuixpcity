package io.github.soclear.oneuix.common

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import io.github.soclear.oneuix.common.Preference.SystemUI.StatusBarComboStyleOneRuntime
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min

/** Live system state for the independent Style One glyph. */
data class StatusComboStyleOneGlyphState(
    val batteryLevel: Int = 100,
    val charging: Boolean = false,
    val quickCharge: Boolean = false,
    val wifiLevel: Int = -1,
    val mobileLevel: Int = -1,
    val simOneLevel: Int = -1,
    val simTwoLevel: Int = -1,
    val networkType: String = "4G",
    val powerSave: Boolean = false,
    val lowBatteryWarning: Boolean = false,
    val tintColor: Int = Color.WHITE,
)

/**
 * Independent renderer for the Style One feature set. The preview and the
 * SystemUI glyph both call this code, so adjustments and live settings stay in sync.
 */
object StatusComboStyleOneGlyphRenderer {
    private const val DESIGN_WIDTH = 260f
    private const val DESIGN_HEIGHT = 120f
    private const val CX = 204f
    private const val CY = 60f
    private const val RADIUS = 42f
    private const val RING_START = 148.69f
    private const val RING_SWEEP = 242.62f
    private const val GAP_IDLE_START = 0.325f
    private const val GAP_IDLE_END = 0.675f
    private const val GAP_CHARGE_START = 0.345f
    private const val GAP_CHARGE_END = 0.655f
    private const val GAP_DUAL_START = 1f - 180f / RING_SWEEP
    private const val GAP_DUAL_END = 180f / RING_SWEEP
    private const val QUICK_CHARGE = 0xFFFFBA28.toInt()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: StatusBarComboStyleOneRuntime,
        state: StatusComboStyleOneGlyphState,
    ) {
        if (width <= 0 || height <= 0) return
        val scale = min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT)
        if (scale <= 0f) return
        val save = canvas.save()
        canvas.translate((width - DESIGN_WIDTH * scale) / 2f, (height - DESIGN_HEIGHT * scale) / 2f)
        canvas.scale(scale, scale)

        val level = state.batteryLevel.coerceIn(0, 100)
        val tint = state.tintColor
        val track = withAlpha(tint, config.trackAlpha)
        val batteryColor = resolveBatteryColor(config, state, tint)
        val wifiInk = config.showWifi && state.wifiLevel >= 1
        val type = state.networkType.trim().uppercase().takeIf {
            config.showMobile && config.mobileTypeMode == 1 && it.isNotEmpty()
        }
        val bolt = config.showValue && config.showBolt && state.charging
        val signalInGlyph = config.showMobile && config.signalMode == 0
        val sims = (if (state.simOneLevel >= 0) 1 else 0) + (if (state.simTwoLevel >= 0) 1 else 0)

        if (config.trioStyle == 1) {
            drawRectangleStyle(canvas, config, state, level, batteryColor, tint, track, wifiInk, bolt)
        } else {
            val dualRows = config.dualSim && signalInGlyph && sims >= 2 && !wifiInk && !state.charging
            val valueInCentre = config.showValue &&
                (config.valueCentred || dualRows || (!wifiInk && type == null))
            val valueInGap = config.showValue && !valueInCentre && !bolt
            val wifiInGap = wifiInk && config.valueCentred && !bolt
            val typeInCentre = type != null && !wifiInk && !valueInCentre
            val typeInGap = type != null && valueInCentre && !wifiInk && !bolt && !dualRows
            val gapUsed = bolt || wifiInGap || valueInGap || typeInGap || dualRows
            val gapStart = when {
                !gapUsed -> 0f
                dualRows -> GAP_DUAL_START
                bolt -> GAP_CHARGE_START
                else -> GAP_IDLE_START
            }
            val gapEnd = when {
                !gapUsed -> 0f
                dualRows -> GAP_DUAL_END
                bolt -> GAP_CHARGE_END
                else -> GAP_IDLE_END
            }
            drawBatteryRing(canvas, level, batteryColor, track, config.ringStroke, gapStart, gapEnd)

            // Centered value moves the Wi-Fi arcs up into the same top mouth.
            // If the bolt owns that mouth, the arcs yield rather than overlap it.
            if (wifiInk && (!config.valueCentred || wifiInGap)) {
                drawWifi(canvas, CX, if (wifiInGap) 20f else 60f, tint, state.wifiLevel, config.arcStroke, wifiInGap)
            }
            if (signalInGlyph) {
                if (dualRows) {
                    drawDotRow(canvas, CX, 34f, state.simOneLevel, tint, track, 0.9f)
                    drawDotRow(canvas, CX, 88f, state.simTwoLevel, tint, track, 0.9f)
                } else {
                    drawDotRow(canvas, CX, 88f, state.mobileLevel, tint, track, 1f)
                }
            }
            if (typeInCentre && type != null) {
                drawNetworkType(canvas, type, CX, 73f, config.typeSize.toFloat(), config.typeWeight, tint, config.typeSuffixScale, 72f)
            }
            if (typeInGap && type != null) {
                drawNetworkType(canvas, type, CX, 30f, config.typeSize.toFloat(), config.typeWeight, tint, config.typeSuffixScale, 65f)
            }
            if (bolt) drawBolt(canvas, CX, 27f, if (state.quickCharge) QUICK_CHARGE else tint)
            if (config.showValue) {
                if (valueInCentre) {
                    drawTextFitted(canvas, level.toString(), CX, 72f, config.valueSize.toFloat(), config.valueWeight, tint, 70f)
                } else if (valueInGap && !bolt) {
                    drawTextFitted(canvas, level.toString(), CX, 30f, config.valueSize.toFloat(), config.valueWeight, tint, 65f)
                }
            }
        }

        // The out-of-glyph cellular view replaces the native signal only when the
        // user explicitly asks for the stacked bars-and-dots alternative.
        if (config.showMobile && config.signalMode == 1 && config.stackedSignal) {
            val signalX = 26f - config.outSignalMarginDp.coerceIn(0, 12) * (DESIGN_HEIGHT / 36f)
            val bars = if (config.dataSimOnly) state.mobileLevel
                else state.simOneLevel.takeIf { it >= 0 } ?: state.mobileLevel
            val second = if (!config.dataSimOnly && state.simTwoLevel >= 0) state.simTwoLevel else -1
            drawStackedSignal(canvas, signalX, 52f, bars, second, tint, track, config.outSignalSizeDp)
        }
        if (config.showMobile && config.mobileTypeMode == 2 && state.networkType.isNotBlank()) {
            val centre = 110f +
                (config.outTypeMarginLeftDp.coerceIn(0, 12) - config.outTypeMarginRightDp.coerceIn(0, 12)) *
                (DESIGN_HEIGHT / 36f)
            val size = config.outTypeSizeDp.coerceIn(6, 30) * (DESIGN_HEIGHT / 36f)
            drawNetworkType(canvas, state.networkType.trim().uppercase(), centre, 67f, size, config.typeWeight, tint, config.typeSuffixScale, 77f)
        }

        canvas.restoreToCount(save)
    }

    private fun drawBatteryRing(
        canvas: Canvas,
        level: Int,
        activeColor: Int,
        trackColor: Int,
        stroke: Int,
        gapStart: Float,
        gapEnd: Float,
    ) {
        rect.set(CX - RADIUS, CY - RADIUS, CX + RADIUS, CY + RADIUS)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke.coerceIn(4, 16).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = trackColor
        drawRingProgress(canvas, 1f, gapStart, gapEnd)
        if (level > 0) {
            paint.color = activeColor
            drawRingProgress(canvas, (level / 100f).coerceIn(0f, 1f), gapStart, gapEnd)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    /** Draw progress along the visible battery path, skipping only its chosen top mouth. */
    private fun drawRingProgress(canvas: Canvas, progress: Float, mouthStart: Float, mouthEnd: Float) {
        val start = mouthStart.coerceIn(0f, 1f)
        val end = mouthEnd.coerceIn(0f, 1f)
        val visiblePath = start + (1f - end)
        if (visiblePath <= 0f) return
        val visibleTo = progress.coerceIn(0f, 1f) * visiblePath
        val firstEnd = min(visibleTo, start)
        if (firstEnd > 0f) {
            canvas.drawArc(rect, RING_START, RING_SWEEP * firstEnd, false, paint)
        }
        val secondVisibleFrom = max(0f, start)
        if (visibleTo > secondVisibleFrom) {
            val pathFrom = end + secondVisibleFrom - start
            val pathTo = end + visibleTo - start
            if (pathTo > pathFrom) {
                canvas.drawArc(rect, RING_START + RING_SWEEP * pathFrom, RING_SWEEP * (pathTo - pathFrom), false, paint)
            }
        }
    }

    private fun drawRectangleStyle(
        canvas: Canvas,
        config: StatusBarComboStyleOneRuntime,
        state: StatusComboStyleOneGlyphState,
        level: Int,
        activeColor: Int,
        tint: Int,
        track: Int,
        wifiInk: Boolean,
        bolt: Boolean,
    ) {
        // The rectangular arrangement uses two vertical signal columns, a top
        // indicator slot, a number and a horizontal battery-level bar.
        if (config.showMobile && config.signalMode == 0) {
            val dual = config.dualSim && state.simOneLevel >= 0 && state.simTwoLevel >= 0
            drawRectDotColumn(canvas, CX - 45f, if (dual) state.simOneLevel else state.mobileLevel, tint, track)
            drawRectDotColumn(canvas, CX + 45f, if (dual) state.simTwoLevel else state.mobileLevel, tint, track)
        }
        val barHeight = config.ringStroke.coerceIn(4, 16).toFloat()
        val top = 105f - barHeight / 2f
        rect.set(CX - 59f, top, CX + 59f, top + barHeight)
        paint.style = Paint.Style.FILL
        paint.color = track
        canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, paint)
        if (level > 0) {
            rect.set(CX - 59f, top, CX - 59f + 118f * level / 100f, top + barHeight)
            paint.color = activeColor
            canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, paint)
        }

        val type = state.networkType.trim().uppercase().takeIf {
            config.showMobile && config.mobileTypeMode == 1 && it.isNotEmpty()
        }
        val canDrawWifi = config.showWifi && state.wifiLevel >= 1
        // Like the source design, the top slot has exactly one owner: bolt,
        // otherwise Wi-Fi arcs, otherwise the network type.
        when {
            bolt -> drawBolt(canvas, CX, 28f, if (state.quickCharge) QUICK_CHARGE else tint)
            canDrawWifi -> drawWifi(canvas, CX, 31f, tint, state.wifiLevel, config.arcStroke, true)
            type != null -> drawNetworkType(canvas, type, CX, 47f, config.typeSize * 1.26f, config.typeWeight, tint, config.typeSuffixScale, 74f)
        }
        if (config.showValue) {
            drawTextFitted(canvas, level.toString(), CX, 94f, config.valueSize * 1.38f, config.valueWeight, tint, 78f)
        }
    }

    private fun drawRectDotColumn(canvas: Canvas, x: Float, level: Int, color: Int, track: Int) {
        for (i in 0..3) {
            paint.style = Paint.Style.FILL
            paint.color = if (i < level.coerceIn(0, 4)) color else track
            canvas.drawCircle(x, 22f + i * 22f, 3.8f, paint)
        }
    }

    private fun drawWifi(canvas: Canvas, cx: Float, cy: Float, color: Int, level: Int, stroke: Int, compact: Boolean) {
        val outer = if (compact) 15f else 27f
        val inner = if (compact) 9f else 18f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke.coerceIn(3, 16).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        if (level >= 3) {
            rect.set(cx - outer, cy - outer, cx + outer, cy + outer)
            paint.color = color
            canvas.drawArc(rect, 225f, 90f, false, paint)
            rect.set(cx - inner, cy - inner, cx + inner, cy + inner)
            canvas.drawArc(rect, 225f, 90f, false, paint)
        } else if (level == 2) {
            rect.set(cx - inner, cy - inner, cx + inner, cy + inner)
            paint.color = color
            canvas.drawArc(rect, 225f, 90f, false, paint)
        }
        paint.style = Paint.Style.FILL
        if (level >= 1) {
            paint.color = color
            canvas.drawCircle(cx, cy + (if (compact) 13f else 25f), if (compact) 2.8f else 3.8f, paint)
        }
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawDotRow(canvas: Canvas, cx: Float, y: Float, level: Int, color: Int, track: Int, scale: Float) {
        val pitch = 13f * scale
        val radius = 5.5f * scale
        for (i in 0..3) {
            paint.style = Paint.Style.FILL
            paint.color = if (i < level.coerceIn(0, 4)) color else track
            canvas.drawCircle(cx + (i - 1.5f) * pitch, y, radius, paint)
        }
    }

    private fun drawStackedSignal(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        barsLevel: Int,
        dotsLevel: Int,
        color: Int,
        track: Int,
        sizeDp: Int,
    ) {
        val scale = (sizeDp / 15f).coerceIn(0.55f, 1.4f)
        val barWidth = 5.8f * scale
        val pitch = 9.6f * scale
        val heights = floatArrayOf(14f, 26f, 38f, 50f).map { it * scale }
        for (i in 0..3) {
            val x = cx + (i - 1.5f) * pitch
            paint.style = Paint.Style.FILL
            paint.color = if (i < barsLevel.coerceIn(0, 4)) color else track
            rect.set(x - barWidth / 2f, cy - heights[i] / 2f, x + barWidth / 2f, cy + heights[i] / 2f)
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, paint)
            if (dotsLevel >= 0) {
                paint.color = if (i < dotsLevel.coerceIn(0, 4)) color else track
                canvas.drawCircle(x, cy + 30f * scale, 2.3f * scale, paint)
            }
        }
    }

    private fun drawTextFitted(canvas: Canvas, text: String, cx: Float, baseline: Float, size: Float, weight: Int, color: Int, maxWidth: Float) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = weightedTypeface(weight)
        paint.textSize = size.coerceIn(8f, 64f)
        val width = paint.measureText(text)
        if (width > maxWidth && width > 0f) paint.textSize *= maxWidth / width
        canvas.drawText(text, cx, baseline, paint)
    }

    private fun drawNetworkType(
        canvas: Canvas, text: String, cx: Float, baseline: Float, size: Float,
        weight: Int, color: Int, suffixScale: Int, maxWidth: Float,
    ) {
        val label = text.uppercase()
        val smallA = label.endsWith("A") && suffixScale in 1..99
        val body = if (smallA) label.dropLast(1) else label
        val safeSize = size.coerceIn(8f, 64f)
        paint.typeface = weightedTypeface(weight)
        paint.textAlign = Paint.Align.LEFT
        paint.color = color
        paint.textSize = safeSize
        val bodyWidth = paint.measureText(body)
        paint.textSize = if (smallA) safeSize * suffixScale / 100f else safeSize
        val suffixWidth = if (smallA) paint.measureText("A") else 0f
        val fullWidth = bodyWidth + suffixWidth
        val scale = if (fullWidth > maxWidth && fullWidth > 0f) maxWidth / fullWidth else 1f
        val left = cx - fullWidth * scale / 2f
        paint.textSize = safeSize * scale
        canvas.drawText(body, left, baseline, paint)
        if (smallA) {
            paint.textSize = safeSize * suffixScale / 100f * scale
            canvas.drawText("A", left + bodyWidth * scale, baseline, paint)
        }
        paint.textAlign = Paint.Align.CENTER
    }

    private fun drawBolt(canvas: Canvas, cx: Float, topBaseline: Float, color: Int) {
        path.reset()
        path.moveTo(cx + 2f, topBaseline - 13f)
        path.lineTo(cx - 8f, topBaseline + 1f)
        path.lineTo(cx - 1f, topBaseline + 1f)
        path.lineTo(cx - 4f, topBaseline + 14f)
        path.lineTo(cx + 9f, topBaseline - 3f)
        path.lineTo(cx + 2f, topBaseline - 3f)
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(path, paint)
    }

    private fun weightedTypeface(weight: Int): Typeface =
        Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), weight.coerceIn(100, 900), false)

    private fun resolveBatteryColor(config: StatusBarComboStyleOneRuntime, state: StatusComboStyleOneGlyphState, tint: Int): Int {
        if (!config.roleColors) return tint
        val darkBackground = luminance(tint) > 0.5f
        return when {
            state.batteryLevel in 0 until 20 ->
                if (darkBackground) config.criticalColorDarkArgb else config.criticalColorLightArgb
            state.powerSave || state.lowBatteryWarning ||
                state.batteryLevel <= config.lowThreshold.coerceIn(5, 50) ->
                if (darkBackground) config.lowColorDarkArgb else config.lowColorLightArgb
            state.charging ->
                if (darkBackground) config.chargingColorDarkArgb else config.chargingColorLightArgb
            else -> tint
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
    private fun luminance(color: Int): Float =
        (0.2126f * Color.red(color) + 0.7152f * Color.green(color) + 0.0722f * Color.blue(color)) / 255f
}
