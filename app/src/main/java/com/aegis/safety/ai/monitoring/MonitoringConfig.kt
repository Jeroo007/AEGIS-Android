package com.aegis.safety.ai.monitoring

/**
 * User-facing knobs for automatic detection.
 *
 * Sensitivity maps to fusion thresholds:
 *   LOW    → WATCH=0.55, HIGH=0.75, CRITICAL=0.90  (fewer alerts, more missed)
 *   MEDIUM → WATCH=0.40, HIGH=0.65, CRITICAL=0.85  (default)
 *   HIGH   → WATCH=0.30, HIGH=0.55, CRITICAL=0.75  (more alerts, more false positives)
 *
 * Confirmation window is the human-in-the-loop delay before escalation.
 */
data class MonitoringConfig(
    val enabled: Boolean = false,
    val audio: Boolean = true,
    val motion: Boolean = true,
    val vision: Boolean = false,
    val sensitivity: Sensitivity = Sensitivity.MEDIUM,
    val confirmationSeconds: Int = 15,
    val notifyOnWatch: Boolean = false,
    val onlyWhenCharging: Boolean = false,
    val quietHours: Pair<Int, Int>? = null   // e.g. (23, 6)
) {
    enum class Sensitivity { LOW, MEDIUM, HIGH }

    fun toFusionThresholds(): Triple<Float, Float, Float> = when (sensitivity) {
        Sensitivity.LOW -> Triple(0.55f, 0.75f, 0.90f)
        Sensitivity.MEDIUM -> Triple(0.40f, 0.65f, 0.85f)
        Sensitivity.HIGH -> Triple(0.30f, 0.55f, 0.75f)
    }

    fun isQuietHour(hour: Int): Boolean {
        val (start, end) = quietHours ?: return false
        return if (start <= end) hour in start until end
        else hour >= start || hour < end
    }
}