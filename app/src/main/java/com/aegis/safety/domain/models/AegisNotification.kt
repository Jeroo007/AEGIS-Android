package com.aegis.safety.domain.models

data class AegisNotification(
    val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val incidentId: String? = null
)

enum class NotificationType {
    SOS_CREATED, DISTRESS_DETECTED, CONTACT_NOTIFIED, PATROL_ASSIGNED,
    PATROL_ARRIVING, INCIDENT_RESOLVED, SAFETY_TIMER_EXPIRED, SYSTEM
}