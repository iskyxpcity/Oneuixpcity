package com.oneuixp.app.ui.screen.tools

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneuixp.app.R
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.oneuix.OneuiXPreferenceBridge
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.dialog.rememberConfirmDialog
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.util.BlurredBar
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import com.oneuixp.app.ui.viewmodel.OneuiXToolsViewModel
import io.github.soclear.oneuix.ui.component.LocalOneuiXGroupedPreference
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SwitchItem
import io.github.soclear.oneuix.ui.component.material.SegmentedColumn
import io.github.soclear.oneuix.ui.component.material.SegmentedListItem
import io.github.soclear.oneuix.ui.component.material.oneuiXSegmentedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * Source-ported Call Return controls. State, persistence and hook configuration are shared;
 * only presentation components differ between Material and Miuix.
 */
@Composable
fun CallReturnDelayScreen() {
    val navigator = LocalNavigator.current
    val model = viewModel<OneuiXToolsViewModel>()
    val preference by model.preference.collectAsStateWithLifecycle()
    val settings = preference.other
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedSeconds = settings.callReturnDelaySeconds.coerceIn(0, 5)
    var seconds by remember(savedSeconds) { mutableFloatStateOf(savedSeconds.toFloat()) }

    val applyDialog = rememberConfirmDialog(
        onConfirm = {
            scope.launch {
                Toast.makeText(context, context.getString(R.string.oneuix_callReturnApplyStarted), Toast.LENGTH_SHORT).show()
                if (!OneuiXPreferenceBridge.synchronizeNow(oneuiXPApp)) {
                    Toast.makeText(context, context.getString(R.string.oneuix_preferenceSync_failed), Toast.LENGTH_LONG).show()
                    return@launch
                }
                val restarted = withContext(Dispatchers.IO) {
                    runCatching {
                        val process = ProcessBuilder(
                            "su", "-c", "am force-stop com.samsung.android.incallui",
                        ).redirectErrorStream(true).start()
                        process.inputStream.use { it.readBytes() }
                        process.waitFor() == 0
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    context.getString(
                        if (restarted) R.string.oneuix_callReturnApplied
                        else R.string.oneuix_callReturnApplyFailed,
                    ),
                    Toast.LENGTH_LONG,
                ).show()
            }
        },
    )
    val applyTitle = stringResource(R.string.oneuix_callReturnApplyTitle)
    val applyMessage = stringResource(R.string.oneuix_callReturnApplyMessage)
    val applyNow = stringResource(R.string.oneuix_callReturnApplyNow)
    val applyLater = stringResource(R.string.oneuix_callReturnApplyLater)
    val askApplyNow = {
        applyDialog.showConfirm(
            title = applyTitle,
            content = applyMessage,
            confirm = applyNow,
            dismiss = applyLater,
        )
    }
    val status = if (settings.callReturnDelayEnabled) {
        stringResource(R.string.oneuix_callReturnStateEnabled, seconds.toInt().coerceIn(0, 5))
    } else {
        stringResource(R.string.oneuix_callReturnStateDisabled)
    }
    val onEnabledChange: (Boolean) -> Unit = { enabled ->
        model.updateData { current ->
            current.copy(other = current.other.copy(callReturnDelayEnabled = enabled))
        }
        askApplyNow()
    }
    val onSecondsPreview: (Float) -> Unit = { seconds = it }
    val onSecondsCommit: () -> Unit = {
        val finalSeconds = seconds.toInt().coerceIn(0, 5)
        model.updateData { current ->
            current.copy(other = current.other.copy(callReturnDelaySeconds = finalSeconds))
        }
        askApplyNow()
    }

    when (LocalUiMode.current) {
        UiMode.Material -> CallReturnDelayMaterial(
            enabled = settings.callReturnDelayEnabled,
            status = status,
            seconds = seconds,
            onBack = navigator::pop,
            onEnabledChange = onEnabledChange,
            onSecondsPreview = onSecondsPreview,
            onSecondsCommit = onSecondsCommit,
        )
        UiMode.Miuix -> CallReturnDelayMiuix(
            enabled = settings.callReturnDelayEnabled,
            status = status,
            seconds = seconds,
            onBack = navigator::pop,
            onEnabledChange = onEnabledChange,
            onSecondsPreview = onSecondsPreview,
            onSecondsCommit = onSecondsCommit,
        )
    }
}

@Composable
private fun CallReturnDelayMaterial(
    enabled: Boolean, status: String, seconds: Float,
    onBack: () -> Unit, onEnabledChange: (Boolean) -> Unit,
    onSecondsPreview: (Float) -> Unit, onSecondsCommit: () -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = { Text(stringResource(R.string.oneuix_callReturnDelay_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scroll,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SegmentedColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                content = listOf(
                    {
                        CompositionLocalProvider(LocalOneuiXGroupedPreference provides true) {
                            SwitchItem(
                                icon = ImageVector.vectorResource(R.drawable.phone_forwarded),
                                title = stringResource(R.string.oneuix_callReturnDelay_title),
                                summary = stringResource(R.string.oneuix_callReturnDelay_summary),
                                checked = enabled,
                                onCheckedChange = onEnabledChange,
                            )
                        }
                    },
                    {
                        SegmentedListItem(
                            colors = oneuiXSegmentedColors(),
                            headlineContent = {
                                CallReturnDelaySliderContent(enabled, status, seconds, onSecondsPreview, onSecondsCommit)
                            },
                        )
                    },
                ),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CallReturnDelayMiuix(
    enabled: Boolean, status: String, seconds: Float,
    onBack: () -> Unit, onEnabledChange: (Boolean) -> Unit,
    onSecondsPreview: (Float) -> Unit, onSecondsCommit: () -> Unit,
) {
    val scroll = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    val title = stringResource(R.string.oneuix_callReturnDelay_title)
    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor, title = title, largeTitle = title,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            val direction = LocalLayoutDirection.current
                            MiuixIcon(
                                modifier = Modifier.graphicsLayer {
                                    if (direction == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                            )
                        }
                    },
                    scrollBehavior = scroll,
                )
            }
        },
        popupHost = {},
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { padding ->
        Box(
            modifier = (if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier).fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize().overScrollVertical().scrollEndHaptic().verticalScroll(rememberScrollState())) {
                Card(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    CompositionLocalProvider(LocalOneuiXGroupedPreference provides true) {
                        Column {
                            SwitchItem(
                                icon = ImageVector.vectorResource(R.drawable.phone_forwarded),
                                title = stringResource(R.string.oneuix_callReturnDelay_title),
                                summary = stringResource(R.string.oneuix_callReturnDelay_summary),
                                checked = enabled,
                                onCheckedChange = onEnabledChange,
                            )
                            CallReturnDelaySliderContent(enabled, status, seconds, onSecondsPreview, onSecondsCommit)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CallReturnDelaySliderContent(
    enabled: Boolean, status: String, seconds: Float,
    onSecondsPreview: (Float) -> Unit, onSecondsCommit: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            UiMode.Miuix -> MiuixText(status, color = colorScheme.onBackground)
        }
        Spacer(Modifier.height(12.dp))
        when (LocalUiMode.current) {
            UiMode.Material -> Text(stringResource(R.string.oneuix_callReturnDelaySeconds_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            UiMode.Miuix -> MiuixText(stringResource(R.string.oneuix_callReturnDelaySeconds_title), color = colorScheme.onBackground)
        }
        Spacer(Modifier.height(4.dp))
        when (LocalUiMode.current) {
            UiMode.Material -> Text(
                stringResource(R.string.oneuix_callReturnDelaySeconds_summary, seconds.toInt().coerceIn(0, 5)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
            )
            UiMode.Miuix -> MiuixText(
                stringResource(R.string.oneuix_callReturnDelaySeconds_summary, seconds.toInt().coerceIn(0, 5)),
                color = colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (LocalUiMode.current) {
                UiMode.Material -> Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                UiMode.Miuix -> MiuixText("0", color = colorScheme.onBackground)
            }
            Spacer(Modifier.width(8.dp))
            OneuiXSlider(
                value = seconds, onValueChange = onSecondsPreview,
                modifier = Modifier.weight(1f), valueRange = 0f..5f, stepSize = 1f,
                enabled = enabled, onValueChangeFinished = onSecondsCommit,
            )
            Spacer(Modifier.width(8.dp))
            when (LocalUiMode.current) {
                UiMode.Material -> Text("5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                UiMode.Miuix -> MiuixText("5", color = colorScheme.onBackground)
            }
        }
        Spacer(Modifier.height(4.dp))
        when (LocalUiMode.current) {
            UiMode.Material -> Text(stringResource(R.string.oneuix_callReturnDelayControlHint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            UiMode.Miuix -> MiuixText(stringResource(R.string.oneuix_callReturnDelayControlHint), color = colorScheme.onBackground)
        }
    }
}
