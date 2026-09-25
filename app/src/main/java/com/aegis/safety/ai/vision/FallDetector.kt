package com.aegis.safety.ai.vision

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Motion-based fall / anomaly detector.
 *
 * Uses accelerometer + gyroscope samples (provided by Android sensors via
 * the caller). Real algorithm: free-fall detection followed by impact spike
 * and post-impact stillness. This is the standard multi-phase fall model.
 *
 * NEVER identifies people. NEVER performs face recognition.
 */
@Singleton
class FallDetector @Inject constructor() {

    data class ImuSample(
        val ax: Float, val ay: Float, val az: Float,
        val gx: Float, val gy: Float, val gz: Float,
        val timestamp: Long
    )

    data class Detection(
        val probability: Float,
        val phase: Phase,
        val timestamp: Long
    )

    enum class Phase { IDLE, FREE_FALL, IMPACT, POST_IMPACT_STILL, CONFIRMED }

    private val _state = MutableStateFlow<Detection?>(null)
    val state: Flow<Detection?> = _state.asStateFlow()

    private var phase = Phase.IDLE
    private var freeFallStart = 0L
    private var impactAt = 0L
    private var lastSampleAt = 0L
    private var gravityBaseline = 9.81f

    fun onSample(s: ImuSample): Detection {
        lastSampleAt = s.timestamp

        val magnitude = sqrt(s.ax * s.ax + s.ay * s.ay + s.az * s.az)
        val gyroMag = sqrt(s.gx * s.gx + s.gy * s.gy + s.gz * s.gz)

        // Adapt baseline slowly
        gravityBaseline = 0.98f * gravityBaseline + 0.02f * magnitude

        val probability: Float
        when (phase) {
            Phase.IDLE -> {
                if (magnitude < FREEFALL_THRESHOLD) {
                    phase = Phase.FREE_FALL
                    freeFallStart = s.timestamp
                }
                probability = 0.05f
            }
            Phase.FREE_FALL -> {
                if (magnitude > IMPACT_THRESHOLD && gyroMag > GYRO_IMPACT_THRESHOLD) {
                    phase = Phase.IMPACT
                    impactAt = s.timestamp
                } else if (s.timestamp - freeFallStart > 1500) {
                    phase = Phase.IDLE
                }
                probability = 0.35f
            }
            Phase.IMPACT -> {
                if (s.timestamp - impactAt > POST_IMPACT_WINDOW_MS) {
                    phase = Phase.POST_IMPACT_STILL
                    probability = 0.65f
                } else {
                    probability = 0.55f
                }
            }
            Phase.POST_IMPACT_STILL -> {
                val still = abs(magnitude - gravityBaseline) < STILLNESS_TOLERANCE
                probability = if (still) 0.90f else 0.60f
                if (still) phase = Phase.CONFIRMED
                if (s.timestamp - impactAt > 8000) phase = Phase.IDLE
            }
            Phase.CONFIRMED -> {
                probability = 0.92f
                if (s.timestamp - impactAt > 15_000) phase = Phase.IDLE
            }
        }

        val d = Detection(probability, phase, s.timestamp)
        _state.value = d
        return d
    }

    fun reset() {
        phase = Phase.IDLE
        freeFallStart = 0
        impactAt = 0
        lastSampleAt = 0
        _state.value = null
    }

    companion object {
        private const val FREEFALL_THRESHOLD = 3.5f
        private const val IMPACT_THRESHOLD = 20f
        private const val GYRO_IMPACT_THRESHOLD = 3f
        private const val POST_IMPACT_WINDOW_MS = 1500L
        private const val STILLNESS_TOLERANCE = 2.0f
    }
}