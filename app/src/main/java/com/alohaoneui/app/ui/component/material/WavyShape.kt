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
import kotlin.math.sin
import java.util.concurrent.ConcurrentHashMap

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
    private val cacheOutlineGlobally: Boolean,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    // Press-driven shapes change their dimensions/radii on every animation frame.
    // Keep only the most recent outline on that short-lived shape instance so
    // shadow, border and clip can reuse it within the frame without churning the
    // shared static cache or invoking its synchronized eviction path.
    private var lastTransientKey: OutlineKey? = null
    private var lastTransientOutline: Outline? = null

    internal constructor(
        density: Float,
        topStart: Dp,
        topEnd: Dp,
        bottomEnd: Dp,
        bottomStart: Dp,
        cacheOutlineGlobally: Boolean = true,
    ) : this(
        density,
        CornerSize(topStart),
        CornerSize(topEnd),
        CornerSize(bottomEnd),
        CornerSize(bottomStart),
        cacheOutlineGlobally,
    )

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = WavyShape(
        density, topStart, topEnd, bottomEnd, bottomStart, cacheOutlineGlobally,
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val key = OutlineKey(
            size.width.toBits(),
            size.height.toBits(),
            topStart.toBits(),
            topEnd.toBits(),
            bottomEnd.toBits(),
            bottomStart.toBits(),
            layoutDirection,
            density.toBits(),
        )

        if (!cacheOutlineGlobally) {
            if (lastTransientKey == key) {
                lastTransientOutline?.let { return it }
            }
            val outline = buildOutline(size, topStart, topEnd, bottomEnd, bottomStart, layoutDirection)
            lastTransientKey = key
            lastTransientOutline = outline
            return outline
        }

        // Stable outlines use the shared cache. Animated press outlines bypass
        // it because their per-frame keys would otherwise churn the bounded map.
        outlineCache[key]?.let { return it }

        val outline = buildOutline(size, topStart, topEnd, bottomEnd, bottomStart, layoutDirection)
        val cached = outlineCache.putIfAbsent(key, outline) ?: outline
        if (outlineCache.size > MAX_CACHE_ENTRIES) {
            synchronized(cacheTrimLock) {
                while (outlineCache.size > MAX_CACHE_ENTRIES) {
                    val candidate = outlineCache.keys.firstOrNull() ?: break
                    outlineCache.remove(candidate)
                }
            }
        }
        return cached
    }

    private fun buildOutline(
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
        // Cubic crest controls are offset by 4/3 amplitude, giving the
        // contour its requested visible amplitude while keeping the original
        // component bounds intact.
        val inset = amplitude

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
        val waveCount = (perimeter / targetWavelength).toInt().coerceIn(12, 128)
        // Two cubic segments per wavelength keep the same small rounded
        // scallop profile with half the path operations of the previous spline.
        val sampleCount = waveCount * SAMPLES_PER_WAVE
        val step = perimeter / sampleCount
        val actualWavelength = perimeter / waveCount
        val path = Path()
        val frame = FloatArray(4)
        val crest = FloatArray(2)
        val trough = FloatArray(2)
        val controlOffset = amplitude * (4f / 3f)

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

        fun controlAt(distance: Float, displacement: Float, out: FloatArray) {
            frameAt(distance)
            out[0] = frame[0] + frame[2] * displacement
            out[1] = frame[1] + frame[3] * displacement
        }

        frameAt(0f)
        path.moveTo(frame[0], frame[1])

        for (waveIndex in 0 until waveCount) {
            val waveStart = waveIndex * actualWavelength

            // Place the two controls at one-third and two-thirds of each
            // half-wave. This preserves a rounded apex but removes the broad,
            // flattened crest caused by coincident control points.
            controlAt(waveStart + (2f / 3f) * step, controlOffset, crest)
            frameAt(waveStart + 2f * step)
            val firstControlX = crest[0]
            val firstControlY = crest[1]
            val crestEndX = frame[0]
            val crestEndY = frame[1]
            controlAt(waveStart + (4f / 3f) * step, controlOffset, crest)
            path.cubicTo(
                firstControlX, firstControlY,
                crest[0], crest[1],
                crestEndX, crestEndY,
            )

            controlAt(waveStart + (8f / 3f) * step, -controlOffset, trough)
            frameAt(waveStart + actualWavelength)
            val secondControlX = trough[0]
            val secondControlY = trough[1]
            val troughEndX = frame[0]
            val troughEndY = frame[1]
            controlAt(waveStart + (10f / 3f) * step, -controlOffset, trough)
            path.cubicTo(
                secondControlX, secondControlY,
                trough[0], trough[1],
                troughEndX, troughEndY,
            )
        }

        path.close()
        return Outline.Generic(path)
    }

    private data class OutlineKey(
        val width: Int, val height: Int,
        val topStart: Int, val topEnd: Int, val bottomEnd: Int, val bottomStart: Int,
        val direction: LayoutDirection, val density: Int,
    )

    private companion object {
        // Concurrent lookups keep cache hits lock-free across Compose nodes.
        // Trim only on overflow; avoid a global monitor on every outline request.
        private const val MAX_CACHE_ENTRIES = 96
        private val outlineCache = ConcurrentHashMap<OutlineKey, Outline>(MAX_CACHE_ENTRIES)
        private val cacheTrimLock = Any()

        private const val WAVE_AMPLITUDE_DP = 0.56f
        private const val WAVE_LENGTH_DP = 9f
        private const val SAMPLES_PER_WAVE = 4
    }
}
