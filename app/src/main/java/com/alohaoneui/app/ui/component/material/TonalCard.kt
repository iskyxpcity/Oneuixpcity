package com.alohaoneui.app.ui.component.material

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.alohaoneui.app.ui.theme.LocalMaterialBorderWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape = MaterialTheme.shapes.large,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val borderWidth = LocalMaterialBorderWidth.current.coerceIn(0f, 1.5f)
    val border = if (borderWidth > 0f) BorderStroke(borderWidth.dp, MaterialTheme.colorScheme.outlineVariant) else null
    val elevation = CardDefaults.cardElevation(defaultElevation = if (LocalMaterialShadowContainers.current) 1.dp else 0.dp)
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor),
        ),
        shape = shape,
        border = border,
        elevation = elevation,
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    val borderWidth = LocalMaterialBorderWidth.current.coerceIn(0f, 1.5f)
    val border = if (borderWidth > 0f) BorderStroke(borderWidth.dp, MaterialTheme.colorScheme.outlineVariant) else null
    val elevation = CardDefaults.cardElevation(defaultElevation = if (LocalMaterialShadowContainers.current) 1.dp else 0.dp)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor),
        ),
        shape = shape,
        border = border,
        elevation = elevation,
    ) {
        content()
    }
}
