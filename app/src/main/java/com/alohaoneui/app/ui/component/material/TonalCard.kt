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
import com.alohaoneui.app.ui.theme.LocalMaterialOutlineWidth
import com.alohaoneui.app.ui.theme.LocalMaterialShadowContainers

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val actualShape = shape ?: voyageCornerShape(20.dp)
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        border = LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
            BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (LocalMaterialShadowContainers.current) 2.dp else 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor),
        ),
        shape = actualShape,
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape? = null,
    content: @Composable () -> Unit,
) {
    val actualShape = shape ?: voyageCornerShape(20.dp)
    Card(
        modifier = modifier,
        border = LocalMaterialOutlineWidth.current.takeIf { it > 0f }?.let {
            BorderStroke(it.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (LocalMaterialShadowContainers.current) 2.dp else 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor),
        ),
        shape = actualShape,
    ) {
        content()
    }
}
