package com.oneuixp.app.ui.screen.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.core.graphics.drawable.toBitmap
import com.oneuixp.app.R
import com.oneuixp.app.oneuix.OneuiXCategory
import com.oneuixp.app.oneuix.OneuiXSettingSpec
import com.oneuixp.app.oneuix.OneuiXSettingType

@Composable
internal fun oneuiXCategoryTitle(category: OneuiXCategory): String {
    val context = LocalContext.current
    val key = "oneuix_category_${category.name.lowercase()}"
    return remember(category, context.resources.configuration.locales) {
        val packageManager = context.packageManager
        val installedLabel = runCatching {
            packageManager.getApplicationInfo(category.packageName, 0)
                .loadLabel(packageManager)
                .toString()
                .takeIf { it.isNotBlank() }
        }.getOrNull()

        // OneUIX-Mod uses the installed Samsung app label as the category name.
        // Keep our localized text only as a fallback when the package is absent.
        installedLabel ?: resolveString(context, listOf(key)) ?: category.fallbackTitle
    }
}

@Composable
internal fun oneuiXSectionTitle(section: String): String {
    val context = LocalContext.current
    val key = when (section) {
        "Android" -> "oneuix_section_android"
        "Status bar" -> "oneuix_section_status_bar"
        "Status bar values" -> "oneuix_section_status_bar_values"
        "Quick settings" -> "oneuix_section_quick_settings"
        "Always On Display" -> "oneuix_section_aod"
        "Notifications" -> "oneuix_section_notifications"
        "Other System UI" -> "oneuix_section_other_system_ui"
        "Power menu actions" -> "oneuix_section_power_menu_actions"
        "Settings" -> "oneuix_section_settings"
        "Call" -> "oneuix_section_call"
        "Camera" -> "oneuix_section_camera"
        "Samsung Internet" -> "oneuix_section_browser"
        "Calendar" -> "oneuix_section_calendar"
        "Dual Messenger" -> "oneuix_section_dual_app"
        "Gallery" -> "oneuix_section_gallery"
        "Galaxy Store" -> "oneuix_section_galaxy_store"
        "Samsung Health Monitor" -> "oneuix_section_health_monitor"
        "One UI Home" -> "oneuix_section_launcher"
        "Messages" -> "oneuix_section_messaging"
        "Samsung Notes" -> "oneuix_section_notes"
        "Photo Editor" -> "oneuix_section_photo_editor"
        "SketchBook" -> "oneuix_section_sketchbook"
        "S Pen" -> "oneuix_section_spen"
        "Galaxy Themes" -> "oneuix_section_theme_center"
        "Samsung Video" -> "oneuix_section_video"
        "Galaxy Wearable" -> "oneuix_section_watch_manager"
        "Weather" -> "oneuix_section_weather"
        else -> null
    }
    return remember(section, context.resources.configuration.locales) {
        key?.let { resolveString(context, listOf(it)) } ?: section
    }
}

@Composable
internal fun oneuiXSettingTitle(spec: OneuiXSettingSpec): String {
    val context = LocalContext.current
    return remember(spec.path, spec.powerActionName, context.resources.configuration.locales) {
        if (spec.type == OneuiXSettingType.PowerAction) {
            val powerKey = spec.powerActionName?.let { "oneuix_power_\$it" }
            powerKey?.let { resolveString(context, listOf(it)) } ?: spec.title
        } else {
            val raw = spec.path.substringAfterLast('.')
            val sourceKey = settingSourceKey(spec.path, raw)
            val snake = camelToSnake(raw)
            resolveString(
                context,
                listOf(
                    "oneuix_${sourceKey}_title",
                    "oneuix_${raw}_title",
                    "oneuix_${snake}_title",
                ),
            ) ?: spec.title
        }
    }
}

@Composable
internal fun oneuiXSettingSummary(spec: OneuiXSettingSpec): String? {
    if (spec.type == OneuiXSettingType.PowerAction) return null
    val context = LocalContext.current
    return remember(spec.path, context.resources.configuration.locales) {
        val raw = spec.path.substringAfterLast('.')
        val sourceKey = settingSourceKey(spec.path, raw)
        val snake = camelToSnake(raw)
        resolveString(
            context,
            listOf(
                "oneuix_${sourceKey}_summary",
                "oneuix_${raw}_summary",
                "oneuix_${snake}_summary",
            ),
        )?.takeUnless { it.contains('%') }
    }
}

