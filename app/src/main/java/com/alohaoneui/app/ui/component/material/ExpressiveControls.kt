package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons

/**
 * Shared Material button controls. Screen code owns actions and content; this
 * component layer owns shape, outline and elevation behavior from Aloha settings.
 */
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val actualShape = shape ?: voyageCornerShape(12.dp)
    val hasShadow = LocalMaterialShadowButtons.current
    val outlineWidth = LocalMaterialOutlineWidth.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = actualShape,
        colors = colors,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (hasShadow) 2.dp else 0.dp,
            pressedElevation = if (hasShadow) 3.dp else 0.dp,
            focusedElevation = if (hasShadow) 2.dp else 0.dp,
            hoveredElevation = if (hasShadow) 2.dp else 0.dp,
            disabledElevation = 0.dp,
        ),
        border = outlineWidth.takeIf { it > 0f }?.let {
            BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        content = content,
    )
}

@Composable
fun ExpressiveTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val actualShape = shape ?: voyageCornerShape(12.dp)
    val outlineWidth = LocalMaterialOutlineWidth.current
    val styledModifier = modifier
        .then(
            outlineWidth.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, actualShape)
            } ?: Modifier
        )
        .clip(actualShape)
    TextButton(
        onClick = onClick,
        modifier = styledModifier,
        enabled = enabled,
        shape = actualShape,
        colors = colors,
        content = content,
    )
}

@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape? = null,
    framed: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val actualShape = shape ?: voyageCornerShape(40.dp)
    val outlineWidth = LocalMaterialOutlineWidth.current
    val styledModifier = modifier
        .then(
            if (framed && LocalMaterialShadowButtons.current) {
                Modifier.shadow(2.dp, actualShape)
            } else Modifier
        )
        .then(
            if (framed && outlineWidth > 0f) {
                Modifier.border(outlineWidth.dp, MaterialTheme.colorScheme.outlineVariant, actualShape)
            } else Modifier
        )
        .clip(actualShape)
    IconButton(
        onClick = onClick,
        modifier = styledModifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * Numeric Material text field used by theme settings. Its frame and palette
 * follow the same global button-shadow and outline settings as other controls.
 */
@Composable
fun ExpressiveOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle? = null,
) {
    val actualShape = voyageCornerShape(12.dp)
    val styledModifier = modifier
        .then(
            if (LocalMaterialShadowButtons.current) Modifier.shadow(2.dp, actualShape)
            else Modifier
        )
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(
                    it.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    actualShape,
                )
            } ?: Modifier
        )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = styledModifier,
        shape = actualShape,
        singleLine = true,
        textStyle = textStyle ?: MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        keyboardOptions = keyboardOptions,
    )
}
