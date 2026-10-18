package com.oneuixp.app.ui.screen.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.widget.Toast
import com.oneuixp.app.R

private const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"
private const val LSPOSED_MANAGER_MAIN_ACTIVITY =
    "org.lsposed.manager.ui.activity.MainActivity"
private const val ANDROID_PER_USER_RANGE = 100_000

/**
 * Opens LSPosed Manager directly on OneuiXP's own module detail/scope screen.
 *
 * The LSPosed Manager deep-link format is module://<packageName>:<userId>.
 * Launch its MainActivity explicitly first so the deep link is delivered to the
 * activity that handles it; fall back to the package launcher for older builds
 * that do not expose the current MainActivity component name.
 */
fun openLsposedModuleActivation(context: Context) {
    val moduleUri = Uri.parse(
        "module://${context.packageName}:${Process.myUid() / ANDROID_PER_USER_RANGE}",
    )
    val flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP

    val directIntent = Intent(Intent.ACTION_VIEW, moduleUri).apply {
        component = ComponentName(
            LSPOSED_MANAGER_PACKAGE,
            LSPOSED_MANAGER_MAIN_ACTIVITY,
        )
        addFlags(flags)
    }

    val openedDirectly = runCatching {
        context.startActivity(directIntent)
    }.isSuccess

    if (openedDirectly) return

    val launcherIntent = runCatching {
        context.packageManager.getLaunchIntentForPackage(LSPOSED_MANAGER_PACKAGE)
    }.getOrNull()

    if (launcherIntent != null) {
        launcherIntent.data = moduleUri
        launcherIntent.addFlags(flags)
        val openedByLauncher = runCatching {
            context.startActivity(launcherIntent)
        }.isSuccess
        if (openedByLauncher) return
    }

    Toast.makeText(
        context,
        R.string.home_lsposed_manager_unavailable,
        Toast.LENGTH_LONG,
    ).show()
}