@Composable
internal fun oneuiXChoiceLabels(spec: OneuiXSettingSpec): List<String> {
    val context = LocalContext.current
    return remember(spec.path, spec.choices, context.resources.configuration.locales) {
        spec.choices.map { choice ->
            val key = when (spec.path) {
                "systemUI.statusBar.statusBarDoubleLineClockSize" -> when (choice.value) {
                    "small" -> "oneuix_statusBarDoubleLineClockSize_small"
                    "compact" -> "oneuix_statusBarDoubleLineClockSize_compact"
                    "standard" -> "oneuix_statusBarDoubleLineClockSize_standard"
                    "large" -> "oneuix_statusBarDoubleLineClockSize_large"
                    "extra_large" -> "oneuix_statusBarDoubleLineClockSize_extraLarge"
                    else -> null
                }

                "systemUI.statusBar.physicalEsimAdapterSimSlot" -> when (choice.value) {
                    "0" -> "oneuix_sim_slot_1"
                    "1" -> "oneuix_sim_slot_2"
                    "2" -> "oneuix_sim_slot_both"
                    else -> null
                }

                "other.watchPairingConnectionMode" -> when (choice.value) {
                    "0" -> "oneuix_watchPairing_mode_none"
                    "1" -> "oneuix_watchPairing_mode_wearos_cn"
                    "2" -> "oneuix_watchPairing_mode_wearos_global"
                    else -> null
                }

                else -> null
            }
            key?.let { resolveString(context, listOf(it)) } ?: choice.label
        }
    }
}


