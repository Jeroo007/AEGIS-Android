package com.aegis.safety.ai.audio

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MelSpectrogram @Inject constructor() {
    fun compute(audio: FloatArray, numBands: Int = 64): Array<FloatArray> {
        return Array(1) { FloatArray(numBands) } // Placeholder
    }
}
