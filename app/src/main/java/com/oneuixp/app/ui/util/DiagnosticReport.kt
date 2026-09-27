package com.oneuixp.app.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.oneuixp.app.BuildConfig
import com.oneuixp.app.statusbar.StatusBridge
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun buildOneuiXPDiagnosticReport(context: Context): String = buildString {
    appendLine("OneuiXP diagnostic")
    appendLine("Generated: ${OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}")
    appendLine("Package: ${BuildConfig.APPLICATION_ID}")
    appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
    appendLine()
    appendLine("[SystemUI]")
    appendLine("Active: ${StatusBridge.recentlyLoaded(context)}")
    appendLine("Successful hook groups: ${StatusBridge.successfulHookCount(context)}")
    appendLine("Accepted load reports: ${StatusBridge.loadReportCount(context)}")
    appendLine("Last report age (s): ${StatusBridge.lastReportAgeSeconds(context)}")
    appendLine()
    appendLine("[Probe]")
    appendLine(StatusBridge.diagnosticSummary(context))
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
