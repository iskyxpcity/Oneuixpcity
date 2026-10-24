package com.alohaoneui.app.alohaoneui

import android.os.Build
import com.alohaoneui.common.ONE_UI_VERSION
import com.alohaoneui.common.Package
import com.alohaoneui.common.PowerMenuAction
import com.alohaoneui.common.Preference
import com.alohaoneui.common.IgnoreUnknownKeysJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

enum class AlohaOneUICategory(
    val packageName: String,
    val fallbackTitle: String,
) {
    Android(Package.ANDROID, "Android"),
    SystemUI(Package.SYSTEMUI, "System UI"),
    Settings(Package.SETTINGS, "Settings"),
    Call(Package.DIALER, "Phone"),
    Camera(Package.CAMERA, "Camera"),
    Browser(Package.BROWSER, "Samsung Internet"),
    Calendar(Package.CALENDAR, "Calendar"),
    DualApp(Package.DUAL_APP, "Dual Messenger"),
    Gallery(Package.GALLERY, "Gallery"),
    GalaxyStore(Package.STORE, "Galaxy Store"),
    HealthMonitor(Package.HEALTH_MONITOR, "Samsung Health Monitor"),
    Launcher(Package.LAUNCHER, "One UI Home"),
    Messaging(Package.MESSAGING, "Messages"),
    Notes(Package.NOTES, "Samsung Notes"),
    PhotoRetouching(Package.PHOTO_RETOUCHING, "Photo Editor"),
    SketchBook(Package.SKETCH_BOOK, "SketchBook"),
    SPen(Package.TRANSLATION, "S Pen translation"),
    ThemeCenter(Package.THEME_CENTER, "Galaxy Themes"),
    ThemeStoreService(Package.THEME_STORE, "主题商店服务"),
    Video(Package.VIDEO, "Samsung Video"),
    WatchManager(Package.WATCH_MANAGER, "Galaxy Wearable"),
    Weather(Package.WEATHER, "Weather"),
}

enum class AlohaOneUISettingType {
    Toggle,
    IntValue,
    FloatValue,
    TextValue,
    ChoiceInt,
    ChoiceText,
    PowerAction,
}

sealed interface AlohaOneUIVisibility {
    data object Always : AlohaOneUIVisibility
    data class Bool(val path: String, val expected: Boolean = true) : AlohaOneUIVisibility
    data class IntEquals(val path: String, val value: Int) : AlohaOneUIVisibility
    data class StringContains(val path: String, val token: String) : AlohaOneUIVisibility
    data class All(val rules: List<AlohaOneUIVisibility>) : AlohaOneUIVisibility
    data class Not(val rule: AlohaOneUIVisibility) : AlohaOneUIVisibility
    data class AndroidAtLeast(val sdk: Int) : AlohaOneUIVisibility
    data class AndroidAtMost(val sdk: Int) : AlohaOneUIVisibility
    data class AndroidEquals(val sdk: Int) : AlohaOneUIVisibility
    data class OneUiAtLeast(val version: Int) : AlohaOneUIVisibility
    data class OneUiBelow(val version: Int) : AlohaOneUIVisibility
    data class ModelPrefix(val prefix: String) : AlohaOneUIVisibility
}

data class AlohaOneUIChoice(
    val value: String,
    val label: String,
)

data class AlohaOneUISettingSpec(
    val path: String,
    val type: AlohaOneUISettingType,
    val section: String,
    val titleOverride: String? = null,
    val summary: String? = null,
    val choices: List<AlohaOneUIChoice> = emptyList(),
    val powerActionName: String? = null,
    val visibility: AlohaOneUIVisibility = AlohaOneUIVisibility.Always,
) {
    val title: String
        get() = titleOverride ?: humanizeSettingName(path.substringAfterLast('.'))
}

private fun humanizeSettingName(raw: String): String {
    val spaced = raw
        .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
        .replace(Regex("(?<=[A-Z])(?=[A-Z][a-z])"), " ")
        .replace("Qs", "QS")
        .replace("Aod", "AOD")
        .replace("Ui", "UI")
        .replace("Wifi", "Wi-Fi")
        .replace("Fcm", "FCM")
        .replace("Esim", "eSIM")
        .replace("Ai", "AI")
    return spaced.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}

