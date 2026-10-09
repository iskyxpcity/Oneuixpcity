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
        val cellWidth = width / 3f
        val labelHeight = (14f * density).coerceAtMost(height * 0.28f)
        val glyphHeight = (height - labelHeight).toInt().coerceAtLeast(1)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val samples = listOf(
            Triple(
                0xFF202124.toInt(),
                Color.WHITE,
                StatusComboStyleOneGlyphState(
                    batteryLevel = 68,
                    charging = true,
                    quickCharge = false,
                    wifiLevel = 4,
                    mobileLevel = 3,
                    simOneLevel = 3,
                    simTwoLevel = 2,
                    networkType = "5GA",
                    tintColor = Color.WHITE,
                ),
            ),
            Triple(
                0xFFF4F4F4.toInt(),
                Color.BLACK,
                StatusComboStyleOneGlyphState(
                    batteryLevel = 35,
                    lowBatteryWarning = true,
                    wifiLevel = 3,
                    mobileLevel = 3,
                    simOneLevel = 3,
                    simTwoLevel = 2,
                    networkType = "5G",
                    tintColor = Color.BLACK,
                ),
            ),
            Triple(
                0xFF202124.toInt(),
                Color.WHITE,
                StatusComboStyleOneGlyphState(
                    batteryLevel = 12,
                    wifiLevel = 2,
                    mobileLevel = 2,
                    simOneLevel = 2,
                    simTwoLevel = 1,
                    networkType = "4G",
                    tintColor = Color.WHITE,
                ),
            ),
        )
        val labels = listOf(
            context.getString(R.string.status_combo_style_one_color_charging),
            context.getString(R.string.status_combo_style_one_color_low),
            context.getString(R.string.status_combo_style_one_color_critical),
        )

        samples.forEachIndexed { index, sample ->
            val left = index * cellWidth
            val right = if (index == samples.lastIndex) width.toFloat() else (index + 1) * cellWidth
            canvas.drawRect(left, 0f, right, height.toFloat(), Paint().apply {
                color = sample.first
            })

            val targetWidth = (cellWidth - 8f * density).toInt().coerceAtLeast(1)
            val targetHeight = (glyphHeight - 2f * density).toInt().coerceAtLeast(1)
            val save = canvas.save()
            canvas.translate(left + (cellWidth - targetWidth) / 2f, 0f)
            StatusComboStyleOneGlyphRenderer.draw(
                canvas, targetWidth, targetHeight, config, sample.third,
            )
            canvas.restoreToCount(save)

            labelPaint.color = sample.second
            canvas.drawText(
                labels[index],
                (left + right) / 2f,
                height - (labelHeight - labelPaint.textSize) / 2f,
                labelPaint,
            )
        }
    }
}
