package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateDpAsState
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
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons
import kotlinx.coroutines.launch

val LocalListItemShapes = compositionLocalOf<ListItemShapes?> { null }


/**
 * Shape geometry context for a segmented row. This lets custom Voyage paths
 * animate their real corner parameters instead of relying on Material 3's
 * CornerBasedShape-only pressed-shape resolver.
 */
private data class SegmentedShapeConfig(val index: Int, val count: Int, val vertical: Boolean = true)
private val LocalSegmentedShapeConfig = compositionLocalOf<SegmentedShapeConfig?> { null }

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
        val capsule = VoyageG2Profile(0.396498825, 1.0, 1.0, 1.0)
    }
}


/**
 * Voyage G² outline reconstruction from ban.java / dan.java / fqw0.java /
 * opw0.java. The two cubic pieces around each circular arc are retained; a
 * single-cubic corner is not equivalent to Voyage's G² profile.
 */
private fun appendVoyageG2Arc(
    path: Path,
    centerX: Double,
    centerY: Double,
    radius: Double,
    curvatureFactor: Double,
    startAngle: Double,
    sweepAngle: Double,
) {
    if (radius <= 0.0) return
    val middle = startAngle + sweepAngle * 0.5
    val centerShift = radius * (1.0 - curvatureFactor)
    val shiftedX = centerX + kotlin.math.cos(middle) * centerShift
    val shiftedY = centerY + kotlin.math.sin(middle) * centerShift
    val arcRadius = radius * curvatureFactor
    if (arcRadius <= 0.0) return
    path.arcTo(
        Rect(
            (shiftedX - arcRadius).toFloat(),
            (shiftedY - arcRadius).toFloat(),
            (shiftedX + arcRadius).toFloat(),
            (shiftedY + arcRadius).toFloat(),
        ),
        (startAngle * 180.0 / Math.PI).toFloat(),
        (sweepAngle * 180.0 / Math.PI).toFloat(),
        false,
    )
}

private fun appendVoyageG2Cubic(
    path: Path,
    firstControl: VoyageG2Profile.Point,
    secondControl: VoyageG2Profile.Point,
    end: VoyageG2Profile.Point,
    transform: (VoyageG2Profile.Point) -> VoyageG2Profile.Point,
) {
    val c1 = transform(firstControl)
    val c2 = transform(secondControl)
    val p3 = transform(end)
    path.cubicTo(
        c1.x.toFloat(), c1.y.toFloat(),
        c2.x.toFloat(), c2.y.toFloat(),
        p3.x.toFloat(), p3.y.toFloat(),
    )
}

