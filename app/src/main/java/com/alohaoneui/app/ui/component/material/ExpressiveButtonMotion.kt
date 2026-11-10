package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Shared Material press motion:
 * - Press/long-press expands smoothly without spring overshoot.
 * - Release first returns almost all the way with a smooth tween.
 * - Only the final settling movement uses a damped spring, matching the
 *   detail-option expansion interaction rather than bouncing throughout.
 *
 * The value is rendered as real horizontal content space by each shared
 * button wrapper, so neighboring controls are laid out around the expansion.
 */
@Composable
internal fun rememberMaterialExpressivePressExpansion(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    expansionPerSide: Dp = 30.dp,
): Dp {
    val expansion = remember(interactionSource) { Animatable(0f) }
    LaunchedEffect(interactionSource, enabled, expansionPerSide) {
        if (!enabled) {
            expansion.snapTo(0f)
            return@LaunchedEffect
        }
        coroutineScope {
            var activePress: PressInteraction.Press? = null
            var motionJob: Job? = null
            interactionSource.interactions.collect { interaction: Interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        activePress = interaction
                        motionJob?.cancel()
                        motionJob = launch {
                            expansion.animateTo(
                                expansionPerSide.value,
                                tween(durationMillis = 150),
                            )
                        }
                    }
                    is PressInteraction.Release -> if (activePress == interaction.press) {
                        activePress = null
                        motionJob?.cancel()
                        motionJob = launch {
                            // Smoothly retract almost the entire expansion first.
                            expansion.animateTo(
                                expansionPerSide.value * 0.10f,
                                tween(durationMillis = 105),
                            )
                            // A single restrained spring is reserved for the final settle.
                            expansion.animateTo(
                                0f,
                                spring(
                                    dampingRatio = 0.72f,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    }
                    is PressInteraction.Cancel -> if (activePress == interaction.press) {
                        activePress = null
                        motionJob?.cancel()
                        motionJob = launch {
                            expansion.animateTo(
                                expansionPerSide.value * 0.10f,
                                tween(durationMillis = 90),
                            )
                            expansion.animateTo(
                                0f,
                                spring(
                                    dampingRatio = 0.82f,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
    return expansion.value.dp
}
