# Sonorita 🤖

**Your Personal AI Assistant — Built natively for Android**

Sonorita is a complete, production-ready Android AI assistant app built with pure Kotlin and Android Studio. It features 42 powerful capabilities including AI chat, voice control, screen analysis, gesture recognition, security suite, and much more.

## ✨ Features

### 🧠 AI Engine
- Multi-provider AI with fallback: Gemini → OpenRouter → Groq → OpenAI
- Auto query classification (simple/medium/research/vision/device command)
- Editable system prompt
- Conversation memory across sessions
- Learning mode for habits and preferences
- Proactive suggestions based on context

### 🎙️ Voice & Wake Word
- STT: Bengali + English with auto-detection
- TTS: Online + offline fallback
- Wake word "Hey Sonorita" 24/7
- Three modes: Active (green), Sleep (blue), Silent (red)
- 10-minute inactivity auto-sleep timer

### 🫧 Floating Bubble
- Draggable overlay on all apps
- Visual state indicators (active/sleep/silent/thinking/speaking)
- Tap to activate voice

### 📺 Live Screen View
- MediaProjection API screen capture
- AI-powered screen analysis
- "screen dekhao" / "what's on screen" commands

### 📱 42 Features
1. AI Engine with multi-provider fallback
2. Voice & Wake Word system
3. Background Service (24/7)
4. Floating Bubble
5. Live Screen View
6. App Usage Tracker
7. Smart Do Not Disturb
8. Gesture Control
9. Eye Tracking
10. Face Recognition
11. Call Handling
12. SMS & Messaging
13. Smart Reply Generator
14. Notification Listener
15. Autopilot (Screen Control)
16. File Manager
17. OCR & Math Solver
18. AR Mode & Object Identifier
19. Dream Journal
20. Voice Translator
21. Clipboard Monitor
22. WiFi Speed Test
23. Network Monitor
24. VPN Toggle
25. NFC Reader
26. System Controls
27. Media Controls
28. Reminders & Timers
29. Location
30. Research → PDF
31. Expense Tracker
32. Habit Tracker
33. To-Do List
34. Note Taking
35. Focus Mode
36. Daily Summary
37. Auto Reply
38. Battery Alerts
39. Contextual Awareness
40. Security Suite (Intruder Photo, Anti-theft, App Lock, Privacy Screen)
41. Intelligent Hotspot Memory
42. Learning Mode

## 🚀 Open in Android Studio

1. **Clone or download** this repository
2. Open **Android Studio** (latest stable)
3. Click **File → Open** and select the `Sonorita/` folder
4. Wait for **Gradle sync** to complete
5. Connect your **Android 13+ device** or start an emulator
6. Click **Run ▶️**

### Build Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Min SDK 33 (Android 13)

## 🔐 Permissions to Grant Manually

After installing, grant these permissions:

### Standard Permissions (requested automatically)
- 🎤 Microphone
- 📞 Phone & Calls
- ✉️ SMS
- 📁 Storage
- 📷 Camera
- 📍 Location
- 🔔 Notifications

### Special Permissions (manual setup required)

#### 1. Enable Accessibility Service
- Go to **Settings → Accessibility**
- Find **Sonorita**
- Toggle ON
- Grant the required permissions

#### 2. Enable Notification Listener
- Go to **Settings → Apps → Special app access → Notification access**
- Find **Sonorita**
- Toggle ON

#### 3. Enable Overlay Permission
- Go to **Settings → Apps → Special app access → Display over other apps**
- Find **Sonorita**
- Toggle ON

#### 4. Disable Battery Optimization
- Go to **Settings → Battery → Battery optimization**
- Find **Sonorita**
- Select **Don't optimize**

#### 5. Enable Media Projection
- On first screen view command, grant the media projection permission
- This is needed for Live Screen View feature

#### 6. Enable Usage Stats Access
- Go to **Settings → Apps → Special app access → Usage access**
- Find **Sonorita**
- Toggle ON

## 🔧 Default Settings

