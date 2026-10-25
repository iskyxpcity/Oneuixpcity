package com.alohaoneui.app.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import com.materialkolor.PaletteStyle

private data class VoyageThemeSection(val title: Int, val icon: ImageVector)

@Composable
fun ThemeSettingsScreenMaterial() {
    val navigator = LocalNavigator.current
    val vm = viewModel<SettingsViewModel>()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val sections = listOf(
        VoyageThemeSection(R.string.theme_group_design, Icons.Filled.Palette),
        VoyageThemeSection(R.string.theme_group_mode, Icons.Filled.Brightness4),
        VoyageThemeSection(R.string.theme_group_shape, Icons.Filled.AspectRatio),
        VoyageThemeSection(R.string.theme_group_components, Icons.Filled.Tune),
        VoyageThemeSection(R.string.theme_group_shadows, Icons.Filled.Layers),
        VoyageThemeSection(R.string.theme_group_assist, Icons.Filled.Visibility),
        VoyageThemeSection(R.string.theme_group_launch, Icons.Filled.RocketLaunch),
        VoyageThemeSection(R.string.theme_group_navigation_bar, Icons.Filled.Navigation),
    )
    Scaffold(topBar = {
        LargeFlexibleTopAppBar(
            navigationIcon = { IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            title = { Text(stringResource(R.string.settings_theme_entry_title)) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
        )
    }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            if (maxWidth >= 600.dp) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail {
                        sections.forEachIndexed { i, s ->
                            NavigationRailItem(selected == i, { selected = i }, icon = { Icon(s.icon, null) }, label = { Text(stringResource(s.title)) }, alwaysShowLabel = true)
                        }
                    }
                    ThemeSection(selected, state, vm, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    ThemeSection(selected, state, vm, Modifier.weight(1f).fillMaxWidth())
                    NavigationBar {
                        sections.forEachIndexed { i, s ->
                            NavigationBarItem(selected == i, { selected = i }, icon = { Icon(s.icon, null) }, label = { Text(stringResource(s.title)) }, alwaysShowLabel = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(index: Int, state: SettingsUiState, vm: SettingsViewModel, modifier: Modifier) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (index) {
            0 -> {
                ThemeGroup {
                    ThemeSwitch(stringResource(R.string.settings_dynamic_color_title), stringResource(R.string.settings_dynamic_color_subtitle), state.materialDynamicColorEnabled, vm::setMaterialDynamicColorEnabled)
                    val styles = listOf(PaletteStyle.Expressive, PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.Vibrant, PaletteStyle.Rainbow, PaletteStyle.FruitSalad, PaletteStyle.Monochrome, PaletteStyle.Fidelity, PaletteStyle.Content)
                    val labels = styles.map { stringResource(when(it) {
                        PaletteStyle.Content -> R.string.settings_palette_style_content
                        PaletteStyle.Expressive -> R.string.settings_palette_style_expressive
                        PaletteStyle.Fidelity -> R.string.settings_palette_style_fidelity
                        PaletteStyle.FruitSalad -> R.string.settings_palette_style_fruit_salad
                        PaletteStyle.Monochrome -> R.string.settings_palette_style_monochrome
                        PaletteStyle.Neutral -> R.string.settings_palette_style_neutral
                        PaletteStyle.Rainbow -> R.string.settings_palette_style_rainbow
                        PaletteStyle.TonalSpot -> R.string.settings_palette_style_tonal_spot
                        PaletteStyle.Vibrant -> R.string.settings_palette_style_vibrant
                    }) }
                    ThemeDropdown(stringResource(R.string.settings_palette_style_title), labels, styles.indexOfFirst { it.name == state.colorStyle }.coerceAtLeast(0), { vm.setColorStyle(styles[it].name) }, !state.materialDynamicColorEnabled || state.keyColor != 0)
                    ThemeSlider(stringResource(R.string.settings_contrast_title), state.materialContrastLevel, -1f..1f, if (kotlin.math.abs(state.materialContrastLevel) < .01f) stringResource(R.string.settings_contrast_standard) else "%.1f".format(state.materialContrastLevel), vm::setMaterialContrastLevel)
                }
                ThemeGroup {
                    Text(stringResource(R.string.theme_scheme_title), Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                    val presets = listOf(
                        Triple(R.string.settings_seed_wallpaper, 0, Color(0xFF6750A4)),
                        Triple(R.string.settings_seed_amber, 0xFFFFB300.toInt(), Color(0xFFFFB300)),
                        Triple(R.string.settings_seed_coral, 0xFFFF6F61.toInt(), Color(0xFFFF6F61)),
                        Triple(R.string.settings_seed_forest, 0xFF2E7D32.toInt(), Color(0xFF2E7D32)),
                        Triple(R.string.settings_seed_indigo, 0xFF3F51B5.toInt(), Color(0xFF3F51B5)),
                        Triple(R.string.settings_seed_ocean, 0xFF006A8E.toInt(), Color(0xFF006A8E)),
                        Triple(R.string.settings_seed_wisteria, 0xFF6750A4.toInt(), Color(0xFF6750A4)),
                    )
                    presets.chunked(3).forEach { row -> Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (label, colorValue, swatch) ->
                            FilterChip(
                                selected = if (colorValue == 0) state.keyColor == 0 else state.keyColor == colorValue,
                                onClick = { vm.setKeyColor(colorValue); vm.setMaterialDynamicColorEnabled(colorValue == 0) },
                                label = { Text(stringResource(label)) },
                                leadingIcon = { Surface(color = swatch, shape = MaterialTheme.shapes.small) { Box(Modifier.size(12.dp)) } },
                            )
                        }
                    } }
                }
            }
            1 -> ThemeGroup {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.settings_theme_mode_system to 0, R.string.settings_theme_mode_light to 1, R.string.settings_theme_mode_dark to 2).forEach { (label, mode) ->
                        FilterChip(selected = state.themeMode == mode || (mode == 2 && state.themeMode == 6), onClick = { vm.setThemeMode(mode) }, label = { Text(stringResource(label)) })
                    }
                }
                ThemeSwitch(stringResource(R.string.settings_pure_black_title), stringResource(R.string.settings_pure_black_subtitle), state.themeMode == 6) { vm.setThemeMode(if (it) 6 else 2) }
            }
            2 -> ThemeGroup {
                val labels = listOf(R.string.theme_shape_rounded, R.string.theme_shape_cut, R.string.theme_shape_squircle, R.string.theme_shape_smooth, R.string.theme_shape_wavy, R.string.theme_shape_scoop, R.string.theme_shape_notch).map { stringResource(it) }
                ThemeDropdown(stringResource(R.string.theme_shape_style_title), labels, state.materialShapeStyle.coerceIn(0, labels.lastIndex), vm::setMaterialShapeStyle)
                ThemeSlider(stringResource(R.string.theme_corner_scale_title), state.materialCornerScale, .5f..1.5f, "${(state.materialCornerScale * 100).toInt()}%", vm::setMaterialCornerScale)
            }
            3 -> ThemeGroup {
                val switchLabels = listOf(R.string.theme_switch_material_you, R.string.theme_switch_compose, R.string.theme_switch_pixel, R.string.theme_switch_fluent, R.string.theme_switch_cupertino, R.string.theme_switch_liquid_glass, R.string.theme_switch_hyper_os, R.string.theme_switch_one_ui).map { stringResource(it) }
                val sliderLabels = listOf(R.string.theme_slider_material_you, R.string.theme_slider_fancy, R.string.theme_slider_material, R.string.theme_slider_hyper_os).map { stringResource(it) }
                ThemeDropdown(stringResource(R.string.theme_switch_style_title), switchLabels, state.materialSwitchStyle.coerceIn(0, switchLabels.lastIndex), vm::setMaterialSwitchStyle)
                ThemeDropdown(stringResource(R.string.theme_slider_style_title), sliderLabels, state.materialSliderStyle.coerceIn(0, sliderLabels.lastIndex), vm::setMaterialSliderStyle)
                ThemeSlider(stringResource(R.string.theme_border_width_title), state.materialBorderWidth, 0f..3f, "%.1f dp".format(state.materialBorderWidth), vm::setMaterialBorderWidth)
            }
            4 -> ThemeGroup {
                ThemeSwitch(stringResource(R.string.theme_shadow_container), null, state.materialShadowContainers, vm::setMaterialShadowContainers)
                ThemeSwitch(stringResource(R.string.theme_shadow_app_bar), null, state.materialShadowAppBars, vm::setMaterialShadowAppBars)
                ThemeSwitch(stringResource(R.string.theme_shadow_button), null, state.materialShadowButtons, vm::setMaterialShadowButtons)
                ThemeSwitch(stringResource(R.string.theme_shadow_fab), null, state.materialShadowFab, vm::setMaterialShadowFab)
                ThemeSwitch(stringResource(R.string.theme_shadow_switch), null, state.materialShadowSwitches, vm::setMaterialShadowSwitches)
                ThemeSwitch(stringResource(R.string.theme_shadow_slider), null, state.materialShadowSliders, vm::setMaterialShadowSliders)
            }
            5 -> ThemeGroup {
                ThemeSwitch(stringResource(R.string.theme_invert_title), null, state.materialInvertColors, vm::setMaterialInvertColors)
                val labels = listOf(R.string.theme_color_blind_off, R.string.theme_color_blind_protanomaly, R.string.theme_color_blind_deuteranomaly, R.string.theme_color_blind_tritanomaly, R.string.theme_color_blind_protanopia, R.string.theme_color_blind_deuteranopia, R.string.theme_color_blind_tritanopia, R.string.theme_color_blind_achromatomaly, R.string.theme_color_blind_achromatopsia).map { stringResource(it) }
                ThemeDropdown(stringResource(R.string.theme_color_blind_title), labels, state.materialColorVisionMode.coerceIn(0, labels.lastIndex), vm::setMaterialColorVisionMode)
            }
            6 -> ThemeGroup {
                ThemeSwitch(stringResource(R.string.theme_launch_follows_title), stringResource(R.string.theme_launch_follows_subtitle), state.materialLaunchFollowsTheme, vm::setMaterialLaunchFollowsTheme)
            }
            else -> ThemeGroup {
                ThemeSwitch(stringResource(R.string.theme_floating_nav_bar_title), stringResource(R.string.theme_floating_nav_bar_subtitle), state.enableFloatingBottomBar, vm::setEnableFloatingBottomBar)
                ThemeSwitch(stringResource(R.string.theme_liquid_glass_title), if (state.enableFloatingBottomBar) stringResource(R.string.theme_liquid_glass_subtitle) else stringResource(R.string.theme_liquid_glass_needs_floating), state.enableFloatingBottomBarBlur, vm::setEnableFloatingBottomBarBlur, state.enableFloatingBottomBar)
            }
        }
    }
}

@Composable
private fun ThemeGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        Column(content = content)
    }
}

@Composable
private fun ThemeSwitch(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked, onChange, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ThemeDropdown(title: String, options: List<String>, selected: Int, onSelected: (Int) -> Unit, enabled: Boolean = true) {
    var expanded by remember(title) { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }, enabled = enabled) { Text(options.getOrElse(selected) { options.firstOrNull().orEmpty() }) }
                DropdownMenu(expanded, { expanded = false }) {
                    options.forEachIndexed { index, option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(index); expanded = false }, enabled = enabled) }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ThemeSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f))
            Text(valueText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value, onChange, valueRange = range)
    }
}
