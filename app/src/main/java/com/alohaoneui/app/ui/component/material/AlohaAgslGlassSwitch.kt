package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.alohaoneui.app.ui.component.liquid.lens
import com.alohaoneui.app.ui.component.liquid.vibrancy
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource

/**
 * Liquid Glass switch adapted from Kyant0/AndroidLiquidGlass LiquidToggle.
 *
 * The track stays theme-linked and fixed. The thumb uses damped drag,
 * velocity-aware spring settling, press deformation and transient lens optics.
 * Only the existing parent backdrop is sampled; this component never creates
 * a pager-level capture and therefore cannot introduce a recursive capture.
 */
@Composable
fun AlohaAgslGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
) {
    val scope = rememberCoroutineScope()
    val pressed by interactionSource.collectIsPressedAsState()
    val fraction = remember { Animatable(if (checked) 1f else 0f, 0.001f) }
    val pressProgress = remember { Animatable(0f, 0.001f) }
    val thumbScaleX = remember { Animatable(1f, 0.001f) }
    val thumbScaleY = remember { Animatable(1f, 0.001f) }
    val velocityTracker = remember { VelocityTracker() }
    var dragging by remember { mutableStateOf(false) }
    var dragVelocity by remember { mutableStateOf(0f) }

    val valueSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 520f,
        visibilityThreshold = 0.001f,
    )
    val pressSpring = spring<Float>(
        dampingRatio = 1f,
        stiffness = 1000f,
        visibilityThreshold = 0.001f,
    )
    val scaleXSpring = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 250f,
        visibilityThreshold = 0.001f,
    )
    val scaleYSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 250f,
        visibilityThreshold = 0.001f,
    )

    LaunchedEffect(checked, dragging) {
        val target = if (checked) 1f else 0f
        if (!dragging && fraction.value != target) {
            fraction.animateTo(target, valueSpring)
        }
    }

    LaunchedEffect(pressed, enabled) {
        if (pressed && enabled) {
            launch { pressProgress.animateTo(1f, pressSpring) }
            launch { thumbScaleX.animateTo(1.5f, scaleXSpring) }
            launch { thumbScaleY.animateTo(1.5f, scaleYSpring) }
        } else {
            launch { pressProgress.animateTo(0f, pressSpring) }
            launch { scaleX.animateTo(1f, scaleXSpring) }
            launch { scaleY.animateTo(1f, scaleYSpring) }
        }
    }

    val scheme = MaterialTheme.colorScheme
    val trackColor = if (checked) scheme.primaryContainer else scheme.surfaceContainerHigh
    val backdrop = LocalVoyageSwitchBackdrop.current
    val glassProgress = pressProgress.value.coerceIn(0f, 1f)
    val glassHighlight = remember {
        Highlight(
            width = 1.dp,
            alpha = 1f,
            style = BloomStroke(
                color = Color.White.copy(alpha = 0.24f),
                innerBlurRadius = 2.dp,
                primaryLight = LightSource(
                    position = LightPosition(0.28f, -0.24f, -0.05f),
                    color = Color.White,
                    intensity = 1.15f,
                ),
                secondaryLight = LightSource(
                    position = LightPosition(0.72f, 0.92f, -0.4f),
                    color = Color.White,
                    intensity = 0.42f,
                ),
            ),
        )
    }

    Box(
        modifier = modifier
            .size(width = 64.dp, height = 28.dp)
            .clickable(
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
            ) {
                onCheckedChange?.invoke(!checked)
            }
            .pointerInput(enabled, onCheckedChange) {
                if (!enabled || onCheckedChange == null) return@pointerInput
                val dragWidthPx = 20.dp.toPx()
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        dragVelocity = 0f
                        velocityTracker.resetTracking()
                        scope.launch {
                            fraction.stop()
                            launch { pressProgress.animateTo(1f, pressSpring) }
                            launch { scaleX.animateTo(1.5f, scaleXSpring) }
                            launch { scaleY.animateTo(1.5f, scaleYSpring) }
                        }
                    },
                    onDragEnd = {
                        val target = if (fraction.value >= 0.5f) 1f else 0f
                        dragging = false
                        onCheckedChange(target == 1f)
                        scope.launch {
                            fraction.animateTo(
                                target,
                                valueSpring,
                                initialVelocity = dragVelocity * 0.75f,
                            )
                            launch { pressProgress.animateTo(0f, pressSpring) }
                            launch { scaleX.animateTo(1f, scaleXSpring) }
                            launch { scaleY.animateTo(1f, scaleYSpring) }
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch {
                            fraction.animateTo(if (checked) 1f else 0f, valueSpring)
                            launch { pressProgress.animateTo(0f, pressSpring) }
                            launch { scaleX.animateTo(1f, scaleXSpring) }
                            launch { scaleY.animateTo(1f, scaleYSpring) }
                        }
                    },
                    onDrag = { change: PointerInputChange, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        dragVelocity = velocityTracker.calculateVelocity().x / dragWidthPx
                        scope.launch {
                            fraction.snapTo((fraction.value + dragAmount.x / dragWidthPx).fastCoerceIn(0f, 1f))
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.size(width = 64.dp, height = 28.dp)) {
            val radius = size.height / 2f
            drawRoundRect(
                color = trackColor.copy(alpha = if (enabled) 1f else 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
            drawRoundRect(
                color = scheme.onSurface.copy(alpha = if (enabled) 0.055f else 0.025f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = 0.7.dp.toPx()),
            )
        }

        val thumbOffset = 2.dp + 20.dp * fraction.value
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(width = 40.dp, height = 24.dp)
                .graphicsLayer {
                    val velocityX = (dragVelocity / 50f * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                    val velocityY = (dragVelocity / 50f * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    scaleX = thumbScaleX.value / (1f - velocityX)
                    scaleY = thumbScaleY.value * (1f - velocityY)
                }
                .shadow(
                    elevation = if (pressed || dragging) 4.dp else 1.5.dp,
                    shape = RoundedCornerShape(50),
                    clip = false,
                )
                .clip(RoundedCornerShape(50))
                .drawBehind {
                    drawRoundRect(
                        color = scheme.surface.copy(alpha = if (enabled) 1f else 0.48f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                    )
                    drawRoundRect(
                        color = scheme.onSurface.copy(alpha = 0.08f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                        style = Stroke(width = 0.65.dp.toPx()),
                    )
                },
        ) {
            if (backdrop != null && enabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(50) },
                            effects = {
                                blur(8.dp.toPx() * (1f - glassProgress))
                                lens(
                                    refractionHeight = 5.dp.toPx() * glassProgress,
                                    refractionAmount = 10.dp.toPx() * glassProgress,
                                    depthEffect = true,
                                    chromaticAberration = 0.24f,
                                )
                            },
                            highlight = {
                                glassHighlight.copy(alpha = glassProgress)
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 1f - glassProgress))
                            },
                        ),
                )
            } else if (enabled && glassProgress > 0.001f) {
                Canvas(Modifier.matchParentSize()) {
                    val p = glassProgress
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.16f * p),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.52f * p),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                        style = Stroke(width = (1.2f * p).dp.toPx()),
                    )
                }
            }
        }
    }
}
