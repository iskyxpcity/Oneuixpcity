package io.github.soclear.oneuix.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import androidx.compose.material3.Slider as MaterialSlider
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import kotlin.math.abs
import kotlin.math.roundToInt

/** Preserve OneUIX slider values/events while drawing with the active OneuiXP theme. */
@Composable
fun OneuiXSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    stepSize: Float? = null,
    hapticOnStep: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val hostView = androidx.compose.ui.platform.LocalView.current
    var visualValue by remember(valueRange, stepSize) {
        mutableFloatStateOf(value.coerceIn(valueRange.start, valueRange.endInclusive))
    }
    var dragging by remember(valueRange, stepSize) { mutableStateOf(false) }
    val lastEmitted = remember(valueRange, stepSize) { floatArrayOf(value) }

    LaunchedEffect(value, dragging, valueRange, stepSize) {
        if (!dragging) {
            visualValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
        }
    }

    fun emit(raw: Float) {
        val bounded = raw.coerceIn(valueRange.start, valueRange.endInclusive)
        visualValue = bounded
        dragging = true
        val step = stepSize
        val snapped = if (step == null || step <= 0f) bounded else (
            ((bounded - valueRange.start) / step).roundToInt() * step + valueRange.start
        ).coerceIn(valueRange.start, valueRange.endInclusive)

        val previous = lastEmitted[0]
        if (snapped == previous) return

        if (hapticOnStep && step != null && step > 0f) {
            val previousIndex = ((previous - valueRange.start) / step).roundToInt()
            val currentIndex = ((snapped - valueRange.start) / step).roundToInt()
            repeat(abs(currentIndex - previousIndex)) {
                hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
        lastEmitted[0] = snapped
        onValueChange(snapped)
    }

    val handleValueChange: (Float) -> Unit = ::emit
    val handleValueChangeFinished: () -> Unit = {
        dragging = false
        visualValue = lastEmitted[0]
        onValueChangeFinished?.invoke()
        Unit
    }
    val effectiveSteps = if (stepSize != null && stepSize > 0f) 0 else steps

    when (LocalUiMode.current) {
        UiMode.Material -> MaterialSlider(
            value = visualValue,
            onValueChange = handleValueChange,
            modifier = modifier,
            valueRange = valueRange,
            steps = effectiveSteps,
            onValueChangeFinished = handleValueChangeFinished,
        )

        UiMode.Miuix -> MiuixSlider(
            value = visualValue,
            onValueChange = handleValueChange,
            modifier = modifier.padding(bottom = 10.dp),
            valueRange = valueRange,
            steps = effectiveSteps,
            onValueChangeFinished = handleValueChangeFinished,
        )
    }
}
