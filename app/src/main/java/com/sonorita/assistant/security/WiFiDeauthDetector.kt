package com.sonorita.assistant.security

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiInfo
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class WiFiDeauthDetector(
    private val context: Context,
    private val preferenceDao: PreferenceDao
) {

    data class DeauthAlert(
        val timestamp: Long,
        val ssid: String,
        val bssid: String,
        val attackType: AttackType,
        val details: String
    )

    enum class AttackType {
        DEAUTH, DISASSOC, ROGUE_AP, EVIL_TWIN, KARMA, MANA
    }

    data class NetworkHealth(
        val ssid: String,
        val signalStrength: Int,
        val channel: Int,
        val security: String,
        val isDeauthDetected: Boolean,
        val isRogueAp: Boolean,
        val clientCount: Int
    )

    private val alerts = mutableListOf<DeauthAlert>()
    private val knownNetworks = mutableMapOf<String, Pair<Int, String>>() // BSSID -> (channel, security)
    private var isMonitoring = false
    private var lastSignalStrength = 0
    private var consecutiveWeakSignals = 0

    fun startMonitoring(): String {
        isMonitoring = true
        return "📡 WiFi Deauth Detection started! Monitoring for attacks..."
    }

    fun stopMonitoring(): String {
        isMonitoring = false
        return "📡 WiFi monitoring stopped."
    }

    fun scanNetworkHealth(): NetworkHealth? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo ?: return null
        val scanResults = wifiManager.scanResults

        val ssid = connectionInfo.ssid?.replace("\"", "") ?: return null
        val bssid = connectionInfo.bssid ?: return null
        val rssi = connectionInfo.rssi
        val frequency = connectionInfo.frequency

        // Check for deauth indicators
        val deauthDetected = detectDeauthPattern(rssi, ssid)

        // Check for rogue APs (same SSID, different BSSID)
        val rogueApDetected = detectRogueAP(ssid, bssid, scanResults)

        // Check for evil twin (same SSID, weaker security)
        val evilTwinDetected = detectEvilTwin(ssid, bssid, scanResults)

        // Count clients (approximate)
        val currentAp = scanResults?.find { it.BSSID == bssid }
        val clientCount = estimateClientCount(currentAp)

        return NetworkHealth(
            ssid = ssid,
            signalStrength = rssi,
            channel = if (frequency > 5000) (frequency - 5000) / 5 else (frequency - 2407) / 5,
            security = currentAp?.capabilities ?: "Unknown",
            isDeauthDetected = deauthDetected,
            isRogueAp = rogueApDetected || evilTwinDetected,
            clientCount = clientCount
        )
    }

    private fun detectDeauthPattern(currentRssi: Int, ssid: String): Boolean {
        // Deauth attacks often cause sudden signal drops
        if (lastSignalStrength == 0) {
            lastSignalStrength = currentRssi
            return false
        }

        val signalDrop = lastSignalStrength - currentRssi
        lastSignalStrength = currentRssi

        // Sudden drop of >20 dBm could indicate deauth
        if (signalDrop > 20) {
            consecutiveWeakSignals++

            if (consecutiveWeakSignals >= 3) {
                val alert = DeauthAlert(
                    timestamp = System.currentTimeMillis(),
                    ssid = ssid,
                    bssid = "",
                    attackType = AttackType.DEAUTH,
                    details = "Sudden signal drop detected (${signalDrop}dBm). Possible deauth attack!"
                )
                alerts.add(alert)
                consecutiveWeakSignals = 0
                return true
            }
        } else {
            consecutiveWeakSignals = 0
        }

        return false
    }

    private fun detectRogueAP(
        connectedSsid: String,
        connectedBssid: String,
        scanResults: List<android.net.wifi.ScanResult>?
    ): Boolean {
        // Find APs with same SSID but different BSSID
        val sameSsidAps = scanResults?.filter {
            it.SSID == connectedSsid && it.BSSID != connectedBssid
        } ?: emptyList()

        if (sameSsidAps.isNotEmpty()) {
            val alert = DeauthAlert(
                timestamp = System.currentTimeMillis(),
                ssid = connectedSsid,
                bssid = connectedBssid,
                attackType = AttackType.ROGUE_AP,
                details = "Found ${sameSsidAps.size} AP(s) with same SSID but different BSSID! " +
                    "Possible rogue AP: ${sameSsidAps.joinToString { it.BSSID }}"
            )
            alerts.add(alert)
            return true
        }

        return false
    }

    private fun detectEvilTwin(
        connectedSsid: String,
        connectedBssid: String,
        scanResults: List<android.net.wifi.ScanResult>?
    ): Boolean {
        // Evil twin has same SSID but often weaker security or different channel
        val currentAp = scanResults?.find { it.BSSID == connectedBssid } ?: return false
        val otherAps = scanResults?.filter {
            it.SSID == connectedSsid && it.BSSID != connectedBssid
        } ?: emptyList()

        for (ap in otherAps) {
            // Check if security is weaker
            val currentSecurity = getSecurityLevel(currentAp.capabilities)
            val otherSecurity = getSecurityLevel(ap.capabilities)

            if (otherSecurity < currentSecurity) {
                val alert = DeauthAlert(
                    timestamp = System.currentTimeMillis(),
                    ssid = connectedSsid,
                    bssid = ap.BSSID,
                    attackType = AttackType.EVIL_TWIN,
                    details = "Possible Evil Twin! AP ${ap.BSSID} has weaker security (${ap.capabilities})"
                )
                alerts.add(alert)
                return true
            }
        }

        return false
    }

    private fun getSecurityLevel(capabilities: String): Int {
        return when {
            capabilities.contains("WPA3") -> 4
            capabilities.contains("WPA2") -> 3
            capabilities.contains("WPA") -> 2
            capabilities.contains("WEP") -> 1
            else -> 0 // Open
        }
    }

    private fun estimateClientCount(ap: android.net.wifi.ScanResult?): Int {
        // Rough estimate based on signal strength and frequency
        // Real implementation would need packet capture
        return -1 // Unknown without packet capture
    }

    fun getSecurityReport(): String {
        val health = scanNetworkHealth() ?: return "Not connected to WiFi."

        return buildString {
            appendLine("📡 WiFi Security Report:")
            appendLine("Network: ${health.ssid}")
            appendLine("Signal: ${health.signalStrength} dBm ${getSignalEmoji(health.signalStrength)}")
            appendLine("Channel: ${health.channel}")
            appendLine("Security: ${health.security}")
            appendLine()

            if (health.isDeauthDetected) {
                appendLine("🚨 DEAUTH ATTACK DETECTED! Someone may be trying to disconnect you!")
            }
            if (health.isRogueAp) {
                appendLine("🚨 ROGUE AP DETECTED! Same network name, different access point!")
            }
            if (!health.isDeauthDetected && !health.isRogueAp) {
                appendLine("✅ No attacks detected. Network looks clean.")
            }

            if (alerts.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ Recent alerts (${alerts.size}):")
                alerts.takeLast(5).forEach { alert ->
                    val time = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(alert.timestamp))
                    appendLine("• $time [${alert.attackType}] ${alert.details.take(60)}")
                }
            }
        }
    }

    fun getProtectionTips(): String {
        return buildString {
            appendLine("🛡️ WiFi Protection Tips:")
            appendLine("• Always use WPA3 or WPA2 encryption")
            appendLine("• Avoid open/public WiFi for sensitive tasks")
            appendLine("• Use VPN on public networks")
            appendLine("• Check for rogue APs regularly")
            appendLine("• Enable MAC filtering if available")
            appendLine("• Update router firmware regularly")
            appendLine("• Disable WPS on your router")
        }
    }

    private fun getSignalEmoji(rssi: Int): String = when {
        rssi > -50 -> "🟢 Excellent"
        rssi > -60 -> "🟡 Good"
        rssi > -70 -> "🟠 Fair"
        else -> "🔴 Weak"
    }

    fun getAlerts(): List<DeauthAlert> = alerts.toList()
    fun isMonitoring(): Boolean = isMonitoring
}
