package com.aegis.safety.domain.models

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val altitude: Double? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val provider: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean =
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        accuracyMeters > 0f && accuracyMeters < 10000f
}