private fun toggles(section: String, vararg paths: String) =
    paths.map { AlohaOneUISettingSpec(it, AlohaOneUISettingType.Toggle, section) }

private fun ints(section: String, vararg paths: String) =
    paths.map { AlohaOneUISettingSpec(it, AlohaOneUISettingType.IntValue, section) }

private fun floats(section: String, vararg paths: String) =
    paths.map { AlohaOneUISettingSpec(it, AlohaOneUISettingType.FloatValue, section) }

private fun texts(section: String, vararg paths: String) =
    paths.map { AlohaOneUISettingSpec(it, AlohaOneUISettingType.TextValue, section) }

private fun choiceInt(
    section: String,
    path: String,
    title: String? = null,
    vararg choices: Pair<Int, String>,
) = AlohaOneUISettingSpec(
    path = path,
    type = AlohaOneUISettingType.ChoiceInt,
    section = section,
    titleOverride = title,
    choices = choices.map { AlohaOneUIChoice(it.first.toString(), it.second) },
)

private fun choiceText(
    section: String,
    path: String,
    title: String? = null,
    vararg choices: Pair<String, String>,
) = AlohaOneUISettingSpec(
    path = path,
    type = AlohaOneUISettingType.ChoiceText,
    section = section,
    titleOverride = title,
    choices = choices.map { AlohaOneUIChoice(it.first, it.second) },
)

private fun powerAction(name: String, label: String) = AlohaOneUISettingSpec(
    path = "systemUI.other.powerMenuActions.$name",
    type = AlohaOneUISettingType.PowerAction,
    section = "Power menu actions",
    titleOverride = label,
    powerActionName = name,
)

object AlohaOneUIFeatureCatalog {
    // Keep the feature counter on the exact same preference semantics as the
    // Features UI. Defaults must be encoded, otherwise default-enabled switches
    // disappear from JSON and are incorrectly treated as disabled.
    private val preferenceValueJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val android =
        toggles(
            "Android",
            "android.disablePinVerifyPer72h",
            "android.modifyMaxNeverKilledAppNum",
            "android.setBlockableNotificationChannel",
            "android.supportAppJumpBlock",
            "android.allowAllRotation",
            "android.liftFcmNetworkLimit",
            "android.disableScreenWakeOnPowerUnplugged",
            "android.disableWritingToolkitGlobally",
        ) + ints("Android", "android.maxNeverKilledAppNum")

