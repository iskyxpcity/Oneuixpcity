package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.MaterialTheme

/**
 * One UI compact toggle proportions: 40 x 22 dp pill track and an 18 dp
 * circular handle with a 2 dp inset. The selected color follows Aloha's
 * active theme while the handle remains a neutral surface.
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
    val direction = LocalLayoutDirection.current
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 900f),
        label = "oneUiSwitchThumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) scheme.primary else scheme.surfaceContainerHigh,
        label = "oneUiSwitchTrack",
    )
    val travel = 18.dp
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .clip(CircleShape)
            .background(if (enabled) trackColor else trackColor.copy(alpha = 0.38f))
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
                .offset(x = if (direction == LayoutDirection.Ltr) travel * progress else -travel * progress)
                .size(18.dp)
                .clip(CircleShape)
                .background(scheme.surface.copy(alpha = if (enabled) 1f else 0.55f)),
        )
    }
}
