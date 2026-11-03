package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalMaterialApi::class)
internal data class AlohaSwitchDragState(
    val state: SwipeableState<Boolean>,
    val startBound: Float,
    val endBound: Float,
    val isRtl: Boolean,
) {
    val positionPx: Float
        get() = state.offset.value.takeIf { it.isFinite() } ?: startBound
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun rememberAlohaSwitchDragState(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean,
    trackWidth: Dp,
    thumbSize: Dp,
    inset: Dp,
): AlohaSwitchDragState {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val startBound = with(density) { inset.toPx() }
    val endBound = with(density) { (trackWidth - thumbSize - inset).toPx() }
    val state = rememberSwipeableState(
        initialValue = checked,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 720f),
    )
    LaunchedEffect(checked) {
        if (state.currentValue != checked) state.animateTo(checked)
    }
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != checked) onCheckedChange?.invoke(state.currentValue)
    }
    return AlohaSwitchDragState(state, startBound, endBound, isRtl)
}

@OptIn(ExperimentalMaterialApi::class)
internal fun Modifier.alohaSwitchSwipeable(
    dragState: AlohaSwitchDragState,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier = swipeable(
    state = dragState.state,
    anchors = mapOf(dragState.startBound to false, dragState.endBound to true),
    thresholds = { _, _ -> FractionalThreshold(0.5f) },
    orientation = Orientation.Horizontal,
    enabled = enabled,
    reverseDirection = dragState.isRtl,
    interactionSource = interactionSource,
    resistance = null,
)
