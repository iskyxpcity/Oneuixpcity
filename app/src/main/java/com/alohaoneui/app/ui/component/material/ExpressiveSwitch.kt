package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    val scheme = MaterialTheme.colorScheme
    // Voyage a2i0 palette: the classic renderers keep track, thumb and outline
    // on separate Material color roles rather than reusing the same accent.
    val checkedTrack = when (style) {
        0 -> colors.checkedTrackColor
        1, 2, 3 -> scheme.onPrimary
        4, 5 -> scheme.primary
        6, 7 -> scheme.primary
        else -> scheme.onPrimary
    }
    val uncheckedTrack = when (style) {
        0 -> colors.uncheckedTrackColor
        1, 2, 3 -> scheme.onSecondary
        4, 5 -> scheme.surfaceVariant
        6 -> scheme.inversePrimary
        7 -> scheme.onSecondaryContainer
        else -> scheme.onSecondary
    }
    val checkedThumb = when (style) {
        0 -> colors.checkedThumbColor
        1, 2, 3 -> scheme.primary
        4, 5, 6, 7 -> scheme.onPrimary
        else -> scheme.primary
    }
    val uncheckedThumb = when (style) {
        0 -> colors.uncheckedThumbColor
        1, 2, 3 -> scheme.inversePrimary
        4, 5 -> scheme.onSurface
        6 -> scheme.secondary
        7 -> scheme.tertiary
        else -> scheme.inversePrimary
    }
    val checkedBorder = when (style) {
        0, 1, 2, 3 -> scheme.primaryContainer
        4, 5 -> scheme.outlineVariant.copy(alpha = 0.33f)
        6 -> Color.Transparent
        7 -> scheme.outlineVariant
        else -> scheme.primaryContainer
    }
    val uncheckedBorder = when (style) {
        0, 1 -> scheme.secondaryContainer
        2, 3 -> scheme.onSecondary
        4, 5 -> scheme.outlineVariant.copy(alpha = 0.33f)
        6 -> Color.Transparent
        7 -> scheme.outlineVariant
        else -> scheme.secondaryContainer
    }
    // Source renderer bounds. Cupertino paints the full 51x31dp track;
    // One UI alone has a 39x22dp outer slot around its 35x18.5dp track.
    val trackSize = when (style) {
        0, 1 -> DpSize(52.dp, 32.dp)
        2 -> DpSize(56.dp, 28.dp)
        3 -> DpSize(48.dp, 24.dp)
        4 -> DpSize(51.dp, 31.dp)
        5 -> DpSize(64.dp, 28.dp)
        6 -> DpSize(50.dp, 28.5.dp)
        7 -> DpSize(35.dp, 18.5.dp)
        else -> DpSize(52.dp, 32.dp)
    }
    val slotSize = when (style) {
        7 -> DpSize(39.dp, 22.dp)
        else -> trackSize
    }
    // The Compose renderer has a fixed 20dp slot with a 16/24/28dp visible
    // thumb. Fluent uses a non-circular 12x12 -> 17x14dp thumb; Cupertino and
    // HyperOS animate scale independently of checked state.
    val targetThumbWidth = when (style) {
        0 -> 20.dp
        1 -> when {
            pressed -> 28.dp
            checked || (showThumbIcon && thumbContent != null) -> 24.dp
            else -> 16.dp
        }
        2 -> 20.dp
        3 -> if (enabled && pressedOrHovered) 17.dp else 12.dp
        4 -> if (enabled && pressedOrHovered) 25.dp else 20.dp
        5 -> 20.dp
        6 -> if (enabled && pressedOrHovered) 23.dp else 20.dp
        7 -> 22.dp
        else -> 20.dp
    }
    val targetThumbHeight = when (style) {
        0 -> 20.dp
        1 -> when {
            pressed -> 28.dp
            checked || (showThumbIcon && thumbContent != null) -> 24.dp
            else -> 16.dp
        }
        2 -> 20.dp
        3 -> if (enabled && pressedOrHovered) 14.dp else 12.dp
        4 -> if (enabled && pressedOrHovered) 25.dp else 20.dp
        5 -> 20.dp
        6 -> if (enabled && pressedOrHovered) 23.dp else 20.dp
        7 -> 22.dp
        else -> 20.dp
    }
    val thumbSizeAnimationSpec = when (style) {
        1 -> spring<Dp>(dampingRatio = 0.78f, stiffness = 620f)
        3 -> tween<Dp>(600, easing = FastOutSlowInEasing)
        4 -> tween<Dp>(300, easing = FastOutSlowInEasing)
        6 -> spring<Dp>(dampingRatio = 0.75f, stiffness = 1500f)
        else -> tween<Dp>(150)
    }
    val thumbWidth by animateDpAsState(
        targetValue = targetThumbWidth,
        animationSpec = thumbSizeAnimationSpec,
        label = "voyageSwitchThumbWidth",
    )
    val thumbHeight by animateDpAsState(
        targetValue = targetThumbHeight,
        animationSpec = thumbSizeAnimationSpec,
        label = "voyageSwitchThumbHeight",
    )
    val thumbSlotWidth = when (style) {
        0, 1, 4, 5, 6 -> 20.dp
        2, 3 -> 16.dp
        7 -> 22.dp
        else -> 20.dp
    }
    val thumbSlotHeight = when (style) {
        0, 1, 4, 5, 6 -> 20.dp
        2, 3 -> 16.dp
        7 -> 22.dp
        else -> 20.dp
    }
    // Voyage uses fixed Dp offsets in Pixel, Fluent, HyperOS and One UI.
    // These are the source left-edge offsets, not a percentage of the track.
    val thumbOffsetStart = when (style) {
        2 -> 4.dp
        3 -> 2.dp
        6, 7 -> 4.dp
        else -> 0.dp
    }
    val thumbOffsetEnd = when (style) {
        2 -> 32.dp
        3 -> 32.dp - thumbWidth / 2
        6 -> 26.dp
        7 -> 20.dp
        else -> trackSize.width - thumbSlotWidth
    }
    val thumbTravel = (thumbOffsetEnd - thumbOffsetStart).coerceAtLeast(0.dp)
    val trackCornerRadius = when (style) {
        0, 1 -> 16.dp
        2 -> 14.dp
        3 -> 4.dp
        4 -> 15.5.dp
        5 -> 14.dp
        6 -> 14.25.dp
        7 -> 9.25.dp
        else -> 16.dp
    }
    val slotCornerRadius = if (style == 7) 11.dp else trackCornerRadius
    val thumbCornerRadius = if (style == 3) 4.dp else thumbHeight / 2
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = when (style) {
            2 -> tween(500, easing = FastOutSlowInEasing)
            3 -> tween(600, easing = FastOutSlowInEasing)
            4, 5 -> tween(300, easing = FastOutSlowInEasing)
            6 -> spring(dampingRatio = 0.75f, stiffness = 1500f)
            7 -> tween(250)
            else -> spring(dampingRatio = 0.78f, stiffness = 620f)
        },
        label = "voyageSwitchThumb",
    )
    // Every renderer uses the live Voyage shape family. Smooth G² now has its
    // source-correct profile, so it no longer needs the former rounded fallback.
    val trackShape = voyageCornerShape(trackCornerRadius)
    val slotShape = voyageCornerShape(slotCornerRadius)
    val thumbShape = voyageCornerShape(thumbCornerRadius)
    val haloShape = voyageCornerShape(thumbHeight / 2f + 2.dp)
    val thumbSlotShape = voyageCornerShape(if (style == 3) 4.dp else thumbSlotHeight / 2f)
    val borderWidth = when (style) { 0, 1 -> 2f; 2 -> 1.8f; 3 -> 1f; 4 -> 2f; 5, 6 -> 0f; 7 -> 0.1f; else -> 2f }
    Box(
        modifier = modifier
            .size(width = slotSize.width, height = slotSize.height)
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
            val trackWidthPx = trackSize.width.toPx()
            val trackHeightPx = trackSize.height.toPx()
            val trackLeft = (w - trackWidthPx) / 2f
            val trackTop = (h - trackHeightPx) / 2f
            val trackColor = if (enabled) {
                if (checked) checkedTrack else uncheckedTrack
            } else {
                when (style) {
                    0 -> if (checked) colors.disabledCheckedTrackColor else colors.disabledUncheckedTrackColor
                    1, 2, 3 -> if (checked) scheme.onTertiary else scheme.background
                    4, 5 -> (if (checked) scheme.primary else scheme.surfaceVariant).copy(alpha = 0.38f)
                    6 -> if (checked) scheme.onSecondaryContainer else scheme.onTertiaryContainer
                    7 -> scheme.background
                    else -> scheme.background
                }
            }
            // The actual outline, not a hard-coded rounded rectangle, keeps the
            // global Voyage shape family and corner scale linked to each style.
            val trackOutline = trackShape.createOutline(
                size = Size(trackWidthPx, trackHeightPx),
                layoutDirection = layoutDirection,
                density = this,
            )
            withTransform({ translate(trackLeft, trackTop) }) {
                if (style == 5) {
                    drawVoyageGlassOutline(
                        outline = trackOutline,
                        bounds = Size(trackWidthPx, trackHeightPx),
                        tint = trackColor,
                        alpha = if (checked) 0.78f else 0.56f,
                    )
                } else {
                    drawVoyageOutline(trackOutline, trackColor)
                }
            }
            val borderPx = borderWidth.dp.toPx()
            if (borderPx > 0f) {
                val borderOutline = trackShape.createOutline(
                    size = Size((trackWidthPx - borderPx).coerceAtLeast(0f), (trackHeightPx - borderPx).coerceAtLeast(0f)),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                val borderColor = if (enabled) {
                    if (checked) checkedBorder else uncheckedBorder
                } else {
                    when (style) {
                        0 -> if (checked) colors.disabledCheckedBorderColor else colors.disabledUncheckedBorderColor
                        1, 2, 3 -> if (checked) scheme.tertiaryContainer else scheme.onBackground
                        4, 5 -> scheme.outlineVariant.copy(alpha = 0.18f)
                        7 -> scheme.outlineVariant
                        else -> Color.Transparent
                    }
                }
                withTransform({ translate(trackLeft + borderPx / 2f, trackTop + borderPx / 2f) }) {
                    drawVoyageOutline(
                        borderOutline,
                        borderColor,
                        style = Stroke(width = borderPx),
                    )
                }
            }
            val thumbWidthPx = thumbWidth.toPx()
            val thumbHeightPx = thumbHeight.toPx()
            val thumbSlotWidthPx = thumbSlotWidth.toPx()
            val left = trackLeft + thumbOffsetStart.toPx() + thumbSlotWidthPx / 2f
            val right = trackLeft + thumbOffsetEnd.toPx() + thumbSlotWidthPx / 2f
            val cx = left + (right - left) * travel
            val cy = trackTop + trackHeightPx / 2f
            if (style == 5) {
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
                        Color.White.copy(alpha = if (checked) 0.22f else 0.10f),
                    )
                }
            }
            val thumbOutline = thumbShape.createOutline(
                size = Size(thumbWidthPx, thumbHeightPx),
                layoutDirection = layoutDirection,
                density = this,
            )
            val thumbColor = if (enabled) {
                if (checked) checkedThumb else uncheckedThumb
            } else {
                when (style) {
                    0 -> if (checked) colors.disabledCheckedThumbColor else colors.disabledUncheckedThumbColor
                    1, 2, 3 -> if (checked) scheme.onSecondaryContainer else scheme.onTertiaryContainer
                    4, 5 -> scheme.onSurface.copy(alpha = 0.38f)
                    6 -> if (checked) scheme.tertiary else scheme.background
                    7 -> if (checked) scheme.onTertiaryContainer else scheme.onSurface
                    else -> scheme.onSurface.copy(alpha = 0.38f)
                }
            }
            withTransform({ translate(cx - thumbWidthPx / 2f, cy - thumbHeightPx / 2f) }) {
                if (style == 5) {
                    drawVoyageGlassOutline(
                        outline = thumbOutline,
                        bounds = Size(thumbWidthPx, thumbHeightPx),
                        tint = thumbColor,
                        alpha = if (checked) 0.96f else 0.88f,
                    )
                    // Tight specular rim and a soft top reflection give the thumb
                    // a glass volume instead of the previous flat white disc.
                    drawVoyageOutline(
                        thumbOutline,
                        Color.White.copy(alpha = if (checked) 0.86f else 0.62f),
                        style = Stroke(width = 0.8.dp.toPx()),
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.72f),
                        start = Offset(thumbWidthPx * 0.28f, thumbHeightPx * 0.23f),
                        end = Offset(thumbWidthPx * 0.68f, thumbHeightPx * 0.23f),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                } else {
                    drawVoyageOutline(thumbOutline, thumbColor)
                }
            }
            // Switch thumb icons/checkmarks are intentionally omitted; the
            // thumb itself communicates the on/off state by position and color.
        }
        if (style == 1 && showThumbIcon && thumbContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = thumbOffsetStart + thumbSlotWidth / 2f +
                            thumbTravel * travel - trackSize.width / 2f
                    )
                    .size(width = thumbSlotWidth, height = thumbSlotHeight)
                    .clip(thumbSlotShape),
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
            colors = listOf(
                Color.White.copy(alpha = 0.66f * alpha),
                tint.copy(alpha = 0.42f * alpha),
                tint.copy(alpha = 0.76f * alpha),
                Color.White.copy(alpha = 0.18f * alpha),
            ),
            startY = 0f,
            endY = bounds.height.coerceAtLeast(1f),
        ),
    )
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.78f * alpha),
        style = Stroke(width = 0.9.dp.toPx()),
    )
}

