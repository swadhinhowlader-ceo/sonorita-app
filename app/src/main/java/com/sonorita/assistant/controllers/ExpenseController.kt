package com.sonorita.assistant.controllers

import com.sonorita.assistant.data.ExpenseDao
import com.sonorita.assistant.data.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.*

class ExpenseController(private val expenseDao: ExpenseDao) {

    suspend fun handleExpense(text: String): String {
        return when {
            text.contains("add") || text.contains("khorcha") || text.contains("টাকা") -> {
                addExpense(text)
            }
            text.contains("total") || text.contains("koto") -> {
                getTotal(text)
            }
            text.contains("report") || text.contains("breakdown") -> {
                getReport(text)
            }
            text.contains("budget") -> {
                handleBudget(text)
            }
            else -> "Expense ki korte chao? (add koro / total koto / report dekhao)"
        }
    }

    private suspend fun addExpense(text: String): String {
        // Parse amount: "500 taka", "500 টাকা", "$50"
        val amountPattern = Regex("(\\d+(?:\\.\\d+)?)\\s*(taka|টাকা|৳|\\$|tk)?", RegexOption.IGNORE_CASE)
        val amountMatch = amountPattern.find(text)
        val amount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            return "Amount bolo. Example: '500 taka add koro'"
        }

        // Auto-categorize
        val category = categorizeExpense(text)

        expenseDao.insert(
            ExpenseEntity(
                amount = amount,
                category = category,
                description = text
            )
        )

        return "💰 $amount টাকা $category e add korchi!"
    }

    private suspend fun getTotal(text: String): String {
        val cal = Calendar.getInstance()

        val (from, label) = when {
            text.contains("today") || text.contains("aj") || text.contains("আজ") -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                Pair(cal.timeInMillis, "আজ")
            }
            text.contains("week") || text.contains("shopta") || text.contains("সপ্তাহ") -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                Pair(cal.timeInMillis, "এই সপ্তাহে")
            }
            text.contains("month") || text.contains("mash") || text.contains("মাস") -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                Pair(cal.timeInMillis, "এই মাসে")
            }
            else -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                Pair(cal.timeInMillis, "আজ")
            }
        }

        val total = expenseDao.getTotalSince(from) ?: 0.0
        return "💰 $label total khorcha: $total টাকা"
    }

    private suspend fun getReport(text: String): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)

        val categories = expenseDao.getCategoryTotals(cal.timeInMillis)
        val total = expenseDao.getTotalSince(cal.timeInMillis) ?: 0.0

        return if (categories.isEmpty()) {
            "এই মাসে কোনো expense record নেই।"
        } else {
            buildString {
                appendLine("📊 এই মাসের Expense Report:")
                appendLine()
                categories.forEach { cat ->
                    appendLine("${getCategoryEmoji(cat.category)} ${cat.category}: ${cat.total} টাকা")
                }
                appendLine()
                appendLine("💰 Total: $total টাকা")
            }
        }
    }

    private fun handleBudget(text: String): String {
        return "Budget set korte Settings e giye monthly budget dao."
    }

    private fun categorizeExpense(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("food") || lower.contains("khabar") || lower.contains("খাবার") -> "Food"
            lower.contains("transport") || lower.contains("bus") || lower.contains("rickshaw") || lower.contains("যাতায়াত") -> "Transport"
            lower.contains("mobile") || lower.contains("recharge") || lower.contains("রিচার্জ") -> "Mobile"
            lower.contains("medicine") || lower.contains("doctor") || lower.contains("ঔষধ") -> "Health"
            lower.contains("clothes") || lower.contains("dress") || lower.contains("পোশাক") -> "Clothes"
            lower.contains("electricity") || lower.contains("bill") || lower.contains("বিল") -> "Bills"
            lower.contains("entertainment") || lower.contains("movie") || lower.contains("মজা") -> "Entertainment"
            else -> "Other"
        }
    }

    private fun getCategoryEmoji(category: String): String = when (category) {
        "Food" -> "🍔"
        "Transport" -> "🚗"
        "Mobile" -> "📱"
        "Health" -> "💊"
        "Clothes" -> "👔"
        "Bills" -> "💡"
        "Entertainment" -> "🎬"
        else -> "📦"
    }
}
