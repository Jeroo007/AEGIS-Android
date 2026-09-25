package com.aegis.safety.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSosRequest(
    @SerialName("latitude") val latitude: Double?,
    @SerialName("longitude") val longitude: Double?,
    @SerialName("accuracy_meters") val accuracyMeters: Float?,
    @SerialName("type") val type: String = "MANUAL_SOS",
    @SerialName("confidence") val confidence: Float? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("is_demo") val isDemo: Boolean = false
)

@Serializable
data class IncidentDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("type") val type: String,
    @SerialName("status") val status: String,
    @SerialName("severity") val severity: String,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("accuracy_meters") val accuracyMeters: Float? = null,
    @SerialName("confidence") val confidence: Float? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("resolved_at") val resolvedAt: Long? = null,
    @SerialName("assigned_patrol_id") val assignedPatrolId: String? = null,
    @SerialName("is_demo") val isDemo: Boolean = false
)

@Serializable
data class LocationUpdateRequest(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("accuracy_meters") val accuracyMeters: Float,
    @SerialName("incident_id") val incidentId: String? = null,
    @SerialName("timestamp") val timestamp: Long
)