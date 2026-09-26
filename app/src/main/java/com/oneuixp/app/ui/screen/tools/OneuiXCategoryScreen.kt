package com.oneuixp.app.ui.screen.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneuixp.app.R
import com.oneuixp.app.oneuix.OneuiXCategory
import com.oneuixp.app.oneuix.OneuiXFeatureCatalog
import com.oneuixp.app.oneuix.OneuiXSettingSpec
import com.oneuixp.app.oneuix.OneuiXSettingType
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.SegmentedColumn
import com.oneuixp.app.ui.component.material.SegmentedDropdownItem
import com.oneuixp.app.ui.component.material.SegmentedSwitchItem
import com.oneuixp.app.ui.component.material.SegmentedTextField
import com.oneuixp.app.ui.component.miuix.EditText
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.util.BlurredBar
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.common.Preference
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun OneuiXCategoryScreen(categoryName: String) {
    val navigator = LocalNavigator.current
    val category = runCatching { OneuiXCategory.valueOf(categoryName) }.getOrNull()
        ?: return
    val toolsViewModel = viewModel<OneuiXToolsViewModel>()
    val preference by toolsViewModel.preference.collectAsStateWithLifecycle()
    val settings = remember(category) { OneuiXFeatureCatalog.settings(category) }

    when (LocalUiMode.current) {
        UiMode.Miuix -> OneuiXCategoryMiuix(
            category = category,
            settings = settings,
            preference = preference,
            toolsViewModel = toolsViewModel,
            onBack = navigator::pop,
        )

        UiMode.Material -> OneuiXCategoryMaterial(
            category = category,
            settings = settings,
            preference = preference,
            toolsViewModel = toolsViewModel,
            onBack = navigator::pop,
        )
    }
}

@Composable
private fun OneuiXCategoryMaterial(
    category: OneuiXCategory,
    settings: List<OneuiXSettingSpec>,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
    onBack: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    androidx.compose.material3.Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = { Text(category.fallbackTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.oneuix_migration_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )

            settings.groupBy { it.section }.forEach { (section, sectionSettings) ->
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    title = section,
                    content = sectionSettings.map { spec ->
                        {
                            MaterialSettingRow(
                                spec = spec,
                                preference = preference,
                                toolsViewModel = toolsViewModel,
                            )
                        }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MaterialSettingRow(
    spec: OneuiXSettingSpec,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
) {
    when (spec.type) {
        OneuiXSettingType.Toggle,
        OneuiXSettingType.PowerAction -> {
            SegmentedSwitchItem(
                title = spec.title,
                summary = spec.summary,
                checked = toolsViewModel.checked(preference, spec),
                onCheckedChange = { toolsViewModel.setChecked(spec, it) },
            )
        }

        OneuiXSettingType.ChoiceInt,
        OneuiXSettingType.ChoiceText -> {
            val current = toolsViewModel.value(preference, spec)
            val index = spec.choices.indexOfFirst { it.value == current }
                .coerceAtLeast(0)
            SegmentedDropdownItem(
                title = spec.title,
                summary = spec.summary,
                items = spec.choices.map { it.label },
                selectedIndex = index,
                onItemSelected = { selected ->
                    spec.choices.getOrNull(selected)?.let {
                        toolsViewModel.setRawValue(spec, it.value)
                    }
                },
            )
        }

        OneuiXSettingType.IntValue,
        OneuiXSettingType.FloatValue,
        OneuiXSettingType.TextValue -> {
            EditableMaterialValue(spec, preference, toolsViewModel)
        }
    }
}

@Composable
private fun EditableMaterialValue(
    spec: OneuiXSettingSpec,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
) {
    val current = toolsViewModel.value(preference, spec)
    var edit by remember(spec.path, current) { mutableStateOf(current) }

    SegmentedTextField(
        label = spec.title,
        value = edit,
        onValueChange = {
            edit = it
            if (spec.type == OneuiXSettingType.TextValue || validNumber(spec, it)) {
                toolsViewModel.setRawValue(spec, it)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = when (spec.type) {
                OneuiXSettingType.IntValue -> KeyboardType.Number
                OneuiXSettingType.FloatValue -> KeyboardType.Decimal
                else -> KeyboardType.Text
            },
        ),
        singleLine = spec.path != "systemUI.statusBar.statusBarClockFormat",
        supportingContent = spec.summary?.let { summary ->
            { Text(summary) }
        },
    )
}

@Composable
private fun OneuiXCategoryMiuix(
    category: OneuiXCategory,
    settings: List<OneuiXSettingSpec>,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = category.fallbackTitle,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            MiuixIcon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) {
                                        scaleX = -1f
                                    }
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = {},
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    top.yukonga.miuix.kmp.basic.Text(
                        text = stringResource(R.string.oneuix_migration_notice),
                        color = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                settings.groupBy { it.section }.forEach { (section, sectionSettings) ->
                    item {
                        top.yukonga.miuix.kmp.basic.Text(
                            text = section,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 16.dp,
                                bottom = 8.dp,
                            ),
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            sectionSettings.forEach { spec ->
                                MiuixSettingRow(
                                    spec = spec,
                                    preference = preference,
                                    toolsViewModel = toolsViewModel,
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MiuixSettingRow(
    spec: OneuiXSettingSpec,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
) {
    when (spec.type) {
        OneuiXSettingType.Toggle,
        OneuiXSettingType.PowerAction -> {
            SwitchPreference(
                title = spec.title,
                summary = spec.summary ?: "",
                checked = toolsViewModel.checked(preference, spec),
                onCheckedChange = { toolsViewModel.setChecked(spec, it) },
            )
        }

        OneuiXSettingType.ChoiceInt,
        OneuiXSettingType.ChoiceText -> {
            val current = toolsViewModel.value(preference, spec)
            val index = spec.choices.indexOfFirst { it.value == current }
                .coerceAtLeast(0)
            OverlayDropdownPreference(
                title = spec.title,
                summary = spec.summary ?: "",
                items = spec.choices.map { it.label },
                selectedIndex = index,
                onSelectedIndexChange = { selected ->
                    spec.choices.getOrNull(selected)?.let {
                        toolsViewModel.setRawValue(spec, it.value)
                    }
                },
            )
        }

        OneuiXSettingType.IntValue,
        OneuiXSettingType.FloatValue,
        OneuiXSettingType.TextValue -> {
            val current = toolsViewModel.value(preference, spec)
            var edit by remember(spec.path, current) { mutableStateOf(current) }

            EditText(
                title = spec.title,
                summary = spec.summary,
                value = edit,
                onValueChange = {
                    edit = it
                    if (spec.type == OneuiXSettingType.TextValue || validNumber(spec, it)) {
                        toolsViewModel.setRawValue(spec, it)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (spec.type) {
                        OneuiXSettingType.IntValue -> KeyboardType.Number
                        OneuiXSettingType.FloatValue -> KeyboardType.Decimal
                        else -> KeyboardType.Text
                    },
                ),
            )
        }
    }
}

private fun validNumber(
    spec: OneuiXSettingSpec,
    value: String,
): Boolean = when (spec.type) {
    OneuiXSettingType.IntValue -> value.toIntOrNull() != null
    OneuiXSettingType.FloatValue -> value.toFloatOrNull() != null
    else -> true
}
