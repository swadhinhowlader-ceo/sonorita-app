package com.sonorita.assistant.ai

import android.content.Context

class MemoryPalace(private val context: Context) {

    data class MemoryEntry(
        val id: String,
        val type: MemoryType,
        val content: String,
        val imagePath: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val tags: List<String> = emptyList(),
        val description: String = ""
    )

    enum class MemoryType {
        SCREENSHOT, PHOTO, CONVERSATION, NOTE, SEARCH, LINK, FILE
    }

    private val memories = mutableListOf<MemoryEntry>()

    fun saveScreenshot(imagePath: String, description: String = "", tags: List<String> = emptyList()): MemoryEntry {
        val entry = MemoryEntry(
            id = "mem_${System.currentTimeMillis()}",
            type = MemoryType.SCREENSHOT,
            content = description,
            imagePath = imagePath,
            tags = tags,
            description = description
        )
        memories.add(entry)
        return entry
    }

    fun saveConversation(summary: String, contact: String): MemoryEntry {
        val entry = MemoryEntry(
            id = "mem_${System.currentTimeMillis()}",
            type = MemoryType.CONVERSATION,
            content = summary,
            tags = listOf(contact),
            description = "Conversation with $contact"
        )
        memories.add(entry)
        return entry
    }

    fun saveNote(title: String, content: String): MemoryEntry {
        val entry = MemoryEntry(
            id = "mem_${System.currentTimeMillis()}",
            type = MemoryType.NOTE,
            content = content,
            tags = listOf(title),
            description = title
        )
        memories.add(entry)
        return entry
    }

    fun saveLink(url: String, title: String): MemoryEntry {
        val entry = MemoryEntry(
            id = "mem_${System.currentTimeMillis()}",
            type = MemoryType.LINK,
            content = url,
            tags = listOf(title),
            description = title
        )
        memories.add(entry)
        return entry
    }

    fun search(query: String): String {
        val results = memories.filter { mem ->
            mem.content.contains(query, ignoreCase = true) ||
            mem.description.contains(query, ignoreCase = true) ||
            mem.tags.any { it.contains(query, ignoreCase = true) }
        }.sortedByDescending { it.timestamp }

        return if (results.isEmpty()) {
            "No memories found for '$query'"
        } else {
            buildString {
                appendLine("🧠 Memory Palace — ${results.size} results for '$query':")
                results.take(10).forEach { mem ->
                    val date = java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(mem.timestamp))
                    val typeIcon = getTypeIcon(mem.type)
                    appendLine("$typeIcon $date — ${mem.description.take(60)}")
                    if (mem.content.isNotEmpty()) {
                        appendLine("   ${mem.content.take(80)}")
                    }
                }
                if (results.size > 10) appendLine("... and ${results.size - 10} more")
            }
        }
    }

    fun getRecentScreenshots(days: Int = 7): String {
        val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        val screenshots = memories.filter {
            it.type == MemoryType.SCREENSHOT && it.timestamp > cutoff
        }.sortedByDescending { it.timestamp }

        return if (screenshots.isEmpty()) {
            "Last $days days e kono screenshot nei."
        } else {
            buildString {
                appendLine("📸 Screenshots (last $days days, ${screenshots.size} total):")
                screenshots.take(10).forEach { mem ->
                    val date = java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(mem.timestamp))
                    appendLine("• $date — ${mem.description.take(50)}")
                }
            }
        }
    }

    fun getStats(): String {
        return buildString {
            appendLine("🧠 Memory Palace Stats:")
            appendLine("Total memories: ${memories.size}")
            val grouped = memories.groupBy { it.type }
            grouped.forEach { (type, entries) ->
                appendLine("${getTypeIcon(type)} ${type.name}: ${entries.size}")
            }
            appendLine("Unique tags: ${memories.flatMap { it.tags }.distinct().size}")
        }
    }

    fun getTimeline(): String {
        val recent = memories.sortedByDescending { it.timestamp }.take(20)

        return buildString {
            appendLine("📜 Memory Timeline:")
            recent.forEach { mem ->
                val date = java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(mem.timestamp))
                appendLine("${getTypeIcon(mem.type)} $date — ${mem.description.take(50)}")
            }
        }
    }

    private fun getTypeIcon(type: MemoryType): String = when (type) {
        MemoryType.SCREENSHOT -> "📸"
        MemoryType.PHOTO -> "📷"
        MemoryType.CONVERSATION -> "💬"
        MemoryType.NOTE -> "📝"
        MemoryType.SEARCH -> "🔍"
        MemoryType.LINK -> "🔗"
        MemoryType.FILE -> "📁"
    }
}
