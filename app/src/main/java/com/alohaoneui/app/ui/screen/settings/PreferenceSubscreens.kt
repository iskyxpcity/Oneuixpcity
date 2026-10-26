package com.alohaoneui.app.ui.screen.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.util.AppLocaleHelper
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme as miuixColors

@Composable
fun LanguageSettingsScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel = viewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLanguage = state.appLanguage
    val options = listOf(stringResource(R.string.settings_language_system), stringResource(R.string.settings_language_english), stringResource(R.string.settings_language_chinese_simplified))
    val tags = listOf("system", "en", "zh-CN")
    val selected = tags.indexOf(selectedLanguage).coerceAtLeast(0)
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixScaffold(topBar = { MiuixTopAppBar(color = miuixColors.surface, title = stringResource(R.string.settings_language)) }, popupHost = {}) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
                MiuixCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    OverlayDropdownPreference(
                        title = stringResource(R.string.settings_language), summary = options[selected],
                        items = options, selectedIndex = selected,
                        onSelectedIndexChange = { index ->
                            val language = tags[index.coerceIn(tags.indices)]
                            viewModel.setAppLanguage(language)
                            AppLocaleHelper.apply(context, language)
                            activity?.recreate()
                        },
                    )
                }
            }
        }
    } else {
        MaterialSettingsScaffold(title = stringResource(R.string.settings_language), onBack = { navigator.pop() }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp)) {
                options.forEachIndexed { index, label ->
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.setAppLanguage(tags[index])
                            AppLocaleHelper.apply(context, tags[index])
                            activity?.recreate()
                        },
                        headlineContent = { Text(label) },
                        trailingContent = { RadioButton(selected = selected == index, onClick = {
                            viewModel.setAppLanguage(tags[index])
                            AppLocaleHelper.apply(context, tags[index])
                            activity?.recreate()
                        }) },
                    )
                }
            }
        }
    }
}

@Composable
fun DisplayTextSettingsScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fontScaleOptions = (80..150 step 5).map { "$it%" }
    val fontScaleValues = (80..150 step 5).map { it / 100f }
    val displayScaleOptions = (85..125 step 5).map { "$it%" }
    val displayScaleValues = (85..125 step 5).map { it / 100f }
    val fontScaleIndex = fontScaleValues.indices.minByOrNull { kotlin.math.abs(fontScaleValues[it] - state.materialFontScale) } ?: 4
    val displayScaleIndex = displayScaleValues.indices.minByOrNull { kotlin.math.abs(displayScaleValues[it] - state.materialDisplayScale) } ?: 3
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixScaffold(topBar = { MiuixTopAppBar(color = miuixColors.surface, title = stringResource(R.string.settings_display_text)) }, popupHost = {}) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
                MiuixCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    OverlayDropdownPreference(
                        title = stringResource(R.string.settings_font_scale_title),
                        items = fontScaleOptions,
                        selectedIndex = fontScaleIndex,
                        onSelectedIndexChange = { viewModel.setMaterialFontScale(fontScaleValues[it.coerceIn(fontScaleValues.indices)]) },
                    )
                    OverlayDropdownPreference(
                        title = stringResource(R.string.settings_display_scale_title),
                        items = displayScaleOptions,
                        selectedIndex = displayScaleIndex,
                        onSelectedIndexChange = { viewModel.setMaterialDisplayScale(displayScaleValues[it.coerceIn(displayScaleValues.indices)]) },
                    )
                    MiuixText(text = stringResource(R.string.settings_display_preview_sample), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
                    OverlayDropdownPreference(title = stringResource(R.string.settings_font_family_title), items = listOf(stringResource(R.string.settings_font_family_default), stringResource(R.string.settings_font_family_serif)), selectedIndex = if (state.materialFontFamily == "Serif") 1 else 0, onSelectedIndexChange = { viewModel.setMaterialFontFamily(if (it == 1) "Serif" else "Default") })
                    SwitchPreference(title = stringResource(R.string.settings_large_text_title), summary = stringResource(R.string.settings_large_text_subtitle), checked = state.materialLargeTextEnabled, onCheckedChange = viewModel::setMaterialLargeTextEnabled)
                    SwitchPreference(title = stringResource(R.string.settings_bold_text_title), checked = state.materialBoldTextEnabled, onCheckedChange = viewModel::setMaterialBoldTextEnabled)
                }
            }
        }
    } else {
        MaterialSettingsScaffold(title = stringResource(R.string.settings_display_text), onBack = { navigator.pop() }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_font_scale_title), modifier = Modifier.weight(1f))
                    Text("${(state.materialFontScale * 100).toInt()}%")
                }
                Slider(state.materialFontScale, viewModel::setMaterialFontScale, valueRange = 0.8f..1.5f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_display_scale_title), modifier = Modifier.weight(1f))
                    Text("${(state.materialDisplayScale * 100).toInt()}%")
                }
                Slider(state.materialDisplayScale, viewModel::setMaterialDisplayScale, valueRange = 0.85f..1.25f)
                Text(stringResource(R.string.settings_display_preview_sample), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                var fontExpanded by remember { mutableStateOf(false) }
                ListItem(modifier = Modifier.clickable { fontExpanded = true }, headlineContent = { Text(stringResource(R.string.settings_font_family_title)) }, supportingContent = { Text(if (state.materialFontFamily == "Serif") stringResource(R.string.settings_font_family_serif) else stringResource(R.string.settings_font_family_default)) })
                DropdownMenu(fontExpanded, { fontExpanded = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.settings_font_family_default)) }, onClick = { viewModel.setMaterialFontFamily("Default"); fontExpanded = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.settings_font_family_serif)) }, onClick = { viewModel.setMaterialFontFamily("Serif"); fontExpanded = false })
                }
                ListItem(headlineContent = { Text(stringResource(R.string.settings_large_text_title)) }, supportingContent = { Text(stringResource(R.string.settings_large_text_subtitle)) }, trailingContent = { Switch(state.materialLargeTextEnabled, viewModel::setMaterialLargeTextEnabled) })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_bold_text_title)) }, trailingContent = { Switch(state.materialBoldTextEnabled, viewModel::setMaterialBoldTextEnabled) })
            }
        }
    }
}

