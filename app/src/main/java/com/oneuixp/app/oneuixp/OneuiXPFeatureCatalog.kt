package com.oneuixp.app.oneuixp

import android.os.Build
import io.github.oneuixp.common.ONE_UI_VERSION
import io.github.oneuixp.common.Package
import io.github.oneuixp.common.PowerMenuAction
import io.github.oneuixp.common.Preference
import io.github.oneuixp.common.IgnoreUnknownKeysJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

enum class OneuiXPCategory(
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

enum class OneuiXPSettingType {
    Toggle,
    IntValue,
    FloatValue,
    TextValue,
    ChoiceInt,
    ChoiceText,
    PowerAction,
}

sealed interface OneuiXPVisibility {
    data object Always : OneuiXPVisibility
    data class Bool(val path: String, val expected: Boolean = true) : OneuiXPVisibility
    data class IntEquals(val path: String, val value: Int) : OneuiXPVisibility
    data class StringContains(val path: String, val token: String) : OneuiXPVisibility
    data class All(val rules: List<OneuiXPVisibility>) : OneuiXPVisibility
    data class Not(val rule: OneuiXPVisibility) : OneuiXPVisibility
    data class AndroidAtLeast(val sdk: Int) : OneuiXPVisibility
    data class AndroidAtMost(val sdk: Int) : OneuiXPVisibility
    data class AndroidEquals(val sdk: Int) : OneuiXPVisibility
    data class OneUiAtLeast(val version: Int) : OneuiXPVisibility
    data class OneUiBelow(val version: Int) : OneuiXPVisibility
    data class ModelPrefix(val prefix: String) : OneuiXPVisibility
}

data class OneuiXPChoice(
    val value: String,
    val label: String,
)

data class OneuiXPSettingSpec(
    val path: String,
    val type: OneuiXPSettingType,
    val section: String,
    val titleOverride: String? = null,
    val summary: String? = null,
    val choices: List<OneuiXPChoice> = emptyList(),
    val powerActionName: String? = null,
    val visibility: OneuiXPVisibility = OneuiXPVisibility.Always,
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
    paths.map { OneuiXPSettingSpec(it, OneuiXPSettingType.Toggle, section) }

private fun ints(section: String, vararg paths: String) =
    paths.map { OneuiXPSettingSpec(it, OneuiXPSettingType.IntValue, section) }

private fun floats(section: String, vararg paths: String) =
    paths.map { OneuiXPSettingSpec(it, OneuiXPSettingType.FloatValue, section) }

private fun texts(section: String, vararg paths: String) =
    paths.map { OneuiXPSettingSpec(it, OneuiXPSettingType.TextValue, section) }

private fun choiceInt(
    section: String,
    path: String,
    title: String? = null,
    vararg choices: Pair<Int, String>,
) = OneuiXPSettingSpec(
    path = path,
    type = OneuiXPSettingType.ChoiceInt,
    section = section,
    titleOverride = title,
    choices = choices.map { OneuiXPChoice(it.first.toString(), it.second) },
)

private fun choiceText(
    section: String,
    path: String,
    title: String? = null,
    vararg choices: Pair<String, String>,
) = OneuiXPSettingSpec(
    path = path,
    type = OneuiXPSettingType.ChoiceText,
    section = section,
    titleOverride = title,
    choices = choices.map { OneuiXPChoice(it.first, it.second) },
)

private fun powerAction(name: String, label: String) = OneuiXPSettingSpec(
    path = "systemUI.other.powerMenuActions.$name",
    type = OneuiXPSettingType.PowerAction,
    section = "Power menu actions",
    titleOverride = label,
    powerActionName = name,
)

object OneuiXPFeatureCatalog {
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

    private val byCategory: Map<OneuiXPCategory, List<OneuiXPSettingSpec>> = mapOf(
        OneuiXPCategory.Android to android,
        OneuiXPCategory.SystemUI to (
            systemUiStatusBar + systemUiQs + systemUiAod + systemUiNotification + systemUiOther
        ),
        OneuiXPCategory.Settings to settings,
        OneuiXPCategory.Call to call,
        OneuiXPCategory.Camera to camera,
        OneuiXPCategory.Browser to toggles(
            "Samsung Internet",
            "other.showMorePlaybackSpeeds",
            "other.redirectCustomTab",
            "other.spoofBrowserCountryCodeToUS",
        ),
        OneuiXPCategory.Calendar to toggles(
            "Calendar",
            "other.enableChineseHolidayDisplay",
        ),
        OneuiXPCategory.DualApp to toggles(
            "Dual Messenger",
            "other.makeAllUserAppsAvailable",
        ),
        OneuiXPCategory.Gallery to toggles(
            "Gallery",
            "other.supportAllGallerySettings",
            "other.supportSharedAlbumsInHide",
            "other.hideVideoEditorStudio",
        ),
        OneuiXPCategory.GalaxyStore to toggles(
            "Galaxy Store",
            "other.blockGalaxyStoreAds",
        ),
        OneuiXPCategory.HealthMonitor to toggles(
            "Samsung Health Monitor",
            "other.bypassHealthMonitorCountryCheck",
        ),
        OneuiXPCategory.Launcher to toggles(
            "One UI Home",
            "other.showMemoryUsageInRecents",
            "other.recentsGridThreeRows",
            "other.hideRecentsCloseAllButton",
            "other.hideAppsSearchBar",
            "other.removeShortcutBadge",
        ),
        OneuiXPCategory.Messaging to toggles(
            "Messages",
            "other.supportBlockMessage",
        ),
        OneuiXPCategory.Notes to toggles(
            "Samsung Notes",
            "other.supportAllNotesFeatures",
        ),
        OneuiXPCategory.PhotoRetouching to toggles(
            "Photo Editor",
            "other.noAIWatermark",
        ),
        OneuiXPCategory.SketchBook to toggles(
            "SketchBook",
            "other.noAIWatermark",
        ),
        OneuiXPCategory.SPen to toggles(
            "S Pen",
            "other.useSPenGoogleTranslate",
        ),
        OneuiXPCategory.ThemeCenter to toggles(
            "Galaxy Themes",
            "other.setThemeTrialNeverExpired",
        ),
        OneuiXPCategory.Video to toggles(
            "Samsung Video",
            "other.showMorePlaybackSpeeds",
        ),
        OneuiXPCategory.WatchManager to (
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
        OneuiXPCategory.Weather to toggles(
            "Weather",
            "other.setWeatherProviderCN",
        ),
    )

    private val visibilityByPath: Map<String, OneuiXPVisibility> = mapOf(
        "android.maxNeverKilledAppNum" to OneuiXPVisibility.Bool("android.modifyMaxNeverKilledAppNum"),
        "android.supportAppJumpBlock" to OneuiXPVisibility.AndroidAtLeast(35),

        "systemUI.statusBar.statusBarLeftPaddingDp" to
            OneuiXPVisibility.Bool("systemUI.statusBar.modifyStatusBarLeftPadding"),
        "systemUI.statusBar.statusBarRightPaddingDp" to
            OneuiXPVisibility.Bool("systemUI.statusBar.modifyStatusBarRightPadding"),
        "systemUI.statusBar.statusBarTopPaddingDp" to
            OneuiXPVisibility.Bool("systemUI.statusBar.modifyStatusBarTopPadding"),
        "systemUI.statusBar.statusBarBottomPaddingDp" to
            OneuiXPVisibility.Bool("systemUI.statusBar.modifyStatusBarBottomPadding"),
        "systemUI.statusBar.batteryIconWidthScale" to
            OneuiXPVisibility.Bool("systemUI.statusBar.setBatteryIconWidthScale"),
        "systemUI.statusBar.batteryIconHeightScale" to
            OneuiXPVisibility.Bool("systemUI.statusBar.setBatteryIconHeightScale"),
        "systemUI.statusBar.hideBatteryPercentageSign" to
            OneuiXPVisibility.AndroidAtMost(34),
        "systemUI.statusBar.addBatteryLevelText" to
            OneuiXPVisibility.OneUiAtLeast(70000),
        "systemUI.statusBar.hideBatteryLevelTextPercentageSign" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.OneUiAtLeast(70000),
                    OneuiXPVisibility.Bool("systemUI.statusBar.addBatteryLevelText"),
                )
            ),
        "systemUI.statusBar.hideBatteryLevelTextChargingIcon" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.OneUiAtLeast(70000),
                    OneuiXPVisibility.Bool("systemUI.statusBar.addBatteryLevelText"),
                )
            ),
        "systemUI.statusBar.statusBarClockFormat" to
            OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
        "systemUI.statusBar.statusBarDoubleLineClockSize" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXPVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXPVisibility.Not(OneuiXPVisibility.ModelPrefix("SM-F966")),
                )
            ),
        "systemUI.statusBar.useFold7CustomDoubleLineClockScale" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXPVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXPVisibility.ModelPrefix("SM-F966"),
                )
            ),
        "systemUI.statusBar.fold7DoubleLineClockTimeScale" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXPVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXPVisibility.ModelPrefix("SM-F966"),
                    OneuiXPVisibility.Bool("systemUI.statusBar.useFold7CustomDoubleLineClockScale"),
                )
            ),
        "systemUI.statusBar.fold7DoubleLineClockDateScale" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXPVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXPVisibility.ModelPrefix("SM-F966"),
                    OneuiXPVisibility.Bool("systemUI.statusBar.useFold7CustomDoubleLineClockScale"),
                )
            ),
        "systemUI.statusBar.modifyDoubleLineClockGap" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXPVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                )
            ),
        "systemUI.statusBar.doubleLineClockGapDp" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXPVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXPVisibility.Bool("systemUI.statusBar.modifyDoubleLineClockGap"),
                )
            ),
        "systemUI.statusBar.setStatusBarClockTextScale" to
            OneuiXPVisibility.Not(
                OneuiXPVisibility.All(
                    listOf(
                        OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                        OneuiXPVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    )
                )
            ),
        "systemUI.statusBar.statusBarClockTextScale" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockTextScale"),
                    OneuiXPVisibility.Not(
                        OneuiXPVisibility.All(
                            listOf(
                                OneuiXPVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                                OneuiXPVisibility.StringContains(
                                    "systemUI.statusBar.statusBarClockFormat",
                                    "\n",
                                ),
                            )
                        )
                    ),
                )
            ),
        "systemUI.statusBar.physicalEsimAdapterSimSlot" to
            OneuiXPVisibility.Bool("systemUI.statusBar.physicalEsimAdapterWorkaround"),
        "systemUI.statusBar.statusBarMaxNotificationIcons" to
            OneuiXPVisibility.Bool("systemUI.statusBar.modifyStatusBarMaxNotificationIcons"),
        "systemUI.statusBar.customCarrierName" to
            OneuiXPVisibility.Bool("systemUI.statusBar.setCustomCarrierName"),

        "systemUI.qs.hideDeviceControlQsTile" to OneuiXPVisibility.AndroidEquals(34),
        "systemUI.qs.hideSmartViewQsTile" to OneuiXPVisibility.AndroidEquals(34),
        "systemUI.qs.turnOn5gQsTile" to OneuiXPVisibility.AndroidEquals(34),
        "systemUI.qs.hideQsBarMediaPlayer" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.AndroidAtLeast(35),
                    OneuiXPVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.hideQsBarNearbyDevicesAndDeviceControl" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.AndroidAtLeast(35),
                    OneuiXPVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.hideQsBarSecurityFooter" to OneuiXPVisibility.AndroidAtLeast(35),
        "systemUI.qs.hideQsBarDataUsage" to OneuiXPVisibility.AndroidAtLeast(35),
        "systemUI.qs.hideQsBarSmartViewAndModes" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.AndroidAtLeast(35),
                    OneuiXPVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.alwaysExpandQsTileChunk" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.AndroidAtLeast(35),
                    OneuiXPVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.alwaysShowTimeDateOnQs" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.AndroidAtLeast(35),
                    OneuiXPVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.addBrightnessProgressToQsBar" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.AndroidAtLeast(35),
                    OneuiXPVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.addVolumeProgressToQsBar" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.AndroidAtLeast(35),
                    OneuiXPVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.showTraditionalChineseDateOnQS" to
            OneuiXPVisibility.AndroidAtLeast(35),
        "systemUI.qs.modifyQSClockTextSize" to
            OneuiXPVisibility.AndroidAtLeast(35),
        "systemUI.qs.qsClockTextSize" to
            OneuiXPVisibility.All(
                listOf(
                    OneuiXPVisibility.AndroidAtLeast(35),
                    OneuiXPVisibility.Bool("systemUI.qs.modifyQSClockTextSize"),
                )
            ),
        "systemUI.aod.hideAODStatusBar" to OneuiXPVisibility.AndroidAtLeast(35),
        "systemUI.aod.aodLockSupportLunar" to OneuiXPVisibility.AndroidAtLeast(35),

        "systemUI.notification.hideOngoingActivityMediaPackages" to
            OneuiXPVisibility.Bool("systemUI.notification.hideOngoingActivityMedia"),

        "other.showMemoryUsageInRecents" to OneuiXPVisibility.AndroidAtLeast(35),
        "other.supplementChinaWearOsGms" to
            OneuiXPVisibility.IntEquals("other.watchPairingConnectionMode", 1),
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
        rule: OneuiXPVisibility,
        preference: Preference,
    ): Boolean = when (rule) {
        OneuiXPVisibility.Always -> true
        is OneuiXPVisibility.Bool ->
            preferenceValue(preference, rule.path).toBooleanStrictOrNull() == rule.expected
        is OneuiXPVisibility.IntEquals ->
            preferenceValue(preference, rule.path).toIntOrNull() == rule.value
        is OneuiXPVisibility.StringContains ->
            preferenceValue(preference, rule.path).contains(rule.token)
        is OneuiXPVisibility.All ->
            rule.rules.all { visibilityMatches(it, preference) }
        is OneuiXPVisibility.Not ->
            !visibilityMatches(rule.rule, preference)
        is OneuiXPVisibility.AndroidAtLeast -> Build.VERSION.SDK_INT >= rule.sdk
        is OneuiXPVisibility.AndroidAtMost -> Build.VERSION.SDK_INT <= rule.sdk
        is OneuiXPVisibility.AndroidEquals -> Build.VERSION.SDK_INT == rule.sdk
        is OneuiXPVisibility.OneUiAtLeast -> ONE_UI_VERSION >= rule.version
        is OneuiXPVisibility.OneUiBelow -> ONE_UI_VERSION < rule.version
        is OneuiXPVisibility.ModelPrefix ->
            Build.MODEL.startsWith(rule.prefix, ignoreCase = true)
    }

    /**
     * Number of enabled OneuiXP feature injections represented by the feature catalog.
     *
     * Count category entries rather than unique preference paths: a shared feature that
     * injects into two target apps is two actual injection targets. Child value fields and
     * power-menu action rows are not counted separately.
     */
    fun enabledInjectionCount(preference: Preference): Int {
        val catalogCount = OneuiXPCategory.entries.sumOf { category ->
            settings(category).count { spec ->
                if (!visibilityMatches(spec.visibility, preference)) {
                    false
                } else {
                    when (spec.type) {
                        OneuiXPSettingType.Toggle ->
                            preferenceValue(preference, spec.path).toBooleanStrictOrNull() == true
                        OneuiXPSettingType.ChoiceInt ->
                            spec.path == "other.watchPairingConnectionMode" &&
                                preferenceValue(preference, spec.path).toIntOrNull() != 0
                        else -> false
                    }
                }
            }
        }

        // Include OneuiXP's newer standalone runtime features alongside the legacy
        // OneUIXP catalog. Count only each feature's master enable switch; appearance,
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

    fun settings(category: OneuiXPCategory): List<OneuiXPSettingSpec> =
        byCategory[category].orEmpty().map { spec ->
            val rule = when {
                spec.type == OneuiXPSettingType.PowerAction ->
                    OneuiXPVisibility.Bool("systemUI.other.customPowerMenu")
                else -> visibilityByPath[spec.path] ?: OneuiXPVisibility.Always
            }
            spec.copy(visibility = rule)
        }
}
