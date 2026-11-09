package com.alohaoneui.app.ui.screen.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.material.SegmentedColumn
import com.alohaoneui.app.ui.component.material.SegmentedListItem
import com.alohaoneui.app.ui.component.material.alohaOneUISegmentedColors
import com.alohaoneui.mod.ui.component.AlohaOneUIInfoItem
import com.alohaoneui.mod.ui.component.LocalAlohaOneUIGroupedPreference
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers

/**
 * Native status-tab group container.
 *
 * Material uses AlohaOneUI's real SegmentedColumn implementation.
 * Miuix uses Miuix's real Card implementation.
 * Shared rows only receive the grouped flag so they render without adding
 * their own outer card.
 */
@Composable
fun StatusThemeGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    items: List<@Composable () -> Unit>,
    compact: Boolean = false,
    titleAlignedToContent: Boolean = false,
) {
    if (items.isEmpty()) return

    val titleVerticalPadding = if (compact) 4.dp else 8.dp
    val groupVerticalPadding = if (compact) 2.dp else 8.dp
    val titleStartPadding = when (LocalUiMode.current) {
        UiMode.Material -> if (titleAlignedToContent) 32.dp else 16.dp
        UiMode.Miuix -> if (titleAlignedToContent) 28.dp else 12.dp
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            when (LocalUiMode.current) {
                UiMode.Material -> Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(start = titleStartPadding, end = 16.dp, top = titleVerticalPadding, bottom = titleVerticalPadding),
                )
                UiMode.Miuix -> MiuixText(
                    text = title,
                    color = colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth().padding(start = titleStartPadding, end = 12.dp, top = titleVerticalPadding, bottom = titleVerticalPadding),
                )
            }
        }
        when (LocalUiMode.current) {
        UiMode.Material -> {
            SegmentedColumn(
                modifier = modifier.padding(horizontal = 16.dp, vertical = groupVerticalPadding),
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
        }

        UiMode.Miuix -> {
            Card(
                modifier = modifier.padding(horizontal = 12.dp, vertical = groupVerticalPadding),
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
    }
}



/**
 * Expanded setting row modeled after SettingsMaterial's Language options:
 * title and control are one sibling SegmentedListItem in the parent group,
 * not a nested card/frame.
 */
@Composable
fun StatusThemeExpandedControlItem(
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    icon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> {
            val hasHeader = !title.isNullOrBlank() || !summary.isNullOrBlank() || icon != null
            SegmentedListItem(
                modifier = modifier,
                colors = alohaOneUISegmentedColors(),
                headlineContent = {
                    if (hasHeader) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                    )
                                }
                                if (!title.isNullOrBlank()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                            if (!summary.isNullOrBlank()) {
                                Text(
                                    text = summary,
                                    modifier = Modifier.padding(start = if (icon != null && !title.isNullOrBlank()) 36.dp else 0.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        content()
                    }
                },
                supportingContent = if (hasHeader) {
                    { content() }
                } else null,
            )
        }
        UiMode.Miuix -> StatusThemeChildItem(
            modifier = modifier,
            title = title,
            summary = summary,
            icon = icon,
            content = content,
        )
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
    title: String? = null,
    summary: String? = null,
    icon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> {
            val hasHeader = !title.isNullOrBlank() || !summary.isNullOrBlank() || icon != null
            SegmentedListItem(
                modifier = modifier,
                colors = alohaOneUISegmentedColors(),
                headlineContent = {
                    if (hasHeader) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                    )
                                }
                                if (!title.isNullOrBlank()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                            if (!summary.isNullOrBlank()) {
                                Text(
                                    text = summary,
                                    modifier = Modifier.padding(start = if (icon != null && !title.isNullOrBlank()) 36.dp else 0.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        content()
                    }
                },
                supportingContent = if (hasHeader) {
                    {
                        CompositionLocalProvider(
                            LocalMaterialShadowContainers provides false,
                        ) {
                            content()
                        }
                    }
                } else null,
            )
        }

        UiMode.Miuix -> {
            Column(modifier = modifier.fillMaxWidth()) {
                if (!title.isNullOrBlank() || summary != null || icon != null) {
                    AlohaOneUIInfoItem(
                        title = title.orEmpty(),
                        summary = summary,
                        icon = icon,
                    )
                }
                content()
            }
        }
    }
}
