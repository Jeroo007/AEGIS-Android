package com.aegis.safety.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.domain.models.AegisNotification
import com.aegis.safety.domain.repository.NotificationRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(val items: List<AegisNotification> = emptyList())

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationRepositoryInterface
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeNotifications().collect { list ->
                _state.update { it.copy(items = list) }
            }
        }
    }

    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }
    fun markRead(id: String) = viewModelScope.launch { repo.markRead(id) }
}