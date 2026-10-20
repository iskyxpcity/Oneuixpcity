#!/usr/bin/env python3
"""Identity migration guards for OneUI Aloha; source checks are not build/device validation."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parent
EXPECTED_APP_ID = "com.oneuialoha.app"
EXPECTED_HOOK_ENTRY = "com.oneuialoha.hook.Main"
EXPECTED_OWNER_CERT = "93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1"

checks = {}
problems = []

def check(label, ok, detail=""):
    checks[label] = bool(ok)
    if not ok:
        problems.append(f"{label}: {detail or 'failed'}")

root_gradle = (ROOT / "build.gradle.kts").read_text()
gradle_app = (ROOT / "app/build.gradle.kts").read_text()
gradle_common = (ROOT / "common/build.gradle.kts").read_text()
gradle_hook = (ROOT / "hook/build.gradle.kts").read_text()
gradle_stub = (ROOT / "stub/build.gradle.kts").read_text()
gradle_props = (ROOT / "gradle.properties").read_text()
settings = (ROOT / "settings.gradle.kts").read_text()
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
hook_manifest = (ROOT / "hook/src/main/AndroidManifest.xml").read_text()
entry_file = ROOT / "app/src/main/resources/META-INF/xposed/java_init.list"
module_file = ROOT / "app/src/main/resources/META-INF/xposed/module.prop"
scope_file = ROOT / "app/src/main/resources/META-INF/xposed/scope.list"
asset_scope_file = ROOT / "app/src/main/assets/xposed_scope"
downloader = (ROOT / "app/src/main/java/com/oneuialoha/app/ui/util/Downloader.kt").read_text()
workflow = (ROOT / ".github/workflows/build.yml").read_text()

check("App namespace and default application ID are Aloha",
      f'namespace = "{EXPECTED_APP_ID}"' in gradle_app
      and f'val defaultManagerPackageName = "{EXPECTED_APP_ID}"' in gradle_app)
check("Common, Hook and Stub namespaces are Aloha",
      'namespace = "com.oneuialoha.common"' in gradle_common
      and 'namespace = "com.oneuialoha.hook"' in gradle_hook
      and 'namespace = "com.oneuialoha.stub"' in gradle_stub)
check("Root project name is Aloha", 'rootProject.name = "OneUIAloha"' in settings)
check("Root Gradle consumes the Aloha versionCode property",
      'findProperty("oneuiAlohaVersionCode")' in root_gradle
      and 'findProperty("oneuixpVersionCode")' not in root_gradle)
check("Gradle version properties are explicit",
      "oneuiAlohaVersionName=1.0.261021.1" in gradle_props
      and "oneuiAlohaVersionCode=100261022" in gradle_props)
version_name = re.search(r"(?m)^oneuiAlohaVersionName=(\S+)$", gradle_props)
version_code = re.search(r"(?m)^oneuiAlohaVersionCode=(\d+)$", gradle_props)
version_ok = False
if version_name and version_code:
    components = [int(v) for v in version_name.group(1).split(".")]
    if len(components) in (3, 4):
        derived = components[0] * 100_000_000 + components[1] * 1_000_000 + components[2] + (components[3] if len(components) == 4 else 0)
        version_ok = derived == int(version_code.group(1))
check("Aloha versionCode matches updater four-part comparison", version_ok,
      "versionName/versionCode drift")

check("App Manifest points at the Aloha Application and themes",
      'android:name=".OneUIAlohaApplication"' in manifest
      and '@style/Theme.OneUIAloha' in manifest
      and '@style/Theme.OneUIAloha.Startup' in manifest
      and 'xposeddescription" android:value="OneUI Aloha' in manifest)
check("Hook Manifest uses Aloha component package",
      "com.oneuialoha.hook.RebootActivity" in hook_manifest)
check("Xposed initialization entry matches renamed Hook class",
      entry_file.read_text().strip() == EXPECTED_HOOK_ENTRY)
module_text = module_file.read_text()
check("Xposed module metadata retains API 101 and Aloha identity",
      "minApiVersion=101" in module_text
      and "targetApiVersion=101" in module_text
      and "description=OneUI Aloha" in module_text)
scope_text = scope_file.read_text()
asset_scope_text = asset_scope_file.read_text()
check("Xposed scope manifests are byte-identical",
      scope_text == asset_scope_text)
scope_lines = [line.strip() for line in scope_text.splitlines() if line.strip()]
check("Xposed self-scope uses only the Aloha application ID",
      bool(scope_lines) and scope_lines[0] == EXPECTED_APP_ID
      and "com.oneuixp.app" not in scope_lines)
check("Updater preserves the XP compatibility feed and Aloha feed",
      'LEGACY_RELEASE_REPO = "iSkyJIE/OneuiXP-Releases"' in downloader
      and 'ALOHA_RELEASE_REPO = "iSkyJIE/OneUIAloha-Releases"' in downloader
      and 'LEGACY_PACKAGE_NAME = "com.oneuixp.app"' in downloader)
check("CI verifies Aloha paths and routes future formal releases to the Aloha feed",
      "python3 ./verify_oneuialoha_identity.py" in workflow
      and 'RELEASE_REPO="iSkyJIE/OneUIAloha-Releases"' in workflow
      and "secrets.ALOHA_RELEASE_TOKEN" in workflow
      and 'iSkyJIE/OneuiXP-Releases' in workflow)

# Validate package declaration == physical path for every Kotlin/Java source under the
# three renamed namespaces. This catches partial moves and mismatched package statements.
package_roots = (
    ROOT / "app/src/main/java/com/oneuialoha",
    ROOT / "common/src/main/java/com/oneuialoha",
    ROOT / "hook/src/main/java/com/oneuialoha",
)
package_errors = []
source_files = []
for package_root in package_roots:
    if not package_root.is_dir():
        package_errors.append(f"missing package root: {package_root.relative_to(ROOT)}")
        continue
    for path in sorted(list(package_root.rglob("*.kt")) + list(package_root.rglob("*.java"))):
        source_files.append(path)
        source = path.read_text(encoding="utf-8")
        match = re.search(r"(?m)^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)", source)
        relative_parent = path.relative_to(package_root).parent.parts
        expected_package = "com.oneuialoha" + (("." + ".".join(relative_parent)) if relative_parent else "")
        if not match:
            package_errors.append(f"missing package declaration: {path.relative_to(ROOT)}")
        elif match.group(1) != expected_package:
            package_errors.append(f"{path.relative_to(ROOT)} declares {match.group(1)}, expected {expected_package}")

check("Source package declarations match their physical paths",
      len(source_files) > 0 and not package_errors, "; ".join(package_errors[:20]))
check("No old source namespace directories remain",
      not any((ROOT / module / "src/main/java/com/oneuixp").exists()
              for module in ("app", "common", "hook", "stub")))
bad_source_refs = []
for path in source_files:
    text = path.read_text(encoding="utf-8")
    # Preserve the legacy package/feed only in the updater. Persisted preferences and
    # DataStore migration identifiers (for example old filenames/markers) are not resource IDs.
    if path == (ROOT / "app/src/main/java/com/oneuialoha/app/ui/util/Downloader.kt"):
        text = text.replace('com.oneuixp.app', '').replace('iSkyJIE/OneuiXP-Releases', '')
    if ("com.oneuixp" in text or "com/oneuixp" in text
            or re.search(r"(?:R\.string\.|@string/)oneuixp_", text, re.IGNORECASE)):
        bad_source_refs.append(str(path.relative_to(ROOT)))
# The sole app-package compatibility package literal belongs in the updater, outside this
# current-identity source scan. Legacy persisted settings keys in Preference/Migration are
# deliberately not renamed by this check.
check("Current Aloha source has no retired package/resource-ID references",
      not bad_source_refs, ", ".join(bad_source_refs[:20]))

legacy_identifier_hits = []
for path in source_files:
    text = path.read_text(encoding="utf-8")
    # A historical storage key is all-lowercase; old class/function identifiers use
    # the mixed-case XP token followed by another CamelCase segment.
    if re.search(r"(?i:oneuixp)(?=[A-Z])[A-Za-z0-9_]*", text):
        legacy_identifier_hits.append(str(path.relative_to(ROOT)))
check("No retired XP identifiers remain in current Kotlin/Java source",
      not legacy_identifier_hits, ", ".join(legacy_identifier_hits[:20]))

# Persisted preference and DataStore keys are intentionally retained for backwards
# compatibility; they are not Android resource IDs and must not be blanket-renamed.
preference_bridge = (ROOT / "app/src/main/java/com/oneuialoha/app/oneuialoha/OneUIAlohaPreferenceBridge.kt").read_text()
theme_ext = (ROOT / "app/src/main/java/com/oneuialoha/app/ui/theme/ThemeExt.kt").read_text()
gallery_hook = (ROOT / "hook/src/main/java/com/oneuialoha/hook/Gallery.kt").read_text()
check("Legacy persisted settings/data keys are intentionally preserved",
      'oneuixp_features.json' in preference_bridge
      and '_oneuixp_remote_synced' in preference_bridge
      and 'oneuixp_theme_color_animation' in theme_ext
      and 'oneuixp_gallery' in gallery_hook)

# Resource IDs are a separate contract from persisted preferences: old prefixed resource names
# and references must all be renamed in every locale, while the settings keys above stay stable.
resource_root = ROOT / "app/src/main/res"
resource_hits = []
for path in resource_root.rglob("*.xml"):
    text = path.read_text(encoding="utf-8")
    if re.search(r"""(?:\bname=["']oneuixp_|@[A-Za-z_-]+/oneuixp_)""", text, re.IGNORECASE):
        resource_hits.append(str(path.relative_to(ROOT)))
check("Android resources contain no old prefixed resource names/references",
      not resource_hits, ", ".join(resource_hits[:20]))
check("Owner certificate baseline is fixed in release validation",
      EXPECTED_OWNER_CERT in workflow.lower())

for label, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + label)
if not all(checks.values()):
    for problem in problems:
        print("IDENTITY ERROR: " + problem, file=sys.stderr)
    print("BLOCKED: Aloha identity preflight failed; do not publish an APK.", file=sys.stderr)
    sys.exit(1)

print(f"Identity preflight passed ({len(source_files)} package/path declarations checked).")
print("This is not a substitute for Gradle CI, APK signature inspection, or Samsung device validation.")
