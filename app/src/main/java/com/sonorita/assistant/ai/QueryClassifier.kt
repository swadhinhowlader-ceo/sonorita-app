package com.sonorita.assistant.ai

object QueryClassifier {

    enum class QueryType {
        SIMPLE,      // Quick answers, calculations, definitions
        MEDIUM,      // General conversation, explanations
        RESEARCH,    // Deep research, web search needed
        VISION,      // Image/screen analysis
        DEVICE_COMMAND // Direct device action, skip AI
    }

    data class Classification(
        val type: QueryType,
        val confidence: Float
    )

    // Device command keywords (Bengali + English)
    private val deviceCommandKeywords = listOf(
        // English
        "flash", "torch", "wifi", "bluetooth", "volume", "brightness",
        "lock", "unlock", "shutdown", "reboot", "battery",
        "call", "dial", "message", "sms", "send",
        "open", "close", "play", "pause", "stop",
        "record", "screenshot", "photo", "camera",
        "remind", "timer", "alarm",
        "meeting mode", "dnd", "do not disturb", "silent",
        "focus mode", "busy mode",
        "hotspot", "vpn", "nfc",
        "gesture mode", "eye mode", "ar mode",
        // Bengali
        "flash", "torchi", "wifi", "bluetooth", "volume", "brightness",
        "lock", "unlock", "shutdown", "reboot", "battery",
        "call", "dial", "message", "sms", "pathao",
        "kholo", "bondho", "play", "pause", "stop",
        "record", "screenshot", "photo", "camera",
        "mone koro", "timer", "alarm",
        "meeting mode", "dnd", "silent",
        "focus mode", "busy mode",
        "hotspot", "vpn", "nfc",
        "gesture mode", "eye mode", "ar mode"
    )

    // Research keywords
    private val researchKeywords = listOf(
        "research", "search", "find", "look up", "explain in detail",
        "tell me about", "what is", "who is", "how does",
        "compare", "difference between", "pros and cons",
        "history of", "analysis", "deep dive",
        "koro research", "khuj", "ki", "ke", "kivabe", "keno",
        "details e bolo", "explain", "bistarito bolo"
    )

    // Vision keywords
    private val visionKeywords = listOf(
        "screen", "this image", "this photo", "what do you see",
        "look at", "analyze this", "read this", "what's on screen",
        "ei image", "ei photo", "ki dekhchi", "screen dekhao",
        "live screen", "screen e ki ache"
    )

    fun classify(text: String): Classification {
        val lowerText = text.lowercase().trim()

        // Check for device commands first
        for (keyword in deviceCommandKeywords) {
            if (lowerText.contains(keyword)) {
                return Classification(QueryType.DEVICE_COMMAND, 0.9f)
            }
        }

        // Check for vision
        for (keyword in visionKeywords) {
            if (lowerText.contains(keyword)) {
                return Classification(QueryType.VISION, 0.85f)
            }
        }

        // Check for research
        for (keyword in researchKeywords) {
            if (lowerText.contains(keyword)) {
                return Classification(QueryType.RESEARCH, 0.8f)
            }
        }

        // Determine if simple or medium based on length and complexity
        val wordCount = lowerText.split("\\s+".toRegex()).size

        return if (wordCount <= 10 && !lowerText.contains("?")) {
            Classification(QueryType.SIMPLE, 0.7f)
        } else {
            Classification(QueryType.MEDIUM, 0.6f)
        }
    }

