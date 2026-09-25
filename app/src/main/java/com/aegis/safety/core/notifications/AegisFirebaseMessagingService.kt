package com.aegis.safety.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class AegisFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("FCM message: ${message.data}")
    }
    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed")
    }
}