package io.github.soclear.oneuix.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import androidx.compose.material3.Slider as MaterialSlider
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.SliderDefaults as MiuixSliderDefaults
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Controlled OneuiXP slider. The owner value is the only source of truth for the
 * thumb position; a separate animated mirror can restore a stale value when a
 * Miuix drag-finished callback arrives before the next composition.
 */
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
    val controlledValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    var lastEmitted by remember(valueRange, stepSize) { mutableFloatStateOf(controlledValue) }

    LaunchedEffect(controlledValue, valueRange, stepSize) {
        lastEmitted = controlledValue
    }

    fun emit(raw: Float) {
        if (!enabled) return
        val bounded = raw.coerceIn(valueRange.start, valueRange.endInclusive)
        val step = stepSize
        val snapped = if (step == null || step <= 0f) bounded else (
            ((bounded - valueRange.start) / step).roundToInt() * step + valueRange.start
        ).coerceIn(valueRange.start, valueRange.endInclusive)
        val previous = lastEmitted
        if (snapped == previous) return

        if (hapticOnStep && step != null && step > 0f) {
            val oldIndex = ((previous - valueRange.start) / step).roundToInt()
            val newIndex = ((snapped - valueRange.start) / step).roundToInt()
            repeat(abs(newIndex - oldIndex)) { hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
        }
        lastEmitted = snapped
        onValueChange(snapped)
    }

    val handleValueChangeFinished: () -> Unit = {
        if (enabled) onValueChangeFinished?.invoke()
        Unit
    }
    // Both themes expose the same value range and emitted values. Material keeps a
    // continuous thumb to avoid visible ticks; Miuix needs native discrete steps so its
    // animated controlled value does not fight the integer-snapped owner value.
    val effectiveSteps = if (stepSize != null && stepSize > 0f) 0 else steps
    val miuixSteps = if (stepSize != null && stepSize > 0f) {
        (((valueRange.endInclusive - valueRange.start) / stepSize).roundToInt() - 1).coerceAtLeast(0)
    } else steps

    when (LocalUiMode.current) {
        UiMode.Material -> MaterialSlider(
            value = controlledValue,
            enabled = enabled,
            onValueChange = ::emit,
            modifier = modifier,
            valueRange = valueRange,
            steps = effectiveSteps,
            onValueChangeFinished = handleValueChangeFinished,
        )
        UiMode.Miuix -> MiuixSlider(
            value = controlledValue,
            enabled = enabled,
            onValueChange = ::emit,
            modifier = modifier.padding(bottom = 10.dp),
            valueRange = valueRange,
            steps = miuixSteps,
            hapticEffect = MiuixSliderDefaults.SliderHapticEffect.None,
            showKeyPoints = false,
            onValueChangeFinished = handleValueChangeFinished,
        )
    }
}
