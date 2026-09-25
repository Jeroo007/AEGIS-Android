package com.aegis.safety.domain.models

data class SafetyTimer(
    val id: String,
    val userId: String,
    val durationMinutes: Int,
    val startedAt: Long,
    val expiresAt: Long,
    val reminderSent: Boolean = false,
    val secondReminderSent: Boolean = false,
    val escalatedToContact: Boolean = false,
    val escalatedToDispatcher: Boolean = false,
    val status: SafetyTimerStatus = SafetyTimerStatus.ACTIVE,
    val label: String? = null
)

enum class SafetyTimerStatus { ACTIVE, EXPIRED, COMPLETED, CANCELLED }