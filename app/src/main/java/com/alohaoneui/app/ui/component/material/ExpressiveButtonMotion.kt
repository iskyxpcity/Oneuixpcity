package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
 * Shared Material press motion.
 *
 * A short tap must not cancel the expansion halfway through: finish the press
 * phase first, then retract smoothly, and reserve spring motion for the final
 * return to the resting size. Long presses use the exact same sequence.
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
                        // Retarget from the current animated value instead of waiting
                        // for a previous phase to finish. This avoids the pause on quick taps.
                        motionJob?.cancel()
                        motionJob = launch {
                            expansion.animateTo(
                                expansionPerSide.value,
                                tween(durationMillis = 230, easing = FastOutSlowInEasing),
                            )
                        }
                    }
                    is PressInteraction.Release -> if (activePress == interaction.press) {
                        activePress = null
                        // Interrupt at the current position and preserve the motion
                        // continuity; the spring is reserved for the final settle.
                        motionJob?.cancel()
                        motionJob = launch {
                            expansion.animateTo(
                                expansionPerSide.value * 0.34f,
                                tween(durationMillis = 190, easing = FastOutSlowInEasing),
                            )
                            expansion.animateTo(
                                0f,
                                spring(
                                    dampingRatio = 0.32f,
                                    stiffness = 260f,
                                ),
                            )
                        }
                    }
                    is PressInteraction.Cancel -> if (activePress == interaction.press) {
                        activePress = null
                        motionJob?.cancel()
                        motionJob = launch {
                            expansion.animateTo(
                                expansionPerSide.value * 0.30f,
                                tween(durationMillis = 170, easing = FastOutSlowInEasing),
                            )
                            expansion.animateTo(
                                0f,
                                spring(
                                    dampingRatio = 0.38f,
                                    stiffness = 300f,
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
