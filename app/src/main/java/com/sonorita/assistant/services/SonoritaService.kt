package com.sonorita.assistant.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.sonorita.assistant.R
import com.sonorita.assistant.SonoritaApp
import com.sonorita.assistant.ai.AIEngine
import com.sonorita.assistant.ai.EmotionDetector
import com.sonorita.assistant.ai.MemoryManager
import com.sonorita.assistant.ai.ProactiveSuggestionEngine
import com.sonorita.assistant.ai.QueryClassifier
import com.sonorita.assistant.ai.ScreenAnalyzer
import com.sonorita.assistant.controllers.*
import com.sonorita.assistant.ui.HomeActivity
import kotlinx.coroutines.*
import java.util.*

class SonoritaService : LifecycleService(), TextToSpeech.OnInitListener {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.sonorita.START"
        const val ACTION_STOP = "com.sonorita.STOP"
        const val ACTION_VOICE_INPUT = "com.sonorita.VOICE_INPUT"
        const val ACTION_TEXT_INPUT = "com.sonorita.TEXT_INPUT"

        // Mode constants
        const val MODE_ACTIVE = "active"
        const val MODE_SLEEP = "sleep"
        const val MODE_SILENT = "silent"

        var currentMode = MODE_ACTIVE
            private set

        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, SonoritaService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SonoritaService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    // Core components
    private lateinit var aiEngine: AIEngine
    private lateinit var memoryManager: MemoryManager
    private lateinit var emotionDetector: EmotionDetector
    private lateinit var screenAnalyzer: ScreenAnalyzer
    private lateinit var proactiveEngine: ProactiveSuggestionEngine

    // Controllers
    private lateinit var commandHandler: CommandHandler
    private lateinit var voiceController: VoiceController
    private lateinit var systemController: SystemController
    private lateinit var callController: CallController
    private lateinit var mediaController: MediaController
    private lateinit var fileController: FileController
    private lateinit var expenseController: ExpenseController
    private lateinit var habitController: HabitController
    private lateinit var todoController: TodoController
    private lateinit var noteController: NoteController
    private lateinit var focusController: FocusController
    private lateinit var researchController: ResearchController
    private lateinit var ocrController: OCRController
    private lateinit var translatorController: TranslatorController
    private lateinit var securityController: SecurityController
    private lateinit var networkController: NetworkController
    private lateinit var autopilotController: AutopilotController
    private lateinit var arController: ARController
    private lateinit var gestureController: GestureDetectionService
    private lateinit var appUsageController: AppUsageController

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsQueue = mutableListOf<String>()

    // Mode management
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val inactivityTimeout = 10 * 60 * 1000L // 10 minutes
    private var isSpeaking = false

    // Coroutine scope
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        initializeComponents()
        startForeground(NOTIFICATION_ID, createNotification("Sonorita active"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                isServiceRunning = true
                initializeTTS()
                resetInactivityTimer()
                updateNotification("Sonorita active - ${currentMode.uppercase()}")
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_VOICE_INPUT -> {
                val text = intent.getStringExtra("text")
                if (!text.isNullOrEmpty()) {
                    onVoiceInput(text)
                }
            }
            ACTION_TEXT_INPUT -> {
                val text = intent.getStringExtra("text")
                if (!text.isNullOrEmpty()) {
                    onTextInput(text)
                }
            }
        }

        return START_STICKY
    }

