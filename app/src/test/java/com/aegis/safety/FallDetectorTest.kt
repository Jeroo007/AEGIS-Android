package com.aegis.safety

import com.aegis.safety.ai.vision.FallDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallDetectorTest {

    private val detector = FallDetector()

    @Test
    fun `free fall then impact then stillness confirms fall`() {
        val t0 = System.currentTimeMillis()
        // Free fall
        repeat(6) { i ->
            detector.onSample(FallDetector.ImuSample(0f, 0f, 1f, 0.2f, 0f, 0f, t0 + i * 20L))
        }
        // Impact
        repeat(3) { i ->
            detector.onSample(FallDetector.ImuSample(6f, 8f, 25f, 5f, 4f, 3f, t0 + 200L + i * 20L))
        }
        // Stillness
        var last = 0f
        repeat(60) { i ->
            last = detector.onSample(FallDetector.ImuSample(
                0.1f, 0.1f, 9.81f, 0.05f, 0.05f, 0.05f, t0 + 500L + i * 100L)).probability
        }
        assertTrue("Fall should be confirmed (prob >= 0.85)", last >= 0.85f)
    }

    @Test
    fun `normal walking stays idle`() {
        val t0 = System.currentTimeMillis()
        var last = 0f
        repeat(100) { i ->
            last = detector.onSample(FallDetector.ImuSample(
                0.5f, 0.5f, 9.9f, 0.3f, 0.3f, 0.2f, t0 + i * 100L)).probability
        }
        assertTrue("Walking should not confirm fall", last < 0.5f)
    }
}