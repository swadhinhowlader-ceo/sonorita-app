package com.sonorita.assistant.controllers

import android.content.Context
import com.sonorita.assistant.ai.*
import com.sonorita.assistant.services.SonoritaService

class CommandHandler(
    private val context: Context,
    private val aiEngine: AIEngine,
    private val memoryManager: MemoryManager,
    private val systemController: SystemController,
    private val callController: CallController,
    private val mediaController: MediaController,
    private val fileController: FileController,
    private val expenseController: ExpenseController,
    private val habitController: HabitController,
    private val todoController: TodoController,
    private val noteController: NoteController,
    private val focusController: FocusController,
    private val researchController: ResearchController,
    private val ocrController: OCRController,
    private val translatorController: TranslatorController,
    private val securityController: SecurityController,
    private val networkController: NetworkController,
    private val autopilotController: AutopilotController,
    private val arController: ARController,
    private val appUsageController: AppUsageController,
    private val screenAnalyzer: ScreenAnalyzer
) {

    suspend fun handleCommand(command: QueryClassifier.DeviceCommand, text: String): String {
        return when (command) {
            // System Controls
            QueryClassifier.DeviceCommand.FLASH -> systemController.toggleFlash()
            QueryClassifier.DeviceCommand.WIFI -> systemController.toggleWifi()
            QueryClassifier.DeviceCommand.BLUETOOTH -> systemController.toggleBluetooth()
            QueryClassifier.DeviceCommand.VOLUME -> systemController.handleVolume(text)
            QueryClassifier.DeviceCommand.BRIGHTNESS -> systemController.handleBrightness(text)
            QueryClassifier.DeviceCommand.LOCK -> systemController.lockDevice()
            QueryClassifier.DeviceCommand.UNLOCK -> systemController.unlockDevice()
            QueryClassifier.DeviceCommand.SHUTDOWN -> systemController.shutdownDevice()
            QueryClassifier.DeviceCommand.REBOOT -> systemController.rebootDevice()
            QueryClassifier.DeviceCommand.BATTERY -> systemController.getBatteryStatus()

            // Communication
            QueryClassifier.DeviceCommand.CALL -> callController.handleCall(text)
            QueryClassifier.DeviceCommand.SMS -> callController.handleSMS(text)
            QueryClassifier.DeviceCommand.MEETING_RECORD -> callController.recordMeeting()

            // Media
            QueryClassifier.DeviceCommand.RECORD_AUDIO -> mediaController.recordAudio()
            QueryClassifier.DeviceCommand.RECORD_VIDEO -> mediaController.recordVideo()
            QueryClassifier.DeviceCommand.SCREENSHOT -> mediaController.takeScreenshot()
            QueryClassifier.DeviceCommand.PHOTO -> mediaController.takePhoto()

            // Reminders
            QueryClassifier.DeviceCommand.REMINDER -> handleReminder(text)

            // Modes
            QueryClassifier.DeviceCommand.MEETING_MODE -> systemController.setMeetingMode()
            QueryClassifier.DeviceCommand.DND -> systemController.setDND(text)
            QueryClassifier.DeviceCommand.FOCUS_MODE -> focusController.startFocus(text)
            QueryClassifier.DeviceCommand.BUSY_MODE -> systemController.setBusyMode()

            // Network
            QueryClassifier.DeviceCommand.HOTSPOT -> systemController.toggleHotspot()
            QueryClassifier.DeviceCommand.VPN -> systemController.toggleVPN()
            QueryClassifier.DeviceCommand.NFC -> systemController.toggleNFC()
            QueryClassifier.DeviceCommand.SPEED_TEST -> networkController.runSpeedTest()
            QueryClassifier.DeviceCommand.NETWORK_MONITOR -> networkController.getDataUsage(text)

            // Features
            QueryClassifier.DeviceCommand.GESTURE_MODE -> arController.toggleGestureMode()
            QueryClassifier.DeviceCommand.EYE_MODE -> arController.toggleEyeMode()
            QueryClassifier.DeviceCommand.AR_MODE -> arController.toggleARMode()
            QueryClassifier.DeviceCommand.TRANSLATE -> translatorController.translate(text)

            // Screen & Analysis
            QueryClassifier.DeviceCommand.LIVE_SCREEN -> handleLiveScreen(text)
            QueryClassifier.DeviceCommand.APP_USAGE -> appUsageController.getUsage(text)

            // Productivity
            QueryClassifier.DeviceCommand.EXPENSE -> expenseController.handleExpense(text)
            QueryClassifier.DeviceCommand.HABIT -> habitController.handleHabit(text)
            QueryClassifier.DeviceCommand.TODO -> todoController.handleTodo(text)
            QueryClassifier.DeviceCommand.NOTE -> noteController.handleNote(text)
            QueryClassifier.DeviceCommand.DAILY_SUMMARY -> generateDailySummary()

            // Security
            QueryClassifier.DeviceCommand.INTRUDER_PHOTO -> securityController.getIntruderPhotos()
            QueryClassifier.DeviceCommand.PRIVACY_SCREEN -> securityController.togglePrivacyScreen()
            QueryClassifier.DeviceCommand.APP_LOCK -> securityController.handleAppLock(text)
            QueryClassifier.DeviceCommand.ANTI_THEFT -> securityController.handleAntiTheft(text)
            QueryClassifier.DeviceCommand.FACE_UNLOCK -> securityController.toggleFaceUnlock()

            // Research & Journal
            QueryClassifier.DeviceCommand.RESEARCH -> researchController.research(text)
            QueryClassifier.DeviceCommand.DREAM_JOURNAL -> handleDreamJournal(text)
        }
    }

    private suspend fun handleReminder(text: String): String {
        // Parse reminder from text
        val timePattern = Regex("(\\d+)\\s*(minute|hour|ghonta|min|hr)", RegexOption.IGNORE_CASE)
        val match = timePattern.find(text)

        return if (match != null) {
            val amount = match.groupValues[1].toIntOrNull() ?: 1
            val unit = match.groupValues[2].lowercase()
            val millis = when {
                unit.startsWith("min") || unit.startsWith("min") -> amount * 60 * 1000L
                unit.startsWith("hour") || unit.startsWith("ghont") || unit.startsWith("hr") -> amount * 60 * 60 * 1000L
                else -> amount * 60 * 1000L
            }

            val title = text.replace(timePattern, "").replace("remind", "").replace("mone koro", "").trim()
            val triggerTime = System.currentTimeMillis() + millis

            val db = com.sonorita.assistant.SonoritaApp.instance.database
            db.reminderDao().insert(
                com.sonorita.assistant.data.ReminderEntity(
                    title = title.ifEmpty { "Reminder" },
                    message = text,
                    triggerTime = triggerTime
                )
            )

            "⏰ ${amount}${if (unit.startsWith("min")) " minute" else " ghonta"} por reminder set korechi!"
        } else {
            "Reminder set korte parlam na. Bolo koto somoy por remind korbo?"
        }
    }

    private suspend fun handleLiveScreen(text: String): String {
        return when {
            text.contains("on") -> "Live screen mode chalu korte screen projection enable korte hobe. Settings e giye enable koro."
            text.contains("off") -> "Live screen mode bondho korechi."
            else -> "Live screen on na off? Bolo ki korte chao."
        }
    }

    private suspend fun handleDreamJournal(text: String): String {
        return when {
            text.contains("dekhao") || text.contains("show") || text.contains("list") -> {
                val dir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS), "Sonorita/DreamJournal")
                if (dir.exists()) {
                    val files = dir.listFiles()?.sortedByDescending { it.lastModified() }
                    if (files.isNullOrEmpty()) {
                        "Dream journal empty. Kono swapno likha nei!"
                    } else {
                        "📓 Dream Journal:\n" + files.take(10).joinToString("\n") { "• ${it.name}" }
                    }
                } else {
                    "Dream journal folder toiri hoy ni."
                }
            }
            text.contains("sunao") || text.contains("read") -> {
                "Dream journal recording mode chalu. Bolo tomar swapno ki chilo..."
            }
            else -> "Dream journal er ki korte chao? (dekhao / sunao / record)"
        }
    }

    private suspend fun generateDailySummary(): String {
        val db = com.sonorita.assistant.SonoritaApp.instance.database
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val startOfDay = cal.timeInMillis

        val expenses = db.expenseDao().getTotalSince(startOfDay) ?: 0.0
        val pendingTodos = db.todoDao().getPending()
        val activeHabits = db.habitDao().getActive()

        return buildString {
            appendLine("📊 আজকের Daily Summary:")
            appendLine("💰 খরচ: $expenses টাকা")
            appendLine("📝 বাকি আছে: tasks check করো")
            appendLine("🏋️ Habit: track করো")
        }
    }
}
