package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.launch

val LocalListItemShapes = compositionLocalOf<ListItemShapes?> { null }

private class VoyagePathShape(
    private val style: Int,
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
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
                // Voyage e7g0 / b2t0: SquircleShape with the source preset smoothing=48.
                // The source converts smoothing to the corner control-point factor:
                // 1 - ((48 / 100) * 0.45 + 0.55) = 0.234.
                val smooth = 1f - ((48f / 100f) * 0.45f + 0.55f)
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
            4 -> {
                // Voyage source: p000/wgo0.java, mo5154ll + m140359lI/m140360lO/
                // m140361lo. Keep its density scaling, radius shrink, phase length,
                // adaptive sampling and quadratic sample joins.
                val minDimension = minOf(w, h)
                if (w <= 0f || h <= 0f || minDimension <= 0f) {
                    path.addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h))
                } else {
                    val scale = (minDimension / (48f * density.density)).coerceIn(0.42f, 1f)
                    val period = (12f * density.density * scale).coerceAtLeast(1f)
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
                        val waveCount = kotlin.math.ceil(perimeterLength / period).toInt().coerceAtLeast(1)
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
                            for (i in 0..count) {
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
                            if (radius <= 0f) return
                            val arcLength = kotlin.math.PI.toFloat() * radius / 2f
                            val count = kotlin.math.ceil(arcLength / sampleStep).toInt().coerceAtLeast(1)
                            for (i in 0..count) {
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
                        addArc(w - rtr, aTL + rtr, rtr, -90f, 90f)
                        addLine(xRight, yRightStart, xRight, yRightEnd, 1f, 0f, rightLength, aTR)
                        addArc(w - rbr, h - rbr, rbr, 0f, 90f)
                        addLine(xBottomStart, yBottom, xBottomEnd, yBottom, 0f, 1f, bottomLength, aBR)
                        addArc(aBL + rbl, h - rbl, rbl, 90f, 90f)
                        addLine(aBL, yLeftStart, aBL, yLeftEnd, -1f, 0f, leftLength, aBL)
                        addArc(aBL + rtl, aTL + rtl, rtl, 180f, 90f)

                        val displaced = samples.map { sample ->
                            val wave = sample.localAmplitude * kotlin.math.sin(
                                sample.phaseDistance * 2f * Math.PI.toFloat() / period
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
                            val last = displaced.last()
                            val first = displaced.first()
                            path.quadraticTo(
                                last.x, last.y,
                                (last.x + first.x) / 2f,
                                (last.y + first.y) / 2f
                            )
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
    val scaledRadius = radius * LocalMaterialCornerScale.current.coerceIn(0f, 1f)
    val style = LocalMaterialShapeStyle.current
    return when (style) {
        1 -> CutCornerShape(scaledRadius)
        2, 3, 4, 5, 6 -> VoyagePathShape(style, scaledRadius, scaledRadius, scaledRadius, scaledRadius)
        else -> RoundedCornerShape(scaledRadius)
    }
}

@Composable
fun alohaOneUISegmentedColors(): ListItemColors = ListItemDefaults.segmentedColors().copy(
    containerColor = colorScheme.surfaceColorAtElevation(1.dp),
    disabledContainerColor = colorScheme.surfaceColorAtElevation(1.dp),
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
    // Voyage vk7.m138245I only scales corner geometry when the factor is below 1.0;
    // factors at or above 1.0 keep the original shape rather than enlarging its corners.
    val cornerScale = LocalMaterialCornerScale.current.coerceIn(0f, 1f)

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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) {
                Modifier.shadow(1.dp, itemShapes.shape)
            } else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, itemShapes.shape)
            } ?: Modifier
        )
    SegmentedListItem(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
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
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) {
                Modifier.shadow(1.dp, itemShapes.shape)
            } else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, itemShapes.shape)
            } ?: Modifier
        )
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = itemShapes,
        modifier = styledModifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
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
    // Selected/radio rows must inherit the same container outline and shadow settings
    // as the default and checkbox overloads; otherwise theme settings are skipped here.
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowContainers.current) {
                Modifier.shadow(1.dp, itemShapes.shape)
            } else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, colorScheme.outlineVariant, itemShapes.shape)
            } ?: Modifier
        )
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = itemShapes,
        modifier = styledModifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
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
                    onCheckedChange = if (onRowClick != null) {
                        { value ->
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onCheckedChange(value)
                        }
                    } else {
                        null
                    },
                    interactionSource = switchInteractionSource,
                )
            }
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
    val haptic = LocalHapticFeedback.current
    val safeIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant,
        )
        summary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEachIndexed { index, label ->
                FilterChip(
                    selected = index == safeIndex,
                    onClick = {
                        if (enabled && index != safeIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onItemSelected(index)
                        }
                    },
                    enabled = enabled,
                    label = { Text(label) },
                    leadingIcon = if (index == safeIndex) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
    }
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
                    DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
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
