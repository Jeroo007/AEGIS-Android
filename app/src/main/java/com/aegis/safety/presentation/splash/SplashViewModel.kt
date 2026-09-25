package com.aegis.safety.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.core.datastore.UserPreferences
import com.aegis.safety.core.security.LocalUserStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashDestination {
    data object Onboarding : SplashDestination
    data object Login : SplashDestination
    data object Home : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val localUserStore: LocalUserStore,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination

    init { resolve() }

    private fun resolve() {
        viewModelScope.launch {
            delay(800)
            val done = prefs.onboardingDone.first()
            _destination.value = when {
                !done -> SplashDestination.Onboarding
                !localUserStore.isLoggedIn() -> SplashDestination.Login
                else -> SplashDestination.Home
            }
        }
    }
}