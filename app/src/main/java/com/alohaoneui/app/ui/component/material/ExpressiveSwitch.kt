package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.blur
import com.alohaoneui.app.ui.component.liquid.vibrancy
import com.alohaoneui.app.ui.component.liquid.lens
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSwitches
import com.alohaoneui.app.ui.theme.LocalMaterialSwitchStyle

val LocalVoyageSwitchBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Voyage's eight selectable switch families. The selector changes the actual
 * track/thumb geometry and palette rather than applying a cosmetic scale to
 * the platform Material switch. Toggle semantics and animated thumb travel
 * remain shared across all styles.
 */
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

    // Material You is rendered by the official Material 3 Switch component.
    // This keeps its measured geometry, thumb motion, semantics and state
    // treatment aligned with the AndroidX implementation instead of a
    // hand-drawn approximation.
    if (style == 0) {
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            thumbContent = thumbContent,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
        return
    }

    // Compose style uses the official Compose Material (Material 2) Switch.
    // It deliberately retains Material 2 proportions and color behavior;
    // it is not a recolored Material 3 control.
    if (style == 1) {
        val scheme = MaterialTheme.colorScheme
        androidx.compose.material.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
            colors = androidx.compose.material.SwitchDefaults.colors(
                checkedThumbColor = scheme.secondary,
                checkedTrackColor = scheme.secondary.copy(alpha = 0.54f),
                uncheckedThumbColor = scheme.outline,
                uncheckedTrackColor = scheme.surfaceVariant,
                disabledCheckedThumbColor = scheme.onSurface.copy(alpha = 0.38f),
                disabledCheckedTrackColor = scheme.onSurface.copy(alpha = 0.12f),
                disabledUncheckedThumbColor = scheme.onSurface.copy(alpha = 0.38f),
                disabledUncheckedTrackColor = scheme.onSurface.copy(alpha = 0.12f),
            ),
        )
        return
    }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val pressedOrHovered = pressed || hovered
    val interactive = pressed || hovered || focused
    val interactionProgress by animateFloatAsState(targetValue = if (interactive && enabled) 1f else 0f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 700f), label = "voyageSwitchInteraction")
    // Liquid Glass is transient: the handle gains its glass treatment only
    // while the user is actively pressing the Material switch.
    val glassInteraction by animateFloatAsState(
        targetValue = if (style == 5 && interactive && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
        label = "voyageLiquidGlassInteraction",
    )
    val glassScale by animateFloatAsState(
        targetValue = if (style == 5 && pressed && enabled) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 460f),
        label = "voyageLiquidGlassScale",
    )
    val scheme = MaterialTheme.colorScheme
    // Each family has its own OFF and ON color recipe. Every role is read
    // from the active Aloha Material scheme, so light/dark and user theme
    // changes propagate without embedding platform brand colors.
    val checkedTrack = when (style) {
        0 -> scheme.primary
        1 -> scheme.primaryContainer
        2 -> scheme.primary
        3 -> scheme.secondaryContainer
        4 -> scheme.tertiary
        5 -> scheme.surfaceContainerHigh
        6 -> scheme.secondary
        else -> scheme.primary
    }
    val uncheckedTrack = when (style) {
        0 -> scheme.surfaceContainerHighest
        1 -> scheme.surfaceContainerLow
        2 -> scheme.surfaceContainer
        3 -> scheme.surfaceContainerHigh
        4 -> scheme.surfaceContainerHighest
        5 -> scheme.surfaceContainerLow
        6 -> scheme.surfaceContainer
        else -> scheme.surfaceContainerHigh
    }
    val checkedThumb = when (style) {
        0 -> scheme.onPrimary
        1 -> scheme.onPrimaryContainer
        2 -> scheme.onPrimary
        3 -> scheme.onSecondaryContainer
        4 -> scheme.onTertiary
        5 -> scheme.surface.copy(alpha = 0.76f)
        6 -> scheme.onSecondary
        else -> scheme.onPrimary
    }
    val uncheckedThumb = when (style) {
        0 -> scheme.outline
        1 -> scheme.onSurfaceVariant
        2 -> scheme.outline
        3 -> scheme.onSurface
        4 -> scheme.surface
        5 -> scheme.surfaceContainerHighest
        6 -> scheme.onSurfaceVariant
        else -> scheme.surface
    }
    val checkedBorder = when (style) {
        0, 2, 7 -> scheme.primary
        1 -> scheme.primaryContainer
        3 -> scheme.secondary
        4 -> scheme.tertiary
        5 -> scheme.outlineVariant
        6 -> scheme.secondary
        else -> scheme.primary
    }
    val uncheckedBorder = when (style) {
        0 -> scheme.outline
        1 -> scheme.outlineVariant
        2 -> scheme.outline
        3 -> scheme.outline
        4 -> scheme.outlineVariant
        5 -> scheme.outlineVariant
        6 -> scheme.outline
        else -> scheme.outlineVariant
    }
    // Per-family control footprints. These are deliberately independent:
    // the styles do not collapse into one shared capsule geometry.
    val trackSize = when (style) {
        0 -> DpSize(52.dp, 32.dp)       // Material You
        1 -> DpSize(52.dp, 32.dp)       // Compose
        2 -> DpSize(56.dp, 28.dp)       // Pixel
        3 -> DpSize(48.dp, 24.dp)       // Fluent
        4 -> DpSize(64.dp, 28.dp)       // Cupertino
        5 -> DpSize(51.dp, 31.dp)       // Liquid Glass
        6 -> DpSize(50.dp, 28.5.dp)    // HyperOS
        else -> DpSize(39.dp, 22.dp)   // One UI outer touch target
    }
    // Each family owns its handle proportions and pressed-state response.
    val sourceThumbDiameter = when (style) {
        0 -> when {
            pressed -> 28.dp
            checked -> 24.dp
            else -> 16.dp
        }
        1 -> when {
            pressed -> 28.dp
            checked || thumbContent != null -> 24.dp
            else -> 16.dp
        }
        2 -> 20.dp
        3 -> if (enabled && pressedOrHovered) 17.dp else 12.dp
        4, 5 -> 20.dp
        6 -> if (enabled && interactive) 23.dp else 20.dp
        7 -> 22.dp
        else -> 20.dp
    }
    val thumbWidth = when (style) {
        0, 1 -> sourceThumbDiameter
        2 -> 20.dp
        3 -> if (enabled && pressedOrHovered) 17.dp else 12.dp
        4 -> 20.dp
        5 -> 23.dp
        6 -> if (enabled && interactive) 23.dp else 20.dp
        else -> 22.dp
    }
    val thumbHeight = when (style) {
        0, 1 -> sourceThumbDiameter
        2 -> 20.dp
        3 -> if (enabled && pressedOrHovered) 17.dp else 12.dp
        4 -> 20.dp
        5 -> 23.dp
        6 -> if (enabled && interactive) 23.dp else 20.dp
        else -> 22.dp
    }
    val thumbEdgeInset = when (style) {
        0, 1, 2 -> 4.dp
        3 -> 3.dp
        4 -> 2.dp
        5 -> 3.dp
        6 -> 2.5.dp
        else -> 0.dp
    }
    val trackCornerRadius = when (style) {
        0, 1 -> 16.dp
        2 -> 14.dp
        3 -> 4.dp
        4 -> 14.dp
        5 -> 15.5.dp
        6 -> 14.25.dp
        else -> 9.25.dp
    }
    // Motion profiles follow each control family's interaction character.
    val travelSpec = when (style) {
        0, 1 -> spring<Float>(dampingRatio = 0.78f, stiffness = 620f)
        2 -> androidx.compose.animation.core.tween<Float>(durationMillis = 500)
        3 -> androidx.compose.animation.core.tween<Float>(durationMillis = 600)
        4, 5 -> androidx.compose.animation.core.tween<Float>(durationMillis = 300)
        6 -> spring<Float>(dampingRatio = 0.75f, stiffness = 1500f)
        else -> androidx.compose.animation.core.tween<Float>(durationMillis = 250)
    }
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = travelSpec,
        label = "voyageSwitchThumb",
    )
    // Keep switch outlines robust for every global Aloha shape preset.
    val trackShape = if (com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle.current == 3) {
        remember(trackCornerRadius) { RoundedCornerShape(trackCornerRadius) }
    } else {
        voyageCornerShape(trackCornerRadius)
    }
    // Handle silhouette is style-specific, rather than a shared capsule.
    val thumbShape = when (style) {
        0 -> RoundedCornerShape(50)
        1 -> RoundedCornerShape(36)
        2 -> RoundedCornerShape(50)
        3 -> RoundedCornerShape(7.dp)
        4 -> RoundedCornerShape(50)
        5 -> RoundedCornerShape(42)
        6 -> RoundedCornerShape(38)
        else -> RoundedCornerShape(50)
    }
    val haloShape = RoundedCornerShape(50)
    // Shared by the Canvas thumb and the Liquid Glass fallback layer.
    val thumbColor = if (!enabled) {
        if (checked) colors.disabledCheckedThumbColor else colors.disabledUncheckedThumbColor
    } else if (interactive && style != 5) {
        if (checked) androidx.compose.ui.graphics.lerp(checkedThumb, scheme.primaryContainer, 0.12f)
        else androidx.compose.ui.graphics.lerp(uncheckedThumb, scheme.onSurfaceVariant, 0.10f)
    } else if (checked) checkedThumb else uncheckedThumb
    Box(
        modifier = modifier
            .size(width = trackSize.width, height = trackSize.height)
            .then(if (LocalMaterialShadowSwitches.current) Modifier.shadow(2.dp, trackShape) else Modifier)
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) },
            )
