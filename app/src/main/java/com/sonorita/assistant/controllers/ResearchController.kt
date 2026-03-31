package com.sonorita.assistant.controllers

import android.content.Context
import android.os.Environment
import com.sonorita.assistant.ai.AIEngine
import com.sonorita.assistant.ai.QueryClassifier
import java.io.File

class ResearchController(private val context: Context, private val aiEngine: AIEngine) {

    suspend fun research(text: String): String {
        val topic = text.replace(Regex("(research|search|koro|বিস্তারিত|khojo)", RegexOption.IGNORE_CASE), "").trim()

        if (topic.isEmpty()) {
            return "Ki research korbo? Bolo topic."
        }

        return try {
            val response = aiEngine.query(
                "Research the following topic thoroughly and provide a comprehensive summary: $topic. " +
                "Include key points, facts, and conclusions. Format it nicely for reading.",
                emptyList()
            )

            // Save as PDF
            val pdfFile = saveToPDF(topic, response.content)

            buildString {
                appendLine("📚 Research complete: $topic")
                appendLine()
                appendLine(response.content.take(500))
                if (response.content.length > 500) {
                    appendLine("...")
                }
                appendLine()
                appendLine("📄 Full report saved: ${pdfFile?.absolutePath ?: "save failed"}")
            }
        } catch (e: Exception) {
            "Research korte parlam na: ${e.message}"
        }
    }

    private fun saveToPDF(title: String, content: String): File? {
        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Sonorita/Research")
            dir.mkdirs()

            val file = File(dir, "research_${System.currentTimeMillis()}.txt")
            file.writeText("Research: $title\n\n$content")
            file
        } catch (e: Exception) {
            null
        }
    }

    fun summarizePDF(filePath: String): String {
        return try {
            val file = File(filePath)
            if (!file.exists()) return "File pai ni: $filePath"

            val text = file.readText()
            // In real implementation, extract PDF text and summarize
            "📄 PDF content (${text.length} chars). Summarize korar jonno AI e pathao."
        } catch (e: Exception) {
            "PDF read korte parlam na: ${e.message}"
        }
    }

    fun summarizeURL(url: String): String {
        return "🌐 URL summarization korte web fetch + AI summarize dorkar. Implementation in progress."
    }
}
