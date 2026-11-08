package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers

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
    val actualShape = shape ?: alohaCornerShape(12.dp)
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
    val actualShape = shape ?: alohaCornerShape(12.dp)
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
    content: @Composable () -> Unit,
) {
    val actualShape = shape ?: alohaCornerShape(40.dp)
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
    placeholder: (@Composable () -> Unit)? = null,
    label: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    textStyle: TextStyle? = null,
) {
    val actualShape = alohaCornerShape(12.dp)
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
        singleLine = singleLine,
        label = label,
        textStyle = textStyle ?: MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        trailingIcon = trailingIcon,
        placeholder = placeholder,
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




/** TextFieldValue overload for shared Aloha Material inline editors. */
@Composable
fun ExpressiveOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val actualShape = alohaCornerShape(12.dp)
    val styledModifier = modifier
        .then(if (LocalMaterialShadowButtons.current) Modifier.shadow(2.dp, actualShape) else Modifier)
        .then(
            LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, actualShape)
            } ?: Modifier
        )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = styledModifier,
        shape = actualShape,
        label = label,
        singleLine = singleLine,
        textStyle = textStyle,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    )
}

/** Shared Material surface; page-level surfaces inherit Aloha shape/elevation/outline settings. */
@Composable
fun ExpressiveSurface(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.contentColorFor(color),
    border: BorderStroke? = null,
    shadowElevation: androidx.compose.ui.unit.Dp? = null,
    tonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val actualShape = shape ?: alohaCornerShape(20.dp)
    val outlineWidth = LocalMaterialOutlineWidth.current
    val actualBorder = border ?: outlineWidth.takeIf { it > 0f }?.let {
        BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    val actualElevation = shadowElevation ?: if (LocalMaterialShadowContainers.current) 2.dp else 0.dp
    if (onClick == null) {
        androidx.compose.material3.Surface(
            modifier = modifier, shape = actualShape, color = color, contentColor = contentColor,
            tonalElevation = tonalElevation, shadowElevation = actualElevation, border = actualBorder,
            content = content,
        )
    } else {
        androidx.compose.material3.Surface(
            onClick = onClick, modifier = modifier, shape = actualShape, color = color,
            contentColor = contentColor, tonalElevation = tonalElevation,
            shadowElevation = actualElevation, border = actualBorder,
            interactionSource = interactionSource, content = content,
        )
    }
}


/** Shared Material top app bar; screens inherit Aloha theme colors unless intentionally overridden. */
@Composable
fun ExpressiveLargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = expressiveTopAppBarColors(),
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    LargeFlexibleTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
    )
}
