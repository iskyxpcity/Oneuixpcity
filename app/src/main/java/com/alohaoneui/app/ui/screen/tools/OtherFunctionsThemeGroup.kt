package com.alohaoneui.app.ui.screen.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.util.shouldShowSplitPane
import com.alohaoneui.app.ui.component.material.SegmentedColumn
import com.alohaoneui.app.ui.component.material.SegmentedListItem
import com.alohaoneui.app.ui.component.material.alohaOneUISegmentedColors
import com.alohaoneui.mod.ui.component.LocalAlohaOneUIGroupedPreference
import top.yukonga.miuix.kmp.basic.Card

/**
 * Theme-native grouping owned by Tools > Other functions.
 * Material uses a real SegmentedColumn; Miuix uses a real Card.
 * Expanded controls stay inside the same native group.
 */
@Composable
fun OtherFunctionsThemeGroup(
    modifier: Modifier = Modifier,
    items: List<@Composable () -> Unit>,
) {
    if (items.isEmpty()) return

    // A parent frame already owns the outer visual group. Nested detail groups
    // still need their own row positions so Material can assign top/middle/bottom shapes.
    if (LocalAlohaOneUIGroupedPreference.current) {
        when (LocalUiMode.current) {
            UiMode.Material -> SegmentedColumn(
                modifier = modifier.fillMaxWidth(),
                content = items,
            )
            UiMode.Miuix -> Column(modifier = modifier.fillMaxWidth()) {
                items.forEach { item -> item() }
            }
        }
        return
    }

    when (LocalUiMode.current) {
        UiMode.Material -> SegmentedColumn(
            modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
            content = items.map { item ->
                {
                    CompositionLocalProvider(
                        LocalAlohaOneUIGroupedPreference provides true,
                    ) {
                        item()
                    }
                }
            },
        )

        UiMode.Miuix -> Card(
            modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
            insideMargin = PaddingValues(0.dp),
        ) {
            CompositionLocalProvider(
                LocalAlohaOneUIGroupedPreference provides true,
            ) {
                Column {
                    items.forEach { item -> item() }
                }
            }
        }
    }
}

/** A child row that keeps Material segment shaping / Miuix card continuity. */
@Composable
fun OtherFunctionsThemeChildItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> SegmentedListItem(
            modifier = modifier,
            onClick = onClick,
            colors = alohaOneUISegmentedColors(),
            headlineContent = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            },
        )

        UiMode.Miuix -> Box(
            modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}


/** Outer four-card frame used by wide layouts; child detail cards retain their own theme components. */
@Composable
fun OtherFunctionsThemeFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> com.alohaoneui.app.ui.component.material.TonalCard(
            modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            Column(modifier = Modifier.padding(10.dp)) { content() }
        }
        UiMode.Miuix -> Card(
            modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
            insideMargin = PaddingValues(10.dp),
        ) {
            Column { content() }
        }
    }
}


/** Preserve each original detail group as one unit; wide windows place those groups row-first in two columns. */
@Composable
fun OtherFunctionsThemeFrameLayout(
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    if (shouldShowSplitPane()) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    rowItems.forEach { item ->
                        OtherFunctionsThemeGroup(
                            modifier = Modifier.weight(1f),
                            items = listOf(item),
                        )
                    }
                    if (rowItems.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            items.forEach { it() }
        }
    }
}
