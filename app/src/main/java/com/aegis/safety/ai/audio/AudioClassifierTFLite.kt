package com.aegis.safety.ai.audio

import androidx.annotation.WorkerThread
import javax.inject.Inject
import javax.inject.Singleton

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
