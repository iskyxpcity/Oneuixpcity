#!/usr/bin/env python3
"""One-shot namespace/path migration. This file is removed after it runs."""
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path.cwd()
SELF = Path("scripts/oneuixp_namespace_migration.py")
WORKFLOW = Path(".github/workflows/oneuixp-namespace-migration.yml")
BINARY_SUFFIXES = {
    ".jpg", ".jpeg", ".png", ".gif", ".webp", ".ico", ".jar", ".apk",
    ".aab", ".ttf", ".otf", ".woff", ".woff2", ".so", ".dex", ".class",
    ".zip", ".pdf", ".mp4", ".webm", ".bin",
}

def keep_license(path: str) -> bool:
    return "license" in path.lower()

def oneuix_replacement(match: re.Match) -> str:
    token = match.group(0)
    if token == "OneuiX":
        return "OneuiXP"
    if token == "OneUIX":
        return "OneUIXP"
    if token == "ONEUIX":
        return "ONEUIXP"
    if token == "oneuix":
        return "oneuixp"
    # Preserve a conventional title style for uncommon mixed-case variants.
    return token + ("P" if token[-1].isupper() else "p")

def rename_token(value: str) -> str:
    return re.sub(r"oneuix(?!p)", oneuix_replacement, value, flags=re.IGNORECASE)

def rename_path(value: str) -> str:
    if keep_license(value):
        return value
    value = value.replace(
        "io/github/soclear/oneuix",
        "io/github/oneuixp",
    )
    value = value.replace(
        "com/oneuixp/app/oneuix",
        "com/oneuixp/app/oneuixp",
    )
    return rename_token(value)

def rename_content(value: str) -> str:
    value = value.replace(
        "io.github.soclear.oneuix",
        "io.github.oneuixp",
    )
    value = value.replace(
        "io/github/soclear/oneuix",
        "io/github/oneuixp",
    )
    value = re.sub(r"io\.github\.soclear\.", "io.github.", value)
    value = re.sub(r"io/github/soclear/", "io/github/", value)
    value = rename_token(value)
    value = value.replace(
        "OneuiXP · OneUIXP full migration",
        "OneuiXP · Independent One UI features",
    )
    return value

def run_git(*args: str, capture: bool = False) -> bytes | None:
    result = subprocess.run(
        ["git", *args],
        cwd=ROOT,
        check=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE,
    )
    return result.stdout if capture else None

def tracked_paths() -> list[Path]:
    raw = run_git("ls-files", "-z", capture=True) or b""
    return [Path(item.decode("utf-8")) for item in raw.split(b"\0") if item]

