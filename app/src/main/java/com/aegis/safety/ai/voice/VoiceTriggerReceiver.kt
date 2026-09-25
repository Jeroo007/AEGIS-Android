package com.aegis.safety.ai.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.aegis.safety.core.notifications.AegisNotifier

@AndroidEntryPoint
class VoiceTriggerReceiver : BroadcastReceiver() {
    @Inject lateinit var notifier: AegisNotifier
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.aegis.safety.WAKE_WORD_DETECTED") {
            notifier.showWakeWordDetected()
        }
    }
}
