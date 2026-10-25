package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import com.alohaoneui.app.ui.theme.LocalMaterialSwitchStyle
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSwitches

/**
 * Material 3 switch styles in Voyage's persisted enum order:
 * Material You, Compose, Pixel, Fluent, Cupertino, Liquid Glass, HyperOS, One UI.
 * Miuix screens use their own switch component and never read this style.
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = expressiveSwitchColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    showThumbIcon: Boolean = true,
) {
    val style = LocalMaterialSwitchStyle.current.coerceIn(0, 7)
    if (style == 0) {
        val switchModifier = if (LocalMaterialShadowSwitches.current) modifier.shadow(1.dp, CircleShape) else modifier
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = switchModifier,
            thumbContent = thumbContent ?: if (showThumbIcon) {
                {
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else null,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
        return
    }

    val scheme = MaterialTheme.colorScheme
    val (trackWidth, trackHeight, thumbSize) = when (style) {
        1 -> Triple(48.dp, 28.dp, 22.dp)  // Compose
        2 -> Triple(52.dp, 32.dp, 24.dp)  // Pixel
        3 -> Triple(40.dp, 20.dp, 12.dp)  // Fluent
        4 -> Triple(50.dp, 30.dp, 26.dp)  // Cupertino
        5 -> Triple(52.dp, 32.dp, 24.dp)  // Liquid Glass
        6 -> Triple(52.dp, 30.dp, 24.dp)  // HyperOS
        else -> Triple(48.dp, 28.dp, 22.dp) // One UI
    }
    val trackShape = when (style) {
        3 -> RoundedCornerShape(5.dp)
        7 -> RoundedCornerShape(10.dp)
        else -> RoundedCornerShape(percent = 50)
    }
    val targetTrackColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.12f)
        style == 4 && checked -> Color(0xFF34C759)
        style == 5 && checked -> scheme.primary.copy(alpha = 0.58f)
        checked -> scheme.primary
        style == 5 -> scheme.surfaceContainerHighest.copy(alpha = 0.72f)
        style == 3 -> scheme.surface
        else -> scheme.surfaceContainerHighest
    }
    val trackColor by animateColorAsState(targetTrackColor, label = "voyage_switch_track")
    val targetThumbColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.38f)
        style == 5 -> scheme.surface.copy(alpha = 0.96f)
        checked && style == 7 -> scheme.onPrimary
        else -> scheme.surface
    }
    val thumbColor by animateColorAsState(targetThumbColor, label = "voyage_switch_thumb")
    val inset = (trackHeight - thumbSize) / 2
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - inset else inset,
        label = "voyage_switch_position",
    )
    val outlineColor = when {
        style == 3 && !checked -> scheme.outline
        style == 7 && !checked -> scheme.outlineVariant
        style == 5 -> scheme.outline.copy(alpha = 0.55f)
        !checked -> scheme.outlineVariant
        else -> scheme.primary
    }
    val outlineWidth = if (style == 3 || style == 5 || style == 7) 1.dp else 0.dp
    val effectiveEnabled = enabled && onCheckedChange != null
    val switchModifier = modifier
        .size(trackWidth, trackHeight)
        .clip(trackShape)
        .background(trackColor, trackShape)
        .border(outlineWidth, outlineColor, trackShape)
        .then(
            if (LocalMaterialShadowSwitches.current && style in setOf(2, 4, 5, 6, 7)) {
                Modifier.shadow(if (style == 5) 3.dp else 1.dp, trackShape)
            } else Modifier
        )
        .toggleable(
            value = checked,
            enabled = effectiveEnabled,
            role = Role.Switch,
            interactionSource = interactionSource,
            indication = null,
            onValueChange = { onCheckedChange?.invoke(it) },
        )

    Box(
        modifier = switchModifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .then(
                    if (LocalMaterialShadowSwitches.current && style in setOf(2, 4, 5, 6, 7)) {
                        Modifier.shadow(if (style == 5) 3.dp else 1.dp, CircleShape)
                    } else Modifier
                )
                .background(thumbColor, if (style == 3) RoundedCornerShape(4.dp) else CircleShape)
                .then(
                    if (style == 5) Modifier.border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                thumbContent != null -> thumbContent()
                showThumbIcon && style == 2 && checked -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = scheme.primary,
                )
            }
        }
    }
}

@Composable
fun expressiveSwitchColors(
    checkedIconColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedIconColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledCheckedThumbColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
    disabledCheckedTrackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledCheckedIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledUncheckedThumbColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
    disabledUncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.12f),
    disabledUncheckedBorderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledUncheckedIconColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
): SwitchColors = SwitchDefaults.colors(
    checkedIconColor = checkedIconColor,
    uncheckedIconColor = uncheckedIconColor,
    disabledCheckedThumbColor = disabledCheckedThumbColor,
    disabledCheckedTrackColor = disabledCheckedTrackColor,
    disabledCheckedIconColor = disabledCheckedIconColor,
    disabledUncheckedThumbColor = disabledUncheckedThumbColor,
    disabledUncheckedTrackColor = disabledUncheckedTrackColor,
    disabledUncheckedBorderColor = disabledUncheckedBorderColor,
    disabledUncheckedIconColor = disabledUncheckedIconColor,
)
