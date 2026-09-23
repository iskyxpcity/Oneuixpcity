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

private fun configuredScopeCount(context: Context): Int =
    runCatching {
        context.assets.open("xposed_scope").bufferedReader().useLines { lines ->
            lines.map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .count()
        }
    }.getOrDefault(0)

private fun readLsposedHomeStatus(
    context: Context,
    scopeCount: Int,
): LsposedHomeStatus {
    val active = StatusBridge.recentlyLoaded(context)
    return LsposedHomeStatus(
        active = active,
        injectionCount = if (active) 1 else 0,
        scopeCount = scopeCount,
    )
}

/**
 * Poll the trusted SystemUI load bridge so the home card follows LSPosed/SystemUI
 * state without requiring a page change or app restart.
 */
@Composable
fun rememberLsposedHomeStatus(): LsposedHomeStatus {
    val context = LocalContext.current.applicationContext
    val scopeCount = remember(context) { configuredScopeCount(context) }
    val status by produceState(
        initialValue = readLsposedHomeStatus(context, scopeCount),
        key1 = context,
        key2 = scopeCount,
    ) {
        while (true) {
            value = readLsposedHomeStatus(context, scopeCount)
            delay(1_000L)
        }
    }
    return status
}
