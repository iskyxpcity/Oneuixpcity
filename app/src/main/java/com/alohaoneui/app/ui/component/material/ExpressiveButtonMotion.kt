package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collect

/**
 * Shared Material 3 Expressive press response.
 * A press always completes its expansion before the spring returns to rest,
 * even when the pointer is released before the expansion animation finishes.
 */
@Composable
internal fun Modifier.materialExpressivePressMotion(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): Modifier {
    val scale = androidx.compose.runtime.remember(interactionSource) { Animatable(1f) }
    LaunchedEffect(interactionSource, enabled) {
        if (!enabled) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        var activePress: PressInteraction.Press? = null
        interactionSource.interactions.collect { interaction: Interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    activePress = interaction
                    // This call is intentionally awaited: Release is handled only
                    // after the full expansion has reached its target.
                    scale.animateTo(1.085f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
                }
                is PressInteraction.Release -> {
                    if (activePress == interaction.press) {
                        activePress = null
                        scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
                    }
                }
                is PressInteraction.Cancel -> {
                    if (activePress == interaction.press) {
                        activePress = null
                        scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
                    }
                }
            }
        }
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
