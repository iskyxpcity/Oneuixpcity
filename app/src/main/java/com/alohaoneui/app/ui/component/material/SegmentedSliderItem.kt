package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val style = LocalMaterialSliderStyle.current.coerceIn(0, 3)
    val scheme = MaterialTheme.colorScheme
    val active = when (style) {
        1 -> scheme.tertiary
        2 -> scheme.secondary
        3 -> scheme.primary
        else -> scheme.primary
    }
    val inactive = when (style) {
        1 -> scheme.tertiaryContainer
        2 -> scheme.surfaceVariant
        3 -> scheme.surfaceContainerHighest
        else -> scheme.secondaryContainer
    }
    // Source check: j5v0.m105238O and z4v0.m147159l both apply
    // Modifier.size(20.dp, 20.dp) to the thumb hit/visual slot.
    // The 16dp and 32dp values belong to separate track containers, not thumbs.
    val thumbSize = DpSize(20.dp, 20.dp)
    val trackHeight = when (style) {
        1 -> 8.dp
        2 -> 4.dp
        3 -> 6.dp
        else -> 4.dp
    }
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
                    modifier = Modifier.fillMaxWidth(),
                    onValueChangeFinished = onValueChangeFinished,
                    interactionSource = interactionSource,
                    thumb = {
                        VoyageSliderThumb(
                            style = style,
                            size = thumbSize,
                            activeColor = active,
                            inactiveColor = inactive,
                            shadowEnabled = LocalMaterialShadowSliders.current,
                            interactionSource = interactionSource,
                        )
                    },
                    track = { state ->
                        VoyageSliderTrack(
                            style = style,
                            state = state,
                            range = valueRange,
                            steps = steps,
                            height = trackHeight,
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
) {
    // Voyage's vhf.m138081l uses a 1dp resting shadow and raises it to 6dp
    // while the interaction source is active. Keep this on the thumb only,
    // never on the whole Slider/touch target.
    val pressed by (interactionSource?.collectIsPressedAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(false) })
    // The thumb is a theme component, not a fixed circle: use the same shared
    // shape factory as settings rows so shape style and corner scale update it.
    val thumbShape = voyageCornerShape(size.minDimension / 2f)
    val thumbModifier = Modifier
        .size(width = size.width, height = size.height)
        .then(
            if (shadowEnabled) {
                Modifier.shadow(
                    elevation = if (pressed) 6.dp else 1.dp,
                    shape = thumbShape,
                    clip = false,
                )
            } else Modifier
        )
        .clip(thumbShape)
        .background(activeColor, thumbShape)
    Box(thumbModifier)
}

@Composable
internal fun VoyageSliderTrack(
    style: Int,
    state: SliderState,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    height: androidx.compose.ui.unit.Dp,
    thumbWidth: androidx.compose.ui.unit.Dp,
    activeColor: Color,
    inactiveColor: Color,
    outlineWidth: androidx.compose.ui.unit.Dp,
    outlineColor: Color,
    shadowEnabled: Boolean = false,
) {
    val passedTickColor = when (style) {
        1 -> MaterialTheme.colorScheme.onTertiary
        2 -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val futureTickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    // Share the app-wide Voyage shape system with the visible track and its
    // shadow. Do not freeze the track as a pill when the user changes shape.
    val trackShape = voyageCornerShape(height / 2f)
    // Keep the track's elevation on the visible track layer, not the full
    // Slider touch target. The same global shadow switch controls thumb + track.
    val trackModifier = Modifier
        .fillMaxWidth()
        .height(height)
        .then(if (shadowEnabled) Modifier.shadow(1.dp, trackShape, clip = false) else Modifier)
        .clip(trackShape)
    Canvas(trackModifier) {
        val h = size.height
        val centerY = h / 2f
        // Keep the visual track endpoints aligned with the actual thumb center.
        // The Material Slider positions the thumb center half a thumb-width from
        // either end of its track slot.
        val inset = thumbWidth.toPx() / 2f
        val left = inset
        val right = (size.width - inset).coerceAtLeast(left)
        val fraction = if (range.endInclusive > range.start) {
            ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        } else 0f
        val activeRight = left + (right - left) * fraction
        val radius = when (style) {
            1 -> h / 2f
            2 -> h * 0.22f
            3 -> h * 0.36f
            else -> h / 2f
        }
        drawRoundRect(
            color = inactiveColor,
            topLeft = Offset(left, centerY - h / 2f),
            size = Size(right - left, h),
            cornerRadius = CornerRadius(radius, radius),
        )
        if (activeRight > left) {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(left, centerY - h / 2f),
                size = Size(activeRight - left, h),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
        // The outline belongs to the visible track, not the Slider's full-height
        // touch target. Drawing it here prevents a large pill-shaped frame around
        // the entire slider row when the global outline width is enabled.
        val outlinePx = outlineWidth.toPx()
        if (outlinePx > 0f) {
            val inset = outlinePx / 2f
            val outlineHeight = (h - outlinePx).coerceAtLeast(0f)
            val outlineWidthPx = (right - left - outlinePx).coerceAtLeast(0f)
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(left + inset, centerY - outlineHeight / 2f),
                size = Size(outlineWidthPx, outlineHeight),
                cornerRadius = CornerRadius(
                    (radius - inset).coerceAtLeast(0f),
                    (radius - inset).coerceAtLeast(0f),
                ),
                style = Stroke(width = outlinePx),
            )
        }
        if (steps > 0) {
            val count = steps + 1
            for (index in 0..count) {
                val x = left + (right - left) * (index.toFloat() / count)
                val passed = index.toFloat() / count <= fraction
                val tickColor = if (passed) passedTickColor else futureTickColor
                val tickRadius = when (style) {
                    1 -> h * 0.16f
                    2 -> h * 0.12f
                    else -> h * 0.14f
                }
                drawCircle(
                    color = tickColor,
                    radius = tickRadius,
                    center = Offset(x, centerY),
                )
            }
        }
    }
}
