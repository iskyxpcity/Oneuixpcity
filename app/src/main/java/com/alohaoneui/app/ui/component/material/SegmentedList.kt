package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.alohaoneui.app.ui.theme.LocalMaterialCornerScale
import com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons
import kotlinx.coroutines.launch

val LocalListItemShapes = compositionLocalOf<ListItemShapes?> { null }


/**
 * Shape geometry context for a segmented row. This lets custom Aloha paths
 * animate their real corner parameters instead of relying on Material 3's
 * CornerBasedShape-only pressed-shape resolver.
 */
private data class SegmentedShapeConfig(val index: Int, val count: Int, val vertical: Boolean = true)
private val LocalSegmentedShapeConfig = compositionLocalOf<SegmentedShapeConfig?> { null }

/**
 * G² continuity profile used by the shared path renderer.
 *
 * The four profile values are, in source order: extended fraction, arc fraction,
 * Bezier curvature scale, and arc curvature scale. The profile is deliberately
 * kept separate from the Squircle smoothing parameter: Aloha uses this profile
 * to construct a cubic/arc transition, not the b2t0 smoothing shortcut.
 */
private data class AlohaG2Profile(
    val extendedFraction: Double,
    val arcFraction: Double,
    val bezierCurvatureScale: Double,
    val arcCurvatureScale: Double,
) {
    data class Point(val x: Double, val y: Double) {
        operator fun plus(other: Point) = Point(x + other.x, y + other.y)
        operator fun minus(other: Point) = Point(x - other.x, y - other.y)
        operator fun times(scale: Double) = Point(x * scale, y * scale)
    }

    data class Cubic(val p0: Point, val p1: Point, val p2: Point, val p3: Point)

    /**
     * The returned cubic is
     * the Bezier segment that joins the G² profile to its circular arc.
     */
    fun cubic(): Cubic {
        val angle = arcFraction * Math.PI / 2.0
        val sin = kotlin.math.sin(angle)
        val cos = kotlin.math.cos(angle)
        val p0 = Point(-extendedFraction, 0.0)
        if (bezierCurvatureScale == 1.0 && arcCurvatureScale == 1.0) {
            val denominator = cos + 1.0
            val tangent = sin / denominator
            return Cubic(
                p0,
                Point((1.0 - 1.5 / denominator) * tangent, 0.0),
                Point(tangent, 0.0),
                Point(sin, 1.0 - cos),
            )
        }

        val inverseArcCurvature = 1.0 / arcCurvatureScale
        val diagonal = 1.0 / kotlin.math.sqrt(2.0)
        val p1 = Point(0.0, 1.0) +
            Point(diagonal, -diagonal) * (1.0 - inverseArcCurvature) +
            Point(sin, -cos) * inverseArcCurvature
        val p2 = Point(1.0, 0.0)
        val p3 = Point(cos, sin)

        val dx = p1.x - p0.x
        val dy = p1.y - p0.y
        val determinant = (p2.x * p3.y) - (p2.y * p3.x)
        val tangentNumerator = (dx * p2.y) + (-dy * p2.x)
        val curvatureTerm = (((1.5 * bezierCurvatureScale) * tangentNumerator * tangentNumerator) /
            determinant / determinant / determinant)
        val firstControlScale = (-((dy * p3.x) - (dx * p3.y)) / determinant) - curvatureTerm
        val secondControlScale = (-tangentNumerator) / determinant
        val c1x = p0.x + maxOf(0.0, p2.x * firstControlScale)
        val c1y = p0.y + maxOf(0.0, p2.y * firstControlScale)
        val c2x = p1.x - maxOf(0.0, p3.x * secondControlScale)
        val c2y = p1.y - maxOf(0.0, p3.y * secondControlScale)
        // fqw0.m95419o() returns wcf(p0, solvedControl1, solvedControl2, p1):
        // in this general branch p1 is the cubic endpoint; p2/p3 are tangent
        // direction points used to solve the controls, not the endpoint.
        return Cubic(p0, Point(c1x, c1y), Point(c2x, c2y), p1)
    }

    /**
     * The corner solver computes eight independent width/height progress values:
     * ((halfExtent / cornerRadius) - 1) / smooth.extendedFraction, clamped
     * to [0, 1]. A zero radius is handled separately by the path builder.
     */
    fun aspectProgress(halfExtent: Float, cornerRadius: Float): Double {
        if (!cornerRadius.isFinite() || !halfExtent.isFinite()) return 0.0
        // In ban.m5206I(), a zero radius with a positive half-extent produces
        // +infinity before clamping, so its aspect progress is 1 (not 0).
        // Keep a zero-sized degenerate shape finite.
        if (cornerRadius <= 0f) return if (halfExtent > 0f) 1.0 else 0.0
        return (((halfExtent.toDouble() / cornerRadius.toDouble()) - 1.0) /
            extendedFraction).coerceIn(0.0, 1.0)
    }

    /**
     * Parameter interpolation is performed independently for each corner. The renderer does not
     * interpolate a single profile per corner: it computes an aspect progress
     * for each adjacent half-extent, uses the pair minimum for the shared
     * corner profile, then scales the extended/arc/Bezier values independently.
     */
    fun interpolateFromCapsule(progress: Double, sharedCornerProgress: Double = progress): AlohaG2Profile {
        val t = progress.coerceIn(0.0, 1.0)
        val shared = sharedCornerProgress.coerceIn(0.0, 1.0)
        fun mix(from: Double, to: Double, amount: Double) = from + (to - from) * amount
        return AlohaG2Profile(
            extendedFraction = mix(capsule.extendedFraction, smooth.extendedFraction, shared) * t,
            // ban.m5206I() derives d65/d66/d67/d68 from the shared
            // minimum of the two adjacent axis-progress values, not from
            // the per-axis progress. This matters for asymmetric corners.
            arcFraction = mix(capsule.arcFraction, smooth.arcFraction, shared),
            bezierCurvatureScale = mix(capsule.bezierCurvatureScale, smooth.bezierCurvatureScale, t),
            arcCurvatureScale = mix(capsule.arcCurvatureScale, smooth.arcCurvatureScale, shared),
        )
    }

    /** Eight progress values in the same TL/TR/BR/BL paired order as ban.m5206I(). */
    fun cornerProgresses(
        width: Float, height: Float,
        topStart: Float, topEnd: Float, bottomEnd: Float, bottomStart: Float,
    ): DoubleArray {
        val halfWidth = width.toDouble() / 2.0
        val halfHeight = height.toDouble() / 2.0
        return doubleArrayOf(
            aspectProgress(halfHeight.toFloat(), topStart),
            aspectProgress(halfWidth.toFloat(), topStart),
            aspectProgress(halfWidth.toFloat(), topEnd),
            aspectProgress(halfHeight.toFloat(), topEnd),
            aspectProgress(halfHeight.toFloat(), bottomEnd),
            aspectProgress(halfWidth.toFloat(), bottomEnd),
            aspectProgress(halfWidth.toFloat(), bottomStart),
            aspectProgress(halfHeight.toFloat(), bottomStart),
        )
    }

    companion object {
        // Constants for the Smooth G² and capsule profiles.
        val smooth = AlohaG2Profile(0.5286651, 0.5555555555555556, 1.0732051, 1.0732051)
        val capsule = AlohaG2Profile(0.396498825, 0.0, 1.0, 1.0)
    }
}

