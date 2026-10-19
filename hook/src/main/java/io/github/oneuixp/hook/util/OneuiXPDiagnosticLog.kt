package io.github.oneuixp.hook.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import io.github.oneuixp.common.StatusBarRuntimeBridge
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The single in-memory diagnostics sink for OneuiXP's injected SystemUI hooks.
 *
 * Feature hooks call record(category, message). Entries are bounded and exported
 * only after the user selects Settings → Send logs; there is no persistent
 * Logcat spam, per-feature debug preference, polling, or app-process heartbeat.
 */
object OneuiXPDiagnosticLog {
    private const val MAX_ENTRIES = 600
    private const val MAX_TOTAL_CHARS = 96 * 1024
    private const val MAX_ENTRY_CHARS = 2048

    private val lock = Any()
    private val entries = ArrayDeque<String>()
    private val receiverInstalled = AtomicBoolean(false)
    private var totalChars = 0
    @Volatile private var liveSnapshotProvider: (() -> String)? = null

    /** Supplies the live geometry snapshot collected by the existing Send logs action. */
    fun setLiveSnapshotProvider(provider: (() -> String)?) {
        liveSnapshotProvider = provider
    }

    fun record(category: String, message: String) {
        val safeMessage = message.replace('\n', ' ').replace('\r', ' ').take(MAX_ENTRY_CHARS)
        val line = "${System.currentTimeMillis()} [${category}] ${safeMessage}"
        synchronized(lock) {
            while (
                entries.isNotEmpty() &&
                (entries.size >= MAX_ENTRIES || totalChars + line.length > MAX_TOTAL_CHARS)
            ) {
                totalChars -= entries.removeFirst().length
            }
            entries.addLast(line)
            totalChars += line.length
        }
    }

    fun install(context: Context) {
        if (!receiverInstalled.compareAndSet(false, true)) return
        val app = context.applicationContext ?: context
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != StatusBarRuntimeBridge.ACTION_DIAGNOSTIC_LOG_REQUEST) return
                val requestId = intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_REQUEST_ID)
                if (requestId.isNullOrBlank()) return

                record("OneuiXPDiagnostics", "Settings requested the SystemUI diagnostic buffer.")
                val snapshot = runCatching {
                    liveSnapshotProvider?.invoke()
                        ?: "(Live status-bar geometry provider is not registered.)"
                }.getOrElse {
                    "Live status-bar geometry snapshot failed: ${it.javaClass.simpleName}: ${it.message.orEmpty()}"
                }.take(48 * 1024)
                record(
                    "OneuiXPDiagnostics",
                    "On-demand live geometry snapshot collected: ${snapshot.length} characters.",
                )
                val payload = buildString {
                    append(dump())
                    appendLine()
                    appendLine("[Live status-bar geometry snapshot]")
                    appendLine(snapshot)
                }
                val reply = Intent(StatusBarRuntimeBridge.ACTION_DIAGNOSTIC_LOG_RESPONSE)
                    .setPackage(StatusBarRuntimeBridge.ONEUIXP_APP_PACKAGE)
                    .putExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_REQUEST_ID, requestId)
                    .putExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_LOG_TEXT, payload)
                runCatching { context.sendBroadcast(reply) }
                    .onFailure {
                        record(
                            "OneuiXPDiagnostics",
                            "Failed to return diagnostic buffer: ${it.javaClass.simpleName}: ${it.message.orEmpty()}",
                        )
                    }
            }
        }
        try {
            val filter = IntentFilter(StatusBarRuntimeBridge.ACTION_DIAGNOSTIC_LOG_REQUEST)
            if (Build.VERSION.SDK_INT >= 33) {
                app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                app.registerReceiver(receiver, filter)
            }
            record("OneuiXPDiagnostics", "On-demand SystemUI diagnostics receiver registered.")
        } catch (error: Exception) {
            receiverInstalled.set(false)
            record(
                "OneuiXPDiagnostics",
                "Receiver registration failed: ${error.javaClass.simpleName}: ${error.message.orEmpty()}",
            )
        }
    }

    private fun dump(): String = synchronized(lock) {
        if (entries.isEmpty()) {
            "(No SystemUI diagnostic events have been recorded since this process started.)"
        } else {
            buildString {
                appendLine("Buffered events: ${entries.size}")
                appendLine("Buffer limit: ${MAX_ENTRIES} entries / ${MAX_TOTAL_CHARS} characters")
                entries.forEach { appendLine(it) }
            }
        }
    }
}
