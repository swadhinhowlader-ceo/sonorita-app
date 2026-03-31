package com.sonorita.assistant.services

import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder

class ScreenProjectionService : Service() {

    companion object {
        var mediaProjection: MediaProjection? = null
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun startProjection(resultCode: Int, data: Intent) {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)
    }

    fun stopProjection() {
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        stopProjection()
        super.onDestroy()
    }
}
