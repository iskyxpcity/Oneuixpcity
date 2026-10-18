package com.oneuixp.app.ui.screen.home

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Exposes only the currently active notification actions needed for the
 * LSPosed module-page shortcut. Notification text/content is never persisted.
 */
class LsposedNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedListener = this
    }

    override fun onListenerDisconnected() {
        if (connectedListener === this) {
            connectedListener = null
        }
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        if (connectedListener === this) {
            connectedListener = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var connectedListener: LsposedNotificationListenerService? = null

        fun hasNotificationAccess(context: Context): Boolean {
            val enabledComponents = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty().split(':')
                .mapNotNull(ComponentName::unflattenFromString)

            val component = ComponentName(context, LsposedNotificationListenerService::class.java)
            return enabledComponents.any { it == component }
        }

        /**
         * LSPosed posts the module notification as package "android" with the
         * module package name as its tag. Reuse that live system-issued
         * PendingIntent rather than recreating LSPosed's private UUID action.
         */
        fun openOwnModuleNotification(context: Context): Boolean {
            val listener = connectedListener ?: return false
            val match: StatusBarNotification = runCatching {
                listener.activeNotifications
                    ?.firstOrNull { notification ->
                        notification.packageName == "android" &&
                            notification.tag == context.packageName &&
                            notification.notification.contentIntent != null
                    }
            }.getOrNull() ?: return false

            val contentIntent = match.notification.contentIntent ?: return false
            return runCatching {
                // Mirror a notification tap: remove it, then send its real action.
                listener.cancelNotification(match.key)
                contentIntent.send()
                true
            }.getOrDefault(false)
        }
    }
}
