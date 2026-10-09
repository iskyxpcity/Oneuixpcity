package com.oneuixp.app.ui.screen.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.SegmentedColumn
import com.oneuixp.app.ui.component.material.SegmentedListItem
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.navigation3.Route
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Parent page for the status-bar three-in-one icon. Detailed controls live
 * on the dedicated Style One child page.
 */
@Composable
fun StatusComboCategoryControls() {
    val navigator = LocalNavigator.current
    val title = stringResource(R.string.status_combo_style_one_category)
    val icon = ImageVector.vectorResource(R.drawable.status_combo)
    val openStyleOne: () -> Unit = {
        navigator.push(Route.StatusCategory(StatusCategory.ComboStyleOne.name))
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                onClick = openStyleOne,
                insideMargin = PaddingValues(
                    start = 10.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF7C6CFF), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        MiuixIcon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    MiuixText(
                        text = title,
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                    )
                    MiuixIcon(
                        imageVector = MiuixIcons.Basic.ArrowRight,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariantActions,
                    )
                }
            }
        }

        UiMode.Material -> {
            SegmentedColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                content = listOf({
                    SegmentedListItem(
                        onClick = openStyleOne,
                        headlineContent = { Text(title) },
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        },
                    )
                }),
            )
        }
    }
}
