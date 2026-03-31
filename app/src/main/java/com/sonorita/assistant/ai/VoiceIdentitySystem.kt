package com.sonorita.assistant.ai

import android.content.Context
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class VoiceIdentitySystem(
    private val context: Context,
    private val preferenceDao: PreferenceDao
) {

    data class VoiceProfile(
        val id: String,
        val name: String,
        val isOwner: Boolean = false,
        val pitchRange: Pair<Float, Float>, // min, max Hz
        val energyPattern: List<Float>,
        val sampleCount: Int = 0,
        val createdAt: Long = System.currentTimeMillis()
    )

    data class SpeakerDetection(
        val isOwner: Boolean,
        val speakerName: String?,
        val confidence: Float,
        val shouldRespond: Boolean
    )

    private val voiceProfiles = mutableListOf<VoiceProfile>()
    private var ownerProfile: VoiceProfile? = null

    // Group chat mode
    private var groupMode = false
    private var isListeningForOwner = true

    init {
        loadProfiles()
    }

    // Voice enrollment - owner registers their voice
    fun enrollOwnerVoice(name: String, audioFeatures: List<Float>): String {
        val profile = VoiceProfile(
            id = "owner",
            name = name,
            isOwner = true,
            pitchRange = Pair(
                audioFeatures.minOrNull() ?: 100f,
                audioFeatures.maxOrNull() ?: 300f
            ),
            energyPattern = audioFeatures.take(50),
            sampleCount = 1
        )

        ownerProfile = profile
        voiceProfiles.removeAll { it.isOwner }
        voiceProfiles.add(profile)
        saveProfiles()

        return "🎤 Voice enrolled! Ami ekhon '${name}' er awaj chinbo. 🎯"
    }

    // Analyze incoming audio to identify speaker
    fun identifySpeaker(audioFeatures: List<Float>): SpeakerDetection {
        if (ownerProfile == null) {
            // No owner profile yet - respond to everyone
            return SpeakerDetection(
                isOwner = true,
                speakerName = null,
                confidence = 0.5f,
                shouldRespond = true
            )
        }

        // Compare features with owner profile
        val similarity = calculateSimilarity(audioFeatures, ownerProfile!!.energyPattern)
        val isOwner = similarity > 0.7f // 70% confidence threshold

        return SpeakerDetection(
            isOwner = isOwner,
            speakerName = if (isOwner) ownerProfile!!.name else null,
            confidence = similarity,
            shouldRespond = isOwner || !groupMode // In group mode, only respond to owner
        )
    }

    // Simplified feature comparison (real implementation uses MFCC/spectral features)
    private fun calculateSimilarity(features1: List<Float>, features2: List<Float>): Float {
        if (features1.isEmpty() || features2.isEmpty()) return 0f

        val minSize = minOf(features1.size, features2.size)
        val diff = (0 until minSize).sumOf { i ->
            Math.abs(features1[i] - features2[i]).toDouble()
        } / minSize

        return (1.0f - (diff / 100.0).toFloat()).coerceIn(0f, 1f)
    }

    // Group chat mode
    fun enableGroupMode() {
        groupMode = true
        preferenceDao.set(PreferenceEntity("group_mode", "true"))
    }

    fun disableGroupMode() {
        groupMode = false
        preferenceDao.set(PreferenceEntity("group_mode", "false"))
    }

    fun isGroupMode(): Boolean = groupMode

    // Determine if AI should respond
    fun shouldRespondToAudio(audioFeatures: List<Float>, text: String = ""): Boolean {
        val detection = identifySpeaker(audioFeatures)

        // Always respond to owner
        if (detection.isOwner) return true

        // In group mode, check if someone directly addresses the AI
        if (groupMode) {
            val aiKeywords = listOf("sonorita", "hey sonorita", "সোনোরিতা", "ai", "assistant")
            val directlyAddressed = aiKeywords.any { text.lowercase().contains(it) }

            if (directlyAddressed) return true

            // In group mode, also casually join conversations when not directly addressed
            // but with lower priority
            return false
        }

        // Not in group mode - respond to everyone
        return true
    }

    // Group chat casual responses
    suspend fun getGroupChatResponse(
        text: String,
        isOwnerSpeaking: Boolean,
        speakerName: String? = null,
        aiEngine: AIEngine
    ): String? {
        return if (isOwnerSpeaking) {
            // Direct command from owner - full response
            null // Let normal processing handle it
        } else if (groupMode) {
            // Someone else speaking in group - maybe join casually
            val shouldJoin = shouldJoinConversation(text)
            if (shouldJoin) {
                try {
                    val response = aiEngine.query(
                        "You're in a group chat with friends. Someone said: \"$text\"\n" +
                        "Give a brief, casual, friendly comment. Like a friend joining the conversation. " +
                        "Keep it very short (1-2 sentences). Be natural, not robotic. Use emojis.",
                        emptyList()
                    )
                    response.content
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    private fun shouldJoinConversation(text: String): Boolean {
        // Don't join too often - probabilistic
        val random = Math.random()

        // Join more on interesting topics
        val interestingKeywords = listOf(
            "?", "ki", "kivabe", "keno", "what", "how", "why",
            "opinion", "thoughts", "suggest", "bolo", "suggest"
        )

        val isInteresting = interestingKeywords.any { text.lowercase().contains(it) }

        return if (isInteresting) random < 0.4 else random < 0.15
    }

    // Get group chat context for AI
    fun getGroupContext(speakerName: String, message: String): String {
        return "[$speakerName said]: $message"
    }

    fun getVoiceProfiles(): String {
        if (voiceProfiles.isEmpty()) return "No voice profiles enrolled. 'amake cheno' bolo to enroll."

        return buildString {
            appendLine("🎤 Voice Profiles:")
            voiceProfiles.forEach { profile ->
                val role = if (profile.isOwner) "👑 Owner" else "👤 Guest"
                appendLine("$role ${profile.name} (${profile.sampleCount} samples)")
            }
            appendLine()
            appendLine("Group mode: ${if (groupMode) "ON — Only responding to owner" else "OFF — Responding to everyone"}")
        }
    }

    fun isOwnerVoiceRegistered(): Boolean = ownerProfile != null

    private fun loadProfiles() {
        // Load from preferences/database
        val ownerName = preferenceDao.get("owner_voice_name")
        if (ownerName != null) {
            ownerProfile = VoiceProfile(
                id = "owner",
                name = ownerName,
                isOwner = true,
                pitchRange = Pair(80f, 300f),
                energyPattern = emptyList(),
                sampleCount = 1
            )
            voiceProfiles.add(ownerProfile!!)
        }

        groupMode = preferenceDao.get("group_mode") == "true"
    }

    private fun saveProfiles() {
        ownerProfile?.let {
            preferenceDao.set(PreferenceEntity("owner_voice_name", it.name))
        }
    }
}
