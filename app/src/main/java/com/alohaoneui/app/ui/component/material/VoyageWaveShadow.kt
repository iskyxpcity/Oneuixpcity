package com.alohaoneui.app.ui.component.material

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.graphics.asComposePaint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Voyage's wave-shadow path, ported as a retained Modifier.Node.
 *
 * Voyage treats wgo0/d23 wave shapes specially: it builds a transparent
 * anti-aliased Paint with a black 18% shadow, then draws the cached Shape
 * outline Path behind the content. Ordinary shapes continue to use the
 * graphics-layer/RenderNode shadow path.
 *
 * The node is retained across recompositions. Geometry and Paint are cached
 * independently, so a new equivalent WavyShape instance does not force a
 * Path rebuild, and an elevation-only change updates the shadow layer without
 * rebuilding the Path.
 */
private data class VoyageWaveShadowElement(
    val shape: WavyShape,
    val elevation: Dp,
) : ModifierNodeElement<VoyageWaveShadowNode>() {

    override fun create() = VoyageWaveShadowNode(shape, elevation)

    override fun update(node: VoyageWaveShadowNode) {
        node.update(shape, elevation)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "voyageWaveShadow"
        properties["shape"] = shape
        properties["elevation"] = elevation
    }
}

private data class VoyageWaveGeometryKey(
    val shape: WavyShapeGeometryKey,
    val width: Float,
    val height: Float,
    val layoutDirection: LayoutDirection,
    val density: Float,
    val fontScale: Float,
)

private class VoyageWaveShadowNode(
    private var shape: WavyShape,
    private var elevation: Dp,
) : Modifier.Node(), DrawModifierNode {

    private var cachedGeometryKey: VoyageWaveGeometryKey? = null
    private var cachedPath: Path? = null
    private var cachedNativePaint: AndroidPaint? = null
    private var cachedPaint: Paint? = null
    private var cachedElevationPx = Float.NaN

    fun update(shape: WavyShape, elevation: Dp) {
        this.shape = shape
        this.elevation = elevation
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val elevationPx = elevation.toPx()
        if (elevationPx > 0f) {
            val geometryKey = VoyageWaveGeometryKey(
                shape = shape.shadowGeometryKey,
                width = size.width,
                height = size.height,
                layoutDirection = layoutDirection,
                density = density,
                fontScale = fontScale,
            )

            if (cachedGeometryKey != geometryKey || cachedPath == null) {
                cachedPath = shape.createOutline(size, layoutDirection, this).toPath()
                cachedGeometryKey = geometryKey
            }

            val path = cachedPath
            if (path != null) {
                val nativePaint = cachedNativePaint ?: AndroidPaint(7).also { created ->
                    created.color = Color.Transparent.toArgb()
                    cachedNativePaint = created
                    cachedPaint = created.asComposePaint()
                }
                val paint = cachedPaint ?: nativePaint.asComposePaint().also {
                    cachedPaint = it
                }

                if (cachedElevationPx != elevationPx) {
                    nativePaint.setShadowLayer(
                        elevationPx,
                        0f,
                        elevationPx * 0.9f,
                        VOYAGE_WAVE_SHADOW_COLOR,
                    )
                    cachedElevationPx = elevationPx
                }

                drawIntoCanvas { canvas ->
                    canvas.drawPath(path, paint)
                }
            }
        }

        // Voyage's z58/tia callback paints the shadow first, then draws content.
        drawContent()
    }

    override fun onDetach() {
        cachedGeometryKey = null
        cachedPath = null
        cachedNativePaint = null
        cachedPaint = null
        cachedElevationPx = Float.NaN
    }

    private fun Outline.toPath(): Path = when (this) {
        is Outline.Generic -> path
        is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(rect) }
    }

    private companion object {
        val VOYAGE_WAVE_SHADOW_COLOR: Int =
            Color.Black.copy(alpha = 0.18f).toArgb()
    }
}

/**
 * Wave-only shadow entry point. It deliberately does not route WavyShape
 * through graphicsLayer shadowElevation; Voyage uses its dedicated Path/Paint
 * branch for this shape family.
 */
internal fun Modifier.voyageWavyShadow(
    shape: Shape,
    elevation: Dp,
): Modifier {
    val waveShape = shape as? WavyShape ?: return this
    if (elevation <= 0.dp) return this
    return this.then(VoyageWaveShadowElement(waveShape, elevation))
}

/**
 * Compatibility name for existing Aloha components. The implementation is
 * now the Voyage retained-node renderer above, not the previous drawWithCache
 * / nativeCanvas path.
 */
internal fun Modifier.alohaWavyShadow(
    shape: Shape,
    elevation: Dp,
): Modifier = voyageWavyShadow(shape, elevation)

/**
 * Button counterpart: wave shapes use Voyage's dedicated renderer; ordinary
 * shapes retain the existing Material graphics-layer shadow.
 */
internal fun Modifier.alohaButtonShadow(
    shape: Shape,
    elevation: Dp,
): Modifier {
    if (elevation <= 0.dp) return this
    if (shape is WavyShape) return voyageWavyShadow(shape, elevation)
    return zIndex(1f).graphicsLayer {
        shadowElevation = elevation.toPx()
        this.shape = shape
        clip = false
    }
}
