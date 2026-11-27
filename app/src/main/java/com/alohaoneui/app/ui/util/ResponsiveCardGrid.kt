package com.alohaoneui.app.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private data class ResponsiveGridParentData(
    val fullSpan: Boolean,
    val column: Int? = null,
)

private class ResponsiveGridFullSpanModifier : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) =
        ResponsiveGridParentData(fullSpan = true)
}

/** Marks a child (for example, a preview or segmented mode selector) as full-width. */
fun Modifier.responsiveGridFullSpan(): Modifier = then(ResponsiveGridFullSpanModifier())

private class ResponsiveGridColumnModifier(
    private val column: Int,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) =
        ResponsiveGridParentData(fullSpan = false, column = column.coerceIn(0, 1))
}

/** Pins a child to a column when the responsive grid is split. */
fun Modifier.responsiveGridColumn(column: Int): Modifier =
    then(ResponsiveGridColumnModifier(column))

/**
 * Equal-width responsive card layout. Expanded windows use two independently
 * flowing columns; each new card is placed at the top of the shorter column.
 * Full-span children start a new row across both columns. Compact windows use
 * one full-width column.
 */
@Composable
fun ResponsiveCardGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val split = shouldShowSplitPane()
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val width = constraints.maxWidth
            val finiteWidth = width != Constraints.Infinity
            val gapX = 16.dp.roundToPx()
            val gapY = 12.dp.roundToPx()
            val parentCardCount = measurables.count {
                (it.parentData as? ResponsiveGridParentData)?.fullSpan != true
            }
            val hasExplicitColumns = measurables.any {
                (it.parentData as? ResponsiveGridParentData)?.column != null
            }
            val columns = if (
                split && finiteWidth &&
                (parentCardCount > 2 || (hasExplicitColumns && parentCardCount > 1))
            ) 2 else 1
            val columnWidth = if (columns == 2) ((width - gapX) / 2).coerceAtLeast(0)
                else if (finiteWidth) width else constraints.minWidth
            val heights = IntArray(columns)
            val placements = ArrayList<Triple<androidx.compose.ui.layout.Placeable, Int, Int>>(measurables.size)

            measurables.forEach { measurable ->
                val fullSpan = (measurable.parentData as? ResponsiveGridParentData)?.fullSpan == true
                if (fullSpan && columns == 2) {
                    val y = maxOf(heights[0], heights[1])
                    val placeable = measurable.measure(
                        Constraints(minWidth = width, maxWidth = width, minHeight = 0, maxHeight = constraints.maxHeight)
                    )
                    placements += Triple(placeable, -1, y)
                    val nextY = y + placeable.height + gapY
                    heights[0] = nextY
                    heights[1] = nextY
                } else {
                    val requestedColumn = (measurable.parentData as? ResponsiveGridParentData)?.column
                    val column = if (columns == 1) 0 else requestedColumn
                        ?: if (heights[0] <= heights[1]) 0 else 1
                    val childWidth = if (fullSpan) width else columnWidth
                    val placeable = measurable.measure(
                        Constraints(minWidth = childWidth, maxWidth = childWidth, minHeight = 0, maxHeight = constraints.maxHeight)
                    )
                    placements += Triple(placeable, if (fullSpan) -1 else column, heights[column])
                    heights[column] += placeable.height + gapY
                }
            }

            val contentHeight = ((heights.maxOrNull() ?: 0) - if (placements.isEmpty()) 0 else gapY)
                .coerceAtLeast(0)
            layout(
                width = (if (finiteWidth) width else columnWidth).coerceAtLeast(constraints.minWidth),
                height = contentHeight.coerceAtLeast(constraints.minHeight),
            ) {
                placements.forEach { (placeable, column, y) ->
                    val x = when (column) {
                        -1 -> 0
                        0 -> 0
                        else -> columnWidth + gapX
                    }
                    placeable.placeRelative(x, y)
                }
            }
        }
    }
}

/** Keeps existing list-based call sites source-compatible. */
@Composable
fun ResponsiveCardGrid(
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>,
) {
    ResponsiveCardGrid(modifier = modifier) {
        content.forEach { card -> Box { card() } }
    }
}
