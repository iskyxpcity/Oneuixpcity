package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialBorderWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSwitches
import com.alohaoneui.app.ui.theme.LocalMaterialSwitchStyle
import androidx.compose.ui.graphics.Color

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    showThumbIcon: Boolean = true,
) {
    val style = LocalMaterialSwitchStyle.current.coerceIn(0, 7)
    val borderWidth = LocalMaterialBorderWidth.current.coerceIn(0f, 1.5f)
    val scheme = MaterialTheme.colorScheme
    val resolvedColors = colors ?: when (style) {
        0 -> SwitchDefaults.colors(
            uncheckedBorderColor = if (borderWidth > 0f) scheme.outline else Color.Transparent,
        )
        1 -> SwitchDefaults.colors(
            checkedTrackColor = scheme.primary,
            checkedThumbColor = scheme.onPrimary,
            uncheckedTrackColor = scheme.surfaceContainerHighest,
            uncheckedThumbColor = scheme.onSurfaceVariant,
            uncheckedBorderColor = if (borderWidth > 0f) scheme.outline else Color.Transparent,
        )
        2 -> SwitchDefaults.colors(
            checkedTrackColor = Color(0xFF1A73E8),
            checkedThumbColor = Color.White,
            uncheckedTrackColor = scheme.surfaceContainerHighest,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = if (borderWidth > 0f) scheme.outline else Color.Transparent,
        )
        3 -> SwitchDefaults.colors(
            checkedTrackColor = scheme.primary,
            checkedThumbColor = scheme.surface,
            uncheckedTrackColor = scheme.surface,
            uncheckedThumbColor = scheme.onSurfaceVariant,
            uncheckedBorderColor = scheme.outline,
        )
        4 -> SwitchDefaults.colors(
            checkedTrackColor = Color(0xFF34C759),
            checkedThumbColor = Color.White,
            uncheckedTrackColor = scheme.surfaceVariant,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = if (borderWidth > 0f) scheme.outline else Color.Transparent,
        )
        5 -> SwitchDefaults.colors(
            checkedTrackColor = scheme.primary.copy(alpha = 0.78f),
            checkedThumbColor = scheme.surface.copy(alpha = 0.96f),
            uncheckedTrackColor = scheme.surfaceContainerHighest.copy(alpha = 0.78f),
            uncheckedThumbColor = scheme.surface.copy(alpha = 0.96f),
            uncheckedBorderColor = scheme.outlineVariant,
        )
        6 -> SwitchDefaults.colors(
            checkedTrackColor = scheme.primary,
            checkedThumbColor = scheme.onPrimary,
            uncheckedTrackColor = scheme.surfaceContainerHighest,
            uncheckedThumbColor = scheme.onSurfaceVariant,
            uncheckedBorderColor = if (borderWidth > 0f) scheme.outline else Color.Transparent,
        )
        else -> SwitchDefaults.colors(
            checkedTrackColor = scheme.primary,
            checkedThumbColor = scheme.surface,
            uncheckedTrackColor = scheme.surfaceContainerHighest,
            uncheckedThumbColor = scheme.onSurfaceVariant,
            uncheckedBorderColor = if (borderWidth > 0f) scheme.outline else Color.Transparent,
        )
    }
    val iconMode = when (style) {
        2, 6 -> 2
        0, 5, 7 -> 1
        else -> 0
    }
    val resolvedThumb = thumbContent ?: if (
        showThumbIcon && (checked || iconMode == 2) && iconMode != 0
    ) {
        {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    } else null
    val resolvedModifier = if (LocalMaterialShadowSwitches.current) {
        modifier.shadow(1.dp, RoundedCornerShape(50))
    } else modifier
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = resolvedModifier,
        thumbContent = resolvedThumb,
        enabled = enabled,
        colors = resolvedColors,
        interactionSource = interactionSource,
    )
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
