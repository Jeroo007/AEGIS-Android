package com.aegis.safety.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Groq uses the OpenAI Chat Completions schema, NOT the Gemini schema.
 * Do not reuse GeminiRequest/GeminiContent for Groq calls.
 */
@Serializable
data class GroqChatRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<GroqMessage>,
    @SerialName("temperature") val temperature: Float = 0.6f,
    @SerialName("max_tokens") val maxTokens: Int = 512,
    @SerialName("top_p") val topP: Float = 0.9f,
    @SerialName("stream") val stream: Boolean = false
)

@Serializable
data class GroqMessage(
    /** "system" | "user" | "assistant" */
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class GroqChatResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("choices") val choices: List<GroqChoice> = emptyList(),
    @SerialName("error") val error: GroqError? = null
)

@Serializable
data class GroqChoice(
    @SerialName("index") val index: Int = 0,
    @SerialName("message") val message: GroqMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class GroqError(
    @SerialName("message") val message: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("code") val code: String? = null
)