package com.alohaoneui.app.ui.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Fence
import androidx.compose.material.icons.rounded.FolderDelete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.UiMode
import com.alohaoneui.app.ui.component.dialog.rememberLoadingDialog
import com.alohaoneui.app.ui.component.miuix.SendLogDialog
import com.alohaoneui.app.ui.theme.LocalEnableBlur
import com.alohaoneui.app.ui.util.BlurredBar
import com.alohaoneui.app.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * @author weishu
 * @date 2023/1/1.
 */
@Composable
fun SettingPagerMiuix(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val loadingDialog = rememberLoadingDialog()
    val showSendLogDialog = rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_check_update),
                            summary = stringResource(id = R.string.settings_check_update_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.SystemUpdate,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_check_update),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.checkUpdate,
                            onCheckedChange = actions.onSetCheckUpdate
                        )

                    }

                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        OverlayDropdownPreference(
                            title = stringResource(id = R.string.settings_ui_mode),
                            summary = stringResource(id = R.string.settings_ui_mode_summary),
                            items = UiMode.entries.map { it.name },
                            startAction = {
                                Icon(
                                    Icons.Rounded.DisplaySettings,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_ui_mode),
                                    tint = colorScheme.onBackground
                                )
                            },
                            selectedIndex = if (uiState.uiMode == UiMode.Material.value) 1 else 0,
                            onSelectedIndexChange = actions.onSetUiModeIndex
                        )
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_theme),
                            summary = stringResource(id = R.string.settings_theme_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Palette,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_theme),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenTheme
                        )
                    }


                    Text(stringResource(R.string.settings_group_language), Modifier.padding(start = 8.dp, top = 16.dp), style = MaterialTheme.typography.titleSmall)
                    Card(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_language_title),
                            items = listOf(stringResource(R.string.settings_language_system), stringResource(R.string.settings_language_english), stringResource(R.string.settings_language_chinese)),
                            selectedIndex = when (uiState.appLanguage) { "en" -> 1; "zh-CN" -> 2; else -> 0 },
                            onSelectedIndexChange = actions.onSetAppLanguage,
                            startAction = { Icon(Icons.Rounded.Language, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }
                        )
                    }
                    Text(stringResource(R.string.settings_group_display), Modifier.padding(start = 8.dp, top = 16.dp), style = MaterialTheme.typography.titleSmall)
                    Card(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_font_scale_title),
                            items = listOf("80%", "90%", "100%", "110%", "120%", "130%", "140%", "150%"),
                            selectedIndex = ((uiState.materialFontScale * 10).toInt() - 8).coerceIn(0, 7),
                            onSelectedIndexChange = { actions.onSetMaterialFontScale((it + 8) / 10f) },
                            startAction = { Icon(Icons.Rounded.DisplaySettings, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }
                        )
                        SwitchPreference(
                            title = stringResource(R.string.settings_large_text_title),
                            summary = stringResource(R.string.settings_large_text_subtitle),
                            checked = uiState.materialLargeTextEnabled,
                            onCheckedChange = actions.onSetMaterialLargeText,
                            startAction = { Icon(Icons.Rounded.DisplaySettings, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }
                        )
                        SwitchPreference(
                            title = stringResource(R.string.settings_bold_text_title),
                            checked = uiState.materialBoldTextEnabled,
                            onCheckedChange = actions.onSetMaterialBoldText,
                            startAction = { Icon(Icons.Rounded.DisplaySettings, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }
                        )
                        ArrowPreference(
                            title = stringResource(R.string.settings_reset_display_title),
                            startAction = { Icon(Icons.Rounded.RestartAlt, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) },
                            onClick = actions.onResetDisplay
                        )
                    }
                    Text(stringResource(R.string.settings_group_behavior), Modifier.padding(start = 8.dp, top = 16.dp), style = MaterialTheme.typography.titleSmall)
                    Card(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_navigation_motion_title),
                            items = listOf(stringResource(R.string.settings_navigation_motion_expressive), stringResource(R.string.settings_navigation_motion_reduced)),
                            selectedIndex = uiState.materialNavigationMotionMode.coerceIn(0, 1),
                            onSelectedIndexChange = actions.onSetNavigationMotion,
                            startAction = { Icon(Icons.Rounded.RestartAlt, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }
                        )
                        SwitchPreference(
                            title = stringResource(R.string.settings_haptic_feedback_title),
                            summary = stringResource(R.string.settings_haptic_feedback_subtitle),
                            checked = uiState.enableHapticFeedback,
                            onCheckedChange = actions.onSetHapticFeedback,
                            startAction = { Icon(Icons.Rounded.RestartAlt, modifier = Modifier.padding(end = 6.dp), contentDescription = null, tint = colorScheme.onBackground) }
                        )
                    }

                    Card(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        ArrowPreference(
                            title = stringResource(id = R.string.send_log),
                            startAction = {
                                Icon(
                                    Icons.Rounded.BugReport,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.send_log),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = { showSendLogDialog.value = true },
                        )
                        SendLogDialog(
                            show = showSendLogDialog.value,
                            onDismissRequest = { showSendLogDialog.value = false },
                            loadingDialog = loadingDialog
                        )
                        val about = stringResource(id = R.string.about)
                        ArrowPreference(
                            title = about,
                            startAction = {
                                Icon(
                                    Icons.Rounded.Info,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = about,
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenAbout,
                        )
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}
