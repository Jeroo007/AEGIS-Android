package com.aegis.safety.domain.models

data class AiScore(
    val audioScore: Float = 0f,
    val visionScore: Float = 0f,
    val motionScore: Float = 0f,
    val explicitSos: Boolean = false,
    val locationContext: Float = 0f,
    val geofenceContext: Float = 0f,
    val signalConsistency: Float = 1f,
    val fusedScore: Float = 0f,
    val level: RiskLevel = RiskLevel.NORMAL,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RiskLevel { NORMAL, WATCH, HIGH_PRIORITY, CRITICAL }