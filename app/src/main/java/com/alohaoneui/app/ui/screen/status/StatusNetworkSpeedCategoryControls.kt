package com.alohaoneui.app.ui.screen.status

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.category.SystemUIEvent
import com.alohaoneui.mod.ui.component.AlohaOneUIActionButton
import com.alohaoneui.mod.ui.component.AlohaOneUIInlineTextField
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.SwitchItem
import kotlin.math.roundToInt

@Composable
fun StatusNetworkSpeedCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    val toolsViewModel = viewModel<AlohaOneUIToolsViewModel>()

    fun preview(nextStatusBar: Preference.SystemUI.StatusBar) {
        toolsViewModel.previewData { current ->
            current.copy(
                systemUI = current.systemUI.copy(
                    statusBar = nextStatusBar,
                ),
            )
        }
    }

    var thresholdExpanded by rememberSaveable { mutableStateOf(false) }
    var threshold by remember(uiState.networkSpeedThresholdKb) {
        mutableIntStateOf(uiState.networkSpeedThresholdKb)
    }

    var lineGapExpanded by rememberSaveable { mutableStateOf(false) }
    var lineGapDp by remember(uiState.networkSpeedLineGapDp) {
        mutableFloatStateOf(uiState.networkSpeedLineGapDp)
    }

    var uploadScaleExpanded by rememberSaveable { mutableStateOf(false) }
    var uploadScale by remember(uiState.networkSpeedUploadTextScale) {
        mutableFloatStateOf(uiState.networkSpeedUploadTextScale)
    }

    var downloadScaleExpanded by rememberSaveable { mutableStateOf(false) }
    var downloadScale by remember(uiState.networkSpeedDownloadTextScale) {
        mutableFloatStateOf(uiState.networkSpeedDownloadTextScale)
    }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                    title = stringResource(id = R.string.alohaoneui_supportRealTimeNetworkSpeed_title),
                    summary = stringResource(id = R.string.alohaoneui_supportRealTimeNetworkSpeed_summary),
                    checked = uiState.supportRealTimeNetworkSpeed,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.SupportRealTimeNetworkSpeed(it))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                    title = stringResource(id = R.string.alohaoneui_showSeparateUpDownNetworkSpeeds_title),
                    summary = stringResource(id = R.string.alohaoneui_showSeparateUpDownNetworkSpeeds_summary),
                    checked = uiState.showSeparateUpDownNetworkSpeeds,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.ShowSeparateUpDownNetworkSpeeds(it))
                    },
                )
            }

            if (uiState.supportRealTimeNetworkSpeed || uiState.showSeparateUpDownNetworkSpeeds) {
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
                        title = stringResource(id = R.string.alohaoneui_networkSpeedLineGap_title),
                        summary = if (uiState.modifyNetworkSpeedLineGap) {
                            stringResource(
                                id = R.string.alohaoneui_networkSpeedLineGap_summary,
                                lineGapDp,
                            )
                        } else {
                            stringResource(id = R.string.alohaoneui_networkSpeedDoubleLineOnly_summary)
                        },
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
                            AlohaOneUISlider(
                                value = lineGapDp,
                                onValueChange = {
                                    lineGapDp = (it * 10f).roundToInt() / 10f
                                    preview(uiState.copy(networkSpeedLineGapDp = lineGapDp))
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
                        title = stringResource(id = R.string.alohaoneui_networkSpeedUploadTextScale_title),
                        summary = if (uiState.setNetworkSpeedUploadTextScale) {
                            stringResource(
                                id = R.string.alohaoneui_networkSpeedUploadTextScale_summary,
                                uploadScale,
                            )
                        } else {
                            stringResource(id = R.string.alohaoneui_networkSpeedDoubleLineOnly_summary)
                        },
                        modifier = Modifier.animateContentSize(),
                        clickable = true,
                        onClick = { uploadScaleExpanded = !uploadScaleExpanded },
                        checked = uiState.setNetworkSpeedUploadTextScale,
                        onCheckedChange = {
                            uploadScaleExpanded = it
                            onEvent(SystemUIEvent.StatusBar.SetNetworkSpeedUploadTextScale(it))
                        },
                    )
                }
                if (uploadScaleExpanded && uiState.setNetworkSpeedUploadTextScale) {
                    add {
                        StatusThemeChildItem {
                            AlohaOneUISlider(
                                value = uploadScale,
                                onValueChange = {
                                    uploadScale = (it * 100f).roundToInt() / 100f
                                    preview(uiState.copy(networkSpeedUploadTextScale = uploadScale))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                valueRange = 0.5f..2f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.NetworkSpeedUploadTextScale(uploadScale)
                                    )
                                },
                            )
                        }
                    }
                }

                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        title = stringResource(id = R.string.alohaoneui_networkSpeedDownloadTextScale_title),
                        summary = if (uiState.setNetworkSpeedDownloadTextScale) {
                            stringResource(
                                id = R.string.alohaoneui_networkSpeedDownloadTextScale_summary,
                                downloadScale,
                            )
                        } else {
                            stringResource(id = R.string.alohaoneui_networkSpeedDoubleLineOnly_summary)
                        },
                        modifier = Modifier.animateContentSize(),
                        clickable = true,
                        onClick = { downloadScaleExpanded = !downloadScaleExpanded },
                        checked = uiState.setNetworkSpeedDownloadTextScale,
                        onCheckedChange = {
                            downloadScaleExpanded = it
                            onEvent(SystemUIEvent.StatusBar.SetNetworkSpeedDownloadTextScale(it))
                        },
                    )
                }
                if (downloadScaleExpanded && uiState.setNetworkSpeedDownloadTextScale) {
                    add {
                        StatusThemeChildItem {
                            AlohaOneUISlider(
                                value = downloadScale,
                                onValueChange = {
                                    downloadScale = (it * 100f).roundToInt() / 100f
                                    preview(uiState.copy(networkSpeedDownloadTextScale = downloadScale))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                valueRange = 0.5f..2f,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.NetworkSpeedDownloadTextScale(downloadScale)
                                    )
                                },
                            )
                        }
                    }
                }
            }

            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                    title = stringResource(id = R.string.alohaoneui_networkSpeedThreshold_title),
                    summary = if (uiState.networkSpeedThresholdKb > 0) {
                        "${uiState.networkSpeedThresholdKb} KB/s"
                    } else {
                        stringResource(id = R.string.alohaoneui_networkSpeedThreshold_scope_summary)
                    },
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
                            AlohaOneUIInlineTextField(
                                value = threshold.toString(),
                                onValueChange = {
                                    threshold = it.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    preview(uiState.copy(networkSpeedThresholdKb = threshold))
                                },
                                modifier = Modifier.weight(1f),
                                labelText = stringResource(
                                    id = R.string.alohaoneui_networkSpeedThreshold_label
                                ),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AlohaOneUIActionButton(
                                text = stringResource(id = R.string.alohaoneui_confirm),
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

    StatusNetworkSpeedRuntimeControls(uiState = uiState)
}
