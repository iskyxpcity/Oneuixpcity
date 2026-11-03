package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import kotlin.math.roundToInt

/**
 * Samsung SESL SwitchCompat reference geometry:
 * 43 dp measured control, 37 x 18.5 dp OFF track, 35 x 18.5 dp ON track,
 * and a 22 dp handle with a 1 dp state-specific stroke.
 *
 * Reference: OneUIProject/oneui-core, appcompat SESL switch drawables/dimens.
 * The active tint and stroke are mapped to Aloha's semantic primary role.
 */
@Composable
internal fun OneUiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor by animateColorAsState(
        targetValue = if (checked) scheme.primary else scheme.surfaceContainerHigh,
        label = "oneUiSwitchTrack",
    )
    val strokeColor by animateColorAsState(
        targetValue = if (checked) scheme.primary else scheme.outline,
        label = "oneUiSwitchThumbStroke",
    )
    val dragState = rememberAlohaSwitchDragState(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        trackWidth = 43.dp,
        thumbSize = 22.dp,
        inset = 3.dp,
    )
    Box(
        modifier = modifier
            .size(width = 43.dp, height = 22.dp)
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
                .align(Alignment.Center)
                .size(width = if (checked) 35.dp else 37.dp, height = 18.5.dp)
                .clip(RoundedCornerShape(9.25.dp))
                .background(if (enabled) trackColor else trackColor.copy(alpha = 0.4f)),
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (if (dragState.isRtl) -dragState.positionPx else dragState.positionPx).roundToInt(),
                        0,
                    )
                }
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFFFAFAFA).copy(alpha = if (enabled) 1f else 0.4f))
                .border(
                    width = 1.dp,
                    color = if (enabled) strokeColor else strokeColor.copy(alpha = 0.4f),
                    shape = CircleShape,
                ),
        )
    }
}
