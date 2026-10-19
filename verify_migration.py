#!/usr/bin/env python3
"""Read-only checks; never mistake source presence for a successful build."""
from pathlib import Path
import sys
root = Path(__file__).resolve().parent
migration_source = (root/'common/src/main/java/io/github/soclear/oneuix/common/ProductionPreferenceMigration.kt').read_text()
preference_source = (root/'common/src/main/java/io/github/soclear/oneuix/common/Preference.kt').read_text()
combo_runtime_source = (root/'hook/src/main/java/io/github/soclear/oneuix/hook/systemui/StatusBarComboStyleOneRuntime.kt').read_text()
network_speed_runtime_source = (root/'hook/src/main/java/io/github/soclear/oneuix/hook/systemui/StatusBarNetworkSpeedRuntime.kt').read_text()
independent_signal_runtime_source = (root/'hook/src/main/java/io/github/soclear/oneuix/hook/systemui/StatusBarSignalIndependentRuntime.kt').read_text()
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
 'Flux-style no heartbeat bridge': not (root/'app/src/main/java/com/oneuixp/app/statusbar/StatusBridge.java').exists() and '${applicationId}.statusbridge' not in (root/'app/src/main/AndroidManifest.xml').read_text() and 'startHeartbeat(' not in (root/'hook/src/main/java/io/github/soclear/oneuix/hook/util/OneuiXPStatusReporter.kt').read_text(),
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
 'Network speed leaves native horizontal margins to Samsung SystemUI': (
     'NETWORK_SPEED_TRAILING_GAP_DP' not in network_speed_runtime_source
     and 'bindTrailingGapHost' not in network_speed_runtime_source
     and 'applyHostTrailingGap' not in network_speed_runtime_source
     and 'reapplyHostTrailingGap' not in network_speed_runtime_source
     and 'params.marginEnd = targetEnd' not in network_speed_runtime_source
     and 'params.marginStart = baseStart' not in network_speed_runtime_source
 )
}
for name, ok in checks.items(): print(('PASS' if ok else 'FAIL') + ': ' + name)
if not all(checks.values()):
 print('BLOCKED: source preflight failed; do not claim APK build success.', file=sys.stderr)
 sys.exit(1)
print('Source preflight passed; Android build and phone tests are still required.')
