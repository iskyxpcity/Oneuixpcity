package com.oneuialoha.app.ui.component.rebootlistpopup

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import com.oneuialoha.app.oneUIAlohaApp
import com.oneuialoha.app.oneuixp.OneUIAlohaPreferenceBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.oneuialoha.app.R
import com.oneuialoha.app.ui.LocalUiMode
import com.oneuialoha.app.ui.UiMode

data class RebootListOption(
    @param:StringRes val labelRes: Int,
    val reason: String,
)

@Composable
fun getRebootListOption(): List<RebootListOption> =
    listOf(
        RebootListOption(R.string.oneuialoha_rebootSystemUI_menu, "statusbar_xp_systemui"),
        RebootListOption(R.string.oneuialoha_softRebootRuntime_menu, "soft_reboot_android_runtime"),
    )

@Composable
fun rememberRebootAction(): (String) -> Unit {
    val scope = rememberCoroutineScope()
    return { reason ->
        if (reason == "statusbar_xp_systemui" || reason == "soft_reboot_android_runtime") {
            scope.launch {
                val synchronized = OneUIAlohaPreferenceBridge.synchronizeNow(oneUIAlohaApp)
                if (!synchronized) {
                    Toast.makeText(
                        oneUIAlohaApp,
                        R.string.oneuialoha_preferenceSync_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }

                when (reason) {
                    "statusbar_xp_systemui" -> {
                        val restarted = withContext(Dispatchers.IO) {
                            runCatching {
                                ProcessBuilder(
                                    "su",
                                    "-c",
                                    "pkill -TERM -f com.android.systemui"
                                ).start()
                                true
                            }.getOrDefault(false)
                        }
                        if (!restarted) {
                            Toast.makeText(
                                oneUIAlohaApp,
                                R.string.oneuialoha_restartSystemUI_failed,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    "soft_reboot_android_runtime" -> {
                        Toast.makeText(
                            oneUIAlohaApp,
                            R.string.oneuialoha_softRebootStarted,
                            Toast.LENGTH_SHORT,
                        ).show()
                        val started = withContext(Dispatchers.IO) {
                            runCatching {
                                ProcessBuilder(
                                    "su",
                                    "-c",
                                    "(sleep 0.3; setprop ctl.restart zygote) >/dev/null 2>&1 &"
                                ).start()
                                true
                            }.getOrDefault(false)
                        }
                        if (!started) {
                            Toast.makeText(
                                oneUIAlohaApp,
                                R.string.oneuialoha_softRebootFailed,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
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
