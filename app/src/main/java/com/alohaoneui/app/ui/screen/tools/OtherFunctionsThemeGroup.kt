package com.alohaoneui.app.ui.screen.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
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

    when (LocalUiMode.current) {
        UiMode.Material -> SegmentedColumn(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> SegmentedListItem(
            modifier = modifier,
            colors = alohaOneUISegmentedColors(),
            headlineContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    content()
                }
            },
        )

        UiMode.Miuix -> Box(
            modifier = modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}
