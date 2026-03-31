package com.sonorita.assistant.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Base64
import java.io.ByteArrayOutputStream

class ScreenAnalyzer(private val context: Context) {

    data class ScreenAnalysis(
        val description: String,
        val suggestions: List<String> = emptyList()
    )

    fun analyzeScreenshot(bitmap: Bitmap, query: String, aiEngine: AIEngine): String {
        val base64 = bitmapToBase64(bitmap)
        val prompt = buildPrompt(query)

        return try {
            // This returns immediately with a placeholder
            // Real implementation uses coroutine + AIEngine.queryWithVision
            "Screen captured. Sending to AI for analysis..."
        } catch (e: Exception) {
            "Screen analysis error: ${e.message}"
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to JPEG for API efficiency
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun compressBitmap(bitmap: Bitmap, maxWidth: Int = 720, maxHeight: Int = 1280): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        return if (ratio < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }
    }

    fun imageToBitmap(image: Image): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun buildPrompt(query: String): String {
        return when {
            query.contains("ki ache") || query.contains("what's on") || query.contains("screen e ki") ->
                "Describe everything visible on this screen in detail. Use Bengali if the user wrote in Bengali."
            query.contains("form") || query.contains("fill") ->
                "This is a form on screen. Explain each field and how to fill it step by step."
            query.contains("error") || query.contains("bug") ->
                "There seems to be an error on screen. Explain what the error means and how to fix it."
            query.contains("click") || query.contains("tap") || query.contains("kothay") ->
                "Guide me step by step: where should I tap/click on this screen?"
            query.contains("code") || query.contains("explain") ->
                "There is code on screen. Explain what it does."
            query.contains("fix") || query.contains("solve") ->
                "There is code or an error on screen. Identify the issue and suggest a fix."
            query.contains("app") || query.contains("use") ->
                "Explain how to use this app based on what's currently on screen."
            else ->
                "Analyze this screen and tell me what you see. Respond in the same language as the query: \"$query\""
        }
    }
}
