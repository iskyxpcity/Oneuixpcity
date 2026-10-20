package com.oneuialoha.app.ui.screen.status

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.oneuialoha.app.R
import com.oneuialoha.common.ONE_UI_VERSION
import com.oneuialoha.common.Preference
import com.oneuialoha.mod.ui.category.SystemUIEvent
import com.oneuialoha.mod.ui.component.OneUIAlohaInfoItem
import com.oneuialoha.mod.ui.component.OneUIAlohaSlider
import com.oneuialoha.mod.ui.component.SwitchItem

@Composable
fun StatusControlCenterCategoryControls(
    uiState: Preference.SystemUI.QS,
    onEvent: (SystemUIEvent) -> Unit,
) {
    var textSize by remember(uiState.qsClockTextSize) {
        mutableFloatStateOf(uiState.qsClockTextSize)
    }
    var textSizeExpanded by rememberSaveable { mutableStateOf(false) }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    icon = ImageVector.vectorResource(id = R.drawable.format_letter_spacing),
                    title = stringResource(id = R.string.oneuialoha_setQsClockMonospaced_title),
                    checked = uiState.setQsClockMonospaced,
                    onCheckedChange = {
                        onEvent(SystemUIEvent.QS.SetQsClockMonospaced(it))
                    },
                )
            }

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                        title = stringResource(id = R.string.oneuialoha_hideDeviceControlQsTile_title),
                        checked = uiState.hideDeviceControlQsTile,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.HideDeviceControlQsTile(it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                        title = stringResource(id = R.string.oneuialoha_hideSmartViewQsTile_title),
                        checked = uiState.hideSmartViewQsTile,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.HideSmartViewQsTile(it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.five_g),
                        title = stringResource(id = R.string.oneuialoha_turnOn5gQsTile_title),
                        summary = stringResource(id = R.string.oneuialoha_turnOn5gQsTile_summary),
                        checked = uiState.turnOn5gQsTile,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.TurnOn5gQsTile(it))
                        },
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                add {
                    OneUIAlohaInfoItem(
                        title = stringResource(id = R.string.oneuialoha_turnOn5gQsTile_title),
                        summary = stringResource(id = R.string.oneuialoha_root5gQsTile_summary),
                        icon = ImageVector.vectorResource(id = R.drawable.five_g),
                    )
                }
                add {
                    OneUIAlohaInfoItem(
                        title = stringResource(id = R.string.oneuialoha_immersiveModeQsTile_title),
                        summary = stringResource(id = R.string.oneuialoha_rootImmersiveModeQsTile_summary),
                        icon = ImageVector.vectorResource(id = R.drawable.immersive_mode),
                    )
                }

                if (ONE_UI_VERSION < 80500) {
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                            title = stringResource(id = R.string.oneuialoha_hideQsBarMediaPlayer_title),
                            checked = uiState.hideQsBarMediaPlayer,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.QS.HideQsBarMediaPlayer(it))
                            },
                        )
                    }
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                            title = stringResource(
                                id = R.string.oneuialoha_hideQsBarNearbyDevicesAndDeviceControl_title
                            ),
                            checked = uiState.hideQsBarNearbyDevicesAndDeviceControl,
                            onCheckedChange = {
                                onEvent(
                                    SystemUIEvent.QS.HideQsBarNearbyDevicesAndDeviceControl(it)
                                )
                            },
                        )
                    }
                }

                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                        title = stringResource(id = R.string.oneuialoha_hideQsBarSecurityFooter_title),
                        summary = stringResource(id = R.string.oneuialoha_hideQsBarSecurityFooter_summary),
                        checked = uiState.hideQsBarSecurityFooter,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.HideQsBarSecurityFooter(it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                        title = stringResource(id = R.string.oneuialoha_hideQsBarDataUsage_title),
                        checked = uiState.hideQsBarDataUsage,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.HideQsBarDataUsage(it))
                        },
                    )
                }

                if (ONE_UI_VERSION < 80500) {
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.tile_medium),
                            title = stringResource(id = R.string.oneuialoha_hideQsBarSmartViewAndModes_title),
                            checked = uiState.hideQsBarSmartViewAndModes,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.QS.HideQsBarSmartViewAndModes(it))
                            },
                        )
                    }
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.expand),
                            title = stringResource(id = R.string.oneuialoha_alwaysExpandQsTileChunk_title),
                            checked = uiState.alwaysExpandQsTileChunk,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.QS.AlwaysExpandQsTileChunk(it))
                            },
                        )
                    }
                    add {
                        SwitchItem(
                            title = stringResource(id = R.string.oneuialoha_alwaysShowTimeDateOnQs_title),
                            icon = ImageVector.vectorResource(
                                id = R.drawable.nest_clock_farsight_digital
                            ),
                            checked = uiState.alwaysShowTimeDateOnQs,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.QS.AlwaysShowTimeDateOnQs(it))
                            },
                        )
                    }
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.light_mode),
                            title = stringResource(
                                id = R.string.oneuialoha_addBrightnessProgressToQsBar_title
                            ),
                            checked = uiState.addBrightnessProgressToQsBar,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.QS.AddBrightnessProgressToQsBar(it))
                            },
                        )
                    }
                    add {
                        SwitchItem(
                            icon = ImageVector.vectorResource(id = R.drawable.music_note),
                            title = stringResource(
                                id = R.string.oneuialoha_addVolumeProgressToQsBar_title
                            ),
                            checked = uiState.addVolumeProgressToQsBar,
                            onCheckedChange = {
                                onEvent(SystemUIEvent.QS.AddVolumeProgressToQsBar(it))
                            },
                        )
                    }
                }

                add {
                    SwitchItem(
                        icon = ImageVector.vectorResource(id = R.drawable.today),
                        title = stringResource(
                            id = R.string.oneuialoha_showTraditionalChineseDateOnQS_title
                        ),
                        checked = uiState.showTraditionalChineseDateOnQS,
                        onCheckedChange = {
                            onEvent(SystemUIEvent.QS.ShowTraditionalChineseDateOnQS(it))
                        },
                    )
                }
                add {
                    SwitchItem(
                        title = stringResource(id = R.string.oneuialoha_modifyQSClockTextSize_title),
                        modifier = Modifier.animateContentSize(),
                        summary = if (uiState.modifyQSClockTextSize) {
                            " %.1fsp".format(textSize)
                        } else null,
                        icon = ImageVector.vectorResource(id = R.drawable.format_size),
                        clickable = true,
                        onClick = { textSizeExpanded = !textSizeExpanded },
                        checked = uiState.modifyQSClockTextSize,
                        onCheckedChange = {
                            if (it && textSize == 32f) {
                                textSizeExpanded = true
                            } else if (!it) {
                                textSizeExpanded = false
                            }
                            onEvent(SystemUIEvent.QS.ModifyQSClockTextSize(it))
                        },
                    )
                }
                if (textSizeExpanded && uiState.modifyQSClockTextSize) {
                    add {
                        StatusThemeChildItem {
                            OneUIAlohaSlider(
                                value = textSize,
                                onValueChange = { textSize = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                valueRange = 15f..70f,
                                onValueChangeFinished = {
                                    onEvent(SystemUIEvent.QS.QSClockTextSize(textSize))
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}
