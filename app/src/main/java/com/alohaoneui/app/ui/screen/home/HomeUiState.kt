package com.alohaoneui.app.ui.screen.home

import androidx.compose.runtime.Immutable
import com.alohaoneui.app.ui.util.update.LatestVersionInfo
import com.alohaoneui.app.ui.util.update.compareAlohaOneUIVersions

/** Only actual AlohaOneUI self-update information; device details are read by UI directly. */
@Immutable
data class HomeUiState(
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentManagerVersionName: String,
) {
    val hasUpdate: Boolean
        get() = latestVersionInfo.versionName.isNotBlank() &&
            compareAlohaOneUIVersions(
                latestVersionInfo.versionName,
                currentManagerVersionName,
            ) > 0
}

@Immutable
data class HomeActions(
    val onInstallUpdate: (LatestVersionInfo) -> Unit,
    val onActivateLsposed: () -> Unit,
)
