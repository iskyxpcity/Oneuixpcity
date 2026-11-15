package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Rounded-corner outline with small circular scallops along its four straight
 * edges. Corner geometry is kept separate from edge geometry: the four corners
 * remain ordinary quarter-circle arcs, while each straight edge is built from
 * repeated circular arcs.
 *
 * This is a static shape. It has no animation, per-frame sampling, or global
 * outline cache.
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
    ): CornerBasedShape = WavyShape(
        density, topStart, topEnd, bottomEnd, bottomStart,
    )

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
        val logicalTopStart = topStart.coerceIn(0f, maxRadius)
        val logicalTopEnd = topEnd.coerceIn(0f, maxRadius)
        val logicalBottomEnd = bottomEnd.coerceIn(0f, maxRadius)
        val logicalBottomStart = bottomStart.coerceIn(0f, maxRadius)

        val tlOriginal = if (layoutDirection == LayoutDirection.Ltr) logicalTopStart else logicalTopEnd
        val trOriginal = if (layoutDirection == LayoutDirection.Ltr) logicalTopEnd else logicalTopStart
        val brOriginal = if (layoutDirection == LayoutDirection.Ltr) logicalBottomEnd else logicalBottomStart
        val blOriginal = if (layoutDirection == LayoutDirection.Ltr) logicalBottomStart else logicalBottomEnd

        val amplitude = (WAVE_AMPLITUDE_DP * density)
            .coerceAtMost(min(width, height) * 0.018f)
        val inset = amplitude
        val tl = (tlOriginal - inset).coerceAtLeast(0f)
        val tr = (trOriginal - inset).coerceAtLeast(0f)
        val br = (brOriginal - inset).coerceAtLeast(0f)
        val bl = (blOriginal - inset).coerceAtLeast(0f)

        val left = inset
        val top = inset
        val right = width - inset
        val bottom = height - inset
        val topLength = (right - left - tl - tr).coerceAtLeast(0f)
        val rightLength = (bottom - top - tr - br).coerceAtLeast(0f)
        val bottomLength = (right - left - br - bl).coerceAtLeast(0f)
        val leftLength = (bottom - top - bl - tl).coerceAtLeast(0f)

        val path = Path()
        path.moveTo(left + tl, top)

        appendCircularWaveEdge(
            path, left + tl, top, topLength,
            tangentX = 1f, tangentY = 0f, normalX = 0f, normalY = -1f,
            angleOffset = 0f, amplitude = amplitude,
        )
        appendCorner(path, right - tr, top + tr, tr, -90f, 90f)

        appendCircularWaveEdge(
            path, right, top + tr, rightLength,
            tangentX = 0f, tangentY = 1f, normalX = 1f, normalY = 0f,
            angleOffset = 90f, amplitude = amplitude,
        )
        appendCorner(path, right - br, bottom - br, br, 0f, 90f)

        appendCircularWaveEdge(
            path, right - br, bottom, bottomLength,
            tangentX = -1f, tangentY = 0f, normalX = 0f, normalY = 1f,
            angleOffset = 180f, amplitude = amplitude,
        )
        appendCorner(path, left + bl, bottom - bl, bl, 90f, 90f)

        appendCircularWaveEdge(
            path, left, bottom - bl, leftLength,
            tangentX = 0f, tangentY = -1f, normalX = -1f, normalY = 0f,
            angleOffset = 270f, amplitude = amplitude,
        )
        appendCorner(path, left + tl, top + tl, tl, 180f, 90f)

        path.close()
        return Outline.Generic(path)
    }

    private fun appendCorner(
        path: Path,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        if (radius <= 0.01f) {
            return
        }
        path.arcTo(
            Rect(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
            ),
            startAngle,
            sweepAngle,
            forceMoveTo = false,
        )
    }

    /**
     * Each half-wave is one circular arc. Alternating the arc's bulge direction
     * produces a regular scalloped edge; arc length and radius are solved once
     * per edge, rather than sampling a spline at multiple points per wave.
     */
    private fun appendCircularWaveEdge(
        path: Path,
        startX: Float,
        startY: Float,
        length: Float,
        tangentX: Float,
        tangentY: Float,
        normalX: Float,
        normalY: Float,
        angleOffset: Float,
        amplitude: Float,
    ) {
        val targetWavelength = (WAVE_LENGTH_DP * density).coerceAtLeast(1f)
        val waveCount = (length / targetWavelength).toInt()
        if (waveCount <= 0 || amplitude <= 0.01f) {
            path.lineTo(startX + tangentX * length, startY + tangentY * length)
            return
        }

        val halfWaveCount = waveCount * 2
        val halfWaveLength = length / halfWaveCount
        val halfChord = halfWaveLength / 2f
        val radius = (halfChord * halfChord + amplitude * amplitude) / (2f * amplitude)
        val centerOffset = radius - amplitude
        val alpha = Math.toDegrees(atan2(centerOffset, halfChord).toDouble()).toFloat()
        val localStartOutward = 180f - alpha
        val localSweepOutward = -(180f - 2f * alpha)

        for (index in 0 until halfWaveCount) {
            val direction = if (index % 2 == 0) 1f else -1f
            val centerDistance = (index + 0.5f) * halfWaveLength
            val centerX = startX +
                tangentX * centerDistance -
                normalX * direction * centerOffset
            val centerY = startY +
                tangentY * centerDistance -
                normalY * direction * centerOffset

            val localStart = if (direction > 0f) {
                localStartOutward
            } else {
                180f + alpha
            }
            val localSweep = if (direction > 0f) {
                localSweepOutward
            } else {
                180f - 2f * alpha
            }

            path.arcTo(
                Rect(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius,
                ),
                angleOffset - localStart,
                -localSweep,
                forceMoveTo = false,
            )
        }
    }

    private companion object {
        private const val WAVE_AMPLITUDE_DP = 0.56f
        private const val WAVE_LENGTH_DP = 9f
    }
}
