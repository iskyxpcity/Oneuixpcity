package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import java.util.concurrent.ConcurrentHashMap

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

        cache[key]?.let { return it }
        val outline = buildOutline(size.width, size.height, tl, tr, br, bl, density)
        val existing = cache.putIfAbsent(key, outline)
        if (cache.size > 512) cache.clear()
        return existing ?: outline
    }

    private companion object {
        // Four samples per scallop preserve a rounded gear-like wave while
        // cutting path vertices in half. Values are shared by every component.
        private val profile = floatArrayOf(0f, 1f, 0f, -1f)
        private val cache = ConcurrentHashMap<Key, Outline>(128)

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

            val amplitude = (0.58f * density).coerceAtMost(min(width, height) * 0.018f)
            // Keep the full wave inside the component bounds. The centerline is inset
            // by one amplitude, so outward crests reach the bounds without clipping.
            val inset = amplitude
            val shapeWidth = (width - inset * 2f).coerceAtLeast(0.1f)
            val shapeHeight = (height - inset * 2f).coerceAtLeast(0.1f)
            val tl = (topLeft - inset).coerceAtLeast(0f)
            val tr = (topRight - inset).coerceAtLeast(0f)
            val br = (bottomRight - inset).coerceAtLeast(0f)
            val bl = (bottomLeft - inset).coerceAtLeast(0f)
            val top = (shapeWidth - tl - tr).coerceAtLeast(0f)
            val right = (shapeHeight - tr - br).coerceAtLeast(0f)
            val bottom = (shapeWidth - br - bl).coerceAtLeast(0f)
            val left = (shapeHeight - bl - tl).coerceAtLeast(0f)
            val quarterPi = PI.toFloat() / 2f
            val tlArc = tl * quarterPi
            val trArc = tr * quarterPi
            val brArc = br * quarterPi
            val blArc = bl * quarterPi
            val perimeter = top + trArc + right + brArc + bottom + blArc + left + tlArc
            if (perimeter <= 0.5f) {
                return Outline.Rounded(RoundRect(0f, 0f, width, height))
            }

            // Fixed physical wave scale: corners do not shrink the ripple and every
            // side uses the same wavelength and amplitude.
            val waveCount = (perimeter / (8f * density).coerceAtLeast(1f))
                .toInt().coerceIn(12, 48)
            val sampleCount = waveCount * profile.size
            val step = perimeter / sampleCount
            val frame = FloatArray(4)
            val path = Path()

            fun frameAt(distance: Float) {
                var d = distance
                if (d < top) {
                    frame[0] = inset + tl + d
                    frame[1] = inset
                    frame[2] = 0f
                    frame[3] = -1f
                    return
                }
                d -= top
                if (d < trArc && tr > 0f) {
                    val angle = -quarterPi + d / tr
                    val c = cos(angle)
                    val s = sin(angle)
                    frame[0] = inset + shapeWidth - tr + tr * c
                    frame[1] = inset + tr + tr * s
                    frame[2] = c
                    frame[3] = s
                    return
                }
                d -= trArc
                if (d < right) {
                    frame[0] = inset + shapeWidth
                    frame[1] = inset + tr + d
                    frame[2] = 1f
                    frame[3] = 0f
                    return
                }
                d -= right
                if (d < brArc && br > 0f) {
                    val angle = d / br
                    val c = cos(angle)
                    val s = sin(angle)
                    frame[0] = inset + shapeWidth - br + br * c
                    frame[1] = inset + shapeHeight - br + br * s
                    frame[2] = c
                    frame[3] = s
                    return
                }
                d -= brArc
                if (d < bottom) {
                    frame[0] = inset + shapeWidth - br - d
                    frame[1] = inset + shapeHeight
                    frame[2] = 0f
                    frame[3] = 1f
                    return
                }
                d -= bottom
                if (d < blArc && bl > 0f) {
                    val angle = quarterPi + d / bl
                    val c = cos(angle)
                    val s = sin(angle)
                    frame[0] = inset + bl + bl * c
                    frame[1] = inset + shapeHeight - bl + bl * s
                    frame[2] = c
                    frame[3] = s
                    return
                }
                d -= blArc
                if (d < left) {
                    frame[0] = inset
                    frame[1] = inset + shapeHeight - bl - d
                    frame[2] = -1f
                    frame[3] = 0f
                    return
                }
                d -= left
                if (tl > 0f) {
                    val angle = PI.toFloat() + d / tl
                    val c = cos(angle)
                    val s = sin(angle)
                    frame[0] = inset + tl + tl * c
                    frame[1] = inset + tl + tl * s
                    frame[2] = c
                    frame[3] = s
                } else {
                    frame[0] = inset + tl
                    frame[1] = inset
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
