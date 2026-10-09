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

/** Live inputs consumed by the independent Style One renderer. */
data class StatusComboStyleOneGlyphState(
    val batteryLevel: Int = 100,
    val charging: Boolean = false,
    val quickCharge: Boolean = false,
    val wifiLevel: Int = 4,
    val mobileLevel: Int = 4,
    val simOneLevel: Int = -1,
    val simTwoLevel: Int = -1,
    val networkType: String = "5G",
    val tintColor: Int = Color.WHITE,
)

/**
 * One drawing implementation is shared by the settings preview and SystemUI.
 * Coordinates are intentionally independent of MIUI's icon view dimensions.
 */
object StatusComboStyleOneGlyphRenderer {
    private const val DESIGN_WIDTH = 260f
    private const val DESIGN_HEIGHT = 120f
    private const val CX = 204f
    private const val CY = 60f
    private const val RADIUS = 42f
    private const val QUICK_CHARGE = 0xFFFFBA28.toInt()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect = RectF()
    private val shapePath = Path()

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

        val tint = state.tintColor
        val darkBackground = luminance(tint) > 0.5f
        val accent = resolveAccent(config, state, darkBackground, tint)
        val track = withAlpha(tint, config.trackAlpha.coerceIn(0, 255))
        val boltVisible = state.charging && config.showBolt
        val topValueVisible = config.showValue && !config.valueCentred && !boltVisible
        // The top break only exists for content actually rendered at twelve o'clock.
        val notch = when {
            boltVisible -> (measure("◆", 22f, config.valueWeight) + 8f).coerceIn(12f, 42f)
            topValueVisible -> (measure(state.batteryLevel.coerceIn(0, 100).toString(), config.valueSize.toFloat(), config.valueWeight) + 12f).coerceIn(0f, 64f)
            else -> 0f
        }

        if (config.trioStyle == 1) drawRoundedBattery(canvas, accent, config.ringStroke, notch)
        else drawRingBattery(canvas, accent, config.ringStroke, notch)

        if (config.showWifi) {
            val compact = config.valueCentred || boltVisible
            drawWifi(canvas, CX, if (compact) 35f else 55f, tint, track, state.wifiLevel, config.arcStroke, compact)
        }

        if (config.showMobile && config.signalMode == 0) {
            val twoSimRows = config.dualSim && !config.showWifi && !boltVisible &&
                state.simOneLevel >= 0 && state.simTwoLevel >= 0
            if (twoSimRows) {
                drawDots(canvas, CX, 77f, state.simOneLevel, accent, track, 0.85f)
                drawDots(canvas, CX, 92f, state.simTwoLevel, accent, track, 0.85f)
            } else {
                drawDots(canvas, CX, 83f, state.mobileLevel, accent, track, 1f)
            }
        }

        if (config.showValue) {
            val centered = config.valueCentred || boltVisible
            drawTextFitted(
                canvas,
                state.batteryLevel.coerceIn(0, 100).toString(),
                CX,
                if (centered) 71f else 31f,
                config.valueSize.toFloat(),
                config.valueWeight,
                accent,
                68f,
            )
        }
        if (boltVisible) drawBolt(canvas, CX, 27f, if (state.quickCharge) QUICK_CHARGE else accent)

        val networkLabel = state.networkType.uppercase().takeIf { it.isNotBlank() }
        if (config.showMobile && networkLabel != null && config.mobileTypeMode == 1) {
            val baseline = when {
                !config.showValue -> 67f
                config.valueCentred && !boltVisible -> 35f
                else -> 48f
            }
            drawNetworkType(canvas, networkLabel, CX, baseline, config.typeSize.toFloat(), config.typeWeight, accent, config.typeSuffixScale, 68f)
        } else if (config.showMobile && networkLabel != null && config.mobileTypeMode == 2) {
            val size = config.outTypeSizeDp.coerceIn(6, 30) * DESIGN_HEIGHT / 36f
            val centre = 105f - config.outTypeMarginRightDp.coerceIn(0, 12) * DESIGN_HEIGHT / 36f
            drawNetworkType(canvas, networkLabel, centre, 67f, size, config.typeWeight, accent, config.typeSuffixScale, 76f)
        }

