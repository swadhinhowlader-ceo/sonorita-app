package com.sonorita.assistant.learn

import com.sonorita.assistant.ai.AIEngine
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class KnowledgeBrain(
    private val aiEngine: AIEngine,
    private val preferenceDao: PreferenceDao
) {

    data class KnowledgeEntry(
        val id: String,
        val topic: String,
        val content: String,
        val source: String,
        val timestamp: Long = System.currentTimeMillis(),
        val tags: List<String> = emptyList(),
        val importance: Int = 5 // 1-10
    )

    data class Flashcard(
        val question: String,
        val answer: String,
        val topic: String,
        val nextReviewDate: Long,
        val difficulty: Int // 1-5
    )

    // Spaced Repetition System
    data class ReviewSchedule(
        val entryId: String,
        val nextReview: Long,
        val interval: Long, // days
        val easeFactor: Float
    )

    private val knowledgeBase = mutableListOf<KnowledgeEntry>()
    private val flashcards = mutableListOf<Flashcard>()
    private val reviewSchedule = mutableListOf<ReviewSchedule>()

    suspend fun saveKnowledge(topic: String, content: String, source: String = "conversation"): KnowledgeEntry {
        val entry = KnowledgeEntry(
            id = "kb_${System.currentTimeMillis()}",
            topic = topic,
            content = content,
            source = source,
            tags = extractTags(content),
            importance = calculateImportance(content)
        )

        knowledgeBase.add(entry)

        // Auto-generate flashcard
        val flashcard = generateFlashcard(entry)
        if (flashcard != null) {
            flashcards.add(flashcard)
        }

        // Save to persistent storage
        preferenceDao.set(PreferenceEntity("kb_${entry.id}", "${topic}|||${content}"))

        return entry
    }

    suspend fun recallKnowledge(query: String): String {
        // Search knowledge base
        val matches = knowledgeBase.filter { entry ->
            entry.topic.contains(query, ignoreCase = true) ||
            entry.content.contains(query, ignoreCase = true) ||
            entry.tags.any { it.contains(query, ignoreCase = true) }
        }

        return if (matches.isNotEmpty()) {
            buildString {
                appendLine("🧠 Knowledge recall for '$query':")
                matches.sortedByDescending { it.importance }.take(5).forEach { entry ->
                    appendLine()
                    appendLine("📚 ${entry.topic}:")
                    appendLine(entry.content.take(200))
                    if (entry.content.length > 200) appendLine("...")
                }
            }
        } else {
            // Ask AI for the answer
            try {
                val response = aiEngine.query(
                    "Explain briefly: $query. Keep it concise for future recall.",
                    emptyList()
                )
                saveKnowledge(query, response.content, "ai")
                "🧠 New knowledge saved! ${response.content.take(200)}"
            } catch (e: Exception) {
                "Knowledge base e '$query' pai ni, r AI query fail: ${e.message}"
            }
        }
    }

    private suspend fun generateFlashcard(entry: KnowledgeEntry): Flashcard? {
        return try {
            val response = aiEngine.query(
                "Create a question-answer pair from this knowledge. " +
                "Format: Q: [question] A: [answer]. Content: ${entry.content.take(500)}",
                emptyList()
            )

            val parts = response.content.split("A:")
            if (parts.size >= 2) {
                val question = parts[0].replace("Q:", "").trim()
                val answer = parts[1].trim()

                Flashcard(
                    question = question,
                    answer = answer,
                    topic = entry.topic,
                    nextReviewDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 1 day
                    difficulty = 3
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun reviewFlashcards(): String {
        val now = System.currentTimeMillis()
        val dueCards = flashcards.filter { it.nextReviewDate <= now }

        return if (dueCards.isEmpty()) {
            "📚 Shob flashcards review hoyeche! Kal abar ashbe."
        } else {
            buildString {
                appendLine("📚 Flashcard Review (${dueCards.size} cards due):")
                dueCards.take(5).forEachIndexed { i, card ->
                    appendLine()
                    appendLine("${i + 1}. Topic: ${card.topic}")
                    appendLine("Q: ${card.question}")
                    appendLine("(Answer dekhte 'answer [number]' bolo)")
                }
            }
        }
    }

    fun getAnswer(cardIndex: Int): String {
        val dueCards = flashcards.filter { it.nextReviewDate <= System.currentTimeMillis() }
        val card = dueCards.getOrNull(cardIndex - 1)
            ?: return "Card $cardIndex pai ni."

        return "A: ${card.answer}"
    }

    fun getKnowledgeStats(): String {
        return buildString {
            appendLine("🧠 Knowledge Base Stats:")
            appendLine("📚 Total entries: ${knowledgeBase.size}")
            appendLine("🃏 Flashcards: ${flashcards.size}")
            appendLine("📅 Topics: ${knowledgeBase.map { it.topic }.distinct().size}")
            appendLine("⭐ Top topics:")
            knowledgeBase.groupBy { it.topic }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(5)
                .forEach { (topic, count) ->
                    appendLine("  • $topic: $count entries")
                }
        }
    }

    private fun extractTags(content: String): List<String> {
        // Simple tag extraction
        val words = content.split(Regex("\\s+"))
            .filter { it.length > 4 }
            .map { it.lowercase().replace(Regex("[^a-z]"), "") }
            .distinct()
        return words.take(10)
    }

    private fun calculateImportance(content: String): Int {
        return when {
            content.length > 500 -> 8
            content.length > 200 -> 6
            content.contains("important", ignoreCase = true) -> 9
            content.contains("remember", ignoreCase = true) -> 8
            else -> 5
        }
    }
}
