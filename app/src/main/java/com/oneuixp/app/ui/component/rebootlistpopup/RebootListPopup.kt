package com.oneuixp.app.ui.component.rebootlistpopup

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import com.oneuixp.app.oneuiXPPApp
import com.oneuixp.app.oneuixp.OneuiXPPPreferenceBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    listOf(
        RebootListOption(R.string.oneuixp_rebootSystemUI_menu, "statusbar_xp_systemui"),
        RebootListOption(R.string.oneuixp_softRebootRuntime_menu, "soft_reboot_android_runtime"),
    )

@Composable
fun rememberRebootAction(): (String) -> Unit {
    val scope = rememberCoroutineScope()
    return { reason ->
        if (reason == "statusbar_xp_systemui" || reason == "soft_reboot_android_runtime") {
            scope.launch {
                val synchronized = OneuiXPPPreferenceBridge.synchronizeNow(oneuiXPPApp)
                if (!synchronized) {
                    Toast.makeText(
                        oneuiXPPApp,
                        R.string.oneuixp_preferenceSync_failed,
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
                                oneuiXPPApp,
                                R.string.oneuixp_restartSystemUI_failed,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    "soft_reboot_android_runtime" -> {
                        Toast.makeText(
                            oneuiXPPApp,
                            R.string.oneuixp_softRebootStarted,
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
                                oneuiXPPApp,
                                R.string.oneuixp_softRebootFailed,
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
