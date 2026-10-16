package com.oneuixp.app.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.oneuixp.app.oneuix.OneuiXPreferenceBridge
import com.oneuixp.app.ui.component.bottombar.BottomBar
import com.oneuixp.app.ui.component.bottombar.SideNavigationRail
import com.oneuixp.app.ui.component.bottombar.useNavigationRail
import com.oneuixp.app.ui.component.bottombar.MainPagerState
import com.oneuixp.app.ui.component.bottombar.rememberMainPagerState
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.navigation3.Navigator
import com.oneuixp.app.ui.navigation3.Route
import com.oneuixp.app.ui.navigation3.rememberNavigator
import com.oneuixp.app.ui.screen.about.AboutScreen
import com.oneuixp.app.ui.screen.about.DisclaimerScreen
import com.oneuixp.app.ui.screen.about.FeatureIntroductionScreen
import com.oneuixp.app.ui.screen.about.VersionChangelogScreen
import com.oneuixp.app.ui.screen.about.VersionChangelog261013Screen
import com.oneuixp.app.ui.screen.about.VersionChangelog261016Screen
import com.oneuixp.app.ui.screen.colorpalette.ColorPaletteScreen
import com.oneuixp.app.ui.screen.home.HomePager
import com.oneuixp.app.ui.screen.home.LocalLsposedHomeStatus
import com.oneuixp.app.ui.screen.home.rememberLsposedHomeStatus
import com.oneuixp.app.ui.screen.status.StatusPager
import com.oneuixp.app.ui.screen.status.StatusCategoryScreen
import com.oneuixp.app.ui.screen.settings.SettingPager
import com.oneuixp.app.ui.screen.tools.ToolsPager
import com.oneuixp.app.ui.screen.tools.OneuiXCategoryScreen
import com.oneuixp.app.ui.theme.LocalColorMode
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.theme.LocalEnableFloatingBottomBar
import com.oneuixp.app.ui.theme.LocalEnableFloatingBottomBarBlur
import com.oneuixp.app.ui.theme.OneuiXPTheme
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import com.oneuixp.app.ui.util.rememberContentReady
import com.oneuixp.app.ui.util.swipeBackToPop
import com.oneuixp.app.ui.util.swipeToChangeMainPage
import com.oneuixp.app.ui.viewmodel.MainActivityViewModel
import com.oneuixp.app.ui.viewmodel.MainPagerConfig
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride

