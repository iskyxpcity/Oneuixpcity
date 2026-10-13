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

                // The native status-bar icons already supply their own padding.
                // Remove only the former 1dp extra inset; preserve larger custom values.
                val glyphLeftMarginZeroDpMigrationDone =
                    (updatedComboRuntime["glyphLeftMarginZeroDpMigrationDone"] as? JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true
                if (!glyphLeftMarginZeroDpMigrationDone) {
                    val oldLeftMargin = updatedComboRuntime["glyphLeftMarginDp"]?.toString()?.toIntOrNull() ?: 0
                    if (oldLeftMargin == 1) {
                        updatedComboRuntime["glyphLeftMarginDp"] = JsonPrimitive(0)
                    }
                    updatedComboRuntime["glyphLeftMarginZeroDpMigrationDone"] = JsonPrimitive(true)
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

                // The 4dp setting still looks too tight on the device. Increase only
                // the outside-signal-to-ring gap; keep the composite icon's own
                // left margin independent and preserve manually chosen other values.
                val outsideSignalMarginSixDpMigrationDone =
                    (updatedComboRuntime["outSignalMarginSixDpMigrationDone"] as? JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true
                if (!outsideSignalMarginSixDpMigrationDone) {
                    val oldOutsideSignalMargin =
                        updatedComboRuntime["outSignalMarginDp"]?.toString()?.toIntOrNull() ?: 6
                    if (oldOutsideSignalMargin == 4) {
                        updatedComboRuntime["outSignalMarginDp"] = JsonPrimitive(6)
                    }
                    updatedComboRuntime["outSignalMarginSixDpMigrationDone"] = JsonPrimitive(true)
                    comboRuntimeChanged = true
                }

                // 8dp is the first clearly separated appearance confirmed on device.
                // Migrate only the former 6dp default; leave other custom spacing untouched.
                val outsideSignalMarginEightDpMigrationDone =
                    (updatedComboRuntime["outSignalMarginEightDpMigrationDone"] as? JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true
                if (!outsideSignalMarginEightDpMigrationDone) {
                    val oldOutsideSignalMargin =
                        updatedComboRuntime["outSignalMarginDp"]?.toString()?.toIntOrNull() ?: 8
                    if (oldOutsideSignalMargin == 6) {
                        updatedComboRuntime["outSignalMarginDp"] = JsonPrimitive(8)
                    }
                    updatedComboRuntime["outSignalMarginEightDpMigrationDone"] = JsonPrimitive(true)
                    comboRuntimeChanged = true
                }

                // Complete the one-time ring-layout migration without erasing user-controlled overall-size values.
                val ringTrailingAnchorMigrationDone =
                    (updatedComboRuntime["ringTrailingAnchorMigrationDone"] as? JsonPrimitive)
                        ?.content?.toBooleanStrictOrNull() == true
                if (!ringTrailingAnchorMigrationDone) {
                    // Do not reset whole-glyph offset or margin values here. These controls
                    // remain part of the Overall Size group and are user-adjustable.
                    // Only replace the known untouched default tuple, keeping custom values intact.
                    val oldSignalGap = updatedComboRuntime["outSignalMarginDp"]?.toString()?.toIntOrNull() ?: 8
                    val oldTypeLeft = updatedComboRuntime["outTypeMarginLeftDp"]?.toString()?.toIntOrNull() ?: 2
                    val oldTypeRight = updatedComboRuntime["outTypeMarginRightDp"]?.toString()?.toIntOrNull() ?: 2
                    val oldSignalLeft = updatedComboRuntime["outSignalMarginLeftDp"]?.toString()?.toIntOrNull() ?: 0
                    val elementOffsetsZero = listOf(
                        "outTypeOffsetXDp", "outTypeOffsetYDp",
                        "outSignalOffsetXDp", "outSignalOffsetYDp",
                    ).all { key -> (updatedComboRuntime[key]?.toString()?.toIntOrNull() ?: 0) == 0 }
                    if (oldSignalGap == 8 && oldTypeLeft == 2 && oldTypeRight == 2 &&
                        oldSignalLeft == 0 && elementOffsetsZero
                    ) {
                        updatedComboRuntime["outSignalMarginDp"] = JsonPrimitive(0)
                        updatedComboRuntime["outTypeMarginLeftDp"] = JsonPrimitive(0)
                        updatedComboRuntime["outTypeMarginRightDp"] = JsonPrimitive(0)
                    }
                    updatedComboRuntime["ringTrailingAnchorMigrationDone"] = JsonPrimitive(true)
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
