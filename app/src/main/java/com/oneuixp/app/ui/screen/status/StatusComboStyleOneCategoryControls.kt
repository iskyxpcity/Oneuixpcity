package com.oneuixp.app.ui.screen.status

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import com.oneuixp.app.ui.component.material.ExpressiveConfirmDialog
import io.github.soclear.oneuix.ui.component.SwitchItem
import io.github.soclear.oneuix.ui.component.SelectItem
import kotlin.math.roundToInt

@Suppress("UNUSED_PARAMETER")
@Composable
fun StatusComboStyleOneCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    val icon = ImageVector.vectorResource(R.drawable.status_combo_style_one)
    val viewModel = viewModel<OneuiXToolsViewModel>()
    val context = LocalContext.current
    val runtime = uiState.comboStyleOneRuntime
    var confirmResetColors by remember { mutableStateOf(false) }

    fun preview(next: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        val json = runCatching {
            IgnoreUnknownKeysJson.encodeToString(
                Preference.SystemUI.StatusBar.serializer(),
                uiState.copy(comboStyleOneRuntime = next),
            )
        }.getOrNull() ?: return
        runCatching {
            context.sendBroadcast(
                Intent(StatusBarRuntimeBridge.ACTION)
                    .setPackage(Package.SYSTEMUI)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON, json),
            )
        }
    }
    fun persist(next: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        viewModel.updateData { current ->
            current.copy(
                systemUI = current.systemUI.copy(
                    statusBar = current.systemUI.statusBar.copy(comboStyleOneRuntime = next),
                ),
            )
        }
    }
    fun previewAndPersist(next: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        preview(next)
        persist(next)
    }

    var ringStroke by remember(runtime.ringStroke) { mutableIntStateOf(runtime.ringStroke.coerceIn(4, 16)) }
    var arcStroke by remember(runtime.arcStroke) { mutableIntStateOf(runtime.arcStroke.coerceIn(3, 16)) }
    var valueSize by remember(runtime.valueSize) { mutableIntStateOf(runtime.valueSize.coerceIn(16, 44)) }
    var valueWeight by remember(runtime.valueWeight) { mutableIntStateOf(runtime.valueWeight) }
    var typeSize by remember(runtime.typeSize) { mutableIntStateOf(runtime.typeSize.coerceIn(16, 44)) }
    var typeWeight by remember(runtime.typeWeight) { mutableIntStateOf(runtime.typeWeight) }
    var suffixScale by remember(runtime.typeSuffixScale) { mutableIntStateOf(runtime.typeSuffixScale.coerceIn(50, 100)) }
    var outTypeSize by remember(runtime.outTypeSizeDp) { mutableIntStateOf(runtime.outTypeSizeDp.coerceIn(8, 26)) }
    var outSignalSize by remember(runtime.outSignalSizeDp) { mutableIntStateOf(runtime.outSignalSizeDp.coerceIn(6, 20)) }
    var outTypeLeft by remember(runtime.outTypeMarginLeftDp) { mutableIntStateOf(runtime.outTypeMarginLeftDp.coerceIn(0, 16)) }
    var outTypeRight by remember(runtime.outTypeMarginRightDp) { mutableIntStateOf(runtime.outTypeMarginRightDp.coerceIn(0, 16)) }
    var outSignalMargin by remember(runtime.outSignalMarginDp) { mutableIntStateOf(runtime.outSignalMarginDp.coerceIn(0, 16)) }
    var lowThreshold by remember(runtime.lowThreshold) { mutableIntStateOf(runtime.lowThreshold.coerceIn(5, 50)) }
    var criticalThreshold by remember(runtime.criticalThreshold) { mutableIntStateOf(runtime.criticalThreshold.coerceIn(1, 20)) }
    var glyphScale by remember(runtime.glyphScalePercent) { mutableIntStateOf(runtime.glyphScalePercent.coerceIn(70, 150)) }
    var trackAlpha by remember(runtime.trackAlpha) {
        mutableIntStateOf((runtime.trackAlpha * 100f / 255f).roundToInt())
    }

    StatusThemeGroup(items = listOf({
        Column {
            OneuiXInfoItem(
                icon = icon,
                title = stringResource(R.string.status_combo_style_one_preview),
                summary = stringResource(R.string.status_combo_style_one_preview_summary),
            )
            StatusThemeChildItem {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    factory = { StatusComboStyleOnePreviewView(it) },
                    update = { it.setConfig(runtime) },
                )
            }
        }
    }, {
        SwitchItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_enable),
            summary = stringResource(R.string.status_combo_style_one_enable_summary),
            checked = runtime.enabled,
            onCheckedChange = { previewAndPersist(runtime.copy(enabled = it)) },
        )
    }))

    if (!runtime.enabled) return

    StatusThemeGroup(items = listOf({
        SelectItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_style),
            entries = listOf(
                stringResource(R.string.status_combo_style_one_style_ring),
                stringResource(R.string.status_combo_style_one_style_rect),
            ),
            selectedIndex = runtime.trioStyle.coerceIn(0, 1),
            onSelectedIndexChange = { previewAndPersist(runtime.copy(trioStyle = it.coerceIn(0, 1))) },
        )
    }, {
        SwitchItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_overlay),
            summary = stringResource(R.string.status_combo_style_one_overlay_summary),
            checked = runtime.overlayGlyph,
            onCheckedChange = { previewAndPersist(runtime.copy(overlayGlyph = it)) },
        )
    }, {
        SwitchItem(icon = icon, title = stringResource(R.string.status_combo_style_one_show_wifi),
            checked = runtime.showWifi, onCheckedChange = { previewAndPersist(runtime.copy(showWifi = it)) })
    }, {
        SwitchItem(icon = icon, title = stringResource(R.string.status_combo_style_one_show_mobile),
            checked = runtime.showMobile, onCheckedChange = { previewAndPersist(runtime.copy(showMobile = it)) })
    }, {
        SwitchItem(icon = icon, title = stringResource(R.string.status_combo_style_one_show_value),
            checked = runtime.showValue, onCheckedChange = { previewAndPersist(runtime.copy(showValue = it)) })
    }, {
        SwitchItem(icon = icon, title = stringResource(R.string.status_combo_style_one_show_bolt),
            checked = runtime.showBolt, onCheckedChange = { previewAndPersist(runtime.copy(showBolt = it)) })
    }, {
        SwitchItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_value_centred),
            summary = stringResource(R.string.status_combo_style_one_value_centred_summary),
            checked = runtime.valueCentred,
            onCheckedChange = { previewAndPersist(runtime.copy(valueCentred = it)) },
        )
    }))

    StatusThemeGroup(items = listOf({
        SelectItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_mobile_type_mode),
            entries = listOf(
                stringResource(R.string.status_combo_style_one_mobile_type_off),
                stringResource(R.string.status_combo_style_one_mobile_type_inside),
                stringResource(R.string.status_combo_style_one_mobile_type_outside),
            ),
            selectedIndex = runtime.mobileTypeMode.coerceIn(0, 2),
            onSelectedIndexChange = { previewAndPersist(runtime.copy(mobileTypeMode = it.coerceIn(0, 2))) },
        )
    }, {
        SelectItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_signal_mode),
            entries = listOf(
                stringResource(R.string.status_combo_style_one_signal_inside),
                stringResource(R.string.status_combo_style_one_signal_outside),
            ),
            selectedIndex = runtime.signalMode.coerceIn(0, 1),
            onSelectedIndexChange = { previewAndPersist(runtime.copy(signalMode = it.coerceIn(0, 1))) },
        )
    }, {
        SwitchItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_dual_sim),
            summary = stringResource(R.string.status_combo_style_one_dual_sim_summary),
            checked = runtime.dualSim,
            onCheckedChange = { previewAndPersist(runtime.copy(dualSim = it)) },
        )
    }, {
        SwitchItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_stacked_signal),
            summary = stringResource(R.string.status_combo_style_one_stacked_signal_summary),
            checked = runtime.stackedSignal,
            onCheckedChange = { previewAndPersist(runtime.copy(stackedSignal = it)) },
        )
    }, {
        SwitchItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_data_sim_only),
            summary = stringResource(R.string.status_combo_style_one_data_sim_only_summary),
            checked = runtime.dataSimOnly,
            onCheckedChange = { previewAndPersist(runtime.copy(dataSimOnly = it)) },
        )
    }))

    StatusThemeGroup(items = listOf({
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_glyph_scale),
            stringResource(R.string.status_combo_style_one_percent_summary, glyphScale), glyphScale.toFloat(), 70f..150f,
            { glyphScale = it.roundToInt(); preview(runtime.copy(glyphScalePercent = glyphScale)) },
            { persist(runtime.copy(glyphScalePercent = glyphScale)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_ring_stroke),
            stringResource(R.string.status_combo_style_one_int_summary, ringStroke), ringStroke.toFloat(), 4f..16f,
            { ringStroke = it.roundToInt(); preview(runtime.copy(ringStroke = ringStroke)) },
            { persist(runtime.copy(ringStroke = ringStroke)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_arc_stroke),
            stringResource(R.string.status_combo_style_one_int_summary, arcStroke), arcStroke.toFloat(), 3f..16f,
            { arcStroke = it.roundToInt(); preview(runtime.copy(arcStroke = arcStroke)) },
            { persist(runtime.copy(arcStroke = arcStroke)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_value_size),
            stringResource(R.string.status_combo_style_one_int_summary, valueSize), valueSize.toFloat(), 16f..44f,
            { valueSize = it.roundToInt(); preview(runtime.copy(valueSize = valueSize)) },
            { persist(runtime.copy(valueSize = valueSize)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_value_weight),
            stringResource(R.string.status_combo_style_one_weight_summary, valueWeight), valueWeight.toFloat(), 100f..900f,
            { valueWeight = (it / 100f).roundToInt() * 100; preview(runtime.copy(valueWeight = valueWeight)) },
            { persist(runtime.copy(valueWeight = valueWeight)) }, stepSize = 100f)
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_type_size),
            stringResource(R.string.status_combo_style_one_int_summary, typeSize), typeSize.toFloat(), 16f..44f,
            { typeSize = it.roundToInt(); preview(runtime.copy(typeSize = typeSize)) },
            { persist(runtime.copy(typeSize = typeSize)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_type_weight),
            stringResource(R.string.status_combo_style_one_weight_summary, typeWeight), typeWeight.toFloat(), 100f..900f,
            { typeWeight = (it / 100f).roundToInt() * 100; preview(runtime.copy(typeWeight = typeWeight)) },
            { persist(runtime.copy(typeWeight = typeWeight)) }, stepSize = 100f)
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_type_suffix_scale),
            stringResource(R.string.status_combo_style_one_percent_summary, suffixScale), suffixScale.toFloat(), 50f..100f,
            { suffixScale = it.roundToInt(); preview(runtime.copy(typeSuffixScale = suffixScale)) },
            { persist(runtime.copy(typeSuffixScale = suffixScale)) })
    }))

    StatusThemeGroup(items = listOf({
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_out_type_size),
            stringResource(R.string.status_combo_style_one_dp_summary, outTypeSize), outTypeSize.toFloat(), 8f..26f,
            { outTypeSize = it.roundToInt(); preview(runtime.copy(outTypeSizeDp = outTypeSize)) },
            { persist(runtime.copy(outTypeSizeDp = outTypeSize)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_out_signal_size),
            stringResource(R.string.status_combo_style_one_dp_summary, outSignalSize), outSignalSize.toFloat(), 6f..20f,
            { outSignalSize = it.roundToInt(); preview(runtime.copy(outSignalSizeDp = outSignalSize)) },
            { persist(runtime.copy(outSignalSizeDp = outSignalSize)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_out_type_margin_left),
            stringResource(R.string.status_combo_style_one_dp_summary, outTypeLeft), outTypeLeft.toFloat(), 0f..16f,
            { outTypeLeft = it.roundToInt(); preview(runtime.copy(outTypeMarginLeftDp = outTypeLeft)) },
            { persist(runtime.copy(outTypeMarginLeftDp = outTypeLeft)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_out_type_margin_right),
            stringResource(R.string.status_combo_style_one_dp_summary, outTypeRight), outTypeRight.toFloat(), 0f..16f,
            { outTypeRight = it.roundToInt(); preview(runtime.copy(outTypeMarginRightDp = outTypeRight)) },
            { persist(runtime.copy(outTypeMarginRightDp = outTypeRight)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_out_signal_margin),
            stringResource(R.string.status_combo_style_one_dp_summary, outSignalMargin), outSignalMargin.toFloat(), 0f..16f,
            { outSignalMargin = it.roundToInt(); preview(runtime.copy(outSignalMarginDp = outSignalMargin)) },
            { persist(runtime.copy(outSignalMarginDp = outSignalMargin)) })
    }))

    StatusThemeGroup(items = listOf({
        SwitchItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_role_colors),
            summary = stringResource(R.string.status_combo_style_one_role_colors_summary),
            checked = runtime.roleColors,
            onCheckedChange = { previewAndPersist(runtime.copy(roleColors = it)) },
        )
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_critical_threshold),
            stringResource(R.string.status_combo_style_one_percent_summary, criticalThreshold), criticalThreshold.toFloat(), 1f..20f,
            { criticalThreshold = it.roundToInt(); preview(runtime.copy(criticalThreshold = criticalThreshold)) },
            { persist(runtime.copy(criticalThreshold = criticalThreshold)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_low_threshold),
            stringResource(R.string.status_combo_style_one_percent_summary, lowThreshold), lowThreshold.toFloat(), 5f..50f,
            { lowThreshold = it.roundToInt(); preview(runtime.copy(lowThreshold = lowThreshold)) },
            { persist(runtime.copy(lowThreshold = lowThreshold)) })
    }, {
        styleOneSlider(icon, stringResource(R.string.status_combo_style_one_track_alpha),
            stringResource(R.string.status_combo_style_one_percent_summary, trackAlpha), trackAlpha.toFloat(), 0f..100f,
            { trackAlpha = it.roundToInt(); val a = (trackAlpha * 255f / 100f).roundToInt(); preview(runtime.copy(trackAlpha = a)) },
            { persist(runtime.copy(trackAlpha = (trackAlpha * 255f / 100f).roundToInt())) })
    }))

    if (runtime.roleColors) {
        StatusThemeGroup(items = listOf({
            styleOneColor(icon, R.string.status_combo_style_one_critical_dark, runtime.criticalColorDarkArgb,
                { previewAndPersist(runtime.copy(criticalColorDarkArgb = it)) }, { preview(runtime.copy(criticalColorDarkArgb = it)) })
        }, {
            styleOneColor(icon, R.string.status_combo_style_one_critical_light, runtime.criticalColorLightArgb,
                { previewAndPersist(runtime.copy(criticalColorLightArgb = it)) }, { preview(runtime.copy(criticalColorLightArgb = it)) })
        }, {
            styleOneColor(icon, R.string.status_combo_style_one_charging_dark, runtime.chargingColorDarkArgb,
                { previewAndPersist(runtime.copy(chargingColorDarkArgb = it)) }, { preview(runtime.copy(chargingColorDarkArgb = it)) })
        }, {
            styleOneColor(icon, R.string.status_combo_style_one_charging_light, runtime.chargingColorLightArgb,
                { previewAndPersist(runtime.copy(chargingColorLightArgb = it)) }, { preview(runtime.copy(chargingColorLightArgb = it)) })
        }, {
            styleOneColor(icon, R.string.status_combo_style_one_low_dark, runtime.lowColorDarkArgb,
                { previewAndPersist(runtime.copy(lowColorDarkArgb = it)) }, { preview(runtime.copy(lowColorDarkArgb = it)) })
        }, {
            styleOneColor(icon, R.string.status_combo_style_one_low_light, runtime.lowColorLightArgb,
                { previewAndPersist(runtime.copy(lowColorLightArgb = it)) }, { preview(runtime.copy(lowColorLightArgb = it)) })
        }))

        StatusThemeGroup(items = listOf({
            OneuiXInfoItem(
                icon = icon,
                title = stringResource(R.string.status_combo_style_one_reset_colors),
                summary = stringResource(R.string.status_combo_style_one_reset_colors_summary),
            )
        }, {
            StatusThemeChildItem {
                OneuiXActionButton(
                    text = stringResource(R.string.status_combo_style_one_reset_colors),
                    onClick = { confirmResetColors = true },
                )
            }
        }))
    }

    if (confirmResetColors) {
        ExpressiveConfirmDialog(
            title = stringResource(R.string.status_combo_style_one_reset_colors),
            message = stringResource(R.string.status_combo_style_one_reset_colors_confirm),
            confirmText = stringResource(R.string.status_combo_style_one_reset),
            dismissText = stringResource(R.string.status_combo_style_one_cancel),
            onConfirm = {
                previewAndPersist(
                    runtime.copy(
                        criticalColorDarkArgb = 0xFFFF3B30.toInt(),
                        criticalColorLightArgb = 0xFFFF3B30.toInt(),
                        chargingColorDarkArgb = 0xFF34C759.toInt(),
                        chargingColorLightArgb = 0xFF1F8F3D.toInt(),
                        lowColorDarkArgb = 0xFFF2B900.toInt(),
                        lowColorLightArgb = 0xFFC99700.toInt(),
                    )
                )
                confirmResetColors = false
            },
            onDismiss = { confirmResetColors = false },
        )
    }

    StatusThemeGroup(items = listOf({
        SwitchItem(
            icon = icon,
            title = stringResource(R.string.status_combo_style_one_debug_log),
            summary = stringResource(R.string.status_combo_style_one_debug_log_summary),
            checked = runtime.debugLog,
            onCheckedChange = { previewAndPersist(runtime.copy(debugLog = it)) },
        )
    }))
}

@Composable
private fun styleOneColor(icon: ImageVector, titleRes: Int, value: Int, onChange: (Int) -> Unit, onPreview: (Int) -> Unit) {
    OneuiXInfoItem(icon = icon, title = stringResource(titleRes), summary = stringResource(R.string.status_combo_style_one_color_summary))
    StatusThemeChildItem {
        StatusClockColorPicker(colorArgb = value, onColorChange = onChange, onColorPreview = onPreview)
    }
}

@Composable
private fun styleOneSlider(
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
    var lastStep by remember(title, range.start, range.endInclusive) { mutableFloatStateOf(value) }
    Column {
        OneuiXInfoItem(icon = icon, title = title, summary = summary)
        StatusThemeChildItem {
            OneuiXSlider(
                value = value,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                valueRange = range,
                steps = 0,
                onValueChange = { raw ->
                    val bounded = raw.coerceIn(range.start, range.endInclusive)
                    val next = (((bounded - range.start) / stepSize).roundToInt() * stepSize + range.start)
                        .coerceIn(range.start, range.endInclusive)
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
