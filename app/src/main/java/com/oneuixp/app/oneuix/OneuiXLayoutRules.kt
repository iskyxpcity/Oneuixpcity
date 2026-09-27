package com.oneuixp.app.oneuix

/**
 * UI ordering/grouping rules copied from the visual order of OneUIX-Mod's
 * DetailPane* screens. This is deliberately separate from Preference so
 * upstream synchronization does not require changing the data model.
 */
object OneuiXLayoutRules {
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

        "other.supplementChinaWearOsGms" to
            "other.watchPairingConnectionMode",
    )

    fun ordered(
        category: OneuiXCategory,
        settings: List<OneuiXSettingSpec>,
    ): List<OneuiXSettingSpec> {
        if (category != OneuiXCategory.SystemUI) return settings
        return settings.sortedWith(
            compareBy<OneuiXSettingSpec> {
                when {
                    it.type == OneuiXSettingType.PowerAction ->
                        systemUiOrderIndex["systemUI.other.customPowerMenu"]?.plus(1) ?: Int.MAX_VALUE
                    else -> systemUiOrderIndex[it.path] ?: Int.MAX_VALUE
                }
            }.thenBy {
                if (it.type == OneuiXSettingType.PowerAction) {
                    powerActionOrder(it.powerActionName)
                } else {
                    0
                }
            }
        )
    }

    fun groupKey(spec: OneuiXSettingSpec): String =
        when {
            spec.type == OneuiXSettingType.PowerAction ->
                "systemUI.other.customPowerMenu"
            else -> parentByPath[spec.path] ?: spec.path
        }

    fun section(
        category: OneuiXCategory,
        spec: OneuiXSettingSpec,
    ): String {
        if (category != OneuiXCategory.SystemUI) return spec.section
        return when {
            spec.path.startsWith("systemUI.statusBar.") -> "Status bar"
            spec.path.startsWith("systemUI.qs.") -> "Quick settings"
            spec.path.startsWith("systemUI.aod.") -> "Always On Display"
            spec.path.startsWith("systemUI.notification.") -> "Notifications"
            spec.path.startsWith("systemUI.other.") ||
                spec.type == OneuiXSettingType.PowerAction -> "Other System UI"
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
