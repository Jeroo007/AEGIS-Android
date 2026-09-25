package com.aegis.safety.ai.voice

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class VoiceTriggerViewModel @Inject constructor() : ViewModel() {
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring = _isMonitoring.asStateFlow()
    
    val modelAvailable = true
    
    fun toggleMonitoring() {
        _isMonitoring.value = !_isMonitoring.value
    }
}
