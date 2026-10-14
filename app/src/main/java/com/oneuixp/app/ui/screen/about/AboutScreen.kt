package com.oneuixp.app.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import com.oneuixp.app.BuildConfig
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode
import com.oneuixp.app.ui.navigation3.LocalNavigator
import com.oneuixp.app.ui.navigation3.Route

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val state = AboutUiState(
        title = stringResource(R.string.about),
        appName = stringResource(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        featureIntroductionTitle = stringResource(R.string.feature_introduction),
    )
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenFeatureIntroduction = dropUnlessResumed {
            navigator.push(Route.FeatureIntroduction)
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix(state, actions)
        UiMode.Material -> AboutScreenMaterial(state, actions)
    }
}
