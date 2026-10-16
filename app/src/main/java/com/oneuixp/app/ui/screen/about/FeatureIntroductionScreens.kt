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
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
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
    val onOpen261016Updates = dropUnlessResumed { navigator.push(Route.VersionChangelog261016) }
    val onOpen261013Updates = dropUnlessResumed { navigator.push(Route.VersionChangelog261013) }
    val onOpenFirstRelease = dropUnlessResumed { navigator.push(Route.VersionChangelog) }
    when (LocalUiMode.current) {
        UiMode.Material -> FeatureIntroductionMaterial(onBack, onOpen261016Updates, onOpen261013Updates, onOpenFirstRelease)
        UiMode.Miuix -> FeatureIntroductionMiuix(onBack, onOpen261016Updates, onOpen261013Updates, onOpenFirstRelease)
    }
}

@Composable
fun VersionChangelogScreen() {
    VersionChangelogContent(isLatestVersion = false)
}

@Composable
fun VersionChangelog261016Screen() {
    VersionChangelogContent(isLatestVersion = true, is261016 = true)
}

@Composable
fun VersionChangelog261013Screen() {
    VersionChangelogContent(isLatestVersion = true)
}

@Composable
private fun VersionChangelogContent(isLatestVersion: Boolean, is261016: Boolean = false) {
    val navigator = LocalNavigator.current
    val onBack = dropUnlessResumed { navigator.pop() }
    when (LocalUiMode.current) {
        UiMode.Material -> VersionChangelogMaterial(onBack, isLatestVersion, is261016)
        UiMode.Miuix -> VersionChangelogMiuix(onBack, isLatestVersion, is261016)
    }
}