    private val systemUiStatusBar =
        toggles(
            "Status bar",
            "systemUI.statusBar.modifyStatusBarLeftPadding",
            "systemUI.statusBar.modifyStatusBarRightPadding",
            "systemUI.statusBar.modifyStatusBarTopPadding",
            "systemUI.statusBar.modifyStatusBarBottomPadding",
            "systemUI.statusBar.setBatteryIconWidthScale",
            "systemUI.statusBar.setBatteryIconHeightScale",
            "systemUI.statusBar.hideBatteryPercentageSign",
            "systemUI.statusBar.hideBatteryIcon",
            "systemUI.statusBar.addBatteryLevelText",
            "systemUI.statusBar.hideBatteryLevelTextPercentageSign",
            "systemUI.statusBar.hideBatteryLevelTextChargingIcon",
            "systemUI.statusBar.supportRealTimeNetworkSpeed",
            "systemUI.statusBar.showSeparateUpDownNetworkSpeeds",
            "systemUI.statusBar.setStatusBarClockFormat",
            "systemUI.statusBar.modifyDoubleLineClockGap",
            "systemUI.statusBar.setStatusBarClockTextScale",
            "systemUI.statusBar.useFold7CustomDoubleLineClockScale",
            "systemUI.statusBar.updateStatusBarClockEverySecond",
            "systemUI.statusBar.hideSecureFolderStatusBarIcon",
            "systemUI.statusBar.physicalEsimAdapterWorkaround",
            "systemUI.statusBar.doubleTapStatusBarToSleep",
            "systemUI.statusBar.modifyStatusBarMaxNotificationIcons",
            "systemUI.statusBar.setCustomCarrierName",
            "systemUI.statusBar.hideLockscreenStatusBar",
            "systemUI.statusBar.restoreBluetoothStatusBarIcon",
        ) +
            floats(
                "Status bar values",
                "systemUI.statusBar.statusBarLeftPaddingDp",
                "systemUI.statusBar.statusBarRightPaddingDp",
                "systemUI.statusBar.statusBarTopPaddingDp",
                "systemUI.statusBar.statusBarBottomPaddingDp",
                "systemUI.statusBar.batteryIconWidthScale",
                "systemUI.statusBar.batteryIconHeightScale",
                "systemUI.statusBar.statusBarClockTextScale",
                "systemUI.statusBar.doubleLineClockGapDp",
                "systemUI.statusBar.fold7DoubleLineClockTimeScale",
                "systemUI.statusBar.fold7DoubleLineClockDateScale",
            ) +
            ints(
                "Status bar values",
                "systemUI.statusBar.networkSpeedThresholdKb",
                "systemUI.statusBar.statusBarMaxNotificationIcons",
            ) +
            texts(
                "Status bar values",
                "systemUI.statusBar.statusBarClockFormat",
                "systemUI.statusBar.customCarrierName",
            ) +
            listOf(
                choiceText(
                    "Status bar values",
                    "systemUI.statusBar.statusBarDoubleLineClockSize",
                    "Double-line clock size",
                    "small" to "Small",
                    "compact" to "Compact",
                    "standard" to "Standard",
                    "large" to "Large",
                    "extra_large" to "Extra large",
                ),
                choiceInt(
                    "Status bar values",
                    "systemUI.statusBar.physicalEsimAdapterSimSlot",
                    "Physical eSIM adapter SIM slot",
                    0 to "SIM 1",
                    1 to "SIM 2",
                    2 to "Both",
                ),
            )

    private val systemUiQs =
        toggles(
            "Quick settings",
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
        ) + floats("Quick settings", "systemUI.qs.qsClockTextSize")

    private val systemUiAod =
        toggles(
            "Always On Display",
            "systemUI.aod.hideAODStatusBar",
            "systemUI.aod.aodLockSupportLunar",
        )

    private val systemUiNotification =
        toggles(
            "Notifications",
            "systemUI.notification.hideOngoingActivityMedia",
            "systemUI.notification.disableNotificationGrouping",
            "systemUI.notification.autoExpandNotifications",
        ) + texts("Notifications", "systemUI.notification.hideOngoingActivityMediaPackages")

    private val systemUiOther =
        toggles(
            "Other System UI",
            "systemUI.other.disableScreenshotCaptureSound",
            "systemUI.other.customPowerMenu",
        ) +
            listOf(
                powerAction(PowerMenuAction.POWER, "Power off"),
                powerAction(PowerMenuAction.DATA_MODE, "Mobile data"),
                powerAction(PowerMenuAction.RESTART, "Restart"),
                powerAction(PowerMenuAction.SAFE_MODE, "Safe mode"),
                powerAction(PowerMenuAction.LOCK_DOWN_MODE, "Lockdown"),
                powerAction(PowerMenuAction.EMERGENCY_CALL, "Emergency call"),
                powerAction(PowerMenuAction.MEDICAL_INFO, "Medical info"),
                powerAction(PowerMenuAction.SIDE_KEY_SETTINGS, "Side key settings"),
                powerAction(PowerMenuAction.FORCE_RESTART_MESSAGE, "Force restart message"),
                powerAction(PowerMenuAction.RESTART_SYSTEMUI, "Restart SystemUI"),
                powerAction(PowerMenuAction.RESTART_RECOVERY, "Restart recovery"),
                powerAction(PowerMenuAction.RESTART_DOWNLOAD, "Restart download"),
            )

