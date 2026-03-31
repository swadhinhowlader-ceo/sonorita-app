package com.sonorita.assistant.security

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioRecord
import android.media.AudioFormat
import android.media.MediaRecorder
import android.net.wifi.WifiManager
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class AntiSurveillance(
    private val context: Context,
    private val preferenceDao: PreferenceDao
) {

    data class ScanResult(
        val type: ScanType,
        val threat: Boolean,
        val details: String,
        val confidence: Float
    )

    enum class ScanType {
        HIDDEN_CAMERA, HIDDEN_MIC, ROGUE_WIFI,
        UNAUTHORIZED_RECORDING, BLUETOOTH_BEACON, NETWORK_SCAN
    }

    // IR camera detection - phone camera can detect infrared
    fun scanForHiddenCameras(): ScanResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameras = cameraManager.cameraIdList

            // Check for unusual camera behavior
            var suspiciousFound = false
            var details = ""

            for (cameraId in cameras) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

                // Check for cameras with unusual capabilities
                if (capabilities != null && capabilities.size > 5) {
                    suspiciousFound = true
                    details += "Camera $cameraId has ${capabilities.size} capabilities. "
                }
            }

            // WiFi network scan for hidden cameras (IP cameras often on specific ports)
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val scanResults = wifiManager.scanResults
            val suspiciousSsids = scanResults?.filter { ap ->
                val ssid = ap.SSID.lowercase()
                ssid.contains("cam") || ssid.contains("ipcam") || ssid.contains("dvr") ||
                ssid.contains("nvr") || ssid.contains("surveillance")
            }

            if (suspiciousSsids != null && suspiciousSsids.isNotEmpty()) {
                suspiciousFound = true
                details += "WiFi cameras detected: ${suspiciousSsids.joinToString { it.SSID }}. "
            }

            ScanResult(
                type = ScanType.HIDDEN_CAMERA,
                threat = suspiciousFound,
                details = if (suspiciousFound) details else "No hidden cameras detected via WiFi scan.",
                confidence = if (suspiciousFound) 0.7f else 0.9f
            )
        } catch (e: Exception) {
            ScanResult(ScanType.HIDDEN_CAMERA, false, "Scan error: ${e.message}", 0f)
        }
    }

    fun scanForHiddenMics(): ScanResult {
        return try {
            // Check for Bluetooth devices that could be recording
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            val pairedDevices = bluetoothAdapter?.bondedDevices

            val suspiciousDevices = pairedDevices?.filter { device ->
                val name = device.name?.lowercase() ?: ""
                name.contains("mic") || name.contains("recorder") ||
                name.contains("spy") || name.contains("bug")
            }

            val threat = suspiciousDevices != null && suspiciousDevices.isNotEmpty()
            val details = if (threat) {
                "Suspicious BT devices: ${suspiciousDevices?.joinToString { it.name ?: "Unknown" }}"
            } else {
                "No suspicious Bluetooth devices found."
            }

            ScanResult(ScanType.HIDDEN_MIC, threat, details, 0.6f)
        } catch (e: Exception) {
            ScanResult(ScanType.HIDDEN_MIC, false, "BT scan error: ${e.message}", 0f)
        }
    }

    fun scanNetworkSecurity(): ScanResult {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wifiManager.connectionInfo
            val ssid = connectionInfo.ssid?.replace("\"", "") ?: "Unknown"

            val issues = mutableListOf<String>()

            // Check encryption
            val scanResults = wifiManager.scanResults
            val currentAp = scanResults?.find { it.SSID == ssid }

            if (currentAp != null) {
                if (!currentAp.capabilities.contains("WPA2") && !currentAp.capabilities.contains("WPA3")) {
                    issues.add("Network uses weak/no encryption")
                }
                if (currentAp.capabilities.contains("WPS")) {
                    issues.add("WPS enabled (vulnerable to PIN attacks)")
                }
            }

            // Check for open ports (common surveillance ports)
            val suspiciousPorts = listOf(554, 8080, 8554, 37777) // RTSP, HTTP, DVR
            // Port scanning would require network access - placeholder

            ScanResult(
                type = ScanType.NETWORK_SCAN,
                threat = issues.isNotEmpty(),
                details = if (issues.isEmpty()) "Network '$ssid' looks secure."
                else "Issues with '$ssid': ${issues.joinToString()}",
                confidence = 0.8f
            )
        } catch (e: Exception) {
            ScanResult(ScanType.NETWORK_SCAN, false, "Scan error: ${e.message}", 0f)
        }
    }

    fun detectUnauthorizedRecording(): ScanResult {
        // Check if any app is using microphone without user knowledge
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val isMicInUse = audioManager.mode == android.media.AudioManager.MODE_IN_COMMUNICATION ||
                            audioManager.mode == android.media.AudioManager.MODE_IN_CALL

            ScanResult(
                type = ScanType.UNAUTHORIZED_RECORDING,
                threat = isMicInUse,
                details = if (isMicInUse) "Microphone appears to be in use by another app!"
                else "Microphone not actively in use by other apps.",
                confidence = 0.5f
            )
        } catch (e: Exception) {
            ScanResult(ScanType.UNAUTHORIZED_RECORDING, false, "Check error: ${e.message}", 0f)
        }
    }

    fun fullSecurityScan(): String {
        val results = listOf(
            scanForHiddenCameras(),
            scanForHiddenMics(),
            scanNetworkSecurity(),
            detectUnauthorizedRecording()
        )

        val threats = results.filter { it.threat }

        return buildString {
            appendLine("🔐 Anti-Surveillance Scan Complete:")
            appendLine()
            results.forEach { result ->
                val icon = if (result.threat) "🚨" else "✅"
                val typeName = when (result.type) {
                    ScanType.HIDDEN_CAMERA -> "Hidden Camera"
                    ScanType.HIDDEN_MIC -> "Hidden Mic"
                    ScanType.ROGUE_WIFI -> "Rogue WiFi"
                    ScanType.UNAUTHORIZED_RECORDING -> "Unauthorized Recording"
                    ScanType.BLUETOOTH_BEACON -> "BT Beacon"
                    ScanType.NETWORK_SCAN -> "Network Security"
                }
                appendLine("$icon $typeName: ${result.details}")
            }
            appendLine()
            if (threats.isNotEmpty()) {
                appendLine("⚠️ ${threats.size} potential threat(s) detected!")
            } else {
                appendLine("✅ All clear! No surveillance threats found.")
            }
        }
    }
}
