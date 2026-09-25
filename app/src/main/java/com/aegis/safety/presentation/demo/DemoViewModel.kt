package com.aegis.safety.presentation.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.ai.audio.AudioDistressAnalyzer
import com.aegis.safety.ai.fusion.MultimodalFusionEngine
import com.aegis.safety.ai.vision.FallDetector
import com.aegis.safety.core.location.LocationProvider
import com.aegis.safety.domain.models.AiScore
import com.aegis.safety.domain.models.IncidentType
import com.aegis.safety.domain.repository.IncidentRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DemoUiState(
    val lastStage: String = "Ready",
    val lastFusedScore: Float = 0f,
    val lastLevel: String = "NORMAL",
    val lastIncidentId: String? = null,
    val running: Boolean = false
)

@HiltViewModel
class DemoViewModel @Inject constructor(
    private val audio: AudioDistressAnalyzer,
    private val fall: FallDetector,
    private val fusion: MultimodalFusionEngine,
    private val locationProvider: LocationProvider,
    private val incidentRepo: IncidentRepositoryInterface
) : ViewModel() {

    private val _state = MutableStateFlow(DemoUiState())
    val state: StateFlow<DemoUiState> = _state.asStateFlow()

    fun simulateAudio() = runPipeline("audio")
    fun simulateFall() = runPipeline("fall")
    fun simulateMultimodal() = runPipeline("multimodal")

    fun simulateSos() {
        viewModelScope.launch {
            _state.update { it.copy(running = true, lastStage = "Simulating SOS") }
            val loc = locationProvider.getCurrentLocation()
            incidentRepo.createSos(loc, IncidentType.MANUAL_SOS, null, "Demo SOS", true)
                .onSuccess { inc ->
                    _state.update {
                        it.copy(running = false, lastStage = "Demo SOS created",
                            lastIncidentId = inc.id, lastLevel = "CRITICAL", lastFusedScore = 1f)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(running = false, lastStage = "Error: ${e.message}") }
                }
        }
    }

    private fun runPipeline(kind: String) {
        viewModelScope.launch {
            _state.update { it.copy(running = true, lastStage = "Generating signal") }

            val audioScore = when (kind) {
                "audio", "multimodal" -> synthesiseAudio()
                else -> 0.05f
            }
            _state.update { it.copy(lastStage = "Audio analysed: ${"%.2f".format(audioScore)}") }

            val motionScore = when (kind) {
                "fall", "multimodal" -> simulateFallSignal()
                else -> 0.05f
            }
            _state.update { it.copy(lastStage = "Motion analysed: ${"%.2f".format(motionScore)}") }

            val visionScore = if (kind == "multimodal") 0.7f else 0.05f

            val input = AiScore(
                audioScore = audioScore,
                visionScore = visionScore,
                motionScore = motionScore,
                explicitSos = false,
                locationContext = 0.4f,
                geofenceContext = 0.2f,
                signalConsistency = 0.9f
            )
            val fused = fusion.fuse(input)
            _state.update {
                it.copy(lastStage = "Fusion complete",
                    lastFusedScore = fused.fusedScore,
                    lastLevel = fused.level.name)
            }

            if (fused.level.name == "CRITICAL" || fused.level.name == "HIGH_PRIORITY") {
                val loc = locationProvider.getCurrentLocation()
                val type = when (kind) {
                    "audio" -> IncidentType.AUDIO_DISTRESS
                    "fall" -> IncidentType.VISION_FALL
                    else -> IncidentType.MULTIMODAL
                }
                incidentRepo.createSos(loc, type, fused.fusedScore, "Demo pipeline event", true)
                    .onSuccess { inc ->
                        _state.update { it.copy(lastStage = "Demo incident created", lastIncidentId = inc.id) }
                    }
                    .onFailure { e ->
                        _state.update { it.copy(lastStage = "Incident error: ${e.message}") }
                    }
            } else {
                _state.update { it.copy(lastStage = "No escalation (confidence below threshold)") }
            }
            _state.update { it.copy(running = false) }
        }
    }

    private fun synthesiseAudio(): Float {
        val sr = 16_000
        val durationMs = 600
        val n = sr * durationMs / 1000
        val samples = ShortArray(n)
        val baseFreq = 900.0
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val env = kotlin.math.sin(Math.PI * (i.toDouble() / n))
            val v = 0.85 * env * (
                kotlin.math.sin(2 * Math.PI * baseFreq * t) +
                0.4 * kotlin.math.sin(2 * Math.PI * baseFreq * 2.1 * t) +
                0.2 * (Math.random() * 2 - 1)
            )
            samples[i] = (v.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
        }
        return audio.analyzePcm(samples, sr).distressProbability
    }

    private fun simulateFallSignal(): Float {
        fall.reset()
        var last = 0f
        val start = System.currentTimeMillis()
        repeat(6) { i ->
            last = fall.onSample(FallDetector.ImuSample(0f, 0f, 1.0f, 0.2f, 0f, 0f, start + i * 20L)).probability
        }
        repeat(3) { i ->
            last = fall.onSample(FallDetector.ImuSample(6f, 8f, 25f, 5f, 4f, 3f, start + 200L + i * 20L)).probability
        }
        repeat(60) { i ->
            last = fall.onSample(FallDetector.ImuSample(0.1f, 0.1f, 9.81f, 0.05f, 0.05f, 0.05f, start + 500L + i * 100L)).probability
        }
        return last
    }
}