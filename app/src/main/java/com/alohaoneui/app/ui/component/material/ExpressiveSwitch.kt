package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialSwitchStyle
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSwitches

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
    val scheme = MaterialTheme.colorScheme
    val switchShape = when (style) {
        3 -> androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
        4 -> androidx.compose.foundation.shape.CircleShape
        5 -> androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
        6 -> androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
        7 -> androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        else -> voyageCornerShape(50.dp)
    }
    val styleColors = when (style) {
        1 -> SwitchDefaults.colors(checkedThumbColor = scheme.onPrimary, checkedTrackColor = scheme.primary, uncheckedThumbColor = scheme.outline, uncheckedTrackColor = scheme.surfaceContainerHighest, checkedBorderColor = scheme.primary, uncheckedBorderColor = scheme.outlineVariant, checkedIconColor = scheme.primary, uncheckedIconColor = scheme.surfaceContainerHighest)
        2 -> SwitchDefaults.colors(checkedThumbColor = scheme.primary, checkedTrackColor = scheme.primaryContainer, uncheckedThumbColor = scheme.outline, uncheckedTrackColor = scheme.surfaceContainerHighest, checkedBorderColor = scheme.primaryContainer, uncheckedBorderColor = scheme.outlineVariant, checkedIconColor = scheme.onPrimary, uncheckedIconColor = scheme.surfaceContainerHighest)
        3 -> SwitchDefaults.colors(checkedThumbColor = scheme.onSecondary, checkedTrackColor = scheme.secondary, uncheckedThumbColor = scheme.surface, uncheckedTrackColor = scheme.surfaceContainerHighest, checkedBorderColor = scheme.secondary, uncheckedBorderColor = scheme.outline, checkedIconColor = scheme.secondary, uncheckedIconColor = scheme.outline)
        4 -> SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759), uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF8E8E93), checkedBorderColor = Color(0xFF34C759), uncheckedBorderColor = Color(0xFF8E8E93), checkedIconColor = Color(0xFF34C759), uncheckedIconColor = Color(0xFF8E8E93))
        5 -> SwitchDefaults.colors(checkedThumbColor = scheme.onTertiaryContainer, checkedTrackColor = scheme.tertiaryContainer, uncheckedThumbColor = scheme.surface, uncheckedTrackColor = scheme.surfaceVariant, checkedBorderColor = scheme.tertiary, uncheckedBorderColor = scheme.outlineVariant, checkedIconColor = scheme.tertiary, uncheckedIconColor = scheme.outline)
        6 -> SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF1684FC), uncheckedThumbColor = scheme.surface, uncheckedTrackColor = scheme.surfaceContainerHighest, checkedBorderColor = Color(0xFF1684FC), uncheckedBorderColor = scheme.outlineVariant, checkedIconColor = Color(0xFF1684FC), uncheckedIconColor = scheme.outline)
        7 -> SwitchDefaults.colors(checkedThumbColor = scheme.onPrimary, checkedTrackColor = scheme.primary, uncheckedThumbColor = scheme.surface, uncheckedTrackColor = scheme.surfaceContainerHighest, checkedBorderColor = scheme.primary, uncheckedBorderColor = scheme.outlineVariant, checkedIconColor = scheme.primary, uncheckedIconColor = scheme.outline)
        else -> colors
    }
    val styledModifier = modifier
        .then(when (style) { 3 -> Modifier.graphicsLayer { scaleX = 1.04f }; 5 -> Modifier.graphicsLayer { alpha = 0.96f }; else -> Modifier })
        .then(
            if (LocalMaterialShadowSwitches.current) Modifier.shadow(2.dp, switchShape) else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, switchShape)
            } ?: Modifier
        )
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = styledModifier,
        thumbContent = thumbContent ?: if (showThumbIcon && (checked || enabled) && style in setOf(0, 2, 3, 5, 6, 7)) {
            {
                Icon(
                    imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else null,
        enabled = enabled,
        colors = styleColors,
        interactionSource = interactionSource
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
