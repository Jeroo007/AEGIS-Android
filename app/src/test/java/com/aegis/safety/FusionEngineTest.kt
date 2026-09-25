package com.aegis.safety

import com.aegis.safety.ai.fusion.MultimodalFusionEngine
import com.aegis.safety.domain.models.AiScore
import com.aegis.safety.domain.models.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FusionEngineTest {

    private val engine = MultimodalFusionEngine()

    @Test
    fun `explicit SOS always escalates to CRITICAL`() {
        val result = engine.fuse(AiScore(
            audioScore = 0f, visionScore = 0f, motionScore = 0f,
            explicitSos = true, signalConsistency = 1f))
        assertEquals(RiskLevel.CRITICAL, result.level)
    }

    @Test
    fun `low signals stay NORMAL`() {
        val result = engine.fuse(AiScore(
            audioScore = 0.05f, visionScore = 0.05f, motionScore = 0.05f,
            explicitSos = false, signalConsistency = 1f))
        assertEquals(RiskLevel.NORMAL, result.level)
    }

    @Test
    fun `high multimodal scores reach CRITICAL`() {
        val result = engine.fuse(AiScore(
            audioScore = 0.9f, visionScore = 0.9f, motionScore = 0.9f,
            explicitSos = false, signalConsistency = 1f))
        assertEquals(RiskLevel.CRITICAL, result.level)
    }

    @Test
    fun `inconsistent signals dampen confidence`() {
        val high = engine.fuse(AiScore(audioScore = 1f, visionScore = 1f, motionScore = 1f,
            signalConsistency = 1f)).fusedScore
        val low = engine.fuse(AiScore(audioScore = 1f, visionScore = 1f, motionScore = 1f,
            signalConsistency = 0.1f)).fusedScore
        assertTrue("Dampened score should be lower", low < high)
    }
}