package com.alohaoneui.hook.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.alohaoneui.common.StatusBarRuntimeBridge
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The single in-memory diagnostics sink for AlohaOneUI's injected SystemUI hooks.
 *
 * Feature hooks call record(category, message). Entries are bounded and exported
 * only after the user selects Settings → Send logs; there is no persistent
 * Logcat spam, per-feature debug preference, polling, or app-process heartbeat.
 */
object AlohaOneUIDiagnosticLog {
    private const val MAX_ENTRIES = 600
    private const val MAX_TOTAL_CHARS = 96 * 1024
    private const val MAX_ENTRY_CHARS = 2048

    private val lock = Any()
    private val entries = ArrayDeque<String>()
    private val globalShortcutEntries = ArrayDeque<String>()
    private val receiverInstalled = AtomicBoolean(false)
    private val systemServerReceiverInstalled = AtomicBoolean(false)
    private var totalChars = 0
    private const val MAX_GLOBAL_SHORTCUT_ENTRIES = 48
    private const val GLOBAL_SHORTCUT_CATEGORY = "AlohaOneUI-GlobalQuickActions"
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
            if (category == GLOBAL_SHORTCUT_CATEGORY) {
                while (globalShortcutEntries.size >= MAX_GLOBAL_SHORTCUT_ENTRIES) {
                    globalShortcutEntries.removeFirst()
                }
                globalShortcutEntries.addLast(line)
            }
        }
    }

    fun install(context: Context) {
        val app = context.applicationContext ?: context

        if (receiverInstalled.compareAndSet(false, true)) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != StatusBarRuntimeBridge.ACTION_DIAGNOSTIC_LOG_REQUEST) return
                    val requestId = intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_REQUEST_ID)
                    if (requestId.isNullOrBlank()) return

                    // A prior bridge registration may have failed even though the main
                    // Send logs receiver is already active. Retry it on the next request.
                    install(context)
                    record("AlohaOneUIDiagnostics", "Settings requested the SystemUI diagnostic buffer.")
                    val snapshot = runCatching {
                        liveSnapshotProvider?.invoke()
                            ?: "(Live status-bar geometry provider is not registered.)"
                    }.getOrElse {
                        "Live status-bar geometry snapshot failed: ${it.javaClass.simpleName}: ${it.message.orEmpty()}"
                    }.take(48 * 1024)
                    record(
                        "AlohaOneUIDiagnostics",
                        "On-demand live geometry snapshot collected: ${snapshot.length} characters.",
                    )
                    val payload = buildString {
                        append(dump())
                        appendLine()
                        appendLine("[Live status-bar geometry snapshot]")
                        appendLine(snapshot)
                    }
                    val reply = Intent(StatusBarRuntimeBridge.ACTION_DIAGNOSTIC_LOG_RESPONSE)
                        .setPackage(StatusBarRuntimeBridge.ALOHAONEUI_APP_PACKAGE)
                        .putExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_REQUEST_ID, requestId)
                        .putExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_LOG_TEXT, payload)
                    runCatching { context.sendBroadcast(reply) }
                        .onFailure {
                            record(
                                "AlohaOneUIDiagnostics",
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
                record("AlohaOneUIDiagnostics", "On-demand SystemUI diagnostics receiver registered.")
            } catch (error: Exception) {
                receiverInstalled.set(false)
                record(
                    "AlohaOneUIDiagnostics",
                    "Receiver registration failed: ${error.javaClass.simpleName}: ${error.message.orEmpty()}",
                )
            }
        }

        // Retry this receiver independently if first registration failed. The general
        // Send logs receiver may already be installed, so its one-time guard must not
        // prevent the SystemServer runtime diagnostics channel from recovering.
        if (systemServerReceiverInstalled.compareAndSet(false, true)) {
            runCatching {
                val systemServerEvents = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action != StatusBarRuntimeBridge.ACTION_SYSTEM_SERVER_DIAGNOSTIC_EVENT) return
                        val category = intent.getStringExtra(
                            StatusBarRuntimeBridge.EXTRA_SYSTEM_SERVER_DIAGNOSTIC_CATEGORY,
                        ) ?: return
                        val message = intent.getStringExtra(
                            StatusBarRuntimeBridge.EXTRA_SYSTEM_SERVER_DIAGNOSTIC_MESSAGE,
                        ) ?: return
                        if (category != GLOBAL_SHORTCUT_CATEGORY) return
                        record(category, message)
                    }
                }
                val eventFilter = IntentFilter(StatusBarRuntimeBridge.ACTION_SYSTEM_SERVER_DIAGNOSTIC_EVENT)
                if (Build.VERSION.SDK_INT >= 33) {
                    app.registerReceiver(
                        systemServerEvents,
                        eventFilter,
                        Manifest.permission.DUMP,
                        null,
                        Context.RECEIVER_EXPORTED,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    app.registerReceiver(
                        systemServerEvents,
                        eventFilter,
                        Manifest.permission.DUMP,
                        null,
                    )
                }
                record("AlohaOneUIDiagnostics", "SystemServer global-shortcut diagnostics bridge registered.")
            }.onFailure {
                systemServerReceiverInstalled.set(false)
                record(
                    "AlohaOneUIDiagnostics",
                    "SystemServer diagnostics bridge registration failed: ${it.javaClass.simpleName}: ${it.message.orEmpty()}",
                )
            }
        }
    }

    private fun dump(): String = synchronized(lock) {
        buildString {
            if (entries.isEmpty()) {
                appendLine("(No SystemUI diagnostic events have been recorded since this process started.)")
            } else {
                appendLine("Buffered events: ${entries.size}")
                appendLine("Buffer limit: ${MAX_ENTRIES} entries / ${MAX_TOTAL_CHARS} characters")
                entries.forEach { appendLine(it) }
            }
            appendLine()
            appendLine("[SystemServer global-shortcut runtime diagnostics]")
            if (globalShortcutEntries.isEmpty()) {
                appendLine("(No bridged system_server global-shortcut events captured in this process.)")
            } else {
                globalShortcutEntries.forEach { appendLine(it) }
            }
        }
    }
}
