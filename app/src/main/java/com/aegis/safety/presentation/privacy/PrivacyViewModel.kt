package com.aegis.safety.presentation.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyUiState(
    val locationSharing: Boolean = true,
    val audioMonitoring: Boolean = false,
    val visionMonitoring: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val monitoringEnabled: Boolean = false
)

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { prefs.locationSharing.collect { v -> _state.update { it.copy(locationSharing = v) } } }
        viewModelScope.launch { prefs.audioMonitoring.collect { v -> _state.update { it.copy(audioMonitoring = v) } } }
        viewModelScope.launch { prefs.visionMonitoring.collect { v -> _state.update { it.copy(visionMonitoring = v) } } }
        viewModelScope.launch { prefs.analyticsEnabled.collect { v -> _state.update { it.copy(analyticsEnabled = v) } } }
        viewModelScope.launch { prefs.notificationsEnabled.collect { v -> _state.update { it.copy(notificationsEnabled = v) } } }
        viewModelScope.launch { prefs.monitoringEnabled.collect { v -> _state.update { it.copy(monitoringEnabled = v) } } }
    }

    fun setLocationSharing(v: Boolean) = viewModelScope.launch { prefs.setLocationSharing(v) }
    fun setAudioMonitoring(v: Boolean) = viewModelScope.launch { prefs.setAudioMonitoring(v) }
    fun setVisionMonitoring(v: Boolean) = viewModelScope.launch { prefs.setVisionMonitoring(v) }
    fun setAnalytics(v: Boolean) = viewModelScope.launch { prefs.setAnalyticsEnabled(v) }
    fun setNotifications(v: Boolean) = viewModelScope.launch { prefs.setNotificationsEnabled(v) }
    fun setMonitoring(v: Boolean) = viewModelScope.launch { prefs.setMonitoringEnabled(v) }
}