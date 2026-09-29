package com.oneuixp.app.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.oneuixp.app.BuildConfig
import com.oneuixp.app.statusbar.LsposedFrameworkStatus
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun buildOneuiXPDiagnosticReport(context: Context): String {
    val framework = LsposedFrameworkStatus.state.value
    return buildString {
        appendLine("OneuiXP diagnostic")
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

fun oneuiXPDiagnosticFileName(): String {
    val stamp = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    return "OneuiXP_diagnostic_${stamp}.txt"
}

fun writeOneuiXPDiagnosticReport(context: Context, uri: Uri) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(buildOneuiXPDiagnosticReport(context).toByteArray(Charsets.UTF_8))
    } ?: error("Could not open diagnostic output")
}

fun createOneuiXPDiagnosticShareIntent(context: Context): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "OneuiXP diagnostic")
        putExtra(Intent.EXTRA_TEXT, buildOneuiXPDiagnosticReport(context))
    }
