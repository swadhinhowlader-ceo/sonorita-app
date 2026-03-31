package com.sonorita.assistant.controllers

import android.content.Context
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.bengali.BengaliTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OCRController(private val context: Context) {

    private val bengaliRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(BengaliTextRecognizerOptions.Builder().build())
    }

    private val englishRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun recognizeText(imageBytes: ByteArray, language: String = "auto"): String {
        // Placeholder - actual implementation requires InputImage from camera/gallery
        return "📸 Image to text conversion ready. Camera/gallery theke image dao."
    }

    fun solveMath(expression: String): String {
        return try {
            // Clean expression
            val cleaned = expression.replace(Regex("[^0-9+\\-*/().]"), "")

            if (cleaned.isEmpty()) {
                return "Math expression pai ni. Screenshot e equation thakle ocr koro."
            }

            // Simple arithmetic evaluation
            val result = evaluateExpression(cleaned)
            "🔢 $expression = $result"
        } catch (e: Exception) {
            "Math solve korte parlam na: ${e.message}"
        }
    }

    private fun evaluateExpression(expr: String): Double {
        // Simple expression evaluator for basic arithmetic
        // Handles: +, -, *, /, parentheses
        var result = 0.0
        var current = 0.0
        var operation = '+'
        var i = 0
        val chars = expr.toCharArray()

        while (i < chars.size) {
            val c = chars[i]

            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < chars.size && (chars[i].isDigit() || chars[i] == '.')) {
                        i++
                    }
                    current = expr.substring(start, i).toDouble()
                    i--
                }
                c == '+' || c == '-' || c == '*' || c == '/' -> {
                    result = when (operation) {
                        '+' -> result + current
                        '-' -> result - current
                        '*' -> result * current
                        '/' -> if (current != 0.0) result / current else 0.0
                        else -> result
                    }
                    operation = c
                    current = 0.0
                }
            }
            i++
        }

        result = when (operation) {
            '+' -> result + current
            '-' -> result - current
            '*' -> result * current
            '/' -> if (current != 0.0) result / current else 0.0
            else -> result
        }

        return result
    }

    fun scanQRCode(): String {
        return "📱 QR code scan korte camera dorkar. Implementation in progress."
    }

    fun scanDocument(): String {
        return "📄 Document scan korte camera dorkar. Multi-page scan available."
    }
}
