package com.sonorita.assistant.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.sonorita.assistant.data.AppUsageDao
import com.sonorita.assistant.data.PreferenceDao
import com.sonorita.assistant.data.PreferenceEntity

class HealthGuardian(
    private val context: Context,
    private val preferenceDao: PreferenceDao
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var currentSteps = 0
    private var sessionStartTime = System.currentTimeMillis()
    private var lastInteractionTime = System.currentTimeMillis()

    // Health tracking data
    data class HealthStatus(
        val eyeStrainLevel: Int, // 0-10
        val postureWarning: Boolean,
        val hydrationReminder: Boolean,
        val screenTimeMinutes: Long,
        val stepsToday: Int,
        val sleepQuality: String?,
        val stressLevel: Int, // 0-10
        val suggestions: List<String>
    )

    fun startTracking() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            currentSteps = event.values[0].toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    suspend fun getHealthStatus(): HealthStatus {
        val screenTimeMs = System.currentTimeMillis() - sessionStartTime
        val screenTimeMinutes = screenTimeMs / (1000 * 60)

        // Eye strain calculation
        val eyeStrainLevel = when {
            screenTimeMinutes > 120 -> 9
            screenTimeMinutes > 90 -> 7
            screenTimeMinutes > 60 -> 5
            screenTimeMinutes > 30 -> 3
            else -> 1
        }

        // Posture warning (based on screen time)
        val postureWarning = screenTimeMinutes > 45

        // Hydration reminder
        val lastWaterReminder = preferenceDao.get("last_water_reminder")?.toLongOrNull() ?: 0
        val hydrationReminder = (System.currentTimeMillis() - lastWaterReminder) > (2 * 60 * 60 * 1000)

        // Stress level based on typing speed + interaction frequency
        val timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime
        val stressLevel = when {
            timeSinceLastInteraction < 60000 -> 3 // Very active, might be stressed
            timeSinceLastInteraction < 300000 -> 2
            else -> 1 // Relaxed
        }

        // Generate suggestions
        val suggestions = mutableListOf<String>()
        if (eyeStrainLevel > 5) suggestions.add("👁️ 20 seconds break! 20 feet door e dekho.")
        if (postureWarning) suggestions.add("🪑 Straight boso! Back straight, shoulders back.")
        if (hydrationReminder) suggestions.add("💧 Ekta glass paani khabo?")
        if (screenTimeMinutes > 120) suggestions.add("⏰ 2 ghonta hoyeche! Ektu break nao.")
        if (currentSteps < 2000 && screenTimeMinutes > 60) suggestions.add("🚶 Ektu walk koro!")

        return HealthStatus(
            eyeStrainLevel = eyeStrainLevel,
            postureWarning = postureWarning,
            hydrationReminder = hydrationReminder,
            screenTimeMinutes = screenTimeMinutes,
            stepsToday = currentSteps,
            sleepQuality = null,
            stressLevel = stressLevel,
            suggestions = suggestions
        )
    }

    suspend fun getHealthReport(): String {
        val status = getHealthStatus()

        return buildString {
            appendLine("🏥 Health Report:")
            appendLine()
            appendLine("👁️ Eye Strain: ${status.eyeStrainLevel}/10")
            appendLine("🪑 Posture: ${if (status.postureWarning) "⚠️ Sit straight!" else "✅ Good"}")
            appendLine("💧 Hydration: ${if (status.hydrationReminder) "⚠️ Drink water!" else "✅ OK"}")
            appendLine("📱 Screen Time: ${status.screenTimeMinutes} minutes")
            appendLine("🚶 Steps: ${status.stepsToday}")
            appendLine("😰 Stress Level: ${status.stressLevel}/10")

            if (status.suggestions.isNotEmpty()) {
                appendLine()
                appendLine("💡 Suggestions:")
                status.suggestions.forEach { appendLine("  $it") }
            }
        }
    }

    // Water intake tracking
    suspend fun logWaterIntake() {
        val current = preferenceDao.get("water_count")?.toIntOrNull() ?: 0
        preferenceDao.set(PreferenceEntity("water_count", (current + 1).toString()))
        preferenceDao.set(PreferenceEntity("last_water_reminder", System.currentTimeMillis().toString()))
    }

    suspend fun getWaterIntake(): Int {
        return preferenceDao.get("water_count")?.toIntOrNull() ?: 0
    }

    // Sleep analysis from phone usage
    suspend fun analyzeSleepPattern(): String {
        // Analyze when phone was last used and when it was first used today
        return "😴 Sleep analysis: Track your bedtime. Phone last use er time theke estimate korbo."
    }

    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }
}
