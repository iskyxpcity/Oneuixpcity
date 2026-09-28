package io.github.soclear.oneuix.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
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
            // Keep OneuiXP Material sliders visually consistent: use a smooth
            // continuous track without Material's discrete tick marks.
            // Callers still normalize/round values in onValueChangeFinished.
            steps = 0,
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
