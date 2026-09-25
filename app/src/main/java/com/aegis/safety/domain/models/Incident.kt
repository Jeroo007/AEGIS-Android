package com.aegis.safety.domain.models

data class Incident(
    val id: String,
    val userId: String,
    val type: IncidentType,
    val status: IncidentStatus,
    val severity: Severity,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val confidence: Float? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val assignedPatrolId: String? = null,
    val isDemo: Boolean = false
)

enum class IncidentType {
    MANUAL_SOS, SAFETY_TIMER, AUDIO_DISTRESS, VISION_FALL, VISION_STRUGGLE,
    MOTION_ANOMALY, MULTIMODAL, GEOFENCE_EXIT, UNKNOWN
}

enum class IncidentStatus {
    CREATED, ACKNOWLEDGED, DISPATCHED, PATROL_ASSIGNED,
    PATROL_EN_ROUTE, ON_SCENE, RESOLVED, CANCELLED, FALSE_POSITIVE
}

enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }