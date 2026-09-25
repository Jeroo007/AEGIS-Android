package com.aegis.safety.presentation.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.core.location.LocationProvider
import com.aegis.safety.core.security.LocalUserStore
import com.aegis.safety.core.sms.SmsSender
import com.aegis.safety.domain.models.EmergencyContact
import com.aegis.safety.domain.models.Incident
import com.aegis.safety.domain.models.IncidentType
import com.aegis.safety.domain.models.LocationSample
import com.aegis.safety.domain.models.SafeZone
import com.aegis.safety.domain.repository.EmergencyContactRepositoryInterface
import com.aegis.safety.domain.repository.IncidentRepositoryInterface
import com.aegis.safety.domain.repository.LocationRepositoryInterface
import com.aegis.safety.domain.repository.SafeZoneRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

sealed interface SosPhase {
    data class Countdown(val remaining: Int) : SosPhase
    data object Sending : SosPhase
    data class Active(val incident: Incident) : SosPhase
    data class Failed(val reason: String) : SosPhase
    data object Cancelled : SosPhase
}

data class SosUiState(
    val phase: SosPhase = SosPhase.Countdown(5),
    val isDemo: Boolean = false,
    val nearestSafeZones: List<SafeZone> = emptyList(),
    val loadingSafeZones: Boolean = false,
    val smsSentCount: Int = 0,
    val smsTotalContacts: Int = 0,
    val smsAttempted: Boolean = false
)

@HiltViewModel
class SosViewModel @Inject constructor(
    private val locationProvider: LocationProvider,
    private val locationRepo: LocationRepositoryInterface,
    private val incidentRepo: IncidentRepositoryInterface,
    private val safeZoneRepo: SafeZoneRepositoryInterface,
    private val contactRepo: EmergencyContactRepositoryInterface,
    private val smsSender: SmsSender,
    private val localUserStore: LocalUserStore
) : ViewModel() {

    private val _state = MutableStateFlow(SosUiState())
    val state: StateFlow<SosUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private var trackingJob: Job? = null
    private var activeIncidentId: String? = null

    fun startCountdown(isDemo: Boolean = false) {
        if (countdownJob?.isActive == true) return
        _state.value = SosUiState(phase = SosPhase.Countdown(5), isDemo = isDemo)
        countdownJob = viewModelScope.launch {
            var remaining = 5
            while (remaining > 0) {
                _state.update { it.copy(phase = SosPhase.Countdown(remaining)) }
                delay(1000)
                remaining--
            }
            triggerSos()
        }
    }

    fun cancel() {
        countdownJob?.cancel()
        trackingJob?.cancel()
        activeIncidentId?.let { id ->
            viewModelScope.launch { incidentRepo.cancelSos(id) }
        }
        activeIncidentId = null
        _state.update { it.copy(phase = SosPhase.Cancelled) }
    }

    private fun triggerSos() {
        viewModelScope.launch {
            _state.update { it.copy(phase = SosPhase.Sending) }
            val loc = locationProvider.getCurrentLocation()
            val result = incidentRepo.createSos(
                location = loc,
                type = IncidentType.MANUAL_SOS,
                confidence = null,
                notes = null,
                isDemo = _state.value.isDemo
            )
            result.onSuccess { incident ->
                activeIncidentId = incident.id
                _state.update { it.copy(phase = SosPhase.Active(incident)) }
                startLiveTracking(incident.id)
                loadNearestSafeZones(loc?.latitude, loc?.longitude)
                notifyContactsViaSms(incident, loc)
            }.onFailure { e ->
                _state.update {
                    it.copy(phase = SosPhase.Failed(e.message ?: "Unknown error"))
                }
            }
        }
    }

    /**
     * Sends SOS SMS to all emergency contacts.
     * Reads directly from the DB (getAllSnapshot) — reliable, no flow timing issues.
     */
    private fun notifyContactsViaSms(incident: Incident, location: LocationSample?) {
        viewModelScope.launch {
            _state.update { it.copy(smsAttempted = true) }

            val contacts: List<EmergencyContact> = runCatching {
                contactRepo.getAllSnapshot()
            }.getOrElse {
                Timber.w(it, "Could not read contacts from DB")
                emptyList()
            }

            val notifyList = contacts.filter {
                it.notifyOnSos && it.phoneNumber.isNotBlank()
            }
            Timber.i("SOS: ${contacts.size} contacts, ${notifyList.size} to notify")
            _state.update { it.copy(smsTotalContacts = notifyList.size) }

            if (notifyList.isEmpty()) return@launch

            val senderName = localUserStore.getCurrentUser()?.fullName ?: "AEGIS user"
            val sent = withContext(Dispatchers.IO) {
                smsSender.sendSosSms(notifyList, incident, location, senderName)
            }
            _state.update { it.copy(smsSentCount = sent) }
        }
    }

    /**
     * Shows bundled zones immediately, then upgrades to OSM results
     * if the network request succeeds within a few seconds.
     */
    private fun loadNearestSafeZones(lat: Double?, lon: Double?) {
        val useLat = lat ?: 13.0827
        val useLon = lon ?: 80.2707

        // 1. Show bundled results instantly
        val bundled = safeZoneRepo.getBundled(useLat, useLon, 6)
        _state.update { it.copy(nearestSafeZones = bundled, loadingSafeZones = true) }

        // 2. Try OSM in the background
        viewModelScope.launch {
            safeZoneRepo.nearest(useLat, useLon, 6)
                .onSuccess { zones ->
                    if (zones.isNotEmpty()) {
                        _state.update { it.copy(nearestSafeZones = zones, loadingSafeZones = false) }
                    } else {
                        _state.update { it.copy(loadingSafeZones = false) }
                    }
                }
                .onFailure {
                    _state.update { it.copy(loadingSafeZones = false) }
                }
        }
    }

    private fun startLiveTracking(incidentId: String) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            locationProvider.locationUpdates().collect { sample ->
                locationRepo.push(sample, incidentId)
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        trackingJob?.cancel()
        super.onCleared()
    }
}