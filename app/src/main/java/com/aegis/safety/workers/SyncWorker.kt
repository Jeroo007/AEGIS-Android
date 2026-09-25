package com.aegis.safety.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aegis.safety.core.database.AegisDao
import com.aegis.safety.core.network.safeApiCall
import com.aegis.safety.core.constants.AegisConstants
import com.aegis.safety.data.remote.AegisApiService
import com.aegis.safety.data.remote.dto.LocationUpdateRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: AegisDao,
    private val api: AegisApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncLocations()
            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "SyncWorker failed")
            Result.retry()
        }
    }

    private suspend fun syncLocations() {
        val pending = dao.unsyncedLocations(limit = 100)
        if (pending.isEmpty()) return
        val syncedIds = mutableListOf<Long>()
        for (loc in pending) {
            val res = safeApiCall {
                api.postLocation(LocationUpdateRequest(
                    latitude = loc.latitude, longitude = loc.longitude,
                    accuracyMeters = loc.accuracyMeters, incidentId = loc.incidentId,
                    timestamp = loc.timestamp))
            }
            if (res.isSuccess) syncedIds.add(loc.id)
            else Timber.w("Failed to sync location ${loc.id}: ${res.exceptionOrNull()?.message}")
        }
        if (syncedIds.isNotEmpty()) dao.markLocationsSynced(syncedIds)
        // Purge old synced locations older than 24h
        dao.purgeOldLocations(System.currentTimeMillis() - 24 * 3600_000L)
    }
}