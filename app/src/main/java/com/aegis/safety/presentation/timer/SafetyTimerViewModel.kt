package com.aegis.safety.presentation.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.core.database.AegisDao
import com.aegis.safety.core.database.entities.SafetyTimerEntity
import com.aegis.safety.domain.models.SafetyTimerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TimerUiState(
    val active: SafetyTimerEntity? = null,
    val remainingSeconds: Long = 0,
    val expired: Boolean = false
)

@HiltViewModel
class SafetyTimerViewModel @Inject constructor(
    private val dao: AegisDao
) : ViewModel() {

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var tickJob: Job? = null

    fun start(minutes: Int, label: String? = null) {
        val now = System.currentTimeMillis()
        val entity = SafetyTimerEntity(
            id = UUID.randomUUID().toString(),
            userId = "me",
            durationMinutes = minutes,
            startedAt = now,
            expiresAt = now + minutes * 60_000L,
            reminderSent = false,
            secondReminderSent = false,
            escalatedToContact = false,
            escalatedToDispatcher = false,
            status = SafetyTimerStatus.ACTIVE.name,
            label = label
        )
        viewModelScope.launch {
            dao.upsertTimer(entity)
            _state.update { it.copy(active = entity, expired = false) }
            startTicking(entity)
        }
    }

    fun cancel() {
        val active = _state.value.active ?: return
        tickJob?.cancel()
        viewModelScope.launch {
            dao.deleteTimer(active.id)
            _state.value = TimerUiState()
        }
    }

    fun checkIn() {
        val active = _state.value.active ?: return
        tickJob?.cancel()
        viewModelScope.launch {
            dao.deleteTimer(active.id)
            _state.value = TimerUiState()
        }
    }

    private fun startTicking(entity: SafetyTimerEntity) {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val remaining = (entity.expiresAt - System.currentTimeMillis()) / 1000
                _state.update { it.copy(remainingSeconds = remaining.coerceAtLeast(0)) }
                if (remaining <= 0) {
                    val updated = entity.copy(status = SafetyTimerStatus.EXPIRED.name)
                    dao.upsertTimer(updated)
                    _state.update { it.copy(active = updated, expired = true) }
                    break
                }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }
}