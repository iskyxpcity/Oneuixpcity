package me.weishu.kernelsu.ui.screen.home

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.ui.util.module.LatestVersionInfo

/** Only actual OneuiXP self-update information; device details are read by UI directly. */
@Immutable
data class HomeUiState(
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentManagerVersionCode: Long,
) {
    val hasUpdate: Boolean
        get() = latestVersionInfo.versionCode > currentManagerVersionCode
}

@Immutable
data class HomeActions(val onOpenUrl: (String) -> Unit)
