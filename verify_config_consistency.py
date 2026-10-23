#!/usr/bin/env python3
"""Static regression checks for feature-count and configuration-transfer completeness."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parent


def read(path: str) -> str:
    target = ROOT / path
    if not target.is_file():
        raise AssertionError(f"Required source file is missing: {path}")
    return target.read_text(encoding="utf-8")


def section(pattern: str, content: str, label: str) -> str:
    match = re.search(pattern, content, re.MULTILINE | re.DOTALL)
    if not match:
        raise AssertionError(f"Could not locate {label}")
    return match.group(1)


repository = read("app/src/main/java/com/alohaoneui/app/data/repository/SettingsRepository.kt")
repository_impl = read("app/src/main/java/com/alohaoneui/app/data/repository/SettingsRepositoryImpl.kt")
catalog = read("app/src/main/java/com/alohaoneui/app/alohaoneui/AlohaOneUIFeatureCatalog.kt")
view_model = read("app/src/main/java/com/alohaoneui/app/ui/viewmodel/AlohaOneUIToolsViewModel.kt")
bridge = read("app/src/main/java/com/alohaoneui/app/alohaoneui/AlohaOneUIPreferenceBridge.kt")
popup = read("app/src/main/java/com/alohaoneui/app/ui/component/configtransfer/ConfigTransferPopup.kt")
ui_state = read("app/src/main/java/com/alohaoneui/app/ui/screen/settings/SettingsUiState.kt")
other_controls = read("app/src/main/java/com/alohaoneui/app/ui/screen/tools/OtherFunctionsControls.kt")
english_strings = read("app/src/main/res/values/alohaoneui_strings.xml")
chinese_strings = read("app/src/main/res/values-zh-rCN/alohaoneui_strings.xml")

interface_body = section(
    r"interface SettingsRepository\s*\{(.*?)\n\}",
    repository,
    "SettingsRepository interface",
)
interface_fields = re.findall(r"^\s*var\s+(\w+)\s*:", interface_body, re.MULTILINE)
backup_body = section(
    r"data class AppSettingsBackup\s*\((.*?)\n\)",
    repository,
    "AppSettingsBackup fields",
)
backup_fields = re.findall(r"^\s*val\s+(\w+)\s*:", backup_body, re.MULTILINE)
if interface_fields != backup_fields:
    raise AssertionError(
        "App settings backup fields no longer exactly match SettingsRepository: "
        f"repository={interface_fields}, backup={backup_fields}"
    )

snapshot_body = section(
    r"override fun snapshotForBackup\(\): AppSettingsBackup\s*=\s*AppSettingsBackup\((.*?)\n    \)",
    repository_impl,
    "app-settings snapshot",
)
for field in interface_fields:
    if not re.search(rf"\b{re.escape(field)}\s*=\s*{re.escape(field)}\b", snapshot_body):
        raise AssertionError(f"App-settings backup snapshot omits '{field}'")

restore_body = section(
    r"override fun restoreFromBackup\(snapshot: AppSettingsBackup\)\s*\{(.*?)\n    \}",
    repository_impl,
    "app-settings restore",
)
settings_keys = {
    "uiMode": "ui_mode",
    "checkUpdate": "check_update",
    "themeMode": "color_mode",
    "miuixMonet": "miuix_monet",
    "keyColor": "key_color",
    "colorStyle": "color_style",
    "colorSpec": "color_spec",
    "enablePredictiveBack": "enable_predictive_back",
    "enableBlur": "enable_blur",
    "enableFloatingBottomBar": "enable_floating_bottom_bar",
    "enableFloatingBottomBarBlur": "enable_floating_bottom_bar_blur",
    "navigationRailExpanded": "nav_rail_expanded",
    "pageScale": "page_scale",
    "enableSwipeBack": "enable_swipe_back",
    "pagerGestureMode": "pager_gesture_mode",
}
for field, key in settings_keys.items():
    if field not in interface_fields or f'"{key}"' not in restore_body:
        raise AssertionError(f"App-settings restore does not persist '{field}' ({key})")

count_body = section(
    r"fun enabledInjectionCount\(preference: Preference\): Int \{(.*?)\n    fun settings\(category",
    catalog,
    "homepage feature-count implementation",
)
other_function_flags = (
    "callReturnDelayEnabled",
    "settingsTwoPaneEnabled",
    "allowAppDowngradeEnabled",
    "disableSecureScreenshotRestrictionEnabled",
    "hidePackageInstallerRecommendationEnabled",
)
shortcut_flags = (
    "threeFingerGestureEnabled",
    "threeFingerPinchEnabled",
    "threeFingerSpreadEnabled",
    "floatingBallEnabled",
    "statusBarShortcutEnabled",
    "navigationKeyShortcutEnabled",
    "screenOffVolumeKeyEnabled",
)
for flag in other_function_flags:
    if f"other.{flag}" not in count_body:
        raise AssertionError(f"Homepage count omits Other functions setting '{flag}'")
for flag in shortcut_flags:
    if f"shortcuts.{flag}" not in count_body:
        raise AssertionError(f"Homepage count omits global shortcut trigger '{flag}'")
for key in (
    "systemUI.other.customPowerMenu",
    "systemUI.other.disableScreenshotCaptureSound",
):
    if key not in catalog:
        raise AssertionError(f"Existing catalog count entry was unexpectedly removed: {key}")

if "preferences = configurationBackupJson.encodeToJsonElement(" not in view_model:
    raise AssertionError("New backup format must encode Preference with defaults included")
if "appSettings = appSettingsRepository.snapshotForBackup()" not in view_model:
    raise AssertionError("Configuration export omits app-level SettingsRepository preferences")
if "bundle?.let { appSettingsRepository.restoreFromBackup(it.appSettings) }" not in view_model:
    raise AssertionError("Configuration restore omits app-level settings")
if "ProductionPreferenceMigration.decode(bundle.preferences.toString())" not in view_model:
    raise AssertionError("Versioned configuration restore bypasses production preference migration")
if "ProductionPreferenceMigration.decode(raw)" not in view_model:
    raise AssertionError("Legacy raw Preference backup compatibility was removed")
if "synchronizeRestoredConfiguration(alohaOneUIApp)" not in view_model:
    raise AssertionError("Restore must use the explicit full-configuration synchronization path")
if bridge.count("suspend fun synchronizeRestoredConfiguration(application: Application): Boolean") != 2:
    raise AssertionError("Full-configuration restore sync must exist and be exposed by the bridge facade")
if "return AlohaOneUIXposedService.synchronizeRestoredConfiguration(application)" not in bridge:
    raise AssertionError("Restore sync facade does not delegate to the implementation")
if "val enableBlur: Boolean = false" not in ui_state:
    raise AssertionError("SettingsUiState blur default no longer matches SettingsRepositoryImpl")

feedback_strings = (
    "alohaoneui_backup_succeeded",
    "alohaoneui_backup_failed",
    "alohaoneui_restore_succeeded",
    "alohaoneui_restore_local_only",
)
for name in feedback_strings:
    if name not in popup:
        raise AssertionError(f"Config transfer UI does not handle message '{name}'")
    if f'name="{name}"' not in english_strings:
        raise AssertionError(f"English resource missing '{name}'")
    if f'name="{name}"' not in chinese_strings:
        raise AssertionError(f"Simplified Chinese resource missing '{name}'")

print(
    "Aloha OneUI configuration consistency checks passed: "
    f"{len(interface_fields)} app settings, {len(other_function_flags)} extra Other functions, "
    f"{len(shortcut_flags)} global shortcut triggers, legacy backup compatibility, and sync facade."
)
