package com.aegis.safety.ai.monitoring

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aegis.safety.MainActivity
import com.aegis.safety.R
import com.aegis.safety.core.audio.AudioCapture
import com.aegis.safety.core.datastore.UserPreferences
import com.aegis.safety.core.notifications.NotificationChannels
import com.aegis.safety.core.sensors.SensorProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that runs the automatic detection pipeline when the
 * user has explicitly enabled monitoring.
 *
 * Foreground service type = microphone (because we capture audio frames)
 * plus we also read IMU. Vision is only enabled when camera permission is
 * present and the user opted in.
 *
 * The service NEVER escalates on its own. It publishes escalations to
 * AutoDetectionCoordinator which broadcasts an intent. MainActivity (or
 * AutoDetectionPromptActivity) handles the confirmation UI.
 *
 * Each signal (audio, IMU) has its own lifecycle-aware sub-job. When the
 * user toggles a signal off, only that job restarts — the others keep
 * running. No duplicate collectors.
 */
@AndroidEntryPoint
class AegisMonitoringService : Service() {

    @Inject lateinit var audio: AudioCapture
    @Inject lateinit var sensors: SensorProvider
    @Inject lateinit var coordinator: AutoDetectionCoordinator
    @Inject lateinit var prefs: UserPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioJob: Job? = null
    private var imuJob: Job? = null
    private var configJob: Job? = null
    private var escalationJob: Job? = null
    private var enabledJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        coordinator.setRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        // If the user already turned monitoring off, stop immediately —
        // this can happen if the service was resurrected by START_STICKY.
        enabledJob?.cancel()
        enabledJob = scope.launch {
            prefs.monitoringEnabled.collect { enabled ->
                if (!enabled) {
                    Timber.i("Monitoring disabled by user — stopping service.")
                    stopSelf()
                }
            }
        }

        observeConfig()
        startAudio()
        startImu()
        observeEscalations()
        return START_STICKY
    }

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------

    /**
     * Rebuilds the coordinator's MonitoringConfig whenever any relevant
     * pref changes. Reads sensitivity + confirmation from DataStore if the
     * keys exist; falls back to defaults otherwise.
     */
    private fun observeConfig() {
        configJob?.cancel()
        configJob = scope.launch {
            combine(
                prefs.monitoringEnabled,
                prefs.audioMonitoring,
                prefs.motionMonitoring,
                prefs.visionMonitoring
            ) { enabled, audio, motion, vision ->
                Quad(enabled, audio, motion, vision)
            }.distinctUntilChanged()
                .collect { q ->
                    val cfg = MonitoringConfig(
                        enabled = q.enabled,
                        audio = q.audio,
                        motion = q.motion,
                        vision = q.vision,
                        sensitivity = prefs.readSensitivityOrDefault(),
                        confirmationSeconds = prefs.readConfirmationOrDefault()
                    )
                    coordinator.setConfig(cfg)
                }
        }
    }

    // -------------------------------------------------------------------------
    // Audio signal
    // -------------------------------------------------------------------------

    /**
     * Restarts the capture pipeline only when the audio pref actually flips.
     * Uses flatMapLatest-equivalent semantics via a manual inner-job cancel.
     */
    @SuppressLint("MissingPermission")
    private fun startAudio() {
        audioJob?.cancel()
        audioJob = scope.launch {
            var inner: Job? = null
            prefs.audioMonitoring
                .distinctUntilChanged()
                .collect { on ->
                    inner?.cancel()
                    inner = null
                    if (!on) return@collect
                    inner = launch {
                        audio.frames(sampleRateHz = 16_000, frameMs = 40)
                            .catch { t -> Timber.e(t, "Audio capture failed") }
                            .collect { frame -> coordinator.onAudioFrame(frame, 16_000) }
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // IMU signal
    // -------------------------------------------------------------------------

    private fun startImu() {
        imuJob?.cancel()
        imuJob = scope.launch {
            var inner: Job? = null
            prefs.motionMonitoring
                .distinctUntilChanged()
                .collect { on ->
                    inner?.cancel()
                    inner = null
                    if (!on) return@collect
                    if (!sensors.isAvailable) {
                        Timber.w("IMU unavailable — motion detection disabled.")
                        return@collect
                    }
                    inner = launch {
                        sensors.imu()
                            .catch { t -> Timber.e(t, "IMU capture failed") }
                            .collect { s -> coordinator.onImuSample(s) }
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Escalation broadcast
    // -------------------------------------------------------------------------

    private fun observeEscalations() {
        escalationJob?.cancel()
        escalationJob = scope.launch {
            coordinator.escalations.collect { esc ->
                val i = Intent(ACTION_AUTO_ESCALATION).apply {
                    setPackage(packageName)
                    putExtra("level", esc.level.name)
                    putExtra("fused", esc.fusedScore)
                    putExtra("audio", esc.audioScore)
                    putExtra("motion", esc.motionScore)
                    putExtra("vision", esc.visionScore)
                    putExtra("engine", esc.audioEngine)
                    putExtra("reason", esc.reason)
                    putExtra("timestamp", esc.timestamp)
                }
                sendBroadcast(i)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onDestroy() {
        audioJob?.cancel()
        imuJob?.cancel()
        configJob?.cancel()
        escalationJob?.cancel()
        enabledJob?.cancel()
        coordinator.setRunning(false)
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
            .setContentTitle(getString(R.string.monitoring_notification_title))
            .setContentText(getString(R.string.monitoring_notification_body))
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private data class Quad(
        val enabled: Boolean,
        val audio: Boolean,
        val motion: Boolean,
        val vision: Boolean
    )

    companion object {
        const val NOTIFICATION_ID = 4001
        const val ACTION_AUTO_ESCALATION = "com.aegis.safety.AUTO_ESCALATION"

        fun start(ctx: Context) {
            val i = Intent(ctx, AegisMonitoringService::class.java)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AegisMonitoringService::class.java))
        }
    }
}