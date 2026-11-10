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
 * A tap completes the full expansion before retracting. Retargeting is only
 * used when a new press interrupts an existing return, so quick taps never
 * reverse halfway through the expansion.
 */
@Composable
internal fun rememberMaterialExpressivePressExpansion(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    expansionPerSide: Dp = 16.dp,
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
                                tween(durationMillis = 190, easing = FastOutSlowInEasing),
                            )
                        }
                    }
                    is PressInteraction.Release -> if (activePress == interaction.press) {
                        activePress = null
                        // Do not cut off a short tap's expansion. Finish the
                        // press phase, glide back most of the distance, then
                        // use a restrained spring only for the final settle.
                        val pressJob = motionJob
                        motionJob = launch {
                            pressJob?.join()
                            expansion.animateTo(
                                expansionPerSide.value * 0.28f,
                                tween(durationMillis = 150, easing = FastOutSlowInEasing),
                            )
                            expansion.animateTo(
                                0f,
                                spring(
                                    dampingRatio = 0.48f,
                                    stiffness = 280f,
                                ),
                            )
                        }
                    }
                    is PressInteraction.Cancel -> if (activePress == interaction.press) {
                        activePress = null
                        val pressJob = motionJob
                        motionJob = launch {
                            pressJob?.join()
                            expansion.animateTo(
                                expansionPerSide.value * 0.24f,
                                tween(durationMillis = 130, easing = FastOutSlowInEasing),
                            )
                            expansion.animateTo(
                                0f,
                                spring(
                                    dampingRatio = 0.52f,
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
