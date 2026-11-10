package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val haptic = LocalHapticFeedback.current
    val pressExpansion = rememberMaterialExpressivePressExpansion(actualInteractionSource, enabled)
    // Feed the same live expansion progress into the detail-row shape engine.
    // Selected buttons use one unified shape; only unselected buttons use
    // leading / middle / trailing geometry.
    val shapeProgress = if (pressMotionEnabled) {
        (pressExpansion.value / 16f).coerceIn(0f, 1f)
    } else 0f
    val animatedShape = if (checked) {
        alohaSegmentedShape(
            index = -1,
            count = 1,
            vertical = false,
            pressProgress = shapeProgress,
        )
    } else {
        alohaSegmentedShape(
            index = shapeIndex,
            count = shapeCount,
            vertical = false,
            pressProgress = shapeProgress,
        )
    }
    val expressiveShapes = shapes.copy(
        shape = animatedShape,
        pressedShape = animatedShape,
        checkedShape = animatedShape,
    )
    ToggleButton(
        checked = checked,
        onCheckedChange = { value -> haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onCheckedChange(value) },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        shapes = expressiveShapes,
        border = LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
            BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        elevation = if (LocalMaterialShadowButtons.current) {
            ToggleButtonDefaults.elevation(
                defaultElevation = 2.dp,
                pressedElevation = 3.dp,
                focusedElevation = 2.dp,
                hoveredElevation = 2.dp,
                disabledElevation = 0.dp,
            )
        } else null,
        interactionSource = actualInteractionSource,
        content = {
            // Only segmented/group members expand horizontally. Standalone
            // toggles (shapeCount == 1) keep their measured width unchanged.
            val groupExpansion = if (shapeCount > 1 && pressMotionEnabled) {
                (pressExpansion.value * 0.30f).coerceAtLeast(0f)
            } else 0f
            Spacer(Modifier.width(groupExpansion.dp))
            content()
            Spacer(Modifier.width(groupExpansion.dp))
        },
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
