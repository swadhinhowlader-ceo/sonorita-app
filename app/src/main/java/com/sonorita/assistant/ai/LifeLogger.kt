package com.sonorita.assistant.ai

import android.content.Context
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class LifeLogger(
    private val context: Context,
    private val preferenceDao: PreferenceDao
) {

    data class LifeLogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val type: LogType,
        val content: String,
        val metadata: Map<String, String> = emptyMap(),
        val tags: List<String> = emptyList()
    )

    enum class LogType {
        SCREENSHOT, CONVERSATION, LOCATION, APP_USAGE,
        CALL, SMS, PHOTO, AUDIO, NOTE, CUSTOM
    }

    private val logEntries = mutableListOf<LifeLogEntry>()

    suspend fun log(type: LogType, content: String, metadata: Map<String, String> = emptyMap(), tags: List<String> = emptyList()) {
        val entry = LifeLogEntry(
            type = type,
            content = content,
            metadata = metadata,
            tags = tags
        )
        logEntries.add(entry)

        // Persist
        val key = "log_${entry.timestamp}"
        val value = "${type.name}|||$content|||${tags.joinToString(",")}"
        preferenceDao.set(PreferenceEntity(key, value))
    }

    fun query(query: String): String {
        val lower = query.lowercase()

        // Parse time filter
        val timeFilter = when {
            lower.contains("today") || lower.contains("aj") -> {
                val today = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                { entry: LifeLogEntry -> entry.timestamp > today }
            }
            lower.contains("yesterday") || lower.contains("goto kal") -> {
                val yesterday = System.currentTimeMillis() - (48 * 60 * 60 * 1000)
                val today = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                { entry: LifeLogEntry -> entry.timestamp in yesterday..today }
            }
            lower.contains("week") || lower.contains("shopta") -> {
                val week = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                { entry: LifeLogEntry -> entry.timestamp > week }
            }
            else -> { _: LifeLogEntry -> true }
        }

        // Parse type filter
        val typeFilter = when {
            lower.contains("screenshot") -> LogType.SCREENSHOT
            lower.contains("call") -> LogType.CALL
            lower.contains("sms") || lower.contains("message") -> LogType.SMS
            lower.contains("location") || lower.contains("gps") -> LogType.LOCATION
            lower.contains("app") -> LogType.APP_USAGE
            lower.contains("photo") -> LogType.PHOTO
            lower.contains("note") -> LogType.NOTE
            else -> null
        }

        // Search
        val results = logEntries.filter { entry ->
            timeFilter(entry) &&
            (typeFilter == null || entry.type == typeFilter) &&
            (entry.content.contains(query, ignoreCase = true) ||
             entry.tags.any { it.contains(query, ignoreCase = true) })
        }.sortedByDescending { it.timestamp }

        return if (results.isEmpty()) {
            "No logs found for '$query'"
        } else {
            buildString {
                appendLine("📜 Life Log — ${results.size} entries found:")
                results.take(10).forEach { entry ->
                    val date = java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(entry.timestamp))
                    val typeIcon = getTypeIcon(entry.type)
                    appendLine("$typeIcon $date — ${entry.content.take(80)}")
                }
                if (results.size > 10) appendLine("... and ${results.size - 10} more")
            }
        }
    }

    fun getLastInteractions(contactName: String): String {
        val interactions = logEntries.filter { entry ->
            entry.content.contains(contactName, ignoreCase = true) ||
            entry.metadata["contact"]?.contains(contactName, ignoreCase = true) == true
        }.sortedByDescending { it.timestamp }

        return if (interactions.isEmpty()) {
            "'$contactName' er sathe kono interaction log e nei."
        } else {
            buildString {
                appendLine("📜 '$contactName' er sathe last interactions:")
                interactions.take(5).forEach { entry ->
                    val date = java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(entry.timestamp))
                    appendLine("• $date — ${entry.type.name}: ${entry.content.take(60)}")
                }
            }
        }
    }

    fun getDailyRecap(date: String = "today"): String {
        val targetDate = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
        val dayStart = java.text.SimpleDateFormat("yyyy-MM-dd").parse(targetDate)?.time ?: 0
        val dayEnd = dayStart + 24 * 60 * 60 * 1000

        val dayEntries = logEntries.filter { it.timestamp in dayStart until dayEnd }

        return if (dayEntries.isEmpty()) {
            "Aj kono log nei."
        } else {
            val grouped = dayEntries.groupBy { it.type }
            buildString {
                appendLine("📜 Daily Recap — $targetDate:")
                grouped.forEach { (type, entries) ->
                    appendLine("${getTypeIcon(type)} ${type.name}: ${entries.size} events")
                }
            }
        }
    }

    fun getStats(): String {
        return buildString {
            appendLine("📊 Life Logger Stats:")
            appendLine("Total entries: ${logEntries.size}")
            val grouped = logEntries.groupBy { it.type }
            grouped.forEach { (type, entries) ->
                appendLine("• ${getTypeIcon(type)} ${type.name}: ${entries.size}")
            }
        }
    }

    private fun getTypeIcon(type: LogType): String = when (type) {
        LogType.SCREENSHOT -> "📸"
        LogType.CONVERSATION -> "💬"
        LogType.LOCATION -> "📍"
        LogType.APP_USAGE -> "📱"
        LogType.CALL -> "📞"
        LogType.SMS -> "✉️"
        LogType.PHOTO -> "📷"
        LogType.AUDIO -> "🎙️"
        LogType.NOTE -> "📝"
        LogType.CUSTOM -> "📌"
    }
}
