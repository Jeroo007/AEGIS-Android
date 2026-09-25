package com.aegis.safety.data.remote

import com.aegis.safety.data.remote.dto.GroqChatRequest
import com.aegis.safety.data.remote.dto.GroqChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApiService {

    /**
     * OpenAI-compatible chat completions.
     * Endpoint:  POST https://api.groq.com/openai/v1/chat/completions
     * Auth:      Authorization: Bearer <GROQ_API_KEY>
     */
    @POST("openai/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authHeader: String,
        @Body body: GroqChatRequest
    ): Response<GroqChatResponse>

    companion object {
        // Verified working Groq models (as of 2024/2025).
        // Check https://console.groq.com/docs/models for the current list.
        const val MODEL_LLAMA_70B = "llama-3.3-70b-versatile"
        const val MODEL_LLAMA_8B  = "llama-3.1-8b-instant"
        const val MODEL_MIXTRAL   = "mixtral-8x7b-32768"
    }
}