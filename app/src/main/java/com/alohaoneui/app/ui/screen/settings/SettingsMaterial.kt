package com.alohaoneui.app.ui.screen.settings

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.alohaoneui.app.ui.component.material.SendLogBottomSheet
import com.alohaoneui.app.ui.component.material.ExpressiveSwitch
import com.alohaoneui.app.ui.component.material.VoyageSlider
import com.alohaoneui.app.ui.component.material.SnackBarHost
import com.alohaoneui.app.ui.util.AppLocaleHelper
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.theme.keyColorOptions
import com.materialkolor.PaletteStyle

@Composable
fun SettingPagerMaterial(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val viewModel = viewModel<SettingsViewModel>()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = remember { SnackbarHostState() }
    var showBottomSheet by remember { mutableStateOf(false) }
    var languageExpanded by rememberSaveable { mutableStateOf(false) }
    var themeExpanded by rememberSaveable { mutableStateOf(false) }
    var colorExpanded by rememberSaveable { mutableStateOf(false) }
    var paletteStyleExpanded by rememberSaveable { mutableStateOf(false) }
    var displayExpanded by rememberSaveable { mutableStateOf(false) }
    var behaviorExpanded by rememberSaveable { mutableStateOf(false) }
    var alohaExpanded by rememberSaveable { mutableStateOf(false) }
    var fontMenu by remember { mutableStateOf(false) }
    var gestureMenu by remember { mutableStateOf(false) }
    var pageScaleMenu by remember { mutableStateOf(false) }

    val languageLabels = listOf(
        stringResource(R.string.settings_language_system),
        stringResource(R.string.settings_language_english),
        stringResource(R.string.settings_language_chinese_simplified),
    )
    val languageTags = listOf("system", "en", "zh-CN")
    val selectedLanguageIndex = languageTags.indexOf(uiState.appLanguage).coerceAtLeast(0)
    val fontLabels = listOf(stringResource(R.string.voyage_font_default), stringResource(R.string.voyage_font_serif))
    val gestureLabels = listOf(
        stringResource(R.string.settings_page_gesture_default),
        stringResource(R.string.settings_page_gesture_cross_axis),
        stringResource(R.string.settings_page_gesture_ios_like),
    )
    val motionLabels = listOf(stringResource(R.string.settings_motion_expressive), stringResource(R.string.settings_motion_standard))

    Scaffold(
        topBar = { TopBar(scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackBarHost(hostState = snackBarHost, modifier = Modifier.padding(bottom = bottomInnerPadding)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            VoyageSettingsAccordion(
                title = stringResource(R.string.settings_group_language),
                icon = Icons.Outlined.Translate,
                expanded = languageExpanded,
                onToggle = { languageExpanded = !languageExpanded },
            ) {
                languageLabels.forEachIndexed { index, label ->
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.setAppLanguage(languageTags[index])
                            AppLocaleHelper.apply(context, languageTags[index])
                            activity?.recreate()
                        },
                        headlineContent = { Text(label) },
                        trailingContent = {
                            RadioButton(
                                selected = selectedLanguageIndex == index,
                                onClick = {
                                    viewModel.setAppLanguage(languageTags[index])
                                    AppLocaleHelper.apply(context, languageTags[index])
                                    activity?.recreate()
                                },
                            )
                        },
                    )
                }
            }
            VoyageSettingsAccordion(
                title = stringResource(R.string.settings_theme),
                icon = Icons.Filled.Palette,
                expanded = themeExpanded,
                onToggle = { themeExpanded = !themeExpanded },
            ) {
                SettingsThemeStyleChoice(
                    title = stringResource(R.string.theme_design_material3),
                    summary = stringResource(R.string.theme_design_material3_subtitle),
                    selected = uiState.uiMode == UiMode.Material.value,
                    icon = Icons.Filled.Palette,
                    onClick = { actions.onSetUiModeIndex(1) },
                )
                SettingsThemeStyleChoice(
                    title = stringResource(R.string.theme_design_miuix),
                    summary = stringResource(R.string.theme_design_miuix_subtitle),
                    selected = uiState.uiMode == UiMode.Miuix.value,
                    icon = Icons.Filled.Widgets,
                    onClick = { actions.onSetUiModeIndex(0) },
                )
                ListItem(
                    modifier = Modifier.clickable(onClick = actions.onOpenTheme),
                    headlineContent = { Text(stringResource(R.string.voyage_theme_page_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_theme_summary)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                )
            }
            VoyageSettingsAccordion(
                title = stringResource(R.string.voyage_settings_group_color),
                icon = Icons.Filled.Palette,
                expanded = colorExpanded,
                onToggle = { colorExpanded = !colorExpanded },
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.voyage_theme_dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.voyage_theme_dynamic_color_summary)) },
                    trailingContent = { Switch(uiState.materialDynamicColorEnabled, viewModel::setMaterialDynamicColorEnabled) },
                )
                Text(stringResource(R.string.voyage_theme_key_color), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SettingsColorChoice(Color(0xFF6750A4), uiState.keyColor == 0, stringResource(R.string.voyage_theme_system_color)) { viewModel.setKeyColor(0) }
                    keyColorOptions.forEach { color ->
                        SettingsColorChoice(Color(color), uiState.keyColor == color, null) { viewModel.setKeyColor(color) }
                    }
                }
                val paletteStyles = PaletteStyle.entries.map { it.name }
                ListItem(
                    modifier = Modifier.clickable { paletteStyleExpanded = true },
                    headlineContent = { Text(stringResource(R.string.settings_color_style)) },
                    supportingContent = { Text(uiState.colorStyle) },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { paletteStyleExpanded = true }) { Text(uiState.colorStyle) }
                            DropdownMenu(expanded = paletteStyleExpanded, onDismissRequest = { paletteStyleExpanded = false }) {
                                paletteStyles.forEach { style ->
                                    DropdownMenuItem(text = { Text(style) }, onClick = { viewModel.setColorStyle(style); paletteStyleExpanded = false })
                                }
                            }
                        }
                    },
                )
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.voyage_theme_contrast), modifier = Modifier.weight(1f))
                        Text(String.format("%.1f", uiState.materialContrastLevel), color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(uiState.materialContrastLevel.coerceIn(-1f, 1f), viewModel::setMaterialContrastLevel, valueRange = -1f..1f)
                }
                ListItem(
                    modifier = Modifier.clickable(onClick = actions.onOpenColorPalette),
                    headlineContent = { Text(stringResource(R.string.voyage_theme_color_editor)) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                )
            }
            VoyageSettingsAccordion(
                title = stringResource(R.string.settings_group_display),
                icon = Icons.Outlined.TextFields,
                expanded = displayExpanded,
                onToggle = { displayExpanded = !displayExpanded },
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_font_scale_title), modifier = Modifier.weight(1f))
                    Text((uiState.materialFontScale * 100).toInt().toString() + "%")
                }
                VoyageSlider(uiState.materialFontScale, viewModel::setMaterialFontScale, valueRange = 0.8f..1.5f, steps = 13, modifier = Modifier.padding(horizontal = 12.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_display_scale_title), modifier = Modifier.weight(1f))
                    Text((uiState.materialDisplayScale * 100).toInt().toString() + "%")
                }
                VoyageSlider(uiState.materialDisplayScale, viewModel::setMaterialDisplayScale, valueRange = 0.85f..1.25f, steps = 7, modifier = Modifier.padding(horizontal = 12.dp))
                Text(stringResource(R.string.settings_display_preview_sample), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
                ListItem(
                    modifier = Modifier.clickable { fontMenu = true },
                    headlineContent = { Text(stringResource(R.string.voyage_theme_font_family)) },
                    supportingContent = { Text(fontLabels[if (uiState.materialFontFamily == "Serif") 1 else 0]) },
                    trailingContent = { TextButton(onClick = { fontMenu = true }) { Text(fontLabels[if (uiState.materialFontFamily == "Serif") 1 else 0]) } },
                )
                DropdownMenu(fontMenu, { fontMenu = false }) {
                    fontLabels.forEachIndexed { index, label ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            viewModel.setMaterialFontFamily(if (index == 1) "Serif" else "Default")
                            fontMenu = false
                        })
                    }
                }
                ListItem(headlineContent = { Text(stringResource(R.string.voyage_theme_large_text)) }, trailingContent = { ExpressiveSwitch(uiState.materialLargeTextEnabled, viewModel::setMaterialLargeTextEnabled) })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_bold_text_title)) }, trailingContent = { ExpressiveSwitch(uiState.materialBoldTextEnabled, viewModel::setMaterialBoldTextEnabled) })
            }
            VoyageSettingsAccordion(
                title = stringResource(R.string.settings_group_behavior),
                icon = Icons.Outlined.Animation,
                expanded = behaviorExpanded,
                onToggle = { behaviorExpanded = !behaviorExpanded },
            ) {
                Choice(R.string.settings_motion_expressive, uiState.materialNavigationMotionMode == 0) { viewModel.setMaterialNavigationMotionMode(0) }
                Choice(R.string.settings_motion_standard, uiState.materialNavigationMotionMode == 1) { viewModel.setMaterialNavigationMotionMode(1) }
                ListItem(headlineContent = { Text(stringResource(R.string.settings_haptic_feedback)) }, trailingContent = { ExpressiveSwitch(uiState.enableHapticFeedback, viewModel::setEnableHapticFeedback) })
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_enable_predictive_back)) },
                    supportingContent = { Text(stringResource(R.string.settings_enable_predictive_back_summary)) },
                    trailingContent = { ExpressiveSwitch(uiState.enablePredictiveBack, { enabled ->
                        viewModel.setEnablePredictiveBack(enabled)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            AlohaOneUIApplication.applyEnableOnBackInvokedCallback(context, enabled)
                            activity?.recreate()
                        }
                    }) },
                )
                ListItem(headlineContent = { Text(stringResource(R.string.settings_enable_swipe_back)) }, supportingContent = { Text(stringResource(R.string.settings_enable_swipe_back_summary)) }, trailingContent = { ExpressiveSwitch(uiState.enableSwipeBack, viewModel::setEnableSwipeBack) })
                ListItem(
                    modifier = Modifier.clickable { gestureMenu = true },
                    headlineContent = { Text(stringResource(R.string.settings_page_gesture_mode)) },
                    supportingContent = { Text(gestureLabels[uiState.pagerGestureMode.coerceIn(0, 2)]) },
                )
                DropdownMenu(gestureMenu, { gestureMenu = false }) {
                    gestureLabels.forEachIndexed { index, label -> DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setPagerGestureMode(index); gestureMenu = false }) }
                }
                ListItem(
                    modifier = Modifier.clickable { pageScaleMenu = true },
                    headlineContent = { Text(stringResource(R.string.settings_page_scale)) },
                    supportingContent = { Text((uiState.pageScale * 100).toInt().toString() + "%") },
                )
                DropdownMenu(pageScaleMenu, { pageScaleMenu = false }) {
                    listOf(80, 90, 100, 110, 120).forEach { percent ->
                        DropdownMenuItem(text = { Text(percent.toString() + "%") }, onClick = { viewModel.setPageScale(percent / 100f); pageScaleMenu = false })
                    }
                }
            }
            VoyageSettingsAccordion(
                title = stringResource(R.string.voyage_settings_group_aloha),
                icon = Icons.Filled.Dashboard,
                expanded = alohaExpanded,
                onToggle = { alohaExpanded = !alohaExpanded },
            ) {
                ListItem(headlineContent = { Text(stringResource(R.string.settings_check_update)) }, supportingContent = { Text(stringResource(R.string.settings_check_update_summary)) }, trailingContent = { ExpressiveSwitch(uiState.checkUpdate, actions.onSetCheckUpdate) })
                ListItem(modifier = Modifier.clickable { showBottomSheet = true }, headlineContent = { Text(stringResource(R.string.send_log)) }, trailingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) })
                ListItem(modifier = Modifier.clickable(onClick = actions.onOpenAbout), headlineContent = { Text(stringResource(R.string.about)) }, trailingContent = { Icon(Icons.Filled.Info, contentDescription = null) })
            }
            Spacer(Modifier.height(bottomInnerPadding))
            if (showBottomSheet) {
                SendLogBottomSheet(onDismiss = { showBottomSheet = false }, snackbarHostState = snackBarHost)
            }
        }
    }
}

@Composable
private fun VoyageSettingsAccordion(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f })
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(content = content)
            }
        }
    }
}

@Composable
private fun Choice(id: Int, selected: Boolean, onClick: () -> Unit) {
    ListItem(modifier = Modifier.clickable(onClick = onClick), headlineContent = { Text(stringResource(id)) }, trailingContent = { RadioButton(selected, onClick) })
}

@Composable
private fun TopBar(scrollBehavior: TopAppBarScrollBehavior? = null) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior,
    )
}


@Composable
private fun SettingsThemeStyleChoice(
    title: String,
    summary: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.large, color = container) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun SettingsColorChoice(color: Color, selected: Boolean, label: String?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(if (selected) 40.dp else 34.dp).clickable(onClick = onClick),
            shape = CircleShape,
            color = color,
            border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        ) {}
        if (label != null) Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
