package com.oneuixp.app.ui.screen.home

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import com.oneuixp.app.BuildConfig
import com.oneuixp.app.R
import com.oneuixp.app.ui.component.dialog.rememberConfirmDialog
import com.oneuixp.app.ui.component.miuix.WarningCard
import com.oneuixp.app.ui.component.rebootlistpopup.RebootListPopupMiuix
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.theme.isInDarkTheme
import com.oneuixp.app.ui.util.BlurredBar
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
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
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusCard()
                        if (state.checkUpdateEnabled) UpdateCard(state, actions)
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
            message = stringResource(R.string.new_version_available, version.versionName),
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

    // Match the ROOT-active KernelSU v3.2.5 three-card dashboard:
    // intrinsic-height row, left status card, right stacked counter cards.
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = CardDefaults.defaultColors(color = mainCardColor),
            pressFeedbackType = PressFeedbackType.Tilt,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxSize().offset(38.dp, 45.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        modifier = Modifier.size(170.dp),
                        imageVector = if (active) Icons.Rounded.CheckCircleOutline
                            else Icons.Rounded.ErrorOutline,
                        tint = if (active) {
                            if (isDynamicColor) colorScheme.primary.copy(alpha = 0.8f)
                            else Color(0xFF36D167)
                        } else colorScheme.error.copy(alpha = 0.8f),
                        contentDescription = null,
                    )
                }
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = when {
                            status.injected -> "工作中"
                            status.scopeActive -> "已激活"
                            status.frameworkConnected && !status.scopeKnown -> "正在读取作用域"
                            status.frameworkConnected -> "作用域未激活"
                            else -> "状态待确认"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "版本：${BuildConfig.VERSION_NAME}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            DashboardSmallCardMiuix(
                title = "模块注入数量",
                value = status.injectionCount.toString(),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(12.dp))
            DashboardSmallCardMiuix(
                title = "作用域数量",
                value = status.scopeCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DashboardSmallCardMiuix(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InfoCard() {
    val info = rememberOneuiXPDeviceInfo()

    @Composable
    fun Item(title: String, content: String, bottom: Dp = 20.dp) {
        Text(
            text = title,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
        )
        Text(
            text = content,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp, bottom = bottom),
        )
    }

    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Item("手机型号", info.model)
            Item("Android / API", info.androidApi)
            Item("One UI", info.oneUi)
            Item("RAM | ROM", info.ramRom)
            Item("设备代号", info.deviceCode)
            Item("处理器", info.chipset)
            Item("ABI / CPU 架构", info.abiArchitecture)
            Item("分辨率 / DPI", info.resolutionDpi)
            Item("安全补丁", info.securityPatch, 0.dp)
        }
    }
}
