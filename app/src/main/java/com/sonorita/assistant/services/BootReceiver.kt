package com.sonorita.assistant.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                // Start service after first unlock
                // We wait for USER_PRESENT before starting
                // to avoid starting before unlock
            }
            Intent.ACTION_USER_PRESENT -> {
                // First unlock after boot - start the service
                val serviceIntent = Intent(context, SonoritaService::class.java).apply {
                    action = SonoritaService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
