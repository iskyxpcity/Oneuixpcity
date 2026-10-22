# Global shortcut upstream port map (2026-10-22)

## Baseline and scope

- Target repository: `iskyxpcity/Oneuixpcity`
- Immutable base used by this migration branch: `9216c83062d67e3feefeb29746e770b2d8a0fc9d` (`main` at verification time).
- Upstream source repository: `iskyxpcity/App20261018Aloha`
- Upstream source baseline: `6772cf55ad8d1c2b4276aa7988bc413d8bc6fade`.
- Migration branch: `refactor/global-shortcut-upstream-port-20261022`.
- No mainline changes are part of this migration. Build #943 belongs to the base commit only and is not evidence for this migration.

This is a source-derived mapping, not a completion declaration. “Difference” fields below record differences already confirmed by reading the named implementations; unverified runtime behavior remains marked pending until code review and a matching build.

## Source file responsibilities and target adapters

| Responsibility | Upstream source / entry points | Target at baseline | Confirmed status and migration intent |
|---|---|---|---|
| Hook entry and lifecycle | `rootmodule/.../SystemUiRootHook.kt`: `handleLoadPackage`, `safeHook`, `safeSystemUiHook`; delegates to `hookGlobalThreeFingerGesture` and `hookNavigationKeyShortcuts`. | `hook/.../Main.kt`: libxposed `onPackageReady` / `onSystemServerStarting`; `GlobalQuickActions.installSystemUi` / `installSystemServer`. | API boundary differs: upstream uses legacy `IXposedHookLoadPackage` / XposedHelpers / XC_MethodHook; target uses libxposed `XposedModule` and hook chains. Port business paths, not the old framework entry class. |
| Runtime types and independent state | `RootModuleTypes.kt`: `SwipeDirection`, `ThreeFingerSettings`, `ThreeFingerPinchSettings`, `ThreeFingerSpreadSettings`, `FloatingBallSettings`, `StatusBarShortcutSettings`, `NavigationKeyRole`, `NavigationKeyShortcutSettings`, `NavigationKeyTouchState`, `ScreenOffVolumeKeySettings`. `RootModuleShared.kt`: per-gesture pointer IDs, offsets, handler/runnable references, context and input-manager references. | `GlobalQuickActions.kt` currently stores most runtime states in one object and uses `Preference.GlobalShortcutActions`. | Restore distinct state per upstream recognizer. Introduce a narrow preference/runtime adapter; do not duplicate execution logic for Material/Miuix. |
| Setting readers | `SystemUiRootHookHelpers.kt`: `threeFingerSettings`, `threeFingerPinchSettings`, `threeFingerSpreadSettings`, `floatingBallSettings(context)`, `statusBarShortcutSettings(context)`, `navigationKeyShortcutSettings(context)`, `screenOffVolumeKeySettings(context)`; source reads module `Settings.Global` keys and uses a cache. | `common/.../Preference.kt` -> `other.globalShortcutActions`; `PreferenceProvider.kt` exposes hook-side preference data; `GlobalQuickActions.currentSettings()`. | Do not copy upstream `Settings.Global` storage to Aloha. Build equivalent typed models from Aloha's single persisted preference snapshot. Validate every source setting and its default/range against the existing Aloha model. |
| Touch-event source | `SystemUiRootHookGesture.kt`: `hookGlobalThreeFingerGesture`, `hookInputManagerServiceForGlobalGesture`, `handleGlobalMotion`; `SystemUiRootHookFloatingBall.kt`: float-ball View's `OnTouchListener`. | `GlobalQuickActions.installSystemUi`, `installSystemServer`, `hookInputManagerServiceService`, `hookGlobalPointerEvents`, `handleGlobalMotion`. | Keep the libxposed registration thin. The actual event ordering, cancellation, original/default event dispatch, and reset paths belong in source-derived runtime functions. Compare hook points before replacing them. |
| Swipe / pinch / spread | `SystemUiRootHookGesture.kt`: `handleThreeFingerMotion`, `recordThreeFingerStart`, `consistentThreeFingerDirection`, `swipeDirection`, `cancelCurrentTouchGesture`, `resetThreeFingerState`; separate `handleThreeFingerPinchMotion`, `recordThreeFingerPinchStart`, `threeFingerPinchAverageSpan`, `resetThreeFingerPinchState`; separate spread equivalents. | `GlobalQuickActions.kt`: `handleThreeFingerMove`, `startThreeFingerGesture`, `averageThreeFingerSpan`, `consumeThreeFingerGesture`, `resetThreeFingerState`. | Confirmed architectural divergence: upstream has three independent recognizers and per-gesture pointer/state/reset paths; target's current movement path calculates span change in the same recognizer that resolves swipe direction. Restore independent source functions and state before considering equivalent behavior. Preserve each distinct source threshold/haptic/action mapping. |
| Floating ball create/update/remove | `SystemUiRootHookFloatingBall.kt`: `ensureFloatingBall`, `registerFloatingBallConfigReceiver`, `removeFloatingBall`, drawing and WindowManager attach/update/remove. | `GlobalQuickActions.kt`: `ensureFloatingBall`, inline View/drawing, `removeFloatingBall`; fields include settings hash, position-initialized marker, WindowManager reference and ensure throttling. | Source behavior has its own lifecycle and config receiver. The current implementation adds hash/generation/host/touch-active state and additional scheduling/fallback branches. Audit each extra field/path against a proven libxposed/device requirement; do not mechanically keep or delete it. Restore source lifecycle and receiver responsibilities. |
| Floating ball touch and auto edge | `SystemUiRootHookFloatingBall.kt`: `handleFloatingBallTouch`, `scheduleFloatingBallAutoEdge`, `cancelFloatingBallAutoEdge`, `snapFloatingBallToNearestEdge`, `updateFloatingBallInnerOffset`, `resetFloatingBallInnerOffset`. | `GlobalQuickActions.kt`: `handleFloatingBallTouch`, `scheduleFloatingBallAutoEdge`, `cancelFloatingBallAutoEdge`, inline snap/offset functions. | Compare DOWN/MOVE/UP/CANCEL branch by branch: click vs double-click deadline, long press vs drag, swipe threshold/direction, inner offset reset, edge timer cancellation/re-arm and post-remove behavior. The base has an earlier fix for throttled main-thread ensure; it is not proof the remaining float-ball lifecycle is source-equivalent. |
| Status bar gestures | `SystemUiRootHookGesture.kt`: `handleStatusBarShortcutMotion`, `resetStatusBarShortcutState`; settings reader above. | `GlobalQuickActions.kt`: `handleStatusBarMotion`, `resetStatusBarState`. | Compare bar-bound detection, left/right area, short tap validity, movement cancellation, delayed single tap vs double tap, long-press runnables, and their cancellation/reset. Current source/target mappings have been located; full branch-by-branch reconciliation remains pending. |
| Navigation key gestures | `SystemUiRootHookGesture.kt`: `hookNavigationKeyShortcuts`, `handleNavigationKeyShortcutTouch`, `detectNavigationKeyRole`; `RootModuleTypes.kt`: `NavigationKeyRole` and `NavigationKeyTouchState`. | `GlobalQuickActions.kt`: `handleNavigationTouch`, `navigationRole`, `navigationDoubleAction`, `navigationLongAction`, `navigationDefaultKey`, `navigationStates`. | Keep Samsung navigation-key role recognition and original default single-tap action semantics. Compare event consumption, view movement/slop, pending single-tap delay, double click, long press, cancellation and default-key injection. Adapt only the Hook API where needed. |
| Screen-off volume | `SystemUiRootHookGesture.kt`: `handleScreenOffVolumeKeyShortcut`, `scheduleScreenOffVolumeSingleTap`, `adjustMusicVolume`, `isScreenInteractive`, cancel/reset helpers. | `GlobalQuickActions.kt`: `handleScreenOffVolumeKey`, `cancelVolumeLongPress`, `cancelVolumeDoubleClick`, `adjustMusicVolume`. | Compare screen interactive state, key down/up and repeat, long-press timeout, double-click timeout, pending-task cancellation, music volume adjustment, haptic behavior, and whether normal volume events are consumed. |
| Action execution and haptics | `SystemUiRootHookGestureActions.kt`: `performGlobalGestureAction`, `performHapticFeedback`, key injection, screenshot/Smart Capture, global actions, long-press key injection, swipe injection, system toggles, scroll actions. | `GlobalQuickActions.kt`: `performGlobalAction`, `performHaptic`, and inline helper/action functions (key injection, policy calls, app/URI launches, screenshot, camera, toggles, cleaners). | Source uses rootmodule helpers and legacy Xposed reflection. Port the action switch/helper bodies and dispatch order, changing only access to libxposed-reflected services and Aloha context. Do not treat matching action labels as functional equivalence. |
| Context / main-thread / input manager / phone policy | `SystemUiRootHookHelpers.kt`: `currentApplicationContext`, `postOnMain`, `postOnMainDelayed`, `getInputManager`-related access and context helpers; `RootModuleShared.kt` state. | `GlobalQuickActions.kt`: own SystemServer context discovery, `mainHandler`, reflected InputManager service and PhoneWindowManager. | Prefer one narrow Aloha runtime adapter to duplicate helper mechanisms. Confirm each helper's thread, return/failure path and call site before migration. |
| Persist/sync semantics | `GlobalThreeFingerGestureActivity.kt`, `ShortcutTriggerConfigActivity.kt`, `RootFeatureStore.kt`, `RootModuleKeys.kt`: `askApplyNow` prompts “是否立即同步？”, “稍后” leaves current module payload active, “立即同步” writes module config via source sync path and float-ball config broadcast; module reads values only once applied. | `GlobalShortcutActionsScreen.kt`, `AlohaOneUIPreferenceBridge.kt`, `AlohaOneUIToolsViewModel.kt`, `Preference.kt`: `deferGlobalShortcutSync`, local DataStore update, `synchronizeGlobalShortcutActionsNow`, pending marker and active remote preference preservation. | Target already has a real deferred-sync mechanism and matching dialog text. Preserve it; verify every field update goes through it, “Later” doesn't publish edited global shortcuts, and immediate sync publishes all fields and notifies the hook observer. Don't migrate the old Shizuku/Settings.Global writer literally into libxposed. |
| UI / categories / Material + Miuix | Source setting activities contain legacy Android UI and grouped action catalogs. | `GlobalShortcutActionsScreen.kt` (7 page entry points, shared preference and sync handler, per-theme render paths); `OtherFunctionsScreen.kt` and `OtherFunctionsThemeGroup.kt`. | UI is out of scope for porting from the old Android Views. Keep Aloha's existing grouped page/subpage hierarchy, source labels/defaults/action grouping, theme-specific components, and one shared business/preference path. Any UI adjustment requires independent Material and Miuix review. |

