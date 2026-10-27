package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val inactiveColor: Color,
    val containerColor: Color,
    val thumbSize: DpSize,
    val trackHeight: androidx.compose.ui.unit.Dp,
)

@Composable
internal fun voyageSliderAppearance(requestedStyle: Int): VoyageSliderAppearance {
    val style = requestedStyle.coerceIn(0, 3)
    val scheme = MaterialTheme.colorScheme
    // All four families use the same theme-driven progress/thumb accent; their
    // rail geometry is taken from the corresponding upstream slider family.
    val active = scheme.primary
    val inactive = scheme.surfaceContainerHighest
    val container = when (style) {
        0 -> scheme.tertiaryContainer.copy(alpha = 0.20f) // Material You
        1 -> scheme.secondaryContainer.copy(alpha = 0.30f) // Fancy
        2 -> scheme.tertiaryContainer.copy(alpha = 0.20f) // Material 2
        else -> scheme.secondaryContainer.copy(alpha = 0.30f) // HyperOS
    }
    val thumbSize = when (style) {
        1 -> DpSize(26.dp, 26.dp) // FancySlider source
        else -> DpSize(20.dp, 20.dp) // M3/M2 default thumb slot; HyperOS hides it
    }
    val trackHeight = when (style) {
        1 -> 38.dp // FancyTrack source
        2 -> 4.dp  // M2Slider source
        3 -> 30.dp // HyperOSSlider source
        else -> 4.dp // Material 3 default track
    }
    return VoyageSliderAppearance(
        style = style,
        activeColor = active,
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
    outlineWidth: androidx.compose.ui.unit.Dp = 0.dp,
    outlineColor: Color = Color.Unspecified,
): Modifier {
    val containerShape = voyageCornerShape(20.dp)
    fun Modifier.withOutline(shape: androidx.compose.ui.graphics.Shape): Modifier =
        then(if (outlineWidth > 0.dp) Modifier.border(outlineWidth, outlineColor, shape) else Modifier)
    return when (style) {
        0 -> Modifier
            .padding(vertical = 2.dp)
            .background(containerColor, containerShape)
            .withOutline(containerShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
        1 -> Modifier
            .background(containerColor, voyageCornerShape(50.dp))
            .withOutline(voyageCornerShape(50.dp))
            .padding(horizontal = 6.dp)
        2 -> Modifier
            .background(containerColor, voyageCornerShape(50.dp))
            .withOutline(voyageCornerShape(50.dp))
            .padding(horizontal = 12.dp)
        else -> Modifier
    }
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
) {
    val appearance = voyageSliderAppearance(LocalMaterialSliderStyle.current)
    val style = appearance.style
    val active = appearance.activeColor
    val inactive = appearance.inactiveColor
    // Source check: j5v0.m105238O and z4v0.m147159l both apply
    // Modifier.size(20.dp, 20.dp) to the thumb hit/visual slot.
    // The 16dp and 32dp values belong to separate track containers, not thumbs.
    val thumbSize = appearance.thumbSize
    val trackHeight = appearance.trackHeight
    val interactionSource = remember { MutableInteractionSource() }
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
            ) {
                if (!title.isNullOrEmpty()) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
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
                            outlineWidth = LocalMaterialOutlineWidth.current.dp,
                            outlineColor = MaterialTheme.colorScheme.outlineVariant,
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
                            activeColor = active,
                            inactiveColor = inactive,
                            shadowEnabled = LocalMaterialShadowSliders.current,
                            interactionSource = interactionSource,
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
                            outlineWidth = LocalMaterialOutlineWidth.current.dp,
                            outlineColor = MaterialTheme.colorScheme.outlineVariant,
                            shadowEnabled = LocalMaterialShadowSliders.current,
                        )
                    },
                )
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
    valueFraction: Float = 0f,
) {
    // HyperOSSlider source deliberately supplies an empty thumb; the wide
    // 30dp track itself is the control surface.
    if (style == 3) return

    val effectiveInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by effectiveInteractionSource.collectIsPressedAsState()
    val dragged by effectiveInteractionSource.collectIsDraggedAsState()
    val hovered by effectiveInteractionSource.collectIsHoveredAsState()
    val focused by effectiveInteractionSource.collectIsFocusedAsState()
    val interacting = pressed || dragged || hovered || focused
    val thumbShape = voyageCornerShape(minOf(size.width, size.height) / 2f)

    Box(
        Modifier.size(width = size.width, height = size.height),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        val thumbModifier = Modifier
            .size(width = size.width, height = size.height)
            .then(if (style == 1) Modifier.rotate(1080f * valueFraction) else Modifier)
            .then(
                if (shadowEnabled) {
                    Modifier.shadow(
                        elevation = if (interacting) 6.dp else 1.dp,
                        shape = thumbShape,
                        clip = false,
                    )
                } else Modifier
            )
            .clip(thumbShape)
            .background(activeColor, thumbShape)
        Box(thumbModifier) {}
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
    outlineWidth: androidx.compose.ui.unit.Dp,
    outlineColor: Color,
    shadowEnabled: Boolean = false,
) {
    val passedTickColor = MaterialTheme.colorScheme.onPrimary
    val futureTickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    val fraction = if (range.endInclusive > range.start) {
        ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    } else 0f
    val trackShape = voyageCornerShape(height / 2f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height)
            .then(if (style == 3) Modifier.background(containerColor, trackShape) else Modifier)
            .then(if (style == 3 && outlineWidth > 0.dp) Modifier.border(outlineWidth, outlineColor, trackShape) else Modifier)
            .then(if (shadowEnabled && style == 3) Modifier.shadow(1.dp, trackShape, clip = false) else Modifier)
    ) {
        val h = size.height
        val y = h / 2f
        val end = Offset(size.width, y)
        val start = Offset(0f, y)
        val cap = if (style == 3) StrokeCap.Butt else StrokeCap.Round
        val trackThickness = h

        when (style) {
            0 -> {
                // Material You: stock Material 3 line track (4dp) with round caps.
                drawLine(inactiveColor, start, end, trackThickness, StrokeCap.Round)
                if (fraction > 0f) {
                    drawLine(activeColor, start, Offset(size.width * fraction, y), trackThickness, StrokeCap.Round)
                }
            }
            1 -> {
                // Fancy: source FancyTrack uses a 38dp capsule and extends each
                // segment by the cap radius so the fill reaches the thumb center.
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f),
                )
                val activeWidth = (size.width * fraction + h / 2f).coerceAtMost(size.width + h / 2f)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(-h / 2f, 0f),
                        size = Size(activeWidth, h),
                        cornerRadius = CornerRadius(h / 2f, h / 2f),
                    )
                }
            }
            2 -> {
                // Material 2: source M2Slider uses a 4dp custom track.
                drawLine(inactiveColor, start, end, trackThickness, StrokeCap.Round)
                if (fraction > 0f) {
                    drawLine(activeColor, start, Offset(size.width * fraction, y), trackThickness, StrokeCap.Round)
                }
            }
            else -> {
                // HyperOS: source HyperOSSlider uses a 30dp track with Butt caps
                // and no visible thumb, giving the track its own broad silhouette.
                drawLine(inactiveColor, start, end, trackThickness, cap)
                if (fraction > 0f) {
                    drawLine(activeColor, start, Offset(size.width * fraction, y), trackThickness, cap)
                }
            }
        }

        if (steps > 0) {
            val count = steps + 1
            for (i in 0..count) {
                val tickFraction = i.toFloat() / count
                drawCircle(
                    color = if (tickFraction <= fraction) passedTickColor else futureTickColor,
                    radius = 1.dp.toPx(),
                    center = Offset(size.width * tickFraction, y),
                )
            }
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

