package com.oneuixp.app.ui.screen.tools

import android.os.Build
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.oneuixp.OneuiXPPreferenceBridge
import com.oneuixp.app.oneuixp.OneuiXPCategory
import com.oneuixp.app.oneuixp.OneuiXPFeatureCatalog
import com.oneuixp.app.oneuixp.OneuiXPLayoutRules
import com.oneuixp.app.oneuixp.OneuiXPSettingSpec
import com.oneuixp.app.oneuixp.OneuiXPSettingType
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.SegmentedColumn
import com.oneuixp.app.ui.component.material.SegmentedDropdownItem
import com.oneuixp.app.ui.component.material.SegmentedListItem
import com.oneuixp.app.ui.component.material.ExpressiveSwitch
import com.oneuixp.app.ui.component.material.SegmentedSwitchItem
import com.oneuixp.app.ui.component.material.SegmentedSliderItem
import com.oneuixp.app.ui.component.material.SegmentedTextField
import com.oneuixp.app.ui.component.rebootlistpopup.RebootListPopup
import com.oneuixp.app.ui.component.miuix.EditText
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.util.BlurredBar
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import com.oneuixp.app.ui.viewmodel.OneuiXPToolsViewModel
import com.oneuixp.common.Preference
import com.oneuixp.common.ONE_UI_VERSION
import com.oneuixp.common.PowerMenuAction
import com.oneuixp.hook.util.StatusBarClockFormatter
import com.oneuixp.mod.ui.category.DetailPaneAndroid
import com.oneuixp.mod.ui.category.AndroidEvent
import com.oneuixp.mod.ui.category.DetailPaneBrowser
import com.oneuixp.mod.ui.category.DetailPaneCalendar
import com.oneuixp.mod.ui.category.DetailPaneCall
import com.oneuixp.mod.ui.category.DetailPaneCamera
import com.oneuixp.mod.ui.category.DetailPaneDualApp
import com.oneuixp.mod.ui.category.DetailPaneGalaxyStore
import com.oneuixp.mod.ui.category.DetailPaneGallery
import com.oneuixp.mod.ui.category.DetailPaneHealthMonitor
import com.oneuixp.mod.ui.category.DetailPaneLauncher
import com.oneuixp.mod.ui.category.DetailPaneMessaging
import com.oneuixp.mod.ui.category.DetailPaneNotes
import com.oneuixp.mod.ui.category.DetailPanePhotoRetouching
import com.oneuixp.mod.ui.category.DetailPaneSPen
import com.oneuixp.mod.ui.category.DetailPaneSettings
import com.oneuixp.mod.ui.category.DetailPaneSketchBook
import com.oneuixp.mod.ui.category.DetailPaneSystemUI
import com.oneuixp.mod.ui.category.SystemUIEvent
import com.oneuixp.mod.ui.category.DetailPaneThemeCenter
import com.oneuixp.mod.ui.category.DetailPaneVideo
import com.oneuixp.mod.ui.category.DetailPaneWatchManager
import com.oneuixp.mod.ui.category.DetailPaneWeather
import com.oneuixp.mod.ui.category.onAndroidEvent
import com.oneuixp.mod.ui.category.onBrowserEvent
import com.oneuixp.mod.ui.category.onCalendarEvent
import com.oneuixp.mod.ui.category.onCallEvent
import com.oneuixp.mod.ui.category.onCameraEvent
import com.oneuixp.mod.ui.category.onDualAppEvent
import com.oneuixp.mod.ui.category.onGalaxyStoreEvent
import com.oneuixp.mod.ui.category.onGalleryEvent
import com.oneuixp.mod.ui.category.onHealthMonitorEvent
import com.oneuixp.mod.ui.category.onLauncherEvent
import com.oneuixp.mod.ui.category.onMessagingEvent
import com.oneuixp.mod.ui.category.onNotesEvent
import com.oneuixp.mod.ui.category.onPhotoRetouchingEvent
import com.oneuixp.mod.ui.category.onSPenEvent
import com.oneuixp.mod.ui.category.onSettingsEvent
import com.oneuixp.mod.ui.category.onSketchBookEvent
import com.oneuixp.mod.ui.category.onSystemUIEvent
import com.oneuixp.mod.ui.category.onThemeCenterEvent
import com.oneuixp.mod.ui.category.onVideoEvent
import com.oneuixp.mod.ui.category.WatchManagerEvent
import com.oneuixp.mod.ui.category.onWatchManagerEvent
import com.oneuixp.mod.ui.category.onWeatherEvent
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

