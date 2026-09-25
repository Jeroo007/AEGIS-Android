package com.aegis.safety.domain.repository

import com.aegis.safety.domain.models.Journey

interface JourneyRepositoryInterface {
    suspend fun startJourney(contactIds: List<String>): Journey
}
