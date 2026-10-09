package com.oneuixp.app.ui.screen.status

import android.content.Intent
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
import io.github.soclear.oneuix.ui.component.SelectItem
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

@Composable
fun StatusComboStyleOneCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    val icon = ImageVector.vectorResource(id = R.drawable.status_combo)
    val toolsViewModel = viewModel<OneuiXToolsViewModel>()
    val context = LocalContext.current
    val runtime = uiState.comboStyleOneRuntime

    var expanded by rememberSaveable { mutableStateOf(runtime.enabled) }
    var size by remember(runtime.sizePercent) { mutableFloatStateOf(runtime.sizePercent) }
    var xOffset by remember(runtime.offsetXDp) { mutableFloatStateOf(runtime.offsetXDp) }
    var yOffset by remember(runtime.offsetYDp) { mutableFloatStateOf(runtime.offsetYDp) }
    var ringStroke by remember(runtime.ringStrokeDp) { mutableFloatStateOf(runtime.ringStrokeDp) }
    var trackAlpha by remember(runtime.trackAlpha) { mutableFloatStateOf(runtime.trackAlpha.toFloat()) }
    var dotSize by remember(runtime.dotsSizePercent) { mutableFloatStateOf(runtime.dotsSizePercent) }
    var dotGap by remember(runtime.dotsGapDp) { mutableFloatStateOf(runtime.dotsGapDp) }
    var numberSize by remember(runtime.batteryTextSizeSp) { mutableFloatStateOf(runtime.batteryTextSizeSp) }
    var numberWeight by remember(runtime.batteryTextFontWeight) { mutableFloatStateOf(runtime.batteryTextFontWeight.toFloat()) }
    var boltSize by remember(runtime.chargingBoltSizePercent) { mutableFloatStateOf(runtime.chargingBoltSizePercent) }
    var boltX by remember(runtime.chargingBoltXDp) { mutableFloatStateOf(runtime.chargingBoltXDp) }
    var boltY by remember(runtime.chargingBoltYDp) { mutableFloatStateOf(runtime.chargingBoltYDp) }

    fun preview(next: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        val payload = runCatching {
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
                    .putExtra(StatusBarRuntimeBridge.EXTRA_STATUS_BAR_JSON, payload),
            )
        }
    }

    fun persist(next: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        toolsViewModel.updateData { current ->
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

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_enable),
                    summary = stringResource(R.string.status_combo_style_one_summary),
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
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_placement),
                    entries = listOf(
                        stringResource(R.string.status_combo_style_one_placement_battery),
                        stringResource(R.string.status_combo_style_one_placement_camera),
                    ),
                    selectedIndex = runtime.placementMode.coerceIn(0, 1),
                    onSelectedIndexChange = {
                        previewAndPersist(runtime.copy(placementMode = it.coerceIn(0, 1)))
                    },
                )
            }
            add {
                SwitchItem(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_wifi),
                    checked = runtime.showWifi,
                    onCheckedChange = { previewAndPersist(runtime.copy(showWifi = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_mobile),
                    checked = runtime.showMobile,
                    onCheckedChange = { previewAndPersist(runtime.copy(showMobile = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_5g),
                    checked = runtime.show5gNetworkType,
                    onCheckedChange = { previewAndPersist(runtime.copy(show5gNetworkType = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_percent),
                    checked = runtime.showBatteryPercent,
                    onCheckedChange = { previewAndPersist(runtime.copy(showBatteryPercent = it)) },
                )
            }
            add {
                SwitchItem(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_bolt),
                    checked = runtime.showChargingBolt,
                    onCheckedChange = { previewAndPersist(runtime.copy(showChargingBolt = it)) },
                )
            }
        },
    )

    StatusThemeGroup(
        items = buildList {
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_size),
                    summary = stringResource(R.string.status_combo_style_one_percent_summary, size.roundToInt()),
                    value = size,
                    range = 60f..180f,
                    onValueChange = { size = it; preview(runtime.copy(sizePercent = it)) },
                    onFinished = { persist(runtime.copy(sizePercent = size)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_offset_x),
                    summary = stringResource(R.string.status_combo_style_one_dp_summary, xOffset.roundToInt()),
                    value = xOffset,
                    range = -24f..24f,
                    onValueChange = { xOffset = it; preview(runtime.copy(offsetXDp = it)) },
                    onFinished = { persist(runtime.copy(offsetXDp = xOffset)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_offset_y),
                    summary = stringResource(R.string.status_combo_style_one_dp_summary, yOffset.roundToInt()),
                    value = yOffset,
                    range = -15f..15f,
                    onValueChange = { yOffset = it; preview(runtime.copy(offsetYDp = it)) },
                    onFinished = { persist(runtime.copy(offsetYDp = yOffset)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_ring_stroke),
                    summary = stringResource(R.string.status_combo_style_one_dp_summary, ringStroke.roundToInt()),
                    value = ringStroke,
                    range = 1f..4f,
                    onValueChange = { ringStroke = it; preview(runtime.copy(ringStrokeDp = it)) },
                    onFinished = { persist(runtime.copy(ringStrokeDp = ringStroke)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_track_alpha),
                    summary = stringResource(R.string.status_combo_style_one_track_summary, trackAlpha.roundToInt()),
                    value = trackAlpha,
                    range = 0f..180f,
                    onValueChange = { trackAlpha = it; preview(runtime.copy(trackAlpha = it.roundToInt())) },
                    onFinished = { persist(runtime.copy(trackAlpha = trackAlpha.roundToInt())) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_dot_size),
                    summary = stringResource(R.string.status_combo_style_one_percent_summary, dotSize.roundToInt()),
                    value = dotSize,
                    range = 50f..180f,
                    onValueChange = { dotSize = it; preview(runtime.copy(dotsSizePercent = it)) },
                    onFinished = { persist(runtime.copy(dotsSizePercent = dotSize)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_dot_gap),
                    summary = stringResource(R.string.status_combo_style_one_dp_summary, dotGap.roundToInt()),
                    value = dotGap,
                    range = 0f..6f,
                    onValueChange = { dotGap = it; preview(runtime.copy(dotsGapDp = it)) },
                    onFinished = { persist(runtime.copy(dotsGapDp = dotGap)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_percent_size),
                    summary = stringResource(R.string.status_combo_style_one_sp_summary, numberSize.roundToInt()),
                    value = numberSize,
                    range = 5f..14f,
                    onValueChange = { numberSize = it; preview(runtime.copy(batteryTextSizeSp = it)) },
                    onFinished = { persist(runtime.copy(batteryTextSizeSp = numberSize)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_percent_weight),
                    summary = stringResource(R.string.status_combo_style_one_weight_summary, numberWeight.roundToInt()),
                    value = numberWeight,
                    range = 100f..900f,
                    onValueChange = { numberWeight = it; preview(runtime.copy(batteryTextFontWeight = it.roundToInt())) },
                    onFinished = { persist(runtime.copy(batteryTextFontWeight = numberWeight.roundToInt())) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_bolt_size),
                    summary = stringResource(R.string.status_combo_style_one_percent_summary, boltSize.roundToInt()),
                    value = boltSize,
                    range = 50f..180f,
                    onValueChange = { boltSize = it; preview(runtime.copy(chargingBoltSizePercent = it)) },
                    onFinished = { persist(runtime.copy(chargingBoltSizePercent = boltSize)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_bolt_x),
                    summary = stringResource(R.string.status_combo_style_one_dp_summary, boltX.roundToInt()),
                    value = boltX,
                    range = -8f..8f,
                    onValueChange = { boltX = it; preview(runtime.copy(chargingBoltXDp = it)) },
                    onFinished = { persist(runtime.copy(chargingBoltXDp = boltX)) },
                )
            }
            add {
                styleOneSlider(
                    icon = icon,
                    title = stringResource(R.string.status_combo_style_one_bolt_y),
                    summary = stringResource(R.string.status_combo_style_one_dp_summary, boltY.roundToInt()),
                    value = boltY,
                    range = -8f..8f,
                    onValueChange = { boltY = it; preview(runtime.copy(chargingBoltYDp = it)) },
                    onFinished = { persist(runtime.copy(chargingBoltYDp = boltY)) },
                )
            }
        },
    )
    @Suppress("UNUSED_VARIABLE")
    val preserveEventSignature = onEvent
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
) {
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
                stepSize = 1f,
                hapticOnStep = true,
                onValueChange = onValueChange,
                onValueChangeFinished = onFinished,
            )
        }
    }
}
