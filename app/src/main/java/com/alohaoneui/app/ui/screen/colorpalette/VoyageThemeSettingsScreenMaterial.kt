package com.alohaoneui.app.ui.screen.colorpalette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DarkMode
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.screen.settings.SettingsUiState
import com.alohaoneui.app.ui.theme.ColorMode
import com.alohaoneui.app.ui.theme.keyColorOptions
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import com.materialkolor.PaletteStyle

@Composable
fun VoyageThemeSettingsScreenMaterial(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    var selectedName by rememberSaveable { mutableStateOf(VoyageThemeCategory.DESIGN.name) }
    var categoryListVisible by rememberSaveable { mutableStateOf(true) }
    val selected = VoyageThemeCategory.valueOf(selectedName)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Scaffold(topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (!wide && !categoryListVisible) categoryListVisible = true else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                title = { Text(if (!wide && !categoryListVisible) stringResource(selected.titleRes) else stringResource(R.string.settings_theme)) },
            )
        }) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (wide || categoryListVisible) {
                    Column(
                        Modifier.then(if (wide) Modifier.width(280.dp).fillMaxHeight() else Modifier.fillMaxWidth())
                            .verticalScroll(rememberScrollState()).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        VoyageThemeCategory.entries.forEach { item ->
                            val active = item == selected
                            NavigationDrawerItem(
                                label = { Text(stringResource(item.titleRes)) },
                                selected = active,
                                onClick = { selectedName = item.name; categoryListVisible = false },
                                icon = {
                                    Icon(
                                        when (item) {
                                            VoyageThemeCategory.MODE -> Icons.Filled.DarkMode
                                            VoyageThemeCategory.DESIGN -> Icons.Filled.Palette
                                            VoyageThemeCategory.COMPONENTS -> Icons.Filled.Widgets
                                            VoyageThemeCategory.LAUNCH -> Icons.Filled.Launch
                                            VoyageThemeCategory.NAVIGATION_BAR -> Icons.Filled.Navigation
                                            VoyageThemeCategory.ASSIST -> Icons.Filled.AccessibilityNew
                                            VoyageThemeCategory.SHAPE -> Icons.Filled.Brush
                                            VoyageThemeCategory.SHADOWS -> Icons.Filled.Tune
                                        },
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                }
                if (wide || !categoryListVisible) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (selected) {
                            VoyageThemeCategory.MODE -> {
                                Header(R.string.voyage_theme_group_mode)
                                Choice(R.string.settings_theme_mode_system, uiState.themeMode == ColorMode.SYSTEM.value) { viewModel.setColorMode(ColorMode.SYSTEM) }
                                Choice(R.string.settings_theme_mode_light, uiState.themeMode == ColorMode.LIGHT.value) { viewModel.setColorMode(ColorMode.LIGHT) }
                                Choice(R.string.settings_theme_mode_dark, uiState.themeMode == ColorMode.DARK.value) { viewModel.setColorMode(ColorMode.DARK) }
                                Choice(R.string.voyage_theme_pure_black, uiState.themeMode == ColorMode.DARK_AMOLED.value) { viewModel.setColorMode(ColorMode.DARK_AMOLED) }
                            }
                            VoyageThemeCategory.DESIGN -> {
                                Header(R.string.voyage_theme_group_design)
                                Toggle(R.string.voyage_theme_dynamic_color, null, uiState.materialDynamicColorEnabled, viewModel::setMaterialDynamicColorEnabled)
                                Text(stringResource(R.string.voyage_theme_key_color), style = MaterialTheme.typography.titleMedium)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ColorChoice(Color(0xFF6750A4), uiState.keyColor == 0, stringResource(R.string.voyage_theme_system_color)) { viewModel.setKeyColor(0) }
                                    keyColorOptions.forEach { color -> ColorChoice(Color(color), uiState.keyColor == color, null) { viewModel.setKeyColor(color) } }
                                }
                                Drop(R.string.settings_color_style, uiState.colorStyle, PaletteStyle.entries.map { it.name }, viewModel::setColorStyle)
                                Slide(R.string.voyage_theme_contrast, uiState.materialContrastLevel, -1f..1f, viewModel::setMaterialContrastLevel)
                            }
                            VoyageThemeCategory.COMPONENTS -> {
                                Header(R.string.voyage_theme_group_components)
                                val switchStyles = listOf(
                    stringResource(R.string.voyage_switch_material_you), stringResource(R.string.voyage_switch_compose),
                    stringResource(R.string.voyage_switch_pixel), stringResource(R.string.voyage_switch_fluent),
                    stringResource(R.string.voyage_switch_cupertino), stringResource(R.string.voyage_switch_liquid_glass),
                    stringResource(R.string.voyage_switch_hyper_os), stringResource(R.string.voyage_switch_one_ui),
                )
                Drop(R.string.voyage_theme_switch_style, switchStyles[uiState.materialSwitchStyle.coerceIn(0, 7)], switchStyles) { viewModel.setMaterialSwitchStyle(switchStyles.indexOf(it).coerceAtLeast(0)) }
                                val sliderStyles = listOf(
                    stringResource(R.string.voyage_slider_material_you), stringResource(R.string.voyage_slider_fancy),
                    stringResource(R.string.voyage_slider_material), stringResource(R.string.voyage_slider_hyper_os),
                )
                Drop(R.string.voyage_theme_slider_style, sliderStyles[uiState.materialSliderStyle.coerceIn(0, 3)], sliderStyles) { viewModel.setMaterialSliderStyle(sliderStyles.indexOf(it).coerceAtLeast(0)) }
                                Drop(R.string.voyage_theme_font_family, if (uiState.materialFontFamily == "Serif") stringResource(R.string.voyage_font_serif) else stringResource(R.string.voyage_font_default), listOf(stringResource(R.string.voyage_font_default), stringResource(R.string.voyage_font_serif))) { viewModel.setMaterialFontFamily(if (it == stringResource(R.string.voyage_font_serif)) "Serif" else "Default") }
                                Slide(R.string.voyage_theme_font_scale, uiState.materialFontScale, 0.8f..1.5f, viewModel::setMaterialFontScale)
                                Slide(R.string.voyage_theme_border_width, uiState.materialBorderWidth, 0f..1.5f, viewModel::setMaterialBorderWidth)
                                Toggle(R.string.voyage_theme_large_text, null, uiState.materialLargeTextEnabled, viewModel::setMaterialLargeTextEnabled)
                                Toggle(R.string.voyage_theme_bold_text, null, uiState.materialBoldTextEnabled, viewModel::setMaterialBoldTextEnabled)
                            }
                            VoyageThemeCategory.LAUNCH -> {
                                Header(R.string.voyage_theme_group_launch)
                                Toggle(R.string.voyage_theme_launch_follows, null, uiState.materialLaunchFollowsTheme, viewModel::setMaterialLaunchFollowsTheme)
                            }
                            VoyageThemeCategory.NAVIGATION_BAR -> {
                                Header(R.string.voyage_theme_group_navigation_bar)
                                Toggle(R.string.voyage_theme_floating_bar, null, uiState.enableFloatingBottomBar, viewModel::setEnableFloatingBottomBar)
                                Toggle(R.string.voyage_theme_floating_bar_blur, null, uiState.enableFloatingBottomBarBlur, viewModel::setEnableFloatingBottomBarBlur)
                                Toggle(R.string.settings_enable_predictive_back, stringResource(R.string.settings_enable_predictive_back_summary), uiState.enablePredictiveBack, viewModel::setEnablePredictiveBack)
                                Toggle(R.string.settings_enable_swipe_back, stringResource(R.string.settings_enable_swipe_back_summary), uiState.enableSwipeBack, viewModel::setEnableSwipeBack)
                                Drop(R.string.settings_page_gesture_mode, uiState.pagerGestureMode.toString(), listOf("0", "1", "2")) { viewModel.setPagerGestureMode(it.toIntOrNull() ?: 0) }
                                Slide(R.string.settings_page_scale, uiState.pageScale, 0.8f..1.2f, viewModel::setPageScale)
                            }
                            VoyageThemeCategory.ASSIST -> {
                                Header(R.string.voyage_theme_group_assist)
                                Toggle(R.string.voyage_theme_invert, null, uiState.materialInvertColors, viewModel::setMaterialInvertColors)
                                val labels = listOf("Off", "Protanomaly", "Deuteranomaly", "Tritanomaly", "Protanopia", "Deuteranopia", "Tritanopia", "Achromatomaly", "Achromatopsia")
                                Drop(R.string.voyage_theme_color_vision, labels[uiState.materialColorVisionMode.coerceIn(0, 8)], labels) { viewModel.setMaterialColorVisionMode(labels.indexOf(it).coerceAtLeast(0)) }
                            }
                            VoyageThemeCategory.SHAPE -> {
                                Header(R.string.voyage_theme_group_shape)
                                val labels = listOf(
                                    stringResource(R.string.voyage_shape_rounded), stringResource(R.string.voyage_shape_cut),
                                    stringResource(R.string.voyage_shape_squircle), stringResource(R.string.voyage_shape_smooth),
                                    stringResource(R.string.voyage_shape_wavy), stringResource(R.string.voyage_shape_scoop),
                                    stringResource(R.string.voyage_shape_notch),
                                )
                                Drop(R.string.voyage_theme_shape_style, labels[uiState.materialShapeStyle.coerceIn(0, labels.lastIndex)], labels) { viewModel.setMaterialShapeStyle(labels.indexOf(it).coerceAtLeast(0)) }
                                Slide(R.string.voyage_theme_corner_scale, uiState.materialCornerScale, 0.5f..1.5f, viewModel::setMaterialCornerScale)
                            }
                            VoyageThemeCategory.SHADOWS -> {
                                Header(R.string.voyage_theme_group_shadows)
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

@Composable private fun Header(id: Int) { Text(stringResource(id), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp)) }
@Composable private fun Choice(id: Int, selected: Boolean, onClick: () -> Unit) {
    ListItem(modifier = Modifier.clickable(onClick = onClick), headlineContent = { Text(stringResource(id)) }, trailingContent = { RadioButton(selected, onClick) })
}
@Composable private fun Toggle(id: Int, summary: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(stringResource(id)) }, supportingContent = summary?.let { { Text(it) } }, trailingContent = { Switch(checked, onChange) })
}
@Composable private fun Slide(id: Int, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id), modifier = Modifier.weight(1f))
            Text(String.format("%.1f", value), color = MaterialTheme.colorScheme.primary)
        }
        Slider(value.coerceIn(range.start, range.endInclusive), onChange, valueRange = range)
    }
}
@Composable private fun Drop(id: Int, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable { expanded = true },
        headlineContent = { Text(stringResource(id)) },
        supportingContent = { Text(selected) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) { Text(selected) }
                DropdownMenu(expanded, { expanded = false }) {
                    options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
                }
            }
        },
    )
}
@Composable private fun ColorChoice(color: Color, selected: Boolean, label: String?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(if (selected) 40.dp else 34.dp).clickable(onClick = onClick), shape = CircleShape, color = color, border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null) {}
        if (label != null) Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