## Action catalogue to reconcile

The source `RootModuleShared.kt` defines these action identifiers; every identifier must be traced through the source action switch and Aloha UI picker/target dispatcher, not merely counted in the menu:

- Navigation/system: `none`, `back`, `home`, `recents`, `notifications`, `screenshot`, `smart_screenshot`, `lock_screen`, `power_menu`.
- Media: `previous_track`, `next_track`, `play_pause`, `media_rewind`, `media_fast_forward`.
- System toggles and cleanup: `flashlight`, `kill_foreground_app`, `memory_clean`, `screen_record`, `airplane_mode`, `bluetooth`, `silent_mode`, `vibrate_mode`, `wifi`, `mobile_data`.
- Camera / scroll: `quick_camera`, `take_photo`, `quick_video`, `scroll_top`, `scroll_bottom`.
- Custom apps: `custom_app:<component>`.
- App-specific shortcuts: `wechat_scan`, `wechat_pay`, `wechat_collect`, `alipay_scan`, `alipay_pay`, `alipay_collect`.

## Settings groups to reconcile

| Trigger group | Source model / relevant values | Target model family |
|---|---|---|
| Three-finger swipe | enable; direction action mapping; touch-cancel threshold; haptic enabled and strength | `threeFingerGestureEnabled`, `threeFingerSwipe*Action`, `threeFingerTouchCancelThresholdDp`, `threeFingerHaptic*` |
| Three-finger pinch | enable; independent threshold; haptic enabled and strength; action | `threeFingerPinchEnabled`, `threeFingerPinchThresholdDp`, `threeFingerPinchHaptic*`, `threeFingerPinchAction` |
| Three-finger spread | enable; independent threshold; haptic enabled and strength; action | `threeFingerSpreadEnabled`, `threeFingerSpreadThresholdDp`, `threeFingerSpreadHaptic*`, `threeFingerSpreadAction` |
| Floating ball | enable; ARGB color; size; auto-edge delay; haptic; click/double-click/four directional swipes; X/Y held in source runtime memory | `floatingBall*` fields in `Preference.GlobalShortcutActions`; legacy `floatingBallX/Y` preference fields are not read by the source-derived runtime so original in-memory position behavior remains |
| Status bar | enable; haptic; double click; left/right click and long press | `statusBarShortcut*`, `statusBarDoubleClickAction`, `statusBarLeft/Right*Action` |
| Navigation keys | enable; haptic; back/home/recents double click and long press | `navigationKeyShortcut*`, `navigation*DoubleTapAction`, `navigation*LongPressAction` |
| Screen-off volume keys | enable; long-press enable/timeout; double-click enable/timeout; haptic | `screenOffVolumeKey*` fields in `Preference.GlobalShortcutActions` |

