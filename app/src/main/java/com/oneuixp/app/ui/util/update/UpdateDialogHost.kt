package com.oneuixp.app.ui.util.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ButtonGroupDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oneuixp.app.BuildConfig
import com.oneuixp.app.R
import com.oneuixp.app.ui.component.material.ExpressiveToggleButton
import com.oneuixp.app.ui.util.checkNewVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import com.oneuixp.app.ui.component.liquid.lens
import com.oneuixp.app.ui.component.liquid.vibrancy
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A version request from the home update card reopens the same global update dialog.
 * The monotonically increasing sequence ensures tapping the same version emits again.
 */
private val updateDialogGlassHighlight: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

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
fun UpdateDialogHost(
    isMiuix: Boolean,
    glassBackdrop: Backdrop,
    enableLiquidGlass: Boolean,
) {
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
            glassBackdrop = glassBackdrop,
            enableLiquidGlass = enableLiquidGlass,
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
    glassBackdrop: Backdrop,
    enableLiquidGlass: Boolean,
    info: LatestVersionInfo,
    isBusy: Boolean,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    if (isMiuix) {
        UpdateDialogMiuix(
            info = info,
            glassBackdrop = glassBackdrop,
            enableLiquidGlass = enableLiquidGlass,
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
    val gradientTop = remember(accent, container) {
        accent.copy(alpha = 0.45f).compositeOver(container)
    }
    val dialogShape = remember { RoundedCornerShape(28.dp) }
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
            color = Color.Transparent,
            contentColor = onContainer,
            shadowElevation = 10.dp,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(gradientTop, container, container),
                        ),
                        shape = dialogShape,
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp),
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
                            color = onContainer,
                            fontWeight = FontWeight.Bold,
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
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveToggleButton(
                        checked = false,
                        onCheckedChange = { onDismiss() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !isBusy,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    ) {
                        Text(stringResource(R.string.update_dialog_cancel))
                    }
                    ExpressiveToggleButton(
                        checked = true,
                        onCheckedChange = { onUpdate() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !isBusy,
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdateAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
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
    glassBackdrop: Backdrop,
    enableLiquidGlass: Boolean,
    isBusy: Boolean,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val notes = remember(info.changelog) { normalizeReleaseNotes(info.changelog) }
    val cardShape = remember { RoundedCornerShape(30.dp) }
    val gradientTop = colors.primary.copy(alpha = 0.32f)
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    val supportsGlass = enableLiquidGlass && isRenderEffectSupported()

    BackHandler(enabled = true) {
        if (!isBusy) onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.26f))
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = { if (!isBusy) onDismiss() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .clip(cardShape)
                .then(
                    if (supportsGlass) {
                        Modifier.drawBackdrop(
                            backdrop = glassBackdrop,
                            shape = { cardShape },
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx(), 4.dp.toPx())
                                lens(
                                    refractionHeight = 24.dp.toPx(),
                                    refractionAmount = 24.dp.toPx(),
                                )
                            },
                            highlight = { updateDialogGlassHighlight.copy(alpha = 0.75f) },
                            onDrawSurface = {
                                drawRect(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            gradientTop,
                                            colors.surfaceContainer.copy(alpha = 0.46f),
                                            colors.surfaceContainer.copy(alpha = 0.42f),
                                        ),
                                    ),
                                )
                            },
                        )
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colors.primary.copy(alpha = 0.26f).compositeOver(colors.surface),
                                    colors.surface,
                                    colors.surface,
                                ),
                            ),
                            cardShape,
                        )
                    }
                )
                .then(
                    if (supportsGlass) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.24f), cardShape)
                    } else {
                        Modifier
                    },
                )
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    MiuixIcon(
                        imageVector = Icons.Filled.SystemUpdateAlt,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MiuixText(
                        text = stringResource(R.string.update_dialog_title),
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = MiuixTheme.textStyles.title4.fontSize,
                    )
                    MiuixText(
                        text = "OneUI XP ${info.versionName}",
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                    )
                }
                Spacer(Modifier.width(8.dp))
                MiuixText(
                    text = stringResource(R.string.update_dialog_new_badge),
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                )
            }

            MiuixText(
                text = stringResource(R.string.update_dialog_current_version, BuildConfig.VERSION_NAME),
                color = colors.onSurfaceVariantSummary,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
            )

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
                            MiuixText(text = "•", color = colors.primary, fontWeight = FontWeight.SemiBold)
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
                    color = colors.error,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixButton(
                    onClick = onDismiss,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = MiuixButtonDefaults.buttonColors(
                        color = colors.surfaceContainerHigh,
                        contentColor = colors.onSurfaceContainerHigh,
                    ),
                ) {
                    MiuixText(text = stringResource(R.string.update_dialog_cancel))
                }
                MiuixButton(
                    onClick = { if (!isBusy) onUpdate() },
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = MiuixButtonDefaults.buttonColorsPrimary(),
                ) {
                    if (isBusy) {
                        InfiniteProgressIndicator(color = colors.onPrimary)
                    } else {
                        MiuixIcon(
                            imageVector = Icons.Filled.SystemUpdateAlt,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    MiuixText(text = stringResource(R.string.update_dialog_install))
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

