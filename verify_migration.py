#!/usr/bin/env python3
"""Read-only checks; never mistake source presence for a successful build."""
from pathlib import Path
import re
import sys
root = Path(__file__).resolve().parent
migration_source = (root/'common/src/main/java/com/oneuixp/common/ProductionPreferenceMigration.kt').read_text()
preference_source = (root/'common/src/main/java/com/oneuixp/common/Preference.kt').read_text()
combo_runtime_source = (root/'hook/src/main/java/com/oneuixp/hook/systemui/StatusBarComboStyleOneRuntime.kt').read_text()
network_speed_runtime_source = (root/'hook/src/main/java/com/oneuixp/hook/systemui/StatusBarNetworkSpeedRuntime.kt').read_text()
independent_signal_runtime_source = (root/'hook/src/main/java/com/oneuixp/hook/systemui/StatusBarSignalIndependentRuntime.kt').read_text()
feature_intro_source = (root/'app/src/main/java/com/oneuixp/app/ui/screen/about/FeatureIntroductionScreens.kt').read_text()
feature_routes_source = (root/'app/src/main/java/com/oneuixp/app/ui/navigation3/Routes.kt').read_text()
main_activity_source = (root/'app/src/main/java/com/oneuixp/app/ui/MainActivity.kt').read_text()
release_note_resources = [
    (root/'app/src/main/res/values/strings.xml').read_text(),
    (root/'app/src/main/res/values-zh-rCN/strings.xml').read_text(),
    (root/'app/src/main/res/values-zh-rHK/strings.xml').read_text(),
    (root/'app/src/main/res/values-zh-rTW/strings.xml').read_text(),
]
legacy_combo_markers = (
    'glyphScaleDefaultMigrationDone', 'glyphScaleDefault100MigrationDone',
    'glyphLeftMarginOneDpMigrationDone', 'glyphLeftMarginZeroDpMigrationDone',
    'glyphLeftMarginRestoreOneDpMigrationDone', 'glyphLeftMarginTwoDpDefaultMigrationDone',
    'glyphLeftMarginHalfDpMigrationDone', 'glyphLeftMarginZeroFinalMigrationDone',
    'outSignalMarginDefaultMigrationDone', 'outSignalMarginReset2MigrationDone',
    'outSignalMarginFourDpMigrationDone', 'outSignalMarginSixDpMigrationDone',
    'outSignalMarginEightDpMigrationDone', 'ringTrailingAnchorMigrationDone',
)
checks = {
 'Gradle wrapper': (root/'gradlew').is_file(),
 'Original Miuix home': (root/'app/src/main/java/com/oneuixp/app/ui/screen/home/HomeMiuix.kt').is_file(),
 'Original Miuix bottom bar': (root/'app/src/main/java/com/oneuixp/app/ui/component/bottombar/BottomBarMiuix.kt').is_file(),
 'Legacy native build disabled': 'externalNativeBuild' not in (root/'app/build.gradle.kts').read_text(),
 'Original LSPosed hook': (root/'app/src/main/java/com/oneuixp/app/statusbar/SystemUiEntry.java').is_file(),
 'Flux-style no heartbeat bridge': not (root/'app/src/main/java/com/oneuixp/app/statusbar/StatusBridge.java').exists() and '${applicationId}.statusbridge' not in (root/'app/src/main/AndroidManifest.xml').read_text() and 'startHeartbeat(' not in (root/'hook/src/main/java/com/oneuixp/hook/util/OneuiXPStatusReporter.kt').read_text(),
 'OneuiXP application ID': 'com.oneuixp.app' in (root/'app/build.gradle.kts').read_text(),
 'Composite glyph is isolated from legacy preference migration': 'comboStyleOneRuntime' not in migration_source and not any(marker in migration_source or marker in preference_source for marker in legacy_combo_markers),
 'Native mobile signal is restored when outside stacking is off': (
     'config.signalMode == 0 || (config.signalMode == 1 && config.stackedSignal)' in combo_runtime_source
     and 'if (!foldMobile) StatusBarSignalIndependentRuntime.restoreNativeSignalViewsForCombo(root)' in combo_runtime_source
     and 'internal fun restoreNativeSignalViewsForCombo(statusBarRoot: ViewGroup)' in independent_signal_runtime_source
 ),
 'Native mobile release does not reapply a stale transparent alpha snapshot': (
     'preserveCurrentAlpha: Boolean = !isMobileNativeSuppressed() && isMobileIcon(view)' in combo_runtime_source
     and 'snapshot.copy(alpha = view.alpha)' in combo_runtime_source
     and 'preserveCurrentAlpha = !foldMobile && isMobileIcon(view)' in combo_runtime_source
 ),
 'Native mobile slot prevents empty status-icon cluster width clamping': (
     'nativeMobileSlotExpected || hasVisibleNativeClusterIconFootprint(cluster)' in combo_runtime_source
     and 'hostFor(cluster) == null || nativeMobileSlotExpected ||' in combo_runtime_source
     and 'val nativeMobileSlotExpected = !isMobileNativeSuppressed()' in combo_runtime_source
 ),
 'Container relayout returns native mobile ownership when unsuppressed': (
     'preserveCurrentAlpha: Boolean = !isMobileNativeSuppressed() && isMobileIcon(view)' in combo_runtime_source
 ),
 'Hidden native signal slots clear all margins and restore exact layout': (
     'collapseNativeSignalSlot(view)' in independent_signal_runtime_source
     and 'params.width = 0' in independent_signal_runtime_source
     and 'params.height = 0' in independent_signal_runtime_source
     and 'margins.setMargins(0, 0, 0, 0)' in independent_signal_runtime_source
     and 'margins.setMarginStart(Int.MIN_VALUE)' in independent_signal_runtime_source
     and 'margins.setMarginEnd(Int.MIN_VALUE)' in independent_signal_runtime_source
     and 'original.hasRelativeMargins' in independent_signal_runtime_source
     and 'view.visibility = View.GONE' in independent_signal_runtime_source
 ),
 'Independent signal runtime defers layout slots already owned by composite glyph': (
     'StatusBarComboStyleOneRuntime.isNativeLayoutOwned(view)' in independent_signal_runtime_source
     and 'internal fun isNativeLayoutOwned(view: View)' in combo_runtime_source
     and 'host.layouts.containsKey(view)' in combo_runtime_source
 ),
 'Composite hidden native slots clear relative margins and restore their baseline': (
     'margins.setMarginStart(Int.MIN_VALUE)' in combo_runtime_source
     and 'margins.setMarginEnd(Int.MIN_VALUE)' in combo_runtime_source
     and 'snapshot.hasRelativeMargins' in combo_runtime_source
 ),
 'Composite renderer collapses standalone native signal leaves': (
     '(name.contains("signal") && v !is ViewGroup)' in combo_runtime_source
 ),
 'Status icon row releases weighted empty space and restores original parent params': (
     'compactNativeStatusIconParentSlot(cluster)' in combo_runtime_source
     and 'width = ViewGroup.LayoutParams.WRAP_CONTENT' in combo_runtime_source
     and 'weight = 0f' in combo_runtime_source
     and 'nativeWifiClusterBaselineParentLayoutParams' in combo_runtime_source
     and 'baselineParentLayoutParams != null' in combo_runtime_source
 ),
 'Status icon row trims trailing residue but preserves visible native icons': (
     'val targetRight = 0' in combo_runtime_source
     and 'val targetMinimumWidth = 0' in combo_runtime_source
     and 'native-icon-present-trim-trailing-space' in combo_runtime_source
     and 'baselineParentLayoutParams != null' in combo_runtime_source
 ),
 'Network speed leaves native horizontal margins to Samsung SystemUI': (
     'NETWORK_SPEED_TRAILING_GAP_DP' not in network_speed_runtime_source
     and 'bindTrailingGapHost' not in network_speed_runtime_source
     and 'applyHostTrailingGap' not in network_speed_runtime_source
     and 'reapplyHostTrailingGap' not in network_speed_runtime_source
     and 'params.marginEnd = targetEnd' not in network_speed_runtime_source
     and 'params.marginStart = baseStart' not in network_speed_runtime_source
 ),
 '1.0.261019.1 release notes appear first in both feature-intro themes': (
     feature_intro_source.index('feature_intro_version_updates_2610191')
     < feature_intro_source.index('feature_intro_version_updates_261018')
     and 'onOpen2610191Updates' in feature_intro_source
     and 'is2610191 -> R.string.version_1_0_2610191_title' in feature_intro_source
     and 'is2610191 -> R.string.version_1_0_2610191_summary' in feature_intro_source
     and feature_intro_source.count('is2610191 -> listOf(') == 2
 ),
 '1.0.261019.1 changelog route is registered and reachable': (
     'data object VersionChangelog2610191' in feature_routes_source
     and 'entry<Route.VersionChangelog2610191>' in main_activity_source
     and 'VersionChangelog2610191Screen()' in main_activity_source
 ),
 'Release note titles and all three update sections are localized': (
     all(
         all(f'name="{key}"' in resource for key in (
             'feature_intro_version_updates_2610191',
             'feature_intro_version_updates_2610191_summary',
             'version_1_0_2610191_title',
             'version_1_0_2610191_summary',
             'changelog_2610191_new_features_title',
             'changelog_2610191_new_features_body',
             'changelog_2610191_optimizations_title',
             'changelog_2610191_optimizations_body',
             'changelog_2610191_fixes_title',
             'changelog_2610191_fixes_body',
         ))
         for resource in release_note_resources
     )
 )
}

