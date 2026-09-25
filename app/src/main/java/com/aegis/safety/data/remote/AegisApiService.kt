package com.aegis.safety.data.remote

import com.aegis.safety.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AegisApiService {

    // ---- Auth ----
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // ---- Users ----
    @GET("users/me")
    suspend fun getMe(): Response<UserDto>

    // ---- Emergency Contacts ----
    @GET("emergency-contacts")
    suspend fun getContacts(): Response<List<EmergencyContactDto>>

    @POST("emergency-contacts")
    suspend fun createContact(@Body body: CreateContactRequest): Response<EmergencyContactDto>

    @PATCH("emergency-contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: String,
        @Body body: UpdateContactRequest
    ): Response<EmergencyContactDto>

    @DELETE("emergency-contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String): Response<Unit>

    // ---- SOS / Incidents ----
    @POST("sos")
    suspend fun createSos(@Body body: CreateSosRequest): Response<IncidentDto>

    @POST("sos/cancel")
    suspend fun cancelSos(@Body body: Map<String, String>): Response<Unit>

    @GET("incidents/my")
    suspend fun getMyIncidents(): Response<List<IncidentDto>>

    @GET("incidents/{id}")
    suspend fun getIncident(@Path("id") id: String): Response<IncidentDto>

    // ---- Location ----
    @POST("location")
    suspend fun postLocation(@Body body: LocationUpdateRequest): Response<Unit>

    // ---- Journeys ----
    @POST("journeys")
    suspend fun startJourney(@Body body: JourneyRequest): Response<Unit>

    @GET("journeys/active")
    suspend fun getActiveJourneys(): Response<Unit>

    @GET("journeys/{id}")
    suspend fun getJourney(@Path("id") id: String): Response<Unit>

    @POST("journeys/{id}/location")
    suspend fun updateLocation(@Path("id") id: String): Response<Unit>

    @POST("journeys/{id}/arrive")
    suspend fun arriveJourney(@Path("id") id: String): Response<Unit>

    @POST("journeys/{id}/cancel")
    suspend fun cancelJourney(@Path("id") id: String): Response<Unit>
}