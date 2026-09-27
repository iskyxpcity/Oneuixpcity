package io.github.soclear.oneuix.ui.category

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.soclear.oneuix.common.ONE_UI_VERSION
import io.github.soclear.oneuix.common.PowerMenuAction
import io.github.soclear.oneuix.common.Preference
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.ui.component.SelectItem
import io.github.soclear.oneuix.ui.component.SwitchItem
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.OneuiXPreferenceGroup
import io.github.soclear.oneuix.ui.component.OneuiXDetailGroup
import io.github.soclear.oneuix.ui.component.OneuiXChildVisibility
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem
import io.github.soclear.oneuix.ui.component.OneuiXInlineTextField
import io.github.soclear.oneuix.ui.component.OneuiXSectionLabel
import io.github.soclear.oneuix.ui.component.OneuiXSupportingText
import io.github.soclear.oneuix.ui.component.OneuiXSwitch
import io.github.soclear.oneuix.hook.util.StatusBarClockFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import io.github.soclear.oneuix.common.R as CommonR

private const val ESIM_ADAPTER_SIM_BOTH = 2
private val DOUBLE_LINE_CLOCK_SIZE_VALUES = listOf(
    "small",
    "compact",
    "standard",
    "large",
    "extra_large",
)

private fun legacyDoubleLineClockSize(scale: Float): String = when {
    scale < 0.925f -> "small"
    scale < 0.975f -> "compact"
    scale < 1.025f -> "standard"
    scale < 1.075f -> "large"
    else -> "extra_large"
}

