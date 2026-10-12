package com.oneuixp.app.ui.screen.status

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
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
    // Match the upstream threshold rule: critical battery uses a fixed 20% cutoff.
    // Normalize legacy saved values so preview and live SystemUI share the rule.
    val runtime = uiState.comboStyleOneRuntime.copy(criticalThreshold = 20)
    var previewRuntime by remember(runtime) { mutableStateOf(runtime) }
    var confirmResetColors by remember { mutableStateOf(false) }
    var editingColorRole by remember { mutableStateOf<StyleOneColorRole?>(null) }
    // Keep drag edits in memory. Apply performs the single persistent write; previews
    // sent to SystemUI are throttled instead of broadcasting for every pointer sample.
    val colorPreviewDraft = remember(runtime) { arrayOf(runtime) }
    val lastColorPreviewAt = remember { longArrayOf(0L) }

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

    fun previewColorDraft(role: StyleOneColorRole, onDarkBackground: Boolean, colorArgb: Int) {
        val next = colorPreviewDraft[0].withRoleColor(role, onDarkBackground, colorArgb)
        colorPreviewDraft[0] = next
        val now = SystemClock.uptimeMillis()
        if (now - lastColorPreviewAt[0] >= 48L) {
            preview(next)
            lastColorPreviewAt[0] = now
        }
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
    var outSignalMarginLeft by remember(runtime.outSignalMarginLeftDp) { mutableIntStateOf(runtime.outSignalMarginLeftDp.coerceIn(0, 16)) }
    var outTypeLeft by remember(runtime.outTypeMarginLeftDp) { mutableIntStateOf(runtime.outTypeMarginLeftDp.coerceIn(0, 16)) }
    var outTypeRight by remember(runtime.outTypeMarginRightDp) { mutableIntStateOf(runtime.outTypeMarginRightDp.coerceIn(0, 16)) }
    var outSignalMargin by remember(runtime.outSignalMarginDp) { mutableIntStateOf(runtime.outSignalMarginDp.coerceIn(0, 16)) }
    var lowThreshold by remember(runtime.lowThreshold) { mutableIntStateOf(runtime.lowThreshold.coerceIn(5, 50)) }
    var glyphScale by remember(runtime.glyphScalePercent) { mutableIntStateOf(runtime.glyphScalePercent.coerceIn(70, 150)) }
    var glyphOffsetX by remember(runtime.glyphOffsetXDp) { mutableIntStateOf(runtime.glyphOffsetXDp.coerceIn(-16, 16)) }
    var glyphOffsetY by remember(runtime.glyphOffsetYDp) { mutableIntStateOf(runtime.glyphOffsetYDp.coerceIn(-16, 16)) }
    var glyphLeftMargin by remember(runtime.glyphLeftMarginDp) { mutableIntStateOf(runtime.glyphLeftMarginDp.coerceIn(0, 16)) }
    var glyphRightMargin by remember(runtime.glyphRightMarginDp) { mutableIntStateOf(runtime.glyphRightMarginDp.coerceIn(0, 16)) }
    var boltSize by remember(runtime.chargingBoltSizePercent) { mutableIntStateOf(runtime.chargingBoltSizePercent.coerceIn(50, 180)) }
    var boltX by remember(runtime.chargingBoltXDp) { mutableIntStateOf(runtime.chargingBoltXDp.coerceIn(-8, 8)) }
    var boltY by remember(runtime.chargingBoltYDp) { mutableIntStateOf(runtime.chargingBoltYDp.coerceIn(-8, 8)) }
    var trackAlpha by remember(runtime.trackAlpha) {
        mutableIntStateOf((runtime.trackAlpha * 100f / 255f).roundToInt())
    }
    var signalDotSize by remember(runtime.signalDotSizePercent) { mutableIntStateOf(runtime.signalDotSizePercent.coerceIn(50, 150)) }
    var signalDotSpacing by remember(runtime.signalDotSpacingDp) { mutableIntStateOf(runtime.signalDotSpacingDp.coerceIn(3, 8)) }
    var signalDotOffsetX by remember(runtime.signalDotOffsetXDp) { mutableIntStateOf(runtime.signalDotOffsetXDp.coerceIn(-8, 8)) }
    var signalDotOffsetY by remember(runtime.signalDotOffsetYDp) { mutableIntStateOf(runtime.signalDotOffsetYDp.coerceIn(-8, 8)) }

    StatusThemeGroup(
        items = listOf({
            StatusThemeChildItem {
                Column {
                    StyleOneInfoItem(title = stringResource(R.string.status_combo_style_one_preview))
                    AndroidView(modifier = Modifier.fillMaxWidth().height(60.dp), factory = { StatusComboStyleOnePreviewView(it) }, update = { it.setConfig(previewRuntime) })
                }
            }
        }),
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.status_combo_style_one_tab_general),
        stringResource(R.string.status_combo_style_one_tab_size),
        stringResource(R.string.status_combo_style_one_tab_color),
    )
    val tabHaptic = LocalHapticFeedback.current
    when (LocalUiMode.current) {
        UiMode.Material -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, label ->
                    ToggleButton(
                        checked = selectedTab == index,
                        onCheckedChange = { checked ->
                            if (checked) {
                                tabHaptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                selectedTab = index
                            }
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) { Text(text = label) }
                }
            }
        }
        UiMode.Miuix -> Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            MiuixTabRow(
                tabs = tabs,
                selectedTabIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                height = 48.dp,
            )
        }
    }

    when (selectedTab) {
        0 -> {
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_appearance), titleAlignedToContent = true, items = listOf({
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_enable), summary = stringResource(R.string.status_combo_style_one_enable_summary), checked = runtime.enabled, onCheckedChange = { previewAndPersist(runtime.copy(enabled = it)) })
            }, {
                SelectItem(icon = null, title = stringResource(R.string.status_combo_style_one_style), summary = stringResource(R.string.status_combo_style_one_style_summary), entries = listOf(stringResource(R.string.status_combo_style_one_style_ring), stringResource(R.string.status_combo_style_one_style_rect)), selectedIndex = runtime.trioStyle.coerceIn(0, 1), enabled = runtime.enabled, onSelectedIndexChange = { previewAndPersist(runtime.copy(trioStyle = it.coerceIn(0, 1))) })
            }, {
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_overlay), summary = stringResource(R.string.status_combo_style_one_overlay_summary), checked = runtime.overlayGlyph, enabled = runtime.enabled, onCheckedChange = { previewAndPersist(runtime.copy(overlayGlyph = it)) })
            }, {
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_show_wifi), summary = stringResource(R.string.status_combo_style_one_show_wifi_summary), checked = runtime.showWifi, enabled = runtime.enabled, onCheckedChange = { previewAndPersist(runtime.copy(showWifi = it)) })
            }, {
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_show_mobile), summary = stringResource(R.string.status_combo_style_one_show_mobile_summary), checked = runtime.showMobile, enabled = runtime.enabled, onCheckedChange = { previewAndPersist(runtime.copy(showMobile = it)) })
            }, {
                SelectItem(
                    icon = null,
                    title = stringResource(R.string.status_combo_style_one_dual_sim),
                    summary = stringResource(R.string.status_combo_style_one_dual_sim_summary),
                    entries = listOf(
                        stringResource(R.string.status_combo_style_one_dual_sim_off),
                        stringResource(R.string.status_combo_style_one_dual_sim_ring_vertical),
                        stringResource(R.string.status_combo_style_one_dual_sim_below_ring),
                    ),
                    selectedIndex = when {
                        runtime.dualSimMode in 1..2 -> runtime.dualSimMode
                        runtime.dualSim -> 1
                        else -> 0
                    },
                    enabled = runtime.enabled && runtime.showMobile,
                    onSelectedIndexChange = { selected ->
                        val mode = selected.coerceIn(0, 2)
                        previewAndPersist(runtime.copy(dualSimMode = mode, dualSim = mode != 0))
                    },
                )
            }, {
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_show_value), summary = stringResource(R.string.status_combo_style_one_show_value_summary), checked = runtime.showValue, enabled = runtime.enabled, onCheckedChange = { previewAndPersist(runtime.copy(showValue = it)) })
            }, {
                SelectItem(icon = null, title = stringResource(R.string.status_combo_style_one_signal_mode), summary = stringResource(R.string.status_combo_style_one_signal_mode_summary), entries = listOf(stringResource(R.string.status_combo_style_one_signal_inside), stringResource(R.string.status_combo_style_one_signal_outside)), selectedIndex = runtime.signalMode.coerceIn(0, 1), enabled = runtime.enabled, onSelectedIndexChange = { previewAndPersist(runtime.copy(signalMode = it.coerceIn(0, 1))) })
            }, {
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_stacked_signal), summary = stringResource(R.string.status_combo_style_one_stacked_signal_summary), checked = runtime.stackedSignal, enabled = runtime.enabled && runtime.signalMode == 1, onCheckedChange = { previewAndPersist(runtime.copy(stackedSignal = it)) })
            }, {
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_data_sim_only), summary = stringResource(R.string.status_combo_style_one_data_sim_only_summary), checked = runtime.dataSimOnly, enabled = runtime.enabled && runtime.signalMode == 1 && runtime.stackedSignal, onCheckedChange = { previewAndPersist(runtime.copy(dataSimOnly = it)) })
            }, {
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_show_bolt), summary = stringResource(R.string.status_combo_style_one_show_bolt_summary), checked = runtime.showBolt, enabled = runtime.enabled && runtime.showValue, onCheckedChange = { previewAndPersist(runtime.copy(showBolt = it)) })
            }, {
                SelectItem(icon = null, title = stringResource(R.string.status_combo_style_one_charging_display), summary = stringResource(R.string.status_combo_style_one_charging_display_summary), entries = listOf(stringResource(R.string.status_combo_style_one_charging_display_bolt), stringResource(R.string.status_combo_style_one_charging_display_alternate)), selectedIndex = runtime.chargingDisplayMode.coerceIn(0, 1), enabled = runtime.enabled && runtime.showValue && runtime.showBolt, onSelectedIndexChange = { previewAndPersist(runtime.copy(chargingDisplayMode = it.coerceIn(0, 1))) })
            }, {
                SelectItem(icon = null, title = stringResource(R.string.status_combo_style_one_mobile_type_mode), summary = stringResource(R.string.status_combo_style_one_mobile_type_mode_summary), entries = listOf(stringResource(R.string.status_combo_style_one_mobile_type_off), stringResource(R.string.status_combo_style_one_mobile_type_inside), stringResource(R.string.status_combo_style_one_mobile_type_outside)), selectedIndex = runtime.mobileTypeMode.coerceIn(0, 2), enabled = runtime.enabled, onSelectedIndexChange = { previewAndPersist(runtime.copy(mobileTypeMode = it.coerceIn(0, 2))) })
            }, {
                SwitchItem(icon = null, title = stringResource(R.string.status_combo_style_one_value_centred), summary = stringResource(R.string.status_combo_style_one_value_centred_summary), checked = runtime.valueCentred, enabled = runtime.enabled && runtime.trioStyle == 0 && runtime.showWifi && runtime.showValue, onCheckedChange = { previewAndPersist(runtime.copy(valueCentred = it)) })
            }))
        }
        1 -> {
            // Overall size, offsets and gaps all affect the complete glyph.
            val ringDotsEnabled = runtime.enabled && runtime.trioStyle == 0 && runtime.showMobile && runtime.signalMode == 0
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_overall_size), titleAlignedToContent = true, items = listOf({
                styleOneSlider(stringResource(R.string.status_combo_style_one_glyph_scale), stringResource(R.string.status_combo_style_one_percent_summary, glyphScale), glyphScale.toFloat(), 70f..150f, { glyphScale = it.roundToInt(); preview(runtime.copy(glyphScalePercent = glyphScale)) }, { final -> persist(runtime.copy(glyphScalePercent = final.roundToInt())) }, enabled = runtime.enabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_glyph_offset_x), stringResource(R.string.status_combo_style_one_glyph_offset_x_summary, glyphOffsetX), glyphOffsetX.toFloat(), -16f..16f, { glyphOffsetX = it.roundToInt(); preview(runtime.copy(glyphOffsetXDp = glyphOffsetX)) }, { final -> persist(runtime.copy(glyphOffsetXDp = final.roundToInt())) }, enabled = runtime.enabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_glyph_offset_y), stringResource(R.string.status_combo_style_one_glyph_offset_y_summary, glyphOffsetY), glyphOffsetY.toFloat(), -16f..16f, { glyphOffsetY = it.roundToInt(); preview(runtime.copy(glyphOffsetYDp = glyphOffsetY)) }, { final -> persist(runtime.copy(glyphOffsetYDp = final.roundToInt())) }, enabled = runtime.enabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_glyph_left_margin), stringResource(R.string.status_combo_style_one_glyph_left_margin_summary, glyphLeftMargin), glyphLeftMargin.toFloat(), 0f..16f, { glyphLeftMargin = it.roundToInt(); preview(runtime.copy(glyphLeftMarginDp = glyphLeftMargin)) }, { final -> persist(runtime.copy(glyphLeftMarginDp = final.roundToInt())) }, enabled = runtime.enabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_glyph_right_margin), stringResource(R.string.status_combo_style_one_glyph_right_margin_summary, glyphRightMargin), glyphRightMargin.toFloat(), 0f..16f, { glyphRightMargin = it.roundToInt(); preview(runtime.copy(glyphRightMarginDp = glyphRightMargin)) }, { final -> persist(runtime.copy(glyphRightMarginDp = final.roundToInt())) }, enabled = runtime.enabled)
            }))
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_signal_dots), titleAlignedToContent = true, items = listOf({
                styleOneSlider(stringResource(R.string.status_combo_style_one_signal_dot_size), stringResource(R.string.status_combo_style_one_signal_dot_size_summary, signalDotSize), signalDotSize.toFloat(), 50f..150f, { signalDotSize = it.roundToInt(); preview(runtime.copy(signalDotSizePercent = signalDotSize)) }, { final -> persist(runtime.copy(signalDotSizePercent = final.roundToInt())) }, enabled = ringDotsEnabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_signal_dot_spacing), stringResource(R.string.status_combo_style_one_signal_dot_spacing_summary, signalDotSpacing), signalDotSpacing.toFloat(), 3f..8f, { signalDotSpacing = it.roundToInt(); preview(runtime.copy(signalDotSpacingDp = signalDotSpacing)) }, { final -> persist(runtime.copy(signalDotSpacingDp = final.roundToInt())) }, enabled = ringDotsEnabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_signal_dot_offset_x), stringResource(R.string.status_combo_style_one_signal_dot_offset_x_summary, signalDotOffsetX), signalDotOffsetX.toFloat(), -8f..8f, { signalDotOffsetX = it.roundToInt(); preview(runtime.copy(signalDotOffsetXDp = signalDotOffsetX)) }, { final -> persist(runtime.copy(signalDotOffsetXDp = final.roundToInt())) }, enabled = ringDotsEnabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_signal_dot_offset_y), stringResource(R.string.status_combo_style_one_signal_dot_offset_y_summary, signalDotOffsetY), signalDotOffsetY.toFloat(), -8f..8f, { signalDotOffsetY = it.roundToInt(); preview(runtime.copy(signalDotOffsetYDp = signalDotOffsetY)) }, { final -> persist(runtime.copy(signalDotOffsetYDp = final.roundToInt())) }, enabled = ringDotsEnabled)
            }))
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_stroke), titleAlignedToContent = true, items = listOf({
                styleOneSlider(stringResource(if (runtime.trioStyle == 1) R.string.status_combo_style_one_ring_stroke_rect else R.string.status_combo_style_one_ring_stroke), stringResource(if (runtime.trioStyle == 1) R.string.status_combo_style_one_ring_stroke_summary_rect else R.string.status_combo_style_one_ring_stroke_summary, ringStroke), ringStroke.toFloat(), 4f..16f, { ringStroke = it.roundToInt(); preview(runtime.copy(ringStroke = ringStroke)) }, { final -> persist(runtime.copy(ringStroke = final.roundToInt())) }, enabled = runtime.enabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_arc_stroke), stringResource(if (runtime.trioStyle == 1) R.string.status_combo_style_one_arc_stroke_summary_rect else R.string.status_combo_style_one_arc_stroke_summary, arcStroke), arcStroke.toFloat(), 3f..16f, { arcStroke = it.roundToInt(); preview(runtime.copy(arcStroke = arcStroke)) }, { final -> persist(runtime.copy(arcStroke = final.roundToInt())) }, enabled = runtime.enabled)
            }))
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_text), titleAlignedToContent = true, items = listOf({
                styleOneSlider(stringResource(R.string.status_combo_style_one_value_size), stringResource(R.string.status_combo_style_one_value_size_summary, valueSize), valueSize.toFloat(), 6f..18f, { valueSize = it.roundToInt(); preview(runtime.copy(valueSize = valueSize)) }, { final -> persist(runtime.copy(valueSize = final.roundToInt())) }, enabled = runtime.enabled)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_value_weight), stringResource(R.string.status_combo_style_one_value_weight_summary, valueWeight), valueWeight.toFloat(), 100f..900f, { valueWeight = (it / 100f).roundToInt() * 100; preview(runtime.copy(valueWeight = valueWeight)) }, { final -> persist(runtime.copy(valueWeight = final.roundToInt())) }, stepSize = 100f, enabled = runtime.enabled && runtime.showValue)
            }))
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_type), titleAlignedToContent = true, items = listOf({
                styleOneSlider(stringResource(R.string.status_combo_style_one_type_size), stringResource(R.string.status_combo_style_one_type_size_summary, typeSize), typeSize.toFloat(), 16f..44f, { typeSize = it.roundToInt(); preview(runtime.copy(typeSize = typeSize)) }, { final -> persist(runtime.copy(typeSize = final.roundToInt())) }, enabled = runtime.enabled && runtime.mobileTypeMode == 1)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_type_size), stringResource(R.string.status_combo_style_one_out_type_size_summary, outTypeSize), outTypeSize.toFloat(), 8f..26f, { outTypeSize = it.roundToInt(); preview(runtime.copy(outTypeSizeDp = outTypeSize)) }, { final -> persist(runtime.copy(outTypeSizeDp = final.roundToInt())) }, enabled = runtime.enabled && runtime.mobileTypeMode == 2)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_type_suffix_scale), stringResource(R.string.status_combo_style_one_type_suffix_scale_summary, suffixScale), suffixScale.toFloat(), 50f..100f, { suffixScale = it.roundToInt(); preview(runtime.copy(typeSuffixScale = suffixScale)) }, { final -> persist(runtime.copy(typeSuffixScale = final.roundToInt())) }, enabled = runtime.enabled && runtime.mobileTypeMode != 0)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_type_margin_left), stringResource(R.string.status_combo_style_one_out_type_margin_left_summary, outTypeLeft), outTypeLeft.toFloat(), 0f..16f, { outTypeLeft = it.roundToInt(); preview(runtime.copy(outTypeMarginLeftDp = outTypeLeft)) }, { final -> persist(runtime.copy(outTypeMarginLeftDp = final.roundToInt())) }, enabled = runtime.enabled && runtime.mobileTypeMode == 2)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_type_margin_right), stringResource(R.string.status_combo_style_one_out_type_margin_right_summary, outTypeRight), outTypeRight.toFloat(), 0f..16f, { outTypeRight = it.roundToInt(); preview(runtime.copy(outTypeMarginRightDp = outTypeRight)) }, { final -> persist(runtime.copy(outTypeMarginRightDp = final.roundToInt())) }, enabled = runtime.enabled && runtime.mobileTypeMode == 2)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_type_weight), stringResource(R.string.status_combo_style_one_type_weight_summary, typeWeight), typeWeight.toFloat(), 100f..900f, { typeWeight = (it / 100f).roundToInt() * 100; preview(runtime.copy(typeWeight = typeWeight)) }, { final -> persist(runtime.copy(typeWeight = final.roundToInt())) }, stepSize = 100f, enabled = runtime.enabled && runtime.mobileTypeMode != 0)
            }))
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_out_signal), titleAlignedToContent = true, items = listOf({
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_signal_size), stringResource(R.string.status_combo_style_one_out_signal_size_summary, outSignalSize), outSignalSize.toFloat(), 6f..20f, { outSignalSize = it.roundToInt(); preview(runtime.copy(outSignalSizeDp = outSignalSize)) }, { final -> persist(runtime.copy(outSignalSizeDp = final.roundToInt())) }, enabled = runtime.enabled && runtime.showMobile && runtime.signalMode == 1 && runtime.stackedSignal)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_signal_margin_left), stringResource(R.string.status_combo_style_one_out_signal_margin_left_summary, outSignalMarginLeft), outSignalMarginLeft.toFloat(), 0f..16f, { outSignalMarginLeft = it.roundToInt(); preview(runtime.copy(outSignalMarginLeftDp = outSignalMarginLeft)) }, { final -> persist(runtime.copy(outSignalMarginLeftDp = final.roundToInt())) }, enabled = runtime.enabled && runtime.showMobile && runtime.signalMode == 1 && runtime.stackedSignal)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_out_signal_margin), stringResource(R.string.status_combo_style_one_out_signal_margin_summary, outSignalMargin), outSignalMargin.toFloat(), 0f..16f, { outSignalMargin = it.roundToInt(); preview(runtime.copy(outSignalMarginDp = outSignalMargin)) }, { final -> persist(runtime.copy(outSignalMarginDp = final.roundToInt())) }, enabled = runtime.enabled && runtime.showMobile && runtime.signalMode == 1 && runtime.stackedSignal)
            }))
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_track), titleAlignedToContent = true, items = listOf({
                styleOneSlider(stringResource(R.string.status_combo_style_one_track_alpha), stringResource(R.string.status_combo_style_one_track_alpha_summary, trackAlpha), trackAlpha.toFloat(), 0f..100f, { trackAlpha = it.roundToInt(); val alpha = (trackAlpha * 255f / 100f).roundToInt(); preview(runtime.copy(trackAlpha = alpha)) }, { final -> persist(runtime.copy(trackAlpha = (final.roundToInt() * 255f / 100f).roundToInt())) }, enabled = runtime.enabled)
            }))
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_charging), titleAlignedToContent = true, items = listOf({
                styleOneSlider(stringResource(R.string.status_combo_style_one_bolt_size), stringResource(R.string.status_combo_style_one_bolt_size_summary, boltSize), boltSize.toFloat(), 50f..180f, { boltSize = it.roundToInt(); preview(runtime.copy(chargingBoltSizePercent = boltSize)) }, { final -> persist(runtime.copy(chargingBoltSizePercent = final.roundToInt())) }, enabled = runtime.enabled && runtime.showValue && runtime.showBolt)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_bolt_x), stringResource(R.string.status_combo_style_one_bolt_x_summary, boltX), boltX.toFloat(), -8f..8f, { boltX = it.roundToInt(); preview(runtime.copy(chargingBoltXDp = boltX)) }, { final -> persist(runtime.copy(chargingBoltXDp = final.roundToInt())) }, enabled = runtime.enabled && runtime.showValue && runtime.showBolt)
            }, {
                styleOneSlider(stringResource(R.string.status_combo_style_one_bolt_y), stringResource(R.string.status_combo_style_one_bolt_y_summary, boltY), boltY.toFloat(), -8f..8f, { boltY = it.roundToInt(); preview(runtime.copy(chargingBoltYDp = boltY)) }, { final -> persist(runtime.copy(chargingBoltYDp = final.roundToInt())) }, enabled = runtime.enabled && runtime.showValue && runtime.showBolt)
            }))
        }
        else -> {
            // Keep the entire state-colour section together:
            // toggle, thresholds, three role rows and reset live in one card.
            val colorItems = mutableListOf<@Composable () -> Unit>()

            colorItems += {
                SwitchItem(
                    icon = null,
                    title = stringResource(R.string.status_combo_style_one_role_colors),
                    summary = stringResource(R.string.status_combo_style_one_role_colors_summary),
                    checked = runtime.roleColors,
                    enabled = runtime.enabled,
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
                        { final -> persist(runtime.copy(lowThreshold = final.roundToInt())) },
                        enabled = runtime.enabled && runtime.roleColors,
                    )
                }
                colorItems += {
                    StyleOneRoleColorRow(
                        title = stringResource(StyleOneColorRole.Low.titleRes),
                        summary = stringResource(R.string.status_combo_style_one_color_summary),
                        darkColor = runtime.lowColorDarkArgb,
                        lightColor = runtime.lowColorLightArgb,
                        enabled = runtime.enabled && runtime.roleColors,
                        onClick = { colorPreviewDraft[0] = runtime; editingColorRole = StyleOneColorRole.Low },
                    )
                }
                colorItems += {
                    StyleOneRoleColorRow(
                        title = stringResource(StyleOneColorRole.Charging.titleRes),
                        summary = stringResource(R.string.status_combo_style_one_color_summary),
                        darkColor = runtime.chargingColorDarkArgb,
                        lightColor = runtime.chargingColorLightArgb,
                        enabled = runtime.enabled && runtime.roleColors,
                        onClick = { colorPreviewDraft[0] = runtime; editingColorRole = StyleOneColorRole.Charging },
                    )
                }
                colorItems += {
                    StyleOneRoleColorRow(
                        title = stringResource(StyleOneColorRole.Critical.titleRes),
                        summary = stringResource(R.string.status_combo_style_one_color_summary),
                        darkColor = runtime.criticalColorDarkArgb,
                        lightColor = runtime.criticalColorLightArgb,
                        enabled = runtime.enabled && runtime.roleColors,
                        onClick = { colorPreviewDraft[0] = runtime; editingColorRole = StyleOneColorRole.Critical },
                    )
                }
                colorItems += {
                    StyleOneRoleColorRow(
                        title = stringResource(StyleOneColorRole.PowerSaveFill.titleRes),
                        summary = stringResource(R.string.status_combo_style_one_power_save_fill_color_summary),
                        darkColor = runtime.powerSaveFillColorDarkArgb,
                        lightColor = runtime.powerSaveFillColorLightArgb,
                        enabled = runtime.enabled,
                        onClick = {
                            colorPreviewDraft[0] = runtime
                            editingColorRole = StyleOneColorRole.PowerSaveFill
                        },
                    )
                }
                colorItems += {
                    StatusThemeChildItem {
                        StyleOneInfoItem(
                            title = stringResource(R.string.status_combo_style_one_reset_colors),
                        summary = stringResource(R.string.status_combo_style_one_reset_colors_summary),
                        modifier = Modifier
                            .alpha(if (runtime.enabled) 1f else 0.45f)
                            .clickable(enabled = runtime.enabled) { confirmResetColors = true },
                        trailingContent = {
                            Text("›", style = Material3Theme.typography.titleLarge)
                        },
                        )
                    }
                }
            StatusThemeGroup(title = stringResource(R.string.status_combo_style_one_group_colors), titleAlignedToContent = true, items = colorItems)

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
                                powerSaveFillColorDarkArgb = 0xBFFFC107.toInt(),
                                powerSaveFillColorLightArgb = 0xBFFFC107.toInt(),
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
                    onColorPreview = { onDark, color -> previewColorDraft(role, onDark, color) },
                    onApply = { dark, light ->
                        val next = runtime.withRoleColor(role, true, dark)
                            .withRoleColor(role, false, light)
                        previewAndPersist(next)
                        colorPreviewDraft[0] = next
                        editingColorRole = null
                    },
                    onCancel = {
                        preview(runtime)
                        colorPreviewDraft[0] = runtime
                        editingColorRole = null
                    },
                    onDismiss = {
                        preview(runtime)
                        colorPreviewDraft[0] = runtime
                        editingColorRole = null
                    },
                )
            }
        }
    }

}

