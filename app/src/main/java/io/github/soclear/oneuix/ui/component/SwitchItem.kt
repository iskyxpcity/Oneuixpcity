package io.github.soclear.oneuix.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.ExpressiveSwitch
import com.oneuixp.app.ui.component.material.SegmentedListItem
import com.oneuixp.app.ui.component.material.SegmentedSwitchItem
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Canonical OneUIX switch semantics rendered by the active OneuiXP theme.
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
    onClick: () -> Unit = {},
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val grouped = LocalOneuiXGroupedPreference.current
    val materialColors = ListItemDefaults.segmentedColors().copy(
        containerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
    )

    when (LocalUiMode.current) {
        UiMode.Material -> {
            if (clickable) {
                SegmentedListItem(
                    modifier = if (grouped) modifier else modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = onClick,
                    colors = if (grouped) materialColors else ListItemDefaults.segmentedColors(),
                    headlineContent = { Text(title) },
                    supportingContent = summary?.let { { Text(it) } },
                    leadingContent = icon?.let { imageVector ->
                        { Icon(imageVector, contentDescription = title) }
                    },
                    trailingContent = {
                        ExpressiveSwitch(
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                        )
                    },
                )
            } else {
                Box(
                    modifier = if (grouped) modifier else modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    SegmentedSwitchItem(
                        icon = icon,
                        colors = if (grouped) materialColors else ListItemDefaults.segmentedColors(),
                        title = title,
                        summary = summary,
                        checked = checked,
                        onCheckedChange = onCheckedChange,
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
                            MiuixSwitch(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                checked = checked,
                                onCheckedChange = onCheckedChange,
                            )
                        },
                        onClick = onClick,
                        role = Role.Button,
                    )
                } else {
                    SwitchPreference(
                        title = title,
                        summary = summary,
                        checked = checked,
                        onCheckedChange = onCheckedChange,
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
                Content()
            } else {
                Card(
                    modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Content()
                }
            }
        }
    }
}
