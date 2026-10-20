package com.oneuialoha.app.ui.screen.home

import androidx.compose.runtime.Immutable
import com.oneuialoha.app.ui.util.update.LatestVersionInfo
import com.oneuialoha.app.ui.util.update.compareOneUIAlohaVersions

/** Only actual OneUIAloha self-update information; device details are read by UI directly. */
@Immutable
data class HomeUiState(
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentManagerVersionName: String,
) {
    val hasUpdate: Boolean
        get() = latestVersionInfo.versionName.isNotBlank() &&
            compareOneUIAlohaVersions(
                latestVersionInfo.versionName,
                currentManagerVersionName,
            ) > 0
}

@Immutable
data class HomeActions(
    val onInstallUpdate: (LatestVersionInfo) -> Unit,
    val onActivateLsposed: () -> Unit,
)
