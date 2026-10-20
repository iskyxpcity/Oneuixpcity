package com.oneuialoha.app.oneuixp

import android.os.Build
import com.oneuialoha.common.ONE_UI_VERSION
import com.oneuialoha.common.Package
import com.oneuialoha.common.PowerMenuAction
import com.oneuialoha.common.Preference
import com.oneuialoha.common.IgnoreUnknownKeysJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

enum class OneUIAlohaCategory(
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
    Video(Package.VIDEO, "Samsung Video"),
    WatchManager(Package.WATCH_MANAGER, "Galaxy Wearable"),
    Weather(Package.WEATHER, "Weather"),
}

enum class OneUIAlohaSettingType {
    Toggle,
    IntValue,
    FloatValue,
    TextValue,
    ChoiceInt,
    ChoiceText,
    PowerAction,
}

sealed interface OneUIAlohaVisibility {
    data object Always : OneUIAlohaVisibility
    data class Bool(val path: String, val expected: Boolean = true) : OneUIAlohaVisibility
    data class IntEquals(val path: String, val value: Int) : OneUIAlohaVisibility
    data class StringContains(val path: String, val token: String) : OneUIAlohaVisibility
    data class All(val rules: List<OneUIAlohaVisibility>) : OneUIAlohaVisibility
    data class Not(val rule: OneUIAlohaVisibility) : OneUIAlohaVisibility
    data class AndroidAtLeast(val sdk: Int) : OneUIAlohaVisibility
    data class AndroidAtMost(val sdk: Int) : OneUIAlohaVisibility
    data class AndroidEquals(val sdk: Int) : OneUIAlohaVisibility
    data class OneUiAtLeast(val version: Int) : OneUIAlohaVisibility
    data class OneUiBelow(val version: Int) : OneUIAlohaVisibility
    data class ModelPrefix(val prefix: String) : OneUIAlohaVisibility
}

data class OneUIAlohaChoice(
    val value: String,
    val label: String,
)

