package com.aegis.safety.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.core.location.LocationProvider
import com.aegis.safety.domain.models.LocationSample
import com.aegis.safety.domain.models.SafeZone
import com.aegis.safety.domain.repository.SafeZoneRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveLocationUiState(
    val current: LocationSample? = null,
    val history: List<LocationSample> = emptyList(),
    val safeZones: List<SafeZone> = emptyList(),
    val loadingZones: Boolean = false
)

@HiltViewModel
class LiveLocationViewModel @Inject constructor(
    private val provider: LocationProvider,
    private val safeZoneRepo: SafeZoneRepositoryInterface
) : ViewModel() {

    private val _state = MutableStateFlow(LiveLocationUiState())
    val state: StateFlow<LiveLocationUiState> = _state.asStateFlow()

    private var lastZonesLat: Double? = null
    private var lastZonesLon: Double? = null

    init {
        // Show bundled zones right away (using Chennai as fallback center)
        val initial = safeZoneRepo.getBundled(13.0827, 80.2707, 12)
        _state.update { it.copy(safeZones = initial) }

        viewModelScope.launch {
            provider.locationUpdates().collect { sample ->
                _state.update { s ->
                    s.copy(current = sample, history = (s.history + sample).takeLast(50))
                }
                maybeRefreshZones(sample.latitude, sample.longitude)
            }
        }
    }

    fun manualRefreshZones() {
        lastZonesLat = null
        val loc = _state.value.current
        if (loc != null) {
            maybeRefreshZones(loc.latitude, loc.longitude, force = true)
        } else {
            // No GPS yet — refresh against the fallback center
            refreshFromNetwork(13.0827, 80.2707)
        }
    }

    private fun maybeRefreshZones(lat: Double, lon: Double, force: Boolean = false) {
        val lastLat = lastZonesLat
        val lastLon = lastZonesLon
        if (!force && lastLat != null && lastLon != null) {
            val dLat = kotlin.math.abs(lat - lastLat)
            val dLon = kotlin.math.abs(lon - lastLon)
            if (dLat < 0.005 && dLon < 0.005) return
        }
        lastZonesLat = lat
        lastZonesLon = lon

        // Show bundled instantly for the new location
        val bundled = safeZoneRepo.getBundled(lat, lon, 12)
        _state.update { it.copy(safeZones = bundled, loadingZones = true) }

        refreshFromNetwork(lat, lon)
    }

    private fun refreshFromNetwork(lat: Double, lon: Double) {
        viewModelScope.launch {
            safeZoneRepo.nearest(lat, lon, 12)
                .onSuccess { zones ->
                    _state.update {
                        it.copy(safeZones = zones, loadingZones = false)
                    }
                }
                .onFailure {
                    _state.update { it.copy(loadingZones = false) }
                }
        }
    }
}