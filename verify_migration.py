#!/usr/bin/env python3
"""Read-only checks; never mistake source presence for a successful build."""
from pathlib import Path
import sys
root = Path(__file__).resolve().parent
checks = {
 'Gradle wrapper': (root/'gradlew').is_file(),
 'Original Miuix home': (root/'app/src/main/java/me/weishu/kernelsu/ui/screen/home/HomeMiuix.kt').is_file(),
 'Original Miuix bottom bar': (root/'app/src/main/java/me/weishu/kernelsu/ui/component/bottombar/BottomBarMiuix.kt').is_file(),
 'KernelSU CMake build disabled': 'externalNativeBuild' not in (root/'app/build.gradle.kts').read_text(),
 'Original LSPosed hook': (root/'app/src/main/java/me/weishu/kernelsu/statusbar/SystemUiEntry.java').is_file(),
 'OneuiXP bridge authority': 'BuildConfig.APPLICATION_ID + ".statusbridge"' in (root/'app/src/main/java/me/weishu/kernelsu/statusbar/SystemUiEntry.java').read_text() and '${applicationId}.statusbridge' in (root/'app/src/main/AndroidManifest.xml').read_text(),
 'OneuiXP application ID': 'com.oneuixp.app' in (root/'app/build.gradle.kts').read_text(),
}
for name, ok in checks.items(): print(('PASS' if ok else 'FAIL') + ': ' + name)
if not all(checks.values()):
 print('BLOCKED: source preflight failed; do not claim APK build success.', file=sys.stderr)
 sys.exit(1)
print('Source preflight passed; Android build and phone tests are still required.')
