package com.sonorita.assistant.controllers

import com.sonorita.assistant.data.NoteDao
import com.sonorita.assistant.data.NoteEntity
import kotlinx.coroutines.flow.first

class NoteController(private val noteDao: NoteDao) {

    suspend fun handleNote(text: String): String {
        return when {
            text.contains("add") || text.contains("koro") || text.contains("লিখো") -> {
                addNote(text)
            }
            text.contains("dekhao") || text.contains("show") || text.contains("list") -> {
                listNotes()
            }
            text.contains("poro") || text.contains("read") -> {
                readNote(text)
            }
            text.contains("delete") || text.contains("remove") -> {
                deleteNote(text)
            }
            text.contains("search") || text.contains("khuj") || text.contains("খুঁজো") -> {
                searchNotes(text)
            }
            else -> "Note ki korte chao? (koro / dekhao / poro / delete)"
        }
    }

    private suspend fun addNote(text: String): String {
        val content = text.replace(Regex("(note|koro|add|লিখো)", RegexOption.IGNORE_CASE), "").trim()

        if (content.isEmpty()) {
            return "Note er content bolo. Example: 'note koro meeting notes'"
        }

        // Auto-generate title from first few words
        val title = content.split(" ").take(5).joinToString(" ")
        val topic = detectTopic(content)

        val id = noteDao.insert(
            NoteEntity(
                title = title,
                content = content,
                topic = topic
            )
        )

        return "📝 Note save korchi! Topic: $topic"
    }

    private suspend fun listNotes(): String {
        val notes = noteDao.getAll().first()

        return if (notes.isEmpty()) {
            "Kono note nei. 'note koro [content]' diye add koro."
        } else {
            buildString {
                appendLine("📝 Saved Notes (${notes.size}):")
                notes.take(10).forEach { note ->
                    val date = java.text.SimpleDateFormat("dd/MM").format(java.util.Date(note.updatedAt))
                    appendLine("• ${note.title} ($date)")
                }
                if (notes.size > 10) {
                    appendLine("... r ${notes.size - 10} ta note ache")
                }
            }
        }
    }

    private suspend fun readNote(text: String): String {
        val query = text.replace(Regex("(poro|read|note)", RegexOption.IGNORE_CASE), "").trim()

        if (query.isEmpty()) {
            return "Kono note porbo? Naam bolo."
        }

        val results = noteDao.search(query)

        return if (results.isNotEmpty()) {
            val note = results.first()
            buildString {
                appendLine("📝 ${note.title}:")
                appendLine(note.content)
                appendLine()
                appendLine("Last updated: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(note.updatedAt))}")
            }
        } else {
            "'$query' naam er kono note pai ni."
        }
    }

    private suspend fun deleteNote(text: String): String {
        return "Note delete korte UI theke korbo."
    }

    private suspend fun searchNotes(text: String): String {
        val query = text.replace(Regex("(search|khuj|note|খুঁজো)", RegexOption.IGNORE_CASE), "").trim()

        if (query.isEmpty()) {
            return "Ki khujbo? Bolo."
        }

        val results = noteDao.search(query)

        return if (results.isEmpty()) {
            "'$query' diye kono note pai ni."
        } else {
            buildString {
                appendLine("🔍 Search results for '$query':")
                results.forEach { note ->
                    appendLine("• ${note.title}")
                }
            }
        }
    }

    private fun detectTopic(content: String): String {
        val lower = content.lowercase()
        return when {
            lower.contains("meeting") || lower.contains("মিটিং") -> "Meeting"
            lower.contains("idea") || lower.contains("আইডিয়া") -> "Ideas"
            lower.contains("todo") || lower.contains("task") || lower.contains("কাজ") -> "Tasks"
            lower.contains("shopping") || lower.contains("কেনাকাটা") -> "Shopping"
            lower.contains("code") || lower.contains("programming") -> "Code"
            lower.contains("diary") || lower.contains("ডায়েরি") -> "Diary"
            else -> "General"
        }
    }
}
