package com.oneuixp.app.ui.screen.status

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneuixp.app.R
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import io.github.oneuixp.common.Preference
import io.github.oneuixp.ui.category.SystemUIEvent
import io.github.oneuixp.ui.component.OneuiXPInfoItem
import io.github.oneuixp.ui.component.OneuiXPSlider
import io.github.oneuixp.ui.component.SwitchItem
import kotlin.math.roundToInt

@Composable
fun StatusBluetoothCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    val bluetoothIcon = ImageVector.vectorResource(id = R.drawable.bluetooth)
    val toolsViewModel = viewModel<OneuiXPToolsViewModel>()
    val runtime = uiState.bluetoothRuntime

    var expanded by rememberSaveable { mutableStateOf(runtime.enabled) }
    var portraitSize by remember(runtime.portraitSizeDp) {
        mutableFloatStateOf(runtime.portraitSizeDp)
    }
    var landscapeSize by remember(runtime.landscapeSizeDp) {
        mutableFloatStateOf(runtime.landscapeSizeDp)
    }
    var horizontalOffset by remember(runtime.horizontalOffsetDp) {
        mutableFloatStateOf(runtime.horizontalOffsetDp)
    }
    var verticalOffset by remember(runtime.verticalOffsetDp) {
        mutableFloatStateOf(runtime.verticalOffsetDp)
    }
    var startMargin by remember(runtime.startMarginDp) {
        mutableFloatStateOf(runtime.startMarginDp)
    }
    var endMargin by remember(runtime.endMarginDp) {
        mutableFloatStateOf(runtime.endMarginDp)
    }

    fun preview(next: Preference.SystemUI.StatusBarBluetoothRuntime) {
        toolsViewModel.previewData { current ->
            current.copy(
                systemUI = current.systemUI.copy(
                    statusBar = current.systemUI.statusBar.copy(
                        bluetoothRuntime = next,
                    ),
                ),
            )
        }
    }

    fun persist(next: Preference.SystemUI.StatusBarBluetoothRuntime) {
        toolsViewModel.updateData { current ->
            current.copy(
                systemUI = current.systemUI.copy(
                    statusBar = current.systemUI.statusBar.copy(
                        bluetoothRuntime = next,
                    ),
                ),
            )
        }
    }

    // Keep the original OneuiXP restore-icon function as its own card.
    StatusThemeGroup(
        items = listOf(
            {
                SwitchItem(
                    icon = bluetoothIcon,
                    title = stringResource(id = R.string.oneuixp_restoreBluetoothStatusBarIcon_title),
                    checked = uiState.restoreBluetoothStatusBarIcon,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.StatusBar.RestoreBluetoothStatusBarIcon(it))
                    },
                )
            },
        ),
    )

    // Flux-style positioning / presentation controls are independent from restore-icon.
    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = bluetoothIcon,
                    title = stringResource(id = R.string.status_bluetooth_runtime_enable),
                    summary = stringResource(id = R.string.status_bluetooth_runtime_enable_summary),
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
                    SwitchItem(
                        icon = bluetoothIcon,
                        title = stringResource(id = R.string.status_bluetooth_show_left),
                        summary = stringResource(id = R.string.status_bluetooth_show_left_summary),
                        checked = runtime.showLeftSide,
                        onCheckedChange = {
                            persist(runtime.copy(showLeftSide = it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = bluetoothIcon,
                        title = stringResource(id = R.string.status_bluetooth_custom_icon),
                        summary = stringResource(id = R.string.status_bluetooth_custom_icon_summary),
                        checked = runtime.customIconEnabled,
                        onCheckedChange = {
                            persist(runtime.copy(customIconEnabled = it))
                        },
                    )
                }

                add {
                    BluetoothSliderSetting(
                        icon = bluetoothIcon,
                        title = stringResource(id = R.string.status_bluetooth_portrait_size),
                        summary = stringResource(
                            id = R.string.status_bluetooth_dp_summary,
                            portraitSize,
                        ),
                        value = portraitSize,
                        range = 6f..32f,
                        onValueChange = {
                            portraitSize = roundBluetoothValue(it)
                            preview(runtime.copy(portraitSizeDp = portraitSize))
                        },
                        onFinished = {
                            persist(runtime.copy(portraitSizeDp = portraitSize))
                        },
                    )
                }
                add {
                    BluetoothSliderSetting(
                        icon = bluetoothIcon,
                        title = stringResource(id = R.string.status_bluetooth_landscape_size),
                        summary = stringResource(
                            id = R.string.status_bluetooth_dp_summary,
                            landscapeSize,
                        ),
                        value = landscapeSize,
                        range = 6f..32f,
                        onValueChange = {
                            landscapeSize = roundBluetoothValue(it)
                            preview(runtime.copy(landscapeSizeDp = landscapeSize))
                        },
                        onFinished = {
                            persist(runtime.copy(landscapeSizeDp = landscapeSize))
                        },
                    )
                }
                add {
                    BluetoothSliderSetting(
                        icon = bluetoothIcon,
                        title = stringResource(id = R.string.status_bluetooth_horizontal_offset),
                        summary = stringResource(
                            id = R.string.status_bluetooth_dp_summary,
                            horizontalOffset,
                        ),
                        value = horizontalOffset,
                        range = -300f..300f,
                        onValueChange = {
                            horizontalOffset = roundBluetoothValue(it)
                            preview(runtime.copy(horizontalOffsetDp = horizontalOffset))
                        },
                        onFinished = {
                            persist(runtime.copy(horizontalOffsetDp = horizontalOffset))
                        },
                    )
                }
                add {
                    BluetoothSliderSetting(
                        icon = bluetoothIcon,
                        title = stringResource(id = R.string.status_bluetooth_vertical_offset),
                        summary = stringResource(
                            id = R.string.status_bluetooth_dp_summary,
                            verticalOffset,
                        ),
                        value = verticalOffset,
                        range = -60f..60f,
                        onValueChange = {
                            verticalOffset = roundBluetoothValue(it)
                            preview(runtime.copy(verticalOffsetDp = verticalOffset))
                        },
                        onFinished = {
                            persist(runtime.copy(verticalOffsetDp = verticalOffset))
                        },
                    )
                }
                add {
                    BluetoothSliderSetting(
                        icon = bluetoothIcon,
                        title = stringResource(id = R.string.status_bluetooth_start_margin),
                        summary = stringResource(
                            id = R.string.status_bluetooth_dp_summary,
                            startMargin,
                        ),
                        value = startMargin,
                        range = -24f..48f,
                        onValueChange = {
                            startMargin = roundBluetoothValue(it)
                            preview(runtime.copy(startMarginDp = startMargin))
                        },
                        onFinished = {
                            persist(runtime.copy(startMarginDp = startMargin))
                        },
                    )
                }
                add {
                    BluetoothSliderSetting(
                        icon = bluetoothIcon,
                        title = stringResource(id = R.string.status_bluetooth_end_margin),
                        summary = stringResource(
                            id = R.string.status_bluetooth_dp_summary,
                            endMargin,
                        ),
                        value = endMargin,
                        range = -24f..48f,
                        onValueChange = {
                            endMargin = roundBluetoothValue(it)
                            preview(runtime.copy(endMarginDp = endMargin))
                        },
                        onFinished = {
                            persist(runtime.copy(endMarginDp = endMargin))
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun BluetoothSliderSetting(
    icon: ImageVector,
    title: String,
    summary: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Column {
        OneuiXPInfoItem(
            icon = icon,
            title = title,
            summary = summary,
        )
        StatusThemeChildItem {
            OneuiXPSlider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                valueRange = range,
                onValueChangeFinished = onFinished,
            )
        }
    }
}

private fun roundBluetoothValue(value: Float): Float =
    (value * 10f).roundToInt() / 10f
