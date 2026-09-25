package com.aegis.safety.ai.fusion

import com.aegis.safety.domain.models.AiScore
import com.aegis.safety.domain.models.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configurable multimodal fusion engine.
 * Weights are configurable — never hardcode in callers.
 */
@Singleton
class MultimodalFusionEngine @Inject constructor() {
    private val _config = MutableStateFlow(FusionConfig())
    val config: StateFlow<FusionConfig> = _config.asStateFlow()
    fun configure(newConfig: FusionConfig) {
        _config.value = newConfig
    }
    fun fuse(input: AiScore, context: ContextSnapshot = ContextSnapshot()): RiskLevel {
        if (input.explicitSos) return RiskLevel.CRITICAL
        return RiskLevel.NORMAL
    }


    data class Weights(
        val audio: Float = 0.40f,
        val vision: Float = 0.40f,
        val motion: Float = 0.20f,
        val explicitSos: Float = 0.20f,
        val context: Float = 0.05f
    )

    private var weights = Weights()

    fun configure(newWeights: Weights) { weights = newWeights }
    fun currentWeights(): Weights = weights

    fun fuse(input: AiScore): AiScore {
        require(input.audioScore in 0f..1f) { "audioScore out of range" }
        require(input.visionScore in 0f..1f) { "visionScore out of range" }
        require(input.motionScore in 0f..1f) { "motionScore out of range" }

        val contextScore = (input.locationContext + input.geofenceContext) / 2f
        val explicit = if (input.explicitSos) 1f else 0f

        val raw = weights.audio * input.audioScore +
            weights.vision * input.visionScore +
            weights.motion * input.motionScore +
            weights.explicitSos * explicit +
            weights.context * contextScore

        // Apply signal consistency penalty (inconsistent signals dampen confidence)
        val consistency = input.signalConsistency.coerceIn(0f, 1f)
        val fused = (raw * consistency).coerceIn(0f, 1f)

        return input.copy(
            explicitSos = input.explicitSos,
            fusedScore = fused,
            level = classify(fused, input.explicitSos)
        )
    }

    /**
     * Manual SOS ALWAYS escalates to CRITICAL regardless of other scores.
     * This is a non-negotiable safety principle.
     */
    private fun classify(score: Float, explicitSos: Boolean): RiskLevel {
        if (explicitSos) return RiskLevel.CRITICAL
        return when {
            score >= 0.85f -> RiskLevel.CRITICAL
            score >= 0.65f -> RiskLevel.HIGH_PRIORITY
            score >= 0.40f -> RiskLevel.WATCH
            else -> RiskLevel.NORMAL
        }
    }
}