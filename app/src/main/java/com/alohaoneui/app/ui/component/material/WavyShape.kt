package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Static, arc-length-based Wavy outline.
 *
 * The wave is spatial geometry only: no animation, frame clock, mutable cache,
 * or per-frame state. A single wavelength and amplitude are used around the
 * complete rounded-rectangle perimeter, including its four corner arcs.
 *
 * Material colors, strokes, shadows and component states remain owned by the
 * Material theme components that consume this shape.
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

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) {
            return Outline.Rounded(RoundRect(0f, 0f, width, height))
        }

        val maxRadius = min(width, height) / 2f
        val ts = topStart.coerceIn(0f, maxRadius)
        val te = topEnd.coerceIn(0f, maxRadius)
        val be = bottomEnd.coerceIn(0f, maxRadius)
        val bs = bottomStart.coerceIn(0f, maxRadius)

        val topLeft = if (layoutDirection == LayoutDirection.Ltr) ts else te
        val topRight = if (layoutDirection == LayoutDirection.Ltr) te else ts
        val bottomRight = if (layoutDirection == LayoutDirection.Ltr) be else bs
        val bottomLeft = if (layoutDirection == LayoutDirection.Ltr) bs else be

        val amplitude = (WAVE_AMPLITUDE_DP * density)
            .coerceAtMost(min(width, height) * 0.018f)
        // Quadratic smoothing reduces the sampled crest to 75% of its control
        // point displacement, so inset by the same amount to avoid clipping.
        val inset = amplitude * 0.75f

        val innerWidth = (width - inset * 2f).coerceAtLeast(0.1f)
        val innerHeight = (height - inset * 2f).coerceAtLeast(0.1f)
        val tl = (topLeft - inset).coerceAtLeast(0f)
        val tr = (topRight - inset).coerceAtLeast(0f)
        val br = (bottomRight - inset).coerceAtLeast(0f)
        val bl = (bottomLeft - inset).coerceAtLeast(0f)

        val topLength = (innerWidth - tl - tr).coerceAtLeast(0f)
        val rightLength = (innerHeight - tr - br).coerceAtLeast(0f)
        val bottomLength = (innerWidth - br - bl).coerceAtLeast(0f)
        val leftLength = (innerHeight - bl - tl).coerceAtLeast(0f)

        val quarterPi = PI.toFloat() / 2f
        val tlArc = tl * quarterPi
        val trArc = tr * quarterPi
        val brArc = br * quarterPi
        val blArc = bl * quarterPi
        val perimeter = topLength + trArc + rightLength + brArc +
            bottomLength + blArc + leftLength + tlArc

        if (perimeter <= 0.5f) {
            return Outline.Rounded(RoundRect(0f, 0f, width, height))
        }

        // Fit an integer number of identical waves to the full perimeter.
        // This keeps crest spacing equal on every edge and through every corner.
        val targetWavelength = (WAVE_LENGTH_DP * density).coerceAtLeast(1f)
        val waveCount = (perimeter / targetWavelength).roundToInt().coerceAtLeast(1)
        val sampleCount = waveCount * SAMPLES_PER_WAVE
        val step = perimeter / sampleCount
        val actualWavelength = perimeter / waveCount
        val path = Path()
        val frame = FloatArray(4)

        fun frameAt(distance: Float) {
            var d = distance

            if (d < topLength) {
                frame[0] = inset + tl + d
                frame[1] = inset
                frame[2] = 0f
                frame[3] = -1f
                return
            }
            d -= topLength

            if (tr > 0f && d < trArc) {
                val angle = -quarterPi + d / tr
                val nx = cos(angle)
                val ny = sin(angle)
                frame[0] = inset + innerWidth - tr + tr * nx
                frame[1] = inset + tr + tr * ny
                frame[2] = nx
                frame[3] = ny
                return
            }
            d -= trArc

            if (d < rightLength) {
                frame[0] = inset + innerWidth
                frame[1] = inset + tr + d
                frame[2] = 1f
                frame[3] = 0f
                return
            }
            d -= rightLength

            if (br > 0f && d < brArc) {
                val angle = d / br
                val nx = cos(angle)
                val ny = sin(angle)
                frame[0] = inset + innerWidth - br + br * nx
                frame[1] = inset + innerHeight - br + br * ny
                frame[2] = nx
                frame[3] = ny
                return
            }
            d -= brArc

            if (d < bottomLength) {
                frame[0] = inset + innerWidth - br - d
                frame[1] = inset + innerHeight
                frame[2] = 0f
                frame[3] = 1f
                return
            }
            d -= bottomLength

            if (bl > 0f && d < blArc) {
                val angle = quarterPi + d / bl
                val nx = cos(angle)
                val ny = sin(angle)
                frame[0] = inset + bl + bl * nx
                frame[1] = inset + innerHeight - bl + bl * ny
                frame[2] = nx
                frame[3] = ny
                return
            }
            d -= blArc

            if (d < leftLength) {
                frame[0] = inset
                frame[1] = inset + innerHeight - bl - d
                frame[2] = -1f
                frame[3] = 0f
                return
            }
            d -= leftLength

            if (tl > 0f) {
                val angle = PI.toFloat() + d / tl
                val nx = cos(angle)
                val ny = sin(angle)
                frame[0] = inset + tl + tl * nx
                frame[1] = inset + tl + tl * ny
                frame[2] = nx
                frame[3] = ny
            } else {
                frame[0] = inset + tl
                frame[1] = inset
                frame[2] = 0f
                frame[3] = -1f
            }
        }

        fun pointAt(distance: Float, out: FloatArray) {
            frameAt(distance)
            val phase = (distance / actualWavelength) * (2f * PI.toFloat())
            val displacement = amplitude * sin(phase)
            out[0] = frame[0] + frame[2] * displacement
            out[1] = frame[1] + frame[3] * displacement
        }

        val first = FloatArray(2)
        val second = FloatArray(2)
        val next = FloatArray(2)

        pointAt(0f, frame)
        first[0] = frame[0]
        first[1] = frame[1]
        pointAt(step, frame)
        second[0] = frame[0]
        second[1] = frame[1]

        // A closed quadratic B-spline: every sample becomes a rounded control
        // point, with endpoints halfway between adjacent samples.
        val startX = (first[0] + second[0]) * 0.5f
        val startY = (first[1] + second[1]) * 0.5f
        path.moveTo(startX, startY)

        var currentX = second[0]
        var currentY = second[1]
        for (index in 1 until sampleCount) {
            val nextIndex = (index + 1) % sampleCount
            if (nextIndex == 0) {
                next[0] = first[0]
                next[1] = first[1]
            } else {
                pointAt(nextIndex * step, frame)
                next[0] = frame[0]
                next[1] = frame[1]
            }

            path.quadraticTo(
                currentX,
                currentY,
                (currentX + next[0]) * 0.5f,
                (currentY + next[1]) * 0.5f,
            )
            currentX = next[0]
            currentY = next[1]
        }

        // Complete the final quadratic segment through the first control point;
        // closing directly here would leave a straight seam across the outline.
        path.quadraticTo(first[0], first[1], startX, startY)
        path.close()
        return Outline.Generic(path)
    }

    private companion object {
        private const val WAVE_AMPLITUDE_DP = 0.8f
        private const val WAVE_LENGTH_DP = 8f
        private const val SAMPLES_PER_WAVE = 4
    }
}
