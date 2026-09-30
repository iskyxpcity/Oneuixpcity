# API 101 entry and legacy fallback are named in META-INF/assets. Keep names stable.
-keep class com.oneuixp.app.statusbar.ModernSystemUiEntry { public <init>(); *; }
-keep class com.oneuixp.app.statusbar.SystemUiEntry { *; }
-keep class com.oneuixp.app.statusbar.SystemUiHeartbeat { *; }
-dontwarn io.github.libxposed.annotation.**

# Flux-style self-process activation probe: reflected from the Xposed entry.
-keep class com.oneuixp.app.statusbar.ModuleStatus { public static boolean isActive(); }


# These classes are provided by Android/Samsung host processes at runtime.
# They are compileOnly stubs and must never be packaged into OneuiXP.
-dontwarn android.os.SystemProperties
-dontwarn com.samsung.android.globalactions.**
