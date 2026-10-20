package com.oneuialoha.hook.util

import android.graphics.Paint

/** Captures the actual Paint passed into the hardware-proven clock span. */
internal object StatusBarClockDrawPaintBridge {
    @Volatile
    private var drawPaint: Paint? = null

    fun capture(base: Paint) {
        drawPaint = Paint(base)
    }

    fun currentPaint(): Paint? = drawPaint?.let(::Paint)
}