@Composable
fun MotionHapticsSettingsScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val motionOptions = listOf(stringResource(R.string.settings_motion_expressive), stringResource(R.string.settings_motion_standard))
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixScaffold(topBar = { MiuixTopAppBar(color = miuixColors.surface, title = stringResource(R.string.settings_motion_haptics)) }, popupHost = {}) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
                MiuixCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    OverlayDropdownPreference(title = stringResource(R.string.settings_motion_style), items = motionOptions, selectedIndex = state.materialNavigationMotionMode.coerceIn(0, 1), onSelectedIndexChange = { viewModel.setMaterialNavigationMotionMode(it.coerceIn(0, 1)) })
                    SwitchPreference(title = stringResource(R.string.settings_haptic_feedback), checked = state.enableHapticFeedback, onCheckedChange = viewModel::setEnableHapticFeedback)
                    SwitchPreference(title = stringResource(R.string.settings_enable_predictive_back), summary = stringResource(R.string.settings_enable_predictive_back_summary), checked = state.enablePredictiveBack, onCheckedChange = viewModel::setEnablePredictiveBack)
                    SwitchPreference(title = stringResource(R.string.settings_enable_swipe_back), summary = stringResource(R.string.settings_enable_swipe_back_summary), checked = state.enableSwipeBack, onCheckedChange = viewModel::setEnableSwipeBack)
                    OverlayDropdownPreference(title = stringResource(R.string.settings_page_gesture_mode), items = listOf(stringResource(R.string.settings_page_gesture_default), stringResource(R.string.settings_page_gesture_cross_axis), stringResource(R.string.settings_page_gesture_ios_like)), selectedIndex = state.pagerGestureMode.coerceIn(0, 2), onSelectedIndexChange = { viewModel.setPagerGestureMode(it.coerceIn(0, 2)) })
                    OverlayDropdownPreference(title = stringResource(R.string.settings_page_scale), items = listOf("80%", "90%", "100%", "110%", "120%"), selectedIndex = ((state.pageScale * 100).toInt() / 10 - 8).coerceIn(0, 4), onSelectedIndexChange = { viewModel.setPageScale((80 + it * 10) / 100f) })
                }
            }
        }
    } else {
        MaterialSettingsScaffold(title = stringResource(R.string.settings_motion_haptics), onBack = { navigator.pop() }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(R.string.settings_motion_expressive, state.materialNavigationMotionMode == 0) { viewModel.setMaterialNavigationMotionMode(0) }
                Choice(R.string.settings_motion_standard, state.materialNavigationMotionMode == 1) { viewModel.setMaterialNavigationMotionMode(1) }
                ListItem(headlineContent = { Text(stringResource(R.string.settings_haptic_feedback)) }, trailingContent = { Switch(state.enableHapticFeedback, viewModel::setEnableHapticFeedback) })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_enable_predictive_back)) }, supportingContent = { Text(stringResource(R.string.settings_enable_predictive_back_summary)) }, trailingContent = { Switch(state.enablePredictiveBack, viewModel::setEnablePredictiveBack) })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_enable_swipe_back)) }, supportingContent = { Text(stringResource(R.string.settings_enable_swipe_back_summary)) }, trailingContent = { Switch(state.enableSwipeBack, viewModel::setEnableSwipeBack) })
                var gestureExpanded by remember { mutableStateOf(false) }
                val gestureLabels = listOf(stringResource(R.string.settings_page_gesture_default), stringResource(R.string.settings_page_gesture_cross_axis), stringResource(R.string.settings_page_gesture_ios_like))
                ListItem(modifier = Modifier.clickable { gestureExpanded = true }, headlineContent = { Text(stringResource(R.string.settings_page_gesture_mode)) }, supportingContent = { Text(gestureLabels[state.pagerGestureMode.coerceIn(0, 2)]) })
                DropdownMenu(gestureExpanded, { gestureExpanded = false }) { gestureLabels.forEachIndexed { index, label -> DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setPagerGestureMode(index); gestureExpanded = false }) } }
                var pageScaleExpanded by remember { mutableStateOf(false) }
                ListItem(modifier = Modifier.clickable { pageScaleExpanded = true }, headlineContent = { Text(stringResource(R.string.settings_page_scale)) }, supportingContent = { Text((state.pageScale * 100).toInt().toString() + "%") })
                DropdownMenu(pageScaleExpanded, { pageScaleExpanded = false }) { listOf(80, 90, 100, 110, 120).forEach { percent -> DropdownMenuItem(text = { Text(percent.toString() + "%") }, onClick = { viewModel.setPageScale(percent / 100f); pageScaleExpanded = false }) } }
            }
        }
    }
}

@Composable
private fun MaterialSettingsScaffold(title: String, onBack: () -> Unit, content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit) {
    Scaffold(topBar = { TopAppBar(navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }, title = { Text(title) }) }, content = content)
}

@Composable
private fun Choice(id: Int, selected: Boolean, onClick: () -> Unit) {
    ListItem(modifier = Modifier.clickable(onClick = onClick), headlineContent = { Text(stringResource(id)) }, trailingContent = { RadioButton(selected, onClick) })
}
