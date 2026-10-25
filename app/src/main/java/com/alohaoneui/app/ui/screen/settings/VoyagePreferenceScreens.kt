package com.alohaoneui.app.ui.screen.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel

private data class LanguageChoice(val tag: String, val labelRes: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val choices = listOf(
        LanguageChoice("", R.string.settings_language_system),
        LanguageChoice("en", R.string.settings_language_english),
        LanguageChoice("zh-CN", R.string.settings_language_chinese),
    )
    val currentTag = if (Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
    } else ""
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_group_language)) },
            navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_language_summary), style = MaterialTheme.typography.bodyMedium)
            choices.forEach { choice ->
                val isSelected = choice.tag.equals(currentTag, ignoreCase = true)
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(stringResource(choice.labelRes)) },
                    trailingContent = if (isSelected) {{ Text("✓", color = MaterialTheme.colorScheme.primary) }} else null,
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (Build.VERSION.SDK_INT >= 33) {
                            context.getSystemService(LocaleManager::class.java).applicationLocales =
                                if (choice.tag.isBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(choice.tag)
                        }
                    },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer),
                    tonalElevation = 0.dp,
                )
            }
            if (Build.VERSION.SDK_INT < 33) Text(stringResource(R.string.settings_language_unsupported), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.settings_language_restart_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayTextSettingsScreen() {
    val navigator = LocalNavigator.current
    val vm = viewModel<SettingsViewModel>()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var fontMenu by remember { mutableStateOf(false) }
    val fontFamilies = listOf("Default", "SansSerif", "Serif", "Monospace", "Cursive")
    val fontLabels = listOf(
        stringResource(R.string.settings_font_family_default),
        stringResource(R.string.settings_font_family_sans_serif),
        stringResource(R.string.settings_font_family_serif),
        stringResource(R.string.settings_font_family_monospace),
        stringResource(R.string.settings_font_family_cursive),
    )
    val selectedFont = fontFamilies.indexOf(state.materialFontFamily).coerceAtLeast(0)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_display_title)) },
            navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_large_text_title))
                    Text(stringResource(R.string.settings_large_text_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = state.materialLargeTextEnabled, onCheckedChange = vm::setMaterialLargeTextEnabled)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_bold_text_title), modifier = Modifier.weight(1f))
                Switch(checked = state.materialBoldTextEnabled, onCheckedChange = vm::setMaterialBoldTextEnabled)
            }
            Text(stringResource(R.string.settings_font_family_title), style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.Box {
                OutlinedTextField(
                    value = fontLabels[selectedFont],
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_font_family_title)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.foundation.layout.Box(Modifier.matchParentSize()) {
                    Spacer(Modifier.fillMaxSize().clickable { fontMenu = true })
                }
                DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                    fontFamilies.forEachIndexed { index, family ->
                        DropdownMenuItem(text = { Text(fontLabels[index]) }, onClick = {
                            vm.setMaterialFontFamily(family)
                            fontMenu = false
                        })
                    }
                }
            }
            Text(stringResource(R.string.settings_font_scale_title) + " · " + "%.0f%%".format(state.materialFontScale * 100), style = MaterialTheme.typography.titleMedium)
            Slider(value = state.materialFontScale, onValueChange = vm::setMaterialFontScale, valueRange = 0.8f..1.5f)
            Text(stringResource(R.string.settings_display_scale_title) + " · " + "%.0f%%".format(state.pageScale * 100), style = MaterialTheme.typography.titleMedium)
            Slider(value = state.pageScale, onValueChange = vm::setPageScale, valueRange = 0.8f..1.3f)
            Text(stringResource(R.string.settings_display_preview_sample), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionHapticsSettingsScreen() {
    val navigator = LocalNavigator.current
    val vm = viewModel<SettingsViewModel>()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_behavior_title)) },
            navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.settings_navigation_motion_title), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_navigation_motion_expressive), modifier = Modifier.weight(1f))
                Switch(checked = state.materialNavigationMotionMode == 0, onCheckedChange = { vm.setMaterialNavigationMotionMode(if (it) 0 else 1) })
                Text(stringResource(R.string.settings_navigation_motion_reduced))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_predictive_back_title))
                    Text(stringResource(R.string.settings_predictive_back_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = state.enablePredictiveBack, onCheckedChange = vm::setEnablePredictiveBack)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_enable_swipe_back))
                    Text(stringResource(R.string.settings_enable_swipe_back_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = state.enableSwipeBack, onCheckedChange = vm::setEnableSwipeBack)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_haptic_feedback_title))
                    Text(stringResource(R.string.settings_haptic_feedback_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = state.enableHapticFeedback, onCheckedChange = { enabled ->
                    vm.setEnableHapticFeedback(enabled)
                    view.isHapticFeedbackEnabled = enabled
                    (context as? Activity)?.window?.decorView?.isHapticFeedbackEnabled = enabled
                })
            }
        }
    }
}
