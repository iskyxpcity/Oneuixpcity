package com.oneuixp.app.ui.screen.status

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneuixp.app.R
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import com.oneuixp.mod.ui.component.OneuiXPActionButton
import com.oneuixp.mod.ui.component.OneuiXPInfoItem
import com.oneuixp.mod.ui.component.OneuiXPInlineTextField
import com.oneuixp.mod.ui.component.OneuiXPSlider
import io.github.soclear.oneuix.ui.component.SelectItem
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

private const val STATUS_SIGNAL_ESIM_BOTH = 2

@Composable
fun StatusSignalCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    val signalIcon = ImageVector.vectorResource(id = R.drawable.flux_status_signal)
    val toolsViewModel = viewModel<OneuiXPToolsViewModel>()
    val context = LocalContext.current
    val runtime = uiState.signalRuntime

    var runtimeExpanded by rememberSaveable { mutableStateOf(runtime.enabled) }
    var portraitSize by remember(runtime.portraitSizeDp) {
        mutableFloatStateOf(runtime.portraitSizeDp)
    }
    var landscapeSize by remember(runtime.landscapeSizeDp) {
        mutableFloatStateOf(runtime.landscapeSizeDp)
    }
    var verticalOffset by remember(runtime.verticalOffsetDp) {
        mutableFloatStateOf(runtime.verticalOffsetDp.coerceIn(-15f, 15f))
    }
    var sideMargin by remember(runtime.startMarginDp, runtime.endMarginDp) {
        mutableFloatStateOf(runtime.startMarginDp.coerceIn(-6f, 6f))
    }

    fun preview(next: Preference.SystemUI.StatusBarSignalRuntime) {
        // Runtime-only preview: finger-following feedback without preference writes.
        val payload = runCatching {
            IgnoreUnknownKeysJson.encodeToString(
                Preference.SystemUI.StatusBar.serializer(),
                uiState.copy(signalRuntime = next),
            )
        }.getOrNull() ?: return
        runCatching {
            context.sendBroadcast(
                Intent(StatusBarRuntimeBridge.ACTION)
                    .setPackage(Package.SYSTEMUI)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON, payload)
            )
        }
    }

    fun persist(next: Preference.SystemUI.StatusBarSignalRuntime) {
        toolsViewModel.updateData { current ->
            current.copy(
                systemUI = current.systemUI.copy(
                    statusBar = current.systemUI.statusBar.copy(signalRuntime = next),
                ),
            )
        }
    }

    fun previewAndPersist(next: Preference.SystemUI.StatusBarSignalRuntime) {
        preview(next)
        persist(next)
    }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = signalIcon,
                    title = stringResource(id = R.string.status_signal_runtime_enable),
                    summary = stringResource(id = R.string.status_signal_runtime_enable_summary),
                    clickable = true,
                    showChildIndicator = true,
                    onClick = {
                        if (runtime.enabled) runtimeExpanded = !runtimeExpanded
                    },
                    checked = runtime.enabled,
                    onCheckedChange = { enabled ->
                        runtimeExpanded = enabled
                        persist(runtime.copy(enabled = enabled))
                    },
                )
            }
            if (runtime.enabled && runtimeExpanded) {
                add {
                    SwitchItem(
                        icon = signalIcon,
                        title = stringResource(id = R.string.status_signal_show_sim1),
                        checked = runtime.showSim1,
                        onCheckedChange = { show ->
                            previewAndPersist(runtime.copy(showSim1 = show))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = signalIcon,
                        title = stringResource(id = R.string.status_signal_show_sim2),
                        checked = runtime.showSim2,
                        onCheckedChange = { show ->
                            previewAndPersist(runtime.copy(showSim2 = show))
                        },
                    )
                }
                add {
                    SignalSliderSetting(
                        icon = signalIcon,
                        title = stringResource(id = R.string.status_signal_portrait_size),
                        summary = stringResource(id = R.string.status_signal_dp_summary, portraitSize),
                        value = portraitSize,
                        range = 8f..32f,
                        onValueChange = {
                            portraitSize = roundSignalValue(it)
                            preview(runtime.copy(portraitSizeDp = portraitSize))
                        },
                        onFinished = { persist(runtime.copy(portraitSizeDp = portraitSize)) },
                    )
                }
                add {
                    SignalSliderSetting(
                        icon = signalIcon,
                        title = stringResource(id = R.string.status_signal_landscape_size),
                        summary = stringResource(id = R.string.status_signal_dp_summary, landscapeSize),
                        value = landscapeSize,
                        range = 8f..32f,
                        onValueChange = {
                            landscapeSize = roundSignalValue(it)
                            preview(runtime.copy(landscapeSizeDp = landscapeSize))
                        },
                        onFinished = { persist(runtime.copy(landscapeSizeDp = landscapeSize)) },
                    )
                }
                add {
                    SignalSliderSetting(
                        icon = signalIcon,
                        title = stringResource(id = R.string.status_signal_vertical_offset),
                        summary = stringResource(id = R.string.status_signal_dp_summary, verticalOffset),
                        value = verticalOffset,
                        range = -15f..15f,
                        onValueChange = {
                            verticalOffset = roundSignalValue(it)
                            preview(runtime.copy(verticalOffsetDp = verticalOffset))
                        },
                        onFinished = { persist(runtime.copy(verticalOffsetDp = verticalOffset)) },
                    )
                }
                add {
                    SignalSliderSetting(
                        icon = signalIcon,
                        title = stringResource(id = R.string.status_signal_start_margin),
                        summary = stringResource(id = R.string.status_signal_dp_summary, sideMargin),
                        value = sideMargin,
                        range = -6f..6f,
                        onValueChange = {
                            sideMargin = roundSignalValue(it)
                            preview(
                                runtime.copy(
                                    startMarginDp = sideMargin,
                                    endMarginDp = sideMargin,
                                ),
                            )
                        },
                        onFinished = {
                            persist(
                                runtime.copy(
                                    startMarginDp = sideMargin,
                                    endMarginDp = sideMargin,
                                ),
                            )
                        },
                    )
                }
            }
        },
    )

    var carrierExpanded by rememberSaveable { mutableStateOf(false) }
    var carrier by remember(uiState.customCarrierName) {
        mutableStateOf(uiState.customCarrierName)
    }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.folder_managed),
                    title = stringResource(id = R.string.oneuixp_hideSecureFolderStatusBarIcon_title),
                    checked = uiState.hideSecureFolderStatusBarIcon,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.HideSecureFolderStatusBarIcon(it))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                    title = stringResource(id = R.string.oneuixp_physicalEsimAdapterWorkaround_title),
                    summary = stringResource(id = R.string.oneuixp_physicalEsimAdapterWorkaround_summary),
                    showChildIndicator = true,
                    checked = uiState.physicalEsimAdapterWorkaround,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.PhysicalEsimAdapterWorkaround(it))
                    },
                )
            }
            if (uiState.physicalEsimAdapterWorkaround) {
                add {
                    SelectItem(
                        icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                        title = stringResource(id = R.string.oneuixp_physicalEsimAdapterSimSlot_title),
                        entries = listOf(
                            stringResource(id = R.string.oneuixp_sim_slot_1),
                            stringResource(id = R.string.oneuixp_sim_slot_2),
                            stringResource(id = R.string.oneuixp_sim_slot_both),
                        ),
                        selectedIndex = uiState.physicalEsimAdapterSimSlot.coerceIn(
                            0,
                            STATUS_SIGNAL_ESIM_BOTH,
                        ),
                        onSelectedIndexChange = {
                            onEvent(SystemUIEvent.StatusBar.PhysicalEsimAdapterSimSlot(it))
                        },
                    )
                }
            }
            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuixp_setCustomCarrierName_title),
                    summary = if (uiState.setCustomCarrierName) uiState.customCarrierName else null,
                    icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { carrierExpanded = !carrierExpanded },
                    checked = uiState.setCustomCarrierName,
                    onCheckedChange = {
                        if (it && uiState.customCarrierName.isEmpty()) carrierExpanded = true
                        if (!it) carrierExpanded = false
                        onEvent(SystemUIEvent.StatusBar.SetCustomCarrierName(it))
                    },
                )
            }
            if (carrierExpanded && uiState.setCustomCarrierName) {
                add {
                    StatusThemeChildItem {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        ) {
                            OneuiXPInlineTextField(
                                value = carrier,
                                onValueChange = { carrier = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OneuiXPActionButton(
                                text = stringResource(id = R.string.oneuixp_confirm),
                                primary = true,
                                onClick = {
                                    onEvent(SystemUIEvent.StatusBar.CustomCarrierName(carrier))
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun SignalSliderSetting(
    icon: ImageVector,
    title: String,
    summary: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    val view = LocalView.current
    var lastHapticStep by remember { mutableIntStateOf(value.roundToInt()) }

    Column {
        OneuiXPInfoItem(icon = icon, title = title, summary = summary)
        StatusThemeChildItem {
            OneuiXPSlider(
                value = value,
                onValueChange = { raw ->
                    val quantized = raw.roundToInt().toFloat().coerceIn(
                        range.start,
                        range.endInclusive,
                    )
                    val step = quantized.roundToInt()
                    if (step != lastHapticStep) {
                        lastHapticStep = step
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    onValueChange(quantized)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                valueRange = range,
                onValueChangeFinished = onFinished,
            )
        }
    }
}

private fun roundSignalValue(value: Float): Float = value.roundToInt().toFloat()
