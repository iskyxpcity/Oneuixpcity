package com.alohaoneui.app.ui.screen.settings

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.AlohaOneUIApplication
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.theme.ColorMode
import com.alohaoneui.app.ui.theme.VoyageSettingsTheme
import com.alohaoneui.app.ui.theme.keyColorOptions
import com.alohaoneui.app.ui.util.AppLocaleHelper
import com.alohaoneui.app.ui.viewmodel.SettingsViewModel
import com.materialkolor.PaletteStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    var languageExpanded by rememberSaveable { mutableStateOf(true) }
    var themeExpanded by rememberSaveable { mutableStateOf(true) }
    var displayExpanded by rememberSaveable { mutableStateOf(true) }
    var motionExpanded by rememberSaveable { mutableStateOf(true) }

    val languageLabels = listOf(
        stringResource(R.string.settings_language_system),
        stringResource(R.string.settings_language_chinese_simplified),
        stringResource(R.string.settings_language_chinese_traditional),
        stringResource(R.string.settings_language_english),
        stringResource(R.string.settings_language_japanese),
        stringResource(R.string.settings_language_korean),
    )
    val languageTags = listOf("system", "zh-CN", "zh-TW", "en", "ja", "ko")
    val selectedLanguageIndex = languageTags.indexOf(uiState.appLanguage).coerceAtLeast(0)
    val transitionLabels = listOf(
        R.string.settings_transition_aosp,
        R.string.settings_transition_miuix,
        R.string.settings_transition_scale,
        R.string.settings_transition_classic,
    )
    val paletteStyle = runCatching { PaletteStyle.valueOf(uiState.colorStyle) }
        .getOrDefault(PaletteStyle.Expressive)
    val colorMode = ColorMode.fromValue(uiState.themeMode)

    VoyageSettingsTheme(uiState) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                LargeFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = actions.onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    title = { Text(stringResource(R.string.settings)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
        ) { padding ->
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                val wide = maxWidth >= 840.dp
                val medium = maxWidth >= 600.dp
                val sectionModifier = Modifier.padding(horizontal = if (wide) 10.dp else 16.dp)

                val language: @Composable ColumnScope.() -> Unit = {
                    VoyageSettingsGroup(
                        title = stringResource(R.string.settings_group_language),
                        icon = Icons.Filled.Language,
                        expanded = languageExpanded,
                        onToggle = { languageExpanded = !languageExpanded },
                        modifier = sectionModifier,
                    ) {
                        languageLabels.forEachIndexed { index, label ->
                            VoyageSettingCard {
                                VoyageSettingRow(
                                    title = label,
                                    icon = Icons.Filled.Language,
                                    onClick = {
                                        viewModel.setAppLanguage(languageTags[index])
                                        AppLocaleHelper.apply(context, languageTags[index])
                                        activity?.recreate()
                                    },
                                    trailing = {
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
                    }
                }

                val theme: @Composable ColumnScope.() -> Unit = {
                    VoyageSettingsGroup(
                        title = stringResource(R.string.settings_theme),
                        icon = Icons.Filled.Palette,
                        expanded = themeExpanded,
                        onToggle = { themeExpanded = !themeExpanded },
                        modifier = sectionModifier,
                    ) {
                        VoyageSettingCard {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Wallpaper,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.voyage_theme_wallpaper_colors),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    ).forEachIndexed { index, color ->
                                        Surface(
                                            modifier = Modifier.weight(if (index == 0) 1.4f else 0.9f)
                                                .height(52.dp)
                                                .clickable {
                                                    viewModel.setKeyColor(
                                                        if (index == 0) 0
                                                        else keyColorOptions.getOrElse(index - 1) { 0 },
                                                    )
                                                },
                                            shape = RoundedCornerShape(18.dp),
                                            color = color,
                                            tonalElevation = 1.dp,
                                        ) {}
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    VoyageChoiceChip(
                                        label = stringResource(R.string.settings_motion_expressive),
                                        selected = paletteStyle == PaletteStyle.Expressive,
                                        onClick = { viewModel.setColorStyle(PaletteStyle.Expressive.name) },
                                    )
                                    VoyageChoiceChip(
                                        label = stringResource(R.string.voyage_theme_follow_system),
                                        selected = colorMode.isSystem,
                                        onClick = { actions.onSetThemeMode(0) },
                                    )
                                }
                            }
                        }

                        VoyageChoicePanel(
                            title = stringResource(R.string.voyage_theme_group_design),
                            options = listOf(
                                stringResource(R.string.theme_design_material3),
                                stringResource(R.string.theme_design_miuix),
                            ),
                            selectedIndex = if (uiState.uiMode == UiMode.Material.value) 0 else 1,
                            onSelected = actions.onSetUiModeIndex,
                        )

                        VoyageChoicePanel(
                            title = stringResource(R.string.voyage_theme_group_mode),
                            options = listOf(
                                stringResource(R.string.voyage_theme_follow_system),
                                stringResource(R.string.settings_theme_mode_light),
                                stringResource(R.string.settings_theme_mode_dark),
                            ),
                            selectedIndex = when {
                                colorMode.isSystem -> 0
                                colorMode.isDark -> 2
                                else -> 1
                            },
                            onSelected = actions.onSetThemeMode,
                        )

                        VoyageSettingCard {
                            VoyageSettingRow(
                                title = stringResource(R.string.voyage_theme_dynamic_color),
                                summary = stringResource(R.string.voyage_theme_dynamic_color_summary),
                                icon = Icons.Filled.Wallpaper,
                                trailing = {
                                    Switch(
                                        checked = uiState.materialDynamicColorEnabled,
                                        onCheckedChange = viewModel::setMaterialDynamicColorEnabled,
                                    )
                                },
                            )
                        }
                        VoyageSettingCard {
                            VoyageSettingRow(
                                title = stringResource(R.string.voyage_theme_page_title),
                                summary = stringResource(R.string.voyage_theme_page_summary),
                                icon = Icons.Filled.Palette,
                                onClick = actions.onOpenTheme,
                                trailing = {
                                    Icon(
                                        Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.graphicsLayer { rotationZ = -90f },
                                    )
                                },
                            )
                        }
                    }
                }

                val display: @Composable ColumnScope.() -> Unit = {
                    VoyageSettingsGroup(
                        title = stringResource(R.string.settings_group_display),
                        icon = Icons.Filled.TextFields,
                        expanded = displayExpanded,
                        onToggle = { displayExpanded = !displayExpanded },
                        modifier = sectionModifier,
                    ) {
                        VoyageSettingCard {
                            VoyageSettingRow(
                                title = stringResource(R.string.voyage_toolbox_title),
                                summary = stringResource(R.string.voyage_toolbox_subtitle),
                                icon = Icons.Filled.TextFields,
                            )
                        }
                        VoyageSettingCard {
                            VoyageSettingRow(
                                title = stringResource(R.string.voyage_theme_large_text),
                                summary = stringResource(R.string.voyage_theme_large_text_summary),
                                icon = Icons.Filled.TextFields,
                                trailing = {
                                    Switch(
                                        checked = uiState.materialLargeTextEnabled,
                                        onCheckedChange = viewModel::setMaterialLargeTextEnabled,
                                    )
                                },
                            )
                        }
                        VoyageSliderCard(
                            title = stringResource(R.string.settings_font_scale_title),
                            icon = Icons.Filled.FontDownload,
                            value = uiState.materialFontScale,
                            valueRange = 0.8f..1.5f,
                            steps = 13,
                            onValueChange = viewModel::setMaterialFontScale,
                        )
                        VoyageSliderCard(
                            title = stringResource(R.string.settings_display_scale_title),
                            icon = Icons.Filled.ZoomIn,
                            value = uiState.materialDisplayScale,
                            valueRange = 0.85f..1.25f,
                            steps = 7,
                            onValueChange = viewModel::setMaterialDisplayScale,
                        )
                        VoyageSettingCard {
                            VoyageSettingRow(
                                title = stringResource(R.string.settings_bold_text_title),
                                icon = Icons.Filled.FormatBold,
                                trailing = {
                                    Switch(
                                        checked = uiState.materialBoldTextEnabled,
                                        onCheckedChange = viewModel::setMaterialBoldTextEnabled,
                                    )
                                },
                            )
                        }
                        VoyageChoicePanel(
                            title = stringResource(R.string.voyage_theme_font_family),
                            options = listOf(
                                stringResource(R.string.voyage_font_default),
                                stringResource(R.string.voyage_font_serif),
                            ),
                            selectedIndex = if (uiState.materialFontFamily == "Serif") 1 else 0,
                            onSelected = {
                                viewModel.setMaterialFontFamily(if (it == 1) "Serif" else "Default")
                            },
                        )
                        VoyageSettingCard {
                            VoyageSettingRow(
                                title = stringResource(R.string.settings_display_restore_defaults),
                                icon = Icons.Filled.DisplaySettings,
                                onClick = {
                                    viewModel.setMaterialFontScale(1f)
                                    viewModel.setMaterialDisplayScale(1f)
                                    viewModel.setMaterialFontFamily("Default")
                                    viewModel.setMaterialLargeTextEnabled(false)
                                    viewModel.setMaterialBoldTextEnabled(false)
                                },
                            )
                        }
                    }
                }

                val motion: @Composable ColumnScope.() -> Unit = {
                    VoyageSettingsGroup(
                        title = stringResource(R.string.settings_group_behavior),
                        icon = Icons.Filled.Animation,
                        expanded = motionExpanded,
                        onToggle = { motionExpanded = !motionExpanded },
                        modifier = sectionModifier,
                    ) {
                        VoyageChoicePanel(
                            title = stringResource(R.string.settings_motion_style),
                            options = listOf(
                                stringResource(R.string.settings_motion_expressive),
                                stringResource(R.string.settings_motion_simplified),
                            ),
                            selectedIndex = uiState.materialNavigationMotionMode.coerceIn(0, 1),
                            onSelected = viewModel::setMaterialNavigationMotionMode,
                        )
                        VoyageChoicePanel(
                            title = stringResource(R.string.settings_transition_style),
                            options = transitionLabels.map { stringResource(it) },
                            selectedIndex = uiState.materialTransitionStyle.coerceIn(0, 3),
                            onSelected = viewModel::setMaterialTransitionStyle,
                        )
                        VoyageSettingCard {
                            VoyageSettingRow(
                                title = stringResource(R.string.settings_enable_predictive_back),
                                summary = stringResource(R.string.settings_enable_predictive_back_summary),
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                trailing = {
                                    Switch(
                                        checked = uiState.enablePredictiveBack,
                                        onCheckedChange = { enabled ->
                                            viewModel.setEnablePredictiveBack(enabled)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                AlohaOneUIApplication.applyEnableOnBackInvokedCallback(context, enabled)
                                                activity?.recreate()
                                            }
                                        },
                                    )
                                },
                            )
                        }
                        VoyageSettingCard {
                            VoyageSettingRow(
                                title = stringResource(R.string.settings_haptic_feedback),
                                summary = stringResource(R.string.settings_haptic_feedback_summary),
                                icon = Icons.Filled.Vibration,
                                trailing = {
                                    Switch(
                                        checked = uiState.enableHapticFeedback,
                                        onCheckedChange = viewModel::setEnableHapticFeedback,
                                    )
                                },
                            )
                        }
                    }
                }

                when {
                    wide -> Row(
                        Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            language()
                            motion()
                            Spacer(Modifier.height(bottomInnerPadding))
                        }
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            theme()
                            Spacer(Modifier.height(bottomInnerPadding))
                        }
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            display()
                            Spacer(Modifier.height(bottomInnerPadding))
                        }
                    }
                    medium -> Row(
                        Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            language()
                            motion()
                            Spacer(Modifier.height(bottomInnerPadding))
                        }
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            theme()
                            display()
                            Spacer(Modifier.height(bottomInnerPadding))
                        }
                    }
                    else -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        language()
                        theme()
                        display()
                        motion()
                        Spacer(Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
    }
}

@Composable
private fun VoyageSettingsGroup(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f },
            )
        }
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun VoyageSettingCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        content = { Column(content = content) },
    )
}

@Composable
private fun VoyageSettingRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun VoyageChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun VoyageChoicePanel(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    VoyageSettingCard {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
            ) {
                options.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedIndex == index,
                        onClick = { onSelected(index) },
                        label = { Text(label) },
                    )
                    if (index < options.lastIndex) Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun VoyageSliderCard(
    title: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    VoyageSettingCard {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        if (kotlin.math.abs(value - 1f) < 0.01f) stringResource(R.string.voyage_theme_contrast_standard)
                        else "\${(value * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Slider(
                value = value.coerceIn(valueRange),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
            )
        }
    }
}
