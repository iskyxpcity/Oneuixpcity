package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialSliderStyle
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSliders
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth

/**
 * Four selectable slider component families. Interaction, range handling and
 * discrete snapping are delegated to Material's SliderState; each family's
 * visible track and thumb are drawn here so styling is not limited to the
 * stock Material thumb/track defaults.
 */

/** Shared style inputs for both the settings-row and generic Material sliders.
 * Values in this adapter preserve the current app mapping until each original
 * Voyage branch is fully matched to its decompiled draw path.
 */
internal data class VoyageSliderAppearance(
    val style: Int,
    val activeColor: Color,
    val thumbColor: Color,
    val inactiveColor: Color,
    val containerColor: Color,
    val thumbSize: DpSize,
    val trackHeight: androidx.compose.ui.unit.Dp,
)

@Composable
internal fun voyageSliderAppearance(requestedStyle: Int): VoyageSliderAppearance {
    val style = requestedStyle.coerceIn(0, 3)
    val scheme = MaterialTheme.colorScheme
    // Match the four Voyage preview families rather than applying one generic
    // Material rail to all of them. Track progress, inactive rail and thumb are
    // independent roles so theme changes propagate without making them identical.
    val active = when (style) {
        0 -> scheme.secondary                 // Material You: filled segment
        1 -> scheme.tertiaryContainer        // Fancy progress uses the same color as its inactive rail
        2 -> scheme.tertiary                 // Material 2: thin accent line
        else -> scheme.secondary             // HyperOS: filled thick segment
    }
    val thumb = scheme.primary
    val inactive = when (style) {
        0 -> scheme.tertiaryContainer        // Material You: tinted inactive rail
        1 -> scheme.surfaceContainerHighest // Fancy: theme-linked neutral rail
        2 -> scheme.tertiaryContainer        // Material 2: fine tinted rail
        else -> scheme.tertiaryContainer    // HyperOS: tinted inactive rail
    }
    val container = when (style) {
        0 -> scheme.surfaceContainerLow
        1 -> scheme.surfaceContainerLow
        2 -> scheme.surfaceContainerLow
        else -> scheme.surfaceContainerLow
    }
    val thumbSize = when (style) {
        0 -> DpSize(2.dp, 32.dp)             // Material You vertical marker
        1 -> DpSize(26.dp, 26.dp)            // Fancy 26dp thumb; halo is drawn separately
        2 -> DpSize(20.dp, 20.dp)            // Material 2 circular thumb
        else -> DpSize(0.dp, 0.dp)           // HyperOS has no separate thumb
    }
    val trackHeight = when (style) {
        0 -> 16.dp                           // Material You pill track + marker
        1 -> 38.dp                           // Fancy rail reduced by 2dp; progress remains 36dp
        2 -> 6.dp                            // Material 2 fine rail
        else -> 24.dp                        // HyperOS thick rail
    }
    return VoyageSliderAppearance(
        style = style,
        activeColor = active,
        thumbColor = thumb,
        inactiveColor = inactive,
        containerColor = container,
        thumbSize = thumbSize,
        trackHeight = trackHeight,
    )
}

/** The source families wrap their track differently; keep that outer surface
 * separate from the active/inactive rail so the rail is never painted in the
 * same color as the thumb. HyperOS wraps the 30dp track itself. */
