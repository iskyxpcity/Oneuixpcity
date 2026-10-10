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
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val hostView = androidx.compose.ui.platform.LocalView.current
    var visualValue by remember(valueRange, stepSize) {
        mutableFloatStateOf(value.coerceIn(valueRange.start, valueRange.endInclusive))
    }
    var dragging by remember(valueRange, stepSize) { mutableStateOf(false) }
    var lastEmitted by remember(valueRange, stepSize) {
        mutableFloatStateOf(value.coerceIn(valueRange.start, valueRange.endInclusive))
    }

    LaunchedEffect(value, dragging, valueRange, stepSize) {
        if (!dragging) {
            val synced = value.coerceIn(valueRange.start, valueRange.endInclusive)
            visualValue = synced
            lastEmitted = synced
        }
    }

    fun emit(raw: Float) {
        if (!enabled) return
        val bounded = raw.coerceIn(valueRange.start, valueRange.endInclusive)
        val step = stepSize
        val snapped = if (step == null || step <= 0f) bounded else (
            ((bounded - valueRange.start) / step).roundToInt() * step + valueRange.start
        ).coerceIn(valueRange.start, valueRange.endInclusive)
        // Keep the thumb on the same quantized value emitted to the caller.
        // Otherwise Miuix can display a raw value that snaps back on release.
        visualValue = snapped
        dragging = true

        val previous = lastEmitted
        if (snapped == previous) return

        if (hapticOnStep && step != null && step > 0f) {
            val previousIndex = ((previous - valueRange.start) / step).roundToInt()
            val currentIndex = ((snapped - valueRange.start) / step).roundToInt()
            repeat(abs(currentIndex - previousIndex)) {
                hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
        lastEmitted = snapped
        onValueChange(snapped)
    }

    val handleValueChange: (Float) -> Unit = ::emit
    val handleValueChangeFinished: () -> Unit = {
        if (enabled) onValueChangeFinished?.invoke()
        visualValue = lastEmitted
        dragging = false
        Unit
    }
    val effectiveSteps = if (stepSize != null && stepSize > 0f) 0 else steps

    when (LocalUiMode.current) {
        UiMode.Material -> MaterialSlider(
            value = visualValue,
            enabled = enabled,
            onValueChange = handleValueChange,
            modifier = modifier,
            valueRange = valueRange,
            steps = effectiveSteps,
            onValueChangeFinished = handleValueChangeFinished,
        )

        UiMode.Miuix -> MiuixSlider(
            value = visualValue,
            enabled = enabled,
            onValueChange = handleValueChange,
            modifier = modifier.padding(bottom = 10.dp),
            valueRange = valueRange,
            steps = effectiveSteps,
            onValueChangeFinished = handleValueChangeFinished,
        )
    }
}
