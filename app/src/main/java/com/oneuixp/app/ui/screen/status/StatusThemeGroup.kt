package com.oneuixp.app.ui.screen.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.SegmentedColumn
import io.github.soclear.oneuix.ui.component.LocalOneuiXGroupedPreference
import top.yukonga.miuix.kmp.basic.Card

/**
 * Native status-tab group container.
 *
 * Material uses OneuiXP's real SegmentedColumn implementation.
 * Miuix uses Miuix's real Card implementation.
 * Shared rows only receive the grouped flag so they render without adding
 * their own outer card.
 */
@Composable
fun StatusThemeGroup(
    modifier: Modifier = Modifier,
    items: List<@Composable () -> Unit>,
) {
    if (items.isEmpty()) return

    when (LocalUiMode.current) {
        UiMode.Material -> {
            SegmentedColumn(
                modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                content = items.map { item ->
                    {
                        CompositionLocalProvider(
                            LocalOneuiXGroupedPreference provides true,
                        ) {
                            item()
                        }
                    }
                },
            )
        }

        UiMode.Miuix -> {
            Card(
                modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                CompositionLocalProvider(
                    LocalOneuiXGroupedPreference provides true,
                ) {
                    Column {
                        items.forEach { item -> item() }
                    }
                }
            }
        }
    }
}
