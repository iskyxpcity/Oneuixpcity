#!/usr/bin/env python3
from pathlib import Path
import os
import re
import shutil
import subprocess

MIGRATION_BRANCH = "migration/independent-package-names-20261019"
SCRIPT_PATH = ".github/scripts/independentize_names.py"
WORKFLOW_PATH = ".github/workflows/independentize-once.yml"
LICENSE_EXCEPTIONS = {"LICENSE", "third_party/OneUIX-Mod-LICENSE.txt"}

def run(*args):
    return subprocess.check_output(args, text=True)

def tracked_paths():
    return [p for p in run("git", "ls-files", "-z").split("\0") if p]

def actual_files():
    result = []
    for base, dirs, names in os.walk("."):
        dirs[:] = [d for d in dirs if d not in {".git", ".gradle", "build", ".migration-rename-tmp"}]
        for name in names:
            result.append((Path(base) / name).as_posix().removeprefix("./"))
    return result

def rename_brand(value):
    return re.sub(
        r"(?i)oneuix(?!p)",
        lambda m: m.group(0) + ("P" if m.group(0)[-1].isupper() else "p"),
        value,
    )

def map_path(path):
    if path in LICENSE_EXCEPTIONS:
        return path
    mappings = [
        ("app/src/main/java/io/github/soclear/oneuix/", "app/src/main/java/com/oneuixp/mod/"),
        ("common/src/main/java/io/github/soclear/oneuix/common/", "common/src/main/java/com/oneuixp/common/"),
        ("hook/src/main/java/io/github/soclear/oneuix/hook/", "hook/src/main/java/com/oneuixp/hook/"),
        ("hook/src/main/java/io/github/soclear/oneuix/", "hook/src/main/java/com/oneuixp/hook/"),
        ("app/src/main/java/com/oneuixp/app/oneuix/", "app/src/main/java/com/oneuixp/app/oneuixp/"),
    ]
    for old, new in mappings:
        if path.startswith(old):
            path = new + path[len(old):]
            break
    return rename_brand(path)

def rewrite_text(text, path):
    replacements = [
        ("app/src/main/java/io/github/soclear/oneuix/", "app/src/main/java/com/oneuixp/mod/"),
        ("common/src/main/java/io/github/soclear/oneuix/common/", "common/src/main/java/com/oneuixp/common/"),
        ("hook/src/main/java/io/github/soclear/oneuix/hook/", "hook/src/main/java/com/oneuixp/hook/"),
        ("hook/src/main/java/io/github/soclear/oneuix/", "hook/src/main/java/com/oneuixp/hook/"),
        ("io.github.soclear.oneuix.common", "com.oneuixp.common"),
        ("io.github.soclear.oneuix.hook", "com.oneuixp.hook"),
        ("io.github.soclear.oneuix.stub", "com.oneuixp.stub"),
        ("io.github.soclear.oneuix.ui", "com.oneuixp.mod.ui"),
        ("io.github.soclear.oneuix", "com.oneuixp.hook"),
        ("io.github.soclear", "com.oneuixp"),
        ("oneuix.applicationId", "oneuixp.applicationId"),
    ]
    for old, new in replacements:
        text = text.replace(old, new)
    text = rename_brand(text)

    if path == ".github/workflows/build.yml":
        token_line = "          PUBLIC_RELEASE_TOKEN: " + chr(36) + "{{ secrets.RELEASE_REPO_TOKEN }}\n"
        text = text.replace(token_line, "")
        start = text.find("            # The in-app updater checks this public binary-release feed.")
        if start >= 0:
            marker = "\n          else\n            gh release upload \"$TAG\""
            end = text.find(marker, start)
            if end < 0:
                raise RuntimeError("Could not safely locate optional public-release mirror block in build.yml")
            text = text[:start] + text[end + 1:]
        if "RELEASE_REPO_TOKEN" in text or "PUBLIC_RELEASE_TOKEN" in text:
            raise RuntimeError("build.yml still references RELEASE_REPO_TOKEN after cleanup")
    return text

def main():
    if run("git", "branch", "--show-current").strip() != MIGRATION_BRANCH:
        raise RuntimeError("Refusing to run on an unexpected branch")

    original = tracked_paths()
    moves = []
    for old in original:
        new = map_path(old)
        if new != old and old not in LICENSE_EXCEPTIONS:
            moves.append((old, new))

    destinations = {}
    original_set = set(original)
    moving_sources = {src for src, _ in moves}
    for old, new in moves:
        if new in original_set and new != old and new not in moving_sources:
            raise RuntimeError(f"Destination already exists; refusing to overwrite: {new}")
        if new in destinations:
            raise RuntimeError(f"Two files map to the same destination: {destinations[new]} and {old} -> {new}")
        destinations[new] = old

    temp_root = Path(".migration-rename-tmp")
    if temp_root.exists():
        shutil.rmtree(temp_root)
    staged = []
    for index, (old, new) in enumerate(moves):
        source = Path(old)
        if not source.exists():
            continue
        temp = temp_root / str(index)
        temp.parent.mkdir(parents=True, exist_ok=True)
        os.replace(source, temp)
        staged.append((temp, Path(new)))
    for temp, dest in staged:
        if dest.exists():
            raise RuntimeError(f"Destination appeared during rename; refusing to overwrite: {dest}")
        dest.parent.mkdir(parents=True, exist_ok=True)
        os.replace(temp, dest)
    if temp_root.exists():
        shutil.rmtree(temp_root)

    changed = []
    for path in actual_files():
        if path in {SCRIPT_PATH, WORKFLOW_PATH} or path in LICENSE_EXCEPTIONS:
            continue
        path_obj = Path(path)
        try:
            data = path_obj.read_bytes()
            if b"\x00" in data:
                continue
            text = data.decode("utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        updated = rewrite_text(text, path)
        if updated != text:
            path_obj.write_text(updated, encoding="utf-8", newline="")
            changed.append(path)

    Path(SCRIPT_PATH).unlink(missing_ok=True)
    Path(WORKFLOW_PATH).unlink(missing_ok=True)
    try:
        Path(".github/scripts").rmdir()
    except OSError:
        pass

    files = actual_files()
    residual_paths = [
        p for p in files
        if ("io/github/soclear" in p.lower() or re.search(r"(?i)oneuix(?!p)", p))
        and p not in LICENSE_EXCEPTIONS
    ]
    if residual_paths:
        raise RuntimeError("Old namespace/brand remains in paths:\n" + "\n".join(residual_paths[:100]))

    residual_namespace = []
    residual_brand = []
    for p in files:
        if p in LICENSE_EXCEPTIONS:
            continue
        try:
            data = Path(p).read_bytes()
            if b"\x00" in data:
                continue
            text = data.decode("utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        if "io.github.soclear" in text.lower() or "io/github/soclear" in text.lower():
            residual_namespace.append(p)
        if re.search(r"(?i)oneuix(?!p)", text):
            residual_brand.append(p)
    if residual_namespace:
        raise RuntimeError("Old SoClear namespace remains in:\n" + "\n".join(sorted(set(residual_namespace))[:100]))
    if residual_brand:
        raise RuntimeError("Old OneUIX brand token remains outside license exceptions in:\n" + "\n".join(sorted(set(residual_brand))[:100]))

    print(f"Renamed {len(moves)} paths and updated {len(changed)} text files.")
    print("Audit passed: old namespace and OneUIX brand token are absent outside preserved license files.")

if __name__ == "__main__":
    main()