private fun createVoyageG2CapsulePath(width: Float, height: Float, radiusPx: Float): Path {
    val path = Path()
    if (width <= 0f || height <= 0f || radiusPx <= 0f) {
        addVoyageRoundRect(path, width, height, radiusPx, radiusPx, radiusPx, radiusPx)
        return path
    }
    if (width == height) {
        path.addOval(Rect(0f, 0f, width, height))
        return path
    }

    val radius = radiusPx.toDouble()
    val smooth = VoyageG2Profile.smooth
    val capsule = VoyageG2Profile.capsule
    val major = maxOf(width, height).toDouble()
    val progress = ((((major * 0.5) / radius) - 1.0) / capsule.extendedFraction)
        .coerceIn(0.0, 1.0)
    val extended = capsule.extendedFraction * progress
    val bezierScale = smooth.bezierCurvatureScale +
        (capsule.bezierCurvatureScale - smooth.bezierCurvatureScale) * progress
    val curve = VoyageG2Profile(
        extendedFraction = extended,
        arcFraction = capsule.arcFraction,
        bezierCurvatureScale = bezierScale,
        arcCurvatureScale = 1.0,
    ).cubic()
    val arcAngle = capsule.arcFraction * Math.PI / 2.0
    val arcOffset = (Math.PI / 2.0 - arcAngle) * 0.5
    val sweep = (arcAngle + arcOffset) * 2.0
    val offset = -radius * extended

    if (width > height) {
        val originX = width.toDouble() - radius
        val bottom = height.toDouble()
        path.moveTo(0f, radius.toFloat())
        appendVoyageG2Arc(path, radius, radius, radius, 1.0, arcOffset + Math.PI / 2.0, sweep)
        appendVoyageG2Cubic(
            path, curve.p2, curve.p1, curve.p0,
        ) { p -> VoyageG2Profile.Point(radius - p.x * radius, p.y * radius) }
        path.lineTo((originX + offset).toFloat(), 0f)
        appendVoyageG2Cubic(
            path, curve.p1, curve.p2, curve.p3,
        ) { p -> VoyageG2Profile.Point(originX + p.x * radius, p.y * radius) }
        appendVoyageG2Arc(
            path, originX, radius, radius, 1.0,
            -(Math.PI / 2.0 - arcOffset), sweep,
        )
        appendVoyageG2Cubic(
            path, curve.p2, curve.p1, curve.p0,
        ) { p -> VoyageG2Profile.Point(originX + p.x * radius, bottom - p.y * radius) }
        path.lineTo((radius - offset).toFloat(), bottom.toFloat())
        appendVoyageG2Cubic(
            path, curve.p1, curve.p2, curve.p3,
        ) { p -> VoyageG2Profile.Point(radius - p.x * radius, bottom - p.y * radius) }
    } else {
        val originY = height.toDouble() - radius
        val right = width.toDouble()
        path.moveTo((radius - offset).toFloat(), 0f)
        appendVoyageG2Cubic(
            path, curve.p1, curve.p2, curve.p3,
        ) { p -> VoyageG2Profile.Point(p.y * radius, radius - p.x * radius) }
        appendVoyageG2Arc(
            path, radius, radius, radius, 1.0,
            -(Math.PI - arcOffset), sweep,
        )
        appendVoyageG2Cubic(
            path, curve.p2, curve.p1, curve.p0,
        ) { p -> VoyageG2Profile.Point(right - p.y * radius, radius - p.x * radius) }
        path.lineTo(right.toFloat(), (originY + offset).toFloat())
        appendVoyageG2Cubic(
            path, curve.p1, curve.p2, curve.p3,
        ) { p -> VoyageG2Profile.Point(right - p.y * radius, originY + p.x * radius) }
        appendVoyageG2Arc(
            path, right - radius, originY, radius, 1.0,
            arcOffset, sweep,
        )
        appendVoyageG2Cubic(
            path, curve.p2, curve.p1, curve.p0,
        ) { p -> VoyageG2Profile.Point(p.y * radius, originY + p.x * radius) }
    }
    path.close()
    return path
}

