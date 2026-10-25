package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.alohaoneui.app.ui.theme.LocalMaterialSliderStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * AlohaOneUI Material slider row. It participates in SegmentedColumn shaping just
 * like switches/dropdowns/text fields and keeps the current-value hint above
 * the slider so numeric settings are never blind.
 */
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
                val sliderStyle = LocalMaterialSliderStyle.current
                val sliderColors = when (sliderStyle) {
                    0 -> SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.tertiary, thumbColor = MaterialTheme.colorScheme.primary)
                    1 -> SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.secondary, thumbColor = MaterialTheme.colorScheme.secondary)
                    2 -> SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary, thumbColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant)
                    else -> SliderDefaults.colors()
                }
                Slider(
                    value = value,
                    colors = sliderColors,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                )
            }
        },
    )
}
