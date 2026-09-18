# OneuiXP native source removal

The unused KernelSU CMake/C++ tree and its UAPI header recovery script have been removed. Gradle's externalNativeBuild had already been disabled, and Natives.kt had already been changed to non-JNI fail-closed compatibility methods. The LSPosed Java SystemUI entry and StatusBridge are untouched.

This is source-level verification only. Legacy Root/module UI and its callers remain in the project, and an Android build and device test are still required. Do not describe this stage as an APK build.
