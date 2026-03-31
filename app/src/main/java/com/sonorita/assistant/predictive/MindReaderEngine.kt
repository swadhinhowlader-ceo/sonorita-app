package com.sonorita.assistant.predictive

import com.sonorita.assistant.ai.MemoryManager
import com.sonorita.assistant.ai.QueryClassifier
import com.sonorita.assistant.data.ConversationDao
import kotlinx.coroutines.flow.first
import java.util.*

class MindReaderEngine(
    private val conversationDao: ConversationDao,
    private val memoryManager: MemoryManager
) {

    data class Prediction(
        val action: String,
        val confidence: Float,
        val reason: String
    )

    // Track user patterns
    private val hourlyPatterns = mutableMapOf<Int, MutableList<String>>()
    private val dailyPatterns = mutableMapOf<Int, MutableList<String>>() // dayOfWeek -> actions

    suspend fun predictNextAction(): Prediction? {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        // Get historical patterns
        val history = conversationDao.getAll()
        if (history.size < 10) return null

        // Analyze time-based patterns
        val recentActions = history.take(100).map { it.content.lowercase() }

        // Time-based predictions
        val prediction = when {
            hour in 6..8 -> {
                val morningActions = listOf("weather", "news", "schedule", "tasks")
                Prediction(
                    action = "Probably want morning summary? Weather, tasks, schedule?",
                    confidence = 0.7f,
                    reason = "It's morning time, you usually check these"
                )
            }
            hour in 12..13 -> {
                Prediction(
                    action = "Lunch break! Want food suggestions nearby?",
                    confidence = 0.5f,
                    reason = "It's lunch time"
                )
            }
            hour in 17..19 -> {
                Prediction(
                    action = "Evening! Want to log today's expenses or check habits?",
                    confidence = 0.6f,
                    reason = "End of work day pattern"
                )
            }
            hour in 22..23 -> {
                Prediction(
                    action = "Late night! Want to set alarm or review tomorrow's tasks?",
                    confidence = 0.7f,
                    reason = "Bedtime pattern"
                )
            }
            else -> null
        }

        // App-based predictions
        val lastApp = memoryManager.getLearnedPreference("last_app")
        val appPrediction = when (lastApp) {
            "youtube" -> Prediction("Continue watching YouTube?", 0.4f, "You were watching YouTube")
            "whatsapp" -> Prediction("Check WhatsApp messages?", 0.5f, "You were messaging")
            else -> null
        }

        // Return best prediction
        return listOfNotNull(prediction, appPrediction)
            .maxByOrNull { it.confidence }
    }

    suspend fun learnPattern(action: String, context: String = "") {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        hourlyPatterns.getOrPut(hour) { mutableListOf() }.add(action)
        dailyPatterns.getOrPut(dayOfWeek) { mutableListOf() }.add(action)

        // Store in preferences
        memoryManager.learnPreference("pattern_${hour}", action)
        memoryManager.learnPreference("last_action", action)

        if (context.isNotEmpty()) {
            memoryManager.learnPreference("last_context", context)
        }
    }

    suspend fun getProactiveSuggestion(): String? {
        val prediction = predictNextAction() ?: return null
        if (prediction.confidence >= 0.6f) {
            return "🤔 ${prediction.action}"
        }
        return null
    }

    suspend fun analyzeTypingSpeed(text: String, typingTimeMs: Long): String? {
        val charsPerSecond = (text.length.toFloat() / typingTimeMs) * 1000

        return when {
            charsPerSecond > 15 -> null // Normal speed
            charsPerSecond < 5 -> {
                // Very slow — might be tired or thinking hard
                "☕ Ektu tired lagche? Ekta break nao?"
            }
            else -> null
        }
    }
}
