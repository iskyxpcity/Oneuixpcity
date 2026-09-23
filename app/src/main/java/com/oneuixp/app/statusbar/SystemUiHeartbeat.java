package com.oneuixp.app.statusbar;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Safe liveness check reused by both API 82 legacy and modern API 101 entry.
 * Neither entry installs UI mutations in the SystemUI process.
 */
public final class SystemUiHeartbeat implements IXposedHookLoadPackage {
    private static boolean started = false;

    public static synchronized void start(Context context) {
        if (started) return;
        started = true;
        final Context appContext = context.getApplicationContext() != null
            ? context.getApplicationContext() : context;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "OneuiXP-SystemUI-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                Uri uri = Uri.parse("content://" + com.oneuixp.app.BuildConfig.APPLICATION_ID + ".statusbridge");
                Bundle result = appContext.getContentResolver().call(uri, "heartbeat", null, null);
                if (result == null || !result.getBoolean("accepted", false)) {
                    Bundle retry = new Bundle();
                    retry.putString("probe", "LOAD_RETRY: original probe unavailable");
                    appContext.getContentResolver().call(uri, "loaded", null, retry);
                }
            } catch (Throwable ignored) {
                // Keep SystemUI safe, even when the manager app is force-stopped.
            }
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (!"com.android.systemui".equals(param.packageName)
                || !"com.android.systemui".equals(param.processName)) return;
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam hook) {
                    if (hook.args[0] instanceof Context) start((Context) hook.args[0]);
                }
            });
        } catch (Throwable ignored) {
            // Never interrupt SystemUI.
        }
    }
}
