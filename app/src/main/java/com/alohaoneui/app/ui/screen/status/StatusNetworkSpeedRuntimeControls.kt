package com.alohaoneui.app.ui.screen.status

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.component.AlohaOneUIInfoItem
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.SwitchItem
import kotlin.math.roundToInt

@Composable
internal fun StatusNetworkSpeedRuntimeControls(
    uiState: Preference.SystemUI.StatusBar,
) {
    val toolsViewModel = viewModel<AlohaOneUIToolsViewModel>()
    val localView = LocalView.current
    val icon = ImageVector.vectorResource(id = R.drawable.net_speed)
    val runtime = uiState.networkSpeedRuntime

    var expanded by rememberSaveable { mutableStateOf(runtime.enabled) }
    var fontSize by remember(runtime.fontSizeSp) { mutableFloatStateOf(runtime.fontSizeSp) }
    var fontWeight by remember(runtime.fontWeight) {
        mutableIntStateOf(runtime.fontWeight.coerceIn(100, 900))
    }
    var horizontalOffset by remember(runtime.horizontalOffsetDp) {
        mutableFloatStateOf(runtime.horizontalOffsetDp)
    }
    var verticalOffset by remember(runtime.verticalOffsetDp) {
        mutableFloatStateOf(runtime.verticalOffsetDp)
    }

    fun preview(next: Preference.SystemUI.StatusBarNetworkSpeedRuntime) {
        toolsViewModel.previewData { current ->
            current.copy(
                systemUI = current.systemUI.copy(
                    statusBar = current.systemUI.statusBar.copy(
                        networkSpeedRuntime = next,
                    ),
                ),
            )
        }
    }

    fun persist(next: Preference.SystemUI.StatusBarNetworkSpeedRuntime) {
        toolsViewModel.updateData { current ->
            current.copy(
                systemUI = current.systemUI.copy(
                    statusBar = current.systemUI.statusBar.copy(
                        networkSpeedRuntime = next,
                    ),
                ),
            )
        }
    }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = icon,
                    title = stringResource(id = R.string.status_network_speed_runtime_enable),
                    summary = stringResource(id = R.string.status_network_speed_runtime_enable_summary),
                    clickable = true,
                    showChildIndicator = true,
                    onClick = {
                        if (runtime.enabled) expanded = !expanded
                    },
                    checked = runtime.enabled,
                    onCheckedChange = { enabled ->
                        expanded = enabled
                        persist(runtime.copy(enabled = enabled))
                    },
                )
            }

            if (runtime.enabled && expanded) {
                add {
                    NetworkSpeedSliderSetting(
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        title = stringResource(id = R.string.status_network_speed_font_size),
                        summary = stringResource(
                            id = R.string.status_network_speed_sp_summary,
                            fontSize,
                        ),
                        value = fontSize,
                        range = 5f..18f,
                        onValueChange = {
                            val next = roundHalf(it)
                            tick(localView, next != fontSize)
                            fontSize = next
                            preview(runtime.copy(fontSizeSp = fontSize))
                        },
                        onFinished = { persist(runtime.copy(fontSizeSp = fontSize)) },
                    )
                }
                add {
                    NetworkSpeedSliderSetting(
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        title = stringResource(id = R.string.status_network_speed_font_weight),
                        summary = fontWeight.toString(),
                        value = fontWeight.toFloat(),
                        range = 100f..900f,
                        onValueChange = {
                            val next = ((it / 100f).roundToInt() * 100).coerceIn(100, 900)
                            tick(localView, next != fontWeight)
                            fontWeight = next
                            preview(runtime.copy(fontWeight = fontWeight))
                        },
                        onFinished = { persist(runtime.copy(fontWeight = fontWeight)) },
                    )
                }
                add {
                    NetworkSpeedSliderSetting(
                        icon = icon,
                        title = stringResource(id = R.string.status_network_speed_horizontal_offset),
                        summary = stringResource(
                            id = R.string.status_network_speed_dp_summary,
                            horizontalOffset,
                        ),
                        value = horizontalOffset,
                        range = -40f..40f,
                        onValueChange = {
                            val next = roundHalf(it)
                            tick(localView, next != horizontalOffset)
                            horizontalOffset = next
                            preview(runtime.copy(horizontalOffsetDp = horizontalOffset))
                        },
                        onFinished = {
                            persist(runtime.copy(horizontalOffsetDp = horizontalOffset))
                        },
                    )
                }
                add {
                    NetworkSpeedSliderSetting(
                        icon = icon,
                        title = stringResource(id = R.string.status_network_speed_vertical_offset),
                        summary = stringResource(
                            id = R.string.status_network_speed_dp_summary,
                            verticalOffset,
                        ),
                        value = verticalOffset,
                        range = -20f..20f,
                        onValueChange = {
                            val next = roundHalf(it)
                            tick(localView, next != verticalOffset)
                            verticalOffset = next
                            preview(runtime.copy(verticalOffsetDp = verticalOffset))
                        },
                        onFinished = {
                            persist(runtime.copy(verticalOffsetDp = verticalOffset))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = icon,
                        title = stringResource(id = R.string.status_network_speed_hide_unit),
                        summary = stringResource(id = R.string.status_network_speed_hide_unit_summary),
                        checked = runtime.hideUnit,
                        onCheckedChange = { persist(runtime.copy(hideUnit = it)) },
                    )
                }
                add {
                    SwitchItem(
                        icon = icon,
                        title = stringResource(id = R.string.status_network_speed_hide_bs),
                        summary = stringResource(id = R.string.status_network_speed_hide_bs_summary),
                        checked = runtime.hideBs,
                        onCheckedChange = { persist(runtime.copy(hideBs = it)) },
                    )
                }
                add {
                    SwitchItem(
                        icon = icon,
                        title = stringResource(id = R.string.status_network_speed_align_right),
                        summary = stringResource(id = R.string.status_network_speed_align_right_summary),
                        checked = runtime.alignRight,
                        onCheckedChange = { persist(runtime.copy(alignRight = it)) },
                    )
                }
                add {
                    SwitchItem(
                        icon = icon,
                        title = stringResource(id = R.string.status_network_speed_hide_on_lockscreen),
                        summary = stringResource(id = R.string.status_network_speed_hide_on_lockscreen_summary),
                        checked = runtime.hideOnLockscreen,
                        onCheckedChange = { persist(runtime.copy(hideOnLockscreen = it)) },
                    )
                }
            }
        },
    )
}

@Composable
private fun NetworkSpeedSliderSetting(
    icon: ImageVector,
    title: String,
    summary: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Column {
        AlohaOneUIInfoItem(icon = icon, title = title, summary = summary)
        StatusThemeChildItem {
            AlohaOneUISlider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                valueRange = range,
                onValueChangeFinished = onFinished,
            )
        }
    }
}

private fun roundHalf(value: Float): Float = (value * 2f).roundToInt() / 2f

private fun tick(view: View, changed: Boolean) {
    if (changed) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}
