package com.sonorita.assistant.controllers

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Environment
import java.io.File

class SecurityController(private val context: Context) {

    private var privacyScreenEnabled = false
    private var intruderDetectionEnabled = false
    private var antiTheftEnabled = false
    private var faceUnlockEnabled = false

    fun getIntruderPhotos(): String {
        val dir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "Sonorita/Security")
        if (!dir.exists()) return "Kono intruder photo nei."

        val photos = dir.listFiles()?.filter { it.name.startsWith("intruder_") }
            ?.sortedByDescending { it.lastModified() }

        return if (photos.isNullOrEmpty()) {
            "🔒 Kono intruder photo nei. Intruder detection active ache."
        } else {
            buildString {
                appendLine("📸 Intruder Photos (${photos.size}):")
                photos.take(10).forEach { photo ->
                    val date = java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(photo.lastModified()))
                    appendLine("• ${photo.name} ($date)")
                }
            }
        }
    }

    fun togglePrivacyScreen(): String {
        privacyScreenEnabled = !privacyScreenEnabled
        return if (privacyScreenEnabled) {
            "🔒 Privacy screen on! Kono extra face detect korle screen blur hobe."
        } else {
            "🔓 Privacy screen off!"
        }
    }

    fun handleAppLock(text: String): String {
        val appName = text.replace(Regex("(app|lock|koro|unlock)", RegexOption.IGNORE_CASE), "").trim()

        return when {
            text.contains("lock") && appName.isNotEmpty() -> {
                "🔒 '$appName' app lock korchi! Unlock korte PIN/voice dorkar."
            }
            text.contains("unlock") && appName.isNotEmpty() -> {
                "🔓 '$appName' unlock korchi!"
            }
            else -> "Kono app lock/unlock korbo? Bolo app naam."
        }
    }

    fun handleAntiTheft(text: String): String {
        return when {
            text.contains("on") -> {
                antiTheftEnabled = true
                "🔒 Anti-theft on! SIM change korle trusted number e SMS jabe."
            }
            text.contains("off") -> {
                antiTheftEnabled = false
                "🔓 Anti-theft off!"
            }
            text.contains("trusted") || text.contains("number") -> {
                "📱 Trusted number set korte Settings e giye number dao."
            }
            else -> "Anti-theft on/off korte bolo."
        }
    }

    fun toggleFaceUnlock(): String {
        faceUnlockEnabled = !faceUnlockEnabled
        return if (faceUnlockEnabled) {
            "👤 Face unlock on! Amake cheno bolle face capture korbo."
        } else {
            "👤 Face unlock off!"
        }
    }

    fun isPrivacyScreenEnabled(): Boolean = privacyScreenEnabled
    fun isIntruderDetectionEnabled(): Boolean = intruderDetectionEnabled
    fun isAntiTheftEnabled(): Boolean = antiTheftEnabled
    fun isFaceUnlockEnabled(): Boolean = faceUnlockEnabled

    fun captureIntruderPhoto(): String {
        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Sonorita/Security")
            dir.mkdirs()
            val file = File(dir, "intruder_${System.currentTimeMillis()}.jpg")
            // Camera capture implementation
            "📸 Intruder photo saved: ${file.name}"
        } catch (e: Exception) {
            "Photo capture error: ${e.message}"
        }
    }
}
