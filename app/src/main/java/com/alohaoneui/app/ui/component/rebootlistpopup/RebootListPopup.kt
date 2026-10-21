package com.alohaoneui.app.ui.component.rebootlistpopup

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import com.alohaoneui.app.alohaOneUIApp
import com.alohaoneui.app.alohaoneui.AlohaOneUIPreferenceBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.alohaoneui.app.R
import com.alohaoneui.app.ui.LocalUiMode
import com.alohaoneui.app.ui.UiMode

data class RebootListOption(
    @param:StringRes val labelRes: Int,
    val reason: String,
)

@Composable
fun getRebootListOption(): List<RebootListOption> =
    listOf(
        RebootListOption(R.string.alohaoneui_rebootSystemUI_menu, "statusbar_xp_systemui"),
        RebootListOption(R.string.alohaoneui_softRebootRuntime_menu, "soft_reboot_android_runtime"),
    )

@Composable
fun rememberRebootAction(): (String) -> Unit {
    val scope = rememberCoroutineScope()
    return { reason ->
        if (reason == "statusbar_xp_systemui" || reason == "soft_reboot_android_runtime") {
            scope.launch {
                val synchronized = AlohaOneUIPreferenceBridge.synchronizeNow(alohaOneUIApp)
                if (!synchronized) {
                    Toast.makeText(
                        alohaOneUIApp,
                        R.string.alohaoneui_preferenceSync_failed,
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
                                alohaOneUIApp,
                                R.string.alohaoneui_restartSystemUI_failed,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    "soft_reboot_android_runtime" -> {
                        Toast.makeText(
                            alohaOneUIApp,
                            R.string.alohaoneui_softRebootStarted,
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
                                alohaOneUIApp,
                                R.string.alohaoneui_softRebootFailed,
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
