package me.weishu.kernelsu.ui.screen.home

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.statusbar.LsposedFrameworkStatus
import me.weishu.kernelsu.statusbar.StatusBridge

@Immutable
data class LsposedHomeStatus(
    val active: Boolean,
    val frameworkConnected: Boolean,
    val scopeActive: Boolean,
    val injected: Boolean,
    val injectionCount: Int,
    val scopeCount: Int,
)

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
    val heartbeatRevision = remember { mutableIntStateOf(0) }
    val heartbeatTick = remember { mutableIntStateOf(0) }

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

    // Read LSPosed's actual configured scope immediately and keep it live while this
    // composable exists. The Binder call is done off the main thread.
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                LsposedFrameworkStatus.refresh()
            }
            heartbeatTick.intValue++
            delay(250L)
        }
    }

    // Re-read heartbeat whenever SystemUI notifies us; the revision also makes the
    // current heartbeat value participate in recomposition.
    val injected = remember(heartbeatRevision.intValue, heartbeatTick.intValue) {
        StatusBridge.recentlyLoaded(context)
    }

    val scopeActive = framework.systemUiScopeActive
    val active = scopeActive || injected
    val actualScopeCount = when {
        framework.connected -> framework.scope.size
        injected -> configuredScopes
        else -> 0
    }

    return LsposedHomeStatus(
        active = active,
        frameworkConnected = framework.connected,
        scopeActive = scopeActive,
        injected = injected,
        injectionCount = if (injected) 1 else 0,
        scopeCount = actualScopeCount,
    )
}
