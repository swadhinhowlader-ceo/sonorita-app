package com.sonorita.assistant.services

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder

class ClipboardMonitorService : Service() {

    private lateinit var clipboardManager: ClipboardManager
    private var lastClipText: String? = null

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip
        val text = clip?.getItemAt(0)?.text?.toString()

        if (text != null && text != lastClipText) {
            lastClipText = text

            // Broadcast clipboard change
            val intent = Intent("com.sonorita.CLIPBOARD_CHANGED").apply {
                putExtra("text", text)
                setPackage(packageName)
            }
            sendBroadcast(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(listener)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(listener)
        super.onDestroy()
    }
}
