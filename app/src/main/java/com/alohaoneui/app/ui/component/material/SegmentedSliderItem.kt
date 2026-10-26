package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSliders

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
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (LocalMaterialShadowSliders.current) Modifier.shadow(2.dp, voyageCornerShape(50.dp))
                            else Modifier
                        )
                        .then(
                            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                                Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, voyageCornerShape(50.dp))
                            } ?: Modifier
                        ),
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                )
            }
        },
    )
}
