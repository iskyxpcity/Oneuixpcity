package me.weishu.kernelsu.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import me.weishu.kernelsu.statusbar.StatusBridge

@Immutable
data class LsposedHomeStatus(
    val active: Boolean,
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

private fun readLsposedHomeStatus(context: Context, scopeCount: Int): LsposedHomeStatus {
    // Distinguish the shipped scope declaration from a confirmed, effective
    // injection. Checking the LSPosed switch cannot inject into an already
    // running SystemUI process; only the trusted live bridge verifies this.
    val active = StatusBridge.recentlyLoaded(context)
    return LsposedHomeStatus(
        active = active,
        injectionCount = if (active) 1 else 0, // confirmed SystemUI process, not hook count
        scopeCount = if (active) scopeCount else 0,
    )
}

/**
 * Updates quickly once SystemUI actually sends its first trusted load report.
 * Scope selection alone is deliberately NOT shown as successful injection.
 */
@Composable
fun rememberLsposedHomeStatus(): LsposedHomeStatus {
    val context = LocalContext.current.applicationContext
    val configuredScopes = remember(context) { configuredScopeCount(context) }
    val status by produceState(
        initialValue = readLsposedHomeStatus(context, configuredScopes),
        key1 = context,
        key2 = configuredScopes,
    ) {
        while (true) {
            value = readLsposedHomeStatus(context, configuredScopes)
            delay(250L)
        }
    }
    return status
}
