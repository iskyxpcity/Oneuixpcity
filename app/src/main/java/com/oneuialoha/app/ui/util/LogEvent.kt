package com.oneuialoha.app.ui.util

import android.content.Context
import java.io.File

/** On-demand, non-root OneUIAloha diagnostic export. */
fun getBugreportFile(context: Context): File {
    val destination = File(context.cacheDir, "OneUIAloha_diagnostic.txt")
    destination.writeText(buildOneUIAlohaDiagnosticReport(context))
    return destination
}
