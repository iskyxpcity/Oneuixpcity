package com.oneuixp.app.ui.screen.status

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.ONE_UI_VERSION
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SelectItem
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

private val POWER_SLOT_KEYS = listOf(
    "power",
    "current",
    "battery_temp",
    "cpu_temp",
    "voltage",
    "battery_level",
    "none",
)

@Composable
fun StatusPowerCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
    onPowerRuntimePreview: (Preference.SystemUI.StatusBarPowerRuntime) -> Unit,
    onPowerRuntimeChange: (Preference.SystemUI.StatusBarPowerRuntime) -> Unit,
) {
    val powerRuntime = uiState.powerRuntime
    val powerIcon = ImageVector.vectorResource(id = R.drawable.battery)
    val slotEntries = listOf(
        stringResource(id = R.string.status_power_slot_power),
        stringResource(id = R.string.status_power_slot_current),
        stringResource(id = R.string.status_power_slot_battery_temp),
        stringResource(id = R.string.status_power_slot_cpu_temp),
        stringResource(id = R.string.status_power_slot_voltage),
        stringResource(id = R.string.status_power_slot_battery_level),
        stringResource(id = R.string.status_power_slot_none),
    )
    val decimalEntries = listOf("0", "1", "2")

    var powerTextSize by remember(powerRuntime.textSizeSp) {
        mutableFloatStateOf(powerRuntime.textSizeSp)
    }
    var refreshInterval by remember(powerRuntime.refreshIntervalMs) {
        mutableFloatStateOf(powerRuntime.refreshIntervalMs.toFloat())
    }
    var columnGap by remember(powerRuntime.columnGapDp) {
        mutableFloatStateOf(powerRuntime.columnGapDp)
    }
    var horizontalOffset by remember(powerRuntime.horizontalOffsetDp) {
        mutableFloatStateOf(powerRuntime.horizontalOffsetDp)
    }
    var verticalOffset by remember(powerRuntime.verticalOffsetDp) {
        mutableFloatStateOf(powerRuntime.verticalOffsetDp)
    }
    var startPadding by remember(powerRuntime.startPaddingDp) {
        mutableFloatStateOf(powerRuntime.startPaddingDp)
    }
    var endPadding by remember(powerRuntime.endPaddingDp) {
        mutableFloatStateOf(powerRuntime.endPaddingDp)
    }

    var widthScale by remember(uiState.batteryIconWidthScale) {
        mutableFloatStateOf(uiState.batteryIconWidthScale)
    }
    var widthExpanded by rememberSaveable { mutableStateOf(false) }

    var heightScale by remember(uiState.batteryIconHeightScale) {
        mutableFloatStateOf(uiState.batteryIconHeightScale)
    }
    var heightExpanded by rememberSaveable { mutableStateOf(false) }

    fun slotIndex(key: String): Int = POWER_SLOT_KEYS.indexOf(key).coerceAtLeast(0)
    fun slotKey(index: Int): String = POWER_SLOT_KEYS[index.coerceIn(0, POWER_SLOT_KEYS.lastIndex)]

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = powerIcon,
                    title = stringResource(id = R.string.status_power_runtime_enable),
                    summary = stringResource(id = R.string.status_power_runtime_enable_summary),
                    showChildIndicator = true,
                    checked = powerRuntime.enabled,
                    onCheckedChange = {
                        onPowerRuntimeChange(powerRuntime.copy(enabled = it))
                    },
                )
            }

            if (powerRuntime.enabled) {
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_1),
                        entries = slotEntries,
                        selectedIndex = slotIndex(powerRuntime.slot1),
                        onSelectedIndexChange = {
                            onPowerRuntimeChange(powerRuntime.copy(slot1 = slotKey(it)))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_1_decimals),
                        entries = decimalEntries,
                        selectedIndex = powerRuntime.slot1Decimals.coerceIn(0, 2),
                        onSelectedIndexChange = {
                            onPowerRuntimeChange(powerRuntime.copy(slot1Decimals = it.coerceIn(0, 2)))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_2),
                        entries = slotEntries,
                        selectedIndex = slotIndex(powerRuntime.slot2),
                        onSelectedIndexChange = {
                            onPowerRuntimeChange(powerRuntime.copy(slot2 = slotKey(it)))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_2_decimals),
                        entries = decimalEntries,
                        selectedIndex = powerRuntime.slot2Decimals.coerceIn(0, 2),
                        onSelectedIndexChange = {
                            onPowerRuntimeChange(powerRuntime.copy(slot2Decimals = it.coerceIn(0, 2)))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_3),
                        entries = slotEntries,
                        selectedIndex = slotIndex(powerRuntime.slot3),
                        onSelectedIndexChange = {
                            onPowerRuntimeChange(powerRuntime.copy(slot3 = slotKey(it)))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_3_decimals),
                        entries = decimalEntries,
                        selectedIndex = powerRuntime.slot3Decimals.coerceIn(0, 2),
                        onSelectedIndexChange = {
                            onPowerRuntimeChange(powerRuntime.copy(slot3Decimals = it.coerceIn(0, 2)))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_4),
                        entries = slotEntries,
                        selectedIndex = slotIndex(powerRuntime.slot4),
                        onSelectedIndexChange = {
                            onPowerRuntimeChange(powerRuntime.copy(slot4 = slotKey(it)))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_4_decimals),
                        entries = decimalEntries,
                        selectedIndex = powerRuntime.slot4Decimals.coerceIn(0, 2),
                        onSelectedIndexChange = {
                            onPowerRuntimeChange(powerRuntime.copy(slot4Decimals = it.coerceIn(0, 2)))
                        },
                    )
                }

                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_double_line),
                        summary = stringResource(id = R.string.status_power_double_line_summary),
                        checked = powerRuntime.doubleLine,
                        onCheckedChange = {
                            onPowerRuntimeChange(powerRuntime.copy(doubleLine = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_absolute_value),
                        checked = powerRuntime.absoluteValue,
                        onCheckedChange = {
                            onPowerRuntimeChange(powerRuntime.copy(absoluteValue = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_hide_unit),
                        checked = powerRuntime.hideUnit,
                        onCheckedChange = {
                            onPowerRuntimeChange(powerRuntime.copy(hideUnit = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_hide_lockscreen),
                        checked = powerRuntime.hideOnLockscreen,
                        onCheckedChange = {
                            onPowerRuntimeChange(powerRuntime.copy(hideOnLockscreen = it))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_font_weight),
                        entries = listOf("300", "400", "500", "600", "700"),
                        selectedIndex = listOf(300, 400, 500, 600, 700)
                            .indexOf(powerRuntime.fontWeight)
                            .coerceAtLeast(1),
                        onSelectedIndexChange = {
                            val weight = listOf(300, 400, 500, 600, 700)[it.coerceIn(0, 4)]
                            onPowerRuntimeChange(powerRuntime.copy(fontWeight = weight))
                        },
                    )
                }

                add {
                    OneuiXInfoItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_text_size),
                        summary = stringResource(
                            id = R.string.status_power_text_size_summary,
                            powerTextSize,
                        ),
                    )
                }
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = powerTextSize,
                            onValueChange = {
                                powerTextSize = (it * 10f).roundToInt() / 10f
                                onPowerRuntimePreview(powerRuntime.copy(textSizeSp = powerTextSize))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 6f..14f,
                            onValueChangeFinished = {
                                onPowerRuntimeChange(powerRuntime.copy(textSizeSp = powerTextSize))
                            },
                        )
                    }
                }

                add {
                    OneuiXInfoItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_refresh_interval),
                        summary = stringResource(
                            id = R.string.status_power_refresh_interval_summary,
                            refreshInterval.roundToInt(),
                        ),
                    )
                }
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = refreshInterval,
                            onValueChange = {
                                refreshInterval = (it / 100f).roundToInt() * 100f
                                onPowerRuntimePreview(
                                    powerRuntime.copy(refreshIntervalMs = refreshInterval.roundToInt())
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 500f..5000f,
                            onValueChangeFinished = {
                                onPowerRuntimeChange(
                                    powerRuntime.copy(refreshIntervalMs = refreshInterval.roundToInt())
                                )
                            },
                        )
                    }
                }

                add {
                    OneuiXInfoItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_column_gap),
                        summary = stringResource(id = R.string.status_power_dp_summary, columnGap),
                    )
                }
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = columnGap,
                            onValueChange = {
                                columnGap = (it * 10f).roundToInt() / 10f
                                onPowerRuntimePreview(powerRuntime.copy(columnGapDp = columnGap))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..12f,
                            onValueChangeFinished = {
                                onPowerRuntimeChange(powerRuntime.copy(columnGapDp = columnGap))
                            },
                        )
                    }
                }

                add {
                    OneuiXInfoItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_horizontal_offset),
                        summary = stringResource(id = R.string.status_power_dp_summary, horizontalOffset),
                    )
                }
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = horizontalOffset,
                            onValueChange = {
                                horizontalOffset = (it * 10f).roundToInt() / 10f
                                onPowerRuntimePreview(
                                    powerRuntime.copy(horizontalOffsetDp = horizontalOffset)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = -30f..30f,
                            onValueChangeFinished = {
                                onPowerRuntimeChange(
                                    powerRuntime.copy(horizontalOffsetDp = horizontalOffset)
                                )
                            },
                        )
                    }
                }

                add {
                    OneuiXInfoItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_vertical_offset),
                        summary = stringResource(id = R.string.status_power_dp_summary, verticalOffset),
                    )
                }
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = verticalOffset,
                            onValueChange = {
                                verticalOffset = (it * 10f).roundToInt() / 10f
                                onPowerRuntimePreview(
                                    powerRuntime.copy(verticalOffsetDp = verticalOffset)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = -12f..12f,
                            onValueChangeFinished = {
                                onPowerRuntimeChange(
                                    powerRuntime.copy(verticalOffsetDp = verticalOffset)
                                )
                            },
                        )
                    }
                }

                add {
                    OneuiXInfoItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_start_padding),
                        summary = stringResource(id = R.string.status_power_dp_summary, startPadding),
                    )
                }
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = startPadding,
                            onValueChange = {
                                startPadding = (it * 10f).roundToInt() / 10f
                                onPowerRuntimePreview(powerRuntime.copy(startPaddingDp = startPadding))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..12f,
                            onValueChangeFinished = {
                                onPowerRuntimeChange(powerRuntime.copy(startPaddingDp = startPadding))
                            },
                        )
                    }
                }

                add {
                    OneuiXInfoItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_end_padding),
                        summary = stringResource(id = R.string.status_power_dp_summary, endPadding),
                    )
                }
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = endPadding,
                            onValueChange = {
                                endPadding = (it * 10f).roundToInt() / 10f
                                onPowerRuntimePreview(powerRuntime.copy(endPaddingDp = endPadding))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..12f,
                            onValueChangeFinished = {
                                onPowerRuntimeChange(powerRuntime.copy(endPaddingDp = endPadding))
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_setBatteryIconWidthScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.setBatteryIconWidthScale) "%.2f".format(widthScale) else null,
                    icon = powerIcon,
                    clickable = true,
                    onClick = { widthExpanded = !widthExpanded },
                    checked = uiState.setBatteryIconWidthScale,
                    onCheckedChange = {
                        if (it && widthScale == 0f) widthExpanded = true
                        else if (!it) widthExpanded = false
                        onEvent(SystemUIEvent.StatusBar.SetBatteryIconWidthScale(it))
                    },
                )
            }
            if (widthExpanded && uiState.setBatteryIconWidthScale) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = widthScale,
                            onValueChange = { widthScale = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0.5f..2f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.StatusBar.BatteryIconWidthScale(widthScale))
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_setBatteryIconHeightScale_title),
                    modifier = Modifier.animateContentSize(),
                    summary = if (uiState.setBatteryIconHeightScale) "%.2f".format(heightScale) else null,
                    icon = powerIcon,
                    clickable = true,
                    onClick = { heightExpanded = !heightExpanded },
                    checked = uiState.setBatteryIconHeightScale,
                    onCheckedChange = {
                        if (it && heightScale == 0f) heightExpanded = true
                        else if (!it) heightExpanded = false
                        onEvent(SystemUIEvent.StatusBar.SetBatteryIconHeightScale(it))
                    },
                )
            }
            if (heightExpanded && uiState.setBatteryIconHeightScale) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = heightScale,
                            onValueChange = { heightScale = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0.5f..2f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.StatusBar.BatteryIconHeightScale(heightScale))
                            },
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.oneuix_hideBatteryPercentageSign_title),
                        summary = stringResource(id = R.string.oneuix_hideBatteryPercentageSign_summary),
                        checked = uiState.hideBatteryPercentageSign,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.StatusBar.HideBatteryPercentageSign(it))
                        },
                    )
                }
            }

            add {
                SwitchItem(
                    icon = powerIcon,
                    title = stringResource(id = R.string.oneuix_hideBatteryIcon_title),
                    summary = stringResource(id = R.string.oneuix_hideBatteryIcon_summary),
                    checked = uiState.hideBatteryIcon,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.HideBatteryIcon(it))
                    },
                )
            }

            if (ONE_UI_VERSION >= 70000) {
                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.oneuix_addBatteryLevelText_title),
                        summary = stringResource(id = R.string.oneuix_addBatteryLevelText_summary),
                        showChildIndicator = true,
                        checked = uiState.addBatteryLevelText,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.StatusBar.AddBatteryLevelText(it))
                        },
                    )
                }
                if (uiState.addBatteryLevelText) {
                    add {
                        SwitchItem(
                            icon = powerIcon,
                            title = stringResource(id = R.string.oneuix_hideBatteryLevelTextPercentageSign_title),
                            checked = uiState.hideBatteryLevelTextPercentageSign,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.StatusBar.HideBatteryLevelTextPercentageSign(it))
                            },
                        )
                    }
                    add {
                        SwitchItem(
                            icon = powerIcon,
                            title = stringResource(id = R.string.oneuix_hideBatteryLevelTextChargingIcon_title),
                            checked = uiState.hideBatteryLevelTextChargingIcon,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.StatusBar.HideBatteryLevelTextChargingIcon(it))
                            },
                        )
                    }
                }
            }
        },
    )
}
