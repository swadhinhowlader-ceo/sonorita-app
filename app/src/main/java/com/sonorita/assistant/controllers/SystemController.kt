package com.sonorita.assistant.controllers

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Build

class SystemController(private val context: Context) {

    private var flashEnabled = false
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun toggleFlash(): String {
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.first()

            flashEnabled = !flashEnabled
            cameraManager.setTorchMode(cameraId, flashEnabled)

            if (flashEnabled) "🔦 Flash chalu!" else "🔦 Flash bondho!"
        } catch (e: Exception) {
            "Flash toggle korte parlam na: ${e.message}"
        }
    }

    fun toggleWifi(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val enabled = wifiManager.isWifiEnabled
            wifiManager.isWifiEnabled = !enabled
            if (!enabled) "📶 WiFi chalu!" else "📶 WiFi bondho!"
        } catch (e: Exception) {
            "WiFi toggle korte parlam na. Settings e giye manually koro."
        }
    }

    fun toggleBluetooth(): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter == null) {
                return "Bluetooth support nei ei device e."
            }

            if (adapter.isEnabled) {
                adapter.disable()
                "🔵 Bluetooth bondho!"
            } else {
                adapter.enable()
                "🔵 Bluetooth chalu!"
            }
        } catch (e: Exception) {
            "Bluetooth toggle korte parlam na: ${e.message}"
        }
    }

    fun handleVolume(text: String): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        return when {
            text.contains("up") || text.contains("barao") || text.contains("beshi") -> {
                val newVolume = (currentVolume + 1).coerceAtMost(maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                "🔊 Volume: ${newVolume * 100 / maxVolume}%"
            }
            text.contains("down") || text.contains("komo") || text.contains("kam") -> {
                val newVolume = (currentVolume - 1).coerceAtLeast(0)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                "🔉 Volume: ${newVolume * 100 / maxVolume}%"
            }
            text.contains("mute") -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                "🔇 Volume mute!"
            }
            else -> {
                "🔊 Current volume: ${currentVolume * 100 / maxVolume}%"
            }
        }
    }

    fun handleBrightness(text: String): String {
        return try {
            val resolver = context.contentResolver
            val current = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)

            when {
                text.contains("up") || text.contains("barao") || text.contains("beshi") -> {
                    val new = (current + 50).coerceAtMost(255)
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, new)
                    "💡 Brightness: ${new * 100 / 255}%"
                }
                text.contains("down") || text.contains("komo") || text.contains("kam") -> {
                    val new = (current - 50).coerceAtLeast(0)
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, new)
                    "💡 Brightness: ${new * 100 / 255}%"
                }
                else -> "💡 Current brightness: ${current * 100 / 255}%"
            }
        } catch (e: Exception) {
            "Brightness control korte parlam na. Settings e giye manually koro."
        }
    }

    fun lockDevice(): String {
        return "Screen lock korte device admin dorkar. Settings e giye enable koro."
    }

    fun unlockDevice(): String {
        return "Unlock korte face recognition use korbo. Settings e enable koro."
    }

    fun shutdownDevice(): String {
        return "Shutdown korte root access dorkar. Eta ei device e possible na."
    }

    fun rebootDevice(): String {
        return "Reboot korte root access dorkar. Eta ei device e possible na."
    }

    fun getBatteryStatus(): String {
        val batteryStatus = context.registerReceiver(null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))

        val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (scale > 0) (level * 100) / scale else -1

        val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == android.os.BatteryManager.BATTERY_STATUS_FULL

        return buildString {
            append("🔋 Battery: $percentage%")
            if (isCharging) append(" ⚡ Charging")
        }
    }

    fun setMeetingMode(): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        return "🔇 Meeting mode on! Shob sound off."
    }

    fun setDND(text: String): String {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        return if (text.contains("off") || text.contains("bondho")) {
            notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            "🔔 DND off! Notification asche."
        } else {
            notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
            "🔕 DND on! Shob notification bondho."
        }
    }

    fun setBusyMode(): String {
        // Auto-reply mode - implemented in SMS handler
        return "📱 Busy mode on! Auto-reply pathabe."
    }

    fun toggleHotspot(): String {
        return "Hotspot toggle korte direct API nei. Settings e giye manually koro."
    }

    fun toggleVPN(): String {
        val intent = Intent(Settings.ACTION_VPN_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "VPN settings khulchi..."
    }

    fun toggleNFC(): String {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "NFC settings khulchi..."
    }
}
