package com.sonorita.assistant.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sonorita.assistant.databinding.ActivitySettingsBinding
import com.sonorita.assistant.SonoritaApp
import com.sonorita.assistant.data.PreferenceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val db by lazy { SonoritaApp.instance.database }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = "Settings"
    }

    private fun loadSettings() {
        CoroutineScope(Dispatchers.Main).launch {
            val prefDao = db.preferenceDao()

            // System prompt
            binding.systemPromptEdit.setText(prefDao.get("system_prompt") ?: "")

            // API keys
            binding.geminiKeysEdit.setText(prefDao.get("gemini_api_keys") ?: "")
            binding.openrouterKeysEdit.setText(prefDao.get("openrouter_api_keys") ?: "")
            binding.groqKeysEdit.setText(prefDao.get("groq_api_keys") ?: "")
            binding.openaiKeysEdit.setText(prefDao.get("openai_api_keys") ?: "")

            // Voice settings
            binding.voiceSpeedSlider.value = prefDao.get("voice_speed")?.toFloatOrNull() ?: 1.0f
            binding.voicePitchSlider.value = prefDao.get("voice_pitch")?.toFloatOrNull() ?: 1.0f

            // Toggles
            binding.wakeWordSwitch.isChecked = prefDao.get("wake_word_enabled") != "false"
            binding.bubbleSwitch.isChecked = prefDao.get("bubble_enabled") != "false"
            binding.learningModeSwitch.isChecked = prefDao.get("learning_mode_enabled") != "false"
            binding.emotionDetectionSwitch.isChecked = prefDao.get("emotion_detection_enabled") != "false"

            // Unlock code
            binding.unlockCodeEdit.setText(prefDao.get("unlock_code") ?: "2003")

            // Battery optimization
            binding.batteryOptSwitch.isChecked = prefDao.get("battery_opt_exempt") == "true"
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }

        binding.clearHistoryButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                db.conversationDao().clearAll()
            }
        }
    }

    private fun saveSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefDao = db.preferenceDao()

            prefDao.set(PreferenceEntity("system_prompt", binding.systemPromptEdit.text.toString()))
            prefDao.set(PreferenceEntity("gemini_api_keys", binding.geminiKeysEdit.text.toString()))
            prefDao.set(PreferenceEntity("openrouter_api_keys", binding.openrouterKeysEdit.text.toString()))
            prefDao.set(PreferenceEntity("groq_api_keys", binding.groqKeysEdit.text.toString()))
            prefDao.set(PreferenceEntity("openai_api_keys", binding.openaiKeysEdit.text.toString()))

            prefDao.set(PreferenceEntity("voice_speed", binding.voiceSpeedSlider.value.toString()))
            prefDao.set(PreferenceEntity("voice_pitch", binding.voicePitchSlider.value.toString()))

            prefDao.set(PreferenceEntity("wake_word_enabled", binding.wakeWordSwitch.isChecked.toString()))
            prefDao.set(PreferenceEntity("bubble_enabled", binding.bubbleSwitch.isChecked.toString()))
            prefDao.set(PreferenceEntity("learning_mode_enabled", binding.learningModeSwitch.isChecked.toString()))
            prefDao.set(PreferenceEntity("emotion_detection_enabled", binding.emotionDetectionSwitch.isChecked.toString()))

            prefDao.set(PreferenceEntity("unlock_code", binding.unlockCodeEdit.text.toString()))
        }

        android.widget.Toast.makeText(this, "Settings saved!", android.widget.Toast.LENGTH_SHORT).show()
    }
}
