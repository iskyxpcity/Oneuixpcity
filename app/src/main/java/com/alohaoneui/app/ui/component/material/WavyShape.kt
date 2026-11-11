package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutDirection
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Independent Wavy geometry engine.
 *
 * The outline is a rounded-rectangle perimeter parameterized by physical arc length.
 * A periodic normal displacement is then evaluated from a fixed profile table.
 * Geometry is deliberately independent from Material colors, borders and shadows:
 * those remain owned by the caller's Material theme components.
 */
internal class WavyShape private constructor(
    private val density: Float,
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    internal constructor(
        density: Float,
        topStart: Dp,
        topEnd: Dp,
        bottomEnd: Dp,
        bottomStart: Dp,
    ) : this(
        density,
        CornerSize(topStart),
        CornerSize(topEnd),
        CornerSize(bottomEnd),
        CornerSize(bottomStart),
    )

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = WavyShape(density, topStart, topEnd, bottomEnd, bottomStart)

    private data class Key(
        val density: Float,
        val width: Float,
        val height: Float,
        val direction: LayoutDirection,
        val topStart: Float,
        val topEnd: Float,
        val bottomEnd: Float,
        val bottomStart: Float,
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val maxRadius = min(size.width, size.height) / 2f
        val ts = topStart.coerceIn(0f, maxRadius)
        val te = topEnd.coerceIn(0f, maxRadius)
        val be = bottomEnd.coerceIn(0f, maxRadius)
        val bs = bottomStart.coerceIn(0f, maxRadius)
        val tl = if (layoutDirection == LayoutDirection.Ltr) ts else te
        val tr = if (layoutDirection == LayoutDirection.Ltr) te else ts
        val br = if (layoutDirection == LayoutDirection.Ltr) be else bs
        val bl = if (layoutDirection == LayoutDirection.Ltr) bs else be
        val key = Key(density, size.width, size.height, layoutDirection, tl, tr, br, bl)

        synchronized(cache) {
            cache[key]?.let { return it }
        }
        val outline = buildOutline(size.width, size.height, tl, tr, br, bl, density)
        synchronized(cache) {
            cache[key]?.let { return it }
            cache[key] = outline
        }
        return outline
    }

    private companion object {
        // Eight evenly spaced samples form a smooth, repeatable scallop cycle.
        // A static table avoids per-point trigonometric work during path construction.
        private val profile = floatArrayOf(
            0f, 0.7071068f, 1f, 0.7071068f,
            0f, -0.7071068f, -1f, -0.7071068f,
        )
        private val cache = object : LinkedHashMap<Key, Outline>(96, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Outline>?): Boolean =
                size > 384
        }

        private fun buildOutline(
            width: Float,
            height: Float,
            topLeft: Float,
            topRight: Float,
            bottomRight: Float,
            bottomLeft: Float,
            density: Float,
        ): Outline {
            if (width <= 0f || height <= 0f) {
                return Outline.Rounded(RoundRect(0f, 0f, width, height))
            }

            val top = (width - topLeft - topRight).coerceAtLeast(0f)
            val right = (height - topRight - bottomRight).coerceAtLeast(0f)
            val bottom = (width - bottomRight - bottomLeft).coerceAtLeast(0f)
            val left = (height - bottomLeft - topLeft).coerceAtLeast(0f)
            val quarterPi = PI.toFloat() / 2f
            val tlArc = topLeft * quarterPi
            val trArc = topRight * quarterPi
            val brArc = bottomRight * quarterPi
            val blArc = bottomLeft * quarterPi
            val perimeter = top + trArc + right + brArc + bottom + blArc + left + tlArc
            if (perimeter <= 0.5f) {
                return Outline.Rounded(RoundRect(0f, 0f, width, height))
            }

            // Fixed physical wave scale: corners do not shrink the ripple and every
            // side uses the same wavelength and amplitude.
            val waveCount = (perimeter / (12f * density).coerceAtLeast(1f))
                .toInt().coerceIn(8, 40)
            val amplitude = (0.72f * density).coerceAtMost(min(width, height) * 0.018f)
            val sampleCount = waveCount * profile.size
            val step = perimeter / sampleCount
            val frame = FloatArray(4)
            val path = Path()

            fun frameAt(distance: Float) {
                var d = distance
                if (d < top) {
                    frame[0] = topLeft + d
                    frame[1] = 0f
                    frame[2] = 0f
                    frame[3] = -1f
                    return
                }
                d -= top
                if (d < trArc && topRight > 0f) {
                    val angle = -quarterPi + d / topRight
                    val c = cos(angle)
                    val s = sin(angle)
                    frame[0] = width - topRight + topRight * c
                    frame[1] = topRight + topRight * s
                    frame[2] = s
                    frame[3] = -c
                    return
                }
                d -= trArc
                if (d < right) {
                    frame[0] = width
                    frame[1] = topRight + d
                    frame[2] = 1f
                    frame[3] = 0f
                    return
                }
                d -= right
                if (d < brArc && bottomRight > 0f) {
                    val angle = d / bottomRight
                    val c = cos(angle)
                    val s = sin(angle)
                    frame[0] = width - bottomRight + bottomRight * c
                    frame[1] = height - bottomRight + bottomRight * s
                    frame[2] = s
                    frame[3] = -c
                    return
                }
                d -= brArc
                if (d < bottom) {
                    frame[0] = width - bottomRight - d
                    frame[1] = height
                    frame[2] = 0f
                    frame[3] = 1f
                    return
                }
                d -= bottom
                if (d < blArc && bottomLeft > 0f) {
                    val angle = quarterPi + d / bottomLeft
                    val c = cos(angle)
                    val s = sin(angle)
                    frame[0] = bottomLeft + bottomLeft * c
                    frame[1] = height - bottomLeft + bottomLeft * s
                    frame[2] = s
                    frame[3] = -c
                    return
                }
                d -= blArc
                if (d < left) {
                    frame[0] = 0f
                    frame[1] = height - bottomLeft - d
                    frame[2] = -1f
                    frame[3] = 0f
                    return
                }
                d -= left
                if (topLeft > 0f) {
                    val angle = PI.toFloat() + d / topLeft
                    val c = cos(angle)
                    val s = sin(angle)
                    frame[0] = topLeft + topLeft * c
                    frame[1] = topLeft + topLeft * s
                    frame[2] = s
                    frame[3] = -c
                } else {
                    frame[0] = topLeft
                    frame[1] = 0f
                    frame[2] = 0f
                    frame[3] = -1f
                }
            }

            for (index in 0 until sampleCount) {
                frameAt(index * step)
                val offset = amplitude * profile[index % profile.size]
                val x = frame[0] + frame[2] * offset
                val y = frame[1] + frame[3] * offset
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            return Outline.Generic(path)
        }
    }
}
