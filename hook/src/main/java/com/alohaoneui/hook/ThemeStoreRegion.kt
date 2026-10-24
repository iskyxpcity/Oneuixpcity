package com.alohaoneui.hook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Process
import android.os.SystemClock
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import com.alohaoneui.common.ModuleActions
import com.alohaoneui.common.Package
import com.alohaoneui.common.Preference
import com.alohaoneui.hook.util.PreferenceProvider
import com.alohaoneui.hook.util.xlog
import java.util.Locale

/**
 * Theme Store region spoofing, ported from LaobaiAppFreezerModule
 * (`SystemUiRootHookThemeRegion.kt` + `SystemUiRootHookThemeStore.kt`).
 *
 * The Store resolves its content region from several independent sources. Patching only one
 * of them leaves the UI half-localised (region tab changes but the catalogue, currency and
 * CSC-gated categories do not), so all three groups are patched here:
 *
 *  1. `SemFloatingFeature` / `SemCscFeature` `getString(...)` — OMC / sales code / locale
 *  2. `SystemProperties` / `SemSystemProperties` `get(...)` — ro.csc.sales_code, region, etc.
 *  3. `TelephonyManager` — SIM / network country ISO and MCC+MNC operator string
 *
 * Two differences from the original module matter for it actually working:
 *
 *  - The hooks are installed **unconditionally**; the region is resolved per call (throttled
 *    re-read of the remote preference file). Turning the switch on later in the app therefore
 *    takes effect without re-hooking the store process.
 *  - The region is re-read at runtime instead of being snapshotted at process start, so
 *    switching the region and restarting the Store is enough — no reboot, no LSPosed reload.
 */
object ThemeStoreRegion {
    /** Remote preference is re-read at most this often while the Store is calling into us. */
    private const val REGION_CACHE_TTL_MS = 1_500L

    /** Keep the module log readable: only the first N rewritten keys are reported. */
    private const val MAX_LOGGED_KEYS = 40

    private class Region(
        val country: String,
        val salesCode: String,
        val localeTag: String,
        val mcc: String,
        val mnc: String,
    )

    context(xposedModule: XposedModule, param: XposedModuleInterface.PackageReadyParam)
    fun hook(settings: Preference.Other) {
        if (param.packageName != Package.THEME_STORE) return
        val loader = param.classLoader
        val lock = Any()
        var cachedRegion: Region? = regionOf(settings)
        var cachedAtMs = SystemClock.elapsedRealtime()
        var loggedKeys = 0

        /**
         * Live region. Mirrors the original module's per-call `Settings.Global` lookups: the
         * switch and the five region fields are re-read from the remote preference file, so an
         * already-hooked Store process picks up a change instead of keeping its first snapshot.
         */
        fun currentRegion(): Region? = synchronized(lock) {
            val now = SystemClock.elapsedRealtime()
            if (now - cachedAtMs >= REGION_CACHE_TTL_MS) {
                cachedAtMs = now
                runCatching {
                    PreferenceProvider.loadPreference()?.other
                }.getOrNull()?.let { live ->
                    cachedRegion = regionOf(live)
                }
            }
            cachedRegion
        }

        fun logHit(what: String) {
            if (loggedKeys >= MAX_LOGGED_KEYS) return
            loggedKeys++
            xlog("theme store region: $what")
        }

        fun hookStringGetters(className: String, methodName: String) {
            runCatching {
                val clazz = loader.loadClass(className)
                var installed = 0
                clazz.declaredMethods
                    .filter { method ->
                        method.name == methodName &&
                            method.parameterTypes.size in 1..2 &&
                            method.parameterTypes[0] == String::class.java
                    }
                    .forEach { method ->
                        xposedModule.hook(method).intercept { chain ->
                            val key = chain.args.firstOrNull() as? String
                            val region = currentRegion()
                            val patched = if (key != null && region != null) {
                                valueForKey(key, region)
                            } else {
                                null
                            }
                            if (patched != null) {
                                logHit("$className.$methodName($key) -> $patched")
                                patched
                            } else {
                                chain.proceed()
                            }
                        }
                        installed++
                    }
                xlog("theme store region: $className.$methodName hooked ($installed)")
            }.onFailure { xlog(it) }
        }

        hookStringGetters("com.samsung.android.feature.SemFloatingFeature", "getString")
        hookStringGetters("com.samsung.android.feature.SemCscFeature", "getString")
        hookStringGetters("android.os.SystemProperties", "get")
        hookStringGetters("android.os.SemSystemProperties", "get")

        // Zero-root restart. The Store resolves and caches its region for the lifetime of the
        // process, exactly why the original module had to force-stop it. Doing that from a
        // root shell is unnecessary now that we are already inside the process: the app sends
        // a broadcast and the Store ends itself, so the next launch resolves the new region.
        runCatching {
            val appClass = loader.loadClass("android.app.Application")
            val attach = appClass.declaredMethods.firstOrNull { method ->
                method.name == "attach" &&
                    method.parameterTypes.isNotEmpty() &&
                    Context::class.java.isAssignableFrom(method.parameterTypes[0])
            } ?: error("Application.attach(Context) not found")
            xposedModule.hook(attach).intercept { chain ->
                val result = chain.proceed()
                runCatching {
                    val context = (chain.thisObject as? Context)
                        ?: (chain.args.firstOrNull() as? Context)
                    if (context != null) registerRegionChangeReceiver(context)
                }.onFailure { xlog(it) }
                result
            }
            xlog("theme store region: restart receiver installed")
        }.onFailure { xlog(it) }

        runCatching {
            val clazz = loader.loadClass("android.telephony.TelephonyManager")
            val operatorMethods = setOf("getNetworkOperator", "getSimOperator")
            val countryMethods = setOf("getNetworkCountryIso", "getSimCountryIso")
            var installed = 0
            clazz.declaredMethods
                .filter {
                    it.parameterTypes.isEmpty() &&
                        (it.name in operatorMethods || it.name in countryMethods)
                }
                .forEach { method ->
                    xposedModule.hook(method).intercept { chain ->
                        val region = currentRegion()
                        if (region == null) {
                            chain.proceed()
                        } else if (method.name in operatorMethods) {
                            region.mcc + region.mnc
                        } else {
                            region.country.lowercase(Locale.US)
                        }
                    }
                    installed++
                }
            xlog("theme store region: TelephonyManager hooked ($installed)")
        }.onFailure { xlog(it) }

        val initial = cachedRegion
        xlog(
            "theme store region active: " + (
                initial?.let {
                    "country=${it.country} salesCode=${it.salesCode} " +
                        "locale=${it.localeTag} mcc=${it.mcc} mnc=${it.mnc}"
                } ?: "disabled (no region stored yet)"
                ),
        )
    }

