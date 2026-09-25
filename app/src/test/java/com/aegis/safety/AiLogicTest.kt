package com.aegis.safety

import com.aegis.safety.ai.calibration.TemperatureScaler
import com.aegis.safety.ai.fusion.MultimodalFusionEngine
import com.aegis.safety.domain.models.AiScore
import com.aegis.safety.domain.models.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

class AiLogicTest {

    @Test
    fun testTemperatureScalerDownScaling() {
        val scaler = TemperatureScaler()
        val result = scaler.scale(0.9f, 2.0f)
        // Check if downscaled correctly
        assertEquals(true, result < 0.9f)
    }

    @Test
    fun testMultimodalFusionEngineForcingCritical() {
        val engine = MultimodalFusionEngine()
        val result = engine.fuse(AiScore(explicitSos = true))
        assertEquals(RiskLevel.CRITICAL, result.level)
    }
}
