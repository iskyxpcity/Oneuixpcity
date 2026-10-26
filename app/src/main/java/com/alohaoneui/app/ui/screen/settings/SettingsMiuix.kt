package com.alohaoneui.app.ui.screen.settings

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.AlohaOneUIApplication
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.dialog.rememberLoadingDialog
import com.alohaoneui.app.ui.component.miuix.SendLogDialog
import com.alohaoneui.app.ui.theme.keyColorOptions
import com.materialkolor.PaletteStyle
import com.alohaoneui.app.ui.theme.LocalEnableBlur
import com.alohaoneui.app.ui.util.AppLocaleHelper
import com.alohaoneui.app.ui.util.BlurredBar
import com.alohaoneui.app.ui.util.rememberBlurBackdrop
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingPagerMiuix(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val viewModel = viewModel<SettingsViewModel>()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    val loadingDialog = rememberLoadingDialog()
    val showSendLogDialog = rememberSaveable { mutableStateOf(false) }
    var languageExpanded by rememberSaveable { mutableStateOf(false) }
    var themeExpanded by rememberSaveable { mutableStateOf(false) }
    var colorExpanded by rememberSaveable { mutableStateOf(false) }
    var displayExpanded by rememberSaveable { mutableStateOf(false) }
    var behaviorExpanded by rememberSaveable { mutableStateOf(false) }
    var alohaExpanded by rememberSaveable { mutableStateOf(false) }

    val languageLabels = listOf(
        stringResource(R.string.settings_language_system),
        stringResource(R.string.settings_language_english),
        stringResource(R.string.settings_language_chinese_simplified),
    )
    val languageTags = listOf("system", "en", "zh-CN")
    val fontLabels = listOf(stringResource(R.string.voyage_font_default), stringResource(R.string.voyage_font_serif))
    val gestureLabels = listOf(
        stringResource(R.string.settings_page_gesture_default),
        stringResource(R.string.settings_page_gesture_cross_axis),
        stringResource(R.string.settings_page_gesture_ios_like),
    )
    val motionLabels = listOf(stringResource(R.string.settings_motion_expressive), stringResource(R.string.settings_motion_standard))

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(color = barColor, title = stringResource(R.string.settings), scrollBehavior = scrollBehavior)
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight().scrollEndHaptic().overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection).padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    MiuixSettingsGroup(stringResource(R.string.settings_group_language), Icons.Rounded.Translate, languageExpanded, { languageExpanded = !languageExpanded }) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_language),
                            summary = stringResource(R.string.settings_language_summary),
                            items = languageLabels,
                            selectedIndex = languageTags.indexOf(uiState.appLanguage).coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                val language = languageTags[index]
                                viewModel.setAppLanguage(language)
                                AppLocaleHelper.apply(context, language)
                                activity?.recreate()
                            },
                        )
                    }
                    MiuixSettingsGroup(stringResource(R.string.settings_theme), Icons.Rounded.Palette, themeExpanded, { themeExpanded = !themeExpanded }) {
                        val uiModeLabels = listOf(stringResource(R.string.theme_design_material3), stringResource(R.string.theme_design_miuix))
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_ui_mode),
                            summary = stringResource(R.string.settings_ui_mode_summary),
                            items = uiModeLabels,
                            selectedIndex = if (uiState.uiMode == UiMode.Material.value) 0 else 1,
                            onSelectedIndexChange = { actions.onSetUiModeIndex(if (it == 0) 1 else 0) },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.voyage_theme_page_title),
                            summary = stringResource(R.string.settings_theme_summary),
                            startAction = { Icon(Icons.Rounded.Palette, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) },
                            onClick = actions.onOpenTheme,
                        )
                    }
                    MiuixSettingsGroup(stringResource(R.string.voyage_settings_group_color), Icons.Rounded.Palette, colorExpanded, { colorExpanded = !colorExpanded }) {
                        SwitchPreference(
                            title = stringResource(R.string.voyage_theme_dynamic_color),
                            summary = stringResource(R.string.voyage_theme_dynamic_color_summary),
                            checked = uiState.materialDynamicColorEnabled,
                            onCheckedChange = viewModel::setMaterialDynamicColorEnabled,
                        )
                        Text(stringResource(R.string.voyage_theme_key_color), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            MiuixSettingsColorChoice(Color(0xFF6750A4), uiState.keyColor == 0, stringResource(R.string.voyage_theme_system_color)) { viewModel.setKeyColor(0) }
                            keyColorOptions.forEach { color ->
                                MiuixSettingsColorChoice(Color(color), uiState.keyColor == color, null) { viewModel.setKeyColor(color) }
                            }
                        }
                        val paletteStyles = PaletteStyle.entries.map { it.name }
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_color_style),
                            items = paletteStyles,
                            selectedIndex = paletteStyles.indexOf(uiState.colorStyle).coerceAtLeast(0),
                            onSelectedIndexChange = { viewModel.setColorStyle(paletteStyles[it]) },
                        )
                        Text(stringResource(R.string.voyage_theme_contrast) + " · " + String.format("%.1f", uiState.materialContrastLevel), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        MiuixSlider(value = uiState.materialContrastLevel.coerceIn(-1f, 1f), onValueChange = viewModel::setMaterialContrastLevel, valueRange = -1f..1f, showKeyPoints = true, keyPoints = listOf(-1f, -0.5f, 0f, 0.5f, 1f), hapticEffect = SliderDefaults.SliderHapticEffect.Step)
                        ArrowPreference(
                            title = stringResource(R.string.voyage_theme_color_editor),
                            startAction = { Icon(Icons.Rounded.Palette, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) },
                            onClick = actions.onOpenColorPalette,
                        )
                    }
                    MiuixSettingsGroup(stringResource(R.string.settings_group_display), Icons.Rounded.TextFields, displayExpanded, { displayExpanded = !displayExpanded }) {
                        Text(stringResource(R.string.settings_font_scale_title) + " · " + (uiState.materialFontScale * 100).toInt().toString() + "%", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        MiuixSlider(value = uiState.materialFontScale, onValueChange = viewModel::setMaterialFontScale, valueRange = 0.8f..1.5f, showKeyPoints = true, keyPoints = listOf(0.8f, 1.0f, 1.2f, 1.5f), hapticEffect = SliderDefaults.SliderHapticEffect.Step)
                        Text(stringResource(R.string.settings_display_scale_title) + " · " + (uiState.materialDisplayScale * 100).toInt().toString() + "%", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        MiuixSlider(value = uiState.materialDisplayScale, onValueChange = viewModel::setMaterialDisplayScale, valueRange = 0.85f..1.25f, showKeyPoints = true, keyPoints = listOf(0.85f, 0.95f, 1.05f, 1.15f, 1.25f), hapticEffect = SliderDefaults.SliderHapticEffect.Step)
                        Text(stringResource(R.string.settings_display_preview_sample), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp))
                        OverlayDropdownPreference(title = stringResource(R.string.voyage_theme_font_family), items = fontLabels, selectedIndex = if (uiState.materialFontFamily == "Serif") 1 else 0, onSelectedIndexChange = { viewModel.setMaterialFontFamily(if (it == 1) "Serif" else "Default") })
                        SwitchPreference(title = stringResource(R.string.voyage_theme_large_text), checked = uiState.materialLargeTextEnabled, onCheckedChange = viewModel::setMaterialLargeTextEnabled)
                        SwitchPreference(title = stringResource(R.string.settings_bold_text_title), checked = uiState.materialBoldTextEnabled, onCheckedChange = viewModel::setMaterialBoldTextEnabled)
                    }
                    MiuixSettingsGroup(stringResource(R.string.settings_group_behavior), Icons.Rounded.Animation, behaviorExpanded, { behaviorExpanded = !behaviorExpanded }) {
                        OverlayDropdownPreference(title = stringResource(R.string.settings_motion_haptics), items = motionLabels, selectedIndex = uiState.materialNavigationMotionMode.coerceIn(0, 1), onSelectedIndexChange = viewModel::setMaterialNavigationMotionMode)
                        SwitchPreference(title = stringResource(R.string.settings_haptic_feedback), checked = uiState.enableHapticFeedback, onCheckedChange = viewModel::setEnableHapticFeedback)
                        SwitchPreference(title = stringResource(R.string.settings_enable_predictive_back), summary = stringResource(R.string.settings_enable_predictive_back_summary), checked = uiState.enablePredictiveBack, onCheckedChange = { enabled ->
                            viewModel.setEnablePredictiveBack(enabled)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                AlohaOneUIApplication.applyEnableOnBackInvokedCallback(context, enabled)
                                activity?.recreate()
                            }
                        })
                        SwitchPreference(title = stringResource(R.string.settings_enable_swipe_back), summary = stringResource(R.string.settings_enable_swipe_back_summary), checked = uiState.enableSwipeBack, onCheckedChange = viewModel::setEnableSwipeBack)
                        OverlayDropdownPreference(title = stringResource(R.string.settings_page_gesture_mode), items = gestureLabels, selectedIndex = uiState.pagerGestureMode.coerceIn(0, 2), onSelectedIndexChange = viewModel::setPagerGestureMode)
                        val pageScaleLabels = listOf("80%", "90%", "100%", "110%", "120%")
                        OverlayDropdownPreference(title = stringResource(R.string.settings_page_scale), items = pageScaleLabels, selectedIndex = ((uiState.pageScale * 100).toInt() / 10 - 8).coerceIn(0, 4), onSelectedIndexChange = { viewModel.setPageScale((80 + it * 10) / 100f) })
                    }
                    MiuixSettingsGroup(stringResource(R.string.voyage_settings_group_aloha), Icons.Rounded.Dashboard, alohaExpanded, { alohaExpanded = !alohaExpanded }) {
                        OverlayDropdownPreference(title = stringResource(R.string.settings_ui_mode), summary = stringResource(R.string.settings_ui_mode_summary), items = UiMode.entries.map { it.name }, selectedIndex = if (uiState.uiMode == UiMode.Material.value) 1 else 0, onSelectedIndexChange = actions.onSetUiModeIndex)
                        SwitchPreference(title = stringResource(R.string.settings_check_update), summary = stringResource(R.string.settings_check_update_summary), startAction = { Icon(Icons.Rounded.SystemUpdate, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }, checked = uiState.checkUpdate, onCheckedChange = actions.onSetCheckUpdate)
                        ArrowPreference(title = stringResource(R.string.send_log), startAction = { Icon(Icons.Rounded.BugReport, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }, onClick = { showSendLogDialog.value = true })
                        ArrowPreference(title = stringResource(R.string.about), startAction = { Icon(Icons.Rounded.Info, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }, onClick = actions.onOpenAbout)
                        SendLogDialog(show = showSendLogDialog.value, onDismissRequest = { showSendLogDialog.value = false }, loadingDialog = loadingDialog)
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun MiuixSettingsGroup(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, modifier = Modifier.padding(end = 10.dp), contentDescription = null, tint = colorScheme.onBackground)
            Text(title, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = colorScheme.onBackground)
        }
        AnimatedVisibility(visible = expanded) {
            Column(content = content)
        }
    }
}


@Composable
private fun MiuixSettingsColorChoice(color: Color, selected: Boolean, label: String?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(if (selected) 40.dp else 34.dp)
                .clip(CircleShape)
                .background(color)
                .border(if (selected) 3.dp else 0.dp, if (selected) colorScheme.primary else Color.Transparent, CircleShape)
                .clickable(onClick = onClick),
        )
        if (label != null) Text(label)
    }
}