    private val settings = toggles(
        "Settings",
        "settings.showForcePeakRefreshRatePreference",
        "settings.supportOutdoorMode",
        "settings.showMoreBatteryInfo",
        "settings.showPackageInfo",
        "settings.showWiFiLinkSpeed",
        "settings.supportAnyFont",
        "settings.supportAutoPowerOnOff",
        "settings.spoofPhoneStatusAsOfficial",
    )

    private val call = toggles(
        "Call",
        "call.supportVoiceCallRecording",
        "call.preferRecordingButton",
        "call.showGeocodedLocationInRecentCall",
        "call.isOpStyleCHN",
        "call.supportCallAndTextOnOtherDevices",
    )

    private val camera = toggles(
        "Camera",
        "camera.supportAllCameraMenu",
        "camera.disableCameraTemperatureCheck",
    )

    private val byCategory: Map<AlohaOneUICategory, List<AlohaOneUISettingSpec>> = mapOf(
        AlohaOneUICategory.Android to android,
        AlohaOneUICategory.SystemUI to (
            systemUiStatusBar + systemUiQs + systemUiAod + systemUiNotification + systemUiOther
        ),
        AlohaOneUICategory.Settings to settings,
        AlohaOneUICategory.Call to call,
        AlohaOneUICategory.Camera to camera,
        AlohaOneUICategory.Browser to toggles(
            "Samsung Internet",
            "other.showMorePlaybackSpeeds",
            "other.redirectCustomTab",
            "other.spoofBrowserCountryCodeToUS",
        ),
        AlohaOneUICategory.Calendar to toggles(
            "Calendar",
            "other.enableChineseHolidayDisplay",
        ),
        AlohaOneUICategory.DualApp to toggles(
            "Dual Messenger",
            "other.makeAllUserAppsAvailable",
        ),
        AlohaOneUICategory.Gallery to toggles(
            "Gallery",
            "other.supportAllGallerySettings",
            "other.supportSharedAlbumsInHide",
            "other.hideVideoEditorStudio",
        ),
        AlohaOneUICategory.GalaxyStore to toggles(
            "Galaxy Store",
            "other.blockGalaxyStoreAds",
        ),
        AlohaOneUICategory.HealthMonitor to toggles(
            "Samsung Health Monitor",
            "other.bypassHealthMonitorCountryCheck",
        ),
        AlohaOneUICategory.Launcher to toggles(
            "One UI Home",
            "other.showMemoryUsageInRecents",
            "other.recentsGridThreeRows",
            "other.hideRecentsCloseAllButton",
            "other.hideAppsSearchBar",
            "other.removeShortcutBadge",
        ),
        AlohaOneUICategory.Messaging to toggles(
            "Messages",
            "other.supportBlockMessage",
        ),
        AlohaOneUICategory.Notes to toggles(
            "Samsung Notes",
            "other.supportAllNotesFeatures",
        ),
        AlohaOneUICategory.PhotoRetouching to toggles(
            "Photo Editor",
            "other.noAIWatermark",
        ),
        AlohaOneUICategory.SketchBook to toggles(
            "SketchBook",
            "other.noAIWatermark",
        ),
        AlohaOneUICategory.SPen to toggles(
            "S Pen",
            "other.useSPenGoogleTranslate",
        ),
        AlohaOneUICategory.ThemeCenter to toggles(
            "Galaxy Themes",
            "other.setThemeTrialNeverExpired",
        ),
        AlohaOneUICategory.ThemeStoreService to toggles(
            "Theme Store Service",
            "other.setThemeStoreRegion",
        ),
        AlohaOneUICategory.Video to toggles(
            "Samsung Video",
            "other.showMorePlaybackSpeeds",
        ),
        AlohaOneUICategory.WatchManager to (
            toggles(
                "Galaxy Wearable",
                "other.bypassWatchPairingRegionCheck",
                "other.supplementChinaWearOsGms",
            ) + listOf(
                choiceInt(
                    "Galaxy Wearable",
                    "other.watchPairingConnectionMode",
                    "Watch pairing connection mode",
                    0 to "Default",
                    1 to "Wear OS China",
                    2 to "Wear OS Global",
                )
            )
        ),
        AlohaOneUICategory.Weather to toggles(
            "Weather",
            "other.setWeatherProviderCN",
        ),
    )

