package com.sonorita.assistant.ai

class DigitalTwin(private val aiEngine: AIEngine, private val memoryManager: MemoryManager) {

    data class UserProfile(
        val name: String = "",
        val communicationStyle: CommunicationStyle = CommunicationStyle.CASUAL,
        val vocabulary: List<String> = emptyList(),
        val commonPhrases: List<String> = emptyList(),
        val responsePatterns: Map<String, String> = emptyMap(),
        val tonePreference: String = "friendly"
    )

    enum class CommunicationStyle {
        FORMAL, CASUAL, FRIENDLY, PROFESSIONAL, HUMOROUS
    }

    private var profile = UserProfile()
    private val messageHistory = mutableListOf<Pair<String, String>>() // user msg, their reply style

    suspend fun trainFromMessages(messages: List<String>) {
        // Analyze user's writing style
        val avgLength = messages.map { it.length }.average().toInt()
        val commonWords = messages.flatMap { it.split(" ") }
            .groupingBy { it.lowercase() }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(20)
            .map { it.first }

        // Detect style
        val style = detectStyle(messages)

        // Save learned style
        memoryManager.learnPreference("twin_style", style.name)
        memoryManager.learnPreference("twin_words", commonWords.joinToString(","))

        profile = profile.copy(
            communicationStyle = style,
            vocabulary = commonWords
        )
    }

    suspend fun generateReply(message: String, context: String = ""): String {
        val style = profile.communicationStyle
        val stylePrompt = when (style) {
            CommunicationStyle.FORMAL -> "Respond formally and professionally."
            CommunicationStyle.CASUAL -> "Respond casually, like texting a friend."
            CommunicationStyle.FRIENDLY -> "Respond warmly and friendly."
            CommunicationStyle.PROFESSIONAL -> "Respond professionally but approachable."
            CommunicationStyle.HUMOROUS -> "Respond with humor and wit."
        }

        val vocabularyHint = if (profile.vocabulary.isNotEmpty()) {
            "Use these words/phrases when natural: ${profile.vocabulary.take(10).joinToString()}"
        } else ""

        return try {
            val response = aiEngine.query(
                "You are responding as if you are me. $stylePrompt $vocabularyHint\n\n" +
                "Context: $context\n" +
                "Someone sent me this message: \"$message\"\n\n" +
                "Respond AS ME, in my style. Keep it authentic. Don't say 'as an AI'.",
                emptyList()
            )
            response.content
        } catch (e: Exception) {
            "Reply generation failed: ${e.message}"
        }
    }

    suspend fun handleWhatsAppAutopilot(contactName: String, message: String): String {
        val context = "Contact: $contactName (WhatsApp)"
        return generateReply(message, context)
    }

    suspend fun handleTelegramAutopilot(contactName: String, message: String): String {
        val context = "Contact: $contactName (Telegram)"
        return generateReply(message, context)
    }

    suspend fun handleEmailAutopilot(sender: String, subject: String, body: String): String {
        val context = "Email from: $sender, Subject: $subject"
        return generateReply(body, context)
    }

    private fun detectStyle(messages: List<String>): CommunicationStyle {
        val formalIndicators = listOf("please", "kindly", "regarding", "furthermore", "sincerely")
        val casualIndicators = listOf("lol", "haha", "btw", "gonna", "wanna", "tbh", "ngl")
        val humorIndicators = listOf("haha", "😂", "🤣", "lmao", "rofl", "jk")

        val lowerMessages = messages.map { it.lowercase() }

        val formalScore = lowerMessages.sumOf { msg -> formalIndicators.count { msg.contains(it) } }
        val casualScore = lowerMessages.sumOf { msg -> casualIndicators.count { msg.contains(it) } }
        val humorScore = lowerMessages.sumOf { msg -> humorIndicators.count { msg.contains(it) } }

        return when {
            humorScore > casualScore && humorScore > formalScore -> CommunicationStyle.HUMOROUS
            casualScore > formalScore -> CommunicationStyle.CASUAL
            formalScore > casualScore -> CommunicationStyle.FORMAL
            else -> CommunicationStyle.FRIENDLY
        }
    }

    fun getTwinStatus(): String {
        return buildString {
            appendLine("🤖 Digital Twin Status:")
            appendLine("Style: ${profile.communicationStyle}")
            appendLine("Vocabulary: ${profile.vocabulary.size} learned words")
            appendLine("Messages analyzed: ${messageHistory.size}")
        }
    }
}
