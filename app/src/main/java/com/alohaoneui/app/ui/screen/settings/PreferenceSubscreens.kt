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
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme as miuixColors

@Composable
fun LanguageSettingsScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel = viewModel<SettingsViewModel>()
    var selectedLanguage by rememberSaveable { mutableStateOf(AppLocaleHelper.getLanguage(context)) }
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
                            selectedLanguage = tags[index.coerceIn(tags.indices)]
                            AppLocaleHelper.apply(context, selectedLanguage)
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
                            selectedLanguage = tags[index]
                            AppLocaleHelper.apply(context, selectedLanguage)
                            activity?.recreate()
                        },
                        headlineContent = { Text(label) },
                        trailingContent = { RadioButton(selected = selected == index, onClick = {
                            selectedLanguage = tags[index]
                            AppLocaleHelper.apply(context, selectedLanguage)
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
    val scaleOptions = (80..150 step 5).map { "$it%" }
    val scaleValues = (80..150 step 5).map { it / 100f }
    val selected = scaleValues.indices.minByOrNull { kotlin.math.abs(scaleValues[it] - state.materialFontScale) } ?: 4
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixScaffold(topBar = { MiuixTopAppBar(color = miuixColors.surface, title = stringResource(R.string.settings_display_text)) }, popupHost = {}) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
                MiuixCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    OverlayDropdownPreference(
                        title = stringResource(R.string.settings_font_scale_title),
                        items = scaleOptions,
                        selectedIndex = selected,
                        onSelectedIndexChange = { viewModel.setMaterialFontScale(scaleValues[it.coerceIn(scaleValues.indices)]) },
                    )
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
                Text(stringResource(R.string.settings_display_preview_sample), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
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
                }
            }
        }
    } else {
        MaterialSettingsScaffold(title = stringResource(R.string.settings_motion_haptics), onBack = { navigator.pop() }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice(R.string.settings_motion_expressive, state.materialNavigationMotionMode == 0) { viewModel.setMaterialNavigationMotionMode(0) }
                Choice(R.string.settings_motion_standard, state.materialNavigationMotionMode == 1) { viewModel.setMaterialNavigationMotionMode(1) }
                ListItem(headlineContent = { Text(stringResource(R.string.settings_haptic_feedback)) }, trailingContent = { Switch(state.enableHapticFeedback, viewModel::setEnableHapticFeedback) })
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
