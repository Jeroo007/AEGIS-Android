package com.aegis.safety.ai.voice

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
