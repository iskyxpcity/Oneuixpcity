package com.oneuialoha.app.oneuialoha

/**
 * UI ordering/grouping rules for the OneUIAloha feature catalog. Keep them
 * separate from persisted Preference data so layout organization can evolve
 * independently of the data model.
 */
object OneUIAlohaLayoutRules {
    private val systemUiOrder = listOf(
        "systemUI.statusBar.modifyStatusBarLeftPadding",
        "systemUI.statusBar.statusBarLeftPaddingDp",
        "systemUI.statusBar.modifyStatusBarRightPadding",
        "systemUI.statusBar.statusBarRightPaddingDp",
        "systemUI.statusBar.statusBarTopPaddingDp",
        "systemUI.statusBar.statusBarBottomPaddingDp",
        "systemUI.statusBar.setBatteryIconWidthScale",
        "systemUI.statusBar.batteryIconWidthScale",
        "systemUI.statusBar.setBatteryIconHeightScale",
        "systemUI.statusBar.batteryIconHeightScale",
        "systemUI.statusBar.hideBatteryPercentageSign",
        "systemUI.statusBar.hideBatteryIcon",
        "systemUI.statusBar.addBatteryLevelText",
        "systemUI.statusBar.hideBatteryLevelTextPercentageSign",
        "systemUI.statusBar.hideBatteryLevelTextChargingIcon",
        "systemUI.statusBar.supportRealTimeNetworkSpeed",
        "systemUI.statusBar.showSeparateUpDownNetworkSpeeds",
        "systemUI.statusBar.networkSpeedThresholdKb",
        "systemUI.statusBar.setStatusBarClockFormat",
        "systemUI.statusBar.statusBarClockFormat",
        "systemUI.statusBar.useFold7CustomDoubleLineClockScale",
        "systemUI.statusBar.fold7DoubleLineClockTimeScale",
        "systemUI.statusBar.fold7DoubleLineClockDateScale",
        "systemUI.statusBar.statusBarDoubleLineClockSize",
        "systemUI.statusBar.doubleLineClockGapDp",
        "systemUI.statusBar.setStatusBarClockTextScale",
        "systemUI.statusBar.statusBarClockTextScale",
        "systemUI.statusBar.updateStatusBarClockEverySecond",
        "systemUI.statusBar.hideSecureFolderStatusBarIcon",
        "systemUI.statusBar.restoreBluetoothStatusBarIcon",
        "systemUI.statusBar.physicalEsimAdapterWorkaround",
        "systemUI.statusBar.physicalEsimAdapterSimSlot",
        "systemUI.statusBar.doubleTapStatusBarToSleep",
        "systemUI.statusBar.modifyStatusBarMaxNotificationIcons",
        "systemUI.statusBar.statusBarMaxNotificationIcons",
        "systemUI.statusBar.setCustomCarrierName",
        "systemUI.statusBar.customCarrierName",
        "systemUI.statusBar.hideLockscreenStatusBar",

        "systemUI.qs.setQsClockMonospaced",
        "systemUI.qs.hideDeviceControlQsTile",
        "systemUI.qs.hideSmartViewQsTile",
        "systemUI.qs.turnOn5gQsTile",
        "systemUI.qs.hideQsBarMediaPlayer",
        "systemUI.qs.hideQsBarNearbyDevicesAndDeviceControl",
        "systemUI.qs.hideQsBarSecurityFooter",
        "systemUI.qs.hideQsBarDataUsage",
        "systemUI.qs.hideQsBarSmartViewAndModes",
        "systemUI.qs.alwaysExpandQsTileChunk",
        "systemUI.qs.alwaysShowTimeDateOnQs",
        "systemUI.qs.addBrightnessProgressToQsBar",
        "systemUI.qs.addVolumeProgressToQsBar",
        "systemUI.qs.showTraditionalChineseDateOnQS",
        "systemUI.qs.modifyQSClockTextSize",
        "systemUI.qs.qsClockTextSize",

        "systemUI.aod.hideAODStatusBar",
        "systemUI.aod.aodLockSupportLunar",

        "systemUI.notification.hideOngoingActivityMedia",
        "systemUI.notification.hideOngoingActivityMediaPackages",
        "systemUI.notification.disableNotificationGrouping",
        "systemUI.notification.autoExpandNotifications",

        "systemUI.other.customPowerMenu",
        "systemUI.other.disableScreenshotCaptureSound",
    )