,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val trackColor = if (enabled) {
                if (checked) checkedTrack else uncheckedTrack
            } else if (style == 0) {
                if (checked) colors.disabledCheckedTrackColor else colors.disabledUncheckedTrackColor
            } else {
                (if (checked) checkedTrack else uncheckedTrack).copy(alpha = 0.38f)
            }
            // The actual outline, not a hard-coded rounded rectangle, keeps the
            // global Voyage shape family and corner scale linked to each style.
            val trackOutline = trackShape.createOutline(
                size = Size(w, h),
                layoutDirection = layoutDirection,
                density = this,
            )
            drawVoyageOutline(trackOutline, trackColor)
            if (interactionProgress > 0.001f && style != 5) {
                drawVoyageOutline(trackOutline, (if (checked) scheme.onPrimary else scheme.onSurfaceVariant).copy(alpha = interactionProgress * if (pressed) 0.12f else 0.08f))
            }
            if (focused && enabled) {
                drawVoyageOutline(trackOutline, scheme.primary.copy(alpha = 0.72f), style = Stroke(width = 1.25.dp.toPx()))
            }
            // Material You (Material 3) alone uses the documented track
            // decoration: outline when OFF, transparent when ON. Other switch
            // families must not inherit this perimeter.
            if (style == 0 && !checked) {
                drawVoyageOutline(
                    trackOutline,
                    scheme.outline.copy(alpha = if (enabled) 1f else 0.38f),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            val thumbWidthPx = thumbWidth.toPx()
            val thumbHeightPx = thumbHeight.toPx()
            val edgeInsetPx = thumbEdgeInset.toPx()
            val left = thumbWidthPx / 2f + edgeInsetPx
            val right = w - thumbWidthPx / 2f - edgeInsetPx
            val cx = left + (right - left) * travel
            val cy = h / 2f
            if (style == 5 && glassInteraction > 0.001f) {
                val haloWidth = thumbWidthPx + 4.dp.toPx()
                val haloHeight = thumbHeightPx + 4.dp.toPx()
                val haloOutline = haloShape.createOutline(
                    size = Size(haloWidth, haloHeight),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                withTransform({ translate(cx - haloWidth / 2f, cy - haloHeight / 2f) }) {
                    drawVoyageOutline(
                        haloOutline,
                        Color.White.copy(alpha = glassInteraction * if (checked) 0.42f else 0.30f),
                    )
                }
            }
            val thumbOutline = thumbShape.createOutline(
                size = Size(thumbWidthPx, thumbHeightPx),
                layoutDirection = layoutDirection,
                density = this,
            )
            withTransform({ translate(cx - thumbWidthPx / 2f, cy - thumbHeightPx / 2f) }) {
                if (enabled && interactionProgress > 0.001f && style != 5) {
                    drawVoyageOutline(thumbOutline, (if (checked) scheme.onPrimary else scheme.onSurfaceVariant).copy(alpha = interactionProgress * 0.10f))
                }
                if (style != 5) drawVoyageOutline(thumbOutline, thumbColor)
            }
            // Switch thumb icons/checkmarks are intentionally omitted; the
            // thumb itself communicates the on/off state by position and color.
        }
        if (style == 5) {
            val backdrop = LocalVoyageSwitchBackdrop.current
            val thumbTravel = trackSize.width - thumbWidth - thumbEdgeInset * 2
            val glassTint = if (checked) scheme.primary else scheme.surfaceContainerHigh
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = thumbTravel * travel - thumbTravel / 2f)
                    .size(width = thumbWidth, height = thumbHeight)
                    .then(
                        if (backdrop != null && enabled && glassInteraction > 0.001f) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { thumbShape },
                                effects = {
                                    vibrancy()
                                    blur(4.dp.toPx() * glassInteraction, 4.dp.toPx() * glassInteraction)
                                    lens(
                                        refractionHeight = 12.dp.toPx() * glassInteraction,
                                        refractionAmount = 16.dp.toPx() * glassInteraction,
                                        depthEffect = true,
                                        chromaticAberration = 0.35f * glassInteraction,
                                    )
                                },
                                onDrawSurface = {
                                    drawRect(glassTint.copy(alpha = 0.12f * glassInteraction))
                                },
                                layerBlock = {
                                    scaleX = glassScale
                                    scaleY = glassScale
                                },
                            )
                        } else {
                            Modifier.clip(thumbShape)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (backdrop == null || glassInteraction <= 0.001f) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = thumbColor,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2f, size.height / 2f),
                        )
                    }
                }
            }
        }
        if (thumbContent != null) {
            val thumbTravel = trackSize.width - thumbWidth - thumbEdgeInset * 2
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = thumbTravel * travel - thumbTravel / 2f)
                    .size(width = thumbWidth, height = thumbHeight)
                    .clip(thumbShape),
                contentAlignment = Alignment.Center,
            ) {
                thumbContent()
            }
        }
    }
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
