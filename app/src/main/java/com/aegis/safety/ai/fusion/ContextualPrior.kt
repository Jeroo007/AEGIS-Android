package com.aegis.safety.ai.fusion

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
