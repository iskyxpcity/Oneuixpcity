package com.oneuixp.app.ui.screen.status

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusComboStyleOneGlyphRenderer
import io.github.soclear.oneuix.common.StatusComboStyleOneGlyphState

/** Preview surface using the same renderer as the status-bar icon. */
class StatusComboStyleOnePreviewView(context: Context) : View(context) {
    private var config = Preference.SystemUI.StatusBarComboStyleOneRuntime()

    fun setConfig(next: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        config = next.copy(criticalThreshold = 20)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val density = resources.displayMetrics.density
        val darkBackground = 0xFF202124.toInt()
        val lightBackground = 0xFFF4F4F4.toInt()
        val labelHeight = (14f * density).coerceAtMost(height * 0.28f)
        val glyphHeight = (height - labelHeight).toInt().coerceAtLeast(1)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        fun sample(
            level: Int,
            dark: Boolean,
            label: String,
            charging: Boolean = false,
            low: Boolean = false,
            wifi: Int = 3,
        ): Triple<Int, Int, Pair<String, StatusComboStyleOneGlyphState>> {
            val tint = if (dark) Color.WHITE else Color.BLACK
            return Triple(
                if (dark) darkBackground else lightBackground,
                tint,
                label to StatusComboStyleOneGlyphState(
                    batteryLevel = level,
                    charging = charging,
                    lowBatteryWarning = low,
                    wifiLevel = wifi,
                    mobileLevel = 3,
                    simOneLevel = 3,
                    simTwoLevel = 2,
                    networkType = if (charging) "5GA" else if (level < 20) "4G" else "5G",
                    tintColor = tint,
                ),
            )
        }

        val samples = listOf(
            sample(68, true, context.getString(R.string.status_combo_style_one_preview_charging_dark), charging = true, wifi = 4),
            sample(68, false, context.getString(R.string.status_combo_style_one_preview_charging_light), charging = true, wifi = 4),
            sample(35, true, context.getString(R.string.status_combo_style_one_preview_low_dark), low = true, wifi = 3),
            sample(35, false, context.getString(R.string.status_combo_style_one_preview_low_light), low = true, wifi = 3),
            sample(12, true, context.getString(R.string.status_combo_style_one_preview_critical_dark), wifi = 2),
            sample(12, false, context.getString(R.string.status_combo_style_one_preview_critical_light), wifi = 2),
        )
        val cellWidth = width / samples.size.toFloat()

        samples.forEachIndexed { index, sample ->
            val left = index * cellWidth
            val right = if (index == samples.lastIndex) width.toFloat() else (index + 1) * cellWidth
            canvas.drawRect(left, 0f, right, height.toFloat(), Paint().apply {
                color = sample.first
            })

            val targetWidth = (cellWidth - 4f * density).toInt().coerceAtLeast(1)
            val targetHeight = ((glyphHeight - 2f * density) *
                (StatusComboStyleOneGlyphRenderer.BASE_GLYPH_HEIGHT_DP / 36f)).toInt().coerceAtLeast(1)
            val save = canvas.save()
            canvas.clipRect(left, 0f, right, glyphHeight.toFloat())
            val targetLeft = left + (cellWidth - targetWidth) / 2f
            val targetTop = (glyphHeight - targetHeight) / 2f +
                config.glyphOffsetYDp * density
            val scalePercent = config.glyphScalePercent.coerceIn(50, 150) / 100f
            // Keep the established preview transform centered on the complete glyph cell.
            canvas.translate(targetLeft + config.glyphOffsetXDp * density, targetTop)
            canvas.translate(targetWidth / 2f, targetHeight / 2f)
            canvas.scale(scalePercent, scalePercent)
            canvas.translate(-targetWidth / 2f, -targetHeight / 2f)
            StatusComboStyleOneGlyphRenderer.draw(
                canvas, targetWidth, targetHeight, config, sample.third.second,
            )
            canvas.restoreToCount(save)

            labelPaint.color = sample.second
            val label = sample.third.first
            val baseline = height - (labelHeight - labelPaint.textSize) / 2f
            val maxLabelWidth = (cellWidth - 4f * density).coerceAtLeast(1f)
            val textScale = if (labelPaint.measureText(label) > maxLabelWidth) {
                maxLabelWidth / labelPaint.measureText(label)
            } else 1f
            val labelSave = canvas.save()
            canvas.scale(textScale, 1f, (left + right) / 2f, baseline)
            canvas.drawText(label, (left + right) / 2f, baseline, labelPaint)
            canvas.restoreToCount(labelSave)
        }
        if (config.chargingDisplayMode == 1) postInvalidateDelayed(5_000L)
    }
}
