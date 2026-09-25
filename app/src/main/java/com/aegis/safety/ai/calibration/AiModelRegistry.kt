package com.aegis.safety.ai.calibration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiModelRegistry @Inject constructor() {
    private val _version = MutableStateFlow(1)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun getTemperature(modelKey: String): Float = 1.5f
    fun getPrecision(modelKey: String): Float = 0.9f
    fun getRecall(modelKey: String): Float = 0.9f
}
