package com.oneuixp.app.ui.screen.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.SegmentedColumn
import com.oneuixp.app.ui.component.material.SegmentedListItem
import com.oneuixp.app.ui.component.material.oneuiXSegmentedColors
import io.github.soclear.oneuix.ui.component.LocalOneuiXGroupedPreference
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

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
    title: String? = null,
    modifier: Modifier = Modifier,
    items: List<@Composable () -> Unit>,
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            when (LocalUiMode.current) {
                UiMode.Material -> Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                )
                UiMode.Miuix -> MiuixText(
                    text = title,
                    color = colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                )
            }
        }
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
}


/**
 * Expanded child content that stays inside the active native group.
 * Material renders it as a real SegmentedListItem so it participates in
 * first/middle/last shapes. Miuix keeps it inside the surrounding Card.
 */
@Composable
fun StatusThemeChildItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> {
            SegmentedListItem(
                modifier = modifier,
                colors = oneuiXSegmentedColors(),
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
        }

        UiMode.Miuix -> {
            Box(
                modifier = modifier.fillMaxWidth(),
            ) {
                content()
            }
        }
    }
}
