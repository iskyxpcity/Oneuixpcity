package com.oneuixp.app.ui.component.rebootlistpopup

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.oneuix.OneuiXPreferenceBridge
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
    listOf(RebootListOption(R.string.reboot_soft, "statusbar_xp_systemui"))

@Composable
fun rememberRebootAction(): (String) -> Unit {
    val scope = rememberCoroutineScope()
    return { reason ->
        if (reason == "statusbar_xp_systemui") {
            scope.launch {
                val synchronized = OneuiXPreferenceBridge.synchronizeNow(oneuiXPApp)
                if (!synchronized) {
                    Toast.makeText(
                        oneuiXPApp,
                        R.string.oneuix_preferenceSync_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }

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
                        oneuiXPApp,
                        R.string.oneuix_restartSystemUI_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
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
