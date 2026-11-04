package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch

/**
 * Material-only Liquid Glass switch, using Kyant's standalone Backdrop engine.
 * No Miuix theme, component, or blur API is used by this control.
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
        label = "materialLiquidGlassPress",
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
    val trackBackdrop = rememberLayerBackdrop()
    val sceneBackdrop = LocalVoyageSwitchBackdrop.current
    val thumbBackdrop = if (sceneBackdrop != null) {
        rememberCombinedBackdrop(sceneBackdrop, trackBackdrop)
    } else {
        trackBackdrop
    }

    val trackTint = if (checked) scheme.primaryContainer else scheme.surfaceContainerHigh
    val thumbTint = if (checked) scheme.onPrimaryContainer else scheme.surface
    val thumbX = if (isLtr) 2.dp + 20.dp * fraction.value else 22.dp - 20.dp * fraction.value

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
        // Capture only the track surface. The thumb and parent Pager are excluded.
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
                                blur(8.dp.toPx() * (1f - pressProgress))
                                lens(
                                    refractionHeight = 5.dp.toPx() * pressProgress,
                                    refractionAmount = 10.dp.toPx() * pressProgress,
                                    depthEffect = true,
                                    chromaticAberration = pressProgress > 0.001f,
                                )
                            },
                            highlight = {
                                Highlight.Ambient.copy(
                                    width = Highlight.Ambient.width / 1.5f,
                                    blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                    alpha = pressProgress,
                                )
                            },
                            shadow = {
                                Shadow(
                                    radius = 4.dp,
                                    color = Color.Black.copy(alpha = 0.05f),
                                )
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = 4.dp * pressProgress,
                                    alpha = pressProgress,
                                )
                            },
                            layerBlock = {
                                scaleX = 1f + 0.10f * pressProgress
                                scaleY = 1f - 0.035f * pressProgress
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 1f - pressProgress))
                                drawRect(thumbTint.copy(alpha = 0.08f + 0.06f * pressProgress))
                            },
                        )
                    } else {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = scheme.surface.copy(alpha = 0.42f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                    size.height / 2f,
                                    size.height / 2f,
                                ),
                            )
                        }
                    },
                ),
        )
    }
}
