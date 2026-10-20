package com.oneuialoha.app.ui.component.configtransfer

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneuialoha.app.R
import com.oneuialoha.app.ui.LocalUiMode
import com.oneuialoha.app.ui.UiMode
import com.oneuialoha.app.ui.viewmodel.OneUIAlohaToolsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Top-left OneUIAloha configuration transfer menu.
 *
 * Keeps KernelSU 3.2.5's navigation-icon position while replacing the original
 * destination action with the application's full Preference backup/restore.
 */
@Composable
fun ConfigTransferPopup() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<OneUIAlohaToolsViewModel>()

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    viewModel.backupTo(stream)
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        viewModel.restoreFrom(stream)
                    }
                } catch (_: Throwable) {
                    Toast.makeText(
                        context,
                        R.string.oneuialoha_restore_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    val onBackup = {
        val name = "OneUIAloha_backup_${
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            )
        }.json"
        backupLauncher.launch(name)
    }
    val onRestore = {
        restoreLauncher.launch(arrayOf("application/json"))
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> ConfigTransferPopupMiuix(
            onBackup = onBackup,
            onRestore = onRestore,
        )
        UiMode.Material -> ConfigTransferPopupMaterial(
            onBackup = onBackup,
            onRestore = onRestore,
        )
    }
}