@Composable
internal fun voyageSliderContainerModifier(
    style: Int,
    containerColor: Color,
): Modifier {
    // Fancy has no outer container: its rail itself is the complete visual surface.
    if (style == 1) return Modifier
    val containerShape = voyageCornerShape(20.dp)
    val containerShadow = if (LocalMaterialShadowSliders.current) {
        Modifier.shadow(2.dp, containerShape, clip = false)
    } else Modifier
    return Modifier
        .then(containerShadow)
        .background(containerColor, voyageCornerShape(20.dp))
        .padding(
            horizontal = when (style) { 0 -> 10.dp; 2 -> 10.dp; else -> 6.dp },
            vertical = when (style) { 0 -> 6.dp; 2 -> 6.dp; else -> 4.dp },
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedSliderItem(
    icon: ImageVector? = null,
    title: String? = null,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    titleTrailingContent: (@Composable () -> Unit)? = null,
) {
    val appearance = voyageSliderAppearance(LocalMaterialSliderStyle.current)
    val style = appearance.style
    val active = appearance.activeColor
    val thumb = appearance.thumbColor
    val inactive = appearance.inactiveColor
    // Source check: j5v0.m105238O and z4v0.m147159l both apply
    // Modifier.size(20.dp, 20.dp) to the thumb hit/visual slot.
    // The 16dp and 32dp values belong to separate track containers, not thumbs.
    val thumbSize = appearance.thumbSize
    val trackHeight = appearance.trackHeight
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val sliderState = rememberSliderState(
        value = value,
        steps = steps,
        trackRange = valueRange,
    )
    LaunchedEffect(value, valueRange, steps) {
        sliderState.value = value.coerceIn(valueRange.start, valueRange.endInclusive)
    }

    SegmentedListItem(
        leadingContent = icon?.let { imageVector ->
            {
                Icon(imageVector = imageVector, contentDescription = title)
            }
        },
        headlineContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                if (!title.isNullOrEmpty()) {
                    if (titleTrailingContent == null) {
                        Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            titleTrailingContent()
                        }
                    }
                }
                if (valueLabel.isNotEmpty()) {
                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                key(style) {
                Slider(
                    state = sliderState,
                    onValueChange = { newValue ->
                        sliderState.value = newValue
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(voyageSliderContainerModifier(
                            style = style,
                            containerColor = appearance.containerColor,
                        )),
                    onValueChangeFinished = onValueChangeFinished,
                    interactionSource = interactionSource,
                    thumb = { thumbState ->
                        val valueFraction = if (valueRange.endInclusive > valueRange.start) {
                            ((thumbState.value - valueRange.start) /
                                (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                        } else 0f
                        VoyageSliderThumb(
                            style = style,
                            size = thumbSize,
                            activeColor = thumb,
                            inactiveColor = inactive,
                            shadowEnabled = LocalMaterialShadowSliders.current,
                            interactionSource = interactionSource,
                            haloVisible = isPressed || isDragged,
                            valueFraction = valueFraction,
                        )
                    },
                    track = { state ->
                        VoyageSliderTrack(
                            style = style,
                            state = state,
                            range = valueRange,
                            steps = steps,
                            height = trackHeight,
                            containerColor = appearance.containerColor,
                            thumbWidth = thumbSize.width,
                            activeColor = active,
                            inactiveColor = inactive,
                            shadowEnabled = LocalMaterialShadowSliders.current,
                        )
                    },
                )
                }
            }
        },
    )
}

@Composable
internal fun VoyageSliderThumb(
    style: Int,
    size: DpSize,
    activeColor: Color,
    inactiveColor: Color,
    shadowEnabled: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    haloVisible: Boolean = false,
    valueFraction: Float = 0f,
) {
    // HyperOS uses the thick track itself as its handle surface.
    if (style == 3) return

    val thumbShape = voyageCornerShape(minOf(size.width, size.height) / 2f)
    when (style) {
        0 -> {
            // Voyage Material You preview uses a slim vertical position marker,
            // not the stock circular M3 thumb. The track and tick marks remain
            // visible on both sides of this marker.
            Box(
                Modifier
                    .size(width = size.width, height = size.height)
                    .clip(voyageCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        1 -> {
            // Fancy's thumb has a soft mint halo surrounding the theme-primary
            // shaped core. The selected shape still follows Voyage's shared
            // shape setting and rotates in the source direction while dragging.
            Box(
                // Keep the existing slider slot dimensions so the rail geometry
                // stays unchanged; the 52dp halo can draw beyond this slot.
                modifier = Modifier
                    .size(width = 50.dp, height = 40.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                if (haloVisible) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.24f)),
                    )
                }
                Box(
                    Modifier
                        .size(width = size.width, height = size.height)
                        .then(if (shadowEnabled) Modifier.shadow(4.dp, thumbShape, clip = false) else Modifier)
                        .then(Modifier.rotate(-1080f * valueFraction))
                        .clip(thumbShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        else -> {
            Box(
                Modifier
                    .size(width = size.width, height = size.height)
                    // Material 2's small wavy circular thumb uses the same global
                    // Voyage shape style as the rest of the UI. Rotate the outline
                    // with value progress so the wave reads as a rolling thumb.
                    .then(Modifier.rotate(-720f * valueFraction))
                    .clip(thumbShape)
                    .background(activeColor),
            )
        }
    }
}

@Composable
internal fun VoyageSliderTrack(
    style: Int,
    state: SliderState,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    height: androidx.compose.ui.unit.Dp,
    containerColor: Color,
    thumbWidth: androidx.compose.ui.unit.Dp,
    activeColor: Color,
    inactiveColor: Color,
    shadowEnabled: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val passedTickColor = when (style) {
        0 -> scheme.onSecondary
        1 -> scheme.primary
        2 -> scheme.primary
        else -> scheme.onSecondary
    }
    val futureTickColor = when (style) {
        0 -> scheme.primary
        1 -> scheme.onSurface.copy(alpha = 0.90f)
        2 -> scheme.primary
        else -> scheme.primary
    }
    val fraction = if (range.endInclusive > range.start) {
        ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    } else 0f
    val trackShape = voyageCornerShape(height / 2f)
    val fancyProgressShape = if (style == 1) voyageCornerShape(18.dp) else trackShape
    val fancyExtension = if (style == 1) thumbWidth / 2f + 10.dp else 0.dp
    val trackModifier = Modifier
        .then(
            if (style == 1) {
                Modifier.layout { measurable, constraints ->
                    val extensionPx = fancyExtension.roundToPx()
                    val horizontalExtension = extensionPx * 2
                    val expandedConstraints = constraints.copy(
                        minWidth = constraints.minWidth + horizontalExtension,
                        maxWidth = if (constraints.hasBoundedWidth) {
                            constraints.maxWidth + horizontalExtension
                        } else {
                            Constraints.Infinity
                        },
                    )
                    val placeable = measurable.measure(expandedConstraints)
                    layout(placeable.width - horizontalExtension, placeable.height) {
                        placeable.place(-extensionPx, 0)
                    }
                }
            } else {
                Modifier
            },
        )
        .fillMaxWidth()
        .height(height)
        .then(if (shadowEnabled) Modifier.shadow(2.dp, trackShape, clip = false) else Modifier)
    Canvas(trackModifier) {
        val h = size.height
        val y = h / 2f
        val cap = StrokeCap.Round
        val extension = fancyExtension.toPx()
        val baseTrackWidth = (size.width - extension * 2f).coerceAtLeast(0f)
        val activeWidth = (baseTrackWidth * fraction).coerceIn(0f, baseTrackWidth)

        when (style) {
            0 -> {
                // Material You: two separately rounded rail segments stop on
                // either side of the slim vertical value marker, as in Voyage.
                val markerGap = 4.dp.toPx()
                val leftEnd = (activeWidth - markerGap / 2f).coerceAtLeast(0f)
                val rightStart = (activeWidth + markerGap / 2f).coerceAtMost(size.width)
                if (leftEnd > 0f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset.Zero,
                        size = Size(leftEnd, h),
                        cornerRadius = CornerRadius(h / 2f, h / 2f),
                    )
                }
                if (rightStart < size.width) {
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(rightStart, 0f),
                        size = Size(size.width - rightStart, h),
                        cornerRadius = CornerRadius(h / 2f, h / 2f),
                    )
                }
            }
            1 -> {
                // Fancy: the rail begins 22dp before the thumb-center range.
                // The thumb slot itself is widened independently; its center is
                // not translated when the active segment receives its extra 6dp.
                val fullRailOutline = trackShape.createOutline(
                    size = Size(size.width, h),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                drawVoyageOutline(fullRailOutline, scheme.surfaceContainerHighest)
                val progressWidth = (
                    baseTrackWidth * fraction + thumbWidth.toPx() / 2f +
                        8.dp.toPx() + extension
                    ).coerceIn(0f, size.width)
                val progressHeight = 38.dp.toPx().coerceAtMost(h)
                if (progressWidth > 0f && progressHeight > 0f) {
                    val progressOutline = fancyProgressShape.createOutline(
                        size = Size(progressWidth, progressHeight),
                        layoutDirection = layoutDirection,
                        density = this,
                    )
                    withTransform({
                        translate(top = (h - progressHeight) / 2f)
                    }) {
                        drawVoyageOutline(progressOutline, activeColor)
                    }
                }
            }
            2 -> {
                // Material 2: a thin accent rail and primary circular thumb.
                drawLine(
                    color = inactiveColor,
                    start = Offset.Zero.copy(y = y),
                    end = Offset(size.width, y),
                    strokeWidth = h,
                    cap = cap,
                )
                if (fraction > 0f) {
                    drawLine(
                        color = activeColor,
                        start = Offset.Zero.copy(y = y),
                        end = Offset(activeWidth, y),
                        strokeWidth = h,
                        cap = cap,
                    )
                }
            }
            else -> {
                // HyperOS: thick full-width pill; no separate thumb is rendered.
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset.Zero,
                    size = Size(size.width, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f),
                )
                if (fraction > 0f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset.Zero,
                        size = Size(activeWidth, h),
                        cornerRadius = CornerRadius(h / 2f, h / 2f),
                    )
                }
            }
        }

        // Always draw visible scale marks, even for continuous sliders. For
        // discrete sliders the marks align with the actual selectable stops.
        val tickIntervals = if (steps > 0) steps + 1 else 20
        for (i in 0..tickIntervals) {
                val tickFraction = i.toFloat() / tickIntervals
                val tickX = if (style == 1) {
                    extension + baseTrackWidth * tickFraction
                } else {
                    size.width * tickFraction
                }
                val passed = tickFraction <= fraction
                val color = when (style) {
                    0 -> if (passed) scheme.onSecondary else scheme.primary
                    1 -> if (passed) scheme.primary else scheme.onSurface.copy(alpha = 0.90f)
                    2 -> scheme.primary
                    else -> if (passed) scheme.onSecondary else scheme.primary
                }
                drawCircle(
                    color = color,
                    radius = when (style) {
                        0 -> 1.1.dp.toPx()
                        1 -> 1.dp.toPx()
                        2 -> 0.9.dp.toPx()
                        else -> 1.dp.toPx()
                    },
                    center = Offset(tickX, y),
                )
        }
    }
}

/**
 * Draw a Compose Shape outline using the Canvas primitives available in the
 * project's pinned Compose version. Rounded outlines are converted to a Path
 * so asymmetric corners and custom Voyage paths are preserved.
 */
internal fun DrawScope.drawVoyageOutline(
    outline: Outline,
    color: Color,
    style: DrawStyle = Fill,
) {
    when (outline) {
        is Outline.Rectangle -> drawRect(
            color = color,
            topLeft = outline.rect.topLeft,
            size = outline.rect.size,
            style = style,
        )
        is Outline.Rounded -> drawPath(
            path = Path().apply { addRoundRect(outline.roundRect) },
            color = color,
            style = style,
        )
        is Outline.Generic -> drawPath(
            path = outline.path,
            color = color,
            style = style,
        )
    }
}

