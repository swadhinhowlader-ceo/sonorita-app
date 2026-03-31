package com.sonorita.assistant.ai

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sonorita.assistant.ai.AIEngine
import java.util.Locale

class LocationIntelligence(
    private val context: Context,
    private val aiEngine: AIEngine
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    data class LocationInfo(
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val city: String,
        val country: String
    )

    data class NearbyPlace(
        val name: String,
        val type: String,
        val distance: String,
        val rating: String?
    )

    data class GeoFence(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float,
        val enterAction: String,
        val exitAction: String,
        val isActive: Boolean = true
    )

    private val geoFences = mutableListOf<GeoFence>()

    fun getCurrentLocation(callback: (LocationInfo?) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = addresses?.firstOrNull()

                    callback(LocationInfo(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address?.getAddressLine(0) ?: "Unknown",
                        city = address?.locality ?: "Unknown",
                        country = address?.countryName ?: "Unknown"
                    ))
                } else {
                    callback(null)
                }
            }.addOnFailureListener {
                callback(null)
            }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    suspend fun getNearbyPlaces(category: String, location: LocationInfo): String {
        return try {
            val response = aiEngine.query(
                "What are some nearby $category near coordinates ${location.latitude}, ${location.longitude} " +
                "in ${location.city}? List 5 options with brief descriptions.",
                emptyList()
            )
            "📍 Nearby $category:\n${response.content}"
        } catch (e: Exception) {
            "Nearby places error: ${e.message}"
        }
    }

    suspend fun getSmartSuggestion(location: LocationInfo): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        return try {
            val response = aiEngine.query(
                "Based on this location: ${location.address}, ${location.city} " +
                "at this time ($hour:00), what smart suggestions do you have? " +
                "Consider: food, transport, weather, nearby attractions. Be practical.",
                emptyList()
            )
            "📍 Smart Suggestions:\n${response.content}"
        } catch (e: Exception) {
            "Suggestion error: ${e.message}"
        }
    }

    suspend fun predictTraffic(from: String, to: String): String {
        return try {
            val response = aiEngine.query(
                "Predict traffic from '$from' to '$to'. " +
                "Estimate travel time by car, public transport, and walking. " +
                "Consider typical traffic patterns.",
                emptyList()
            )
            "🚗 Traffic Prediction:\n${response.content}"
        } catch (e: Exception) {
            "Traffic prediction error: ${e.message}"
        }
    }

    fun addGeoFence(name: String, lat: Double, lng: Double, radius: Float, enterAction: String, exitAction: String) {
        geoFences.add(GeoFence(name, lat, lng, radius, enterAction, exitAction))
    }

    fun checkGeoFences(currentLat: Double, currentLng: Double): List<String> {
        val alerts = mutableListOf<String>()

        geoFences.filter { it.isActive }.forEach { fence ->
            val distance = calculateDistance(currentLat, currentLng, fence.latitude, fence.longitude)

            if (distance <= fence.radiusMeters) {
                alerts.add("📍 Entering ${fence.name}: ${fence.enterAction}")
            }
        }

        return alerts
    }

    fun getGeoFenceList(): String {
        if (geoFences.isEmpty()) return "Kono geo-fence set nei."

        return buildString {
            appendLine("📍 Geo-Fences:")
            geoFences.forEach { fence ->
                val status = if (fence.isActive) "🟢" else "⚫"
                appendLine("$status ${fence.name}: Enter='${fence.enterAction}', Exit='${fence.exitAction}'")
            }
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    // Time-based location intelligence
    suspend fun getLocationBasedReminder(locationName: String, action: String): String {
        return "📍 Reminder set: When you reach '$locationName', $action"
    }
}
