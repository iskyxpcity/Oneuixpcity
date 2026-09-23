package me.weishu.kernelsu.ui.util

import android.content.Context
import me.weishu.kernelsu.statusbar.StatusBridge
import java.io.File

/** Non-root, application-only diagnostic export for legacy callers. */
fun getBugreportFile(context: Context): File {
    val destination = File(context.cacheDir, "OneuiXP_diagnostic.txt")
    destination.writeText(
        "OneuiXP diagnostic\\n" +
        "SystemUI loaded: ${StatusBridge.recentlyLoaded(context)}\\n" +
        "Report count: ${StatusBridge.loadReportCount(context)}\\n" +
        "Last report age (s): ${StatusBridge.lastReportAgeSeconds(context)}\\n" +
        StatusBridge.diagnosticSummary(context) + "\\n",
    )
    return destination
}
