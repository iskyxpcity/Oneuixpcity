package com.oneuixp.app.ui.component.material

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.oneuixp.app.R
import com.oneuixp.app.statusbar.StatusBridge
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun oneuiXPDiagnostic(context: android.content.Context): String =
    "OneuiXP diagnostic\n" +
    "SystemUI loaded this boot: ${StatusBridge.recentlyLoaded(context)}\n" +
    "Accepted load reports this boot (not injections): ${StatusBridge.loadReportCount(context)}\n" +
    "Most recent report age (seconds): ${StatusBridge.lastReportAgeSeconds(context)}\n\n" +
    StatusBridge.diagnosticSummary(context) + "\n"

@Composable
fun SendLogBottomSheet(onDismiss: () -> Unit, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val saveTitle = stringResource(R.string.save_log)
    val shareTitle = stringResource(R.string.send_log)
    val savedText = stringResource(R.string.log_saved)
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val success = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(oneuiXPDiagnostic(context).toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open diagnostic output")
                }.isSuccess
                snackbarHostState.showSnackbar(if (success) savedText else "OneuiXP log export failed")
            }
        }
        onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(
                    modifier = Modifier.size(64.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        val stamp = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))
                        saveLauncher.launch("OneuiXP_diagnostic_${stamp}.txt")
                    },
                ) { Icon(Icons.Filled.Save, contentDescription = saveTitle) }
                Spacer(Modifier.height(8.dp))
                Text(saveTitle)
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(
                    modifier = Modifier.size(64.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        context.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "OneuiXP diagnostic")
                                putExtra(Intent.EXTRA_TEXT, oneuiXPDiagnostic(context))
                            },
                            shareTitle,
                        ))
                        onDismiss()
                    },
                ) { Icon(Icons.Filled.Share, contentDescription = shareTitle) }
                Spacer(Modifier.height(8.dp))
                Text(shareTitle)
            }
        }
    }
}
