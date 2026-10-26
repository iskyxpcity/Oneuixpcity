package com.alohaoneui.app.ui.screen.colorpalette

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.screen.settings.SettingsUiState
import com.alohaoneui.app.ui.theme.ColorMode
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel

@Composable
fun VoyageThemeSettingsScreen() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VoyageThemeSettingsScreenMaterial(uiState, viewModel) {
        if (navigator.backStackSize() > 1) navigator.pop()
    }
}

@Composable
fun VoyageThemeSettingsScreenMaterial(uiState: SettingsUiState, viewModel: SettingsViewModel, onBack: () -> Unit) {
    var expandedCategories by rememberSaveable { mutableStateOf(emptyList<String>()) }
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                title = { Text(stringResource(R.string.settings_theme)) },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VoyageThemeCategory.entries.forEach { category ->
                val expanded = category.name in expandedCategories
                VoyageThemeAccordion(
                    title = stringResource(category.titleRes),
                    icon = when (category) {
                        VoyageThemeCategory.DESIGN -> Icons.Filled.Palette
                        VoyageThemeCategory.NAVIGATION_BAR -> Icons.Filled.Navigation
                        VoyageThemeCategory.MODE -> Icons.Filled.DarkMode
                        VoyageThemeCategory.LAUNCH -> Icons.Filled.Launch
                        VoyageThemeCategory.ASSIST -> Icons.Filled.AccessibilityNew
                        VoyageThemeCategory.SHAPE -> Icons.Filled.Tune
                        VoyageThemeCategory.COMPONENTS -> Icons.Filled.Widgets
                        VoyageThemeCategory.SHADOWS -> Icons.Filled.Tune
                    },
                    expanded = expanded,
                    onToggle = { expandedCategories = if (expanded) expandedCategories - category.name else expandedCategories + category.name },
                ) {
                    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        when (category) {
                            VoyageThemeCategory.DESIGN -> {
                                ThemeStyleChoice(stringResource(R.string.theme_design_material3), stringResource(R.string.theme_design_material3_subtitle), uiState.uiMode == UiMode.Material.value, Icons.Filled.Palette) { viewModel.setUiMode(UiMode.Material.value) }
                                ThemeStyleChoice(stringResource(R.string.theme_design_miuix), stringResource(R.string.theme_design_miuix_subtitle), uiState.uiMode == UiMode.Miuix.value, Icons.Filled.Widgets) { viewModel.setUiMode(UiMode.Miuix.value) }
                            }
                            VoyageThemeCategory.NAVIGATION_BAR -> {
                                Toggle(R.string.voyage_theme_floating_nav_bar_title, R.string.voyage_theme_floating_nav_bar_subtitle, uiState.enableFloatingBottomBar, viewModel::setEnableFloatingBottomBar)
                                val liquidSummary = when {
                                    Build.VERSION.SDK_INT < 33 -> R.string.voyage_theme_liquid_glass_unsupported
                                    !uiState.enableFloatingBottomBar -> R.string.voyage_theme_liquid_glass_needs_floating
                                    else -> R.string.voyage_theme_liquid_glass_subtitle
                                }
                                Toggle(R.string.voyage_theme_liquid_glass_title, liquidSummary, uiState.enableFloatingBottomBarBlur, viewModel::setEnableFloatingBottomBarBlur, enabled = Build.VERSION.SDK_INT >= 33 && uiState.enableFloatingBottomBar)
                            }
                            VoyageThemeCategory.MODE -> {
                                ThemeStyleChoice(stringResource(R.string.settings_theme_mode_system), "", ColorMode.fromValue(uiState.themeMode).toNonMonetMode() == ColorMode.SYSTEM.value, Icons.Filled.Brightness4) { viewModel.setColorMode(ColorMode.SYSTEM) }
                                ThemeStyleChoice(stringResource(R.string.settings_theme_mode_light), "", ColorMode.fromValue(uiState.themeMode).toNonMonetMode() == ColorMode.LIGHT.value, Icons.Filled.Brightness7) { viewModel.setColorMode(ColorMode.LIGHT) }
                                ThemeStyleChoice(stringResource(R.string.settings_theme_mode_dark), "", ColorMode.fromValue(uiState.themeMode).toNonMonetMode() == ColorMode.DARK.value, Icons.Filled.Brightness3) { viewModel.setColorMode(ColorMode.DARK) }
                                Toggle(R.string.voyage_theme_pure_black, R.string.voyage_theme_pure_black_summary, uiState.themeMode == ColorMode.DARK_AMOLED.value) { enabled -> viewModel.setColorMode(if (enabled) ColorMode.DARK_AMOLED else ColorMode.DARK) }
                            }
                            VoyageThemeCategory.LAUNCH -> Toggle(R.string.voyage_theme_launch_follows, R.string.voyage_theme_launch_follows_summary, uiState.materialLaunchFollowsTheme, viewModel::setMaterialLaunchFollowsTheme)
                            VoyageThemeCategory.ASSIST -> {
                                Toggle(R.string.voyage_theme_invert, null, uiState.materialInvertColors, viewModel::setMaterialInvertColors)
                                val labels = listOf(
                                    stringResource(R.string.voyage_color_blind_off),
                                    stringResource(R.string.voyage_color_blind_protanomaly),
                                    stringResource(R.string.voyage_color_blind_deuteranomaly),
                                    stringResource(R.string.voyage_color_blind_tritanomaly),
                                    stringResource(R.string.voyage_color_blind_protanopia),
                                    stringResource(R.string.voyage_color_blind_deuteranopia),
                                    stringResource(R.string.voyage_color_blind_tritanopia),
                                    stringResource(R.string.voyage_color_blind_achromatomaly),
                                    stringResource(R.string.voyage_color_blind_achromatopsia),
                                )
                                Drop(R.string.voyage_theme_color_vision, labels[uiState.materialColorVisionMode.coerceIn(0, labels.lastIndex)], labels) { viewModel.setMaterialColorVisionMode(labels.indexOf(it).coerceAtLeast(0)) }
                            }
                            VoyageThemeCategory.SHAPE -> {
                                val labels = listOf(
                                    stringResource(R.string.voyage_shape_rounded), stringResource(R.string.voyage_shape_cut),
                                    stringResource(R.string.voyage_shape_squircle), stringResource(R.string.voyage_shape_smooth),
                                    stringResource(R.string.voyage_shape_wavy), stringResource(R.string.voyage_shape_scoop),
                                    stringResource(R.string.voyage_shape_notch),
                                )
                                val materialEnabled = uiState.uiMode == UiMode.Material.value
                                if (!materialEnabled) Text(stringResource(R.string.theme_design_overridden), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Drop(R.string.voyage_theme_shape_style, labels[uiState.materialShapeStyle.coerceIn(0, labels.lastIndex)], labels, enabled = materialEnabled) { viewModel.setMaterialShapeStyle(labels.indexOf(it).coerceAtLeast(0)) }
                                Slide(R.string.voyage_theme_corner_scale, uiState.materialCornerScale, 0f..2f, viewModel::setMaterialCornerScale, enabled = materialEnabled)
                            }
                            VoyageThemeCategory.COMPONENTS -> {
                                val switches = listOf(
                                    stringResource(R.string.voyage_switch_material_you), stringResource(R.string.voyage_switch_compose),
                                    stringResource(R.string.voyage_switch_pixel), stringResource(R.string.voyage_switch_fluent),
                                    stringResource(R.string.voyage_switch_cupertino), stringResource(R.string.voyage_switch_liquid_glass),
                                    stringResource(R.string.voyage_switch_hyper_os), stringResource(R.string.voyage_switch_one_ui),
                                )
                                val materialEnabled = uiState.uiMode == UiMode.Material.value
                                if (!materialEnabled) Text(stringResource(R.string.theme_design_overridden), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Drop(R.string.voyage_theme_switch_style, switches[uiState.materialSwitchStyle.coerceIn(0, switches.lastIndex)], switches, enabled = materialEnabled) { viewModel.setMaterialSwitchStyle(switches.indexOf(it).coerceAtLeast(0)) }
                                val sliders = listOf(
                                    stringResource(R.string.voyage_slider_material_you), stringResource(R.string.voyage_slider_fancy),
                                    stringResource(R.string.voyage_slider_material), stringResource(R.string.voyage_slider_hyper_os),
                                )
                                Drop(R.string.voyage_theme_slider_style, sliders[uiState.materialSliderStyle.coerceIn(0, sliders.lastIndex)], sliders, enabled = materialEnabled) { viewModel.setMaterialSliderStyle(sliders.indexOf(it).coerceAtLeast(0)) }
                                Slide(R.string.voyage_theme_border_width, uiState.materialBorderWidth, 0f..1.5f, viewModel::setMaterialBorderWidth, enabled = materialEnabled)
                            }
                            VoyageThemeCategory.SHADOWS -> {
                                Toggle(R.string.voyage_shadow_container, null, uiState.materialShadowContainers, viewModel::setMaterialShadowContainers)
                                Toggle(R.string.voyage_shadow_app_bar, null, uiState.materialShadowAppBars, viewModel::setMaterialShadowAppBars)
                                Toggle(R.string.voyage_shadow_button, null, uiState.materialShadowButtons, viewModel::setMaterialShadowButtons)
                                Toggle(R.string.voyage_shadow_fab, null, uiState.materialShadowFab, viewModel::setMaterialShadowFab)
                                Toggle(R.string.voyage_shadow_switch, null, uiState.materialShadowSwitches, viewModel::setMaterialShadowSwitches)
                                Toggle(R.string.voyage_shadow_slider, null, uiState.materialShadowSliders, viewModel::setMaterialShadowSliders)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun ThemeStyleChoice(title: String, summary: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = container,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
@Composable private fun Choice(id: Int, selected: Boolean, onClick: () -> Unit) {
    ListItem(modifier = Modifier.clickable(onClick = onClick), headlineContent = { Text(stringResource(id)) }, trailingContent = { RadioButton(selected, onClick) })
}
@Composable private fun Toggle(id: Int, summaryId: Int?, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(
        headlineContent = { Text(stringResource(id)) },
        supportingContent = summaryId?.let { { Text(stringResource(it)) } },
        trailingContent = { Switch(checked, onChange, enabled = enabled) },
        modifier = Modifier.clickable(enabled = enabled) { onChange(!checked) },
    )
}
@Composable private fun Slide(id: Int, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, enabled: Boolean = true) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id), modifier = Modifier.weight(1f))
            Text(String.format("%.1f", value), color = MaterialTheme.colorScheme.primary)
        }
        Slider(value.coerceIn(range.start, range.endInclusive), onChange, valueRange = range, enabled = enabled)
    }
}
@Composable private fun Drop(id: Int, selected: String, options: List<String>, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable(enabled = enabled) { expanded = true },
        headlineContent = { Text(stringResource(id)) },
        supportingContent = { Text(selected) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }, enabled = enabled) { Text(selected) }
                DropdownMenu(expanded && enabled, { expanded = false }) {
                    options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
                }
            }
        },
    )
}


@Composable
private fun VoyageThemeAccordion(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f })
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp), content = content)
            }
        }
    }
}
