package com.aegis.safety.ai.vision

data class PoseLandmark(val x: Float, val y: Float, val z: Float, val visibility: Float)
data class PoseFrame(val landmarks: List<PoseLandmark>) {
    companion object {
        const val NOSE = 0
        const val LEFT_EYE_INNER = 1
        // ...
    }
}