    private val systemUiOrderIndex =
        systemUiOrder.withIndex().associate { it.value to it.index }

    private val categoryOrder: Map<OneUIAlohaCategory, List<String>> = mapOf(
        OneUIAlohaCategory.Android to listOf(
            "android.disablePinVerifyPer72h",
            "android.modifyMaxNeverKilledAppNum",
            "android.maxNeverKilledAppNum",
            "android.setBlockableNotificationChannel",
            "android.supportAppJumpBlock",
            "android.allowAllRotation",
            "android.liftFcmNetworkLimit",
            "android.disableScreenWakeOnPowerUnplugged",
            "android.disableWritingToolkitGlobally",
        ),
        OneUIAlohaCategory.Settings to listOf(
            "settings.showForcePeakRefreshRatePreference",
            "settings.supportOutdoorMode",
            "settings.showMoreBatteryInfo",
            "settings.showPackageInfo",
            "settings.showWiFiLinkSpeed",
            "settings.supportAnyFont",
            "settings.supportAutoPowerOnOff",
            "settings.spoofPhoneStatusAsOfficial",
        ),
        OneUIAlohaCategory.Call to listOf(
            "call.supportVoiceCallRecording",
            "call.preferRecordingButton",
            "call.showGeocodedLocationInRecentCall",
            "call.isOpStyleCHN",
            "call.supportCallAndTextOnOtherDevices",
        ),
        OneUIAlohaCategory.Camera to listOf(
            "camera.supportAllCameraMenu",
            "camera.disableCameraTemperatureCheck",
        ),
        OneUIAlohaCategory.Browser to listOf(
            "other.showMorePlaybackSpeeds",
            "other.redirectCustomTab",
            "other.spoofBrowserCountryCodeToUS",
        ),
        OneUIAlohaCategory.Calendar to listOf(
            "other.enableChineseHolidayDisplay",
        ),
        OneUIAlohaCategory.DualApp to listOf(
            "other.makeAllUserAppsAvailable",
        ),
        OneUIAlohaCategory.Gallery to listOf(
            "other.supportAllGallerySettings",
            "other.supportSharedAlbumsInHide",
            "other.hideVideoEditorStudio",
        ),
        OneUIAlohaCategory.GalaxyStore to listOf(
            "other.blockGalaxyStoreAds",
        ),
        OneUIAlohaCategory.HealthMonitor to listOf(
            "other.bypassHealthMonitorCountryCheck",
        ),
        OneUIAlohaCategory.Launcher to listOf(
            "other.showMemoryUsageInRecents",
            "other.recentsGridThreeRows",
            "other.hideRecentsCloseAllButton",
            "other.hideAppsSearchBar",
            "other.removeShortcutBadge",
        ),
        OneUIAlohaCategory.Messaging to listOf(
            "other.supportBlockMessage",
        ),
        OneUIAlohaCategory.Notes to listOf(
            "other.supportAllNotesFeatures",
        ),
        OneUIAlohaCategory.PhotoRetouching to listOf(
            "other.noAIWatermark",
        ),
        OneUIAlohaCategory.SketchBook to listOf(
            "other.noAIWatermark",
        ),
        OneUIAlohaCategory.SPen to listOf(
            "other.useSPenGoogleTranslate",
        ),
        OneUIAlohaCategory.ThemeCenter to listOf(
            "other.setThemeTrialNeverExpired",
        ),
        OneUIAlohaCategory.Video to listOf(
            "other.showMorePlaybackSpeeds",
        ),
        OneUIAlohaCategory.WatchManager to listOf(
            "other.watchPairingConnectionMode",
            "other.bypassWatchPairingRegionCheck",
            "other.supplementChinaWearOsGms",
        ),
        OneUIAlohaCategory.Weather to listOf(
            "other.setWeatherProviderCN",
        ),
    )

