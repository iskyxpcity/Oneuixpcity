package com.oneuialoha.app.statusbar;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Report only after SystemUI's application context becomes available. */
public final class SystemUiEntry implements IXposedHookLoadPackage {
    // Application.attach may be observed more than once; report at most once per process.
    private static boolean reportedThisProcess = false;
    /** Read-only class discovery. Never instantiate or hook Samsung classes. */
    private static String probe(ClassLoader loader) {
        String[] names = {
            "com.android.systemui.statusbar.phone.PhoneStatusBarView",
            "com.android.systemui.statusbar.phone.StatusBarIconController",
            "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy",
            "com.android.systemui.statusbar.StatusBarWifiView",
            "com.android.systemui.statusbar.StatusBarMobileView",
            "com.android.systemui.battery.BatteryMeterView",
            "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView",
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
        };
        StringBuilder result = new StringBuilder();
        for (String name : names) {
            boolean found = false;
            try { Class.forName(name, false, loader); found = true; }
            catch (Throwable ignored) { /* Unknown ROM classes must not break SystemUI. */ }
            if (result.length() != 0) result.append("\n");
            result.append(found ? "FOUND " : "NOT_FOUND ").append(name);
        }
        return result.toString();
    }

    /** Read-only, bounded metadata. No instances, invocations, or view mutation. */
    private static String probeDetails(ClassLoader loader) {
        String[] names = {
            "com.android.systemui.statusbar.phone.PhoneStatusBarView",
            "com.android.systemui.statusbar.phone.StatusBarSignalPolicy",
            "com.android.systemui.battery.BatteryMeterView",
            "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView",
            "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView"
        };
        StringBuilder out = new StringBuilder("Alpha54 READ_ONLY\n");
        for (String name : names) {
            out.append("\n").append(name).append("\n");
            try {
                Class<?> type = Class.forName(name, false, loader);
                Class<?> parent = type.getSuperclass();
                out.append("FOUND parent=").append(parent == null ? "none" : parent.getName()).append("\n");
                java.lang.reflect.Constructor<?>[] ctors = type.getDeclaredConstructors();
                out.append("constructors=").append(ctors.length).append("\n");
                java.lang.reflect.Field[] fields = type.getDeclaredFields();
                java.util.Arrays.sort(fields, new java.util.Comparator<java.lang.reflect.Field>() {
                    @Override public int compare(java.lang.reflect.Field a, java.lang.reflect.Field b) {
                        return a.getName().compareTo(b.getName());
                    }
                });
                out.append("fields(").append(fields.length).append("):");
                int fieldCount = 0;
                for (java.lang.reflect.Field f : fields) {
                    String n = f.getName().toLowerCase(java.util.Locale.ROOT);
                    if (!(n.contains("icon") || n.contains("view") || n.contains("container")
                            || n.contains("battery") || n.contains("wifi") || n.contains("mobile")
                            || n.contains("signal") || n.contains("state"))) continue;
                    if (fieldCount++ >= 10) break;
                    out.append(" ").append(f.getName()).append(":").append(f.getType().getSimpleName());
                }
                out.append("\n");
                java.lang.reflect.Method[] methods = type.getDeclaredMethods();
                java.util.Arrays.sort(methods, new java.util.Comparator<java.lang.reflect.Method>() {
                    @Override public int compare(java.lang.reflect.Method a, java.lang.reflect.Method b) {
                        return a.getName().compareTo(b.getName());
                    }
                });
                out.append("methods(").append(methods.length).append("):");
                int methodCount = 0;
                for (java.lang.reflect.Method m : methods) {
                    String n = m.getName().toLowerCase(java.util.Locale.ROOT);
                    if (!(n.contains("attach") || n.contains("inflate") || n.contains("update")
                            || n.contains("bind") || n.contains("icon") || n.contains("state")
                            || n.contains("battery") || n.contains("wifi") || n.contains("mobile")
                            || n.contains("signal") || n.contains("visible"))) continue;
                    if (methodCount++ >= 12) break;
                    out.append(" ").append(m.getName()).append("(");
                    Class<?>[] params = m.getParameterTypes();
                    for (int i = 0; i < params.length && i < 3; i++) {
                        if (i > 0) out.append(",");
                        out.append(params[i].getSimpleName());
                    }
                    if (params.length > 3) out.append(",...");
                    out.append(")");
                }
                out.append("\n");
            } catch (Throwable error) {
                out.append("NOT_FOUND_OR_REFLECTION_ERROR ")
                   .append(error.getClass().getSimpleName()).append("\n");
            }
        }
        return out.length() > 12000 ? out.substring(0, 12000) + "\nTRUNCATED" : out.toString();
    }

    /**
     * Shared, read-only load report for modern API 101 and legacy API 82.
     * A failed provider call remains retryable via SystemUiHeartbeat.
     */
    public static void reportFromModern(Context context, ClassLoader loader) {
        synchronized (SystemUiEntry.class) {
            if (reportedThisProcess) return;
            try {
                Uri uri = Uri.parse("content://" + com.oneuialoha.app.BuildConfig.APPLICATION_ID + ".statusbridge");
                android.os.Bundle report = new android.os.Bundle();
                report.putString("probe", probeDetails(loader));
                android.os.Bundle response = context.getContentResolver().call(uri, "loaded", null, report);
                if (response != null && response.getBoolean("accepted", false)) {
                    reportedThisProcess = true;
                }
            } catch (Throwable ignored) {
                // The heartbeat retry handles a temporarily unavailable manager.
            }
        }
    }

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (!"com.android.systemui".equals(param.packageName)
                || !"com.android.systemui".equals(param.processName)) return;
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam hook) {
                    if (hook.args[0] instanceof Context) {
                        reportFromModern((Context) hook.args[0], param.classLoader);
                    }
                }
            });
        } catch (Throwable ignored) {
            // Never interrupt SystemUI.
        }
    }
}
