package com.oneuixp.app.ui.util.update

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oneuixp.app.BuildConfig
import com.oneuixp.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Checks the public OneUI XP release feed without blocking the first frame.
 * A dialog is only shown when the release feed advertises a newer stable version.
 */
@Composable
fun UpdateDialogHost(isMiuix: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<LatestVersionInfo?>(null) }
    var dialogVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val candidate = withContext(Dispatchers.IO) { checkNewVersion() }
        if (candidate.versionName.isNotBlank() && candidate.downloadUrl.isNotBlank()) {
            updateInfo = candidate
            dialogVisible = true
        }
    }

    val latest = updateInfo
    if (dialogVisible && latest != null) {
        UpdateDialog(
            isMiuix = isMiuix,
            info = latest,
            isBusy = busy,
            statusMessage = statusMessage,
            onDismiss = { if (!busy) dialogVisible = false },
            onUpdate = {
                if (!busy) {
                    scope.launch {
                        busy = true
                        statusMessage = null
                        try {
                            when (val downloaded = withContext(Dispatchers.IO) {
                                downloadAndVerifyUpdate(context, latest)
                            }) {
                                is UpdateDownloadResult.Success -> {
                                    when (val install = withContext(Dispatchers.IO) {
                                        submitUpdateSession(context, downloaded.apk)
                                    }) {
                                        UpdateInstallResult.Submitted -> {
                                            dialogVisible = false
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.update_dialog_install_submitted),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                        UpdateInstallResult.PermissionRequired -> {
                                            val opened = openUnknownSourcesSettings(context)
                                            statusMessage = context.getString(
                                                if (opened) R.string.update_dialog_permission_needed
                                                else R.string.update_dialog_permission_open_failed
                                            )
                                        }
                                        is UpdateInstallResult.Error -> {
                                            statusMessage = context.getString(
                                                R.string.update_dialog_failed_detail,
                                                install.message,
                                            )
                                        }
                                    }
                                }
                                is UpdateDownloadResult.Error -> {
                                    statusMessage = context.getString(
                                        R.string.update_dialog_failed_detail,
                                        downloaded.message,
                                    )
                                }
                            }
                        } catch (throwable: Throwable) {
                            statusMessage = context.getString(
                                R.string.update_dialog_failed_detail,
                                throwable.message ?: context.getString(R.string.update_dialog_unknown_error),
                            )
                        } finally {
                            busy = false
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun UpdateDialog(
    isMiuix: Boolean,
    info: LatestVersionInfo,
    isBusy: Boolean,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    val container = if (isMiuix) MiuixTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
    val onContainer = if (isMiuix) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
    val accent = if (isMiuix) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val onAccent = if (isMiuix) Color.White else MaterialTheme.colorScheme.onPrimary
    val muted = onContainer.copy(alpha = 0.68f)
    val dialogShape = RoundedCornerShape(if (isMiuix) 32.dp else 26.dp)
    val actionShape = RoundedCornerShape(50.dp)
    val notes = remember(info.changelog) { normalizeReleaseNotes(info.changelog) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isBusy,
            dismissOnClickOutside = !isBusy,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 440.dp),
            shape = dialogShape,
            color = container,
            contentColor = onContainer,
            shadowElevation = if (isMiuix) 12.dp else 8.dp,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.13f),
                    ) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdateAlt,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(25.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.update_dialog_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onContainer,
                        )
                        Text(
                            text = "OneUI XP ${info.versionName}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = onContainer,
                        )
                    }
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        contentColor = accent,
                        shape = RoundedCornerShape(50.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.update_dialog_new_badge),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.update_dialog_current_version, BuildConfig.VERSION_NAME),
                    color = muted,
                    fontSize = 13.sp,
                )

                Text(
                    text = stringResource(R.string.update_dialog_changelog_title),
                    color = onContainer,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )

                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (notes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.update_dialog_no_changelog),
                            color = muted,
                            fontSize = 14.sp,
                        )
                    } else {
                        notes.forEach { note ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = note,
                                    color = onContainer,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                )
                            }
                        }
                    }
                }

                Box(
                    Modifier.fillMaxWidth().height(1.dp).background(onContainer.copy(alpha = 0.10f))
                )

                if (isBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = accent,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.update_dialog_downloading),
                            color = muted,
                            fontSize = 13.sp,
                        )
                    }
                } else if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage,
                        color = accent,
                        fontSize = 13.sp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.update_dialog_auto_restart),
                        color = muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.weight(1f).height(48.dp)
                            .clip(actionShape)
                            .background(container)
                            .border(1.dp, accent.copy(alpha = 0.65f), actionShape)
                            .clickable(enabled = !isBusy, onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.update_dialog_cancel),
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Box(
                        modifier = Modifier.weight(1f).height(48.dp)
                            .clip(actionShape)
                            .background(accent)
                            .clickable(enabled = !isBusy, onClick = onUpdate),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = onAccent,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.update_dialog_install),
                                color = onAccent,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun normalizeReleaseNotes(markdown: String): List<String> =
    markdown
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?m)^\\s*#{1,6}\\s*"), "")
        .replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("<[^>]+>"), "")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

