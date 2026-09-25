package com.aegis.safety.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.safety.core.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {
    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setOnboardingDone(true)
            onDone()
        }
    }
}