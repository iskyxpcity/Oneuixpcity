package com.alohaoneui.hook

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.alohaoneui.common.Package
import com.alohaoneui.hook.systemui.AOD
import com.alohaoneui.hook.systemui.ESIM
import com.alohaoneui.hook.systemui.HideBatteryIcon
import com.alohaoneui.hook.systemui.Notification
import com.alohaoneui.hook.systemui.Other
import com.alohaoneui.hook.systemui.QS
import com.alohaoneui.hook.systemui.StatusBar
import com.alohaoneui.hook.systemui.StatusBarComboStyleOneRuntime
import com.alohaoneui.hook.systemui.StatusBarSignalIndependentRuntime
import com.alohaoneui.hook.systemui.StatusBarBurnInProtectionRuntime
import com.alohaoneui.hook.systemui.OfficialMainStatusBarMod
import com.alohaoneui.hook.systemui.powermenu.PowerMenu
import com.alohaoneui.hook.util.AlohaOneUIStatusReporter
import com.alohaoneui.hook.util.PreferenceProvider
import com.alohaoneui.hook.util.addAssetPath
import com.alohaoneui.hook.util.xlog

class Main : XposedModule() {
    private companion object {
        // AlohaOneUI migration: imported feature hooks are active.
        const val ENABLE_ALOHAONEUI_FUNCTIONS = true
    }

