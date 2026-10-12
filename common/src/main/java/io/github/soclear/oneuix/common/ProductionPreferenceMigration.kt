package io.github.soclear.oneuix.common

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
            // One-time migrations for the Three-in-One size defaults.
            val comboRuntime = statusBar["comboStyleOneRuntime"] as? JsonObject
            if (comboRuntime != null) {
                val updatedComboRuntime = comboRuntime.toMutableMap()
                var comboRuntimeChanged = false

                val newDefaultMigrationDone =
                    (updatedComboRuntime["glyphScaleDefault100MigrationDone"] as? JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true
                if (!newDefaultMigrationDone) {
                    val priorMigrationDone =
                        (updatedComboRuntime["glyphScaleDefaultMigrationDone"] as? JsonPrimitive)
                            ?.content?.toBooleanStrictOrNull() == true
                    val oldScale = updatedComboRuntime["glyphScalePercent"]?.toString()?.toIntOrNull()
                    if (priorMigrationDone && oldScale == 90) {
                        updatedComboRuntime["glyphScalePercent"] = JsonPrimitive(100)
                    }
                    updatedComboRuntime["glyphScaleDefaultMigrationDone"] = JsonPrimitive(true)
                    updatedComboRuntime["glyphScaleDefault100MigrationDone"] = JsonPrimitive(true)
                    comboRuntimeChanged = true
                }

                // Give the combined icon a small 1dp left inset to complement
                // the native status icons' own spacing. Migrate only the old
                // zero default; preserve intentionally larger custom values.
                val glyphLeftMarginOneDpMigrationDone =
                    (updatedComboRuntime["glyphLeftMarginOneDpMigrationDone"] as? JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true
                if (!glyphLeftMarginOneDpMigrationDone) {
                    val oldLeftMargin = updatedComboRuntime["glyphLeftMarginDp"]?.toString()?.toIntOrNull() ?: 1
                    if (oldLeftMargin == 0) {
                        updatedComboRuntime["glyphLeftMarginDp"] = JsonPrimitive(1)
                    }
                    updatedComboRuntime["glyphLeftMarginOneDpMigrationDone"] = JsonPrimitive(true)
                    comboRuntimeChanged = true
                }

                val marginResetTo2MigrationDone =
                    (updatedComboRuntime["outSignalMarginReset2MigrationDone"] as? JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true
                if (!marginResetTo2MigrationDone) {
                    val previousBuild1406MarginMigration =
                        (updatedComboRuntime["outSignalMarginDefaultMigrationDone"] as? JsonPrimitive)
                            ?.content?.toBooleanStrictOrNull() == true
                    val oldMargin = updatedComboRuntime["outSignalMarginDp"]?.toString()?.toIntOrNull() ?: 2
                    // Build #1406 mistakenly migrated the old 2dp default to 8dp.
                    // Undo only that value once; preserve other intentionally chosen margins.
                    if (previousBuild1406MarginMigration && oldMargin == 8) {
                        updatedComboRuntime["outSignalMarginDp"] = JsonPrimitive(2)
                    }
                    updatedComboRuntime["outSignalMarginReset2MigrationDone"] = JsonPrimitive(true)
                    comboRuntimeChanged = true
                }

                // Build #43 showed the old 2dp outside-signal gap was visually
                // too tight on the ring layout. Raise the untouched default to 4dp,
                // while preserving any other user-selected spacing.
                val outsideSignalMarginFourDpMigrationDone =
                    (updatedComboRuntime["outSignalMarginFourDpMigrationDone"] as? JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true
                if (!outsideSignalMarginFourDpMigrationDone) {
                    val oldOutsideSignalMargin =
                        updatedComboRuntime["outSignalMarginDp"]?.toString()?.toIntOrNull() ?: 4
                    if (oldOutsideSignalMargin == 2) {
                        updatedComboRuntime["outSignalMarginDp"] = JsonPrimitive(4)
                    }
                    updatedComboRuntime["outSignalMarginFourDpMigrationDone"] = JsonPrimitive(true)
                    comboRuntimeChanged = true
                }

                if (comboRuntimeChanged) {
                    updatedStatusBar["comboStyleOneRuntime"] = JsonObject(updatedComboRuntime)
                }
            }
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
