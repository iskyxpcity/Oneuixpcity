package me.weishu.kernelsu.ui.screen.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.statusbar.StatusBridge
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** OneuiXP SystemUI status only. No KernelSU module backend. */
@Composable
fun ModulePager(bottomInnerPadding: Dp, isCurrentPage: Boolean = true) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> {
            val scrollBehavior = MiuixScrollBehavior()
            val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
            val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
            Scaffold(
                topBar = {
                    BlurredBar(backdrop) {
                        TopAppBar(
                            color = barColor,
                            title = stringResource(R.string.oneuixp_status),
                            scrollBehavior = scrollBehavior,
                        )
                    }
                },
                popupHost = {},
                contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal),
            ) { innerPadding ->
                androidx.compose.foundation.layout.Box(
                    modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .verticalScroll(rememberScrollState())
                            .padding(innerPadding)
                            .padding(horizontal = 12.dp),
                    ) {
                        StatusPageContent(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
        UiMode.Material -> {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            ExpressiveScaffold(
                topBar = {
                    LargeFlexibleTopAppBar(
                        title = { Text(stringResource(R.string.oneuixp_status)) },
                        colors = expressiveTopAppBarColors(),
                        windowInsets = WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        scrollBehavior = scrollBehavior,
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ) { innerPadding ->
                Column(
                    modifier = Modifier.fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding),
                ) {
                    StatusPageContent(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun StatusPageContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    // A report confirms module load, not that a specific status bar hook works.
    val loaded = remember(context, refresh) { StatusBridge.recentlyLoaded(context) }
    val reports = remember(context, refresh) { StatusBridge.loadReportCount(context) }
    val diagnostic = remember(context, refresh) { StatusBridge.diagnosticSummary(context) }
    val ageSeconds = remember(context, refresh) { StatusBridge.lastReportAgeSeconds(context) }
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("OneuiXP · 状态栏")
        Text(if (loaded) "本次开机已收到 SystemUI 模块加载报告（功能尚未验证）"
             else "未收到本次开机的 LSPosed / SystemUI 加载报告，当前未确认工作")
        Text("本次开机加载报告：${reports} 次（不等于实际注入次数）")
        Text(if (ageSeconds >= 0) "最近一次报告：${ageSeconds} 秒前" else "最近一次报告：本次开机无记录")
        Text("功能探测：${diagnostic}")
        Text("LSPosed 作用域数量与实际注入次数：尚未接入独立检测")
        androidx.compose.material3.Button(
            onClick = { refresh++ },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("刷新检测") }
    }
}
