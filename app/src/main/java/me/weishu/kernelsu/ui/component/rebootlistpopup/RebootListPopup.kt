package me.weishu.kernelsu.ui.component.rebootlistpopup

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode

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
                // OneuiXP utility action only; no KernelSU reboot/recovery/bootloader paths.
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
