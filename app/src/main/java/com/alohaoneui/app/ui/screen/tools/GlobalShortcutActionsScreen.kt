package com.alohaoneui.app.ui.screen.tools

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.R
import com.alohaoneui.app.alohaOneUIApp
import com.alohaoneui.app.alohaoneui.AlohaOneUIPreferenceBridge
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.material.SegmentedColumn
import com.alohaoneui.app.ui.component.material.SegmentedListItem
import com.alohaoneui.app.ui.component.material.alohaOneUISegmentedColors
import com.alohaoneui.app.ui.navigation3.LocalNavigator
import com.alohaoneui.app.ui.theme.LocalEnableBlur
import com.alohaoneui.app.ui.util.BlurredBar
import com.alohaoneui.app.ui.util.rememberBlurBackdrop
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel
import com.alohaoneui.common.Preference
import com.alohaoneui.mod.ui.component.AlohaOneUISlider
import com.alohaoneui.mod.ui.component.LocalAlohaOneUIGroupedPreference
import com.alohaoneui.mod.ui.component.SelectItem
import com.alohaoneui.mod.ui.component.SwitchItem
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private const val ACTION_NONE = "none"
private const val ACTION_CUSTOM_APP = "custom_app"
private const val ACTION_CUSTOM_APP_PREFIX = "custom_app:"

private data class ShortcutActionOption(
    val id: String,
    val label: String,
)

private data class ShortcutLauncherApp(
    val label: String,
    val component: ComponentName,
)

/**
 * Full migration of App20261018Aloha's “全局快捷动作” settings. The trigger descriptions,
 * thresholds, haptic controls, action choices and LSPosed/system-server scope note are kept
 * together here so the new entry doesn't silently omit controls from the source app.
 */
