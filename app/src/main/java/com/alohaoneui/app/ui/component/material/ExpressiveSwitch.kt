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
    val scheme = MaterialTheme.colorScheme
    val checkedTrack = when (style) {
        0 -> colors.checkedTrackColor
        1 -> scheme.primary
        2 -> scheme.primaryContainer
        3 -> scheme.secondary
        4 -> Color(0xFF34C759)
        5 -> scheme.tertiaryContainer
        6 -> Color(0xFF1684FC)
        7 -> scheme.primary
        else -> scheme.primary
    }
    val uncheckedTrack = scheme.surfaceContainerHighest
    val checkedThumb = when (style) {
        0 -> colors.checkedThumbColor
        1 -> scheme.onPrimary
        2 -> scheme.primary
        3 -> scheme.onSecondary
        5 -> scheme.onTertiaryContainer
        7 -> scheme.onPrimary
        else -> Color.White
    }
    val uncheckedThumb = scheme.outline
    val checkedBorder = when (style) {
        2 -> scheme.primaryContainer
        3 -> scheme.secondary
        4 -> Color(0xFF34C759)
        5 -> scheme.tertiary
        6 -> Color(0xFF1684FC)
        else -> checkedTrack
    }
    val uncheckedBorder = scheme.outline
    // Exact outer slot dimensions from Voyage's j7w0.m105365O dispatch:
    // 0 z4v0.m147164o = 52x32; 1 frt0.m95463l = 52x32;
    // 2 it70.m103876l = 56x28; 3 ckw0.m9293l = 48x24;
    // 4 rdf.m127934l = 51x31; 5 rdf.m127933O -> m127932I = 64x28;
    // 6 y2t0.m144445l = 50x28.5; 7 l140.m110104l = 39x22.
    // Voyage source track bounds. Styles 4 and 7 have a larger outer slot;
    // their actual painted tracks remain the smaller inner dimensions.
    val trackSize = when (style) {
        0, 1 -> DpSize(52.dp, 32.dp)
        2 -> DpSize(56.dp, 28.dp)
        3 -> DpSize(48.dp, 24.dp)
        4 -> DpSize(48.dp, 28.dp)
        5 -> DpSize(64.dp, 28.dp)
        6 -> DpSize(50.dp, 28.5.dp)
        7 -> DpSize(35.dp, 18.5.dp)
        else -> DpSize(52.dp, 32.dp)
    }
    val slotSize = when (style) {
        4 -> DpSize(51.dp, 31.dp)
        7 -> DpSize(39.dp, 22.dp)
        else -> trackSize
    }
    // Preserve source interaction-state distinctions, while giving the visible
    // thumb a fuller capsule silhouette that occupies about 65% of track width.
    // Source thumb diameters by Voyage renderer state:
    // z4v0/frt0 16/24/28; it70 20; ckw0 12/14/17;
    // rdf.l 20; rdf.I 24; y2t0 20/23/26; l140 14/16/18.
    val sourceThumbDiameter = when (style) {
        0, 1 -> when {
            pressed -> 28.dp
            checked || (style == 1 && thumbContent != null) -> 24.dp
            else -> 16.dp
        }
        2 -> 20.dp
        3 -> when {
            enabled && pressedOrHovered -> 17.dp
            checked -> 14.dp
            else -> 12.dp
        }
        4 -> 20.dp
        5 -> 24.dp
        6 -> when {
            enabled && pressedOrHovered -> 26.dp
            checked -> 23.dp
            else -> 20.dp
        }
        7 -> when {
            enabled && pressedOrHovered -> 18.dp
            checked -> 16.dp
            else -> 14.dp
        }
        else -> 20.dp
    }
    // These source renderers use circular thumb diameters, not a width derived
    // from a percentage of the track. Style 7 retains its 22dp outer thumb slot.
    val thumbWidth = sourceThumbDiameter
    val thumbHeight = sourceThumbDiameter
    val thumbSlotWidth = if (style == 7) 22.dp else sourceThumbDiameter + 3.dp
    val thumbSlotHeight = if (style == 7) 22.dp else sourceThumbDiameter + 3.dp
    val trackCornerRadius = when (style) {
        2 -> 14.dp
        3 -> 4.dp
        4 -> 14.dp
        5 -> 14.dp
        6 -> 14.25.dp
        7 -> 9.25.dp
        else -> 16.dp
    }
    val slotCornerRadius = when (style) {
        4 -> 15.5.dp
        7 -> 11.dp
        else -> trackCornerRadius
    }
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 620f),
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
    val slotShape = if (com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle.current == 3) {
        remember(slotCornerRadius) { RoundedCornerShape(slotCornerRadius) }
    } else {
        voyageCornerShape(slotCornerRadius)
    }
    val thumbShape = if (com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle.current == 3) {
        remember(thumbHeight) { RoundedCornerShape(thumbHeight / 2f) }
    } else {
        voyageCornerShape(thumbHeight / 2f)
    }
    val haloShape = if (com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle.current == 3) {
        remember(thumbHeight) { RoundedCornerShape(thumbHeight / 2f + 2.dp) }
    } else {
        voyageCornerShape(thumbHeight / 2f + 2.dp)
    }
    val thumbSlotShape = if (com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle.current == 3) {
        remember(thumbSlotHeight) { RoundedCornerShape(thumbSlotHeight / 2f) }
    } else {
        voyageCornerShape(thumbSlotHeight / 2f)
    }
    val borderWidth = LocalMaterialOutlineWidth.current
    Box(
        modifier = modifier
            .size(width = slotSize.width, height = slotSize.height)
            .then(if (LocalMaterialShadowSwitches.current) Modifier.shadow(2.dp, slotShape) else Modifier)
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
            } else if (style == 0) {
                if (checked) colors.disabledCheckedTrackColor else colors.disabledUncheckedTrackColor
            } else {
                (if (checked) checkedTrack else uncheckedTrack).copy(alpha = 0.38f)
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
                withTransform({ translate(trackLeft + borderPx / 2f, trackTop + borderPx / 2f) }) {
                    drawVoyageOutline(
                        borderOutline,
                        (if (checked) checkedBorder else uncheckedBorder).copy(alpha = if (enabled) 1f else 0.45f),
                        style = Stroke(width = borderPx),
                    )
                }
            }
            val thumbWidthPx = thumbWidth.toPx()
            val thumbHeightPx = thumbHeight.toPx()
            val thumbSlotWidthPx = thumbSlotWidth.toPx()
            val left = trackLeft + thumbSlotWidthPx / 2f
            val right = trackLeft + trackWidthPx - thumbSlotWidthPx / 2f
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
            } else if (style == 0) {
                if (checked) colors.disabledCheckedThumbColor else colors.disabledUncheckedThumbColor
            } else {
                (if (checked) checkedThumb else uncheckedThumb).copy(alpha = 0.65f)
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
        if (thumbContent != null) {
            val thumbTravel = trackSize.width - thumbSlotWidth
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = thumbTravel * travel - thumbTravel / 2f)
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
