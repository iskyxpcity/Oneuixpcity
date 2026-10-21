package com.alohaoneui.hook.systemui.powermenu

import android.os.Handler
import android.os.Looper
import android.os.Process
import com.samsung.android.globalactions.presentation.SamsungGlobalActions
import com.samsung.android.globalactions.presentation.viewmodel.ActionInfo
import com.samsung.android.globalactions.presentation.viewmodel.ActionViewModel
import com.samsung.android.globalactions.presentation.viewmodel.ViewType
import com.alohaoneui.common.PowerMenuAction
import com.alohaoneui.hook.R
import com.alohaoneui.hook.util.currentContext
import com.alohaoneui.common.R as CommonR

class RestartSystemUIActionViewModel(
    private val globalActions: SamsungGlobalActions,
) : ActionViewModel {
    private val actionInfo = ActionInfo().apply {
        val context = currentContext()
        name = PowerMenuAction.RESTART_SYSTEMUI
        viewType = ViewType.CENTER_ICON_3P_VIEW
        icon = R.drawable.ic_restart_system_ui
        label = context.getString(CommonR.string.restartSystemUI)
    }

    override fun getActionInfo(): ActionInfo = actionInfo

    override fun onPress() {
        if (!globalActions.isActionConfirming()) {
            globalActions.confirmAction(this)
            return
        }

        globalActions.dismissDialog(false)
        Handler(Looper.getMainLooper()).postDelayed({
            Process.killProcess(Process.myPid())
        }, 100L)
    }

    override fun setActionInfo(actionInfo: ActionInfo) {}
}
