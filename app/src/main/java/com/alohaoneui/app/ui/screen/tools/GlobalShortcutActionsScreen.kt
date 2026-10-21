package com.alohaoneui.app.ui.screen.tools

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import kotlin.math.roundToInt
import kotlin.math.min
import kotlin.math.hypot
import kotlin.math.atan2
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.alohaoneui.app.ui.navigation3.Route
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
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private const val ACTION_NONE = "none"
private const val ACTION_CUSTOM_APP = "custom_app"
private const val ACTION_CUSTOM_APP_PREFIX = "custom_app:"

private data class ShortcutActionOption(val id: String, val label: String)
private data class ShortcutActionGroup(val title: String, val actions: List<ShortcutActionOption>)
private data class ShortcutLauncherApp(val label: String, val component: ComponentName)

/** Group names, order and persisted IDs match App20261018Aloha/RootGestureActions.kt. */
private val shortcutActionGroups = listOf(
    ShortcutActionGroup("默认", listOf(ShortcutActionOption(ACTION_NONE, "无动作"))),
    ShortcutActionGroup("系统动作", listOf(
        ShortcutActionOption("back", "返回"),
        ShortcutActionOption("home", "主页"),
        ShortcutActionOption("recents", "最近任务"),
        ShortcutActionOption("screenshot", "截图"),
        ShortcutActionOption("smart_screenshot", "智能截图"),
        ShortcutActionOption("lock_screen", "关屏"),
        ShortcutActionOption("power_menu", "电源菜单"),
        ShortcutActionOption("kill_foreground_app", "结束当前程序"),
        ShortcutActionOption("memory_clean", "内存清理"),
    )),
    ShortcutActionGroup("相机 / 录屏", listOf(
        ShortcutActionOption("take_photo", "拍照"),
        ShortcutActionOption("quick_video", "录像"),
        ShortcutActionOption("screen_record", "录制屏幕"),
    )),
    ShortcutActionGroup("媒体控制", listOf(
        ShortcutActionOption("previous_track", "上一曲"),
        ShortcutActionOption("next_track", "下一曲"),
        ShortcutActionOption("play_pause", "播放 / 暂停"),
        ShortcutActionOption("media_rewind", "快退"),
        ShortcutActionOption("media_fast_forward", "快进"),
    )),
    ShortcutActionGroup("快捷开关", listOf(
        ShortcutActionOption("flashlight", "手电筒"),
        ShortcutActionOption("airplane_mode", "飞行模式"),
        ShortcutActionOption("bluetooth", "蓝牙"),
        ShortcutActionOption("silent_mode", "静音模式"),
        ShortcutActionOption("vibrate_mode", "振动模式"),
        ShortcutActionOption("wifi", "WiFi"),
        ShortcutActionOption("mobile_data", "移动数据"),
    )),
    ShortcutActionGroup("快捷支付", listOf(
        ShortcutActionOption("wechat_scan", "微信扫一扫"),
        ShortcutActionOption("wechat_pay", "微信付款"),
        ShortcutActionOption("wechat_collect", "微信收款"),
        ShortcutActionOption("alipay_scan", "支付宝扫码"),
        ShortcutActionOption("alipay_pay", "支付宝付款"),
        ShortcutActionOption("alipay_collect", "支付宝收款"),
    )),
    ShortcutActionGroup("自定义", listOf(ShortcutActionOption(ACTION_CUSTOM_APP, "自定义程序"))),
)
private val shortcutActionOptions = shortcutActionGroups.flatMap { it.actions }
private typealias ShortcutSettingsUpdater =
    ((Preference.GlobalShortcutActions) -> Preference.GlobalShortcutActions) -> Unit