@Composable
internal fun oneuiXSettingIcon(spec: OneuiXSettingSpec): ImageVector? {
    val resId = when (spec.path) {
        "android.disablePinVerifyPer72h" -> R.drawable.lock_clock
        "android.modifyMaxNeverKilledAppNum" -> R.drawable.apps
        "android.setBlockableNotificationChannel" -> R.drawable.notifications
        "android.supportAppJumpBlock" -> R.drawable.block
        "android.allowAllRotation" -> R.drawable.expand
        "android.liftFcmNetworkLimit" -> R.drawable.google_play
        "android.disableScreenWakeOnPowerUnplugged" -> R.drawable.mobile_screensaver
        "android.disableWritingToolkitGlobally" -> R.drawable.format_letter_spacing

        "systemUI.statusBar.modifyStatusBarLeftPadding",
        "systemUI.statusBar.modifyStatusBarRightPadding",
        "systemUI.statusBar.statusBarTopPaddingDp",
        "systemUI.statusBar.statusBarBottomPaddingDp" -> R.drawable.padding

        "systemUI.statusBar.setBatteryIconWidthScale",
        "systemUI.statusBar.setBatteryIconHeightScale",
        "systemUI.statusBar.hideBatteryPercentageSign",
        "systemUI.statusBar.hideBatteryIcon",
        "systemUI.statusBar.addBatteryLevelText",
        "systemUI.statusBar.hideBatteryLevelTextPercentageSign",
        "systemUI.statusBar.hideBatteryLevelTextChargingIcon",
        "systemUI.aod.hideAODStatusBar" -> R.drawable.battery

        "systemUI.statusBar.supportRealTimeNetworkSpeed",
        "systemUI.statusBar.showSeparateUpDownNetworkSpeeds",
        "systemUI.statusBar.networkSpeedThresholdKb" -> R.drawable.net_speed

        "systemUI.statusBar.setStatusBarClockFormat",
        "systemUI.statusBar.updateStatusBarClockEverySecond",
        "systemUI.qs.alwaysShowTimeDateOnQs" -> R.drawable.nest_clock_farsight_digital

        "systemUI.statusBar.useFold7CustomDoubleLineClockScale",
        "systemUI.statusBar.statusBarDoubleLineClockSize",
        "systemUI.statusBar.setStatusBarClockTextScale",
        "systemUI.qs.modifyQSClockTextSize" -> R.drawable.format_size

        "systemUI.statusBar.hideSecureFolderStatusBarIcon" -> R.drawable.folder_managed
        "systemUI.statusBar.restoreBluetoothStatusBarIcon" -> R.drawable.bluetooth
        "systemUI.statusBar.physicalEsimAdapterWorkaround",
        "systemUI.statusBar.physicalEsimAdapterSimSlot",
        "systemUI.statusBar.setCustomCarrierName" -> R.drawable.sim_card
        "systemUI.statusBar.doubleTapStatusBarToSleep",
        "systemUI.statusBar.hideLockscreenStatusBar" -> R.drawable.mobile_screensaver
        "systemUI.statusBar.modifyStatusBarMaxNotificationIcons" -> R.drawable.notifications

        "systemUI.qs.setQsClockMonospaced" -> R.drawable.format_letter_spacing
        "systemUI.qs.hideDeviceControlQsTile",
        "systemUI.qs.hideSmartViewQsTile",
        "systemUI.qs.hideQsBarMediaPlayer",
        "systemUI.qs.hideQsBarNearbyDevicesAndDeviceControl",
        "systemUI.qs.hideQsBarSecurityFooter",
        "systemUI.qs.hideQsBarDataUsage",
        "systemUI.qs.hideQsBarSmartViewAndModes" -> R.drawable.tile_medium
        "systemUI.qs.turnOn5gQsTile" -> R.drawable.five_g
        "systemUI.qs.alwaysExpandQsTileChunk" -> R.drawable.expand
        "systemUI.qs.addBrightnessProgressToQsBar" -> R.drawable.light_mode
        "systemUI.qs.addVolumeProgressToQsBar" -> R.drawable.music_note
        "systemUI.qs.showTraditionalChineseDateOnQS",
        "systemUI.aod.aodLockSupportLunar" -> R.drawable.today

        "systemUI.notification.hideOngoingActivityMedia" -> R.drawable.music_note
        "systemUI.notification.disableNotificationGrouping",
        "systemUI.notification.autoExpandNotifications" -> R.drawable.notifications
        "systemUI.other.customPowerMenu" -> R.drawable.power_settings_new
        "systemUI.other.disableScreenshotCaptureSound" -> R.drawable.screenshot

        "settings.showForcePeakRefreshRatePreference" -> R.drawable.logo_dev
        "settings.supportOutdoorMode" -> R.drawable.light_mode
        "settings.showMoreBatteryInfo" -> R.drawable.battery
        "settings.showPackageInfo" -> R.drawable.apk_document
        "settings.showWiFiLinkSpeed" -> R.drawable.wifi_link_speed
        "settings.supportAnyFont" -> R.drawable.font
        "settings.supportAutoPowerOnOff" -> R.drawable.power_settings_new
        "settings.spoofPhoneStatusAsOfficial" -> R.drawable.mobile_screensaver

        "call.supportVoiceCallRecording",
        "call.preferRecordingButton" -> R.drawable.call_recording
        "call.showGeocodedLocationInRecentCall" -> R.drawable.person_pin_circle
        "call.isOpStyleCHN" -> R.drawable.call_log
        "call.supportCallAndTextOnOtherDevices" -> R.drawable.phone_forwarded

        "camera.supportAllCameraMenu",
        "camera.disableCameraTemperatureCheck" -> R.drawable.camera

        "other.showMorePlaybackSpeeds" -> R.drawable.fast_forward
        "other.redirectCustomTab" -> R.drawable.tab_move
        "other.spoofBrowserCountryCodeToUS" -> R.drawable.language_us
        "other.enableChineseHolidayDisplay" -> R.drawable.today
        "other.makeAllUserAppsAvailable" -> R.drawable.apk_document
        "other.supportAllGallerySettings",
        "other.supportSharedAlbumsInHide",
        "other.hideVideoEditorStudio" -> R.drawable.photo_library
        "other.blockGalaxyStoreAds" -> R.drawable.ad_off
        "other.bypassHealthMonitorCountryCheck" -> R.drawable.health_metrics
        "other.showMemoryUsageInRecents" -> R.drawable.memory
        "other.recentsGridThreeRows" -> R.drawable.tile_medium
        "other.hideRecentsCloseAllButton" -> R.drawable.block
        "other.hideAppsSearchBar" -> R.drawable.apps
        "other.removeShortcutBadge" -> R.drawable.position_bottom_right
        "other.supportBlockMessage" -> R.drawable.sms
        "other.supportAllNotesFeatures" -> R.drawable.notes
        "other.noAIWatermark" -> R.drawable.branding_watermark
        "other.useSPenGoogleTranslate" -> R.drawable.spen
        "other.setThemeTrialNeverExpired" -> R.drawable.format_paint
        "other.watchPairingConnectionMode" -> R.drawable.watch_pairing
        "other.bypassWatchPairingRegionCheck" -> R.drawable.lock_open
        "other.supplementChinaWearOsGms" -> R.drawable.google_play
        "other.setWeatherProviderCN" -> R.drawable.partly_cloudy_day
        else -> null
    }
    return resId?.let { ImageVector.vectorResource(it) }
}

