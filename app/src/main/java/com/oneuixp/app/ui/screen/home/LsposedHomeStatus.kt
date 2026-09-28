package com.oneuixp.app.ui.screen.home

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.oneuixp.app.oneuix.OneuiXFeatureCatalog
import com.oneuixp.app.oneuix.oneuiXDataStore
import com.oneuixp.app.statusbar.LsposedFrameworkStatus
import com.oneuixp.app.statusbar.StatusBridge
import com.oneuixp.app.statusbar.SystemUiPingProbe
import io.github.soclear.oneuix.common.Preference

@Immutable
data class LsposedHomeStatus(
    val active: Boolean,
    val frameworkConnected: Boolean,
    val scopeKnown: Boolean,
    val scopeActive: Boolean,
    val injected: Boolean,
    val injectionCount: Int,
    val scopeCount: Int,
)

val LocalLsposedHomeStatus = staticCompositionLocalOf {
    LsposedHomeStatus(
        active = false,
        frameworkConnected = false,
        scopeKnown = false,
        scopeActive = false,
        injected = false,
        injectionCount = 0,
        scopeCount = 0,
    )
}

private fun configuredScopeCount(context: Context): Int = runCatching {
    context.assets.open("xposed_scope").bufferedReader().useLines { lines ->
        lines.map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .count()
    }
}.getOrDefault(0)

/**
 * Shows two independent facts:
 * 1) LSPosed module/service + the manager's actual configured scope (direct service API).
 * 2) Whether SystemUI has really loaded this module in the running process (heartbeat bridge).
 *
 * This means the UI can show "scope activated" immediately, without waiting for SystemUI restart.
 */
@Composable
fun rememberLsposedHomeStatus(): LsposedHomeStatus {
    val context = LocalContext.current.applicationContext
    val configuredScopes = remember(context) { configuredScopeCount(context) }
    val framework by LsposedFrameworkStatus.state.collectAsState()
    val preference by context.oneuiXDataStore.data.collectAsState(initial = Preference())
    val heartbeatRevision = remember { mutableIntStateOf(0) }
    val heartbeatTick = remember { mutableIntStateOf(0) }
    val pingAlive = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                heartbeatRevision.intValue++
            }
        }
        context.contentResolver.registerContentObserver(
            StatusBridge.statusUri(context),
            true,
            observer,
        )
        onDispose {
            runCatching {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    // Flux-style activation refresh: query libxposed while the activity is
    // visible, and independently PING the live SystemUI hook as a fallback.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                withContext(Dispatchers.IO) {
                    LsposedFrameworkStatus.refresh()
                }
                heartbeatTick.intValue++
                delay(250L)
            }
        }
    }

    LaunchedEffect(lifecycleOwner, context) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                pingAlive.value = SystemUiPingProbe.probe(context)
                delay(500L)
            }
        }
    }

    // Re-read heartbeat whenever SystemUI notifies us; the revision also makes the
    // current heartbeat value participate in recomposition.
    val injected = remember(heartbeatRevision.intValue, heartbeatTick.intValue) {
        StatusBridge.recentlyLoaded(context)
    }

    val scopeActive = framework.systemUiScopeActive
    // Match Flux_Decor's connection semantics exactly:
    // when libxposed can answer, its live scope is authoritative; only if the
    // service/scope query is unavailable do we fall back to a directed ordered
    // PING answered by the currently hooked SystemUI process.
    val active = if (framework.connected && framework.scopeKnown) {
        scopeActive
    } else {
        pingAlive.value
    }
    val featureInjectionCount = remember(preference) {
        OneuiXFeatureCatalog.enabledInjectionCount(preference)
    }
    val actualScopeCount = when {
        framework.connected && framework.scopeKnown -> framework.scope.size
        injected -> configuredScopes
        else -> 0
    }

    return LsposedHomeStatus(
        active = active,
        frameworkConnected = framework.connected,
        scopeKnown = framework.scopeKnown,
        scopeActive = scopeActive,
        injected = pingAlive.value,
        injectionCount = featureInjectionCount,
        scopeCount = actualScopeCount,
    )
}
