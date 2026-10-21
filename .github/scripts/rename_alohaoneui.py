#!/usr/bin/env python3
"""One-time, branch-scoped source/path identity rename for Aloha OneUI."""
from pathlib import Path
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
DOWNLOADER = "app/src/main/java/com/oneuialoha/app/ui/util/Downloader.kt"
VERIFIER = "verify_oneuialoha_identity.py"
WORKFLOW = ".github/workflows/build.yml"

# These exact legacy asset names remain supported for old updater clients.
LEGACY = "__LEGACY_APK_PREFIX_PRESERVED_74C6__"
LEGACY_OUTPUT = "__LEGACY_APK_OUTPUT_PRESERVED_74C6__"

CONTENT_REPLACEMENTS = (
    ("https://github.com/iSkyJIE/OneUIAloha-Releases", "https://github.com/iSkyJIE/AlohaOneUI"),
    ("OneUIAloha-Releases", "AlohaOneUI"),
    ("OneUI Aloha", "Aloha OneUI"),
    ("ONEUI ALOHA", "ALOHA ONEUI"),
    ("ONEUI-ALOHA", "ALOHA-ONEUI"),
    ("oneui-aloha", "aloha-oneui"),
    ("ONEUI_ALOHA", "ALOHA_ONEUI"),
    ("oneui_aloha", "aloha_oneui"),
    ("OneUIAloha", "AlohaOneUI"),
    ("OneuiAloha", "AlohaOneui"),
    ("oneUIAloha", "alohaOneUI"),
    ("oneuiAloha", "alohaOneUI"),
    ("ONEUIALOHA", "ALOHAONEUI"),
    ("oneuialoha", "alohaoneui"),
)

def apply_text_replacements(text: str, rel: str) -> str:
    if rel == DOWNLOADER:
        text = text.replace("(?:AlohaOneUI|OneUIAloha)", f"(?:AlohaOneUI|{LEGACY})")
        text = text.replace('"OneUIAloha_${versionName}.apk"', f'"{LEGACY}_${{versionName}}.apk"')
        text = text.replace('"OneUIAloha.apk"', f'"{LEGACY}.apk"')
        text = text.replace("legacy OneUIAloha names", f"legacy {LEGACY} names")
    if rel == WORKFLOW:
        text = text.replace('OneUIAloha_${VERSION_NAME}.apk', LEGACY_OUTPUT)
    if rel == VERIFIER:
        text = text.replace("'OneUIAloha' in downloader", f"'{LEGACY}' in downloader")
        text = text.replace("'OneUIAloha_${VERSION_NAME}.apk' in workflow", f"'{LEGACY_OUTPUT}' in workflow")

    for old, new in CONTENT_REPLACEMENTS:
        text = text.replace(old, new)

    if rel == DOWNLOADER:
        text = text.replace(LEGACY, "OneUIAloha")
    if rel == WORKFLOW:
        text = text.replace(LEGACY_OUTPUT, "OneUIAloha_${VERSION_NAME}.apk")
    if rel == VERIFIER:
        text = text.replace(LEGACY, "OneUIAloha")
        text = text.replace(LEGACY_OUTPUT, "OneUIAloha_${VERSION_NAME}.apk")
    return text

def rename_path(rel: str) -> str:
    out = rel
    for old, new in CONTENT_REPLACEMENTS:
        out = out.replace(old, new)
    return out

def main() -> None:
    os.chdir(ROOT)
    tracked = subprocess.check_output(["git", "ls-files", "-z"]).decode().split("\0")
    tracked = [p for p in tracked if p]
    payloads = []
    destinations = set()

    for rel in tracked:
        source = ROOT / rel
        if source.is_symlink():
            raw = os.readlink(source).encode("utf-8")
            kind = "symlink"
        elif source.is_file():
            raw = source.read_bytes()
            kind = "file"
        else:
            # Git submodule entries and absent tracked paths are not text files.
            continue

        transformed = raw
        try:
            decoded = raw.decode("utf-8")
        except UnicodeDecodeError:
            decoded = None
        if decoded is not None and rel != WORKFLOW:
            # The workflow is already updated through the repository contents API.
            # Do not rewrite .github/workflows via GITHUB_TOKEN: GitHub rejects that
            # write without a separate workflows permission.
            transformed = apply_text_replacements(decoded, rel).encode("utf-8")

        new_rel = rename_path(rel)
        if new_rel in destinations:
            raise SystemExit(f"Destination collision for {new_rel}")
        destinations.add(new_rel)
        payloads.append((rel, new_rel, transformed, kind, source.stat().st_mode if kind == "file" else None))

    source_set = {item[0] for item in payloads}
    for old_rel, new_rel, _, _, _ in payloads:
        dest = ROOT / new_rel
        if old_rel != new_rel and dest.exists() and new_rel not in source_set:
            raise SystemExit(f"Refusing to overwrite untracked/existing destination: {new_rel}")

    # Recreate from the tracked snapshot so nested directory renames are collision-safe.
    for old_rel, _, _, kind, _ in payloads:
        source = ROOT / old_rel
        if kind == "symlink":
            source.unlink(missing_ok=True)
        elif source.is_file():
            source.unlink()

    for _, new_rel, data, kind, mode in payloads:
        dest = ROOT / new_rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        if kind == "symlink":
            dest.symlink_to(data.decode("utf-8"))
        else:
            dest.write_bytes(data)
            if mode is not None:
                dest.chmod(mode)

    # The migration helper is intentionally one-shot and is removed from the resulting commit.
    (ROOT / ".github/scripts/rename_alohaoneui.py").unlink(missing_ok=True)
    print(f"Renamed {len(payloads)} tracked paths/files; application ID is now com.alohaoneui.app.")
    print("Legacy OneUIAloha APK filename recognition/output alias intentionally retained.")

if __name__ == "__main__":
    main()
