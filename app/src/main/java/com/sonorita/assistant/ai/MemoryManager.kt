package com.sonorita.assistant.ai

import com.sonorita.assistant.data.ConversationDao
import com.sonorita.assistant.data.PreferenceEntity
import com.sonorita.assistant.data.ConversationEntity
import com.sonorita.assistant.data.PreferenceDao
import kotlinx.coroutines.flow.first

class MemoryManager(
    private val conversationDao: ConversationDao,
    private val preferenceDao: PreferenceDao
) {

    suspend fun getConversationHistory(limit: Int = 20): List<String> {
        val conversations = conversationDao.getRecent(limit).first()
        return conversations.reversed().map { entity ->
            when (entity.role) {
                "user" -> "User: ${entity.content}"
                "assistant" -> "Assistant: ${entity.content}"
                else -> entity.content
            }
        }
    }

    suspend fun saveMessage(role: String, content: String, provider: String? = null) {
        conversationDao.insert(
            ConversationEntity(
                role = role,
                content = content,
                provider = provider
            )
        )

        // Keep database manageable
        conversationDao.trim(keep = 2000)
    }

    suspend fun clearHistory() {
        conversationDao.clearAll()
    }

    suspend fun getPreference(key: String, default: String = ""): String {
        return preferenceDao.get(key) ?: default
    }

    suspend fun setPreference(key: String, value: String) {
        preferenceDao.set(PreferenceEntity(key, value))
    }

    suspend fun getUserPreferences(): Map<String, String> {
        return mapOf(
            "voice_gender" to getPreference("voice_gender", "female"),
            "voice_speed" to getPreference("voice_speed", "1.0"),
            "voice_pitch" to getPreference("voice_pitch", "1.0"),
            "language" to getPreference("language", "auto"),
            "wake_word_enabled" to getPreference("wake_word_enabled", "true"),
            "bubble_enabled" to getPreference("bubble_enabled", "true")
        )
    }

    suspend fun learnPreference(key: String, value: String) {
        setPreference("learned_$key", value)
    }

    suspend fun getLearnedPreference(key: String): String? {
        return preferenceDao.get("learned_$key")
    }

    // Track usage patterns
    suspend fun logAppUsage(packageName: String, appName: String, durationMs: Long) {
        // Store in app_usage table via DAO
    }

    // Track conversation context
    suspend fun getFullContext(): String {
        val history = getConversationHistory(30)
        val preferences = getUserPreferences()

        return buildString {
            appendLine("=== Conversation History ===")
            history.forEach { appendLine(it) }
            appendLine()
            appendLine("=== User Preferences ===")
            preferences.forEach { (key, value) -> appendLine("$key: $value") }
        }
    }
}
