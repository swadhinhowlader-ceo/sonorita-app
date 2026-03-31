package com.sonorita.assistant.controllers

import android.content.Context
import android.accessibilityservice.AccessibilityServiceInfo
import com.senorita.assistant.services.SonoritaAccessibilityService

class AutopilotController(private val context: Context) {

    private val accessibilityService = SonoritaAccessibilityService.instance

    fun openApp(packageName: String): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "📱 App kholchi: $packageName"
            } else {
                "App '$packageName' install nei."
            }
        } catch (e: Exception) {
            "App open korte parlam na: ${e.message}"
        }
    }

    fun typeText(text: String): String {
        val service = accessibilityService ?: return "Accessibility service active nei. Settings e giye enable koro."

        val root = service.getRootNode() ?: return "Active window pai ni."
        val focused = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)

        return if (focused != null && service.typeText(focused, text)) {
            "⌨️ Type korchi: $text"
        } else {
            "Text type korte parlam na. Kono input field focused nei."
        }
    }

    fun clickByText(text: String): String {
        val service = accessibilityService ?: return "Accessibility service active nei."

        val root = service.getRootNode() ?: return "Active window pai ni."
        val node = service.findNodeByText(root, text)

        return if (node != null && service.clickNode(node)) {
            "👆 Click korchi: $text"
        } else {
            "'$text' button/element pai ni."
        }
    }

    fun scrollForward(): String {
        val service = accessibilityService ?: return "Accessibility service active nei."
        val root = service.getRootNode() ?: return "Active window pai ni."

        return if (service.scrollToFind(root, "")) {
            "⬇️ Scroll korchi..."
        } else {
            "Scroll korte parlam na."
        }
    }

    fun getForegroundApp(): String {
        val service = accessibilityService ?: return "Accessibility service active nei."
        return service.getAppPackageName() ?: "Unknown app"
    }

    fun isWhatsAppAutopilot(): Boolean {
        val app = getForegroundApp()
        return app.contains("whatsapp")
    }

    fun sendWhatsAppMessage(contact: String, message: String): String {
        val service = accessibilityService ?: return "Accessibility service active nei."

        return try {
            // Open WhatsApp
            openApp("com.whatsapp")

            // In real implementation: find contact, type message, send
            "📱 WhatsApp e '$contact' ke message pathate hobe. Implementation in progress."
        } catch (e: Exception) {
            "WhatsApp message error: ${e.message}"
        }
    }

    fun sendTelegramMessage(contact: String, message: String): String {
        return try {
            openApp("org.telegram.messenger")
            "📱 Telegram e '$contact' ke message pathate hobe."
        } catch (e: Exception) {
            "Telegram message error: ${e.message}"
        }
    }

    fun handleSmartNLP(text: String): String {
        // Parse intent from natural language
        return when {
            text.contains("open") || text.contains("kholo") -> {
                val appName = text.replace(Regex("(open|kholo|the|app)", RegexOption.IGNORE_CASE), "").trim()
                openApp(appName)
            }
            text.contains("type") || text.contains("likho") -> {
                val content = text.replace(Regex("(type|likho)", RegexOption.IGNORE_CASE), "").trim()
                typeText(content)
            }
            text.contains("click") || text.contains("tap") -> {
                val element = text.replace(Regex("(click|tap|koro)", RegexOption.IGNORE_CASE), "").trim()
                clickByText(element)
            }
            text.contains("scroll") -> scrollForward()
            else -> "Autopilot command bujhte parlam na."
        }
    }
}
