package com.oneuixp.app.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.oneuixp.app.oneuix.OneuiXFeatureCatalog
import com.oneuixp.app.oneuix.oneuiXDataStore
import com.oneuixp.app.statusbar.LsposedFrameworkStatus
import com.oneuixp.app.statusbar.SystemUiPingProbe
import io.github.soclear.oneuix.common.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext

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
 * Home activation:
 * refresh once whenever the UI enters RESUMED, then rely on service bind/death
 * callbacks. The LSPosed service is the enabled-state gate; PING only tells us
 * whether the already-enabled module is live inside SystemUI.
 * No 250 ms scope polling and no 500 ms PING polling.
 */
@Composable
fun rememberLsposedHomeStatus(): LsposedHomeStatus {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuredScopes = remember(context) { configuredScopeCount(context) }
    val framework by LsposedFrameworkStatus.state.collectAsState()
    val preference by context.oneuiXDataStore.data.collectAsState(initial = Preference())

    LaunchedEffect(lifecycleOwner, context) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            withContext(Dispatchers.IO) {
                LsposedFrameworkStatus.refreshScope()
            }
            LsposedFrameworkStatus.updatePingAlive(
                SystemUiPingProbe.probe(context),
            )
            awaitCancellation()
        }
    }

    val featureInjectionCount = remember(preference) {
        OneuiXFeatureCatalog.enabledInjectionCount(preference)
    }

    val actualScopeCount = when {
        framework.connected && framework.scopeKnown -> framework.scope.size
        else -> 0
    }

    return LsposedHomeStatus(
        active = framework.active,
        frameworkConnected = framework.connected,
        scopeKnown = framework.scopeKnown,
        scopeActive = framework.systemUiScopeActive,
        injected = framework.connected && framework.pingAlive,
        injectionCount = featureInjectionCount,
        scopeCount = actualScopeCount,
    )
}
