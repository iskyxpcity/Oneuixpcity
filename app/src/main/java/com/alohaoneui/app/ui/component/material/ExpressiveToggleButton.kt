package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.animateDpAsState
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
import com.alohaoneui.app.ui.component.material.alohaSegmentedShape
import com.alohaoneui.app.ui.component.material.alohaButtonShadowElevation
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
    val groupExpansionPerSide = LocalMaterialButtonGroupExpansionPerSide.current
    val isGroupMember = groupExpansionPerSide > 0.dp
    val shadowElevationTarget = alohaButtonShadowElevation(isGroupMember = isGroupMember)
    val hasShadow = shadowElevationTarget > 0.dp
    val usesAlohaWaveShadow = animatedShape is WavyShape
    val shadowElevation by animateDpAsState(
        targetValue = shadowElevationTarget,
        label = "toggleButtonShadow",
    )
    ToggleButton(
        checked = checked,
        onCheckedChange = { value -> haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onCheckedChange(value) },
        modifier = if (usesAlohaWaveShadow) modifier.alohaWavyShadow(animatedShape, shadowElevation) else modifier,
        enabled = enabled,
        colors = colors,
        shapes = expressiveShapes,
        border = LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
            BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        elevation = if (usesAlohaWaveShadow) {
            // The custom 1dp Aloha wave shadow is drawn by the modifier above.
            // Keep Material's native elevation at zero to avoid a second shadow.
            ToggleButtonDefaults.elevation(
                defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp,
                hoveredElevation = 0.dp, disabledElevation = 0.dp,
            )
        } else if (isGroupMember) {
            if (hasShadow) ToggleButtonDefaults.elevation(
                defaultElevation = 1.dp, pressedElevation = 1.dp, focusedElevation = 1.dp,
                hoveredElevation = 1.dp, disabledElevation = 0.dp,
            ) else null
        } else ToggleButtonDefaults.elevation(
            defaultElevation = if (hasShadow && !usesAlohaWaveShadow) 1.dp else 0.dp,
            pressedElevation = if (hasShadow && !usesAlohaWaveShadow) 1.dp else 0.dp,
            focusedElevation = if (hasShadow && !usesAlohaWaveShadow) 1.dp else 0.dp,
            hoveredElevation = if (hasShadow && !usesAlohaWaveShadow) 1.dp else 0.dp,
            disabledElevation = 0.dp,
        ),
        interactionSource = actualInteractionSource,
        content = {
            // The group container supplies the one shared expansion limit.
            // Outside ExpressiveButtonGroup the CompositionLocal is 0.dp,
            // so standalone toggles only morph their themed shape.
            // Group membership comes from ExpressiveButtonGroup's scope, not
            // shapeCount: some pages rely on Material's supplied button shapes
            // and intentionally leave shapeIndex/shapeCount at their defaults.
            val groupExpansion = if (pressMotionEnabled) {
                groupExpansionPerSide * shapeProgress
            } else 0.dp
            Spacer(Modifier.width(groupExpansion))
            content()
            Spacer(Modifier.width(groupExpansion))
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
