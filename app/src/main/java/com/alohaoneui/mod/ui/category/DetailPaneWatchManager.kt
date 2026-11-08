            add {
                SelectItem(
                                            icon = ImageVector.vectorResource(id = R.drawable.watch_pairing),
                                            title = stringResource(id = R.string.alohaoneui_watchPairing_connectionMode_title),
                                            summary = stringResource(id = R.string.alohaoneui_watchPairing_connectionMode_summary),
                                            entries = listOf(
                                                stringResource(id = R.string.alohaoneui_watchPairing_mode_none),
                                                stringResource(id = R.string.alohaoneui_watchPairing_mode_wearos_cn),
                                                stringResource(id = R.string.alohaoneui_watchPairing_mode_wearos_global)
                                            ),
                                            selectedIndex = uiState.watchPairingConnectionMode,
                                            onSelectedIndexChange = { onEvent(WatchManagerEvent.WatchPairingConnectionMode(it)) }
                                        )
            }
            add {
                SwitchItem(
                                            icon = ImageVector.vectorResource(id = R.drawable.lock_open),
                                            title = stringResource(id = R.string.alohaoneui_bypassWatchPairingRegionCheck_title),
                                            summary = stringResource(id = R.string.alohaoneui_bypassWatchPairingRegionCheck_summary),
                                            checked = uiState.bypassWatchPairingRegionCheck,
                                            onCheckedChange = { onEvent(WatchManagerEvent.BypassWatchPairingRegionCheck(it)) }
                                        )
            }
            if (uiState.watchPairingConnectionMode == WATCH_PAIRING_MODE_CN) add {
                SwitchItem(
                                icon = ImageVector.vectorResource(id = R.drawable.google_play),
                                title = stringResource(id = R.string.alohaoneui_supplementChinaWearOsGms_title),
                                summary = stringResource(id = R.string.alohaoneui_supplementChinaWearOsGms_summary),
                                checked = uiState.supplementChinaWearOsGms,
                                onCheckedChange = { onEvent(WatchManagerEvent.SupplementChinaWearOsGms(it)) }
                            )
            }
        }
    )
}

sealed interface WatchManagerEvent {
    @JvmInline
    value class BypassWatchPairingRegionCheck(val value: Boolean) : WatchManagerEvent

    @JvmInline
    value class WatchPairingConnectionMode(val value: Int) : WatchManagerEvent

    @JvmInline
    value class SupplementChinaWearOsGms(val value: Boolean) : WatchManagerEvent
}

fun AlohaOneUIToolsViewModel.onWatchManagerEvent(event: WatchManagerEvent) {
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
