package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
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
    val uncheckedTrack = when (style) {
        0 -> colors.uncheckedTrackColor
        4 -> Color(0xFF8E8E93)
        5 -> scheme.surfaceVariant
        else -> scheme.surfaceContainerHighest
    }
    val checkedThumb = when (style) {
        0 -> colors.checkedThumbColor
        1 -> scheme.onPrimary
        2 -> scheme.primary
        3 -> scheme.onSecondary
        5 -> scheme.onTertiaryContainer
        7 -> scheme.onPrimary
        else -> Color.White
    }
    val uncheckedThumb = when (style) {
        0 -> colors.uncheckedThumbColor
        1, 2 -> scheme.outline
        3, 5, 6, 7 -> scheme.surface
        else -> Color.White
    }
    val checkedBorder = when (style) {
        2 -> scheme.primaryContainer
        3 -> scheme.secondary
        4 -> Color(0xFF34C759)
        5 -> scheme.tertiary
        6 -> Color(0xFF1684FC)
        else -> checkedTrack
    }
    val uncheckedBorder = when (style) {
        4 -> Color(0xFF8E8E93)
        3 -> scheme.outline
        else -> scheme.outlineVariant
    }
    // Exact outer slot dimensions from Voyage's j7w0.m105365O dispatch:
    // 0 z4v0.m147164o = 52x32; 1 frt0.m95463l = 52x32;
    // 2 it70.m103876l = 56x28; 3 ckw0.m9293l = 48x24;
    // 4 rdf.m127934l = 51x31; 5 rdf.m127933O -> m127932I = 64x28;
    // 6 y2t0.m144445l = 50x28.5; 7 l140.m110104l = 39x22.
    val trackSize = when (style) {
        2 -> DpSize(56.dp, 28.dp)
        3 -> DpSize(48.dp, 24.dp)
        4 -> DpSize(51.dp, 31.dp)
        5 -> DpSize(64.dp, 28.dp)
        6 -> DpSize(50.dp, 28.5.dp)
        7 -> DpSize(39.dp, 22.dp)
        else -> DpSize(52.dp, 32.dp)
    }
    // Thumb slots are style-specific in the original composables; interaction
    // expansion is driven by the shared interaction source, not checked state.
    val thumbDiameter = when (style) {
        0 -> 20.dp // z4v0.m147162ll: fixed 20dp thumb slot.
        1 -> when {
            pressed -> 28.dp // frt0.m95463l expands the thumb while interacting.
            checked || thumbContent != null -> 24.dp
            else -> 16.dp
        }
        2 -> 20.dp // it70.m103876l: 20dp thumb, separate from its 16dp icon.
        3 -> if (enabled && pressed) 17.dp else 12.dp // ckw0.m9293l.
        4, 5 -> 20.dp // rdf.m127934l / m127933O.
        6 -> if (enabled && pressed) 23.dp else 20.dp // y2t0.m144445l.
        7 -> 22.dp // l140.m110104l.
        else -> 20.dp
    }
    val thumbInset = ((trackSize.height - thumbDiameter) / 2f).coerceAtLeast(0.dp)
    val trackCornerRadius = when (style) {
        2 -> 14.dp
        3 -> 4.dp
        4 -> 15.5.dp
        5 -> 14.dp
        6 -> 14.25.dp
        7 -> 11.dp
        else -> 16.dp
    }
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 620f),
        label = "voyageSwitchThumb",
    )
    val trackShape = voyageCornerShape(trackCornerRadius)
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
            val radius = trackCornerRadius.toPx()
            drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = CornerRadius(radius, radius),
            )
            if (borderWidth > 0f) {
                drawRoundRect(
                    color = (if (checked) checkedBorder else uncheckedBorder).copy(alpha = if (enabled) 1f else 0.45f),
                    topLeft = Offset(borderWidth.dp.toPx() / 2f, borderWidth.dp.toPx() / 2f),
                    size = Size(w - borderWidth.dp.toPx(), h - borderWidth.dp.toPx()),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = borderWidth.dp.toPx()),
                )
            }
            val thumbRadius = thumbDiameter.toPx() / 2f
            val inset = thumbInset.toPx()
            val left = inset + thumbRadius
            val right = w - inset - thumbRadius
            val cx = left + (right - left) * travel
            val cy = h / 2f
            if (style == 5) {
                drawCircle(color = Color.White.copy(alpha = if (checked) 0.22f else 0.10f), radius = thumbRadius + 2.dp.toPx(), center = Offset(cx, cy))
            }
            drawCircle(
                color = if (enabled) {
                    if (checked) checkedThumb else uncheckedThumb
                } else if (style == 0) {
                    if (checked) colors.disabledCheckedThumbColor else colors.disabledUncheckedThumbColor
                } else {
                    (if (checked) checkedThumb else uncheckedThumb).copy(alpha = 0.65f)
                },
                radius = thumbRadius,
                center = Offset(cx, cy),
            )
            if (showThumbIcon && thumbContent == null && style in setOf(0, 2, 3, 5, 6, 7)) {
                val iconColor = if (checked) {
                    if (style == 0) colors.checkedIconColor else checkedTrack
                } else {
                    if (style == 0) colors.uncheckedIconColor else uncheckedTrack
                }
                val stroke = (h * 0.065f).coerceAtLeast(1.4.dp.toPx())
                if (checked) {
                    drawLine(iconColor, Offset(cx - thumbRadius * 0.38f, cy), Offset(cx - thumbRadius * 0.08f, cy + thumbRadius * 0.24f), stroke, StrokeCap.Round)
                    drawLine(iconColor, Offset(cx - thumbRadius * 0.08f, cy + thumbRadius * 0.24f), Offset(cx + thumbRadius * 0.42f, cy - thumbRadius * 0.30f), stroke, StrokeCap.Round)
                } else if (style == 2 || style == 3 || style == 6 || style == 7) {
                    drawLine(iconColor, Offset(cx - thumbRadius * 0.25f, cy - thumbRadius * 0.25f), Offset(cx + thumbRadius * 0.25f, cy + thumbRadius * 0.25f), stroke, StrokeCap.Round)
                    drawLine(iconColor, Offset(cx + thumbRadius * 0.25f, cy - thumbRadius * 0.25f), Offset(cx - thumbRadius * 0.25f, cy + thumbRadius * 0.25f), stroke, StrokeCap.Round)
                }
            }
        }
        if (thumbContent != null) {
            val thumbTravel = trackSize.width - thumbDiameter - (thumbInset * 2f)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = thumbTravel * travel - thumbTravel / 2f)
                    .size(thumbDiameter),
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
