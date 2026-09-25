package com.aegis.safety.ai.vision

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
