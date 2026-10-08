package com.oneuixp.app.ui.screen.status

import kotlin.math.roundToInt

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneuixp.app.R
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXInfoItem
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SwitchItem
import io.github.soclear.oneuix.ui.component.SelectItem

@Composable
fun StatusComboCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    val comboIcon = ImageVector.vectorResource(id = R.drawable.status_combo)
    val toolsViewModel = viewModel<OneuiXToolsViewModel>()
    val context = LocalContext.current
    val runtime = uiState.comboRuntime

    var expanded by rememberSaveable { mutableStateOf(runtime.enabled) }
    var size by remember(runtime.sizePercent) { mutableFloatStateOf(runtime.sizePercent) }
    var xOffset by remember(runtime.offsetXDp) { mutableFloatStateOf(runtime.offsetXDp) }
    var yOffset by remember(runtime.offsetYDp) { mutableFloatStateOf(runtime.offsetYDp) }
    var arcStroke by remember(runtime.arcStrokePercent) { mutableFloatStateOf(runtime.arcStrokePercent) }
    var blackDepthPercent by remember(runtime.batteryBlackDepth) {
        mutableFloatStateOf(runtime.batteryBlackDepth * 100f)
    }
    var dotsGap by remember(runtime.dotsGapDp) { mutableFloatStateOf(runtime.dotsGapDp) }
    var dotsSize by remember(runtime.dotsSizePercent) { mutableFloatStateOf(runtime.dotsSizePercent) }
    var innerSize by remember(runtime.innerSizePercent) { mutableFloatStateOf(runtime.innerSizePercent) }
    var innerY by remember(runtime.innerYDp) { mutableFloatStateOf(runtime.innerYDp) }
    var levelTextSize by remember(runtime.batteryTextSizeSp) {
        mutableFloatStateOf(runtime.batteryTextSizeSp)
    }
    var levelTextWeight by remember(runtime.batteryTextFontWeight) {
        mutableFloatStateOf(runtime.batteryTextFontWeight.toFloat())
    }
    var boltSize by remember(runtime.chargingBoltSizePercent) {
        mutableFloatStateOf(runtime.chargingBoltSizePercent)
    }
    var boltX by remember(runtime.chargingBoltXDp) { mutableFloatStateOf(runtime.chargingBoltXDp) }
    var boltY by remember(runtime.chargingBoltYDp) { mutableFloatStateOf(runtime.chargingBoltYDp) }

    fun preview(next: Preference.SystemUI.StatusBarComboRuntime) {
        val payload = runCatching {
            IgnoreUnknownKeysJson.encodeToString(
                Preference.SystemUI.StatusBar.serializer(),
                uiState.copy(comboRuntime = next),
            )
        }.getOrNull() ?: return
        runCatching {
            context.sendBroadcast(
                Intent(StatusBarRuntimeBridge.ACTION)
                    .setPackage(Package.SYSTEMUI)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON, payload),
            )
        }
    }

    fun persist(next: Preference.SystemUI.StatusBarComboRuntime) {
        toolsViewModel.updateData { current ->
            current.copy(
                systemUI = current.systemUI.copy(
                    statusBar = current.systemUI.statusBar.copy(comboRuntime = next),
                ),
            )
        }
    }

    fun previewAndPersist(next: Preference.SystemUI.StatusBarComboRuntime) {
        preview(next)
        persist(next)
    }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_enable),
                    summary = stringResource(R.string.status_combo_enable_summary),
                    clickable = true,
                    showChildIndicator = true,
                    onClick = { if (runtime.enabled) expanded = !expanded },
                    checked = runtime.enabled,
                    onCheckedChange = {
                        expanded = it
                        previewAndPersist(runtime.copy(enabled = it))
                    },
                )
            }
        },
    )

    if (!runtime.enabled || !expanded) return

    StatusThemeGroup(
        items = buildList {
            add {
                SelectItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_style),
                    entries = listOf(
                        stringResource(R.string.status_combo_style_ring),
                        stringResource(R.string.status_combo_style_rect),
                    ),
                    selectedIndex = runtime.trioStyle.coerceIn(0, 1),
                    onSelectedIndexChange = {
                        previewAndPersist(runtime.copy(trioStyle = it.coerceIn(0, 1)))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_overlay),
                    summary = stringResource(R.string.status_combo_overlay_summary),
                    checked = runtime.overlayGlyph,
                    onCheckedChange = { previewAndPersist(runtime.copy(overlayGlyph = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_show_wifi),
                    checked = runtime.showWifi,
                    onCheckedChange = { previewAndPersist(runtime.copy(showWifi = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_show_mobile),
                    checked = runtime.showMobile,
                    onCheckedChange = { previewAndPersist(runtime.copy(showMobile = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_show_battery),
                    checked = runtime.showBattery,
                    onCheckedChange = { previewAndPersist(runtime.copy(showBattery = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_dual_sim),
                    summary = stringResource(R.string.status_combo_dual_sim_summary),
                    checked = runtime.dualSim,
                    onCheckedChange = { previewAndPersist(runtime.copy(dualSim = it)) },
                )
            }
            add {
                SelectItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_signal_mode),
                    entries = listOf(
                        stringResource(R.string.status_combo_signal_in_ring),
                        stringResource(R.string.status_combo_signal_out_ring),
                    ),
                    selectedIndex = runtime.signalMode.coerceIn(0, 1),
                    onSelectedIndexChange = {
                        previewAndPersist(runtime.copy(signalMode = it.coerceIn(0, 1)))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_stacked_signal),
                    checked = runtime.stackedSignal,
                    onCheckedChange = { previewAndPersist(runtime.copy(stackedSignal = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_data_sim_only),
                    summary = stringResource(R.string.status_combo_data_sim_only_summary),
                    checked = runtime.dataSimOnly,
                    onCheckedChange = { previewAndPersist(runtime.copy(dataSimOnly = it)) },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_battery_text),
                    checked = runtime.showValue || runtime.showBatteryLevelText,
                    onCheckedChange = {
                        previewAndPersist(
                            runtime.copy(
                                showValue = it,
                                showBatteryLevelText = it,
                            ),
                        )
                    },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_5g_type),
                    checked = runtime.show5gNetworkType,
                    onCheckedChange = { previewAndPersist(runtime.copy(show5gNetworkType = it)) },
                )
            }
            add {
                SelectItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_mobile_type),
                    entries = listOf(
                        stringResource(R.string.status_combo_mobile_type_off),
                        stringResource(R.string.status_combo_mobile_type_in),
                        stringResource(R.string.status_combo_mobile_type_out),
                    ),
                    selectedIndex = runtime.mobileTypeMode.coerceIn(0, 2),
                    onSelectedIndexChange = {
                        previewAndPersist(runtime.copy(mobileTypeMode = it.coerceIn(0, 2)))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_charging_bolt),
                    checked = runtime.showChargingBolt,
                    onCheckedChange = { previewAndPersist(runtime.copy(showChargingBolt = it)) },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_size),
                    summary = stringResource(R.string.status_combo_percent_summary, size.roundToInt()),
                    value = size,
                    range = 50f..250f,
                    onValueChange = {
                        size = it
                        preview(runtime.copy(sizePercent = it))
                    },
                    onFinished = { persist(runtime.copy(sizePercent = size)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_offset_x),
                    summary = stringResource(R.string.status_combo_dp_summary, xOffset.roundToInt()),
                    value = xOffset,
                    range = -24f..24f,
                    onValueChange = {
                        xOffset = it
                        preview(runtime.copy(offsetXDp = it))
                    },
                    onFinished = { persist(runtime.copy(offsetXDp = xOffset)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_offset_y),
                    summary = stringResource(R.string.status_combo_dp_summary, yOffset.roundToInt()),
                    value = yOffset,
                    range = -15f..15f,
                    onValueChange = {
                        yOffset = it
                        preview(runtime.copy(offsetYDp = it))
                    },
                    onFinished = { persist(runtime.copy(offsetYDp = yOffset)) },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_arc_stroke),
                    summary = stringResource(R.string.status_combo_percent_summary, arcStroke.roundToInt()),
                    value = arcStroke,
                    range = 50f..200f,
                    onValueChange = {
                        arcStroke = it
                        preview(runtime.copy(arcStrokePercent = it))
                    },
                    onFinished = { persist(runtime.copy(arcStrokePercent = arcStroke)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_battery_depth),
                    summary = stringResource(
                        R.string.status_combo_percent_summary,
                        blackDepthPercent.roundToInt(),
                    ),
                    value = blackDepthPercent,
                    range = 15f..100f,
                    onValueChange = {
                        blackDepthPercent = it
                        preview(runtime.copy(batteryBlackDepth = it / 100f))
                    },
                    onFinished = {
                        persist(
                            runtime.copy(
                                batteryBlackDepth = blackDepthPercent / 100f,
                            ),
                        )
                    },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_level_text_size),
                    summary = stringResource(R.string.status_combo_sp_summary, levelTextSize.roundToInt()),
                    value = levelTextSize,
                    range = 5f..18f,
                    onValueChange = {
                        levelTextSize = it
                        preview(runtime.copy(batteryTextSizeSp = it))
                    },
                    onFinished = { persist(runtime.copy(batteryTextSizeSp = levelTextSize)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_level_text_weight),
                    summary = stringResource(R.string.status_combo_weight_summary, runtime.batteryTextFontWeight),
                    value = levelTextWeight,
                    range = 100f..900f,
                    onValueChange = {
                        val weight = it.roundToNearestHundred().coerceIn(100, 900)
                        levelTextWeight = weight.toFloat()
                        preview(runtime.copy(batteryTextFontWeight = weight))
                    },
                    stepSize = 100f,
                    onFinished = {
                        persist(
                            runtime.copy(
                                batteryTextFontWeight = levelTextWeight.roundToInt().coerceIn(100, 900),
                            ),
                        )
                    },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_dots_gap),
                    summary = stringResource(R.string.status_combo_dp_summary, dotsGap.roundToInt()),
                    value = dotsGap,
                    range = 0f..8f,
                    onValueChange = {
                        dotsGap = it
                        preview(runtime.copy(dotsGapDp = it))
                    },
                    onFinished = { persist(runtime.copy(dotsGapDp = dotsGap)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_dots_size),
                    summary = stringResource(R.string.status_combo_percent_summary, dotsSize.roundToInt()),
                    value = dotsSize,
                    range = 50f..180f,
                    onValueChange = {
                        dotsSize = it
                        preview(runtime.copy(dotsSizePercent = it))
                    },
                    onFinished = { persist(runtime.copy(dotsSizePercent = dotsSize)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_inner_size),
                    summary = stringResource(R.string.status_combo_percent_summary, innerSize.roundToInt()),
                    value = innerSize,
                    range = 50f..180f,
                    onValueChange = {
                        innerSize = it
                        preview(runtime.copy(innerSizePercent = it))
                    },
                    onFinished = { persist(runtime.copy(innerSizePercent = innerSize)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_inner_y),
                    summary = stringResource(R.string.status_combo_dp_summary, innerY.roundToInt()),
                    value = innerY,
                    range = -8f..8f,
                    onValueChange = {
                        innerY = it
                        preview(runtime.copy(innerYDp = it))
                    },
                    onFinished = { persist(runtime.copy(innerYDp = innerY)) },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_wifi_two_arcs),
                    checked = runtime.wifiTwoArcs,
                    onCheckedChange = { previewAndPersist(runtime.copy(wifiTwoArcs = it)) },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_bolt_size),
                    summary = stringResource(R.string.status_combo_percent_summary, boltSize.roundToInt()),
                    value = boltSize,
                    range = 50f..180f,
                    onValueChange = {
                        boltSize = it
                        preview(runtime.copy(chargingBoltSizePercent = it))
                    },
                    onFinished = { persist(runtime.copy(chargingBoltSizePercent = boltSize)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_bolt_x),
                    summary = stringResource(R.string.status_combo_dp_summary, boltX.roundToInt()),
                    value = boltX,
                    range = -8f..8f,
                    onValueChange = {
                        boltX = it
                        preview(runtime.copy(chargingBoltXDp = it))
                    },
                    onFinished = { persist(runtime.copy(chargingBoltXDp = boltX)) },
                )
            }
            add {
                comboSlider(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_bolt_y),
                    summary = stringResource(R.string.status_combo_dp_summary, boltY.roundToInt()),
                    value = boltY,
                    range = -8f..8f,
                    onValueChange = {
                        boltY = it
                        preview(runtime.copy(chargingBoltYDp = it))
                    },
                    onFinished = { persist(runtime.copy(chargingBoltYDp = boltY)) },
                )
            }
        },
    )
    
    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_value_centered),
                    checked = runtime.valueCentred,
                    onCheckedChange = { previewAndPersist(runtime.copy(valueCentred = it)) },
                )
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_ring_stroke,
                    runtime.ringStroke,
                    4,
                    16,
                ) { previewAndPersist(runtime.copy(ringStroke = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_arc_stroke_exact,
                    runtime.arcStroke,
                    3,
                    16,
                ) { previewAndPersist(runtime.copy(arcStroke = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_value_size_exact,
                    runtime.valueSize,
                    16,
                    44,
                ) { previewAndPersist(runtime.copy(valueSize = it)) }
            }
            add {
                comboWeightSlider(
                    comboIcon,
                    R.string.status_combo_value_weight_exact,
                    runtime.valueWeight,
                ) { previewAndPersist(runtime.copy(valueWeight = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_type_size,
                    runtime.typeSize,
                    16,
                    44,
                ) { previewAndPersist(runtime.copy(typeSize = it)) }
            }
            add {
                comboWeightSlider(
                    comboIcon,
                    R.string.status_combo_type_weight,
                    runtime.typeWeight,
                ) { previewAndPersist(runtime.copy(typeWeight = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_type_suffix,
                    runtime.typeSuffixScale,
                    50,
                    100,
                ) { previewAndPersist(runtime.copy(typeSuffixScale = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_out_type_size,
                    runtime.outTypeSizeDp,
                    8,
                    26,
                ) { previewAndPersist(runtime.copy(outTypeSizeDp = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_out_signal_size,
                    runtime.outSignalSizeDp,
                    6,
                    20,
                ) { previewAndPersist(runtime.copy(outSignalSizeDp = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_out_signal_margin,
                    runtime.outSignalMarginDp,
                    0,
                    16,
                ) { previewAndPersist(runtime.copy(outSignalMarginDp = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_out_type_margin_left,
                    runtime.outTypeMarginLeftDp,
                    0,
                    16,
                ) { previewAndPersist(runtime.copy(outTypeMarginLeftDp = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_out_type_margin_right,
                    runtime.outTypeMarginRightDp,
                    0,
                    16,
                ) { previewAndPersist(runtime.copy(outTypeMarginRightDp = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_low_threshold,
                    runtime.lowBatteryThreshold,
                    5,
                    50,
                ) { previewAndPersist(runtime.copy(lowBatteryThreshold = it)) }
            }
            add {
                comboIntSlider(
                    comboIcon,
                    R.string.status_combo_track_alpha,
                    runtime.trackAlpha,
                    0,
                    255,
                ) { previewAndPersist(runtime.copy(trackAlpha = it)) }
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_role_colors),
                    summary = stringResource(R.string.status_combo_role_colors_summary),
                    checked = runtime.roleColors,
                    onCheckedChange = { previewAndPersist(runtime.copy(roleColors = it)) },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                OneuiXInfoItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_critical_dark),
                    summary = stringResource(R.string.status_combo_color_summary),
                )
                StatusThemeChildItem {
                    StatusClockColorPicker(
                        colorArgb = runtime.criticalColorDarkArgb,
                        onColorChange = { persist(runtime.copy(criticalColorDarkArgb = it)) },
                        onColorPreview = { preview(runtime.copy(criticalColorDarkArgb = it)) },
                    )
                }
            }
            add {
                OneuiXInfoItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_critical_light),
                    summary = stringResource(R.string.status_combo_color_summary),
                )
                StatusThemeChildItem {
                    StatusClockColorPicker(
                        colorArgb = runtime.criticalColorLightArgb,
                        onColorChange = { persist(runtime.copy(criticalColorLightArgb = it)) },
                        onColorPreview = { preview(runtime.copy(criticalColorLightArgb = it)) },
                    )
                }
            }
            add {
                OneuiXInfoItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_charging_dark),
                    summary = stringResource(R.string.status_combo_color_summary),
                )
                StatusThemeChildItem {
                    StatusClockColorPicker(
                        colorArgb = runtime.chargingColorDarkArgb,
                        onColorChange = { persist(runtime.copy(chargingColorDarkArgb = it)) },
                        onColorPreview = { preview(runtime.copy(chargingColorDarkArgb = it)) },
                    )
                }
            }
            add {
                OneuiXInfoItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_charging_light),
                    summary = stringResource(R.string.status_combo_color_summary),
                )
                StatusThemeChildItem {
                    StatusClockColorPicker(
                        colorArgb = runtime.chargingColorLightArgb,
                        onColorChange = { persist(runtime.copy(chargingColorLightArgb = it)) },
                        onColorPreview = { preview(runtime.copy(chargingColorLightArgb = it)) },
                    )
                }
            }
            add {
                OneuiXInfoItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_quick_charge),
                    summary = stringResource(R.string.status_combo_color_summary),
                )
                StatusThemeChildItem {
                    StatusClockColorPicker(
                        colorArgb = runtime.quickChargeColorArgb,
                        onColorChange = { persist(runtime.copy(quickChargeColorArgb = it)) },
                        onColorPreview = { preview(runtime.copy(quickChargeColorArgb = it)) },
                    )
                }
            }
            add {
                OneuiXInfoItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_low_dark),
                    summary = stringResource(R.string.status_combo_color_summary),
                )
                StatusThemeChildItem {
                    StatusClockColorPicker(
                        colorArgb = runtime.lowColorDarkArgb,
                        onColorChange = { persist(runtime.copy(lowColorDarkArgb = it)) },
                        onColorPreview = { preview(runtime.copy(lowColorDarkArgb = it)) },
                    )
                }
            }
            add {
                OneuiXInfoItem(
                    icon = comboIcon,
                    title = stringResource(R.string.status_combo_low_light),
                    summary = stringResource(R.string.status_combo_color_summary),
                )
                StatusThemeChildItem {
                    StatusClockColorPicker(
                        colorArgb = runtime.lowColorLightArgb,
                        onColorChange = { persist(runtime.copy(lowColorLightArgb = it)) },
                        onColorPreview = { preview(runtime.copy(lowColorLightArgb = it)) },
                    )
                }
            }
        },
    )


}

@Composable
private fun comboSlider(
    icon: ImageVector,
    title: String,
    summary: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
    stepSize: Float = 1f,
) {
    val view = LocalView.current
    var lastStep by remember(title, range.start, range.endInclusive) {
        mutableFloatStateOf(value)
    }

    fun stepped(raw: Float): Float {
        val clamped = raw.coerceIn(range.start, range.endInclusive)
        val normalized = ((clamped - range.start) / stepSize).roundToInt() * stepSize + range.start
        return normalized.coerceIn(range.start, range.endInclusive)
    }

    Column {
        OneuiXInfoItem(
            icon = icon,
            title = title,
            summary = summary,
        )
        StatusThemeChildItem {
            OneuiXSlider(
                value = value,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                valueRange = range,
                steps = 0,
                onValueChange = { raw ->
                    val next = stepped(raw)
                    if (next != lastStep) {
                        lastStep = next
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    onValueChange(next)
                },
                onValueChangeFinished = onFinished,
            )
        }
    }
}

@Composable
private fun comboIntSlider(
    icon: ImageVector,
    titleRes: Int,
    value: Int,
    minValue: Int,
    maxValue: Int,
    onChange: (Int) -> Unit,
) {
    comboSlider(
        icon = icon,
        title = stringResource(titleRes),
        summary = stringResource(R.string.status_combo_int_summary, value),
        value = value.toFloat(),
        range = minValue.toFloat()..maxValue.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        onFinished = {},
    )
}

@Composable
private fun comboWeightSlider(
    icon: ImageVector,
    titleRes: Int,
    value: Int,
    onChange: (Int) -> Unit,
) {
    comboSlider(
        icon = icon,
        title = stringResource(titleRes),
        summary = stringResource(R.string.status_combo_weight_summary, value),
        value = value.toFloat(),
        range = 100f..900f,
        stepSize = 100f,
        onValueChange = {
            onChange((it / 100f).roundToInt() * 100)
        },
        onFinished = {},
    )
}

private fun Float.roundToNearestHundred(): Int =
    (this / 100f).roundToInt() * 100
