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
    val containerColor: Color,
    val thumbSize: DpSize,
    val trackHeight: androidx.compose.ui.unit.Dp,
)

@Composable
internal fun voyageSliderAppearance(requestedStyle: Int): VoyageSliderAppearance {
    val style = requestedStyle.coerceIn(0, 3)
    val scheme = MaterialTheme.colorScheme
    // All four families use the same theme-driven progress/thumb accent; their
    // rail geometry is taken from the corresponding upstream slider family.
    val active = scheme.primary
    val inactive = scheme.surfaceContainerHighest
    val container = when (style) {
        0 -> scheme.tertiaryContainer.copy(alpha = 0.20f) // Material You
        1 -> scheme.secondaryContainer.copy(alpha = 0.30f) // Fancy
        2 -> scheme.tertiaryContainer.copy(alpha = 0.20f) // Material 2
        else -> scheme.secondaryContainer.copy(alpha = 0.30f) // HyperOS
    }
    val thumbSize = when (style) {
        1 -> DpSize(26.dp, 26.dp) // FancySlider source
        else -> DpSize(20.dp, 20.dp) // M3/M2 default thumb slot; HyperOS hides it
    }
    val trackHeight = when (style) {
        1 -> 38.dp // FancyTrack source
        2 -> 4.dp  // M2Slider source
        3 -> 30.dp // HyperOSSlider source
        else -> 4.dp // Material 3 default track
    }
    return VoyageSliderAppearance(
        style = style,
        activeColor = active,
        inactiveColor = inactive,
        containerColor = container,
        thumbSize = thumbSize,
        trackHeight = trackHeight,
    )
}

/** The source families wrap their track differently; keep that outer surface
 * separate from the active/inactive rail so the rail is never painted in the
 * same color as the thumb. HyperOS wraps the 30dp track itself. */
internal fun voyageSliderContainerModifier(
    style: Int,
    containerColor: Color,
): Modifier {
    val containerShape = voyageCornerShape(20.dp)
    return when (style) {
        0 -> Modifier
            .padding(vertical = 2.dp)
            .background(containerColor, containerShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
        1 -> Modifier
            .background(containerColor, voyageCornerShape(50.dp))
            .padding(horizontal = 6.dp)
        2 -> Modifier
            .background(containerColor, voyageCornerShape(50.dp))
            .padding(horizontal = 12.dp)
        else -> Modifier
    }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(voyageSliderContainerModifier(style, appearance.containerColor)),
                    onValueChangeFinished = onValueChangeFinished,
                    interactionSource = interactionSource,
                    thumb = { thumbState ->
                        val valueFraction = if (valueRange.endInclusive > valueRange.start) {
                            ((thumbState.value - valueRange.start) /
                                (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                        } else 0f
                        VoyageSliderThumb(
                            style = style,
                            size = thumbSize,
                            activeColor = active,
                            inactiveColor = inactive,
                            shadowEnabled = LocalMaterialShadowSliders.current,
                            interactionSource = interactionSource,
                            valueFraction = valueFraction,
                        )
                    },
                    track = { state ->
                        VoyageSliderTrack(
                            style = style,
                            state = state,
                            range = valueRange,
                            steps = steps,
                            height = trackHeight,
                            containerColor = appearance.containerColor,
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

        // Preserve discrete tick marks for all four styles. Material 2 draws
        // its marks on the classic line above; the other families align marks
        // to the center of their own track channel.
        if (steps > 0 && style != 2) {
            val count = steps + 1
            val tickInset = when (style) {
                1 -> (insetPx * 0.65f).coerceAtLeast(1f)
                3 -> (insetPx * 0.45f).coerceAtLeast(1f)
                else -> insetPx
            }
            val tickWidth = (width - tickInset * 2f).coerceAtLeast(1f)
            for (i in 0..count) {
                val x = tickInset + tickWidth * i / count
                drawCircle(
                    color = if (i.toFloat() / count <= fraction) passedTickColor else futureTickColor,
                    radius = 1.dp.toPx(),
                    center = Offset(x, h / 2f),
                )
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

