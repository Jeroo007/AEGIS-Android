package com.aegis.safety.ai.voice

interface WakeWordModel {
    val isReady: Boolean
    fun predict(features: Array<FloatArray>): Float
}
