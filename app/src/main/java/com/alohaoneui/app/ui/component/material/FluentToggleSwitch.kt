package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.MaterialTheme
import kotlin.math.roundToInt

/**
 * Fluent 2 Toggle Switch geometry and interaction adapted for Aloha's theme.
 * Design reference: Microsoft Fluent UI Android ToggleSwitch / ToggleSwitchTokens.
 * Source project license: MIT, Copyright (c) Microsoft Corporation.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun FluentToggleSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val scheme = MaterialTheme.colorScheme

    val trackColor by animateColorAsState(
        targetValue = if (checked) scheme.primary else scheme.surfaceContainerHigh,
        animationSpec = tween(durationMillis = 500),
        label = "fluentSwitchTrack",
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) scheme.onPrimary else scheme.surface,
        animationSpec = tween(durationMillis = 180),
        label = "fluentSwitchKnob",
    )
    val knobSize by animateDpAsState(
        targetValue = if (pressed && enabled) 24.dp else 26.dp,
        animationSpec = tween(durationMillis = 120),
        label = "fluentSwitchKnobSize",
    )
    val trackInset = if (pressed && enabled) 4.dp else 3.dp
    val travelWidth = if (pressed && enabled) 22.dp else 23.dp
    val minBound = with(density) { trackInset.toPx() }
    val maxBound = with(density) { travelWidth.toPx() }

    val swipeState = rememberSwipeableState(
        initialValue = checked,
        animationSpec = tween(durationMillis = 300),
    )
    LaunchedEffect(checked) {
        if (swipeState.currentValue != checked) swipeState.animateTo(checked)
    }
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue != checked) {
            onCheckedChange?.invoke(swipeState.currentValue)
        }
    }

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .swipeable(
                state = swipeState,
                anchors = mapOf(minBound to false, maxBound to true),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                enabled = enabled && onCheckedChange != null,
                reverseDirection = layoutDirection == LayoutDirection.Rtl,
                interactionSource = interactionSource,
                resistance = null,
            )
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val currentOffset = swipeState.offset.value
        val knobOffset = if (currentOffset.isNaN()) minBound else currentOffset
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.roundToInt(), 0) }
                .size(knobSize)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (enabled) knobColor
                    else Color.lerp(knobColor, scheme.surface, 0.38f),
                ),
        )
    }
}
