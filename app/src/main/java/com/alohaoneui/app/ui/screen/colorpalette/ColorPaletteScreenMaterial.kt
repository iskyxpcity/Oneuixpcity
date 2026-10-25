package com.alohaoneui.app.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.materialkolor.PaletteStyle
import com.materialkolor.hct.Hct
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.component.bottombar.useNavigationRail
import com.alohaoneui.app.ui.component.material.ExpressiveToggleButton
import com.alohaoneui.app.ui.component.material.SegmentedColumn
import com.alohaoneui.app.ui.component.material.SegmentedDropdownItem
import com.alohaoneui.app.ui.component.material.SegmentedSwitchItem
import com.alohaoneui.app.ui.component.material.TonalCard
import com.alohaoneui.app.ui.theme.ColorMode
import com.alohaoneui.app.ui.theme.LocalMaterialShadowAppBars
import androidx.compose.ui.draw.shadow
import com.alohaoneui.app.ui.theme.rememberAlohaOneUIColorScheme
import com.alohaoneui.app.ui.theme.keyColorOptions

@Composable
fun ColorPaletteScreenMaterial(
    state: ColorPaletteUiState,
    actions: ColorPaletteScreenActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState = state.uiState
    val currentColorMode = state.currentColorMode
    val currentKeyColor = uiState.keyColor
    val colorStyle = state.currentPaletteStyle
    val colorSpec = state.currentColorSpec
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val extractedColor = withContext(Dispatchers.IO) {
                    runCatching { extractAverageSeedColor(context, uri) }.getOrNull()
                }
                extractedColor?.let(actions.onSetKeyColor)
            }
        }
    }

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = if (LocalMaterialShadowAppBars.current) Modifier.shadow(2.dp) else Modifier,
                navigationIcon = {
                    IconButton(
                        onClick = actions.onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                title = { Text(stringResource(R.string.settings_theme)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val captionBar = WindowInsets.captionBar.asPaddingValues()

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaterialThemeGroup(
                title = stringResource(R.string.theme_group_design),
                icon = Icons.Filled.Palette,
            ) {
            val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()
            val isAmoled = currentColorMode.isAmoled
            ThemePreviewCard(
                keyColor = currentKeyColor,
                isDark = isDark,
                isAmoled = isAmoled,
                paletteStyle = colorStyle,
                colorSpec = colorSpec,
                contrastLevel = uiState.materialContrastLevel,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ColorButtonMaterial(
                        color = Color.Unspecified,
                        isSelected = currentKeyColor == 0,
                        isDark = isDark,
                        isAmoled = isAmoled,
                        paletteStyle = colorStyle,
                        colorSpec = colorSpec,
                        contrastLevel = uiState.materialContrastLevel,
                        onClick = {
                            actions.onSetKeyColor(0)
                        }
                    )
                }

                items(keyColorOptions) { color ->
                    ColorButtonMaterial(
                        color = Color(color),
                        isSelected = currentKeyColor == color,
                        isDark = isDark,
                        isAmoled = isAmoled,
                        paletteStyle = colorStyle,
                        colorSpec = colorSpec,
                        contrastLevel = uiState.materialContrastLevel,
                        onClick = {
                            actions.onSetKeyColor(color)
                        }
                    )
                }
            }

            var seedHex by remember(currentKeyColor) {
                mutableStateOf(
                    if (currentKeyColor == 0) "" else
                        (currentKeyColor and 0x00FFFFFF).toString(16).uppercase().padStart(6, '0')
                )
            }
            val parsedSeedColor = seedHex.trim().removePrefix("#").takeIf { value ->
                value.length == 6 && value.all { it.digitToIntOrNull(16) != null }
            }?.toLongOrNull(16)?.toInt()?.or(0xFF000000.toInt())

            val hctBaseColor = if (currentKeyColor == 0) MaterialTheme.colorScheme.primary.toArgb() else currentKeyColor
            val initialHct = remember(currentKeyColor, hctBaseColor) { Hct.fromInt(hctBaseColor) }
            var hctHue by remember(currentKeyColor, hctBaseColor) { mutableFloatStateOf(initialHct.hue.toFloat()) }
            var hctChroma by remember(currentKeyColor, hctBaseColor) { mutableFloatStateOf(initialHct.chroma.toFloat()) }
            var hctTone by remember(currentKeyColor, hctBaseColor) { mutableFloatStateOf(initialHct.tone.toFloat()) }

            TonalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.theme_editor_hct_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.theme_editor_hue), modifier = Modifier.weight(1f))
                        Text("${hctHue.toInt()}°", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(value = hctHue, onValueChange = { hctHue = it }, valueRange = 0f..360f, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.theme_editor_chroma), modifier = Modifier.weight(1f))
                        Text(hctChroma.toInt().toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(value = hctChroma, onValueChange = { hctChroma = it }, valueRange = 0f..120f, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.theme_editor_tone), modifier = Modifier.weight(1f))
                        Text(hctTone.toInt().toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(value = hctTone, onValueChange = { hctTone = it }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            actions.onSetKeyColor(
                                Hct.from(
                                    hue = hctHue.toDouble(),
                                    chroma = hctChroma.toDouble(),
                                    tone = hctTone.toDouble()
                                ).toInt()
                            )
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.theme_editor_apply_hct))
                    }
                }
            }

            var schemeName by remember { mutableStateOf("") }
            TonalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.theme_saved_schemes_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = schemeName,
                            onValueChange = { schemeName = it.take(40) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.theme_saved_scheme_name)) },
                            singleLine = true,
                        )
                        Button(
                            onClick = {
                                actions.onSaveMaterialColorScheme(schemeName)
                                schemeName = ""
                            },
                            enabled = schemeName.isNotBlank(),
                        ) { Text(stringResource(R.string.theme_saved_scheme_save)) }
                    }
                    if (uiState.materialColorSchemes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.theme_saved_schemes_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    } else {
                        uiState.materialColorSchemes.forEach { scheme ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(scheme.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${scheme.colorStyle} · ${if (scheme.keyColor == 0) stringResource(R.string.theme_saved_scheme_dynamic) else "#${scheme.keyColor.toUInt().toString(16).takeLast(6).uppercase()}"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                Button(onClick = { actions.onApplyMaterialColorScheme(scheme) }) {
                                    Text(stringResource(R.string.theme_saved_scheme_apply))
                                }
                                Button(onClick = { actions.onDeleteMaterialColorScheme(scheme.name) }) {
                                    Text(stringResource(R.string.theme_saved_scheme_delete))
                                }
                            }
                        }
                    }
                }
            }

            TonalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.theme_extract_color_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.theme_extract_color_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.theme_extract_color_action))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = seedHex,
                    onValueChange = { seedHex = it.take(7) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.theme_editor_hex)) },
                    placeholder = { Text("6750A4") },
                    singleLine = true,
                    isError = seedHex.isNotBlank() && parsedSeedColor == null,
                )
                Button(
                    onClick = { parsedSeedColor?.let(actions.onSetKeyColor) },
                    enabled = parsedSeedColor != null,
                ) {
                    Text(stringResource(R.string.theme_editor_apply_hex))
                }
            }


            }

            MaterialThemeGroup(
                title = stringResource(R.string.theme_group_navigation_bar),
                icon = Icons.AutoMirrored.Rounded.MenuOpen,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SegmentedColumn(
                        modifier = Modifier.padding(top = 4.dp),
                        content = listOf(
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.AutoMirrored.Rounded.MenuOpen,
                                    title = stringResource(id = R.string.settings_enable_predictive_back),
                                    summary = stringResource(id = R.string.settings_enable_predictive_back_summary),
                                    checked = uiState.enablePredictiveBack,
                                    onCheckedChange = actions.onSetEnablePredictiveBack
                                )
                            }
                        )
                    )
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Rounded.Swipe,
                                title = stringResource(R.string.settings_enable_swipe_back),
                                summary = stringResource(R.string.settings_enable_swipe_back_summary),
                                checked = uiState.enableSwipeBack,
                                onCheckedChange = actions.onSetEnableSwipeBack,
                            )
                        },
                        {
                            val pagerGestureOptions = listOf(
                                stringResource(R.string.settings_page_gesture_default),
                                stringResource(R.string.settings_page_gesture_cross_axis),
                                stringResource(R.string.settings_page_gesture_ios_like),
                            )
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.ViewCarousel,
                                title = stringResource(R.string.settings_page_gesture_mode),
                                items = pagerGestureOptions,
                                selectedIndex = uiState.pagerGestureMode.coerceIn(0, pagerGestureOptions.lastIndex),
                                onItemSelected = actions.onSetPagerGestureMode,
                            )
                        },
                    ),
                )

                TonalCard(modifier = Modifier.padding(top = 4.dp)) {
                    var sliderValue by remember(uiState.pageScale) { mutableFloatStateOf(uiState.pageScale) }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.AspectRatio,
                                contentDescription = stringResource(id = R.string.settings_page_scale),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_page_scale),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_page_scale_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = "${(sliderValue * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { actions.onSetPageScale(sliderValue) },
                            valueRange = 0.8f..1.1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

            }

            MaterialThemeGroup(
                title = stringResource(R.string.theme_group_mode),
                icon = Icons.Filled.Brightness4,
            ) {
                val selectedModeIndex = when {
                    currentColorMode.isSystem -> 0
                    currentColorMode.isDark -> 2
                    else -> 1
                }
                val themeModeLabels = listOf(
                    stringResource(R.string.settings_theme_mode_system),
                    stringResource(R.string.settings_theme_mode_light),
                    stringResource(R.string.settings_theme_mode_dark),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    themeModeLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedModeIndex == index,
                            onClick = { actions.onSetThemeMode(index) },
                            label = { Text(label) },
                        )
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.Palette,
                                title = stringResource(R.string.theme_dynamic_color_title),
                                summary = stringResource(R.string.theme_dynamic_color_subtitle),
                                checked = uiState.materialDynamicColorEnabled && uiState.keyColor == 0,
                                onCheckedChange = actions.onSetMaterialDynamicColorEnabled,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                icon = Icons.Filled.Brightness1,
                                title = stringResource(R.string.settings_pure_black_title),
                                summary = stringResource(R.string.settings_pure_black_subtitle),
                                checked = currentColorMode.isAmoled,
                                onCheckedChange = { enabled ->
                                    actions.onSetColorMode(if (enabled) ColorMode.DARK_AMOLED else ColorMode.DARK)
                                },
                            )
                        },
                        {
                            val styles = PaletteStyle.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.settings_color_style),
                                items = styles.map { style ->
                                    stringResource(
                                        id = when (style.name) {
                                            "Content" -> R.string.settings_palette_style_content
                                            "Expressive" -> R.string.settings_palette_style_expressive
                                            "Fidelity" -> R.string.settings_palette_style_fidelity
                                            "FruitSalad" -> R.string.settings_palette_style_fruit_salad
                                            "Monochrome" -> R.string.settings_palette_style_monochrome
                                            "Neutral" -> R.string.settings_palette_style_neutral
                                            "Rainbow" -> R.string.settings_palette_style_rainbow
                                            "TonalSpot" -> R.string.settings_palette_style_tonal_spot
                                            "Vibrant" -> R.string.settings_palette_style_vibrant
                                            else -> R.string.settings_palette_style_tonal_spot
                                        }
                                    )
                                },
                                selectedIndex = styles.indexOf(colorStyle),
                                onItemSelected = { index ->
                                    actions.onSetColorStyle(styles[index].name)
                                }
                            )
                        },
                    )
                )

                TonalCard(modifier = Modifier.padding(top = 4.dp)) {
                    var contrastLevel by remember(uiState.materialContrastLevel) { mutableFloatStateOf(uiState.materialContrastLevel) }
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.settings_material_contrast), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = if (kotlin.math.abs(contrastLevel) < 0.01f) stringResource(R.string.settings_contrast_normal) else String.format(java.util.Locale.ROOT, "%+.1f", contrastLevel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(text = String.format(java.util.Locale.ROOT, "%+.1f", contrastLevel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(value = contrastLevel, onValueChange = { contrastLevel = it }, onValueChangeFinished = { actions.onSetMaterialContrastLevel(contrastLevel) }, valueRange = -1.0f..1.0f, modifier = Modifier.fillMaxWidth())
                    }
                }

                TonalCard(modifier = Modifier.padding(top = 4.dp)) {
                    var fontScale by remember(uiState.materialFontScale) { mutableFloatStateOf(uiState.materialFontScale) }
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(R.string.settings_material_font_scale), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = stringResource(R.string.settings_material_font_scale_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(text = "${(fontScale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(value = fontScale, onValueChange = { fontScale = it }, onValueChangeFinished = { actions.onSetMaterialFontScale(fontScale) }, valueRange = 0.8f..1.5f, modifier = Modifier.fillMaxWidth())
                    }
                }


            }

            MaterialThemeGroup(
                title = stringResource(R.string.theme_group_assist),
                icon = Icons.Rounded.DesignServices,
            ) {
                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val colorVisionLabels = listOf(
                                stringResource(R.string.settings_material_color_vision_off),
                                stringResource(R.string.settings_material_color_vision_grayscale),
                                stringResource(R.string.settings_material_color_vision_protanopia),
                                stringResource(R.string.settings_material_color_vision_deuteranopia),
                                stringResource(R.string.settings_material_color_vision_tritanopia),
                            )
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.DesignServices,
                                title = stringResource(R.string.settings_material_color_vision),
                                summary = stringResource(R.string.settings_material_color_vision_summary),
                                items = colorVisionLabels,
                                selectedIndex = uiState.materialColorVisionMode.coerceIn(0, colorVisionLabels.lastIndex),
                                onItemSelected = actions.onSetMaterialColorVisionMode,
                            )
                        }
                    ),
                )


            }

            MaterialThemeGroup(
                title = stringResource(R.string.theme_group_shape),
                icon = Icons.Rounded.Style,
            ) {
                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val shapeLabels = listOf(
                                stringResource(R.string.theme_shape_rounded),
                                stringResource(R.string.theme_shape_cut),
                                stringResource(R.string.theme_shape_squircle),
                                stringResource(R.string.theme_shape_smooth),
                                stringResource(R.string.theme_shape_wavy),
                                stringResource(R.string.theme_shape_scoop),
                                stringResource(R.string.theme_shape_notch),
                            )
                            SegmentedDropdownItem(
                                icon = Icons.Filled.Tune,
                                title = stringResource(R.string.theme_shape_style_title),
                                items = shapeLabels,
                                selectedIndex = uiState.materialShapeStyle.coerceIn(0, shapeLabels.lastIndex),
                                onItemSelected = actions.onSetMaterialShapeStyle,
                            )
                        },
                    ),
                )

                TonalCard(modifier = Modifier.padding(top = 4.dp)) {
                    var cornerScale by remember(uiState.materialCornerScale) { mutableFloatStateOf(uiState.materialCornerScale) }
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.settings_material_corner_scale), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(text = "${(cornerScale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(value = cornerScale, onValueChange = { cornerScale = it }, onValueChangeFinished = { actions.onSetMaterialCornerScale(cornerScale) }, valueRange = 0.5f..1.5f, modifier = Modifier.fillMaxWidth())
                    }
                }


            }

            MaterialThemeGroup(
                title = stringResource(R.string.theme_group_components),
                icon = Icons.Filled.Tune,
            ) {
                TonalCard(modifier = Modifier.padding(top = 4.dp)) {
                    var borderWidth by remember(uiState.materialBorderWidth) { mutableFloatStateOf(uiState.materialBorderWidth) }
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.theme_component_border_width), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(text = String.format(java.util.Locale.ROOT, "%.1f dp", borderWidth), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(value = borderWidth, onValueChange = { borderWidth = it }, onValueChangeFinished = { actions.onSetMaterialBorderWidth(borderWidth) }, valueRange = 0.0f..3.0f, steps = 5, modifier = Modifier.fillMaxWidth())
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val switchLabels = listOf(
                                stringResource(R.string.theme_switch_material_you),
                                stringResource(R.string.theme_switch_compose),
                                stringResource(R.string.theme_switch_pixel),
                                stringResource(R.string.theme_switch_fluent),
                                stringResource(R.string.theme_switch_cupertino),
                                stringResource(R.string.theme_switch_liquid_glass),
                                stringResource(R.string.theme_switch_hyper_os),
                                stringResource(R.string.theme_switch_one_ui),
                            )
                            SegmentedDropdownItem(
                                icon = Icons.Filled.Tune,
                                title = stringResource(R.string.theme_switch_style_title),
                                items = switchLabels,
                                selectedIndex = uiState.materialSwitchStyle.coerceIn(0, switchLabels.lastIndex),
                                onItemSelected = actions.onSetMaterialSwitchStyle,
                            )
                        },
                        {
                            val sliderLabels = listOf(
                                stringResource(R.string.theme_slider_material_you),
                                stringResource(R.string.theme_slider_fancy),
                                stringResource(R.string.theme_slider_material),
                                stringResource(R.string.theme_slider_hyper_os),
                            )
                            SegmentedDropdownItem(
                                icon = Icons.Filled.Tune,
                                title = stringResource(R.string.theme_slider_style_title),
                                items = sliderLabels,
                                selectedIndex = uiState.materialSliderStyle.coerceIn(0, sliderLabels.lastIndex),
                                onItemSelected = actions.onSetMaterialSliderStyle,
                            )
                        },
                    ),
                )

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val fontFamilyValues = listOf("Default", "Serif")
                            val fontFamilyLabels = listOf(
                                stringResource(R.string.settings_material_font_family_default),
                                stringResource(R.string.settings_material_font_family_serif),
                            )
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.DesignServices,
                                title = stringResource(R.string.settings_material_font_family),
                                items = fontFamilyLabels,
                                selectedIndex = fontFamilyValues.indexOf(uiState.materialFontFamily).coerceAtLeast(0),
                                onItemSelected = { index -> actions.onSetMaterialFontFamily(fontFamilyValues[index]) },
                            )
                        }
                    ),
                )


            }

            MaterialThemeGroup(
                title = stringResource(R.string.theme_group_shadows),
                icon = Icons.Filled.Brightness1,
            ) {
                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_container), checked = uiState.materialShadowContainers, onCheckedChange = actions.onSetMaterialShadowContainers) },
                        { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_app_bar), checked = uiState.materialShadowAppBars, onCheckedChange = actions.onSetMaterialShadowAppBars) },
                        { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_button), checked = uiState.materialShadowButtons, onCheckedChange = actions.onSetMaterialShadowButtons) },
                        { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_fab), checked = uiState.materialShadowFab, onCheckedChange = actions.onSetMaterialShadowFab) },
                        { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_switch), checked = uiState.materialShadowSwitches, onCheckedChange = actions.onSetMaterialShadowSwitches) },
                        { SegmentedSwitchItem(icon = Icons.Filled.Tune, title = stringResource(R.string.theme_shadow_slider), checked = uiState.materialShadowSliders, onCheckedChange = actions.onSetMaterialShadowSliders) },
                    ),
                )


            }

            Spacer(modifier = Modifier.height(16.dp + navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()))
        }
    }
}

