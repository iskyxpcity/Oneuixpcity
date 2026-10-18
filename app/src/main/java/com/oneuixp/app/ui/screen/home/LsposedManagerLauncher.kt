package com.oneuixp.app.ui.screen.home

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
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
 * LSPosed Manager handles module:// intents only after its service Binder is
 * alive. On a cold launch it can ignore the first intent before the connection
 * is ready. Re-deliver the same deep link with a bounded PendingIntent retry so
 * the existing MainActivity can handle it via onNewIntent after startup settles.
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

    val deepLinkPendingIntent = runCatching {
        PendingIntent.getActivity(
            appContext,
            SystemClock.uptimeMillis().toInt(),
            intentToRetry,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull() ?: return

    // Android 14+ requires the PendingIntent sender to explicitly opt in to
    // background activity starts. These short retries are tied to the user's
    // card tap and only re-deliver this exact LSPosed module deep link.
    val retryOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val mode = if (Build.VERSION.SDK_INT >= 36) {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        } else {
            @Suppress("DEPRECATION")
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(mode)
            .toBundle()
    } else {
        null
    }

    val handler = Handler(Looper.getMainLooper())
    MODULE_DEEP_LINK_RETRY_DELAYS_MS.forEach { delayMs ->
        handler.postDelayed(
            {
                runCatching {
                    if (retryOptions != null) {
                        deepLinkPendingIntent.send(
                            appContext,
                            0,
                            null,
                            null,
                            null,
                            null,
                            retryOptions,
                        )
                    } else {
                        deepLinkPendingIntent.send()
                    }
                }
            },
            delayMs,
        )
    }
}
