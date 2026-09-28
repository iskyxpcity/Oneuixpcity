package com.oneuixp.app.statusbar

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Flux-style live SystemUI probe.
 *
 * This is an on-demand ordered broadcast to the running SystemUI hook. A stale
 * persisted heartbeat cannot satisfy it; only the live injected receiver can
 * return RESULT_OK.
 */
object SystemUiPingProbe {
    private const val ACTION_PING = "com.oneuixp.app.PING"
    private const val SYSTEM_UI = "com.android.systemui"

    suspend fun probe(context: Context): Boolean =
        withTimeoutOrNull(150L) {
            suspendCancellableCoroutine { continuation ->
                val finalReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (continuation.isActive) {
                            continuation.resume(resultCode == Activity.RESULT_OK)
                        }
                    }
                }

                val intent = Intent(ACTION_PING)
                    .setPackage(SYSTEM_UI)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        }
                    }

                runCatching {
                    context.sendOrderedBroadcast(
                        intent,
                        null,
                        finalReceiver,
                        Handler(Looper.getMainLooper()),
                        Activity.RESULT_CANCELED,
                        null,
                        null,
                    )
                }.onFailure {
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        } ?: false
}
