package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

/**
 * Width-based Material press feedback. The animated value is inserted as real
 * leading/trailing content spacing so the button's surface expands in layout,
 * fills its shape, and pushes siblings apart instead of scaling over them.
 * Press interactions include pointer long-press: the expansion remains active
 * until Release/Cancel, then springs back.
 */
@Composable
internal fun rememberMaterialExpressivePressExpansion(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    expansionPerSide: Dp = 12.dp,
): Dp {
    val expansion = remember(interactionSource) { Animatable(0f) }
    LaunchedEffect(interactionSource, enabled, expansionPerSide) {
        if (!enabled) {
            expansion.snapTo(0f)
            return@LaunchedEffect
        }
        var activePress: PressInteraction.Press? = null
        interactionSource.interactions.collect { interaction: Interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    activePress = interaction
                    expansion.animateTo(
                        expansionPerSide.value,
                        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
                    )
                }
                is PressInteraction.Release -> if (activePress == interaction.press) {
                    activePress = null
                    expansion.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
                }
                is PressInteraction.Cancel -> if (activePress == interaction.press) {
                    activePress = null
                    expansion.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
                }
            }
        }
    }
    return expansion.value.dp
}
