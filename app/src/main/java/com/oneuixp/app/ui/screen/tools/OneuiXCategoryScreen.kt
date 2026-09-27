package com.oneuixp.app.ui.screen.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.time.LocalDateTime
import kotlin.math.roundToInt
import android.widget.Toast
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
import com.oneuixp.app.ui.component.material.SegmentedSliderItem
import com.oneuixp.app.ui.component.material.SegmentedTextField
import com.oneuixp.app.ui.component.miuix.EditText
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.util.BlurredBar
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.hook.util.StatusBarClockFormatter
import io.github.soclear.oneuix.ui.category.DetailPaneAndroid
import io.github.soclear.oneuix.ui.category.AndroidEvent
import io.github.soclear.oneuix.ui.category.DetailPaneBrowser
import io.github.soclear.oneuix.ui.category.DetailPaneCalendar
import io.github.soclear.oneuix.ui.category.DetailPaneCall
import io.github.soclear.oneuix.ui.category.DetailPaneCamera
import io.github.soclear.oneuix.ui.category.DetailPaneDualApp
import io.github.soclear.oneuix.ui.category.DetailPaneGalaxyStore
import io.github.soclear.oneuix.ui.category.DetailPaneGallery
import io.github.soclear.oneuix.ui.category.DetailPaneHealthMonitor
import io.github.soclear.oneuix.ui.category.DetailPaneLauncher
import io.github.soclear.oneuix.ui.category.DetailPaneMessaging
import io.github.soclear.oneuix.ui.category.DetailPaneNotes
import io.github.soclear.oneuix.ui.category.DetailPanePhotoRetouching
import io.github.soclear.oneuix.ui.category.DetailPaneSPen
import io.github.soclear.oneuix.ui.category.DetailPaneSettings
import io.github.soclear.oneuix.ui.category.DetailPaneSketchBook
import io.github.soclear.oneuix.ui.category.DetailPaneSystemUI
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.category.DetailPaneThemeCenter
import io.github.soclear.oneuix.ui.category.DetailPaneVideo
import io.github.soclear.oneuix.ui.category.DetailPaneWatchManager
import io.github.soclear.oneuix.ui.category.DetailPaneWeather
import io.github.soclear.oneuix.ui.category.onAndroidEvent
import io.github.soclear.oneuix.ui.category.onBrowserEvent
import io.github.soclear.oneuix.ui.category.onCalendarEvent
import io.github.soclear.oneuix.ui.category.onCallEvent
import io.github.soclear.oneuix.ui.category.onCameraEvent
import io.github.soclear.oneuix.ui.category.onDualAppEvent
import io.github.soclear.oneuix.ui.category.onGalaxyStoreEvent
import io.github.soclear.oneuix.ui.category.onGalleryEvent
import io.github.soclear.oneuix.ui.category.onHealthMonitorEvent
import io.github.soclear.oneuix.ui.category.onLauncherEvent
import io.github.soclear.oneuix.ui.category.onMessagingEvent
import io.github.soclear.oneuix.ui.category.onNotesEvent
import io.github.soclear.oneuix.ui.category.onPhotoRetouchingEvent
import io.github.soclear.oneuix.ui.category.onSPenEvent
import io.github.soclear.oneuix.ui.category.onSettingsEvent
import io.github.soclear.oneuix.ui.category.onSketchBookEvent
import io.github.soclear.oneuix.ui.category.onSystemUIEvent
import io.github.soclear.oneuix.ui.category.onThemeCenterEvent
import io.github.soclear.oneuix.ui.category.onVideoEvent
import io.github.soclear.oneuix.ui.category.onWatchManagerEvent
import io.github.soclear.oneuix.ui.category.onWeatherEvent
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

    if (LocalUiMode.current == UiMode.Miuix) {
        OneuiXOriginalCategoryScreen(
            category = category,
            preference = preference,
            toolsViewModel = toolsViewModel,
            onBack = navigator::pop,
        )
        return
    }

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

    OneuiXCategoryMaterial(
        category = category,
        groups = groups,
        preference = preference,
        toolsViewModel = toolsViewModel,
        onBack = navigator::pop,
    )
}

