package io.github.soclear.oneuix.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Preference(
    val android: Android = Android(),
    val systemUI: SystemUI = SystemUI(),
    val settings: Settings = Settings(),
    val call: Call = Call(),
    val camera: Camera = Camera(),
    val other: Other = Other(),
) {
    @Serializable
    data class Android(
        val disablePinVerifyPer72h: Boolean = false,
        val modifyMaxNeverKilledAppNum: Boolean = false,
        val maxNeverKilledAppNum: Int = 5,
        val setBlockableNotificationChannel: Boolean = false,
        val supportAppJumpBlock: Boolean = false,
        val allowAllRotation: Boolean = false,
        val liftFcmNetworkLimit: Boolean = false,
        val disableScreenWakeOnPowerUnplugged: Boolean = false,
        val disableWritingToolkitGlobally: Boolean = false,
    )

    @Serializable
    data class SystemUI(
        val statusBar: StatusBar = StatusBar(),
        val qs: QS = QS(),
        val aod: AOD = AOD(),
        val notification: Notification = Notification(),
        val other: Other = Other(),
    ) {
        @Serializable
        data class StatusBarClockBlockStyle(
            val blockIndex: Int = 0,
            val source: String = "",
            val enabled: Boolean = false,
            val overrideTextSize: Boolean = false,
            val textSizeSp: Float = 12f,
            val overrideFontWeight: Boolean = false,
            val fontWeight: Int = 400,
            val overrideTextColor: Boolean = false,
            val textColorArgb: Int = 0xFFFFFFFF.toInt(),
            val backgroundEnabled: Boolean = false,
            val backgroundColorArgb: Int = 0xFF000000.toInt(),
            val backgroundAlpha: Float = 0.25f,
            val backgroundBorderEnabled: Boolean = false,
            val backgroundBorderColorArgb: Int = 0xFFFFFFFF.toInt(),
            val backgroundBorderWidthDp: Float = 1f,
            val cornerRadiusDp: Float = 4f,
            val horizontalPaddingDp: Float = 2f,
            val verticalPaddingDp: Float = 0f,
            val horizontalOffsetDp: Float = 0f,
            val verticalOffsetDp: Float = 0f,
            val verticalAlignment: String = "middle",
        )

        @Serializable
        data class StatusBarPowerRuntime(
            val enabled: Boolean = false,
            val overlayOnTop: Boolean = false,
            val placementSide: String = "right",
            val slot1: String = "power",
            val slot2: String = "battery_temp",
            val slot3: String = "cpu_temp",
            val slot4: String = "current",
            val slot1Decimals: Int = 1,
            val slot2Decimals: Int = 1,
            val slot3Decimals: Int = 1,
            val slot4Decimals: Int = 0,
            val doubleLine: Boolean = true,
            val absoluteValue: Boolean = true,
            val hideUnit: Boolean = false,
            val refreshIntervalMs: Int = 1000,
            val textSizeSp: Float = 8.5f,
            val fontWeight: Int = 400,
            val columnGapDp: Float = 2f,
            val rowGapDp: Float = 0f,
            val horizontalOffsetDp: Float = 0f,
            val verticalOffsetDp: Float = 0f,
            val startPaddingDp: Float = 1f,
            val endPaddingDp: Float = 1f,
            val hideOnLockscreen: Boolean = false,
        )

        @Serializable
        data class StatusBarWifiArcStyle(
            val length: Float = 1f,
            val strokeWidthDp: Float = 1.4f,
            val leftRadius: Float = 1f,
            val rightRadius: Float = 1f,
            val leftTiltDeg: Float = 0f,
            val rightTiltDeg: Float = 0f,
            val bend: Float = 0f,
            val yOffsetDp: Float = 0f,
        )

        @Serializable
        data class StatusBarWifiRuntime(
            val enabled: Boolean = false,
            val editorEnabled: Boolean = false,
            val hideActivity: Boolean = false,
            val hideStandardBadge: Boolean = false,
            val extendStandardBadge: Boolean = false,
            val arcCount: Int = 3,
            val overallSizeDp: Float = 16f,
            val blackDepth: Float = 1f,
            val verticalOffsetDp: Float = 0f,
            val startMarginDp: Float = 0f,
            val endMarginDp: Float = 0f,
            val preset: String = "default",
            val arc1: StatusBarWifiArcStyle = StatusBarWifiArcStyle(
                length = 1f,
                strokeWidthDp = 1.5f,
                leftRadius = 1f,
                rightRadius = 1f,
                bend = 0.08f,
                yOffsetDp = -0.6f,
            ),
            val arc2: StatusBarWifiArcStyle = StatusBarWifiArcStyle(
                length = 0.84f,
                strokeWidthDp = 1.45f,
                leftRadius = 0.84f,
                rightRadius = 0.84f,
                bend = 0.06f,
                yOffsetDp = -0.1f,
            ),
            val arc3: StatusBarWifiArcStyle = StatusBarWifiArcStyle(
                length = 0.66f,
                strokeWidthDp = 1.4f,
                leftRadius = 0.66f,
                rightRadius = 0.66f,
                bend = 0.04f,
                yOffsetDp = 0.45f,
            ),
            val arc4: StatusBarWifiArcStyle = StatusBarWifiArcStyle(
                length = 0.48f,
                strokeWidthDp = 1.35f,
                leftRadius = 0.48f,
                rightRadius = 0.48f,
                bend = 0.02f,
                yOffsetDp = 0.9f,
            ),
        )

        @Serializable
        data class StatusBarBluetoothRuntime(
            val enabled: Boolean = false,
            val showLeftSide: Boolean = false,
            val customIconEnabled: Boolean = false,
            val portraitSizeDp: Float = 14f,
            val landscapeSizeDp: Float = 14f,
            val horizontalOffsetDp: Float = 0f,
            val verticalOffsetDp: Float = 0f,
            val startMarginDp: Float = 0f,
            val endMarginDp: Float = 0f,
        )

        @Serializable
        data class StatusBarSignalRuntime(
            val enabled: Boolean = false,
            val showSim1: Boolean = true,
            val showSim2: Boolean = true,
            val portraitSizeDp: Float = 16f,
            val landscapeSizeDp: Float = 16f,
            val horizontalOffsetDp: Float = 0f,
            val verticalOffsetDp: Float = 0f,
            val startMarginDp: Float = 0f,
            val endMarginDp: Float = 0f,
        )

        @Serializable
        data class StatusBarComboStyleOneRuntime(
            val enabled: Boolean = false,
            val overlayGlyph: Boolean = true,
            val showWifi: Boolean = true,
            val showMobile: Boolean = true,
            val showValue: Boolean = true,
            val showBolt: Boolean = true,
            // 0 = bolt takes priority while charging; 1 = alternate bolt/value every 5 seconds.
            val chargingDisplayMode: Int = 0,
            val chargingBoltSizePercent: Int = 100,
            val chargingBoltXDp: Int = 0,
            val chargingBoltYDp: Int = 0,
            val mobileTypeMode: Int = 0,
            val valueCentred: Boolean = false,
            val trioStyle: Int = 0,
            val dualSim: Boolean = false,
            // 0 = single/data SIM only, 1 = two rows at top/bottom of ring, 2 = two rows below ring.
            // The legacy boolean is retained so older saved preferences keep their prior dual-row behavior.
            val dualSimMode: Int = 0,
            // Ring-style signal-dot tuning. Spacing is centre-to-centre at 100% overall scale.
            val signalDotSizePercent: Int = 100,
            val signalDotSpacingDp: Int = 5,
            val signalDotOffsetXDp: Int = 0,
            val signalDotOffsetYDp: Int = 0,
            val signalMode: Int = 0,
            val stackedSignal: Boolean = false,
            val dataSimOnly: Boolean = false,
            val roleColors: Boolean = true,
            val glyphScalePercent: Int = 100,
            val glyphScaleDefaultMigrationDone: Boolean = false,
            val glyphScaleDefault100MigrationDone: Boolean = false,
            val glyphOffsetXDp: Int = 0,
            val glyphOffsetYDp: Int = 0,
            val glyphLeftMarginDp: Int = 2,
            val glyphLeftMarginOneDpMigrationDone: Boolean = false,
            val glyphLeftMarginZeroDpMigrationDone: Boolean = false,
            val glyphLeftMarginRestoreOneDpMigrationDone: Boolean = false,
            val glyphLeftMarginTwoDpDefaultMigrationDone: Boolean = false,
            val glyphRightMarginDp: Int = 0,
            val criticalColorDarkArgb: Int = 0xFFFF3B30.toInt(),
            val criticalColorLightArgb: Int = 0xFFFF3B30.toInt(),
            val chargingColorDarkArgb: Int = 0xFF34C759.toInt(),
            val chargingColorLightArgb: Int = 0xFF1F8F3D.toInt(),
            // Defaults preserve native status-bar tint for ordinary charging and blue for fast charging.
            val normalChargingBoltColorDarkArgb: Int = 0xFFFFFFFF.toInt(),
            val normalChargingBoltColorLightArgb: Int = 0xFF000000.toInt(),
            val quickChargingBoltColorDarkArgb: Int = 0xFF2196F3.toInt(),
            val quickChargingBoltColorLightArgb: Int = 0xFF2196F3.toInt(),
            val lowColorDarkArgb: Int = 0xFFF2B900.toInt(),
            val lowColorLightArgb: Int = 0xFFC99700.toInt(),
            val powerSaveFillColorDarkArgb: Int = 0xBFFFC107.toInt(),
            val powerSaveFillColorLightArgb: Int = 0xBFFFC107.toInt(),
            val lowThreshold: Int = 20,
            // HyperDuo's critical cutoff is fixed at 20%; retain the key for
            // saved-setting compatibility but sanitize it to this upstream value.
            val criticalThreshold: Int = 20,
            val ringStroke: Int = 14,
            val arcStroke: Int = 12,
            val valueSize: Int = 11,
            val valueWeight: Int = 700,
            val typeSize: Int = 32,
            val outTypeSizeDp: Int = 14,
            val outSignalSizeDp: Int = 15,
            // Outer/left margin for the self-drawn signal lane; zero preserves existing geometry.
            val outSignalMarginLeftDp: Int = 0,
            val outTypeMarginLeftDp: Int = 0,
            val outTypeMarginRightDp: Int = 0,
            val outTypeOffsetXDp: Int = 0,
            val outTypeOffsetYDp: Int = 0,
            val outSignalMarginDp: Int = 0,
            val outSignalOffsetXDp: Int = 0,
            val outSignalOffsetYDp: Int = 0,
            val outSignalMarginDefaultMigrationDone: Boolean = false,
            val outSignalMarginReset2MigrationDone: Boolean = false,
            val outSignalMarginFourDpMigrationDone: Boolean = false,
            val outSignalMarginSixDpMigrationDone: Boolean = false,
            val outSignalMarginEightDpMigrationDone: Boolean = false,
            // One-time ring-layout migration marker; overall position and spacing controls remain active.
            val ringTrailingAnchorMigrationDone: Boolean = false,
            val typeSuffixScale: Int = 65,
            val typeWeight: Int = 700,
            val trackAlpha: Int = 56,
        )

        @Serializable
        data class StatusBarNetworkSpeedRuntime(
            val enabled: Boolean = false,
            val fontSizeSp: Float = 8.5f,
            val fontWeight: Int = 400,
            val horizontalOffsetDp: Float = 0f,
            val verticalOffsetDp: Float = 0f,
            val hideUnit: Boolean = false,
            val hideBs: Boolean = false,
            val alignRight: Boolean = false,
            val hideOnLockscreen: Boolean = false,
        )

        @Serializable
        data class StatusBar(
            val modifyStatusBarLeftPadding: Boolean = false,
            val statusBarLeftPaddingDp: Float = 0f,
            val modifyStatusBarRightPadding: Boolean = false,
            val statusBarRightPaddingDp: Float = 0f,
            val modifyStatusBarTopPadding: Boolean = false,
            val statusBarTopPaddingDp: Float = 0f,
            val modifyStatusBarBottomPadding: Boolean = false,
            val statusBarBottomPaddingDp: Float = 0f,
            val statusBarBurnInProtectionEnabled: Boolean = false,
            val statusBarBurnInProtectionAmplitudeDp: Float = 0.50f,
            val statusBarBurnInProtectionPeriodSeconds: Int = 60,
            val setBatteryIconWidthScale: Boolean = false,
            val batteryIconWidthScale: Float = 1f,
            val setBatteryIconHeightScale: Boolean = false,
            val batteryIconHeightScale: Float = 1f,
            val hideBatteryPercentageSign: Boolean = false,
            val hideBatteryIcon: Boolean = false,
            val addBatteryLevelText: Boolean = false,
            val hideBatteryLevelTextPercentageSign: Boolean = false,
            val hideBatteryLevelTextChargingIcon: Boolean = false,
            val powerRuntime: StatusBarPowerRuntime = StatusBarPowerRuntime(),
            val comboStyleOneRuntime: StatusBarComboStyleOneRuntime = StatusBarComboStyleOneRuntime(),
            val wifiRuntime: StatusBarWifiRuntime = StatusBarWifiRuntime(),
            val bluetoothRuntime: StatusBarBluetoothRuntime = StatusBarBluetoothRuntime(),
            val signalRuntime: StatusBarSignalRuntime = StatusBarSignalRuntime(),
            val networkSpeedRuntime: StatusBarNetworkSpeedRuntime = StatusBarNetworkSpeedRuntime(),
            val supportRealTimeNetworkSpeed: Boolean = true,
            val showSeparateUpDownNetworkSpeeds: Boolean = false,
            val networkSpeedThresholdKb: Int = 0,
            val modifyNetworkSpeedLineGap: Boolean = false,
            val networkSpeedLineGapDp: Float = 0f,
            // Legacy #556/#557 whole-text fields are retained for preference compatibility.
            val setNetworkSpeedTextScale: Boolean = false,
            val networkSpeedTextScale: Float = 1f,
            val setNetworkSpeedUploadTextScale: Boolean = false,
            val networkSpeedUploadTextScale: Float = 1f,
            val setNetworkSpeedDownloadTextScale: Boolean = false,
            val networkSpeedDownloadTextScale: Float = 1f,
            val setStatusBarClockFormat: Boolean = false,
            val statusBarClockFormat: String = "HH:mm",
            val enableStatusBarClockRichStyle: Boolean = false,
            val statusBarClockBlockStyles: List<StatusBarClockBlockStyle> = emptyList(),
            val modifyStatusBarClockPosition: Boolean = false,
            val statusBarClockOffsetXDp: Float = 0f,
            val statusBarClockOffsetYDp: Float = 0f,
            val statusBarClockLeftMarginDp: Float = 0f,
            val statusBarClockRightMarginDp: Float = 0f,
            val statusBarClockAlignment: String = "native",
            val statusBarClockFixedContentWidthDp: Float = 0f,
            // Empty means this is a pre-V11 preference. The runtime maps its old float preset.
            val statusBarDoubleLineClockSize: String = "",
            val statusBarDoubleLineClockVerticalPosition: String = "middle",
            val setStatusBarClockTextScale: Boolean = false,
            val statusBarClockTextScale: Float = 1f,
            val modifyDoubleLineClockGap: Boolean = false,
            val doubleLineClockGapDp: Float = 0f,
            val useFold7CustomDoubleLineClockScale: Boolean = false,
            val fold7DoubleLineClockTimeScale: Float = 1f,
            val fold7DoubleLineClockDateScale: Float = 1f,
            val updateStatusBarClockEverySecond: Boolean = false,
            val hideSecureFolderStatusBarIcon: Boolean = false,
            val physicalEsimAdapterWorkaround: Boolean = false,
            val physicalEsimAdapterSimSlot: Int = 1,
            val doubleTapStatusBarToSleep: Boolean = false,
            val modifyStatusBarMaxNotificationIcons: Boolean = false,
            val statusBarMaxNotificationIcons: Int = 4,
            val setCustomCarrierName: Boolean = false,
            val customCarrierName: String = "",
            val hideLockscreenStatusBar: Boolean = false,
            val restoreBluetoothStatusBarIcon: Boolean = false,
        )

        @Serializable
        data class QS(
            val setQsClockMonospaced: Boolean = false,
            val hideDeviceControlQsTile: Boolean = false,
            val hideSmartViewQsTile: Boolean = false,
            val turnOn5gQsTile: Boolean = false,
            val hideQsBarMediaPlayer: Boolean = false,
            val hideQsBarNearbyDevicesAndDeviceControl: Boolean = false,
            val hideQsBarSecurityFooter: Boolean = false,
            val hideQsBarDataUsage: Boolean = false,
            val hideQsBarSmartViewAndModes: Boolean = false,
            val alwaysExpandQsTileChunk: Boolean = false,
            val alwaysShowTimeDateOnQs: Boolean = false,
            val addBrightnessProgressToQsBar: Boolean = false,
            val addVolumeProgressToQsBar: Boolean = false,
            val showTraditionalChineseDateOnQS: Boolean = false,
            val modifyQSClockTextSize: Boolean = false,
            val qsClockTextSize: Float = 32f,
        )

        @Serializable
        data class AOD(
            val hideAODStatusBar: Boolean = false,
            val aodLockSupportLunar: Boolean = false,
        )

        @Serializable
        data class Notification(
            val hideOngoingActivityMedia: Boolean = false,
            val hideOngoingActivityMediaPackages: String = "",
            val disableNotificationGrouping: Boolean = false,
            val autoExpandNotifications: Boolean = false
        )

        @Serializable
        data class Other(
            val disableScreenshotCaptureSound: Boolean = false,
            val customPowerMenu: Boolean = false,
            val powerMenuActions: List<PowerMenuAction> = PowerMenuAction.defaultPreferences()
        )
    }

    @Serializable
    data class Settings(
        val showForcePeakRefreshRatePreference: Boolean = true,
        val supportOutdoorMode: Boolean = false,
        val showMoreBatteryInfo: Boolean = true,
        val showPackageInfo: Boolean = true,
        val showWiFiLinkSpeed: Boolean = false,
        val supportAnyFont: Boolean = true,
        val supportAutoPowerOnOff: Boolean = false,
        val spoofPhoneStatusAsOfficial: Boolean = false,
    )

    @Serializable
    data class Call(
        val supportVoiceCallRecording: Boolean = true,
        val preferRecordingButton: Boolean = true,
        val showGeocodedLocationInRecentCall: Boolean = false,
        val isOpStyleCHN: Boolean = false,
        val supportCallAndTextOnOtherDevices: Boolean = false,
    )

    @Serializable
    data class Camera(
        val supportAllCameraMenu: Boolean = true,
        val disableCameraTemperatureCheck: Boolean = false,
    )

    @Serializable
    data class Other(
        val blockGalaxyStoreAds: Boolean = true,
        val makeAllUserAppsAvailable: Boolean = true,
        val setWeatherProviderCN: Boolean = false,
        val showMemoryUsageInRecents: Boolean = false,
        val recentsGridThreeRows: Boolean = false,
        val hideRecentsCloseAllButton: Boolean = false,
        val showMorePlaybackSpeeds: Boolean = false,
        val redirectCustomTab: Boolean = false,
        val supportAllGallerySettings: Boolean = true,
        val supportSharedAlbumsInHide: Boolean = false,
        val hideVideoEditorStudio: Boolean = false,
        val supportAllNotesFeatures: Boolean = true,
        val enableChineseHolidayDisplay: Boolean = false,
        val supportBlockMessage: Boolean = true,
        val setThemeTrialNeverExpired: Boolean = true,
        val spoofBrowserCountryCodeToUS: Boolean = false,
        val noAIWatermark: Boolean = true,
        val bypassHealthMonitorCountryCheck: Boolean = false,
        val useSPenGoogleTranslate: Boolean = false,
        val hideAppsSearchBar: Boolean = false,
        val removeShortcutBadge: Boolean = false,
        val bypassWatchPairingRegionCheck: Boolean = false,
        val watchPairingConnectionMode: Int = 0,
        val supplementChinaWearOsGms: Boolean = false,
    )

    companion object {
        const val FILE_NAME = "preference.json"
    }
}

val IgnoreUnknownKeysJson = Json {
    ignoreUnknownKeys = true
}
