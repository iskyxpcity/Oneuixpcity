package com.oneuixp.app.ui.screen.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.oneuixp.app.R

private const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"

/**
 * Opens LSPosed Manager directly on OneuiXP's module detail/scope screen.
 * Current LSPosed MainActivity handles the external `module://<package>:<userId>`
 * URI and navigates to the selected module when its manager service is available.
 */
fun openLsposedModuleActivation(context: Context) {
    val launchIntent = runCatching {
        context.packageManager.getLaunchIntentForPackage(LSPOSED_MANAGER_PACKAGE)
    }.getOrNull()

    if (launchIntent == null) {
        Toast.makeText(
            context,
            R.string.home_lsposed_manager_unavailable,
            Toast.LENGTH_LONG,
        ).show()
        return
    }

    launchIntent.data = Uri.parse("module://${context.packageName}:0")
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching {
        context.startActivity(launchIntent)
    }.onFailure {
        Toast.makeText(
            context,
            R.string.home_lsposed_manager_unavailable,
            Toast.LENGTH_LONG,
        ).show()
    }
}
