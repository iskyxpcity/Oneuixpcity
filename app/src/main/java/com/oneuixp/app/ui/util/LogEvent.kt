package com.oneuixp.app.ui.util

import android.content.Context
import java.io.File

/** On-demand, non-root OneuiXP diagnostic export. */
fun getBugreportFile(context: Context): File {
    val destination = File(context.cacheDir, "OneuiXP_diagnostic.txt")
    destination.writeText(buildOneuiXPDiagnosticReport(context))
    return destination
}
