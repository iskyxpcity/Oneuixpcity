package com.oneuixp.app.ui.screen.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.widget.Toast
import com.oneuixp.app.R

private const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"
private const val ANDROID_PER_USER_RANGE = 100_000

/**
 * Requests LSPosed Manager to open OneuiXP's own module detail/scope page.
 *
 * LSPosed exposes this navigation through MainActivity's module:// deep link
 * and only routes it when its manager Binder is alive. Send the request once.
 * Do not schedule delayed retries: they can relaunch LSPosed after the user has
 * already returned to OneuiXP, creating an unwanted navigation loop.
 */
fun openLsposedModuleActivation(context: Context) {
    val moduleUri = Uri.parse(
        "module://${context.packageName}:${Process.myUid() / ANDROID_PER_USER_RANGE}",
    )
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

    launchIntent.data = moduleUri
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
