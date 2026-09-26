package com.oneuixp.app.ui.screen.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
        resolveString(context, listOf(key)) ?: category.fallbackTitle
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
internal fun rememberOneuiXCategoryIcon(category: OneuiXCategory): ImageBitmap {
    val context = LocalContext.current
    return remember(category) {
        val drawable = runCatching {
            context.packageManager.getApplicationIcon(category.packageName)
        }.getOrElse {
            requireNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
        }
        drawable.toBitmap(width = 128, height = 128).asImageBitmap()
    }
}

private fun settingSourceKey(path: String, raw: String): String = when (path) {
    "android.maxNeverKilledAppNum" -> "modifyMaxNeverKilledAppNum"
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
