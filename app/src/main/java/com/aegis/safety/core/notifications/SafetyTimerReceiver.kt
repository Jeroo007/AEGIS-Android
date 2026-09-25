package com.aegis.safety.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class SafetyTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("SafetyTimerReceiver fired")
    }
}