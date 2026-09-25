import os
import re

# Paths
BASE_DIR = r"d:\AEGIS\AEGIS-Android"
APP_DIR = os.path.join(BASE_DIR, "app/src/main/java/com/aegis/safety")
RES_DIR = os.path.join(BASE_DIR, "app/src/main/res")

def ensure_dir(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)

def create_file(rel_path, content):
    full_path = os.path.join(APP_DIR, rel_path)
    ensure_dir(full_path)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)

# 2. AiModelRegistry & TemperatureScaler
create_file("ai/calibration/AiModelRegistry.kt", """package com.aegis.safety.ai.calibration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiModelRegistry @Inject constructor() {
    private val _version = MutableStateFlow(1)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun getTemperature(modelKey: String): Float = 1.5f
    fun getPrecision(modelKey: String): Float = 0.9f
    fun getRecall(modelKey: String): Float = 0.9f
}
""")

create_file("ai/calibration/TemperatureScaler.kt", """package com.aegis.safety.ai.calibration

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.ln

@Singleton
class TemperatureScaler @Inject constructor() {
    fun scale(probability: Float, temperature: Float): Float {
        if (probability <= 0f) return 0f
        if (probability >= 1f) return 1f
        val logit = ln(probability / (1 - probability))
        val scaledLogit = logit / temperature
        return (1 / (1 + exp(-scaledLogit))).toFloat()
    }
    
    fun expectedCalibrationError(probs: List<Float>, labels: List<Int>, bins: Int = 10): Float {
        return 0f // Placeholder
    }
}
""")

# 3. MelSpectrogram
create_file("ai/audio/MelSpectrogram.kt", """package com.aegis.safety.ai.audio

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MelSpectrogram @Inject constructor() {
    fun compute(audio: FloatArray, numBands: Int = 64): Array<FloatArray> {
        return Array(1) { FloatArray(numBands) } // Placeholder
    }
}
""")

# 4. AudioClassifierTFLite
create_file("ai/audio/AudioClassifierTFLite.kt", """package com.aegis.safety.ai.audio

import androidx.annotation.WorkerThread
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class AudioClassifierTFLite @Inject constructor() {
    var isReady = false
        private set

    @WorkerThread
    fun predict(spectrogram: Array<FloatArray>): FloatArray? {
        if (!isReady) return null
        return floatArrayOf(0.1f, 0.9f)
    }
}
""")

# 5. PoseLandmark, PoseClassifierTFLite, PoseFallClassifier
create_file("ai/vision/PoseLandmark.kt", """package com.aegis.safety.ai.vision

data class PoseLandmark(val x: Float, val y: Float, val z: Float, val visibility: Float)
data class PoseFrame(val landmarks: List<PoseLandmark>) {
    companion object {
        const val NOSE = 0
        const val LEFT_EYE_INNER = 1
        // ...
    }
}
""")

create_file("ai/vision/PoseClassifierTFLite.kt", """package com.aegis.safety.ai.vision

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoseClassifierTFLite @Inject constructor() {
    fun predict(sequence: List<PoseFrame>): FloatArray? {
        return null // normal, fall, struggle
    }
}
""")

create_file("ai/vision/PoseFallClassifier.kt", """package com.aegis.safety.ai.vision

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoseFallClassifier @Inject constructor(
    private val tflite: PoseClassifierTFLite
) {
    fun processFrame(frame: PoseFrame): Float {
        return 0f
    }
}
""")

# 6. FusionConfig, ContextualPrior
create_file("ai/fusion/FusionConfig.kt", """package com.aegis.safety.ai.fusion

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
""")

create_file("ai/fusion/ContextualPrior.kt", """package com.aegis.safety.ai.fusion

data class ContextSnapshot(
    val isNight: Boolean = false,
    val inHighRiskGeofence: Boolean = false
)

class ContextualPrior {
    fun getPrior(snapshot: ContextSnapshot): Float {
        var prior = 1.0f
        if (snapshot.isNight) prior *= 1.25f
        if (snapshot.inHighRiskGeofence) prior *= 1.5f
        return prior
    }
}
""")

# 7. FederatedLearningClient
create_file("ai/federated/FederatedLearningClient.kt", """package com.aegis.safety.ai.federated

import javax.inject.Inject
import javax.inject.Singleton

interface FederatedLearningClient {
    fun uploadWeights()
}

@Singleton
class NoOpFederatedLearningClient @Inject constructor() : FederatedLearningClient {
    override fun uploadWeights() {
        // Does nothing
    }
}
""")

# 8. AudioCapture
create_file("core/audio/AudioCapture.kt", """package com.aegis.safety.core.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCapture @Inject constructor() {
    fun startStreaming(): Flow<ShortArray> = flow {
        // Stream 16kHz PCM
    }
}
""")

