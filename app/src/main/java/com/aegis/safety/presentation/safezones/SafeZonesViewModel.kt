package com.aegis.safety.presentation.safezones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.core.location.LocationProvider
import com.aegis.safety.domain.models.SafeZone
import com.aegis.safety.domain.repository.SafeZoneRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SafeZonesUiState(
    val zones: List<SafeZone> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val userLat: Double? = null,
    val userLon: Double? = null,
    val radiusMeters: Float = DEFAULT_RADIUS_M
) {
    val noResults: Boolean get() = !loading && error == null && zones.isEmpty()

    companion object {
        const val DEFAULT_RADIUS_M = 15_000f
        const val MAX_RADIUS_M = 60_000f
    }
}

@HiltViewModel
class SafeZonesViewModel @Inject constructor(
    private val repo: SafeZoneRepositoryInterface,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SafeZonesUiState())
    val state: StateFlow<SafeZonesUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = load(_state.value.radiusMeters)

    fun expandRadius() {
        val next = (_state.value.radiusMeters * 2f)
            .coerceAtMost(SafeZonesUiState.MAX_RADIUS_M)
        load(next)
    }

    private fun load(radiusMeters: Float) {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true, error = null, radiusMeters = radiusMeters)
            }

            val loc = locationProvider.getCurrentLocation()
            if (loc == null) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "Could not get your location. Enable GPS and try again."
                    )
                }
                return@launch
            }

            _state.update { it.copy(userLat = loc.latitude, userLon = loc.longitude) }

            repo.nearest(
                lat = loc.latitude,
                lon = loc.longitude,
                limit = 40,
                maxDistanceMeters = radiusMeters
            ).onSuccess { zones ->
                Timber.i("Loaded %d nearby places within %.1f km",
                    zones.size, radiusMeters / 1000f)
                _state.update { it.copy(zones = zones, loading = false) }
            }.onFailure { e ->
                Timber.e(e, "Failed to load nearby places")
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Failed to load")
                }
            }
        }
    }
}