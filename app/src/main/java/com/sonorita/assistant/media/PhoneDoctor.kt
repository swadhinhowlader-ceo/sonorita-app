package com.sonorita.assistant.media

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.app.ActivityManager
import android.os.Build
import android.os.BatteryManager
import java.io.File

class PhoneDoctor(private val context: Context) {

    data class HealthReport(
        val batteryHealth: BatteryHealth,
        val storageHealth: StorageHealth,
        val performanceHealth: PerformanceHealth,
        val suggestions: List<String>,
        val overallScore: Int // 0-100
    )

    data class BatteryHealth(
        val percentage: Int,
        val isCharging: Boolean,
        val temperature: Float,
        val voltage: Int,
        val healthStatus: String,
        val estimatedHoursLeft: Float
    )

    data class StorageHealth(
        val totalGB: Float,
        val usedGB: Float,
        val freeGB: Float,
        val usagePercent: Float,
        val largeFiles: List<FileInfo>
    )

    data class FileInfo(
        val name: String,
        val path: String,
        val sizeMB: Float
    )

    data class PerformanceHealth(
        val ramTotalMB: Long,
        val ramUsedMB: Long,
        val ramFreeMB: Long,
        val cpuCores: Int,
        val androidVersion: String,
        val isLowRam: Boolean
    )

    fun getFullHealthReport(): HealthReport {
        val battery = getBatteryHealth()
        val storage = getStorageHealth()
        val performance = getPerformanceHealth()

        val suggestions = mutableListOf<String>()

        // Battery suggestions
        if (battery.percentage < 20) suggestions.add("🔋 Battery low! Charge koro.")
        if (battery.temperature > 40) suggestions.add("🌡️ Phone gorom! Ektu bondho rakhao.")
        if (battery.percentage < 50 && !battery.isCharging) suggestions.add("💡 Battery 50% er niche. Background apps bondho koro.")

        // Storage suggestions
        if (storage.usagePercent > 90) suggestions.add("💾 Storage almost full! Large files delete koro.")
        if (storage.usagePercent > 80) suggestions.add("💾 Storage 80%+ full. Cleanup koro.")
        if (storage.largeFiles.isNotEmpty()) {
            suggestions.add("📁 ${storage.largeFiles.size} ta boro file ache. Check koro.")
        }

        // Performance suggestions
        if (performance.ramFreeMB < 500) suggestions.add("🧠 RAM low! Background apps clear koro.")
        if (performance.isLowRam) suggestions.add("🧠 Low RAM device. Heavy apps bondho rakhao.")

        // Calculate overall score
        var score = 100
        if (battery.percentage < 20) score -= 20
        if (battery.temperature > 40) score -= 10
        if (storage.usagePercent > 90) score -= 25
        if (storage.usagePercent > 80) score -= 15
        if (performance.ramFreeMB < 500) score -= 15
        score = score.coerceIn(0, 100)

        return HealthReport(battery, storage, performance, suggestions, score)
    }

    private fun getBatteryHealth(): BatteryHealth {
        val batteryStatus = context.registerReceiver(null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (scale > 0) (level * 100) / scale else -1

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

        val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val healthStatus = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        // Estimate hours left (rough: 4000mAh battery, average 300mA drain)
        val estimatedHours = if (!isCharging) percentage / 10f else -1f

        return BatteryHealth(percentage, isCharging, temperature, voltage, healthStatus, estimatedHours)
    }

    private fun getStorageHealth(): StorageHealth {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes

        val totalGB = totalBytes / (1024f * 1024f * 1024f)
        val freeGB = freeBytes / (1024f * 1024f * 1024f)
        val usedGB = usedBytes / (1024f * 1024f * 1024f)
        val usagePercent = (usedGB / totalGB) * 100

        // Find large files
        val largeFiles = findLargeFiles()

        return StorageHealth(totalGB, usedGB, freeGB, usagePercent, largeFiles)
    }

    private fun getPerformanceHealth(): PerformanceHealth {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMB = memoryInfo.totalMem / (1024 * 1024)
        val freeMB = memoryInfo.availMem / (1024 * 1024)
        val usedMB = totalMB - freeMB

        return PerformanceHealth(
            ramTotalMB = totalMB,
            ramUsedMB = usedMB,
            ramFreeMB = freeMB,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            androidVersion = Build.VERSION.RELEASE,
            isLowRam = memoryInfo.lowMemory
        )
    }

    private fun findLargeFiles(maxResults: Int = 5): List<FileInfo> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val largeFiles = mutableListOf<FileInfo>()

        try {
            downloadsDir.walkTopDown()
                .filter { it.isFile && it.length() > 50 * 1024 * 1024 } // > 50MB
                .sortedByDescending { it.length() }
                .take(maxResults)
                .forEach { file ->
                    largeFiles.add(FileInfo(
                        name = file.name,
                        path = file.absolutePath,
                        sizeMB = file.length() / (1024f * 1024f)
                    ))
                }
        } catch (e: Exception) {
            // Permission issue
        }

        return largeFiles
    }

    fun getReportString(): String {
        val report = getFullHealthReport()

        return buildString {
            appendLine("📱 Phone Health Report")
            appendLine("Overall Score: ${report.overallScore}/100 ${getScoreEmoji(report.overallScore)}")
            appendLine()

            appendLine("🔋 Battery:")
            appendLine("  Level: ${report.batteryHealth.percentage}%")
            appendLine("  Status: ${if (report.batteryHealth.isCharging) "⚡ Charging" else "🔌 Not charging"}")
            appendLine("  Temperature: ${report.batteryHealth.temperature}°C")
            appendLine("  Health: ${report.batteryHealth.healthStatus}")
            if (report.batteryHealth.estimatedHoursLeft > 0) {
                appendLine("  Estimated: ${String.format("%.1f", report.batteryHealth.estimatedHoursLeft)}h left")
            }
            appendLine()

            appendLine("💾 Storage:")
            appendLine("  Total: ${String.format("%.1f", report.storageHealth.totalGB)}GB")
            appendLine("  Used: ${String.format("%.1f", report.storageHealth.usedGB)}GB (${String.format("%.0f", report.storageHealth.usagePercent)}%)")
            appendLine("  Free: ${String.format("%.1f", report.storageHealth.freeGB)}GB")
            if (report.storageHealth.largeFiles.isNotEmpty()) {
                appendLine("  Large files:")
                report.storageHealth.largeFiles.forEach { file ->
                    appendLine("    • ${file.name}: ${String.format("%.1f", file.sizeMB)}MB")
                }
            }
            appendLine()

            appendLine("🧠 Performance:")
            appendLine("  RAM: ${report.performanceHealth.ramUsedMB}MB / ${report.performanceHealth.ramTotalMB}MB")
            appendLine("  Free RAM: ${report.performanceHealth.ramFreeMB}MB")
            appendLine("  CPU Cores: ${report.performanceHealth.cpuCores}")
            appendLine("  Android: ${report.performanceHealth.androidVersion}")
            appendLine()

            if (report.suggestions.isNotEmpty()) {
                appendLine("💡 Suggestions:")
                report.suggestions.forEach { appendLine("  $it") }
            }
        }
    }

    private fun getScoreEmoji(score: Int): String = when {
        score >= 90 -> "🟢 Excellent"
        score >= 70 -> "🟡 Good"
        score >= 50 -> "🟠 Fair"
        else -> "🔴 Poor"
    }

    fun getCachedApps(): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = am.runningAppProcesses
            "📱 Running apps: ${runningApps?.size ?: 0}"
        } catch (e: Exception) {
            "Running apps check error: ${e.message}"
        }
    }
}