private fun createVoyageG2Path(
    width: Float,
    height: Float,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
): Path {
    if (width <= 0f || height <= 0f) {
        val path = Path()
        addVoyageRoundRect(path, width, height, topLeft, topRight, bottomRight, bottomLeft)
        return path
    }
    val allEqual = topLeft == topRight && topRight == bottomRight && bottomRight == bottomLeft
    if (allEqual && (topLeft + topRight == width || topLeft + bottomLeft == height)) {
        return createVoyageG2CapsulePath(width, height, topLeft)
    }

    val path = Path()
    val w = width.toDouble()
    val h = height.toDouble()
    val tl = topLeft.toDouble()
    val tr = topRight.toDouble()
    val br = bottomRight.toDouble()
    val bl = bottomLeft.toDouble()
    val progresses = VoyageG2Profile.smooth.cornerProgresses(
        width, height, topLeft, topRight, bottomRight, bottomLeft,
    )
    fun profile(axisProgress: Double, shared: Double) =
        VoyageG2Profile.smooth.interpolateFromCapsule(axisProgress, shared)
    fun point(x: Double, y: Double) = VoyageG2Profile.Point(x, y)
    fun append(
        first: VoyageG2Profile.Point,
        second: VoyageG2Profile.Point,
        end: VoyageG2Profile.Point,
        transform: (VoyageG2Profile.Point) -> VoyageG2Profile.Point,
    ) = appendVoyageG2Cubic(path, first, second, end, transform)

    val tlShared = minOf(progresses[0], progresses[1])
    val tlVertical = profile(progresses[0], tlShared)
    val tlHorizontal = profile(progresses[1], tlShared)
    val tlV = tlVertical.cubic()
    val tlH = tlHorizontal.cubic()

    val trShared = minOf(progresses[2], progresses[3])
    val trHorizontal = profile(progresses[2], trShared)
    val trVertical = profile(progresses[3], trShared)
    val trH = trHorizontal.cubic()
    val trV = trVertical.cubic()

    val brShared = minOf(progresses[4], progresses[5])
    val brVertical = profile(progresses[4], brShared)
    val brHorizontal = profile(progresses[5], brShared)
    val brV = brVertical.cubic()
    val brH = brHorizontal.cubic()

    val blShared = minOf(progresses[6], progresses[7])
    val blHorizontal = profile(progresses[6], blShared)
    val blVertical = profile(progresses[7], blShared)
    val blH = blHorizontal.cubic()
    val blV = blVertical.cubic()

    path.moveTo(0f, (tl - tlV.p0.x * tl).toFloat())
    if (tl > 0.0) {
        append(tlV.p1, tlV.p2, tlV.p3) { p -> point(p.y * tl, tl - p.x * tl) }
        appendVoyageG2Arc(
            path, tl, tl, tl, 1.0 / tlVertical.arcCurvatureScale,
            ((1.0 - tlVertical.arcFraction) * Math.PI / 4.0) + Math.PI,
            tlVertical.arcFraction * Math.PI / 2.0,
        )
        append(tlH.p2, tlH.p1, tlH.p0) { p -> point(tl - p.x * tl, p.y * tl) }
    }
    path.lineTo((w - tr + tr * trH.p0.x).toFloat(), 0f)

    if (tr > 0.0) {
        append(trH.p1, trH.p2, trH.p3) { p -> point(w - p.x * tr, p.y * tr) }
        appendVoyageG2Arc(
            path, w - tr, tr, tr, 1.0 / trVertical.arcCurvatureScale,
            ((1.0 - trVertical.arcFraction) * Math.PI / 4.0) - Math.PI / 2.0,
            trVertical.arcFraction * Math.PI / 2.0,
        )
        append(trV.p2, trV.p1, trV.p0) { p -> point(w - p.y * tr, tr - p.x * tr) }
    }
    path.lineTo(w.toFloat(), (h - br + br * brV.p0.x).toFloat())

    if (br > 0.0) {
        append(brV.p1, brV.p2, brV.p3) { p ->
            point(w - p.y * br, h - br + p.x * br)
        }
        appendVoyageG2Arc(
            path, w - br, h - br, br, 1.0 / brVertical.arcCurvatureScale,
            (1.0 - brVertical.arcFraction) * Math.PI / 4.0,
            brVertical.arcFraction * Math.PI / 2.0,
        )
        append(brH.p2, brH.p1, brH.p0) { p ->
            point(w - br + p.x * br, h - p.y * br)
        }
    }
    path.lineTo((bl + bl * blH.p0.x).toFloat(), h.toFloat())

    if (bl > 0.0) {
        append(blH.p1, blH.p2, blH.p3) { p ->
            point(p.y * bl, h - bl + p.x * bl)
        }
        appendVoyageG2Arc(
            path, bl, h - bl, bl, 1.0 / blVertical.arcCurvatureScale,
            ((1.0 - blVertical.arcFraction) * Math.PI / 4.0) + Math.PI / 2.0,
            blVertical.arcFraction * Math.PI / 2.0,
        )
        append(blV.p2, blV.p1, blV.p0) { p ->
            point(p.x * bl, h - bl + p.y * bl)
        }
    }
    path.close()
    return path
}

private fun addVoyageRoundRect(
    path: Path,
    width: Float,
    height: Float,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
) {
    path.addRoundRect(
        androidx.compose.ui.geometry.RoundRect(
            0f, 0f, width, height,
            androidx.compose.ui.geometry.CornerRadius(topLeft, topLeft),
            androidx.compose.ui.geometry.CornerRadius(topRight, topRight),
            androidx.compose.ui.geometry.CornerRadius(bottomRight, bottomRight),
            androidx.compose.ui.geometry.CornerRadius(bottomLeft, bottomLeft),
        ),
    )
}

/**
 * Source-faithful Wavy outline from wgo0.java. The perimeter is sampled at
 * max(waveLength / 8, perimeter / 360, 1px); each sample is displaced along
 * its local normal by a sinusoid whose phase advances continuously around the
 * entire perimeter. The source uses OR (not AND) for adjacent-corner edge
 * amplitudes and does not scale the corner radii with the wave-size factor.
 */
