package com.aegis.safety.ai.monitoring

import com.aegis.safety.ai.audio.AudioDistressAnalyzer
import com.aegis.safety.ai.fusion.MultimodalFusionEngine
import com.aegis.safety.ai.vision.FallDetector
import com.aegis.safety.domain.models.AiScore
import com.aegis.safety.domain.models.RiskLevel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The brain of automatic detection (LITE build).
 *
 * Aggregates audio (AudioDistressAnalyzer) + motion (FallDetector) signals,
 * runs them through MultimodalFusionEngine, and — when the fused score
 * crosses the HIGH or CRITICAL threshold for the user's chosen sensitivity
 * — emits an [Escalation] for the app to open the confirmation prompt.
 *
 * LITE BUILD — no pose/vision path, no per-engine tags. Upgrade by replacing
 * this class with the full version once Tier-2 AI is integrated.
 *
 * CRITICAL INVARIANTS:
 *  - Manual SOS bypasses this coordinator entirely (handled in SosViewModel).
 *  - This class NEVER creates an incident directly. It only emits events.
 *  - Cooldowns and a rolling event log prevent notification storms.
 */
@Singleton
class AutoDetectionCoordinator @Inject constructor(
    private val audio: AudioDistressAnalyzer,
    private val fall: FallDetector,
    private val fusion: MultimodalFusionEngine
) {

    data class Escalation(
        val level: RiskLevel,
        val fusedScore: Float,
        val audioScore: Float,
        val motionScore: Float,
        val visionScore: Float,
        val audioEngine: String,
        val visionEngine: String,
        val timestamp: Long,
        val reason: String
    )

    data class State(
        val running: Boolean = false,
        val lastLevel: RiskLevel = RiskLevel.NORMAL,
        val lastScore: Float = 0f,
        val escalationsInLastHour: Int = 0
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _escalations = MutableSharedFlow<Escalation>(extraBufferCapacity = 4)
    val escalations: SharedFlow<Escalation> = _escalations.asSharedFlow()

    private var lastEscalationAt = 0L
    private val escalationTimestamps = ArrayDeque<Long>()

    // last-seen values so partial signal updates don't reset others to 0
    private var lastAudio = 0f
    private var lastMotion = 0f

    // Injected at service start via setConfig()
    var currentConfig: MonitoringConfig = MonitoringConfig()
        private set

    fun setConfig(cfg: MonitoringConfig) { currentConfig = cfg }

    fun setRunning(v: Boolean) { _state.value = _state.value.copy(running = v) }

    fun reset() {
        audio.reset()
        fall.reset()
        lastAudio = 0f
        lastMotion = 0f
        lastEscalationAt = 0L
        escalationTimestamps.clear()
        _state.value = State()
    }

    /** Feed a fresh audio PCM frame. Called from the monitoring service. */
    fun onAudioFrame(samples: ShortArray, sampleRateHz: Int) {
        val result = audio.analyzePcm(samples, sampleRateHz)
        lastAudio = result.distressProbability
        evaluate()
    }

    /** Feed a fresh IMU sample. Called from the monitoring service. */
    fun onImuSample(sample: FallDetector.ImuSample) {
        val det = fall.onSample(sample)
        lastMotion = det.probability
        evaluate()
    }

    private fun evaluate() {
        val cfg = currentConfig
        val (watchT, highT, critT) = cfg.toFusionThresholds()

        val input = AiScore(
            audioScore = lastAudio,
            visionScore = 0f,
            motionScore = lastMotion,
            explicitSos = false,
            locationContext = 0f,
            geofenceContext = 0f,
            signalConsistency = 1f
        )

        // MultimodalFusionEngine returns a fused score; we apply our own
        // sensitivity thresholds (independent of the engine's internal level)
        // so the user's Sensitivity setting has real effect.
        val fused = fusion.fuse(input)
        val level = classify(fused.fusedScore, watchT, highT, critT)

        _state.value = _state.value.copy(lastLevel = level, lastScore = fused.fusedScore)

        if (level == RiskLevel.NORMAL || level == RiskLevel.WATCH) return

        val now = System.currentTimeMillis()
        pruneEscalationWindow(now)

        if (now - lastEscalationAt < MIN_INTERVAL_MS) return
        if (escalationTimestamps.size >= MAX_PER_HOUR) {
            Timber.w("Auto-detection rate-limited (%d/hr)", escalationTimestamps.size)
            return
        }

        lastEscalationAt = now
        escalationTimestamps.addLast(now)
        _state.value = _state.value.copy(escalationsInLastHour = escalationTimestamps.size)

        val reason = buildString {
            append("fused="); append("%.2f".format(fused.fusedScore))
            append(" audio="); append("%.2f".format(lastAudio))
            append(" motion="); append("%.2f".format(lastMotion))
            append(" level="); append(level.name)
        }
        Timber.i("Auto-detection escalation: %s", reason)

        _escalations.tryEmit(
            Escalation(
                level = level,
                fusedScore = fused.fusedScore,
                audioScore = lastAudio,
                motionScore = lastMotion,
                visionScore = 0f,
                audioEngine = "heuristic",
                visionEngine = "n/a",
                timestamp = now,
                reason = reason
            )
        )
    }

    private fun classify(score: Float, watch: Float, high: Float, crit: Float): RiskLevel =
        when {
            score >= crit -> RiskLevel.CRITICAL
            score >= high -> RiskLevel.HIGH_PRIORITY
            score >= watch -> RiskLevel.WATCH
            else -> RiskLevel.NORMAL
        }

    private fun pruneEscalationWindow(now: Long) {
        while (escalationTimestamps.isNotEmpty() &&
            now - escalationTimestamps.first() > 3_600_000L
        ) escalationTimestamps.removeFirst()
        _state.value = _state.value.copy(escalationsInLastHour = escalationTimestamps.size)
    }

    companion object {
        private const val MIN_INTERVAL_MS = 20_000L      // 20 s between escalations
        private const val MAX_PER_HOUR = 12              // hard ceiling
    }
}