    private var processName = ""

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) = with(param) {
        if (packageName == "com.alohaoneui.app" && processName == "com.alohaoneui.app") {
            runCatching {
                val method = classLoader
                    .loadClass("com.alohaoneui.app.statusbar.ModuleStatus")
                    .getDeclaredMethod("isActive")
                this@Main.hook(method).intercept { true }
            }
            return@with
        }

        AlohaOneUIStatusReporter.installIfSystemUi(this@Main, param, processName)
        if (!ENABLE_ALOHAONEUI_FUNCTIONS) return@with

        val preference = PreferenceProvider.loadPreference() ?: return@with

        if (packageName == Package.SYSTEMUI && processName == Package.SYSTEMUI) {
            xlog(
                "CHAIN_PROBE SystemUI dispatch " +
                    "leftEnabled=${preference.systemUI.statusBar.modifyStatusBarLeftPadding}, " +
                    "leftDp=${preference.systemUI.statusBar.statusBarLeftPaddingDp}, " +
                    "clockEnabled=${preference.systemUI.statusBar.setStatusBarClockFormat}, " +
                    "clockFormat=${preference.systemUI.statusBar.statusBarClockFormat.replace("\n", "\\n")}, " +
                    "maxIconsEnabled=${preference.systemUI.statusBar.modifyStatusBarMaxNotificationIcons}, " +
                    "maxIcons=${preference.systemUI.statusBar.statusBarMaxNotificationIcons}",
                priority = Log.INFO,
            )
        } else if (packageName == Package.LAUNCHER) {
            xlog(
                "CHAIN_PROBE Launcher dispatch " +
                    "showMemoryUsageInRecents=${preference.other.showMemoryUsageInRecents}",
                priority = Log.INFO,
            )
        }

        when (packageName) {
            Package.BROWSER -> {
                if (preference.other.showMorePlaybackSpeeds) {
                    Browser.showMorePlaybackSpeeds()
                }

                if (preference.other.spoofBrowserCountryCodeToUS) {
                    Browser.setCountryIsoCode("US")
                }

                if (preference.other.redirectCustomTab) {
                    Browser.redirectCustomTab()
                }
            }

            Package.CALENDAR -> {
                if (preference.other.enableChineseHolidayDisplay) {
                    Calendar.enableChineseHolidayDisplay()
                }
            }

            Package.CAMERA -> {
                Camera.setBooleanFeature(
                    supportAllMenu = preference.camera.supportAllCameraMenu,
                    disableTemperatureCheck = preference.camera.disableCameraTemperatureCheck
                )
            }

            Package.DIALER -> {
                if (preference.call.supportVoiceCallRecording) {
                    Call.supportVoiceCallRecording(
                        preference.call.preferRecordingButton
                    )
                }

                if (preference.call.showGeocodedLocationInRecentCall) {
                    Call.showGeocodedLocationInRecentCall()
                }

                if (preference.call.isOpStyleCHN) {
                    Call.isOpStyleCHN()
                }
            }

            Package.DUAL_APP -> {
                if (preference.other.makeAllUserAppsAvailable) {
                    DualApp.makeAllUserAppsAvailable()
                }
            }

            Package.GALLERY -> {
                if (preference.other.supportAllGallerySettings) {
                    Gallery.supportAllSettings()
                }

                if (preference.other.supportSharedAlbumsInHide) {
                    Gallery.supportSharedAlbumsInHide()
                }

                if (preference.other.hideVideoEditorStudio) {
                    Gallery.hideVideoEditorStudio()
                }
            }

            Package.HEALTH_MONITOR -> {
                if (preference.other.bypassHealthMonitorCountryCheck) {
                    HealthMonitor.bypassCountryCheck()
                }
            }

            Package.INCALLUI -> {
                if (preference.call.supportVoiceCallRecording) {
                    Call.supportVoiceCallRecording(
                        preference.call.preferRecordingButton
                    )
                }
                Call.hookCallReturnDelay()
            }

            Package.LAUNCHER -> {
                if (preference.other.showMemoryUsageInRecents) {
                    Launcher.showMemoryUsageInRecents()
                }

                if (preference.other.recentsGridThreeRows) {
                    Launcher.enableThreeRowsRecentsGrid()
                }

                if (preference.other.hideRecentsCloseAllButton) {
                    Launcher.hideRecentsCloseAllButton()
                }

                if (preference.other.hideAppsSearchBar) {
                    Launcher.hideAppsSearchBar()
                }

                if (preference.other.removeShortcutBadge) {
                    Launcher.removeShortcutBadge()
                }
            }

            Package.MDEC_SERVICE -> {
                if (preference.call.supportCallAndTextOnOtherDevices) {
                    MdecService.supportCallAndTextOnOtherDevices()
                }
            }

            Package.MESSAGING -> {
                if (preference.other.supportBlockMessage) {
                    Messaging.isSupportBlock()
                }
            }

            Package.NOTES -> {
                if (preference.other.supportAllNotesFeatures) {
                    Notes.supportAllFeatures()
                }
            }

            Package.PHOTO_RETOUCHING -> {
                if (preference.other.noAIWatermark) {
                    PhotoRetouching.noAIWatermark()
                }
            }

            Package.SETTINGS -> {
                if (preference.other.settingsTwoPaneEnabled) {
                    Settings.enableSettingsTwoPane()
                }

                if (preference.android.setBlockableNotificationChannel) {
                    Android.setBlockableNotificationChannel()
                }

                if (preference.settings.showForcePeakRefreshRatePreference) {
                    Settings.showForcePeakRefreshRatePreference()
                }

                if (preference.settings.supportOutdoorMode) {
                    Settings.supportOutdoorMode()
                }

                if (preference.settings.showMoreBatteryInfo) {
                    Settings.showMoreBatteryInfo()
                }

                if (preference.settings.showPackageInfo) {
                    Settings.showPackageInfo()
                }

                if (preference.settings.showWiFiLinkSpeed) {
                    Network.showWiFiLinkSpeed()
                }

                if (preference.settings.supportAnyFont) {
                    Settings.supportAnyFont()
                }

                if (preference.android.supportAppJumpBlock) {
                    CoreRune.supportAppJumpBlockSettings()
                }

                if (preference.systemUI.statusBar.supportRealTimeNetworkSpeed) {
                    Network.supportRealTimeNetworkSpeed()
                }

                if (preference.settings.supportAutoPowerOnOff) {
                    Settings.supportAutoPowerOnOff()
                }

                if (preference.settings.spoofPhoneStatusAsOfficial) {
                    Settings.spoofPhoneStatusAsOfficial()
                }
            }

            Package.SKETCH_BOOK -> {
                if (preference.other.noAIWatermark) {
                    SketchBook.noAIWatermark()
                }
            }

            Package.SM_CN -> {
                if (preference.settings.spoofPhoneStatusAsOfficial) {
                    SMCN.spoofPhoneStatusAsOfficial()
                }
            }

            Package.STORE -> {
                if (preference.other.blockGalaxyStoreAds) {
                    GalaxyStore.blockGalaxyStoreAds()
                }
            }

            Package.PACKAGE_INSTALLER, Package.SAMSUNG_PACKAGE_INSTALLER -> {
                PackageInstallerHooks.hideRecommendations()
            }

            Package.SYSTEMUI if (processName == Package.SYSTEMUI) -> {
                StatusBarBurnInProtectionRuntime.install(this@Main, param)
                StatusBarComboStyleOneRuntime.install(this@Main, param)
                StatusBarSignalIndependentRuntime.install(this@Main, param)

                if (preference.android.setBlockableNotificationChannel) {
                    Android.setBlockableNotificationChannel()
                }

                if (preference.systemUI.notification.autoExpandNotifications) {
                    Notification.autoExpandNotifications()
                }

                run {
                    val leftPaddingDp =
                        if (preference.systemUI.statusBar.modifyStatusBarLeftPadding) {
                            preference.systemUI.statusBar.statusBarLeftPaddingDp
                        } else null
                    val rightPaddingDp =
                        if (preference.systemUI.statusBar.modifyStatusBarRightPadding) {
                            preference.systemUI.statusBar.statusBarRightPaddingDp
                        } else null
                    StatusBar.setStatusBarPaddingDp(leftPaddingDp, rightPaddingDp)
                    val topPaddingDp =
                        if (preference.systemUI.statusBar.modifyStatusBarTopPadding) {
                            preference.systemUI.statusBar.statusBarTopPaddingDp
                        } else 0f
                    val bottomPaddingDp =
                        if (preference.systemUI.statusBar.modifyStatusBarBottomPadding) {
                            preference.systemUI.statusBar.statusBarBottomPaddingDp
                        } else 0f
                    OfficialMainStatusBarMod.installVerticalPadding(
                        topPaddingDp,
                        bottomPaddingDp,
                    )
                }

                run {
                    val widthScale = if (preference.systemUI.statusBar.setBatteryIconWidthScale) {
                        preference.systemUI.statusBar.batteryIconWidthScale
                    } else null
                    val heightScale = if (preference.systemUI.statusBar.setBatteryIconHeightScale) {
                        preference.systemUI.statusBar.batteryIconHeightScale
                    } else null
                    StatusBar.setBatteryIconScale(widthScale, heightScale)
                }

                if (preference.systemUI.statusBar.hideBatteryPercentageSign) {
                    StatusBar.hideBatteryPercentageSign()
                }

                if (preference.systemUI.statusBar.hideBatteryIcon) {
                    HideBatteryIcon.apply()
                }

                if (preference.systemUI.statusBar.addBatteryLevelText) {
                    StatusBar.addBatteryLevelText(
                        hidePercentSign = preference.systemUI.statusBar.hideBatteryLevelTextPercentageSign,
                        hideChargingIcon = preference.systemUI.statusBar.hideBatteryLevelTextChargingIcon,
                    )
                }

                val networkBar = preference.systemUI.statusBar
                if (networkBar.supportRealTimeNetworkSpeed) {
                    Network.supportRealTimeNetworkSpeed()
                }

                // StatusBarNetworkSpeedRuntime is the single SystemUI presentation owner.
                // Do not install a second NetspeedView display hook. The dedicated runtime consumes
                // these preferences and owns threshold, line gap, per-line scale, styling,
                // and the separate upload/download data source.

                val bar = preference.systemUI.statusBar
                StatusBar.updateStatusBarClockRuntime(bar)
                OfficialMainStatusBarMod.configureClock(
                    bar.statusBarDoubleLineClockSize,
                    bar.statusBarClockTextScale,
                    if (bar.modifyDoubleLineClockGap) bar.doubleLineClockGapDp else 0f,
                    bar.useFold7CustomDoubleLineClockScale,
                    bar.fold7DoubleLineClockTimeScale,
                    bar.fold7DoubleLineClockDateScale,
                )
                // Always install the Samsung clock hook. Runtime preferences decide
                // whether to override the clock or pass through to Samsung unchanged.
                StatusBar.setStatusBarClockFormat(bar.statusBarClockFormat)

                StatusBar.installStatusBarClockTextScaleRuntime()

                StatusBar.installStatusBarClockSecondControl(
                    preference.systemUI.statusBar.updateStatusBarClockEverySecond
                )

                if (preference.systemUI.statusBar.hideSecureFolderStatusBarIcon) {
                    StatusBar.hideSecureFolderStatusBarIcon()
                }

                if (preference.systemUI.statusBar.restoreBluetoothStatusBarIcon) {
                    StatusBar.restoreBluetoothStatusBarIcon()
                }

                if (preference.systemUI.statusBar.physicalEsimAdapterWorkaround) {
                    ESIM.workaroundPhysicalEsimAdapter(
                        preference.systemUI.statusBar.physicalEsimAdapterSimSlot
                    )
                }

                if (preference.systemUI.statusBar.doubleTapStatusBarToSleep) {
                    StatusBar.doubleTapStatusBarToSleep()
                }

                if (preference.systemUI.statusBar.modifyStatusBarMaxNotificationIcons) {
                    val max = preference.systemUI.statusBar.statusBarMaxNotificationIcons
                    Notification.setStatusBarMaxNotificationIcons(max)
                }

                if (preference.systemUI.statusBar.setCustomCarrierName) {
                    StatusBar.setCustomCarrierName(preference.systemUI.statusBar.customCarrierName)
                }

                if (preference.systemUI.statusBar.hideLockscreenStatusBar) {
                    StatusBar.hideLockscreenStatusBar()
                }

                if (preference.settings.supportOutdoorMode) {
                    QS.supportOutdoorMode()
                }

                run {
                    val monospaced = preference.systemUI.qs.setQsClockMonospaced
                    val modifyTextSize = preference.systemUI.qs.modifyQSClockTextSize
                    val textSize = preference.systemUI.qs.qsClockTextSize
                    QS.setQsClockStyle(monospaced, modifyTextSize, textSize)
                }

                if (preference.systemUI.qs.hideDeviceControlQsTile) {
                    QS.hideDeviceControlQsTile()
                }

                if (preference.systemUI.qs.hideSmartViewQsTile) {
                    QS.hideSmartViewQsTile()
                }

                if (preference.systemUI.qs.turnOn5gQsTile) {
                    Network.turnOn5gQsTile()
                }

                run {
                    val qsBarSet = buildSet {
                        if (preference.systemUI.qs.hideQsBarMediaPlayer) {
                            add(QS.QsBar.MediaPlayer)
                        }
                        if (preference.systemUI.qs.hideQsBarNearbyDevicesAndDeviceControl) {
                            add(QS.QsBar.NearbyDevicesAndDeviceControl)
                        }
                        if (preference.systemUI.qs.hideQsBarSecurityFooter) {
                            add(QS.QsBar.SecurityFooter)
                        }
                        if (preference.systemUI.qs.hideQsBarDataUsage) {
                            add(QS.QsBar.DataUsage)
                        }
                        if (preference.systemUI.qs.hideQsBarSmartViewAndModes) {
                            add(QS.QsBar.SmartViewAndModes)
                        }
                    }

                    QS.hideQsBar(qsBarSet)
                }

                if (preference.systemUI.qs.alwaysExpandQsTileChunk) {
                    QS.alwaysExpandQsTileChunk()
                }

                if (preference.systemUI.qs.alwaysShowTimeDateOnQs) {
                    QS.alwaysShowTimeDateOnQs()
                }

                if (preference.systemUI.qs.addBrightnessProgressToQsBar) {
                    QS.addBrightnessProgressToQsBar()
                }

                if (preference.systemUI.qs.addVolumeProgressToQsBar) {
                    QS.addVolumeProgressToQsBar()
                }

                if (preference.systemUI.qs.showTraditionalChineseDateOnQS) {
                    QS.showTraditionalChineseDateOnQS()
                }

                if (preference.systemUI.aod.hideAODStatusBar) {
                    AOD.hideAODStatusBar()
                }

                if (preference.systemUI.aod.aodLockSupportLunar) {
                    AOD.aodLockSupportLunar()
                }

                if (preference.systemUI.notification.disableNotificationGrouping) {
                    Notification.disableNotificationGrouping()
                }
                if (preference.systemUI.notification.hideOngoingActivityMedia) {
                    Notification.hideOngoingActivityMedia(
                        preference.systemUI.notification.hideOngoingActivityMediaPackages
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toSet()
                    )
                }
                if (preference.systemUI.other.customPowerMenu) {
                    addAssetPath(moduleApplicationInfo.sourceDir)
                    PowerMenu.hookPowerMenuActions(
                        preference.systemUI.other.powerMenuActions,
                    )
                }
            }

            Package.SYSTEMUI if (processName == "${Package.SYSTEMUI}:screenshot") -> {
                if (preference.systemUI.other.disableScreenshotCaptureSound) {
                    Other.disableScreenshotCaptureSound()
                }
            }

            Package.TELEPHONYUI -> {
                if (preference.call.supportVoiceCallRecording) {
                    Call.supportVoiceCallRecording(
                        preference.call.preferRecordingButton
                    )
                }

                if (preference.systemUI.qs.turnOn5gQsTile) {
                    Network.turnOn5gQsTile()
                }
            }

            Package.THEME_CENTER -> {
                if (preference.other.setThemeTrialNeverExpired) {
                    ThemeCenter.setTrialNeverExpired()
                }
            }

            Package.WEATHER -> {
                if (preference.other.setWeatherProviderCN) {
                    Weather.setProviderCN()
                }
            }

            Package.VIDEO -> {
                if (preference.other.showMorePlaybackSpeeds) {
                    Video.showMorePlaybackSpeeds()
                }
            }

            Package.WATCH_MANAGER -> {
                if (preference.other.bypassWatchPairingRegionCheck ||
                    preference.other.watchPairingConnectionMode != WatchPairing.MODE_NONE
                ) {
                    WatchPairing.init(
                        bypassRegionCheck = preference.other.bypassWatchPairingRegionCheck,
                        connectionMode = preference.other.watchPairingConnectionMode,
                        supplementChinaWearOsGms = preference.other.supplementChinaWearOsGms
                    )
                }
            }

            Package.TRANSLATION -> {
                if (preference.other.useSPenGoogleTranslate) {
                    SPen.switchTranslateSource(useGoogle = true)
                }
            }
        }
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) = with(param) {
        if (!ENABLE_ALOHAONEUI_FUNCTIONS) return@with

        val preference = PreferenceProvider.loadPreference() ?: return@with

        if (preference.android.disableWritingToolkitGlobally) {
            Android.disableWritingToolkitGlobally()
        }

        if (preference.android.disablePinVerifyPer72h) {
            Android.disablePinVerifyPer72h()
        }

        if (preference.android.modifyMaxNeverKilledAppNum) {
            Android.setMaxNeverKilledAppNum(
                preference.android.maxNeverKilledAppNum
            )
        }

        if (preference.android.setBlockableNotificationChannel) {
            Android.setBlockableNotificationChannel()
        }

        if (preference.android.supportAppJumpBlock) {
            CoreRune.supportAppJumpBlockAndroid()
        }

        if (preference.android.allowAllRotation) {
            CoreRune.allowAllRotation()
        }

        if (preference.android.liftFcmNetworkLimit) {
            Android.liftFcmNetworkLimit()
        }

        if (preference.android.disableScreenWakeOnPowerUnplugged) {
            Android.disableScreenWakeOnPowerUnplugged()
        }

        if (preference.other.allowAppDowngradeEnabled) {
            Android.hookAllowAppDowngrade()
        }
        if (preference.other.disableSecureScreenshotRestrictionEnabled) {
            Android.hookDisableSecureScreenshotRestriction()
        }
    }
}
