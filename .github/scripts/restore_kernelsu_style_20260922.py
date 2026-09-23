#!/usr/bin/env python3
"""One-time, branch-only correction: KernelSU 3.3.0 theme defaults and app bars.

This script changes visual components only; it must never rewrite LSPosed or
SystemUI implementation files. Run from the OneuiXP repository root.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / 'app/src/main/java/me/weishu/kernelsu/ui'

def edit(rel, old, new):
    path = APP / rel
    text = path.read_text()
    n = text.count(old)
    if n != 1:
        raise AssertionError(f'{rel}: expected one occurrence, found {n}: {old[:70]!r}')
    path.write_text(text.replace(old, new, 1))
    print('Patched:', rel)

# v3.3.0 KernelSU's Miuix default is null in non-Monet mode; the previous
# OneuiXP forced system_accent1_500 even when the user disabled Monet.
edit('theme/MiuixTheme.kt', '''    val resolvedKeyColor: Color = if (appSettings.keyColor != 0) {
        Color(appSettings.keyColor)
    } else {
        // Match OneuiXP Material: the default accent is the Android system accent.
        val accentId = context.resources.getIdentifier("system_accent1_500", "color", "android")
        if (accentId != 0) Color(context.getColor(accentId))
        else if (darkTheme) dynamicDarkColorScheme(context).primary
        else dynamicLightColorScheme(context).primary
    }''', '''    // Original KernelSU 3.3.0 palette rules: no custom key color in normal
    // Miuix mode; use the framework's dynamic primary only for Monet mode.
    val resolvedKeyColor: Color? = when {
        appSettings.keyColor != 0 -> Color(appSettings.keyColor)
        appSettings.colorMode.isMonet ->
            if (darkTheme) dynamicDarkColorScheme(context).primary
            else dynamicLightColorScheme(context).primary
        else -> null
    }''')

# Material's Color.Unspecified is a deliberate input to the original
# rememberKernelSUColorScheme, not equivalent to a forced Android seed color.
edit('theme/MaterialTheme.kt', '''import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
''', '')
edit('theme/MaterialTheme.kt', '''    val seedColor = if (appSettings.keyColor != 0) {
        Color(appSettings.keyColor)
    } else {
        // OneuiXP default accent follows Android's system accent resource.
        // Do not derive a separate app accent from wallpaper inside the app.
        val accentId = context.resources.getIdentifier("system_accent1_500", "color", "android")
        if (accentId != 0) {
            Color(context.getColor(accentId))
        } else if (darkTheme) {
            dynamicDarkColorScheme(context).primary
        } else {
            dynamicLightColorScheme(context).primary
        }
    }

    val colorScheme = rememberKernelSUColorScheme(
        seedColor = seedColor,''', '''    val dynamicColor = appSettings.keyColor == 0

    val colorScheme = rememberKernelSUColorScheme(
        seedColor = if (dynamicColor) Color.Unspecified else Color(appSettings.keyColor),''')

# Remove custom brown fallback; make default and selected-color behavior match
# the real KernelSU Miuix icon's dynamic/static palette branches. Keep the
# requested enlarged icon size and no redundant product name at the bottom.
edit('screen/home/HomeMiuix.kt', '''                    tint = if (active) {
                        if (isDynamicColor) colorScheme.primary
                        else if (isInDarkTheme()) Color(0xFF36D167)
                        else Color(0xFF78542D)
                    } else colorScheme.error,''', '''                    tint = if (active) {
                        if (isDynamicColor) colorScheme.primary.copy(alpha = 0.8f)
                        else Color(0xFF36D167)
                    } else colorScheme.error.copy(alpha = 0.8f),''')

# Material was left at 138dp/38%-alpha and kept a bottom OneuiXP label even
# after Miuix changed. Apply the same product-neutral dashboard corrections.
edit('screen/home/HomeMaterial.kt', '''                        .size(138.dp),
                    tint = contentColor.copy(alpha = 0.38f),''', '''                        .size(165.dp),
                    tint = MaterialTheme.colorScheme.primary,''')
edit('screen/home/HomeMaterial.kt', '''                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "OneuiXP",
                        style = MaterialTheme.typography.titleMedium,
                    )''', '''                    Spacer(Modifier.weight(1f))''')

# All four principal pages must use the existing KernelSU collapsing title
# scaffold. Home and Settings already do, as do both ColorPalette pages;
# the stripped-down OneuiXP Status and Tools tabs are the missing two.
module = r'''package me.weishu.kernelsu.ui.screen.module

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
'''

superuser = r'''package me.weishu.kernelsu.ui.screen.superuser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** Empty OneuiXP tools tab. No KernelSU superuser interface or root access. */
@Composable
fun SuperUserPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
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
                            title = stringResource(R.string.oneuixp_tools),
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
                            .padding(innerPadding),
                    ) {
                        // Keep the tools page intentionally empty while enabling
                        // the upstream scroll/large-title collapse gesture.
                        Spacer(Modifier.height(screenHeight))
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
                        title = { Text(stringResource(R.string.oneuixp_tools)) },
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
                    Spacer(Modifier.height(screenHeight))
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}
'''

(APP / 'screen/module/ModuleScreen.kt').write_text(module)
(APP / 'screen/superuser/SuperUserScreen.kt').write_text(superuser)
print('Restored collapsing app bars: status and tools, Miuix and Material')

# Consistency guard for this migration; keep the original injection backend.
assert 'system_accent1_500' not in (APP / 'theme/MiuixTheme.kt').read_text()
assert 'system_accent1_500' not in (APP / 'theme/MaterialTheme.kt').read_text()
assert 'seedColor = if (dynamicColor) Color.Unspecified' in (APP / 'theme/MaterialTheme.kt').read_text()
assert 'else -> null' in (APP / 'theme/MiuixTheme.kt').read_text()
for f in ['screen/module/ModuleScreen.kt', 'screen/superuser/SuperUserScreen.kt']:
    s = (APP / f).read_text()
    assert 'MiuixScrollBehavior()' in s and 'LargeFlexibleTopAppBar(' in s
    assert 'nestedScroll(scrollBehavior.nestedScrollConnection)' in s
print('PASS: upstream palette branches; both theme app bars; injection files untouched')
