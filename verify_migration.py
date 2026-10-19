#!/usr/bin/env python3
"""Read-only source and independence checks; this does not replace a successful Android build."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parent
checks = {
    "Gradle wrapper": (root / "gradlew").is_file(),
    "Original Miuix home": (root / "app/src/main/java/com/oneuixp/app/ui/screen/home/HomeMiuix.kt").is_file(),
    "Original Miuix bottom bar": (root / "app/src/main/java/com/oneuixp/app/ui/component/bottombar/BottomBarMiuix.kt").is_file(),
    "Legacy native build disabled": "externalNativeBuild" not in (root / "app/build.gradle.kts").read_text(),
    "Original LSPosed hook": (root / "app/src/main/java/com/oneuixp/app/statusbar/SystemUiEntry.java").is_file(),
    "Flux-style no heartbeat bridge": not (root / "app/src/main/java/com/oneuixp/app/statusbar/StatusBridge.java").exists()
        and "${applicationId}.statusbridge" not in (root / "app/src/main/AndroidManifest.xml").read_text()
        and "startHeartbeat(" not in (root / "hook/src/main/java/com/oneuixp/hook/util/OneuiXPStatusReporter.kt").read_text(),
    "OneUI XP application ID": "com.oneuixp.app" in (root / "app/build.gradle.kts").read_text(),
}

# Construct legacy markers in fragments so this checker does not flag its own code.
old_namespace = "io.github." + "soclear"
old_token = "RELEASE_REPO_" + "TOKEN"
old_brand = re.compile(r"(?:OneuiX|OneUIX|oneuix)P[a-z]")
ignored_dirs = {".git", ".gradle", "build"}
license_name = "LICENSE"
namespace_hits = []
brand_hits = []
token_hits = []

for path in root.rglob("*"):
    if not path.is_file():
        continue
    rel = path.relative_to(root).as_posix()
    if any(part in ignored_dirs for part in path.parts):
        continue
    if path.name.upper() == license_name or "LICENSE" in path.name.upper():
        continue
    if rel == "verify_migration.py":
        continue

    if old_namespace.lower() in rel.lower() or old_namespace.replace(".", "/").lower() in rel.lower():
        namespace_hits.append(rel)
    if old_brand.search(rel):
        brand_hits.append(rel)

    try:
        data = path.read_bytes()
        if b"\x00" in data:
            continue
        text = data.decode("utf-8")
    except (UnicodeDecodeError, OSError):
        continue

    if old_namespace.lower() in text.lower() or old_namespace.replace(".", "/").lower() in text.lower():
        namespace_hits.append(rel)
    if old_brand.search(text):
        brand_hits.append(rel)
    if old_token in text:
        token_hits.append(rel)

checks["No legacy namespace outside license texts"] = not namespace_hits
checks["No old OneUIX token outside license texts"] = not brand_hits
checks["No cross-repository release token reference"] = not token_hits

for name, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + name)
if not all(checks.values()):
    for label, items in (("namespace", namespace_hits), ("brand", brand_hits), ("release token", token_hits)):
        if items:
            print(f"Residual {label} references: " + ", ".join(sorted(set(items))), file=sys.stderr)
    print("BLOCKED: source preflight failed; do not claim APK build success.", file=sys.stderr)
    sys.exit(1)
print("Source preflight passed; Android build and phone tests are still required.")
