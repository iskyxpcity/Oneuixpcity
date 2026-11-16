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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Voyage-compatible perimeter wave shape.
 *
 * The wave is evaluated against one continuous rounded-rectangle perimeter:
 * straight edges and all four corner arcs share the same accumulated distance
 * and phase. This is intentionally not a set of independent edge scallops.
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

    private data class OutlineKey(
        val density: Float,
        val width: Float,
        val height: Float,
        val layoutDirection: LayoutDirection,
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
        val key = OutlineKey(
            density, size.width, size.height, layoutDirection,
            topStart, topEnd, bottomEnd, bottomStart,
        )
        synchronized(outlineCache) {
            outlineCache[key]?.let { return it }
        }

        // Generate outside the lock so one expensive wave path never blocks
        // unrelated outline lookups on other Compose nodes.
        val generated = createPerimeterOutline(
            size, topStart, topEnd, bottomEnd, bottomStart, layoutDirection,
        )
        synchronized(outlineCache) {
            outlineCache[key]?.let { return it }
            outlineCache[key] = generated
            return generated
        }
    }

    private fun createPerimeterOutline(
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

        val minDimension = min(width, height)
        val maxRadius = minDimension / 2f
        val tlOriginal = (if (layoutDirection == LayoutDirection.Ltr) topStart else topEnd)
            .coerceIn(0f, maxRadius)
        val trOriginal = (if (layoutDirection == LayoutDirection.Ltr) topEnd else topStart)
            .coerceIn(0f, maxRadius)
        val brOriginal = (if (layoutDirection == LayoutDirection.Ltr) bottomEnd else bottomStart)
            .coerceIn(0f, maxRadius)
        val blOriginal = (if (layoutDirection == LayoutDirection.Ltr) bottomStart else bottomEnd)
            .coerceIn(0f, maxRadius)

        if (tlOriginal <= 0f && trOriginal <= 0f &&
            brOriginal <= 0f && blOriginal <= 0f
        ) {
            return Outline.Rounded(RoundRect(0f, 0f, width, height))
        }

        // Voyage scales both wavelength and amplitude for compact controls.
        val sizeScale = (minDimension / (48f * density)).coerceIn(0.42f, 1f)
        val amplitude = (WAVE_AMPLITUDE_DP * density * sizeScale)
            .coerceIn(0f, minDimension / 4f)
        if (amplitude <= 0.01f) {
            return Outline.Rounded(
                RoundRect(
                    left = 0f, top = 0f, right = width, bottom = height,
                    topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(tlOriginal),
                    topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(trOriginal),
                    bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(brOriginal),
                    bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(blOriginal),
                ),
            )
        }

        // The base rounded rectangle is inset by one amplitude. Its corner
        // radii are reduced by the same amount, exactly as in Voyage's mapper.
        val tl = (tlOriginal - amplitude).coerceAtLeast(0f)
        val tr = (trOriginal - amplitude).coerceAtLeast(0f)
        val br = (brOriginal - amplitude).coerceAtLeast(0f)
        val bl = (blOriginal - amplitude).coerceAtLeast(0f)

        val topWave = if (tlOriginal > 0f || trOriginal > 0f) amplitude else 0f
        val rightWave = if (trOriginal > 0f || brOriginal > 0f) amplitude else 0f
        val bottomWave = if (brOriginal > 0f || blOriginal > 0f) amplitude else 0f
        val leftWave = if (blOriginal > 0f || tlOriginal > 0f) amplitude else 0f

        // Each side is inset only when at least one of its adjoining
        // original corners exists; this mirrors Voyage's side-specific
        // corner-to-edge transition rules.
        val left = leftWave
        val top = topWave
        val right = width - rightWave
        val bottom = height - bottomWave

        val topLength = max(0f, right - tr - (left + tl))
        val rightLength = max(0f, bottom - br - (top + tr))
        val bottomLength = max(0f, right - br - (left + bl))
        val leftLength = max(0f, bottom - bl - (top + tl))

        val perimeter = topLength + rightLength + bottomLength + leftLength +
            ((tl + tr + br + bl) * (PI.toFloat() / 2f))

        if (perimeter <= 0.01f) {
            return Outline.Rounded(RoundRect(0f, 0f, width, height))
        }

        // Fit an integer number of cycles around the closed perimeter. This
        // prevents a phase jump where the final point meets the first point.
        val targetWavelength = (WAVE_LENGTH_DP * density * sizeScale).coerceAtLeast(1f)
        val cycleCount = max(1, ceil(perimeter / targetWavelength).toInt())
        val wavelength = perimeter / cycleCount

        // Keep the original adaptive sampling limits: at least eight samples
        // per wave, at most about 360 samples around the whole outline, and
        // never use a sub-pixel sampling interval.
        val sampleStep = max(
            1f,
            max(wavelength / SAMPLES_PER_WAVE, perimeter / MAX_PERIMETER_SAMPLES),
        )

        val path = Path()
        val state = WavePathState(path, wavelength, amplitude)

        appendLine(
            state, left + tl, top, right - tr, top,
            normalX = 0f, normalY = -1f, length = topLength,
            waveAmplitude = topWave, sampleStep = sampleStep,
        )
        appendCorner(
            state, right - tr, top + tr, tr,
            startAngle = -PI.toFloat() / 2f,
            waveAmplitude = amplitude, sampleStep = sampleStep,
        )
        appendLine(
            state, right, top + tr, right, bottom - br,
            normalX = 1f, normalY = 0f, length = rightLength,
            waveAmplitude = rightWave, sampleStep = sampleStep,
        )
        appendCorner(
            state, right - br, bottom - br, br,
            startAngle = 0f,
            waveAmplitude = amplitude, sampleStep = sampleStep,
        )
        appendLine(
            state, right - br, bottom, left + bl, bottom,
            normalX = 0f, normalY = 1f, length = bottomLength,
            waveAmplitude = bottomWave, sampleStep = sampleStep,
        )
        appendCorner(
            state, left + bl, bottom - bl, bl,
            startAngle = PI.toFloat() / 2f,
            waveAmplitude = amplitude, sampleStep = sampleStep,
        )
        appendLine(
            state, left, bottom - bl, left, top + tl,
            normalX = -1f, normalY = 0f, length = leftLength,
            waveAmplitude = leftWave, sampleStep = sampleStep,
        )
        appendCorner(
            state, left + tl, top + tl, tl,
            startAngle = PI.toFloat(),
            waveAmplitude = amplitude, sampleStep = sampleStep,
        )

        state.close()
        return Outline.Generic(path)
    }

    private class WavePathState(
        val path: Path,
        val wavelength: Float,
        val amplitude: Float,
    ) {
        var distance = 0f
        var hasPrevious = false
        var firstX = 0f
        var firstY = 0f
        var previousX = 0f
        var previousY = 0f

        fun append(baseX: Float, baseY: Float, normalX: Float, normalY: Float, waveAmplitude: Float) {
            val displacement = waveAmplitude *
                sin((distance * TWO_PI) / wavelength)
            val x = baseX + normalX * displacement
            val y = baseY + normalY * displacement

            if (!hasPrevious) {
                path.moveTo(x, y)
                firstX = x
                firstY = y
                hasPrevious = true
            } else {
                path.quadraticTo(
                    previousX,
                    previousY,
                    (previousX + x) / 2f,
                    (previousY + y) / 2f,
                )
            }
            previousX = x
            previousY = y
        }

        fun close() {
            if (!hasPrevious) return
            path.quadraticTo(previousX, previousY, firstX, firstY)
            path.close()
        }
    }

    private fun appendLine(
        state: WavePathState,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        normalX: Float,
        normalY: Float,
        length: Float,
        waveAmplitude: Float,
        sampleStep: Float,
    ) {
        val steps = max(1, ceil(length / sampleStep).toInt())
        for (index in 0..steps) {
            val fraction = index.toFloat() / steps
            state.append(
                startX + (endX - startX) * fraction,
                startY + (endY - startY) * fraction,
                normalX,
                normalY,
                waveAmplitude,
            )
            if (index < steps) state.distance += length / steps
        }
    }

    private fun appendCorner(
        state: WavePathState,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        waveAmplitude: Float,
        sampleStep: Float,
    ) {
        val arcLength = radius * PI.toFloat() / 2f
        val steps = max(1, ceil(arcLength / sampleStep).toInt())
        for (index in 0..steps) {
            val fraction = index.toFloat() / steps
            val angle = startAngle + (PI.toFloat() / 2f) * fraction
            val normalX = cos(angle)
            val normalY = sin(angle)
            state.append(
                centerX + radius * normalX,
                centerY + radius * normalY,
                normalX,
                normalY,
                waveAmplitude,
            )
            if (index < steps) state.distance += arcLength / steps
        }
    }

    private companion object {
        private const val WAVE_LENGTH_DP = 12f
        private const val WAVE_AMPLITUDE_DP = 0.9f
        private const val MAX_PERIMETER_SAMPLES = 360f
        private const val SAMPLES_PER_WAVE = 8f
        private const val TWO_PI = 6.2831855f
        // Shared bounded LRU: WavyShape is recreated during animated corner
        // interpolation, so an instance-local cache is discarded every frame.
        // Sharing by resolved geometry allows stable/equal outlines across
        // instances and controls to reuse the exact same Outline object.
        private val outlineCache = object : java.util.LinkedHashMap<OutlineKey, Outline>(128, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<OutlineKey, Outline>?,
            ): Boolean = size > 768
        }

    }
}