# Construct legacy markers in fragments so this checker does not flag its own code.
old_namespace = "io.github." + "soclear"
legacy_owner_token = "soclear"
legacy_project_segment = "oneuix"
old_token = "RELEASE_REPO_" + "TOKEN"
old_brand = re.compile(r"(?:OneuiX|OneUIX|oneuix)P[a-z]")
legacy_name_pattern = re.compile(r"(?i)oneuix(?!p)")
ignored_dirs = {".git", ".gradle", "build"}
namespace_hits = []
brand_hits = []
legacy_name_hits = []
owner_hits = []
legacy_segment_hits = []
token_hits = []

for path in root.rglob("*"):
    if not path.is_file():
        continue
    rel = path.relative_to(root).as_posix()
    if any(part in ignored_dirs for part in path.parts):
        continue
    if "LICENSE" in path.name.upper():
        continue
    if rel == "verify_migration.py":
        continue

    if old_namespace.lower() in rel.lower() or old_namespace.replace(".", "/").lower() in rel.lower():
        namespace_hits.append(rel)
    if legacy_owner_token in rel.lower():
        owner_hits.append(rel)
    if re.search(r"(?:^|/)oneuix(?:/|$)", rel, re.IGNORECASE):
        legacy_segment_hits.append(rel)
    if old_brand.search(rel):
        brand_hits.append(rel)
    if legacy_name_pattern.search(rel):
        legacy_name_hits.append(rel)

    try:
        data = path.read_bytes()
        if b"\x00" in data:
            continue
        text = data.decode("utf-8")
    except (UnicodeDecodeError, OSError):
        continue

    if old_namespace.lower() in text.lower() or old_namespace.replace(".", "/").lower() in text.lower():
        namespace_hits.append(rel)
    if legacy_owner_token in text.lower():
        owner_hits.append(rel)
    if re.search(r"(?i)(?:^|[./\\])oneuix(?:$|[./\\])", text):
        legacy_segment_hits.append(rel)
    if old_brand.search(text):
        brand_hits.append(rel)
    if legacy_name_pattern.search(text):
        legacy_name_hits.append(rel)
    if old_token in text:
        token_hits.append(rel)

checks["No legacy namespace outside license texts"] = not namespace_hits
checks["No legacy soclear ownership token outside license texts"] = not owner_hits
checks["No standalone legacy oneuix namespace/path segment outside license texts"] = not legacy_segment_hits
checks["No old OneUIX token outside license texts"] = not brand_hits
checks["No legacy oneuix naming token outside license texts"] = not legacy_name_hits
checks["No cross-repository release token reference"] = not token_hits

for name, ok in checks.items(): print(('PASS' if ok else 'FAIL') + ': ' + name)
if not all(checks.values()):
 for label, items in (("namespace", namespace_hits), ("socl-ear", owner_hits), ("oneuix path segment", legacy_segment_hits), ("old oneuix name", legacy_name_hits), ("brand", brand_hits), ("release token", token_hits)):
  if items: print(f"Residual {label} references: " + ", ".join(sorted(set(items))), file=sys.stderr)
 print('BLOCKED: source preflight failed; do not claim APK build success.', file=sys.stderr)
 sys.exit(1)
print('Source preflight passed; Android build and phone tests are still required.')
