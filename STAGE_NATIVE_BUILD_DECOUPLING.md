# OneuiXP native build decoupling

- Disabled all app Gradle externalNativeBuild/CMake blocks and removed pinned NDK configuration.
- CI no longer downloads NDK or attempts to restore the six KernelSU UAPI headers.
- Source preflight checks the CMake build is disabled instead of requiring those headers.
- This is **build configuration only**: Natives.kt still loads libkernelsu and legacy Kotlin call sites remain. A successful Gradle build does NOT establish runtime safety; audit/remove all reachable JNI callers before installing or calling this a completed migration.
- Preserve LSPosed/SystemUI Java hooks, StatusBridge and the dual UI themes.
- No APK has been built or tested in this environment.
