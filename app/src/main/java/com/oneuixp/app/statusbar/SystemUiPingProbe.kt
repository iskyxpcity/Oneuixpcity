package com.oneuixp.app.statusbar

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Flux-style on-demand SystemUI probe.
 *
 * V2 intentionally uses a new action/protocol so an old receiver left inside an
 * already-running SystemUI process cannot make a newly installed manager build
 * look active after the LSPosed module has been disabled.
 */
object SystemUiPingProbe {
    const val ACTION_PING = "com.oneuixp.app.PING.V2"
    const val EXTRA_PROTOCOL = "com.oneuixp.app.PING_PROTOCOL"
    const val PROTOCOL = "oneuixp-flux-v2"
    private const val SYSTEM_UI = "com.android.systemui"

    suspend fun probe(context: Context): Boolean =
        withTimeoutOrNull(250L) {
            suspendCancellableCoroutine { continuation ->
                val finalReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (!continuation.isActive) return
                        val protocol = resultExtras?.getString(EXTRA_PROTOCOL)
                        continuation.resume(
                            resultCode == Activity.RESULT_OK && protocol == PROTOCOL
                        )
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
                        Bundle(),
                    )
                }.onFailure {
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        } ?: false
}