@Composable
fun expressiveSwitchColors(
    checkedTrackColor: Color = MaterialTheme.colorScheme.onPrimary,
    checkedThumbColor: Color = MaterialTheme.colorScheme.primary,
    checkedBorderColor: Color = MaterialTheme.colorScheme.primaryContainer,
    checkedIconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    uncheckedTrackColor: Color = MaterialTheme.colorScheme.onSecondary,
    uncheckedThumbColor: Color = MaterialTheme.colorScheme.inversePrimary,
    uncheckedBorderColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    uncheckedIconColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    disabledCheckedThumbColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    disabledCheckedTrackColor: Color = MaterialTheme.colorScheme.onTertiary,
    disabledCheckedBorderColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    disabledCheckedIconColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    disabledUncheckedThumbColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    disabledUncheckedTrackColor: Color = MaterialTheme.colorScheme.background,
    disabledUncheckedBorderColor: Color = MaterialTheme.colorScheme.onBackground,
    disabledUncheckedIconColor: Color = MaterialTheme.colorScheme.onSurface,
): SwitchColors = SwitchDefaults.colors(
    checkedTrackColor = checkedTrackColor,
    checkedThumbColor = checkedThumbColor,
    checkedBorderColor = checkedBorderColor,
    checkedIconColor = checkedIconColor,
    uncheckedTrackColor = uncheckedTrackColor,
    uncheckedThumbColor = uncheckedThumbColor,
    uncheckedBorderColor = uncheckedBorderColor,
    uncheckedIconColor = uncheckedIconColor,
    disabledCheckedThumbColor = disabledCheckedThumbColor,
    disabledCheckedTrackColor = disabledCheckedTrackColor,
    disabledCheckedBorderColor = disabledCheckedBorderColor,
    disabledCheckedIconColor = disabledCheckedIconColor,
    disabledUncheckedThumbColor = disabledUncheckedThumbColor,
    disabledUncheckedTrackColor = disabledUncheckedTrackColor,
    disabledUncheckedBorderColor = disabledUncheckedBorderColor,
    disabledUncheckedIconColor = disabledUncheckedIconColor,
)
