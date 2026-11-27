package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers
import com.alohaoneui.app.ui.theme.LocalMaterialShadowHostActive

/**
 * Interpolate an explicitly supplied CornerBasedShape as well as the default
 * Aloha shape. This keeps standalone press morphing active when a screen supplies
 * its own rounded or wave shape.
 */
private class AlohaPressCornerSize(
    private val base: CornerSize,
    private val pressedRadius: androidx.compose.ui.unit.Dp,
    private val progress: Float,
) : CornerSize {
    override fun toPx(
        shapeSize: Size,
        density: Density,
    ): Float {
        val start = base.toPx(shapeSize, density)
        val end = with(density) { pressedRadius.toPx() }
        return start + (end - start) * progress
    }
}

private fun Shape.withAlohaPressProgress(
    progress: Float,
    shapeIndex: Int = -1,
    shapeCount: Int = 1,
): Shape {
    val corners = this as? CornerBasedShape ?: return this
    val fraction = progress.coerceIn(0f, 1f)
    if (fraction <= 0f) return this
    val isOuterSegment = shapeCount > 1 &&
        (shapeIndex == 0 || shapeIndex == shapeCount - 1)
    val pressedRadius = if (isOuterSegment) 4.dp else 8.dp
    return corners.copy(
        topStart = AlohaPressCornerSize(corners.topStart, pressedRadius, fraction),
        topEnd = AlohaPressCornerSize(corners.topEnd, pressedRadius, fraction),
        bottomEnd = AlohaPressCornerSize(corners.bottomEnd, pressedRadius, fraction),
        bottomStart = AlohaPressCornerSize(corners.bottomStart, pressedRadius, fraction),
    )
}

/**
 * The group container is the single source of truth for horizontal press expansion.
 * Standalone buttons receive zero and therefore never change their measured width.
 */
private val MaterialButtonGroupExpansionPerSide = 12.dp

internal val LocalMaterialButtonGroupExpansionPerSide = compositionLocalOf { 0.dp }

/**
 * Single source of truth for every Material button-family shadow.
 * Every independent button keeps its own shadow when the button-shadow
 * preference is enabled, even when it is placed inside a container surface.
 * Button-group members use the same elevation so the group does not silently
 * lose its button shadow merely because its parent already has a container shadow.
 */
@Composable
internal fun alohaButtonShadowElevation(
    isGroupMember: Boolean = false,
    enabled: Boolean = true,
): androidx.compose.ui.unit.Dp =
    if (enabled && LocalMaterialShadowButtons.current) 1.dp else 0.dp

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
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
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
    val groupExpansionPerSide = LocalMaterialButtonGroupExpansionPerSide.current
    val isGroupMember = groupExpansionPerSide > 0.dp
    // Only standalone controls interpolate a caller-supplied shape. Group members
    // keep the exact segmented-shape behavior used by the connected button group.
    val actualShape = if (!isGroupMember) {
        shape?.withAlohaPressProgress(shapeProgress, shapeIndex, shapeCount)
            ?: alohaSegmentedShape(index = shapeIndex, count = shapeCount, vertical = false, pressProgress = shapeProgress)
    } else {
        shape ?: alohaSegmentedShape(index = shapeIndex, count = shapeCount, vertical = false, pressProgress = shapeProgress)
    }
    val shadowElevation = alohaButtonShadowElevation(isGroupMember = isGroupMember)
    val hasShadow = shadowElevation > 0.dp
    val haptic = LocalHapticFeedback.current
    val outlineWidth = LocalMaterialOutlineWidth.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onClick() },
        modifier = modifier,
        enabled = enabled,
        interactionSource = actualInteractionSource,
        shape = actualShape,
        colors = colors,
        contentPadding = contentPadding,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = shadowElevation,
            pressedElevation = shadowElevation,
            focusedElevation = shadowElevation,
            hoveredElevation = shadowElevation,
            disabledElevation = 0.dp,
        ),
        border = outlineWidth.takeIf { it > 0f }?.let { BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant) },
        content = {
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
    val groupExpansionPerSide = LocalMaterialButtonGroupExpansionPerSide.current
    val isGroupMember = groupExpansionPerSide > 0.dp
    val actualShape = if (!isGroupMember) {
        shape?.withAlohaPressProgress(shapeProgress, shapeIndex, shapeCount)
            ?: alohaSegmentedShape(index = shapeIndex, count = shapeCount, vertical = false, pressProgress = shapeProgress)
    } else {
        shape ?: alohaSegmentedShape(index = shapeIndex, count = shapeCount, vertical = false, pressProgress = shapeProgress)
    }
    val outlineWidth = LocalMaterialOutlineWidth.current
    // Use the same group-aware shadow policy as filled and toggle buttons.
    val shadowElevation = alohaButtonShadowElevation(isGroupMember = isGroupMember)
    val hasShadow = shadowElevation > 0.dp
    val haptic = LocalHapticFeedback.current
    // Every text button uses Material3's native Surface elevation renderer,
    // matching ExpressiveButtonSurface regardless of the selected shape.
    val styledModifier = modifier.then(
        outlineWidth.takeIf { it > 0f }?.let {
            Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, actualShape)
        } ?: Modifier
    )
    TextButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.VirtualKey); onClick() },
        modifier = styledModifier,
        enabled = enabled,
        shape = actualShape,
        colors = colors,
        elevation = if (hasShadow) {
            ButtonDefaults.buttonElevation(
                defaultElevation = shadowElevation,
                pressedElevation = shadowElevation,
                focusedElevation = shadowElevation,
                hoveredElevation = shadowElevation,
                disabledElevation = 0.dp,
            )
        } else null,
        interactionSource = actualInteractionSource,
        content = {
            val groupExpansion = if (pressMotionEnabled) groupExpansionPerSide * shapeProgress else 0.dp
            Spacer(Modifier.width(groupExpansion))
            content()
            Spacer(Modifier.width(groupExpansion))
        },
    )
}