private class AlohaPathShape private constructor(
    private val style: Int,
    private val densityScale: Float,
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {
    constructor(
        style: Int,
        densityScale: Float,
        topStart: Dp,
        topEnd: Dp,
        bottomEnd: Dp,
        bottomStart: Dp,
    ) : this(
        style,
        densityScale,
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
    ): CornerBasedShape = AlohaPathShape(style, densityScale, topStart, topEnd, bottomEnd, bottomStart)

    private data class OutlineKey(
        val style: Int,
        val densityScale: Float,
        val width: Float,
        val height: Float,
        val direction: LayoutDirection,
        val topStart: Float,
        val topEnd: Float,
        val bottomEnd: Float,
        val bottomStart: Float,
    )

    // Share generated outlines across all instances. During radius adjustment many
    // rows use the same dimensions and radii; a per-instance cache rebuilt on every
    // animated value causes redundant Path allocation and frame drops.
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        // CornerBasedShape resolves each CornerSize to pixels before calling this
        // method, so the exact pixel radii are also the complete cache identity.
        val key = OutlineKey(
            style, densityScale, size.width, size.height, layoutDirection,
            topStart, topEnd, bottomEnd, bottomStart,
        )

        synchronized(outlineCache) {
            outlineCache[key]?.let { return it }
        }
        // Build outside the shared lock: complex Wavy outlines must not block
        // other shape lookups while the radius slider is being dragged.
        val generated = createAlohaOutline(
            size, layoutDirection, topStart, topEnd, bottomEnd, bottomStart, densityScale,
        )
        synchronized(outlineCache) {
            outlineCache[key]?.let { return it }
            outlineCache[key] = generated
            return generated
        }
    }

    private companion object {
        // Bounded process-wide LRU: reuse equal outlines across rows/components,
        // while limiting retained Path objects when users explore many radii.
        private val outlineCache = object : java.util.LinkedHashMap<OutlineKey, Outline>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<OutlineKey, Outline>?): Boolean = size > 768
        }
    }

    private data class GeometryPoint(val x: Float, val y: Float)

    private data class WaveFrame(
        val x: Float,
        val y: Float,
        val tangentX: Float,
        val tangentY: Float,
        val curvature: Float,
    )

    private data class WaveSample(
        val x: Float,
        val y: Float,
        val derivativeX: Float,
        val derivativeY: Float,
    )

    /**
     * Appends a canonical PH quintic G² corner. The normalized control polygon is
     * (0,0), (c,0), (c,0), (1,1-c), (1,1-c), (1,1), where
     * c = 3(6-sqrt(2))/17. Compose Path exposes cubic segments, so the exact
     * quintic is converted to short cubic Hermite spans using its analytic
     * position and tangent. This keeps the corner smooth without per-frame
     * numerical solving or trigonometric work.
     *
     * orientation: 0=top-right, 1=bottom-right, 2=bottom-left, 3=top-left.
     */
    private fun appendPhQuinticCorner(
        path: Path,
        originX: Float,
        originY: Float,
        radius: Float,
        orientation: Int,
    ) {
        val c = 3f * (6f - kotlin.math.sqrt(2f)) / 17f
        val spans = 12
        val dt = 1f / spans

        fun mapX(x: Float, y: Float): Float = when (orientation) {
            1 -> -y
            2 -> -x
            3 -> y
            else -> x
        }
        fun mapY(x: Float, y: Float): Float = when (orientation) {
            1 -> x
            2 -> -y
            3 -> -x
            else -> y
        }
        fun point(t: Float): GeometryPoint {
            val u = 1f - t
            val u2 = u * u
            val t2 = t * t
            val b1 = 5f * u2 * u2 * t
            val b2 = 10f * u2 * u * t2
            val b3 = 10f * u2 * t * t2
            val b4 = 5f * u * t2 * t2
            val b5 = t2 * t2 * t
            return GeometryPoint(
                c * (b1 + b2) + b3 + b4 + b5,
                (1f - c) * (b3 + b4) + b5,
            )
        }
        fun derivative(t: Float): GeometryPoint {
            val u = 1f - t
            val u2 = u * u
            val t2 = t * t
            val first = 5f * c * u2 * u2
            val middle = 30f * (1f - c) * u2 * t2
            val last = 5f * c * t2 * t2
            return GeometryPoint(first + middle, middle + last)
        }
        fun absoluteX(p: GeometryPoint): Float = originX + radius * mapX(p.x, p.y)
        fun absoluteY(p: GeometryPoint): Float = originY + radius * mapY(p.x, p.y)
        fun tangentX(p: GeometryPoint): Float = radius * mapX(p.x, p.y)
        fun tangentY(p: GeometryPoint): Float = radius * mapY(p.x, p.y)

        for (index in 0 until spans) {
            val t0 = index * dt
            val t1 = (index + 1) * dt
            val p0 = point(t0)
            val p1 = point(t1)
            val d0 = derivative(t0)
            val d1 = derivative(t1)
            val control1 = GeometryPoint(
                p0.x + d0.x * dt / 3f,
                p0.y + d0.y * dt / 3f,
            )
            val control2 = GeometryPoint(
                p1.x - d1.x * dt / 3f,
                p1.y - d1.y * dt / 3f,
            )
            path.cubicTo(
                originX + radius * mapX(control1.x, control1.y),
                originY + radius * mapY(control1.x, control1.y),
                originX + radius * mapX(control2.x, control2.y),
                originY + radius * mapY(control2.x, control2.y),
                absoluteX(p1),
                absoluteY(p1),
            )
        }
    }

    private fun roundedRectWaveFrameAt(
        distance: Float,
        width: Float,
        height: Float,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
    ): WaveFrame {
        val topLength = (width - topLeft - topRight).coerceAtLeast(0f)
        val rightLength = (height - topRight - bottomRight).coerceAtLeast(0f)
        val bottomLength = (width - bottomRight - bottomLeft).coerceAtLeast(0f)
        val leftLength = (height - bottomLeft - topLeft).coerceAtLeast(0f)
        val topRightArc = topRight * (kotlin.math.PI.toFloat() / 2f)
        val bottomRightArc = bottomRight * (kotlin.math.PI.toFloat() / 2f)
        val bottomLeftArc = bottomLeft * (kotlin.math.PI.toFloat() / 2f)
        val topLeftArc = topLeft * (kotlin.math.PI.toFloat() / 2f)
        var d = distance.coerceAtLeast(0f)

        if (d < topLength) return WaveFrame(topLeft + d, 0f, 1f, 0f, 0f)
        d -= topLength
        if (d < topRightArc && topRight > 0f) {
            val angle = -kotlin.math.PI.toFloat() / 2f + d / topRight
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            return WaveFrame(
                width - topRight + topRight * cos,
                topRight + topRight * sin,
                -sin,
                cos,
                1f / topRight,
            )
        }
        d -= topRightArc
        if (d < rightLength) return WaveFrame(width, topRight + d, 0f, 1f, 0f)
        d -= rightLength
        if (d < bottomRightArc && bottomRight > 0f) {
            val angle = d / bottomRight
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            return WaveFrame(
                width - bottomRight + bottomRight * cos,
                height - bottomRight + bottomRight * sin,
                -sin,
                cos,
                1f / bottomRight,
            )
        }
        d -= bottomRightArc
        if (d < bottomLength) return WaveFrame(width - bottomRight - d, height, -1f, 0f, 0f)
        d -= bottomLength
        if (d < bottomLeftArc && bottomLeft > 0f) {
            val angle = kotlin.math.PI.toFloat() / 2f + d / bottomLeft
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            return WaveFrame(
                bottomLeft + bottomLeft * cos,
                height - bottomLeft + bottomLeft * sin,
                -sin,
                cos,
                1f / bottomLeft,
            )
        }
        d -= bottomLeftArc
        if (d < leftLength) return WaveFrame(0f, height - bottomLeft - d, 0f, -1f, 0f)
        d -= leftLength
        if (d < topLeftArc && topLeft > 0f) {
            val angle = kotlin.math.PI.toFloat() + d / topLeft
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            return WaveFrame(
                topLeft + topLeft * cos,
                topLeft + topLeft * sin,
                -sin,
                cos,
                1f / topLeft,
            )
        }
        return WaveFrame(topLeft, 0f, 1f, 0f, 0f)
    }

    private fun appendWavyPerimeter(
        path: Path,
        width: Float,
        height: Float,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
        densityScale: Float,
    ) {
        val topLength = (width - topLeft - topRight).coerceAtLeast(0f)
        val rightLength = (height - topRight - bottomRight).coerceAtLeast(0f)
        val bottomLength = (width - bottomRight - bottomLeft).coerceAtLeast(0f)
        val leftLength = (height - bottomLeft - topLeft).coerceAtLeast(0f)
        val cornerSum = topLeft + topRight + bottomRight + bottomLeft
        val perimeter = topLength + rightLength + bottomLength + leftLength +
            cornerSum * (kotlin.math.PI.toFloat() / 2f)
        val minDimension = minOf(width, height)
        if (perimeter <= 0.5f || minDimension <= 0f) {
            path.addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, width, height))
            return
        }

        val positiveCornerRadii = listOf(topLeft, topRight, bottomRight, bottomLeft).filter { it > 0f }
        val smallestCorner = positiveCornerRadii.minOrNull() ?: (minDimension * 0.05f)
        val amplitude = minOf(
            1.6f * densityScale,
            minDimension * 0.045f,
            smallestCorner * 0.22f,
        ).coerceAtLeast(0.25f * densityScale)
        val waveCount = (perimeter / (18f * densityScale).coerceAtLeast(1f))
            .toInt().coerceIn(3, 32)
        val segments = waveCount * 2
        val step = perimeter / segments
        val angularFrequency = (2f * kotlin.math.PI.toFloat() * waveCount) / perimeter
        val harmonicScale = 1f / 1.22f

        fun sample(distance: Float): WaveSample {
            val frame = roundedRectWaveFrameAt(
                distance, width, height, topLeft, topRight, bottomRight, bottomLeft,
            )
            val phase = angularFrequency * distance
            val sin1 = kotlin.math.sin(phase)
            val cos1 = kotlin.math.cos(phase)
            val sin3 = kotlin.math.sin(3f * phase)
            val cos3 = kotlin.math.cos(3f * phase)
            val displacement = amplitude * (sin1 + 0.22f * sin3) * harmonicScale
            val displacementDerivative = amplitude * angularFrequency *
                (cos1 + 0.66f * cos3) * harmonicScale
            val normalX = frame.tangentY
            val normalY = -frame.tangentX
            val tangentScale = 1f + displacement * frame.curvature
            return WaveSample(
                frame.x + normalX * displacement,
                frame.y + normalY * displacement,
                frame.tangentX * tangentScale + normalX * displacementDerivative,
                frame.tangentY * tangentScale + normalY * displacementDerivative,
            )
        }

        val first = sample(0f)
        path.moveTo(first.x, first.y)
        for (index in 0 until segments) {
            val startDistance = index * step
            val endDistance = (index + 1) * step
            val start = sample(startDistance)
            val end = sample(endDistance)
            path.cubicTo(
                start.x + start.derivativeX * step / 3f,
                start.y + start.derivativeY * step / 3f,
                end.x - end.derivativeX * step / 3f,
                end.y - end.derivativeY * step / 3f,
                end.x,
                end.y,
            )
        }
        path.close()
    }

    private fun createAlohaOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        densityScale: Float,
    ): Outline {
        val ts = topStart.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val te = topEnd.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val be = bottomEnd.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val bs = bottomStart.coerceIn(0f, minOf(size.width, size.height) / 2f)
        // Concave-arc (style 5) and notch (style 6) defaults were visually
        // too deep. Halve only these styles' effective geometry; the shared corner-scale
        // setting remains multiplicative and other shape styles keep their original values.
        val depthScale = if (style == 5 || style == 6) 0.5f else 1f
        val tl = (if (layoutDirection == LayoutDirection.Ltr) ts else te) * depthScale
        val tr = (if (layoutDirection == LayoutDirection.Ltr) te else ts) * depthScale
        val br = (if (layoutDirection == LayoutDirection.Ltr) be else bs) * depthScale
        val bl = (if (layoutDirection == LayoutDirection.Ltr) bs else be) * depthScale
        val w = size.width
        val h = size.height

        // The shape generator delegates equal-radius shapes that touch a full
        // half-extent to m5208l(). Its square branch is a true circular outline,
        // not the generic four-cubic approximation used for ordinary G² corners.
        // Preserve that degenerate case before constructing the general path.
        if (style == 3 && w > 0f && w == h &&
            tl == tr && tr == br && br == bl && tl * 2f == w
        ) {
            return Outline.Rounded(
                androidx.compose.ui.geometry.RoundRect(
                    0f, 0f, w, h,
                    androidx.compose.ui.geometry.CornerRadius(w / 2f, h / 2f),
                ),
            )
        }

        val path = Path()
        when (style) {
            2 -> {
                // Squircle uses the smoothing-profile path,
                // not the G² Smooth corner family.
                val smooth = 0.234f
                path.moveTo(tl, 0f)
                path.lineTo(w - tr, 0f)
                path.cubicTo(w - tr * smooth, 0f, w, tr * smooth, w, tr)
                path.lineTo(w, h - br)
                path.cubicTo(w, h - br * smooth, w - br * smooth, h, w - br, h)
                path.lineTo(bl, h)
                path.cubicTo(bl * smooth, h, 0f, h - bl * smooth, 0f, h - bl)
                path.lineTo(0f, tl)
                path.cubicTo(0f, tl * smooth, tl * smooth, 0f, tl, 0f)
            }
            3 -> {
                // Canonical G² PH quintic corner (Farouki, 2014).
                // Its curvature is zero where it meets each straight edge.
                // Four rotated/reflected copies preserve asymmetric corner radii.
                path.moveTo(tl, 0f)
                path.lineTo(w - tr, 0f)
                if (tr > 0f) appendPhQuinticCorner(path, w - tr, 0f, tr, 0)
                else path.lineTo(w, 0f)
                path.lineTo(w, h - br)
                if (br > 0f) appendPhQuinticCorner(path, w, h - br, br, 1)
                else path.lineTo(w, h)
                path.lineTo(bl, h)
                if (bl > 0f) appendPhQuinticCorner(path, bl, h, bl, 2)
                else path.lineTo(0f, h)
                path.lineTo(0f, tl)
                if (tl > 0f) appendPhQuinticCorner(path, 0f, tl, tl, 3)
                else path.lineTo(0f, 0f)
            }
            4 -> {
                // Wavy is generated along the complete rounded-rectangle perimeter.
                // The periodic normal displacement continues through all four arcs,
                // rather than stopping at each straight edge.
                if (w <= 0f || h <= 0f) {
                    path.addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h))
                } else {
                    appendWavyPerimeter(path, w, h, tl, tr, br, bl, densityScale)
                }
            }
            5 -> {
                path.moveTo(tl, 0f)
                path.lineTo(w - tr, 0f)
                path.arcTo(Rect(w - tr, -tr, w + tr, tr), 180f, -90f, false)
                path.lineTo(w, h - br)
                path.arcTo(Rect(w - br, h - br, w + br, h + br), -90f, -90f, false)
                path.lineTo(bl, h)
                path.arcTo(Rect(-bl, h - bl, bl, h + bl), 0f, -90f, false)
                path.lineTo(0f, tl)
                path.arcTo(Rect(-tl, -tl, tl, tl), 90f, -90f, false)
            }
            6 -> {
                path.moveTo(tl, 0f)
                path.lineTo(w - tr, 0f)
                path.lineTo(w - tr, tr)
                path.lineTo(w, tr)
                path.lineTo(w, h - br)
                path.lineTo(w - br, h - br)
                path.lineTo(w - br, h)
                path.lineTo(bl, h)
                path.lineTo(bl, h - bl)
                path.lineTo(0f, h - bl)
                path.lineTo(0f, tl)
                path.lineTo(tl, tl)
                path.close()
            }
            else -> {
                path.addRoundRect(androidx.compose.ui.geometry.RoundRect(
                    left = 0f, top = 0f, right = w, bottom = h,
                    topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(tl, tl),
                    topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(tr, tr),
                    bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(br, br),
                    bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(bl, bl),
                ))
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun alohaCornerShape(radius: androidx.compose.ui.unit.Dp): CornerBasedShape {
    val scaledRadius = radius * LocalMaterialCornerScale.current.coerceIn(0f, 2f)
    val style = LocalMaterialShapeStyle.current
    val densityScale = LocalDensity.current.density
    return when (style) {
        1 -> remember(style, scaledRadius) { CutCornerShape(scaledRadius) }
        2, 3, 4, 5, 6 -> remember(style, scaledRadius, densityScale) {
            AlohaPathShape(style, densityScale, scaledRadius, scaledRadius, scaledRadius, scaledRadius)
        }
        else -> remember(style, scaledRadius) { RoundedCornerShape(scaledRadius) }
    }
}

@Composable
fun alohaOneUISegmentedColors(): ListItemColors = ListItemDefaults.segmentedColors().copy(
    // Keep selectable list rows below dialog/sheet surfaces in the Material 3
    // tonal hierarchy instead of painting the whole list at dialog elevation.
    containerColor = colorScheme.surfaceContainerLow,
    disabledContainerColor = colorScheme.surfaceContainerLow,
    supportingContentColor = colorScheme.outline,
    // Selection follows Aloha OneUI's live theme palette (custom seed / Monet,
    // palette style, light-dark mode), using its secondary accent roles.
    selectedContainerColor = colorScheme.secondaryContainer,
    selectedContentColor = colorScheme.onSecondaryContainer,
    selectedLeadingContentColor = colorScheme.onSecondaryContainer,
    selectedTrailingContentColor = colorScheme.onSecondaryContainer,
    selectedOverlineContentColor = colorScheme.onSecondaryContainer,
    selectedSupportingContentColor = colorScheme.onSecondaryContainer,
)

/**
 * Segmented-row corner rule:
 * 25dp outer / 4dp inner corners, animated with dampingRatio=0.4 and stiffness=200.
 * vertical=true joins rows top-to-bottom; false joins selection buttons left-to-right.
 */
@Composable
fun alohaSegmentedShape(
    index: Int,
    count: Int,
    vertical: Boolean = true,
    pressed: Boolean = false,
    pressProgress: Float? = null,
): CornerBasedShape {
    val single = count == 1 || index == -1
    val first = single || index == 0
    val last = single || index == count - 1
    // Keep the exact detail-row corner geometry and active theme scale. The optional
    // progress path interpolates each real corner continuously with the button's
    // width expansion instead of switching the whole shape at a pressed threshold.
    val cornerScale = LocalMaterialCornerScale.current.coerceIn(0f, 2f)
    val densityScale = LocalDensity.current.density
    val pressedRadius = when {
        !pressed -> null
        count > 1 && (first || last) -> 4.dp
        else -> 8.dp
    }
    val progress = pressProgress?.coerceIn(0f, 1f)
    fun corner(rest: Dp, pressedValue: Dp): Dp {
        val radius = if (progress != null) {
            rest + (pressedValue - rest) * progress
        } else {
            pressedRadius ?: rest
        }
        return radius * cornerScale
    }

    val outerPressedRadius = if (count > 1 && (first || last)) 4.dp else 8.dp
    val cornerAnimation: FiniteAnimationSpec<Dp> = if (progress != null) snap() else spring(dampingRatio = 0.45f, stiffness = 300f)
    val topStart by animateDpAsState(
        targetValue = corner(if (first) 25.dp else 4.dp, outerPressedRadius),
        animationSpec = cornerAnimation,
        label = "alohaSegmentTopStart",
    )
    val topEnd by animateDpAsState(
        targetValue = corner(if (if (vertical) first else last) 25.dp else 4.dp, outerPressedRadius),
        animationSpec = cornerAnimation,
        label = "alohaSegmentTopEnd",
    )
    val bottomEnd by animateDpAsState(
        targetValue = corner(if (last) 25.dp else 4.dp, outerPressedRadius),
        animationSpec = cornerAnimation,
        label = "alohaSegmentBottomEnd",
    )
    val bottomStart by animateDpAsState(
        targetValue = corner(if (if (vertical) last else first) 25.dp else 4.dp, outerPressedRadius),
        animationSpec = cornerAnimation,
        label = "alohaSegmentBottomStart",
    )

    // An underdamped spring can briefly overshoot below its target when the
    // global corner scale changes (including a transition to 0.dp). CornerBasedShape
    // validates resolved radii before AlohaPathShape.createOutline() can clamp them,
    // so sanitize every animated value before constructing any CornerBasedShape.
    val safeTopStart = topStart.coerceAtLeast(0.dp)
    val safeTopEnd = topEnd.coerceAtLeast(0.dp)
    val safeBottomEnd = bottomEnd.coerceAtLeast(0.dp)
    val safeBottomStart = bottomStart.coerceAtLeast(0.dp)

    return when (LocalMaterialShapeStyle.current) {
        1 -> CutCornerShape(safeTopStart, safeTopEnd, safeBottomEnd, safeBottomStart)
        2, 3, 4, 5, 6 -> AlohaPathShape(LocalMaterialShapeStyle.current, densityScale, safeTopStart, safeTopEnd, safeBottomEnd, safeBottomStart)
        else -> RoundedCornerShape(safeTopStart, safeTopEnd, safeBottomEnd, safeBottomStart)
    }
}

@Composable
private fun defaultSingleSegmentedShape(index: Int, count: Int): ListItemShapes {
    // Material 3 resolves shape independently for default, pressed and selected
    // states. Apply Aloha's live theme shape to all three slots; otherwise the
    // row silently falls back to Material 3 defaults as soon as it is pressed
    // or selected, even though the normal state follows the active theme shape settings.
    val themeShape = alohaSegmentedShape(index, count, vertical = true)
    return ListItemDefaults.segmentedShapes(index, count).copy(
        shape = themeShape,
        pressedShape = themeShape,
        selectedShape = themeShape,
    )
}

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    visibleLen: Int = 0,
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            content.forEachIndexed { index, itemContent ->
                val itemCount = if (visibleLen > 0) visibleLen else content.size
                CompositionLocalProvider(
                    LocalListItemShapes provides defaultSingleSegmentedShape(index, itemCount),
                    LocalSegmentedShapeConfig provides SegmentedShapeConfig(index, itemCount),
                ) {
                    itemContent()
                }
            }
        }
    }
}


