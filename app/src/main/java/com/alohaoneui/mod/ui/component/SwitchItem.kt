package com.alohaoneui.mod.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider as MaterialVerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.material.ExpressiveSwitch
import com.alohaoneui.app.ui.component.material.SegmentedListItem
import com.alohaoneui.app.ui.component.material.SegmentedSwitchItem
import com.alohaoneui.app.ui.component.material.alohaOneUISegmentedColors
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.VerticalDivider as MiuixVerticalDivider
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Canonical AlohaOneUI switch semantics rendered by the active AlohaOneUI theme.
 * The source DetailPane keeps ownership of ordering, strings, state and events;
 * this adapter only decides how the row is drawn.
 */
@Composable
fun SwitchItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    clickable: Boolean = false,
    showChildIndicator: Boolean = clickable,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val grouped = LocalAlohaOneUIGroupedPreference.current

    when (LocalUiMode.current) {
        UiMode.Material -> {
            if (clickable) {
                SegmentedListItem(
                    modifier = (if (grouped) modifier else modifier.padding(horizontal = 16.dp, vertical = 4.dp)).alpha(if (enabled) 1f else 0.45f),
                    enabled = enabled,
                    onClick = { if (enabled) onClick() },
                    colors = alohaOneUISegmentedColors(),
                    headlineContent = { Text(title) },
                    supportingContent = summary?.let { { Text(it) } },
                    leadingContent = icon?.let { imageVector ->
                        { Icon(imageVector, contentDescription = title) }
                    },
                    trailingContent = {
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showChildIndicator) {
                                MaterialVerticalDivider(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(top = 8.dp, bottom = 8.dp, end = 15.dp),
                                )
                            }
                            ExpressiveSwitch(
                                checked = checked,
                                enabled = enabled,
                                onCheckedChange = { if (enabled) onCheckedChange(it) },
                            )
                        }
                    },
                )
            } else {
                Box(
                    modifier = (if (grouped) modifier else modifier.padding(horizontal = 16.dp, vertical = 4.dp)).alpha(if (enabled) 1f else 0.45f),
                ) {
                    SegmentedSwitchItem(
                        icon = icon,
                        colors = alohaOneUISegmentedColors(),
                        title = title,
                        summary = summary,
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = { if (enabled) onCheckedChange(it) },
                    )
                }
            }
        }

        UiMode.Miuix -> {
            @Composable
            fun Content() {
                if (clickable) {
                    BasicComponent(
                        title = title,
                        summary = summary,
                        enabled = enabled,
                        modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
                        startAction = icon?.let { imageVector ->
                            {
                                MiuixIcon(
                                    imageVector = imageVector,
                                    contentDescription = title,
                                    tint = colorScheme.onSurface,
                                )
                            }
                        },
                        endActions = {
                            if (showChildIndicator) {
                                MiuixVerticalDivider(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .padding(end = 12.dp),
                                )
                            }
                            MiuixSwitch(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                checked = checked,
                                enabled = enabled,
                                onCheckedChange = { if (enabled) onCheckedChange(it) },
                            )
                        },
                        onClick = { if (enabled) onClick() },
                        role = Role.Button,
                    )
                } else if (showChildIndicator) {
                    BasicComponent(
                        title = title,
                        summary = summary,
                        enabled = enabled,
                        modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
                        startAction = icon?.let { imageVector ->
                            {
                                MiuixIcon(
                                    imageVector = imageVector,
                                    contentDescription = title,
                                    tint = colorScheme.onSurface,
                                )
                            }
                        },
                        endActions = {
                            MiuixVerticalDivider(
                                modifier = Modifier
                                    .height(32.dp)
                                    .padding(end = 12.dp),
                            )
                            MiuixSwitch(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                checked = checked,
                                enabled = enabled,
                                onCheckedChange = { if (enabled) onCheckedChange(it) },
                            )
                        },
                        onClick = { if (enabled) onCheckedChange(!checked) },
                        role = Role.Switch,
                    )
                } else {
                    SwitchPreference(
                        title = title,
                        summary = summary,
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = { if (enabled) onCheckedChange(it) },
                        startAction = icon?.let { imageVector ->
                            {
                                MiuixIcon(
                                    imageVector = imageVector,
                                    contentDescription = title,
                                    tint = colorScheme.onSurface,
                                )
                            }
                        },
                    )
                }
            }

            if (grouped) {
                Box(modifier = Modifier.alpha(if (enabled) 1f else 0.45f)) { Content() }
            } else {
                Card(
                    modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp).alpha(if (enabled) 1f else 0.45f),
                ) {
                    Content()
                }
            }
        }
    }
}
