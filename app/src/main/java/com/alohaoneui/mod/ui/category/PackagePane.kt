package com.alohaoneui.mod.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alohaoneui.mod.ui.component.AlohaOneUIDetailSegmentedGroup

@Composable
internal fun PackagePane(
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AlohaOneUIDetailSegmentedGroup(
            modifier = Modifier,
            content = content,
        )
    }
}
