package com.sonorita.assistant.controllers

import com.sonorita.assistant.data.HabitDao
import com.sonorita.assistant.data.HabitEntity
import com.sonorita.assistant.data.HabitCompletionEntity
import kotlinx.coroutines.flow.first

class HabitController(private val habitDao: HabitDao) {

    suspend fun handleHabit(text: String): String {
        return when {
            text.contains("add") || text.contains("notun") || text.contains("নতুন") -> {
                addHabit(text)
            }
            text.contains("complete") || text.contains("done") || text.contains("korchi") -> {
                completeHabit(text)
            }
            text.contains("streak") || text.contains("list") || text.contains("dekhao") -> {
                listHabits()
            }
            text.contains("delete") || text.contains("remove") -> {
                deleteHabit(text)
            }
            else -> "Habit ki korte chao? (add koro / complete / dekhao)"
        }
    }

    private suspend fun addHabit(text: String): String {
        val name = text.replace(Regex("(add|habit|koro|notun|নতুন)", RegexOption.IGNORE_CASE), "").trim()

        if (name.isEmpty()) {
            return "Habit naam bolo. Example: 'habit add koro exercise'"
        }

        val id = habitDao.insert(
            HabitEntity(name = name)
        )

        return "🏋️ Habit '$name' add korchi! Daily track korbo."
    }

    private suspend fun completeHabit(text: String): String {
        val habits = habitDao.getActive().first()
        val habitName = text.replace(Regex("(complete|done|korchi|habit)", RegexOption.IGNORE_CASE), "").trim()

        val habit = habits.find { it.name.contains(habitName, ignoreCase = true) }

        return if (habit != null) {
            val newStreak = habit.streak + 1
            val longestStreak = maxOf(newStreak, habit.longestStreak)

            habitDao.update(habit.copy(
                streak = newStreak,
                longestStreak = longestStreak,
                lastCompletedDate = System.currentTimeMillis()
            ))

            habitDao.insertCompletion(
                HabitCompletionEntity(habitId = habit.id)
            )

            val emoji = when {
                newStreak >= 30 -> "🏆"
                newStreak >= 7 -> "⭐"
                newStreak >= 3 -> "🔥"
                else -> "✅"
            }

            "$emoji ${habit.name} complete! Streak: $newStreak days!"
        } else {
            "Habit '$habitName' pai ni. 'habit dekhao' diye check koro."
        }
    }

    private suspend fun listHabits(): String {
        val habits = habitDao.getActive().first()

        return if (habits.isEmpty()) {
            "Kono active habit nei. 'habit add koro [name]' diye add koro."
        } else {
            buildString {
                appendLine("🏋️ Active Habits:")
                habits.forEach { habit ->
                    val streakEmoji = when {
                        habit.streak >= 30 -> "🏆"
                        habit.streak >= 7 -> "⭐"
                        habit.streak >= 3 -> "🔥"
                        else -> "📌"
                    }
                    appendLine("$streakEmoji ${habit.name} — Streak: ${habit.streak} days (Best: ${habit.longestStreak})")
                }
            }
        }
    }

    private suspend fun deleteHabit(text: String): String {
        return "Habit delete korte Room DB direct access dorkar. Eta UI theke korbo."
    }
}
