package com.aegis.safety.data.repository

import com.aegis.safety.domain.repository.JourneyRepositoryInterface
import com.aegis.safety.domain.models.Journey
import com.aegis.safety.domain.models.JourneyStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor() : JourneyRepositoryInterface {
    override suspend fun startJourney(contactIds: List<String>): Journey {
        return Journey(UUID.randomUUID().toString(), JourneyStatus.ACTIVE)
    }
    
    suspend fun pushLocation(id: String, lat: Double, lng: Double) {}
}
