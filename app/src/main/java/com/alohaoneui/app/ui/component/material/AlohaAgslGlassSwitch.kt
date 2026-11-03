package com.alohaoneui.app.ui.component.material

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialShapeStyle
import top.yukonga.miuix.kmp.blur.Backdrop
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
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = 520f,
            visibilityThreshold = 0.001f,
        ),
        label = "alohaLiquidGlassThumbTravel",
    )
    val glassProgress by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 560f),
        label = "alohaLiquidGlassThumbInteraction",
    )
    val scheme = MaterialTheme.colorScheme
    val trackColor = if (checked) {
        scheme.primaryContainer
    } else {
        scheme.surfaceContainerHigh
    }
    val thumbColor = if (checked) scheme.onPrimaryContainer else scheme.onSurface
    val backdrop = LocalVoyageSwitchBackdrop.current
    val trackShape = remember(LocalMaterialShapeStyle.current) {
        voyageCornerShape(15.dp)
    }
    val glassHighlight = remember {
        Highlight(
            width = 1.dp,
            alpha = 1f,
            style = BloomStroke(
                color = Color.White.copy(alpha = 0.18f),
                innerBlurRadius = 1.5.dp,
                primaryLight = LightSource(
                    position = LightPosition(0.28f, -0.24f, -0.05f),
                    color = Color.White,
                    intensity = 0.9f,
                ),
                secondaryLight = LightSource(
                    position = LightPosition(0.72f, 0.92f, -0.4f),
                    color = Color.White,
                    intensity = 0.28f,
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
                .shadow(
                    elevation = if (pressed && enabled) 3.dp else 1.5.dp,
                    shape = CircleShape,
                    clip = false,
                )
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(
                        color = thumbColor.copy(alpha = if (enabled) 1f else 0.48f),
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
                                blur(2.5.dp.toPx(), 2.5.dp.toPx())
                                lens(
                                    refractionHeight = 11.dp.toPx(),
                                    refractionAmount = 8.dp.toPx(),
                                    depthEffect = true,
                                    chromaticAberration = 0.12f,
                                )
                            },
                            highlight = {
                                glassHighlight.copy(alpha = 0.82f * glassProgress)
                            },
                            onDrawSurface = {
                                drawCircle(
                                    Color.White.copy(alpha = 0.10f * glassProgress)
                                )
                            },
                        ),
                )
            } else if (pressed && enabled) {
                // Safe fallback when no parent backdrop is available: a
                // brief specular response, without pretending to refract.
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(alpha = 0.12f))
                    drawArc(
                        color = Color.White.copy(alpha = 0.42f),
                        startAngle = 205f,
                        sweepAngle = 125f,
                        useCenter = false,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
        }
    }
}
