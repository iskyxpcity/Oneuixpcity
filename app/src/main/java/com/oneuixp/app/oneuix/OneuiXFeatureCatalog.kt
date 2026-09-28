package com.oneuixp.app.oneuix

import android.os.Build
import io.github.soclear.oneuix.common.ONE_UI_VERSION
import io.github.soclear.oneuix.common.Package
import io.github.soclear.oneuix.common.PowerMenuAction
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.common.IgnoreUnknownKeysJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

enum class OneuiXCategory(
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

enum class OneuiXSettingType {
    Toggle,
    IntValue,
    FloatValue,
    TextValue,
    ChoiceInt,
    ChoiceText,
    PowerAction,
}

sealed interface OneuiXVisibility {
    data object Always : OneuiXVisibility
    data class Bool(val path: String, val expected: Boolean = true) : OneuiXVisibility
    data class IntEquals(val path: String, val value: Int) : OneuiXVisibility
    data class StringContains(val path: String, val token: String) : OneuiXVisibility
    data class All(val rules: List<OneuiXVisibility>) : OneuiXVisibility
    data class Not(val rule: OneuiXVisibility) : OneuiXVisibility
    data class AndroidAtLeast(val sdk: Int) : OneuiXVisibility
    data class AndroidAtMost(val sdk: Int) : OneuiXVisibility
    data class AndroidEquals(val sdk: Int) : OneuiXVisibility
    data class OneUiAtLeast(val version: Int) : OneuiXVisibility
    data class OneUiBelow(val version: Int) : OneuiXVisibility
    data class ModelPrefix(val prefix: String) : OneuiXVisibility
}

data class OneuiXChoice(
    val value: String,
    val label: String,
)

data class OneuiXSettingSpec(
    val path: String,
    val type: OneuiXSettingType,
    val section: String,
    val titleOverride: String? = null,
    val summary: String? = null,
    val choices: List<OneuiXChoice> = emptyList(),
    val powerActionName: String? = null,
    val visibility: OneuiXVisibility = OneuiXVisibility.Always,
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
    paths.map { OneuiXSettingSpec(it, OneuiXSettingType.Toggle, section) }

private fun ints(section: String, vararg paths: String) =
    paths.map { OneuiXSettingSpec(it, OneuiXSettingType.IntValue, section) }

private fun floats(section: String, vararg paths: String) =
    paths.map { OneuiXSettingSpec(it, OneuiXSettingType.FloatValue, section) }

private fun texts(section: String, vararg paths: String) =
    paths.map { OneuiXSettingSpec(it, OneuiXSettingType.TextValue, section) }

private fun choiceInt(
    section: String,
    path: String,
    title: String? = null,
    vararg choices: Pair<Int, String>,
) = OneuiXSettingSpec(
    path = path,
    type = OneuiXSettingType.ChoiceInt,
    section = section,
    titleOverride = title,
    choices = choices.map { OneuiXChoice(it.first.toString(), it.second) },
)

private fun choiceText(
    section: String,
    path: String,
    title: String? = null,
    vararg choices: Pair<String, String>,
) = OneuiXSettingSpec(
    path = path,
    type = OneuiXSettingType.ChoiceText,
    section = section,
    titleOverride = title,
    choices = choices.map { OneuiXChoice(it.first, it.second) },
)

private fun powerAction(name: String, label: String) = OneuiXSettingSpec(
    path = "systemUI.other.powerMenuActions.$name",
    type = OneuiXSettingType.PowerAction,
    section = "Power menu actions",
    titleOverride = label,
    powerActionName = name,
)

object OneuiXFeatureCatalog {
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

    private val byCategory: Map<OneuiXCategory, List<OneuiXSettingSpec>> = mapOf(
        OneuiXCategory.Android to android,
        OneuiXCategory.SystemUI to (
            systemUiStatusBar + systemUiQs + systemUiAod + systemUiNotification + systemUiOther
        ),
        OneuiXCategory.Settings to settings,
        OneuiXCategory.Call to call,
        OneuiXCategory.Camera to camera,
        OneuiXCategory.Browser to toggles(
            "Samsung Internet",
            "other.showMorePlaybackSpeeds",
            "other.redirectCustomTab",
            "other.spoofBrowserCountryCodeToUS",
        ),
        OneuiXCategory.Calendar to toggles(
            "Calendar",
            "other.enableChineseHolidayDisplay",
        ),
        OneuiXCategory.DualApp to toggles(
            "Dual Messenger",
            "other.makeAllUserAppsAvailable",
        ),
        OneuiXCategory.Gallery to toggles(
            "Gallery",
            "other.supportAllGallerySettings",
            "other.supportSharedAlbumsInHide",
            "other.hideVideoEditorStudio",
        ),
        OneuiXCategory.GalaxyStore to toggles(
            "Galaxy Store",
            "other.blockGalaxyStoreAds",
        ),
        OneuiXCategory.HealthMonitor to toggles(
            "Samsung Health Monitor",
            "other.bypassHealthMonitorCountryCheck",
        ),
        OneuiXCategory.Launcher to toggles(
            "One UI Home",
            "other.showMemoryUsageInRecents",
            "other.recentsGridThreeRows",
            "other.hideRecentsCloseAllButton",
            "other.hideAppsSearchBar",
            "other.removeShortcutBadge",
        ),
        OneuiXCategory.Messaging to toggles(
            "Messages",
            "other.supportBlockMessage",
        ),
        OneuiXCategory.Notes to toggles(
            "Samsung Notes",
            "other.supportAllNotesFeatures",
        ),
        OneuiXCategory.PhotoRetouching to toggles(
            "Photo Editor",
            "other.noAIWatermark",
        ),
        OneuiXCategory.SketchBook to toggles(
            "SketchBook",
            "other.noAIWatermark",
        ),
        OneuiXCategory.SPen to toggles(
            "S Pen",
            "other.useSPenGoogleTranslate",
        ),
        OneuiXCategory.ThemeCenter to toggles(
            "Galaxy Themes",
            "other.setThemeTrialNeverExpired",
        ),
        OneuiXCategory.Video to toggles(
            "Samsung Video",
            "other.showMorePlaybackSpeeds",
        ),
        OneuiXCategory.WatchManager to (
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
        OneuiXCategory.Weather to toggles(
            "Weather",
            "other.setWeatherProviderCN",
        ),
    )

    private val visibilityByPath: Map<String, OneuiXVisibility> = mapOf(
        "android.maxNeverKilledAppNum" to OneuiXVisibility.Bool("android.modifyMaxNeverKilledAppNum"),
        "android.supportAppJumpBlock" to OneuiXVisibility.AndroidAtLeast(35),

        "systemUI.statusBar.statusBarLeftPaddingDp" to
            OneuiXVisibility.Bool("systemUI.statusBar.modifyStatusBarLeftPadding"),
        "systemUI.statusBar.statusBarRightPaddingDp" to
            OneuiXVisibility.Bool("systemUI.statusBar.modifyStatusBarRightPadding"),
        "systemUI.statusBar.statusBarTopPaddingDp" to
            OneuiXVisibility.Bool("systemUI.statusBar.modifyStatusBarTopPadding"),
        "systemUI.statusBar.statusBarBottomPaddingDp" to
            OneuiXVisibility.Bool("systemUI.statusBar.modifyStatusBarBottomPadding"),
        "systemUI.statusBar.batteryIconWidthScale" to
            OneuiXVisibility.Bool("systemUI.statusBar.setBatteryIconWidthScale"),
        "systemUI.statusBar.batteryIconHeightScale" to
            OneuiXVisibility.Bool("systemUI.statusBar.setBatteryIconHeightScale"),
        "systemUI.statusBar.hideBatteryPercentageSign" to
            OneuiXVisibility.AndroidAtMost(34),
        "systemUI.statusBar.addBatteryLevelText" to
            OneuiXVisibility.OneUiAtLeast(70000),
        "systemUI.statusBar.hideBatteryLevelTextPercentageSign" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.OneUiAtLeast(70000),
                    OneuiXVisibility.Bool("systemUI.statusBar.addBatteryLevelText"),
                )
            ),
        "systemUI.statusBar.hideBatteryLevelTextChargingIcon" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.OneUiAtLeast(70000),
                    OneuiXVisibility.Bool("systemUI.statusBar.addBatteryLevelText"),
                )
            ),
        "systemUI.statusBar.statusBarClockFormat" to
            OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
        "systemUI.statusBar.statusBarDoubleLineClockSize" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXVisibility.Not(OneuiXVisibility.ModelPrefix("SM-F966")),
                )
            ),
        "systemUI.statusBar.useFold7CustomDoubleLineClockScale" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXVisibility.ModelPrefix("SM-F966"),
                )
            ),
        "systemUI.statusBar.fold7DoubleLineClockTimeScale" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXVisibility.ModelPrefix("SM-F966"),
                    OneuiXVisibility.Bool("systemUI.statusBar.useFold7CustomDoubleLineClockScale"),
                )
            ),
        "systemUI.statusBar.fold7DoubleLineClockDateScale" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXVisibility.ModelPrefix("SM-F966"),
                    OneuiXVisibility.Bool("systemUI.statusBar.useFold7CustomDoubleLineClockScale"),
                )
            ),
        "systemUI.statusBar.modifyDoubleLineClockGap" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                )
            ),
        "systemUI.statusBar.doubleLineClockGapDp" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                    OneuiXVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    OneuiXVisibility.Bool("systemUI.statusBar.modifyDoubleLineClockGap"),
                )
            ),
        "systemUI.statusBar.setStatusBarClockTextScale" to
            OneuiXVisibility.Not(
                OneuiXVisibility.All(
                    listOf(
                        OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                        OneuiXVisibility.StringContains("systemUI.statusBar.statusBarClockFormat", "\n"),
                    )
                )
            ),
        "systemUI.statusBar.statusBarClockTextScale" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockTextScale"),
                    OneuiXVisibility.Not(
                        OneuiXVisibility.All(
                            listOf(
                                OneuiXVisibility.Bool("systemUI.statusBar.setStatusBarClockFormat"),
                                OneuiXVisibility.StringContains(
                                    "systemUI.statusBar.statusBarClockFormat",
                                    "\n",
                                ),
                            )
                        )
                    ),
                )
            ),
        "systemUI.statusBar.physicalEsimAdapterSimSlot" to
            OneuiXVisibility.Bool("systemUI.statusBar.physicalEsimAdapterWorkaround"),
        "systemUI.statusBar.statusBarMaxNotificationIcons" to
            OneuiXVisibility.Bool("systemUI.statusBar.modifyStatusBarMaxNotificationIcons"),
        "systemUI.statusBar.customCarrierName" to
            OneuiXVisibility.Bool("systemUI.statusBar.setCustomCarrierName"),

        "systemUI.qs.hideDeviceControlQsTile" to OneuiXVisibility.AndroidEquals(34),
        "systemUI.qs.hideSmartViewQsTile" to OneuiXVisibility.AndroidEquals(34),
        "systemUI.qs.turnOn5gQsTile" to OneuiXVisibility.AndroidEquals(34),
        "systemUI.qs.hideQsBarMediaPlayer" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.AndroidAtLeast(35),
                    OneuiXVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.hideQsBarNearbyDevicesAndDeviceControl" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.AndroidAtLeast(35),
                    OneuiXVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.hideQsBarSecurityFooter" to OneuiXVisibility.AndroidAtLeast(35),
        "systemUI.qs.hideQsBarDataUsage" to OneuiXVisibility.AndroidAtLeast(35),
        "systemUI.qs.hideQsBarSmartViewAndModes" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.AndroidAtLeast(35),
                    OneuiXVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.alwaysExpandQsTileChunk" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.AndroidAtLeast(35),
                    OneuiXVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.alwaysShowTimeDateOnQs" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.AndroidAtLeast(35),
                    OneuiXVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.addBrightnessProgressToQsBar" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.AndroidAtLeast(35),
                    OneuiXVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.addVolumeProgressToQsBar" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.AndroidAtLeast(35),
                    OneuiXVisibility.OneUiBelow(80500),
                )
            ),
        "systemUI.qs.showTraditionalChineseDateOnQS" to
            OneuiXVisibility.AndroidAtLeast(35),
        "systemUI.qs.modifyQSClockTextSize" to
            OneuiXVisibility.AndroidAtLeast(35),
        "systemUI.qs.qsClockTextSize" to
            OneuiXVisibility.All(
                listOf(
                    OneuiXVisibility.AndroidAtLeast(35),
                    OneuiXVisibility.Bool("systemUI.qs.modifyQSClockTextSize"),
                )
            ),
        "systemUI.aod.hideAODStatusBar" to OneuiXVisibility.AndroidAtLeast(35),
        "systemUI.aod.aodLockSupportLunar" to OneuiXVisibility.AndroidAtLeast(35),

        "systemUI.notification.hideOngoingActivityMediaPackages" to
            OneuiXVisibility.Bool("systemUI.notification.hideOngoingActivityMedia"),

        "other.showMemoryUsageInRecents" to OneuiXVisibility.AndroidAtLeast(35),
        "other.supplementChinaWearOsGms" to
            OneuiXVisibility.IntEquals("other.watchPairingConnectionMode", 1),
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
        rule: OneuiXVisibility,
        preference: Preference,
    ): Boolean = when (rule) {
        OneuiXVisibility.Always -> true
        is OneuiXVisibility.Bool ->
            preferenceValue(preference, rule.path).toBooleanStrictOrNull() == rule.expected
        is OneuiXVisibility.IntEquals ->
            preferenceValue(preference, rule.path).toIntOrNull() == rule.value
        is OneuiXVisibility.StringContains ->
            preferenceValue(preference, rule.path).contains(rule.token)
        is OneuiXVisibility.All ->
            rule.rules.all { visibilityMatches(it, preference) }
        is OneuiXVisibility.Not ->
            !visibilityMatches(rule.rule, preference)
        is OneuiXVisibility.AndroidAtLeast -> Build.VERSION.SDK_INT >= rule.sdk
        is OneuiXVisibility.AndroidAtMost -> Build.VERSION.SDK_INT <= rule.sdk
        is OneuiXVisibility.AndroidEquals -> Build.VERSION.SDK_INT == rule.sdk
        is OneuiXVisibility.OneUiAtLeast -> ONE_UI_VERSION >= rule.version
        is OneuiXVisibility.OneUiBelow -> ONE_UI_VERSION < rule.version
        is OneuiXVisibility.ModelPrefix ->
            Build.MODEL.startsWith(rule.prefix, ignoreCase = true)
    }

    /**
     * Number of enabled OneuiXP feature injections represented by the feature catalog.
     *
     * Count category entries rather than unique preference paths: a shared feature that
     * injects into two target apps is two actual injection targets. Child value fields and
     * power-menu action rows are not counted separately.
     */
    fun enabledInjectionCount(preference: Preference): Int =
        OneuiXCategory.entries.sumOf { category ->
            settings(category).count { spec ->
                if (!visibilityMatches(spec.visibility, preference)) {
                    false
                } else {
                    when (spec.type) {
                        OneuiXSettingType.Toggle ->
                            preferenceValue(preference, spec.path).toBooleanStrictOrNull() == true
                        OneuiXSettingType.ChoiceInt ->
                            spec.path == "other.watchPairingConnectionMode" &&
                                preferenceValue(preference, spec.path).toIntOrNull() != 0
                        else -> false
                    }
                }
            }
        }

    fun settings(category: OneuiXCategory): List<OneuiXSettingSpec> =
        byCategory[category].orEmpty().map { spec ->
            val rule = when {
                spec.type == OneuiXSettingType.PowerAction ->
                    OneuiXVisibility.Bool("systemUI.other.customPowerMenu")
                else -> visibilityByPath[spec.path] ?: OneuiXVisibility.Always
            }
            spec.copy(visibility = rule)
        }
}