        if (config.showMobile && config.signalMode == 1 && config.stackedSignal) {
            val centre = 26f - config.outSignalMarginDp.coerceIn(0, 12) * DESIGN_HEIGHT / 36f
            val bars = if (config.dataSimOnly) state.mobileLevel
                else state.simOneLevel.takeIf { it >= 0 } ?: state.mobileLevel
            val dots = if (!config.dataSimOnly && state.simTwoLevel >= 0) state.simTwoLevel else -1
            drawStackedSignal(canvas, centre, 59f, bars, dots, accent, track, config.outSignalSizeDp)
        }

        canvas.restoreToCount(save)
    }

    private fun drawRingBattery(canvas: Canvas, color: Int, stroke: Int, notch: Float) {
        arcRect.set(CX - RADIUS, CY - RADIUS, CX + RADIUS, CY + RADIUS)
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke.coerceIn(3, 28).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        if (notch <= 0f) {
            canvas.drawCircle(CX, CY, RADIUS, paint)
        } else {
            val ratio = (notch / (RADIUS * 2f)).coerceIn(0f, 0.94f)
            val gapDegrees = Math.toDegrees(2.0 * asin(ratio.toDouble() / 2.0)).toFloat() + 4f
            canvas.drawArc(arcRect, -90f + gapDegrees / 2f, 360f - gapDegrees, false, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawRoundedBattery(canvas: Canvas, color: Int, stroke: Int, notch: Float) {
        val left = CX - 45f
        val right = CX + 45f
        val top = 14f
        val bottom = 106f
        val radius = 15f
        val gap = notch.coerceIn(0f, 70f) / 2f
        shapePath.reset()
        shapePath.moveTo(left + radius, top)
        if (gap > 0f) {
            shapePath.lineTo(CX - gap, top)
            shapePath.moveTo(CX + gap, top)
            shapePath.lineTo(right - radius, top)
        } else shapePath.lineTo(right - radius, top)
        shapePath.quadTo(right, top, right, top + radius)
        shapePath.lineTo(right, bottom - radius)
        shapePath.quadTo(right, bottom, right - radius, bottom)
        shapePath.lineTo(left + radius, bottom)
        shapePath.quadTo(left, bottom, left, bottom - radius)
        shapePath.lineTo(left, top + radius)
        shapePath.quadTo(left, top, left + radius, top)
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke.coerceIn(3, 28).toFloat()
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        canvas.drawPath(shapePath, paint)
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawWifi(canvas: Canvas, cx: Float, cy: Float, color: Int, track: Int, level: Int, stroke: Int, compact: Boolean) {
        val radii = if (compact) floatArrayOf(18f, 12f, 6f) else floatArrayOf(25f, 17f, 9f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke.coerceIn(2, 20) * if (compact) 0.75f else 0.65f
        paint.strokeCap = Paint.Cap.ROUND
        radii.forEachIndexed { i, radius ->
            arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
            paint.color = if (i < level.coerceIn(0, 4).coerceAtMost(3)) color else track
            canvas.drawArc(arcRect, 225f, 90f, false, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = if (level > 0) color else track
        canvas.drawCircle(cx, cy + radii.last() * 0.75f, if (compact) 3f else 4f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawDots(canvas: Canvas, cx: Float, y: Float, level: Int, color: Int, track: Int, scale: Float) {
        val pitch = 13f * scale
        val radius = 3.6f * scale
        for (i in 0..3) {
            paint.style = Paint.Style.FILL
            paint.color = if (i < level.coerceIn(0, 4)) color else track
            canvas.drawCircle(cx + (i - 1.5f) * pitch, y, radius, paint)
        }
    }

    private fun drawStackedSignal(canvas: Canvas, cx: Float, cy: Float, barsLevel: Int, dotsLevel: Int, color: Int, track: Int, sizeDp: Int) {
        val scale = (sizeDp / 15f).coerceIn(0.55f, 1.4f)
        val barWidth = 5.8f * scale
        val pitch = 9.6f * scale
        val heights = floatArrayOf(10f, 18f, 27f, 36f).map { it * scale }
        for (i in 0..3) {
            val x = cx + (i - 1.5f) * pitch
            paint.color = if (i < barsLevel.coerceIn(0, 4)) color else track
            paint.style = Paint.Style.FILL
            arcRect.set(x - barWidth / 2f, cy - heights[i] / 2f, x + barWidth / 2f, cy + heights[i] / 2f)
            canvas.drawRoundRect(arcRect, barWidth / 2f, barWidth / 2f, paint)
            if (dotsLevel >= 0) {
                paint.color = if (i < dotsLevel.coerceIn(0, 4)) color else track
                canvas.drawCircle(x, cy + 25f * scale, 2.3f * scale, paint)
            }
        }
    }

    private fun drawTextFitted(canvas: Canvas, text: String, cx: Float, baseline: Float, size: Float, weight: Int, color: Int, maxWidth: Float) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif", weight.coerceIn(100, 900), false)
        paint.textSize = size.coerceIn(8f, 64f)
        val measured = paint.measureText(text)
        if (measured > maxWidth && measured > 0f) paint.textSize *= maxWidth / measured
        canvas.drawText(text, cx, baseline, paint)
    }

    private fun drawNetworkType(canvas: Canvas, text: String, cx: Float, baseline: Float, size: Float, weight: Int, color: Int, suffixScale: Int, maxWidth: Float) {
        val label = text.uppercase()
        val shrunkA = label.endsWith("A") && suffixScale in 1..99
        val base = if (shrunkA) label.dropLast(1) else label
        val safeSize = size.coerceIn(8f, 64f)
        paint.typeface = Typeface.create("sans-serif", weight.coerceIn(100, 900), false)
        paint.textAlign = Paint.Align.LEFT
        paint.color = color
        paint.textSize = safeSize
        val baseWidth = paint.measureText(base)
        paint.textSize = if (shrunkA) safeSize * suffixScale / 100f else safeSize
        val suffixWidth = if (shrunkA) paint.measureText("A") else 0f
        val total = baseWidth + suffixWidth
        val factor = if (total > maxWidth && total > 0f) maxWidth / total else 1f
        val start = cx - total * factor / 2f
        paint.textSize = safeSize * factor
        canvas.drawText(base, start, baseline, paint)
        if (shrunkA) {
            paint.textSize = safeSize * suffixScale / 100f * factor
            canvas.drawText("A", start + baseWidth * factor, baseline, paint)
        }
        paint.textAlign = Paint.Align.CENTER
    }

    private fun drawBolt(canvas: Canvas, cx: Float, baselineY: Float, color: Int) {
        shapePath.reset()
        shapePath.moveTo(cx + 2f, baselineY - 13f)
        shapePath.lineTo(cx - 8f, baselineY + 1f)
        shapePath.lineTo(cx - 1f, baselineY + 1f)
        shapePath.lineTo(cx - 4f, baselineY + 14f)
        shapePath.lineTo(cx + 9f, baselineY - 3f)
        shapePath.lineTo(cx + 2f, baselineY - 3f)
        shapePath.close()
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawPath(shapePath, paint)
    }

    private fun measure(text: String, size: Float, weight: Int): Float {
        paint.typeface = Typeface.create("sans-serif", weight.coerceIn(100, 900), false)
        paint.textSize = size.coerceIn(8f, 64f)
        return paint.measureText(text)
    }

    private fun resolveAccent(config: StatusBarComboStyleOneRuntime, state: StatusComboStyleOneGlyphState, darkBackground: Boolean, tint: Int): Int {
        if (!config.roleColors) return tint
        return when {
            state.charging -> if (darkBackground) config.chargingColorDarkArgb else config.chargingColorLightArgb
            state.batteryLevel <= 5 -> if (darkBackground) config.criticalColorDarkArgb else config.criticalColorLightArgb
            state.batteryLevel <= config.lowThreshold.coerceIn(1, 100) ->
                if (darkBackground) config.lowColorDarkArgb else config.lowColorLightArgb
            else -> tint
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
    private fun luminance(color: Int): Float = (0.2126f * Color.red(color) + 0.7152f * Color.green(color) + 0.0722f * Color.blue(color)) / 255f
}