private data class VoyageWaveSample(val x: Float, val y: Float)

private fun createVoyageWavyPath(
    width: Float,
    height: Float,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
    density: Density,
): Path {
    val path = Path()
    val minDimension = minOf(width, height)
    if (width <= 0f || height <= 0f || minDimension <= 0f) {
        addVoyageRoundRect(path, width, height, topLeft, topRight, bottomRight, bottomLeft)
        return path
    }
    val scale = (minDimension / (48f * density.density)).coerceIn(0.42f, 1f)
    val period = 12f * density.density * scale
    val amplitude = minOf(0.9f * density.density * scale, minDimension / 4f)
    if (period <= 0f || amplitude <= 0f) {
        addVoyageRoundRect(path, width, height, topLeft, topRight, bottomRight, bottomLeft)
        return path
    }

    val tl = topLeft.coerceIn(0f, minDimension)
    val tr = topRight.coerceIn(0f, minDimension)
    val br = bottomRight.coerceIn(0f, minDimension)
    val bl = bottomLeft.coerceIn(0f, minDimension)
    fun edgeAmplitude(first: Float, second: Float) =
        if (first > 0f || second > 0f) amplitude else 0f
    val topAmplitude = edgeAmplitude(tl, tr)
    val rightAmplitude = edgeAmplitude(tr, br)
    val bottomAmplitude = edgeAmplitude(br, bl)
    val leftAmplitude = edgeAmplitude(bl, tl)
    if (topAmplitude == 0f && rightAmplitude == 0f &&
        bottomAmplitude == 0f && leftAmplitude == 0f
    ) {
        addVoyageRoundRect(path, width, height, tl, tr, br, bl)
        return path
    }

    val tlRadius = tl - amplitude
    val trRadius = tr - amplitude
    val brRadius = br - amplitude
    val blRadius = bl - amplitude
    val topStartX = if (tl > 0f) leftAmplitude + tlRadius else 0f
    val topEndX = if (tr > 0f) width - trRadius else width
    val rightStartY = if (tr > 0f) topAmplitude + trRadius else 0f
    val rightEndY = if (br > 0f) height - brRadius else height
    val bottomStartX = if (br > 0f) width - brRadius else width
    val bottomEndX = if (bl > 0f) leftAmplitude + blRadius else 0f
    val leftStartY = if (bl > 0f) height - blRadius else height
    val leftEndY = if (tl > 0f) topAmplitude + tlRadius else 0f

    val topLength = topEndX - topStartX
    val rightLength = rightEndY - rightStartY
    val bottomLength = bottomStartX - bottomEndX
    val leftLength = leftStartY - leftEndY
    val quarterPi = (Math.PI.toFloat() / 2f)
    val perimeter = topLength + quarterPi * trRadius +
        rightLength + quarterPi * brRadius +
        bottomLength + quarterPi * blRadius +
        leftLength + quarterPi * tlRadius
    val waveCount = kotlin.math.ceil(perimeter / period).toInt()
    if (waveCount <= 0 || !perimeter.isFinite()) {
        addVoyageRoundRect(path, width, height, tl, tr, br, bl)
        return path
    }
    val waveLength = perimeter / waveCount
    val sampleStep = maxOf(waveLength / 8f, perimeter / 360f, 1f)

    var phaseDistance = 0f
    var hasStarted = false
    var firstSample: VoyageWaveSample? = null
    var previousSample: VoyageWaveSample? = null

    fun emit(
        x: Float,
        y: Float,
        normalX: Float,
        normalY: Float,
        localAmplitude: Float,
        phase: Float,
    ) {
        val displacement = kotlin.math.sin(
            (phase.toDouble() * 2.0 * Math.PI) / period.toDouble(),
        ).toFloat() * localAmplitude
        val sample = VoyageWaveSample(
            x + normalX * displacement,
            y + normalY * displacement,
        )
        val previous = previousSample
        if (!hasStarted || previous == null) {
            path.moveTo(sample.x, sample.y)
            firstSample = sample
            hasStarted = true
        } else {
            path.quadraticTo(
                previous.x,
                previous.y,
                (previous.x + sample.x) * 0.5f,
                (previous.y + sample.y) * 0.5f,
            )
        }
        previousSample = sample
    }

    fun sampleLine(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        normalX: Float,
        normalY: Float,
        localAmplitude: Float,
    ) {
        val dx = endX - startX
        val dy = endY - startY
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        val count = kotlin.math.ceil(length / sampleStep).toInt()
        val firstIndex = if (hasStarted) 1 else 0
        if (count > 0 && firstIndex <= count) {
            for (index in firstIndex..count) {
                val t = index.toFloat() / count.toFloat()
                emit(
                    startX + dx * t,
                    startY + dy * t,
                    normalX,
                    normalY,
                    localAmplitude,
                    phaseDistance + length * t,
                )
            }
        }
        phaseDistance += length
    }

    fun sampleArc(
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
    ) {
        val arcLength = quarterPi * radius
        val count = kotlin.math.ceil(arcLength / sampleStep).toInt()
        val firstIndex = if (hasStarted) 1 else 0
        if (count > 0 && firstIndex <= count) {
            for (index in firstIndex..count) {
                val t = index.toFloat() / count.toFloat()
                val angle = startAngle + quarterPi * t
                val normalX = kotlin.math.cos(angle)
                val normalY = kotlin.math.sin(angle)
                emit(
                    centerX + normalX * radius,
                    centerY + normalY * radius,
                    normalX,
                    normalY,
                    amplitude,
                    phaseDistance + arcLength * t,
                )
            }
        }
        phaseDistance += arcLength
    }

    sampleLine(topStartX, topAmplitude, topEndX, topAmplitude, 0f, -1f, topAmplitude)
    sampleArc(width - trRadius, topAmplitude + trRadius, trRadius, -Math.PI.toFloat() / 2f)
    sampleLine(width - trRadius, rightStartY, width - trRadius, rightEndY, 1f, 0f, rightAmplitude)
    sampleArc(width - brRadius, height - brRadius, brRadius, 0f)
    sampleLine(bottomStartX, height - bottomAmplitude, bottomEndX, height - bottomAmplitude, 0f, 1f, bottomAmplitude)
    sampleArc(leftAmplitude + blRadius, height - blRadius, blRadius, Math.PI.toFloat() / 2f)
    sampleLine(leftAmplitude, leftStartY, leftAmplitude, leftEndY, -1f, 0f, leftAmplitude)
    sampleArc(leftAmplitude + tlRadius, topAmplitude + tlRadius, tlRadius, Math.PI.toFloat())

    val previous = previousSample
    val first = firstSample
    if (previous != null && first != null) {
        path.quadraticTo(
            previous.x,
            previous.y,
            (previous.x + first.x) * 0.5f,
            (previous.y + first.y) * 0.5f,
        )
        path.close()
    }
    return path
}

