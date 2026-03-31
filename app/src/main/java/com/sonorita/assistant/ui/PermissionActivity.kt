package com.sonorita.assistant.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.sonorita.assistant.databinding.ActivityPermissionBinding
import com.sonorita.assistant.utils.PermissionHelper
import com.sonorita.assistant.services.SonoritaService

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updatePermissionStatus()
        if (PermissionHelper.hasAllPermissions(this)) {
            startApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updatePermissionStatus()
    }

    private fun setupUI() {
        binding.requestPermissionsButton.setOnClickListener {
            PermissionHelper.requestAllPermissions(this, permissionLauncher)
        }

        binding.continueButton.setOnClickListener {
            if (PermissionHelper.hasAllPermissions(this)) {
                startApp()
            } else {
                android.widget.Toast.makeText(this, "Please grant all permissions first", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        binding.batteryOptButton.setOnClickListener {
            PermissionHelper.requestBatteryOptimizationExemption(this)
        }

        binding.overlayButton.setOnClickListener {
            PermissionHelper.requestOverlayPermission(this)
        }

        binding.usageStatsButton.setOnClickListener {
            PermissionHelper.requestUsageStatsPermission(this)
        }

        binding.accessibilityButton.setOnClickListener {
            PermissionHelper.requestAccessibilityPermission(this)
        }
    }

    private fun updatePermissionStatus() {
        binding.audioPermissionStatus.text = if (PermissionHelper.hasAudioPermission(this)) "✅" else "❌"
        binding.phonePermissionStatus.text = if (PermissionHelper.hasPhonePermission(this)) "✅" else "❌"
        binding.smsPermissionStatus.text = if (PermissionHelper.hasSMSPermission(this)) "✅" else "❌"
        binding.storagePermissionStatus.text = if (PermissionHelper.hasStoragePermission(this)) "✅" else "❌"
        binding.cameraPermissionStatus.text = if (PermissionHelper.hasCameraPermission(this)) "✅" else "❌"
        binding.locationPermissionStatus.text = if (PermissionHelper.hasLocationPermission(this)) "✅" else "❌"
        binding.overlayPermissionStatus.text = if (PermissionHelper.hasOverlayPermission(this)) "✅" else "❌"
        binding.notificationPermissionStatus.text = if (PermissionHelper.hasNotificationPermission(this)) "✅" else "❌"
    }

    private fun startApp() {
        SonoritaService.startService(this)
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
