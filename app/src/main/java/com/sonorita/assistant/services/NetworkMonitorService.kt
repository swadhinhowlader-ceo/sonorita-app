package com.sonorita.assistant.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class NetworkMonitorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    // Network monitoring is handled via NetworkController
    // This service exists for foreground service binding
}
