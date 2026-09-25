package com.aegis.safety.presentation.journey

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JourneyViewModel @Inject constructor() : ViewModel() {
    val canStart = true
    fun start() {}
    fun arrive() {}
    fun cancel() {}
}
