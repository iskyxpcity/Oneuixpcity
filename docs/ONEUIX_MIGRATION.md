# OneUIX-Mod → OneuiXP migration

Source repository: `iSkyJIE/OneUIX-Mod`  
Source baseline: `303fb4d69e6d2ad2d55349beea34afecd7a24e8a`  
Target branch: `migration/oneuix-full-into-oneuixp`

## Stage 1 — isolated source import

This stage imports the upstream `common`, `hook`, and `stub` modules into OneuiXP without activating them.

Locked rules for this stage:

- OneuiXP's existing `app` module, UI, launcher, startup, signing, LSPosed status reporting, and current SystemUI-only scope remain unchanged.
- The OneUIX hook module is **not** an `app` dependency yet, so its Xposed metadata and 25-package scope are not packaged into the OneuiXP APK.
- Original OneUIX package namespaces are preserved inside the isolated modules to keep source provenance clear and make future upstream synchronization practical.
- Only build-compatibility edits are applied to imported module Gradle files: Java source/target 25 → 21 to match the current OneuiXP CI JDK.
- The temporary Gradle property `oneuix.applicationId=com.oneuixp.app` only supplies the imported common module's BuildConfig constant. It does not activate hooks.
- The imported source is AGPL-3.0 licensed. A verbatim copy of the source project's license is stored at `third_party/OneUIX-Mod-LICENSE.txt`.

## Next gated stage

Only after Stage 1 compiles green:
1. upgrade/normalize the modern libxposed API layer to API 102;
2. merge OneuiXP heartbeat/status reporting into a single modern hook entry;
3. add remote preference synchronization;
4. expand Xposed scope only when the corresponding hook groups are ready for Samsung phone testing.

Do not merge this migration branch into `main` merely because CI is green.


## Stage 2 — API102 single modern entry (in progress)

Stage 1 compiled successfully in GitHub Actions Run #273.

Stage 2 rules:
- The app now packages `common` + `hook`.
- Modern Xposed metadata is owned only by the app and points to `io.github.soclear.oneuix.hook.Main`.
- The imported hook module's duplicate `META-INF/xposed/*` files are removed.
- Modern metadata is API 102, but legacy API82 `assets/xposed_init` reporting fallback remains for now.
- `OneuiXPStatusReporter` moves SystemUI load reporting + heartbeat into the single modern hook entry.
- `OneuiXPreferenceBridge` introduces DataStore + libxposed remote-file synchronization.
- **All imported OneUIX feature hooks remain blocked by `ENABLE_ONEUIX_FUNCTIONS = false`.**
- Xposed scope remains exactly one package: `com.android.systemui`.
- Miuix/Material UI and existing OneuiXP pages remain unchanged.

Phone validation for this stage is about API102 activation, live LSPosed state, StatusBridge heartbeat, startup/UI regressions, and install stability — not OneUIX features yet.
