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
import androidx.compose.foundation.layout.size
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
import com.oneuixp.app.oneuix.OneuiXLayoutRules
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

private data class OneuiXSettingGroup(
    val section: String,
    val items: List<OneuiXSettingSpec>,
)

@Composable
fun OneuiXCategoryScreen(categoryName: String) {
    val navigator = LocalNavigator.current
    val category = runCatching { OneuiXCategory.valueOf(categoryName) }.getOrNull()
        ?: return
    val toolsViewModel = viewModel<OneuiXToolsViewModel>()
    val preference by toolsViewModel.preference.collectAsStateWithLifecycle()

    val allSettings = remember(category) {
        OneuiXFeatureCatalog.settings(category)
    }

    val orderedVisibleSettings = OneuiXLayoutRules
        .ordered(category, allSettings)
        .filter { toolsViewModel.isVisible(preference, it) }

    val groups = remember(category, orderedVisibleSettings) {
        val orderedGroups = linkedMapOf<String, MutableList<OneuiXSettingSpec>>()
        orderedVisibleSettings.forEach { spec ->
            orderedGroups
                .getOrPut(OneuiXLayoutRules.groupKey(spec)) { mutableListOf() }
                .add(spec)
        }
        orderedGroups.values.map { items ->
            OneuiXSettingGroup(
                section = OneuiXLayoutRules.section(category, items.first()),
                items = items,
            )
        }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> OneuiXCategoryMiuix(
            category = category,
            groups = groups,
            preference = preference,
            toolsViewModel = toolsViewModel,
            onBack = navigator::pop,
        )

        UiMode.Material -> OneuiXCategoryMaterial(
            category = category,
            groups = groups,
            preference = preference,
            toolsViewModel = toolsViewModel,
            onBack = navigator::pop,
        )
    }
}

