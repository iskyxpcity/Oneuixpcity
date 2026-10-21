package com.alohaoneui.app.ui.util

import android.content.Context
import java.io.File

/** On-demand, non-root AlohaOneUI diagnostic export. */
fun getBugreportFile(context: Context): File {
    val destination = File(context.cacheDir, "AlohaOneUI_diagnostic.txt")
    destination.writeText(buildAlohaOneUIDiagnosticReport(context))
    return destination
}
