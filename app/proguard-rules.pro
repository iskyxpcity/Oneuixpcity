# Protobuf
-shrinkunusedprotofields

# Commons-compress
-dontwarn com.github.luben.zstd.**

# API 101 entry and legacy fallback are named in META-INF/assets. Keep names stable.
-keep class me.weishu.kernelsu.statusbar.ModernSystemUiEntry { public <init>(); *; }
-keep class me.weishu.kernelsu.statusbar.SystemUiEntry { *; }
-keep class me.weishu.kernelsu.statusbar.SystemUiHeartbeat { *; }
-dontwarn io.github.libxposed.annotation.**