## Verification gates

1. Audit source and target function bodies and their call sites, including all reset/cancel/timeout branches, before substituting any implementation.
2. Keep source logic separate by responsibility and record each unavoidable libxposed / Preference / action-access adapter.
3. Verify the seven Aloha settings pages and the existing Material/Miuix UI code remain unchanged unless a demonstrated synchronization defect requires a focused fix.
4. After source migration and static call-chain review are complete, run the branch build and verify that the build's `head_sha` is exactly the reviewed migration commit.
5. Samsung `SM-F966U1` / Android 17 acceptance is separate from CI: verify all gesture triggers, float-ball click/double-click/long press/drag/auto-edge/config refresh, status-bar regions, each navigation key, screen-off volume timing, each action, and both UI themes. Until then, no claim of full functional acceptance.


## Implementation checkpoint — 2026-10-22

On `refactor/global-shortcut-upstream-port-20261022`, the first implementation tranche restores separate source-derived runtime files:

- `GlobalQuickActionsModels.kt`: upstream action IDs, thresholds, per-trigger typed settings/state models.
- `GlobalQuickActionsPreferences.kt`: thin adapter from Aloha's `Preference.GlobalShortcutActions`, preserving the existing stored preference format.
- `GlobalQuickActionsGesture.kt`: upstream runtime handlers from `SystemUiRootHookGesture.kt`; legacy entry installers excluded and reflection calls routed through the Aloha adapter.
- `GlobalQuickActionsFloatingBall.kt`: source lifecycle/touch/auto-edge functions; the upstream package broadcast receiver is intentionally replaced by Aloha's preference URI observer.
- `GlobalQuickActionsGestureActions.kt` and `GlobalQuickActionsAppLaunch.kt`: source action switch and companion helpers restored as separate files.
- `GlobalQuickActionsHelpers.kt`: narrow context/main-thread/reflection adapter.
- `GlobalQuickActions.kt`: replaces the monolithic business implementation with libxposed hook registration, source-shaped state ownership, existing one-shot SystemServer diagnostics, and the Aloha preference observer.

