package com.alohaoneui.app.ui.component.material

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Shared Material 3 Expressive press response.
 *
 * The control gently expands while pressed and springs back to its resting
 * size on release. Graphics-layer scaling leaves measurement and neighboring
 * layout positions unchanged, so standalone controls never reflow their row.
 */
@Composable
internal fun Modifier.materialExpressivePressMotion(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) 1.045f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "materialExpressiveButtonPress",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
