package com.oneuixp.hook.systemui.powermenu

import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.samsung.android.globalactions.presentation.SamsungGlobalActions
import com.samsung.android.globalactions.presentation.viewmodel.ActionInfo
import com.samsung.android.globalactions.presentation.viewmodel.ActionViewModel
import com.samsung.android.globalactions.presentation.viewmodel.ViewType
import com.oneuixp.hook.RebootActivity
import com.oneuixp.common.BuildConfig
import com.oneuixp.common.PowerMenuAction
import com.oneuixp.hook.R
import com.oneuixp.hook.util.currentContext
import com.oneuixp.common.R as CommonR

class RestartRecoveryActionViewModel(
    private val globalActions: SamsungGlobalActions,
) : ActionViewModel {
    private val actionInfo = ActionInfo().apply {
        val context = currentContext()
        name = PowerMenuAction.RESTART_RECOVERY
        viewType = ViewType.CENTER_ICON_3P_VIEW
        icon = R.drawable.ic_restart_recovery
        label = context.getString(CommonR.string.restartRecovery)
    }

    override fun getActionInfo(): ActionInfo = actionInfo

    override fun onPress() {
        if (!globalActions.isActionConfirming()) {
            globalActions.confirmAction(this)
            return
        }

        globalActions.dismissDialog(false)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent("recovery").apply {
                setClassName(BuildConfig.MODULE_APPLICATION_ID, RebootActivity::class.java.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            currentContext().startActivity(intent)
        }, 100L)
    }

    override fun setActionInfo(actionInfo: ActionInfo) {}
}
