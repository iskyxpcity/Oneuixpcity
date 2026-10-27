package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.alohaoneui.app.ui.theme.LocalMaterialCornerScale
import com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons
import kotlinx.coroutines.launch

val LocalListItemShapes = compositionLocalOf<ListItemShapes?> { null }

/**
 * Voyage's source G² continuity profile (decompiled p000/dan.java).
 *
 * The four profile values are, in source order: extended fraction, arc fraction,
 * Bezier curvature scale, and arc curvature scale. The profile is deliberately
 * kept separate from the Squircle smoothing parameter: Voyage uses this profile
 * to construct a cubic/arc transition, not the b2t0 smoothing shortcut.
 */
private data class VoyageG2Profile(
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
     * Direct port of dan.m88494l() and fqw0.m95419o(): the returned cubic is
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
     * ban.m5206I() computes eight independent width/height progress values:
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
     * Direct parameter interpolation from ban.m5206I(). The source does not
     * interpolate a single profile per corner: it computes an aspect progress
     * for each adjacent half-extent, uses the pair minimum for the shared
     * corner profile, then scales the extended/arc/Bezier values independently.
     */
    fun interpolateFromCapsule(progress: Double, sharedCornerProgress: Double = progress): VoyageG2Profile {
        val t = progress.coerceIn(0.0, 1.0)
        val shared = sharedCornerProgress.coerceIn(0.0, 1.0)
        fun mix(from: Double, to: Double, amount: Double) = from + (to - from) * amount
        return VoyageG2Profile(
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
        // dan.f104767lI and dan.f104768lO from Voyage.
        val smooth = VoyageG2Profile(0.5286651, 0.5555555555555556, 1.0732051, 1.0732051)
        val capsule = VoyageG2Profile(0.396498825, 0.0, 1.0, 1.0)
    }
}

private class VoyagePathShape(
    private val style: Int,
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
) : Shape {
    private data class OutlineKey(
        val width: Float, val height: Float, val density: Float, val direction: LayoutDirection,
        val topStart: Float, val topEnd: Float, val bottomEnd: Float, val bottomStart: Float,
    )
    private val outlineCache = object : java.util.LinkedHashMap<OutlineKey, Outline>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<OutlineKey, Outline>?): Boolean = size > 24
    }

    @Synchronized
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val key = OutlineKey(
            size.width, size.height, density.density, layoutDirection,
            topStart.value, topEnd.value, bottomEnd.value, bottomStart.value,
        )
        outlineCache[key]?.let { return it }
        return createVoyageOutline(size, layoutDirection, density).also { outlineCache[key] = it }
    }

    private fun createVoyageOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val ts = with(density) { topStart.toPx() }.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val te = with(density) { topEnd.toPx() }.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val be = with(density) { bottomEnd.toPx() }.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val bs = with(density) { bottomStart.toPx() }.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val tl = if (layoutDirection == LayoutDirection.Ltr) ts else te
        val tr = if (layoutDirection == LayoutDirection.Ltr) te else ts
        val br = if (layoutDirection == LayoutDirection.Ltr) be else bs
        val bl = if (layoutDirection == LayoutDirection.Ltr) bs else be
        val w = size.width
        val h = size.height

        // Voyage ban.m5206I() delegates equal-radius shapes that touch a full
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
                // Voyage Squircle is the smoothing-profile path in e7g0/b2t0,
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
                // Voyage G² Smooth profile: use the control-point scale derived from
                // dan.m88494l() rather than reusing the Squircle smoothing=48 shortcut.
                // The profile is source-defined (dan.f104767lI); its second control
                // point supplies the tangent scale used at each radius-sized corner.
                data class CornerInsets(val horizontal: Float, val vertical: Float)

                val progress = VoyageG2Profile.smooth.cornerProgresses(w, h, tl, tr, br, bl)
                val tlShared = minOf(progress[0], progress[1])
                val trShared = minOf(progress[2], progress[3])
                val brShared = minOf(progress[4], progress[5])
                val blShared = minOf(progress[6], progress[7])

                data class ProfileInsets(val horizontal: Float, val vertical: Float)

                fun profileInsets(axisProgress: Double, cornerProgress: Double): ProfileInsets {
                    val profile = VoyageG2Profile.smooth
                        .interpolateFromCapsule(axisProgress, cornerProgress)
                    val cubic = profile.cubic()
                    // Use the solved Bezier controls, not p2/p3 (which are
                    // tangent-direction construction points in fqw0.m95419o()).
                    // The old p2.x lookup was always 1.0 and therefore collapsed
                    // every corner to the same arbitrary 8% inset.
                    val xSpan = (cubic.p3.x - cubic.p0.x).let { kotlin.math.abs(it) }
                    val ySpan = (cubic.p3.y - cubic.p0.y).let { kotlin.math.abs(it) }
                    val horizontal = if (xSpan > 1e-9) {
                        kotlin.math.abs(cubic.p1.x - cubic.p0.x) / xSpan
                    } else 0.0
                    val vertical = if (ySpan > 1e-9) {
                        kotlin.math.abs(cubic.p3.y - cubic.p2.y) / ySpan
                    } else 0.0
                    return ProfileInsets(
                        horizontal.coerceIn(0.0, 1.0).toFloat(),
                        vertical.coerceIn(0.0, 1.0).toFloat(),
                    )
                }

                fun cornerInsets(horizontalIndex: Int, verticalIndex: Int, shared: Double): CornerInsets {
                    val horizontal = profileInsets(progress[horizontalIndex], shared)
                    val vertical = profileInsets(progress[verticalIndex], shared)
                    return CornerInsets(
                        horizontal = horizontal.horizontal,
                        vertical = vertical.vertical,
                    )
                }

                val tlInset = cornerInsets(1, 0, tlShared)
                val trInset = cornerInsets(2, 3, trShared)
                val brInset = cornerInsets(5, 4, brShared)
                val blInset = cornerInsets(6, 7, blShared)
                path.moveTo(tl, 0f)
                path.lineTo(w - tr, 0f)
                if (tr > 0f) path.cubicTo(w - tr * trInset.horizontal, 0f, w, tr * trInset.vertical, w, tr)
                path.lineTo(w, h - br)
                if (br > 0f) path.cubicTo(w, h - br * brInset.vertical, w - br * brInset.horizontal, h, w - br, h)
                path.lineTo(bl, h)
                if (bl > 0f) path.cubicTo(bl * blInset.horizontal, h, 0f, h - bl * blInset.vertical, 0f, h - bl)
                path.lineTo(0f, tl)
                if (tl > 0f) path.cubicTo(0f, tl * tlInset.vertical, tl * tlInset.horizontal, 0f, tl, 0f)
            }
            4 -> {
                // Voyage source: p000/wgo0.java, mo5154ll + m140359lI/m140360lO/
                // m140361lo. Keep its density scaling, radius shrink, phase length,
                // adaptive sampling and quadratic sample joins.
                val minDimension = minOf(w, h)
                if (w <= 0f || h <= 0f || minDimension <= 0f) {
                    path.addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h))
                } else {
                    val scale = (minDimension / (48f * density.density)).coerceIn(0.42f, 1f)
                    // wgo0.mo5154ll uses 12dp as the target wave length only to
                    // choose an integer wave count; m140361lo receives the adjusted
                    // perimeterLength / waveCount as its actual sine period.
                    val targetWaveLength = (12f * density.density * scale).coerceAtLeast(1f)
                    val amplitude = (0.9f * density.density * scale)
                        .coerceIn(0f, minDimension / 4f)
                    val aTL = if (tl > 0f || tr > 0f) amplitude else 0f
                    val aTR = if (tr > 0f || br > 0f) amplitude else 0f
                    val aBR = if (br > 0f || bl > 0f) amplitude else 0f
                    val aBL = if (bl > 0f || tl > 0f) amplitude else 0f
                    val rtl = (tl - amplitude).coerceAtLeast(0f)
                    val rtr = (tr - amplitude).coerceAtLeast(0f)
                    val rbr = (br - amplitude).coerceAtLeast(0f)
                    val rbl = (bl - amplitude).coerceAtLeast(0f)

                    // Direct translation of Voyage p000/wgo0.java:
                    // each corner's wave amplitude is enabled only when either
                    // adjacent corner radius is non-zero.
                    val xRight = w - aTR
                    val yBottom = h - aBR
                    val xTopStart = if (tl > 0f) aBL + rtl else 0f
                    val xTopEnd = if (tr > 0f) xRight - rtr else w
                    val yRightStart = if (tr > 0f) aTL + rtr else 0f
                    val yRightEnd = if (br > 0f) yBottom - rbr else h
                    val xBottomStart = if (br > 0f) xRight - rbr else w
                    val xBottomEnd = if (bl > 0f) aBL + rbl else 0f
                    val yLeftStart = if (bl > 0f) yBottom - rbl else h
                    val yLeftEnd = if (tl > 0f) aTL + rtl else 0f
                    val topLength = (xTopEnd - xTopStart).coerceAtLeast(0f)
                    val rightLength = (yRightEnd - yRightStart).coerceAtLeast(0f)
                    val bottomLength = (xBottomStart - xBottomEnd).coerceAtLeast(0f)
                    val leftLength = (yLeftStart - yLeftEnd).coerceAtLeast(0f)
                    val perimeterLength = topLength + rightLength + bottomLength + leftLength +
                        (rtl + rtr + rbr + rbl) * (Math.PI.toFloat() / 2f)

                    if (amplitude <= 0f || perimeterLength <= 0f) {
                        path.addRoundRect(androidx.compose.ui.geometry.RoundRect(
                            left = 0f, top = 0f, right = w, bottom = h,
                            topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(tl, tl),
                            topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(tr, tr),
                            bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(br, br),
                            bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(bl, bl),
                        ))
                    } else {
                        // Voyage wgo0.java: split the perimeter into waves first,
                        // then choose subdivisions from BOTH the per-wave length and
                        // the total path length. Keep the source constants explicit.
                        val waveCount = kotlin.math.ceil(perimeterLength / targetWaveLength).toInt().coerceAtLeast(1)
                        val waveLength = perimeterLength / waveCount
                        val sourceArcSubdivisions = 8f
                        val sourcePathSubdivisions = 360f
                        val sampleStep = maxOf(
                            waveLength / sourceArcSubdivisions,
                            perimeterLength / sourcePathSubdivisions,
                            1f
                        )
                        class WaveSample(
                            val point: androidx.compose.ui.geometry.Offset,
                            val normal: androidx.compose.ui.geometry.Offset,
                            val phaseDistance: Float,
                            val localAmplitude: Float,
                        )
                        val samples = mutableListOf<WaveSample>()
                        var phaseDistance = 0f

                        fun addLine(
                            x0: Float, y0: Float, x1: Float, y1: Float,
                            nx: Float, ny: Float, length: Float, localAmplitude: Float,
                        ) {
                            val count = kotlin.math.ceil(length / sampleStep).toInt().coerceAtLeast(1)
                            // Voyage's shared path-start flag makes only the very first
                            // segment emit t=0; every later segment begins at its t=1/count
                            // sample so adjacent edges do not duplicate their join point.
                            val firstSample = if (samples.isEmpty()) 0 else 1
                            for (i in firstSample..count) {
                                val t = i.toFloat() / count
                                samples += WaveSample(
                                    androidx.compose.ui.geometry.Offset(
                                        x0 + (x1 - x0) * t,
                                        y0 + (y1 - y0) * t
                                    ),
                                    androidx.compose.ui.geometry.Offset(nx, ny),
                                    phaseDistance + length * t,
                                    localAmplitude
                                )
                            }
                            phaseDistance += length
                        }

                        fun addArc(
                            cx: Float, cy: Float, radius: Float,
                            startDegrees: Float, sweepDegrees: Float,
                        ) {
                            // Voyage still samples a zero-radius quarter arc: it has
                            // zero perimeter length, but its terminal normal can affect the
                            // displaced sample. Keep that source edge case instead of skipping it.
                            val arcLength = kotlin.math.PI.toFloat() * radius / 2f
                            val count = kotlin.math.ceil(arcLength / sampleStep).toInt().coerceAtLeast(1)
                            val firstSample = if (samples.isEmpty()) 0 else 1
                            for (i in firstSample..count) {
                                val t = i.toFloat() / count
                                val angle = Math.toRadians((startDegrees + sweepDegrees * t).toDouble())
                                val nx = kotlin.math.cos(angle).toFloat()
                                val ny = kotlin.math.sin(angle).toFloat()
                                samples += WaveSample(
                                    androidx.compose.ui.geometry.Offset(cx + radius * nx, cy + radius * ny),
                                    androidx.compose.ui.geometry.Offset(nx, ny),
                                    phaseDistance + arcLength * t,
                                    amplitude
                                )
                            }
                            phaseDistance += arcLength
                        }

                        // Voyage's perimeter order: top, TR arc, right, BR arc,
                        // bottom, BL arc, left, TL arc. Distances are accumulated
                        // by the source segment lengths, not sample-to-sample chords.
                        addLine(xTopStart, aTL, xTopEnd, aTL, 0f, -1f, topLength, aTL)
                        addArc(xRight - rtr, aTL + rtr, rtr, -90f, 90f)
                        addLine(xRight, yRightStart, xRight, yRightEnd, 1f, 0f, rightLength, aTR)
                        addArc(xRight - rbr, h - rbr, rbr, 0f, 90f)
                        addLine(xBottomStart, yBottom, xBottomEnd, yBottom, 0f, 1f, bottomLength, aBR)
                        addArc(aBL + rbl, h - rbl, rbl, 90f, 90f)
                        addLine(aBL, yLeftStart, aBL, yLeftEnd, -1f, 0f, leftLength, aBL)
                        addArc(aBL + rtl, aTL + rtl, rtl, 180f, 90f)

                        val displaced = samples.map { sample ->
                            val wave = sample.localAmplitude * kotlin.math.sin(
                                sample.phaseDistance * 2f * Math.PI.toFloat() / waveLength
                            )
                            androidx.compose.ui.geometry.Offset(
                                sample.point.x + sample.normal.x * wave,
                                sample.point.y + sample.normal.y * wave
                            )
                        }
                        if (displaced.isNotEmpty()) {
                            path.moveTo(displaced.first().x, displaced.first().y)
                            for (i in 1 until displaced.size) {
                                val previous = displaced[i - 1]
                                val current = displaced[i]
                                path.quadraticTo(
                                    previous.x, previous.y,
                                    (previous.x + current.x) / 2f,
                                    (previous.y + current.y) / 2f
                                )
                            }
                            // Voyage's m140361lo emits quadratic segments only while
                            // advancing through the sampled path. The source then closes
                            // the path directly; it does not add an extra last-to-first
                            // quadratic segment.
                            path.close()
                        }
                    }
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
fun voyageCornerShape(radius: androidx.compose.ui.unit.Dp): Shape {
    val scaledRadius = radius * LocalMaterialCornerScale.current.coerceIn(0f, 2f)
    val style = LocalMaterialShapeStyle.current
    return when (style) {
        1 -> remember(style, scaledRadius) { CutCornerShape(scaledRadius) }
        2, 3, 4, 5, 6 -> remember(style, scaledRadius) {
            VoyagePathShape(style, scaledRadius, scaledRadius, scaledRadius, scaledRadius)
        }
        else -> remember(style, scaledRadius) { RoundedCornerShape(scaledRadius) }
    }
}

@Composable
fun alohaOneUISegmentedColors(): ListItemColors = ListItemDefaults.segmentedColors().copy(
    containerColor = colorScheme.surfaceContainerHigh,
    disabledContainerColor = colorScheme.surfaceContainerHigh,
    supportingContentColor = colorScheme.outline
)

/**
 * Direct port of Voyage's zee0.m147825O shape rule:
 * 16dp outer / 4dp inner corners, animated with dampingRatio=0.4 and stiffness=200.
 * vertical=true joins rows top-to-bottom; false joins selection buttons left-to-right.
 */
@Composable
fun voyageSegmentedShape(index: Int, count: Int, vertical: Boolean = true): Shape {
    val single = count == 1 || index == -1
    val first = single || index == 0
    val last = single || index == count - 1
    // The setting exposes a 0.0..2.0 scale. Preserve the full range so values
    // above 1.0 enlarge corner geometry instead of silently behaving like 1.0.
    val cornerScale = LocalMaterialCornerScale.current.coerceIn(0f, 2f)

    val topStart by animateDpAsState(
        targetValue = (if (first) 16.dp else 4.dp) * cornerScale,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "voyageSegmentTopStart",
    )
    val topEnd by animateDpAsState(
        targetValue = (if (if (vertical) first else last) 16.dp else 4.dp) * cornerScale,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "voyageSegmentTopEnd",
    )
    val bottomEnd by animateDpAsState(
        targetValue = (if (last) 16.dp else 4.dp) * cornerScale,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "voyageSegmentBottomEnd",
    )
    val bottomStart by animateDpAsState(
        targetValue = (if (if (vertical) last else first) 16.dp else 4.dp) * cornerScale,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "voyageSegmentBottomStart",
    )

    return when (LocalMaterialShapeStyle.current) {
        1 -> CutCornerShape(topStart, topEnd, bottomEnd, bottomStart)
        2, 3, 4, 5, 6 -> VoyagePathShape(LocalMaterialShapeStyle.current, topStart, topEnd, bottomEnd, bottomStart)
        else -> RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    }
}

@Composable
private fun defaultSingleSegmentedShape(index: Int, count: Int): ListItemShapes =
    ListItemDefaults.segmentedShapes(index, count).copy(
        shape = voyageSegmentedShape(index, count, vertical = true)
    )

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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content.forEachIndexed { index, itemContent ->
                CompositionLocalProvider(
                    LocalListItemShapes provides defaultSingleSegmentedShape(
                        index = index,
                        count = if (visibleLen > 0) visibleLen else content.size
                    ),
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
    Surface(
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
    ) {
        content()
    }
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
    // Voyage's setting rows use their native ripple and source shape morphing; no
    // synthetic press-scale curve is applied to the entire row.
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) Modifier.shadow(1.dp, itemShapes.shape)
            else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, itemShapes.shape)
            } ?: Modifier
        )
        .then(if (enabled) Modifier else Modifier.alpha(0.5f))
    SegmentedListItem(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        enabled = enabled,
        colors = colors,
        interactionSource = resolvedInteractionSource,
        shapes = itemShapes,
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
    // Voyage's setting rows use their native ripple and source shape morphing; no
    // synthetic press-scale curve is applied to the entire row.
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) Modifier.shadow(1.dp, itemShapes.shape)
            else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, itemShapes.shape)
            } ?: Modifier
        )
        .then(if (enabled) Modifier else Modifier.alpha(0.5f))
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = itemShapes,
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
    // Voyage's setting rows use their native ripple and source shape morphing; no
    // synthetic press-scale curve is applied to the entire row.
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) Modifier.shadow(1.dp, itemShapes.shape)
            else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, itemShapes.shape)
            } ?: Modifier
        )
        .then(if (enabled) Modifier else Modifier.alpha(0.5f))
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = itemShapes,
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
    title: String,
    summary: String? = null,
    items: List<String>,
    colors: ListItemColors = alohaOneUISegmentedColors(),
    enabled: Boolean = true,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    // Voyage k9t0.m108317Il / vfw0.m137946o renders a framed setting row,
    // then opens the native choice surface. It does not render a permanent FlowRow of chips.
    SegmentedDropdownItem(
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
                        shapes = MenuDefaults.groupShapes(),
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
                enabled = enabled
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
                modifier = Modifier.size(24.dp)
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
