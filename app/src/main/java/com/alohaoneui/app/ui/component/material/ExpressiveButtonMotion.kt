package com.alohaoneui.app.ui.component.material

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Shared Material press motion.
 *
 * Short taps remain expanded until both the 150 ms minimum feedback interval
 * and the expansion spring have completed. Long presses retract immediately
 * on release from their current spring position.
 */
@Composable
internal fun rememberMaterialExpressivePressExpansion(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    expansionPerSide: Dp = 16.dp,
): Dp {
    val expansion = remember(interactionSource) { Animatable(0f) }
    val longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
    LaunchedEffect(interactionSource, enabled, expansionPerSide, longPressTimeoutMillis) {
        if (!enabled) {
            expansion.snapTo(0f)
            return@LaunchedEffect
        }
        coroutineScope {
            var activePress: PressInteraction.Press? = null
            var pressStartedAt = 0L
            // Keep the spring animation and the short-tap hold timer separate.
            // A new press must cancel both; otherwise cancelling the hold timer
            // can orphan the expansion job started by the previous press.
            var animationJob: Job? = null
            var releaseJob: Job? = null
            var pressGeneration = 0L

            interactionSource.interactions.collect { interaction: Interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        activePress = interaction
                        pressStartedAt = SystemClock.uptimeMillis()
                        pressGeneration++
                        releaseJob?.cancel()
                        releaseJob = null
                        animationJob?.cancel()
                        animationJob = launch {
                            expansion.animateTo(
                                expansionPerSide.value,
                                spring(dampingRatio = 0.35f, stiffness = 300f),
                            )
                        }
                    }

                    is PressInteraction.Release -> if (activePress == interaction.press) {
                        activePress = null
                        val heldForMillis = SystemClock.uptimeMillis() - pressStartedAt
                        val releasedGeneration = pressGeneration
                        releaseJob?.cancel()
                        releaseJob = launch {
                            val isShortTap = heldForMillis < longPressTimeoutMillis
                            if (isShortTap) {
                                // Short tap: both conditions must be met before retracting.
                                // The delay preserves the minimum visible response; join()
                                // lets the expansion spring finish instead of cancelling it.
                                delay(150L)
                                if (activePress != null || pressGeneration != releasedGeneration) {
                                    return@launch
                                }
                                animationJob?.join()
                            } else {
                                // Long press: retract immediately from the current position.
                                animationJob?.cancelAndJoin()
                            }

                            // Ignore a stale release if another press started while waiting.
                            if (activePress == null && pressGeneration == releasedGeneration) {
                                animationJob = launch {
                                    expansion.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = 0.35f,
                                            stiffness = 100f,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    is PressInteraction.Cancel -> if (activePress == interaction.press) {
                        activePress = null
                        pressGeneration++
                        releaseJob?.cancel()
                        releaseJob = null
                        animationJob?.cancel()
                        animationJob = launch {
                            expansion.animateTo(
                                0f,
                                spring(
                                    dampingRatio = 0.35f,
                                    stiffness = 100f,
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
