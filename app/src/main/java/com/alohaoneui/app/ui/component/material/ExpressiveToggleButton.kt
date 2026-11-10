package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons
import com.alohaoneui.app.ui.component.material.alohaSegmentedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Material 3 Expressive toggle button styling adapted from KernelSU v3.3.1.
 * Miuix screens intentionally use their own selection components instead.
 */
@Composable
fun ExpressiveToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = expressiveToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    pressMotionEnabled: Boolean = true,
    shapeIndex: Int = -1,
    shapeCount: Int = 1,
    content: @Composable RowScope.() -> Unit,
) {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressExpansion = rememberMaterialExpressivePressExpansion(actualInteractionSource, enabled)
    // Reuse the exact segmented-corner rule used by detail option rows.
    // It animates the four real corner radii, respects leading/middle/trailing
    // geometry, and follows the user's global Material shape style and scale.
    // A selected option is a single visual pill, regardless of its position
    // in the connected group. Only unselected options retain leading/middle/
    // trailing geometry; their pressed morph follows the shared detail-row rule.
    val animatedShape = if (checked) {
        alohaSegmentedShape(
            index = -1,
            count = 1,
            vertical = false,
            pressed = false,
        )
    } else {
        alohaSegmentedShape(
            index = shapeIndex,
            count = shapeCount,
            vertical = false,
            pressed = pressMotionEnabled && pressExpansion > 0.1.dp,
        )
    }
    val expressiveShapes = shapes.copy(
        shape = animatedShape,
        pressedShape = animatedShape,
        checkedShape = animatedShape,
    )
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        shapes = expressiveShapes,
        border = LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
            BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        elevation = if (LocalMaterialShadowButtons.current) ToggleButtonDefaults.elevation() else null,
        interactionSource = actualInteractionSource,
        content = { Spacer(Modifier.width(if (pressMotionEnabled) pressExpansion else 0.dp)); content(); Spacer(Modifier.width(if (pressMotionEnabled) pressExpansion else 0.dp)) },
    )
}

@Composable
fun expressiveToggleButtonColors(
    checkedContainerColor: Color = MaterialTheme.colorScheme.primary,
    checkedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
): ToggleButtonColors = ToggleButtonDefaults.colors(
    checkedContainerColor = checkedContainerColor,
    checkedContentColor = checkedContentColor,
    containerColor = containerColor,
    contentColor = contentColor,
)