private val shortcutActionOptions = listOf(
    ShortcutActionOption(ACTION_NONE, "无动作"),
    ShortcutActionOption("back", "系统动作 · 返回"),
    ShortcutActionOption("home", "系统动作 · 主页"),
    ShortcutActionOption("recents", "系统动作 · 最近任务"),
    ShortcutActionOption("screenshot", "系统动作 · 截图"),
    ShortcutActionOption("smart_screenshot", "系统动作 · 智能截图"),
    ShortcutActionOption("lock_screen", "系统动作 · 关屏"),
    ShortcutActionOption("power_menu", "系统动作 · 电源菜单"),
    ShortcutActionOption("kill_foreground_app", "系统动作 · 结束当前程序"),
    ShortcutActionOption("memory_clean", "系统动作 · 内存清理"),
    ShortcutActionOption("take_photo", "相机 / 录屏 · 拍照"),
    ShortcutActionOption("quick_video", "相机 / 录屏 · 录像"),
    ShortcutActionOption("screen_record", "相机 / 录屏 · 录制屏幕"),
    ShortcutActionOption("previous_track", "媒体控制 · 上一曲"),
    ShortcutActionOption("next_track", "媒体控制 · 下一曲"),
    ShortcutActionOption("play_pause", "媒体控制 · 播放 / 暂停"),
    ShortcutActionOption("media_rewind", "媒体控制 · 快退"),
    ShortcutActionOption("media_fast_forward", "媒体控制 · 快进"),
    ShortcutActionOption("flashlight", "快捷开关 · 手电筒"),
    ShortcutActionOption("airplane_mode", "快捷开关 · 飞行模式"),
    ShortcutActionOption("bluetooth", "快捷开关 · 蓝牙"),
    ShortcutActionOption("silent_mode", "快捷开关 · 静音模式"),
    ShortcutActionOption("vibrate_mode", "快捷开关 · 振动模式"),
    ShortcutActionOption("wifi", "快捷开关 · WiFi"),
    ShortcutActionOption("mobile_data", "快捷开关 · 移动数据"),
    ShortcutActionOption("wechat_scan", "快捷支付 · 微信扫一扫"),
    ShortcutActionOption("wechat_pay", "快捷支付 · 微信付款"),
    ShortcutActionOption("wechat_collect", "快捷支付 · 微信收款"),
    ShortcutActionOption("alipay_scan", "快捷支付 · 支付宝扫码"),
    ShortcutActionOption("alipay_pay", "快捷支付 · 支付宝付款"),
    ShortcutActionOption("alipay_collect", "快捷支付 · 支付宝收款"),
    ShortcutActionOption(ACTION_CUSTOM_APP, "自定义 · 选择应用"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalShortcutActionsScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)

    when (LocalUiMode.current) {
        UiMode.Material -> {
            Scaffold(
                topBar = {
                    BlurredBar(backdrop) {
                        LargeFlexibleTopAppBar(
                            title = { Text(stringResource(R.string.alohaoneui_global_shortcut_actions_title)) },
                            navigationIcon = {
                                IconButton(onClick = navigator::pop) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    }
                },
            ) { innerPadding ->
                Column(
                    modifier = (if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding),
                ) {
                    GlobalShortcutActionsContent()
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        UiMode.Miuix -> {
            val scrollBehavior = MiuixScrollBehavior()
            val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
            MiuixScaffold(
                topBar = {
                    BlurredBar(backdrop) {
                        MiuixTopAppBar(
                            color = barColor,
                            title = stringResource(R.string.alohaoneui_global_shortcut_actions_title),
                            navigationIcon = {
                                MiuixIconButton(onClick = navigator::pop) {
                                    MiuixIcon(
                                        imageVector = MiuixIcons.Back,
                                        contentDescription = null,
                                        tint = colorScheme.onSurface,
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    }
                },
                popupHost = {},
            ) { innerPadding ->
                Column(
                    modifier = (if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding),
                ) {
                    GlobalShortcutActionsContent()
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun GlobalShortcutActionsContent() {
    val model = viewModel<AlohaOneUIToolsViewModel>()
    val preference by model.preference.collectAsStateWithLifecycle()
    val settings = preference.other.globalShortcutActions

    fun update(change: (Preference.GlobalShortcutActions) -> Preference.GlobalShortcutActions) {
        model.updateData { current ->
            current.copy(
                other = current.other.copy(
                    globalShortcutActions = change(current.other.globalShortcutActions)
                )
            )
        }
    }

    SectionCard(
        title = "全局快捷动作",
        summary = "集中管理三指、状态栏、息屏按键等快捷触发。所有动作默认关闭；保存设置后会通过 Aloha OneUI 的远程 Preference 链同步给 LSPosed 模块。",
    ) {
        ShortcutBodyText(
            "说明：部分动作需要系统框架或 SystemUI 作用域。请在 LSPosed 中确认 Aloha OneUI 已勾选 system（系统框架）与 com.android.systemui；修改后如果某个 Hook 尚未载入，可软重启对应系统进程后再测试。",
        )
    }

    SectionCard(
        title = "全局三指滑动",
        summary = "三根手指同时向上、下、左或右滑动，可触发所选快捷动作。",
    ) {
        SwitchItem(
            title = "启用全局三指手势",
            summary = "监听全局触摸事件；关闭后不再接管三指滑动。",
            checked = settings.threeFingerGestureEnabled,
            onCheckedChange = { value -> update { it.copy(threeFingerGestureEnabled = value) } },
        )
        NumericShortcutSlider(
            title = "禁止交互阈值",
            summary = "阈值指三根手指移动的距离。数值越小越灵敏、越容易接管触摸；数值越大误触越少，但页面可能先随手势滚动。",
            value = settings.threeFingerTouchCancelThresholdDp,
            range = 1..20,
            enabled = settings.threeFingerGestureEnabled,
            format = { "三指滑动：${it} dp 后接管" },
            onCommit = { value -> update { it.copy(threeFingerTouchCancelThresholdDp = value) } },
        )
        SwitchItem(
            title = "振动反馈",
            summary = "动作触发时提供触觉反馈。",
            checked = settings.threeFingerHapticEnabled,
            enabled = settings.threeFingerGestureEnabled,
            onCheckedChange = { value -> update { it.copy(threeFingerHapticEnabled = value) } },
        )
        HapticStrengthSlider(
            value = settings.threeFingerHapticStrength,
            enabled = settings.threeFingerGestureEnabled && settings.threeFingerHapticEnabled,
            onCommit = { value -> update { it.copy(threeFingerHapticStrength = value) } },
        )
        ActionSelector("三指上划", "三根手指同时向上滑动。", settings.threeFingerSwipeUpAction, settings.threeFingerGestureEnabled) {
            selected -> update { current -> current.copy(threeFingerSwipeUpAction = selected) }
        }
        ActionSelector("三指下划", "三根手指同时向下滑动。", settings.threeFingerSwipeDownAction, settings.threeFingerGestureEnabled) {
            selected -> update { current -> current.copy(threeFingerSwipeDownAction = selected) }
        }
        ActionSelector("三指左划", "三根手指同时向左滑动。", settings.threeFingerSwipeLeftAction, settings.threeFingerGestureEnabled) {
            selected -> update { current -> current.copy(threeFingerSwipeLeftAction = selected) }
        }
        ActionSelector("三指右划", "三根手指同时向右滑动。", settings.threeFingerSwipeRightAction, settings.threeFingerGestureEnabled) {
            selected -> update { current -> current.copy(threeFingerSwipeRightAction = selected) }
        }
    }

    SectionCard(
        title = "三指捏合",
        summary = "三根手指同时向内收拢，超过阈值时触发动作。",
    ) {
        SwitchItem(
            title = "启用三指捏合",
            summary = "独立于三指滑动开关。",
            checked = settings.threeFingerPinchEnabled,
            onCheckedChange = { value -> update { it.copy(threeFingerPinchEnabled = value) } },
        )
        NumericShortcutSlider(
            title = "捏合阈值",
            summary = "以三指平均跨度的缩小距离判断是否完成捏合。",
            value = settings.threeFingerPinchThresholdDp,
            range = 8..160,
            enabled = settings.threeFingerPinchEnabled,
            format = { "跨度变化：${it} dp" },
            onCommit = { value -> update { it.copy(threeFingerPinchThresholdDp = value) } },
        )
        SwitchItem(
            title = "振动反馈",
            checked = settings.threeFingerPinchHapticEnabled,
            enabled = settings.threeFingerPinchEnabled,
            onCheckedChange = { value -> update { it.copy(threeFingerPinchHapticEnabled = value) } },
        )
        HapticStrengthSlider(
            value = settings.threeFingerPinchHapticStrength,
            enabled = settings.threeFingerPinchEnabled && settings.threeFingerPinchHapticEnabled,
            onCommit = { value -> update { it.copy(threeFingerPinchHapticStrength = value) } },
        )
        ActionSelector("捏合动作", "三根手指向内靠拢时执行。", settings.threeFingerPinchAction, settings.threeFingerPinchEnabled) {
            selected -> update { current -> current.copy(threeFingerPinchAction = selected) }
        }
    }

    SectionCard(
        title = "三指展开",
        summary = "三根手指同时向外张开，超过阈值时触发动作。",
    ) {
        SwitchItem(
            title = "启用三指展开",
            summary = "独立于三指滑动与捏合。",
            checked = settings.threeFingerSpreadEnabled,
            onCheckedChange = { value -> update { it.copy(threeFingerSpreadEnabled = value) } },
        )
        NumericShortcutSlider(
            title = "展开阈值",
            summary = "以三指平均跨度的增大距离判断是否完成展开。",
            value = settings.threeFingerSpreadThresholdDp,
            range = 8..160,
            enabled = settings.threeFingerSpreadEnabled,
            format = { "跨度变化：${it} dp" },
            onCommit = { value -> update { it.copy(threeFingerSpreadThresholdDp = value) } },
        )
        SwitchItem(
            title = "振动反馈",
            checked = settings.threeFingerSpreadHapticEnabled,
            enabled = settings.threeFingerSpreadEnabled,
            onCheckedChange = { value -> update { it.copy(threeFingerSpreadHapticEnabled = value) } },
        )
        HapticStrengthSlider(
            value = settings.threeFingerSpreadHapticStrength,
            enabled = settings.threeFingerSpreadEnabled && settings.threeFingerSpreadHapticEnabled,
            onCommit = { value -> update { it.copy(threeFingerSpreadHapticStrength = value) } },
        )
        ActionSelector("展开动作", "三根手指向外张开时执行。", settings.threeFingerSpreadAction, settings.threeFingerSpreadEnabled) {
            selected -> update { current -> current.copy(threeFingerSpreadAction = selected) }
        }
    }

    SectionCard(
        title = "悬浮球",
        summary = "设置悬浮球颜色、大小、自动贴边时间与单击/双击/划动动作。",
    ) {
        SwitchItem(
            title = "启用悬浮球",
            summary = "在系统界面上显示可拖动悬浮球；长按后可拖动位置。",
            checked = settings.floatingBallEnabled,
            onCheckedChange = { value -> update { it.copy(floatingBallEnabled = value) } },
        )
        FloatingBallColorControl(
            argb = settings.floatingBallColorArgb,
            enabled = settings.floatingBallEnabled,
            onCommit = { value -> update { it.copy(floatingBallColorArgb = value) } },
        )
        NumericShortcutSlider(
            title = "悬浮球大小",
            summary = "调整悬浮球直径。",
            value = settings.floatingBallSizeDp,
            range = 40..88,
            enabled = settings.floatingBallEnabled,
            format = { "${it} dp" },
            onCommit = { value -> update { it.copy(floatingBallSizeDp = value) } },
        )
        NumericShortcutSlider(
            title = "自动贴边延时",
            summary = "松开悬浮球后等待指定时间，再自动靠向屏幕边缘；0 表示关闭自动贴边。",
            value = settings.floatingBallAutoEdgeDelaySeconds,
            range = 0..10,
            enabled = settings.floatingBallEnabled,
            format = { "${it} 秒" },
            onCommit = { value -> update { it.copy(floatingBallAutoEdgeDelaySeconds = value) } },
        )
        SwitchItem(
            title = "振动反馈",
            checked = settings.floatingBallHapticEnabled,
            enabled = settings.floatingBallEnabled,
            onCheckedChange = { value -> update { it.copy(floatingBallHapticEnabled = value) } },
        )
        HapticStrengthSlider(
            value = settings.floatingBallHapticStrength,
            enabled = settings.floatingBallEnabled && settings.floatingBallHapticEnabled,
            onCommit = { value -> update { it.copy(floatingBallHapticStrength = value) } },
        )
        ActionSelector("单击动作", "轻触一次悬浮球。", settings.floatingBallClickAction, settings.floatingBallEnabled) {
            selected -> update { current -> current.copy(floatingBallClickAction = selected) }
        }
        ActionSelector("双击动作", "快速连续轻触两次悬浮球。", settings.floatingBallDoubleClickAction, settings.floatingBallEnabled) {
            selected -> update { current -> current.copy(floatingBallDoubleClickAction = selected) }
        }
        ActionSelector("上划动作", "从悬浮球向上滑动。", settings.floatingBallSwipeUpAction, settings.floatingBallEnabled) {
            selected -> update { current -> current.copy(floatingBallSwipeUpAction = selected) }
        }
        ActionSelector("下划动作", "从悬浮球向下滑动。", settings.floatingBallSwipeDownAction, settings.floatingBallEnabled) {
            selected -> update { current -> current.copy(floatingBallSwipeDownAction = selected) }
        }
        ActionSelector("左划动作", "从悬浮球向左滑动。", settings.floatingBallSwipeLeftAction, settings.floatingBallEnabled) {
            selected -> update { current -> current.copy(floatingBallSwipeLeftAction = selected) }
        }
        ActionSelector("右划动作", "从悬浮球向右滑动。", settings.floatingBallSwipeRightAction, settings.floatingBallEnabled) {
            selected -> update { current -> current.copy(floatingBallSwipeRightAction = selected) }
        }
    }

    SectionCard(
        title = "状态栏快捷动作",
        summary = "设置状态栏双击、左上角/右上角单击和长按动作。",
    ) {
        SwitchItem(
            title = "启用状态栏快捷动作",
            summary = "只在状态栏区域内识别触发；手指移动超过点击范围后会取消。",
            checked = settings.statusBarShortcutEnabled,
            onCheckedChange = { value -> update { it.copy(statusBarShortcutEnabled = value) } },
        )
        SwitchItem(
            title = "振动反馈",
            checked = settings.statusBarShortcutHapticEnabled,
            enabled = settings.statusBarShortcutEnabled,
            onCheckedChange = { value -> update { it.copy(statusBarShortcutHapticEnabled = value) } },
        )
        HapticStrengthSlider(
            value = settings.statusBarShortcutHapticStrength,
            enabled = settings.statusBarShortcutEnabled && settings.statusBarShortcutHapticEnabled,
            onCommit = { value -> update { it.copy(statusBarShortcutHapticStrength = value) } },
        )
        ActionSelector("状态栏双击", "快速双击状态栏任意位置。", settings.statusBarDoubleClickAction, settings.statusBarShortcutEnabled) {
            selected -> update { current -> current.copy(statusBarDoubleClickAction = selected) }
        }
        ActionSelector("左侧单击", "单击状态栏左半侧。", settings.statusBarLeftClickAction, settings.statusBarShortcutEnabled) {
            selected -> update { current -> current.copy(statusBarLeftClickAction = selected) }
        }
        ActionSelector("左侧长按", "长按状态栏左半侧。", settings.statusBarLeftLongPressAction, settings.statusBarShortcutEnabled) {
            selected -> update { current -> current.copy(statusBarLeftLongPressAction = selected) }
        }
        ActionSelector("右侧单击", "单击状态栏右半侧。", settings.statusBarRightClickAction, settings.statusBarShortcutEnabled) {
            selected -> update { current -> current.copy(statusBarRightClickAction = selected) }
        }
        ActionSelector("右侧长按", "长按状态栏右半侧。", settings.statusBarRightLongPressAction, settings.statusBarShortcutEnabled) {
            selected -> update { current -> current.copy(statusBarRightLongPressAction = selected) }
        }
    }

    SectionCard(
        title = "虚拟导航键动作",
        summary = "设置返回、Home、最近任务按钮的双击与长按动作。",
    ) {
        SwitchItem(
            title = "启用虚拟导航键动作",
            summary = "适用于系统提供的虚拟返回、主页、最近任务按键；手势导航模式下没有这些虚拟键。",
            checked = settings.navigationKeyShortcutEnabled,
            onCheckedChange = { value -> update { it.copy(navigationKeyShortcutEnabled = value) } },
        )
        SwitchItem(
            title = "振动反馈",
            checked = settings.navigationKeyHapticEnabled,
            enabled = settings.navigationKeyShortcutEnabled,
            onCheckedChange = { value -> update { it.copy(navigationKeyHapticEnabled = value) } },
        )
        HapticStrengthSlider(
            value = settings.navigationKeyHapticStrength,
            enabled = settings.navigationKeyShortcutEnabled && settings.navigationKeyHapticEnabled,
            onCommit = { value -> update { it.copy(navigationKeyHapticStrength = value) } },
        )
        ActionSelector("返回键双击", "快速双击返回键。", settings.navigationBackDoubleTapAction, settings.navigationKeyShortcutEnabled) {
            selected -> update { current -> current.copy(navigationBackDoubleTapAction = selected) }
        }
        ActionSelector("返回键长按", "长按返回键。", settings.navigationBackLongPressAction, settings.navigationKeyShortcutEnabled) {
            selected -> update { current -> current.copy(navigationBackLongPressAction = selected) }
        }
        ActionSelector("主页键双击", "快速双击 Home 键。", settings.navigationHomeDoubleTapAction, settings.navigationKeyShortcutEnabled) {
            selected -> update { current -> current.copy(navigationHomeDoubleTapAction = selected) }
        }
        ActionSelector("主页键长按", "长按 Home 键。", settings.navigationHomeLongPressAction, settings.navigationKeyShortcutEnabled) {
            selected -> update { current -> current.copy(navigationHomeLongPressAction = selected) }
        }
        ActionSelector("最近任务键双击", "快速双击最近任务键。", settings.navigationRecentsDoubleTapAction, settings.navigationKeyShortcutEnabled) {
            selected -> update { current -> current.copy(navigationRecentsDoubleTapAction = selected) }
        }
        ActionSelector("最近任务键长按", "长按最近任务键。", settings.navigationRecentsLongPressAction, settings.navigationKeyShortcutEnabled) {
            selected -> update { current -> current.copy(navigationRecentsLongPressAction = selected) }
        }
    }

    SectionCard(
        title = "息屏音量键动作",
        summary = "息屏时长按音量键切歌，双击音量键播放/暂停；亮屏不接管原来的音量键。",
    ) {
        SwitchItem(
            title = "启用息屏音量键动作",
            summary = "仅在屏幕关闭时接管音量键；亮屏后保留系统原有音量控制。",
            checked = settings.screenOffVolumeKeyEnabled,
            onCheckedChange = { value -> update { it.copy(screenOffVolumeKeyEnabled = value) } },
        )
        SwitchItem(
            title = "启用长按切歌",
            summary = "音量上键：下一曲；音量下键：上一曲。",
            checked = settings.screenOffVolumeLongPressEnabled,
            enabled = settings.screenOffVolumeKeyEnabled,
            onCheckedChange = { value -> update { it.copy(screenOffVolumeLongPressEnabled = value) } },
        )
        NumericShortcutSlider(
            title = "长按判定时间",
            summary = "按住多久后触发切歌。",
            value = settings.screenOffVolumeLongPressTimeoutMs,
            range = 350..2000,
            enabled = settings.screenOffVolumeKeyEnabled && settings.screenOffVolumeLongPressEnabled,
            format = { "${it} ms" },
            onCommit = { value -> update { it.copy(screenOffVolumeLongPressTimeoutMs = value) } },
        )
        SwitchItem(
            title = "启用双击播放 / 暂停",
            summary = "在屏幕关闭时快速双击同一个音量键，发送媒体播放/暂停。",
            checked = settings.screenOffVolumeDoubleClickEnabled,
            enabled = settings.screenOffVolumeKeyEnabled,
            onCheckedChange = { value -> update { it.copy(screenOffVolumeDoubleClickEnabled = value) } },
        )
        NumericShortcutSlider(
            title = "双击间隔",
            summary = "两次按键之间允许的最大时间。",
            value = settings.screenOffVolumeDoubleClickTimeoutMs,
            range = 100..1000,
            enabled = settings.screenOffVolumeKeyEnabled && settings.screenOffVolumeDoubleClickEnabled,
            format = { "${it} ms" },
            onCommit = { value -> update { it.copy(screenOffVolumeDoubleClickTimeoutMs = value) } },
        )
        SwitchItem(
            title = "启用振动反馈",
            checked = settings.screenOffVolumeHapticEnabled,
            enabled = settings.screenOffVolumeKeyEnabled,
            onCheckedChange = { value -> update { it.copy(screenOffVolumeHapticEnabled = value) } },
        )
        HapticStrengthSlider(
            value = settings.screenOffVolumeHapticStrength,
            enabled = settings.screenOffVolumeKeyEnabled && settings.screenOffVolumeHapticEnabled,
            onCommit = { value -> update { it.copy(screenOffVolumeHapticStrength = value) } },
        )
        ShortcutBodyText(
            "注意：LSPosed 需要为 Aloha OneUI 启用 system（系统框架）作用域。某些系统版本会在软重启后才重新载入按键 Hook；本选项不应改变亮屏时的原生音量行为。",
        )
    }
}

/** A compact themed group that keeps option order and state identical between Material and Miuix. */
@Composable
private fun SectionCard(
    title: String,
    summary: String,
    content: @Composable () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = summary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CompositionLocalProvider(LocalAlohaOneUIGroupedPreference provides true) {
                    content()
                }
            }
        }

        UiMode.Miuix -> MiuixCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                MiuixText(
                    text = title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                MiuixText(
                    text = summary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = colorScheme.onSurface,
                )
                CompositionLocalProvider(LocalAlohaOneUIGroupedPreference provides true) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ShortcutBodyText(text: String) {
    when (LocalUiMode.current) {
        UiMode.Material -> Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        UiMode.Miuix -> MiuixText(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = colorScheme.onSurface,
        )
    }
}

@Composable
private fun NumericShortcutSlider(
    title: String,
    summary: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    format: (Int) -> String,
    onCommit: (Int) -> Unit,
) {
    var preview by remember(value, range) { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value, range) { preview = value.toFloat() }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        when (LocalUiMode.current) {
            UiMode.Material -> {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(format(preview.toInt()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            UiMode.Miuix -> {
                MiuixText(title, color = colorScheme.onSurface)
                MiuixText(summary, color = colorScheme.onSurface)
                MiuixText(format(preview.toInt()), color = colorScheme.primary)
            }
        }
        AlohaOneUISlider(
            value = preview,
            onValueChange = { preview = it },
            modifier = Modifier.fillMaxWidth(),
            valueRange = range.first.toFloat()..range.last.toFloat(),
            stepSize = 1f,
            enabled = enabled,
            onValueChangeFinished = { onCommit(preview.toInt().coerceIn(range.first, range.last)) },
        )
    }
}

@Composable
private fun HapticStrengthSlider(
    value: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit,
) {
    NumericShortcutSlider(
        title = "振动强度",
        summary = "振动强度范围：1–100%。",
        value = value.coerceIn(1, 100),
        range = 1..100,
        enabled = enabled,
        format = { "${it}%" },
        onCommit = onCommit,
    )
}

@Composable
private fun FloatingBallColorControl(
    argb: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit,
) {
    var colorText by remember(argb) { mutableStateOf("#" + argb.toUInt().toString(16).uppercase().padStart(8, '0')) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text("悬浮球颜色", style = MaterialTheme.typography.titleSmall)
            UiMode.Miuix -> MiuixText("悬浮球颜色", color = colorScheme.onSurface)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(argb)),
            )
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = colorText,
                onValueChange = { colorText = it },
                enabled = enabled,
                singleLine = true,
                label = { Text("ARGB，例如 #AA5E7CFF") },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = enabled && colorText.trim().matches(Regex("#[0-9a-fA-F]{8}")),
                onClick = {
                    colorText.trim().removePrefix("#").toLongOrNull(16)?.let { onCommit(it.toInt()) }
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("应用")
            }
        }
    }
}

@Composable
private fun ActionSelector(
    title: String,
    explanation: String,
    selectedAction: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }
    val apps = remember(context) { loadShortcutLauncherApps(context) }
    val customAction = selectedAction.startsWith(ACTION_CUSTOM_APP_PREFIX)
    val selectedLabel = remember(context, selectedAction, apps) {
        when {
            customAction -> {
                val flattened = selectedAction.removePrefix(ACTION_CUSTOM_APP_PREFIX)
                val selected = ComponentName.unflattenFromString(flattened)
                apps.firstOrNull { it.component == selected }?.label ?: "自定义程序"
            }
            else -> shortcutActionOptions.firstOrNull { it.id == selectedAction }?.label ?: "无动作"
        }
    }
    val selectedIndex = if (customAction) {
        shortcutActionOptions.indexOfFirst { it.id == ACTION_CUSTOM_APP }
    } else {
        shortcutActionOptions.indexOfFirst { it.id == selectedAction }.coerceAtLeast(0)
    }

    SelectItem(
        title = title,
        summary = explanation + "
当前：" + selectedLabel,
        entries = shortcutActionOptions.map { it.label },
        selectedIndex = selectedIndex.coerceAtLeast(0),
        enabled = enabled,
        onSelectedIndexChange = { index ->
            val choice = shortcutActionOptions.getOrNull(index) ?: return@SelectItem
            if (choice.id == ACTION_CUSTOM_APP) showAppPicker = true else onSelected(choice.id)
        },
    )

    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("选择自定义应用") },
            text = {
                if (apps.isEmpty()) {
                    Text("没有找到可从桌面启动的应用。")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                        items(apps, key = { it.component.flattenToString() }) { app ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onSelected(ACTION_CUSTOM_APP_PREFIX + app.component.flattenToString())
                                    showAppPicker = false
                                },
                            ) {
                                Text(
                                    text = app.label,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false }) { Text("取消") }
            },
        )
    }
}

private fun loadShortcutLauncherApps(context: Context): List<ShortcutLauncherApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return runCatching {
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { resolve ->
                val info = resolve.activityInfo ?: return@mapNotNull null
                if (info.packageName == context.packageName) return@mapNotNull null
                val component = ComponentName(info.packageName, info.name)
                val label = runCatching { info.loadLabel(pm).toString() }
                    .getOrDefault(info.packageName)
                ShortcutLauncherApp(label, component)
            }
            .distinctBy { it.component.flattenToString() }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}

/*
 * ActionSelector passes the new choice as a string while updateData remains a pure
 * Preference transformation. This holder exists only to make action updates explicit
 * at each control site and has no persisted state of its own.
 */
