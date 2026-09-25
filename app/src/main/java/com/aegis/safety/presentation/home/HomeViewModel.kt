package com.aegis.safety.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.core.datastore.UserPreferences
import com.aegis.safety.core.location.LocationProvider
import com.aegis.safety.core.security.LocalUserStore
import com.aegis.safety.domain.models.LocationSample
import com.aegis.safety.domain.repository.IncidentRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val greetingName: String = "Friend",
    val location: LocationSample? = null,
    val gpsStatus: String = "Unknown",
    val networkStatus: String = "Online",
    val monitoringStatus: Boolean = false,
    val pendingCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationProvider: LocationProvider,
    private val prefs: UserPreferences,
    private val incidentRepo: IncidentRepositoryInterface,
    localUserStore: LocalUserStore
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        val name = localUserStore.getCurrentUser()?.fullName?.substringBefore(" ") ?: "Friend"
        _state.update { it.copy(greetingName = name) }
        observeMonitoring()
        observePending()
        refreshLocation()
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val loc = locationProvider.getCurrentLocation()
            _state.update {
                it.copy(
                    location = loc,
                    gpsStatus = when {
                        loc == null -> "Unavailable"
                        loc.accuracyMeters <= 20f -> "Strong (±${loc.accuracyMeters.toInt()}m)"
                        loc.accuracyMeters <= 60f -> "Good (±${loc.accuracyMeters.toInt()}m)"
                        else -> "Weak (±${loc.accuracyMeters.toInt()}m)"
                    }
                )
            }
            loc?.let {
                prefs.saveLastLocation(it.latitude, it.longitude,
                    it.accuracyMeters, it.timestamp)
            }
        }
    }

    private fun observeMonitoring() {
        viewModelScope.launch {
            prefs.monitoringEnabled.collect { enabled ->
                _state.update { it.copy(monitoringStatus = enabled) }
            }
        }
    }

    private fun observePending() {
        viewModelScope.launch {
            incidentRepo.observePendingCount().collect { n ->
                _state.update { it.copy(pendingCount = n) }
            }
        }
    }

    fun setMonitoring(enabled: Boolean) {
        viewModelScope.launch { prefs.setMonitoringEnabled(enabled) }
    }
}