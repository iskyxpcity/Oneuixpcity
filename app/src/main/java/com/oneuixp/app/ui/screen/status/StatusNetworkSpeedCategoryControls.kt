package com.oneuixp.app.ui.screen.status

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import io.github.soclear.oneuix.ui.component.OneuiXInlineTextField
import io.github.soclear.oneuix.ui.component.SwitchItem

@Composable
fun StatusNetworkSpeedCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var threshold by remember(uiState.networkSpeedThresholdKb) {
        mutableIntStateOf(uiState.networkSpeedThresholdKb)
    }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                    title = stringResource(id = R.string.oneuix_supportRealTimeNetworkSpeed_title),
                    summary = stringResource(id = R.string.oneuix_supportRealTimeNetworkSpeed_summary),
                    checked = uiState.supportRealTimeNetworkSpeed,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.SupportRealTimeNetworkSpeed(it))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                    title = stringResource(id = R.string.oneuix_showSeparateUpDownNetworkSpeeds_title),
                    summary = stringResource(id = R.string.oneuix_showSeparateUpDownNetworkSpeeds_summary),
                    checked = uiState.showSeparateUpDownNetworkSpeeds,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.ShowSeparateUpDownNetworkSpeeds(it))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                    title = stringResource(id = R.string.oneuix_networkSpeedThreshold_title),
                    summary = if (uiState.networkSpeedThresholdKb > 0) {
                        "${uiState.networkSpeedThresholdKb} KB/s"
                    } else null,
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.networkSpeedThresholdKb > 0,
                    onCheckedChange = {
                        if (it && threshold == 0) threshold = 1
                        if (!it) {
                            threshold = 0
                            expanded = false
                        } else {
                            expanded = true
                        }
                        onEvent(SystemUIEvent.StatusBar.NetworkSpeedThreshold(threshold))
                    },
                )
            }
            if (expanded && uiState.networkSpeedThresholdKb > 0) {
                add {
                    StatusThemeChildItem {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        ) {
                            OneuiXInlineTextField(
                                value = threshold.toString(),
                                onValueChange = {
                                    threshold = it.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                },
                                modifier = Modifier.weight(1f),
                                labelText = stringResource(
                                    id = R.string.oneuix_networkSpeedThreshold_label
                                ),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OneuiXActionButton(
                                text = stringResource(id = R.string.oneuix_confirm),
                                primary = true,
                                onClick = {
                                    onEvent(SystemUIEvent.StatusBar.NetworkSpeedThreshold(threshold))
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}
