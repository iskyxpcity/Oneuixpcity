package me.weishu.kernelsu.ui.screen.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.statusbar.StatusBridge
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** OneuiXP status page. No KernelSU module repository, installer or root operations. */
@Composable
fun ModulePager(bottomInnerPadding: Dp, isCurrentPage: Boolean = true) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> Scaffold(
            topBar = { TopAppBar(color = colorScheme.surface, title = stringResource(R.string.oneuixp_status)) }
        ) { padding -> StatusPageContent(Modifier.fillMaxSize().padding(padding).padding(bottom = bottomInnerPadding)) }
        UiMode.Material -> androidx.compose.material3.Scaffold(
            topBar = { androidx.compose.material3.TopAppBar(title = { Text(stringResource(R.string.oneuixp_status)) }) }
        ) { padding -> StatusPageContent(Modifier.fillMaxSize().padding(padding).padding(bottom = bottomInnerPadding)) }
    }
}

@Composable
private fun StatusPageContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    // Refresh is intentionally user-driven; a report does not prove a functional status-bar hook.
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
        Button(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("刷新检测") }
    }
}
