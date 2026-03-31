package com.sonorita.assistant.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.sonorita.assistant.R
import com.sonorita.assistant.SonoritaApp
import com.sonorita.assistant.services.SonoritaService

class FloatingBubbleService : Service() {

    companion object {
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingBubbleService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var params: WindowManager.LayoutParams

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createBubble()
        registerStateReceiver()
    }

    private fun createBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Create bubble view programmatically
        bubbleView = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble)
            val size = (56 * resources.displayMetrics.density).toInt()
            layoutParams = android.widget.FrameLayout.LayoutParams(size, size)
        }

        // Window params
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        // Touch handling
        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true
                        }
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        windowManager.updateViewLayout(bubbleView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Tap → open HomeActivity
                            val intent = Intent(this@FloatingBubbleService, HomeActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(bubbleView, params)
    }

    private fun registerStateReceiver() {
        val filter = android.content.IntentFilter().apply {
            addAction("com.sonorita.BUBBLE_STATE")
        }
        registerReceiver(stateReceiver, filter)
    }

    private val stateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getStringExtra("state") ?: return
            updateBubbleAppearance(state)
        }
    }

    private fun updateBubbleAppearance(state: String) {
        val color = when (state) {
            "active" -> 0xFF4CAF50.toInt()
            "sleep" -> 0xFF2196F3.toInt()
            "silent" -> 0xFFF44336.toInt()
            "thinking" -> 0xFFFFEB3B.toInt()
            "speaking" -> 0xFF00BCD4.toInt()
            "screen_view" -> 0xFF9C27B0.toInt()
            "gesture" -> 0xFFFF9800.toInt()
            else -> 0xFF9E9E9E.toInt()
        }

        (bubbleView as? ImageView)?.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
            setStroke(4, 0x40FFFFFF.toInt())
        }
    }

    override fun onDestroy() {
        isRunning = false
        try {
            unregisterReceiver(stateReceiver)
        } catch (e: Exception) {}
        try {
            windowManager.removeView(bubbleView)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
