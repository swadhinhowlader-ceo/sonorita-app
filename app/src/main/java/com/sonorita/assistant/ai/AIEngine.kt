package com.sonorita.assistant.ai

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AIEngine(private val context: Context, private val preferenceDao: PreferenceDao) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Provider fallback order: Gemini → OpenRouter → Groq → OpenAI
    private val providers = listOf("gemini", "openrouter", "groq", "openai")

    data class AIResponse(
        val content: String,
        val provider: String,
        val model: String? = null
    )

    data class ProviderConfig(
        val name: String,
        val apiKeys: List<String>,
        val baseUrl: String,
        val models: List<String>
    )

    suspend fun query(prompt: String, conversationHistory: List<String> = emptyList()): AIResponse {
        val queryType = QueryClassifier.classify(prompt).type
        val providerConfig = getProviderConfig()

        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null

            for (provider in providers) {
                val config = providerConfig[provider] ?: continue
                if (config.apiKeys.isEmpty()) continue

                for (apiKey in config.apiKeys) {
                    try {
                        val response = callProvider(provider, config, apiKey, prompt, queryType, conversationHistory)
                        return@withContext AIResponse(response, provider)
                    } catch (e: Exception) {
                        lastException = e
                        continue // Try next key
                    }
                }
            }

            throw lastException ?: Exception("No AI provider available")
        }
    }

    private suspend fun callProvider(
        provider: String,
        config: ProviderConfig,
        apiKey: String,
        prompt: String,
        queryType: QueryClassifier.QueryType,
        history: List<String>
    ): String {
        val systemPrompt = preferenceDao.get("system_prompt")
            ?: "You are Sonorita, a helpful personal AI assistant. Respond in the same language the user uses. Be concise and helpful."

        val model = selectModel(provider, queryType, config.models)

        return when (provider) {
            "gemini" -> callGemini(apiKey, model, systemPrompt, prompt, history)
            "openrouter" -> callOpenRouter(apiKey, model, systemPrompt, prompt, history)
            "groq" -> callGroq(apiKey, model, systemPrompt, prompt, history)
            "openai" -> callOpenAI(apiKey, model, systemPrompt, prompt, history)
            else -> throw Exception("Unknown provider: $provider")
        }
    }

    private fun selectModel(provider: String, queryType: QueryClassifier.QueryType, models: List<String>): String {
        val defaultModel = models.firstOrNull() ?: getDefaultModel(provider)

        return when (queryType) {
            QueryClassifier.QueryType.SIMPLE -> getLightModel(provider)
            QueryClassifier.QueryType.MEDIUM -> defaultModel
            QueryClassifier.QueryType.RESEARCH -> getSmartModel(provider)
            QueryClassifier.QueryType.VISION -> getVisionModel(provider)
            QueryClassifier.QueryType.DEVICE_COMMAND -> "" // Not used for device commands
        }
    }

    private fun getDefaultModel(provider: String): String = when (provider) {
        "gemini" -> "gemini-1.5-flash"
        "openrouter" -> "anthropic/claude-3.5-sonnet"
        "groq" -> "llama3-70b-8192"
        "openai" -> "gpt-4o-mini"
        else -> ""
    }

    private fun getLightModel(provider: String): String = when (provider) {
        "gemini" -> "gemini-1.5-flash"
        "openrouter" -> "meta-llama/llama-3.1-8b-instruct"
        "groq" -> "llama3-8b-8192"
        "openai" -> "gpt-4o-mini"
        else -> ""
    }

    private fun getSmartModel(provider: String): String = when (provider) {
        "gemini" -> "gemini-1.5-pro"
        "openrouter" -> "anthropic/claude-3.5-sonnet"
        "groq" -> "llama3-70b-8192"
        "openai" -> "gpt-4o"
        else -> ""
    }

    private fun getVisionModel(provider: String): String = when (provider) {
        "gemini" -> "gemini-1.5-flash"
        "openrouter" -> "anthropic/claude-3.5-sonnet"
        "groq" -> "llama3-70b-8192"
        "openai" -> "gpt-4o"
        else -> ""
    }

    private fun callGemini(apiKey: String, model: String, systemPrompt: String, prompt: String, history: List<String>): String {
        val contents = mutableListOf<Map<String, Any>>()

        // Add conversation history
        for (msg in history) {
            val role = if (msg.startsWith("User:")) "user" else "model"
            val text = msg.removePrefix("User: ").removePrefix("Assistant: ")
            contents.add(mapOf("role" to role, "parts" to listOf(mapOf("text" to text))))
        }

        // Add current prompt
        contents.add(mapOf("role" to "user", "parts" to listOf(mapOf("text" to prompt))))

        val body = gson.toJson(mapOf(
            "system_instruction" to mapOf("parts" to listOf(mapOf("text" to systemPrompt))),
            "contents" to contents,
            "generationConfig" to mapOf("temperature" to 0.7, "maxOutputTokens" to 4096)
        ))

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from Gemini")

        if (!response.isSuccessful) throw Exception("Gemini error: ${response.code} - $responseBody")

        val json = gson.fromJson(responseBody, Map::class.java)
        val candidates = json["candidates"] as? List<Map<String, Any>>
        val content = candidates?.firstOrNull()?.get("content") as? Map<String, Any>
        val parts = content?.get("parts") as? List<Map<String, Any>>
        val text = parts?.firstOrNull()?.get("text") as? String

        return text ?: throw Exception("No text in Gemini response")
    }

    private fun callOpenRouter(apiKey: String, model: String, systemPrompt: String, prompt: String, history: List<String>): String {
        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to systemPrompt))

        for (msg in history) {
            val role = if (msg.startsWith("User:")) "user" else "assistant"
            val text = msg.removePrefix("User: ").removePrefix("Assistant: ")
            messages.add(mapOf("role" to role, "content" to text))
        }
        messages.add(mapOf("role" to "user", "content" to prompt))

        val body = gson.toJson(mapOf("model" to model, "messages" to messages, "max_tokens" to 4096))

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) throw Exception("OpenRouter error: ${response.code}")

        val json = gson.fromJson(responseBody, Map::class.java)
        val choices = json["choices"] as? List<Map<String, Any>>
        val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
        return message?.get("content") as? String ?: throw Exception("No content in response")
    }

    private fun callGroq(apiKey: String, model: String, systemPrompt: String, prompt: String, history: List<String>): String {
        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to systemPrompt))

        for (msg in history) {
            val role = if (msg.startsWith("User:")) "user" else "assistant"
            val text = msg.removePrefix("User: ").removePrefix("Assistant: ")
            messages.add(mapOf("role" to role, "content" to text))
        }
        messages.add(mapOf("role" to "user", "content" to prompt))

        val body = gson.toJson(mapOf("model" to model, "messages" to messages, "max_tokens" to 4096))

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) throw Exception("Groq error: ${response.code}")

        val json = gson.fromJson(responseBody, Map::class.java)
        val choices = json["choices"] as? List<Map<String, Any>>
        val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
        return message?.get("content") as? String ?: throw Exception("No content in response")
    }

    private fun callOpenAI(apiKey: String, model: String, systemPrompt: String, prompt: String, history: List<String>): String {
        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to systemPrompt))

        for (msg in history) {
            val role = if (msg.startsWith("User:")) "user" else "assistant"
            val text = msg.removePrefix("User: ").removePrefix("Assistant: ")
            messages.add(mapOf("role" to role, "content" to text))
        }
        messages.add(mapOf("role" to "user", "content" to prompt))

        val body = gson.toJson(mapOf("model" to model, "messages" to messages, "max_tokens" to 4096))

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) throw Exception("OpenAI error: ${response.code}")

        val json = gson.fromJson(responseBody, Map::class.java)
        val choices = json["choices"] as? List<Map<String, Any>>
        val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
        return message?.get("content") as? String ?: throw Exception("No content in response")
    }

    private suspend fun getProviderConfig(): Map<String, ProviderConfig> {
        val configs = mutableMapOf<String, ProviderConfig>()

        for (provider in providers) {
            val keysJson = preferenceDao.get("${provider}_api_keys")
            val keys = if (keysJson != null) {
                try {
                    gson.fromJson(keysJson, Array<String>::class.java).toList()
                } catch (e: Exception) {
                    listOf(keysJson)
                }
            } else {
                emptyList()
            }

            val baseUrl = when (provider) {
                "gemini" -> "https://generativelanguage.googleapis.com"
                "openrouter" -> "https://openrouter.ai/api/v1"
                "groq" -> "https://api.groq.com/openai/v1"
                "openai" -> "https://api.openai.com/v1"
                else -> ""
            }

            configs[provider] = ProviderConfig(
                name = provider,
                apiKeys = keys,
                baseUrl = baseUrl,
                models = emptyList()
            )
        }

        return configs
    }

    suspend fun queryWithVision(prompt: String, imageBase64: String): AIResponse {
        val visionProvider = providers.firstOrNull { p ->
            val config = getProviderConfig()[p]
            config != null && config.apiKeys.isNotEmpty()
        } ?: throw Exception("No provider available for vision")

        val config = getProviderConfig()[visionProvider]!!
        val apiKey = config.apiKeys.first()
        val model = getVisionModel(visionProvider)

        return withContext(Dispatchers.IO) {
            val systemPrompt = preferenceDao.get("system_prompt")
                ?: "You are Sonorita, a helpful personal AI assistant. Analyze the image and respond helpfully."

            val body = when (visionProvider) {
                "gemini" -> {
                    val contents = listOf(
                        mapOf(
                            "role" to "user",
                            "parts" to listOf(
                                mapOf("text" to prompt),
                                mapOf("inlineData" to mapOf("mimeType" to "image/jpeg", "data" to imageBase64))
                            )
                        )
                    )
                    gson.toJson(mapOf(
                        "system_instruction" to mapOf("parts" to listOf(mapOf("text" to systemPrompt))),
                        "contents" to contents,
                        "generationConfig" to mapOf("temperature" to 0.7, "maxOutputTokens" to 4096)
                    ))
                }
                else -> {
                    val messages = listOf(
                        mapOf("role" to "system", "content" to systemPrompt),
                        mapOf("role" to "user", "content" to listOf(
                            mapOf("type" to "text", "text" to prompt),
                            mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$imageBase64"))
                        ))
                    )
                    gson.toJson(mapOf("model" to model, "messages" to messages, "max_tokens" to 4096))
                }
            }

            val url = when (visionProvider) {
                "gemini" -> "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
                "groq" -> "https://api.groq.com/openai/v1/chat/completions"
                "openai" -> "https://api.openai.com/v1/chat/completions"
                else -> throw Exception("Unknown vision provider")
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))

            if (visionProvider != "gemini") {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) throw Exception("$visionProvider vision error: ${response.code}")

            val resultText = when (visionProvider) {
                "gemini" -> {
                    val json = gson.fromJson(responseBody, Map::class.java)
                    val candidates = json["candidates"] as? List<Map<String, Any>>
                    val content = candidates?.firstOrNull()?.get("content") as? Map<String, Any>
                    val parts = content?.get("parts") as? List<Map<String, Any>>
                    parts?.firstOrNull()?.get("text") as? String
                }
                else -> {
                    val json = gson.fromJson(responseBody, Map::class.java)
                    val choices = json["choices"] as? List<Map<String, Any>>
                    val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
                    message?.get("content") as? String
                }
            }

            AIResponse(resultText ?: throw Exception("No text in vision response"), visionProvider, model)
        }
    }
}
