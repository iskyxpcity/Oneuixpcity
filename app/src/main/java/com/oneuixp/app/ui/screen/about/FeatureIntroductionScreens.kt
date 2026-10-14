package com.oneuixp.app.ui.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.component.material.SegmentedColumn
import com.oneuixp.app.ui.component.material.SegmentedListItem
import com.oneuixp.app.ui.component.material.TopBarBackButton
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.navigation3.Route
import com.oneuixp.app.ui.theme.LocalEnableBlur
import com.oneuixp.app.ui.util.BlurredBar
import com.oneuixp.app.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun FeatureIntroductionScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    val onOpenUpdates = dropUnlessResumed { navigator.push(Route.VersionChangelog) }
    when (LocalUiMode.current) {
        UiMode.Material -> FeatureIntroductionMaterial(onBack, onOpenUpdates)
        UiMode.Miuix -> FeatureIntroductionMiuix(onBack, onOpenUpdates)
    }
}

@Composable
fun VersionChangelogScreen() {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Material -> VersionChangelogMaterial(onBack)
        UiMode.Miuix -> VersionChangelogMiuix(onBack)
    }
}

@Composable
private fun FeatureIntroductionMaterial(onBack: () -> Unit, onOpenUpdates: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.feature_introduction)) },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            item {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    content = listOf(
                        {
                            SegmentedListItem(
                                onClick = onOpenUpdates,
                                headlineContent = { Text(stringResource(R.string.feature_intro_version_updates)) },
                                supportingContent = { Text(stringResource(R.string.feature_intro_version_updates_summary)) },
                                leadingContent = {
                                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null)
                                },
                                trailingContent = {
                                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                },
                            )
                        },
                    ),
                )
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FeatureIntroductionMiuix(onBack: () -> Unit, onOpenUpdates: () -> Unit) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixSmallTopAppBar(
                    title = stringResource(R.string.feature_introduction),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    titleColor = colorScheme.onSurface,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            MiuixIcon(imageVector = MiuixIcons.Back, contentDescription = null, tint = colorScheme.onBackground)
                        }
                    },
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .scrollEndHaptic()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    MiuixCard(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        ArrowPreference(
                            title = stringResource(R.string.feature_intro_version_updates),
                            summary = stringResource(R.string.feature_intro_version_updates_summary),
                            onClick = onOpenUpdates,
                        )
                    }
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun VersionChangelogMaterial(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.version_updates_title)) },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.version_1_0_261013_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.version_1_0_261013_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { MaterialChangelogSection(R.string.changelog_261013_feature_intro_title, R.string.changelog_261013_feature_intro_body) }
            item { MaterialChangelogSection(R.string.changelog_261013_dual_ui_title, R.string.changelog_261013_dual_ui_body) }
            item { MaterialChangelogSection(R.string.changelog_261013_first_release_title, R.string.changelog_261013_first_release_body) }
            item { MaterialChangelogSection(R.string.changelog_261013_version_label_title, R.string.changelog_261013_version_label_body) }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.version_1_0_260930_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.version_1_0_260930_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { MaterialChangelogSection(R.string.changelog_section_combo_title, R.string.changelog_section_combo_body) }
            item { MaterialChangelogSection(R.string.changelog_section_statusbar_title, R.string.changelog_section_statusbar_body) }
            item { MaterialChangelogSection(R.string.changelog_section_network_title, R.string.changelog_section_network_body) }
            item { MaterialChangelogSection(R.string.changelog_section_power_title, R.string.changelog_section_power_body) }
            item { MaterialChangelogSection(R.string.changelog_section_ui_title, R.string.changelog_section_ui_body) }
            item { MaterialChangelogSection(R.string.changelog_section_diagnostics_title, R.string.changelog_section_diagnostics_body) }
        }
    }
}

@Composable
private fun MaterialChangelogSection(titleRes: Int, bodyRes: Int) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(bodyRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VersionChangelogMiuix(onBack: () -> Unit) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixSmallTopAppBar(
                    title = stringResource(R.string.version_updates_title),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    titleColor = colorScheme.onSurface,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            MiuixIcon(imageVector = MiuixIcons.Back, contentDescription = null, tint = colorScheme.onBackground)
                        }
                    },
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().overScrollVertical().scrollEndHaptic()
                    .nestedScroll(scrollBehavior.nestedScrollConnection).padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    MiuixCard(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiuixText(
                                text = stringResource(R.string.version_1_0_261013_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onBackground,
                            )
                            MiuixText(
                                text = stringResource(R.string.version_1_0_261013_summary),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
                item { MiuixChangelogSection(R.string.changelog_261013_feature_intro_title, R.string.changelog_261013_feature_intro_body) }
                item { MiuixChangelogSection(R.string.changelog_261013_dual_ui_title, R.string.changelog_261013_dual_ui_body) }
                item { MiuixChangelogSection(R.string.changelog_261013_first_release_title, R.string.changelog_261013_first_release_body) }
                item { MiuixChangelogSection(R.string.changelog_261013_version_label_title, R.string.changelog_261013_version_label_body) }
                item {
                    MiuixCard(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiuixText(
                                text = stringResource(R.string.version_1_0_260930_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onBackground,
                            )
                            MiuixText(
                                text = stringResource(R.string.version_1_0_260930_summary),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
                item { MiuixChangelogSection(R.string.changelog_section_combo_title, R.string.changelog_section_combo_body) }
                item { MiuixChangelogSection(R.string.changelog_section_statusbar_title, R.string.changelog_section_statusbar_body) }
                item { MiuixChangelogSection(R.string.changelog_section_network_title, R.string.changelog_section_network_body) }
                item { MiuixChangelogSection(R.string.changelog_section_power_title, R.string.changelog_section_power_body) }
                item { MiuixChangelogSection(R.string.changelog_section_ui_title, R.string.changelog_section_ui_body) }
                item { MiuixChangelogSection(R.string.changelog_section_diagnostics_title, R.string.changelog_section_diagnostics_body) }
                item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun MiuixChangelogSection(titleRes: Int, bodyRes: Int) {
    MiuixCard(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MiuixText(text = stringResource(titleRes), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onBackground)
            MiuixText(text = stringResource(bodyRes), fontSize = 14.sp, color = colorScheme.onSurfaceVariantSummary)
        }
    }
}

