package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import com.alohaoneui.app.ui.component.liquid.lens
import com.alohaoneui.app.ui.component.liquid.rememberCombinedBackdrop
import com.alohaoneui.app.ui.component.liquid.vibrancy

/**
 * Liquid Glass switch based on Kyant0/AndroidLiquidGlass LiquidToggle.
 *
 * The scene backdrop supplied by MainActivity is an isolated background-only
 * layer. The track has its own capture layer, so the moving lens never samples
 * a layer that contains itself or another switch.
 */
@Composable
fun AlohaAgslGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
) {
    val scheme = MaterialTheme.colorScheme
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "alohaLiquidGlassPress",
    )

    val density = LocalDensity.current
    val dragWidthPx = with(density) { 20.dp.toPx() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragDirection = if (isLtr) 1f else -1f
    val scope = rememberCoroutineScope()
    val fraction = remember { Animatable(if (checked) 1f else 0f) }

    LaunchedEffect(checked) {
        fraction.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        )
    }

    val dragState = rememberDraggableState { delta ->
        scope.launch {
            fraction.snapTo(
                (fraction.value + delta / dragWidthPx * dragDirection).coerceIn(0f, 1f),
            )
        }
    }

    val trackShape = voyageCornerShape(14.dp)
    val thumbShape = RoundedCornerShape(50)
    val trackBackdrop = rememberLayerBackdrop { drawContent() }
    val sceneBackdrop = LocalVoyageSwitchBackdrop.current
    val thumbBackdrop = if (sceneBackdrop != null) {
        rememberCombinedBackdrop(sceneBackdrop, trackBackdrop)
    } else {
        trackBackdrop
    }

    val trackTint = if (checked) {
        scheme.primaryContainer
    } else {
        scheme.surfaceContainerHigh
    }
    val thumbTint = if (checked) {
        scheme.onPrimaryContainer
    } else {
        scheme.surface
    }
    val thumbX = if (isLtr) {
        2.dp + 20.dp * fraction.value
    } else {
        22.dp - 20.dp * fraction.value
    }

    Box(
        modifier = modifier
            .size(width = 64.dp, height = 28.dp)
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) },
            )
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = enabled && onCheckedChange != null,
                onDragStopped = {
                    val target = fraction.value >= 0.5f
                    onCheckedChange?.invoke(target)
                    scope.launch {
                        fraction.animateTo(
                            if (target) 1f else 0f,
                            animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
                        )
                    }
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // The captured track is deliberately a separate layer. It contains
        // only the track surface, never the thumb or the surrounding Pager.
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 28.dp)
                .layerBackdrop(trackBackdrop)
                .drawBehind {
                    val radius = size.height / 2f
                    drawRoundRect(
                        color = trackTint.copy(alpha = if (enabled) 0.28f else 0.12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    )
                    drawRoundRect(
                        color = scheme.outlineVariant.copy(alpha = if (enabled) 0.34f else 0.16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                        style = Stroke(width = 0.7.dp.toPx()),
                    )
                },
        )

        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .size(width = 40.dp, height = 24.dp)
                .shadow(
                    elevation = if (pressed && enabled) 3.dp else 1.5.dp,
                    shape = thumbShape,
                    clip = false,
                )
                .then(
                    if (enabled) {
                        Modifier.drawBackdrop(
                            backdrop = thumbBackdrop,
                            shape = { thumbShape },
                            effects = {
                                vibrancy()
                                val blurAmount = 8.dp.toPx() * (1f - pressProgress)
                                blur(blurAmount, blurAmount)
                                lens(
                                    refractionHeight = 5.dp.toPx() * pressProgress,
                                    refractionAmount = 10.dp.toPx() * pressProgress,
                                    depthEffect = true,
                                    chromaticAberration = 0.42f * pressProgress,
                                )
                            },
                            highlight = {
                                Highlight.Ambient.copy(
                                    alpha = 0.58f + 0.28f * pressProgress,
                                )
                            },
                            onDrawSurface = {
                                drawRect(
                                    color = thumbTint.copy(alpha = 0.12f + 0.08f * pressProgress),
                                )
                            },
                            layerBlock = {
                                scaleX = 1f + 0.10f * pressProgress
                                scaleY = 1f - 0.035f * pressProgress
                            },
                        )
                    } else {
                        Modifier
                            .background(
                                color = scheme.surface.copy(alpha = 0.42f),
                                shape = thumbShape,
                            )
                    },
                ),
        )
    }
}
