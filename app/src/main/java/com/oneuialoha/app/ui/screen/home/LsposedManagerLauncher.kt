package com.oneuialoha.app.ui.screen.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.oneuialoha.app.R

private const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"

/**
 * Opens the LSPosed Manager app.
 *
 * The external module:// route is conditional on LSPosed Manager's internal
 * Binder state and does not reliably open this module's scope page on all
 * devices. Keep this action predictable: launch the manager's normal entry
 * point once, without a deep link or delayed retries.
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

    launchIntent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP,
    )

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