@Composable
private fun MaterialThemeGroup(
    title: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content,
                )
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    contrastLevel: Float = 0.0f,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val useRail = useNavigationRail()

    val colorScheme = rememberAlohaOneUIColorScheme(
        seedColor = if (keyColor == 0) Color.Unspecified else Color(keyColor),
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
        contrastLevelOverride = contrastLevel.toDouble()
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, color = colorScheme.outlineVariant)
        ) {
            val content: @Composable ColumnScope.() -> Unit = {
                // top bar
                Box(
                    modifier = Modifier
                        .height(if (useRail) 36.dp else 48.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, top = if (useRail) 8.dp else 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val showInfoCard = maxHeight >= 72.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TonalCard(
                            containerColor = colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            content = { }
                        )
                        if (showInfoCard) {
                            TonalCard(
                                containerColor = colorScheme.surfaceBright,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                content = { }
                            )
                        }
                    }
                }
            }

            if (useRail) {
                Row {
                    Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(36.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) { content() }
                }
            } else {
                Column {
                    content()

                    // bottom bar
                    Surface(
                        color = colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    contrastLevel: Float = 0.0f,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = rememberAlohaOneUIColorScheme(
        seedColor = color,
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
        contrastLevelOverride = contrastLevel.toDouble(),
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}


private fun extractAverageSeedColor(context: Context, uri: Uri): Int? {
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    return try {
        val sampleWidth = minOf(48, bitmap.width.coerceAtLeast(1))
        val sampleHeight = minOf(48, bitmap.height.coerceAtLeast(1))
        val sample = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)
        try {
            var red = 0L
            var green = 0L
            var blue = 0L
            var count = 0L
            for (y in 0 until sample.height) {
                for (x in 0 until sample.width) {
                    val pixel = sample.getPixel(x, y)
                    if (android.graphics.Color.alpha(pixel) < 16) continue
                    red += android.graphics.Color.red(pixel)
                    green += android.graphics.Color.green(pixel)
                    blue += android.graphics.Color.blue(pixel)
                    count++
                }
            }
            if (count == 0L) null else android.graphics.Color.rgb(
                (red / count).toInt(),
                (green / count).toInt(),
                (blue / count).toInt(),
            )
        } finally {
            if (sample !== bitmap) sample.recycle()
        }
    } finally {
        bitmap.recycle()
    }
}
