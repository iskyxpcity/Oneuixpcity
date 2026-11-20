package com.alohaoneui.app.ui.component.material

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * Draws the non-convex WavyShape shadow through an isolated Compose graphics
 * layer. RenderEffect blurs the filled outline itself, so concave wave valleys
 * remain part of the silhouette instead of relying on Paint.setShadowLayer's
 * hardware-canvas path behavior.
 *
 * The offscreen layer is expanded by three blur radii on every side. This keeps
 * the blur from being clipped at the component bounds. The actual content is
 * drawn afterward and is never blurred.
 */
internal fun Modifier.alohaWavyShadow(
    shape: Shape,
    elevation: Dp,
): Modifier {
    if (shape !is WavyShape || elevation <= 0.dp) return this
    return this then WavyShadowElement(shape, elevation)
}

private class WavyShadowElement(
    private val shape: Shape,
    private val elevation: Dp,
) : ModifierNodeElement<WavyShadowNode>() {
    override fun create(): WavyShadowNode = WavyShadowNode(shape, elevation)

    override fun update(node: WavyShadowNode) {
        node.shape = shape
        node.elevation = elevation
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "alohaWavyShadow"
        properties["shape"] = shape
        properties["elevation"] = elevation
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is WavyShadowElement &&
            shape == other.shape && elevation == other.elevation)

    override fun hashCode(): Int = 31 * shape.hashCode() + elevation.hashCode()
}

private class WavyShadowNode(
    var shape: Shape,
    var elevation: Dp,
) : Modifier.Node(), DrawModifierNode {
    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null
    private var previousBlurRadius = Float.NaN
    private val shadowPaint = Paint().apply { color = Color.Black.copy(alpha = 46f / 255f) }

    override fun onAttach() {
        shadowLayer = requireGraphicsContext().createGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }

    override fun onDetach() {
        shadowLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        shadowLayer = null
        previousBlurRadius = Float.NaN
    }

    override fun ContentDrawScope.draw() {
        val layer = shadowLayer
        val radius = elevation.toPx()
        if (layer != null && radius > 0f && size.width > 0f && size.height > 0f) {
            val offsetY = radius * 0.9f
            val padding = ceil(radius * 3f + offsetY + 1f).toInt()
            val layerWidth = ceil(size.width).toInt() + padding * 2
            val layerHeight = ceil(size.height).toInt() + padding * 2
            val outline = shape.createOutline(size, layoutDirection, this)

            if (previousBlurRadius != radius) {
                layer.renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                previousBlurRadius = radius
            }

            layer.record(
                size = IntSize(layerWidth, layerHeight),
                density = this,
                layoutDirection = layoutDirection,
            ) {
                drawContext.canvas.save()
                drawContext.canvas.translate(padding.toFloat(), padding.toFloat())
                drawOutline(outline, shadowPaint)
                drawContext.canvas.restore()
            }

            drawContext.canvas.save()
            drawContext.canvas.translate(-padding.toFloat(), -padding.toFloat() + offsetY)
            drawLayer(layer)
            drawContext.canvas.restore()
        }
        drawContent()
    }
}

/**
 * Button counterpart to the Material 3 container elevation used by detail rows.
 * Ordinary shapes keep the native graphics-layer elevation path; only WavyShape
 * uses the custom non-convex outline renderer above.
 */
internal fun Modifier.alohaButtonShadow(
    shape: Shape,
    elevation: Dp,
): Modifier {
    if (elevation <= 0.dp) return this
    if (shape is WavyShape) return alohaWavyShadow(shape, elevation)
    return this.zIndex(1f).graphicsLayer {
            shadowElevation = elevation.toPx()
            this.shape = shape
            clip = false
        }
}