The app UI file, Aloha persisted preference model, Material/Miuix components, seven-page hierarchy, and deferred “是否立即同步？” mechanism are not part of this tranche. The source action constant `quick_camera` is present in the upstream catalog, but the source action dispatcher does not currently have an explicit branch for it; this discrepancy is tracked rather than silently creating new behavior.

This is an implementation checkpoint, not a completion declaration. The source comparison, Kotlin build, and Samsung `SM-F966U1` / Android 17 acceptance remain separate gates.


## Static review before the first migration build

- Verified the new hook runtime has distinct swipe, pinch, spread, status-bar, navigation-key and screen-off-volume handlers sourced from `SystemUiRootHookGesture.kt`; the legacy Xposed installer functions are not present in the target port.
- Verified the float-ball create/remove, touch and auto-edge methods come from `SystemUiRootHookFloatingBall.kt`; configuration refresh uses the existing Aloha preference URI observer rather than the upstream package broadcast.
- Verified action dispatch bodies are sourced from `SystemUiRootHookGestureActions.kt` plus `SystemUiRootHookAppLaunch.kt`; old `XposedHelpers` / `XposedBridge` symbols were removed from the transferred runtime files.
- Verified the current target app-side setting model and UI hierarchy were not edited by this migration tranche. The existing deferred sync marker and “稍后 / 立即同步” code remain at their base-commit blobs.
- Corrected the target's status-bar-height adapter to preserve the upstream fallback and changed reflection lookup to snapshot the current class before enumerating methods. Build and final symbol verification follow this source review.


