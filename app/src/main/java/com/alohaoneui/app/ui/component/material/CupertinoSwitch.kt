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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme

/**
 * Cupertino UISwitch silhouette: 51 x 31 dp track, 27 dp circular handle,
 * 2 dp end inset and a softly elevated white/surface handle.
 * Color roles follow Aloha's active scheme rather than fixed iOS accent values.
 */
@Composable
internal fun CupertinoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor by animateColorAsState(
        targetValue = if (checked) scheme.primary else scheme.surfaceContainerHighest,
        label = "cupertinoSwitchTrack",
    )
    val dragState = rememberAlohaSwitchDragState(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        trackWidth = 51.dp,
        thumbSize = 27.dp,
        inset = 2.dp,
    )
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
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
                .size(27.dp)
                .shadow(1.5.dp, CircleShape)
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.55f)),
        )
    }
}
