package com.aegis.safety.ai.fusion

import kotlinx.serialization.Serializable

@Serializable
data class FusionConfig(
    val weightAudio: Float = 0.28f,
    val weightVision: Float = 0.27f,
    val weightMotion: Float = 0.15f,
    val weightExplicitSos: Float = 0.20f,
    val weightContext: Float = 0.10f,
    val thresholdLow: Float = 0.40f,
    val thresholdHigh: Float = 0.65f,
    val thresholdCritical: Float = 0.85f,
    val cooldownSeconds: Int = 15,
    val highRiskGeofenceMultiplier: Float = 1.5f,
    val nightMultiplier: Float = 1.25f,
    val userFpRate: Float = 0.0f
) {
    fun normalizedWeights(): FusionConfig {
        val sum = weightAudio + weightVision + weightMotion + weightExplicitSos + weightContext
        return this.copy(
            weightAudio = weightAudio / sum,
            weightVision = weightVision / sum,
            weightMotion = weightMotion / sum,
            weightExplicitSos = weightExplicitSos / sum,
            weightContext = weightContext / sum
        )
    }
}
