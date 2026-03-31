package com.sonorita.assistant.ai

import com.sonorita.assistant.ai.AIEngine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class RealtimeTranslator(private val aiEngine: AIEngine) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    data class TranslationResult(
        val original: String,
        val translated: String,
        val sourceLanguage: String,
        val targetLanguage: String,
        val speakerName: String? = null
    )

    data class ConversationTranslation(
        val translations: List<TranslationResult>,
        val detectedLanguages: Set<String>
    )

    // Multi-person conversation mode
    private var isConversationMode = false
    private val speakers = mutableMapOf<String, String>() // name -> language preference
    private val conversationHistory = mutableListOf<TranslationResult>()

    fun startConversationMode() {
        isConversationMode = true
        conversationHistory.clear()
    }

    fun stopConversationMode(): String {
        isConversationMode = false
        val summary = buildString {
            appendLine("🌐 Conversation Summary:")
            appendLine("${conversationHistory.size} translations made")
            appendLine("Languages detected: ${conversationHistory.map { it.sourceLanguage }.toSet().joinToString()}")
        }
        conversationHistory.clear()
        return summary
    }

    fun addSpeaker(name: String, language: String) {
        speakers[name] = language
    }

    suspend fun translateForConversation(text: String, speakerName: String? = null): TranslationResult {
        val detectedLang = detectLanguage(text)
        val targetLang = speakers[speakerName] ?: if (detectedLang == "bn") "en" else "bn"

        val translated = translateText(text, detectedLang, targetLang)

        val result = TranslationResult(
            original = text,
            translated = translated,
            sourceLanguage = detectedLang,
            targetLanguage = targetLang,
            speakerName = speakerName
        )

        conversationHistory.add(result)
        return result
    }

    suspend fun multiPersonTranslate(text: String, speakerName: String): Map<String, String> {
        val sourceLang = detectLanguage(text)
        val results = mutableMapOf<String, String>()

        // Translate to each speaker's preferred language
        for ((name, lang) in speakers) {
            if (lang != sourceLang) {
                results[name] = translateText(text, sourceLang, lang)
            }
        }

        return results
    }

    private fun detectLanguage(text: String): String {
        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        return if (hasBengali) "bn" else "en"
    }

    private suspend fun translateText(text: String, from: String, to: String): String {
        return try {
            val response = aiEngine.query(
                "Translate the following text from ${getLanguageName(from)} to ${getLanguageName(to)}. " +
                "Only output the translation, nothing else. Text: \"$text\"",
                emptyList()
            )
            response.content
        } catch (e: Exception) {
            "Translation error: ${e.message}"
        }
    }

    // Voice cloning placeholder - uses AI to mimic user's style
    suspend fun translateInUserStyle(text: String, from: String, to: String, userStyle: String): String {
        return try {
            val response = aiEngine.query(
                "Translate this text from ${getLanguageName(from)} to ${getLanguageName(to)}. " +
                "Write it in this style/tone: \"$userStyle\". " +
                "Only output the translation. Text: \"$text\"",
                emptyList()
            )
            response.content
        } catch (e: Exception) {
            "Translation error: ${e.message}"
        }
    }

    private fun getLanguageName(code: String): String = when (code) {
        "bn" -> "Bengali"
        "en" -> "English"
        "hi" -> "Hindi"
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "zh" -> "Chinese"
        "ar" -> "Arabic"
        else -> code
    }

    fun getConversationSummary(): String {
        return buildString {
            appendLine("🌐 Translation History (${conversationHistory.size} items):")
            conversationHistory.takeLast(10).forEach { t ->
                val speaker = t.speakerName?.let { "[$it] " } ?: ""
                appendLine("${speaker}${t.sourceLanguage}→${t.targetLanguage}: ${t.original.take(30)}... → ${t.translated.take(30)}...")
            }
        }
    }
}