/**
 * Shared frame for option groups that contain their own chips/tiles instead of a single ListItem.
 * It inherits the row's segmented shape and consumes the same global container shadow/outline
 * settings as neighboring settings rows.
 */
@Composable
internal fun SegmentedOptionFrame(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val itemShapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1)
    val configuredOutline = LocalMaterialOutlineWidth.current
    val frameBorder = if (configuredOutline > 0f) {
        BorderStroke(configuredOutline.dp, colorScheme.outlineVariant)
    } else {
        // Keep the structural option frame visible even when optional control outlines are disabled.
        BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.45f))
    }
    ExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = itemShapes.shape,
        color = colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = if (LocalMaterialShadowContainers.current) 1.dp else 0.dp,
        border = frameBorder,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
fun SegmentedItem(
    index: Int,
    count: Int,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalListItemShapes provides defaultSingleSegmentedShape(index, count),
        LocalSegmentedShapeConfig provides SegmentedShapeConfig(index, count),
    ) {
        content()
    }
}

/**
 * Keep a short tap visible for at least one brief interval. Compose batches
 * state writes before a frame, so Press + Release in one frame can otherwise
 * disappear when represented only by collectIsPressedAsState().
 *
 * Track each Press by identity so overlapping presses and cancellation remain
 * correctly paired. The existing shape spring remains responsible for motion.
 */
