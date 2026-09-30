package com.oneuixp.app.ui.screen.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.navigation3.Navigator
import com.oneuixp.app.ui.util.update.UpdateDownloadResult
import com.oneuixp.app.ui.util.update.downloadAndVerifyUpdate
import com.oneuixp.app.ui.util.update.UpdateInstallResult
import com.oneuixp.app.ui.util.update.openUnknownSourcesSettings
import com.oneuixp.app.ui.util.update.submitUpdateSession
import com.oneuixp.app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true
) {
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasActivated by remember { mutableStateOf(false) }
    if (isCurrentPage) hasActivated = true

    if (hasActivated) {
        LaunchedEffect(Unit) {
            viewModel.refresh()
        }
    }

    val scope = rememberCoroutineScope()
    var updateInProgress by remember { mutableStateOf(false) }

    val actions = HomeActions(
        onInstallUpdate = { version ->
            if (updateInProgress) {
                Toast.makeText(context, "更新正在处理中", Toast.LENGTH_SHORT).show()
            } else {
                updateInProgress = true
                Toast.makeText(
                    context,
                    "正在下载 OneuiXP ${version.versionName}",
                    Toast.LENGTH_SHORT,
                ).show()

                scope.launch {
                    try {
                        when (val result = downloadAndVerifyUpdate(context, version)) {
                            is UpdateDownloadResult.Success -> {
                                Toast.makeText(
                                    context,
                                    "更新包校验完成，正在交给系统安装器",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                when (val install = submitUpdateSession(context, result.apk)) {
                                    UpdateInstallResult.Submitted -> {
                                        Toast.makeText(
                                            context,
                                            "已提交系统安装会话",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }

                                    UpdateInstallResult.PermissionRequired -> {
                                        val opened = openUnknownSourcesSettings(context)
                                        Toast.makeText(
                                            context,
                                            if (opened) {
                                                "请允许 OneuiXP 安装未知应用，返回后再次点击升级"
                                            } else {
                                                "无法打开“安装未知应用”系统设置"
                                            },
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }

                                    is UpdateInstallResult.Error -> {
                                        Toast.makeText(
                                            context,
                                            install.message,
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                }
                            }

                            is UpdateDownloadResult.Error -> {
                                Toast.makeText(
                                    context,
                                    result.message,
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    } catch (throwable: Throwable) {
                        Toast.makeText(
                            context,
                            throwable.message?.takeIf { it.isNotBlank() }
                                ?: "更新失败，请重试",
                            Toast.LENGTH_LONG,
                        ).show()
                    } finally {
                        updateInProgress = false
                    }
                }
            }
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> HomePagerMaterial(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}
