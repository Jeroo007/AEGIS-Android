package com.aegis.safety

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aegis.safety.core.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AegisApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                NotificationChannels.ALERTS,
                getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                NotificationChannels.EMERGENCY,
                getString(R.string.channel_emergency),
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                NotificationChannels.LOCATION,
                getString(R.string.channel_location),
                NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannel(
                NotificationChannels.SYSTEM,
                getString(R.string.channel_system),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannels(channels)
    }
}