/**
 * Filled action control for members of an ExpressiveButtonGroup.
 * It uses the same shared Material container, outline, elevation, segmented
 * geometry and horizontal press expansion as the home button group.
 */
@Composable
fun ExpressiveButtonGroupAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    shapeIndex: Int,
    shapeCount: Int,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ExpressiveButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        shapeIndex = shapeIndex,
        shapeCount = shapeCount,
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
    pressMotionEnabled: Boolean = true,
    shadowEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressExpansion = rememberMaterialExpressivePressExpansion(actualInteractionSource, enabled)
    val shapeProgress = if (pressMotionEnabled) (pressExpansion.value / 16f).coerceIn(0f, 1f) else 0f
    val groupExpansionPerSide = LocalMaterialButtonGroupExpansionPerSide.current
    val actualShape = if (groupExpansionPerSide > 0.dp) {
        shape ?: alohaSegmentedShape(index = -1, count = 1, vertical = false, pressProgress = shapeProgress)
    } else {
        shape?.withAlohaPressProgress(shapeProgress)
            ?: alohaSegmentedShape(index = -1, count = 1, vertical = false, pressProgress = shapeProgress)
    }
    val outlineWidth = LocalMaterialOutlineWidth.current
    val shadowElevationTarget = alohaButtonShadowElevation(
        isGroupMember = groupExpansionPerSide > 0.dp,
        enabled = shadowEnabled,
    )
    val hasShadow = shadowElevationTarget > 0.dp
    val haptic = LocalHapticFeedback.current
    val shadowElevation by animateDpAsState(shadowElevationTarget, label = "iconButtonShadow")
    // Keep the shadow layer outside the content clip so the outline is not cut off.
    val styledModifier = modifier
        .then(if (hasShadow) Modifier.alohaButtonShadow(actualShape, shadowElevation) else Modifier)
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
    // Input frames belong to the container-shadow preference, not the button preference.
    val styledModifier = modifier.then(
        if (LocalMaterialShadowContainers.current) {
            Modifier.alohaContainerShadow(actualShape, 2.dp)
        } else Modifier
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
    // Inline input frames follow container shadows, independently of button shadows.
    val styledModifier = modifier.then(
        if (LocalMaterialShadowContainers.current) Modifier.alohaContainerShadow(actualShape, 2.dp)
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
    // The global container-shadow setting is authoritative, including callers that
    // provide a component-specific elevation override.
    val actualElevation = if (LocalMaterialShadowContainers.current) {
        shadowElevation ?: 2.dp
    } else {
        0.dp
    }
    // Match the color-choice reference: Material3 Surface owns the shadow
    // for every shape, including WavyShape. Never mix two shadow renderers.
    val surfaceModifier = modifier
    val materialElevation = actualElevation
    if (onClick == null) {
        androidx.compose.material3.Surface(
            modifier = surfaceModifier, shape = actualShape, color = color, contentColor = contentColor,
            tonalElevation = tonalElevation, shadowElevation = materialElevation, border = actualBorder,
            content = {
                CompositionLocalProvider(LocalMaterialShadowHostActive provides (LocalMaterialShadowHostActive.current || actualElevation > 0.dp)) { content() }
            },
        )
    } else {
        androidx.compose.material3.Surface(
            onClick = onClick, modifier = surfaceModifier, shape = actualShape, color = color,
            contentColor = contentColor, tonalElevation = tonalElevation,
            shadowElevation = materialElevation, border = actualBorder,
            interactionSource = interactionSource, content = {
                CompositionLocalProvider(LocalMaterialShadowHostActive provides (LocalMaterialShadowHostActive.current || actualElevation > 0.dp)) { content() }
            },
        )
    }
}


/**
 * A filled clickable surface owned by the button-shadow preference.
 * Use this for compact value chips; ordinary ExpressiveSurface remains owned
 * exclusively by the container-shadow preference.
 */
@Composable
fun ExpressiveButtonSurface(
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
    // Preserve the optional API override but cap it at the shared 1dp button standard.
    val actualElevation = if (alohaButtonShadowElevation() > 0.dp) {
        (shadowElevation ?: 1.dp).coerceIn(0.dp, 1.dp)
    } else 0.dp
    // Keep the button-surface reference on the same Material3 native
    // elevation renderer for every shape; WavyShape must not lose its shadow.
    val surfaceModifier = modifier
    val materialElevation = actualElevation
    if (onClick == null) {
        androidx.compose.material3.Surface(
            modifier = surfaceModifier, shape = actualShape, color = color, contentColor = contentColor,
            tonalElevation = tonalElevation, shadowElevation = materialElevation, border = actualBorder,
            content = {
                CompositionLocalProvider(LocalMaterialShadowHostActive provides (LocalMaterialShadowHostActive.current || actualElevation > 0.dp)) { content() }
            },
        )
    } else {
        androidx.compose.material3.Surface(
            onClick = onClick, modifier = surfaceModifier, shape = actualShape, color = color,
            contentColor = contentColor, tonalElevation = tonalElevation,
            shadowElevation = materialElevation, border = actualBorder,
            interactionSource = interactionSource, content = {
                CompositionLocalProvider(LocalMaterialShadowHostActive provides (LocalMaterialShadowHostActive.current || actualElevation > 0.dp)) { content() }
            },
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
        navigationIcon = {
            CompositionLocalProvider(LocalMaterialShadowButtons provides false) {
                navigationIcon()
            }
        },
        actions = {
            CompositionLocalProvider(LocalMaterialShadowButtons provides false) {
                actions()
            }
        },
        colors = colors,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
    )
}