package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialSliderStyle

/**
 * Slider row with Voyage's four component families. Each family supplies its own
 * thumb dimensions, track thickness and track treatment while Slider retains
 * the real Material pointer/drag/keyboard semantics and step snapping.
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
    val styleColors = when (style) {
        1 -> SliderDefaults.colors(
            thumbColor = scheme.tertiary,
            activeTrackColor = scheme.tertiary,
            inactiveTrackColor = scheme.tertiaryContainer,
            activeTickColor = scheme.onTertiary,
            inactiveTickColor = scheme.onTertiaryContainer,
        )
        2 -> SliderDefaults.colors(
            thumbColor = scheme.secondary,
            activeTrackColor = scheme.secondary,
            inactiveTrackColor = scheme.surfaceVariant,
            activeTickColor = scheme.onSecondary,
            inactiveTickColor = scheme.onSurfaceVariant,
        )
        3 -> SliderDefaults.colors(
            thumbColor = scheme.primary,
            activeTrackColor = scheme.primary,
            inactiveTrackColor = scheme.surfaceContainerHighest,
            activeTickColor = scheme.onPrimary,
            inactiveTickColor = scheme.outline,
        )
        else -> SliderDefaults.colors(
            thumbColor = scheme.primary,
            activeTrackColor = scheme.primary,
            inactiveTrackColor = scheme.secondaryContainer,
            activeTickColor = scheme.onPrimary,
            inactiveTickColor = scheme.onSecondaryContainer,
        )
    }
    val thumbSize = when (style) {
        1 -> DpSize(24.dp, 24.dp) // Fancy: enlarged thumb
        2 -> DpSize(16.dp, 16.dp) // Material: compact circular thumb
        3 -> DpSize(20.dp, 20.dp) // HyperOS: broad thumb
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
    LaunchedEffect(value) {
        sliderState.value = value
    }

    SegmentedListItem(
        leadingContent = icon?.let { imageVector ->
            {
                Icon(
                    imageVector = imageVector,
                    contentDescription = title,
                )
            }
        },
        headlineContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                if (!title.isNullOrEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
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
                    colors = styleColors,
                    interactionSource = interactionSource,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            colors = styleColors,
                            thumbSize = thumbSize,
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(trackHeight),
                            colors = styleColors,
                            thumbTrackGapSize = when (style) {
                                1 -> 7.dp
                                2 -> 4.dp
                                3 -> 5.dp
                                else -> 4.dp
                            },
                            trackInsideCornerSize = when (style) {
                                1 -> 4.dp
                                2 -> 2.dp
                                3 -> 3.dp
                                else -> 2.dp
                            },
                        )
                    },
                )
            }
        },
    )
}