private data class OneuiXPSettingGroup(
    val section: String,
    val items: List<OneuiXPSettingSpec>,
)

@Composable
fun OneuiXPCategoryScreen(categoryName: String) {
    val navigator = LocalNavigator.current
    val category = runCatching { OneuiXPCategory.valueOf(categoryName) }.getOrNull()
        ?: return
    val toolsViewModel = viewModel<OneuiXPToolsViewModel>()
    val preference by toolsViewModel.preference.collectAsStateWithLifecycle()

    /*
     * One functional tree, two skins.
     *
     * Every category now uses the canonical feature DetailPane implementation
     * for ordering, strings, parent/child visibility, state, validation and
     * events. SwitchItem / SelectItem / OneuiXPSlider / OneuiXP special controls
     * choose only the Miuix or Material rendering.
     *
     * Do not rebuild Material behavior from FeatureCatalog paths again: that
     * duplicated the application logic and caused missing controls, misplaced
     * previews and incorrect default-value restoration.
     */
    OneuiXPOriginalCategoryScreen(
        category = category,
        preference = preference,
        toolsViewModel = toolsViewModel,
        onBack = navigator::pop,
    )
}

private fun oneuiXPMaterialPlatformVisible(
    preference: Preference,
    spec: OneuiXPSettingSpec,
): Boolean {
    val sdk = Build.VERSION.SDK_INT
    val statusBar = preference.systemUI.statusBar

    return when (spec.path) {
        "android.supportAppJumpBlock" ->
            sdk >= Build.VERSION_CODES.VANILLA_ICE_CREAM

        "systemUI.statusBar.hideBatteryPercentageSign" ->
            sdk <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        "systemUI.statusBar.addBatteryLevelText",
        "systemUI.statusBar.hideBatteryLevelTextPercentageSign",
        "systemUI.statusBar.hideBatteryLevelTextChargingIcon" ->
            ONE_UI_VERSION >= 70000

        "systemUI.statusBar.statusBarDoubleLineClockSize",
        "systemUI.statusBar.doubleLineClockGapDp" ->
            statusBar.setStatusBarClockFormat &&
                statusBar.statusBarClockFormat.contains('\n')

        "systemUI.statusBar.useFold7CustomDoubleLineClockScale" ->
            statusBar.setStatusBarClockFormat &&
                statusBar.statusBarClockFormat.contains('\n') &&
                Build.MODEL.startsWith("SM-F966", ignoreCase = true)

        "systemUI.statusBar.fold7DoubleLineClockTimeScale",
        "systemUI.statusBar.fold7DoubleLineClockDateScale" ->
            statusBar.setStatusBarClockFormat &&
                statusBar.statusBarClockFormat.contains('\n') &&
                Build.MODEL.startsWith("SM-F966", ignoreCase = true) &&
                statusBar.useFold7CustomDoubleLineClockScale

        "systemUI.qs.hideDeviceControlQsTile",
        "systemUI.qs.hideSmartViewQsTile",
        "systemUI.qs.turnOn5gQsTile" ->
            sdk == Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        "systemUI.qs.hideQsBarMediaPlayer",
        "systemUI.qs.hideQsBarNearbyDevicesAndDeviceControl",
        "systemUI.qs.hideQsBarSmartViewAndModes",
        "systemUI.qs.alwaysExpandQsTileChunk",
        "systemUI.qs.alwaysShowTimeDateOnQs",
        "systemUI.qs.addBrightnessProgressToQsBar",
        "systemUI.qs.addVolumeProgressToQsBar" ->
            sdk >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                ONE_UI_VERSION < 80500

        "systemUI.qs.hideQsBarSecurityFooter",
        "systemUI.qs.hideQsBarDataUsage",
        "systemUI.qs.showTraditionalChineseDateOnQS",
        "systemUI.qs.modifyQSClockTextSize",
        "systemUI.qs.qsClockTextSize",
        "systemUI.aod.hideAODStatusBar",
        "systemUI.aod.aodLockSupportLunar" ->
            sdk >= Build.VERSION_CODES.VANILLA_ICE_CREAM

        else -> true
    }
}