@Composable
private fun MutableInteractionSource.collectIsPressedWithMinimumDuration(
    minimumPressedMillis: Long = 100L,
): Boolean {
    val activePresses = remember(this) { mutableStateListOf<PressInteraction.Press>() }
    LaunchedEffect(this) {
        interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> activePresses.add(interaction)
                is PressInteraction.Release -> launch {
                    delay(minimumPressedMillis)
                    activePresses.remove(interaction.press)
                }
                is PressInteraction.Cancel -> launch {
                    delay(minimumPressedMillis)
                    activePresses.remove(interaction.press)
                }
            }
        }
    }
    return activePresses.isNotEmpty()
}

@Composable
private fun ListItemShapes.animatedAlohaShapes(
    config: SegmentedShapeConfig?,
    pressed: Boolean,
): Pair<ListItemShapes, Shape> {
    val animatedShape = config?.let { alohaSegmentedShape(it.index, it.count, it.vertical, pressed) } ?: shape
    return copy(shape = animatedShape, pressedShape = animatedShape, selectedShape = animatedShape) to animatedShape
}

@Composable
fun SegmentedListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    interactionSource: MutableInteractionSource? = null,
    headlineContent: @Composable () -> Unit,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val itemShapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1)
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed = resolvedInteractionSource.collectIsPressedWithMinimumDuration()
    val (animatedShapes, outlineShape) = itemShapes.animatedAlohaShapes(LocalSegmentedShapeConfig.current, isPressed)
    // Keep the optional outer shadow and outline on the exact same state shape used
    // by the Material 3 row for press/selection feedback.
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) Modifier.shadow(1.dp, outlineShape)
            else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, outlineShape)
            } ?: Modifier
        )
        .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
    SegmentedListItem(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        enabled = enabled,
        colors = colors,
        interactionSource = resolvedInteractionSource,
        shapes = animatedShapes,
        modifier = styledModifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = Alignment.CenterVertically,
        content = headlineContent
    )
}

