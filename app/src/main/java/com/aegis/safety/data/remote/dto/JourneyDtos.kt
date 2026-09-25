package com.aegis.safety.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JourneyRequest(
    @SerialName("contact_ids") val contactIds: List<String>
)
