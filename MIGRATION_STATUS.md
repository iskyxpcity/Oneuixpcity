# OneuiXP migration status

- Source: original patched Alpha55 manager archive, not a recreated UI.
- App display name/default application ID: `OneuiXP` / `com.oneuixp.app`.
- Kotlin namespace `me.weishu.kernelsu` and JNI library `kernelsu` remain **internal compatibility identifiers**. Blindly renaming them would break imports, generated resources and `System.loadLibrary` references. A full namespace migration must be validated with a successful build.
- Native dependency blocker: `app/src/main/cpp/uapi` is a broken relative symlink in the exported manager-only archive. Run `./restore_native_headers.sh` in a network-enabled environment to retrieve the exact v3.3.0 upstream headers and vendor the resulting `uapi` directory into this repository. This script has NOT been run successfully in this offline environment.
- No GitHub migration commit, build success, APK or phone test is claimed by this archive.

- Stage 5: added an independent GitHub Actions build workflow that restores the exact pinned v3.3.0 UAPI before preflight, builds the original migrated UI, validates APK ZIP, and publishes checksum only after successful build. This workflow is included in the source archive, NOT yet pushed or executed on GitHub.

- Stage 6: strengthened UAPI restoration to require all six pinned headers and reject dangling symlinks; preflight now verifies the LSPosed hook and bridge authority; CI passes explicit OneuiXP identity to Gradle. Still requires GitHub commit and actual CI execution.
