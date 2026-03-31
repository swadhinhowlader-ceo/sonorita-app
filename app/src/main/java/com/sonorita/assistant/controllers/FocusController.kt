package com.sonorita.assistant.controllers

import android.app.NotificationManager
import android.content.Context
import android.os.CountDownTimer

class FocusController(private val context: Context) {

    private var focusTimer: CountDownTimer? = null
    private var isFocusActive = false
    private var focusEndTime: Long = 0

    fun startFocus(text: String): String {
        if (isFocusActive) {
            return "Focus mode already active! 'focus mode bondho' to stop."
        }

        // Parse duration
        val timePattern = Regex("(\\d+)\\s*(minute|hour|ghonta|min|hr)", RegexOption.IGNORE_CASE)
        val match = timePattern.find(text)
        val amount = match?.groupValues?.get(1)?.toIntOrNull() ?: 60
        val unit = match?.groupValues?.get(2)?.lowercase() ?: "minute"

        val durationMs = when {
            unit.startsWith("min") -> amount * 60 * 1000L
            unit.startsWith("hour") || unit.startsWith("ghont") || unit.startsWith("hr") -> amount * 60 * 60 * 1000L
            else -> amount * 60 * 1000L
        }

        // Enable DND
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

        // Start timer
        isFocusActive = true
        focusEndTime = System.currentTimeMillis() + durationMs

        focusTimer = object : CountDownTimer(durationMs, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = millisUntilFinished / 60000
                if (minutesLeft % 10 == 0L && minutesLeft > 0) {
                    // Could broadcast remaining time
                }
            }

            override fun onFinish() {
                stopFocus()
            }
        }.start()

        val hours = durationMs / (60 * 60 * 1000)
        val minutes = (durationMs % (60 * 60 * 1000)) / (60 * 1000)
        val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

        return "🎯 Focus mode on! $durationStr er jonno shob notification bondho. Kaj koro!"
    }

    fun stopFocus(): String {
        if (!isFocusActive) {
            return "Focus mode active nei."
        }

        focusTimer?.cancel()
        focusTimer = null
        isFocusActive = false

        // Restore notifications
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

        return "✅ Focus mode bondho! Shob notification abar asche."
    }

    fun isFocusActive(): Boolean = isFocusActive

    fun getRemainingTime(): Long {
        return if (isFocusActive) {
            (focusEndTime - System.currentTimeMillis()).coerceAtLeast(0)
        } else 0
    }
}
