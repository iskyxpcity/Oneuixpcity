package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.MaterialTheme

/**
 * HyperOS-style switch silhouette based on Xiaomi HyperOS Settings references:
 * 50 x 28 dp track, inset circular white handle and saturated active track.
 * Aloha semantic accent colors replace fixed Xiaomi brand colors.
 */
@Composable
internal fun HyperOsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor by animateColorAsState(
        targetValue = if (checked) scheme.secondary else scheme.surfaceContainerHigh,
        label = "hyperOsSwitchTrack",
    )
    val dragState = rememberAlohaSwitchDragState(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        trackWidth = 50.dp,
        thumbSize = 20.dp,
        inset = 2.5.dp,
    )
    Box(
        modifier = modifier
            .size(width = 50.dp, height = 28.dp)
            .clip(CircleShape)
            .background(if (enabled) trackColor else trackColor.copy(alpha = 0.38f))
            .alohaSwitchSwipeable(dragState, enabled && onCheckedChange != null, interactionSource)
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
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (if (dragState.isRtl) -dragState.positionPx else dragState.positionPx).roundToInt(),
                        0,
                    )
                }
                .size(20.dp)
                .shadow(1.5.dp, CircleShape)
                .clip(CircleShape)
                .background(scheme.surface.copy(alpha = if (enabled) 1f else 0.55f)),
        )
    }
}
