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
    content: @Composable RowScope.() -> Unit,
) {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressExpansion = rememberMaterialExpressivePressExpansion(actualInteractionSource, enabled)
    // Drive corner interpolation from the same animated expansion value used
    // to push neighboring buttons apart. Selected buttons rest at the maximum
    // theme-linked corner; unselected buttons smoothly fill toward it while pressed.
    val expansionProgress = (pressExpansion / 30.dp).coerceIn(0f, 1f)
    val restingCorner = if (checked) 40.dp else 4.dp
    val animatedCorner = restingCorner + (40.dp - restingCorner) * expansionProgress
    val animatedShape = alohaCornerShape(animatedCorner)
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
