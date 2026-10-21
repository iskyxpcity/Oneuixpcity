package com.alohaoneui.app.ui.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import com.alohaoneui.app.BuildConfig
import com.alohaoneui.app.statusbar.LsposedFrameworkStatus
import com.alohaoneui.common.Package
import com.alohaoneui.common.StatusBarRuntimeBridge
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

fun buildAlohaOneUIDiagnosticReport(context: Context): String {
    val framework = LsposedFrameworkStatus.state.value
    return buildString {
        appendLine("AlohaOneUI diagnostic")
        appendLine("Generated: ${OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}")
        appendLine("Package: ${BuildConfig.APPLICATION_ID}")
        appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine()
        appendLine("[LSPosed]")
        appendLine("Service connected: ${framework.connected}")
        appendLine("Scope known: ${framework.scopeKnown}")
        appendLine("SystemUI scope: ${framework.systemUiScopeActive}")
        appendLine("SystemUI ping: ${framework.pingAlive}")
        appendLine("Active: ${framework.active}")
        appendLine("Scope count: ${framework.scope.size}")
        appendLine()
        appendLine("[Probe]")
        appendLine("Flux-style on-demand PING only; persistent heartbeat bridge removed.")
    }
}

/**
 * Builds the single canonical export used by Settings → Send logs.
 * SystemUI records bounded events in memory and replies only to an explicit export
 * request. This adds no polling, restart, or background heartbeat.
 */
suspend fun buildAlohaOneUIDiagnosticReportForExport(context: Context): String {
    val appReport = buildAlohaOneUIDiagnosticReport(context)
    val runtimeLog = requestSystemUiDiagnosticLog(context)
    return buildString {
        append(appReport)
        appendLine()
        appendLine("[SystemUI runtime diagnostics]")
        appendLine(runtimeLog)
    }
}

private suspend fun requestSystemUiDiagnosticLog(context: Context): String {
    val appContext = context.applicationContext ?: context
    val requestId = UUID.randomUUID().toString()
    val response = CompletableDeferred<String?>()
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != StatusBarRuntimeBridge.ACTION_DIAGNOSTIC_LOG_RESPONSE) return
            if (intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_REQUEST_ID) != requestId) return
            response.complete(intent.getStringExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_LOG_TEXT))
        }
    }
    var receiverRegistered = false
    try {
        val filter = IntentFilter(StatusBarRuntimeBridge.ACTION_DIAGNOSTIC_LOG_RESPONSE)
        if (Build.VERSION.SDK_INT >= 33) {
            // Only accept the reply from a privileged process such as SystemUI.
            appContext.registerReceiver(
                receiver,
                filter,
                Manifest.permission.DUMP,
                null,
                Context.RECEIVER_EXPORTED,
            )
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter, Manifest.permission.DUMP, null)
        }
        receiverRegistered = true
        appContext.sendBroadcast(
            Intent(StatusBarRuntimeBridge.ACTION_DIAGNOSTIC_LOG_REQUEST)
                .setPackage(Package.SYSTEMUI)
                .putExtra(StatusBarRuntimeBridge.EXTRA_DIAGNOSTIC_REQUEST_ID, requestId),
        )
        return withTimeoutOrNull(5000L) { response.await() }
            ?: "No SystemUI diagnostic response received within 5 seconds. " +
                "The SystemUI hook may not be active yet or the live geometry snapshot did not finish; " +
                "the app status report is still included."
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        return "Unable to collect SystemUI diagnostics: ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
    } finally {
        if (receiverRegistered) runCatching { appContext.unregisterReceiver(receiver) }
    }
}

fun alohaOneUIDiagnosticFileName(): String {
    val stamp = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    return "AlohaOneUI_diagnostic_${stamp}.txt"
}

fun writeAlohaOneUIDiagnosticReport(
    context: Context,
    uri: Uri,
    reportText: String = buildAlohaOneUIDiagnosticReport(context),
) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(reportText.toByteArray(Charsets.UTF_8))
    } ?: error("Could not open diagnostic output")
}

fun createAlohaOneUIDiagnosticShareIntent(
    context: Context,
    reportText: String = buildAlohaOneUIDiagnosticReport(context),
): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, "AlohaOneUI diagnostic")
    putExtra(Intent.EXTRA_TEXT, reportText)
}