@Composable
fun SegmentedListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    interactionSource: MutableInteractionSource? = null,
    headlineContent: @Composable () -> Unit,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val itemShapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1)
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed = resolvedInteractionSource.collectIsPressedWithMinimumDuration()
    val (animatedShapes, outlineShape) = itemShapes.animatedAlohaShapes(LocalSegmentedShapeConfig.current, isPressed)
    // Keep the optional outer shadow and outline on the exact same state shape used
    // by the Material 3 row for press/selection feedback.
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) Modifier.shadow(1.dp, outlineShape)
            else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, outlineShape)
            } ?: Modifier
        )
        .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = animatedShapes,
        modifier = styledModifier,
        enabled = enabled,
        colors = colors,
        interactionSource = resolvedInteractionSource,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = Alignment.CenterVertically,
        onLongClick = onLongClick,
        content = headlineContent
    )
}

@Composable
fun SegmentedListItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    interactionSource: MutableInteractionSource? = null,
    headlineContent: @Composable () -> Unit,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val itemShapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1)
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed = resolvedInteractionSource.collectIsPressedWithMinimumDuration()
    val (animatedShapes, outlineShape) = itemShapes.animatedAlohaShapes(LocalSegmentedShapeConfig.current, isPressed)
    // Keep the optional outer shadow and outline on the exact same state shape used
    // by the Material 3 row for press/selection feedback.
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) Modifier.shadow(1.dp, outlineShape)
            else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, outlineShape)
            } ?: Modifier
        )
        .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = animatedShapes,
        modifier = styledModifier,
        enabled = enabled,
        colors = colors,
        interactionSource = resolvedInteractionSource,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = Alignment.CenterVertically,
        onLongClick = onLongClick,
        content = headlineContent
    )
}

