# OneUI Aloha full identity migration audit

Audit date: 2026-10-20 (HST)  
Repository: `iskyxpcity/Oneuixpcity`  
Migration branch: `migration/oneui-aloha-full-identity-20261020`  
Exact starting commit: `3a073a85ef05f1ec7371c3d27e7aa371bb8b01d5`  
Starting tree: `775a8c146d78f4132f2c8752c4820ef85d20cf6f`

## Change boundary

This work is isolated to the migration branch above. `main` was checked at `9606f8060ffd28bde97031b6e5760ad4f58c8178` and is not a target for direct writes, force updates, PR merge, or release publication without separate authorization. Do not base the identity migration on the XP bridge candidates `d7b77803ebd83681a5179ba32b78bae7bc52e482` or `e7340dbd6f882b741fd3db34707407723b374513`.

## Initial inventory

The recursive Git tree at the starting commit has 596 entries, consisting of 450 blobs and 146 tree/directory entries. The old application identity is distributed across source packages, class/file names, Gradle configuration, Android manifest/theme identifiers, localized resource filenames/keys/text, Xposed metadata and scopes, validator scripts, workflow checks and release/artifact naming.

Known identity-bearing locations include:
- `app/src/main/java/com/oneuixp/app/**`
- `app/src/main/java/com/oneuixp/mod/**` (inspect package declarations and imports; do not assume every `mod` path is XP-specific)
- `common/src/main/java/com/oneuixp/common/**`
- `hook/src/main/java/com/oneuixp/hook/**`
- `app/build.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`
- `app/src/main/AndroidManifest.xml`, resource values and localized string files
- `app/src/main/assets/xposed_scope`
- `app/src/main/resources/META-INF/xposed/{java_init.list,module.prop,scope.list}`
- `hook/src/main/resources/META-INF/xposed/scope.list`
- `.github/workflows/build.yml`, `.github/workflows/kernelsu-upstream-watch.yml`
- `verify_migration.py`, `verify_no_jni.py`, `README.md`, `docs/**`

This list is an initial inventory, not proof that the final residue audit is complete.

## Old-to-new mapping

| Existing identity / location | OneUI Aloha target | Treatment |
|---|---|---|
| Display label `OneUI XP` / `OneuiXP` | `OneUI Aloha` | Replace for current product-facing identity; preserve historical release facts only when explicitly historical. |
| Code/artifact prefix `OneuiXP`, `OneUIXP`, `ONEUIXP` | `OneUIAloha` / `ONEUIALOHA` | Rename code identifiers, file names, output/artifact names, workflow checks and docs. Signing-secret variable names are a protected exception until CI secret bindings are verified. |
| `com.oneuixp.app` | `com.oneuialoha.app` | App namespace/application ID and self-package identity. The legacy package is retained only in explicit XP updater/migration compatibility code or stored-data migration when required. |
| `com.oneuixp.common` | `com.oneuialoha.common` | Rename package declaration, import graph and matching source paths. |
| `com.oneuixp.hook` | `com.oneuialoha.hook` | Rename package declaration/imports and matching source paths. |
| Hook entry `com.oneuixp.hook.Main` | `com.oneuialoha.hook.Main` | Keep actual class, package/path and Xposed entry in exact agreement. |
| Root Gradle project `OneuiXP` | `OneUIAloha` | Rename root project identity; retain neutral module names `:app`, `:common`, `:hook`, `:stub` unless audit shows a module is specifically brand-bound. |
| `oneuixpVersionName`, `oneuixpVersionCode` | `oneuiAlohaVersionName`, `oneuiAlohaVersionCode` | Establish a product-specific monotonic version strategy; do not inherit XP bridge version without checking existing Aloha release. |
| `ONEUIXP_PACKAGE_NAME`, `ONEUIXP_NAME` | `ONEUIALOHA_PACKAGE_NAME`, `ONEUIALOHA_NAME` | Rename build overrides and all validators/workflows consistently. |
| `ONEUIXP_KEYSTORE_*`, `ONEUIXP_KEY_ALIAS`, `ONEUIXP_KEY_PASSWORD` | No automatic rename | Preserve currently configured CI secret/env bindings until repository Actions secret names can be safely audited. Never read or expose secret values. |
| `Theme.OneuiXP*`, manifest application class `.OneuiXPApplication` | Aloha-named equivalents | Rename style references, class declaration, paths, manifest and resource names together. |
| `oneuixp_*.xml`, `R.string.oneuixp_*` | `oneuialoha_*.xml`, `R.string.oneuialoha_*` | Rename resource filenames and symbol references consistently across locales; preserve necessary old persisted setting keys separately. |
| `META-INF/xposed/java_init.list` | `com.oneuialoha.hook.Main` | Required exact entry string. |
| Xposed self scope `com.oneuixp.app` | `com.oneuialoha.app` | Update app and hook scope copies together; preserve all Android/Samsung target packages and assert equality. |
| Xposed `module.prop` name/id/description | Aloha identity | Update identity fields while retaining API compatibility levels. |
| Build validator greps, APK names, README/docs | Aloha identity | Update expectations in the same commits as the corresponding code/path moves. |

## Explicit compatibility / preservation list

1. `iSkyJIE/OneuiXP-Releases` must remain for installed legacy OneUI XP builds' normal-update fallback. It is not stale branding in the migration updater.
2. `iSkyJIE/OneUIAloha-Releases` is the Aloha migration/self-update feed. It must remain separate from the XP feed.
3. `com.oneuixp.app` remains only where legacy installed XP identity is intentionally identified by the updater or where a documented migration requires reading historic XP data; all such occurrences require a named comment and tests.
4. DataStore/preferences migration constants and persisted keys must not be renamed away until migration compatibility has been checked.
5. License texts, third-party notices, upstream package names and external target packages (Android/SystemUI/Samsung) are not to be blanket-rewritten.
6. Existing Owner signing certificate SHA-256 baseline is `93bf61a6493c6b336383832eb41301bd8fe061f43566e7e384e3fdd9fc1c26a1`. Do not change keys or secrets. A successful compile is not certificate verification.
7. A changed application ID is a separately installed Android app; do not represent it as an in-place XP upgrade and do not claim automatic user-data transfer.

