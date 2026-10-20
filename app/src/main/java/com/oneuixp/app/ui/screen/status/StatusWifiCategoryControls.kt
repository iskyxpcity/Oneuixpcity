package com.oneuialoha.app.ui.screen.status

import androidx.compose.foundation.layout.Column
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
import com.oneuialoha.app.R
import com.oneuialoha.common.Preference
import com.oneuialoha.mod.ui.component.OneUIAlohaInfoItem
import com.oneuialoha.mod.ui.component.OneUIAlohaSlider
import com.oneuialoha.mod.ui.component.SelectItem
import com.oneuialoha.mod.ui.component.SwitchItem
import kotlin.math.roundToInt

private val WIFI_PRESET_KEYS = listOf("default", "compact", "wide")

@Composable
fun StatusWifiCategoryControls(
    wifiRuntime: Preference.SystemUI.StatusBarWifiRuntime,
    onPreview: (Preference.SystemUI.StatusBarWifiRuntime) -> Unit,
    onChange: (Preference.SystemUI.StatusBarWifiRuntime) -> Unit,
) {
    val wifiIcon = ImageVector.vectorResource(id = R.drawable.flux_status_wifi)
    val presetEntries = listOf(
        stringResource(id = R.string.status_wifi_preset_default),
        stringResource(id = R.string.status_wifi_preset_compact),
        stringResource(id = R.string.status_wifi_preset_wide),
    )
    val arcCountEntries = listOf("3", "4")

    var expanded by rememberSaveable { mutableStateOf(wifiRuntime.enabled) }
    var showArcEditor by rememberSaveable { mutableStateOf(false) }
    var size by remember(wifiRuntime.overallSizeDp) { mutableFloatStateOf(wifiRuntime.overallSizeDp) }
    var depth by remember(wifiRuntime.blackDepth) { mutableFloatStateOf(wifiRuntime.blackDepth) }
    var verticalOffset by remember(wifiRuntime.verticalOffsetDp) {
        mutableFloatStateOf(wifiRuntime.verticalOffsetDp)
    }
    var startMargin by remember(wifiRuntime.startMarginDp) {
        mutableFloatStateOf(wifiRuntime.startMarginDp)
    }
    var endMargin by remember(wifiRuntime.endMarginDp) {
        mutableFloatStateOf(wifiRuntime.endMarginDp)
    }
    val arcStates = listOf(
        remember(wifiRuntime.arc1) { mutableStateOf(wifiRuntime.arc1) },
        remember(wifiRuntime.arc2) { mutableStateOf(wifiRuntime.arc2) },
        remember(wifiRuntime.arc3) { mutableStateOf(wifiRuntime.arc3) },
        remember(wifiRuntime.arc4) { mutableStateOf(wifiRuntime.arc4) },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = wifiIcon,
                    title = stringResource(id = R.string.status_wifi_runtime_enable),
                    summary = stringResource(id = R.string.status_wifi_runtime_enable_summary),
                    clickable = true,
                    showChildIndicator = true,
                    onClick = {
                        if (wifiRuntime.enabled) expanded = !expanded
                    },
                    checked = wifiRuntime.enabled,
                    onCheckedChange = { enabled ->
                        expanded = enabled
                        onChange(wifiRuntime.copy(enabled = enabled))
                    },
                )
            }

            if (wifiRuntime.enabled && expanded) {
                add {
                    SwitchItem(
                        icon = wifiIcon,
                        title = stringResource(id = R.string.status_wifi_editor_enable),
                        summary = stringResource(id = R.string.status_wifi_editor_enable_summary),
                        checked = wifiRuntime.editorEnabled,
                        onCheckedChange = {
                            onChange(wifiRuntime.copy(editorEnabled = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = wifiIcon,
                        title = stringResource(id = R.string.status_wifi_hide_activity),
                        checked = wifiRuntime.hideActivity,
                        onCheckedChange = {
                            onChange(wifiRuntime.copy(hideActivity = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = wifiIcon,
                        title = stringResource(id = R.string.status_wifi_hide_standard_badge),
                        summary = stringResource(id = R.string.status_wifi_hide_standard_badge_summary),
                        checked = wifiRuntime.hideStandardBadge,
                        onCheckedChange = {
                            onChange(wifiRuntime.copy(hideStandardBadge = it))
                        },
                    )
                }
                if (!wifiRuntime.hideStandardBadge) {
                    add {
                        SwitchItem(
                            icon = wifiIcon,
                            title = stringResource(id = R.string.status_wifi_extend_standard_badge),
                            summary = stringResource(id = R.string.status_wifi_extend_standard_badge_summary),
                            checked = wifiRuntime.extendStandardBadge,
                            onCheckedChange = {
                                onChange(wifiRuntime.copy(extendStandardBadge = it))
                            },
                        )
                    }
                }

                add {
                    WifiSliderSetting(
                        icon = wifiIcon,
                        title = stringResource(id = R.string.status_wifi_overall_size),
                        summary = stringResource(id = R.string.status_wifi_dp_summary, size),
                        value = size,
                        range = 8f..32f,
                        onValueChange = {
                            size = round1(it)
                            onPreview(wifiRuntime.copy(overallSizeDp = size))
                        },
                        onFinished = {
                            onChange(wifiRuntime.copy(overallSizeDp = size))
                        },
                    )
                }
                add {
                    WifiSliderSetting(
                        icon = wifiIcon,
                        title = stringResource(id = R.string.status_wifi_black_depth),
                        summary = stringResource(id = R.string.status_wifi_percent_summary, depth * 100f),
                        value = depth,
                        range = 0.25f..1f,
                        onValueChange = {
                            depth = round2(it)
                            onPreview(wifiRuntime.copy(blackDepth = depth))
                        },
                        onFinished = {
                            onChange(wifiRuntime.copy(blackDepth = depth))
                        },
                    )
                }
                add {
                    WifiSliderSetting(
                        icon = wifiIcon,
                        title = stringResource(id = R.string.status_wifi_vertical_offset),
                        summary = stringResource(id = R.string.status_wifi_dp_summary, verticalOffset),
                        value = verticalOffset,
                        range = -24f..24f,
                        onValueChange = {
                            verticalOffset = round1(it)
                            onPreview(wifiRuntime.copy(verticalOffsetDp = verticalOffset))
                        },
                        onFinished = {
                            onChange(wifiRuntime.copy(verticalOffsetDp = verticalOffset))
                        },
                    )
                }
                add {
                    WifiSliderSetting(
                        icon = wifiIcon,
                        title = stringResource(id = R.string.status_wifi_start_margin),
                        summary = stringResource(id = R.string.status_wifi_dp_summary, startMargin),
                        value = startMargin,
                        range = -12f..36f,
                        onValueChange = {
                            startMargin = round1(it)
                            onPreview(wifiRuntime.copy(startMarginDp = startMargin))
                        },
                        onFinished = {
                            onChange(wifiRuntime.copy(startMarginDp = startMargin))
                        },
                    )
                }
                add {
                    WifiSliderSetting(
                        icon = wifiIcon,
                        title = stringResource(id = R.string.status_wifi_end_margin),
                        summary = stringResource(id = R.string.status_wifi_dp_summary, endMargin),
                        value = endMargin,
                        range = -12f..36f,
                        onValueChange = {
                            endMargin = round1(it)
                            onPreview(wifiRuntime.copy(endMarginDp = endMargin))
                        },
                        onFinished = {
                            onChange(wifiRuntime.copy(endMarginDp = endMargin))
                        },
                    )
                }

                if (wifiRuntime.editorEnabled) {
                    add {
                        SelectItem(
                            icon = wifiIcon,
                            title = stringResource(id = R.string.status_wifi_arc_count),
                            entries = arcCountEntries,
                            selectedIndex = (wifiRuntime.arcCount - 3).coerceIn(0, 1),
                            onSelectedIndexChange = {
                                onChange(wifiRuntime.copy(arcCount = it.coerceIn(0, 1) + 3))
                            },
                        )
                    }
                    add {
                        SelectItem(
                            icon = wifiIcon,
                            title = stringResource(id = R.string.status_wifi_preset),
                            entries = presetEntries,
                            selectedIndex = WIFI_PRESET_KEYS
                                .indexOf(wifiRuntime.preset)
                                .takeIf { it >= 0 } ?: 0,
                            onSelectedIndexChange = { index ->
                                val key = WIFI_PRESET_KEYS[index.coerceIn(0, WIFI_PRESET_KEYS.lastIndex)]
                                val next = applyWifiPreset(wifiRuntime, key)
                                arcStates[0].value = next.arc1
                                arcStates[1].value = next.arc2
                                arcStates[2].value = next.arc3
                                arcStates[3].value = next.arc4
                                onChange(next)
                            },
                        )
                    }
                    add {
                        SwitchItem(
                            icon = wifiIcon,
                            title = stringResource(id = R.string.status_wifi_advanced_arc_editor),
                            summary = stringResource(id = R.string.status_wifi_advanced_arc_editor_summary),
                            clickable = true,
                            showChildIndicator = true,
                            onClick = { showArcEditor = !showArcEditor },
                            checked = showArcEditor,
                            onCheckedChange = { showArcEditor = it },
                        )
                    }

                    if (showArcEditor) {
                        repeat(wifiRuntime.arcCount.coerceIn(3, 4)) { index ->
                            val state = arcStates[index]
                            add {
                                OneUIAlohaInfoItem(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_title, index + 1),
                                    summary = stringResource(id = R.string.status_wifi_arc_title_summary),
                                )
                            }
                            add {
                                WifiSliderSetting(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_length),
                                    summary = stringResource(
                                        id = R.string.status_wifi_percent_summary,
                                        state.value.length * 100f,
                                    ),
                                    value = state.value.length,
                                    range = 0.35f..1.2f,
                                    onValueChange = { value ->
                                        state.value = state.value.copy(length = round2(value))
                                        onPreview(withArc(wifiRuntime, index, state.value))
                                    },
                                    onFinished = {
                                        onChange(withArc(wifiRuntime, index, state.value).copy(preset = "custom"))
                                    },
                                )
                            }
                            add {
                                WifiSliderSetting(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_width),
                                    summary = stringResource(
                                        id = R.string.status_wifi_dp_summary,
                                        state.value.strokeWidthDp,
                                    ),
                                    value = state.value.strokeWidthDp,
                                    range = 0.5f..4f,
                                    onValueChange = { value ->
                                        state.value = state.value.copy(strokeWidthDp = round1(value))
                                        onPreview(withArc(wifiRuntime, index, state.value))
                                    },
                                    onFinished = {
                                        onChange(withArc(wifiRuntime, index, state.value).copy(preset = "custom"))
                                    },
                                )
                            }
                            add {
                                WifiSliderSetting(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_left_radius),
                                    summary = stringResource(
                                        id = R.string.status_wifi_percent_summary,
                                        state.value.leftRadius * 100f,
                                    ),
                                    value = state.value.leftRadius,
                                    range = 0.5f..1.5f,
                                    onValueChange = { value ->
                                        state.value = state.value.copy(leftRadius = round2(value))
                                        onPreview(withArc(wifiRuntime, index, state.value))
                                    },
                                    onFinished = {
                                        onChange(withArc(wifiRuntime, index, state.value).copy(preset = "custom"))
                                    },
                                )
                            }
                            add {
                                WifiSliderSetting(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_right_radius),
                                    summary = stringResource(
                                        id = R.string.status_wifi_percent_summary,
                                        state.value.rightRadius * 100f,
                                    ),
                                    value = state.value.rightRadius,
                                    range = 0.5f..1.5f,
                                    onValueChange = { value ->
                                        state.value = state.value.copy(rightRadius = round2(value))
                                        onPreview(withArc(wifiRuntime, index, state.value))
                                    },
                                    onFinished = {
                                        onChange(withArc(wifiRuntime, index, state.value).copy(preset = "custom"))
                                    },
                                )
                            }
                            add {
                                WifiSliderSetting(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_left_tilt),
                                    summary = stringResource(
                                        id = R.string.status_wifi_degree_summary,
                                        state.value.leftTiltDeg,
                                    ),
                                    value = state.value.leftTiltDeg,
                                    range = -35f..35f,
                                    onValueChange = { value ->
                                        state.value = state.value.copy(leftTiltDeg = round1(value))
                                        onPreview(withArc(wifiRuntime, index, state.value))
                                    },
                                    onFinished = {
                                        onChange(withArc(wifiRuntime, index, state.value).copy(preset = "custom"))
                                    },
                                )
                            }
                            add {
                                WifiSliderSetting(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_right_tilt),
                                    summary = stringResource(
                                        id = R.string.status_wifi_degree_summary,
                                        state.value.rightTiltDeg,
                                    ),
                                    value = state.value.rightTiltDeg,
                                    range = -35f..35f,
                                    onValueChange = { value ->
                                        state.value = state.value.copy(rightTiltDeg = round1(value))
                                        onPreview(withArc(wifiRuntime, index, state.value))
                                    },
                                    onFinished = {
                                        onChange(withArc(wifiRuntime, index, state.value).copy(preset = "custom"))
                                    },
                                )
                            }
                            add {
                                WifiSliderSetting(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_bend),
                                    summary = stringResource(
                                        id = R.string.status_wifi_signed_summary,
                                        state.value.bend,
                                    ),
                                    value = state.value.bend,
                                    range = -1f..1f,
                                    onValueChange = { value ->
                                        state.value = state.value.copy(bend = round2(value))
                                        onPreview(withArc(wifiRuntime, index, state.value))
                                    },
                                    onFinished = {
                                        onChange(withArc(wifiRuntime, index, state.value).copy(preset = "custom"))
                                    },
                                )
                            }
                            add {
                                WifiSliderSetting(
                                    icon = wifiIcon,
                                    title = stringResource(id = R.string.status_wifi_arc_y_offset),
                                    summary = stringResource(
                                        id = R.string.status_wifi_dp_summary,
                                        state.value.yOffsetDp,
                                    ),
                                    value = state.value.yOffsetDp,
                                    range = -10f..10f,
                                    onValueChange = { value ->
                                        state.value = state.value.copy(yOffsetDp = round1(value))
                                        onPreview(withArc(wifiRuntime, index, state.value))
                                    },
                                    onFinished = {
                                        onChange(withArc(wifiRuntime, index, state.value).copy(preset = "custom"))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun WifiSliderSetting(
    icon: ImageVector,
    title: String,
    summary: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Column {
        OneUIAlohaInfoItem(
            icon = icon,
            title = title,
            summary = summary,
        )
        StatusThemeChildItem {
            OneUIAlohaSlider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                valueRange = range,
                onValueChangeFinished = onFinished,
            )
        }
    }
}

private fun withArc(
    runtime: Preference.SystemUI.StatusBarWifiRuntime,
    index: Int,
    arc: Preference.SystemUI.StatusBarWifiArcStyle,
): Preference.SystemUI.StatusBarWifiRuntime = when (index) {
    0 -> runtime.copy(arc1 = arc)
    1 -> runtime.copy(arc2 = arc)
    2 -> runtime.copy(arc3 = arc)
    else -> runtime.copy(arc4 = arc)
}

private fun applyWifiPreset(
    runtime: Preference.SystemUI.StatusBarWifiRuntime,
    key: String,
): Preference.SystemUI.StatusBarWifiRuntime {
    fun arc(
        length: Float,
        width: Float,
        radius: Float,
        bend: Float,
        y: Float,
    ) = Preference.SystemUI.StatusBarWifiArcStyle(
        length = length,
        strokeWidthDp = width,
        leftRadius = radius,
        rightRadius = radius,
        bend = bend,
        yOffsetDp = y,
    )

    return when (key) {
        "compact" -> runtime.copy(
            preset = key,
            overallSizeDp = 14f,
            arc1 = arc(0.88f, 1.45f, 0.9f, 0.08f, -0.4f),
            arc2 = arc(0.72f, 1.4f, 0.9f, 0.06f, 0.1f),
            arc3 = arc(0.56f, 1.35f, 0.9f, 0.04f, 0.6f),
            arc4 = arc(0.42f, 1.3f, 0.9f, 0.02f, 1f),
        )
        "wide" -> runtime.copy(
            preset = key,
            overallSizeDp = 18f,
            arc1 = arc(1.12f, 1.55f, 1.08f, 0.12f, -0.8f),
            arc2 = arc(0.94f, 1.5f, 1.08f, 0.09f, -0.2f),
            arc3 = arc(0.76f, 1.45f, 1.08f, 0.06f, 0.4f),
            arc4 = arc(0.58f, 1.4f, 1.08f, 0.03f, 0.9f),
        )
        else -> runtime.copy(
            preset = "default",
            overallSizeDp = 16f,
            arc1 = arc(1f, 1.5f, 1f, 0.08f, -0.6f),
            arc2 = arc(0.84f, 1.45f, 1f, 0.06f, -0.1f),
            arc3 = arc(0.66f, 1.4f, 1f, 0.04f, 0.45f),
            arc4 = arc(0.48f, 1.35f, 1f, 0.02f, 0.9f),
        )
    }
}

private fun round1(value: Float): Float = (value * 10f).roundToInt() / 10f
private fun round2(value: Float): Float = (value * 100f).roundToInt() / 100f