class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OneuiXPreferenceBridge.ensureSynchronized(application)

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedMainPage by viewModel.selectedMainPage.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            val uiMode = uiState.uiMode
            val lsposedStatus = rememberLsposedHomeStatus()
            val darkMode = appSettings.colorMode.isDark ||
                (appSettings.colorMode.isSystem && isSystemInDarkTheme())

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            val navigator = rememberNavigator(Route.Main)
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
            }

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalEnableBlur provides uiState.enableBlur,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
                LocalUiMode provides uiMode,
                LocalLsposedHomeStatus provides lsposedStatus,
            ) {
                OneuiXPTheme(appSettings = appSettings, uiMode = uiMode) {
                    val mainScreenEntry = @Composable {
                        MainScreen(
                            initialPage = selectedMainPage,
                            onPageChanged = viewModel::setSelectedMainPage,
                            pagerGestureMode = uiState.pagerGestureMode,
                            enablePredictiveBack = uiState.enablePredictiveBack,
                        )
                    }

                    val nestedBackEventState = rememberNavigationEventState(NavigationEventInfo.None)
                    val navDisplay = @Composable {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .swipeBackToPop(
                                    enabled = uiState.enableSwipeBack && navigator.backStackSize() > 1,
                                    onBack = {
                                        if (navigator.backStackSize() > 1) navigator.pop()
                                    },
                                )
                                .swipeToChangeMainPage(
                                    enabled = navigator.backStackSize() > 1,
                                    currentPage = selectedMainPage,
                                    pageCount = MainPagerConfig.PAGE_COUNT,
                                    onPageChange = { page ->
                                        viewModel.setSelectedMainPage(page)
                                        navigator.popUntil { it == Route.Main }
                                    },
                                ),
                        ) {
                            // Navigation3 uses predictive callbacks when enabled. If the user
                            // turns predictive back off, retain ordinary back-stack navigation
                            // through the compatible OnBackPressed path.
                            BackHandler(
                                enabled = !uiState.enablePredictiveBack && navigator.backStackSize() > 1,
                            ) {
                                navigator.pop()
                            }
                            // Keep predictive back attached to the Activity-level navigator,
                            // not only to MainScreen. It must pop any nested route depth.
                            NavigationBackHandler(
                                state = nestedBackEventState,
                                isBackEnabled = uiState.enablePredictiveBack && navigator.backStackSize() > 1,
                                onBackCompleted = {
                                    if (navigator.backStackSize() > 1) navigator.pop()
                                },
                            )
                            NavDisplay(
                            backStack = navigator.backStack,
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                            onBack = { navigator.pop() },
                            entryProvider = entryProvider {
                                entry<Route.Main> { mainScreenEntry() }
                                entry<Route.About> { AboutScreen() }
                                entry<Route.Disclaimer> { DisclaimerScreen() }
                                entry<Route.FeatureIntroduction> { FeatureIntroductionScreen() }
                                entry<Route.VersionChangelog> { VersionChangelogScreen() }
                                entry<Route.VersionChangelog261013> { VersionChangelog261013Screen() }
                                entry<Route.VersionChangelog261016> { VersionChangelog261016Screen() }
                                entry<Route.ColorPalette> { ColorPaletteScreen() }
                                entry<Route.OneuiXCategory> { key -> OneuiXCategoryScreen(key.category) }
                                entry<Route.StatusCategory> { key -> StatusCategoryScreen(key.category) }
                                entry<Route.Home> { mainScreenEntry() }
                                entry<Route.Tools> { mainScreenEntry() }
                                entry<Route.Status> { mainScreenEntry() }
                                entry<Route.Settings> { mainScreenEntry() }
                            },
                            )
                        }
                    }

                    when (uiMode) {
                        UiMode.Material -> androidx.compose.material3.Scaffold { navDisplay() }
                        UiMode.Miuix -> Scaffold { navDisplay() }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OneuiXPreferenceBridge.ensureSynchronized(application)
    }
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
    pagerGestureMode: Int = PagerInterceptionMode.Native.ordinal,
    enablePredictiveBack: Boolean = false,
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { MainPagerConfig.PAGE_COUNT },
    )
    val mainPagerState = rememberMainPagerState(pagerState)

    // The main entry is saveable while a detail route is on top. If a page-switch
    // gesture changes the selected tab from a nested route, restore the PagerState
    // to that new selection when MainScreen becomes active again.
    LaunchedEffect(initialPage) {
        if (pagerState.currentPage != initialPage) {
            pagerState.animateScrollToPage(initialPage)
            mainPagerState.syncPage()
        }
    }

    val interceptPagerAxis = pagerGestureMode == PagerInterceptionMode.CrossAxisInterceptor.ordinal
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = mainPagerState.pagerState,
        snapAnimationSpec = PagerNavigationSpringSpec,
    )
    val nativePagerNestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
        mainPagerState.pagerState,
        Orientation.Horizontal,
    )
    val uiMode = LocalUiMode.current
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    val blurBackdrop = rememberBlurBackdrop(enableBlur)

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onPageChanged(settledPage)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(mainPagerState, navController, enablePredictiveBack)

    CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
        val contentReady = rememberContentReady()

        // Re-read the actual window bounds so fold/unfold and freeform resizing adapt
        // without depending on a device model or requiring an Activity restart.
        // In landscape, wide, and unfolded windows both themes use a collapsible side rail.
        // The floating bottom-bar preference is still respected on compact windows.
        val useWideLayout = useNavigationRail()
        val showNavigationRail = useWideLayout

        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(
                modifier = (if (blurBackdrop != null) {
                    Modifier.fillMaxSize().layerBackdrop(blurBackdrop)
                } else {
                    Modifier.fillMaxSize()
                }),
            ) {
                val responsivePagerModifier = if (useWideLayout) {
                    Modifier
                        .widthIn(max = 1280.dp)
                        .fillMaxSize()
                        .align(Alignment.Center)
                } else {
                    Modifier.fillMaxSize()
                }
                HorizontalPager(
                    modifier = responsivePagerModifier.then(
                        if (enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier
                    ).pagerGestureOverride(
                        pagerState = mainPagerState.pagerState,
                        mode = pagerGestureMode,
                        enabled = true,
                    ),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 3 else 0,
                    userScrollEnabled = !interceptPagerAxis,
                    pageNestedScrollConnection = if (interceptPagerAxis) {
                        PagerGestureNestedScrollConnection
                    } else {
                        nativePagerNestedScrollConnection
                    },
                    flingBehavior = pagerFlingBehavior,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (page) {
                        0 -> if (isCurrentPage || contentReady) {
                            HomePager(navController, bottomInnerPadding, isCurrentPage)
                        }
                        1 -> if (isCurrentPage || contentReady) {
                            ToolsPager(navController, bottomInnerPadding, isCurrentPage)
                        }
                        2 -> if (isCurrentPage || contentReady) {
                            StatusPager(navController, bottomInnerPadding, isCurrentPage)
                        }
                        3 -> if (isCurrentPage || contentReady) {
                            SettingPager(navController, bottomInnerPadding, isCurrentPage)
                        }
                    }
                }
            }
        }

        if (showNavigationRail) {
            // A side rail is used for expanded windows and landscape layouts; the existing
            // four destinations and their theme-specific icons remain unchanged.
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navigationBottomPadding =
                WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold {
                    Row {
                        SideNavigationRail(modifier = Modifier.fillMaxHeight())
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navigationBottomPadding)
                        }
                    }
                }

                UiMode.Miuix -> Scaffold { _ ->
                    Row {
                        SideNavigationRail(modifier = Modifier.fillMaxHeight())
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navigationBottomPadding)
                        }
                    }
                }
            }
        } else {
            // Compact windows retain the validated bottom-bar implementation, including
            // Miuix's native floating selection indicator when that style is enabled.
            val bottomBar = @Composable {
                Box(modifier = Modifier.fillMaxWidth()) {
                    BottomBar(
                        blurBackdrop = blurBackdrop,
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    bottomBar = bottomBar,
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> Scaffold(
                    bottomBar = bottomBar,
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}

@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
    enablePredictiveBack: Boolean,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main &&
                navController.backStackSize() == 1 &&
                mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    BackHandler(
        enabled = !enablePredictiveBack && isPagerBackHandlerEnabled,
    ) {
        mainState.animateToPage(0)
    }

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enablePredictiveBack && isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        },
    )
}
