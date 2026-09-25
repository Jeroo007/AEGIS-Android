package com.aegis.safety.ai.vision

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoseClassifierTFLite @Inject constructor() {
    fun predict(sequence: List<PoseFrame>): FloatArray? {
        return null // normal, fall, struggle
    }
}
