package com.oneuixp.app.ui.screen.status

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneuixp.app.R
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.StatusBarRuntimeBridge
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.OneuiXActionButton
import com.oneuixp.app.ui.component.material.ExpressiveConfirmDialog
import io.github.soclear.oneuix.ui.component.SwitchItem
import io.github.soclear.oneuix.ui.component.SelectItem
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import kotlin.math.roundToInt

@Suppress("UNUSED_PARAMETER")
@Composable
fun StatusComboStyleOneCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    val viewModel = viewModel<OneuiXToolsViewModel>()
    val context = LocalContext.current
    // Match HyperDuo exactly: critical battery is a fixed 20% cutoff.
    // Normalize legacy saved values so preview and live SystemUI share the rule.
    val runtime = uiState.comboStyleOneRuntime.copy(criticalThreshold = 20)
    var previewRuntime by remember(runtime) { mutableStateOf(runtime) }
    var confirmResetColors by remember { mutableStateOf(false) }
    var editingColorRole by remember { mutableStateOf<StyleOneColorRole?>(null) }

    fun preview(next: Preference.SystemUI.StatusBarComboStyleOneRuntime) {
        previewRuntime = next.copy(criticalThreshold = 20)
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
    var valueSize by remember(runtime.valueSize) { mutableIntStateOf(runtime.valueSize.coerceIn(6, 18)) }
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
    var glyphScale by remember(runtime.glyphScalePercent) { mutableIntStateOf(runtime.glyphScalePercent.coerceIn(70, 150)) }
    var boltSize by remember(runtime.chargingBoltSizePercent) { mutableIntStateOf(runtime.chargingBoltSizePercent.coerceIn(50, 180)) }
    var boltX by remember(runtime.chargingBoltXDp) { mutableIntStateOf(runtime.chargingBoltXDp.coerceIn(-8, 8)) }
    var boltY by remember(runtime.chargingBoltYDp) { mutableIntStateOf(runtime.chargingBoltYDp.coerceIn(-8, 8)) }
    var trackAlpha by remember(runtime.trackAlpha) {
        mutableIntStateOf((runtime.trackAlpha * 100f / 255f).roundToInt())
    }

    StatusThemeGroup(items = listOf({
        Column {
            StyleOneInfoItem(
                title = stringResource(R.string.status_combo_style_one_preview),
                summary = stringResource(R.string.status_combo_style_one_preview_summary),
            )
            StatusThemeChildItem {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    factory = { StatusComboStyleOnePreviewView(it) },
                    update = { it.setConfig(previewRuntime) },
                )
            }
        }
    }, {
        SwitchItem(
            title = stringResource(R.string.status_combo_style_one_enable),
            summary = stringResource(R.string.status_combo_style_one_enable_summary),
            checked = runtime.enabled,
            onCheckedChange = { previewAndPersist(runtime.copy(enabled = it)) },
        )
    }))

    if (!runtime.enabled) return

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.status_combo_style_one_tab_general),
        stringResource(R.string.status_combo_style_one_tab_size),
        stringResource(R.string.status_combo_style_one_tab_color),
    )
    // Reuse the same theme-aware buttons used by the rest of OneuiXP.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, label ->
            StyleOneBackgroundButton(
                label = label,
                selected = selectedTab == index,
                modifier = Modifier.weight(1f).height(44.dp),
                onClick = { selectedTab = index },
            )
        }
    }

    when (selectedTab) {
        0 -> {
            StatusThemeGroup(items = listOf({
                StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_group_appearance))
            }, {
                SelectItem(
                    title = stringResource(R.string.status_combo_style_one_style),
                    summary = stringResource(R.string.status_combo_style_one_style_summary),
                    entries = listOf(
                        stringResource(R.string.status_combo_style_one_style_ring),
                        stringResource(R.string.status_combo_style_one_style_rect),
                    ),
                    selectedIndex = runtime.trioStyle.coerceIn(0, 1),
                    onSelectedIndexChange = {
                        previewAndPersist(runtime.copy(trioStyle = it.coerceIn(0, 1)))
                    },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_overlay),
                    summary = stringResource(R.string.status_combo_style_one_overlay_summary),
                    checked = runtime.overlayGlyph,
                    onCheckedChange = { previewAndPersist(runtime.copy(overlayGlyph = it)) },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_show_wifi),
                    summary = stringResource(R.string.status_combo_style_one_show_wifi_summary),
                    checked = runtime.showWifi,
                    onCheckedChange = { previewAndPersist(runtime.copy(showWifi = it)) },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_show_mobile),
                    summary = stringResource(R.string.status_combo_style_one_show_mobile_summary),
                    checked = runtime.showMobile,
                    onCheckedChange = { previewAndPersist(runtime.copy(showMobile = it)) },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_dual_sim),
                    summary = stringResource(R.string.status_combo_style_one_dual_sim_summary),
                    checked = runtime.dualSim,
                    onCheckedChange = { previewAndPersist(runtime.copy(dualSim = it)) },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_show_value),
                    summary = stringResource(R.string.status_combo_style_one_show_value_summary),
                    checked = runtime.showValue,
                    onCheckedChange = { previewAndPersist(runtime.copy(showValue = it)) },
                )
            }, {
                SelectItem(
                    title = stringResource(R.string.status_combo_style_one_signal_mode),
                    summary = stringResource(R.string.status_combo_style_one_signal_mode_summary),
                    entries = listOf(
                        stringResource(R.string.status_combo_style_one_signal_inside),
                        stringResource(R.string.status_combo_style_one_signal_outside),
                    ),
                    selectedIndex = runtime.signalMode.coerceIn(0, 1),
                    onSelectedIndexChange = { previewAndPersist(runtime.copy(signalMode = it.coerceIn(0, 1))) },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_stacked_signal),
                    summary = stringResource(R.string.status_combo_style_one_stacked_signal_summary),
                    checked = runtime.stackedSignal,
                    onCheckedChange = { previewAndPersist(runtime.copy(stackedSignal = it)) },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_data_sim_only),
                    summary = stringResource(R.string.status_combo_style_one_data_sim_only_summary),
                    checked = runtime.dataSimOnly,
                    onCheckedChange = { previewAndPersist(runtime.copy(dataSimOnly = it)) },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_show_bolt),
                    summary = stringResource(R.string.status_combo_style_one_show_bolt_summary),
                    checked = runtime.showBolt,
                    onCheckedChange = { previewAndPersist(runtime.copy(showBolt = it)) },
                )
            }, {
                SelectItem(
                    title = stringResource(R.string.status_combo_style_one_charging_display),
                    summary = stringResource(R.string.status_combo_style_one_charging_display_summary),
                    entries = listOf(
                        stringResource(R.string.status_combo_style_one_charging_display_bolt),
                        stringResource(R.string.status_combo_style_one_charging_display_alternate),
                    ),
                    selectedIndex = runtime.chargingDisplayMode.coerceIn(0, 1),
                    onSelectedIndexChange = {
                        previewAndPersist(runtime.copy(chargingDisplayMode = it.coerceIn(0, 1)))
                    },
                )
            }, {
                SelectItem(
                    title = stringResource(R.string.status_combo_style_one_mobile_type_mode),
                    summary = stringResource(R.string.status_combo_style_one_mobile_type_mode_summary),
                    entries = listOf(
                        stringResource(R.string.status_combo_style_one_mobile_type_off),
                        stringResource(R.string.status_combo_style_one_mobile_type_inside),
                        stringResource(R.string.status_combo_style_one_mobile_type_outside),
                    ),
                    selectedIndex = runtime.mobileTypeMode.coerceIn(0, 2),
                    onSelectedIndexChange = {
                        previewAndPersist(runtime.copy(mobileTypeMode = it.coerceIn(0, 2)))
                    },
                )
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_value_centred),
                    summary = stringResource(R.string.status_combo_style_one_value_centred_summary),
                    checked = runtime.valueCentred,
                    onCheckedChange = { previewAndPersist(runtime.copy(valueCentred = it)) },
                )
            }))

            StatusThemeGroup(items = listOf({
                StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_group_advanced))
            }, {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_debug_log),
                    summary = stringResource(R.string.status_combo_style_one_debug_log_summary),
                    checked = runtime.debugLog,
                    onCheckedChange = { previewAndPersist(runtime.copy(debugLog = it)) },
                )
            }))
        }
        1 -> {
            StatusThemeGroup(items = listOf({
                StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_group_appearance))
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_glyph_scale),
                    stringResource(R.string.status_combo_style_one_percent_summary, glyphScale), glyphScale.toFloat(), 70f..150f,
                    { glyphScale = it.roundToInt(); preview(runtime.copy(glyphScalePercent = glyphScale)) },
                    { persist(runtime.copy(glyphScalePercent = glyphScale)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_bolt_size),
                    stringResource(R.string.status_combo_style_one_percent_summary, boltSize), boltSize.toFloat(), 50f..180f,
                    { boltSize = it.roundToInt(); preview(runtime.copy(chargingBoltSizePercent = boltSize)) },
                    { persist(runtime.copy(chargingBoltSizePercent = boltSize)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_bolt_x),
                    stringResource(R.string.status_combo_style_one_dp_summary, boltX), boltX.toFloat(), -8f..8f,
                    { boltX = it.roundToInt(); preview(runtime.copy(chargingBoltXDp = boltX)) },
                    { persist(runtime.copy(chargingBoltXDp = boltX)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_bolt_y),
                    stringResource(R.string.status_combo_style_one_dp_summary, boltY), boltY.toFloat(), -8f..8f,
                    { boltY = it.roundToInt(); preview(runtime.copy(chargingBoltYDp = boltY)) },
                    { persist(runtime.copy(chargingBoltYDp = boltY)) })
            }))

            StatusThemeGroup(items = listOf({
                StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_group_stroke))
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_ring_stroke),
                    stringResource(R.string.status_combo_style_one_int_summary, ringStroke), ringStroke.toFloat(), 4f..16f,
                    { ringStroke = it.roundToInt(); preview(runtime.copy(ringStroke = ringStroke)) },
                    { persist(runtime.copy(ringStroke = ringStroke)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_arc_stroke),
                    stringResource(R.string.status_combo_style_one_int_summary, arcStroke), arcStroke.toFloat(), 3f..16f,
                    { arcStroke = it.roundToInt(); preview(runtime.copy(arcStroke = arcStroke)) },
                    { persist(runtime.copy(arcStroke = arcStroke)) })
            }))

            StatusThemeGroup(items = listOf({
                StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_group_text))
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_value_size),
                    stringResource(R.string.status_combo_style_one_sp_summary, valueSize), valueSize.toFloat(), 6f..18f,
                    { valueSize = it.roundToInt(); preview(runtime.copy(valueSize = valueSize)) },
                    { persist(runtime.copy(valueSize = valueSize)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_value_weight),
                    stringResource(R.string.status_combo_style_one_weight_summary, valueWeight), valueWeight.toFloat(), 100f..900f,
                    { valueWeight = (it / 100f).roundToInt() * 100; preview(runtime.copy(valueWeight = valueWeight)) },
                    { persist(runtime.copy(valueWeight = valueWeight)) }, stepSize = 100f)
            }))

            StatusThemeGroup(items = listOf({
                StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_group_type))
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_type_size),
                    stringResource(R.string.status_combo_style_one_int_summary, typeSize), typeSize.toFloat(), 16f..44f,
                    { typeSize = it.roundToInt(); preview(runtime.copy(typeSize = typeSize)) },
                    { persist(runtime.copy(typeSize = typeSize)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_type_weight),
                    stringResource(R.string.status_combo_style_one_weight_summary, typeWeight), typeWeight.toFloat(), 100f..900f,
                    { typeWeight = (it / 100f).roundToInt() * 100; preview(runtime.copy(typeWeight = typeWeight)) },
                    { persist(runtime.copy(typeWeight = typeWeight)) }, stepSize = 100f)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_type_suffix_scale),
                    stringResource(R.string.status_combo_style_one_percent_summary, suffixScale), suffixScale.toFloat(), 50f..100f,
                    { suffixScale = it.roundToInt(); preview(runtime.copy(typeSuffixScale = suffixScale)) },
                    { persist(runtime.copy(typeSuffixScale = suffixScale)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_type_size),
                    stringResource(R.string.status_combo_style_one_dp_summary, outTypeSize), outTypeSize.toFloat(), 8f..26f,
                    { outTypeSize = it.roundToInt(); preview(runtime.copy(outTypeSizeDp = outTypeSize)) },
                    { persist(runtime.copy(outTypeSizeDp = outTypeSize)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_type_margin_left),
                    stringResource(R.string.status_combo_style_one_dp_summary, outTypeLeft), outTypeLeft.toFloat(), 0f..16f,
                    { outTypeLeft = it.roundToInt(); preview(runtime.copy(outTypeMarginLeftDp = outTypeLeft)) },
                    { persist(runtime.copy(outTypeMarginLeftDp = outTypeLeft)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_type_margin_right),
                    stringResource(R.string.status_combo_style_one_dp_summary, outTypeRight), outTypeRight.toFloat(), 0f..16f,
                    { outTypeRight = it.roundToInt(); preview(runtime.copy(outTypeMarginRightDp = outTypeRight)) },
                    { persist(runtime.copy(outTypeMarginRightDp = outTypeRight)) })
            }))

            StatusThemeGroup(items = listOf({
                StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_group_out_signal))
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_signal_size),
                    stringResource(R.string.status_combo_style_one_dp_summary, outSignalSize), outSignalSize.toFloat(), 6f..20f,
                    { outSignalSize = it.roundToInt(); preview(runtime.copy(outSignalSizeDp = outSignalSize)) },
                    { persist(runtime.copy(outSignalSizeDp = outSignalSize)) })
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_signal_margin),
                    stringResource(R.string.status_combo_style_one_dp_summary, outSignalMargin), outSignalMargin.toFloat(), 0f..16f,
                    { outSignalMargin = it.roundToInt(); preview(runtime.copy(outSignalMarginDp = outSignalMargin)) },
                    { persist(runtime.copy(outSignalMarginDp = outSignalMargin)) })
            }))

            StatusThemeGroup(items = listOf({
                StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_group_track))
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_track_alpha),
                    stringResource(R.string.status_combo_style_one_percent_summary, trackAlpha), trackAlpha.toFloat(), 0f..100f,
                    { trackAlpha = it.roundToInt(); val a = (trackAlpha * 255f / 100f).roundToInt(); preview(runtime.copy(trackAlpha = a)) },
                    { persist(runtime.copy(trackAlpha = (trackAlpha * 255f / 100f).roundToInt())) })
            }))
        }
        else -> {
            // Keep the entire state-colour section together like HyperDuo:
            // toggle, thresholds, three role rows and reset live in one card.
            val colorItems = mutableListOf<@Composable () -> Unit>()
            colorItems += {
                SwitchItem(
                    title = stringResource(R.string.status_combo_style_one_role_colors),
                    summary = stringResource(R.string.status_combo_style_one_role_colors_summary),
                    checked = runtime.roleColors,
                    onCheckedChange = { previewAndPersist(runtime.copy(roleColors = it)) },
                )
            }
            colorItems += {
                    styleOneSlider(
                        stringResource(R.string.status_combo_style_one_low_threshold),
                        stringResource(R.string.status_combo_style_one_low_threshold_summary, lowThreshold),
                        lowThreshold.toFloat(),
                        5f..50f,
                        { lowThreshold = it.roundToInt(); preview(runtime.copy(lowThreshold = lowThreshold)) },
                        { persist(runtime.copy(lowThreshold = lowThreshold)) },
                        enabled = runtime.roleColors,
                    )
                }
                colorItems += {
                    StyleOneRoleColorRow(
                        title = stringResource(StyleOneColorRole.Low.titleRes),
                        summary = stringResource(R.string.status_combo_style_one_color_summary),
                        darkColor = runtime.lowColorDarkArgb,
                        lightColor = runtime.lowColorLightArgb,
                        enabled = runtime.roleColors,
                        onClick = { editingColorRole = StyleOneColorRole.Low },
                    )
                }
                colorItems += {
                    StyleOneRoleColorRow(
                        title = stringResource(StyleOneColorRole.Charging.titleRes),
                        summary = stringResource(R.string.status_combo_style_one_color_summary),
                        darkColor = runtime.chargingColorDarkArgb,
                        lightColor = runtime.chargingColorLightArgb,
                        enabled = runtime.roleColors,
                        onClick = { editingColorRole = StyleOneColorRole.Charging },
                    )
                }
                colorItems += {
                    StyleOneRoleColorRow(
                        title = stringResource(StyleOneColorRole.Critical.titleRes),
                        summary = stringResource(R.string.status_combo_style_one_color_summary),
                        darkColor = runtime.criticalColorDarkArgb,
                        lightColor = runtime.criticalColorLightArgb,
                        enabled = runtime.roleColors,
                        onClick = { editingColorRole = StyleOneColorRole.Critical },
                    )
                }
                colorItems += {
                    StyleOneInfoItem(
                        title = stringResource(R.string.status_combo_style_one_reset_colors),
                        summary = stringResource(R.string.status_combo_style_one_reset_colors_summary),
                        modifier = Modifier
                            .alpha(if (runtime.roleColors) 1f else 0.45f)
                            .clickable(enabled = runtime.roleColors) { confirmResetColors = true },
                        trailingContent = {
                            Text("›", style = Material3Theme.typography.titleLarge)
                        },
                    )
                }
            StatusThemeGroup(items = colorItems)

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

            val role = editingColorRole
            if (role != null) {
                StyleOneRoleColorDialog(
                    roleTitle = stringResource(role.titleRes),
                    darkLabel = stringResource(R.string.status_combo_style_one_color_on_dark),
                    lightLabel = stringResource(R.string.status_combo_style_one_color_on_light),
                    summary = stringResource(R.string.status_combo_style_one_color_dialog_summary),
                    darkColorArgb = runtime.colorForRole(role, onDarkBackground = true),
                    lightColorArgb = runtime.colorForRole(role, onDarkBackground = false),
                    onColorChange = { onDark, color ->
                        previewAndPersist(runtime.withRoleColor(role, onDark, color))
                    },
                    onDismiss = { editingColorRole = null },
                )
            }
        }
    }

}