private enum class StyleOneColorRole(val titleRes: Int) {
    Low(R.string.status_combo_style_one_color_low),
    Charging(R.string.status_combo_style_one_color_charging),
    Critical(R.string.status_combo_style_one_color_critical),
    PowerSaveFill(R.string.status_combo_style_one_color_power_save_fill),
}

private fun Preference.SystemUI.StatusBarComboStyleOneRuntime.colorForRole(
    role: StyleOneColorRole,
    onDarkBackground: Boolean,
): Int = when (role) {
    StyleOneColorRole.Low -> if (onDarkBackground) lowColorDarkArgb else lowColorLightArgb
    StyleOneColorRole.Charging -> if (onDarkBackground) chargingColorDarkArgb else chargingColorLightArgb
    StyleOneColorRole.Critical -> if (onDarkBackground) criticalColorDarkArgb else criticalColorLightArgb
    StyleOneColorRole.PowerSaveFill ->
        if (onDarkBackground) powerSaveFillColorDarkArgb else powerSaveFillColorLightArgb
}

private fun Preference.SystemUI.StatusBarComboStyleOneRuntime.withRoleColor(
    role: StyleOneColorRole,
    onDarkBackground: Boolean,
    colorArgb: Int,
): Preference.SystemUI.StatusBarComboStyleOneRuntime = when (role) {
    StyleOneColorRole.Low -> if (onDarkBackground) copy(lowColorDarkArgb = colorArgb) else copy(lowColorLightArgb = colorArgb)
    StyleOneColorRole.Charging -> if (onDarkBackground) copy(chargingColorDarkArgb = colorArgb) else copy(chargingColorLightArgb = colorArgb)
    StyleOneColorRole.Critical -> if (onDarkBackground) copy(criticalColorDarkArgb = colorArgb) else copy(criticalColorLightArgb = colorArgb)
    StyleOneColorRole.PowerSaveFill ->
        if (onDarkBackground) copy(powerSaveFillColorDarkArgb = colorArgb) else copy(powerSaveFillColorLightArgb = colorArgb)
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
                MiuixButtonDefaults.buttonColorsPrimary()
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
    onColorPreview: (Boolean, Int) -> Unit,
    onApply: (Int, Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    var onDark by remember(roleTitle) { mutableStateOf(true) }
    var draftDarkColor by remember(roleTitle) { mutableIntStateOf(darkColorArgb) }
    var draftLightColor by remember(roleTitle) { mutableIntStateOf(lightColorArgb) }

    @Composable
    fun PickerContent() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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

            StyleOneColorPalette(
                colorArgb = if (onDark) draftDarkColor else draftLightColor,
                onColorChanged = { color ->
                    if (onDark) draftDarkColor = color else draftLightColor = color
                    onColorPreview(onDark, color)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OneuiXActionButton(
                    text = stringResource(R.string.status_combo_style_one_color_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                )
                OneuiXActionButton(
                    text = stringResource(R.string.status_combo_style_one_color_apply),
                    onClick = { onApply(draftDarkColor, draftLightColor) },
                    modifier = Modifier.weight(1f),
                    primary = true,
                )
            }
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
private fun StyleOneColorPalette(
    colorArgb: Int,
    onColorChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hsv = remember(colorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(colorArgb, it) }
    }
    val alpha = AndroidColor.alpha(colorArgb)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(colorArgb))
                .border(1.dp, Color(0x55000000), RoundedCornerShape(8.dp)),
        )
        StyleOneSaturationValuePicker(
            colorArgb = colorArgb,
            onColorChange = onColorChanged,
        )
        StyleOnePickerLabel(stringResource(R.string.status_combo_style_one_color_hue, hsv[0].roundToInt()))
        StyleOneHueSlider(
            hue = hsv[0],
            onHueChange = { hue ->
                onColorChanged(
                    AndroidColor.HSVToColor(
                        alpha,
                        floatArrayOf(hue.coerceIn(0f, 360f), hsv[1], hsv[2]),
                    ),
                )
            },
        )
        StyleOnePickerLabel(
            stringResource(
                R.string.status_combo_style_one_color_opacity,
                (alpha * 100f / 255f).roundToInt(),
            ),
        )
        StyleOneAlphaSlider(
            colorArgb = colorArgb,
            onAlphaChange = { nextAlpha ->
                onColorChanged(
                    AndroidColor.HSVToColor(
                        nextAlpha.coerceIn(0, 255),
                        floatArrayOf(hsv[0], hsv[1], hsv[2]),
                    ),
                )
            },
        )
    }
}