@Composable
internal fun rememberOneuiXCategoryIcon(category: OneuiXCategory): ImageBitmap {
    val context = LocalContext.current
    return remember(category) {
        val packageManager = context.packageManager
        val applicationInfo = runCatching {
            packageManager.getApplicationInfo(category.packageName, 0)
        }.getOrNull()

        val drawable = applicationInfo
            ?.loadIcon(packageManager)
            ?: requireNotNull(
                context.getDrawable(R.mipmap.oneuix_mod_category_fallback)
            )

        // Match OneUIX-Mod exactly: preserve the drawable's intrinsic/adaptive
        // icon rendering instead of forcing it into a 128x128 bitmap first.
        drawable.toBitmap().asImageBitmap()
    }
}

private fun settingSourceKey(path: String, raw: String): String = when (path) {
    "android.maxNeverKilledAppNum" -> "modifyMaxNeverKilledAppNum"
    "systemUI.statusBar.modifyStatusBarLeftPadding" -> "statusBarLeftPaddingDp"
    "systemUI.statusBar.modifyStatusBarRightPadding" -> "statusBarRightPaddingDp"
    "systemUI.statusBar.useFold7CustomDoubleLineClockScale" -> "fold7CustomDoubleLineClockScale"
    "systemUI.statusBar.modifyStatusBarMaxNotificationIcons" -> "setStatusBarMaxNotificationIcons"
    "systemUI.statusBar.batteryIconWidthScale" -> "setBatteryIconWidthScale"
    "systemUI.statusBar.batteryIconHeightScale" -> "setBatteryIconHeightScale"
    "systemUI.statusBar.statusBarClockFormat" -> "setStatusBarClockFormat"
    "systemUI.statusBar.statusBarClockTextScale" -> "setStatusBarClockTextScale"
    "systemUI.statusBar.doubleLineClockGapDp" -> "statusBarDoubleLineClockGap"
    "systemUI.statusBar.networkSpeedThresholdKb" -> "networkSpeedThreshold"
    "systemUI.statusBar.statusBarMaxNotificationIcons" -> "setStatusBarMaxNotificationIcons"
    "systemUI.statusBar.customCarrierName" -> "setCustomCarrierName"
    "systemUI.qs.qsClockTextSize" -> "modifyQSClockTextSize"
    "systemUI.notification.hideOngoingActivityMediaPackages" -> "hideOngoingActivityMedia"
    "other.watchPairingConnectionMode" -> "watchPairing_connectionMode"
    else -> raw
}

private fun camelToSnake(value: String): String =
    value.replace(Regex("([a-z0-9])([A-Z])"), "\$1_\$2").lowercase()

private fun resolveString(
    context: android.content.Context,
    keys: List<String>,
): String? {
    keys.distinct().forEach { key ->
        val id = context.resources.getIdentifier(key, "string", context.packageName)
        if (id != 0) return context.getString(id)
    }
    return null
}
