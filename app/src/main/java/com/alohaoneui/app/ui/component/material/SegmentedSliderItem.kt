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
    val innerInset: androidx.compose.ui.unit.Dp,
)

@Composable
internal fun voyageSliderAppearance(requestedStyle: Int): VoyageSliderAppearance {
    val style = requestedStyle.coerceIn(0, 3)
    val scheme = MaterialTheme.colorScheme
    // Keep the four named Voyage families visually distinct. The enclosing rail
    // uses a secondary/tertiary container tone; it must never reuse the thumb fill.
    val active = when (style) {
        1 -> scheme.tertiary
        2 -> scheme.secondary
        3 -> scheme.primary
        else -> scheme.primary
    }
    val inactive = when (style) {
        1, 3 -> scheme.tertiaryContainer
        else -> scheme.secondaryContainer
    }
    // Voyage uses a visibly substantial outer rail (32dp), not a thin line.
    // Per-family inset and thumb proportions differentiate Material You/Fancy/
    // Material/Hyper OS while the shared Voyage shape setting still controls shape.
    val trackHeight = 32.dp
    val thumbSize = when (style) {
        1 -> DpSize(22.dp, 22.dp) // Fancy: slightly larger thumb.
        2 -> DpSize(18.dp, 18.dp) // Material: compact thumb.
        3 -> DpSize(20.dp, 24.dp) // Hyper OS: vertically elongated thumb.
        else -> DpSize(20.dp, 20.dp) // Material You.
    }
    val innerInset = when (style) {
        1 -> 3.dp
        2 -> 5.dp
        3 -> 4.dp
        else -> 4.dp
    }
    return VoyageSliderAppearance(
        style = style,
        activeColor = active,
        inactiveColor = inactive,
        thumbSize = thumbSize,
        trackHeight = trackHeight,
        innerInset = innerInset,
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
    val trackInset = appearance.innerInset
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
                            innerInset = trackInset,
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
    innerInset: androidx.compose.ui.unit.Dp,
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
    // All four styles keep a roomy 32dp color-container behind the rail,
    // but the rail geometry is style-specific rather than one shared capsule.
    // The original Voyage DEX methods are still being matched; these branches
    // are deliberately isolated so each can be corrected against the source.
    val containerShape = voyageCornerShape(height / 2f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height)
            .then(if (shadowEnabled) Modifier.shadow(1.dp, containerShape, clip = false) else Modifier)
    ) {
        val h = size.height
        val insetPx = innerInset.toPx()
        val fraction = if (range.endInclusive > range.start) {
            ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        } else 0f
        val outlinePx = outlineWidth.toPx()
        val width = size.width

        // Secondary/tertiary container is a separate outer surface; it never
        // shares the thumb's accent fill. Global Voyage shape/corner scale
        // continues to control the outside silhouette.
        drawVoyageOutline(
            containerShape.createOutline(Size(width, h), layoutDirection, this),
            inactiveColor,
        )

        when (style) {
            0 -> {
                // Material You: inset, full-height pill rail with a pill-shaped
                // active fill. This is intentionally not the thin Material 2 line.
                val left = insetPx
                val railWidth = (width - 2f * insetPx).coerceAtLeast(1f)
                val railHeight = (h - 2f * insetPx).coerceAtLeast(1f)
                val railShape = voyageCornerShape(railHeight.toDp() / 2f)
                val railOutline = railShape.createOutline(Size(railWidth, railHeight), layoutDirection, this)
                withTransform({ translate(left, insetPx) }) {
                    drawVoyageOutline(railOutline, inactiveColor.copy(alpha = 0.62f))
                    val activeWidth = railWidth * fraction
                    if (activeWidth > 0f) {
                        clipRect(left = 0f, top = 0f, right = activeWidth, bottom = railHeight) {
                            drawVoyageOutline(railOutline, activeColor)
                        }
                    }
                    if (outlinePx > 0f) drawVoyageOutline(railOutline, outlineColor, Stroke(outlinePx))
                }
            }
            1 -> {
                // Fancy: a broad inset rail with a slimmer centered channel;
                // the active end is clipped from the same channel to avoid
                // changing the surrounding secondary/tertiary container.
                val channelHeight = (h * 0.56f).coerceAtLeast(1f)
                val top = (h - channelHeight) / 2f
                val left = (insetPx * 0.65f).coerceAtLeast(1f)
                val channelWidth = (width - left * 2f).coerceAtLeast(1f)
                val channelShape = voyageCornerShape(channelHeight.toDp() / 2f)
                val channelOutline = channelShape.createOutline(Size(channelWidth, channelHeight), layoutDirection, this)
                withTransform({ translate(left, top) }) {
                    drawVoyageOutline(channelOutline, inactiveColor.copy(alpha = 0.72f))
                    val activeWidth = channelWidth * fraction
                    if (activeWidth > 0f) {
                        clipRect(left = 0f, top = 0f, right = activeWidth, bottom = channelHeight) {
                            drawVoyageOutline(channelOutline, activeColor)
                        }
                    }
                    if (outlinePx > 0f) drawVoyageOutline(channelOutline, outlineColor, Stroke(outlinePx))
                }
            }
            2 -> {
                // Material 2: classic narrow 4dp active/inactive line centered
                // inside the wider colored holder; circular end caps are retained.
                val railHeight = 4.dp.toPx()
                val top = (h - railHeight) / 2f
                val left = insetPx
                val railWidth = (width - 2f * insetPx).coerceAtLeast(1f)
                drawRoundRect(
                    color = inactiveColor.copy(alpha = 0.78f),
                    topLeft = Offset(left, top),
                    size = Size(railWidth, railHeight),
                    cornerRadius = CornerRadius(railHeight / 2f, railHeight / 2f),
                )
                val activeWidth = railWidth * fraction
                if (activeWidth > 0f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(left, top),
                        size = Size(activeWidth, railHeight),
                        cornerRadius = CornerRadius(railHeight / 2f, railHeight / 2f),
                    )
                }
                if (steps > 0) {
                    val count = steps + 1
                    for (i in 0..count) {
                        val x = left + railWidth * i / count
                        drawCircle(
                            color = if (i.toFloat() / count <= fraction) passedTickColor else futureTickColor,
                            radius = 1.dp.toPx(),
                            center = Offset(x, h / 2f),
                        )
                    }
                }
            }
            else -> {
                // HyperOS: thick, edge-to-edge pill channel. It has a separate
                // geometry branch so its filled track does not inherit Fancy's
                // inset channel or Material 2's hairline rail.
                val left = (insetPx * 0.45f).coerceAtLeast(1f)
                val railWidth = (width - 2f * left).coerceAtLeast(1f)
                val railHeight = (h * 0.78f).coerceAtLeast(1f)
                val top = (h - railHeight) / 2f
                val railShape = voyageCornerShape(railHeight.toDp() / 2f)
                val railOutline = railShape.createOutline(Size(railWidth, railHeight), layoutDirection, this)
                withTransform({ translate(left, top) }) {
                    drawVoyageOutline(railOutline, inactiveColor.copy(alpha = 0.82f))
                    val activeWidth = railWidth * fraction
                    if (activeWidth > 0f) {
                        clipRect(left = 0f, top = 0f, right = activeWidth, bottom = railHeight) {
                            drawVoyageOutline(railOutline, activeColor)
                        }
                    }
                    if (outlinePx > 0f) drawVoyageOutline(railOutline, outlineColor, Stroke(outlinePx))
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

