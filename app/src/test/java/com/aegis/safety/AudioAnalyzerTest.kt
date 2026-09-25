package com.aegis.safety

import com.aegis.safety.ai.audio.AudioDistressAnalyzer
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioAnalyzerTest {

    private val analyzer = AudioDistressAnalyzer()

    @Test
    fun `high frequency loud signal scores higher than silence`() {
        analyzer.reset()
        val sr = 16_000
        // Loud high-pitched scream-like
        val scream = ShortArray(sr / 2) { i ->
            val t = i.toDouble() / sr
            (0.9 * sin(2 * PI * 900 * t) * 32767).toInt().toShort()
        }
        val screamScore = analyzer.analyzePcm(scream, sr).distressProbability

        analyzer.reset()
        val silence = ShortArray(sr / 2)
        val silenceScore = analyzer.analyzePcm(silence, sr).distressProbability

        assertTrue("Scream should score higher than silence",
            screamScore > silenceScore)
    }
}