@Composable
private fun OneuiXOriginalCategoryScreen(
    category: OneuiXCategory,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
    onBack: () -> Unit,
) {
    @Composable
    fun OriginalContent(modifier: Modifier) {
        when (category) {
            OneuiXCategory.Android -> DetailPaneAndroid(
                uiState = preference.android,
                onEvent = toolsViewModel::onAndroidEvent,
                modifier = modifier,
            )
            OneuiXCategory.SystemUI -> DetailPaneSystemUI(
                uiState = preference.systemUI,
                onEvent = toolsViewModel::onSystemUIEvent,
                modifier = modifier,
            )
            OneuiXCategory.Settings -> DetailPaneSettings(
                uiState = preference.settings,
                onEvent = toolsViewModel::onSettingsEvent,
                modifier = modifier,
            )
            OneuiXCategory.Call -> DetailPaneCall(
                uiState = preference.call,
                onEvent = toolsViewModel::onCallEvent,
                modifier = modifier,
            )
            OneuiXCategory.Camera -> DetailPaneCamera(
                uiState = preference.camera,
                onEvent = toolsViewModel::onCameraEvent,
                modifier = modifier,
            )
            OneuiXCategory.Browser -> DetailPaneBrowser(preference.other, toolsViewModel::onBrowserEvent, modifier)
            OneuiXCategory.Calendar -> DetailPaneCalendar(preference.other, toolsViewModel::onCalendarEvent, modifier)
            OneuiXCategory.DualApp -> DetailPaneDualApp(preference.other, toolsViewModel::onDualAppEvent, modifier)
            OneuiXCategory.Gallery -> DetailPaneGallery(preference.other, toolsViewModel::onGalleryEvent, modifier)
            OneuiXCategory.GalaxyStore -> DetailPaneGalaxyStore(preference.other, toolsViewModel::onGalaxyStoreEvent, modifier)
            OneuiXCategory.HealthMonitor -> DetailPaneHealthMonitor(preference.other, toolsViewModel::onHealthMonitorEvent, modifier)
            OneuiXCategory.Launcher -> DetailPaneLauncher(preference.other, toolsViewModel::onLauncherEvent, modifier)
            OneuiXCategory.Messaging -> DetailPaneMessaging(preference.other, toolsViewModel::onMessagingEvent, modifier)
            OneuiXCategory.Notes -> DetailPaneNotes(preference.other, toolsViewModel::onNotesEvent, modifier)
            OneuiXCategory.PhotoRetouching -> DetailPanePhotoRetouching(preference.other, toolsViewModel::onPhotoRetouchingEvent, modifier)
            OneuiXCategory.SketchBook -> DetailPaneSketchBook(preference.other, toolsViewModel::onSketchBookEvent, modifier)
            OneuiXCategory.SPen -> DetailPaneSPen(preference.other, toolsViewModel::onSPenEvent, modifier)
            OneuiXCategory.ThemeCenter -> DetailPaneThemeCenter(preference.other, toolsViewModel::onThemeCenterEvent, modifier)
            OneuiXCategory.Video -> DetailPaneVideo(preference.other, toolsViewModel::onVideoEvent, modifier)
            OneuiXCategory.WatchManager -> DetailPaneWatchManager(preference.other, toolsViewModel::onWatchManagerEvent, modifier)
            OneuiXCategory.Weather -> DetailPaneWeather(preference.other, toolsViewModel::onWeatherEvent, modifier)
        }
    }

    when (LocalUiMode.current) {
        UiMode.Material -> {
            val scrollBehavior =
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            androidx.compose.material3.Scaffold(
                topBar = {
                    LargeFlexibleTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
                OriginalContent(
                    Modifier
                        .padding(innerPadding)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                )
            }
        }

        UiMode.Miuix -> {
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
                                            if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
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
                contentWindowInsets = WindowInsets.systemBars
                    .add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal),
            ) { innerPadding ->
                Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                    OriginalContent(
                        Modifier
                            .padding(innerPadding)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    )
                }
            }
        }
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
            if (category == OneuiXCategory.SystemUI) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val restarted = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val process = ProcessBuilder(
                                            "su",
                                            "-c",
                                            "pid=\$(pidof com.android.systemui); " +
                                                "if [ -n \"\$pid\" ]; then kill \$pid; " +
                                                "else killall com.android.systemui || " +
                                                "am force-stop com.android.systemui; fi"
                                        ).start()
                                        process.waitFor(5, TimeUnit.SECONDS) &&
                                            process.exitValue() == 0
                                    }.getOrDefault(false)
                                }
                                Toast.makeText(
                                    context,
                                    if (restarted) R.string.oneuix_restartSystemUI_success
                                    else R.string.oneuix_restartSystemUI_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    ) {
                        Text(stringResource(io.github.soclear.oneuix.common.R.string.restartSystemUI))
                    }
                }
            }

            groups
                .groupBy { it.section }
                .forEach { (section, sectionGroups) ->
                    SegmentedColumn(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        title = oneuiXSectionTitle(section),
                        content = sectionGroups.flatMap { group ->
                            group.items.mapIndexed { index, spec ->
                                {
                                    MaterialSettingRow(
                                        spec = spec,
                                        preference = preference,
                                        toolsViewModel = toolsViewModel,
                                        hasChildren = index == 0 && group.items.size > 1,
                                        isChild = index > 0,
                                    )
                                }
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
    hasChildren: Boolean = false,
    isChild: Boolean = false,
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

    materialSliderConfig(spec.path)?.let { config ->
        MaterialSliderValue(
            spec = spec,
            title = title,
            preference = preference,
            toolsViewModel = toolsViewModel,
            config = config,
            isChild = isChild,
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
                showChildIndicator = hasChildren,
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
            if (spec.path == "systemUI.statusBar.statusBarClockFormat") {
                MaterialClockFormatEditor(
                    spec = spec,
                    title = title,
                    preference = preference,
                    toolsViewModel = toolsViewModel,
                )
            } else {
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
}

private data class MaterialSliderConfig(
    val min: Float,
    val max: Float,
    val steps: Int = 0,
    val storageDecimals: Int? = null,
)

private fun materialSliderConfig(path: String): MaterialSliderConfig? = when (path) {
    "android.maxNeverKilledAppNum" ->
        MaterialSliderConfig(5f, 30f, steps = 24, storageDecimals = 0)

    "systemUI.statusBar.statusBarLeftPaddingDp",
    "systemUI.statusBar.statusBarRightPaddingDp" ->
        MaterialSliderConfig(0f, 100f)

    "systemUI.statusBar.statusBarTopPaddingDp",
    "systemUI.statusBar.statusBarBottomPaddingDp" ->
        MaterialSliderConfig(0f, 8f, steps = 79, storageDecimals = 1)

    "systemUI.statusBar.batteryIconWidthScale",
    "systemUI.statusBar.batteryIconHeightScale" ->
        MaterialSliderConfig(0.5f, 2f)

    "systemUI.statusBar.fold7DoubleLineClockTimeScale",
    "systemUI.statusBar.fold7DoubleLineClockDateScale" ->
        MaterialSliderConfig(0.5f, 1.1f, steps = 39, storageDecimals = 2)

    "systemUI.statusBar.doubleLineClockGapDp" ->
        MaterialSliderConfig(0f, 2f, steps = 19, storageDecimals = 1)

    "systemUI.statusBar.statusBarClockTextScale" ->
        MaterialSliderConfig(0.5f, 2.5f)

    "systemUI.statusBar.statusBarMaxNotificationIcons" ->
        MaterialSliderConfig(4f, 20f, steps = 15, storageDecimals = 0)

    "systemUI.qs.qsClockTextSize" ->
        MaterialSliderConfig(15f, 70f)

    else -> null
}

@Composable
private fun materialSliderValueLabel(
    path: String,
    value: Float,
): String = when (path) {
    "android.maxNeverKilledAppNum" -> value.roundToInt().toString()

    "systemUI.statusBar.statusBarLeftPaddingDp",
    "systemUI.statusBar.statusBarRightPaddingDp" -> "%.1f dp".format(value)

    "systemUI.statusBar.statusBarTopPaddingDp" ->
        stringResource(R.string.oneuix_statusBarTopPaddingDp_summary, value)

    "systemUI.statusBar.statusBarBottomPaddingDp" ->
        stringResource(R.string.oneuix_statusBarBottomPaddingDp_summary, value)

    "systemUI.statusBar.batteryIconWidthScale",
    "systemUI.statusBar.batteryIconHeightScale" -> "%.2f".format(value)

    "systemUI.statusBar.fold7DoubleLineClockTimeScale" ->
        stringResource(R.string.oneuix_fold7DoubleLineClockTimeScale_summary, value)

    "systemUI.statusBar.fold7DoubleLineClockDateScale" ->
        stringResource(R.string.oneuix_fold7DoubleLineClockDateScale_summary, value)

    "systemUI.statusBar.doubleLineClockGapDp" ->
        stringResource(R.string.oneuix_statusBarDoubleLineClockGap_summary, value)

    "systemUI.statusBar.statusBarClockTextScale" -> "%.2fx".format(value)
    "systemUI.statusBar.statusBarMaxNotificationIcons" -> value.roundToInt().toString()
    "systemUI.qs.qsClockTextSize" -> "%.1fsp".format(value)
    else -> value.toString()
}

private fun sliderStorageValue(
    value: Float,
    config: MaterialSliderConfig,
): String = when (config.storageDecimals) {
    0 -> value.roundToInt().toString()
    1 -> ((value * 10f).roundToInt() / 10f).toString()
    2 -> ((value * 100f).roundToInt() / 100f).toString()
    else -> value.toString()
}

private fun commitMaterialSliderValue(
    spec: OneuiXSettingSpec,
    value: Float,
    config: MaterialSliderConfig,
    toolsViewModel: OneuiXToolsViewModel,
) {
    when (spec.path) {
        "android.maxNeverKilledAppNum" ->
            toolsViewModel.onAndroidEvent(
                AndroidEvent.MaxNeverKilledAppNum(value.roundToInt())
            )

        "systemUI.statusBar.statusBarLeftPaddingDp" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.StatusBarLeftPaddingDp(value)
            )

        "systemUI.statusBar.statusBarRightPaddingDp" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.StatusBarRightPaddingDp(value)
            )

        "systemUI.statusBar.statusBarTopPaddingDp" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.StatusBarTopPaddingDp(
                    (value * 10f).roundToInt() / 10f
                )
            )

        "systemUI.statusBar.statusBarBottomPaddingDp" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.StatusBarBottomPaddingDp(
                    (value * 10f).roundToInt() / 10f
                )
            )

        "systemUI.statusBar.batteryIconWidthScale" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.BatteryIconWidthScale(value)
            )

        "systemUI.statusBar.batteryIconHeightScale" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.BatteryIconHeightScale(value)
            )

        "systemUI.statusBar.fold7DoubleLineClockTimeScale" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.Fold7DoubleLineClockTimeScale(
                    (value * 100f).roundToInt() / 100f
                )
            )

        "systemUI.statusBar.fold7DoubleLineClockDateScale" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.Fold7DoubleLineClockDateScale(
                    (value * 100f).roundToInt() / 100f
                )
            )

        "systemUI.statusBar.doubleLineClockGapDp" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.DoubleLineClockGapDp(
                    (value * 10f).roundToInt() / 10f
                )
            )

        "systemUI.statusBar.statusBarClockTextScale" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.StatusBarClockTextScale(value)
            )

        "systemUI.statusBar.statusBarMaxNotificationIcons" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.StatusBar.StatusBarMaxNotificationIcons(value.roundToInt())
            )

        "systemUI.qs.qsClockTextSize" ->
            toolsViewModel.onSystemUIEvent(
                SystemUIEvent.QS.QSClockTextSize(value)
            )

        else ->
            toolsViewModel.setRawValue(spec, sliderStorageValue(value, config))
    }
}

