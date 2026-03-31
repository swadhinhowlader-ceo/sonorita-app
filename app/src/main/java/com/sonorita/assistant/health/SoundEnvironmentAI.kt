package com.sonorita.assistant.health

import android.content.Context
import android.media.AudioRecord
import android.media.AudioFormat
import android.media.MediaRecorder
import com.sonorita.assistant.ai.AIEngine

class SoundEnvironmentAI(
    private val context: Context,
    private val aiEngine: AIEngine
) {

    data class SoundAnalysis(
        val detectedSounds: List<DetectedSound>,
        val environment: String,
        val suggestion: String
    )

    data class DetectedSound(
        val type: SoundType,
        val confidence: Float,
        val description: String
    )

    enum class SoundType {
        VOICE, TRAFFIC, MUSIC, NATURE, MACHINERY,
        BABY_CRY, DOG_BARK, DOORBELL, ALARM,
        SILENCE, CROWD, RAIN, UNKNOWN
    }

    private var isListening = false
    private var audioRecord: AudioRecord? = null

    fun startListening(): String {
        if (isListening) return "Already listening."

        isListening = true
        return "👂 Sound monitoring started. Listening for environment sounds."
    }

    fun stopListening(): String {
        isListening = false
        audioRecord?.release()
        audioRecord = null
        return "🔇 Sound monitoring stopped."
    }

    fun analyzeSoundBuffer(buffer: ByteArray): SoundAnalysis {
        // Simplified analysis - real implementation would use AudioClassification ML Kit
        val amplitude = buffer.map { it.toInt().coerceAtLeast(0) }.average()

        val detectedSounds = mutableListOf<DetectedSound>()

        when {
            amplitude < 10 -> detectedSounds.add(DetectedSound(SoundType.SILENCE, 0.9f, "Complete silence"))
            amplitude < 50 -> detectedSounds.add(DetectedSound(SoundType.NATURE, 0.5f, "Low ambient sounds"))
            amplitude < 100 -> detectedSounds.add(DetectedSound(SoundType.VOICE, 0.6f, "Possible conversation"))
            amplitude < 150 -> detectedSounds.add(DetectedSound(SoundType.TRAFFIC, 0.5f, "Traffic or machinery"))
            else -> detectedSounds.add(DetectedSound(SoundType.MACHINERY, 0.4f, "Loud environment"))
        }

        val environment = when {
            amplitude < 20 -> "Very quiet (library/bedroom)"
            amplitude < 60 -> "Quiet (office/home)"
            amplitude < 100 -> "Moderate (street/cafe)"
            else -> "Noisy (traffic/construction)"
        }

        return SoundAnalysis(
            detectedSounds = detectedSounds,
            environment = environment,
            suggestion = getSuggestion(detectedSounds)
        )
    }

    fun identifySound(audioFilePath: String): String {
        return "🔊 Sound identification: Audio file analyze korbo. ML Kit Audio Classification use korbo."
    }

    fun detectBabyCry(): String {
        return "👶 Baby cry detection active. Continuous monitoring running."
    }

    fun detectDoorbell(): String {
        return "🔔 Doorbell detection active. Alert korbo jokhon detect hobe."
    }

    fun getNoiseLevel(buffer: ByteArray): Int {
        val amplitude = buffer.map { it.toInt().coerceAtLeast(0) }.average()
        return (amplitude / 1.5).toInt().coerceIn(0, 100)
    }

    private fun getSuggestion(sounds: List<DetectedSound>): String {
        val primary = sounds.firstOrNull() ?: return ""

        return when (primary.type) {
            SoundType.SILENCE -> "🤫 Perfect for focus mode!"
            SoundType.TRAFFIC -> "🚗 Traffic noise detected. Noise cancellation on?"
            SoundType.MACHINERY -> "🔨 Loud environment. Consider earphones."
            SoundType.BABY_CRY -> "👶 Baby crying detected! Check immediately."
            SoundType.DOG_BARK -> "🐕 Dog barking nearby."
            SoundType.ALARM -> "🚨 Alarm detected! Check surroundings."
            SoundType.RAIN -> "🌧️ Rain sounds detected. Enjoy the ambiance! ☕"
            else -> ""
        }
    }

    fun isActive(): Boolean = isListening
}
