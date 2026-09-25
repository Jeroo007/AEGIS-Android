package com.aegis.safety.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val status: String,
    val severity: String,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val confidence: Float?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long?,
    val assignedPatrolId: String?,
    val isDemo: Boolean,
    val synced: Boolean = true
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String?,
    val priority: Int,
    val verified: Boolean,
    val notifyOnSos: Boolean,
    val createdAt: Long,
    val synced: Boolean = true
)

@Entity(tableName = "pending_events")
data class PendingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payloadJson: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null
)

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val incidentId: String?,
    val timestamp: Long,
    val synced: Boolean = false
)

@Entity(tableName = "safety_timers")
data class SafetyTimerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val durationMinutes: Int,
    val startedAt: Long,
    val expiresAt: Long,
    val reminderSent: Boolean,
    val secondReminderSent: Boolean,
    val escalatedToContact: Boolean,
    val escalatedToDispatcher: Boolean,
    val status: String,
    val label: String?
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val createdAt: Long,
    val read: Boolean,
    val incidentId: String?
)