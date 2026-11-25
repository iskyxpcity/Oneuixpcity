package com.alohaoneui.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

/**
 * Places peer-level cards in two equal-width, independently flowing columns
 * on expanded/landscape windows. Each column starts at the same top edge and
 * the next card is placed in the currently shorter column, avoiding the large
 * empty row gaps produced by FlowRow when peer cards have different heights.
 *
 * The caller must pass only independent peer cards. Card contents and styling
 * remain untouched; compact windows retain a single full-width column.
 */
@Composable
fun ResponsiveCardGrid(
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    val split = shouldShowSplitPane()
    Layout(
        modifier = modifier,
        content = {
            content.forEach { card ->
                androidx.compose.foundation.layout.Box {
                    card()
                }
            }
        },
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val gap = 16.dp.roundToPx()
            val width = constraints.maxWidth
            if (!split || width == ConstraintsInfinity) {
                val placeables = measurables.map {
                    it.measure(
                        constraints.copy(
                            minWidth = if (width == ConstraintsInfinity) constraints.minWidth else width,
                            maxWidth = width,
                            minHeight = 0,
                        )
                    )
                }
                val spacing = 8.dp.roundToPx()
                val contentHeight = placeables.sumOf { it.height } +
                    spacing * (placeables.size - 1).coerceAtLeast(0)
                layout(
                    width = if (width == ConstraintsInfinity) {
                        placeables.maxOfOrNull { it.width } ?: constraints.minWidth
                    } else width,
                    height = contentHeight.coerceAtLeast(constraints.minHeight),
                ) {
                    var y = 0
                    placeables.forEach { placeable ->
                        placeable.placeRelative(0, y)
                        y += placeable.height + spacing
                    }
                }
            } else {
                val columnWidth = ((width - gap) / 2).coerceAtLeast(0)
                val heights = intArrayOf(0, 0)
                val placements = ArrayList<Pair<androidx.compose.ui.layout.Placeable, Int>>(measurables.size)
                measurables.forEach { measurable ->
                    val column = if (heights[0] <= heights[1]) 0 else 1
                    val placeable = measurable.measure(
                        androidx.compose.ui.unit.Constraints(
                            minWidth = columnWidth,
                            maxWidth = columnWidth,
                            minHeight = 0,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    placements += placeable to column
                    heights[column] += placeable.height + 12.dp.roundToPx()
                }
                val contentHeight = (maxOf(heights[0], heights[1]) -
                    if (placements.isEmpty()) 0 else 12.dp.roundToPx())
                    .coerceAtLeast(0)
                layout(
                    width = width,
                    height = contentHeight.coerceAtLeast(constraints.minHeight),
                ) {
                    val y = intArrayOf(0, 0)
                    placements.forEach { (placeable, column) ->
                        val x = if (column == 0) 0 else columnWidth + gap
                        placeable.placeRelative(x, y[column])
                        y[column] += placeable.height + 12.dp.roundToPx()
                    }
                }
            }
        }
    }
}

private const val ConstraintsInfinity = Int.MAX_VALUE