@Composable
fun DetailPaneSystemUI(
    uiState: Preference.SystemUI,
    onEvent: (SystemUIEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DividerText(R.string.oneuix_status_bar)
        OneuiXDetailGroup {
        OneuiXPreferenceGroup {
            var padding by remember {
                mutableFloatStateOf(uiState.statusBar.statusBarLeftPaddingDp)
            }
            var expanded by rememberSaveable { mutableStateOf(false) }

            SwitchItem(
                title = stringResource(id = R.string.oneuix_statusBarLeftPaddingDp_title),
                modifier = Modifier.animateContentSize(),
                summary = if (uiState.statusBar.modifyStatusBarLeftPadding) {
                    "%.1fdp".format(padding)
                } else null,
                icon = ImageVector.vectorResource(id = R.drawable.padding),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.statusBar.modifyStatusBarLeftPadding,
                onCheckedChange = {
                    if (it && padding == 0f) {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.StatusBar.ModifyStatusBarLeftPadding(it))
                }
            )
            OneuiXChildVisibility(expanded && uiState.statusBar.modifyStatusBarLeftPadding) {
                OneuiXSlider(
                    value = padding,
                    onValueChange = { padding = it },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    valueRange = 0f..100f,
                    onValueChangeFinished = {
                        onEvent(SystemUIEvent.StatusBar.StatusBarLeftPaddingDp(padding))
                    }
                )
            }
        }
        OneuiXPreferenceGroup {
            var padding by remember {
                mutableFloatStateOf(uiState.statusBar.statusBarRightPaddingDp)
            }
            var expanded by rememberSaveable { mutableStateOf(false) }

            SwitchItem(
                title = stringResource(id = R.string.oneuix_statusBarRightPaddingDp_title),
                modifier = Modifier.animateContentSize(),
                summary = if (uiState.statusBar.modifyStatusBarRightPadding) {
                    "%.1fdp".format(padding)
                } else null,
                icon = ImageVector.vectorResource(id = R.drawable.padding),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.statusBar.modifyStatusBarRightPadding,
                onCheckedChange = {
                    if (it && padding == 0f) {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.StatusBar.ModifyStatusBarRightPadding(it))
                }
            )
            OneuiXChildVisibility(expanded && uiState.statusBar.modifyStatusBarRightPadding) {
                OneuiXSlider(
                    value = padding,
                    onValueChange = { padding = it },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    valueRange = 0f..100f,
                    onValueChangeFinished = {
                        onEvent(SystemUIEvent.StatusBar.StatusBarRightPaddingDp(padding))
                    }
                )
            }
        }
        StatusBarVerticalPaddingControl(
            title = stringResource(id = R.string.oneuix_statusBarTopPaddingDp_title),
            summaryRes = R.string.oneuix_statusBarTopPaddingDp_summary,
            enabled = uiState.statusBar.modifyStatusBarTopPadding,
            currentValue = uiState.statusBar.statusBarTopPaddingDp,
            onEnabledChange = {
                onEvent(SystemUIEvent.StatusBar.ModifyStatusBarTopPadding(it))
            },
            onValueChangeFinished = {
                onEvent(SystemUIEvent.StatusBar.StatusBarTopPaddingDp(it))
            }
        )
        StatusBarVerticalPaddingControl(
            title = stringResource(id = R.string.oneuix_statusBarBottomPaddingDp_title),
            summaryRes = R.string.oneuix_statusBarBottomPaddingDp_summary,
            enabled = uiState.statusBar.modifyStatusBarBottomPadding,
            currentValue = uiState.statusBar.statusBarBottomPaddingDp,
            onEnabledChange = {
                onEvent(SystemUIEvent.StatusBar.ModifyStatusBarBottomPadding(it))
            },
            onValueChangeFinished = {
                onEvent(SystemUIEvent.StatusBar.StatusBarBottomPaddingDp(it))
            }
        )
        OneuiXPreferenceGroup {
            var widthScale by remember {
                mutableFloatStateOf(uiState.statusBar.batteryIconWidthScale)
            }
            var expanded by rememberSaveable { mutableStateOf(false) }

            SwitchItem(
                title = stringResource(id = R.string.oneuix_setBatteryIconWidthScale_title),
                modifier = Modifier.animateContentSize(),
                summary = if (uiState.statusBar.setBatteryIconWidthScale) {
                    "%.2f".format(widthScale)
                } else null,
                icon = ImageVector.vectorResource(id = R.drawable.battery),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.statusBar.setBatteryIconWidthScale,
                onCheckedChange = {
                    if (it && widthScale == 0f) {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.StatusBar.SetBatteryIconWidthScale(it))
                }
            )
            OneuiXChildVisibility(expanded && uiState.statusBar.setBatteryIconWidthScale) {
                OneuiXSlider(
                    value = widthScale,
                    onValueChange = { widthScale = it },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    valueRange = 0.5f..2f,
                    onValueChangeFinished = {
                        onEvent(SystemUIEvent.StatusBar.BatteryIconWidthScale(widthScale))
                    }
                )
            }
        }
        OneuiXPreferenceGroup {
            var heightScale by remember {
                mutableFloatStateOf(uiState.statusBar.batteryIconHeightScale)
            }
            var expanded by rememberSaveable { mutableStateOf(false) }

            SwitchItem(
                title = stringResource(id = R.string.oneuix_setBatteryIconHeightScale_title),
                modifier = Modifier.animateContentSize(),
                summary = if (uiState.statusBar.setBatteryIconHeightScale) {
                    "%.2f".format(heightScale)
                } else null,
                icon = ImageVector.vectorResource(id = R.drawable.battery),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.statusBar.setBatteryIconHeightScale,
                onCheckedChange = {
                    if (it && heightScale == 0f) {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.StatusBar.SetBatteryIconHeightScale(it))
                }
            )
            OneuiXChildVisibility(expanded && uiState.statusBar.setBatteryIconHeightScale) {
                OneuiXSlider(
                    value = heightScale,
                    onValueChange = { heightScale = it },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    valueRange = 0.5f..2f,
                    onValueChangeFinished = {
                        onEvent(SystemUIEvent.StatusBar.BatteryIconHeightScale(heightScale))
                    }
                )
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.battery),
                title = stringResource(id = R.string.oneuix_hideBatteryPercentageSign_title),
                summary = stringResource(id = R.string.oneuix_hideBatteryPercentageSign_summary),
                checked = uiState.statusBar.hideBatteryPercentageSign,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.HideBatteryPercentageSign(it))
                }
            )
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.battery),
            title = stringResource(id = R.string.oneuix_hideBatteryIcon_title),
            summary = stringResource(id = R.string.oneuix_hideBatteryIcon_summary),
            checked = uiState.statusBar.hideBatteryIcon,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.HideBatteryIcon(it))
            }
        )
        if (ONE_UI_VERSION >= 70000) {
            OneuiXPreferenceGroup {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.battery),
                title = stringResource(id = R.string.oneuix_addBatteryLevelText_title),
                summary = stringResource(id = R.string.oneuix_addBatteryLevelText_summary),
                showChildIndicator = true,
                checked = uiState.statusBar.addBatteryLevelText,
                onCheckedChange = {
                    onEvent(SystemUIEvent.StatusBar.AddBatteryLevelText(it))
                }
            )
            OneuiXChildVisibility(uiState.statusBar.addBatteryLevelText) {
                Column {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.battery),
                        title = stringResource(id = R.string.oneuix_hideBatteryLevelTextPercentageSign_title),
                        checked = uiState.statusBar.hideBatteryLevelTextPercentageSign,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.StatusBar.HideBatteryLevelTextPercentageSign(it))
                        }
                    )
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.battery),
                        title = stringResource(id = R.string.oneuix_hideBatteryLevelTextChargingIcon_title),
                        checked = uiState.statusBar.hideBatteryLevelTextChargingIcon,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.StatusBar.HideBatteryLevelTextChargingIcon(it))
                        }
                    )
                }
            }
            }
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.net_speed),
            title = stringResource(id = R.string.oneuix_supportRealTimeNetworkSpeed_title),
            summary = stringResource(id = R.string.oneuix_supportRealTimeNetworkSpeed_summary),
            checked = uiState.statusBar.supportRealTimeNetworkSpeed,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.SupportRealTimeNetworkSpeed(it))
            }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.net_speed),
            title = stringResource(id = R.string.oneuix_showSeparateUpDownNetworkSpeeds_title),
            summary = stringResource(id = R.string.oneuix_showSeparateUpDownNetworkSpeeds_summary),
            checked = uiState.statusBar.showSeparateUpDownNetworkSpeeds,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.ShowSeparateUpDownNetworkSpeeds(it))
            }
        )
        OneuiXPreferenceGroup {
            var expanded by rememberSaveable { mutableStateOf(false) }
            var threshold by remember {
                mutableIntStateOf(uiState.statusBar.networkSpeedThresholdKb)
            }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.net_speed),
                title = stringResource(id = R.string.oneuix_networkSpeedThreshold_title),
                summary = if (uiState.statusBar.networkSpeedThresholdKb > 0) {
                    "${uiState.statusBar.networkSpeedThresholdKb} KB/s"
                } else null,
                modifier = Modifier.animateContentSize(),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.statusBar.networkSpeedThresholdKb > 0,
                onCheckedChange = {
                    if (it && threshold == 0) threshold = 1
                    if (!it) threshold = 0
                    onEvent(SystemUIEvent.StatusBar.NetworkSpeedThreshold(threshold))
                }
            )
            OneuiXChildVisibility(expanded && uiState.statusBar.networkSpeedThresholdKb > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    OneuiXInlineTextField(
                        value = threshold.toString(),
                        onValueChange = { threshold = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                        modifier = Modifier.weight(1f),
                        labelText = stringResource(id = R.string.oneuix_networkSpeedThreshold_label),
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
        OneuiXPreferenceGroup {
        Column {
            var expanded by rememberSaveable { mutableStateOf(false) }

            SwitchItem(
                title = stringResource(id = R.string.oneuix_setStatusBarClockFormat_title),
                modifier = Modifier.animateContentSize(),
                summary = if (uiState.statusBar.setStatusBarClockFormat) {
                    uiState.statusBar.statusBarClockFormat
                } else null,
                icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.statusBar.setStatusBarClockFormat,
                onCheckedChange = {
                    if (it && uiState.statusBar.statusBarClockFormat == "HH:mm") {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.StatusBar.SetStatusBarClockFormat(it))
                }
            )

            OneuiXChildVisibility(expanded && uiState.statusBar.setStatusBarClockFormat) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    var tempDataTimeFormat by remember {
                        mutableStateOf(uiState.statusBar.statusBarClockFormat)
                    }
                    var label by remember { mutableStateOf("") }
                    OneuiXInlineTextField(
                        value = tempDataTimeFormat,
                        onValueChange = {
                            tempDataTimeFormat = it
                            label = runCatching {
                                StatusBarClockFormatter.format(it, LocalDateTime.now())
                            }.getOrElse { "error" }
                        },
                        modifier = Modifier.weight(1f),
                        labelText = label.takeIf { it.isNotBlank() },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OneuiXActionButton(
                        text = stringResource(id = R.string.oneuix_confirm),
                        primary = true,
                        onClick = {
                            var right = true
                            label = try {
                                StatusBarClockFormatter.format(
                                    tempDataTimeFormat,
                                    LocalDateTime.now()
                                )
                            } catch (_: Throwable) {
                                right = false
                                "error"
                            }
                            if (right) {
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarClockFormat(
                                        tempDataTimeFormat
                                    )
                                )
                            }
                        },
                    )
                }
            }
        }
        if (
            uiState.statusBar.setStatusBarClockFormat &&
                uiState.statusBar.statusBarClockFormat.contains('\n')
        ) {
            Column {
                val isFold7 = Build.MODEL.startsWith("SM-F966", ignoreCase = true)
                if (isFold7) {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        title = stringResource(
                            id = R.string.oneuix_fold7CustomDoubleLineClockScale_title
                        ),
                        showChildIndicator = true,
                checked = uiState.statusBar.useFold7CustomDoubleLineClockScale,
                        onCheckedChange = {
                            onEvent(
                                SystemUIEvent.StatusBar.SetFold7CustomDoubleLineClockScale(it)
                            )
                        }
                    )
                    OneuiXChildVisibility(uiState.statusBar.useFold7CustomDoubleLineClockScale) {
                        Column {
                            var timeScale by remember(
                                uiState.statusBar.fold7DoubleLineClockTimeScale
                            ) {
                                mutableFloatStateOf(
                                    uiState.statusBar.fold7DoubleLineClockTimeScale
                                )
                            }
                            OneuiXInfoItem(
                                title = stringResource(
                                    id = R.string.oneuix_fold7DoubleLineClockTimeScale_title
                                ),
                                summary = stringResource(
                                    id = R.string.oneuix_fold7DoubleLineClockTimeScale_summary,
                                    timeScale,
                                ),
                            )
                            OneuiXSlider(
                                value = timeScale,
                                onValueChange = { timeScale = it },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                valueRange = 0.50f..1.10f,
                                steps = 39,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.Fold7DoubleLineClockTimeScale(
                                            (timeScale * 100f).roundToInt() / 100f
                                        )
                                    )
                                }
                            )
                            var dateScale by remember(
                                uiState.statusBar.fold7DoubleLineClockDateScale
                            ) {
                                mutableFloatStateOf(
                                    uiState.statusBar.fold7DoubleLineClockDateScale
                                )
                            }
                            OneuiXInfoItem(
                                title = stringResource(
                                    id = R.string.oneuix_fold7DoubleLineClockDateScale_title
                                ),
                                summary = stringResource(
                                    id = R.string.oneuix_fold7DoubleLineClockDateScale_summary,
                                    dateScale,
                                ),
                            )
                            OneuiXSlider(
                                value = dateScale,
                                onValueChange = { dateScale = it },
                                modifier = Modifier.padding(horizontal = 16.dp),
                                valueRange = 0.50f..1.10f,
                                steps = 39,
                                onValueChangeFinished = {
                                    onEvent(
                                        SystemUIEvent.StatusBar.Fold7DoubleLineClockDateScale(
                                            (dateScale * 100f).roundToInt() / 100f
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                if (!isFold7) {
                    val sizeLabels = listOf(
                        stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_small),
                        stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_compact),
                        stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_standard),
                        stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_large),
                        stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_extraLarge),
                    )
                    val selectedSize = uiState.statusBar.statusBarDoubleLineClockSize.ifBlank {
                        legacyDoubleLineClockSize(uiState.statusBar.statusBarClockTextScale)
                    }
                    val selectedIndex = DOUBLE_LINE_CLOCK_SIZE_VALUES.indexOf(
                        selectedSize
                    ).coerceAtLeast(0)
                    SelectItem(
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        title = stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_title),
                        entries = sizeLabels,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = {
                            onEvent(
                                SystemUIEvent.StatusBar.StatusBarDoubleLineClockSize(
                                    DOUBLE_LINE_CLOCK_SIZE_VALUES[it]
                                )
                            )
                        }
                    )
                }
                var lineGapDp by remember(uiState.statusBar.doubleLineClockGapDp) {
                    mutableFloatStateOf(uiState.statusBar.doubleLineClockGapDp)
                }
                var gapExpanded by rememberSaveable { mutableStateOf(false) }
                OneuiXPreferenceGroup {
                    SwitchItem(
                        title = stringResource(id = R.string.oneuix_statusBarDoubleLineClockGap_title),
                        summary = if (uiState.statusBar.modifyDoubleLineClockGap) {
                            stringResource(
                                id = R.string.oneuix_statusBarDoubleLineClockGap_summary,
                                lineGapDp
                            )
                        } else null,
                        icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
                        clickable = true,
                        onClick = { gapExpanded = !gapExpanded },
                        checked = uiState.statusBar.modifyDoubleLineClockGap,
                        onCheckedChange = {
                            gapExpanded = it
                            onEvent(SystemUIEvent.StatusBar.ModifyDoubleLineClockGap(it))
                        }
                    )
                    OneuiXChildVisibility(
                        gapExpanded && uiState.statusBar.modifyDoubleLineClockGap
                    ) {
                        OneuiXSlider(
                            value = lineGapDp,
                            onValueChange = { lineGapDp = it },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            valueRange = 0f..2f,
                            steps = 19,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.DoubleLineClockGapDp(
                                        (lineGapDp * 10f).roundToInt() / 10f
                                    )
                                )
                            }
                        )
                    }
                }
                if (!isFold7) {
                    OneuiXSupportingText(
                        text = stringResource(id = R.string.oneuix_statusBarDoubleLineClockSize_summary),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            Column {
                var scale by remember {
                    mutableFloatStateOf(uiState.statusBar.statusBarClockTextScale)
                }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.oneuix_setStatusBarClockTextScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.statusBar.setStatusBarClockTextScale) {
                        "%.2fx".format(scale)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.format_size),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.statusBar.setStatusBarClockTextScale,
                    onCheckedChange = {
                        if (it && scale == 1f) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.StatusBar.SetStatusBarClockTextScale(it))
                    }
                )
                OneuiXChildVisibility(expanded && uiState.statusBar.setStatusBarClockTextScale) {
                    OneuiXSlider(
                        value = scale,
                        onValueChange = { scale = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 0.5f..2.5f,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.StatusBar.StatusBarClockTextScale(scale))
                        }
                    )
                }
            }
        }
        }
        SwitchItem(
            title = stringResource(id = R.string.oneuix_updateStatusBarClockEverySecond_title),
            summary = stringResource(id = R.string.oneuix_updateStatusBarClockEverySecond_summary),
            icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
            checked = uiState.statusBar.updateStatusBarClockEverySecond,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.UpdateStatusBarClockEverySecond(it))
            }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.folder_managed),
            title = stringResource(id = R.string.oneuix_hideSecureFolderStatusBarIcon_title),
            checked = uiState.statusBar.hideSecureFolderStatusBarIcon,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.HideSecureFolderStatusBarIcon(it))
            }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.bluetooth),
            title = stringResource(id = R.string.oneuix_restoreBluetoothStatusBarIcon_title),
            checked = uiState.statusBar.restoreBluetoothStatusBarIcon,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.RestoreBluetoothStatusBarIcon(it))
            }
        )
        OneuiXPreferenceGroup {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.sim_card),
            title = stringResource(id = R.string.oneuix_physicalEsimAdapterWorkaround_title),
            summary = stringResource(id = R.string.oneuix_physicalEsimAdapterWorkaround_summary),
            showChildIndicator = true,
                checked = uiState.statusBar.physicalEsimAdapterWorkaround,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.PhysicalEsimAdapterWorkaround(it))
            }
        )
        OneuiXChildVisibility(uiState.statusBar.physicalEsimAdapterWorkaround) {
            SelectItem(
                icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                title = stringResource(id = R.string.oneuix_physicalEsimAdapterSimSlot_title),
                entries = listOf(
                    stringResource(id = R.string.oneuix_sim_slot_1),
                    stringResource(id = R.string.oneuix_sim_slot_2),
                    stringResource(id = R.string.oneuix_sim_slot_both)
                ),
                selectedIndex = uiState.statusBar.physicalEsimAdapterSimSlot.coerceIn(
                    0,
                    ESIM_ADAPTER_SIM_BOTH
                ),
                onSelectedIndexChange = {
                    onEvent(SystemUIEvent.StatusBar.PhysicalEsimAdapterSimSlot(it))
                }
            )
        }
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
            title = stringResource(id = R.string.oneuix_doubleTapStatusBarToSleep_title),
            checked = uiState.statusBar.doubleTapStatusBarToSleep,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.DoubleTapStatusBarToSleep(it))
            }
        )
        OneuiXPreferenceGroup {
            var max by remember {
                mutableIntStateOf(uiState.statusBar.statusBarMaxNotificationIcons)
            }
            var expanded by rememberSaveable { mutableStateOf(false) }

            SwitchItem(
                title = stringResource(id = R.string.oneuix_setStatusBarMaxNotificationIcons_title),
                modifier = Modifier.animateContentSize(),
                summary = if (uiState.statusBar.modifyStatusBarMaxNotificationIcons) " $max" else null,
                icon = ImageVector.vectorResource(id = R.drawable.notifications),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.statusBar.modifyStatusBarMaxNotificationIcons,
                onCheckedChange = {
                    if (it && max == 4) {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.StatusBar.ModifyStatusBarMaxNotificationIcons(it))
                }
            )
            OneuiXChildVisibility(expanded && uiState.statusBar.modifyStatusBarMaxNotificationIcons) {
                OneuiXSlider(
                    value = max.toFloat(),
                    onValueChange = {
                        val next = it.roundToInt()
                        if (next != max) {
                            max = next
                            onEvent(SystemUIEvent.StatusBar.StatusBarMaxNotificationIcons(next))
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    valueRange = 4f..20f,
                    steps = 15,
                )
            }
        }
        OneuiXPreferenceGroup {
            var expanded by rememberSaveable { mutableStateOf(false) }

            SwitchItem(
                title = stringResource(id = R.string.oneuix_setCustomCarrierName_title),
                modifier = Modifier.animateContentSize(),
                summary = if (uiState.statusBar.setCustomCarrierName) {
                    uiState.statusBar.customCarrierName
                } else null,
                icon = ImageVector.vectorResource(id = R.drawable.sim_card),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.statusBar.setCustomCarrierName,
                onCheckedChange = {
                    if (it && uiState.statusBar.customCarrierName.isEmpty()) {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.StatusBar.SetCustomCarrierName(it))
                }
            )

            OneuiXChildVisibility(expanded && uiState.statusBar.setCustomCarrierName) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    var tempCustomCarrierName by remember {
                        mutableStateOf(uiState.statusBar.customCarrierName)
                    }

                    OneuiXInlineTextField(
                        value = tempCustomCarrierName,
                        onValueChange = { tempCustomCarrierName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OneuiXActionButton(
                        text = stringResource(id = R.string.oneuix_confirm),
                        primary = true,
                        onClick = {
                            onEvent(
                                SystemUIEvent.StatusBar.CustomCarrierName(
                                    tempCustomCarrierName
                                )
                            )
                        },
                    )
                }
            }
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.mobile_screensaver),
            title = stringResource(id = R.string.oneuix_hideLockscreenStatusBar_title),
            checked = uiState.statusBar.hideLockscreenStatusBar,
            onCheckedChange = {
                onEvent(SystemUIEvent.StatusBar.HideLockscreenStatusBar(it))
            }
        )

        }
        DividerText(R.string.oneuix_qs)
        OneuiXDetailGroup {
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
            title = stringResource(id = R.string.oneuix_setQsClockMonospaced_title),
            checked = uiState.qs.setQsClockMonospaced,
            onCheckedChange = {
                onEvent(SystemUIEvent.QS.SetQsClockMonospaced(it))
            }
        )
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                title = stringResource(id = R.string.oneuix_hideDeviceControlQsTile_title),
                checked = uiState.qs.hideDeviceControlQsTile,
                onCheckedChange = {
                    onEvent(SystemUIEvent.QS.HideDeviceControlQsTile(it))
                }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                title = stringResource(id = R.string.oneuix_hideSmartViewQsTile_title),
                checked = uiState.qs.hideSmartViewQsTile,
                onCheckedChange = {
                    onEvent(SystemUIEvent.QS.HideSmartViewQsTile(it))
                }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.five_g),
                title = stringResource(id = R.string.oneuix_turnOn5gQsTile_title),
                summary = stringResource(id = R.string.oneuix_turnOn5gQsTile_summary),
                checked = uiState.qs.turnOn5gQsTile,
                onCheckedChange = {
                    onEvent(SystemUIEvent.QS.TurnOn5gQsTile(it))
                }
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            OneuiXInfoItem(
                title = stringResource(id = R.string.oneuix_turnOn5gQsTile_title),
                summary = stringResource(id = R.string.oneuix_root5gQsTile_summary),
                icon = ImageVector.vectorResource(id = R.drawable.five_g),
            )
            OneuiXInfoItem(
                title = stringResource(id = R.string.oneuix_immersiveModeQsTile_title),
                summary = stringResource(id = R.string.oneuix_rootImmersiveModeQsTile_summary),
                icon = ImageVector.vectorResource(id = R.drawable.immersive_mode),
            )
            if (ONE_UI_VERSION < 80500) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                    title = stringResource(id = R.string.oneuix_hideQsBarMediaPlayer_title),
                    checked = uiState.qs.hideQsBarMediaPlayer,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.HideQsBarMediaPlayer(it))
                    }
                )
            }
            if (ONE_UI_VERSION < 80500) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                    title = stringResource(id = R.string.oneuix_hideQsBarNearbyDevicesAndDeviceControl_title),
                    checked = uiState.qs.hideQsBarNearbyDevicesAndDeviceControl,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.HideQsBarNearbyDevicesAndDeviceControl(it))
                    }
                )
            }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                title = stringResource(id = R.string.oneuix_hideQsBarSecurityFooter_title),
                summary = stringResource(id = R.string.oneuix_hideQsBarSecurityFooter_summary),
                checked = uiState.qs.hideQsBarSecurityFooter,
                onCheckedChange = {
                    onEvent(SystemUIEvent.QS.HideQsBarSecurityFooter(it))
                }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                title = stringResource(id = R.string.oneuix_hideQsBarDataUsage_title),
                checked = uiState.qs.hideQsBarDataUsage,
                onCheckedChange = {
                    onEvent(SystemUIEvent.QS.HideQsBarDataUsage(it))
                }
            )
            if (ONE_UI_VERSION < 80500) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                    title = stringResource(id = R.string.oneuix_hideQsBarSmartViewAndModes_title),
                    checked = uiState.qs.hideQsBarSmartViewAndModes,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.HideQsBarSmartViewAndModes(it))
                    }
                )
            }
            if (ONE_UI_VERSION < 80500) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.expand),
                    title = stringResource(id = R.string.oneuix_alwaysExpandQsTileChunk_title),
                    checked = uiState.qs.alwaysExpandQsTileChunk,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.AlwaysExpandQsTileChunk(it))
                    }
                )
            }
            if (ONE_UI_VERSION < 80500) {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_alwaysShowTimeDateOnQs_title),
                    icon = ImageVector.vectorResource(id = R.drawable.nest_clock_farsight_digital),
                    checked = uiState.qs.alwaysShowTimeDateOnQs,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.AlwaysShowTimeDateOnQs(it))
                    }
                )
            }
            if (ONE_UI_VERSION < 80500) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.light_mode),
                    title = stringResource(id = R.string.oneuix_addBrightnessProgressToQsBar_title),
                    checked = uiState.qs.addBrightnessProgressToQsBar,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.AddBrightnessProgressToQsBar(it))
                    }
                )
            }
            if (ONE_UI_VERSION < 80500) {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.music_note),
                    title = stringResource(id = R.string.oneuix_addVolumeProgressToQsBar_title),
                    checked = uiState.qs.addVolumeProgressToQsBar,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.AddVolumeProgressToQsBar(it))
                    }
                )
            }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.today),
                title = stringResource(id = R.string.oneuix_showTraditionalChineseDateOnQS_title),
                checked = uiState.qs.showTraditionalChineseDateOnQS,
                onCheckedChange = {
                    onEvent(SystemUIEvent.QS.ShowTraditionalChineseDateOnQS(it))
                }
            )
            OneuiXPreferenceGroup {
                var textSize by remember { mutableFloatStateOf(uiState.qs.qsClockTextSize) }
                var expanded by rememberSaveable { mutableStateOf(false) }

                SwitchItem(
                    title = stringResource(id = R.string.oneuix_modifyQSClockTextSize_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.qs.modifyQSClockTextSize) {
                        " %.1fsp".format(textSize)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.format_size),
                    clickable = true,
                    onClick = { expanded = !expanded },
                    checked = uiState.qs.modifyQSClockTextSize,
                    onCheckedChange = {
                        if (it && textSize == 32f) {
                            expanded = true
                        } else if (!it) {
                            expanded = false
                        }
                        onEvent(SystemUIEvent.QS.ModifyQSClockTextSize(it))
                    }
                )

                OneuiXChildVisibility(expanded && uiState.qs.modifyQSClockTextSize) {
                    OneuiXSlider(
                        value = textSize,
                        onValueChange = { textSize = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        valueRange = 15f..70f,
                        onValueChangeFinished = {
                            onEvent(SystemUIEvent.QS.QSClockTextSize(textSize))
                        }
                    )
                }
            }
        }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            DividerText(R.string.oneuix_aod)
            OneuiXDetailGroup {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.battery),
                title = stringResource(id = R.string.oneuix_hideAODStatusBar_title),
                checked = uiState.aod.hideAODStatusBar,
                onCheckedChange = {
                    onEvent(SystemUIEvent.AOD.HideAODStatusBar(it))
                }
            )
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.today),
                title = stringResource(id = R.string.oneuix_aodLockSupportLunar_title),
                checked = uiState.aod.aodLockSupportLunar,
                onCheckedChange = {
                    onEvent(SystemUIEvent.AOD.AODLockSupportLunar(it))
                }
            )
            }
        }

        DividerText(R.string.oneuix_notification)
        OneuiXDetailGroup {
        OneuiXPreferenceGroup {
            var expanded by rememberSaveable { mutableStateOf(false) }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.music_note),
                title = stringResource(id = R.string.oneuix_hideOngoingActivityMedia_title),
                summary = if (uiState.notification.hideOngoingActivityMedia && uiState.notification.hideOngoingActivityMediaPackages.isNotEmpty()) {
                    uiState.notification.hideOngoingActivityMediaPackages
                } else {
                    stringResource(id = R.string.oneuix_hideOngoingActivityMedia_summary)
                },
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.notification.hideOngoingActivityMedia,
                onCheckedChange = {
                    if (it && uiState.notification.hideOngoingActivityMediaPackages.isEmpty()) {
                        expanded = true
                    } else if (!it) {
                        expanded = false
                    }
                    onEvent(SystemUIEvent.Notification.HideOngoingActivityMedia(it))
                }
            )
            OneuiXChildVisibility(expanded && uiState.notification.hideOngoingActivityMedia) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    var tempPackages by remember {
                        mutableStateOf(uiState.notification.hideOngoingActivityMediaPackages)
                    }
                    OneuiXInlineTextField(
                        value = tempPackages,
                        onValueChange = { tempPackages = it },
                        modifier = Modifier.weight(1f),
                        labelText = stringResource(id = R.string.oneuix_hideOngoingActivityMedia_packages_hint),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OneuiXActionButton(
                        text = stringResource(id = R.string.oneuix_confirm),
                        primary = true,
                        onClick = {
                            onEvent(SystemUIEvent.Notification.HideOngoingActivityMediaPackages(tempPackages))
                        },
                    )
                }
            }
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.notifications),
            title = stringResource(id = R.string.oneuix_disableNotificationGrouping_title),
            summary = stringResource(id = R.string.oneuix_disableNotificationGrouping_summary),
            checked = uiState.notification.disableNotificationGrouping,
            onCheckedChange = {
                onEvent(SystemUIEvent.Notification.DisableNotificationGrouping(it))
            }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.notifications),
            title = stringResource(id = R.string.oneuix_autoExpandNotifications_title),
            checked = uiState.notification.autoExpandNotifications,
            onCheckedChange = {
                onEvent(SystemUIEvent.Notification.AutoExpandNotifications(it))
            }
        )

        }
        DividerText(R.string.oneuix_other)
        OneuiXDetailGroup {
        OneuiXPreferenceGroup {
            var expanded by rememberSaveable { mutableStateOf(false) }
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.power_settings_new),
                title = stringResource(id = R.string.oneuix_customPowerMenu_title),
                clickable = true,
                onClick = { expanded = !expanded },
                checked = uiState.other.customPowerMenu,
                onCheckedChange = {
                    expanded = it
                    onEvent(SystemUIEvent.Other.CustomPowerMenu(it))
                }
            )
            AnimatedVisibility(expanded && uiState.other.customPowerMenu) {
                PowerMenuActionEditor(
                    actions = uiState.other.powerMenuActions,
                    onActionsChange = {
                        onEvent(SystemUIEvent.Other.PowerMenuActions(it))
                    }
                )
            }
        }
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.screenshot),
            title = stringResource(id = R.string.oneuix_disableScreenshotCaptureSound_title),
            checked = uiState.other.disableScreenshotCaptureSound,
            onCheckedChange = {
                onEvent(SystemUIEvent.Other.DisableScreenshotCaptureSound(it))
            }
        )
        }
    }
}

