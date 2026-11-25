package com.alohaoneui.app.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Places peer-level content cards side by side in expanded/landscape windows.
 *
 * The caller must pass only independent peer cards. The contents of each card
 * remain untouched, and compact windows retain the original single-column flow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResponsiveCardGrid(
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    if (shouldShowSplitPane()) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2,
        ) {
            content.forEach { card ->
                Box(Modifier.weight(1f)) {
                    card()
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content.forEach { card -> Box(Modifier.fillMaxWidth()) { card() } }
        }
    }
}
