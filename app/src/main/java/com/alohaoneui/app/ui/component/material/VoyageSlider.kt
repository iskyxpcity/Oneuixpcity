package com.alohaoneui.app.ui.component.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alohaoneui.app.ui.theme.LocalMaterialSliderStyle

/** Uses the persisted Voyage Material slider style for shared settings controls. */
@Composable
fun VoyageSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = when (LocalMaterialSliderStyle.current.coerceIn(0, 3)) {
        0 -> SliderDefaults.colors()
        1 -> SliderDefaults.colors(
            activeTrackColor = scheme.primary,
            inactiveTrackColor = scheme.secondaryContainer,
            thumbColor = scheme.primary,
            activeTickColor = scheme.onPrimary,
            inactiveTickColor = scheme.onSecondaryContainer,
        )
        2 -> SliderDefaults.colors(
            activeTrackColor = scheme.primary,
            inactiveTrackColor = scheme.surfaceVariant,
            thumbColor = scheme.primary,
            activeTickColor = scheme.onPrimary,
            inactiveTickColor = scheme.onSurfaceVariant,
        )
        else -> SliderDefaults.colors(
            activeTrackColor = scheme.secondary,
            inactiveTrackColor = scheme.surfaceContainerHighest,
            thumbColor = scheme.secondary,
            activeTickColor = scheme.onSecondary,
            inactiveTickColor = scheme.onSurfaceVariant,
        )
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
    )
}
