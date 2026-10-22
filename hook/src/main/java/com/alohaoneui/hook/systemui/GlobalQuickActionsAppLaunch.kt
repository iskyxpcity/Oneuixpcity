package com.alohaoneui.hook.systemui

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.UserHandle
import android.util.Log
import android.widget.Toast

internal fun GlobalQuickActions.startActivityComponent(context: Context, packageName: String, className: String) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        component = ComponentName(packageName, className)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

internal fun GlobalQuickActions.startActivityUri(context: Context, uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

internal fun GlobalQuickActions.openQuickCamera(context: Context) {
    runCatching {
        context.startActivity(Intent("android.media.action.STILL_IMAGE_CAMERA").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.sec.android.app.camera")
        })
    }.recoverCatching {
        startActivityComponent(context, "com.sec.android.app.camera", "com.sec.android.app.camera.Camera")
    }.getOrThrow()
}

internal fun GlobalQuickActions.openCameraCapture(context: Context, video: Boolean) {
    val action = if (video) "android.media.action.VIDEO_CAMERA" else "android.media.action.IMAGE_CAPTURE"
    runCatching {
        context.startActivity(Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.sec.android.app.camera")
        })
    }.recoverCatching {
        openQuickCamera(context)
    }.getOrThrow()
}

internal fun GlobalQuickActions.startCustomApp(context: Context, action: String) {
    val flattened = action.removePrefix(ACTION_CUSTOM_APP_PREFIX)
    val component = ComponentName.unflattenFromString(flattened) ?: return
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        this.component = component
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
    context.startActivity(intent)
}

internal fun GlobalQuickActions.killForegroundApp(context: Context) {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
    val task = runCatching {
        @Suppress("DEPRECATION")
        activityManager.getRunningTasks(1).firstOrNull()
    }.getOrNull() ?: return
    val component = task.topActivity ?: return
    val packageName = component.packageName
    if (isProtectedKillPackage(packageName)) {
        showToast(context, "不允许结束该程序")
        return
    }
    runCatching {
        GlobalQuickActions.callCompatible(activityManager, "semRemoveTask", task.id, 1)
    }.recoverCatching {
        GlobalQuickActions.callCompatible(activityManager, "removeTask", task.id)
    }
    runCatching {
        GlobalQuickActions.callCompatible(activityManager, "forceStopPackage", packageName)
    }.recoverCatching {
        runShellCommand("am force-stop ${shellQuote(packageName)}")
    }
    showToast(context, "已结束程序")
}

internal fun GlobalQuickActions.cleanMemory(context: Context) {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
    val protectedPackages = setOf(
        "android",
        "system",
        PACKAGE_ANDROID,
        PACKAGE_SYSTEMUI,
        PACKAGE_APP
    )
    activityManager.runningAppProcesses.orEmpty().forEach { process ->
        val packageName = process.processName.substringBefore(":")
        if (process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) return@forEach
        if (packageName in protectedPackages || packageName.startsWith("com.android.")) return@forEach
        runCatching { activityManager.killBackgroundProcesses(packageName) }
    }
    showToast(context, "内存已清理")
}

internal fun GlobalQuickActions.toggleScreenRecord(context: Context) {
    runCatching {
        startSamsungCaptureSideButtonAction(context, "record_screen")
    }.recoverCatching {
        val intent = Intent("com.samsung.android.app.screenrecorder.ACTION_START").apply {
            component = ComponentName(
                "com.samsung.android.app.smartcapture",
                "com.samsung.android.app.smartcapture.screenrecorder.ScreenRecorderService"
            )
        }
        val currentUser = UserHandle::class.java.getDeclaredField("CURRENT").apply { isAccessible = true }.get(null) as UserHandle
        runCatching {
            GlobalQuickActions.callCompatible(context, "startServiceAsUser", intent, currentUser)
        }.recoverCatching {
            context.startService(intent)
        }.getOrThrow()
    }.onFailure {
        runShellCommand("cmd statusbar click-tile com.samsung.android.app.smartcapture/com.samsung.android.app.screenrecorder.view.RecordScreenTile")
    }
}

internal fun GlobalQuickActions.isProtectedKillPackage(packageName: String): Boolean {
    return packageName == PACKAGE_ANDROID ||
        packageName == PACKAGE_SYSTEMUI ||
        packageName == PACKAGE_APP ||
        packageName == "android" ||
        packageName == "system" ||
        packageName.startsWith("com.android.systemui")
}

internal fun GlobalQuickActions.shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

internal fun GlobalQuickActions.showToast(context: Context, text: String) {
    postOnMain {
        runCatching {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun GlobalQuickActions.getPowerManager(context: Context): Any {
    return context.getSystemService(Context.POWER_SERVICE)
}

internal fun GlobalQuickActions.getInputManager(): Any {
    return GlobalQuickActions.callCompatible(
        Class.forName("android.hardware.input.InputManager"),
        "getInstance"
    ) ?: throw IllegalStateException("InputManager.getInstance() returned null")
}