    private val parentByPath = mapOf(
        "android.maxNeverKilledAppNum" to
            "android.modifyMaxNeverKilledAppNum",

        "systemUI.statusBar.statusBarLeftPaddingDp" to
            "systemUI.statusBar.modifyStatusBarLeftPadding",
        "systemUI.statusBar.statusBarRightPaddingDp" to
            "systemUI.statusBar.modifyStatusBarRightPadding",
        "systemUI.statusBar.batteryIconWidthScale" to
            "systemUI.statusBar.setBatteryIconWidthScale",
        "systemUI.statusBar.batteryIconHeightScale" to
            "systemUI.statusBar.setBatteryIconHeightScale",
        "systemUI.statusBar.hideBatteryLevelTextPercentageSign" to
            "systemUI.statusBar.addBatteryLevelText",
        "systemUI.statusBar.hideBatteryLevelTextChargingIcon" to
            "systemUI.statusBar.addBatteryLevelText",
        "systemUI.statusBar.statusBarClockFormat" to
            "systemUI.statusBar.setStatusBarClockFormat",
        "systemUI.statusBar.useFold7CustomDoubleLineClockScale" to
            "systemUI.statusBar.setStatusBarClockFormat",
        "systemUI.statusBar.fold7DoubleLineClockTimeScale" to
            "systemUI.statusBar.setStatusBarClockFormat",
        "systemUI.statusBar.fold7DoubleLineClockDateScale" to
            "systemUI.statusBar.setStatusBarClockFormat",
        "systemUI.statusBar.statusBarDoubleLineClockSize" to
            "systemUI.statusBar.setStatusBarClockFormat",
        "systemUI.statusBar.doubleLineClockGapDp" to
            "systemUI.statusBar.setStatusBarClockFormat",
        "systemUI.statusBar.statusBarClockTextScale" to
            "systemUI.statusBar.setStatusBarClockTextScale",
        "systemUI.statusBar.physicalEsimAdapterSimSlot" to
            "systemUI.statusBar.physicalEsimAdapterWorkaround",
        "systemUI.statusBar.statusBarMaxNotificationIcons" to
            "systemUI.statusBar.modifyStatusBarMaxNotificationIcons",
        "systemUI.statusBar.customCarrierName" to
            "systemUI.statusBar.setCustomCarrierName",
        "systemUI.qs.qsClockTextSize" to
            "systemUI.qs.modifyQSClockTextSize",
        "systemUI.notification.hideOngoingActivityMediaPackages" to
            "systemUI.notification.hideOngoingActivityMedia",
    )

