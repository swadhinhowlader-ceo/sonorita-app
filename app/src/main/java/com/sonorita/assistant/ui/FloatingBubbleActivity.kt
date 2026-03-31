package com.sonorita.assistant.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class FloatingBubbleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This activity is transparent and handles the floating bubble tap
        // Tapping the bubble opens HomeActivity
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
