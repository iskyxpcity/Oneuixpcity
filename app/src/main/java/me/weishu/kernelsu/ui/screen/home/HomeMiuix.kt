package me.weishu.kernelsu.ui.screen.home

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.WarningLevel
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.miuix.WarningCard
import me.weishu.kernelsu.ui.component.rebootlistpopup.RebootListPopupMiuix
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(state: HomeUiState, actions: HomeActions, bottomInnerPadding: Dp) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.app_name),
                    actions = { RebootListPopupMiuix() },
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
                modifier = Modifier.fillMaxHeight().scrollEndHaptic().overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection).padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.checkUpdateEnabled) UpdateCard(state, actions)
                        StatusCard()
                        InfoCard()
                        Spacer(Modifier.height(bottomInnerPadding))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(state: HomeUiState, actions: HomeActions) {
    val version = state.latestVersionInfo
    val title = stringResource(R.string.module_changelog)
    val update = stringResource(R.string.module_update)
    val dialog = rememberConfirmDialog(onConfirm = { actions.onOpenUrl(version.downloadUrl) })
    AnimatedVisibility(
        visible = state.hasUpdate,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        WarningCard(
            message = stringResource(R.string.new_version_available, version.versionCode),
            level = WarningLevel.Notice,
            onClick = {
                if (version.changelog.isEmpty()) actions.onOpenUrl(version.downloadUrl)
                else dialog.showConfirm(
                    title = title,
                    content = version.changelog,
                    markdown = true,
                    confirm = update,
                )
            },
        )
    }
}

@Composable
private fun StatusCard() {
    val status = rememberLsposedHomeStatus()
    val active = status.active
    val mainCardColor = if (active) {
        when {
            isDynamicColor -> colorScheme.secondaryContainer
            isInDarkTheme() -> Color(0xFF1A3825)
            else -> Color(0xFFDFFAE4)
        }
    } else colorScheme.errorContainer

    Row(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = CardDefaults.defaultColors(color = mainCardColor),
            pressFeedbackType = PressFeedbackType.Tilt,
        ) {
            Box(Modifier.fillMaxSize()) {
                // Keep precisely the upstream KernelSU 110dp symbol, 27/31dp offset
                // and 80% dynamic-primary tint instead of the previous 138dp/58% tint.
                Icon(
                    imageVector = if (active) Icons.Rounded.CheckCircleOutline
                    else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .offset(x = 27.dp, y = 31.dp).size(110.dp),
                    tint = if (active) {
                        if (isDynamicColor) colorScheme.primary.copy(alpha = 0.8f)
                        else Color(0xFF36D167)
                    } else colorScheme.error.copy(alpha = 0.8f),
                )
                Column(modifier = Modifier.fillMaxSize().padding(16.dp, 14.dp)) {
                    // Upstream uses 22sp titles. Explicit lines prevent the extra
                    // full-width angle bracket from wrapping alone at half width.
                    Text(
                        text = if (active) "工作中" else "等待激活",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (active) "＜已激活＞" else "＜未收到注入报告＞",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (active) "LSPosed / SystemUI" else "需等待 SystemUI 加载模块",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "OneuiXP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardSmallCardMiuix(
                title = "模块注入数量", value = status.injectionCount.toString(),
                modifier = Modifier.weight(1f),
            )
            DashboardSmallCardMiuix(
                title = "作用域数量", value = status.scopeCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DashboardSmallCardMiuix(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), pressFeedbackType = PressFeedbackType.Tilt) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = value, fontSize = 34.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun InfoCard() {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memory = ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
    val storage = StatFs(Environment.getDataDirectory().path)
    fun gigabytes(value: Long) = String.format("%.1f GB", value / 1073741824.0)
    val oneUi = runCatching {
        val cls = Class.forName("android.os.SystemProperties")
        val get = cls.getMethod("get", String::class.java, String::class.java)
        (get.invoke(null, "ro.build.version.oneui", "") as String).ifBlank { "-" }
    }.getOrDefault("-")
    val chipset = if (Build.VERSION.SDK_INT >= 31)
        Build.SOC_MODEL.ifBlank { Build.HARDWARE } else Build.HARDWARE
    val architecture = System.getProperty("os.arch") ?: "-"

    @Composable
    fun Item(title: String, content: String, bottom: Dp = 20.dp) {
        Text(
            text = title, fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium, color = colorScheme.onSurface,
        )
        Text(
            text = content, fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp, bottom = bottom),
        )
    }
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Item("手机型号", "${Build.MANUFACTURER} ${Build.MODEL}")
            Item("Android / API", "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            Item("One UI", oneUi)
            Item("RAM | ROM", "${gigabytes(memory.totalMem)} | ${gigabytes(storage.totalBytes)}")
            Item("设备代号", Build.DEVICE)
            Item("处理器", chipset)
            Item("ABI / CPU 架构", "${Build.SUPPORTED_ABIS.firstOrNull() ?: "-"} / $architecture")
            Item("分辨率 / DPI", "${metrics.widthPixels}×${metrics.heightPixels} / ${metrics.densityDpi}dpi")
            Item("安全补丁", Build.VERSION.SECURITY_PATCH.ifBlank { "-" }, 0.dp)
        }
    }
}
