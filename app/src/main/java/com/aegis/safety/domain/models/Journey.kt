package com.aegis.safety.domain.models

data class Journey(val id: String, val status: JourneyStatus)
enum class JourneyStatus { ACTIVE, COMPLETED, CANCELLED }