private enum class StyleOneColorRole(val titleRes: Int) {
    Low(R.string.status_combo_style_one_color_low),
    Charging(R.string.status_combo_style_one_color_charging),
    Critical(R.string.status_combo_style_one_color_critical),
}

private fun Preference.SystemUI.StatusBarComboStyleOneRuntime.colorForRole(
    role: StyleOneColorRole,
    onDarkBackground: Boolean,
): Int = when (role) {
    StyleOneColorRole.Low -> if (onDarkBackground) lowColorDarkArgb else lowColorLightArgb
    StyleOneColorRole.Charging -> if (onDarkBackground) chargingColorDarkArgb else chargingColorLightArgb
    StyleOneColorRole.Critical -> if (onDarkBackground) criticalColorDarkArgb else criticalColorLightArgb
}

private fun Preference.SystemUI.StatusBarComboStyleOneRuntime.withRoleColor(
    role: StyleOneColorRole,
    onDarkBackground: Boolean,
    colorArgb: Int,
): Preference.SystemUI.StatusBarComboStyleOneRuntime = when (role) {
    StyleOneColorRole.Low -> if (onDarkBackground) copy(lowColorDarkArgb = colorArgb) else copy(lowColorLightArgb = colorArgb)
    StyleOneColorRole.Charging -> if (onDarkBackground) copy(chargingColorDarkArgb = colorArgb) else copy(chargingColorLightArgb = colorArgb)
    StyleOneColorRole.Critical -> if (onDarkBackground) copy(criticalColorDarkArgb = colorArgb) else copy(criticalColorLightArgb = colorArgb)
}