## Function-body and UI/action-catalog audit

The transferred runtime bodies have been compared against current upstream bodies after only the recorded integration substitutions:

- `SystemUiRootHookGesture.kt` runtime block: body equality confirmed after receiver rename (`SystemUiRootHook` → `GlobalQuickActions`) and replacement of two legacy Xposed reflection/logging calls. The three legacy hook installers were excluded and replaced by the libxposed adapter.
- `SystemUiRootHookFloatingBall.kt`: body equality confirmed after excluding the legacy broadcast receiver and adding Aloha WindowManager/coordinate state tracking. Create/remove, DOWN/MOVE/UP/CANCEL branches, click/double-click, long-press/drag, inner offset and auto-edge scheduling/cancellation methods are all present.
- `SystemUiRootHookGestureActions.kt`: body equality confirmed after replacing Xposed reflection/field access/log calls.
- `SystemUiRootHookAppLaunch.kt`: body equality confirmed after replacing Xposed reflection and preserving the source's `InputManager.getInstance()` call rather than substituting the separately hooked `InputManagerService`.
- Source UI action IDs and Aloha picker IDs were compared as literal values: the source-exposed list has matching entries; `none` is the default action and `custom_app` uses the existing `custom_app:<component>` selection flow. Action branches for every source-exposed executable ID are present in the upstream dispatcher body. Source's unexposed `quick_camera` / notification constants remain documented source inconsistencies/dead entries rather than new Aloha UI options.
- Source typed model fields map to Aloha's existing `Preference.GlobalShortcutActions` fields in one adapter. The old individual source caches are replaced by one snapshot cache because Aloha reads the complete remote `Preference` object, while preserving the source's 1000 ms cache interval.
- App-side files intentionally untouched by this branch include `GlobalShortcutActionsScreen.kt` (base blob `6b040c4395ded15e0bc0c307b92fd745a07756fe`), `AlohaOneUIPreferenceBridge.kt` (base blob `aa205331cd5b8967a1b748040a69209f240d3db3`), and `Preference.kt` (base blob `b9cbecae9d13aca05d6c8c79cddf673c586321c7`). The existing page hierarchy, Material/Miuix rendering branches, action labels/order and deferred synchronization protocol remain unchanged.


## Model-constant cross-check

The union of constants referenced by all imported source runtime bodies was compared with the new target model constants and the corresponding upstream `RootModuleShared.kt` declarations. The static scan found one naming mismatch, `STATUS_BAR_DOUBLE_TAP_MS`, which is now restored to the exact upstream identifier. Android `MotionEvent.ACTION_*` / `Intent.ACTION_*` references were excluded from the feature-ID comparison. The action IDs `scroll_top` and `scroll_bottom` are defined in the target model even though the current upstream picker doesn't expose them, and their source dispatcher implementations remain present.
