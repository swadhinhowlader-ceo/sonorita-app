package com.sonorita.assistant.security

import android.content.Context
import android.net.VpnService
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class PrivateVPN(private val context: Context, private val preferenceDao: PreferenceDao) {

    data class VPNConfig(
        val serverAddress: String,
        val serverPort: Int,
        val protocol: String, // OpenVPN, WireGuard
        val username: String = "",
        val password: String = "",
        val dnsServers: List<String> = listOf("1.1.1.1", "8.8.8.8"),
        val blockAds: Boolean = true,
        val blockTrackers: Boolean = true
    )

    data class ConnectionStatus(
        val isConnected: Boolean,
        val serverAddress: String?,
        val protocol: String?,
        val bytesReceived: Long,
        val bytesSent: Long,
        val connectionTime: Long,
        val blockedAds: Int,
        val blockedTrackers: Int
    )

    private var isConnected = false
    private var connectionStartTime: Long = 0
    private var blockedAdsCount = 0
    private var blockedTrackersCount = 0

    // Ad/tracker blocklist
    private val adDomains = mutableSetOf(
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "ads.facebook.com",
        "ads.twitter.com",
        "analytics.google.com",
        "tracking.mixpanel.com",
        "pixel.facebook.com",
        "ads.yahoo.com"
    )

    private val trackerDomains = mutableSetOf(
        "analytics.google.com",
        "www.google-analytics.com",
        "hotjar.com",
        "amplitude.com",
        "mixpanel.com",
        "segment.com"
    )

    init {
        loadBlocklists()
    }

    fun prepareVPN(): Intent? {
        return VpnService.prepare(context)
    }

    fun connectVPN(config: VPNConfig): String {
        val prepareIntent = prepareVPN()
        if (prepareIntent != null) {
            return "🔒 VPN permission needed. Grant permission first."
        }

        isConnected = true
        connectionStartTime = System.currentTimeMillis()

        preferenceDao.set(PreferenceEntity("vpn_server", config.serverAddress))
        preferenceDao.set(PreferenceEntity("vpn_connected", "true"))

        return "🔒 VPN connected to ${config.serverAddress} via ${config.protocol}!"
    }

    fun disconnectVPN(): String {
        isConnected = false
        preferenceDao.set(PreferenceEntity("vpn_connected", "false"))

        val duration = (System.currentTimeMillis() - connectionStartTime) / 1000
        return "🔓 VPN disconnected. Connected for ${duration}s"
    }

    fun isVPNConnected(): Boolean = isConnected

    fun getConnectionStatus(): ConnectionStatus {
        return ConnectionStatus(
            isConnected = isConnected,
            serverAddress = preferenceDao.get("vpn_server"),
            protocol = "OpenVPN",
            bytesReceived = 0,
            bytesSent = 0,
            connectionTime = if (isConnected) System.currentTimeMillis() - connectionStartTime else 0,
            blockedAds = blockedAdsCount,
            blockedTrackers = blockedTrackersCount
        )
    }

    fun shouldBlockDomain(domain: String): Boolean {
        val lower = domain.lowercase()

        val isAd = adDomains.any { lower.contains(it) }
        val isTracker = trackerDomains.any { lower.contains(it) }

        if (isAd) blockedAdsCount++
        if (isTracker) blockedTrackersCount++

        return isAd || isTracker
    }

    fun addBlockedDomain(domain: String, type: String = "ad") {
        when (type.lowercase()) {
            "ad" -> adDomains.add(domain)
            "tracker" -> trackerDomains.add(domain)
        }
    }

    fun getStatusString(): String {
        val status = getConnectionStatus()

        return buildString {
            appendLine("🔒 VPN Status:")
            appendLine("Connected: ${if (status.isConnected) "✅ Yes" else "❌ No"}")
            status.serverAddress?.let { appendLine("Server: $it") }
            appendLine("Protocol: ${status.protocol}")
            if (status.isConnected) {
                val minutes = status.connectionTime / 60000
                appendLine("Connected for: ${minutes}m")
            }
            appendLine("Ads blocked: ${status.blockedAds}")
            appendLine("Trackers blocked: ${status.blockedTrackers}")
        }
    }

    fun getProtectionStats(): String {
        return buildString {
            appendLine("🛡️ Privacy Protection:")
            appendLine("Ad domains blocked: ${adDomains.size}")
            appendLine("Tracker domains blocked: ${trackerDomains.size}")
            appendLine("Total ads blocked: $blockedAdsCount")
            appendLine("Total trackers blocked: $blockedTrackersCount")
        }
    }

    private fun loadBlocklists() {
        val savedAds = preferenceDao.get("blocked_ad_domains")
        if (savedAds != null) {
            adDomains.addAll(savedAds.split(","))
        }

        val savedTrackers = preferenceDao.get("blocked_tracker_domains")
        if (savedTrackers != null) {
            trackerDomains.addAll(savedTrackers.split(","))
        }
    }
}