@Composable
fun SegmentedSwitchItem(
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    title: String,
    summary: String? = null,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    checked: Boolean,
    enabled: Boolean = true,
    dimWhenDisabled: Boolean = true,
    showChildIndicator: Boolean = false,
    expanded: Boolean? = null,
    onRowClick: (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val rowEnabled = enabled || !dimWhenDisabled
    val interactionSource = remember { MutableInteractionSource() }
    val switchInteractionSource = remember { MutableInteractionSource() }

    SegmentedListItem(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            if (onRowClick != null) {
                onRowClick()
            } else {
                onCheckedChange(!checked)
            }
        },
        enabled = rowEnabled,
        interactionSource = interactionSource,
        colors = colors,
        headlineContent = { Text(title) },
        leadingContent = leadingContent ?: icon?.let { { Icon(it, title) } },
        trailingContent = {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showChildIndicator) {
                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(top = 8.dp, bottom = 8.dp, end = 15.dp),
                    )
                }
                ExpressiveSwitch(
                    checked = checked,
                    enabled = enabled,
                    // The trailing switch must remain independently clickable even
                    // when the whole row also toggles the same setting. Compose consumes
                    // the child's click so the parent row does not apply a second toggle.
                    onCheckedChange = { value ->
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onCheckedChange(value)
                    },
                    interactionSource = switchInteractionSource,
                )
            }
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

