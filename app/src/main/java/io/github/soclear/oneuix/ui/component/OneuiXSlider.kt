package io.github.soclear.oneuix.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Slider as MaterialSlider
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider

/** Preserve OneUIX slider values/events while drawing with the active OneuiXP theme. */
@Composable
fun OneuiXSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = valueRange,
            // OneuiXP Material slider style: keep the track visually smooth.
            // Explicitly hide Material3 tick marks as well as using a continuous
            // visual track, while callers keep their own value normalization.
            steps = 0,
            colors = SliderDefaults.colors(
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent,
            ),
            onValueChangeFinished = onValueChangeFinished,
        )

        UiMode.Miuix -> MiuixSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.padding(bottom = 10.dp),
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}