private enum class GlobalShortcutPageKind(
    val title: String,
    val subtitle: String,
    val enableLabel: String,
    val note: String,
) {
    ThreeFingerSwipe("全局三指手势", "", "启用全局三指手势",
        "注意：LSPosed 需要给模块勾选 android / 系统框架作用域。关掉后再次软重启即可完全停用。"),
    ThreeFingerPinch("全局三指捏合", "三根手指同时向内收拢时触发一个独立动作。", "启用全局三指捏合",
        "注意：LSPosed 需要给模块勾选 android / 系统框架作用域。关掉后再次软重启即可完全停用。"),
    ThreeFingerSpread("全局三指展开", "三根手指同时向外分开时触发一个独立动作。", "启用全局三指展开",
        "注意：LSPosed 需要给模块勾选 android / 系统框架作用域。关掉后再次软重启即可完全停用。"),
    FloatingBall("悬浮球", "", "启用悬浮球",
        "注意：LSPosed 需要给模块勾选 android / 系统框架作用域。关掉后再次软重启即可完全停用。"),
    StatusBar("状态栏快捷动作", "", "启用状态栏快捷动作",
        "注意：LSPosed 需要给模块勾选 android / 系统框架作用域。关掉后再次软重启即可完全停用。"),
    NavigationKeys("虚拟导航键动作", "", "启用虚拟导航键动作",
        "注意：仅三键导航下生效，LSPosed 需要勾选系统界面作用域。"),
    ScreenOffVolume("息屏音量键动作",
        "息屏时长按音量键切歌，双击音量键播放/暂停；亮屏不接管原来的音量键。",
        "启用息屏音量键动作",
        "注意：LSPosed 需要给模块勾选 android / 系统框架作用域。关掉后再次软重启即可完全停用。"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalShortcutActionsScreen() {
    val navigator = LocalNavigator.current
    val model = viewModel<AlohaOneUIToolsViewModel>()
    val preference by model.preference.collectAsStateWithLifecycle()
    val settings = preference.other.globalShortcutActions
    val enabledThreeFingerCount = listOf(
        settings.threeFingerGestureEnabled,
        settings.threeFingerPinchEnabled,
        settings.threeFingerSpreadEnabled,
    ).count { it }

    GlobalShortcutPageFrame(
        title = "全局快捷动作",
        subtitle = "集中管理三指、悬浮球、状态栏与按键快捷触发。",
        onBack = navigator::pop,
    ) {
        GlobalShortcutEntryCard(
            title = "全局三指动作",
            description = "统一设置三指滑动、捏合和展开动作。",
            state = if (enabledThreeFingerCount == 0) "未启用" else "已启用 ${enabledThreeFingerCount} 项",
            iconColor = Color(0xFF5C6BC0),
            onClick = { navigator.push(Route.GlobalThreeFingerActions) },
        )
        GlobalShortcutEntryCard(
            title = "悬浮球",
            description = "设置悬浮球颜色、大小和单击/双击/划动动作。",
            state = if (settings.floatingBallEnabled) "已启用" else "未启用",
            iconColor = Color(0xFF00897B),
            onClick = { navigator.push(Route.GlobalFloatingBallShortcut) },
        )
        GlobalShortcutEntryCard(
            title = "状态栏快捷动作",
            description = "设置状态栏双击、左上角/右上角单击和长按动作。",
            state = if (settings.statusBarShortcutEnabled) "已启用" else "未启用",
            iconColor = Color(0xFF607D8B),
            onClick = { navigator.push(Route.GlobalStatusBarShortcut) },
        )
        GlobalShortcutEntryCard(
            title = "虚拟导航键动作",
            description = "设置返回、Home、最近任务的双击和长按动作。",
            state = if (settings.navigationKeyShortcutEnabled) "已启用" else "未启用",
            iconColor = Color(0xFF8E24AA),
            onClick = { navigator.push(Route.GlobalNavigationKeyShortcut) },
        )
        GlobalShortcutEntryCard(
            title = "息屏音量键动作",
            description = "息屏长按音量键切歌，双击音量键播放/暂停。",
            state = if (settings.screenOffVolumeKeyEnabled) "已启用" else "未启用",
            iconColor = Color(0xFFEF6C00),
            onClick = { navigator.push(Route.GlobalScreenOffVolumeKeyShortcut) },
        )
    }
}

/** Separate second-level page, matching the original three-finger action submenu. */
@Composable
fun GlobalThreeFingerActionsScreen() {
    val navigator = LocalNavigator.current
    val model = viewModel<AlohaOneUIToolsViewModel>()
    val preference by model.preference.collectAsStateWithLifecycle()
    val settings = preference.other.globalShortcutActions

    GlobalShortcutPageFrame(
        title = "全局三指动作",
        subtitle = "统一管理三指滑动、捏合和展开触发方式。",
        onBack = navigator::pop,
    ) {
        GlobalShortcutEntryCard(
            title = "三指滑动",
            description = "设置三指上划、下划、左划、右划动作。",
            state = if (settings.threeFingerGestureEnabled) "已启用" else "未启用",
            iconColor = Color(0xFF5C6BC0),
            onClick = { navigator.push(Route.GlobalThreeFingerSwipe) },
        )
        GlobalShortcutEntryCard(
            title = "三指捏合",
            description = "设置三根手指同时向内捏合时执行的动作。",
            state = if (settings.threeFingerPinchEnabled) "已启用" else "未启用",
            iconColor = Color(0xFF00897B),
            onClick = { navigator.push(Route.GlobalThreeFingerPinch) },
        )
        GlobalShortcutEntryCard(
            title = "三指展开",
            description = "设置三根手指同时向外展开时执行的动作。",
            state = if (settings.threeFingerSpreadEnabled) "已启用" else "未启用",
            iconColor = Color(0xFF7E57C2),
            onClick = { navigator.push(Route.GlobalThreeFingerSpread) },
        )
    }
}

@Composable
fun GlobalThreeFingerSwipeScreen() = GlobalShortcutSettingsScreen(GlobalShortcutPageKind.ThreeFingerSwipe)
@Composable
fun GlobalThreeFingerPinchScreen() = GlobalShortcutSettingsScreen(GlobalShortcutPageKind.ThreeFingerPinch)
@Composable
fun GlobalThreeFingerSpreadScreen() = GlobalShortcutSettingsScreen(GlobalShortcutPageKind.ThreeFingerSpread)
@Composable
fun GlobalFloatingBallShortcutScreen() = GlobalShortcutSettingsScreen(GlobalShortcutPageKind.FloatingBall)
@Composable
fun GlobalStatusBarShortcutScreen() = GlobalShortcutSettingsScreen(GlobalShortcutPageKind.StatusBar)
@Composable
fun GlobalNavigationKeyShortcutScreen() = GlobalShortcutSettingsScreen(GlobalShortcutPageKind.NavigationKeys)
@Composable
fun GlobalScreenOffVolumeKeyShortcutScreen() = GlobalShortcutSettingsScreen(GlobalShortcutPageKind.ScreenOffVolume)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalShortcutSettingsScreen(kind: GlobalShortcutPageKind) {
    val navigator = LocalNavigator.current
    val model = viewModel<AlohaOneUIToolsViewModel>()
    val preference by model.preference.collectAsStateWithLifecycle()
    val settings = preference.other.globalShortcutActions
    val update: ShortcutSettingsUpdater = { change ->
        model.updateData { current ->
            current.copy(other = current.other.copy(globalShortcutActions = change(current.other.globalShortcutActions)))
        }
    }
    GlobalShortcutPageFrame(title = kind.title, subtitle = kind.subtitle, onBack = navigator::pop) {
        GlobalShortcutSettingsContent(kind = kind, settings = settings, update = update)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalShortcutPageFrame(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    when (LocalUiMode.current) {
        UiMode.Material -> {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            Scaffold(
                topBar = {
                    BlurredBar(backdrop) {
                        LargeFlexibleTopAppBar(
                            title = { Text(title) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                            scrollBehavior = scrollBehavior,
                        )
                    }
                },
            ) { innerPadding ->
                Column(
                    modifier = (if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                ) {
                    if (subtitle.isNotBlank()) ShortcutBodyText(subtitle)
                    content()
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
                            title = title,
                            navigationIcon = {
                                MiuixIconButton(onClick = onBack) {
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
                    if (subtitle.isNotBlank()) ShortcutBodyText(subtitle)
                    content()
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun GlobalShortcutEntryCard(
    title: String,
    description: String,
    state: String,
    iconColor: Color,
    onClick: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clickable(onClick = onClick),
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(iconColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_nav_gesture),
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        state,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.startsWith("已启用")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        UiMode.Miuix -> MiuixCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(iconColor),
                    contentAlignment = Alignment.Center,
                ) {
                    MiuixIcon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_nav_gesture),
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    MiuixText(title, color = colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    MiuixText(state, color = if (state.startsWith("已启用")) colorScheme.primary else colorScheme.onSurfaceVariantSummary)
                    MiuixText(description, color = colorScheme.onSurface)
                }
                MiuixIcon(
                    modifier = Modifier.padding(start = 8.dp).size(width = 10.dp, height = 16.dp),
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariantActions,
                )
            }
        }
    }
}

@Composable
private fun GlobalShortcutSettingsContent(
    kind: GlobalShortcutPageKind,
    settings: Preference.GlobalShortcutActions,
    update: ShortcutSettingsUpdater,
) {
    when (kind) {
        GlobalShortcutPageKind.ThreeFingerSwipe -> {
            ShortcutFeatureEnableCard(kind, settings.threeFingerGestureEnabled) { value ->
                update { it.copy(threeFingerGestureEnabled = value) }
            }
            ThresholdShortcutCard(
                title = "禁止交互阈值",
                summary = "阈值决定三指滑动移动多远后接管触摸。数值越小越灵敏；数值越大越不容易误触。",
                value = settings.threeFingerTouchCancelThresholdDp,
                range = 1..20,
                enabled = settings.threeFingerGestureEnabled,
                format = { "三指滑动：${it}dp 后接管" },
                helpTitle = "禁止交互阈值",
                helpMessage = "“阈值”指的是三根手指滑动的距离。\n· 数值越小：三指刚开始动一点点，就会立刻接管触摸，页面更不容易滚动，但更灵敏\n· 数值越大：要三指滑动更远一点才接管，误触更少，但页面可能会先跟着滚一点",
                onCommit = { value -> update { it.copy(threeFingerTouchCancelThresholdDp = value) } },
            )
            ShortcutSectionHeading("三指滑动动作")
            ShortcutHapticCard(
                enabled = settings.threeFingerGestureEnabled,
                hapticEnabled = settings.threeFingerHapticEnabled,
                strength = settings.threeFingerHapticStrength,
                onHapticEnabled = { value -> update { it.copy(threeFingerHapticEnabled = value) } },
                onStrength = { value -> update { it.copy(threeFingerHapticStrength = value) } },
            )
            ShortcutActionRow("上划", "三根手指同时向上滑动。", settings.threeFingerSwipeUpAction, settings.threeFingerGestureEnabled) {
                value -> update { it.copy(threeFingerSwipeUpAction = value) }
            }
            ShortcutActionRow("下划", "三根手指同时向下滑动。", settings.threeFingerSwipeDownAction, settings.threeFingerGestureEnabled) {
                value -> update { it.copy(threeFingerSwipeDownAction = value) }
            }
            ShortcutActionRow("左划", "三根手指同时向左滑动。", settings.threeFingerSwipeLeftAction, settings.threeFingerGestureEnabled) {
                value -> update { it.copy(threeFingerSwipeLeftAction = value) }
            }
            ShortcutActionRow("右划", "三根手指同时向右滑动。", settings.threeFingerSwipeRightAction, settings.threeFingerGestureEnabled) {
                value -> update { it.copy(threeFingerSwipeRightAction = value) }
            }
        }
        GlobalShortcutPageKind.ThreeFingerPinch -> {
            ShortcutFeatureEnableCard(kind, settings.threeFingerPinchEnabled) { value ->
                update { it.copy(threeFingerPinchEnabled = value) }
            }
            ThresholdShortcutCard(
                title = "捏合阈值",
                summary = "以三指平均跨度的缩小距离判断是否完成捏合。",
                value = settings.threeFingerPinchThresholdDp,
                range = 8..160,
                enabled = settings.threeFingerPinchEnabled,
                format = { "跨度变化：${it}dp" },
                helpTitle = "捏合阈值",
                helpMessage = "“捏合阈值”指三根手指之间的平均距离向内缩小了多少。\n· 数值越小：轻微收拢就会触发，更灵敏，但更容易误触\n· 数值越大：需要更明显的捏合动作，误触更少，但需要做得更完整",
                onCommit = { value -> update { it.copy(threeFingerPinchThresholdDp = value) } },
            )
            ShortcutSectionHeading("触发动作")
            ShortcutHapticCard(
                enabled = settings.threeFingerPinchEnabled,
                hapticEnabled = settings.threeFingerPinchHapticEnabled,
                strength = settings.threeFingerPinchHapticStrength,
                onHapticEnabled = { value -> update { it.copy(threeFingerPinchHapticEnabled = value) } },
                onStrength = { value -> update { it.copy(threeFingerPinchHapticStrength = value) } },
            )
            ShortcutActionRow("三指捏合", "三根手指同时向内捏合，收缩距离达到阈值后执行。", settings.threeFingerPinchAction, settings.threeFingerPinchEnabled) {
                value -> update { it.copy(threeFingerPinchAction = value) }
            }
        }
        GlobalShortcutPageKind.ThreeFingerSpread -> {
            ShortcutFeatureEnableCard(kind, settings.threeFingerSpreadEnabled) { value ->
                update { it.copy(threeFingerSpreadEnabled = value) }
            }
            ThresholdShortcutCard(
                title = "展开阈值",
                summary = "以三指平均跨度的增大距离判断是否完成展开。",
                value = settings.threeFingerSpreadThresholdDp,
                range = 8..160,
                enabled = settings.threeFingerSpreadEnabled,
                format = { "跨度变化：${it}dp" },
                helpTitle = "展开阈值",
                helpMessage = "“展开阈值”指的是三根手指之间的平均距离向外扩大了多少。\n· 数值越小：稍微分开一点就会触发，更灵敏，但更容易误触\n· 数值越大：需要明显展开才触发，误触更少，但动作要做得更完整",
                onCommit = { value -> update { it.copy(threeFingerSpreadThresholdDp = value) } },
            )
            ShortcutSectionHeading("触发动作")
            ShortcutHapticCard(
                enabled = settings.threeFingerSpreadEnabled,
                hapticEnabled = settings.threeFingerSpreadHapticEnabled,
                strength = settings.threeFingerSpreadHapticStrength,
                onHapticEnabled = { value -> update { it.copy(threeFingerSpreadHapticEnabled = value) } },
                onStrength = { value -> update { it.copy(threeFingerSpreadHapticStrength = value) } },
            )
            ShortcutActionRow("三指展开", "三根手指同时向外展开，扩大距离达到阈值后执行。", settings.threeFingerSpreadAction, settings.threeFingerSpreadEnabled) {
                value -> update { it.copy(threeFingerSpreadAction = value) }
            }
        }
        GlobalShortcutPageKind.FloatingBall -> {
            ShortcutFeatureEnableCard(kind, settings.floatingBallEnabled) { value ->
                update { it.copy(floatingBallEnabled = value) }
            }
            FloatingBallColorControl(
                argb = settings.floatingBallColorArgb,
                enabled = settings.floatingBallEnabled,
                onCommit = { value -> update { it.copy(floatingBallColorArgb = value) } },
            )
            SectionCard("悬浮球大小", "") {
                NumericShortcutSlider(
                    title = "",
                    summary = "",
                    value = settings.floatingBallSizeDp,
                    range = 40..88,
                    enabled = settings.floatingBallEnabled,
                    format = { "${it} dp" },
                    onCommit = { value -> update { it.copy(floatingBallSizeDp = value) } },
                    showTitleAndSummary = false,
                )
            }
            SectionCard("自动贴边", "") {
                NumericShortcutSlider(
                    title = "",
                    summary = "",
                    value = settings.floatingBallAutoEdgeDelaySeconds,
                    range = 0..10,
                    enabled = settings.floatingBallEnabled,
                    format = { if (it <= 0) "已关闭" else "${it} 秒无操作后贴边" },
                    onCommit = { value -> update { it.copy(floatingBallAutoEdgeDelaySeconds = value) } },
                    showTitleAndSummary = false,
                )
            }
            ShortcutSectionHeading("触发动作")
            ShortcutHapticCard(
                enabled = settings.floatingBallEnabled,
                hapticEnabled = settings.floatingBallHapticEnabled,
                strength = settings.floatingBallHapticStrength,
                onHapticEnabled = { value -> update { it.copy(floatingBallHapticEnabled = value) } },
                onStrength = { value -> update { it.copy(floatingBallHapticStrength = value) } },
            )
            ShortcutActionRow("单击", "点击一次悬浮球。", settings.floatingBallClickAction, settings.floatingBallEnabled) {
                value -> update { it.copy(floatingBallClickAction = value) }
            }
            ShortcutActionRow("双击", "快速点击两次悬浮球。", settings.floatingBallDoubleClickAction, settings.floatingBallEnabled) {
                value -> update { it.copy(floatingBallDoubleClickAction = value) }
            }
            ShortcutActionRow("上划", "按住悬浮球向上滑。", settings.floatingBallSwipeUpAction, settings.floatingBallEnabled) {
                value -> update { it.copy(floatingBallSwipeUpAction = value) }
            }
            ShortcutActionRow("下划", "按住悬浮球向下滑。", settings.floatingBallSwipeDownAction, settings.floatingBallEnabled) {
                value -> update { it.copy(floatingBallSwipeDownAction = value) }
            }
            ShortcutActionRow("左划", "按住悬浮球向左滑。", settings.floatingBallSwipeLeftAction, settings.floatingBallEnabled) {
                value -> update { it.copy(floatingBallSwipeLeftAction = value) }
            }
            ShortcutActionRow("右划", "按住悬浮球向右滑。", settings.floatingBallSwipeRightAction, settings.floatingBallEnabled) {
                value -> update { it.copy(floatingBallSwipeRightAction = value) }
            }
        }
        GlobalShortcutPageKind.StatusBar -> {
            ShortcutFeatureEnableCard(kind, settings.statusBarShortcutEnabled) { value ->
                update { it.copy(statusBarShortcutEnabled = value) }
            }
            ShortcutSectionHeading("触发动作")
            ShortcutHapticCard(
                enabled = settings.statusBarShortcutEnabled,
                hapticEnabled = settings.statusBarShortcutHapticEnabled,
                strength = settings.statusBarShortcutHapticStrength,
                onHapticEnabled = { value -> update { it.copy(statusBarShortcutHapticEnabled = value) } },
                onStrength = { value -> update { it.copy(statusBarShortcutHapticStrength = value) } },
            )
            ShortcutActionRow("状态栏双击", "快速点击状态栏任意位置两次。", settings.statusBarDoubleClickAction, settings.statusBarShortcutEnabled) {
                value -> update { it.copy(statusBarDoubleClickAction = value) }
            }
            ShortcutActionRow("状态栏左上角单击", "点击状态栏左上角区域。", settings.statusBarLeftClickAction, settings.statusBarShortcutEnabled) {
                value -> update { it.copy(statusBarLeftClickAction = value) }
            }
            ShortcutActionRow("状态栏左上角长按", "长按状态栏左上角区域。", settings.statusBarLeftLongPressAction, settings.statusBarShortcutEnabled) {
                value -> update { it.copy(statusBarLeftLongPressAction = value) }
            }
            ShortcutActionRow("状态栏右上角单击", "点击状态栏右上角区域。", settings.statusBarRightClickAction, settings.statusBarShortcutEnabled) {
                value -> update { it.copy(statusBarRightClickAction = value) }
            }
            ShortcutActionRow("状态栏右上角长按", "长按状态栏右上角区域。", settings.statusBarRightLongPressAction, settings.statusBarShortcutEnabled) {
                value -> update { it.copy(statusBarRightLongPressAction = value) }
            }
        }
        GlobalShortcutPageKind.NavigationKeys -> {
            ShortcutFeatureEnableCard(kind, settings.navigationKeyShortcutEnabled) { value ->
                update { it.copy(navigationKeyShortcutEnabled = value) }
            }
            ShortcutSectionHeading("触发动作")
            ShortcutHapticCard(
                enabled = settings.navigationKeyShortcutEnabled,
                hapticEnabled = settings.navigationKeyHapticEnabled,
                strength = settings.navigationKeyHapticStrength,
                onHapticEnabled = { value -> update { it.copy(navigationKeyHapticEnabled = value) } },
                onStrength = { value -> update { it.copy(navigationKeyHapticStrength = value) } },
            )
            ShortcutActionRow("双击返回", "快速点击返回键两次。", settings.navigationBackDoubleTapAction, settings.navigationKeyShortcutEnabled) {
                value -> update { it.copy(navigationBackDoubleTapAction = value) }
            }
            ShortcutActionRow("长按返回", "按住返回键不放。", settings.navigationBackLongPressAction, settings.navigationKeyShortcutEnabled) {
                value -> update { it.copy(navigationBackLongPressAction = value) }
            }
            ShortcutActionRow("双击 Home", "快速点击 Home 键两次。", settings.navigationHomeDoubleTapAction, settings.navigationKeyShortcutEnabled) {
                value -> update { it.copy(navigationHomeDoubleTapAction = value) }
            }
            ShortcutActionRow("长按 Home", "按住 Home 键不放。", settings.navigationHomeLongPressAction, settings.navigationKeyShortcutEnabled) {
                value -> update { it.copy(navigationHomeLongPressAction = value) }
            }
            ShortcutActionRow("双击最近任务", "快速点击最近任务键两次。", settings.navigationRecentsDoubleTapAction, settings.navigationKeyShortcutEnabled) {
                value -> update { it.copy(navigationRecentsDoubleTapAction = value) }
            }
            ShortcutActionRow("长按最近任务", "按住最近任务键不放。", settings.navigationRecentsLongPressAction, settings.navigationKeyShortcutEnabled) {
                value -> update { it.copy(navigationRecentsLongPressAction = value) }
            }
        }
        GlobalShortcutPageKind.ScreenOffVolume -> {
            ShortcutFeatureEnableCard(kind, settings.screenOffVolumeKeyEnabled) { value ->
                update { it.copy(screenOffVolumeKeyEnabled = value) }
            }
            SectionCard("长按切歌", "音量上：下一曲；音量下：上一曲。") {
                SwitchItem(
                    title = "启用长按切歌",
                    summary = "仅屏幕关闭时长按音量键触发；亮屏保持系统原有音量控制。",
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
            }
            SectionCard("双击播放/暂停", "息屏时快速双击同一个音量键，发送媒体播放/暂停。") {
                SwitchItem(
                    title = "启用双击播放/暂停",
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
            }
            ShortcutHapticCard(
                enabled = settings.screenOffVolumeKeyEnabled,
                hapticEnabled = settings.screenOffVolumeHapticEnabled,
                strength = settings.screenOffVolumeHapticStrength,
                onHapticEnabled = { value -> update { it.copy(screenOffVolumeHapticEnabled = value) } },
                onStrength = { value -> update { it.copy(screenOffVolumeHapticStrength = value) } },
                summary = "切歌或播放/暂停触发成功时振动一次。",
            )
        }
    }
}

@Composable
private fun ShortcutFeatureEnableCard(
    kind: GlobalShortcutPageKind,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    // Mirrors the source enable card: one switch row, its status, then the scope note.
    when (LocalUiMode.current) {
        UiMode.Material -> Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                CompositionLocalProvider(LocalAlohaOneUIGroupedPreference provides true) {
                    SwitchItem(
                        title = kind.enableLabel,
                        summary = if (enabled) "已启用" else "未启用",
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                    )
                }
                ShortcutBodyText(kind.note)
            }
        }
        UiMode.Miuix -> MiuixCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                CompositionLocalProvider(LocalAlohaOneUIGroupedPreference provides true) {
                    SwitchItem(
                        title = kind.enableLabel,
                        summary = if (enabled) "已启用" else "未启用",
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                    )
                }
                ShortcutBodyText(kind.note)
            }
        }
    }
}

@Composable
private fun ShortcutSectionHeading(text: String) {
    when (LocalUiMode.current) {
        UiMode.Material -> Text(
            text = text,
            modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        UiMode.Miuix -> MiuixText(
            text = text,
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
            color = colorScheme.onSurfaceVariantSummary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ShortcutHapticCard(
    enabled: Boolean,
    hapticEnabled: Boolean,
    strength: Int,
    onHapticEnabled: (Boolean) -> Unit,
    onStrength: (Int) -> Unit,
    summary: String = "动作触发时提供触觉反馈。",
) {
    SectionCard("振动反馈", summary) {
        SwitchItem(
            title = "启用振动反馈",
            summary = null,
            checked = hapticEnabled,
            enabled = enabled,
            onCheckedChange = onHapticEnabled,
        )
        HapticStrengthSlider(
            value = strength,
            enabled = enabled && hapticEnabled,
            onCommit = onStrength,
        )
    }
}

@Composable
private fun ThresholdShortcutCard(
    title: String,
    summary: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    format: (Int) -> String,
    helpTitle: String,
    helpMessage: String,
    onCommit: (Int) -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }
    // Source layout: title with an adjacent “?” affordance, explanatory line, value and slider.
    // Both Aloha themes keep their own row, colors and typography instead of a generic button at the bottom.
    when (LocalUiMode.current) {
        UiMode.Material -> Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { showHelp = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = summary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumericShortcutSlider(
                    title = "",
                    summary = "",
                    value = value,
                    range = range,
                    enabled = enabled,
                    format = format,
                    onCommit = onCommit,
                    showTitleAndSummary = false,
                )
            }
        }
        UiMode.Miuix -> MiuixCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText(
                        text = title,
                        modifier = Modifier.weight(1f),
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                            .background(colorScheme.surface)
                            .clickable { showHelp = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        MiuixText("?", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                MiuixText(
                    text = summary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = colorScheme.onSurfaceVariantSummary,
                )
                NumericShortcutSlider(
                    title = "",
                    summary = "",
                    value = value,
                    range = range,
                    enabled = enabled,
                    format = format,
                    onCommit = onCommit,
                    showTitleAndSummary = false,
                )
            }
        }
    }
    if (showHelp) ShortcutInformationDialog(helpTitle, helpMessage) { showHelp = false }
}
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
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                if (summary.isNotBlank()) {
                    MiuixText(
                        text = summary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
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
    showTitleAndSummary: Boolean = true,
) {
    var preview by remember(value, range) { mutableFloatStateOf(value.toFloat()) }
    LaunchedEffect(value, range) { preview = value.toFloat() }
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = if (showTitleAndSummary) 16.dp else 0.dp,
            vertical = 8.dp,
        ),
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> {
                if (showTitleAndSummary) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(format(preview.toInt()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            UiMode.Miuix -> {
                if (showTitleAndSummary) {
                    MiuixText(title, color = colorScheme.onSurface)
                    MiuixText(summary, color = colorScheme.onSurfaceVariantSummary)
                }
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
private fun FloatingBallColorControl(argb: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    SectionCard("悬浮球颜色", "点击颜色预览，使用颜色轮调整色相、亮度和透明度。") {
        val labelColor = run {
            val lum = 0.299 * ((argb shr 16) and 0xFF) +
                0.587 * ((argb shr 8) and 0xFF) + 0.114 * (argb and 0xFF)
            if (lum > 150) Color.Black else Color.White
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
                .clip(RoundedCornerShape(16.dp))
                .background(Color(argb))
                .clickable(enabled = enabled) { showPicker = true }
                .padding(14.dp),
        ) {
            when (LocalUiMode.current) {
                UiMode.Material -> Text(String.format("#%08X", argb), color = labelColor, fontWeight = FontWeight.SemiBold)
                UiMode.Miuix -> MiuixText(String.format("#%08X", argb), color = labelColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (showPicker) {
        ShortcutColorPickerDialog(
            initialColor = argb,
            onDismiss = { showPicker = false },
            onConfirm = {
                onCommit(it)
                showPicker = false
            },
        )
    }
}

@Composable
private fun ShortcutColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember(initialColor) { mutableFloatStateOf(((initialColor ushr 24) and 0xFF).toFloat()) }
    val selectedColor = Color.hsv(hue, saturation, brightness, alpha.roundToInt().coerceIn(0, 255) / 255f)
    val selectedArgb = android.graphics.Color.HSVToColor(
        alpha.roundToInt().coerceIn(0, 255),
        floatArrayOf(hue, saturation, brightness),
    )

    ShortcutDialogSurface(
        title = "悬浮球颜色",
        subtitle = "拖动颜色轮选择色相与饱和度，再调整亮度和透明度。",
        onDismiss = onDismiss,
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text(
                String.format("#%08X", selectedArgb),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(selectedColor).padding(vertical = 14.dp, horizontal = 12.dp),
                color = if (0.299 * selectedColor.red + 0.587 * selectedColor.green + 0.114 * selectedColor.blue > 0.55) Color.Black else Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            UiMode.Miuix -> MiuixText(
                String.format("#%08X", selectedArgb),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(selectedColor).padding(vertical = 14.dp, horizontal = 12.dp),
                color = if (0.299 * selectedColor.red + 0.587 * selectedColor.green + 0.114 * selectedColor.blue > 0.55) Color.Black else Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.CenterHorizontally)
                .pointerInput(Unit) {
                    detectTapGestures { point ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = min(size.width, size.height) / 2f - 10.dp.toPx()
                        val dx = point.x - centerX
                        val dy = point.y - centerY
                        saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                        hue = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = min(size.width, size.height) / 2f - 10.dp.toPx()
                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY
                        saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                        hue = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                    }
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f - 10.dp.toPx()
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.hsv(0f, 1f, 1f), Color.hsv(60f, 1f, 1f),
                        Color.hsv(120f, 1f, 1f), Color.hsv(180f, 1f, 1f),
                        Color.hsv(240f, 1f, 1f), Color.hsv(300f, 1f, 1f),
                        Color.hsv(360f, 1f, 1f),
                    ),
                    center,
                ),
                radius = radius,
                center = center,
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), center, radius),
                radius = radius,
                center = center,
            )
            if (brightness < 1f) drawCircle(Color.Black.copy(alpha = 1f - brightness), radius = radius, center = center)
            val angle = hue * Math.PI.toFloat() / 180f
            val markerRadius = radius * saturation
            val marker = Offset(center.x + kotlin.math.cos(angle) * markerRadius, center.y + kotlin.math.sin(angle) * markerRadius)
            drawCircle(Color.White, radius = 9.dp.toPx(), center = marker, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.65f), radius = 9.dp.toPx(), center = marker, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
        }
        ColorWheelSlider("亮度", (brightness * 255f).roundToInt()) { brightness = it / 255f }
        ColorWheelSlider("透明度", alpha.roundToInt()) { alpha = it.toFloat() }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            GlobalShortcutActionButton("取消", false, onDismiss, Modifier.weight(1f).padding(end = 6.dp))
            GlobalShortcutActionButton(
                "确定",
                true,
                { onConfirm(selectedArgb) },
                Modifier.weight(1f).padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun ColorWheelSlider(title: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            when (LocalUiMode.current) {
                UiMode.Material -> Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                UiMode.Miuix -> MiuixText(title, modifier = Modifier.weight(1f), color = colorScheme.onSurface)
            }
            when (LocalUiMode.current) {
                UiMode.Material -> Text(value.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                UiMode.Miuix -> MiuixText(value.toString(), color = colorScheme.onSurfaceVariantSummary)
            }
        }
        AlohaOneUISlider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
            modifier = Modifier.fillMaxWidth(),
            valueRange = 0f..255f,
            stepSize = 1f,
            enabled = true,
        )
    }
}

@Composable
private fun ShortcutActionRow(
    title: String,
    description: String,
    selectedAction: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val apps = remember(context) { loadShortcutLauncherApps(context) }
    var showActionPicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    val isCustomApp = selectedAction.startsWith(ACTION_CUSTOM_APP_PREFIX)
    val selectedLabel = remember(context, selectedAction, apps) {
        if (isCustomApp) {
            val component = ComponentName.unflattenFromString(selectedAction.removePrefix(ACTION_CUSTOM_APP_PREFIX))
            val appName = apps.firstOrNull { it.component == component }?.label ?: "自定义程序"
            "自定义程序 · $appName"
        } else {
            shortcutActionOptions.firstOrNull { it.id == selectedAction }?.label ?: "无动作"
        }
    }
    when (LocalUiMode.current) {
        UiMode.Material -> Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
                .clickable(enabled = enabled) { showActionPicker = true },
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    selectedLabel,
                    modifier = Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled && selectedAction != ACTION_NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(description, modifier = Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        UiMode.Miuix -> MiuixCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
                .clickable(enabled = enabled) { showActionPicker = true },
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                MiuixText(title, color = colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                MiuixText(
                    selectedLabel,
                    modifier = Modifier.padding(top = 5.dp),
                    color = if (enabled && selectedAction != ACTION_NONE) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
                )
                MiuixText(description, modifier = Modifier.padding(top = 5.dp), color = colorScheme.onSurface)
            }
        }
    }
    if (showActionPicker) {
        GroupedShortcutActionPicker(
            title = title,
            message = "选择这个触发器执行的动作。",
            selectedAction = selectedAction,
            onDismiss = { showActionPicker = false },
            onSelect = { action ->
                showActionPicker = false
                if (action == ACTION_CUSTOM_APP) showAppPicker = true else onSelected(action)
            },
        )
    }
    if (showAppPicker) {
        CustomShortcutAppPicker(
            apps = apps,
            onDismiss = { showAppPicker = false },
            onSelect = { component ->
                onSelected(ACTION_CUSTOM_APP_PREFIX + component.flattenToString())
                showAppPicker = false
            },
        )
    }
}

@Composable
private fun GroupedShortcutActionPicker(
    title: String,
    message: String,
    selectedAction: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val selectedGroup = shortcutActionGroups.firstOrNull { group ->
        group.actions.any {
            it.id == selectedAction || (it.id == ACTION_CUSTOM_APP && selectedAction.startsWith(ACTION_CUSTOM_APP_PREFIX))
        }
    }?.title ?: "默认"
    var expandedGroups by remember(selectedAction) { mutableStateOf(setOf("默认", selectedGroup)) }
    ShortcutDialogSurface(title, message, onDismiss) {
        Column(modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
            shortcutActionGroups.forEach { group ->
                ShortcutActionGroupCard(
                    group = group,
                    selectedAction = selectedAction,
                    expanded = group.title in expandedGroups,
                    onToggle = {
                        expandedGroups = if (group.title in expandedGroups) expandedGroups - group.title
                        else expandedGroups + group.title
                    },
                    onSelect = onSelect,
                )
            }
        }
        GlobalShortcutActionButton("取消", false, onDismiss, Modifier.fillMaxWidth().padding(top = 10.dp))
    }
}

@Composable
private fun ShortcutActionGroupCard(
    group: ShortcutActionGroup,
    selectedAction: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val accentColor = when (LocalUiMode.current) {
        UiMode.Material -> MaterialTheme.colorScheme.primary
        UiMode.Miuix -> colorScheme.primary
    }
    val unselectedBackground = when (LocalUiMode.current) {
        UiMode.Material -> MaterialTheme.colorScheme.surfaceContainerHighest
        UiMode.Miuix -> colorScheme.surface
    }
    val secondary = when (LocalUiMode.current) {
        UiMode.Material -> MaterialTheme.colorScheme.onSurfaceVariant
        UiMode.Miuix -> colorScheme.onSurfaceVariantSummary
    }
    when (LocalUiMode.current) {
        UiMode.Material -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(18.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(group.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${group.actions.size} 项", color = secondary, style = MaterialTheme.typography.bodySmall)
                    Text(if (expanded) "⌃" else "⌄", modifier = Modifier.padding(start = 8.dp), color = secondary)
                }
                if (expanded) ShortcutActionGrid(group, selectedAction, accentColor, unselectedBackground, onSelect)
            }
        }
        UiMode.Miuix -> MiuixCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText(group.title, modifier = Modifier.weight(1f), color = colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    MiuixText("${group.actions.size} 项", color = secondary)
                    MiuixText(if (expanded) "⌃" else "⌄", modifier = Modifier.padding(start = 8.dp), color = secondary)
                }
                if (expanded) ShortcutActionGrid(group, selectedAction, accentColor, unselectedBackground, onSelect)
            }
        }
    }
}

@Composable
private fun ShortcutActionGrid(
    group: ShortcutActionGroup,
    selectedAction: String,
    selectedBackground: Color,
    unselectedBackground: Color,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        group.actions.chunked(2).forEach { rowActions ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowActions.forEach { option ->
                    val selected = option.id == selectedAction ||
                        (option.id == ACTION_CUSTOM_APP && selectedAction.startsWith(ACTION_CUSTOM_APP_PREFIX))
                    val foreground = if (selected) Color.White else when (LocalUiMode.current) {
                        UiMode.Material -> MaterialTheme.colorScheme.onSurface
                        UiMode.Miuix -> colorScheme.onSurface
                    }
                    Box(
                        modifier = Modifier.weight(1f).padding(4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) selectedBackground else unselectedBackground)
                            .clickable { onSelect(option.id) }
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        when (LocalUiMode.current) {
                            UiMode.Material -> Text(option.label, color = foreground, style = MaterialTheme.typography.bodySmall)
                            UiMode.Miuix -> MiuixText(option.label, color = foreground)
                        }
                    }
                }
                if (rowActions.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CustomShortcutAppPicker(
    apps: List<ShortcutLauncherApp>,
    onDismiss: () -> Unit,
    onSelect: (ComponentName) -> Unit,
) {
    ShortcutDialogSurface("选择自定义应用", "选择这个触发器要打开的应用。", onDismiss) {
        if (apps.isEmpty()) {
            ShortcutBodyText("没有找到可从桌面启动的应用。")
        } else {
            Column(modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                apps.forEach { app ->
                    when (LocalUiMode.current) {
                        UiMode.Material -> Text(
                            app.label,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(app.component) }.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        UiMode.Miuix -> MiuixText(
                            app.label,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(app.component) }.padding(12.dp),
                            color = colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        GlobalShortcutActionButton("取消", false, onDismiss, Modifier.fillMaxWidth().padding(top = 10.dp))
    }
}

@Composable
private fun ShortcutDialogSurface(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        when (LocalUiMode.current) {
            UiMode.Material -> Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).widthIn(max = 520.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    content()
                }
            }
            UiMode.Miuix -> MiuixCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).widthIn(max = 520.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    MiuixText(title, color = colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    if (subtitle.isNotBlank()) {
                        MiuixText(subtitle, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp), color = colorScheme.onSurface)
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun ShortcutInformationDialog(title: String, message: String, onDismiss: () -> Unit) {
    ShortcutDialogSurface(title, message, onDismiss) {
        GlobalShortcutActionButton("知道了", true, onDismiss, Modifier.fillMaxWidth().padding(top = 10.dp))
    }
}

@Composable
private fun GlobalShortcutActionButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when (LocalUiMode.current) {
        UiMode.Material -> if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
        UiMode.Miuix -> if (primary) colorScheme.primary else colorScheme.surface
    }
    val foreground = if (primary) Color.White else when (LocalUiMode.current) {
        UiMode.Material -> MaterialTheme.colorScheme.onSurface
        UiMode.Miuix -> colorScheme.onSurface
    }
    Box(
        modifier = modifier.clip(RoundedCornerShape(999.dp)).background(background).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text(label, color = foreground, fontWeight = FontWeight.SemiBold)
            UiMode.Miuix -> MiuixText(label, color = foreground, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun loadShortcutLauncherApps(context: Context): List<ShortcutLauncherApp> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return runCatching {
        packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { resolve ->
                val info = resolve.activityInfo ?: return@mapNotNull null
                if (info.packageName == context.packageName) return@mapNotNull null
                val component = ComponentName(info.packageName, info.name)
                val label = runCatching { info.loadLabel(packageManager).toString() }.getOrDefault(info.packageName)
                ShortcutLauncherApp(label, component)
            }
            .distinctBy { it.component.flattenToString() }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}
