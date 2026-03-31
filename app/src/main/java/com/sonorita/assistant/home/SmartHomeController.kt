package com.sonorita.assistant.home

import android.content.Context
import android.content.Intent

class SmartHomeController(private val context: Context) {

    data class Device(
        val name: String,
        val type: DeviceType,
        val room: String,
        val isOn: Boolean = false,
        val value: Int = 0 // brightness, temperature, etc.
    )

    enum class DeviceType {
        LIGHT, FAN, AC, TV, SPEAKER, LOCK, CAMERA, THERMOSTAT
    }

    data class Scene(
        val name: String,
        val description: String,
        val actions: List<DeviceAction>
    )

    data class DeviceAction(
        val deviceName: String,
        val action: String,
        val value: Int? = null
    )

    private val devices = mutableListOf<Device>()
    private val scenes = mutableListOf<Scene>()

    init {
        // Default scenes
        scenes.addAll(listOf(
            Scene(
                name = "Ghumir Mode",
                description = "Shob bondho, light dim, AC comfortable",
                actions = listOf(
                    DeviceAction("Main Light", "off"),
                    DeviceAction("AC", "on", 24),
                    DeviceAction("Fan", "on", 2),
                    DeviceAction("TV", "off")
                )
            ),
            Scene(
                name = "Morning Mode",
                description = "Lights on, news play",
                actions = listOf(
                    DeviceAction("Main Light", "on", 100),
                    DeviceAction("TV", "on"),
                    DeviceAction("AC", "off")
                )
            ),
            Scene(
                name = "Movie Mode",
                description = "Lights dim, TV on, volume up",
                actions = listOf(
                    DeviceAction("Main Light", "on", 20),
                    DeviceAction("TV", "on"),
                    DeviceAction("Speaker", "volume", 70)
                )
            ),
            Scene(
                name = "Party Mode",
                description = "Lights colorful, music loud",
                actions = listOf(
                    DeviceAction("Main Light", "color", 0),
                    DeviceAction("Speaker", "volume", 80)
                )
            ),
            Scene(
                name = "Work Mode",
                description = "Bright lights, silent, focused",
                actions = listOf(
                    DeviceAction("Main Light", "on", 100),
                    DeviceAction("Speaker", "off"),
                    DeviceAction("TV", "off")
                )
            )
        ))
    }

    fun addDevice(name: String, type: DeviceType, room: String) {
        devices.add(Device(name, type, room))
    }

    fun controlDevice(name: String, action: String, value: Int? = null): String {
        val device = devices.find { it.name.equals(name, ignoreCase = true) }
            ?: return "Device '$name' pai ni. Add koro first."

        return when (action.lowercase()) {
            "on" -> {
                val idx = devices.indexOf(device)
                devices[idx] = device.copy(isOn = true)
                "💡 ${device.name} chalu!"
            }
            "off" -> {
                val idx = devices.indexOf(device)
                devices[idx] = device.copy(isOn = false)
                "💡 ${device.name} bondho!"
            }
            "set" -> {
                val idx = devices.indexOf(device)
                devices[idx] = device.copy(isOn = true, value = value ?: 50)
                "💡 ${device.name} set to $value"
            }
            else -> "Unknown action: $action"
        }
    }

    fun activateScene(sceneName: String): String {
        val scene = scenes.find { it.name.equals(sceneName, ignoreCase = true) }
            ?: return "Scene '$sceneName' pai ni."

        val results = scene.actions.joinToString("\n") { action ->
            controlDevice(action.deviceName, action.action, action.value)
        }

        return "🏠 ${scene.name} activated!\n$results"
    }

    fun getDeviceList(): String {
        if (devices.isEmpty()) return "Kono smart device add nei. 'device add koro [name] [type]' bolo."

        return buildString {
            appendLine("🏠 Smart Home Devices:")
            devices.forEach { device ->
                val status = if (device.isOn) "🟢 ON" else "⚫ OFF"
                appendLine("• ${device.name} (${device.type}, ${device.room}) — $status")
            }
        }
    }

    fun getSceneList(): String {
        return buildString {
            appendLine("🏠 Available Scenes:")
            scenes.forEach { scene ->
                appendLine("• ${scene.name}: ${scene.description}")
            }
        }
    }

    // Auto-suggestions based on time
    fun getTimeBasedSuggestion(): String? {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 6..7 -> "☀️ Sokal hoyeche! 'morning mode' chalu koro?"
            in 22..23 -> "🌙 Ghumir shomoy! 'ghumir mode' chalu koro?"
            in 19..21 -> "🎬 Movie dekhbe? 'movie mode' chalu koro?"
            else -> null
        }
    }

    // IoT integration placeholder - would use MQTT/HTTP APIs
    fun connectToSmartHub(hubType: String, ipAddress: String): String {
        return "🔗 $hubType hub e connect hocche... (Implementation needs IoT SDK)"
    }
}