private class VoyagePathShape(
    private val style: Int,
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
) : Shape {
    private data class OutlineKey(
        val style: Int,
        val width: Float, val height: Float, val density: Float, val direction: LayoutDirection,
        val topStart: Float, val topEnd: Float, val bottomEnd: Float, val bottomStart: Float,
    )

    // Share generated outlines across all instances. During radius adjustment many
    // rows use the same dimensions and radii; a per-instance cache rebuilt on every
    // animated value causes redundant Path allocation and frame drops.
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // Keep the cache key at the exact Float radius. Quantizing Wavy
        // radii to 0.5dp made the outline jump in visible half-dp steps while
        // the user dragged the continuous corner-scale slider. The bounded LRU
        // still limits retained outlines; exact keys preserve continuous geometry.
        fun cacheRadius(value: Float): Float = value

        val key = OutlineKey(
            style, size.width, size.height, density.density, layoutDirection,
            cacheRadius(topStart.value), cacheRadius(topEnd.value),
            cacheRadius(bottomEnd.value), cacheRadius(bottomStart.value),
        )

        synchronized(outlineCache) {
            outlineCache[key]?.let { return it }
        }
        // Build outside the shared lock: complex Wavy outlines must not block
        // other shape lookups while the radius slider is being dragged.
        val generated = createVoyageOutline(size, layoutDirection, density)
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

    private fun createVoyageOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val ts = with(density) { topStart.toPx() }.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val te = with(density) { topEnd.toPx() }.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val be = with(density) { bottomEnd.toPx() }.coerceIn(0f, minOf(size.width, size.height) / 2f)
        val bs = with(density) { bottomStart.toPx() }.coerceIn(0f, minOf(size.width, size.height) / 2f)
        // Voyage vk7/yge0 scales ConcaveArc and Notch corner radii to 0.65f;
        // apply that source scale before building either custom outline.
        val depthScale = if (style == 5 || style == 6) 0.5f else 1f
        val tl = (if (layoutDirection == LayoutDirection.Ltr) ts else te) * depthScale
        val tr = (if (layoutDirection == LayoutDirection.Ltr) te else ts) * depthScale
        val br = (if (layoutDirection == LayoutDirection.Ltr) be else bs) * depthScale
        val bl = (if (layoutDirection == LayoutDirection.Ltr) bs else be) * depthScale
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
                // Direct ban.I / ban.l path: cubic-to-arc-to-cubic at every corner.
                path.addPath(createVoyageG2Path(w, h, tl, tr, br, bl))
            }
            4 -> {
                // Direct wgo0 perimeter sampling with the original 12dp period,
                // 0.9dp amplitude, OR edge activation, and shared phase.
                path.addPath(createVoyageWavyPath(w, h, tl, tr, br, bl, density))
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
 * Direct port of Voyage's zee0.m147825O shape rule:
 * 16dp outer / 4dp inner corners, animated with dampingRatio=0.4 and stiffness=200.
 * vertical=true joins rows top-to-bottom; false joins selection buttons left-to-right.
 */
@Composable
fun voyageSegmentedShape(index: Int, count: Int, vertical: Boolean = true, pressed: Boolean = false): Shape {
    val single = count == 1 || index == -1
    val first = single || index == 0
    val last = single || index == count - 1
    // The setting exposes a 0.0..2.0 scale. Preserve the full range so values
    // above 1.0 enlarge corner geometry instead of silently behaving like 1.0.
    val cornerScale = LocalMaterialCornerScale.current.coerceIn(0f, 2f)
    // Outer items morph into the middle item's 4dp geometry. Middle items
    // contract slightly. Both targets remain linked to the live theme scale.
    val pressedRadius = when {
        !pressed -> null
        count > 1 && (first || last) -> 4.dp
        else -> 7.dp
    }

    val topStart by animateDpAsState(
        targetValue = (pressedRadius ?: if (first) 16.dp else 4.dp) * cornerScale,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "voyageSegmentTopStart",
    )
    val topEnd by animateDpAsState(
        targetValue = (pressedRadius ?: if (if (vertical) first else last) 16.dp else 4.dp) * cornerScale,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "voyageSegmentTopEnd",
    )
    val bottomEnd by animateDpAsState(
        targetValue = (pressedRadius ?: if (last) 16.dp else 4.dp) * cornerScale,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "voyageSegmentBottomEnd",
    )
    val bottomStart by animateDpAsState(
        targetValue = (pressedRadius ?: if (if (vertical) last else first) 16.dp else 4.dp) * cornerScale,
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
private fun defaultSingleSegmentedShape(index: Int, count: Int): ListItemShapes {
    // Material 3 resolves shape independently for default, pressed and selected
    // states. Apply Aloha's live theme shape to all three slots; otherwise the
    // row silently falls back to Material 3 defaults as soon as it is pressed
    // or selected, even though the normal state follows Voyage/Aloha settings.
    val themeShape = voyageSegmentedShape(index, count, vertical = true)
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
        LocalSegmentedShapeConfig provides SegmentedShapeConfig(index, count),
    ) {
        content()
    }
}

@Composable
private fun ListItemShapes.animatedAlohaShapes(
    config: SegmentedShapeConfig?,
    pressed: Boolean,
): Pair<ListItemShapes, Shape> {
    val animatedShape = config?.let { voyageSegmentedShape(it.index, it.count, it.vertical, pressed) } ?: shape
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
    val isPressed by resolvedInteractionSource.collectIsPressedAsState()
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
    val isPressed by resolvedInteractionSource.collectIsPressedAsState()
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
    val isPressed by resolvedInteractionSource.collectIsPressedAsState()
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
    // Voyage k9t0.m108317Il / vfw0.m137946o renders a framed setting row,
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
