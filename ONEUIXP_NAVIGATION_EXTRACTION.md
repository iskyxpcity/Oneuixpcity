OneuiXP navigation extraction (2026-09-17)
- Continued from OneuiXP_active_navigation_isolated_source.tar.gz; no reset to upstream KernelSU.
- Four-tab Miuix and Material bottom bars and Material navigation rail now use OneuiXP Status Bar XP for tab three instead of KernelSU module label.
- Third page heading uses the same independent label. The page remains a placeholder; status functionality is not claimed.
- Old KernelSU authorization/module count badge computation removed from live navigation; no synthetic status badges.
- Retained existing statusbar/SystemUiEntry.java and StatusBridge.java, plus existing home LSPosed status UI.
- Removed orphaned C++ tree and native header restoration script, preserving prior native build isolation.
- Source guards passed. No Android SDK/Gradle distribution available in this runtime, so APK was not compiled or device-tested.
