package com.aegis.safety.data.repository

import com.aegis.safety.core.database.AegisDao
import com.aegis.safety.core.database.entities.IncidentEntity
import com.aegis.safety.core.network.safeApiCall
import com.aegis.safety.data.remote.AegisApiService
import com.aegis.safety.data.remote.dto.CreateSosRequest
import com.aegis.safety.domain.models.*
import com.aegis.safety.domain.repository.IncidentRepositoryInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val api: AegisApiService,
    private val dao: AegisDao
) : IncidentRepositoryInterface {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeIncidents(): Flow<List<Incident>> =
        dao.observeIncidents().map { list -> list.map { it.toDomain() } }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun createSos(
        location: LocationSample?,
        type: IncidentType,
        confidence: Float?,
        notes: String?,
        isDemo: Boolean
    ): Result<Incident> {
        val localId = UUID.randomUUID().toString()
        val incident = Incident(
            id = localId,
            userId = "me",
            type = type,
            status = IncidentStatus.CREATED,
            severity = if (type == IncidentType.MANUAL_SOS) Severity.CRITICAL else Severity.HIGH,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyMeters = location?.accuracyMeters,
            confidence = confidence,
            notes = notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDemo = isDemo
        )
        dao.upsertIncident(incident.toEntity(synced = false))

        syncScope.launch {
            runCatching {
                val response = safeApiCall {
                    api.createSos(CreateSosRequest(
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        accuracyMeters = location?.accuracyMeters,
                        type = type.name,
                        confidence = confidence,
                        notes = notes,
                        isDemo = isDemo
                    ))
                }
                response.getOrNull()?.let { dto ->
                    dao.deleteIncident(localId)
                    dao.upsertIncident(dto.toEntity(synced = true))
                }
            }
        }

        return Result.success(incident)
    }

    override suspend fun cancelSos(incidentId: String): Result<Unit> {
        dao.getIncident(incidentId)?.let {
            dao.upsertIncident(it.copy(
                status = IncidentStatus.CANCELLED.name,
                updatedAt = System.currentTimeMillis()
            ))
        }
        syncScope.launch { runCatching { api.cancelSos(mapOf("incident_id" to incidentId)) } }
        return Result.success(Unit)
    }

    override suspend fun refresh() {
        syncScope.launch {
            runCatching {
                val remote = safeApiCall { api.getMyIncidents() }.getOrNull() ?: return@launch
                remote.forEach { dao.upsertIncident(it.toEntity(synced = true)) }
            }
        }
    }
}

private fun IncidentEntity.toDomain() = Incident(
    id, userId, runCatching { IncidentType.valueOf(type) }.getOrDefault(IncidentType.UNKNOWN),
    runCatching { IncidentStatus.valueOf(status) }.getOrDefault(IncidentStatus.CREATED),
    runCatching { Severity.valueOf(severity) }.getOrDefault(Severity.MEDIUM),
    latitude, longitude, accuracyMeters, confidence, notes,
    createdAt, updatedAt, resolvedAt, assignedPatrolId, isDemo)

private fun Incident.toEntity(synced: Boolean = true) = IncidentEntity(
    id, userId, type.name, status.name, severity.name, latitude, longitude,
    accuracyMeters, confidence, notes, createdAt, updatedAt, resolvedAt,
    assignedPatrolId, isDemo, synced)

private fun com.aegis.safety.data.remote.dto.IncidentDto.toEntity(synced: Boolean) = IncidentEntity(
    id, userId, type, status, severity, latitude, longitude, accuracyMeters, confidence,
    notes, createdAt, updatedAt, resolvedAt, assignedPatrolId, isDemo, synced)