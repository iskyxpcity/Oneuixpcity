package com.alohaoneui.common

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Upstream 1.8 moved four production MOD values from systemUI.other to notification.
 * Keep the old keys and their actual values; only fill fields not already set in the new schema.
 */
object ProductionPreferenceMigration {
    private val notificationKeys = listOf(
        "autoExpandNotifications",
        "disableNotificationGrouping",
        "hideOngoingActivityMedia",
        "hideOngoingActivityMediaPackages",
    )

    fun decode(raw: String): Preference {
        val root = IgnoreUnknownKeysJson.parseToJsonElement(raw) as? JsonObject
            ?: return IgnoreUnknownKeysJson.decodeFromString<Preference>(raw)
        val systemUI = root["systemUI"] as? JsonObject
            ?: return IgnoreUnknownKeysJson.decodeFromString<Preference>(raw)
        val oldOther = systemUI["other"] as? JsonObject ?: JsonObject(emptyMap())
        val existingNotification = systemUI["notification"] as? JsonObject ?: JsonObject(emptyMap())
        val updatedNotification = existingNotification.toMutableMap()
        notificationKeys.forEach { key ->
            if (key !in updatedNotification) {
                oldOther[key]?.let { updatedNotification[key] = it }
            }
        }

        val updatedSystemUI = systemUI.toMutableMap()
        var changed = false
        if (updatedNotification != existingNotification) {
            updatedSystemUI["notification"] = JsonObject(updatedNotification)
            changed = true
        }

        val statusBar = systemUI["statusBar"] as? JsonObject
        if (statusBar != null) {
            val updatedStatusBar = statusBar.toMutableMap()
            if ("modifyStatusBarTopPadding" !in updatedStatusBar) {
                val oldTop = statusBar["statusBarTopPaddingDp"]?.toString()?.toFloatOrNull() ?: 0f
                updatedStatusBar["modifyStatusBarTopPadding"] = JsonPrimitive(oldTop != 0f)
            }
            if ("modifyStatusBarBottomPadding" !in updatedStatusBar) {
                val oldBottom = statusBar["statusBarBottomPaddingDp"]?.toString()?.toFloatOrNull() ?: 0f
                updatedStatusBar["modifyStatusBarBottomPadding"] = JsonPrimitive(oldBottom != 0f)
            }
            if ("modifyDoubleLineClockGap" !in updatedStatusBar) {
                val oldGap = statusBar["doubleLineClockGapDp"]?.toString()?.toFloatOrNull() ?: 1f
                updatedStatusBar["modifyDoubleLineClockGap"] = JsonPrimitive(oldGap != 1f)
            }
            // The composite glyph owns its defaults and stored values. Do not run component-specific
            // migrations from this shared preference decoder.
            if (updatedStatusBar != statusBar) {
                updatedSystemUI["statusBar"] = JsonObject(updatedStatusBar)
                changed = true
            }
        }

        if (!changed) {
            return IgnoreUnknownKeysJson.decodeFromString<Preference>(raw)
        }
        val updatedRoot = root.toMutableMap()
        updatedRoot["systemUI"] = JsonObject(updatedSystemUI)
        return IgnoreUnknownKeysJson.decodeFromString<Preference>(JsonObject(updatedRoot).toString())
    }

    /** Production main's local DataStore has no sync marker. An experimental
     * upstream remote preference file must not silently replace that original
     * MOD configuration during the first upgrade. Once synchronized, remote
     * settings become authoritative except for explicit offline local edits.
     */
    fun select(
        local: Preference?,
        remote: Preference?,
        localUnsynced: Boolean,
        localFromProduction: Boolean,
    ): Preference {
        val defaults = Preference()
        return when {
            localUnsynced && local != null -> local
            localFromProduction && local != null && local != defaults -> local
            remote != null && remote != defaults -> remote
            local != null && local != defaults -> local
            remote != null -> remote
            local != null -> local
            else -> defaults
        }
    }
}
