package me.weishu.kernelsu.ui.screen.home

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val active = StatusBridge.recentlyLoaded(context)
    return LsposedHomeStatus(
        active = active,
        injectionCount = if (active) 1 else 0,
        scopeCount = if (active) scopeCount else 0,
    )
}

/**
 * React immediately when SystemUI reports module load/heartbeat.
 * A short polling loop remains only as a fallback and to detect heartbeat expiry.
 */
@Composable
fun rememberLsposedHomeStatus(): LsposedHomeStatus {
    val context = LocalContext.current.applicationContext
    val configuredScopes = remember(context) { configuredScopeCount(context) }
    var status by remember(context, configuredScopes) {
        mutableStateOf(readLsposedHomeStatus(context, configuredScopes))
    }
    val revision = remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                revision.intValue++
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

    LaunchedEffect(context, configuredScopes, revision.intValue) {
        status = readLsposedHomeStatus(context, configuredScopes)
    }

    LaunchedEffect(context, configuredScopes) {
        while (true) {
            delay(250L)
            val latest = readLsposedHomeStatus(context, configuredScopes)
            if (latest != status) status = latest
        }
    }

    return status
}
