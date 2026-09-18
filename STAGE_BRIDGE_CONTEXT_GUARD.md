# StatusBridge context guard

- Resolve and validate the provider Context before querying PackageManager; prevents a null-context crash in the SystemUI report path.
- Reuse the validated Context for persistence.
- Read boot report timestamps from one SharedPreferences handle in recentlyLoaded().
- No changes to the SystemUI UID requirement, report counting semantics, LSPosed hooks, or UI layout.
- Static checks only; Android build and on-device verification remain pending.