data class OneUIAlohaSettingSpec(
    val path: String,
    val type: OneUIAlohaSettingType,
    val section: String,
    val titleOverride: String? = null,
    val summary: String? = null,
    val choices: List<OneUIAlohaChoice> = emptyList(),
    val powerActionName: String? = null,
    val visibility: OneUIAlohaVisibility = OneUIAlohaVisibility.Always,
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
    paths.map { OneUIAlohaSettingSpec(it, OneUIAlohaSettingType.Toggle, section) }

private fun ints(section: String, vararg paths: String) =
    paths.map { OneUIAlohaSettingSpec(it, OneUIAlohaSettingType.IntValue, section) }

private fun floats(section: String, vararg paths: String) =
    paths.map { OneUIAlohaSettingSpec(it, OneUIAlohaSettingType.FloatValue, section) }

private fun texts(section: String, vararg paths: String) =
    paths.map { OneUIAlohaSettingSpec(it, OneUIAlohaSettingType.TextValue, section) }

private fun choiceInt(
    section: String,
    path: String,
    title: String? = null,
    vararg choices: Pair<Int, String>,
) = OneUIAlohaSettingSpec(
    path = path,
    type = OneUIAlohaSettingType.ChoiceInt,
    section = section,
    titleOverride = title,
    choices = choices.map { OneUIAlohaChoice(it.first.toString(), it.second) },
)

private fun choiceText(
    section: String,
    path: String,
    title: String? = null,
    vararg choices: Pair<String, String>,
) = OneUIAlohaSettingSpec(
    path = path,
    type = OneUIAlohaSettingType.ChoiceText,
    section = section,
    titleOverride = title,
    choices = choices.map { OneUIAlohaChoice(it.first, it.second) },
)

private fun powerAction(name: String, label: String) = OneUIAlohaSettingSpec(
    path = "systemUI.other.powerMenuActions.$name",
    type = OneUIAlohaSettingType.PowerAction,
    section = "Power menu actions",
    titleOverride = label,
    powerActionName = name,
)

object OneUIAlohaFeatureCatalog {
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

    private val byCategory: Map<OneUIAlohaCategory, List<OneUIAlohaSettingSpec>> = mapOf(
        OneUIAlohaCategory.Android to android,
        OneUIAlohaCategory.SystemUI to (
            systemUiStatusBar + systemUiQs + systemUiAod + systemUiNotification + systemUiOther
        ),
        OneUIAlohaCategory.Settings to settings,
        OneUIAlohaCategory.Call to call,
        OneUIAlohaCategory.Camera to camera,
        OneUIAlohaCategory.Browser to toggles(
            "Samsung Internet",
            "other.showMorePlaybackSpeeds",
            "other.redirectCustomTab",
            "other.spoofBrowserCountryCodeToUS",
        ),
        OneUIAlohaCategory.Calendar to toggles(
            "Calendar",
            "other.enableChineseHolidayDisplay",
        ),
        OneUIAlohaCategory.DualApp to toggles(
            "Dual Messenger",
            "other.makeAllUserAppsAvailable",
        ),
        OneUIAlohaCategory.Gallery to toggles(
            "Gallery",
            "other.supportAllGallerySettings",
            "other.supportSharedAlbumsInHide",
            "other.hideVideoEditorStudio",
        ),
        OneUIAlohaCategory.GalaxyStore to toggles(
            "Galaxy Store",
            "other.blockGalaxyStoreAds",
        ),
        OneUIAlohaCategory.HealthMonitor to toggles(
            "Samsung Health Monitor",
            "other.bypassHealthMonitorCountryCheck",
        ),
        OneUIAlohaCategory.Launcher to toggles(
            "One UI Home",
            "other.showMemoryUsageInRecents",
            "other.recentsGridThreeRows",
            "other.hideRecentsCloseAllButton",
            "other.hideAppsSearchBar",
            "other.removeShortcutBadge",
        ),
        OneUIAlohaCategory.Messaging to toggles(
            "Messages",
            "other.supportBlockMessage",
        ),
        OneUIAlohaCategory.Notes to toggles(
            "Samsung Notes",
            "other.supportAllNotesFeatures",
        ),
        OneUIAlohaCategory.PhotoRetouching to toggles(
            "Photo Editor",
            "other.noAIWatermark",
        ),
        OneUIAlohaCategory.SketchBook to toggles(
            "SketchBook",
            "other.noAIWatermark",
        ),
        OneUIAlohaCategory.SPen to toggles(
            "S Pen",
            "other.useSPenGoogleTranslate",
        ),
        OneUIAlohaCategory.ThemeCenter to toggles(
            "Galaxy Themes",
            "other.setThemeTrialNeverExpired",
        ),
        OneUIAlohaCategory.Video to toggles(
            "Samsung Video",
            "other.showMorePlaybackSpeeds",
        ),
        OneUIAlohaCategory.WatchManager to (
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
        OneUIAlohaCategory.Weather to toggles(
            "Weather",
            "other.setWeatherProviderCN",
        ),
    )

    private val visibilityByPath: Map<String, OneUIAlohaVisibility> = mapOf(
        "android.maxNeverKilledAppNum" to OneUIAlohaVisibility.Bool("android.modifyMaxNeverKilledAppNum"),
        "android.supportAppJumpBlock" to OneUIAlohaVisibility.AndroidAtLeast(35),

        "systemUI.statusBar.statusBarLeftPaddingDp" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.modifyStatusBarLeftPadding"),
        "systemUI.statusBar.statusBarRightPaddingDp" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.modifyStatusBarRightPadding"),
        "systemUI.statusBar.statusBarTopPaddingDp" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.modifyStatusBarTopPadding"),
        "systemUI.statusBar.statusBarBottomPaddingDp" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.modifyStatusBarBottomPadding"),
        "systemUI.statusBar.batteryIconWidthScale" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.setBatteryIconWidthScale"),
        "systemUI.statusBar.batteryIconHeightScale" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.setBatteryIconHeightScale"),
        "systemUI.statusBar.hideBatteryPercentageSign" to
            OneUIAlohaVisibility.AndroidAtMost(34),
        "systemUI.statusBar.addBatteryLevelText" to
            OneUIAlohaVisibility.OneUiAtLeast(70000),
        "systemUI.statusBar.hideBatteryLevelTextPercentageSign" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.OneUiAtLeast(70000),
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.addBatteryLevelText"),
                )
            ),
        "systemUI.statusBar.hideBatteryLevelTextChargingIcon" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.OneUiAtLeast(70000),
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.addBatteryLevelText"),
                )
            ),
        "systemUI.statusBar.statusBarClockFormat" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
        "systemUI.statusBar.statusBarDoubleLineClockSize" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneUIAlohaVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneUIAlohaVisibility.Not(OneUIAlohaVisibility.ModelPrefix("SM-F966")),
                )
            ),
        "systemUI.statusBar.useFold7CustomDoubleLineClockScale" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneUIAlohaVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneUIAlohaVisibility.ModelPrefix("SM-F966"),
                )
            ),
        "systemUI.statusBar.fold7DoubleLineClockTimeScale" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneUIAlohaVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneUIAlohaVisibility.ModelPrefix("SM-F966"),
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.useFold7CustomDoubleLineClockScale"),
                )
            ),
        "systemUI.statusBar.fold7DoubleLineClockDateScale" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneUIAlohaVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneUIAlohaVisibility.ModelPrefix("SM-F966"),
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.useFold7CustomDoubleLineClockScale"),
                )
            ),
        "systemUI.statusBar.modifyDoubleLineClockGap" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneUIAlohaVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                )
            ),
        "systemUI.statusBar.doubleLineClockGapDp" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneUIAlohaVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.modifyDoubleLineClockGap"),
                )
            ),
        "systemUI.statusBar.setStatusBarClockTextScale" to
            OneUIAlohaVisibility.Not(
                OneUIAlohaVisibility.All(
                    listOf(
                        OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                        OneUIAlohaVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    )
                )
            ),
        "systemUI.statusBar.statusBarClockTextScale" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockTextScale"),
                    OneUIAlohaVisibility.Not(
                        OneUIAlohaVisibility.All(
                            listOf(
                                OneUIAlohaVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                                OneUIAlohaVisibility.StringContains(
                                    "systemUI.statusBar.statusBarClockFormat",
                                    "\n",
                                ),
                            )
                        )
                    ),
                )
            ),
        "systemUI.statusBar.physicalEsimAdapterSimSlot" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.physicalEsimAdapterWorkaround"),
        "systemUI.statusBar.statusBarMaxNotificationIcons" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.modifyStatusBarMaxNotificationIcons"),
        "systemUI.statusBar.customCarrierName" to
            OneUIAlohaVisibility.Bool("systemUI.statusBar.setCustomCarrierName"),

        "systemUI.qs.hideDeviceControlQsTile" to OneUIAlohaVisibility.AndroidEquals(34),
        "systemUI.qs.hideSmartViewQsTile" to OneUIAlohaVisibility.AndroidEquals(34),
        "systemUI.qs.turnOn5gQsTile" to OneUIAlohaVisibility.AndroidEquals(34),
        "systemUI.qs.hideQsBarMediaPlayer" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.AndroidAtLeast(35),
                    OneUIAlohaVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.hideQsBarNearbyDevicesAndDeviceControl" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.AndroidAtLeast(35),
                    OneUIAlohaVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.hideQsBarSecurityFooter" to OneUIAlohaVisibility.AndroidAtLeast(35),
        "systemUI.qs.hideQsBarDataUsage" to OneUIAlohaVisibility.AndroidAtLeast(35),
        "systemUI.qs.hideQsBarSmartViewAndModes" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.AndroidAtLeast(35),
                    OneUIAlohaVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.alwaysExpandQsTileChunk" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.AndroidAtLeast(35),
                    OneUIAlohaVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.alwaysShowTimeDateOnQs" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.AndroidAtLeast(35),
                    OneUIAlohaVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.addBrightnessProgressToQsBar" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.AndroidAtLeast(35),
                    OneUIAlohaVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.addVolumeProgressToQsBar" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.AndroidAtLeast(35),
                    OneUIAlohaVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.showTraditionalChineseDateOnQS" to
            OneUIAlohaVisibility.AndroidAtLeast(35),
        "systemUI.qs.modifyQSClockTextSize" to
            OneUIAlohaVisibility.AndroidAtLeast(35),
        "systemUI.qs.qsClockTextSize" to
            OneUIAlohaVisibility.All(
                listOf(
                    OneUIAlohaVisibility.AndroidAtLeast(35),
                    OneUIAlohaVisibility.Bool("systemUI.qs.modifyQSClockTextSize"),
                )
            ),
        "systemUI.aod.hideAODStatusBar" to OneUIAlohaVisibility.AndroidAtLeast(35),
        "systemUI.aod.aodLockSupportLunar" to OneUIAlohaVisibility.AndroidAtLeast(35),

        "systemUI.notification.hideOngoingActivityMediaPackages" to
            OneUIAlohaVisibility.Bool("systemUI.notification.hideOngoingActivityMedia"),

        "other.showMemoryUsageInRecents" to OneUIAlohaVisibility.AndroidAtLeast(35),
        "other.supplementChinaWearOsGms" to
            OneUIAlohaVisibility.IntEquals("other.watchPairingConnectionMode", 1),
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
        rule: OneUIAlohaVisibility,
        preference: Preference,
    ): Boolean = when (rule) {
        OneUIAlohaVisibility.Always -> true
        is OneUIAlohaVisibility.Bool ->
            preferenceValue(preference, rule.path).toBooleanStrictOrNull() == rule.expected
        is OneUIAlohaVisibility.IntEquals ->
            preferenceValue(preference, rule.path).toIntOrNull() == rule.value
        is OneUIAlohaVisibility.StringContains ->
            preferenceValue(preference, rule.path).contains(rule.token)
        is OneUIAlohaVisibility.All ->
            rule.rules.all { visibilityMatches(it, preference) }
        is OneUIAlohaVisibility.Not ->
            !visibilityMatches(rule.rule, preference)
        is OneUIAlohaVisibility.AndroidAtLeast -> Build.VERSION.SDK_INT >= rule.sdk
        is OneUIAlohaVisibility.AndroidAtMost -> Build.VERSION.SDK_INT <= rule.sdk
        is OneUIAlohaVisibility.AndroidEquals -> Build.VERSION.SDK_INT == rule.sdk
        is OneUIAlohaVisibility.OneUiAtLeast -> ONE_UI_VERSION >= rule.version
        is OneUIAlohaVisibility.OneUiBelow -> ONE_UI_VERSION < rule.version
        is OneUIAlohaVisibility.ModelPrefix ->
            Build.MODEL.startsWith(rule.prefix, ignoreCase = true)
    }

    /**
     * Number of enabled OneUIAloha feature injections represented by the feature catalog.
     *
     * Count category entries rather than unique preference paths: a shared feature that
     * injects into two target apps is two actual injection targets. Child value fields and
     * power-menu action rows are not counted separately.
     */
    fun enabledInjectionCount(preference: Preference): Int {
        val catalogCount = OneUIAlohaCategory.entries.sumOf { category ->
            settings(category).count { spec ->
                if (!visibilityMatches(spec.visibility, preference)) {
                    false
                } else {
                    when (spec.type) {
                        OneUIAlohaSettingType.Toggle ->
                            preferenceValue(preference, spec.path).toBooleanStrictOrNull() == true
                        OneUIAlohaSettingType.ChoiceInt ->
                            spec.path == "other.watchPairingConnectionMode" &&
                                preferenceValue(preference, spec.path).toIntOrNull() != 0
                        else -> false
                    }
                }
            }
        }

        // Include OneUIAloha's newer standalone runtime features alongside the legacy
        // OneUIAloha catalog. Count only each feature's master enable switch; appearance,
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

        return catalogCount + runtimeFeatureCount
    }

    fun settings(category: OneUIAlohaCategory): List<OneUIAlohaSettingSpec> =
        byCategory[category].orEmpty().map { spec ->
            val rule = when {
                spec.type == OneUIAlohaSettingType.PowerAction ->
                    OneUIAlohaVisibility.Bool("systemUI.other.customPowerMenu")
                else -> visibilityByPath[spec.path] ?: OneUIAlohaVisibility.Always
            }
            spec.copy(visibility = rule)
        }
}
