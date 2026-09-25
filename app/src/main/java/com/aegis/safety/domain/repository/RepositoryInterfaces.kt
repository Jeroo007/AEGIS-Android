package com.aegis.safety.domain.repository

import com.aegis.safety.domain.models.*
import kotlinx.coroutines.flow.Flow

interface AuthRepositoryInterface {
    fun login(email: String, password: String): Flow<Result<User>>
    fun register(email: String, password: String, fullName: String, phone: String?): Flow<Result<User>>
    suspend fun logout()
    fun isLoggedIn(): Boolean
}

interface EmergencyContactRepositoryInterface {
    fun observeContacts(): Flow<List<EmergencyContact>>
    /** Direct, non-flow snapshot from the DB — used by SOS to read contacts reliably. */
    suspend fun getAllSnapshot(): List<EmergencyContact>
    suspend fun refresh()
    suspend fun create(name: String, phone: String, relationship: String?): Result<EmergencyContact>
    suspend fun update(contact: EmergencyContact): Result<EmergencyContact>
    suspend fun delete(id: String): Result<Unit>
}

interface IncidentRepositoryInterface {
    fun observeIncidents(): Flow<List<Incident>>
    fun observePendingCount(): Flow<Int>
    suspend fun createSos(location: LocationSample?, type: IncidentType, confidence: Float?, notes: String?, isDemo: Boolean): Result<Incident>
    suspend fun cancelSos(incidentId: String): Result<Unit>
    suspend fun refresh()
}

interface LocationRepositoryInterface {
    fun observeLocations(): Flow<List<LocationSample>>
    suspend fun getCurrent(): LocationSample?
    suspend fun push(sample: LocationSample, incidentId: String?): Result<Unit>
}

interface SafeZoneRepositoryInterface {
    fun getBundled(
        lat: Double,
        lon: Double,
        limit: Int = 30,
        maxDistanceMeters: Float = 15_000f
    ): List<SafeZone>

    suspend fun nearest(
        lat: Double,
        lon: Double,
        limit: Int = 30,
        maxDistanceMeters: Float = 15_000f    // 15 km — covers most of urban Coimbatore
    ): Result<List<SafeZone>>
}

interface NotificationRepositoryInterface {
    fun observeNotifications(): Flow<List<AegisNotification>>
    suspend fun markRead(id: String)
    suspend fun markAllRead()
}