package com.alohaoneui.app.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.material.SegmentedColumn
import com.alohaoneui.app.ui.component.material.SegmentedDropdownItem
import com.alohaoneui.app.ui.component.material.SegmentedListItem
import com.alohaoneui.app.ui.component.material.SegmentedSwitchItem
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.navigation3.Route
import com.alohaoneui.app.ui.theme.ColorMode
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import com.alohaoneui.app.ui.theme.LocalMaterialShadowAppBars
import androidx.compose.ui.draw.shadow

@Composable
fun ThemeSettingsScreenMaterial() {
    val navigator = LocalNavigator.current
    val viewModel = viewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = if (LocalMaterialShadowAppBars.current) Modifier.shadow(2.dp) else Modifier,
                navigationIcon = {
                    IconButton(onClick = { if (navigator.backStackSize() > 1) navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(stringResource(R.string.settings_theme_entry_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            ThemeSettingsGroup {
                SegmentedColumn(content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Palette,
                            title = stringResource(R.string.theme_dynamic_color_title),
                            summary = stringResource(R.string.theme_dynamic_color_subtitle),
                            checked = state.materialDynamicColorEnabled && state.keyColor == 0,
                            onCheckedChange = viewModel::setMaterialDynamicColorEnabled,
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = { navigator.push(Route.ColorPalette) },
                            headlineContent = { Text(stringResource(R.string.theme_theme_palette_entry)) },
                            supportingContent = { Text(stringResource(R.string.theme_theme_palette_entry_summary)) },
                            leadingContent = { Icon(Icons.Filled.Palette, null) },
                        )
                    },
                ))
            }
            ThemeSettingsGroup {
                SegmentedColumn(content = listOf(
                    {
                        SegmentedDropdownItem(
                            icon = Icons.Filled.Tune,
                            title = stringResource(R.string.theme_component_font_family),
                            items = listOf(
                                stringResource(R.string.settings_material_font_family_default),
                                stringResource(R.string.settings_material_font_family_serif),
                            ),
                            selectedIndex = listOf("Default", "Serif").indexOf(state.materialFontFamily).coerceAtLeast(0),
                            onItemSelected = { index ->
                                viewModel.setMaterialFontFamily(listOf("Default", "Serif")[index])
                            },
                        )
                    },
                    {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(stringResource(R.string.settings_material_font_scale))
                            Text(String.format(java.util.Locale.ROOT, "%.0f%%", state.materialFontScale * 100f), style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = state.materialFontScale,
                                onValueChange = viewModel::setMaterialFontScale,
                                valueRange = 0.8f..1.5f,
                            )
                        }
                    },
                ))
            }
            ThemeSettingsGroup {
                SegmentedColumn(content = listOf(
                    { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_container), checked = state.materialShadowContainers, onCheckedChange = viewModel::setMaterialShadowContainers) },
                    { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_app_bar), checked = state.materialShadowAppBars, onCheckedChange = viewModel::setMaterialShadowAppBars) },
                    { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_button), checked = state.materialShadowButtons, onCheckedChange = viewModel::setMaterialShadowButtons) },
                    { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_switch), checked = state.materialShadowSwitches, onCheckedChange = viewModel::setMaterialShadowSwitches) },
                    { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_slider), checked = state.materialShadowSliders, onCheckedChange = viewModel::setMaterialShadowSliders) },
                ))
            }
            ThemeSettingsGroup {
                SegmentedColumn(content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Tune,
                            title = stringResource(R.string.theme_invert_title),
                            checked = state.materialInvertColors,
                            onCheckedChange = viewModel::setMaterialInvertColors,
                        )
                    },
                    {
                        SegmentedDropdownItem(
                            icon = Icons.Filled.Tune,
                            title = stringResource(R.string.theme_assist_color_vision),
                            summary = stringResource(R.string.settings_material_color_vision_summary),
                            items = listOf(
                                stringResource(R.string.theme_color_blind_off),
                                stringResource(R.string.theme_color_blind_protanomaly),
                                stringResource(R.string.theme_color_blind_deuteranomaly),
                                stringResource(R.string.theme_color_blind_tritanomaly),
                                stringResource(R.string.theme_color_blind_protanopia),
                                stringResource(R.string.theme_color_blind_deuteranopia),
                                stringResource(R.string.theme_color_blind_tritanopia),
                                stringResource(R.string.theme_color_blind_achromatomaly),
                                stringResource(R.string.theme_color_blind_achromatopsia),
                            ),
                            selectedIndex = state.materialColorVisionMode,
                            onItemSelected = viewModel::setMaterialColorVisionMode,
                        )
                    },
                ))
            }
            ThemeSettingsGroup {
                SegmentedColumn(content = listOf({
                    SegmentedSwitchItem(
                        icon = Icons.Filled.DisplaySettings,
                        title = stringResource(R.string.theme_launch_follows_title),
                        summary = stringResource(R.string.theme_launch_follows_subtitle),
                        checked = state.materialLaunchFollowsTheme,
                        onCheckedChange = viewModel::setMaterialLaunchFollowsTheme,
                    )
                }))
            }
            ThemeSettingsGroup {
                val colorMode = ColorMode.fromValue(state.themeMode)
                val selectedMode = when {
                    colorMode.isSystem -> 0
                    colorMode.isDark -> 2
                    else -> 1
                }
                SegmentedColumn(content = listOf(
                    {
                        SegmentedDropdownItem(
                            icon = Icons.Filled.DisplaySettings,
                            title = stringResource(R.string.theme_group_mode),
                            items = listOf(
                                stringResource(R.string.theme_mode_system),
                                stringResource(R.string.theme_mode_light),
                                stringResource(R.string.theme_mode_dark),
                            ),
                            selectedIndex = selectedMode,
                            onItemSelected = { index ->
                                viewModel.setColorMode(
                                    when (index) {
                                        1 -> ColorMode.LIGHT
                                        2 -> ColorMode.DARK
                                        else -> ColorMode.SYSTEM
                                    },
                                )
                            },
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.DisplaySettings,
                            title = stringResource(R.string.settings_pure_black_title),
                            summary = stringResource(R.string.settings_pure_black_subtitle),
                            checked = state.themeMode == ColorMode.DARK_AMOLED.value,
                            onCheckedChange = { enabled ->
                                viewModel.setColorMode(if (enabled) ColorMode.DARK_AMOLED else ColorMode.DARK)
                            },
                        )
                    },
                ))
            }
            ThemeSettingsGroup {
                SegmentedColumn(content = listOf(
                    {
                        SegmentedDropdownItem(
                            icon = Icons.Filled.DisplaySettings,
                            title = stringResource(R.string.theme_group_design),
                            summary = stringResource(R.string.settings_ui_mode_summary),
                            items = listOf(stringResource(R.string.theme_design_miuix), stringResource(R.string.theme_design_material3)),
                            selectedIndex = if (state.uiMode == UiMode.Material.value) 1 else 0,
                            onItemSelected = { index ->
                                viewModel.setUiMode(if (index == 1) UiMode.Material.value else UiMode.Miuix.value)
                                if (index == 0 && navigator.backStackSize() > 1) navigator.pop()
                            },
                        )
                    },
                ))
            }
            ThemeSettingsGroup {
                SegmentedColumn(content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.DisplaySettings,
                            title = stringResource(R.string.theme_nav_floating),
                            summary = stringResource(R.string.theme_nav_floating_summary),
                            checked = state.enableFloatingBottomBar,
                            onCheckedChange = viewModel::setEnableFloatingBottomBar,
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Tune,
                            title = stringResource(R.string.theme_nav_blur),
                            checked = state.enableFloatingBottomBarBlur,
                            onCheckedChange = viewModel::setEnableFloatingBottomBarBlur,
                        )
                    },
                ))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeSettingsGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        content = content,
    )
}
