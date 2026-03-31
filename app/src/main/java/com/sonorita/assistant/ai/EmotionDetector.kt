package com.sonorita.assistant.ai


class EmotionDetector {

    data class EmotionResult(
        val emotion: Emotion,
        val confidence: Float,
        val emoji: String
    )

    enum class Emotion {
        HAPPY, SAD, ANGRY, NEUTRAL, EXCITED, CONFUSED, FRUSTRATED, CALM
    }

    // Simple keyword-based emotion detection
    // Can be enhanced with ML models later
    fun detectEmotion(text: String): EmotionResult {
        val lower = text.lowercase()

        val scores = mutableMapOf<Emotion, Float>()

        // Happy indicators
        val happyWords = listOf("happy", "great", "awesome", "love", "wonderful", "amazing",
            "খুশি", "দারুণ", "চমৎকার", "ভালো", "অসাধারণ", "😊", "😄", "❤️", "🎉")
        scores[Emotion.HAPPY] = happyWords.count { lower.contains(it) }.toFloat()

        // Sad indicators
        val sadWords = listOf("sad", "unhappy", "depressed", "miss", "sorry", "cry",
            "দুঃখ", "কষ্ট", "মন খারাপ", "😢", "😭")
        scores[Emotion.SAD] = sadWords.count { lower.contains(it) }.toFloat()

        // Angry indicators
        val angryWords = listOf("angry", "mad", "furious", "hate", "annoyed",
            "রাগ", "ক্ষেপে", "😤", "😠")
        scores[Emotion.ANGRY] = angryWords.count { lower.contains(it) }.toFloat()

        // Excited indicators
        val excitedWords = listOf("excited", "wow", "omg", "can't wait", "amazing",
            "চমৎকার", "অবিশ্বসনীয়", "🤩", "🔥", "⚡")
        scores[Emotion.EXCITED] = excitedWords.count { lower.contains(it) }.toFloat()

        // Confused indicators
        val confusedWords = listOf("confused", "don't understand", "what", "how", "why",
            "বুঝতে পারছি না", "কীভাবে", "🤔", "❓")
        scores[Emotion.CONFUSED] = confusedWords.count { lower.contains(it) }.toFloat()

        // Frustrated indicators
        val frustratedWords = listOf("frustrated", "ugh", "damn", "not working", "failed",
            "বিরক্ত", "😫", "😤")
        scores[Emotion.FRUSTRATED] = frustratedWords.count { lower.contains(it) }.toFloat()

        // Find dominant emotion
        val dominant = scores.maxByOrNull { it.value }

        return if (dominant != null && dominant.value > 0) {
            EmotionResult(
                emotion = dominant.key,
                confidence = (dominant.value / scores.values.sum()).coerceIn(0f, 1f),
                emoji = getEmoji(dominant.key)
            )
        } else {
            EmotionResult(Emotion.NEUTRAL, 0.5f, "😐")
        }
    }

    private fun getEmoji(emotion: Emotion): String = when (emotion) {
        Emotion.HAPPY -> "😊"
        Emotion.SAD -> "😢"
        Emotion.ANGRY -> "😤"
        Emotion.NEUTRAL -> "😐"
        Emotion.EXCITED -> "🤩"
        Emotion.CONFUSED -> "🤔"
        Emotion.FRUSTRATED -> "😫"
        Emotion.CALM -> "😌"
    }

    fun detectLanguage(text: String): String {
        // Simple heuristic
        val hasBengaliChars = text.any { it in '\u0980'..'\u09FF' }
        return if (hasBengaliChars) "bn" else "en"
    }
}
