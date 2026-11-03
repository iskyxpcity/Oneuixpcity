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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSwitches
import com.alohaoneui.app.ui.theme.LocalMaterialSwitchStyle

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
    // All eight switch families use the active Material color scheme. Each
    // family keeps a distinct tonal identity without fixed brand colors.
    // Voyage palette roles mapped to the active Aloha Material scheme.
    val checkedTrack = when (style) {
        0, 1 -> scheme.primary
        2 -> scheme.primaryContainer
        3 -> scheme.secondaryContainer
        4 -> scheme.tertiaryContainer
        5 -> scheme.surfaceContainerHighest
        6 -> scheme.secondaryContainer
        else -> scheme.primary
    }
    val uncheckedTrack = when (style) {
        3 -> scheme.surfaceContainer
        5 -> scheme.surfaceContainerHigh
        else -> scheme.surfaceContainerHighest
    }
    val checkedThumb = when (style) {
        0, 1 -> scheme.onPrimary
        2 -> scheme.onPrimaryContainer
        3 -> scheme.onSecondaryContainer
        4 -> scheme.onTertiaryContainer
        5 -> scheme.onSurface
        6 -> scheme.onSecondaryContainer
        else -> scheme.onPrimary
    }
    val uncheckedThumb = when (style) {
        2, 7 -> scheme.outline
        3, 5 -> scheme.onSurfaceVariant
        else -> scheme.onSurfaceVariant
    }
    val checkedBorder = when (style) {
        0, 1, 2, 7 -> scheme.primary
        3 -> scheme.secondary
        4 -> scheme.tertiary
        5 -> scheme.outlineVariant
        else -> scheme.secondary
    }
    val uncheckedBorder = scheme.outlineVariant
    // Outer layout dimensions transcribed from the corresponding Voyage
    // implementations (j7w0 dispatch -> z4v0/frt0/it70/ckw0/rdf/y2t0/l140).
    // Cupertino and Liquid Glass intentionally have different footprints.
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
    // Preserve source interaction-state distinctions, while giving the visible
    // thumb a fuller capsule silhouette that occupies about 65% of track width.
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
    // Match Voyage's per-family motion timings instead of one shared curve.
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
    // Voyage G² Smooth's general profile can degenerate at the near-capsule
    // dimensions used by switch tracks/thumbs. Keep the Smooth family visually
    // continuous with a capsule outline for switches, rather than letting a
    // degenerate custom path make the Canvas fail to draw every switch.
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
    val borderWidth = LocalMaterialOutlineWidth.current
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
            if (style == 5 && glassInteraction > 0.001f) {
                drawVoyageGlassOutline(
                    outline = trackOutline,
                    bounds = Size(w, h),
                    tint = if (checked) scheme.primary else scheme.surfaceContainerHighest,
                    alpha = glassInteraction * if (checked) 0.90f else 0.76f,
                )
            }
            val borderPx = borderWidth.dp.toPx()
            if (borderPx > 0f) {
                val borderOutline = trackShape.createOutline(
                    size = Size((w - borderPx).coerceAtLeast(0f), (h - borderPx).coerceAtLeast(0f)),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                withTransform({ translate(borderPx / 2f, borderPx / 2f) }) {
                    drawVoyageOutline(
                        borderOutline,
                        (if (checked) checkedBorder else uncheckedBorder).copy(alpha = if (enabled) 1f else 0.45f),
                        style = Stroke(width = borderPx),
                    )
                }
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
            val thumbColor = if (!enabled) {
                if (checked) colors.disabledCheckedThumbColor else colors.disabledUncheckedThumbColor
            } else if (interactive && style != 5) {
                if (checked) androidx.compose.ui.graphics.lerp(checkedThumb, scheme.primaryContainer, 0.12f)
                else androidx.compose.ui.graphics.lerp(uncheckedThumb, scheme.onSurfaceVariant, 0.10f)
            } else if (checked) checkedThumb else uncheckedThumb
            withTransform({ translate(cx - thumbWidthPx / 2f, cy - thumbHeightPx / 2f) }) {
                if (enabled && interactionProgress > 0.001f && style != 5) {
                    drawVoyageOutline(thumbOutline, (if (checked) scheme.onPrimary else scheme.onSurfaceVariant).copy(alpha = interactionProgress * 0.10f))
                }
                drawVoyageOutline(thumbOutline, thumbColor)
                if (style == 5 && glassInteraction > 0.001f) {
                    // The active thumb expands slightly and gains a layered
                    // specular shell while pressed, hovered or focused.
                    withTransform({
                        scale(
                            scaleX = glassScale,
                            scaleY = glassScale,
                            pivot = Offset(thumbWidthPx / 2f, thumbHeightPx / 2f),
                        )
                    }) {
                        drawVoyageGlassOutline(
                            outline = thumbOutline,
                            bounds = Size(thumbWidthPx, thumbHeightPx),
                            tint = if (checked) scheme.primary else scheme.surfaceContainerHigh,
                            alpha = glassInteraction,
                        )
                        drawVoyageOutline(
                            thumbOutline,
                            Color.White.copy(alpha = glassInteraction * 0.98f),
                            style = Stroke(width = 1.15.dp.toPx()),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = glassInteraction * 0.94f),
                            start = Offset(thumbWidthPx * 0.22f, thumbHeightPx * 0.22f),
                            end = Offset(thumbWidthPx * 0.72f, thumbHeightPx * 0.22f),
                            strokeWidth = 1.4.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            // Switch thumb icons/checkmarks are intentionally omitted; the
            // thumb itself communicates the on/off state by position and color.
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

/** Draw a lightweight Liquid Glass treatment using translucent vertical layers
 * and specular edge reflections. This is intentionally shader-free so the switch
 * remains inexpensive while scrolling lists of settings.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVoyageGlassOutline(
    outline: Outline,
    bounds: Size,
    tint: Color,
    alpha: Float,
) {
    val path = Path().apply {
        when (outline) {
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
        }
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.94f * alpha),
            0.16f to tint.copy(alpha = 0.22f * alpha),
            0.48f to tint.copy(alpha = 0.58f * alpha),
            0.76f to tint.copy(alpha = 0.80f * alpha),
            1.0f to Color.White.copy(alpha = 0.38f * alpha),
            startY = 0f,
            endY = bounds.height.coerceAtLeast(1f),
        ),
    )
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.76f * alpha),
                Color.White.copy(alpha = 0.16f * alpha),
                Color.Transparent,
            ),
            center = Offset(bounds.width * 0.28f, bounds.height * 0.12f),
            radius = (bounds.maxDimension * 0.92f).coerceAtLeast(1f),
        ),
    )
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.96f * alpha),
        style = Stroke(width = 1.05.dp.toPx()),
    )
    drawPath(
        path = path,
        color = tint.copy(alpha = 0.30f * alpha),
        style = Stroke(width = 0.65.dp.toPx()),
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
