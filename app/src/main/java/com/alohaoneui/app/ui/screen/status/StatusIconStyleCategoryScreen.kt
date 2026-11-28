package com.alohaoneui.app.ui.screen.status

import androidx.compose.foundation.ScrollState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.material.ExpressiveLargeTopAppBar
import com.alohaoneui.app.ui.component.material.ExpressiveScaffold
import com.alohaoneui.app.ui.component.material.TopBarBackButton
import com.alohaoneui.app.ui.component.rebootlistpopup.RebootListPopup
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.theme.LocalEnableBlur
import com.alohaoneui.app.ui.util.BlurredBar
import com.alohaoneui.app.ui.util.rememberBlurBackdrop
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun StatusIconStyleCategoryScreen(targetKey: String) {
    val target = StatusIconStyleTarget.fromKey(targetKey) ?: return
    val navigator = LocalNavigator.current
    when (LocalUiMode.current) {
        UiMode.Material -> StatusIconStyleMaterial(target, navigator::pop)
        UiMode.Miuix -> StatusIconStyleMiuix(target, navigator::pop)
    }
}

@Composable
private fun Modifier.statusIconStyleScroll(state: ScrollState): Modifier =
    when (LocalUiMode.current) {
        UiMode.Miuix -> this.overScrollVertical().scrollEndHaptic().verticalScroll(state)
        UiMode.Material -> this.verticalScroll(state).padding(top = 8.dp)
    }

@Composable
private fun StatusIconStyleContent(
    target: StatusIconStyleTarget,
    modifier: Modifier = Modifier,
) {
    val toolsViewModel = viewModel<AlohaOneUIToolsViewModel>()
    val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value
    Column(
        modifier = modifier.fillMaxSize().statusIconStyleScroll(rememberScrollState()),
    ) {
        StatusBarIconStylesCategoryControls(
            target = target,
            iconStyles = preference.systemUI.statusBar.iconStyles,
            onChange = { updated ->
                toolsViewModel.updateData { current ->
                    current.copy(
                        systemUI = current.systemUI.copy(
                            statusBar = current.systemUI.statusBar.copy(iconStyles = updated),
                        ),
                    )
                }
            },
            onApply = toolsViewModel::applyNow,
        )
    }
}

@Composable
private fun StatusIconStyleMaterial(target: StatusIconStyleTarget, onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    ExpressiveScaffold(
        topBar = {
            ExpressiveLargeTopAppBar(
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                title = { Text(target.title) },
                actions = { RebootListPopup() },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            StatusIconStyleContent(target, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StatusIconStyleMiuix(target: StatusIconStyleTarget, onBack: () -> Unit) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = target.title,
                    largeTitle = target.title,
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
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = (if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding),
        ) {
            StatusIconStyleContent(target, Modifier.fillMaxSize())
        }
    }
}