    private val directChildrenByParent: Map<String, Set<String>> = mapOf(
        "android.modifyMaxNeverKilledAppNum" to setOf("android.maxNeverKilledAppNum"),
        "systemUI.statusBar.modifyStatusBarLeftPadding" to setOf("systemUI.statusBar.statusBarLeftPaddingDp"),
        "systemUI.statusBar.modifyStatusBarRightPadding" to setOf("systemUI.statusBar.statusBarRightPaddingDp"),
        "systemUI.statusBar.setBatteryIconWidthScale" to setOf("systemUI.statusBar.batteryIconWidthScale"),
        "systemUI.statusBar.setBatteryIconHeightScale" to setOf("systemUI.statusBar.batteryIconHeightScale"),
        "systemUI.statusBar.addBatteryLevelText" to setOf(
            "systemUI.statusBar.hideBatteryLevelTextPercentageSign",
            "systemUI.statusBar.hideBatteryLevelTextChargingIcon",
        ),
        "systemUI.statusBar.setStatusBarClockFormat" to setOf(
            "systemUI.statusBar.statusBarClockFormat",
            "systemUI.statusBar.statusBarDoubleLineClockSize",
            "systemUI.statusBar.useFold7CustomDoubleLineClockScale",
            "systemUI.statusBar.doubleLineClockGapDp",
        ),
        "systemUI.statusBar.useFold7CustomDoubleLineClockScale" to setOf(
            "systemUI.statusBar.fold7DoubleLineClockTimeScale",
            "systemUI.statusBar.fold7DoubleLineClockDateScale",
        ),
        "systemUI.statusBar.setStatusBarClockTextScale" to setOf("systemUI.statusBar.statusBarClockTextScale"),
        "systemUI.statusBar.physicalEsimAdapterWorkaround" to setOf("systemUI.statusBar.physicalEsimAdapterSimSlot"),
        "systemUI.statusBar.modifyStatusBarMaxNotificationIcons" to setOf("systemUI.statusBar.statusBarMaxNotificationIcons"),
        "systemUI.statusBar.setCustomCarrierName" to setOf("systemUI.statusBar.customCarrierName"),
        "systemUI.qs.modifyQSClockTextSize" to setOf("systemUI.qs.qsClockTextSize"),
        "systemUI.notification.hideOngoingActivityMedia" to setOf("systemUI.notification.hideOngoingActivityMediaPackages"),
        "systemUI.other.customPowerMenu" to setOf("powerMenuActions"),
    )

    fun hasChildren(path: String): Boolean =
        directChildrenByParent[path]?.isNotEmpty() == true

    fun ordered(
        category: OneUIAlohaCategory,
        settings: List<OneUIAlohaSettingSpec>,
    ): List<OneUIAlohaSettingSpec> {
        if (category == OneUIAlohaCategory.SystemUI) {
            return settings.sortedWith(
                compareBy<OneUIAlohaSettingSpec> {
                    when {
                        it.type == OneUIAlohaSettingType.PowerAction ->
                            systemUiOrderIndex["systemUI.other.customPowerMenu"] ?: Int.MAX_VALUE
                        else -> systemUiOrderIndex[it.path] ?: Int.MAX_VALUE
                    }
                }.thenBy {
                    if (it.type == OneUIAlohaSettingType.PowerAction) {
                        1 + powerActionOrder(it.powerActionName)
                    } else {
                        0
                    }
                }
            )
        }

        val order = categoryOrder[category] ?: return settings
        val index = order.withIndex().associate { it.value to it.index }
        return settings.sortedBy { index[it.path] ?: Int.MAX_VALUE }
    }

    fun groupKey(spec: OneUIAlohaSettingSpec): String =
        when {
            spec.type == OneUIAlohaSettingType.PowerAction ->
                "systemUI.other.customPowerMenu"
            else -> parentByPath[spec.path] ?: spec.path
        }

    fun section(
        category: OneUIAlohaCategory,
        spec: OneUIAlohaSettingSpec,
    ): String {
        if (category != OneUIAlohaCategory.SystemUI) return spec.section
        return when {
            spec.path.startsWith("systemUI.statusBar.") -> "Status bar"
            spec.path.startsWith("systemUI.qs.") -> "Quick settings"
            spec.path.startsWith("systemUI.aod.") -> "Always On Display"
            spec.path.startsWith("systemUI.notification.") -> "Notifications"
            spec.path.startsWith("systemUI.other.") ||
                spec.type == OneUIAlohaSettingType.PowerAction -> "Other System UI"
            else -> spec.section
        }
    }

    private fun powerActionOrder(name: String?): Int =
        when (name) {
            "power" -> 0
            "data_mode" -> 1
            "restart" -> 2
            "safe_mode" -> 3
            "lock_down_mode" -> 4
            "emergency_call" -> 5
            "medical_info" -> 6
            "side_key_settings" -> 7
            "force_restart_message" -> 8
            "restart_systemui" -> 9
            "restart_recovery" -> 10
            "restart_download" -> 11
            else -> Int.MAX_VALUE
        }
}