- **Unlock Code:** `2003`
- **Default AI Provider:** Gemini
- **Voice:** Female, speed 1.0, pitch 1.0
- **Wake Word:** Enabled ("Hey Sonorita")
- **Inactivity Timeout:** 10 minutes
- **Floating Bubble:** Enabled

## 📁 Project Structure

```
Sonorita/
├── app/
│   ├── src/main/
│   │   ├── java/com/sonorita/assistant/
│   │   │   ├── SonoritaApp.kt          # Application class
│   │   │   ├── ui/                     # Activities & UI
│   │   │   │   ├── SplashActivity.kt
│   │   │   │   ├── HomeActivity.kt
│   │   │   │   ├── SettingsActivity.kt
│   │   │   │   ├── PermissionActivity.kt
│   │   │   │   ├── ChatAdapter.kt
│   │   │   │   └── FloatingBubbleActivity.kt
│   │   │   ├── ai/                     # AI Engine
│   │   │   │   ├── AIEngine.kt
│   │   │   │   ├── QueryClassifier.kt
│   │   │   │   ├── MemoryManager.kt
│   │   │   │   ├── EmotionDetector.kt
│   │   │   │   ├── ScreenAnalyzer.kt
│   │   │   │   └── ProactiveSuggestionEngine.kt
│   │   │   ├── services/               # Background services
│   │   │   │   ├── SonoritaService.kt
│   │   │   │   ├── BootReceiver.kt
│   │   │   │   ├── SonoritaAccessibilityService.kt
│   │   │   │   ├── SonoritaNotificationListener.kt
│   │   │   │   ├── CallReceiver.kt
│   │   │   │   ├── SmsReceiver.kt
│   │   │   │   ├── ClipboardMonitorService.kt
│   │   │   │   ├── ScreenProjectionService.kt
│   │   │   │   ├── GestureDetectionService.kt
│   │   │   │   └── NetworkMonitorService.kt
│   │   │   ├── controllers/            # Feature controllers
│   │   │   │   ├── CommandHandler.kt
│   │   │   │   ├── VoiceController.kt
│   │   │   │   ├── SystemController.kt
│   │   │   │   ├── CallController.kt
│   │   │   │   ├── MediaController.kt
│   │   │   │   ├── FileController.kt
│   │   │   │   ├── ExpenseController.kt
│   │   │   │   ├── HabitController.kt
│   │   │   │   ├── TodoController.kt
│   │   │   │   ├── NoteController.kt
│   │   │   │   ├── FocusController.kt
│   │   │   │   ├── ResearchController.kt
│   │   │   │   ├── OCRController.kt
│   │   │   │   ├── TranslatorController.kt
│   │   │   │   ├── SecurityController.kt
│   │   │   │   ├── NetworkController.kt
│   │   │   │   ├── AutopilotController.kt
│   │   │   │   ├── ARController.kt
│   │   │   │   └── AppUsageController.kt
│   │   │   ├── data/                   # Database
│   │   │   │   └── SonoritaDatabase.kt
│   │   │   └── utils/                  # Utilities
│   │   │       ├── PermissionHelper.kt
│   │   │       └── NetworkUtils.kt
│   │   ├── res/                        # Resources
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   ├── values/
│   │   │   └── xml/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🔧 Troubleshooting

### App crashes on startup
- Make sure all permissions are granted
- Check that Accessibility Service is enabled
- Disable battery optimization

### Voice not working
- Check microphone permission
- Ensure TTS engine is installed (Settings → Language → Text-to-speech)
- Check if Google Speech Services is installed

### Floating bubble not showing
- Enable overlay permission
- Check that the service is running (notification should appear)

### AI not responding
- Check internet connection
- Verify API keys are entered in Settings
- Try switching providers

### Screen view not working
- Grant media projection permission
- Check that the service is running

## 📜 License

This project is provided as-is for personal use.

## 🤖 Built With

- Kotlin
- Android SDK 34
- Room Database
- ML Kit (Text Recognition, Face Detection, Translation, Pose Detection)
- OkHttp
- Google Material Design
- TextToSpeech / SpeechRecognizer

---

**Sonorita** — Your AI, Your Rules. 🤖✨
