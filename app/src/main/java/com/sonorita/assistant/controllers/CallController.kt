package com.sonorita.assistant.controllers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.SmsManager

class CallController(private val context: Context) {

    fun handleCall(text: String): String {
        val contactName = extractContactName(text)
        val phoneNumber = findContactPhone(contactName) ?: extractPhoneNumber(text)

        return if (phoneNumber != null) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "📞 $contactName ke call korchi..."
            } catch (e: Exception) {
                "Call korte parlam na: ${e.message}"
            }
        } else {
            "Contact khuje pete parlam na. Number dao na."
        }
    }

    fun handleSMS(text: String): String {
        val parts = text.split("pathao", "send", "sms", ignoreCase = true)
        if (parts.size < 2) return "Kake pathabo? R ki likhbo?"

        val recipient = parts[0].trim()
        val message = parts.drop(1).joinToString(" ").trim()

        val phoneNumber = findContactPhone(recipient) ?: extractPhoneNumber(recipient)

        return if (phoneNumber != null && message.isNotEmpty()) {
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                "✉️ $recipient ke SMS pathalam!"
            } catch (e: Exception) {
                "SMS pathate parlam na: ${e.message}"
            }
        } else {
            "Number or message missing. Bolo kake ki pathabo."
        }
    }

    fun recordMeeting(): String {
        return try {
            val dir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS), "Sonorita/Meetings")
            dir.mkdirs()
            val file = java.io.File(dir, "meeting_${System.currentTimeMillis()}.mp3")
            "🎙️ Meeting recording shuru! File: ${file.absolutePath}"
        } catch (e: Exception) {
            "Meeting record korte parlam na: ${e.message}"
        }
    }

    fun getCallLog(): String {
        return try {
            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                null, null, null,
                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 10"
            )

            val calls = mutableListOf<String>()
            cursor?.use {
                while (it.moveToNext()) {
                    val number = it.getString(it.getColumnIndexOrThrow(android.provider.CallLog.Calls.NUMBER))
                    val type = it.getInt(it.getColumnIndexOrThrow(android.provider.CallLog.Calls.TYPE))
                    val date = it.getLong(it.getColumnIndexOrThrow(android.provider.CallLog.Calls.DATE))

                    val typeStr = when (type) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        android.provider.CallLog.Calls.MISSED_TYPE -> "Missed"
                        else -> "Unknown"
                    }
                    calls.add("$typeStr: $number (${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(date))})")
                }
            }

            if (calls.isEmpty()) "Kono call history nei."
            else "📞 Recent calls:\n" + calls.joinToString("\n")
        } catch (e: Exception) {
            "Call log porte parlam na: ${e.message}"
        }
    }

    private fun extractContactName(text: String): String {
        // Simple extraction - improve with NLP
        val words = text.replace(Regex("(call|dial|sms|message|pathao|koro)", RegexOption.IGNORE_CASE), "").trim()
        return words.split(" ").firstOrNull { it.length > 2 } ?: "Unknown"
    }

    private fun extractPhoneNumber(text: String): String? {
        val phonePattern = Regex("\\+?[\\d\\s()-]{7,15}")
        return phonePattern.find(text)?.value?.replace(Regex("[\\s()-]"), "")
    }

    private fun findContactPhone(name: String): String? {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
