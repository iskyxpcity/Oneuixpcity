package com.alohaoneui.app.ui.screen.status

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alohaoneui.app.ui.viewmodel.AlohaOneUIToolsViewModel

@Composable
fun StatusWifiCategoryControls() {
    val toolsViewModel = viewModel<AlohaOneUIToolsViewModel>()
    val preference = toolsViewModel.preference.collectAsStateWithLifecycle().value

    StatusWifiCategoryControls(
        wifiRuntime = preference.systemUI.statusBar.wifiRuntime,
        onPreview = { runtime ->
            toolsViewModel.previewData { current ->
                current.copy(
                    systemUI = current.systemUI.copy(
                        statusBar = current.systemUI.statusBar.copy(
                            wifiRuntime = runtime,
                        ),
                    ),
                )
            }
        },
        onChange = { runtime ->
            toolsViewModel.updateData { current ->
                current.copy(
                    systemUI = current.systemUI.copy(
                        statusBar = current.systemUI.statusBar.copy(
                            wifiRuntime = runtime,
                        ),
                    ),
                )
            }
        },
    )
}
