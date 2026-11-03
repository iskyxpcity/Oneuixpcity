package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.blur
import com.alohaoneui.app.ui.component.liquid.lens
import com.alohaoneui.app.ui.component.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource

/**
 * Liquid Glass switch interaction:
 * - the track remains a quiet, theme-linked switch track;
 * - only the moving thumb receives the transient glass treatment;
 * - the thumb travels with a spring and settles with a restrained bounce;
 * - no whole-control scaling, permanent halo, or glass track.
 *
 * Backdrop sampling is limited to the thumb layer. It uses the existing
 * pager backdrop supplied by LocalVoyageSwitchBackdrop and never creates
 * another capture layer around the switch itself.
 */
@Composable
fun AlohaAgslGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val glassPulse = remember { Animatable(0f) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(checked) {
        if (firstComposition) {
            firstComposition = false
        } else {
            glassPulse.snapTo(0f)
            glassPulse.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.48f, stiffness = 560f),
            )
            glassPulse.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = 360f),
            )
        }
    }
    val glassProgress = maxOf(
        glassPulse.value,
        if (pressed && enabled) 0.72f else 0f,
    )
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = 520f,
            visibilityThreshold = 0.001f,
        ),
        label = "alohaLiquidGlassThumbTravel",
    )
    val scheme = MaterialTheme.colorScheme
    val trackColor = if (checked) {
        scheme.primaryContainer
    } else {
        scheme.surfaceContainerHigh
    }
    val backdrop = LocalVoyageSwitchBackdrop.current
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
            .size(width = 64.dp, height = 30.dp)
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.size(width = 64.dp, height = 30.dp)) {
            val radius = size.height / 2f
            drawRoundRect(
                color = trackColor.copy(alpha = if (enabled) 1f else 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
            // A restrained inner edge gives the track definition without
            // turning it into a permanent glass capsule.
            drawRoundRect(
                color = scheme.onSurface.copy(alpha = if (enabled) 0.055f else 0.025f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = 0.7.dp.toPx()),
            )
        }

        val thumbOffset = 3.dp + 34.dp * travel
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .graphicsLayer {
                    // Stretch along the direction of travel while the glass
                    // pulse is active, then spring back to a circular thumb.
                    scaleX = 1f + 0.34f * glassProgress
                    scaleY = 1f - 0.12f * glassProgress
                }
                .shadow(
                    elevation = if (pressed && enabled) 3.dp else 1.5.dp,
                    shape = CircleShape,
                    clip = false,
                )
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(
                        color = scheme.surface.copy(alpha = if (enabled) 1f else 0.48f),
                    )
                    drawCircle(
                        color = scheme.onSurface.copy(alpha = 0.08f),
                        style = Stroke(width = 0.65.dp.toPx()),
                    )
                },
        ) {
            // The lens is strictly an interaction overlay on the thumb.
            // At rest this layer is transparent and the thumb is an ordinary
            // theme-colored handle, not a permanently frosted glass button.
            if (backdrop != null && enabled && glassProgress > 0.001f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(1.6.dp.toPx(), 1.6.dp.toPx())
                                lens(
                                    refractionHeight = 16.dp.toPx(),
                                    refractionAmount = 18.dp.toPx(),
                                    depthEffect = true,
                                    chromaticAberration = 0.24f,
                                )
                            },
                            highlight = {
                                glassHighlight.copy(alpha = 0.96f * glassProgress)
                            },
                            onDrawSurface = {
                                drawCircle(
                                    Color.White.copy(alpha = 0.16f * glassProgress)
                                )
                            },
                        ),
                )
            }
            if (backdrop == null && enabled && glassProgress > 0.001f) {
                // Visible transient lens treatment when the app's optional
                // live backdrop capture is disabled. This is a procedural
                // glass highlight, not a claim of true scene refraction.
                Canvas(Modifier.matchParentSize()) {
                    val p = glassProgress.coerceIn(0f, 1f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.13f * p),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.48f * p),
                        style = Stroke(width = (1.25f * p).dp.toPx()),
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.72f * p),
                        startAngle = 202f,
                        sweepAngle = 116f,
                        useCenter = false,
                        style = Stroke(width = (1.35f * p).dp.toPx()),
                    )
                    drawArc(
                        color = Color(0xFFB8DFFF).copy(alpha = 0.22f * p),
                        startAngle = 18f,
                        sweepAngle = 92f,
                        useCenter = false,
                        style = Stroke(width = (1.1f * p).dp.toPx()),
                    )
                }
            }
        }
    }
}
