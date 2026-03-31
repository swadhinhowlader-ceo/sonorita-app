package com.sonorita.assistant.security

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.content.pm.PackageManager
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class IntrusionDetectionSystem(
    private val context: Context,
    private val preferenceDao: PreferenceDao
) {

    data class ThreatAlert(
        val type: ThreatType,
        val severity: Severity,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val action: String = ""
    )

    enum class ThreatType {
        ROGUE_WIFI, SUSPICIOUS_APP, PERMISSION_ABUSE,
        SPAM_CALL, DATA_LEAK, NETWORK_ATTACK,
        UNAUTHORIZED_ACCESS, MALWARE_SIGNATURE
    }

    enum class Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    private val alerts = mutableListOf<ThreatAlert>()
    private val trustedNetworks = mutableSetOf<String>()
    private val knownSpamNumbers = mutableSetOf<String>()

    init {
        // Load trusted networks
        val saved = preferenceDao.get("trusted_networks")
        if (saved != null) {
            trustedNetworks.addAll(saved.split(","))
        }
    }

    fun startMonitoring(): String {
        // Register for network changes
        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }

        return "🛡️ Intrusion Detection System active! Monitoring threats..."
    }

    fun scanForThreats(): List<ThreatAlert> {
        alerts.clear()

        // 1. Check for rogue WiFi
        checkRogueWifi()

        // 2. Check for suspicious apps
        checkSuspiciousApps()

        // 3. Check for permission abuse
        checkPermissionAbuse()

        // 4. Check for data leaks
        checkDataLeaks()

        return alerts.toList()
    }

    private fun checkRogueWifi() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wifiManager.connectionInfo
            val ssid = connectionInfo.ssid?.replace("\"", "") ?: return

            // Check if WiFi is open (no encryption)
            val scanResults = wifiManager.scanResults
            val currentAp = scanResults?.find { it.SSID == ssid }

            if (currentAp != null && currentAp.capabilities.contains("WEP")) {
                alerts.add(ThreatAlert(
                    type = ThreatType.ROGUE_WIFI,
                    severity = Severity.MEDIUM,
                    message = "⚠️ '$ssid' WEP encryption use kore — outdated & insecure!",
                    action = "Consider using a VPN on this network."
                ))
            }

            if (currentAp != null && !currentAp.capabilities.contains("WPA") && !currentAp.capabilities.contains("WEP")) {
                alerts.add(ThreatAlert(
                    type = ThreatType.ROGUE_WIFI,
                    severity = Severity.HIGH,
                    message = "🚨 '$ssid' OPEN network — no encryption! Data vulnerable!",
                    action = "Do NOT enter passwords or sensitive info on this network."
                ))
            }

            // Check if network is trusted
            if (ssid !in trustedNetworks && ssid != "<unknown ssid>") {
                alerts.add(ThreatAlert(
                    type = ThreatType.ROGUE_WIFI,
                    severity = Severity.LOW,
                    message = "📡 Unknown network: '$ssid'. Add to trusted list?",
                    action = "'trusted wifi add koro $ssid'"
                ))
            }
        } catch (e: Exception) {
            // Permission issue
        }
    }

    private fun checkSuspiciousApps() {
        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            // Check for apps with dangerous permissions
            val dangerousApps = installedApps.filter { appInfo ->
                val permissions = try {
                    pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                        .requestedPermissions?.toList() ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                val dangerousPerms = listOf(
                    "READ_SMS", "SEND_SMS", "READ_CONTACTS", "RECORD_AUDIO",
                    "CAMERA", "ACCESS_FINE_LOCATION", "READ_CALL_LOG"
                )

                permissions.count { it in dangerousPerms } >= 3
            }

            dangerousApps.take(3).forEach { app ->
                val appName = pm.getApplicationLabel(app).toString()
                alerts.add(ThreatAlert(
                    type = ThreatType.SUSPICIOUS_APP,
                    severity = Severity.MEDIUM,
                    message = "⚠️ '$appName' has many sensitive permissions",
                    action = "Review permissions in Settings > Apps"
                ))
            }
        } catch (e: Exception) {
            // Permission issue
        }
    }

    private fun checkPermissionAbuse() {
        // Check if any app has overlay permission + accessibility + notification listener
        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)

            val suspicious = installedApps.filter { app ->
                val perms = try {
                    pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                        .requestedPermissions?.toList() ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                perms.contains("SYSTEM_ALERT_WINDOW") &&
                perms.contains("BIND_ACCESSIBILITY_SERVICE")
            }

            suspicious.forEach { app ->
                if (app.packageName != context.packageName) {
                    val appName = pm.getApplicationLabel(app).toString()
                    alerts.add(ThreatAlert(
                        type = ThreatType.PERMISSION_ABUSE,
                        severity = Severity.HIGH,
                        message = "🚨 '$appName' has overlay + accessibility — potential screen hijack!",
                        action = "Review and disable if not trusted."
                    ))
                }
            }
        } catch (e: Exception) {}
    }

    private fun checkDataLeaks() {
        // Check clipboard for sensitive data patterns
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            val text = clip?.getItemAt(0)?.text?.toString() ?: return

            val sensitivePatterns = listOf(
                Regex("\\b\\d{16}\\b"), // Credit card
                Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"), // SSN
                Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // Email
                Regex("\\b\\d{10,12}\\b") // Phone
            )

            sensitivePatterns.forEach { pattern ->
                if (pattern.containsMatchIn(text)) {
                    alerts.add(ThreatAlert(
                        type = ThreatType.DATA_LEAK,
                        severity = Severity.HIGH,
                        message = "🔒 Sensitive data detected in clipboard! Clear it.",
                        action = "Clipboard has sensitive info that other apps can read."
                    ))
                }
            }
        } catch (e: Exception) {}
    }

    fun addTrustedNetwork(ssid: String) {
        trustedNetworks.add(ssid)
        preferenceDao.set(PreferenceEntity("trusted_networks", trustedNetworks.joinToString(",")))
    }

    fun addSpamNumber(number: String) {
        knownSpamNumbers.add(number)
        preferenceDao.set(PreferenceEntity("spam_numbers", knownSpamNumbers.joinToString(",")))
    }

    fun isSpamCall(number: String): Boolean {
        return number in knownSpamNumbers
    }

    fun getReport(): String {
        val threats = scanForThreats()

        return if (threats.isEmpty()) {
            "🛡️ Security scan complete. No threats detected! ✅"
        } else {
            buildString {
                appendLine("🛡️ Security Report — ${threats.size} issues found:")
                threats.forEach { alert ->
                    val icon = when (alert.severity) {
                        Severity.CRITICAL -> "🔴"
                        Severity.HIGH -> "🟠"
                        Severity.MEDIUM -> "🟡"
                        Severity.LOW -> "🟢"
                    }
                    appendLine("$icon ${alert.message}")
                    if (alert.action.isNotEmpty()) {
                        appendLine("   → ${alert.action}")
                    }
                }
            }
        }
    }
}