@Composable
private fun DividerText(@StringRes id: Int) = OneuiXSectionLabel(
    text = stringResource(id),
    modifier = Modifier.padding(start = 16.dp, top = 32.dp, end = 16.dp),
)

@Composable
private fun PowerMenuActionEditor(
    actions: List<PowerMenuAction>,
    onActionsChange: (List<PowerMenuAction>) -> Unit,
) {
    val normalizedActions = PowerMenuAction.normalize(actions)
    Column {
        normalizedActions.forEachIndexed { index, action ->
            val visiblePosition = normalizedActions.take(index + 1).count { it.visible }
            OneuiXInfoItem(
                title = stringResource(id = powerMenuActionTitle(action.name)),
                summary = if (action.visible) {
                    stringResource(
                        id = R.string.oneuix_powerMenuActionVisible_summary,
                        visiblePosition
                    )
                } else {
                    stringResource(id = R.string.oneuix_powerMenuActionHidden_summary)
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OneuiXActionButton(
                            text = stringResource(id = R.string.oneuix_moveUp),
                            enabled = index > 0,
                            onClick = {
                                onActionsChange(normalizedActions.move(index, index - 1))
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OneuiXActionButton(
                            text = stringResource(id = R.string.oneuix_moveDown),
                            enabled = index < normalizedActions.lastIndex,
                            onClick = {
                                onActionsChange(normalizedActions.move(index, index + 1))
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OneuiXSwitch(
                            checked = action.visible,
                            onCheckedChange = { visible ->
                                onActionsChange(
                                    normalizedActions.mapIndexed { actionIndex, item ->
                                        if (actionIndex == index) item.copy(visible = visible) else item
                                    }
                                )
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun StatusBarVerticalPaddingControl(
    title: String,
    @StringRes summaryRes: Int,
    enabled: Boolean,
    currentValue: Float,
    onEnabledChange: (Boolean) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
) {
    var value by remember(currentValue) { mutableFloatStateOf(currentValue) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    OneuiXPreferenceGroup {
        SwitchItem(
            title = title,
            summary = if (enabled) {
                stringResource(id = summaryRes, value)
            } else null,
            icon = ImageVector.vectorResource(id = R.drawable.padding),
            modifier = Modifier.animateContentSize(),
            clickable = true,
            onClick = { expanded = !expanded },
            checked = enabled,
            onCheckedChange = {
                expanded = it
                onEnabledChange(it)
            },
        )
        OneuiXChildVisibility(expanded && enabled) {
            OneuiXSlider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.padding(horizontal = 16.dp),
                valueRange = 0f..8f,
                steps = 79,
                onValueChangeFinished = {
                    onValueChangeFinished((value * 10f).roundToInt() / 10f)
                }
            )
        }
    }
}

private fun List<PowerMenuAction>.move(fromIndex: Int, toIndex: Int): List<PowerMenuAction> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }

@StringRes
private fun powerMenuActionTitle(actionName: String): Int = when (actionName) {
    PowerMenuAction.POWER -> R.string.oneuix_powerMenuAction_power
    PowerMenuAction.DATA_MODE -> R.string.oneuix_powerMenuAction_dataMode
    PowerMenuAction.RESTART -> R.string.oneuix_powerMenuAction_restart
    PowerMenuAction.SAFE_MODE -> R.string.oneuix_powerMenuAction_safeMode
    PowerMenuAction.LOCK_DOWN_MODE -> R.string.oneuix_powerMenuAction_lockDownMode
    PowerMenuAction.EMERGENCY_CALL -> R.string.oneuix_powerMenuAction_emergencyCall
    PowerMenuAction.MEDICAL_INFO -> R.string.oneuix_powerMenuAction_medicalInfo
    PowerMenuAction.SIDE_KEY_SETTINGS -> R.string.oneuix_sideKeySettings
    PowerMenuAction.FORCE_RESTART_MESSAGE -> R.string.oneuix_powerMenuAction_forceRestartMessage
    PowerMenuAction.RESTART_SYSTEMUI -> CommonR.string.restartSystemUI
    PowerMenuAction.RESTART_RECOVERY -> CommonR.string.restartRecovery
    PowerMenuAction.RESTART_DOWNLOAD -> CommonR.string.restartDownload
    else -> R.string.oneuix_other
}

sealed interface SystemUIEvent {
    sealed interface StatusBar : SystemUIEvent {
        @JvmInline
        value class ModifyStatusBarLeftPadding(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarLeftPaddingDp(val value: Float) : StatusBar

        @JvmInline
        value class ModifyStatusBarRightPadding(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarRightPaddingDp(val value: Float) : StatusBar

        @JvmInline
        value class ModifyStatusBarTopPadding(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarTopPaddingDp(val value: Float) : StatusBar

        @JvmInline
        value class ModifyStatusBarBottomPadding(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarBottomPaddingDp(val value: Float) : StatusBar

        @JvmInline
        value class SetBatteryIconWidthScale(val value: Boolean) : StatusBar

        @JvmInline
        value class BatteryIconWidthScale(val value: Float) : StatusBar

        @JvmInline
        value class SetBatteryIconHeightScale(val value: Boolean) : StatusBar

        @JvmInline
        value class BatteryIconHeightScale(val value: Float) : StatusBar

        @JvmInline
        value class HideBatteryPercentageSign(val value: Boolean) : StatusBar

        @JvmInline
        value class HideBatteryIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class AddBatteryLevelText(val value: Boolean) : StatusBar

        @JvmInline
        value class HideBatteryLevelTextPercentageSign(val value: Boolean) : StatusBar

        @JvmInline
        value class HideBatteryLevelTextChargingIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class SupportRealTimeNetworkSpeed(val value: Boolean) : StatusBar

        @JvmInline
        value class ShowSeparateUpDownNetworkSpeeds(val value: Boolean) : StatusBar

        @JvmInline
        value class NetworkSpeedThreshold(val value: Int) : StatusBar

        @JvmInline
        value class SetStatusBarClockFormat(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarClockFormat(val value: String) : StatusBar

        @JvmInline
        value class StatusBarDoubleLineClockSize(val value: String) : StatusBar

        @JvmInline
        value class ModifyDoubleLineClockGap(val value: Boolean) : StatusBar

        @JvmInline
        value class DoubleLineClockGapDp(val value: Float) : StatusBar

        @JvmInline
        value class SetFold7CustomDoubleLineClockScale(val value: Boolean) : StatusBar

        @JvmInline
        value class Fold7DoubleLineClockTimeScale(val value: Float) : StatusBar

        @JvmInline
        value class Fold7DoubleLineClockDateScale(val value: Float) : StatusBar

        @JvmInline
        value class SetStatusBarClockTextScale(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarClockTextScale(val value: Float) : StatusBar

        @JvmInline
        value class UpdateStatusBarClockEverySecond(val value: Boolean) : StatusBar

        @JvmInline
        value class HideSecureFolderStatusBarIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class RestoreBluetoothStatusBarIcon(val value: Boolean) : StatusBar

        @JvmInline
        value class PhysicalEsimAdapterWorkaround(val value: Boolean) : StatusBar

        @JvmInline
        value class PhysicalEsimAdapterSimSlot(val value: Int) : StatusBar

        @JvmInline
        value class DoubleTapStatusBarToSleep(val value: Boolean) : StatusBar

        @JvmInline
        value class ModifyStatusBarMaxNotificationIcons(val value: Boolean) : StatusBar

        @JvmInline
        value class StatusBarMaxNotificationIcons(val value: Int) : StatusBar

        @JvmInline
        value class SetCustomCarrierName(val value: Boolean) : StatusBar

        @JvmInline
        value class CustomCarrierName(val value: String) : StatusBar

        @JvmInline
        value class HideLockscreenStatusBar(val value: Boolean) : StatusBar
    }

    sealed interface QS : SystemUIEvent {
        @JvmInline
        value class SetQsClockMonospaced(val value: Boolean) : QS

        @JvmInline
        value class HideDeviceControlQsTile(val value: Boolean) : QS

        @JvmInline
        value class HideSmartViewQsTile(val value: Boolean) : QS

        @JvmInline
        value class TurnOn5gQsTile(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarMediaPlayer(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarNearbyDevicesAndDeviceControl(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarSecurityFooter(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarDataUsage(val value: Boolean) : QS

        @JvmInline
        value class HideQsBarSmartViewAndModes(val value: Boolean) : QS

        @JvmInline
        value class AlwaysExpandQsTileChunk(val value: Boolean) : QS

        @JvmInline
        value class AlwaysShowTimeDateOnQs(val value: Boolean) : QS

        @JvmInline
        value class AddBrightnessProgressToQsBar(val value: Boolean) : QS

        @JvmInline
        value class AddVolumeProgressToQsBar(val value: Boolean) : QS

        @JvmInline
        value class ShowTraditionalChineseDateOnQS(val value: Boolean) : QS

        @JvmInline
        value class ModifyQSClockTextSize(val value: Boolean) : QS

        @JvmInline
        value class QSClockTextSize(val value: Float) : QS
    }

    sealed interface AOD : SystemUIEvent {
        @JvmInline
        value class HideAODStatusBar(val value: Boolean) : AOD

        @JvmInline
        value class AODLockSupportLunar(val value: Boolean) : AOD
    }

    sealed interface Notification : SystemUIEvent {
        @JvmInline
        value class HideOngoingActivityMedia(val value: Boolean) : Notification

        @JvmInline
        value class HideOngoingActivityMediaPackages(val value: String) : Notification

        @JvmInline
        value class DisableNotificationGrouping(val value: Boolean) : Notification

        @JvmInline
        value class AutoExpandNotifications(val value: Boolean) : Notification
    }

    sealed interface Other : SystemUIEvent {
        @JvmInline
        value class CustomPowerMenu(val value: Boolean) : Other

        @JvmInline
        value class PowerMenuActions(val value: List<PowerMenuAction>) : Other

        @JvmInline
        value class DisableScreenshotCaptureSound(val value: Boolean) : Other
    }
}

fun OneuiXToolsViewModel.onSystemUIEvent(event: SystemUIEvent) {
    when (event) {
        is SystemUIEvent.StatusBar -> onStatusBarEvent(event)
        is SystemUIEvent.QS -> onQSEvent(event)
        is SystemUIEvent.AOD -> onAODEvent(event)
        is SystemUIEvent.Notification -> onNotification(event)
        is SystemUIEvent.Other -> onOtherEvent(event)
    }
}

private fun OneuiXToolsViewModel.onStatusBarEvent(event: SystemUIEvent.StatusBar) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.StatusBar.ModifyStatusBarLeftPadding -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyStatusBarLeftPadding = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarLeftPaddingDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarLeftPaddingDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.ModifyStatusBarRightPadding -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyStatusBarRightPadding = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarRightPaddingDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarRightPaddingDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.ModifyStatusBarTopPadding -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyStatusBarTopPadding = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarTopPaddingDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarTopPaddingDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.ModifyStatusBarBottomPadding -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyStatusBarBottomPadding = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarBottomPaddingDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarBottomPaddingDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetBatteryIconWidthScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setBatteryIconWidthScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.BatteryIconWidthScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            batteryIconWidthScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideLockscreenStatusBar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideLockscreenStatusBar = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetBatteryIconHeightScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setBatteryIconHeightScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.BatteryIconHeightScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            batteryIconHeightScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideBatteryPercentageSign -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideBatteryPercentageSign = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideBatteryIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideBatteryIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.AddBatteryLevelText -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            addBatteryLevelText = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideBatteryLevelTextPercentageSign -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideBatteryLevelTextPercentageSign = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideBatteryLevelTextChargingIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideBatteryLevelTextChargingIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SupportRealTimeNetworkSpeed -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            supportRealTimeNetworkSpeed = event.value
                        )
                    )
                )
            }


            is SystemUIEvent.StatusBar.ShowSeparateUpDownNetworkSpeeds -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            showSeparateUpDownNetworkSpeeds = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.NetworkSpeedThreshold -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            networkSpeedThresholdKb = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetStatusBarClockFormat -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setStatusBarClockFormat = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockFormat -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockFormat = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarDoubleLineClockSize -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarDoubleLineClockSize = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.ModifyDoubleLineClockGap -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyDoubleLineClockGap = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.DoubleLineClockGapDp -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            doubleLineClockGapDp = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetFold7CustomDoubleLineClockScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            useFold7CustomDoubleLineClockScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.Fold7DoubleLineClockTimeScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            fold7DoubleLineClockTimeScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.Fold7DoubleLineClockDateScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            fold7DoubleLineClockDateScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetStatusBarClockTextScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setStatusBarClockTextScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarClockTextScale -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarClockTextScale = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.UpdateStatusBarClockEverySecond -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            updateStatusBarClockEverySecond = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.HideSecureFolderStatusBarIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            hideSecureFolderStatusBarIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.RestoreBluetoothStatusBarIcon -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            restoreBluetoothStatusBarIcon = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.PhysicalEsimAdapterWorkaround -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            physicalEsimAdapterWorkaround = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.PhysicalEsimAdapterSimSlot -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            physicalEsimAdapterSimSlot = event.value.coerceIn(
                                0,
                                ESIM_ADAPTER_SIM_BOTH
                            )
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.DoubleTapStatusBarToSleep -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            doubleTapStatusBarToSleep = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.ModifyStatusBarMaxNotificationIcons -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            modifyStatusBarMaxNotificationIcons = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.StatusBarMaxNotificationIcons -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            statusBarMaxNotificationIcons = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.SetCustomCarrierName -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            setCustomCarrierName = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.StatusBar.CustomCarrierName -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        statusBar = preference.systemUI.statusBar.copy(
                            customCarrierName = event.value
                        )
                    )
                )
            }
        }
    }
}

private fun OneuiXToolsViewModel.onQSEvent(event: SystemUIEvent.QS) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.QS.SetQsClockMonospaced -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            setQsClockMonospaced = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideDeviceControlQsTile -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideDeviceControlQsTile = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideSmartViewQsTile -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideSmartViewQsTile = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.TurnOn5gQsTile -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            turnOn5gQsTile = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarMediaPlayer -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarMediaPlayer = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarNearbyDevicesAndDeviceControl -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarNearbyDevicesAndDeviceControl = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarSecurityFooter -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarSecurityFooter = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarDataUsage -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarDataUsage = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.HideQsBarSmartViewAndModes -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            hideQsBarSmartViewAndModes = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.AlwaysExpandQsTileChunk -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            alwaysExpandQsTileChunk = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.AlwaysShowTimeDateOnQs -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            alwaysShowTimeDateOnQs = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.AddBrightnessProgressToQsBar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            addBrightnessProgressToQsBar = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.AddVolumeProgressToQsBar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            addVolumeProgressToQsBar = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.ShowTraditionalChineseDateOnQS -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            showTraditionalChineseDateOnQS = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.ModifyQSClockTextSize -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            modifyQSClockTextSize = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.QS.QSClockTextSize -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        qs = preference.systemUI.qs.copy(
                            qsClockTextSize = event.value
                        )
                    )
                )
            }
        }
    }
}

private fun OneuiXToolsViewModel.onAODEvent(event: SystemUIEvent.AOD) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.AOD.HideAODStatusBar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        aod = preference.systemUI.aod.copy(
                            hideAODStatusBar = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.AOD.AODLockSupportLunar -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        aod = preference.systemUI.aod.copy(
                            aodLockSupportLunar = event.value
                        )
                    )
                )
            }
        }
    }
}

