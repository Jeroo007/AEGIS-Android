package com.aegis.safety.core.location

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aegis.safety.MainActivity
import com.aegis.safety.R
import com.aegis.safety.core.constants.AegisConstants
import com.aegis.safety.core.notifications.NotificationChannels
import com.aegis.safety.domain.models.LocationSample
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class EmergencyLocationService : Service() {

    @Inject lateinit var locationProvider: LocationProvider

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var incidentId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        incidentId = intent?.getStringExtra(EXTRA_INCIDENT_ID)
        startForeground(NOTIFICATION_ID, buildNotification())
        startLocationLoop()
        return START_STICKY
    }

    private fun startLocationLoop() {
        scope.launch {
            locationProvider.locationUpdates(
                intervalMs = AegisConstants.EMERGENCY_LOCATION_INTERVAL_MS,
                fastestMs = 2_000L
            ).onEach { sample ->
                // Broadcast to app so ViewModel/WS can pick it up
                val i = Intent(ACTION_LOCATION_SAMPLE).apply {
                    putExtra("lat", sample.latitude)
                    putExtra("lon", sample.longitude)
                    putExtra("acc", sample.accuracyMeters)
                    putExtra("ts", sample.timestamp)
                    putExtra("incidentId", incidentId)
                }
                sendBroadcast(i)
            }.launchIn(this)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, NotificationChannels.LOCATION)
            .setSmallIcon(R.drawable.ic_stat_aegis)
            .setContentTitle(getString(R.string.emergency_location_active))
            .setContentText(getString(R.string.sharing_live_location))
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val EXTRA_INCIDENT_ID = "incident_id"
        const val ACTION_LOCATION_SAMPLE = "com.aegis.safety.LOCATION_SAMPLE"
        const val NOTIFICATION_ID = 1001
    }
}