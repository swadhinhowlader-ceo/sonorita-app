package com.sonorita.assistant.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sonorita.assistant.SonoritaApp
import com.sonorita.assistant.databinding.ActivityHomeBinding
import com.sonorita.assistant.services.SonoritaService
import com.sonorita.assistant.controllers.VoiceController

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private lateinit var voiceController: VoiceController
    private var isListening = false

    data class ChatMessage(
        val content: String,
        val isUser: Boolean,
        val emoji: String? = null,
        val provider: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val responseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.sonorita.AI_RESPONSE" -> {
                    val content = intent.getStringExtra("content") ?: return
                    val emoji = intent.getStringExtra("emoji")
                    val provider = intent.getStringExtra("provider")

                    runOnUiThread {
                        addMessage(content, isUser = false, emoji = emoji, provider = provider)
                        binding.progressBar.visibility = View.GONE
                    }
                }
                "com.sonorita.BUBBLE_STATE" -> {
                    val state = intent.getStringExtra("state") ?: return
                    runOnUiThread {
                        updateStatusIndicator(state)
                    }
                }
                "com.sonorita.INCOMING_CALL" -> {
                    val number = intent.getStringExtra("number") ?: "Unknown"
                    runOnUiThread {
                        addMessage("📞 Incoming call from: $number", isUser = false)
                    }
                }
                "com.sonorita.SMS_RECEIVED" -> {
                    val sender = intent.getStringExtra("sender") ?: "Unknown"
                    val body = intent.getStringExtra("body") ?: ""
                    runOnUiThread {
                        addMessage("✉️ $sender: $body", isUser = false)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupVoiceController()
        registerReceivers()
    }

    private fun setupUI() {
        // Input field
        binding.sendButton.setOnClickListener {
            val text = binding.inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                sendTextCommand(text)
                binding.inputField.text?.clear()
            }
        }

        // Voice button
        binding.voiceButton.setOnClickListener {
            toggleVoiceListening()
        }

        // Settings button
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Update status
        updateStatusIndicator(SonoritaService.currentMode)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity).apply {
                stackFromEnd = true
            }
            adapter = this@HomeActivity.adapter
        }
    }

    private fun setupVoiceController() {
        voiceController = VoiceController(this)
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction("com.sonorita.AI_RESPONSE")
            addAction("com.sonorita.BUBBLE_STATE")
            addAction("com.sonorita.INCOMING_CALL")
            addAction("com.sonorita.SMS_RECEIVED")
        }
        registerReceiver(responseReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    private fun sendTextCommand(text: String) {
        addMessage(text, isUser = true)
        binding.progressBar.visibility = View.VISIBLE

        val intent = Intent(this, SonoritaService::class.java).apply {
            action = SonoritaService.ACTION_TEXT_INPUT
            putExtra("text", text)
        }
        startService(intent)
    }

    private fun toggleVoiceListening() {
        if (isListening) {
            voiceController.stopListening()
            isListening = false
            binding.voiceButton.setImageResource(android.R.drawable.ic_btn_speak_now)
        } else {
            voiceController.startListening(
                onResult = { text ->
                    runOnUiThread {
                        addMessage(text, isUser = true)
                        binding.progressBar.visibility = View.VISIBLE

                        val intent = Intent(this, SonoritaService::class.java).apply {
                            action = SonoritaService.ACTION_VOICE_INPUT
                            putExtra("text", text)
                        }
                        startService(intent)
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    }
                }
            )
            isListening = true
            binding.voiceButton.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun addMessage(content: String, isUser: Boolean, emoji: String? = null, provider: String? = null) {
        messages.add(ChatMessage(content, isUser, emoji, provider))
        adapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun updateStatusIndicator(state: String) {
        val (color, text) = when (state) {
            "active" -> Pair(0xFF4CAF50.toInt(), "ACTIVE 🟢")
            "sleep" -> Pair(0xFF2196F3.toInt(), "SLEEP 🔵")
            "silent" -> Pair(0xFFF44336.toInt(), "SILENT 🔴")
            "thinking" -> Pair(0xFFFFEB3B.toInt(), "THINKING 🟡")
            "speaking" -> Pair(0xFF00BCD4.toInt(), "SPEAKING 🔵")
            else -> Pair(0xFF9E9E9E.toInt(), "IDLE")
        }
        binding.statusText.text = text
        binding.statusIndicator.setBackgroundColor(color)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(responseReceiver)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
