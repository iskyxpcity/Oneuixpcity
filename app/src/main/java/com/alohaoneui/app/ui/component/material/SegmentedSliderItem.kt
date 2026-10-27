package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.Canvas
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
    val thumbSize = when (style) {
        1 -> DpSize(24.dp, 24.dp)
        2 -> DpSize(16.dp, 16.dp)
        3 -> DpSize(20.dp, 20.dp)
        else -> DpSize(20.dp, 20.dp)
    }
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
                        )
                    },
                )
            }
        },
    )
}

@Composable
private fun VoyageSliderThumb(
    style: Int,
    size: DpSize,
    activeColor: Color,
    inactiveColor: Color,
    interactionSource: MutableInteractionSource,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    Box(Modifier.size(width = size.width, height = size.height)) {
        Canvas(Modifier.fillMaxSize()) {
            val d = minOf(size.width.toPx(), size.height.toPx()) * if (pressed) 0.94f else 1f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            when (style) {
                1 -> {
                    drawCircle(activeColor.copy(alpha = 0.18f), radius = d * 0.50f, center = center)
                    drawCircle(activeColor, radius = d * 0.36f, center = center)
                    drawCircle(Color.White, radius = d * 0.12f, center = center)
                }
                2 -> {
                    drawCircle(activeColor, radius = d * 0.34f, center = center)
                    drawCircle(inactiveColor, radius = d * 0.34f, center = center, style = Stroke(width = 1.dp.toPx()))
                }
                3 -> {
                    drawCircle(activeColor, radius = d * 0.46f, center = center)
                    drawCircle(Color.White.copy(alpha = 0.95f), radius = d * 0.20f, center = center)
                }
                else -> {
                    drawCircle(activeColor, radius = d * 0.40f, center = center)
                    drawCircle(activeColor.copy(alpha = 0.20f), radius = d * 0.50f, center = center, style = Stroke(width = 1.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun VoyageSliderTrack(
    style: Int,
    state: SliderState,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    height: androidx.compose.ui.unit.Dp,
    thumbWidth: androidx.compose.ui.unit.Dp,
    activeColor: Color,
    inactiveColor: Color,
) {
    val passedTickColor = when (style) {
        1 -> MaterialTheme.colorScheme.onTertiary
        2 -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val futureTickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    Canvas(Modifier.fillMaxWidth().height(height)) {
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
