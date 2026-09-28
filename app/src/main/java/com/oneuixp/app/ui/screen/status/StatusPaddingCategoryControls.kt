package com.oneuixp.app.ui.screen.status

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
import com.oneuixp.app.R
import io.github.soclear.oneuix.common.Preference
import io.github.soclear.oneuix.ui.category.SystemUIEvent
import io.github.soclear.oneuix.ui.component.OneuiXSlider
import io.github.soclear.oneuix.ui.component.SwitchItem
import kotlin.math.roundToInt

/**
 * OneuiXP-owned whole-status-bar padding controls.
 *
 * These values affect the whole status bar and therefore live in their own
 * top-level category instead of Signal.
 */
@Composable
fun StatusPaddingCategoryControls(
    uiState: Preference.SystemUI.StatusBar,
    onEvent: (SystemUIEvent) -> Unit,
) {
    var left by remember(uiState.statusBarLeftPaddingDp) {
        mutableFloatStateOf(uiState.statusBarLeftPaddingDp)
    }
    var leftExpanded by rememberSaveable { mutableStateOf(false) }

    var right by remember(uiState.statusBarRightPaddingDp) {
        mutableFloatStateOf(uiState.statusBarRightPaddingDp)
    }
    var rightExpanded by rememberSaveable { mutableStateOf(false) }

    var top by remember(uiState.statusBarTopPaddingDp) {
        mutableFloatStateOf(uiState.statusBarTopPaddingDp)
    }
    var topExpanded by rememberSaveable { mutableStateOf(false) }

    var bottom by remember(uiState.statusBarBottomPaddingDp) {
        mutableFloatStateOf(uiState.statusBarBottomPaddingDp)
    }
    var bottomExpanded by rememberSaveable { mutableStateOf(false) }

    StatusThemeGroup(
        items = buildList {
            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_statusBarLeftPaddingDp_title),
                    summary = if (uiState.modifyStatusBarLeftPadding) "%.1fdp".format(left) else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { leftExpanded = !leftExpanded },
                    checked = uiState.modifyStatusBarLeftPadding,
                    onCheckedChange = {
                        leftExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarLeftPadding(it))
                    },
                )
            }
            if (leftExpanded && uiState.modifyStatusBarLeftPadding) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = left,
                            onValueChange = { left = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..100f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarLeftPaddingDp(left))
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_statusBarRightPaddingDp_title),
                    summary = if (uiState.modifyStatusBarRightPadding) "%.1fdp".format(right) else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { rightExpanded = !rightExpanded },
                    checked = uiState.modifyStatusBarRightPadding,
                    onCheckedChange = {
                        rightExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarRightPadding(it))
                    },
                )
            }
            if (rightExpanded && uiState.modifyStatusBarRightPadding) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = right,
                            onValueChange = { right = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..100f,
                            onValueChangeFinished = {
                                onEvent(SystemUIEvent.StatusBar.StatusBarRightPaddingDp(right))
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_statusBarTopPaddingDp_title),
                    summary = if (uiState.modifyStatusBarTopPadding) {
                        stringResource(id = R.string.status_padding_top_summary, top)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { topExpanded = !topExpanded },
                    checked = uiState.modifyStatusBarTopPadding,
                    onCheckedChange = {
                        topExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarTopPadding(it))
                    },
                )
            }
            if (topExpanded && uiState.modifyStatusBarTopPadding) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = top,
                            onValueChange = { top = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..8f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarTopPaddingDp(
                                        (top * 10f).roundToInt() / 10f
                                    )
                                )
                            },
                        )
                    }
                }
            }

            add {
                SwitchItem(
                    title = stringResource(id = R.string.oneuix_statusBarBottomPaddingDp_title),
                    summary = if (uiState.modifyStatusBarBottomPadding) {
                        stringResource(id = R.string.status_padding_bottom_summary, bottom)
                    } else null,
                    icon = ImageVector.vectorResource(id = R.drawable.padding),
                    modifier = Modifier.animateContentSize(),
                    clickable = true,
                    onClick = { bottomExpanded = !bottomExpanded },
                    checked = uiState.modifyStatusBarBottomPadding,
                    onCheckedChange = {
                        bottomExpanded = it
                        onEvent(SystemUIEvent.StatusBar.ModifyStatusBarBottomPadding(it))
                    },
                )
            }
            if (bottomExpanded && uiState.modifyStatusBarBottomPadding) {
                add {
                    StatusThemeChildItem {
                        OneuiXSlider(
                            value = bottom,
                            onValueChange = { bottom = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            valueRange = 0f..8f,
                            onValueChangeFinished = {
                                onEvent(
                                    SystemUIEvent.StatusBar.StatusBarBottomPaddingDp(
                                        (bottom * 10f).roundToInt() / 10f
                                    )
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}
