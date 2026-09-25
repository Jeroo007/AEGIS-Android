package com.aegis.safety.domain.models

data class SafeZone(
    val id: String,
    val name: String,
    val type: SafeZoneType,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val phoneNumber: String? = null,
    val address: String? = null,
    val open24h: Boolean = false,
    val distanceMeters: Float? = null,
    val etaMinutes: Int? = null,
    val verified: Boolean = false
)

enum class SafeZoneType {
    POLICE_STATION, HOSPITAL, SECURITY_OFFICE, CAMPUS_SECURITY,
    PATROL_BASE, VERIFIED_PUBLIC, TRANSIT, PARKING, OTHER
}