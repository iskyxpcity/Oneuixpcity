package com.oneuialoha.app.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneuialoha.app.R
import com.oneuialoha.app.ui.component.material.SegmentedColumn
import com.oneuialoha.app.ui.component.material.SegmentedListItem
import com.oneuialoha.app.ui.component.material.TopBarBackButton

@Composable
fun AboutScreenMaterial(
    state: AboutUiState,
    actions: AboutScreenActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    TopBarBackButton(onClick = actions.onBack)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 64.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(id = R.drawable.logo002),
                            contentDescription = null,
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        modifier = Modifier.padding(top = 12.dp),
                        text = state.appName,
                        fontWeight = FontWeight.Medium,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize
                    )
                    Text(
                        text = state.versionName,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                }
            }
            item {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    content = listOf(
                        {
                            SegmentedListItem(
                                onClick = actions.onOpenFeatureIntroduction,
                                headlineContent = { Text(state.featureIntroductionTitle) }
                            )
                        },
                        {
                            SegmentedListItem(
                                onClick = actions.onOpenDisclaimer,
                                headlineContent = { Text(state.disclaimerTitle) }
                            )
                        },
                    )
                )
                Spacer(
                    Modifier.height(
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                    )
                )
            }
        }
    }
}
