package com.oneuixp.app.ui.screen.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.component.rebootlistpopup.RebootListPopup
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.util.BlurredBar
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.ui.category.onSystemUIEvent
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun StatusCategoryScreen(categoryName: String) {
    val navigator = LocalNavigator.current
    val category = runCatching { StatusCategory.valueOf(categoryName) }.getOrNull() ?: return

    when (LocalUiMode.current) {
        UiMode.Material -> StatusCategoryMaterial(
            category = category,
            onBack = navigator::pop,
        )
        UiMode.Miuix -> StatusCategoryMiuix(
            category = category,
            onBack = navigator::pop,
        )
    }
}

@Composable
private fun StatusCategoryContent(
    category: StatusCategory,
    modifier: Modifier = Modifier,
) {
    when (category) {
        StatusCategory.Padding -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusPaddingCategoryControls(
                    uiState = preference.systemUI.statusBar,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.Time -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusTimeCategoryControls(
                    uiState = preference.systemUI.statusBar,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.Notification -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusNotificationCategoryControls(
                    uiState = preference.systemUI.statusBar,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.NetworkSpeed -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusNetworkSpeedCategoryControls(
                    uiState = preference.systemUI.statusBar,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.Power -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusPowerCategoryControls(
                    uiState = preference.systemUI.statusBar,
                    onEvent = toolsViewModel::onSystemUIEvent,
                    onPowerRuntimePreview = { runtime ->
                        toolsViewModel.previewData { current ->
                            current.copy(
                                systemUI = current.systemUI.copy(
                                    statusBar = current.systemUI.statusBar.copy(
                                        powerRuntime = runtime,
                                    ),
                                ),
                            )
                        }
                    },
                    onPowerRuntimeChange = { runtime ->
                        toolsViewModel.updateData { current ->
                            current.copy(
                                systemUI = current.systemUI.copy(
                                    statusBar = current.systemUI.statusBar.copy(
                                        powerRuntime = runtime,
                                    ),
                                ),
                            )
                        }
                    },
                )
            }
        }
        StatusCategory.Signal -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusSignalCategoryControls(
                    uiState = preference.systemUI.statusBar,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.Wifi -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusWifiCategoryControls(
                    wifiRuntime = preference.systemUI.statusBar.wifiRuntime,
                    onPreview = { runtime ->
                        toolsViewModel.previewData { current ->
                            current.copy(
                                systemUI = current.systemUI.copy(
                                    statusBar = current.systemUI.statusBar.copy(
                                        wifiRuntime = runtime,
                                    ),
                                ),
                            )
                        }
                    },
                    onChange = { runtime ->
                        toolsViewModel.updateData { current ->
                            current.copy(
                                systemUI = current.systemUI.copy(
                                    statusBar = current.systemUI.statusBar.copy(
                                        wifiRuntime = runtime,
                                    ),
                                ),
                            )
                        }
                    },
                )
            }
        }
        StatusCategory.Bluetooth -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusBluetoothCategoryControls(
                    uiState = preference.systemUI.statusBar,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.Lockscreen -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusLockscreenCategoryControls(
                    statusBar = preference.systemUI.statusBar,
                    aod = preference.systemUI.aod,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.NotificationCenter -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusNotificationCenterCategoryControls(
                    uiState = preference.systemUI.notification,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.ControlCenter -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusControlCenterCategoryControls(
                    uiState = preference.systemUI.qs,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
        StatusCategory.Other -> {
            val toolsViewModel = viewModel<OneuiXToolsViewModel>()
            val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                StatusOtherCategoryControls(
                    uiState = preference.systemUI.other,
                    onEvent = toolsViewModel::onSystemUIEvent,
                )
            }
        }
    }
}

@Composable
private fun StatusCategoryMaterial(
    category: StatusCategory,
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = { Text(stringResource(category.titleRes)) },
                actions = { RebootListPopup() },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            StatusCategoryContent(
                category = category,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun StatusCategoryMiuix(
    category: StatusCategory,
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
                    title = stringResource(category.titleRes),
                    actions = { RebootListPopup() },
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
        Box(
            modifier = (if (backdrop != null) {
                Modifier.layerBackdrop(backdrop)
            } else {
                Modifier
            })
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            StatusCategoryContent(
                category = category,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
