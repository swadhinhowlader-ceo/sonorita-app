package com.sonorita.assistant.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SonoritaNotificationListener : NotificationListenerService() {

    companion object {
        var instance: SonoritaNotificationListener? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras?.getString("android.title") ?: ""
        val text = extras?.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""

        // Skip own notifications
        if (packageName == "com.sonorita.assistant") return

        // Skip low priority
        if (notification.priority < 0) return

        // Broadcast notification
        val intent = android.content.Intent("com.sonorita.NOTIFICATION").apply {
            putExtra("package", packageName)
            putExtra("title", title)
            putExtra("text", bigText.ifEmpty { text })
            putExtra("id", sbn.id)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Handle notification removal
    }

    override fun onListenerDisconnected() {
        instance = null
        super.onListenerDisconnected()
    }

    fun getActiveNotifications(): List<StatusBarNotification> {
        return try {
            activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun cancelNotification(key: String) {
        try {
            cancelNotification(key)
        } catch (e: Exception) {
            // Handle error
        }
    }
}
