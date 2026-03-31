package com.sonorita.assistant.controllers

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.sonorita.assistant.data.AppUsageDao
import com.sonorita.assistant.data.AppUsageEntity
import java.text.SimpleDateFormat
import java.util.*

class AppUsageController(
    private val context: Context,
    private val appUsageDao: AppUsageDao
) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    suspend fun getUsage(text: String): String {
        if (!hasUsagePermission()) {
            return "📊 App usage check korte permission dorkar. Settings > Usage Access e giye enable koro."
        }

        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val (from, label) = when {
            text.contains("aj") || text.contains("today") -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                Pair(cal.timeInMillis, "আজ")
            }
            text.contains("week") || text.contains("shopta") -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                Pair(cal.timeInMillis, "এই সপ্তাহে")
            }
            text.contains("month") || text.contains("mash") -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                Pair(cal.timeInMillis, "এই মাসে")
            }
            else -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                Pair(cal.timeInMillis, "আজ")
            }
        }

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            from,
            System.currentTimeMillis()
        )

        if (stats.isNullOrEmpty()) {
            return "Kono usage data nei."
        }

        val sorted = stats
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(10)

        val totalMs = sorted.sumOf { it.totalTimeInForeground }

        return buildString {
            appendLine("📊 $label App Usage:")
            sorted.forEach { stat ->
                val hours = stat.totalTimeInForeground / (1000 * 60 * 60)
                val minutes = (stat.totalTimeInForeground % (1000 * 60 * 60)) / (1000 * 60)
                val appName = getAppName(stat.packageName)
                appendLine("• $appName: ${hours}h ${minutes}m")
            }
            appendLine()
            val totalHours = totalMs / (1000 * 60 * 60)
            val totalMinutes = (totalMs % (1000 * 60 * 60)) / (1000 * 60)
            appendLine("⏱️ Total screen time: ${totalHours}h ${totalMinutes}m")
        }
    }

    fun getAppUsageForDate(date: String): String {
        if (!hasUsagePermission()) return "Permission needed."

        val cal = Calendar.getInstance()
        cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: return "Invalid date"
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            cal.timeInMillis + 24 * 60 * 60 * 1000
        )

        return if (stats.isNullOrEmpty()) {
            "No data for $date"
        } else {
            stats.filter { it.totalTimeInForeground > 0 }
                .sortedByDescending { it.totalTimeInForeground }
                .joinToString("\n") {
                    "${getAppName(it.packageName)}: ${it.totalTimeInForeground / 60000} min"
                }
        }
    }

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsagePermission() {
        val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.split(".").lastOrNull() ?: packageName
        }
    }
}
