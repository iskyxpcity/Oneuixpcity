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

A live GitHub API read of `iSkyJIE/OneUIAloha-Releases/releases/latest` returned tag `v1.0.261021`, published `2026-10-20T10:21:48Z`, with asset `OneuiAloha_1.0.261021.apk` (27,565,729 bytes; advertised asset digest `sha256:a3ed4100719f18a2e4ee879662b51a6b49b34ba3d3cf44e40b71df6b2bc52075`). This differs from the handoff's prior no-Release status. The connector exposed release metadata but did not provide APK bytes, so this asset's embedded applicationId, versionCode, signature certificate and V2 signature state remain unverified. Do not overwrite this release. Version strategy must account for its current tag and updater's numeric-version parsing.

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
