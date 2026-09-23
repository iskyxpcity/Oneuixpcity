package me.weishu.kernelsu.statusbar;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import java.lang.reflect.Method;

/**
 * Modern libxposed API 101 entry, modelled on the verified Flux Decor module
 * registration format. Only the SystemUI main process is hooked, read-only.
 * It replaces the old entry in modern LSPosed while preserving the existing
 * trusted heartbeat/provider behavior.
 */
public final class ModernSystemUiEntry extends XposedModule {
    private static final String TAG = "OneuiXP-ModernXposed";
    private volatile boolean inSystemUiProcess;
    private volatile boolean hookInstalled;

    public ModernSystemUiEntry() {
        super();
    }

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        inSystemUiProcess = "com.android.systemui".equals(param.getProcessName());
        Log.i(TAG, "Modern module loaded in " + param.getProcessName());
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!inSystemUiProcess || !"com.android.systemui".equals(param.getPackageName())) {
            return;
        }
        if (hookInstalled) return;
        try {
            // Intercept the same safe Android lifecycle point used by the
            // legacy integration; do not hook vendor-specific views here.
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attach).intercept(chain -> {
                Object result = chain.proceed();
                try {
                    Object params = chain.getArgs();
                    Context ctx = null;
                    if (params instanceof java.util.List) {
                        java.util.List<?> values = (java.util.List<?>) params;
                        if (!values.isEmpty() && values.get(0) instanceof Context) {
                            ctx = (Context) values.get(0);
                        }
                    } else if (params instanceof Object[]) {
                        Object[] values = (Object[]) params;
                        if (values.length > 0 && values[0] instanceof Context) {
                            ctx = (Context) values[0];
                        }
                    }
                    if (ctx != null) {
                        SystemUiEntry.reportFromModern(ctx, param.getDefaultClassLoader());
                        SystemUiHeartbeat.start(ctx);
                    }
                } catch (Throwable e) {
                    Log.w(TAG, "Modern load-report callback failed safely", e);
                }
                return result;
            });
            hookInstalled = true;
            Log.i(TAG, "Modern SystemUI Application.attach hook installed");
        } catch (Throwable e) {
            // Do not break SystemUI on unsupported ROM/runtime combinations.
            Log.w(TAG, "Modern SystemUI hook unavailable; leaving process untouched", e);
        }
    }
}
