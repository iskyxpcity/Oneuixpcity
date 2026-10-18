package com.oneuixp.app.ui.screen.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Toast
import com.oneuixp.app.R

private const val LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager"
private const val LSPOSED_MANAGER_MAIN_ACTIVITY =
    "org.lsposed.manager.ui.activity.MainActivity"
private const val ANDROID_PER_USER_RANGE = 100_000
private val MODULE_DEEP_LINK_RETRY_DELAYS_MS = listOf(700L, 1_700L, 3_500L)

/**
 * Opens LSPosed Manager directly on OneuiXP's own module detail/scope screen.
 *
 * LSPosed Manager currently only handles module:// intents once its service
 * Binder is alive. On a cold launch it may receive and ignore the first intent
 * before that connection is ready, leaving the user on the manager home page.
 * Re-deliver the same deep link a few times through singleTop/clearTop so the
 * existing MainActivity handles it via onNewIntent after startup settles.
 */
fun openLsposedModuleActivation(context: Context) {
    val appContext = context.applicationContext
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

    val startedIntent = runCatching {
        appContext.startActivity(directIntent)
        directIntent
    }.getOrNull()

    val intentToRetry = startedIntent ?: run {
        val launcherIntent = runCatching {
            appContext.packageManager.getLaunchIntentForPackage(LSPOSED_MANAGER_PACKAGE)
        }.getOrNull()?.apply {
            data = moduleUri
            addFlags(flags)
        }

        launcherIntent?.takeIf { intent ->
            runCatching { appContext.startActivity(intent) }.isSuccess
        }
    }

    if (intentToRetry == null) {
        Toast.makeText(
            context,
            R.string.home_lsposed_manager_unavailable,
            Toast.LENGTH_LONG,
        ).show()
        return
    }

    // MainActivity.handleIntent() in LSPosed only routes module:// when its
    // Binder is already connected. These bounded re-deliveries cover cold start.
    val handler = Handler(Looper.getMainLooper())
    MODULE_DEEP_LINK_RETRY_DELAYS_MS.forEach { delayMs ->
        handler.postDelayed(
            {
                runCatching {
                    appContext.startActivity(Intent(intentToRetry).apply {
                        data = moduleUri
                        addFlags(flags)
                    })
                }
            },
            delayMs,
        )
    }
}
