package me.weishu.kernelsu.statusbar;

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

/** A lightweight liveness check for the SystemUI process, without modifying SystemUI views. */
public final class SystemUiHeartbeat implements IXposedHookLoadPackage {
    private static boolean started = false;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (!"com.android.systemui".equals(param.packageName)
                || !"com.android.systemui".equals(param.processName)) return;
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam hook) {
                    final Context context = (Context) hook.args[0];
                    synchronized (SystemUiHeartbeat.class) {
                        if (started) return;
                        started = true;
                    }
                    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                        Thread thread = new Thread(runnable, "OneuiXP-SystemUI-heartbeat");
                        thread.setDaemon(true);
                        return thread;
                    });
                    executor.scheduleWithFixedDelay(() -> {
                        try {
                            Uri uri = Uri.parse("content://" + me.weishu.kernelsu.BuildConfig.APPLICATION_ID + ".statusbridge");
                            Bundle result = context.getContentResolver().call(uri, "heartbeat", null, null);
                            if (result == null || !result.getBoolean("accepted", false)) {
                                // Recover if the one-time load callback ran before the provider was ready.
                                Bundle retry = new Bundle();
                                retry.putString("probe", "LOAD_RETRY: original probe unavailable");
                                context.getContentResolver().call(uri, "loaded", null, retry);
                            }
                        } catch (Throwable ignored) {
                            // Do not interrupt SystemUI or terminate the monitor on transient errors.
                        }
                    }, 0L, 5L, TimeUnit.SECONDS);
                }
            });
        } catch (Throwable ignored) {
            // A ROM-specific failure must never prevent SystemUI from starting.
        }
    }
}
