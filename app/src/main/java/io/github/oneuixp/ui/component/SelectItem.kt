package io.github.oneuixp.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.SegmentedDropdownItem
import com.oneuixp.app.ui.component.material.oneuiXPSegmentedColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** OneUIXP selection semantics rendered with the active OneuiXP theme. */
@Composable
fun SelectItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    entries: List<String>,
    selectedIndex: Int,
    enabled: Boolean = true,
    onSelectedIndexChange: (Int) -> Unit
) {
    val safeIndex = if (entries.isEmpty()) 0 else selectedIndex.coerceIn(0, entries.lastIndex)
    val grouped = LocalOneuiXPGroupedPreference.current

    when (LocalUiMode.current) {
        UiMode.Material -> {
            Box(
                modifier = (if (grouped) modifier else modifier.padding(horizontal = 16.dp, vertical = 4.dp)).alpha(if (enabled) 1f else 0.45f),
            ) {
                SegmentedDropdownItem(
                    enabled = enabled,
                    icon = icon,
                    colors = oneuiXPSegmentedColors(),
                    title = title,
                    summary = summary,
                    items = entries,
                    selectedIndex = safeIndex,
                    onItemSelected = onSelectedIndexChange,
                )
            }
        }

        UiMode.Miuix -> {
            @Composable
            fun Content() {
                OverlayDropdownPreference(
                    title = title,
                    summary = summary,
                    enabled = enabled,
                    items = entries,
                    selectedIndex = safeIndex,
                    startAction = icon?.let { imageVector ->
                        {
                            MiuixIcon(
                                imageVector = imageVector,
                                contentDescription = title,
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                    onSelectedIndexChange = onSelectedIndexChange,
                )
            }

            if (grouped) {
                Box(modifier = Modifier.alpha(if (enabled) 1f else 0.45f)) { Content() }
            } else {
                Card(
                    modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp).alpha(if (enabled) 1f else 0.45f),
                ) {
                    Content()
                }
            }
        }
    }
}
