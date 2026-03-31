package com.sonorita.assistant.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.displayOriginatingAddress ?: "Unknown"
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }

        // Broadcast SMS received
        val broadcast = Intent("com.sonorita.SMS_RECEIVED").apply {
            putExtra("sender", sender)
            putExtra("body", body)
            setPackage(context.packageName)
        }
        context.sendBroadcast(broadcast)
    }
}
