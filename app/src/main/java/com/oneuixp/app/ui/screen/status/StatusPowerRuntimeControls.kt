package com.oneuixp.app.ui.screen.status

import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
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

private val POWER_PLACEMENT_KEYS = listOf("right", "left")

@Composable
internal fun StatusPowerRuntimeControls(
    powerRuntime: Preference.SystemUI.StatusBarPowerRuntime,
    onPreview: (Preference.SystemUI.StatusBarPowerRuntime) -> Unit,
    onChange: (Preference.SystemUI.StatusBarPowerRuntime) -> Unit,
) {
    val powerIcon = ImageVector.vectorResource(id = R.drawable.battery)
    val hapticView = LocalView.current

    fun tickIfChanged(previous: Float, next: Float) {
        if (previous != next) {
            hapticView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
    val slotEntries = listOf(
        stringResource(id = R.string.status_power_slot_power),
        stringResource(id = R.string.status_power_slot_current),
        stringResource(id = R.string.status_power_slot_battery_temp),
        stringResource(id = R.string.status_power_slot_cpu_temp),
        stringResource(id = R.string.status_power_slot_voltage),
        stringResource(id = R.string.status_power_slot_battery_level),
        stringResource(id = R.string.status_power_slot_none),
    )
    val placementEntries = listOf(
        stringResource(id = R.string.status_power_placement_right),
        stringResource(id = R.string.status_power_placement_left),
    )
    val decimalEntries = listOf("0", "1", "2")
    val fontWeights = listOf(300, 400, 500, 600, 700, 800, 900)

    var expanded by rememberSaveable { mutableStateOf(powerRuntime.enabled) }
    var powerTextSize by remember(powerRuntime.textSizeSp) {
        mutableFloatStateOf(powerRuntime.textSizeSp)
    }
    var refreshInterval by remember(powerRuntime.refreshIntervalMs) {
        mutableFloatStateOf(powerRuntime.refreshIntervalMs.toFloat())
    }
    var columnGap by remember(powerRuntime.columnGapDp) {
        mutableFloatStateOf(powerRuntime.columnGapDp)
    }
    var rowGap by remember(powerRuntime.rowGapDp) {
        mutableFloatStateOf(powerRuntime.rowGapDp)
    }
    var horizontalOffset by remember(powerRuntime.horizontalOffsetDp) {
        mutableFloatStateOf(powerRuntime.horizontalOffsetDp.coerceIn(-150f, 150f))
    }
    var verticalOffset by remember(powerRuntime.verticalOffsetDp) {
        mutableFloatStateOf(powerRuntime.verticalOffsetDp.coerceIn(-30f, 30f))
    }
    var startPadding by remember(powerRuntime.startPaddingDp) {
        mutableFloatStateOf(powerRuntime.startPaddingDp)
    }
    var endPadding by remember(powerRuntime.endPaddingDp) {
        mutableFloatStateOf(powerRuntime.endPaddingDp)
    }

    fun slotIndex(key: String): Int = POWER_SLOT_KEYS.indexOf(key).coerceAtLeast(0)
    fun slotKey(index: Int): String = POWER_SLOT_KEYS[index.coerceIn(0, POWER_SLOT_KEYS.lastIndex)]
    fun placementIndex(key: String): Int =
        POWER_PLACEMENT_KEYS.indexOf(key).takeIf { it >= 0 } ?: 0
    fun placementKey(index: Int): String =
        POWER_PLACEMENT_KEYS[index.coerceIn(0, POWER_PLACEMENT_KEYS.lastIndex)]

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = powerIcon,
                    title = stringResource(id = R.string.status_power_runtime_enable),
                    summary = stringResource(id = R.string.status_power_runtime_enable_summary),
                    clickable = true,
                    showChildIndicator = true,
                    onClick = {
                        if (powerRuntime.enabled) expanded = !expanded
                    },
                    checked = powerRuntime.enabled,
                    onCheckedChange = { enabled ->
                        expanded = enabled
                        onChange(powerRuntime.copy(enabled = enabled))
                    },
                )
            }

            if (powerRuntime.enabled && expanded) {
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_placement_side),
                        entries = placementEntries,
                        selectedIndex = placementIndex(powerRuntime.placementSide),
                        onSelectedIndexChange = {
                            onChange(powerRuntime.copy(placementSide = placementKey(it)))
                        },
                    )
                }

                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_overlay_on_top),
                        summary = stringResource(id = R.string.status_power_overlay_on_top_summary),
                        checked = powerRuntime.overlayOnTop,
                        onCheckedChange = {
                            onChange(powerRuntime.copy(overlayOnTop = it))
                        },
                    )
                }

                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_slot_1),
                        entries = slotEntries,
                        selectedIndex = slotIndex(powerRuntime.slot1),
                        onSelectedIndexChange = {
                            onChange(powerRuntime.copy(slot1 = slotKey(it)))
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
                            onChange(powerRuntime.copy(slot1Decimals = it.coerceIn(0, 2)))
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
                            onChange(powerRuntime.copy(slot2 = slotKey(it)))
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
                            onChange(powerRuntime.copy(slot2Decimals = it.coerceIn(0, 2)))
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
                            onChange(powerRuntime.copy(slot3 = slotKey(it)))
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
                            onChange(powerRuntime.copy(slot3Decimals = it.coerceIn(0, 2)))
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
                            onChange(powerRuntime.copy(slot4 = slotKey(it)))
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
                            onChange(powerRuntime.copy(slot4Decimals = it.coerceIn(0, 2)))
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
                            onChange(powerRuntime.copy(doubleLine = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_absolute_value),
                        checked = powerRuntime.absoluteValue,
                        onCheckedChange = {
                            onChange(powerRuntime.copy(absoluteValue = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_hide_unit),
                        checked = powerRuntime.hideUnit,
                        onCheckedChange = {
                            onChange(powerRuntime.copy(hideUnit = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_hide_lockscreen),
                        checked = powerRuntime.hideOnLockscreen,
                        onCheckedChange = {
                            onChange(powerRuntime.copy(hideOnLockscreen = it))
                        },
                    )
                }
                add {
                    SelectItem(
                        icon = powerIcon,
                        title = stringResource(id = R.string.status_power_font_weight),
                        entries = fontWeights.map(Int::toString),
                        selectedIndex = fontWeights
                            .indexOf(powerRuntime.fontWeight)
                            .coerceAtLeast(1),
                        onSelectedIndexChange = {
                            val weight = fontWeights[it.coerceIn(0, fontWeights.lastIndex)]
                            onChange(powerRuntime.copy(fontWeight = weight))
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
                                val next = roundPowerHalfStep(it)
                                tickIfChanged(powerTextSize, next)
                                powerTextSize = next
                                onPreview(powerRuntime.copy(textSizeSp = powerTextSize))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 6f..18f,
                            onValueChangeFinished = {
                                onChange(powerRuntime.copy(textSizeSp = powerTextSize))
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
                                val next = (it / 500f).roundToInt() * 500f
                                tickIfChanged(refreshInterval, next)
                                refreshInterval = next
                                onPreview(
                                    powerRuntime.copy(refreshIntervalMs = refreshInterval.roundToInt())
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 500f..5000f,
                            onValueChangeFinished = {
                                onChange(
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
                                val next = roundPowerHalfStep(it)
                                tickIfChanged(columnGap, next)
                                columnGap = next
                                onPreview(powerRuntime.copy(columnGapDp = columnGap))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..12f,
                            onValueChangeFinished = {
                                onChange(powerRuntime.copy(columnGapDp = columnGap))
                            },
                        )
                    }
                }

                if (powerRuntime.doubleLine) {
                    add {
                        OneuiXInfoItem(
                            icon = powerIcon,
                            title = stringResource(id = R.string.status_power_row_gap),
                            summary = stringResource(id = R.string.status_power_dp_summary, rowGap),
                        )
                    }
                    add {
                        StatusThemeChildItem {
                            OneuiXSlider(
                                value = rowGap,
                                onValueChange = {
                                    val next = roundPowerHalfStep(it)
                                    tickIfChanged(rowGap, next)
                                    rowGap = next
                                    onPreview(powerRuntime.copy(rowGapDp = rowGap))
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                valueRange = -4f..8f,
                                onValueChangeFinished = {
                                    onChange(powerRuntime.copy(rowGapDp = rowGap))
                                },
                            )
                        }
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
                                val next = roundPowerHalfStep(it)
                                tickIfChanged(horizontalOffset, next)
                                horizontalOffset = next
                                onPreview(powerRuntime.copy(horizontalOffsetDp = horizontalOffset))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = -150f..150f,
                            onValueChangeFinished = {
                                onChange(powerRuntime.copy(horizontalOffsetDp = horizontalOffset))
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
                                val next = roundPowerHalfStep(it)
                                tickIfChanged(verticalOffset, next)
                                verticalOffset = next
                                onPreview(powerRuntime.copy(verticalOffsetDp = verticalOffset))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = -30f..30f,
                            onValueChangeFinished = {
                                onChange(powerRuntime.copy(verticalOffsetDp = verticalOffset))
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
                                val next = roundPowerHalfStep(it)
                                tickIfChanged(startPadding, next)
                                startPadding = next
                                onPreview(powerRuntime.copy(startPaddingDp = startPadding))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..12f,
                            onValueChangeFinished = {
                                onChange(powerRuntime.copy(startPaddingDp = startPadding))
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
                                val next = roundPowerHalfStep(it)
                                tickIfChanged(endPadding, next)
                                endPadding = next
                                onPreview(powerRuntime.copy(endPaddingDp = endPadding))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..12f,
                            onValueChangeFinished = {
                                onChange(powerRuntime.copy(endPaddingDp = endPadding))
                            },
                        )
                    }
                }
            }
        },
    )
}

private fun roundPowerHalfStep(value: Float): Float =
    (value * 2f).roundToInt() / 2f
