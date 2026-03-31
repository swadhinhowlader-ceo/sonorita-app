package com.sonorita.assistant.controllers

import com.sonorita.assistant.data.TodoDao
import com.sonorita.assistant.data.TodoEntity
import kotlinx.coroutines.flow.first

class TodoController(private val todoDao: TodoDao) {

    suspend fun handleTodo(text: String): String {
        return when {
            text.contains("add") || text.contains("notun") || text.contains("নতুন") -> {
                addTodo(text)
            }
            text.contains("complete") || text.contains("done") || text.contains("korechi") -> {
                completeTodo(text)
            }
            text.contains("delete") || text.contains("remove") || text.contains("bondho") -> {
                deleteTodo(text)
            }
            text.contains("dekhao") || text.contains("show") || text.contains("list") -> {
                listTodos()
            }
            text.contains("aj") || text.contains("today") -> {
                getTodayTodos()
            }
            else -> "Todo ki korte chao? (add koro / complete / dekhao)"
        }
    }

    private suspend fun addTodo(text: String): String {
        val title = text.replace(Regex("(add|task|todo|koro|notun|নতুন)", RegexOption.IGNORE_CASE), "").trim()

        if (title.isEmpty()) {
            return "Task naam bolo. Example: 'task add koro email check'"
        }

        val id = todoDao.insert(
            TodoEntity(title = title)
        )

        return "📝 Task '$title' add korchi!"
    }

    private suspend fun completeTodo(text: String): String {
        val pending = todoDao.getPending().first()
        val taskTitle = text.replace(Regex("(complete|done|korechi|task|todo)", RegexOption.IGNORE_CASE), "").trim()

        val task = pending.find { it.title.contains(taskTitle, ignoreCase = true) }

        return if (task != null) {
            todoDao.complete(task.id)
            "✅ '${task.title}' complete korchi!"
        } else {
            "Task '$taskTitle' pai ni. 'tasks dekhao' diye check koro."
        }
    }

    private suspend fun deleteTodo(text: String): String {
        val all = todoDao.getAll().first()
        val taskTitle = text.replace(Regex("(delete|remove|bondho|task|todo)", RegexOption.IGNORE_CASE), "").trim()

        val task = all.find { it.title.contains(taskTitle, ignoreCase = true) }

        return if (task != null) {
            todoDao.delete(task)
            "🗑️ '${task.title}' delete korchi!"
        } else {
            "Task '$taskTitle' pai ni."
        }
    }

    private suspend fun listTodos(): String {
        val pending = todoDao.getPending().first()

        return if (pending.isEmpty()) {
            "🎉 Shob tasks complete! Kono pending task nei."
        } else {
            buildString {
                appendLine("📝 Pending Tasks (${pending.size}):")
                pending.forEachIndexed { index, todo ->
                    val dueStr = todo.dueTime?.let {
                        " (Due: ${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(it))})"
                    } ?: ""
                    appendLine("${index + 1}. ${todo.title}$dueStr")
                }
            }
        }
    }

    private suspend fun getTodayTodos(): String {
        val all = todoDao.getPending().first()
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis

        val todayTasks = all.filter { todo ->
            val todoDate = java.util.Calendar.getInstance().apply {
                timeInMillis = todo.createdAt
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }.timeInMillis
            todoDate == today || (todo.dueTime != null && todo.dueTime >= today)
        }

        return if (todayTasks.isEmpty()) {
            "📝 Ajker jonno kono task nei!"
        } else {
            buildString {
                appendLine("📝 Ajker Tasks (${todayTasks.size}):")
                todayTasks.forEachIndexed { index, todo ->
                    appendLine("${index + 1}. ${todo.title}")
                }
            }
        }
    }
}
