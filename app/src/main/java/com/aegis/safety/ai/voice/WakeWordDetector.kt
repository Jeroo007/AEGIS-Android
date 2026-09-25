package com.aegis.safety.ai.voice

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
