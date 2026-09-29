package com.oneuixp.app.ui.screen.status

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

@Composable
fun StatusNetworkSpeedCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    var thresholdExpanded by rememberSaveable { mutableStateOf(false) }
    var threshold by remember(uiState.networkSpeedThresholdKb) {
        mutableIntStateOf(uiState.networkSpeedThresholdKb)
    }

    var lineGapExpanded by rememberSaveable { mutableStateOf(false) }
    var lineGapDp by remember(uiState.networkSpeedLineGapDp) {
        mutableFloatStateOf(uiState.networkSpeedLineGapDp)
    }

    var textScaleExpanded by rememberSaveable { mutableStateOf(false) }
    var textScale by remember(uiState.networkSpeedTextScale) {
        mutableFloatStateOf(uiState.networkSpeedTextScale)
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
                    icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
                    title = stringResource(id = R.string.oneuix_networkSpeedLineGap_title),
                    summary = if (uiState.modifyNetworkSpeedLineGap) {
                        stringResource(
                            id = R.string.oneuix_networkSpeedLineGap_summary,
                            lineGapDp,
                        )
                    } else null,
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { lineGapExpanded = !lineGapExpanded },
                    checked = uiState.modifyNetworkSpeedLineGap,
                    onCheckedChange = {
                        lineGapExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyNetworkSpeedLineGap(it))
                    },
                )
            }
            if (lineGapExpanded && uiState.modifyNetworkSpeedLineGap) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = lineGapDp,
                            onValueChange = {
                                lineGapDp = (it * 10f).roundToInt() / 10f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            valueRange = -4f..4f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.NetworkSpeedLineGapDp(lineGapDp)
                                )
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.format_size),
                    title = stringResource(id = R.string.oneuix_networkSpeedTextScale_title),
                    summary = if (uiState.setNetworkSpeedTextScale) {
                        stringResource(
                            id = R.string.oneuix_networkSpeedTextScale_summary,
                            textScale,
                        )
                    } else null,
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { textScaleExpanded = !textScaleExpanded },
                    checked = uiState.setNetworkSpeedTextScale,
                    onCheckedChange = {
                        textScaleExpanded = it
                        onEvent(SystemUIEvent.StatusBar.SetNetworkSpeedTextScale(it))
                    },
                )
            }
            if (textScaleExpanded && uiState.setNetworkSpeedTextScale) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = textScale,
                            onValueChange = {
                                textScale = (it * 100f).roundToInt() / 100f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            valueRange = 0.5f..2f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.NetworkSpeedTextScale(textScale)
                                )
                            },
                        )
                    }
                }
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
                    onClick = { thresholdExpanded = !thresholdExpanded },
                    checked = uiState.networkSpeedThresholdKb > 0,
                    onCheckedChange = {
                        if (it && threshold == 0) threshold = 1
                        if (!it) {
                            threshold = 0
                            thresholdExpanded = false
                        } else {
                            thresholdExpanded = true
                        }
                        onEvent(SystemUIEvent.StatusBar.NetworkSpeedThreshold(threshold))
                    },
                )
            }
            if (thresholdExpanded && uiState.networkSpeedThresholdKb > 0) {
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
