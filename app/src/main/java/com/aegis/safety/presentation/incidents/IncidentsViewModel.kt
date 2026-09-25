package com.aegis.safety.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.domain.models.Incident
import com.aegis.safety.domain.repository.IncidentRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncidentsUiState(
    val incidents: List<Incident> = emptyList(),
    val loading: Boolean = true
)

@HiltViewModel
class IncidentsViewModel @Inject constructor(
    private val repo: IncidentRepositoryInterface
) : ViewModel() {

    private val _state = MutableStateFlow(IncidentsUiState())
    val state: StateFlow<IncidentsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeIncidents().collect { list ->
                _state.update { it.copy(incidents = list, loading = false) }
            }
        }
        viewModelScope.launch { repo.refresh() }
    }
}