package com.alohaoneui.app.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * Equal-width responsive card layout. Expanded windows use two independently
 * flowing columns; each new card is placed at the top of the shorter column.
 * Compact windows use one full-width column.
 */
@Composable
fun ResponsiveCardGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val split = shouldShowSplitPane()
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val width = constraints.maxWidth
            val finiteWidth = width != Constraints.Infinity
            val gapX = 16.dp.roundToPx()
            val gapY = 12.dp.roundToPx()
            val columns = if (split && finiteWidth) 2 else 1
            val columnWidth = if (columns == 2) {
                ((width - gapX) / 2).coerceAtLeast(0)
            } else if (finiteWidth) width else constraints.minWidth

            val heights = IntArray(columns)
            val placements = ArrayList<Triple<androidx.compose.ui.layout.Placeable, Int, Int>>(measurables.size)
            measurables.forEach { measurable ->
                val column = if (columns == 1) 0 else if (heights[0] <= heights[1]) 0 else 1
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = columnWidth,
                        maxWidth = columnWidth,
                        minHeight = 0,
                        maxHeight = constraints.maxHeight,
                    )
                )
                placements += Triple(placeable, column, heights[column])
                heights[column] += placeable.height + gapY
            }
            val contentHeight = (heights.maxOrNull() ?: 0) -
                if (placements.isEmpty()) 0 else gapY
            val layoutWidth = if (finiteWidth) width else columnWidth
            layout(
                width = layoutWidth.coerceAtLeast(constraints.minWidth),
                height = contentHeight.coerceAtLeast(constraints.minHeight),
            ) {
                placements.forEach { (placeable, column, y) ->
                    val x = if (column == 0) 0 else columnWidth + gapX
                    placeable.placeRelative(x, y)
                }
            }
        }
    }
}

/** Keeps the existing list-based call sites source-compatible. */
@Composable
fun ResponsiveCardGrid(
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>,
) {
    ResponsiveCardGrid(modifier = modifier) {
        content.forEach { card ->
            Box { card() }
        }
    }
}
