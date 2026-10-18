package com.oneuixp.app.ui.util.update

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oneuixp.app.BuildConfig
import com.oneuixp.app.R
import com.oneuixp.app.ui.util.checkNewVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A version request from the home update card reopens the same global update dialog.
 * The monotonically increasing sequence ensures tapping the same version emits again.
 */
data class UpdateDialogRequest(val sequence: Long, val info: LatestVersionInfo)

object UpdateDialogRequests {
    private val sequence = AtomicLong(0L)
    private val _requests = MutableStateFlow<UpdateDialogRequest?>(null)
    val requests: StateFlow<UpdateDialogRequest?> = _requests.asStateFlow()

    fun open(info: LatestVersionInfo) {
        _requests.value = UpdateDialogRequest(sequence.incrementAndGet(), info)
    }

    fun clear(requestSequence: Long) {
        if (_requests.value?.sequence == requestSequence) {
            _requests.value = null
        }
    }
}

fun requestUpdateDialog(info: LatestVersionInfo) {
    UpdateDialogRequests.open(info)
}

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
        UpdateDialogRequests.requests.collect { request ->
            if (request != null) {
                UpdateDialogRequests.clear(request.sequence)
                updateInfo = request.info
                dialogVisible = true
                statusMessage = null
            }
        }
    }

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
    if (isMiuix) {
        UpdateDialogMiuix(
            info = info,
            isBusy = isBusy,
            statusMessage = statusMessage,
            onDismiss = onDismiss,
            onUpdate = onUpdate,
        )
    } else {
        UpdateDialogMaterial(
            info = info,
            isBusy = isBusy,
            statusMessage = statusMessage,
            onDismiss = onDismiss,
            onUpdate = onUpdate,
        )
    }
}

@Composable
private fun UpdateDialogMaterial(
    info: LatestVersionInfo,
    isBusy: Boolean,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    val container = MaterialTheme.colorScheme.surface
    val onContainer = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
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
            shape = RoundedCornerShape(28.dp),
            color = container,
            contentColor = onContainer,
            shadowElevation = 10.dp,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdateAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.update_dialog_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = onContainer,
                        )
                        Text(
                            text = "OneUI XP ${info.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = onContainer,
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.update_dialog_new_badge),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.update_dialog_current_version, BuildConfig.VERSION_NAME),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )

                Text(
                    text = stringResource(R.string.update_dialog_changelog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (notes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.update_dialog_no_changelog),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        notes.forEach { note ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", color = accent, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = note,
                                    color = onContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(onContainer.copy(alpha = 0.12f)))

                if (isBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.update_dialog_downloading),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else if (!statusMessage.isNullOrBlank()) {
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !isBusy,
                        onClick = onDismiss,
                    ) {
                        Text(stringResource(R.string.update_dialog_cancel))
                    }
                    Button(
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !isBusy,
                        onClick = onUpdate,
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.update_dialog_install))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDialogMiuix(
    info: LatestVersionInfo,
    isBusy: Boolean,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val notes = remember(info.changelog) { normalizeReleaseNotes(info.changelog) }

    // Use Miuix's native overlay dialog and native text actions so this isn't
    // just a Material dialog recolored to resemble Miuix.
    OverlayDialog(
        show = true,
        title = stringResource(R.string.update_dialog_title),
        summary = "OneUI XP ${info.versionName}",
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiuixText(
                        text = stringResource(R.string.update_dialog_new_badge),
                        color = colors.primary,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(12.dp))
                    MiuixText(
                        text = stringResource(R.string.update_dialog_current_version, BuildConfig.VERSION_NAME),
                        color = colors.onSurfaceVariantSummary,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                    )
                }

                MiuixText(
                    text = stringResource(R.string.update_dialog_changelog_title),
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MiuixTheme.textStyles.title4.fontSize,
                )

                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (notes.isEmpty()) {
                        MiuixText(
                            text = stringResource(R.string.update_dialog_no_changelog),
                            color = colors.onSurfaceVariantSummary,
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                        )
                    } else {
                        notes.forEach { note ->
                            Row(verticalAlignment = Alignment.Top) {
                                MiuixText(
                                    text = "•",
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.width(8.dp))
                                MiuixText(
                                    text = note,
                                    color = colors.onSurface,
                                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                                )
                            }
                        }
                    }
                }

                if (isBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InfiniteProgressIndicator(color = colors.primary)
                        Spacer(Modifier.width(10.dp))
                        MiuixText(
                            text = stringResource(R.string.update_dialog_downloading),
                            color = colors.onSurfaceVariantSummary,
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                        )
                    }
                } else if (!statusMessage.isNullOrBlank()) {
                    MiuixText(
                        text = statusMessage,
                        color = colors.primary,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                    )
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    MiuixTextButton(
                        text = stringResource(R.string.update_dialog_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    MiuixTextButton(
                        text = stringResource(R.string.update_dialog_install),
                        onClick = { if (!isBusy) onUpdate() },
                        modifier = Modifier.weight(1f),
                        colors = MiuixButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        },
    )
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

