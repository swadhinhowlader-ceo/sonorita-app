package com.sonorita.assistant.controllers

import android.content.Context
import com.sonorita.assistant.ai.AIEngine
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class AIEmailManager(
    private val context: Context,
    private val aiEngine: AIEngine,
    private val preferenceDao: PreferenceDao
) {

    data class EmailSummary(
        val from: String,
        val subject: String,
        val preview: String,
        val category: EmailCategory,
        val priority: Priority,
        val timestamp: Long
    )

    enum class EmailCategory {
        IMPORTANT, PROMOTIONAL, SOCIAL, UPDATES, SPAM, NEWSLETTER, BILLS, UNKNOWN
    }

    enum class Priority {
        URGENT, HIGH, NORMAL, LOW, JUNK
    }

    private val blockedSenders = mutableSetOf<String>()
    private val importantContacts = mutableSetOf<String>()

    init {
        val saved = preferenceDao.get("blocked_senders")
        if (saved != null) blockedSenders.addAll(saved.split(","))
        val savedImportant = preferenceDao.get("important_contacts")
        if (savedImportant != null) importantContacts.addAll(savedImportant.split(","))
    }

    suspend fun categorizeEmail(from: String, subject: String, body: String): EmailCategory {
        val lowerSubject = subject.lowercase()
        val lowerBody = body.lowercase()

        return when {
            blockedSenders.any { from.contains(it, ignoreCase = true) } -> EmailCategory.SPAM
            importantContacts.any { from.contains(it, ignoreCase = true) } -> EmailCategory.IMPORTANT
            lowerSubject.contains("invoice") || lowerSubject.contains("bill") ||
            lowerSubject.contains("payment") -> EmailCategory.BILLS
            lowerSubject.contains("unsubscribe") || lowerSubject.contains("offer") ||
            lowerSubject.contains("discount") || lowerSubject.contains("sale") -> EmailCategory.PROMOTIONAL
            lowerSubject.contains("newsletter") || lowerSubject.contains("weekly digest") -> EmailCategory.NEWSLETTER
            lowerSubject.contains("notification") || lowerSubject.contains("alert") -> EmailCategory.UPDATES
            lowerSubject.contains("friend") || lowerSubject.contains("liked") ||
            lowerSubject.contains("commented") -> EmailCategory.SOCIAL
            else -> {
                // Use AI for complex categorization
                try {
                    val response = aiEngine.query(
                        "Categorize this email into one of: IMPORTANT, PROMOTIONAL, SOCIAL, UPDATES, SPAM, NEWSLETTER, BILLS. " +
                        "From: $from, Subject: $subject. Only output the category name.",
                        emptyList()
                    )
                    EmailCategory.valueOf(response.content.trim().uppercase())
                } catch (e: Exception) {
                    EmailCategory.UNKNOWN
                }
            }
        }
    }

    suspend fun draftReply(emailSubject: String, emailBody: String, tone: String = "professional"): String {
        return try {
            val response = aiEngine.query(
                "Draft a $tone reply to this email. " +
                "Subject: $emailSubject\nBody: $emailBody\n\n" +
                "Write a concise, appropriate reply. Output only the reply text.",
                emptyList()
            )
            response.content
        } catch (e: Exception) {
            "Reply draft failed: ${e.message}"
        }
    }

    suspend fun summarizeEmails(emails: List<EmailSummary>): String {
        val grouped = emails.groupBy { it.category }

        return buildString {
            appendLine("📧 Email Summary (${emails.size} emails):")
            grouped.forEach { (category, list) ->
                val icon = getCategoryIcon(category)
                appendLine("$icon ${category.name}: ${list.size}")
                list.take(3).forEach { email ->
                    appendLine("   • ${email.from}: ${email.subject.take(40)}")
                }
                if (list.size > 3) appendLine("   ... r ${list.size - 3} ta")
            }
        }
    }

    fun smartSearch(query: String, emails: List<EmailSummary>): List<EmailSummary> {
        return emails.filter { email ->
            email.from.contains(query, ignoreCase = true) ||
            email.subject.contains(query, ignoreCase = true) ||
            email.preview.contains(query, ignoreCase = true)
        }
    }

    fun blockSender(email: String) {
        blockedSenders.add(email)
        preferenceDao.set(PreferenceEntity("blocked_senders", blockedSenders.joinToString(",")))
    }

    fun markImportant(email: String) {
        importantContacts.add(email)
        preferenceDao.set(PreferenceEntity("important_contacts", importantContacts.joinToString(",")))
    }

    fun getPriority(email: EmailSummary): Priority {
        return when {
            email.category == EmailCategory.SPAM -> Priority.JUNK
            email.category == EmailCategory.IMPORTANT -> Priority.URGENT
            email.category == EmailCategory.BILLS -> Priority.HIGH
            email.category == EmailCategory.UPDATES -> Priority.NORMAL
            else -> Priority.LOW
        }
    }

    suspend fun suggestUnsubscribe(emails: List<EmailSummary>): List<String> {
        val newsletters = emails.filter { it.category == EmailCategory.NEWSLETTER || it.category == EmailCategory.PROMOTIONAL }
        val frequent = newsletters.groupBy { it.from }
            .filter { it.value.size >= 3 }
            .keys.toList()

        return if (frequent.isNotEmpty()) {
            buildList {
                add("📧 Unsubscribe suggestions (3+ emails from same sender):")
                frequent.forEach { sender ->
                    add("• $sender (${newsletters.count { it.from == sender }} emails)")
                }
            }
        } else {
            emptyList()
        }
    }

    fun getCategoryIcon(category: EmailCategory): String = when (category) {
        EmailCategory.IMPORTANT -> "🔴"
        EmailCategory.PROMOTIONAL -> "🟡"
        EmailCategory.SOCIAL -> "🔵"
        EmailCategory.UPDATES -> "🟢"
        EmailCategory.SPAM -> "⚫"
        EmailCategory.NEWSLETTER -> "📰"
        EmailCategory.BILLS -> "💰"
        EmailCategory.UNKNOWN -> "❓"
    }

    fun getStats(): String {
        return buildString {
            appendLine("📧 Email Manager Stats:")
            appendLine("Blocked senders: ${blockedSenders.size}")
            appendLine("Important contacts: ${importantContacts.size}")
        }
    }
}