@Composable
private fun OneuiXPOriginalCategoryScreen(
    category: OneuiXPCategory,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
    onBack: () -> Unit,
) {
    @Composable
    fun OriginalContent(modifier: Modifier) {
        when (category) {
            OneuiXPCategory.Android -> DetailPaneAndroid(
                uiState = preference.android,
                onEvent = toolsViewModel::onAndroidEvent,
                modifier = modifier,
            )
            OneuiXPCategory.SystemUI -> DetailPaneSystemUI(
                uiState = preference.systemUI,
                onEvent = toolsViewModel::onSystemUIEvent,
                modifier = modifier,
            )
            OneuiXPCategory.Settings -> DetailPaneSettings(
                uiState = preference.settings,
                onEvent = toolsViewModel::onSettingsEvent,
                modifier = modifier,
            )
            OneuiXPCategory.Call -> DetailPaneCall(
                uiState = preference.call,
                onEvent = toolsViewModel::onCallEvent,
                modifier = modifier,
            )
            OneuiXPCategory.Camera -> DetailPaneCamera(
                uiState = preference.camera,
                onEvent = toolsViewModel::onCameraEvent,
                modifier = modifier,
            )
            OneuiXPCategory.Browser -> DetailPaneBrowser(preference.other, toolsViewModel::onBrowserEvent, modifier)
            OneuiXPCategory.Calendar -> DetailPaneCalendar(preference.other, toolsViewModel::onCalendarEvent, modifier)
            OneuiXPCategory.DualApp -> DetailPaneDualApp(preference.other, toolsViewModel::onDualAppEvent, modifier)
            OneuiXPCategory.Gallery -> DetailPaneGallery(preference.other, toolsViewModel::onGalleryEvent, modifier)
            OneuiXPCategory.GalaxyStore -> DetailPaneGalaxyStore(preference.other, toolsViewModel::onGalaxyStoreEvent, modifier)
            OneuiXPCategory.HealthMonitor -> DetailPaneHealthMonitor(preference.other, toolsViewModel::onHealthMonitorEvent, modifier)
            OneuiXPCategory.Launcher -> DetailPaneLauncher(preference.other, toolsViewModel::onLauncherEvent, modifier)
            OneuiXPCategory.Messaging -> DetailPaneMessaging(preference.other, toolsViewModel::onMessagingEvent, modifier)
            OneuiXPCategory.Notes -> DetailPaneNotes(preference.other, toolsViewModel::onNotesEvent, modifier)
            OneuiXPCategory.PhotoRetouching -> DetailPanePhotoRetouching(preference.other, toolsViewModel::onPhotoRetouchingEvent, modifier)
            OneuiXPCategory.SketchBook -> DetailPaneSketchBook(preference.other, toolsViewModel::onSketchBookEvent, modifier)
            OneuiXPCategory.SPen -> DetailPaneSPen(preference.other, toolsViewModel::onSPenEvent, modifier)
            OneuiXPCategory.ThemeCenter -> DetailPaneThemeCenter(preference.other, toolsViewModel::onThemeCenterEvent, modifier)
            OneuiXPCategory.Video -> DetailPaneVideo(preference.other, toolsViewModel::onVideoEvent, modifier)
            OneuiXPCategory.WatchManager -> DetailPaneWatchManager(preference.other, toolsViewModel::onWatchManagerEvent, modifier)
            OneuiXPCategory.Weather -> DetailPaneWeather(preference.other, toolsViewModel::onWeatherEvent, modifier)
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
                        title = { Text(oneuiXPCategoryTitle(category)) },
                        actions = {
                            if (category == OneuiXPCategory.SystemUI) {
                                RebootListPopup()
                            }
                        },
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
                            title = oneuiXPCategoryTitle(category),
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
                            actions = {
                                if (category == OneuiXPCategory.SystemUI) {
                                    RebootListPopup()
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
private fun OneuiXPCategoryMaterial(
    category: OneuiXPCategory,
    groups: List<OneuiXPSettingGroup>,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
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
                title = { Text(oneuiXPCategoryTitle(category)) },
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
            if (category == OneuiXPCategory.SystemUI) {
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
                                val synchronized =
                                    OneuiXPPreferenceBridge.synchronizeNow(oneuiXPApp)
                                if (!synchronized) {
                                    Toast.makeText(
                                        context,
                                        R.string.oneuixp_preferenceSync_failed,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@launch
                                }
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
                                    if (restarted) R.string.oneuixp_restartSystemUI_success
                                    else R.string.oneuixp_restartSystemUI_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    ) {
                        Text(stringResource(com.oneuixp.common.R.string.restartSystemUI))
                    }
                }
            }

            groups
                .groupBy { it.section }
                .forEach { (section, sectionGroups) ->
                    SegmentedColumn(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        title = oneuiXPSectionTitle(section),
                        content = buildList {
                            sectionGroups.forEach { group ->
                                group.items.forEachIndexed { index, spec ->
                                    add {
                                        MaterialSettingRow(
                                            spec = spec,
                                            preference = preference,
                                            toolsViewModel = toolsViewModel,
                                            hasChildren = OneuiXPLayoutRules.hasChildren(spec.path) ||
                                                (index == 0 && group.items.size > 1),
                                            isChild = index > 0,
                                        )
                                    }

                                    if (
                                        category == OneuiXPCategory.SystemUI &&
                                        section == "Quick settings" &&
                                        spec.path == "systemUI.qs.setQsClockMonospaced" &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                                    ) {
                                        add {
                                            MaterialOneuiXPInfoRow(
                                                title = stringResource(R.string.oneuixp_turnOn5gQsTile_title),
                                                summary = stringResource(R.string.oneuixp_root5gQsTile_summary),
                                                icon = ImageVector.vectorResource(R.drawable.five_g),
                                            )
                                        }
                                        add {
                                            MaterialOneuiXPInfoRow(
                                                title = stringResource(R.string.oneuixp_immersiveModeQsTile_title),
                                                summary = stringResource(R.string.oneuixp_rootImmersiveModeQsTile_summary),
                                                icon = ImageVector.vectorResource(R.drawable.immersive_mode),
                                            )
                                        }
                                    }
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
private fun MaterialOneuiXPInfoRow(
    title: String,
    summary: String,
    icon: ImageVector,
) {
    SegmentedListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
            )
        },
    )
}

@Composable
private fun MaterialSettingRow(
    spec: OneuiXPSettingSpec,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
    hasChildren: Boolean = false,
    isChild: Boolean = false,
) {
    val title = oneuiXPSettingTitle(spec)
    val summary = oneuiXPSettingSummary(spec)
    val icon = oneuiXPSettingIcon(spec)

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
        OneuiXPSettingType.Toggle -> {
            SegmentedSwitchItem(
                icon = icon,
                title = title,
                summary = summary,
                checked = toolsViewModel.checked(preference, spec),
                showChildIndicator = hasChildren,
                onCheckedChange = { toolsViewModel.setChecked(spec, it) },
            )
        }

        OneuiXPSettingType.PowerAction -> {
            MaterialPowerActionRow(
                spec = spec,
                preference = preference,
                toolsViewModel = toolsViewModel,
            )
        }

        OneuiXPSettingType.ChoiceInt,
        OneuiXPSettingType.ChoiceText -> {
            val current = toolsViewModel.value(preference, spec)
            val labels = oneuiXPChoiceLabels(spec)
            val index = spec.choices.indexOfFirst { it.value == current }
                .coerceAtLeast(0)

            SegmentedDropdownItem(
                icon = icon,
                title = title,
                summary = summary,
                items = labels,
                selectedIndex = index,
                onItemSelected = { selected ->
                    spec.choices.getOrNull(selected)?.let { choice ->
                        when (spec.path) {
                            "systemUI.statusBar.physicalEsimAdapterSimSlot" ->
                                toolsViewModel.onSystemUIEvent(
                                    SystemUIEvent.StatusBar.PhysicalEsimAdapterSimSlot(
                                        choice.value.toIntOrNull() ?: 0
                                    )
                                )

                            "systemUI.statusBar.statusBarDoubleLineClockSize" ->
                                toolsViewModel.onSystemUIEvent(
                                    SystemUIEvent.StatusBar.StatusBarDoubleLineClockSize(
                                        choice.value
                                    )
                                )

                            "other.watchPairingConnectionMode" ->
                                toolsViewModel.onWatchManagerEvent(
                                    WatchManagerEvent.WatchPairingConnectionMode(
                                        choice.value.toIntOrNull() ?: 0
                                    )
                                )

                            else -> toolsViewModel.setRawValue(spec, choice.value)
                        }
                    }
                },
            )
        }

        OneuiXPSettingType.IntValue,
        OneuiXPSettingType.FloatValue,
        OneuiXPSettingType.TextValue -> {
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
        stringResource(R.string.oneuixp_statusBarTopPaddingDp_summary, value)

    "systemUI.statusBar.statusBarBottomPaddingDp" ->
        stringResource(R.string.oneuixp_statusBarBottomPaddingDp_summary, value)

    "systemUI.statusBar.batteryIconWidthScale",
    "systemUI.statusBar.batteryIconHeightScale" -> "%.2f".format(value)

    "systemUI.statusBar.fold7DoubleLineClockTimeScale" ->
        stringResource(R.string.oneuixp_fold7DoubleLineClockTimeScale_summary, value)

    "systemUI.statusBar.fold7DoubleLineClockDateScale" ->
        stringResource(R.string.oneuixp_fold7DoubleLineClockDateScale_summary, value)

    "systemUI.statusBar.doubleLineClockGapDp" ->
        stringResource(R.string.oneuixp_statusBarDoubleLineClockGap_summary, value)

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
    spec: OneuiXPSettingSpec,
    value: Float,
    config: MaterialSliderConfig,
    toolsViewModel: OneuiXPToolsViewModel,
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
    spec: OneuiXPSettingSpec,
    title: String,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
    config: MaterialSliderConfig,
    isChild: Boolean,
) {
    val current = toolsViewModel.value(preference, spec).toFloatOrNull() ?: config.min
    var value by remember(spec.path, current) {
        mutableStateOf(current.coerceIn(config.min, config.max))
    }

    SegmentedSliderItem(
        icon = if (isChild) null else oneuiXPSettingIcon(spec),
        title = if (
            !isChild ||
            spec.path == "systemUI.statusBar.fold7DoubleLineClockTimeScale" ||
            spec.path == "systemUI.statusBar.fold7DoubleLineClockDateScale" ||
            spec.path == "systemUI.statusBar.doubleLineClockGapDp"
        ) title else null,
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
private fun MaterialPowerActionRow(
    spec: OneuiXPSettingSpec,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
) {
    val name = spec.powerActionName ?: return
    val actions = PowerMenuAction.normalize(preference.systemUI.other.powerMenuActions)
    val index = actions.indexOfFirst { it.name == name }
    val action = actions.getOrNull(index) ?: return
    val visiblePosition = actions.take(index + 1).count { it.visible }

    SegmentedListItem(
        headlineContent = { Text(oneuiXPSettingTitle(spec)) },
        supportingContent = {
            Text(
                if (action.visible) {
                    stringResource(
                        R.string.oneuixp_powerMenuActionVisible_summary,
                        visiblePosition,
                    )
                } else {
                    stringResource(R.string.oneuixp_powerMenuActionHidden_summary)
                }
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    enabled = index > 0,
                    onClick = {
                        toolsViewModel.onSystemUIEvent(
                            SystemUIEvent.Other.PowerMenuActions(
                                actions.movePowerAction(index, index - 1)
                            )
                        )
                    },
                ) {
                    Text(stringResource(R.string.oneuixp_moveUp))
                }
                TextButton(
                    enabled = index in 0 until actions.lastIndex,
                    onClick = {
                        toolsViewModel.onSystemUIEvent(
                            SystemUIEvent.Other.PowerMenuActions(
                                actions.movePowerAction(index, index + 1)
                            )
                        )
                    },
                ) {
                    Text(stringResource(R.string.oneuixp_moveDown))
                }
                ExpressiveSwitch(
                    checked = action.visible,
                    onCheckedChange = { visible ->
                        toolsViewModel.onSystemUIEvent(
                            SystemUIEvent.Other.PowerMenuActions(
                                actions.mapIndexed { actionIndex, item ->
                                    if (actionIndex == index) {
                                        item.copy(visible = visible)
                                    } else {
                                        item
                                    }
                                }
                            )
                        )
                    },
                )
            }
        },
    )
}

private fun List<PowerMenuAction>.movePowerAction(
    fromIndex: Int,
    toIndex: Int,
): List<PowerMenuAction> = toMutableList().apply {
    if (
        fromIndex !in indices ||
        toIndex !in indices ||
        fromIndex == toIndex
    ) {
        return@apply
    }
    val moved = removeAt(fromIndex)
    add(toIndex, moved)
}

@Composable
private fun NetworkSpeedThresholdMaterial(
    spec: OneuiXPSettingSpec,
    title: String,
    summary: String?,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
) {
    val current = toolsViewModel.value(preference, spec).toIntOrNull() ?: 0
    var expanded by remember(spec.path) { mutableStateOf(false) }
    var thresholdText by remember(spec.path, current) {
        mutableStateOf(current.coerceAtLeast(1).toString())
    }

    Column {
        SegmentedSwitchItem(
            icon = oneuiXPSettingIcon(spec),
            title = title,
            summary = if (current > 0) "$current KB/s" else summary,
            checked = current > 0,
            showChildIndicator = true,
            onRowClick = { expanded = !expanded },
            onCheckedChange = { enabled ->
                if (enabled) {
                    val next = thresholdText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    thresholdText = next.toString()
                    expanded = true
                    toolsViewModel.onSystemUIEvent(
                        SystemUIEvent.StatusBar.NetworkSpeedThreshold(next)
                    )
                } else {
                    expanded = false
                    toolsViewModel.onSystemUIEvent(
                        SystemUIEvent.StatusBar.NetworkSpeedThreshold(0)
                    )
                }
            },
        )

        if (expanded && current > 0) {
            SegmentedTextField(
                label = stringResource(R.string.oneuixp_networkSpeedThreshold_label),
                value = thresholdText,
                onValueChange = { raw ->
                    thresholdText = raw.filter(Char::isDigit)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                trailingContent = {
                    TextButton(
                        onClick = {
                            val value = thresholdText.toIntOrNull()?.coerceAtLeast(1)
                                ?: return@TextButton
                            thresholdText = value.toString()
                            toolsViewModel.onSystemUIEvent(
                                SystemUIEvent.StatusBar.NetworkSpeedThreshold(value)
                            )
                        },
                    ) {
                        Text(stringResource(R.string.oneuixp_confirm))
                    }
                },
            )
        }
    }
}

@Composable
private fun MaterialClockFormatEditor(
    spec: OneuiXPSettingSpec,
    title: String,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
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
        // OneuiXP shows the formatted preview above the raw format text.
        // The parent switch already carries the setting title.
        label = preview.ifBlank { current },
        value = edit,
        onValueChange = {
            edit = it
            preview = runCatching {
                StatusBarClockFormatter.format(it, LocalDateTime.now())
            }.getOrElse { "error" }
        },
        singleLine = false,
        leadingContent = null,
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
                Text(stringResource(R.string.oneuixp_confirm))
            }
        },
        supportingContent = null,
    )
}

@Composable
private fun EditableMaterialValue(
    spec: OneuiXPSettingSpec,
    title: String,
    summary: String?,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
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
            if (!requiresConfirm && (spec.type == OneuiXPSettingType.TextValue || validNumber(spec, it))) {
                toolsViewModel.setRawValue(spec, it)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = when (spec.type) {
                OneuiXPSettingType.IntValue -> KeyboardType.Number
                OneuiXPSettingType.FloatValue -> KeyboardType.Decimal
                else -> KeyboardType.Text
            },
        ),
        singleLine = spec.path != "systemUI.statusBar.statusBarClockFormat",
        leadingContent = if (showIcon) {
            oneuiXPSettingIcon(spec)?.let { imageVector ->
                { Icon(imageVector = imageVector, contentDescription = null) }
            }
        } else {
            null
        },
        trailingContent = if (requiresConfirm) {
            {
                TextButton(
                    onClick = {
                        if (spec.type == OneuiXPSettingType.TextValue || validNumber(spec, edit)) {
                            toolsViewModel.setRawValue(spec, edit)
                        }
                    },
                ) {
                    Text(stringResource(R.string.oneuixp_confirm))
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
private fun OneuiXPCategoryMiuix(
    category: OneuiXPCategory,
    groups: List<OneuiXPSettingGroup>,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
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
                    title = oneuiXPCategoryTitle(category),
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
                                text = oneuiXPSectionTitle(group.section),
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
    spec: OneuiXPSettingSpec,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
) {
    val title = oneuiXPSettingTitle(spec)
    val summary = oneuiXPSettingSummary(spec)
    val icon = oneuiXPSettingIcon(spec)

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
        OneuiXPSettingType.Toggle,
        OneuiXPSettingType.PowerAction -> {
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

        OneuiXPSettingType.ChoiceInt,
        OneuiXPSettingType.ChoiceText -> {
            val current = toolsViewModel.value(preference, spec)
            val labels = oneuiXPChoiceLabels(spec)
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

        OneuiXPSettingType.IntValue,
        OneuiXPSettingType.FloatValue,
        OneuiXPSettingType.TextValue -> {
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
    spec: OneuiXPSettingSpec,
    title: String,
    summary: String?,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
) {
    val current = toolsViewModel.value(preference, spec).toIntOrNull() ?: 0
    SwitchPreference(
        title = title,
        summary = if (current > 0) "$current KB/s" else summary,
        startAction = oneuiXPSettingIcon(spec)?.let { imageVector ->
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
    spec: OneuiXPSettingSpec,
    title: String,
    summary: String?,
    preference: Preference,
    toolsViewModel: OneuiXPToolsViewModel,
    showIcon: Boolean = true,
) {
    val current = toolsViewModel.value(preference, spec)
    var edit by remember(spec.path, current) { mutableStateOf(current) }

    EditText(
        title = title,
        summary = summary,
        startAction = if (showIcon) {
            oneuiXPSettingIcon(spec)?.let { imageVector ->
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
            if (spec.type == OneuiXPSettingType.TextValue || validNumber(spec, it)) {
                toolsViewModel.setRawValue(spec, it)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = when (spec.type) {
                OneuiXPSettingType.IntValue -> KeyboardType.Number
                OneuiXPSettingType.FloatValue -> KeyboardType.Decimal
                else -> KeyboardType.Text
            },
        ),
    )
}

private fun validNumber(
    spec: OneuiXPSettingSpec,
    value: String,
): Boolean = when (spec.type) {
    OneuiXPSettingType.IntValue -> value.toIntOrNull() != null
    OneuiXPSettingType.FloatValue -> value.toFloatOrNull() != null
    else -> true
}