    private fun initializeComponents() {
        val db = SonoritaApp.instance.database

        aiEngine = AIEngine(this, db.preferenceDao())
        memoryManager = MemoryManager(db.conversationDao(), db.preferenceDao())
        emotionDetector = EmotionDetector()
        screenAnalyzer = ScreenAnalyzer(this)
        proactiveEngine = ProactiveSuggestionEngine(db.conversationDao(), db.reminderDao(), db.habitDao())

        // Initialize controllers
        voiceController = VoiceController(this)
        systemController = SystemController(this)
        callController = CallController(this)
        mediaController = MediaController(this)
        fileController = FileController(this)
        expenseController = ExpenseController(db.expenseDao())
        habitController = HabitController(db.habitDao())
        todoController = TodoController(db.todoDao())
        noteController = NoteController(db.noteDao())
        focusController = FocusController(this)
        researchController = ResearchController(this, aiEngine)
        ocrController = OCRController(this)
        translatorController = TranslatorController(this)
        securityController = SecurityController(this)
        networkController = NetworkController(this, db.speedTestDao(), db.appUsageDao())
        autopilotController = AutopilotController(this)
        arController = ARController(this, aiEngine)
        appUsageController = AppUsageController(this, db.appUsageDao())

        commandHandler = CommandHandler(
            this, aiEngine, memoryManager, systemController, callController,
            mediaController, fileController, expenseController, habitController,
            todoController, noteController, focusController, researchController,
            ocrController, translatorController, securityController, networkController,
            autopilotController, arController, appUsageController, screenAnalyzer
        )
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Set language
                engine.language = Locale("bn", "BD")
                val result = engine.setLanguage(Locale("bn", "BD"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.language = Locale.US
                }

                // Configure voice
                val speed = runBlocking { memoryManager.getPreference("voice_speed", "1.0").toFloatOrNull() ?: 1.0f }
                val pitch = runBlocking { memoryManager.getPreference("voice_pitch", "1.0").toFloatOrNull() ?: 1.0f }
                engine.setSpeechRate(speed)
                engine.setPitch(pitch)

                // Try to set female voice
                val voices = engine.voices
                val femaleVoice = voices?.find {
                    it.name.contains("female", ignoreCase = true) ||
                    it.name.contains("bn", ignoreCase = true)
                }
                if (femaleVoice != null) {
                    engine.voice = femaleVoice
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        serviceScope.launch { updateBubbleState("speaking") }
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        serviceScope.launch { updateBubbleState(currentMode) }
                    }

                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                    }
                })

                ttsReady = true
                // Speak queued messages
                ttsQueue.forEach { speakInternal(it) }
                ttsQueue.clear()
            }
        }
    }

    fun speak(text: String) {
        if (currentMode == MODE_SILENT) return

        if (ttsReady) {
            speakInternal(text)
        } else {
            ttsQueue.add(text)
        }
    }

    private fun speakInternal(text: String) {
        val utteranceId = "sonorita_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    fun onVoiceInput(text: String) {
        resetInactivityTimer()
        if (currentMode == MODE_SILENT && !text.lowercase().contains("start") && !text.lowercase().contains("hey sonorita")) {
            return
        }

        // Check for mode commands
        when {
            text.lowercase().contains("stop") -> {
                setMode(MODE_SILENT)
                speak("Silent mode activated.")
                return
            }
            text.lowercase().contains("start") || text.lowercase().contains("hey sonorita") -> {
                setMode(MODE_ACTIVE)
                speak("Yes, I'm listening.")
                return
            }
        }

        processInput(text, isVoice = true)
    }

    fun onTextInput(text: String) {
        resetInactivityTimer()
        processInput(text, isVoice = false)
    }

    private fun processInput(text: String, isVoice: Boolean) {
        serviceScope.launch {
            try {
                updateBubbleState("thinking")

                // Save user message
                memoryManager.saveMessage("user", text)

                // Check if it's a device command
                val deviceCommand = QueryClassifier.classifyDeviceCommand(text)
                if (deviceCommand != null) {
                    val result = commandHandler.handleCommand(deviceCommand, text)
                    if (isVoice && result.isNotEmpty()) {
                        speak(result)
                    }
                    memoryManager.saveMessage("assistant", result)
                    updateBubbleState(currentMode)
                    return@launch
                }

                // Get conversation history
                val history = memoryManager.getConversationHistory(20)

                // Query AI
                val response = aiEngine.query(text, history)
                val emotion = emotionDetector.detectEmotion(response.content)

                // Save AI response
                memoryManager.saveMessage("assistant", response.content, response.provider)

                // Speak response if voice input
                if (isVoice) {
                    speak(response.content)
                }

                // Send response to UI
                broadcastResponse(response.content, emotion.emoji, response.provider)

                updateBubbleState(currentMode)

            } catch (e: Exception) {
                val errorMsg = "Sorry, I encountered an error: ${e.message}"
                if (isVoice) speak(errorMsg)
                broadcastResponse(errorMsg, "😔", "error")
                updateBubbleState(currentMode)
            }
        }
    }

    fun setMode(mode: String) {
        currentMode = mode
        updateBubbleState(mode)
        updateNotification("Sonorita - ${mode.uppercase()}")

        when (mode) {
            MODE_ACTIVE -> resetInactivityTimer()
            MODE_SLEEP -> {} // Already in sleep
            MODE_SILENT -> inactivityHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun resetInactivityTimer() {
        inactivityHandler.removeCallbacksAndMessages(null)
        if (currentMode != MODE_SILENT) {
            setMode(MODE_ACTIVE)
            inactivityHandler.postDelayed({
                if (currentMode == MODE_ACTIVE && !isSpeaking) {
                    setMode(MODE_SLEEP)
                }
            }, inactivityTimeout)
        }
    }

    private fun updateBubbleState(state: String) {
        val intent = Intent("com.sonorita.BUBBLE_STATE").apply {
            putExtra("state", state)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun broadcastResponse(content: String, emoji: String, provider: String) {
        val intent = Intent("com.sonorita.AI_RESPONSE").apply {
            putExtra("content", content)
            putExtra("emoji", emoji)
            putExtra("provider", provider)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun createNotification(text: String): Notification {
        val channelId = SonoritaApp.CHANNEL_SERVICE
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, HomeActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sonorita")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        isServiceRunning = false
        inactivityHandler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        serviceScope.cancel()

        // Restart service
        val intent = Intent(this, SonoritaService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Service keeps running even when app is removed from recents
    }

    // Getters for components
    fun getAIEngine() = aiEngine
    fun getMemoryManager() = memoryManager
    fun getCommandHandler() = commandHandler
    fun getVoiceController() = voiceController
    fun getScreenAnalyzer() = screenAnalyzer
}
