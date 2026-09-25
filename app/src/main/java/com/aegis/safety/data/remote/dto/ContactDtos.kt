package com.aegis.safety.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmergencyContactDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("relationship") val relationship: String? = null,
    @SerialName("priority") val priority: Int = 0,
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("notify_on_sos") val notifyOnSos: Boolean = true,
    @SerialName("created_at") val createdAt: Long
)

@Serializable
data class CreateContactRequest(
    @SerialName("name") val name: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("relationship") val relationship: String? = null,
    @SerialName("priority") val priority: Int = 0,
    @SerialName("notify_on_sos") val notifyOnSos: Boolean = true
)

@Serializable
data class UpdateContactRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("relationship") val relationship: String? = null,
    @SerialName("priority") val priority: Int? = null,
    @SerialName("notify_on_sos") val notifyOnSos: Boolean? = null
)