package com.alohaoneui.app.ui.component.material

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import android.graphics.Color as AndroidColor

/**
 * Aloha-native Liquid Glass switch.
 *
 * Android 13+ uses the platform AGSL RuntimeShader for the glass tint,
 * refractive edge, specular ridge and depth shading. Older Android versions
 * use the same geometry and spring interaction with Compose gradients.
 *
 * This control intentionally does not capture its containing pager. A
 * backdrop source that includes the switch itself can form a recursive render
 * dependency on some devices and was the cause of the previous crash path.
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
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 1.055f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "alohaGlassSwitchPressScale",
    )
    val travel by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 620f),
        label = "alohaGlassSwitchTravel",
    )
    val scheme = MaterialTheme.colorScheme
    val trackTint = if (checked) scheme.primaryContainer else scheme.surfaceContainerHigh
    val thumbTint = if (checked) scheme.surface else scheme.surfaceContainerHighest
    val activeAlpha = if (enabled) 1f else 0.45f
    val trackShader = rememberAgslShader(TRACK_SHADER)
    val thumbShader = rememberAgslShader(THUMB_SHADER)

    Box(
        modifier = modifier
            .size(width = 64.dp, height = 30.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) },
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackRadius = CornerRadius(size.height / 2f, size.height / 2f)
            val trackBrush = trackShader?.let { ShaderBrush(it) }
            if (trackShader != null && trackBrush != null) {
                updateGlassShader(
                    shader = trackShader,
                    width = size.width,
                    height = size.height,
                    tint = trackTint.copy(alpha = 0.88f * activeAlpha),
                    pressed = if (pressed) 1f else 0f,
                    selected = if (checked) 1f else 0f,
                )
                drawRoundRect(brush = trackBrush, cornerRadius = trackRadius)
            } else {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            trackTint.copy(alpha = 0.82f * activeAlpha),
                            trackTint.copy(alpha = 0.48f * activeAlpha),
                            scheme.surface.copy(alpha = 0.72f * activeAlpha),
                        ),
                    ),
                    cornerRadius = trackRadius,
                )
            }

            drawRoundRect(
                color = Color.White.copy(alpha = (if (pressed) 0.50f else 0.34f) * activeAlpha),
                cornerRadius = trackRadius,
                style = Stroke(width = 0.8.dp.toPx()),
            )
            drawLine(
                color = scheme.onSurface.copy(alpha = 0.10f * activeAlpha),
                start = Offset(8.dp.toPx(), size.height - 2.dp.toPx()),
                end = Offset(size.width - 8.dp.toPx(), size.height - 2.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )

            val thumbSize = 23.dp.toPx()
            val inset = 3.5.dp.toPx()
            val thumbCenterX = inset + thumbSize / 2f +
                (size.width - thumbSize - inset * 2f) * travel
            val thumbTop = (size.height - thumbSize) / 2f
            val thumbTopLeft = Offset(thumbCenterX - thumbSize / 2f, thumbTop)
            val thumbCorner = CornerRadius(thumbSize / 2f, thumbSize / 2f)
            val thumbDrawSize = androidx.compose.ui.geometry.Size(thumbSize, thumbSize)

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.16f * activeAlpha),
                topLeft = Offset(thumbTopLeft.x, thumbTopLeft.y + 1.4.dp.toPx()),
                size = thumbDrawSize,
                cornerRadius = thumbCorner,
            )
            if (pressed && enabled) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.16f),
                    topLeft = Offset(thumbTopLeft.x - 3.dp.toPx(), thumbTopLeft.y - 3.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        thumbSize + 6.dp.toPx(),
                        thumbSize + 6.dp.toPx(),
                    ),
                    cornerRadius = CornerRadius(thumbSize, thumbSize),
                )
            }

            val thumbBrush = thumbShader?.let { ShaderBrush(it) }
            if (thumbShader != null && thumbBrush != null) {
                updateGlassShader(
                    shader = thumbShader,
                    width = thumbSize,
                    height = thumbSize,
                    tint = thumbTint.copy(alpha = 0.94f * activeAlpha),
                    pressed = if (pressed) 1f else 0f,
                    selected = if (checked) 1f else 0f,
                )
                drawRoundRect(
                    brush = thumbBrush,
                    topLeft = thumbTopLeft,
                    size = thumbDrawSize,
                    cornerRadius = thumbCorner,
                )
            } else {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.96f * activeAlpha),
                            thumbTint.copy(alpha = 0.92f * activeAlpha),
                            scheme.surfaceContainerHigh.copy(alpha = 0.82f * activeAlpha),
                        ),
                        startY = thumbTop,
                        endY = thumbTop + thumbSize,
                    ),
                    topLeft = thumbTopLeft,
                    size = thumbDrawSize,
                    cornerRadius = thumbCorner,
                )
            }
            drawRoundRect(
                color = Color.White.copy(alpha = 0.62f * activeAlpha),
                topLeft = thumbTopLeft,
                size = thumbDrawSize,
                cornerRadius = thumbCorner,
                style = Stroke(width = 0.8.dp.toPx()),
            )
            drawArc(
                color = Color.White.copy(alpha = 0.42f * activeAlpha),
                startAngle = 198f,
                sweepAngle = 142f,
                useCenter = false,
                topLeft = Offset(thumbTopLeft.x + 3.dp.toPx(), thumbTopLeft.y + 2.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(
                    thumbSize - 6.dp.toPx(),
                    thumbSize - 6.dp.toPx(),
                ),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun rememberAgslShader(source: String): RuntimeShader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return remember(source) { RuntimeShader(source) }
}

private fun DrawScope.updateGlassShader(
    shader: RuntimeShader,
    width: Float,
    height: Float,
    tint: Color,
    pressed: Float,
    selected: Float,
) {
    shader.setFloatUniform("resolution", width, height)
    shader.setColorUniform(
        "tint",
        AndroidColor.valueOf(tint.red, tint.green, tint.blue, tint.alpha),
    )
    shader.setColorUniform("highlight", AndroidColor.valueOf(1f, 1f, 1f, 1f))
    shader.setFloatUniform("pressed", pressed)
    shader.setFloatUniform("selected", selected)
}

private const val TRACK_SHADER = """
uniform float2 resolution;
layout(color) uniform half4 tint;
layout(color) uniform half4 highlight;
uniform float pressed;
uniform float selected;

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float rim = 1.0 - smoothstep(0.0, 0.13, edge);
    float specular = exp(-distance(uv, float2(0.24, 0.18)) * 18.0);
    float upperRidge = exp(-pow((uv.y - 0.16) * 15.0, 2.0));
    float lowerRidge = exp(-pow((uv.y - 0.88) * 12.0, 2.0));
    float colorShift = 0.025 * selected;
    half3 rgb = tint.rgb * 0.72;
    rgb += highlight.rgb * (specular * 0.24 + upperRidge * 0.12 + rim * 0.16);
    rgb += half3(colorShift, colorShift * 0.55, colorShift * 0.85);
    rgb -= tint.rgb * lowerRidge * 0.08;
    rgb += half3(0.018, 0.024, 0.032) * pressed;
    return half4(rgb, tint.a * 0.84 + rim * 0.08);
}
"""

private const val THUMB_SHADER = """
uniform float2 resolution;
layout(color) uniform half4 tint;
layout(color) uniform half4 highlight;
uniform float pressed;
uniform float selected;

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float rim = 1.0 - smoothstep(0.0, 0.20, edge);
    float specular = exp(-distance(uv, float2(0.27, 0.17)) * 22.0);
    float lowerShade = smoothstep(0.35, 1.0, uv.y) * 0.12;
    float chroma = rim * (0.035 + 0.025 * selected);
    half3 rgb = tint.rgb * (0.82 - lowerShade);
    rgb += highlight.rgb * (specular * 0.36 + rim * 0.20);
    rgb += half3(chroma, chroma * 0.48, chroma * 0.78);
    rgb += half3(0.025, 0.03, 0.04) * pressed;
    return half4(rgb, tint.a * 0.90 + rim * 0.06);
}
"""
