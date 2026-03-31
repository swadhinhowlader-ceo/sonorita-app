package com.sonorita.assistant.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class GestureDetectionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    // Gesture detection is handled in ARController and CameraX
    // This service keeps the foreground service type for camera
}