@Composable
private fun StyleOneColorSwatch(color: Int) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(Color(color), CircleShape)
            .border(1.dp, Color(0x66000000), CircleShape),
    )
}

@Composable
private fun StyleOneRoleColorRow(
    title: String,
    summary: String,
    darkColor: Int,
    lightColor: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    StatusThemeChildItem {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.45f)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StyleOneColorSwatch(darkColor)
                StyleOneColorSwatch(lightColor)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(title, style = Material3Theme.typography.titleSmall)
                when (LocalUiMode.current) {
                    UiMode.Material -> Text(
                        summary,
                        style = Material3Theme.typography.bodySmall,
                        color = Material3Theme.colorScheme.onSurfaceVariant,
                    )
                    UiMode.Miuix -> MiuixText(
                        text = summary,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            Text("›", style = Material3Theme.typography.titleLarge)
        }
    }
}

@Composable
private fun StyleOneBackgroundButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialButton(
            onClick = onClick,
            modifier = modifier,
            colors = MaterialButtonDefaults.buttonColors(
                containerColor = if (selected) Material3Theme.colorScheme.primaryContainer
                    else Material3Theme.colorScheme.surfaceVariant,
                contentColor = if (selected) Material3Theme.colorScheme.onPrimaryContainer
                    else Material3Theme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(label)
        }
        UiMode.Miuix -> MiuixButton(
            onClick = onClick,
            modifier = modifier,
            colors = if (selected) {
                MiuixButtonDefaults.buttonColors()
            } else {
                MiuixButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                )
            },
        ) {
            MiuixText(text = label)
        }
    }
}