# 9. WakeWordModel, OnnxWakeWordModel, WakeWordDetector
create_file("ai/voice/WakeWordModel.kt", """package com.aegis.safety.ai.voice

interface WakeWordModel {
    val isReady: Boolean
    fun predict(features: Array<FloatArray>): Float
}
""")

create_file("ai/voice/OnnxWakeWordModel.kt", """package com.aegis.safety.ai.voice

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnnxWakeWordModel @Inject constructor(
    // private val melSpectrogram: MelSpectrogram
) : WakeWordModel {
    override val isReady: Boolean = false
    override fun predict(features: Array<FloatArray>): Float {
        return 0f
    }
}
""")

create_file("ai/voice/WakeWordDetector.kt", """package com.aegis.safety.ai.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class Detection(val confidence: Float, val timestamp: Long)

@Singleton
class WakeWordDetector @Inject constructor(
    private val model: WakeWordModel
) {
    val detections = MutableSharedFlow<Detection>()
    
    suspend fun processFrame(frame: ShortArray) {
        // ...
    }
}
""")

# 10. Voice Trigger Components
create_file("ai/voice/VoiceTriggerService.kt", """package com.aegis.safety.ai.voice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VoiceTriggerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
""")

create_file("ai/voice/VoiceTriggerReceiver.kt", """package com.aegis.safety.ai.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.aegis.safety.core.notifications.AegisNotifier

@AndroidEntryPoint
class VoiceTriggerReceiver : BroadcastReceiver() {
    @Inject lateinit var notifier: AegisNotifier
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.aegis.safety.WAKE_WORD_DETECTED") {
            notifier.showWakeWordDetected()
        }
    }
}
""")

create_file("ai/voice/VoiceTriggerViewModel.kt", """package com.aegis.safety.ai.voice

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class VoiceTriggerViewModel @Inject constructor() : ViewModel() {
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring = _isMonitoring.asStateFlow()
    
    val modelAvailable = true
    
    fun toggleMonitoring() {
        _isMonitoring.value = !_isMonitoring.value
    }
}
""")

create_file("core/notifications/AegisNotifier.kt", """package com.aegis.safety.core.notifications

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AegisNotifier @Inject constructor() {
    fun showWakeWordDetected() {}
    fun showPotentialEvent() {}
    fun showOfflineQueue() {}
}
""")

# 11. Live Journey Components
create_file("domain/models/Journey.kt", """package com.aegis.safety.domain.models

data class Journey(val id: String, val status: JourneyStatus)
enum class JourneyStatus { ACTIVE, COMPLETED, CANCELLED }
""")

create_file("core/database/entities/JourneyEntity.kt", """package com.aegis.safety.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey val id: String,
    val contactIds: String,
    val synced: Boolean
)
""")

create_file("data/remote/dto/JourneyDtos.kt", """package com.aegis.safety.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JourneyRequest(
    @SerialName("contact_ids") val contactIds: List<String>
)
""")

create_file("domain/repository/JourneyRepositoryInterface.kt", """package com.aegis.safety.domain.repository

import com.aegis.safety.domain.models.Journey

interface JourneyRepositoryInterface {
    suspend fun startJourney(contactIds: List<String>): Journey
}
""")

create_file("data/repository/JourneyRepository.kt", """package com.aegis.safety.data.repository

import com.aegis.safety.domain.repository.JourneyRepositoryInterface
import com.aegis.safety.domain.models.Journey
import com.aegis.safety.domain.models.JourneyStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor() : JourneyRepositoryInterface {
    override suspend fun startJourney(contactIds: List<String>): Journey {
        return Journey(UUID.randomUUID().toString(), JourneyStatus.ACTIVE)
    }
    
    suspend fun pushLocation(id: String, lat: Double, lng: Double) {}
}
""")

create_file("data/repository/JourneyTracker.kt", """package com.aegis.safety.data.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyTracker @Inject constructor() {
    fun start() {}
    fun stop() {}
}
""")

create_file("presentation/journey/JourneyViewModel.kt", """package com.aegis.safety.presentation.journey

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JourneyViewModel @Inject constructor() : ViewModel() {
    val canStart = true
    fun start() {}
    fun arrive() {}
    fun cancel() {}
}
""")

create_file("presentation/journey/JourneyScreen.kt", """package com.aegis.safety.presentation.journey

import androidx.compose.runtime.Composable

@Composable
fun JourneyScreen() {}
""")

create_file("presentation/journey/JourneyActiveScreen.kt", """package com.aegis.safety.presentation.journey

import androidx.compose.runtime.Composable

@Composable
fun JourneyActiveScreen() {}
""")

print("Python script executed.")