@Composable
private fun FeatureIntroductionMaterial(
    onBack: () -> Unit,
    onOpen261016Updates: () -> Unit,
    onOpen261013Updates: () -> Unit,
    onOpenFirstRelease: () -> Unit,
) {
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
                                onClick = onOpen261016Updates,
                                headlineContent = { Text(stringResource(R.string.feature_intro_version_updates_261016)) },
                                supportingContent = { Text(stringResource(R.string.feature_intro_version_updates_261016_summary)) },
                                leadingContent = {
                                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null)
                                },
                                trailingContent = {
                                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                },
                            )
                        },
                        {
                            SegmentedListItem(
                                onClick = onOpen261013Updates,
                                headlineContent = { Text(stringResource(R.string.feature_intro_version_updates_261013)) },
                                supportingContent = { Text(stringResource(R.string.feature_intro_version_updates_261013_summary)) },
                                leadingContent = {
                                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null)
                                },
                                trailingContent = {
                                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                                },
                            )
                        },
                        {
                            SegmentedListItem(
                                onClick = onOpenFirstRelease,
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
private fun FeatureIntroductionMiuix(
    onBack: () -> Unit,
    onOpen261016Updates: () -> Unit,
    onOpen261013Updates: () -> Unit,
    onOpenFirstRelease: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    title = stringResource(R.string.feature_introduction),
                    largeTitle = stringResource(R.string.feature_introduction),
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
                            title = stringResource(R.string.feature_intro_version_updates_261016),
                            summary = stringResource(R.string.feature_intro_version_updates_261016_summary),
                            onClick = onOpen261016Updates,
                        )
                    }
                }
                item {
                    MiuixCard(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                        ArrowPreference(
                            title = stringResource(R.string.feature_intro_version_updates_261013),
                            summary = stringResource(R.string.feature_intro_version_updates_261013_summary),
                            onClick = onOpen261013Updates,
                        )
                    }
                }
                item {
                    MiuixCard(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                        ArrowPreference(
                            title = stringResource(R.string.feature_intro_version_updates),
                            summary = stringResource(R.string.feature_intro_version_updates_summary),
                            onClick = onOpenFirstRelease,
                        )
                    }
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun VersionChangelogMaterial(onBack: () -> Unit, isLatestVersion: Boolean, is261016: Boolean) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val versionTitle = when {
        is261016 -> R.string.version_1_0_261016_title
        isLatestVersion -> R.string.version_1_0_261013_title
        else -> R.string.version_1_0_260930_title
    }
    val versionDate = when {
        is261016 -> R.string.version_1_0_261016_summary
        isLatestVersion -> R.string.version_1_0_261013_summary
        else -> R.string.version_1_0_260930_summary
    }
    val updateRows = when {
        is261016 -> listOf(
            R.string.changelog_261016_statusbar_stability_title to R.string.changelog_261016_statusbar_stability_body,
            R.string.changelog_261016_burnin_title to R.string.changelog_261016_burnin_body,
        )
        isLatestVersion -> listOf(
            R.string.changelog_261013_feature_intro_title to R.string.changelog_261013_feature_intro_body,
            R.string.changelog_261013_dual_ui_title to R.string.changelog_261013_dual_ui_body,
            R.string.changelog_261013_version_split_title to R.string.changelog_261013_version_split_body,
        )
        else -> listOf(
            R.string.changelog_section_combo_title to R.string.changelog_section_combo_body,
            R.string.changelog_section_statusbar_title to R.string.changelog_section_statusbar_body,
            R.string.changelog_section_network_title to R.string.changelog_section_network_body,
            R.string.changelog_section_power_title to R.string.changelog_section_power_body,
            R.string.changelog_section_ui_title to R.string.changelog_section_ui_body,
            R.string.changelog_section_diagnostics_title to R.string.changelog_section_diagnostics_body,
        )
    }
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
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(versionTitle),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(versionDate),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SegmentedColumn(
                        modifier = Modifier.padding(top = 8.dp),
                        content = updateRows.map { (titleRes, bodyRes) ->
                            {
                                SegmentedListItem(
                                    headlineContent = { Text(stringResource(titleRes)) },
                                    supportingContent = { Text(stringResource(bodyRes)) },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionChangelogMiuix(onBack: () -> Unit, isLatestVersion: Boolean, is261016: Boolean) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else colorScheme.surface
    val versionTitle = when {
        is261016 -> R.string.version_1_0_261016_title
        isLatestVersion -> R.string.version_1_0_261013_title
        else -> R.string.version_1_0_260930_title
    }
    val versionDate = when {
        is261016 -> R.string.version_1_0_261016_summary
        isLatestVersion -> R.string.version_1_0_261013_summary
        else -> R.string.version_1_0_260930_summary
    }
    val updateRows = when {
        is261016 -> listOf(
            R.string.changelog_261016_statusbar_stability_title to R.string.changelog_261016_statusbar_stability_body,
            R.string.changelog_261016_burnin_title to R.string.changelog_261016_burnin_body,
        )
        isLatestVersion -> listOf(
            R.string.changelog_261013_feature_intro_title to R.string.changelog_261013_feature_intro_body,
            R.string.changelog_261013_dual_ui_title to R.string.changelog_261013_dual_ui_body,
            R.string.changelog_261013_version_split_title to R.string.changelog_261013_version_split_body,
        )
        else -> listOf(
            R.string.changelog_section_combo_title to R.string.changelog_section_combo_body,
            R.string.changelog_section_statusbar_title to R.string.changelog_section_statusbar_body,
            R.string.changelog_section_network_title to R.string.changelog_section_network_body,
            R.string.changelog_section_power_title to R.string.changelog_section_power_body,
            R.string.changelog_section_ui_title to R.string.changelog_section_ui_body,
            R.string.changelog_section_diagnostics_title to R.string.changelog_section_diagnostics_body,
        )
    }
    MiuixScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    title = stringResource(R.string.version_updates_title),
                    largeTitle = stringResource(R.string.version_updates_title),
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
                        Column {
                            BasicComponent(
                                title = stringResource(versionTitle),
                                summary = stringResource(versionDate),
                            )
                            updateRows.forEach { (titleRes, bodyRes) ->
                                BasicComponent(
                                    title = stringResource(titleRes),
                                    summary = stringResource(bodyRes),
                                )
                            }
                        }
                    }
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

