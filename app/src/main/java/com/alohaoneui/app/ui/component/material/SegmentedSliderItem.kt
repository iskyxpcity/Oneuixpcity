package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialSliderStyle
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSliders
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth

/**
 * Four selectable slider component families. Interaction, range handling and
 * discrete snapping are delegated to Material's SliderState; each family's
 * visible track and thumb are drawn here so styling is not limited to the
 * stock Material thumb/track defaults.
 */

/** Shared style inputs for both the settings-row and generic Material sliders.
 * Values in this adapter preserve the current app mapping until each original
 * Voyage branch is fully matched to its decompiled draw path.
 */
internal data class VoyageSliderAppearance(
    val style: Int,
    val activeColor: Color,
    val inactiveColor: Color,
    val thumbSize: DpSize,
    val trackHeight: androidx.compose.ui.unit.Dp,
)

@Composable
internal fun voyageSliderAppearance(requestedStyle: Int): VoyageSliderAppearance {
    val style = requestedStyle.coerceIn(0, 3)
    val scheme = MaterialTheme.colorScheme
    val active = when (style) {
        1 -> scheme.tertiary
        2 -> scheme.secondary
        else -> scheme.primary
    }
    // Slider's unfilled track uses the same neutral container tone as the
    // switch's off-state track, while preserving theme/dynamic color updates.
    val inactive = scheme.surfaceContainerHighest
    // The neutral track is an outer capsule that surrounds the thumb, like a
    // switch track. The active progress is drawn as an inset inner rail.
    val trackHeight = 24.dp
    return VoyageSliderAppearance(
        style = style,
        activeColor = active,
        inactiveColor = inactive,
        thumbSize = DpSize(20.dp, 20.dp),
        trackHeight = trackHeight,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedSliderItem(
    icon: ImageVector? = null,
    title: String? = null,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val appearance = voyageSliderAppearance(LocalMaterialSliderStyle.current)
    val style = appearance.style
    val active = appearance.activeColor
    val inactive = appearance.inactiveColor
    // Source check: j5v0.m105238O and z4v0.m147159l both apply
    // Modifier.size(20.dp, 20.dp) to the thumb hit/visual slot.
    // The 16dp and 32dp values belong to separate track containers, not thumbs.
    val thumbSize = appearance.thumbSize
    val trackHeight = appearance.trackHeight
    val interactionSource = remember { MutableInteractionSource() }
    val sliderState = rememberSliderState(
        value = value,
        steps = steps,
        trackRange = valueRange,
    )
    LaunchedEffect(value, valueRange, steps) {
        sliderState.value = value.coerceIn(valueRange.start, valueRange.endInclusive)
    }

    SegmentedListItem(
        leadingContent = icon?.let { imageVector ->
            {
                Icon(imageVector = imageVector, contentDescription = title)
            }
        },
        headlineContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                if (!title.isNullOrEmpty()) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Slider(
                    state = sliderState,
                    onValueChange = { newValue ->
                        sliderState.value = newValue
                        onValueChange(newValue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onValueChangeFinished = onValueChangeFinished,
                    interactionSource = interactionSource,
                    thumb = {
                        VoyageSliderThumb(
                            style = style,
                            size = thumbSize,
                            activeColor = active,
                            inactiveColor = inactive,
                            shadowEnabled = LocalMaterialShadowSliders.current,
                            interactionSource = interactionSource,
                        )
                    },
                    track = { state ->
                        VoyageSliderTrack(
                            style = style,
                            state = state,
                            range = valueRange,
                            steps = steps,
                            height = trackHeight,
                            thumbWidth = thumbSize.width,
                            activeColor = active,
                            inactiveColor = inactive,
                            outlineWidth = LocalMaterialOutlineWidth.current.dp,
                            outlineColor = MaterialTheme.colorScheme.outlineVariant,
                            shadowEnabled = LocalMaterialShadowSliders.current,
                        )
                    },
                )
            }
        },
    )
}

@Composable
internal fun VoyageSliderThumb(
    style: Int,
    size: DpSize,
    activeColor: Color,
    inactiveColor: Color,
    shadowEnabled: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    // Voyage's vhf.m138081l uses 1dp at rest and 6dp while its interaction
    // list is non-empty. Slider drags are DragInteraction events, not Press
    // events, so observe the full set of supported interaction states here.
    val effectiveInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by effectiveInteractionSource.collectIsPressedAsState()
    val dragged by effectiveInteractionSource.collectIsDraggedAsState()
    val hovered by effectiveInteractionSource.collectIsHoveredAsState()
    val focused by effectiveInteractionSource.collectIsFocusedAsState()
    val interacting = pressed || dragged || hovered || focused

    // Keep the 20dp source thumb slot, but use the shared Voyage shape factory
    // for its actual fill, clip and shadow silhouette. Shape style and corner
    // scale changes therefore update the thumb itself, not just its parent.
    val visualThumbSize = DpSize(
        width = (size.width - 4.dp).coerceAtLeast(1.dp),
        height = (size.height - 4.dp).coerceAtLeast(1.dp),
    )
    val thumbShape = voyageCornerShape(minOf(visualThumbSize.width, visualThumbSize.height) / 2f)
    Box(
        Modifier.size(width = size.width, height = size.height),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        val thumbModifier = Modifier
            .size(width = visualThumbSize.width, height = visualThumbSize.height)
            .then(
                if (shadowEnabled) {
                    Modifier.shadow(
                        elevation = if (interacting) 6.dp else 1.dp,
                        shape = thumbShape,
                        clip = false,
                    )
                } else Modifier
            )
            .clip(thumbShape)
            .background(activeColor, thumbShape)
        Box(thumbModifier) {}
    }
}

@Composable
internal fun VoyageSliderTrack(
    style: Int,
    state: SliderState,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    height: androidx.compose.ui.unit.Dp,
    thumbWidth: androidx.compose.ui.unit.Dp,
    activeColor: Color,
    inactiveColor: Color,
    outlineWidth: androidx.compose.ui.unit.Dp,
    outlineColor: Color,
    shadowEnabled: Boolean = false,
) {
    val passedTickColor = when (style) {
        1 -> MaterialTheme.colorScheme.onTertiary
        2 -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val futureTickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    // Voyage's vhf.m138081l applies elevation to the 20dp thumb, while
    // vhf.m138077ll draws the track segments and ticks without a track shadow.
    // Keep the track linked to the shared shape system without adding elevation
    // that is not present in the source draw path.
    val trackShape = voyageCornerShape(height / 2f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height)
            .then(if (shadowEnabled) Modifier.shadow(1.dp, trackShape, clip = false) else Modifier)
    ) {
        val h = size.height
        // The neutral container is the full outer rail. The thumb travels inside
        // this capsule rather than sitting over a thin line as in stock Material.
        val outerOutline = trackShape.createOutline(
            size = Size(size.width, h),
            layoutDirection = layoutDirection,
            density = this,
        )
        drawVoyageOutline(outerOutline, inactiveColor)

        val innerInset = 4.dp.toPx()
        val innerHeight = (h - innerInset * 2f).coerceAtLeast(1f)
        val innerWidth = (size.width - innerInset * 2f).coerceAtLeast(1f)
        val fraction = if (range.endInclusive > range.start) {
            ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        } else 0f
        val activeWidthPx = innerWidth * fraction
        val innerShape = voyageCornerShape(with(this) { (innerHeight / density).dp / 2f })
        val innerOutline = innerShape.createOutline(
            size = Size(innerWidth, innerHeight),
            layoutDirection = layoutDirection,
            density = this,
        )
        withTransform({ translate(innerInset, innerInset) }) {
            if (activeWidthPx >= innerHeight) {
                val activeOutline = innerShape.createOutline(
                    size = Size(activeWidthPx, innerHeight),
                    layoutDirection = layoutDirection,
                    density = this,
                )
                drawVoyageOutline(activeOutline, activeColor)
            } else if (activeWidthPx > 0f) {
                clipRect(left = 0f, top = 0f, right = activeWidthPx, bottom = innerHeight) {
                    drawVoyageOutline(innerOutline, activeColor)
                }
            }

            val outlinePx = outlineWidth.toPx()
            if (outlinePx > 0f) {
                drawVoyageOutline(
                    outline = innerOutline,
                    color = outlineColor,
                    style = Stroke(width = outlinePx),
                )
            }

            if (steps > 0) {
                val count = steps + 1
                for (i in 0..count) {
                    val x = if (count > 0) innerWidth * i / count else 0f
                    drawCircle(
                        color = if (x <= activeWidthPx) passedTickColor else futureTickColor,
                        radius = 1.dp.toPx(),
                        center = Offset(x, innerHeight / 2f),
                    )
                }
            }
        }
    }
}

/**
 * Draw a Compose Shape outline using the Canvas primitives available in the
 * project's pinned Compose version. Rounded outlines are converted to a Path
 * so asymmetric corners and custom Voyage paths are preserved.
 */
internal fun DrawScope.drawVoyageOutline(
    outline: Outline,
    color: Color,
    style: DrawStyle = Fill,
) {
    when (outline) {
        is Outline.Rectangle -> drawRect(
            color = color,
            topLeft = outline.rect.topLeft,
            size = outline.rect.size,
            style = style,
        )
        is Outline.Rounded -> drawPath(
            path = Path().apply { addRoundRect(outline.roundRect) },
            color = color,
            style = style,
        )
        is Outline.Generic -> drawPath(
            path = outline.path,
            color = color,
            style = style,
        )
    }
}

