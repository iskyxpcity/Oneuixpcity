package com.oneuialoha.hook.systemui.powermenu

import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.samsung.android.globalactions.presentation.SamsungGlobalActions
import com.samsung.android.globalactions.presentation.viewmodel.ActionInfo
import com.samsung.android.globalactions.presentation.viewmodel.ActionViewModel
import com.samsung.android.globalactions.presentation.viewmodel.ViewType
import com.oneuialoha.hook.RebootActivity
import com.oneuialoha.common.BuildConfig
import com.oneuialoha.common.PowerMenuAction
import com.oneuialoha.hook.R
import com.oneuialoha.hook.util.currentContext
import com.oneuialoha.common.R as CommonR

class RestartDownloadActionViewModel(
    private val globalActions: SamsungGlobalActions,
) : ActionViewModel {
    private val actionInfo = ActionInfo().apply {
        val context = currentContext()
        name = PowerMenuAction.RESTART_DOWNLOAD
        viewType = ViewType.CENTER_ICON_3P_VIEW
        icon = R.drawable.ic_restart_download
        label = context.getString(CommonR.string.restartDownload)
    }

    override fun getActionInfo(): ActionInfo = actionInfo

    override fun onPress() {
        if (!globalActions.isActionConfirming()) {
            globalActions.confirmAction(this)
            return
        }

        globalActions.dismissDialog(false)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent("download").apply {
                setClassName(BuildConfig.MODULE_APPLICATION_ID, RebootActivity::class.java.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            currentContext().startActivity(intent)
        }, 100L)
    }

    override fun setActionInfo(actionInfo: ActionInfo) {}
}
