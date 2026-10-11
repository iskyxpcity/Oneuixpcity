package com.oneuixp.app.ui.component.miuix

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import com.oneuixp.app.ui.util.createOneuiXPDiagnosticShareIntent
import com.oneuixp.app.ui.util.buildOneuiXPDiagnosticReportForExport
import com.oneuixp.app.ui.util.oneuiXPDiagnosticFileName
import com.oneuixp.app.ui.util.writeOneuiXPDiagnosticReport
import com.oneuixp.app.ui.component.dialog.LoadingDialogHandle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import kotlinx.coroutines.launch

@Composable
fun SendLogDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    loadingDialog: LoadingDialogHandle,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reportToExport by remember { mutableStateOf<String?>(null) }
    val savedText = stringResource(R.string.log_saved)
    val shareTitle = stringResource(R.string.send_log)
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                writeOneuiXPDiagnosticReport(
                    context,
                    uri,
                    reportToExport ?: buildOneuiXPDiagnosticReport(context),
                )
                Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                Toast.makeText(context, "诊断日志保存失败：${error.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            }
        }
    }
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        insideMargin = DpSize(0.dp, 0.dp),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                text = stringResource(R.string.send_log),
                fontSize = MiuixTheme.textStyles.title4.fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = colorScheme.onSurface
            )
            ArrowPreference(
                title = stringResource(R.string.save_log),
                startAction = {
                    Icon(Icons.Rounded.Save, contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp), tint = colorScheme.onSurface)
                },
                onClick = {
                    scope.launch {
                        reportToExport = buildOneuiXPDiagnosticReportForExport(context)
                        saveLauncher.launch(oneuiXPDiagnosticFileName())
                        onDismissRequest()
                    }
                },
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            )
            ArrowPreference(
                title = stringResource(R.string.send_log),
                startAction = {
                    Icon(Icons.Rounded.Share, contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp), tint = colorScheme.onSurface)
                },
                onClick = {
                    scope.launch {
                        val report = buildOneuiXPDiagnosticReportForExport(context)
                        context.startActivity(
                            Intent.createChooser(
                                createOneuiXPDiagnosticShareIntent(context, report),
                                shareTitle,
                            )
                        )
                        onDismissRequest()
                    }
                },
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            )
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp)
                    .padding(horizontal = 24.dp)
            )
        }
    )
}
