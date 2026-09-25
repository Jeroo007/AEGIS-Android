package com.aegis.safety.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.aegis.safety.core.constants.AegisConstants
import com.aegis.safety.domain.models.LocationSample
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationSample? = suspendCancellableCoroutine { cont ->
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) cont.resume(loc.toSample())
                else {
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { l2 -> cont.resume(l2?.toSample()) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }.addOnFailureListener { cont.resume(null) }
        } catch (t: SecurityException) {
            cont.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(
        intervalMs: Long = AegisConstants.LOCATION_UPDATE_INTERVAL_MS,
        fastestMs: Long = AegisConstants.LOCATION_FASTEST_INTERVAL_MS
    ): Flow<LocationSample> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(fastestMs)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toSample()) }
            }
        }
        try {
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (t: SecurityException) {
            close(t)
        }
        awaitClose { fused.removeLocationUpdates(callback) }
    }

    fun hasLocationPermission(): Boolean =
        LocationPermissionHelper.hasForeground(context)

    private fun Location.toSample() = LocationSample(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        altitude = if (hasAltitude()) altitude else null,
        speedMps = if (hasSpeed()) speed else null,
        bearingDegrees = if (hasBearing()) bearing else null,
        provider = provider,
        timestamp = time
    )
}