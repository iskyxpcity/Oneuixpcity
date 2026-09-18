# Stage 9: remove KernelSU navigation-badge coupling

Removed the KernelSU native manager/root checks, eager module loading and kernel superuser-count queries from MainActivity's navigation badge logic. Existing four pager destinations, Miuix UI, statusbar source and LSPosed entry remain untouched. A neutral NavigationBadgeState is passed to existing navigation components.

This is partial decoupling: home/module/settings and other code still depend on KernelSU. No APK compilation or device testing has been performed. Do not remove JNI/CMake until all remaining references have been resolved.