@Composable
fun SegmentedChoiceOptionsItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    enabled: Boolean = true,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    // The framed setting row uses a shared container shape and interaction treatment,
    // then opens the native choice surface. It does not render a permanent FlowRow of chips.
    SegmentedDropdownItem(
        icon = icon,
        title = title,
        summary = summary,
        items = items,
        colors = colors,
        enabled = enabled,
        selectedIndex = selectedIndex,
        onItemSelected = onItemSelected,
    )
}

@Composable
fun SegmentedDropdownItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    val hasItems = items.isNotEmpty()
    val safeIndex = if (hasItems) {
        selectedIndex.coerceIn(0, items.lastIndex)
    } else {
        -1
    }

    SegmentedListItem(
        onClick = if (enabled) {
            {
                onClick?.invoke()
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                expanded = true
            }
        } else null,
        enabled = enabled,
        colors = colors,
        leadingContent = icon?.let { { Icon(it, title) } },
        headlineContent = { Text(text = title) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = {
            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                // Match HyperDuo/Miuix: always show a direction cue, and flip it while
                // the selection menu is expanded so dropdown rows are visibly interactive.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (hasItems && safeIndex >= 0) items[safeIndex] else "",
                        textAlign = TextAlign.End,
                        color = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp).size(24.dp),
                        tint = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenuPopup(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuGroup(
                        shapes = MenuDefaults.groupShapes(
                        shape = alohaCornerShape(20.dp),
                        inactiveShape = alohaCornerShape(20.dp),
                    ),
                        shadowElevation = if (LocalMaterialShadowContainers.current) {
                            MenuDefaults.ShadowElevation
                        } else 0.dp,
                        border = LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                            BorderStroke(it.dp, colorScheme.outlineVariant)
                        },
                    ) {
                        items.forEachIndexed { index, text ->
                            SelectableDropdownMenuItem(
                                text = { Text(text) },
                                selected = index == safeIndex,
                                onClick = {
                                    if (index in items.indices) {
                                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                        onItemSelected(index)
                                    }
                                    expanded = false
                                },
                                shapes = MenuDefaults.itemShape(index = index, count = items.size),
                                leadingIcon = {
                                    if (index == safeIndex) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SegmentedRadioItem(
    title: String,
    summary: String? = null,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    SegmentedListItem(
        selected = selected,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        enabled = enabled,
        colors = colors,
        headlineContent = { Text(title) },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorScheme.primary,
                    unselectedColor = colorScheme.onSurfaceVariant,
                ),
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

@Composable
fun SegmentedCheckboxItem(
    title: String,
    summary: String? = null,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    SegmentedListItem(
        checked = checked,
        onCheckedChange = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onCheckedChange(it)
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        headlineContent = { Text(title) },
        leadingContent = {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                interactionSource = interactionSource,
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    uncheckedColor = colorScheme.onSurfaceVariant,
                    checkmarkColor = colorScheme.onPrimary,
                ),
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

@Composable
fun SegmentedTextField(
    modifier: Modifier = Modifier,
    label: String = "",
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(colorScheme.primary),
    placeholder: @Composable (() -> Unit)? = { Text("-") },
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    isError: Boolean = false
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    SegmentedListItem(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(focusRequester),
        colors = colors,
        onClick = { focusRequester.requestFocus() },
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        headlineContent = {
            Column {
                if (label.isNotEmpty()) {
                    Text(text = label, color = if (isError) colorScheme.error else colors.contentColor)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = textStyle.copy(
                        colors.supportingContentColor,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    ),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                    visualTransformation = visualTransformation,
                    onTextLayout = onTextLayout,
                    interactionSource = interactionSource,
                    cursorBrush = cursorBrush,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty() && placeholder != null) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                CompositionLocalProvider(
                                    LocalContentColor provides colors.supportingContentColor
                                ) {
                                    ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                                        placeholder()
                                    }
                                }
                            }
                        }
                        innerTextField()
                    }
                )
            }
        }
    )
}
