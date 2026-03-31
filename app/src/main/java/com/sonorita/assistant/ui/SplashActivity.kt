package com.sonorita.assistant.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.sonorita.assistant.databinding.ActivitySplashBinding
import com.sonorita.assistant.services.SonoritaService
import com.sonorita.assistant.utils.PermissionHelper

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check permissions and start
        handler.postDelayed({
            if (PermissionHelper.hasAllPermissions(this)) {
                startMainApp()
            } else {
                startActivity(Intent(this, PermissionActivity::class.java))
            }
            finish()
        }, 2000)
    }

    private fun startMainApp() {
        // Start background service
        SonoritaService.startService(this)

        // Start main activity
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
