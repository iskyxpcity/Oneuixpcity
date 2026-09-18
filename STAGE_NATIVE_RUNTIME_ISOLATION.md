# Native runtime isolation / branch reconciliation

Base: OneuiXP_batch_native_build_decoupling_source.tar.gz.
Merged the correct Home state and HomeViewModel from batch_home_state_cleanup, HomeScreen's simplified actions from batch_home_dead_actions_cleanup, SettingsRepositoryImpl/LogEvent/IntentDispatcher from batch_legacy_intent_isolation, and the MainActivity/Routes cleanup from batch_unreachable_routes_cleanup.

KsuIsValid is now a no-op: legacy KernelSU-only settings are not rendered, and rendering Settings does not query the removed JNI library. Existing LSPosed status reporting is independent and unchanged. Other obsolete screens/repositories still reference Natives and must be isolated or removed before runtime safety can be claimed. No APK compilation or device verification performed.
