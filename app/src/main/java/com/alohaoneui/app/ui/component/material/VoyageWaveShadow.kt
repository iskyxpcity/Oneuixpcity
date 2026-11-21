package com.alohaoneui.app.ui.component.material

import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Direct translation of Voyage's wave-only shadow branch:
 * n5n.Il -> o7g.OI -> x1j/sia -> z58(case 12) -> wxj.
 *
 * Voyage uses its cache-draw node (not the RenderNode elevation branch) for
 * wgo0. The cache builder creates the outline Path and Paint once; the
 * behind-content callback only submits that Path/Paint to the Compose Canvas.
 */
internal fun Modifier.voyageWavyShadow(
    shape: Shape,
    elevation: Dp,
): Modifier {
    val waveShape = shape as? WavyShape ?: return this
    if (elevation <= 0.dp) return this

    return drawWithCache {
        // Equivalent to ree0Var.l(size, layoutDirection, density) in Voyage.
        val outline = waveShape.createOutline(size, layoutDirection, this)
        val path = outline.toVoyagePath()

        // Voyage c9c0.l() creates Paint(7): anti-alias + filter-bitmap + dither.
        // Its fill color is transparent; the non-opaque shadow color supplies
        // the actual 18% black shadow.
        val elevationPx = elevation.toPx()
        val nativePaint = AndroidPaint(7).apply {
            color = android.graphics.Color.TRANSPARENT
            setShadowLayer(
                elevationPx,
                0f,
                elevationPx * 0.9f,
                VOYAGE_WAVE_SHADOW_COLOR,
            )
        }
        val paint = nativePaint.asComposePaint()

        // Voyage's wxj callback is onDrawBehind: shadow first, content after.
        onDrawBehind {
            drawIntoCanvas { canvas ->
                canvas.drawPath(path, paint)
            }
        }
    }
}

private fun Outline.toVoyagePath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(rect) }
}

private const val VOYAGE_WAVE_SHADOW_COLOR: Int = 0x2E000000

/** Existing call sites retain their name while using Voyage's exact cache-draw path. */
internal fun Modifier.alohaWavyShadow(
    shape: Shape,
    elevation: Dp,
): Modifier = voyageWavyShadow(shape, elevation)

/** Ordinary shapes keep the existing Material graphics-layer shadow behavior. */
internal fun Modifier.alohaButtonShadow(
    shape: Shape,
    elevation: Dp,
): Modifier {
    if (elevation <= 0.dp) return this
    if (shape is WavyShape) return voyageWavyShadow(shape, elevation)
    return androidx.compose.ui.zIndex(1f).graphicsLayer {
        shadowElevation = elevation.toPx()
        this.shape = shape
        clip = false
    }
}
