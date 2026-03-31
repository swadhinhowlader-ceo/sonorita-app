package com.sonorita.assistant.ai

import com.sonorita.assistant.data.ConversationDao
import com.sonorita.assistant.data.HabitDao
import com.sonorita.assistant.data.ReminderDao
import kotlinx.coroutines.flow.first

class ProactiveSuggestionEngine(
    private val conversationDao: ConversationDao,
    private val reminderDao: ReminderDao,
    private val habitDao: HabitDao
) {

    data class ProactiveSuggestion(
        val message: String,
        val priority: Int, // 1=low, 5=high
        val category: Category
    )

    enum class Category {
        MORNING_GREETING, TASK_REMINDER, HABIT_REMINDER,
        SCHEDULE_CHECK, WEATHER, MOTIVATION, BREAK_SUGGESTION
    }

    suspend fun getSuggestions(): List<ProactiveSuggestion> {
        val suggestions = mutableListOf<ProactiveSuggestion>()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        // Morning greeting (6-9 AM)
        if (hour in 6..9) {
            suggestions.add(ProactiveSuggestion(
                message = "শুভ সকাল! ☀️ আজকের দিনের জন্য কিছু জানতে চাও?",
                priority = 3,
                category = Category.MORNING_GREETING
            ))
        }

        // Check for due reminders
        val dueReminders = reminderDao.getDue()
        if (dueReminders.isNotEmpty()) {
            suggestions.add(ProactiveSuggestion(
                message = "⏰ তোমার ${dueReminders.size}টি reminder due আছে!",
                priority = 5,
                category = Category.TASK_REMINDER
            ))
        }

        // Habit reminders (evening)
        if (hour in 20..22) {
            val activeHabits = habitDao.getActive().first()
            if (activeHabits.isNotEmpty()) {
                suggestions.add(ProactiveSuggestion(
                    message = "🏋️ আজকের habit track করো! ${activeHabits.size}টি active habit আছে।",
                    priority = 4,
                    category = Category.HABIT_REMINDER
                ))
            }
        }

        // Break suggestion (work hours)
        if (hour in 11..16) {
            suggestions.add(ProactiveSuggestion(
                message = "☕ একটু বিরতি নাও! ঘন্টাখানেক হয়েছে কাজ করতে।",
                priority = 2,
                category = Category.BREAK_SUGGESTION
            ))
        }

        return suggestions.sortedByDescending { it.priority }
    }

    suspend fun getProactiveResponseIfNeeded(): String? {
        val suggestions = getSuggestions()
        return suggestions.firstOrNull()?.message
    }
}
