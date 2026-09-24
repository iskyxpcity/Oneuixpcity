package com.oneuixp.app.ui.component.rebootlistpopup

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.oneuixp.app.R
import com.oneuixp.app.ui.LocalUiMode
import com.oneuixp.app.ui.UiMode

data class RebootListOption(
    @param:StringRes val labelRes: Int,
    val reason: String,
)

@Composable
fun getRebootListOption(): List<RebootListOption> =
    listOf(RebootListOption(R.string.reboot_soft, "statusbar_xp_systemui"))

@Composable
fun rememberRebootAction(): (String) -> Unit {
    return { reason ->
        if (reason == "statusbar_xp_systemui") {
            runCatching {
                // OneuiXP utility action: restart SystemUI only.
                ProcessBuilder("su", "-c", "pkill -TERM -f com.android.systemui").start()
            }
        }
    }
}

@Composable
fun RebootListPopup() {
    when (LocalUiMode.current) {
        UiMode.Miuix -> RebootListPopupMiuix()
        UiMode.Material -> RebootListPopupMaterial()
    }
}
