package com.alohaoneui.app.ui.component.material

import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Voyage's dedicated WavyShape shadow renderer.
 *
 * Voyage does not send its wave outline through the normal graphics-layer
 * elevation shadow. It converts the outline to a Path and draws a transparent
 * Paint with a native shadow layer (radius = elevation px, y = radius * 0.9).
 * Other shapes deliberately keep their existing Material shadow path.
 */
internal fun Modifier.voyageWavyShadow(
    shape: androidx.compose.ui.graphics.Shape,
    elevation: Dp,
): Modifier {
    if (shape !is WavyShape || elevation <= 0.dp) return this

    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Generic -> outline.path
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        }
        val radiusPx = elevation.toPx()
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // Voyage uses a transparent fill; only the native shadow is visible.
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                radiusPx,
                0f,
                radiusPx * 0.9f,
                android.graphics.Color.argb(46, 0, 0, 0),
            )
        }

        onDrawWithContent {
            drawContent()
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(path.asAndroidPath(), shadowPaint)
            }
        }
    }
}