@Composable
private fun StyleOnePickerLabel(text: String) {
    when (LocalUiMode.current) {
        UiMode.Material -> Text(text = text, style = Material3Theme.typography.bodySmall)
        UiMode.Miuix -> MiuixText(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun StyleOneSaturationValuePicker(
    colorArgb: Int,
    onColorChange: (Int) -> Unit,
) {
    val hsv = remember(colorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(colorArgb, it) }
    }
    val hue = hsv[0]
    val alpha = AndroidColor.alpha(colorArgb)
    val saturation = hsv[1]
    val value = hsv[2]

    fun select(position: Offset, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val nextSaturation = (position.x / width).coerceIn(0f, 1f)
        val nextValue = (1f - position.y / height).coerceIn(0f, 1f)
        onColorChange(AndroidColor.HSVToColor(alpha, floatArrayOf(hue, nextSaturation, nextValue)))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(hue, alpha) {
                detectTapGestures { select(it, size.width.toFloat(), size.height.toFloat()) }
            }
            .pointerInput(hue, alpha) {
                detectDragGestures(
                    onDragStart = { select(it, size.width.toFloat(), size.height.toFloat()) },
                    onDrag = { change, _ ->
                        select(change.position, size.width.toFloat(), size.height.toFloat())
                        change.consume()
                    },
                )
            },
    ) {
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val marker = Offset(
            x = saturation.coerceIn(0f, 1f) * size.width,
            y = (1f - value.coerceIn(0f, 1f)) * size.height,
        )
        drawCircle(Color.Black.copy(alpha = 0.72f), 9.dp.toPx(), marker, style = Stroke(width = 3.dp.toPx()))
        drawCircle(Color.White, 8.dp.toPx(), marker, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun StyleOneHueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
) {
    val currentOnChange = rememberUpdatedState(onHueChange)
    val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                fun select(x: Float) {
                    if (size.width > 0) currentOnChange.value((x / size.width).coerceIn(0f, 1f) * 360f)
                }
                detectTapGestures { select(it.x) }
            }
            .pointerInput(Unit) {
                fun select(x: Float) {
                    if (size.width > 0) currentOnChange.value((x / size.width).coerceIn(0f, 1f) * 360f)
                }
                detectDragGestures(
                    onDragStart = { select(it.x) },
                    onDrag = { change, _ -> select(change.position.x); change.consume() },
                )
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(colors),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
        )
        val x = hue.coerceIn(0f, 360f) / 360f * size.width
        drawCircle(Color.Black.copy(alpha = 0.72f), 10.dp.toPx(), Offset(x, size.height / 2f))
        drawCircle(Color.White, 8.dp.toPx(), Offset(x, size.height / 2f))
    }
}

@Composable
private fun StyleOneAlphaSlider(
    colorArgb: Int,
    onAlphaChange: (Int) -> Unit,
) {
    val currentOnChange = rememberUpdatedState(onAlphaChange)
    val alpha = AndroidColor.alpha(colorArgb)
    val red = AndroidColor.red(colorArgb)
    val green = AndroidColor.green(colorArgb)
    val blue = AndroidColor.blue(colorArgb)
    val transparent = Color(red / 255f, green / 255f, blue / 255f, 0f)
    val opaque = Color(red / 255f, green / 255f, blue / 255f, 1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                fun select(x: Float) {
                    if (size.width > 0) currentOnChange.value((x / size.width).coerceIn(0f, 1f).times(255f).roundToInt())
                }
                detectTapGestures { select(it.x) }
            }
            .pointerInput(Unit) {
                fun select(x: Float) {
                    if (size.width > 0) currentOnChange.value((x / size.width).coerceIn(0f, 1f).times(255f).roundToInt())
                }
                detectDragGestures(
                    onDragStart = { select(it.x) },
                    onDrag = { change, _ -> select(change.position.x); change.consume() },
                )
            },
    ) {
        val cellSize = 8.dp.toPx()
        val columns = (size.width / cellSize).toInt() + 1
        val rows = (size.height / cellSize).toInt() + 1
        for (row in 0 until rows) for (column in 0 until columns) {
            drawRect(
                color = if ((row + column) % 2 == 0) Color(0xFFE0E0E0) else Color(0xFF9E9E9E),
                topLeft = Offset(column * cellSize, row * cellSize),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
            )
        }
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(transparent, opaque)),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
        )
        val x = alpha / 255f * size.width
        drawCircle(Color.Black.copy(alpha = 0.72f), 10.dp.toPx(), Offset(x, size.height / 2f))
        drawCircle(Color.White, 8.dp.toPx(), Offset(x, size.height / 2f))
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
    onFinished: (Float) -> Unit,
    stepSize: Float = 1f,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    var lastStep by remember(title, range.start, range.endInclusive) { mutableFloatStateOf(value) }
    val sliderContentPadding = when (LocalUiMode.current) {
        // SegmentedListItem already applies the native horizontal content inset.
        UiMode.Material -> Modifier
        // Miuix Card needs a modest inset without a duplicate oversized child margin.
        UiMode.Miuix -> Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    }
    StatusThemeChildItem {
        Column(
            modifier = Modifier.fillMaxWidth().then(sliderContentPadding).alpha(if (enabled) 1f else 0.45f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                when (LocalUiMode.current) {
                    UiMode.Material -> {
                        Text(text = title, style = Material3Theme.typography.titleSmall)
                        Text(text = summary, style = Material3Theme.typography.bodySmall, color = Material3Theme.colorScheme.onSurfaceVariant)
                    }
                    UiMode.Miuix -> {
                        MiuixText(text = title, color = MiuixTheme.colorScheme.onSurface)
                        MiuixText(text = summary, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
            OneuiXSlider(
                value = value,
                modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f),
                valueRange = range, steps = 0,
                stepSize = stepSize,
                hapticOnStep = false,
                enabled = enabled,
                onValueChange = { raw ->
                    if (enabled) {
                        val bounded = raw.coerceIn(range.start, range.endInclusive)
                        val next = (((bounded - range.start) / stepSize).roundToInt() * stepSize + range.start).coerceIn(range.start, range.endInclusive)
                        if (next != lastStep) { lastStep = next; view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
                        onValueChange(next)
                    }
                },
                onValueChangeFinished = { if (enabled) onFinished(lastStep) },
            )
        }
    }
}