## Release/version observation at audit time

Correction after a fresh check on 2026-10-20 HST: `GET /repos/iSkyJIE/OneUIAloha-Releases/releases?per_page=10` returned an empty list, and `/releases/latest` returned 404. The earlier note in this file claiming an Aloha `v1.0.261021` release could not be reproduced and must not be treated as authoritative; no Aloha public Release was found in this recheck. The XP public feed does have `v1.0.261020.1` with `OneuiXP_1.0.261020.1.apk` (3,645,763 bytes; advertised SHA-256 `74bd6e2297667b63380830dab806e210f7a22b13a35fec5dad2e82c81b01f97a`). The updater accepts three- or four-component versions and computes a numeric comparison code from their components. Accordingly this branch's isolated Aloha candidate is `1.0.261021.1` / `versionCode=100261022`, which is monotonic relative to the XP tag but is not published and has not yet been verified in an APK. Do not publish/overwrite any public Release during this migration.

## Required completion gates

- [ ] Rename source package declarations, paths, brand-bound class/object/file names and all imports together.
- [ ] Update appId/namespace, manifest classes/styles/authorities/providers, resource names/keys and release labels.
- [ ] Update Xposed entry, module metadata, self-scope and both scope copies; assert paths/declarations/entries/scopes match.
- [ ] Update build preflight and CI checks to Aloha paths while preserving XP feed compatibility checks explicitly.
- [ ] Audit auto-sync/upstream workflow: retain genuine third-party KernelSU monitoring only if its upstream/repository wiring is intentional; remove stale project-repository linkage if found.
- [ ] Scan all files and report each remaining XP name/package/URL as compatibility, legal/upstream, historical text or unresolved residue.
- [ ] Select an Aloha versionName/versionCode only after documenting the current release/updater contract.
- [ ] Build optimized APK on the final branch HEAD with a completed/success Actions run; inspect package name, app label, version fields, APK ZIP integrity, V2 and Owner certificate fingerprint.
- [ ] Do not claim Samsung device acceptance without real-device evidence; do not modify main, merge a PR or publish/overwrite a public release without separate authorization.


## Migration progress update (2026-10-20 HST, after initial implementation batches)

- Branch: `migration/oneui-aloha-full-identity-20261020`; starting base remains `3a073a85ef05f1ec7371c3d27e7aa371bb8b01d5`. The branch was updated only with non-force commits. `main` was rechecked at `9606f8060ffd28bde97031b6e5760ad4f58c8178` and remains untouched.
- Source path sweep: the recursive tree no longer contains Kotlin/Java sources under the old `com/oneuixp` directory. App, mod, common and hook source files were moved to `com/oneuialoha` package paths in auditable batches; code package declarations/imports and brand-specific class/file names were changed along with each batch.
- Build identity: Gradle root/project version properties, app/common/hook/stub namespaces, app ID default, app manifest application class/theme and hook reboot activity, R8 keep rules, Xposed init entry, module description and both Xposed self-scope files have been updated for Aloha. Resource XML file names and string keys are migrated; the identity verifier scans Android resource definitions and references.
- Updater compatibility: `LEGACY_PACKAGE_NAME=com.oneuixp.app` and `iSkyJIE/OneuiXP-Releases` are intentionally retained in the updater for old XP installations; `iSkyJIE/OneUIAloha-Releases` remains the separate Aloha feed. The updater's migration target must remain a separate-app install because the app ID differs.
- CI: `verify_oneuialoha_identity.py` was added and wired into `.github/workflows/build.yml`. It checks package/path agreement, app/module identity, Xposed entry and scopes, versionCode arithmetic, resource-key residue, feed separation, and signing-certificate baseline. Workflow build paths and output names now use Aloha identifiers. Formal release routing points at the dedicated Aloha release repository and requires the `ALOHA_RELEASE_TOKEN` secret; migration-branch builds use the private source repository's prerelease path.
- Upstream workflow: `.github/workflows/kernelsu-upstream-watch.yml` only compares a curated list of real `tiann/KernelSU` UI/theme files and Material/Miuix pins, then creates/updates a review issue. It does not automatically sync third-party source into this branch; its logic is retained and its project-facing wording was renamed to Aloha.
- Not complete yet: full residual/content scan, reference integrity check for every renamed Android resource, source preflight result, Gradle preflight/build, final APK metadata/signature/V2/ZIP/hash checks, Actions run confirmation, and diagnostic/maintenance ZIP packaging. Do not consider the branch releasable until those gates pass on the final HEAD.


## Version-code validation correction (2026-10-20 HST)

- Actions Run #766 (`39434732600`) built an Owner-signed Release APK but correctly failed candidate validation: the APK label, package, versionName, V2 signature and Owner certificate were correct, while its actual `versionCode=31465` did not match the required Aloha value `100261022`.
- Root `build.gradle.kts` had read the obsolete `oneuixpVersionCode` property. The migration branch now reads `oneuiAlohaVersionCode`, so the explicit Aloha version property can be used by the Android app module. A version-source regression guard is also included in `verify_oneuialoha_identity.py`.
- The failed candidate was not published. A new run on the follow-up HEAD must confirm the APK's actual versionCode and finish all remaining Release validation before any APK is delivered.
