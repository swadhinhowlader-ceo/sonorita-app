package com.sonorita.assistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.sonorita.assistant.data.SonoritaDatabase

class SonoritaApp : Application() {

    companion object {
        lateinit var instance: SonoritaApp
            private set

        const val CHANNEL_SERVICE = "sonorita_service"
        const val CHANNEL_ALERTS = "sonorita_alerts"
        const val CHANNEL_REMINDERS = "sonorita_reminders"
        const val CHANNEL_CALLS = "sonorita_calls"
    }

    val database: SonoritaDatabase by lazy {
        SonoritaDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Sonorita Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sonorita background service"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Sonorita Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important alerts and notifications"
                enableVibration(true)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Sonorita Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders and timers"
            }

            val callsChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Sonorita Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Call-related notifications"
            }

            manager.createNotificationChannels(
                listOf(serviceChannel, alertsChannel, remindersChannel, callsChannel)
            )
        }
    }
}