private fun OneuiXToolsViewModel.onNotification(event: SystemUIEvent.Notification) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.Notification.HideOngoingActivityMedia -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        notification = preference.systemUI.notification.copy(
                            hideOngoingActivityMedia = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Notification.HideOngoingActivityMediaPackages -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        notification = preference.systemUI.notification.copy(
                            hideOngoingActivityMediaPackages = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Notification.DisableNotificationGrouping -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        notification = preference.systemUI.notification.copy(
                            disableNotificationGrouping = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Notification.AutoExpandNotifications -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        notification = preference.systemUI.notification.copy(
                            autoExpandNotifications = event.value
                        )
                    )
                )
            }
        }
    }
}

private fun OneuiXToolsViewModel.onOtherEvent(event: SystemUIEvent.Other) {
    updateData { preference ->
        when (event) {
            is SystemUIEvent.Other.CustomPowerMenu -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            customPowerMenu = event.value
                        )
                    )
                )
            }

            is SystemUIEvent.Other.PowerMenuActions -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            powerMenuActions = PowerMenuAction.normalize(event.value)
                        )
                    )
                )
            }

            is SystemUIEvent.Other.DisableScreenshotCaptureSound -> {
                preference.copy(
                    systemUI = preference.systemUI.copy(
                        other = preference.systemUI.other.copy(
                            disableScreenshotCaptureSound = event.value
                        )
                    )
                )
            }
        }
    }
}
