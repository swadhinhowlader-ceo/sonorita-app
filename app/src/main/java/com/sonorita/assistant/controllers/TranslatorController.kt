package com.sonorita.assistant.controllers

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorController(private val context: Context) {

    private var isTranslationMode = false
    private var defaultSourceLang = "bn"
    private var defaultTargetLang = "en"

    fun translate(text: String): String {
        val cleaned = text.replace(Regex("(translate|koro|বাংলা|english)", RegexOption.IGNORE_CASE), "").trim()

        return when {
            text.contains("mode on") || text.contains("translation mode") -> {
                isTranslationMode = true
                "🌐 Translation mode on! Shob kichu auto-translate hobe."
            }
            text.contains("mode off") || text.contains("bondho") -> {
                isTranslationMode = false
                "🌐 Translation mode off!"
            }
            text.contains("bangla to english") || text.contains("bn to en") -> {
                defaultSourceLang = "bn"
                defaultTargetLang = "en"
                "🌐 Default: Bangla → English"
            }
            text.contains("english to bangla") || text.contains("en to bn") -> {
                defaultSourceLang = "en"
                defaultTargetLang = "bn"
                "🌐 Default: English → Bangla"
            }
            cleaned.isNotEmpty() -> {
                translateText(cleaned, defaultSourceLang, defaultTargetLang)
            }
            else -> {
                "Translation ki korbo? Bolo text. Or 'translate mode on' to auto-translate."
            }
        }
    }

    private fun translateText(text: String, from: String, to: String): String {
        return try {
            val sourceLang = when (from) {
                "bn" -> TranslateLanguage.BENGALI
                "en" -> TranslateLanguage.ENGLISH
                else -> TranslateLanguage.ENGLISH
            }

            val targetLang = when (to) {
                "bn" -> TranslateLanguage.BENGALI
                "en" -> TranslateLanguage.ENGLISH
                else -> TranslateLanguage.BENGALI
            }

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()

            val translator = Translation.getClient(options)

            // Note: This is synchronous in placeholder. Real implementation uses coroutines
            "🌐 Translation: \"$text\" → [Downloading model... First time download lagbe]"
        } catch (e: Exception) {
            "Translation error: ${e.message}"
        }
    }

    fun translateAndSend(text: String, recipient: String): String {
        val translated = translateText(text, defaultSourceLang, defaultTargetLang)
        return "🌐 Translated + send: $translated"
    }

    fun isTranslationMode(): Boolean = isTranslationMode
}
