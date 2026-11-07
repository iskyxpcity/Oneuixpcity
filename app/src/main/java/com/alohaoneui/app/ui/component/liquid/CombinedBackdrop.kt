// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).
// Mirrored from compose-miuix-ui example.

package com.alohaoneui.app.ui.component.liquid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.Backdrop

@Stable
class CombinedBackdrop(
    val first: Backdrop,
    val second: Backdrop,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = first.isCoordinatesDependent || second.isCoordinatesDependent

    override val offsetResidualX: Float get() = first.offsetResidualX
    override val offsetResidualY: Float get() = first.offsetResidualY

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        with(first) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        with(second) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
    }
}

@Composable
fun rememberCombinedBackdrop(first: Backdrop, second: Backdrop): Backdrop =
    remember(first, second) { CombinedBackdrop(first, second) }


/**
 * Applies the LiquidToggle's vertical backdrop-sampling inset to one source only.
 * The scale is centered on the consuming glass surface and never changes the
 * surface geometry or the horizontal sampling scale.
 */
@Stable
private class VerticallyScaledBackdrop(
    private val source: Backdrop,
    private val currentScaleY: () -> Float,
) : Backdrop {

    override val isCoordinatesDependent: Boolean
        get() = source.isCoordinatesDependent

    override val offsetResidualX: Float
        get() = source.offsetResidualX

    override val offsetResidualY: Float
        get() = source.offsetResidualY

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) {
        val verticalScale = currentScaleY().coerceIn(0f, 1f)
        withTransform({
            scale(
                scaleX = 1f,
                scaleY = verticalScale,
                pivot = Offset(size.width / 2f, size.height / 2f),
            )
        }) {
            with(source) {
                drawBackdrop(density, coordinates, layerBlock, downscaleFactor)
            }
        }
    }
}

@Composable
fun rememberVerticallyScaledBackdrop(
    backdrop: Backdrop,
    scaleY: () -> Float,
): Backdrop {
    val latestScaleY by rememberUpdatedState(scaleY)
    return remember(backdrop) {
        VerticallyScaledBackdrop(backdrop) { latestScaleY() }
    }
}
