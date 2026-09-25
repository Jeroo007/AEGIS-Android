package com.aegis.safety.data.repository

import com.aegis.safety.core.database.AegisDao
import com.aegis.safety.core.database.entities.LocationEntity
import com.aegis.safety.core.datastore.UserPreferences
import com.aegis.safety.core.location.LocationProvider
import com.aegis.safety.data.remote.AegisApiService
import com.aegis.safety.data.remote.dto.LocationUpdateRequest
import com.aegis.safety.domain.models.LocationSample
import com.aegis.safety.domain.repository.LocationRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val provider: LocationProvider,
    private val api: AegisApiService,
    private val dao: AegisDao,
    private val prefs: UserPreferences
) : LocationRepositoryInterface {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeLocations(): Flow<List<LocationSample>> = flowOf(emptyList())

    override suspend fun getCurrent(): LocationSample? {
        val sample = provider.getCurrentLocation()
        if (sample != null && sample.isValid()) {
            prefs.saveLastLocation(
                sample.latitude, sample.longitude,
                sample.accuracyMeters, sample.timestamp)
            dao.insertLocation(sample.toEntity(null))
        }
        return sample
    }

    override suspend fun push(sample: LocationSample, incidentId: String?): Result<Unit> {
        if (!sample.isValid()) {
            return Result.failure(IllegalArgumentException("Invalid location"))
        }
        dao.insertLocation(sample.toEntity(incidentId))
        syncScope.launch {
            runCatching {
                api.postLocation(LocationUpdateRequest(
                    latitude = sample.latitude,
                    longitude = sample.longitude,
                    accuracyMeters = sample.accuracyMeters,
                    incidentId = incidentId,
                    timestamp = sample.timestamp))
            }
        }
        return Result.success(Unit)
    }
}

private fun LocationSample.toEntity(incidentId: String?) = LocationEntity(
    latitude = latitude, longitude = longitude, accuracyMeters = accuracyMeters,
    incidentId = incidentId, timestamp = timestamp, synced = false)