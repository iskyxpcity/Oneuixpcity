# OneuiXP KernelSU UI/theme extraction test

Branch: `test/kernel-ui-dual-theme-20260922`
Base: `main@90e42b2d7978cfd3ba34a15674e1ef86c8be262f`

- Keeps the original KernelSU-derived Miuix and Material visual/navigation framework.
- Renames the live theme entry point to OneuiXP and keeps runtime switching through `UiMode`.
- Uses Android system accent as the default accent for both Material and Miuix; user-selected key colors still override it.
- Keeps OneuiXP LSPosed/SystemUI entry, scope assets and StatusBridge unchanged.
- Disconnects remaining SettingsRepository KernelSU feature execution paths.
- Reduces the top-right power menu to OneuiXP's SystemUI restart action only.
- Does not merge to `main`; APK/device testing is required first.
