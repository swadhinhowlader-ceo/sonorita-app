package com.sonorita.assistant.predictive

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.sonorita.assistant.data.PreferenceDao

class MoodDashboard(
    private val context: Context,
    private val preferenceDao: PreferenceDao
) {

    data class MoodState(
        val mood: Mood,
        val confidence: Float,
        val emoji: String,
        val suggestion: String
    )

    enum class Mood {
        VERY_HAPPY, HAPPY, NEUTRAL, SAD, ANGRY, STRESSED, TIRED, EXCITED
    }

    data class MoodEntry(
        val mood: Mood,
        val timestamp: Long = System.currentTimeMillis(),
        val note: String = ""
    )

    private val moodHistory = mutableListOf<MoodEntry>()

    fun analyzeFromText(text: String): MoodState {
        val lower = text.lowercase()

        val scores = mutableMapOf<Mood, Float>()

        // Happy indicators
        val happyWords = listOf("happy", "great", "awesome", "love", "wonderful",
            "খুশি", "দারুণ", "চমৎকার", "ভালো", "😊", "😄", "❤️", "🎉", "haha", "lol")
        scores[Mood.VERY_HAPPY] = happyWords.count { lower.contains(it) }.toFloat() * 1.5f
        scores[Mood.HAPPY] = happyWords.count { lower.contains(it) }.toFloat()

        // Sad indicators
        val sadWords = listOf("sad", "unhappy", "depressed", "miss", "sorry", "cry", "lonely",
            "দুঃখ", "কষ্ট", "মন খারাপ", "😢", "😭", "alone")
        scores[Mood.SAD] = sadWords.count { lower.contains(it) }.toFloat() * 1.5f

        // Angry indicators
        val angryWords = listOf("angry", "mad", "furious", "hate", "annoyed", "frustrated",
            "রাগ", "ক্ষেপে", "😤", "😠", "ugh", "damn")
        scores[Mood.ANGRY] = angryWords.count { lower.contains(it) }.toFloat() * 1.3f

        // Stressed indicators
        val stressWords = listOf("stressed", "overwhelmed", "busy", "deadline", "pressure",
            "চাপ", "ব্যস্ত", "😫", "😩", "tired", "exhausted")
        scores[Mood.STRESSED] = stressWords.count { lower.contains(it) }.toFloat() * 1.2f

        // Excited indicators
        val excitedWords = listOf("excited", "wow", "omg", "can't wait", "amazing",
            "অবিশ্বসনীয়", "🤩", "🔥", "⚡", "yay")
        scores[Mood.EXCITED] = excitedWords.count { lower.contains(it) }.toFloat() * 1.4f

        val dominant = scores.maxByOrNull { it.value }

        val mood = if (dominant != null && dominant.value > 0) dominant.key else Mood.NEUTRAL
        val confidence = (dominant?.value?.coerceIn(0f, 5f) ?: 0f) / 5f

        val entry = MoodEntry(mood)
        moodHistory.add(entry)

        return MoodState(
            mood = mood,
            confidence = confidence.coerceIn(0f, 1f),
            emoji = getEmoji(mood),
            suggestion = getSuggestion(mood)
        )
    }

    fun getToneResponse(text: String, originalResponse: String): String {
        val mood = analyzeFromText(text)

        return when (mood.mood) {
            Mood.SAD -> "I hear you. ${originalResponse.take(100)}... Take your time. 💙"
            Mood.ANGRY -> "I understand your frustration. Let me help. ${originalResponse.take(100)}"
            Mood.STRESSED -> "Take a deep breath. ${originalResponse.take(100)}... You've got this. 💪"
            Mood.EXCITED -> "That's amazing! 🎉 ${originalResponse.take(100)}"
            Mood.VERY_HAPPY -> "Love the energy! 😊 ${originalResponse.take(100)}"
            else -> originalResponse
        }
    }

    fun getDailyMoodReport(): String {
        if (moodHistory.isEmpty()) return "No mood data yet."

        val today = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val todayMoods = moodHistory.filter { it.timestamp > today }

        if (todayMoods.isEmpty()) return "Ajker mood data nei."

        val moodCounts = todayMoods.groupBy { it.mood }.mapValues { it.value.size }
        val dominant = moodCounts.maxByOrNull { it.value }?.key ?: Mood.NEUTRAL

        return buildString {
            appendLine("📊 Ajker Mood Report:")
            appendLine()
            moodCounts.forEach { (mood, count) ->
                appendLine("${getEmoji(mood)} ${mood.name}: $count times")
            }
            appendLine()
            appendLine("Dominant mood: ${getEmoji(dominant)} ${dominant.name}")
            appendLine(getSuggestion(dominant))
        }
    }

    fun getMoodTrend(days: Int = 7): String {
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        val recentMoods = moodHistory.filter { it.timestamp > cutoff }

        if (recentMoods.isEmpty()) return "No mood trend data."

        val avgMood = recentMoods.groupBy { it.mood }
            .maxByOrNull { it.value.size }?.key ?: Mood.NEUTRAL

        return "📈 Last $days days dominant mood: ${getEmoji(avgMood)} ${avgMood.name}"
    }

    private fun getEmoji(mood: Mood): String = when (mood) {
        Mood.VERY_HAPPY -> "😄"
        Mood.HAPPY -> "😊"
        Mood.NEUTRAL -> "😐"
        Mood.SAD -> "😢"
        Mood.ANGRY -> "😤"
        Mood.STRESSED -> "😫"
        Mood.TIRED -> "😴"
        Mood.EXCITED -> "🤩"
    }

    private fun getSuggestion(mood: Mood): String = when (mood) {
        Mood.SAD -> "💙 Kichu kotha bolo. Ami shunchi. Or ekta gaan shono."
        Mood.ANGRY -> "😤 Ektu deep breath nao. R bolo ki hoyeche."
        Mood.STRESSED -> "🧘 5 minute break nao. Or focus mode chalu koro."
        Mood.TIRED -> "😴 Ektu rest nao. Phone bondho koro ektu."
        Mood.EXCITED -> "⚡ Ei energy use koro! Ki korte chao?"
        Mood.HAPPY -> "😊 Bhalo laglo shunte! Ei momentum e kaj koro."
        Mood.VERY_HAPPY -> "🎉 Shobkichu bhalo! Ei din ta enjoy koro!"
        Mood.NEUTRAL -> ""
    }
}
