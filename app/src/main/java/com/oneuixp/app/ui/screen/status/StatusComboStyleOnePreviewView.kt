package com.oneuixp.app.ui.screen.status

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import kotlin.math.roundToInt
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusComboStyleOneGlyphRenderer
import io.github.soclear.oneuix.common.StatusComboStyleOneGlyphState

/** Preview surface using the same renderer as the status-bar icon. */
class StatusComboStyleOnePreviewView(context: Context) : View(context) {
    private var config = Preference.SystemUI.StatusBarComboStyleOneRuntime()

    fun setConfig(next: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        config = next
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFF202124.toInt())
        val targetWidth = (StatusComboStyleOneGlyphRenderer.glyphWidthDp(config) *
            resources.displayMetrics.density).roundToInt()
        val targetHeight = (StatusComboStyleOneGlyphRenderer.glyphHeightDp(config) *
            resources.displayMetrics.density).roundToInt()
        val save = canvas.save()
        canvas.translate((width - targetWidth) / 2f, (height - targetHeight) / 2f)
        StatusComboStyleOneGlyphRenderer.draw(
            canvas, targetWidth, targetHeight, config,
            StatusComboStyleOneGlyphState(
                batteryLevel = 68,
                charging = true,
                wifiLevel = 4,
                mobileLevel = 3,
                simOneLevel = 3,
                simTwoLevel = 2,
                networkType = "5GA",
                tintColor = Color.WHITE,
            ),
        )
        canvas.restoreToCount(save)
    }
}
