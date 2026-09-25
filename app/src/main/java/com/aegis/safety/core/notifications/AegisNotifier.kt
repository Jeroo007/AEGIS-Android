package com.aegis.safety.core.notifications

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AegisNotifier @Inject constructor() {
    fun showWakeWordDetected() {}
    fun showPotentialEvent() {}
    fun showOfflineQueue() {}
}
