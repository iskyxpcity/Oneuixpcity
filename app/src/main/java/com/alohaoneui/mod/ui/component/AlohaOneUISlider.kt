package com.alohaoneui.mod.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.material3.rememberSliderState
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.component.material.VoyageSliderThumb
import com.alohaoneui.app.ui.component.material.VoyageSliderTrack
import com.alohaoneui.app.ui.theme.LocalMaterialSliderStyle
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSliders
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Controlled AlohaOneUI slider. The owner value is the only source of truth for the
 * thumb position; a separate animated mirror can restore a stale value when a
 * Miuix drag-finished callback arrives before the next composition.
 */
@Composable
fun AlohaOneUISlider(
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
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    var lastEmitted by remember(valueRange, stepSize, steps) { mutableFloatStateOf(controlledValue) }

    fun snapValue(raw: Float): Float {
        val bounded = raw.coerceIn(valueRange.start, valueRange.endInclusive)
        val step = stepSize
        if (step != null && step > 0f) {
            return (((bounded - valueRange.start) / step).roundToInt() * step + valueRange.start)
                .coerceIn(valueRange.start, valueRange.endInclusive)
        }
        if (steps > 0) {
            val count = steps + 1
            val interval = (valueRange.endInclusive - valueRange.start) / count
            return (valueRange.start + ((bounded - valueRange.start) / interval).roundToInt() * interval)
                .coerceIn(valueRange.start, valueRange.endInclusive)
        }
        return bounded
    }

    fun emit(raw: Float) {
        if (!enabled) return
        val snapped = snapValue(raw)
        val previous = lastEmitted
        if (snapped == previous) return

        val step = stepSize ?: if (steps > 0) {
            (valueRange.endInclusive - valueRange.start) / (steps + 1)
        } else null
        if (hapticOnStep && step != null && step > 0f) {
            val oldIndex = ((previous - valueRange.start) / step).roundToInt()
            val newIndex = ((snapped - valueRange.start) / step).roundToInt()
            repeat(abs(newIndex - oldIndex)) {
                hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
        lastEmitted = snapped
        latestOnValueChange(snapped)
    }

    val handleValueChangeFinished: () -> Unit = {
        if (enabled) latestOnValueChangeFinished?.invoke()
        Unit
    }
    val effectiveSteps = if (stepSize != null && stepSize > 0f) 0 else steps

    val materialAppearance = com.alohaoneui.app.ui.component.material.voyageSliderAppearance(
        LocalMaterialSliderStyle.current,
    )
    val materialSliderStyle = materialAppearance.style
    val materialScheme = androidx.compose.material3.MaterialTheme.colorScheme
    val materialActiveColor = materialAppearance.activeColor
    val materialInactiveColor = materialAppearance.inactiveColor
    val materialThumbSize = materialAppearance.thumbSize
    val materialTrackHeight = materialAppearance.trackHeight
    val materialInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val materialSliderState = rememberSliderState(
        value = controlledValue,
        steps = effectiveSteps,
        trackRange = valueRange,
    )
    LaunchedEffect(controlledValue, valueRange, effectiveSteps) {
        materialSliderState.value = controlledValue
    }

    when (LocalUiMode.current) {
        UiMode.Material -> MaterialSlider(
            state = materialSliderState,
            enabled = enabled,
            onValueChange = ::emit,
            modifier = modifier.fillMaxWidth(),
            onValueChangeFinished = handleValueChangeFinished,
            interactionSource = materialInteractionSource,
            thumb = {
                VoyageSliderThumb(
                    style = materialSliderStyle,
                    size = materialThumbSize,
                    activeColor = materialActiveColor,
                    inactiveColor = materialInactiveColor,
                    shadowEnabled = LocalMaterialShadowSliders.current,
                    interactionSource = materialInteractionSource,
                )
            },
            track = { state ->
                VoyageSliderTrack(
                    style = materialSliderStyle,
                    state = state,
                    range = valueRange,
                    steps = effectiveSteps,
                    height = materialTrackHeight,
                    thumbWidth = materialThumbSize.width,
                    activeColor = materialActiveColor,
                    inactiveColor = materialInactiveColor,
                    outlineWidth = LocalMaterialOutlineWidth.current.dp,
                    outlineColor = materialScheme.outlineVariant,
                )
            },
        )
        UiMode.Miuix -> {
            val reverse = LocalLayoutDirection.current == LayoutDirection.Rtl
            val palette = MiuixTheme.colorScheme
            val trackBackground = if (enabled) palette.sliderBackground else palette.disabledSecondary
            val trackForeground = if (enabled) palette.primary else palette.disabledPrimarySlider
            val thumbColor = if (enabled) palette.onPrimary else palette.disabledOnPrimary
            var widthPx by remember { mutableIntStateOf(0) }
            var heightPx by remember { mutableIntStateOf(0) }
            var dragOffsetPx by remember { mutableFloatStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }
            // Draw the thumb directly at the pointer-derived position. Miuix's built-in
            // Slider animates every controlled value with a spring, visibly trailing
            // Material during drag and real-time preview.
            var visualValue by remember(valueRange, stepSize, steps) {
                mutableFloatStateOf(controlledValue)
            }

            LaunchedEffect(controlledValue, isDragging, valueRange, stepSize, steps) {
                lastEmitted = snapValue(controlledValue)
                if (!isDragging) visualValue = controlledValue
            }

            fun valueAtPosition(x: Float): Float {
                val thumbRadius = heightPx / 2f
                val availableWidth = (widthPx - 2f * thumbRadius).coerceAtLeast(0f)
                val fraction = if (availableWidth <= 0f) 0f
                    else ((x - thumbRadius) / availableWidth).coerceIn(0f, 1f)
                val effectiveFraction = if (reverse) 1f - fraction else fraction
                return valueRange.start + (valueRange.endInclusive - valueRange.start) * effectiveFraction
            }

            fun updateFromPosition(x: Float) {
                val raw = valueAtPosition(x)
                visualValue = raw
                emit(raw)
            }

            Canvas(
                modifier = modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(CircleShape)
                    .onSizeChanged {
                        widthPx = it.width
                        heightPx = it.height
                    }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        enabled = enabled,
                        state = rememberDraggableState { delta ->
                            dragOffsetPx += delta
                            updateFromPosition(dragOffsetPx)
                        },
                        onDragStarted = { offset ->
                            isDragging = true
                            dragOffsetPx = offset.x
                            updateFromPosition(dragOffsetPx)
                        },
                        onDragStopped = {
                            isDragging = false
                            handleValueChangeFinished()
                        },
                    ),
            ) {
                drawRect(trackBackground)
                if (isDragging && enabled) drawRect(Color.Black, alpha = 0.044f)

                val thumbRadius = size.height / 2f
                val availableWidth = (size.width - 2f * thumbRadius).coerceAtLeast(0f)
                val fraction = ((visualValue - valueRange.start) /
                    (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                val effectiveFraction = if (reverse) 1f - fraction else fraction
                val centerX = thumbRadius + effectiveFraction * availableWidth
                val startX = if (reverse) size.width else 0f
                drawLine(
                    color = trackForeground,
                    start = Offset(startX, size.height / 2f),
                    end = Offset(centerX, size.height / 2f),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius * 0.72f,
                    center = Offset(centerX, size.height / 2f),
                )
            }
        }
    }
}
