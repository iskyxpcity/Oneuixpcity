-adaptresourcefilecontents META-INF/xposed/java_init.list
-dontwarn io.github.libxposed.annotation.**
-keep,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
-keepnames class io.github.soclear.oneuix.hook.Main

# Preserve the component name across APK updates.
-keepnames class io.github.soclear.oneuix.RebootActivity