    /**
     * Ends the Store process when the region changes. `Context.RECEIVER_EXPORTED` is required
     * because the broadcast comes from the module app; the action is namespaced to this module
     * and the sender targets the Store package, so nothing else can trigger it.
     */
    context(xposedModule: XposedModule)
    private fun registerRegionChangeReceiver(context: Context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != ModuleActions.THEME_STORE_REGION_CHANGED) return
                xlog("theme store region: preference changed, ending store process")
                runCatching { Process.killProcess(Process.myPid()) }
            }
        }
        runCatching {
            val filter = IntentFilter(ModuleActions.THEME_STORE_REGION_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
        }.onFailure { xlog(it) }
    }

    /** Same key mapping as the original module, including the `.countryiso` suffix form. */
    private fun valueForKey(key: String, region: Region): String? {
        val normalized = key.lowercase(Locale.US)
        return when {
            normalized.contains("sales_code") ||
                normalized.contains("salescode") ||
                normalized.contains("omc_code") ||
                normalized.contains("omccode") -> region.salesCode

            normalized.contains("locale") ||
                normalized.contains("language") -> region.localeTag

            normalized == "mcc" ||
                normalized.endsWith(".mcc") ||
                normalized.contains("regionmcc") -> region.mcc

            normalized == "mnc" ||
                normalized.endsWith(".mnc") -> region.mnc

            normalized.contains("mcc_mnc") ||
                normalized.contains("mccmnc") -> region.mcc + region.mnc

            normalized.contains("country") ||
                normalized.contains("countryiso") ||
                normalized.contains("region") ||
                normalized.endsWith(".cc") ||
                normalized.endsWith(".countryiso") -> region.country

            normalized.contains("csc") -> region.salesCode

            else -> null
        }
    }

    private fun regionOf(settings: Preference.Other): Region? {
        if (!settings.setThemeStoreRegion) return null
        val country = settings.themeStoreRegionCountry.trim().uppercase(Locale.US)
            .takeIf { it.length == 2 } ?: DEFAULT_COUNTRY
        val salesCode = settings.themeStoreRegionSalesCode.trim().uppercase(Locale.US)
            .takeIf { it.isNotBlank() } ?: DEFAULT_SALES_CODE
        val localeTag = settings.themeStoreRegionLocale.trim()
            .takeIf { it.isNotBlank() } ?: DEFAULT_LOCALE
        val mcc = settings.themeStoreRegionMcc.trim().filter(Char::isDigit)
            .takeIf { it.length == 3 } ?: DEFAULT_MCC
        val mnc = settings.themeStoreRegionMnc.trim().filter(Char::isDigit)
            .takeIf { it.length in 2..3 } ?: DEFAULT_MNC
        return Region(country, salesCode, localeTag, mcc, mnc)
    }

    private const val DEFAULT_COUNTRY = "KR"
    private const val DEFAULT_SALES_CODE = "KOO"
    private const val DEFAULT_LOCALE = "ko-KR"
    private const val DEFAULT_MCC = "450"
    private const val DEFAULT_MNC = "08"
}
