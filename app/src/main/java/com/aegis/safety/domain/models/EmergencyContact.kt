package com.aegis.safety.domain.models

data class EmergencyContact(
    val id: String,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String? = null,
    val priority: Int = 0,
    val verified: Boolean = false,
    val notifyOnSos: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)