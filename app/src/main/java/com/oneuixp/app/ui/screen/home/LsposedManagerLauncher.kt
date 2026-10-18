package com.oneuixp.app.ui.screen.home

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import com.oneuixp.app.R

private const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"

/**
 * If LSPosed's own module notification is still active, reuse its PendingIntent.
 * Otherwise launch the manager's normal entry point exactly once.
 *
 * Notification access is requested only after the user taps the inactive card.
 * Returning from system settings never causes an automatic launch or retry.
 */
fun openLsposedModuleActivation(context: Context) {
    if (!LsposedNotificationListenerService.hasNotificationAccess(context)) {
        AlertDialog.Builder(context)
            .setTitle(R.string.lsposed_notification_access_title)
            .setMessage(R.string.lsposed_notification_access_message)
            .setPositiveButton(R.string.lsposed_notification_access_open_settings) { _, _ ->
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure {
                    openLsposedManagerHome(context)
                }
            }
            .setNegativeButton(R.string.lsposed_open_manager_home) { _, _ ->
                openLsposedManagerHome(context)
            }
            .setOnCancelListener {
                openLsposedManagerHome(context)
            }
            .show()
        return
    }

    if (LsposedNotificationListenerService.openOwnModuleNotification(context)) {
        return
    }

    openLsposedManagerHome(context)
}

private fun openLsposedManagerHome(context: Context) {
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
