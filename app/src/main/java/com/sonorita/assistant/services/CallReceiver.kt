package com.sonorita.assistant.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.sonorita.assistant.SonoritaApp

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown"
                // Broadcast incoming call
                val broadcast = Intent("com.sonorita.INCOMING_CALL").apply {
                    putExtra("number", phoneNumber)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(broadcast)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended
                val broadcast = Intent("com.sonorita.CALL_ENDED").apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(broadcast)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered
                val broadcast = Intent("com.sonorita.CALL_ANSWERED").apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(broadcast)
            }
        }
    }
}