@Composable
private fun StyleOneRoleColorDialog(
    roleTitle: String,
    darkLabel: String,
    lightLabel: String,
    summary: String,
    darkColorArgb: Int,
    lightColorArgb: Int,
    onColorChange: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var onDark by remember(roleTitle) { mutableStateOf(true) }

    @Composable
    fun PickerContent() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StyleOneBackgroundButton(
                    label = darkLabel,
                    selected = onDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onDark = true },
                )
                StyleOneBackgroundButton(
                    label = lightLabel,
                    selected = !onDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onDark = false },
                )
            }
            ColorPalette(
                color = Color(if (onDark) darkColorArgb else lightColorArgb),
                onColorChanged = { onColorChange(onDark, it.toArgb()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    when (LocalUiMode.current) {
        UiMode.Material -> Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = roleTitle)
                    Text(text = summary)
                    PickerContent()
                }
            }
        }

        UiMode.Miuix -> WindowDialog(
            show = true,
            title = roleTitle,
            onDismissRequest = onDismiss,
            content = {
                Column {
                    MiuixText(text = summary)
                    PickerContent()
                }
            },
        )
    }
}

@Composable
private fun StyleOneInfoItem(
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (summary == null) 0.dp else 3.dp),
        ) {
            when (LocalUiMode.current) {
                UiMode.Material -> {
                    Text(text = title, style = Material3Theme.typography.titleSmall)
                    if (summary != null) {
                        Text(
                            text = summary,
                            style = Material3Theme.typography.bodySmall,
                            color = Material3Theme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                UiMode.Miuix -> {
                    MiuixText(text = title, color = MiuixTheme.colorScheme.onSurface)
                    if (summary != null) {
                        MiuixText(
                            text = summary,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
        if (trailingContent != null) {
            Box(contentAlignment = Alignment.Center) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun styleOneSlider(
    title: String,
    summary: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
    stepSize: Float = 1f,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    var lastStep by remember(title, range.start, range.endInclusive) { mutableFloatStateOf(value) }
    Column {
        StyleOneInfoItem(
            title = title,
            summary = summary,
            modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
        )
        StatusThemeChildItem {
            OneuiXSlider(
                value = value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .alpha(if (enabled) 1f else 0.45f),
                valueRange = range,
                steps = 0,
                onValueChange = { raw ->
                    if (enabled) {
                        val bounded = raw.coerceIn(range.start, range.endInclusive)
                        val next = (((bounded - range.start) / stepSize).roundToInt() * stepSize + range.start)
                            .coerceIn(range.start, range.endInclusive)
                        if (next != lastStep) {
                            lastStep = next
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        onValueChange(next)
                    }
                },
                onValueChangeFinished = { if (enabled) onFinished() },
            )
        }
    }
}
