package com.aegis.safety.ai

import com.aegis.safety.ai.audio.AudioDistressAnalyzer
import com.aegis.safety.ai.fusion.MultimodalFusionEngine
import com.aegis.safety.ai.monitoring.AutoDetectionCoordinator
import com.aegis.safety.ai.vision.FallDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides @Singleton
    fun provideFusion() = MultimodalFusionEngine()

    @Provides @Singleton
    fun provideAudio() = AudioDistressAnalyzer()

    @Provides @Singleton
    fun provideFall() = FallDetector()

    @Provides @Singleton
    fun provideAutoDetectionCoordinator(
        audio: AudioDistressAnalyzer,
        fall: FallDetector,
        fusion: MultimodalFusionEngine
    ): AutoDetectionCoordinator = AutoDetectionCoordinator(audio, fall, fusion)
}