    fun classifyDeviceCommand(text: String): DeviceCommand? {
        val lower = text.lowercase().trim()

        return when {
            lower.contains("flash") || lower.contains("torch") -> DeviceCommand.FLASH
            lower.contains("wifi") -> DeviceCommand.WIFI
            lower.contains("bluetooth") -> DeviceCommand.BLUETOOTH
            lower.contains("volume") -> DeviceCommand.VOLUME
            lower.contains("brightness") -> DeviceCommand.BRIGHTNESS
            lower.contains("lock") && !lower.contains("unlock") && !lower.contains("app") -> DeviceCommand.LOCK
            lower.contains("unlock") -> DeviceCommand.UNLOCK
            lower.contains("shutdown") -> DeviceCommand.SHUTDOWN
            lower.contains("reboot") -> DeviceCommand.REBOOT
            lower.contains("battery") -> DeviceCommand.BATTERY
            lower.contains("call") || lower.contains("dial") -> DeviceCommand.CALL
            lower.contains("message") || lower.contains("sms") -> DeviceCommand.SMS
            lower.contains("record") && lower.contains("audio") -> DeviceCommand.RECORD_AUDIO
            lower.contains("record") && lower.contains("video") -> DeviceCommand.RECORD_VIDEO
            lower.contains("screenshot") -> DeviceCommand.SCREENSHOT
            lower.contains("photo") || lower.contains("camera") -> DeviceCommand.PHOTO
            lower.contains("remind") || lower.contains("timer") || lower.contains("alarm") -> DeviceCommand.REMINDER
            lower.contains("meeting mode") -> DeviceCommand.MEETING_MODE
            lower.contains("dnd") || lower.contains("do not disturb") -> DeviceCommand.DND
            lower.contains("focus mode") -> DeviceCommand.FOCUS_MODE
            lower.contains("busy mode") -> DeviceCommand.BUSY_MODE
            lower.contains("hotspot") -> DeviceCommand.HOTSPOT
            lower.contains("vpn") -> DeviceCommand.VPN
            lower.contains("nfc") -> DeviceCommand.NFC
            lower.contains("gesture mode") -> DeviceCommand.GESTURE_MODE
            lower.contains("eye mode") -> DeviceCommand.EYE_MODE
            lower.contains("ar mode") -> DeviceCommand.AR_MODE
            lower.contains("meeting record") -> DeviceCommand.MEETING_RECORD
            lower.contains("speed test") || lower.contains("net speed") -> DeviceCommand.SPEED_TEST
            lower.contains("app usage") -> DeviceCommand.APP_USAGE
            lower.contains("daily summary") || lower.contains("morning summary") -> DeviceCommand.DAILY_SUMMARY
            lower.contains("intruder photo") -> DeviceCommand.INTRUDER_PHOTO
            lower.contains("dream journal") -> DeviceCommand.DREAM_JOURNAL
            lower.contains("translate") -> DeviceCommand.TRANSLATE
            lower.contains("live screen") -> DeviceCommand.LIVE_SCREEN
            lower.contains("expense") || lower.contains("kharcha") -> DeviceCommand.EXPENSE
            lower.contains("habit") -> DeviceCommand.HABIT
            lower.contains("task") || lower.contains("todo") -> DeviceCommand.TODO
            lower.contains("note") -> DeviceCommand.NOTE
            lower.contains("privacy screen") -> DeviceCommand.PRIVACY_SCREEN
            lower.contains("app lock") -> DeviceCommand.APP_LOCK
            lower.contains("anti-theft") -> DeviceCommand.ANTI_THEFT
            lower.contains("face unlock") -> DeviceCommand.FACE_UNLOCK
            lower.contains("research") -> DeviceCommand.RESEARCH
            lower.contains("speed test") -> DeviceCommand.SPEED_TEST
            lower.contains("data usage") || lower.contains("net speed") -> DeviceCommand.NETWORK_MONITOR
            else -> null
        }
    }

    enum class DeviceCommand {
        FLASH, WIFI, BLUETOOTH, VOLUME, BRIGHTNESS, LOCK, UNLOCK,
        SHUTDOWN, REBOOT, BATTERY, CALL, SMS, RECORD_AUDIO, RECORD_VIDEO,
        SCREENSHOT, PHOTO, REMINDER, MEETING_MODE, DND, FOCUS_MODE,
        BUSY_MODE, HOTSPOT, VPN, NFC, GESTURE_MODE, EYE_MODE, AR_MODE,
        MEETING_RECORD, SPEED_TEST, APP_USAGE, DAILY_SUMMARY,
        INTRUDER_PHOTO, DREAM_JOURNAL, TRANSLATE, LIVE_SCREEN,
        EXPENSE, HABIT, TODO, NOTE, PRIVACY_SCREEN, APP_LOCK,
        ANTI_THEFT, FACE_UNLOCK, RESEARCH, NETWORK_MONITOR
    }
}
