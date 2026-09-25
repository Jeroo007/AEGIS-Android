package com.aegis.safety.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aegis.safety.ai.monitoring.MonitoringConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("aegis_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val AUDIO_MONITORING = booleanPreferencesKey("audio_monitoring")
        val VISION_MONITORING = booleanPreferencesKey("vision_monitoring")
        val MOTION_MONITORING = booleanPreferencesKey("motion_monitoring")
        val LOCATION_SHARING = booleanPreferencesKey("location_sharing")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_THEME = stringPreferencesKey("dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val LAST_LAT = doublePreferencesKey("last_lat")
        val LAST_LON = doublePreferencesKey("last_lon")
        val LAST_ACCURACY = floatPreferencesKey("last_accuracy")
        val LAST_LOCATION_TS = longPreferencesKey("last_location_ts")

        // Auto-detection
        val MONITORING_SENSITIVITY = stringPreferencesKey("monitoring_sensitivity")
        val MONITORING_CONFIRMATION = intPreferencesKey("monitoring_confirmation")
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val monitoringEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MONITORING_ENABLED] ?: false }
    val audioMonitoring: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUDIO_MONITORING] ?: false }
    val visionMonitoring: Flow<Boolean> = context.dataStore.data.map { it[Keys.VISION_MONITORING] ?: false }
    val motionMonitoring: Flow<Boolean> = context.dataStore.data.map { it[Keys.MOTION_MONITORING] ?: true }
    val locationSharing: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOCATION_SHARING] ?: true }
    val analyticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ANALYTICS_ENABLED] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }
    val darkTheme: Flow<String> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: "system" }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val demoMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DEMO_MODE] ?: false }

    /**
     * Auto-detection sensitivity as a string: "LOW" | "MEDIUM" | "HIGH".
     * Parse with `MonitoringConfig.Sensitivity.valueOf(raw)`.
     */
    val monitoringSensitivity: Flow<String> = context.dataStore.data.map {
        it[Keys.MONITORING_SENSITIVITY] ?: MonitoringConfig.Sensitivity.MEDIUM.name
    }

    /** Human-in-the-loop confirmation window in seconds (5..60). */
    val monitoringConfirmation: Flow<Int> = context.dataStore.data.map {
        (it[Keys.MONITORING_CONFIRMATION] ?: 15).coerceIn(5, 60)
    }

    suspend fun setOnboardingDone(value: Boolean) = edit { it[Keys.ONBOARDING_DONE] = value }
    suspend fun setMonitoringEnabled(value: Boolean) = edit { it[Keys.MONITORING_ENABLED] = value }
    suspend fun setAudioMonitoring(value: Boolean) = edit { it[Keys.AUDIO_MONITORING] = value }
    suspend fun setVisionMonitoring(value: Boolean) = edit { it[Keys.VISION_MONITORING] = value }
    suspend fun setMotionMonitoring(value: Boolean) = edit { it[Keys.MOTION_MONITORING] = value }
    suspend fun setLocationSharing(value: Boolean) = edit { it[Keys.LOCATION_SHARING] = value }
    suspend fun setAnalyticsEnabled(value: Boolean) = edit { it[Keys.ANALYTICS_ENABLED] = value }
    suspend fun setNotificationsEnabled(value: Boolean) = edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
    suspend fun setDarkTheme(value: String) = edit { it[Keys.DARK_THEME] = value }
    suspend fun setLanguage(value: String) = edit { it[Keys.LANGUAGE] = value }
    suspend fun setDemoMode(value: Boolean) = edit { it[Keys.DEMO_MODE] = value }

    /**
     * Persist the sensitivity enum name. Callers pass the enum's `.name`
     * so this layer stays independent of `MonitoringConfig`.
     */
    suspend fun setMonitoringSensitivity(value: String) {
        val normalized = runCatching {
            MonitoringConfig.Sensitivity.valueOf(value).name
        }.getOrDefault(MonitoringConfig.Sensitivity.MEDIUM.name)
        edit { it[Keys.MONITORING_SENSITIVITY] = normalized }
    }

    /** Persist confirmation window in seconds. Clamped to 5..60. */
    suspend fun setMonitoringConfirmation(seconds: Int) {
        edit { it[Keys.MONITORING_CONFIRMATION] = seconds.coerceIn(5, 60) }
    }

    /**
     * Synchronous read used by [com.aegis.safety.ai.monitoring.AegisMonitoringService]
     * when it needs the current value immediately (e.g. when building a
     * fresh [MonitoringConfig] on a DataStore change). Never call from the
     * main thread — it suspends until the first value is available.
     */
    suspend fun readSensitivityOrDefault(): MonitoringConfig.Sensitivity {
        val raw = monitoringSensitivity.first()
        return runCatching {
            MonitoringConfig.Sensitivity.valueOf(raw)
        }.getOrDefault(MonitoringConfig.Sensitivity.MEDIUM)
    }

    /**
     * Synchronous read of the confirmation window. See notes on
     * [readSensitivityOrDefault].
     */
    suspend fun readConfirmationOrDefault(): Int =
        monitoringConfirmation.first().coerceIn(5, 60)

    suspend fun saveLastLocation(lat: Double, lon: Double, accuracy: Float, ts: Long) {
        edit {
            it[Keys.LAST_LAT] = lat
            it[Keys.LAST_LON] = lon
            it[Keys.LAST_ACCURACY] = accuracy
            it[Keys.LAST_LOCATION_TS] = ts
        }
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}