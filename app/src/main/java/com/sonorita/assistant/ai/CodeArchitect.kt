package com.sonorita.assistant.ai

import com.sonorita.assistant.ai.AIEngine

class CodeArchitect(private val aiEngine: AIEngine) {

    data class CodeAnalysis(
        val language: String,
        val summary: String,
        val issues: List<String>,
        val suggestions: List<String>,
        val complexity: Int // 1-10
    )

    suspend fun explainCode(code: String): String {
        return try {
            val response = aiEngine.query(
                "Explain this code in simple terms. Break it down step by step. " +
                "Respond in the same language the user uses. Code:\n```\n$code\n```",
                emptyList()
            )
            "📖 Code Explanation:\n${response.content}"
        } catch (e: Exception) {
            "Code explain error: ${e.message}"
        }
    }

    suspend fun fixCode(code: String, error: String = ""): String {
        return try {
            val prompt = if (error.isNotEmpty()) {
                "Fix this code. Error: $error\nCode:\n```\n$code\n```\nProvide the fixed code with explanation."
            } else {
                "Review this code for bugs and issues. Suggest fixes.\nCode:\n```\n$code\n```"
            }

            val response = aiEngine.query(prompt, emptyList())
            "🔧 Code Fix:\n${response.content}"
        } catch (e: Exception) {
            "Code fix error: ${e.message}"
        }
    }

    suspend fun optimizeCode(code: String): String {
        return try {
            val response = aiEngine.query(
                "Optimize this code for performance and readability. " +
                "Explain each optimization. Code:\n```\n$code\n```",
                emptyList()
            )
            "⚡ Optimized Code:\n${response.content}"
        } catch (e: Exception) {
            "Code optimize error: ${e.message}"
        }
    }

    suspend fun convertCode(code: String, targetLanguage: String): String {
        return try {
            val response = aiEngine.query(
                "Convert this code to $targetLanguage. Preserve all functionality. " +
                "Code:\n```\n$code\n```",
                emptyList()
            )
            "🔄 Converted to $targetLanguage:\n${response.content}"
        } catch (e: Exception) {
            "Code convert error: ${e.message}"
        }
    }

    suspend fun generateCode(description: String, language: String = "Kotlin"): String {
        return try {
            val response = aiEngine.query(
                "Write $language code for: $description. " +
                "Include comments. Make it production-ready.",
                emptyList()
            )
            "💻 Generated $language Code:\n${response.content}"
        } catch (e: Exception) {
            "Code generation error: ${e.message}"
        }
    }

    suspend fun analyzeCode(code: String): CodeAnalysis {
        return try {
            val response = aiEngine.query(
                "Analyze this code. Provide:\n" +
                "1. Language detected\n" +
                "2. Summary\n" +
                "3. Issues/bugs\n" +
                "4. Suggestions\n" +
                "5. Complexity (1-10)\n\nCode:\n```\n$code\n```",
                emptyList()
            )

            CodeAnalysis(
                language = detectLanguage(code),
                summary = response.content.take(200),
                issues = extractList(response.content, "issue"),
                suggestions = extractList(response.content, "suggest"),
                complexity = 5
            )
        } catch (e: Exception) {
            CodeAnalysis("unknown", "Analysis failed", emptyList(), emptyList(), 0)
        }
    }

    suspend fun codeFromScreenshot(imageBase64: String): String {
        return "📸 Screenshot theke code extract korte ScreenAnalyzer use korbo. Vision model e pathbo."
    }

    private fun detectLanguage(code: String): String {
        return when {
            code.contains("fun ") || code.contains("val ") || code.contains("var ") -> "Kotlin"
            code.contains("def ") || code.contains("import ") && code.contains("print") -> "Python"
            code.contains("function ") || code.contains("const ") || code.contains("let ") -> "JavaScript"
            code.contains("public class") || code.contains("System.out") -> "Java"
            code.contains("#include") || code.contains("int main") -> "C/C++"
            code.contains("func ") || code.contains("package main") -> "Go"
            code.contains("fn ") || code.contains("let mut") -> "Rust"
            else -> "Unknown"
        }
    }

    private fun extractList(text: String, keyword: String): List<String> {
        val lines = text.split("\n")
        val result = mutableListOf<String>()
        var capturing = false

        for (line in lines) {
            if (line.lowercase().contains(keyword)) {
                capturing = true
                continue
            }
            if (capturing && line.trim().startsWith("-")) {
                result.add(line.trim().removePrefix("-").trim())
            } else if (capturing && line.trim().isEmpty()) {
                capturing = false
            }
        }

        return result.take(5)
    }
}
