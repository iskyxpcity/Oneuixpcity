package me.weishu.kernelsu.statusbar;

import android.content.ContentProvider;
import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.content.SharedPreferences;

/** Accept load reports only from the SystemUI UID. */
public final class StatusBridge extends ContentProvider {
    private static final String PREF = "statusbar_lsposed_bridge";
    private static final int MAX_PROBE_LENGTH = 12_000;
    @Override public boolean onCreate() { return true; }
    @Override public synchronized Bundle call(String method, String arg, Bundle extras) {
        if ("loaded".equals(method)) {
            Context context = getContext();
            if (context == null) throw new IllegalStateException("Provider context unavailable");
            int uid = android.os.Binder.getCallingUid();
            String[] names = context.getPackageManager().getPackagesForUid(uid);
            boolean systemUi = false;
            if (names != null) for (String name : names) if ("com.android.systemui".equals(name)) systemUi = true;
            if (!systemUi || uid == Process.myUid()) throw new SecurityException("SystemUI caller required");
            SharedPreferences prefs = context.getSharedPreferences(PREF, 0);
            String probe = extras == null ? "NOT_RUN" : extras.getString("probe", "NOT_RUN");
            if (probe == null) probe = "NOT_RUN";
            if (probe.length() > MAX_PROBE_LENGTH) probe = probe.substring(0, MAX_PROBE_LENGTH);
            long elapsed = SystemClock.elapsedRealtime();
            long bootEpoch = System.currentTimeMillis() - elapsed;
            long previousBoot = prefs.getLong("boot_epoch", Long.MIN_VALUE);
            boolean sameBoot = previousBoot != Long.MIN_VALUE
                && Math.abs(bootEpoch - previousBoot) < 120_000L
                && elapsed >= prefs.getLong("elapsed", -1L);
            int count = sameBoot ? prefs.getInt("load_reports", 0) : 0;
            boolean persisted = prefs.edit()
                .putLong("boot_epoch", bootEpoch)
                .putLong("elapsed", elapsed)
                .putInt("load_reports", count == Integer.MAX_VALUE ? count : count + 1)
                .putString("probe", probe)
                .commit();
            Bundle result = new Bundle(); result.putBoolean("accepted", persisted); return result;
        }
        throw new IllegalArgumentException("Unknown operation");
    }
    public static boolean recentlyLoaded(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, 0);
        long recorded = prefs.getLong("elapsed", -1L);
        long now = SystemClock.elapsedRealtime();
        long previousBoot = prefs.getLong("boot_epoch", Long.MIN_VALUE);
        long currentBoot = System.currentTimeMillis() - now;
        return recorded >= 0 && now >= recorded && previousBoot != Long.MIN_VALUE
            && Math.abs(currentBoot - previousBoot) < 120_000L;
    }
    /** Counts accepted load reports, not distinct injections or LSPosed scopes. */
    public static int loadReportCount(android.content.Context context) {
        return recentlyLoaded(context)
            ? context.getSharedPreferences(PREF, 0).getInt("load_reports", 0) : 0;
    }
    /** Age of the latest accepted SystemUI report; -1 means no report this boot. */
    public static long lastReportAgeSeconds(android.content.Context context) {
        if (!recentlyLoaded(context)) return -1L;
        long recorded = context.getSharedPreferences(PREF, 0).getLong("elapsed", -1L);
        return recorded < 0 ? -1L : (SystemClock.elapsedRealtime() - recorded) / 1000L;
    }
    public static String diagnosticSummary(android.content.Context context) {
        if (!recentlyLoaded(context)) return "NOT_RUN";
        return context.getSharedPreferences(PREF, 0).getString("probe", "NOT_RUN");
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
