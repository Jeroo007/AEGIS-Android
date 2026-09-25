package com.aegis.safety.presentation.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.domain.repository.AuthRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val phone: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
) {
    val canSubmitLogin: Boolean get() = email.contains("@") && password.length >= 6 && !loading
    val canSubmitRegister: Boolean get() = canSubmitLogin && fullName.isNotBlank() && !loading
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepositoryInterface
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v.trim(), error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onFullName(v: String) = _state.update { it.copy(fullName = v.trim(), error = null) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v.trim(), error = null) }

    fun login() {
        val s = _state.value
        if (!s.canSubmitLogin) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.login(s.email, s.password).collect { result ->
                result.onSuccess { _state.update { it.copy(loading = false, success = true) } }
                    .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Login failed") } }
            }
        }
    }

    fun register() {
        val s = _state.value
        if (!s.canSubmitRegister) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.register(s.email, s.password, s.fullName, s.phone.ifBlank { null }).collect { result ->
                result.onSuccess { _state.update { it.copy(loading = false, success = true) } }
                    .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Registration failed") } }
            }
        }
    }
}