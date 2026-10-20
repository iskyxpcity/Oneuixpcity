package com.oneuialoha.mod.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.oneuialoha.app.R
import com.oneuialoha.common.Preference
import com.oneuialoha.app.ui.viewmodel.OneUIAlohaToolsViewModel
import com.oneuialoha.mod.ui.component.SelectItem
import com.oneuialoha.mod.ui.component.SwitchItem

private const val WATCH_PAIRING_MODE_CN = 1

@Composable
fun DetailPaneWatchManager(
    uiState: Preference.Other,
    onEvent: (WatchManagerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PackagePane(modifier) {
        SelectItem(
            icon = ImageVector.vectorResource(id = R.drawable.watch_pairing),
            title = stringResource(id = R.string.oneuialoha_watchPairing_connectionMode_title),
            summary = stringResource(id = R.string.oneuialoha_watchPairing_connectionMode_summary),
            entries = listOf(
                stringResource(id = R.string.oneuialoha_watchPairing_mode_none),
                stringResource(id = R.string.oneuialoha_watchPairing_mode_wearos_cn),
                stringResource(id = R.string.oneuialoha_watchPairing_mode_wearos_global)
            ),
            selectedIndex = uiState.watchPairingConnectionMode,
            onSelectedIndexChange = { onEvent(WatchManagerEvent.WatchPairingConnectionMode(it)) }
        )
        SwitchItem(
            icon = ImageVector.vectorResource(id = R.drawable.lock_open),
            title = stringResource(id = R.string.oneuialoha_bypassWatchPairingRegionCheck_title),
            summary = stringResource(id = R.string.oneuialoha_bypassWatchPairingRegionCheck_summary),
            checked = uiState.bypassWatchPairingRegionCheck,
            onCheckedChange = { onEvent(WatchManagerEvent.BypassWatchPairingRegionCheck(it)) }
        )
        if (uiState.watchPairingConnectionMode == WATCH_PAIRING_MODE_CN) {
            SwitchItem(
                icon = ImageVector.vectorResource(id = R.drawable.google_play),
                title = stringResource(id = R.string.oneuialoha_supplementChinaWearOsGms_title),
                summary = stringResource(id = R.string.oneuialoha_supplementChinaWearOsGms_summary),
                checked = uiState.supplementChinaWearOsGms,
                onCheckedChange = { onEvent(WatchManagerEvent.SupplementChinaWearOsGms(it)) }
            )
        }
    }
}

sealed interface WatchManagerEvent {
    @JvmInline
    value class BypassWatchPairingRegionCheck(val value: Boolean) : WatchManagerEvent

    @JvmInline
    value class WatchPairingConnectionMode(val value: Int) : WatchManagerEvent

    @JvmInline
    value class SupplementChinaWearOsGms(val value: Boolean) : WatchManagerEvent
}

fun OneUIAlohaToolsViewModel.onWatchManagerEvent(event: WatchManagerEvent) {
    updateData { preference ->
        when (event) {
            is WatchManagerEvent.BypassWatchPairingRegionCheck -> preference.copy(
                other = preference.other.copy(
                    bypassWatchPairingRegionCheck = event.value
                )
            )

            is WatchManagerEvent.WatchPairingConnectionMode -> preference.copy(
                other = preference.other.copy(
                    watchPairingConnectionMode = event.value,
                    supplementChinaWearOsGms = if (event.value == WATCH_PAIRING_MODE_CN) {
                        preference.other.supplementChinaWearOsGms
                    } else {
                        false
                    }
                )
            )

            is WatchManagerEvent.SupplementChinaWearOsGms -> preference.copy(
                other = preference.other.copy(
                    supplementChinaWearOsGms = event.value
                )
            )
        }
    }
}
