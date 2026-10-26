package com.alohaoneui.app.ui.screen.colorpalette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.CallToAction
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import com.materialkolor.hct.Hct
import com.alohaoneui.app.data.repository.MaterialColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
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
    var showColorEditor by rememberSaveable { mutableStateOf(false) }
    var showSaveScheme by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteScheme by rememberSaveable { mutableStateOf<String?>(null) }
    var schemeName by rememberSaveable { mutableStateOf("") }
    var hctHue by rememberSaveable { mutableFloatStateOf(270f) }
    var hctChroma by rememberSaveable { mutableFloatStateOf(48f) }
    var hctTone by rememberSaveable { mutableFloatStateOf(55f) }

    val isCompact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
    var expandedCategories by rememberSaveable(isCompact) {
        mutableStateOf(if (isCompact) VoyageThemeCategory.entries.map { it.name } else emptyList())
    }
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(stringResource(R.string.voyage_theme_page_title)) },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VoyageThemeCategory.entries.forEach { category ->
                val expanded = category.name in expandedCategories
                VoyageThemeAccordion(
                    category = category,
                    expanded = expanded,
                    onToggle = {
                        expandedCategories = if (expanded) expandedCategories - category.name
                        else expandedCategories + category.name
                    },
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                            when (category) {
                                VoyageThemeCategory.MODE -> {
    
                                    Choice(R.string.settings_theme_mode_system, ColorMode.fromValue(uiState.themeMode).toNonMonetMode() == ColorMode.SYSTEM.value) { viewModel.setColorMode(ColorMode.SYSTEM) }
                                    Choice(R.string.settings_theme_mode_light, ColorMode.fromValue(uiState.themeMode).toNonMonetMode() == ColorMode.LIGHT.value) { viewModel.setColorMode(ColorMode.LIGHT) }
                                    Choice(R.string.settings_theme_mode_dark, ColorMode.fromValue(uiState.themeMode).toNonMonetMode() == ColorMode.DARK.value && uiState.themeMode != ColorMode.DARK_AMOLED.value) { viewModel.setColorMode(ColorMode.DARK) }
                                    Choice(R.string.voyage_theme_pure_black, uiState.themeMode == ColorMode.DARK_AMOLED.value) { viewModel.setColorMode(ColorMode.DARK_AMOLED) }
                                }
                                VoyageThemeCategory.DESIGN -> {
    
                                    Toggle(R.string.voyage_theme_dynamic_color, null, uiState.materialDynamicColorEnabled, viewModel::setMaterialDynamicColorEnabled)
                                    Text(stringResource(R.string.voyage_theme_key_color), style = MaterialTheme.typography.titleMedium)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        ColorChoice(MaterialTheme.colorScheme.primary, uiState.keyColor == 0 && uiState.materialDynamicColorEnabled, stringResource(R.string.voyage_theme_system_color)) {
                                            viewModel.setKeyColor(0)
                                            viewModel.setMaterialDynamicColorEnabled(true)
                                        }
                                        keyColorOptions.forEach { color -> ColorChoice(Color(color), uiState.keyColor == color, null) {
                                            viewModel.setKeyColor(color)
                                            viewModel.setMaterialDynamicColorEnabled(false)
                                        } }
                                    }
                                    val paletteChoices = listOf(
                                        stringResource(R.string.voyage_palette_tonal_spot) to PaletteStyle.TonalSpot,
                                        stringResource(R.string.voyage_palette_expressive) to PaletteStyle.Expressive,
                                        stringResource(R.string.voyage_palette_fidelity) to PaletteStyle.Fidelity,
                                        stringResource(R.string.voyage_palette_fruit_salad) to PaletteStyle.FruitSalad,
                                        stringResource(R.string.voyage_palette_monochrome) to PaletteStyle.Monochrome,
                                        stringResource(R.string.voyage_palette_neutral) to PaletteStyle.Neutral,
                                        stringResource(R.string.voyage_palette_rainbow) to PaletteStyle.Rainbow,
                                        stringResource(R.string.voyage_palette_vibrant) to PaletteStyle.Vibrant,
                                        stringResource(R.string.voyage_palette_content) to PaletteStyle.Content,
                                    )
                                    val currentPaletteLabel = paletteChoices.firstOrNull { it.second.name == uiState.colorStyle }?.first ?: paletteChoices.first().first
                                    Drop(R.string.settings_color_style, currentPaletteLabel, paletteChoices.map { it.first }) { label ->
                                        viewModel.setColorStyle(paletteChoices.first { it.first == label }.second.name)
                                    }
                                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(R.string.voyage_theme_contrast), modifier = Modifier.weight(1f))
                                            TextButton(onClick = { viewModel.setMaterialContrastLevel(0f) }) {
                                                Text(stringResource(R.string.voyage_theme_contrast_standard))
                                            }
                                        }
                                        Slider(uiState.materialContrastLevel.coerceIn(-1f, 1f), viewModel::setMaterialContrastLevel, valueRange = -1f..1f)
                                    }
                                    Button(onClick = {
                                        val seed = if (uiState.keyColor == 0) MaterialTheme.colorScheme.primary.toArgb() else uiState.keyColor
                                        val hct = Hct.fromInt(seed)
                                        hctHue = hct.hue.toFloat()
                                        hctChroma = hct.chroma.toFloat()
                                        hctTone = hct.tone.toFloat()
                                        showColorEditor = true
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Filled.Edit, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.voyage_theme_color_editor))
                                    }
                                    Button(onClick = { schemeName = ""; showSaveScheme = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.voyage_theme_save_scheme))
                                    }
                                    Text(stringResource(R.string.voyage_theme_saved_schemes), style = MaterialTheme.typography.titleMedium)
                                    if (uiState.materialColorSchemes.isEmpty()) {
                                        Text(stringResource(R.string.voyage_theme_saved_schemes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        uiState.materialColorSchemes.forEach { scheme ->
                                            ListItem(
                                                headlineContent = { Text(scheme.name) },
                                                supportingContent = { Text(scheme.colorStyle) },
                                                trailingContent = {
                                                    Row {
                                                        IconButton(onClick = { viewModel.applyMaterialColorScheme(scheme) }) {
                                                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.voyage_theme_apply_scheme))
                                                        }
                                                        IconButton(onClick = { pendingDeleteScheme = scheme.name }) {
                                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.voyage_theme_delete_scheme))
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                                VoyageThemeCategory.COMPONENTS -> {
    
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
                                    val defaultFontLabel = stringResource(R.string.voyage_font_default)
                    val serifFontLabel = stringResource(R.string.voyage_font_serif)
                    Drop(R.string.voyage_theme_font_family, if (uiState.materialFontFamily == "Serif") serifFontLabel else defaultFontLabel, listOf(defaultFontLabel, serifFontLabel)) { viewModel.setMaterialFontFamily(if (it == serifFontLabel) "Serif" else "Default") }
                                    Slide(R.string.voyage_theme_font_scale, uiState.materialFontScale, 0.8f..1.5f, viewModel::setMaterialFontScale)
                                    Slide(R.string.voyage_theme_border_width, uiState.materialBorderWidth, 0f..1.5f, viewModel::setMaterialBorderWidth)
                                    Toggle(R.string.voyage_theme_large_text, null, uiState.materialLargeTextEnabled, viewModel::setMaterialLargeTextEnabled)
                                    Toggle(R.string.voyage_theme_bold_text, null, uiState.materialBoldTextEnabled, viewModel::setMaterialBoldTextEnabled)
                                }
                                VoyageThemeCategory.LAUNCH -> {
    
                                    Toggle(R.string.voyage_theme_launch_follows, null, uiState.materialLaunchFollowsTheme, viewModel::setMaterialLaunchFollowsTheme)
                                }
                                VoyageThemeCategory.NAVIGATION_BAR -> {
    
                                    Toggle(R.string.voyage_theme_floating_bar, null, uiState.enableFloatingBottomBar, viewModel::setEnableFloatingBottomBar)
                                    Toggle(R.string.voyage_theme_floating_bar_blur, null, uiState.enableFloatingBottomBarBlur, viewModel::setEnableFloatingBottomBarBlur)
                                    Toggle(R.string.settings_enable_predictive_back, stringResource(R.string.settings_enable_predictive_back_summary), uiState.enablePredictiveBack, viewModel::setEnablePredictiveBack)
                                    Toggle(R.string.settings_enable_swipe_back, stringResource(R.string.settings_enable_swipe_back_summary), uiState.enableSwipeBack, viewModel::setEnableSwipeBack)
    val pagerGestureOptions = listOf(
                                        stringResource(R.string.settings_page_gesture_default),
                                        stringResource(R.string.settings_page_gesture_cross_axis),
                                        stringResource(R.string.settings_page_gesture_ios_like),
                                    )
                                    Drop(R.string.settings_page_gesture_mode, pagerGestureOptions[uiState.pagerGestureMode.coerceIn(0, pagerGestureOptions.lastIndex)], pagerGestureOptions) { viewModel.setPagerGestureMode(pagerGestureOptions.indexOf(it).coerceAtLeast(0)) }
                                    Slide(R.string.settings_page_scale, uiState.pageScale, 0.8f..1.2f, viewModel::setPageScale)
                                }
                                VoyageThemeCategory.ASSIST -> {
    
                                    Toggle(R.string.voyage_theme_invert, null, uiState.materialInvertColors, viewModel::setMaterialInvertColors)
                                    val labels = listOf(
                                        stringResource(R.string.voyage_color_vision_off),
                                        stringResource(R.string.voyage_color_vision_protanomaly),
                                        stringResource(R.string.voyage_color_vision_deuteranomaly),
                                        stringResource(R.string.voyage_color_vision_tritanomaly),
                                        stringResource(R.string.voyage_color_vision_protanopia),
                                        stringResource(R.string.voyage_color_vision_deuteranopia),
                                        stringResource(R.string.voyage_color_vision_tritanopia),
                                        stringResource(R.string.voyage_color_vision_achromatomaly),
                                        stringResource(R.string.voyage_color_vision_achromatopsia),
                                    )
                                    Drop(R.string.voyage_theme_color_vision, labels[uiState.materialColorVisionMode.coerceIn(0, 8)], labels) { viewModel.setMaterialColorVisionMode(labels.indexOf(it).coerceAtLeast(0)) }
                                }
                                VoyageThemeCategory.SHAPE -> {
    
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

    if (showColorEditor) {
        AlertDialog(
            onDismissRequest = { showColorEditor = false },
            title = { Text(stringResource(R.string.voyage_theme_color_editor)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(Modifier.fillMaxWidth().height(44.dp), color = Color(Hct.from(hctHue.toDouble(), hctChroma.toDouble(), hctTone.toDouble()).toInt()), shape = MaterialTheme.shapes.medium) {}
                    HctSlider(R.string.voyage_theme_hue, hctHue, 0f..360f) { hctHue = it }
                    HctSlider(R.string.voyage_theme_chroma, hctChroma, 0f..100f) { hctChroma = it }
                    HctSlider(R.string.voyage_theme_tone, hctTone, 0f..100f) { hctTone = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setKeyColor(Hct.from(hctHue.toDouble(), hctChroma.toDouble(), hctTone.toDouble()).toInt())
                    viewModel.setMaterialDynamicColorEnabled(false)
                    showColorEditor = false
                }) { Text(stringResource(R.string.voyage_theme_apply_color)) }
            },
            dismissButton = {
                TextButton(onClick = { showColorEditor = false }) { Text(stringResource(R.string.voyage_theme_cancel)) }
            },
        )
    }
    if (showSaveScheme) {
        AlertDialog(
            onDismissRequest = { showSaveScheme = false },
            title = { Text(stringResource(R.string.voyage_theme_save_scheme)) },
            text = {
                OutlinedTextField(
                    value = schemeName,
                    onValueChange = { schemeName = it },
                    label = { Text(stringResource(R.string.voyage_theme_scheme_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (schemeName.isNotBlank()) {
                        viewModel.saveMaterialColorScheme(schemeName.trim())
                        showSaveScheme = false
                    }
                }, enabled = schemeName.isNotBlank()) { Text(stringResource(R.string.voyage_theme_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveScheme = false }) { Text(stringResource(R.string.voyage_theme_cancel)) }
            },
        )
    }
    pendingDeleteScheme?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDeleteScheme = null },
            title = { Text(stringResource(R.string.voyage_theme_delete_scheme_title)) },
            text = { Text(stringResource(R.string.voyage_theme_delete_scheme_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMaterialColorScheme(name); pendingDeleteScheme = null }) {
                    Text(stringResource(R.string.voyage_theme_delete_scheme))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteScheme = null }) { Text(stringResource(R.string.voyage_theme_cancel)) }
            },
        )
    }
}

@Composable
private fun VoyageThemeAccordion(
    category: VoyageThemeCategory,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val icon = when (category) {
        VoyageThemeCategory.DESIGN -> Icons.Outlined.Style
        VoyageThemeCategory.NAVIGATION_BAR -> Icons.Outlined.CallToAction
        VoyageThemeCategory.MODE -> Icons.Outlined.DarkMode
        VoyageThemeCategory.LAUNCH -> Icons.Outlined.AppShortcut
        VoyageThemeCategory.ASSIST -> Icons.Outlined.Visibility
        VoyageThemeCategory.SHAPE -> Icons.Outlined.Category
        VoyageThemeCategory.COMPONENTS -> Icons.Outlined.ToggleOn
        VoyageThemeCategory.SHADOWS -> Icons.Outlined.Layers
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(category.titleRes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable private fun HctSlider(id: Int, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id), modifier = Modifier.weight(1f))
            Text(value.toInt().toString(), color = MaterialTheme.colorScheme.primary)
        }
        Slider(value.coerceIn(range.start, range.endInclusive), onChange, valueRange = range)
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