    private val visibilityByPath: Map<String, AlohaOneUIVisibility> = mapOf(
        "android.maxNeverKilledAppNum" to AlohaOneUIVisibility.Bool("android.modifyMaxNeverKilledAppNum"),
        "android.supportAppJumpBlock" to AlohaOneUIVisibility.AndroidAtLeast(35),

        "systemUI.statusBar.statusBarLeftPaddingDp" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.modifyStatusBarLeftPadding"),
        "systemUI.statusBar.statusBarRightPaddingDp" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.modifyStatusBarRightPadding"),
        "systemUI.statusBar.statusBarTopPaddingDp" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.modifyStatusBarTopPadding"),
        "systemUI.statusBar.statusBarBottomPaddingDp" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.modifyStatusBarBottomPadding"),
        "systemUI.statusBar.batteryIconWidthScale" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.setBatteryIconWidthScale"),
        "systemUI.statusBar.batteryIconHeightScale" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.setBatteryIconHeightScale"),
        "systemUI.statusBar.hideBatteryPercentageSign" to
            AlohaOneUIVisibility.AndroidAtMost(34),
        "systemUI.statusBar.addBatteryLevelText" to
            AlohaOneUIVisibility.OneUiAtLeast(70000),
        "systemUI.statusBar.hideBatteryLevelTextPercentageSign" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.OneUiAtLeast(70000),
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.addBatteryLevelText"),
                )
            ),
        "systemUI.statusBar.hideBatteryLevelTextChargingIcon" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.OneUiAtLeast(70000),
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.addBatteryLevelText"),
                )
            ),
        "systemUI.statusBar.statusBarClockFormat" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
        "systemUI.statusBar.statusBarDoubleLineClockSize" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    AlohaOneUIVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    AlohaOneUIVisibility.Not(AlohaOneUIVisibility.ModelPrefix("SM-F966")),
                )
            ),
        "systemUI.statusBar.useFold7CustomDoubleLineClockScale" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    AlohaOneUIVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    AlohaOneUIVisibility.ModelPrefix("SM-F966"),
                )
            ),
        "systemUI.statusBar.fold7DoubleLineClockTimeScale" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    AlohaOneUIVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    AlohaOneUIVisibility.ModelPrefix("SM-F966"),
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.useFold7CustomDoubleLineClockScale"),
                )
            ),
        "systemUI.statusBar.fold7DoubleLineClockDateScale" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    AlohaOneUIVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    AlohaOneUIVisibility.ModelPrefix("SM-F966"),
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.useFold7CustomDoubleLineClockScale"),
                )
            ),
        "systemUI.statusBar.modifyDoubleLineClockGap" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    AlohaOneUIVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                )
            ),
        "systemUI.statusBar.doubleLineClockGapDp" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    AlohaOneUIVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.modifyDoubleLineClockGap"),
                )
            ),
        "systemUI.statusBar.setStatusBarClockTextScale" to
            AlohaOneUIVisibility.Not(
                AlohaOneUIVisibility.All(
                    listOf(
                        AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                        AlohaOneUIVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    )
                )
            ),
        "systemUI.statusBar.statusBarClockTextScale" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockTextScale"),
                    AlohaOneUIVisibility.Not(
                        AlohaOneUIVisibility.All(
                            listOf(
                                AlohaOneUIVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                                AlohaOneUIVisibility.StringContains(
                                    "systemUI.statusBar.statusBarClockFormat",
                                    "\n",
                                ),
                            )
                        )
                    ),
                )
            ),
        "systemUI.statusBar.physicalEsimAdapterSimSlot" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.physicalEsimAdapterWorkaround"),
        "systemUI.statusBar.statusBarMaxNotificationIcons" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.modifyStatusBarMaxNotificationIcons"),
        "systemUI.statusBar.customCarrierName" to
            AlohaOneUIVisibility.Bool("systemUI.statusBar.setCustomCarrierName"),

        "systemUI.qs.hideDeviceControlQsTile" to AlohaOneUIVisibility.AndroidEquals(34),
        "systemUI.qs.hideSmartViewQsTile" to AlohaOneUIVisibility.AndroidEquals(34),
        "systemUI.qs.turnOn5gQsTile" to AlohaOneUIVisibility.AndroidEquals(34),
        "systemUI.qs.hideQsBarMediaPlayer" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.AndroidAtLeast(35),
                    AlohaOneUIVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.hideQsBarNearbyDevicesAndDeviceControl" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.AndroidAtLeast(35),
                    AlohaOneUIVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.hideQsBarSecurityFooter" to AlohaOneUIVisibility.AndroidAtLeast(35),
        "systemUI.qs.hideQsBarDataUsage" to AlohaOneUIVisibility.AndroidAtLeast(35),
        "systemUI.qs.hideQsBarSmartViewAndModes" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.AndroidAtLeast(35),
                    AlohaOneUIVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.alwaysExpandQsTileChunk" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.AndroidAtLeast(35),
                    AlohaOneUIVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.alwaysShowTimeDateOnQs" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.AndroidAtLeast(35),
                    AlohaOneUIVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.addBrightnessProgressToQsBar" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.AndroidAtLeast(35),
                    AlohaOneUIVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.addVolumeProgressToQsBar" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.AndroidAtLeast(35),
                    AlohaOneUIVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.showTraditionalChineseDateOnQS" to
            AlohaOneUIVisibility.AndroidAtLeast(35),
        "systemUI.qs.modifyQSClockTextSize" to
            AlohaOneUIVisibility.AndroidAtLeast(35),
        "systemUI.qs.qsClockTextSize" to
            AlohaOneUIVisibility.All(
                listOf(
                    AlohaOneUIVisibility.AndroidAtLeast(35),
                    AlohaOneUIVisibility.Bool("systemUI.qs.modifyQSClockTextSize"),
                )
            ),
        "systemUI.aod.hideAODStatusBar" to AlohaOneUIVisibility.AndroidAtLeast(35),
        "systemUI.aod.aodLockSupportLunar" to AlohaOneUIVisibility.AndroidAtLeast(35),

        "systemUI.notification.hideOngoingActivityMediaPackages" to
            AlohaOneUIVisibility.Bool("systemUI.notification.hideOngoingActivityMedia"),

        "other.showMemoryUsageInRecents" to AlohaOneUIVisibility.AndroidAtLeast(35),
        "other.supplementChinaWearOsGms" to
            AlohaOneUIVisibility.IntEquals("other.watchPairingConnectionMode", 1),
    )

    private fun preferenceValue(preference: Preference, path: String): String {
        var current: JsonElement = preferenceValueJson.encodeToJsonElement(
            Preference.serializer(),
            preference,
        )
        for (key in path.split('.')) {
            current = (current as? JsonObject)?.get(key) ?: return ""
        }
        return current.jsonPrimitive.content
    }

    private fun visibilityMatches(
        rule: AlohaOneUIVisibility,
        preference: Preference,
    ): Boolean = when (rule) {
        AlohaOneUIVisibility.Always -> true
        is AlohaOneUIVisibility.Bool ->
            preferenceValue(preference, rule.path).toBooleanStrictOrNull() == rule.expected
        is AlohaOneUIVisibility.IntEquals ->
            preferenceValue(preference, rule.path).toIntOrNull() == rule.value
        is AlohaOneUIVisibility.StringContains ->
            preferenceValue(preference, rule.path).contains(rule.token)
        is AlohaOneUIVisibility.All ->
            rule.rules.all { visibilityMatches(it, preference) }
        is AlohaOneUIVisibility.Not ->
            !visibilityMatches(rule.rule, preference)
        is AlohaOneUIVisibility.AndroidAtLeast -> Build.VERSION.SDK_INT >= rule.sdk
        is AlohaOneUIVisibility.AndroidAtMost -> Build.VERSION.SDK_INT <= rule.sdk
        is AlohaOneUIVisibility.AndroidEquals -> Build.VERSION.SDK_INT == rule.sdk
        is AlohaOneUIVisibility.OneUiAtLeast -> ONE_UI_VERSION >= rule.version
        is AlohaOneUIVisibility.OneUiBelow -> ONE_UI_VERSION < rule.version
        is AlohaOneUIVisibility.ModelPrefix ->
            Build.MODEL.startsWith(rule.prefix, ignoreCase = true)
    }

    /**
     * Number of enabled AlohaOneUI feature injections represented by the feature catalog.
     *
     * Count category entries rather than unique preference paths: a shared feature that
     * injects into two target apps is two actual injection targets. Child value fields and
     * power-menu action rows are not counted separately.
     */
    fun enabledInjectionCount(preference: Preference): Int {
        val catalogCount = AlohaOneUICategory.entries.sumOf { category ->
            settings(category).count { spec ->
                if (!visibilityMatches(spec.visibility, preference)) {
                    false
                } else {
                    when (spec.type) {
                        AlohaOneUISettingType.Toggle ->
                            preferenceValue(preference, spec.path).toBooleanStrictOrNull() == true
                        AlohaOneUISettingType.ChoiceInt ->
                            spec.path == "other.watchPairingConnectionMode" &&
                                preferenceValue(preference, spec.path).toIntOrNull() != 0
                        else -> false
                    }
                }
            }
        }

        // Include AlohaOneUI's newer standalone runtime features alongside the legacy
        // AlohaOneUI catalog. Count only each feature's master enable switch; appearance,
        // size, color, spacing, and per-element settings are not separate injections.
        val statusBar = preference.systemUI.statusBar
        val runtimeFeatureCount = listOf(
            statusBar.powerRuntime.enabled,
            statusBar.comboStyleOneRuntime.enabled,
            statusBar.wifiRuntime.enabled,
            statusBar.bluetoothRuntime.enabled,
            statusBar.signalRuntime.enabled,
            statusBar.networkSpeedRuntime.enabled,
            statusBar.enableStatusBarClockRichStyle,
        ).count { it }

        // These five switches live on the legacy "Other functions" page rather than
        // AlohaOneUICategory. System UI's custom power menu and screenshot-sound switches
        // are already present in systemUiOther above, so do not count them twice.
        val other = preference.other
        val otherFunctionsCount = listOf(
            other.callReturnDelayEnabled,
            other.settingsTwoPaneEnabled,
            other.allowAppDowngradeEnabled,
            other.disableSecureScreenshotRestrictionEnabled,
            other.hidePackageInstallerRecommendationEnabled,
        ).count { it }

        // Count each independently enabled trigger group once. Individual action slots,
        // haptics, colors, sizing, and thresholds are configuration details, not features.
        val shortcuts = other.globalShortcutActions
        val globalShortcutCount = listOf(
            shortcuts.threeFingerGestureEnabled,
            shortcuts.threeFingerPinchEnabled,
            shortcuts.threeFingerSpreadEnabled,
            shortcuts.floatingBallEnabled,
            shortcuts.statusBarShortcutEnabled,
            shortcuts.navigationKeyShortcutEnabled,
            shortcuts.screenOffVolumeKeyEnabled,
        ).count { it }

        return catalogCount + runtimeFeatureCount + otherFunctionsCount + globalShortcutCount
    }

    fun settings(category: AlohaOneUICategory): List<AlohaOneUISettingSpec> =
        byCategory[category].orEmpty().map { spec ->
            val rule = when {
                spec.type == AlohaOneUISettingType.PowerAction ->
                    AlohaOneUIVisibility.Bool("systemUI.other.customPowerMenu")
                else -> visibilityByPath[spec.path] ?: AlohaOneUIVisibility.Always
            }
            spec.copy(visibility = rule)
        }
}