@Composable
private fun OneuiXCategoryMaterial(
    category: OneuiXCategory,
    groups: List<OneuiXSettingGroup>,
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
                title = { Text(oneuiXCategoryTitle(category)) },
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
            var previousSection: String? = null
            groups.forEach { group ->
                val showSection = previousSection != group.section
                SegmentedColumn(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = if (showSection) 8.dp else 4.dp,
                    ),
                    title = if (showSection) {
                        oneuiXSectionTitle(group.section)
                    } else {
                        ""
                    },
                    content = group.items.map { spec ->
                        {
                            MaterialSettingRow(
                                spec = spec,
                                preference = preference,
                                toolsViewModel = toolsViewModel,
                            )
                        }
                    },
                )
                previousSection = group.section
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
    val title = oneuiXSettingTitle(spec)
    val summary = oneuiXSettingSummary(spec)
    val icon = oneuiXSettingIcon(spec)

    if (spec.path == "systemUI.statusBar.networkSpeedThresholdKb") {
        NetworkSpeedThresholdMaterial(
            spec = spec,
            title = title,
            summary = summary,
            preference = preference,
            toolsViewModel = toolsViewModel,
        )
        return
    }

    when (spec.type) {
        OneuiXSettingType.Toggle,
        OneuiXSettingType.PowerAction -> {
            SegmentedSwitchItem(
                icon = icon,
                title = title,
                summary = summary,
                checked = toolsViewModel.checked(preference, spec),
                onCheckedChange = { toolsViewModel.setChecked(spec, it) },
            )
        }

        OneuiXSettingType.ChoiceInt,
        OneuiXSettingType.ChoiceText -> {
            val current = toolsViewModel.value(preference, spec)
            val labels = oneuiXChoiceLabels(spec)
            val index = spec.choices.indexOfFirst { it.value == current }
                .coerceAtLeast(0)

            SegmentedDropdownItem(
                icon = icon,
                title = title,
                summary = summary,
                items = labels,
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
            EditableMaterialValue(
                spec = spec,
                title = title,
                summary = summary,
                preference = preference,
                toolsViewModel = toolsViewModel,
            )
        }
    }
}

@Composable
private fun NetworkSpeedThresholdMaterial(
    spec: OneuiXSettingSpec,
    title: String,
    summary: String?,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
) {
    val current = toolsViewModel.value(preference, spec).toIntOrNull() ?: 0
    SegmentedSwitchItem(
        icon = oneuiXSettingIcon(spec),
        title = title,
        summary = if (current > 0) "$current KB/s" else summary,
        checked = current > 0,
        onCheckedChange = { enabled ->
            toolsViewModel.setRawValue(
                spec,
                if (enabled) current.coerceAtLeast(1).toString() else "0",
            )
        },
    )
    if (current > 0) {
        EditableMaterialValue(
            spec = spec,
            title = title,
            summary = summary,
            preference = preference,
            toolsViewModel = toolsViewModel,
            showIcon = false,
        )
    }
}

@Composable
private fun EditableMaterialValue(
    spec: OneuiXSettingSpec,
    title: String,
    summary: String?,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
    showIcon: Boolean = true,
) {
    val current = toolsViewModel.value(preference, spec)
    var edit by remember(spec.path, current) { mutableStateOf(current) }

    SegmentedTextField(
        label = title,
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
        leadingContent = if (showIcon) {
            oneuiXSettingIcon(spec)?.let { imageVector ->
                { Icon(imageVector = imageVector, contentDescription = null) }
            }
        } else {
            null
        },
        supportingContent = summary?.let { text ->
            { Text(text) }
        },
    )
}

@Composable
private fun OneuiXCategoryMiuix(
    category: OneuiXCategory,
    groups: List<OneuiXSettingGroup>,
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
                    title = oneuiXCategoryTitle(category),
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
                var previousSection: String? = null
                groups.forEach { group ->
                    if (previousSection != group.section) {
                        item {
                            top.yukonga.miuix.kmp.basic.Text(
                                text = oneuiXSectionTitle(group.section),
                                color = colorScheme.primary,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 16.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                        ) {
                            group.items.forEach { spec ->
                                MiuixSettingRow(
                                    spec = spec,
                                    preference = preference,
                                    toolsViewModel = toolsViewModel,
                                )
                            }
                        }
                    }
                    previousSection = group.section
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
    val title = oneuiXSettingTitle(spec)
    val summary = oneuiXSettingSummary(spec)
    val icon = oneuiXSettingIcon(spec)

    if (spec.path == "systemUI.statusBar.networkSpeedThresholdKb") {
        NetworkSpeedThresholdMiuix(
            spec = spec,
            title = title,
            summary = summary,
            preference = preference,
            toolsViewModel = toolsViewModel,
        )
        return
    }

    when (spec.type) {
        OneuiXSettingType.Toggle,
        OneuiXSettingType.PowerAction -> {
            SwitchPreference(
                title = title,
                summary = summary,
                startAction = icon?.let { imageVector ->
                    {
                        MiuixIcon(
                            imageVector = imageVector,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colorScheme.onSurface,
                        )
                    }
                },
                checked = toolsViewModel.checked(preference, spec),
                onCheckedChange = { toolsViewModel.setChecked(spec, it) },
            )
        }

        OneuiXSettingType.ChoiceInt,
        OneuiXSettingType.ChoiceText -> {
            val current = toolsViewModel.value(preference, spec)
            val labels = oneuiXChoiceLabels(spec)
            val index = spec.choices.indexOfFirst { it.value == current }
                .coerceAtLeast(0)

            OverlayDropdownPreference(
                title = title,
                summary = summary,
                startAction = icon?.let { imageVector ->
                    {
                        MiuixIcon(
                            imageVector = imageVector,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colorScheme.onSurface,
                        )
                    }
                },
                items = labels,
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
            EditableMiuixValue(
                spec = spec,
                title = title,
                summary = summary,
                preference = preference,
                toolsViewModel = toolsViewModel,
            )
        }
    }
}

@Composable
private fun NetworkSpeedThresholdMiuix(
    spec: OneuiXSettingSpec,
    title: String,
    summary: String?,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
) {
    val current = toolsViewModel.value(preference, spec).toIntOrNull() ?: 0
    SwitchPreference(
        title = title,
        summary = if (current > 0) "$current KB/s" else summary,
        startAction = oneuiXSettingIcon(spec)?.let { imageVector ->
            {
                MiuixIcon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = colorScheme.onSurface,
                )
            }
        },
        checked = current > 0,
        onCheckedChange = { enabled ->
            toolsViewModel.setRawValue(
                spec,
                if (enabled) current.coerceAtLeast(1).toString() else "0",
            )
        },
    )
    if (current > 0) {
        EditableMiuixValue(
            spec = spec,
            title = title,
            summary = summary,
            preference = preference,
            toolsViewModel = toolsViewModel,
            showIcon = false,
        )
    }
}

@Composable
private fun EditableMiuixValue(
    spec: OneuiXSettingSpec,
    title: String,
    summary: String?,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
    showIcon: Boolean = true,
) {
    val current = toolsViewModel.value(preference, spec)
    var edit by remember(spec.path, current) { mutableStateOf(current) }

    EditText(
        title = title,
        summary = summary,
        startAction = if (showIcon) {
            oneuiXSettingIcon(spec)?.let { imageVector ->
                {
                    MiuixIcon(
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = colorScheme.onSurface,
                    )
                }
            }
        } else {
            null
        },
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

private fun validNumber(
    spec: OneuiXSettingSpec,
    value: String,
): Boolean = when (spec.type) {
    OneuiXSettingType.IntValue -> value.toIntOrNull() != null
    OneuiXSettingType.FloatValue -> value.toFloatOrNull() != null
    else -> true
}
