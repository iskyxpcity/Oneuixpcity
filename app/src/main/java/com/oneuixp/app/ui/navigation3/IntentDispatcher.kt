package com.oneuixp.app.ui.navigation3

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * OneuiXP no longer handles KernelSU module installation, action shortcuts or WebUI links.
 * Consume incoming intents so the activity's channel cannot accumulate stale requests.
 * LSPosed/SystemUI status reporting uses its own provider and is unaffected.
 */
@Composable
fun IntentDispatcher(intentChannel: ReceiveChannel<Intent>) {
    LaunchedEffect(intentChannel) {
        for (ignoredIntent in intentChannel) {
            // Legacy KernelSU actions are intentionally unsupported.
        }
    }
}
