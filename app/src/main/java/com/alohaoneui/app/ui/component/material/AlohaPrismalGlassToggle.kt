/*
 * Adapted from PrismalAGSL PrismalGlassToggle (v1.0.4).
 * Original project: https://github.com/styropyr0/PrismalAGSL
 * License: Apache-2.0. Aloha-specific changes: semantic theme colors and switch semantics.
 */
package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.components.LocalPrismalParentGlassLayer
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.effects.prismalBlur
import com.styropyr0.prismal.effects.prismalLens
import com.styropyr0.prismal.interactive.PrismalSpringMotion
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalMergedSource
import com.styropyr0.prismal.sources.rememberPrismalWrappedSource
import com.styropyr0.prismal.specular.PrismalSpecular
import kotlinx.coroutines.flow.collectLatest

/**
 * Prismal's AGSL liquid-glass toggle adapted to Aloha's semantic theme palette.
 * The backdrop is an independent source; the thumb additionally samples the
 * locally recorded track so its lens never captures its own rendering layer.
 */
@Composable
fun AlohaPrismalGlassToggle(
    selected: Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: PrismalBackdrop,
    trackOffColor: Color,
    trackOnColor: Color,
    thumbTint: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        PrismalSpringMotion(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (selected) 0f else 1f
                    onSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) didDrag = dragAmount.x != 0f
                val delta = dragAmount.x / dragWidth
                fraction = if (isLtr) {
                    (fraction + delta).fastCoerceIn(0f, 1f)
                } else {
                    (fraction - delta).fastCoerceIn(0f, 1f)
                }
            },
        )
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }.collectLatest { dampedDragAnimation.updateValue(it) }
    }
    LaunchedEffect(dampedDragAnimation, selected) {
        snapshotFlow { selected }.collectLatest { isSelected ->
            val target = if (isSelected) 1f else 0f
            if (target != fraction) {
                fraction = target
                dampedDragAnimation.animateToValue(target)
            }
        }
    }

    val trackBackdrop = rememberPrismalGlassLayer()
    val parentGlassLayer = LocalPrismalParentGlassLayer.current
    val underlayBackdrop = parentGlassLayer ?: backdrop

    trackBackdrop.readSamplingState()
    parentGlassLayer?.readSamplingState()

    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .prismalGlassLayer(trackBackdrop)
                .clip(PrismalCapsule())
                .drawBehind {
                    val trackFraction = dampedDragAnimation.value
                    drawRect(lerp(trackOffColor, trackOnColor, trackFraction).copy(alpha = 0.30f))
                }
                .size(64.dp, 28.dp),
        )

        Box(
            Modifier
                .graphicsLayer {
                    val trackFraction = dampedDragAnimation.value
                    val padding = 2.dp.toPx()
                    translationX = if (isLtr) {
                        lerp(padding, padding + dragWidth, trackFraction)
                    } else {
                        lerp(-padding, -(padding + dragWidth), trackFraction)
                    }
                }
                .semantics {
                    role = Role.Switch
                    toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
                }
                .then(dampedDragAnimation.modifier)
                .drawPrismalGlass(
                    backdrop = rememberPrismalMergedSource(
                        underlayBackdrop,
                        rememberPrismalWrappedSource(trackBackdrop) { drawPrismalGlass ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) { drawPrismalGlass() }
                        },
                    ),
                    shape = { PrismalCapsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        applyPrismalGlassEffects(
                            density = density,
                            adaptiveLuminance = false,
                            luminance = 0.5f,
                            blurRadiusPx = with(density) { 8.dp.toPx() } * (1f - progress),
                            refractionHeightPx = with(density) { 5.dp.toPx() } * progress,
                            refractionAmountPx = with(density) { 10.dp.toPx() } * progress,
                            chromaticAberration = 0.35f,
                        )
                    },
                    specular = {
                        val progress = dampedDragAnimation.pressProgress
                        PrismalSpecular.Ambient.copy(
                            width = PrismalSpecular.Ambient.width / 1.5f,
                            blurRadius = PrismalSpecular.Ambient.blurRadius / 1.5f,
                            alpha = progress,
                        )
                    },
                    depthShadow = {
                        PrismalDepthShadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.05f))
                    },
                    depthInset = {
                        val progress = dampedDragAnimation.pressProgress
                        PrismalDepthInset(radius = 4.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        drawRect(thumbTint.copy(alpha = 0.18f))
                    },
                )
                .size(40.dp, 24.dp),
        )
    }
}
