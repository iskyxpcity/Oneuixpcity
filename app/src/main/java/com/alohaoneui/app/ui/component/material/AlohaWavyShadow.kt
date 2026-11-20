package com.alohaoneui.app.ui.component.material

import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Aloha's dedicated WavyShape shadow renderer.
 *
 * Aloha does not send its wave outline through the normal graphics-layer
 * elevation shadow. It converts the outline to a Path and draws a transparent
 * Paint with a native shadow layer (radius = elevation px, y = radius * 0.9).
 * Other shapes deliberately keep their existing Material shadow path.
 */
internal fun Modifier.alohaWavyShadow(
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
            // Aloha uses a transparent fill; only the native shadow is visible.
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                radiusPx,
                0f,
                radiusPx * 0.9f,
                android.graphics.Color.argb(46, 0, 0, 0),
            )
        }

        // Aloha installs this as a behind-content draw node: the shadow
        // is painted first, then the component content is drawn over it.
        onDrawBehind {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(path.asAndroidPath(), shadowPaint)
            }
        }
    }
}


/**
 * Button counterpart to the Material 3 container elevation used by detail rows.
 * It keeps the button's own preference independent while matching the same
 * graphics-layer shadow geometry for ordinary shapes and Aloha's path renderer
 * for WavyShape.
 */
internal fun Modifier.alohaButtonShadow(
    shape: androidx.compose.ui.graphics.Shape,
    elevation: Dp,
): Modifier {
    if (elevation <= 0.dp) return this
    if (shape is WavyShape) return alohaWavyShadow(shape, elevation)
    return graphicsLayer {
        shadowElevation = elevation.toPx()
        this.shape = shape
        clip = false
    }
}