@Composable
private fun MaterialSliderValue(
    spec: OneuiXSettingSpec,
    title: String,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
    config: MaterialSliderConfig,
    isChild: Boolean,
) {
    val current = toolsViewModel.value(preference, spec).toFloatOrNull() ?: config.min
    var value by remember(spec.path, current) {
        mutableStateOf(current.coerceIn(config.min, config.max))
    }

    SegmentedSliderItem(
        icon = if (isChild) null else oneuiXSettingIcon(spec),
        title = if (isChild) null else title,
        valueLabel = materialSliderValueLabel(spec.path, value),
        value = value,
        onValueChange = { value = it },
        valueRange = config.min..config.max,
        steps = config.steps,
        onValueChangeFinished = {
            commitMaterialSliderValue(
                spec = spec,
                value = value,
                config = config,
                toolsViewModel = toolsViewModel,
            )
        },
    )
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
private fun MaterialClockFormatEditor(
    spec: OneuiXSettingSpec,
    title: String,
    preference: Preference,
    toolsViewModel: OneuiXToolsViewModel,
) {
    val current = toolsViewModel.value(preference, spec)
    var edit by remember(spec.path, current) { mutableStateOf(current) }
    var preview by remember(spec.path, current) {
        mutableStateOf(
            runCatching {
                StatusBarClockFormatter.format(current, LocalDateTime.now())
            }.getOrDefault("")
        )
    }

    SegmentedTextField(
        label = title,
        value = edit,
        onValueChange = { edit = it },
        singleLine = false,
        leadingContent = oneuiXSettingIcon(spec)?.let { imageVector ->
            { Icon(imageVector = imageVector, contentDescription = null) }
        },
        trailingContent = {
            TextButton(
                onClick = {
                    val formatted = runCatching {
                        StatusBarClockFormatter.format(edit, LocalDateTime.now())
                    }
                    if (formatted.isSuccess) {
                        preview = formatted.getOrThrow()
                        toolsViewModel.onSystemUIEvent(
                            SystemUIEvent.StatusBar.StatusBarClockFormat(edit)
                        )
                    } else {
                        preview = "error"
                    }
                },
            ) {
                Text(stringResource(R.string.oneuix_confirm))
            }
        },
        supportingContent = {
            Text(preview.ifBlank { current })
        },
    )
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

    val requiresConfirm = spec.path in setOf(
        "systemUI.statusBar.networkSpeedThresholdKb",
        "systemUI.statusBar.statusBarClockFormat",
        "systemUI.statusBar.customCarrierName",
        "systemUI.notification.hideOngoingActivityMediaPackages",
    )

    SegmentedTextField(
        label = title,
        value = edit,
        onValueChange = {
            edit = it
            if (!requiresConfirm && (spec.type == OneuiXSettingType.TextValue || validNumber(spec, it))) {
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
        trailingContent = if (requiresConfirm) {
            {
                TextButton(
                    onClick = {
                        if (spec.type == OneuiXSettingType.TextValue || validNumber(spec, edit)) {
                            toolsViewModel.setRawValue(spec, edit)
                        }
                    },
                ) {
                    Text(stringResource(R.string.oneuix_confirm))
                }
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
