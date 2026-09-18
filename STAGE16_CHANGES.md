# Stage 16 — Home entry isolation

Removed the KernelSU late-load/jailbreak service invocation from `HomeScreen.kt`, including its 30-second timeout and obsolete imports. The `HomeActions` callback is retained as a no-op solely to avoid breaking existing theme components during the forthcoming card replacement; it must be removed along with its UI affordance before release.

Not yet done: home card data replacement, other KernelSU dependencies, Gradle build, device verification. No APK is claimed.
