package com.sonorita.assistant.controllers

import android.content.Context
import com.sonorita.assistant.ai.AIEngine
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class AISocialMediaManager(
    private val context: Context,
    private val aiEngine: AIEngine,
    private val preferenceDao: PreferenceDao
) {

    data class PostSchedule(
        val platform: String,
        val content: String,
        val scheduledTime: Long,
        val status: ScheduleStatus = ScheduleStatus.PENDING
    )

    enum class ScheduleStatus {
        PENDING, POSTED, FAILED, CANCELLED
    }

    data class EngagementMetrics(
        val likes: Int,
        val comments: Int,
        val shares: Int,
        val reach: Int,
        val engagementRate: Float
    )

    private val scheduledPosts = mutableListOf<PostSchedule>()
    private val postHistory = mutableListOf<PostSchedule>()

    suspend fun generatePost(topic: String, platform: String, tone: String = "engaging"): String {
        val charLimit = when (platform.lowercase()) {
            "twitter", "x" -> 280
            "instagram" -> 2200
            "facebook" -> 63206
            "linkedin" -> 3000
            else -> 500
        }

        return try {
            val response = aiEngine.query(
                "Write a $tone social media post for $platform about: $topic\n" +
                "Max characters: $charLimit\n" +
                "Include relevant hashtags. Make it catchy and shareable.\n" +
                "Output only the post text, nothing else.",
                emptyList()
            )
            response.content.take(charLimit)
        } catch (e: Exception) {
            "Post generation failed: ${e.message}"
        }
    }

    suspend fun suggestHashtags(content: String, count: Int = 5): List<String> {
        return try {
            val response = aiEngine.query(
                "Suggest $count trending and relevant hashtags for this post: \"$content\"\n" +
                "Output as comma-separated list with # symbol.",
                emptyList()
            )
            response.content.split(",")
                .map { it.trim() }
                .filter { it.startsWith("#") }
                .take(count)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun analyzeBestTimeToPost(platform: String): String {
        return try {
            val response = aiEngine.query(
                "What is the best time to post on $platform for maximum engagement? " +
                "Consider timezone, audience activity, and algorithm factors. " +
                "Be specific with times.",
                emptyList()
            )
            "📊 Best time for $platform:\n${response.content}"
        } catch (e: Exception) {
            "Analysis failed: ${e.message}"
        }
    }

    fun schedulePost(platform: String, content: String, scheduledTime: Long): String {
        val post = PostSchedule(platform, content, scheduledTime)
        scheduledPosts.add(post)
        return "📅 Post scheduled for ${platform} at ${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(scheduledTime))}"
    }

    suspend fun generateThread(topic: String, tweetCount: Int = 5): List<String> {
        return try {
            val response = aiEngine.query(
                "Create a Twitter/X thread of $tweetCount tweets about: $topic\n" +
                "Each tweet max 280 chars. Number them 1/$tweetCount, 2/$tweetCount, etc.\n" +
                "Make it informative and engaging. Separate tweets with ---",
                emptyList()
            )
            response.content.split("---")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(tweetCount)
        } catch (e: Exception) {
            listOf("Thread generation failed: ${e.message}")
        }
    }

    suspend fun generateCaption(imageDescription: String, platform: String): String {
        return try {
            val response = aiEngine.query(
                "Write an engaging caption for a $platform post. " +
                "Image description: $imageDescription\n" +
                "Include emojis and hashtags. Keep it natural.",
                emptyList()
            )
            response.content
        } catch (e: Exception) {
            "Caption generation failed: ${e.message}"
        }
    }

    suspend fun analyzeEngagement(metrics: EngagementMetrics): String {
        return try {
            val response = aiEngine.query(
                "Analyze these social media engagement metrics and suggest improvements:\n" +
                "Likes: ${metrics.likes}\n" +
                "Comments: ${metrics.comments}\n" +
                "Shares: ${metrics.shares}\n" +
                "Reach: ${metrics.reach}\n" +
                "Engagement Rate: ${metrics.engagementRate}%\n\n" +
                "Give specific, actionable advice.",
                emptyList()
            )
            "📊 Engagement Analysis:\n${response.content}"
        } catch (e: Exception) {
            "Analysis failed: ${e.message}"
        }
    }

    suspend fun autoReply(comment: String, postContext: String): String {
        return try {
            val response = aiEngine.query(
                "Write a friendly, engaging reply to this comment: \"$comment\"\n" +
                "Post context: $postContext\n" +
                "Keep it authentic and encourage further engagement.",
                emptyList()
            )
            response.content
        } catch (e: Exception) {
            "Auto-reply failed: ${e.message}"
        }
    }

    fun getScheduledPosts(): String {
        val pending = scheduledPosts.filter { it.status == ScheduleStatus.PENDING }

        return if (pending.isEmpty()) {
            "No scheduled posts."
        } else {
            buildString {
                appendLine("📅 Scheduled Posts (${pending.size}):")
                pending.forEach { post ->
                    val time = java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(post.scheduledTime))
                    appendLine("• ${post.platform} at $time: ${post.content.take(50)}...")
                }
            }
        }
    }

    fun getScheduledCount(): Int = scheduledPosts.count { it.status == ScheduleStatus.PENDING }
}
