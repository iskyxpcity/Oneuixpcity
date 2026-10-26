package com.alohaoneui.app.ui.screen.colorpalette

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.materialkolor.hct.Hct
import com.alohaoneui.app.data.repository.MaterialColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.UiMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.screen.settings.SettingsUiState
import com.alohaoneui.app.ui.theme.ColorMode
import com.alohaoneui.app.ui.component.material.ExpressiveSwitch
import com.alohaoneui.app.ui.component.material.VoyageSlider
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import com.materialkolor.PaletteStyle

@Composable
fun VoyageColorSchemeEditorScreenMaterial(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    schemeEditorOnly: Boolean = false,
    schemeIndex: Int = -1,
    onOpenColorSchemeEditor: (Int) -> Unit = {},
) {
    var showColorEditor by rememberSaveable { mutableStateOf(false) }
    var showSaveScheme by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteScheme by rememberSaveable { mutableStateOf<String?>(null) }
    var schemeName by rememberSaveable(schemeEditorOnly, schemeIndex) { mutableStateOf(uiState.materialColorSchemes.getOrNull(schemeIndex)?.name.orEmpty()) }
    var hctHue by rememberSaveable { mutableFloatStateOf(270f) }
    var hctChroma by rememberSaveable { mutableFloatStateOf(48f) }
    var hctTone by rememberSaveable { mutableFloatStateOf(55f) }
    val currentSeedColor = if (uiState.keyColor == 0) MaterialTheme.colorScheme.primary.toArgb() else uiState.keyColor
    LaunchedEffect(schemeEditorOnly, schemeIndex, uiState.materialColorSchemes) {
        if (schemeEditorOnly && schemeIndex in uiState.materialColorSchemes.indices) {
            viewModel.applyMaterialColorScheme(uiState.materialColorSchemes[schemeIndex])
        }
    }

    var expandedCategories by rememberSaveable(schemeEditorOnly) {
        mutableStateOf(
            if (schemeEditorOnly) listOf(VoyageThemeCategory.DESIGN.name)
            else listOf(
                VoyageThemeCategory.DESIGN.name,
                VoyageThemeCategory.MODE.name,
                VoyageThemeCategory.NAVIGATION_BAR.name,
                VoyageThemeCategory.LAUNCH.name,
                VoyageThemeCategory.PALETTE.name,
            )
        )
    }
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(stringResource(if (schemeEditorOnly) R.string.voyage_theme_color_editor else R.string.voyage_theme_page_title)) },
            )
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val columns = when {
                schemeEditorOnly -> listOf(listOf(VoyageThemeCategory.DESIGN))
                maxWidth >= 600.dp -> listOf(
                    listOf(VoyageThemeCategory.DESIGN, VoyageThemeCategory.MODE, VoyageThemeCategory.COMPONENTS),
                    listOf(VoyageThemeCategory.NAVIGATION_BAR, VoyageThemeCategory.LAUNCH, VoyageThemeCategory.ASSIST, VoyageThemeCategory.SHAPE, VoyageThemeCategory.SHADOWS),
                    listOf(VoyageThemeCategory.PALETTE),
                )
                else -> listOf(
                    listOf(
                        VoyageThemeCategory.DESIGN,
                        VoyageThemeCategory.MODE,
                        VoyageThemeCategory.NAVIGATION_BAR,
                        VoyageThemeCategory.LAUNCH,
                        VoyageThemeCategory.PALETTE,
                        VoyageThemeCategory.ASSIST,
                        VoyageThemeCategory.SHAPE,
                        VoyageThemeCategory.COMPONENTS,
                        VoyageThemeCategory.SHADOWS,
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            columns.forEach { columnCategories ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    columnCategories.forEach { category ->
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
                                    Choice(R.string.settings_theme_mode_dark, ColorMode.fromValue(uiState.themeMode).toNonMonetMode() == ColorMode.DARK.value) { viewModel.setColorMode(ColorMode.DARK) }
                                    Toggle(R.string.voyage_theme_pure_black, stringResource(R.string.voyage_theme_pure_black_summary), uiState.themeMode == ColorMode.DARK_AMOLED.value, onChange = { enabled -> viewModel.setColorMode(if (enabled) ColorMode.DARK_AMOLED else ColorMode.DARK) })
                                }
                                VoyageThemeCategory.DESIGN -> {
                                    if (!schemeEditorOnly) {
                                        ThemeStyleChoice(
                                            title = stringResource(R.string.theme_design_material3),
                                            summary = stringResource(R.string.theme_design_material3_subtitle),
                                            icon = Icons.Outlined.Palette,
                                            selected = uiState.uiMode == UiMode.Material.value,
                                            onClick = { viewModel.setUiMode(UiMode.Material.value) },
                                        )
                                        ThemeStyleChoice(
                                            title = stringResource(R.string.theme_design_miuix),
                                            summary = stringResource(R.string.theme_design_miuix_subtitle),
                                            icon = Icons.Outlined.PhoneAndroid,
                                            selected = uiState.uiMode == UiMode.Miuix.value,
                                            onClick = { viewModel.setUiMode(UiMode.Miuix.value) },
                                        )
                                    }
                                    if (schemeEditorOnly) {
                                        Text(stringResource(R.string.voyage_theme_key_color), style = MaterialTheme.typography.titleMedium)
                                        val editorSeedPresets = listOf(
                                            stringResource(R.string.voyage_seed_wisteria) to 0xFF6750A4.toInt(),
                                            stringResource(R.string.voyage_seed_ocean) to 0xFF006A6A.toInt(),
                                            stringResource(R.string.voyage_seed_forest) to 0xFF426900.toInt(),
                                            stringResource(R.string.voyage_seed_amber) to 0xFF8C5000.toInt(),
                                            stringResource(R.string.voyage_seed_coral) to 0xFF9C4146.toInt(),
                                            stringResource(R.string.voyage_seed_indigo) to 0xFF4054A4.toInt(),
                                        )
                                        Row(
                                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            editorSeedPresets.forEach { (label, color) ->
                                                ColorChoice(Color(color), uiState.keyColor == color, label) {
                                                    viewModel.setKeyColor(color)
                                                    viewModel.setMaterialDynamicColorEnabled(false)
                                                }
                                            }
                                        }
                                        val currentPrimaryArgb = MaterialTheme.colorScheme.primary.toArgb()
                                        Button(onClick = {
                                            val seed = if (uiState.keyColor == 0) currentPrimaryArgb else uiState.keyColor
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
                                        Button(onClick = { if (schemeIndex < 0) schemeName = ""; showSaveScheme = true }, modifier = Modifier.fillMaxWidth()) {
                                            Text(stringResource(R.string.voyage_theme_save_scheme))
                                        }
                                        Text(stringResource(R.string.voyage_theme_saved_schemes), style = MaterialTheme.typography.titleMedium)
                                        if (uiState.materialColorSchemes.isEmpty()) {
                                            Text(stringResource(R.string.voyage_theme_saved_schemes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            uiState.materialColorSchemes.forEachIndexed { index, scheme ->
                                                ListItem(
                                                    headlineContent = { Text(scheme.name) },
                                                    supportingContent = { Text(scheme.colorStyle) },
                                                    trailingContent = {
                                                        Row {
                                                            IconButton(onClick = { onOpenColorSchemeEditor(index) }) {
                                                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.voyage_theme_color_editor))
                                                            }
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
                                }
                                VoyageThemeCategory.PALETTE -> {
                                    Text(stringResource(R.string.voyage_theme_wallpaper_colors), style = MaterialTheme.typography.titleMedium)
                                    val wallpaperColors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        wallpaperColors.forEach { color ->
                                            Surface(
                                                modifier = Modifier.weight(1f).height(28.dp).clickable {
                                                    viewModel.setKeyColor(color.toArgb())
                                                    viewModel.setMaterialDynamicColorEnabled(false)
                                                },
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                                color = color,
                                            ) {}
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val expressive = stringResource(R.string.voyage_palette_expressive)
                                        FilledTonalButton(
                                            onClick = { viewModel.setColorStyle(PaletteStyle.Expressive.name) },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (uiState.colorStyle == PaletteStyle.Expressive.name) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            ),
                                        ) { Text(expressive) }
                                        FilledTonalButton(
                                            onClick = {
                                                viewModel.setKeyColor(0)
                                                viewModel.setMaterialDynamicColorEnabled(true)
                                            },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (uiState.materialDynamicColorEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            ),
                                        ) { Text(stringResource(R.string.voyage_theme_follow_system)) }
                                    }
                                    Toggle(
                                        R.string.voyage_theme_dynamic_color,
                                        stringResource(R.string.voyage_theme_dynamic_color_summary),
                                        uiState.materialDynamicColorEnabled,
                                        viewModel::setMaterialDynamicColorEnabled,
                                    )
                                    ListItem(
                                        modifier = Modifier.clickable(
                                            enabled = !uiState.materialDynamicColorEnabled,
                                            onClick = { onOpenColorSchemeEditor(-1) },
                                        ),
                                        headlineContent = { Text(stringResource(R.string.voyage_theme_scheme_title)) },
                                        supportingContent = {
                                            if (uiState.materialDynamicColorEnabled) {
                                                Text(stringResource(R.string.voyage_theme_scheme_dynamic_disabled))
                                            }
                                        },
                                        trailingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                    )
                                    val seedColors = listOf(
                                        0xFFA8A0CD.toInt(), 0xFF75ADB0.toInt(), 0xFF96AC7B.toInt(), 0xFFBBA07B.toInt(),
                                        0xFFC3989E.toInt(), 0xFF95A2CD.toInt(), 0xFFF08181.toInt(), 0xFFB27880.toInt(),
                                        0xFFBA957B.toInt(), 0xFFF4B47B.toInt(), 0xFFF2F38B.toInt(), 0xFFB9E68B.toInt(),
                                        0xFF80D0B2.toInt(), 0xFF75C8CC.toInt(), 0xFF75A7FA.toInt(), 0xFFF1AAEB.toInt(),
                                        0xFFE079B0.toInt(), 0xFFE2C2F9.toInt(), 0xFFB28DF0.toInt(), 0xFF768DB1.toInt(),
                                        0xFFF4F7FA.toInt(), 0xFF75787B.toInt(),
                                    )
                                    ColorPaletteGrid(
                                        colors = seedColors,
                                        selectedColor = uiState.keyColor,
                                        dynamicSelected = uiState.materialDynamicColorEnabled,
                                        onSelect = { color ->
                                            viewModel.setKeyColor(color)
                                            viewModel.setMaterialDynamicColorEnabled(false)
                                        },
                                        onAdd = {
                                            val seed = if (uiState.keyColor == 0) MaterialTheme.colorScheme.primary.toArgb() else uiState.keyColor
                                            val hct = Hct.fromInt(seed)
                                            hctHue = hct.hue.toFloat()
                                            hctChroma = hct.chroma.toFloat()
                                            hctTone = hct.tone.toFloat()
                                            showColorEditor = true
                                        },
                                    )
                                    val paletteChoices = listOf(
                                        stringResource(R.string.voyage_palette_tonal_spot) to PaletteStyle.TonalSpot,
                                        stringResource(R.string.voyage_palette_neutral) to PaletteStyle.Neutral,
                                        stringResource(R.string.voyage_palette_vibrant) to PaletteStyle.Vibrant,
                                        stringResource(R.string.voyage_palette_expressive) to PaletteStyle.Expressive,
                                        stringResource(R.string.voyage_palette_rainbow) to PaletteStyle.Rainbow,
                                        stringResource(R.string.voyage_palette_fruit_salad) to PaletteStyle.FruitSalad,
                                        stringResource(R.string.voyage_palette_monochrome) to PaletteStyle.Monochrome,
                                        stringResource(R.string.voyage_palette_fidelity) to PaletteStyle.Fidelity,
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
                                        VoyageSlider(uiState.materialContrastLevel.coerceIn(-1f, 1f), viewModel::setMaterialContrastLevel, valueRange = -1f..1f)
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
                                    Slide(R.string.voyage_theme_border_width, uiState.materialBorderWidth, 0f..1.5f, viewModel::setMaterialBorderWidth)
                                }
                                VoyageThemeCategory.LAUNCH -> {
    
                                    Toggle(R.string.voyage_theme_launch_follows, null, uiState.materialLaunchFollowsTheme, viewModel::setMaterialLaunchFollowsTheme)
                                }
                                VoyageThemeCategory.NAVIGATION_BAR -> {
                                    Toggle(R.string.voyage_theme_floating_bar, stringResource(R.string.voyage_theme_floating_bar_summary), uiState.enableFloatingBottomBar, viewModel::setEnableFloatingBottomBar)
                                    Toggle(
                                        R.string.voyage_theme_floating_bar_blur,
                                        stringResource(
                                            when {
                                                android.os.Build.VERSION.SDK_INT < 33 -> R.string.voyage_theme_floating_bar_blur_unsupported
                                                !uiState.enableFloatingBottomBar -> R.string.voyage_theme_floating_bar_blur_requires_floating
                                                else -> R.string.voyage_theme_floating_bar_blur_summary
                                            }
                                        ),
                                        uiState.enableFloatingBottomBarBlur,
                                        viewModel::setEnableFloatingBottomBarBlur,
                                        enabled = android.os.Build.VERSION.SDK_INT >= 33 && uiState.enableFloatingBottomBar,
                                    )
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
                                    IconShapeGallery(
                                        selectedShape = uiState.materialIconShape,
                                        title = stringResource(R.string.voyage_theme_icon_shape_title),
                                        randomDescription = stringResource(R.string.voyage_theme_icon_shape_random),
                                        onSelect = viewModel::setMaterialIconShape,
                                    )
                                }
                                VoyageThemeCategory.SHADOWS -> {
    
                                    Toggle(R.string.voyage_shadow_container, null, uiState.materialShadowContainers, viewModel::setMaterialShadowContainers)
                                    Toggle(R.string.voyage_shadow_app_bar, null, uiState.materialShadowAppBars, viewModel::setMaterialShadowAppBars)
                                    Toggle(R.string.voyage_shadow_button, null, uiState.materialShadowButtons, viewModel::setMaterialShadowButtons)
                                    Toggle(R.string.voyage_shadow_fab, null, uiState.materialShadowFab, viewModel::setMaterialShadowFab)
                                    Toggle(R.string.voyage_shadow_switch, null, uiState.materialShadowSwitches, viewModel::setMaterialShadowSwitches)
                                    Toggle(R.string.voyage_shadow_slider, null, uiState.materialShadowSliders, viewModel::setMaterialShadowSliders)
                                    ThemeResetCard {
                                        viewModel.setUiMode(UiMode.Material.value)
                                        viewModel.setColorMode(ColorMode.SYSTEM)
                                        viewModel.setKeyColor(0)
                                        viewModel.setMaterialDynamicColorEnabled(true)
                                        viewModel.setColorStyle(PaletteStyle.Expressive.name)
                                        viewModel.setColorSpec(com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.SPEC_2025.name)
                                        viewModel.setMaterialContrastMode(-1)
                                        viewModel.setMaterialContrastLevel(0f)
                                        viewModel.setMaterialCornerScale(1f)
                                        viewModel.setMaterialBorderWidth(0f)
                                        viewModel.setMaterialFontFamily("Default")
                                        viewModel.setMaterialColorVisionMode(0)
                                        viewModel.setMaterialNavigationMotionMode(0)
                                        viewModel.setMaterialInvertColors(false)
                                        viewModel.setMaterialShapeStyle(0)
                                        viewModel.setMaterialIconShape(0)
                                        viewModel.setMaterialSwitchStyle(0)
                                        viewModel.setMaterialSliderStyle(1)
                                        viewModel.setMaterialLaunchFollowsTheme(true)
                                        viewModel.setEnableFloatingBottomBar(false)
                                        viewModel.setEnableFloatingBottomBarBlur(false)
                                        viewModel.setMaterialShadowContainers(true)
                                        viewModel.setMaterialShadowAppBars(true)
                                        viewModel.setMaterialShadowButtons(true)
                                        viewModel.setMaterialShadowFab(true)
                                        viewModel.setMaterialShadowSwitches(true)
                                        viewModel.setMaterialShadowSliders(true)
                                    }
                                }
                            }
                    }
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
        VoyageThemeCategory.PALETTE -> Icons.Outlined.Palette
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(category.titleRes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
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

@Composable
private fun ThemeStyleChoice(
    title: String,
    summary: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable private fun HctSlider(id: Int, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id), modifier = Modifier.weight(1f))
            Text(value.toInt().toString(), color = MaterialTheme.colorScheme.primary)
        }
        VoyageSlider(value.coerceIn(range.start, range.endInclusive), onChange, valueRange = range)
      }
    }
}

@Composable private fun Header(id: Int) { Text(stringResource(id), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp)) }
@Composable private fun Choice(id: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            headlineContent = { Text(stringResource(id)) },
            trailingContent = { RadioButton(selected, onClick) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
@Composable private fun Toggle(
    id: Int,
    summary: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ListItem(
            modifier = Modifier.clickable(enabled = enabled) { onChange(!checked) },
            headlineContent = { Text(stringResource(id)) },
            supportingContent = summary?.let { { Text(it) } },
            trailingContent = {
                ExpressiveSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
@Composable private fun Slide(id: Int, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
      Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            val valueText = if (id == R.string.voyage_theme_corner_scale) {
                "${String.format("%.1f", value).removeSuffix(".0")}×"
            } else {
                "${String.format("%.1f", value).removeSuffix(".0")} dp"
            }
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(valueText, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        VoyageSlider(value.coerceIn(range.start, range.endInclusive), onChange, valueRange = range)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun Drop(id: Int, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(id), style = MaterialTheme.typography.titleMedium)
            FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val active = option == selected
                FilledTonalButton(
                    onClick = { onSelect(option) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) { Text(option, style = MaterialTheme.typography.labelLarge) }
            }
            }
        }
    }
}
@Composable
private fun ThemeResetCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.voyage_theme_reset_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(stringResource(R.string.voyage_theme_reset_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.72f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IconShapeGallery(
    selectedShape: Int,
    title: String,
    randomDescription: String,
    onSelect: (Int) -> Unit,
) {
    val polygons = listOf(
        MaterialShapes.Square, MaterialShapes.Slanted, MaterialShapes.Arch, MaterialShapes.Fan, MaterialShapes.Arrow,
        MaterialShapes.SemiCircle, MaterialShapes.Oval, MaterialShapes.Pill, MaterialShapes.Triangle, MaterialShapes.Diamond,
        MaterialShapes.ClamShell, MaterialShapes.Pentagon, MaterialShapes.Gem, MaterialShapes.VerySunny, MaterialShapes.Sunny,
        MaterialShapes.Cookie4Sided, MaterialShapes.Cookie6Sided, MaterialShapes.Cookie7Sided, MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided,
        MaterialShapes.Ghostish, MaterialShapes.Clover4Leaf, MaterialShapes.Clover8Leaf, MaterialShapes.Burst, MaterialShapes.SoftBurst,
        MaterialShapes.Boom, MaterialShapes.SoftBoom, MaterialShapes.Flower, MaterialShapes.Puffy, MaterialShapes.PuffyDiamond,
        MaterialShapes.PixelCircle, MaterialShapes.PixelTriangle, MaterialShapes.Bun, MaterialShapes.Heart,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = 5,
        ) {
            Surface(
                modifier = Modifier.size(40.dp).clickable { onSelect(0) },
                shape = MaterialShapes.Cookie12Sided.toShape(),
                color = if (selectedShape == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 1.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                    ) {}
                }
            }
            polygons.forEachIndexed { index, polygon ->
                val storedIndex = index + 1
                Surface(
                    modifier = Modifier.size(40.dp).clickable { onSelect(storedIndex) },
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    color = if (selectedShape == storedIndex) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 1.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = polygon.toShape(),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {}
                    }
                }
            }
            Surface(
                modifier = Modifier.size(40.dp).clickable {
                    onSelect(kotlin.random.Random.nextInt(1, polygons.size + 1))
                },
                shape = MaterialShapes.Cookie12Sided.toShape(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 1.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Shuffle, contentDescription = randomDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorPaletteGrid(
    colors: List<Int>,
    selectedColor: Int,
    dynamicSelected: Boolean,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4,
    ) {
        colors.forEachIndexed { index, value ->
            ColorChoice(
                color = Color(value),
                selected = if (dynamicSelected) index == 0 else value == selectedColor,
                label = null,
                onClick = { onSelect(value) },
            )
        }
        Surface(
            modifier = Modifier.size(52.dp).clickable(onClick = onAdd),
            shape = MaterialShapes.Cookie12Sided.toShape(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.voyage_theme_color_editor), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable private fun ColorChoice(color: Color, selected: Boolean, label: String?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(52.dp).clickable(onClick = onClick),
            shape = MaterialShapes.Cookie12Sided.toShape(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(Modifier.size(40.dp), shape = CircleShape, color = color) {}
                if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = if (color.luminance() < 0.5f) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
        if (label != null) Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