def main() -> int:
    tracked = tracked_paths()
    protected = {SELF.as_posix(), WORKFLOW.as_posix()}
    plans = []
    for src in tracked:
        rel = src.as_posix()
        if rel in protected or keep_license(rel) or not src.is_file():
            continue
        suffix = src.suffix.lower()
        data = src.read_bytes()
        if suffix in BINARY_SUFFIXES or b"\0" in data:
            new_rel = rename_path(rel)
            if new_rel != rel:
                plans.append((src, Path(new_rel), data))
            continue
        try:
            original = data.decode("utf-8")
        except UnicodeDecodeError:
            new_rel = rename_path(rel)
            if new_rel != rel:
                plans.append((src, Path(new_rel), data))
            continue
        new_rel = rename_path(rel)
        # Markdown bodies document historical source provenance and are retained;
        # their own filenames are still brought under the OneuiXP name.
        updated = original if suffix == ".md" else rename_content(original)
        new_data = updated.encode("utf-8")
        if new_rel != rel or new_data != data:
            plans.append((src, Path(new_rel), new_data))

    moves = {src for src, dest, _ in plans if src != dest}
    destinations = [dest for _, dest, _ in plans]
    if len(set(destinations)) != len(destinations):
        raise RuntimeError("Namespace migration would create duplicate destination paths.")
    for src, dest, _ in plans:
        if dest.exists() and dest != src and dest not in moves:
            raise RuntimeError(f"Refusing to overwrite an unrelated existing path: {dest}")

    # Write all destinations before unlinking old names, so each move is lossless.
    for src, dest, data in plans:
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(data)
    for src, dest, _ in plans:
        if src != dest and src.exists():
            src.unlink()

    # Add an explicit current-state note while keeping older migration entries
    # unchanged as historical provenance.
    migration_doc = Path("docs/ONEUIXP_MIGRATION.md")
    if migration_doc.is_file():
        doc = migration_doc.read_text(encoding="utf-8")
        marker = "## Current namespace independence — 2026-10-19"
        if marker not in doc:
            doc += (
                "\n\n" + marker + "\n\n"
                "The historical stages above retain their original OneUIX-Mod source identity "
                "and package names as provenance. Current OneuiXP source code uses the independent "
                "internal namespaces `io.github.oneuixp.common`, `io.github.oneuixp.hook`, "
                "and `io.github.oneuixp.ui.category`; their source directories and all build/entry "
                "references were moved together. The app install identity remains "
                "`com.oneuixp.app` for upgrade compatibility. The preserved third-party license "
                "file is not renamed or edited. This migration does not modify formal `main`.\n"
            )
            migration_doc.write_text(doc, encoding="utf-8")

    run_git("add", "-A")
    indexed = tracked_paths()
    old_patterns = [
        re.compile(r"soclear", re.IGNORECASE),
        re.compile(r"oneuix(?!p)", re.IGNORECASE),
    ]
    leftovers = []
    for path in indexed:
        rel = path.as_posix()
        if rel in protected or keep_license(rel) or path.suffix.lower() == ".md":
            continue
        if any(pattern.search(rel) for pattern in old_patterns):
            leftovers.append(f"path: {rel}")
            continue
        if not path.is_file():
            continue
        raw = path.read_bytes()
        if b"\0" in raw:
            continue
        try:
            content = raw.decode("utf-8")
        except UnicodeDecodeError:
            continue
        for pattern in old_patterns:
            match = pattern.search(content)
            if match:
                line = content.count("\n", 0, match.start()) + 1
                leftovers.append(f"{rel}:{line}: {match.group(0)}")
                break

    required = [
        "common/src/main/java/io/github/oneuixp/common/Preference.kt",
        "hook/src/main/java/io/github/oneuixp/hook/Main.kt",
        "app/src/main/java/io/github/oneuixp/ui/category/DetailPaneSystemUI.kt",
        "app/src/main/java/com/oneuixp/app/oneuixp/OneuiXPFeatureCatalog.kt",
    ]
    missing = [path for path in required if not Path(path).is_file()]
    java_init = Path("app/src/main/resources/META-INF/xposed/java_init.list")
    if not java_init.is_file() or "io.github.oneuixp.hook.Main" not in java_init.read_text(encoding="utf-8"):
        missing.append("app/src/main/resources/META-INF/xposed/java_init.list -> io.github.oneuixp.hook.Main")
    app_gradle = Path("app/build.gradle.kts").read_text(encoding="utf-8")
    if 'defaultManagerPackageName = "com.oneuixp.app"' not in app_gradle or 'namespace = "com.oneuixp.app"' not in app_gradle:
        missing.append("application identity com.oneuixp.app")
    for path in ("common/build.gradle.kts", "hook/build.gradle.kts"):
        content = Path(path).read_text(encoding="utf-8")
        if "io.github.soclear" in content or "io.github.oneuixp" not in content:
            missing.append(f"Gradle namespace in {path}")

    if leftovers:
        print("Old namespace/name tokens remain outside historical Markdown/license/tooling:")
        print("\n".join(leftovers[:100]))
        raise RuntimeError(f"Found {len(leftovers)} old namespace/name references.")
    if missing:
        raise RuntimeError("Migration validation failed: " + ", ".join(missing))
    print(f"Namespace/path migration staged and validated: {len(plans)} files rewritten or moved.")
    print("Validated new internal packages, path moves, Xposed entry, and unchanged app ID.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
