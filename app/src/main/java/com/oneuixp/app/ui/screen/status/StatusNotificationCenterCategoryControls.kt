package com.oneuixp.app.ui.screen.status

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import com.oneuixp.common.Preference
import com.oneuixp.mod.ui.category.SystemUIEvent
import com.oneuixp.mod.ui.component.OneuiXPActionButton
import com.oneuixp.mod.ui.component.OneuiXPInlineTextField
import com.oneuixp.mod.ui.component.SwitchItem

@Composable
fun StatusNotificationCenterCategoryControls(
    uiState: Preference.SystemUI.Notification,
    onEvent: (SystemUIEvent) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var packages by remember(uiState.hideOngoingActivityMediaPackages) {
        mutableStateOf(uiState.hideOngoingActivityMediaPackages)
    }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.music_note),
                    title = stringResource(id = R.string.oneuixp_hideOngoingActivityMedia_title),
                    summary = if (
                        uiState.hideOngoingActivityMedia &&
                        uiState.hideOngoingActivityMediaPackages.isNotEmpty()
                    ) {
                        uiState.hideOngoingActivityMediaPackages
                    } else {
                        stringResource(id = R.string.oneuixp_hideOngoingActivityMedia_summary)
                    },
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.hideOngoingActivityMedia,
                    onCheckedChange = {
                        if (it && uiState.hideOngoingActivityMediaPackages.isEmpty()) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.Notification.HideOngoingActivityMedia(it))
                    },
                )
            }
            if (expanded && uiState.hideOngoingActivityMedia) {
                add {
                    StatusThemeChildItem {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        ) {
                            OneuiXPInlineTextField(
                                value = packages,
                                onValueChange = { packages = it },
                                modifier = Modifier.weight(1f),
                                labelText = stringResource(
                                    id = R.string.oneuixp_hideOngoingActivityMedia_packages_hint
                                ),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OneuiXPActionButton(
                                text = stringResource(id = R.string.oneuixp_confirm),
                                primary = true,
                                onClick = {
                                    onEvent(
                                        SystemUIEvent.Notification.HideOngoingActivityMediaPackages(
                                            packages
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.notifications),
                    title = stringResource(id = R.string.oneuixp_disableNotificationGrouping_title),
                    summary = stringResource(id = R.string.oneuixp_disableNotificationGrouping_summary),
                    checked = uiState.disableNotificationGrouping,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.Notification.DisableNotificationGrouping(it))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.notifications),
                    title = stringResource(id = R.string.oneuixp_autoExpandNotifications_title),
                    checked = uiState.autoExpandNotifications,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.Notification.AutoExpandNotifications(it))
                    },
                )
            }
        },
    )
}
