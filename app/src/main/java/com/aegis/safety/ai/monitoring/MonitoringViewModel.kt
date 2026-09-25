package com.aegis.safety.presentation.monitoring

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.ai.monitoring.AegisMonitoringService
import com.aegis.safety.ai.monitoring.AutoDetectionCoordinator
import com.aegis.safety.ai.monitoring.MonitoringConfig
import com.aegis.safety.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonitoringUiState(
    val enabled: Boolean = false,
    val audio: Boolean = true,
    val motion: Boolean = true,
    val vision: Boolean = false,
    val sensitivity: MonitoringConfig.Sensitivity = MonitoringConfig.Sensitivity.MEDIUM,
    val confirmationSeconds: Int = 15,
    val escalationsLastHour: Int = 0
)

@HiltViewModel
class MonitoringViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
    private val coordinator: AutoDetectionCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(MonitoringUiState())
    val state: StateFlow<MonitoringUiState> = _state.asStateFlow()

    /** Debounces rapid slider changes so we don't hammer DataStore. */
    private var confirmationDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.monitoringEnabled.collect { v -> _state.update { it.copy(enabled = v) } }
        }
        viewModelScope.launch {
            prefs.audioMonitoring.collect { v -> _state.update { it.copy(audio = v) } }
        }
        viewModelScope.launch {
            prefs.motionMonitoring.collect { v -> _state.update { it.copy(motion = v) } }
        }
        viewModelScope.launch {
            prefs.visionMonitoring.collect { v -> _state.update { it.copy(vision = v) } }
        }
        viewModelScope.launch {
            prefs.monitoringSensitivity.collect { raw ->
                val parsed = runCatching {
                    MonitoringConfig.Sensitivity.valueOf(raw)
                }.getOrDefault(MonitoringConfig.Sensitivity.MEDIUM)
                _state.update { it.copy(sensitivity = parsed) }
            }
        }
        viewModelScope.launch {
            prefs.monitoringConfirmation.collect { v ->
                _state.update { it.copy(confirmationSeconds = v.coerceIn(5, 60)) }
            }
        }
        viewModelScope.launch {
            coordinator.state.collect { s ->
                _state.update { it.copy(escalationsLastHour = s.escalationsInLastHour) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setMonitoringEnabled(enabled)
            if (enabled) {
                AegisMonitoringService.start(context)
            } else {
                AegisMonitoringService.stop(context)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Signal toggles
    // -------------------------------------------------------------------------

    fun setAudio(v: Boolean) = viewModelScope.launch { prefs.setAudioMonitoring(v) }
    fun setMotion(v: Boolean) = viewModelScope.launch { prefs.setMotionMonitoring(v) }
    fun setVision(v: Boolean) = viewModelScope.launch { prefs.setVisionMonitoring(v) }

    // -------------------------------------------------------------------------
    // Sensitivity
    // -------------------------------------------------------------------------

    /**
     * Persists the sensitivity choice. The service is the single writer of
     * the coordinator's [MonitoringConfig] — it observes this DataStore key
     * and pushes a fresh config when it changes.
     */
    fun setSensitivity(s: MonitoringConfig.Sensitivity) {
        viewModelScope.launch { prefs.setMonitoringSensitivity(s.name) }
    }

    // -------------------------------------------------------------------------
    // Confirmation window
    // -------------------------------------------------------------------------

    /**
     * Updates the state immediately for UI feedback, then debounces the
     * DataStore write by 250 ms. Prevents one file write per slider frame.
     */
    fun setConfirmationSeconds(sec: Int) {
        val clamped = sec.coerceIn(5, 60)
        _state.update { it.copy(confirmationSeconds = clamped) }

        confirmationDebounceJob?.cancel()
        confirmationDebounceJob = viewModelScope.launch {
            delay(250)
            prefs.setMonitoringConfirmation(clamped)
        }
    }

    override fun onCleared() {
        // If the user is mid-drag when the ViewModel is destroyed, flush the
        // pending value so nothing is lost. We can't launch here reliably, so
        // we accept the last debounced value (already committed 250 ms ago)
        // as the source of truth. Any tighter guarantee requires a foreground
        // scope — out of scope for a settings screen.
        confirmationDebounceJob?.cancel()
        super.onCleared()
    }
}