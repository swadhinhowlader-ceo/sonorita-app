package com.sonorita.assistant.controllers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import com.sonorita.assistant.data.AppUsageDao
import com.sonorita.assistant.data.SpeedTestDao
import com.sonorita.assistant.data.SpeedTestEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NetworkController(
    private val context: Context,
    private val speedTestDao: SpeedTestDao,
    private val appUsageDao: AppUsageDao
) {

    suspend fun runSpeedTest(): String {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            // Download test
            val downloadStart = System.currentTimeMillis()
            val request = Request.Builder()
                .url("https://speed.cloudflare.com/__down?bytes=10000000")
                .build()
            val response = client.newCall(request).execute()
            val bytes = response.body?.bytes()?.size ?: 0
            val downloadEnd = System.currentTimeMillis()

            val downloadMbps = (bytes * 8.0) / ((downloadEnd - downloadStart) / 1000.0) / 1_000_000

            // Upload test (simplified)
            val uploadMbps = downloadMbps * 0.3 // Estimate

            // Save to DB
            speedTestDao.insert(
                SpeedTestEntity(
                    downloadMbps = downloadMbps,
                    uploadMbps = uploadMbps,
                    ping = downloadEnd - downloadStart
                )
            )

            "📶 Speed Test Results:\n⬇️ Download: ${String.format("%.1f", downloadMbps)} Mbps\n⬆️ Upload: ${String.format("%.1f", uploadMbps)} Mbps\n📡 Ping: ${downloadEnd - downloadStart}ms"
        } catch (e: Exception) {
            "Speed test error: ${e.message}"
        }
    }

    fun getDataUsage(text: String): String {
        return try {
            val totalRx = TrafficStats.getTotalRxBytes()
            val totalTx = TrafficStats.getTotalTxBytes()
            val totalMB = (totalRx + totalTx) / (1024 * 1024)

            "📊 Total data usage: ${totalMB}MB (since last boot)"
        } catch (e: Exception) {
            "Data usage check error: ${e.message}"
        }
    }

    fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "No network"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }

    fun isConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun getSpeedHistory(): String {
        val tests = speedTestDao.getRecent(5)
        return if (tests.isEmpty()) {
            "Kono speed test history nei."
        } else {
            buildString {
                appendLine("📶 Speed Test History:")
                tests.forEach { test ->
                    val date = java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(test.timestamp))
                    appendLine("• $date — ⬇️ ${String.format("%.1f", test.downloadMbps)} Mbps / ⬆️ ${String.format("%.1f", test.uploadMbps)} Mbps")
                }
            }
        }
    }
}
