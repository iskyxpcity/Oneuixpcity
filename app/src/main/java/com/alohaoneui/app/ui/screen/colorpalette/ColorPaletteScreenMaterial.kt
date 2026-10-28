package com.alohaoneui.app.ui.screen.colorpalette

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.component.bottombar.useNavigationRail
import com.alohaoneui.app.ui.component.material.ExpressiveToggleButton
import com.alohaoneui.app.ui.component.material.ExpressiveContentDialog
import com.alohaoneui.app.ui.component.material.IconShapePresetPicker
import com.alohaoneui.app.ui.component.material.IconShapePresetDetailScreen
import com.alohaoneui.app.ui.component.material.SegmentedColumn
import com.alohaoneui.app.ui.component.material.SegmentedChoiceOptionsItem
import com.alohaoneui.app.ui.component.material.SegmentedSwitchItem
import com.alohaoneui.app.ui.component.material.SegmentedSliderItem
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.app.ui.component.material.TonalCard
import com.alohaoneui.app.ui.component.material.voyageSegmentedShape
import com.alohaoneui.app.ui.component.material.voyageCornerShape
import com.alohaoneui.app.ui.component.material.iconShapeForStyle
import com.alohaoneui.app.ui.theme.ColorMode
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowButtons
import com.alohaoneui.app.ui.theme.LocalMaterialShadowTopBars
import com.alohaoneui.app.ui.theme.LocalMaterialShadowSliders
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
    var showIconShapeDetails by remember { mutableStateOf(false) }
    BackHandler(enabled = showIconShapeDetails) { showIconShapeDetails = false }
    if (showIconShapeDetails) {
        IconShapePresetDetailScreen(
            selectedIndex = uiState.materialIconShapeStyle,
            onSelected = { style ->
                actions.onSetMaterialIconShapeStyle(style)
            },
            onBack = { showIconShapeDetails = false },
        )
        return
    }

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier
                    .then(if (LocalMaterialShadowTopBars.current) Modifier.shadow(2.dp) else Modifier)
                    .then(
                        LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                            Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant)
                        } ?: Modifier
                    ),
                navigationIcon = {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    IconButton(
                        modifier = Modifier
                            .then(
                                if (LocalMaterialShadowButtons.current) {
                                    Modifier.shadow(2.dp, CircleShape)
                                } else Modifier
                            )
                            .then(
                                LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                                    Modifier.border(it.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                } ?: Modifier
                            ),
                        onClick = actions.onBack,
                        interactionSource = backInteractionSource,
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
            val isDark = currentColorMode.isDark || currentColorMode.isSystem && isSystemInDarkTheme()
            val isAmoled = currentColorMode.isAmoled
            ThemePreviewCard(
                keyColor = currentKeyColor,
                isDark = isDark,
                isAmoled = isAmoled,
                paletteStyle = colorStyle,
                colorSpec = colorSpec,
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
                        onClick = {
                            actions.onSetKeyColor(color)
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(
                    listOf(ColorMode.SYSTEM) to stringResource(R.string.settings_theme_mode_system),
                    listOf(ColorMode.LIGHT) to stringResource(R.string.settings_theme_mode_light),
                    listOf(ColorMode.DARK) to stringResource(R.string.settings_theme_mode_dark),
                    listOf(ColorMode.DARK_AMOLED) to stringResource(R.string.settings_theme_mode_dark)
                )

                options.chunked(4).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rowOptions.forEachIndexed { index, (modes, label) ->
                            val sourceShape = voyageSegmentedShape(
                                index = index,
                                count = rowOptions.size,
                                vertical = false,
                            )
                            ExpressiveToggleButton(
                                checked = currentColorMode in modes,
                                onCheckedChange = {
                                    if (it) {
                                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                        actions.onSetColorMode(modes.first())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .semantics { role = Role.RadioButton },
                                colors = ToggleButtonDefaults.colors(
                                    checkedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                shapes = ToggleButtonShapes(
                                    shape = sourceShape,
                                    pressedShape = sourceShape,
                                    checkedShape = sourceShape,
                                ),
                            ) {
                                Icon(
                                    imageVector = when (modes.first()) {
                                        ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                        ColorMode.LIGHT -> Icons.Filled.Brightness7
                                        ColorMode.DARK -> Icons.Filled.Brightness3
                                        ColorMode.DARK_AMOLED -> Icons.Filled.Brightness1
                                        else -> Icons.Filled.Brightness4
                                    },
                                    contentDescription = label
                                )
                            }
                        }
                    }
                }

                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    content = listOf(
                        {
                            val styles = PaletteStyle.entries
                            SegmentedChoiceOptionsItem(
                                title = stringResource(R.string.settings_color_style),
                                items = styles.map { paletteStyleLabel(it.name) },
                                selectedIndex = styles.indexOf(colorStyle),
                                onItemSelected = { index ->
                                    actions.onSetColorStyle(styles[index].name)
                                }
                            )
                        },
                        {
                            val specs = ColorSpec.SpecVersion.entries
                            SegmentedChoiceOptionsItem(
                                title = stringResource(R.string.settings_color_spec),
                                items = specs.map { colorSpecLabel(it.name) },
                                selectedIndex = specs.indexOf(colorSpec).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    actions.onSetColorSpec(specs[index].name)
                                }
                            )
                        }
                    )
                )

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
                            SegmentedChoiceOptionsItem(
                                title = stringResource(R.string.settings_page_gesture_mode),
                                items = pagerGestureOptions,
                                selectedIndex = uiState.pagerGestureMode.coerceIn(0, pagerGestureOptions.lastIndex),
                                onItemSelected = actions.onSetPagerGestureMode,
                            )
                        },
                    ),
                )

                TonalCard(
                    modifier = Modifier.padding(top = 4.dp),
                    shape = voyageCornerShape(20.dp),
                ) {
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

                        AlohaOneUISlider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { actions.onSetPageScale(sliderValue) },
                            valueRange = 0.8f..1.1f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Voyage SHAPE group: the slider remains a row in the source-style group.
                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    title = stringResource(R.string.theme_group_shape),
                    content = listOf(
                        {
                            SegmentedChoiceOptionsItem(
                                title = stringResource(R.string.theme_shape_style_title),
                                items = listOf(
                                    stringResource(R.string.theme_shape_rounded),
                                    stringResource(R.string.theme_shape_cut),
                                    stringResource(R.string.theme_shape_squircle),
                                    stringResource(R.string.theme_shape_smooth),
                                    stringResource(R.string.theme_shape_wavy),
                                    stringResource(R.string.theme_shape_scoop),
                                    stringResource(R.string.theme_shape_notch),
                                ),
                                selectedIndex = uiState.materialShapeStyle,
                                onItemSelected = actions.onSetMaterialShapeStyle,
                            )
                        },
                        {
                            var cornerScale by remember(uiState.materialCornerScale) {
                                mutableFloatStateOf(uiState.materialCornerScale)
                            }
                            var showCornerValueDialog by remember { mutableStateOf(false) }
                            var cornerScaleInput by remember { mutableStateOf("") }
                            val cornerPreviewShape = iconShapeForStyle(uiState.materialIconShapeStyle)
                                ?: RoundedCornerShape(14.dp)
                            SegmentedSliderItem(
                                title = stringResource(R.string.theme_corner_scale_title),
                                valueLabel = "",
                                value = cornerScale,
                                onValueChange = { cornerScale = kotlin.math.round(it * 20f) / 20f },
                                valueRange = 0.0f..2.0f,
                                // 39 inner stops produce 0.05 increments over 0.0–2.0.
                                steps = 39,
                                onValueChangeFinished = { actions.onSetMaterialCornerScale(cornerScale) },
                                titleTrailingContent = {
                                    TextButton(
                                        onClick = {
                                            cornerScaleInput = String.format(java.util.Locale.ROOT, "%.2f", cornerScale)
                                            showCornerValueDialog = true
                                        },
                                        modifier = Modifier
                                            .clip(voyageCornerShape(12.dp))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                voyageCornerShape(12.dp),
                                            ),
                                    ) {
                                        Text(
                                            text = String.format(java.util.Locale.ROOT, "%.2f", cornerScale),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                            )
                            if (showCornerValueDialog) {
                                ExpressiveContentDialog(
                                    onDismissRequest = { showCornerValueDialog = false },
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.theme_corner_scale_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        // Vector-style preview: the selected icon-shape is the
                                        // background silhouette; 1 sits left of two stacked 2s.
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(cornerPreviewShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            ) {
                                                Text(
                                                    text = "1",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                )
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                ) {
                                                    Text(
                                                        text = "2",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                    )
                                                    Text(
                                                        text = "2",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                    )
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = cornerScaleInput,
                                            onValueChange = { input ->
                                                if (input.length <= 6 && input.all { it.isDigit() || it == '.' }) {
                                                    cornerScaleInput = input
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            label = { Text("0.00 – 2.00") },
                                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                                textAlign = TextAlign.Center,
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            TextButton(onClick = { showCornerValueDialog = false }) {
                                                Text(stringResource(android.R.string.cancel))
                                            }
                                            TextButton(
                                                onClick = {
                                                    val parsed = cornerScaleInput.toFloatOrNull()
                                                    if (parsed != null && parsed.isFinite() && parsed in 0f..2f) {
                                                        val snapped = (kotlin.math.round(parsed * 20f) / 20f)
                                                            .coerceIn(0f, 2f)
                                                        cornerScale = snapped
                                                        actions.onSetMaterialCornerScale(snapped)
                                                        showCornerValueDialog = false
                                                    }
                                                },
                                            ) {
                                                Text(stringResource(android.R.string.ok))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                )

                // Voyage COMPONENTS group: outline width is another source-style slider row.
                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    title = stringResource(R.string.theme_group_components),
                    content = listOf(
                        {
                            SegmentedChoiceOptionsItem(
                                title = stringResource(R.string.theme_switch_style_title),
                                items = listOf(
                                    stringResource(R.string.theme_switch_material_you),
                                    stringResource(R.string.theme_switch_compose),
                                    stringResource(R.string.theme_switch_pixel),
                                    stringResource(R.string.theme_switch_fluent),
                                    stringResource(R.string.theme_switch_cupertino),
                                    stringResource(R.string.theme_switch_liquid_glass),
                                    stringResource(R.string.theme_switch_hyper_os),
                                    stringResource(R.string.theme_switch_one_ui),
                                ),
                                selectedIndex = uiState.materialSwitchStyle,
                                onItemSelected = actions.onSetMaterialSwitchStyle,
                            )
                        },
                        {
                            SegmentedChoiceOptionsItem(
                                title = stringResource(R.string.theme_slider_style_title),
                                items = listOf(
                                    stringResource(R.string.theme_slider_material_you),
                                    stringResource(R.string.theme_slider_fancy),
                                    stringResource(R.string.theme_slider_material),
                                    stringResource(R.string.theme_slider_hyper_os),
                                ),
                                selectedIndex = uiState.materialSliderStyle,
                                onItemSelected = actions.onSetMaterialSliderStyle,
                            )
                        },
                        {
                            IconShapePresetPicker(
                                selectedIndex = uiState.materialIconShapeStyle,
                                onOpen = { showIconShapeDetails = true },
                            )
                        },
                        {
                            var outlineWidth by remember(uiState.materialOutlineWidth) {
                                mutableFloatStateOf(uiState.materialOutlineWidth)
                            }
                            SegmentedSliderItem(
                                title = stringResource(R.string.theme_border_width_title),
                                valueLabel = String.format(java.util.Locale.ROOT, "%.1f", outlineWidth),
                                value = outlineWidth,
                                onValueChange = { outlineWidth = it },
                                valueRange = 0.0f..1.5f,
                                steps = 14,
                                onValueChangeFinished = { actions.onSetMaterialOutlineWidth(outlineWidth) },
                            )
                        }
                    )
                )

                // Voyage SHADOWS group exposes six independent component switches.
                SegmentedColumn(
                    modifier = Modifier.padding(top = 4.dp),
                    title = stringResource(R.string.theme_group_shadows),
                    content = listOf(
                        {
                            SegmentedSwitchItem(
                                title = stringResource(R.string.theme_shadow_container),
                                checked = uiState.materialShadowContainers,
                                onCheckedChange = actions.onSetMaterialShadowContainers,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                title = stringResource(R.string.theme_shadow_app_bar),
                                checked = uiState.materialShadowTopBars,
                                onCheckedChange = actions.onSetMaterialShadowTopBars,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                title = stringResource(R.string.theme_shadow_button),
                                checked = uiState.materialShadowButtons,
                                onCheckedChange = actions.onSetMaterialShadowButtons,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                title = stringResource(R.string.theme_shadow_fab),
                                checked = uiState.materialShadowFloatingButtons,
                                onCheckedChange = actions.onSetMaterialShadowFloatingButtons,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                title = stringResource(R.string.theme_shadow_switch),
                                checked = uiState.materialShadowSwitches,
                                onCheckedChange = actions.onSetMaterialShadowSwitches,
                            )
                        },
                        {
                            SegmentedSwitchItem(
                                title = stringResource(R.string.theme_shadow_slider),
                                checked = uiState.materialShadowSliders,
                                onCheckedChange = actions.onSetMaterialShadowSliders,
                            )
                        },
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp + navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()))
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
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = colorScheme.surfaceContainer,
            shape = voyageCornerShape(20.dp),
            border = LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                BorderStroke(it.dp, color = colorScheme.outlineVariant)
            }
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
                            shape = voyageCornerShape(8.dp),
                            content = { }
                        )
                        if (showInfoCard) {
                            TonalCard(
                                containerColor = colorScheme.surfaceBright,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = voyageCornerShape(8.dp),
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
private fun paletteStyleLabel(name: String): String = when (name) {
    "TonalSpot" -> stringResource(R.string.palette_style_tonal_spot)
    "Neutral" -> stringResource(R.string.palette_style_neutral)
    "Vibrant" -> stringResource(R.string.palette_style_vibrant)
    "Expressive" -> stringResource(R.string.palette_style_expressive)
    "Rainbow" -> stringResource(R.string.palette_style_rainbow)
    "FruitSalad" -> stringResource(R.string.palette_style_fruit_salad)
    "Monochrome" -> stringResource(R.string.palette_style_monochrome)
    "Fidelity" -> stringResource(R.string.palette_style_fidelity)
    "Content" -> stringResource(R.string.palette_style_content)
    "Cmf" -> stringResource(R.string.palette_style_cmf)
    else -> name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
private fun colorSpecLabel(name: String): String = when (name) {
    "SPEC_2021" -> stringResource(R.string.color_spec_2021)
    "SPEC_2025" -> stringResource(R.string.color_spec_2025)
    else -> name.replace("SPEC_", "").replace('_', ' ')
}

@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = rememberAlohaOneUIColorScheme(
        seedColor = color,
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        interactionSource = interactionSource,
        shape = voyageCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier
            .size(72.dp)
            .then(
                if (LocalMaterialShadowButtons.current) {
                    Modifier.shadow(if (isSelected) 2.dp else 1.dp, voyageCornerShape(20.dp))
                } else Modifier
            )
            .then(
                LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                    Modifier.border(it.dp, colorScheme.outlineVariant, voyageCornerShape(20.dp))
                } ?: Modifier
            )
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
                            .then(
                                LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
                                    Modifier.border(it.dp, colorScheme.primary, CircleShape)
                                } ?: Modifier
                            ),
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
