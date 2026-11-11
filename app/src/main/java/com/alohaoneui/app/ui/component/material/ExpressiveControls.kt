package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers

/**
 * The group container is the single source of truth for horizontal press expansion.
 * Standalone buttons receive zero and therefore never change their measured width.
 */
private val MaterialButtonGroupExpansionPerSide = 12.dp
internal val LocalMaterialButtonGroupExpansionPerSide = compositionLocalOf { 0.dp }

/**
 * Project-wide Material 3 Expressive button-group container.
 * All group members read the same per-side expansion limit from this scope.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveButtonGroup(
    modifier: Modifier = Modifier.fillMaxWidth(),
    expandedRatio: Float = 0.30f,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(
        ButtonGroupDefaults.ConnectedSpaceBetween,
        alignment = Alignment.CenterHorizontally,
    ),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    overflowIndicator: @Composable (ButtonGroupMenuState) -> Unit = { menuState ->
        ExpressiveIconButton(
            onClick = {
                if (menuState.isShowing) menuState.dismiss() else menuState.show()
            },
        ) {
            Icon(Icons.Default.MoreHoriz, contentDescription = "More options")
        }
    },
    content: ButtonGroupScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalMaterialButtonGroupExpansionPerSide provides MaterialButtonGroupExpansionPerSide,
    ) {
        ButtonGroup(
            overflowIndicator = overflowIndicator,
            modifier = modifier,
            expandedRatio = expandedRatio,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content,
        )
    }
}

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
    interactionSource: MutableInteractionSource? = null,
    pressMotionEnabled: Boolean = true,
    shapeIndex: Int = -1,
    shapeCount: Int = 1,
    content: @Composable RowScope.() -> Unit,
) {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressExpansion = rememberMaterialExpressivePressExpansion(actualInteractionSource, enabled)
    val shapeProgress = if (pressMotionEnabled) {
        (pressExpansion.value / 16f).coerceIn(0f, 1f)
    } else 0f
    val actualShape = shape ?: alohaSegmentedShape(
        index = shapeIndex,
        count = shapeCount,
        vertical = false,
        pressProgress = shapeProgress,
    )
    val hasShadow = LocalMaterialShadowButtons.current
    val haptic = LocalHapticFeedback.current
    val outlineWidth = LocalMaterialOutlineWidth.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onClick() },
        modifier = modifier,
        enabled = enabled,
        interactionSource = actualInteractionSource,
        shape = actualShape,
        colors = colors,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (hasShadow) 2.dp else 0.dp,
            pressedElevation = if (hasShadow) 3.dp else 0.dp,
            focusedElevation = if (hasShadow) 2.dp else 0.dp,
            hoveredElevation = if (hasShadow) 2.dp else 0.dp,
            disabledElevation = 0.dp,
        ),
        border = outlineWidth.takeIf { it > 0f }?.let { BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant) },
        content = {
            val groupExpansionPerSide = LocalMaterialButtonGroupExpansionPerSide.current
            val groupExpansion = if (pressMotionEnabled) groupExpansionPerSide * shapeProgress else 0.dp
            Spacer(Modifier.width(groupExpansion))
            content()
            Spacer(Modifier.width(groupExpansion))
        },
    )
}

@Composable
fun ExpressiveTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    pressMotionEnabled: Boolean = true,
    shapeIndex: Int = -1,
    shapeCount: Int = 1,
    content: @Composable RowScope.() -> Unit,
) {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressExpansion = rememberMaterialExpressivePressExpansion(actualInteractionSource, enabled)
    val shapeProgress = if (pressMotionEnabled) {
        (pressExpansion.value / 16f).coerceIn(0f, 1f)
    } else 0f
    val actualShape = shape ?: alohaSegmentedShape(
        index = shapeIndex,
        count = shapeCount,
        vertical = false,
        pressProgress = shapeProgress,
    )
    val outlineWidth = LocalMaterialOutlineWidth.current
    val hasShadow = LocalMaterialShadowButtons.current
    val haptic = LocalHapticFeedback.current
    val shadowElevation by animateDpAsState(if (hasShadow && pressExpansion.value > 0.1f) 3.dp else if (hasShadow) 2.dp else 0.dp, label = "textButtonShadow")
    val styledModifier = modifier
        .then(if (hasShadow) Modifier.shadow(shadowElevation, actualShape) else Modifier)
        .then(
            outlineWidth.takeIf { it > 0f }?.let {
                Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, actualShape)
            } ?: Modifier
        )
        .clip(actualShape)
    TextButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onClick() },
        modifier = styledModifier,
        enabled = enabled,
        shape = actualShape,
        colors = colors,
        interactionSource = actualInteractionSource,
        content = {
            val groupExpansionPerSide = LocalMaterialButtonGroupExpansionPerSide.current
            val groupExpansion = if (pressMotionEnabled) groupExpansionPerSide * shapeProgress else 0.dp
            Spacer(Modifier.width(groupExpansion))
            content()
            Spacer(Modifier.width(groupExpansion))
        },
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
    pressMotionEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressExpansion = rememberMaterialExpressivePressExpansion(actualInteractionSource, enabled)
    val shapeProgress = (pressExpansion.value / 16f).coerceIn(0f, 1f)
    val actualShape = shape ?: alohaSegmentedShape(
        index = -1,
        count = 1,
        vertical = false,
        pressProgress = shapeProgress,
    )
    val outlineWidth = LocalMaterialOutlineWidth.current
    val hasShadow = LocalMaterialShadowButtons.current
    val haptic = LocalHapticFeedback.current
    val shadowElevation by animateDpAsState(if (hasShadow && pressExpansion.value > 0.1f) 3.dp else if (hasShadow) 2.dp else 0.dp, label = "iconButtonShadow")
    val styledModifier = modifier
        .then(if (hasShadow) Modifier.shadow(shadowElevation, actualShape) else Modifier)
        .then(
            Modifier
        )
        .then(
            if (framed && outlineWidth > 0f) {
                Modifier.border(outlineWidth.dp, MaterialTheme.colorScheme.outlineVariant, actualShape)
            } else Modifier
        )
        .clip(actualShape)
    IconButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onClick() },
        modifier = styledModifier,
        enabled = enabled,
        colors = colors,
        interactionSource = actualInteractionSource,
        content = { content() },
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
    // Keep the field as a real Material 3 outlined input: native focused and
    // unfocused strokes provide the visual separation from the dialog/list surface.
    val styledModifier = modifier.then(
        if (LocalMaterialShadowButtons.current) Modifier.shadow(2.dp, actualShape)
        else Modifier
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
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.primary,
            // Material 3 outlined fields are transparent; let the parent surface show through.
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
    // Use the same native Material 3 outline and container hierarchy for inline editors.
    val styledModifier = modifier.then(
        if (LocalMaterialShadowButtons.current) Modifier.shadow(2.dp, actualShape)
        else Modifier
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
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
