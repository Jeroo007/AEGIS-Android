package com.aegis.safety.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.domain.models.ChatMessage
import com.aegis.safety.domain.repository.ChatRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null
) {
    val canSend: Boolean get() = input.isNotBlank() && !sending
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: ChatRepositoryInterface
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeHistory().collect { list ->
                _state.update { it.copy(messages = list) }
            }
        }
    }

    fun onInput(v: String) = _state.update { it.copy(input = v, error = null) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.sending) return
        _state.update { it.copy(input = "", sending = true, error = null) }

        viewModelScope.launch {
            repo.send(text)
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            sending = false,
                            error = e.message ?: "Failed to reach assistant"
                        )
                    }
                }
                .onSuccess {
                    _state.update { it.copy(sending = false) }
                }
        }
    }

    fun clear() = viewModelScope.launch { repo.clear() }
}