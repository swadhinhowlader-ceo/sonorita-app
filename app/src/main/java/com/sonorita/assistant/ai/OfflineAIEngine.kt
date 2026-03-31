package com.sonorita.assistant.ai

import android.content.Context
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class OfflineAIEngine(
    private val context: Context,
    private val preferenceDao: PreferenceDao
) {

    private var isOfflineMode = false

    // Offline command patterns (Bengali + English)
    private val offlineCommands = mapOf(
        // System
        "flash on" to "flash_on", "flash off" to "flash_off",
        "ফ্ল্যাশ জ্বালাও" to "flash_on", "ফ্ল্যাশ বন্ধ করো" to "flash_off",
        "wifi on" to "wifi_on", "wifi off" to "wifi_off",
        "ওয়াইফাই চালু" to "wifi_on", "ওয়াইফাই বন্ধ" to "wifi_off",
        "bluetooth on" to "bt_on", "bluetooth off" to "bt_off",
        "ব্লুটুথ চালু" to "bt_on", "ব্লুটুথ বন্ধ" to "bt_off",
        "volume up" to "vol_up", "volume down" to "vol_down",
        "ভলিউম বাড়াও" to "vol_up", "ভলিউম কমাও" to "vol_down",
        "battery" to "battery", "ব্যাটারি" to "battery",
        "lock" to "lock", "unlock" to "unlock",
        "screenshot" to "screenshot",

        // Time & Date
        "time" to "time", "date" to "date",
        "সময় কত" to "time", "তারিখ কত" to "date",
        "ki somoy" to "time", "ki tarikh" to "date",

        // Calls & SMS
        "call" to "call", "sms" to "sms",
        "কল করো" to "call", "এসএমএস" to "sms",

        // Basic
        "help" to "help", "সাহায্য" to "help",
        "stop" to "stop", "start" to "start",
        "status" to "status"
    )

    // Offline responses (no internet needed)
    private val offlineResponses = mapOf(
        "flash_on" to "🔦 Flash chalu!",
        "flash_off" to "🔦 Flash bondho!",
        "wifi_on" to "📶 WiFi chalu!",
        "wifi_off" to "📶 WiFi bondho!",
        "bt_on" to "🔵 Bluetooth chalu!",
        "bt_off" to "🔵 Bluetooth bondho!",
        "vol_up" to "🔊 Volume barachi!",
        "vol_down" to "🔉 Volume komachi!",
        "battery" to "🔋 Battery check korchi...",
        "lock" to "🔒 Screen lock korchi!",
        "screenshot" to "📸 Screenshot nicchi!",
        "time" to "⏰ Somoy: ${getCurrentTime()}",
        "date" to "📅 Tarikh: ${getCurrentDate()}",
        "stop" to "🔇 SILENT mode on!",
        "start" to "🔊 ACTIVE mode on!",
        "status" to "📊 Status check korchi...",
        "help" to "🆘 Available commands: flash on/off, wifi on/off, bluetooth on/off, volume up/down, battery, lock, time, date, screenshot, stop, start"
    )

    fun enableOfflineMode() {
        isOfflineMode = true
        preferenceDao.set(PreferenceEntity("offline_mode", "true"))
    }

    fun disableOfflineMode() {
        isOfflineMode = false
        preferenceDao.set(PreferenceEntity("offline_mode", "false"))
    }

    fun isOfflineMode(): Boolean = isOfflineMode

    fun processOffline(text: String): String {
        val lower = text.lowercase().trim()

        // Find matching command
        for ((pattern, command) in offlineCommands) {
            if (lower.contains(pattern.lowercase())) {
                return offlineResponses[command] ?: "Command '$command' recognized but no offline response."
            }
        }

        // Fallback response
        return if (isOfflineMode) {
            "📵 Offline mode e AI chat available na. " +
            "Internet on koro or offline commands use koro: flash, wifi, bluetooth, volume, battery, time, date"
        } else {
            "Command bujhte parlam na. Internet connect koro for AI chat."
        }
    }

    fun getOfflineCapabilities(): String {
        return buildString {
            appendLine("📵 Offline Capabilities:")
            appendLine("• System controls (flash, wifi, bluetooth, volume)")
            appendLine("• Phone controls (lock, unlock, screenshot)")
            appendLine("• Time & Date")
            appendLine("• Battery status")
            appendLine("• Call & SMS (basic)")
            appendLine("• File operations (local)")
            appendLine("• Notes (local storage)")
            appendLine()
            appendLine("❌ Needs Internet:")
            appendLine("• AI Chat")
            appendLine("• Web search")
            appendLine("• Translation (other languages)")
            appendLine("• Research")
        }
    }

    private fun getCurrentTime(): String {
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}
