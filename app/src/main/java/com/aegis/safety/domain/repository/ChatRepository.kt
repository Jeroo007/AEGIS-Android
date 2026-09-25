package com.aegis.safety.domain.repository

import com.aegis.safety.BuildConfig
import com.aegis.safety.core.database.AegisDao
import com.aegis.safety.core.database.entities.ChatMessageEntity
import com.aegis.safety.data.remote.GroqApiService
import com.aegis.safety.data.remote.dto.GroqChatRequest
import com.aegis.safety.data.remote.dto.GroqMessage
import com.aegis.safety.domain.models.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: GroqApiService,
    private val dao: AegisDao
) : ChatRepositoryInterface {

    override fun observeHistory(): Flow<List<ChatMessage>> =
        dao.observeChat().map { list ->
            list.map {
                ChatMessage(
                    id = it.id,
                    role = if (it.role == "user") ChatMessage.Role.USER else ChatMessage.Role.MODEL,
                    text = it.text,
                    timestamp = it.timestamp
                )
            }
        }

    override suspend fun send(userText: String): Result<ChatMessage> {
        // 1. Persist the user turn immediately (so it shows in the UI even if the API fails)
        dao.insertChatMessage(
            ChatMessageEntity(role = "user", text = userText, timestamp = System.currentTimeMillis())
        )

        val apiKey = BuildConfig.GROQ_API_KEY
        if (apiKey.isBlank() || !apiKey.startsWith("gsk_")) {
            return Result.failure(IllegalStateException(
                "GROQ_API_KEY missing or malformed. Add gsk_... to local.properties."
            ))
        }

        // 2. Rebuild conversation — newest first from DAO, reverse for chronological
        val history = dao.getRecentChat(limit = 20).reversed()

        // 3. Build OpenAI-style message list: system first, then alternating user/assistant
        val messages = buildList {
            add(GroqMessage("system", SYSTEM_PROMPT))
            history.forEach { m ->
                add(
                    GroqMessage(
                        role = if (m.role == "user") "user" else "assistant",
                        content = m.text
                    )
                )
            }
        }

        val request = GroqChatRequest(
            model = GroqApiService.MODEL_LLAMA_8B,   // fast + cheap; switch to 70B if you want more depth
            messages = messages,
            temperature = 0.6f,
            maxTokens = 512,
            topP = 0.9f,
            stream = false
        )

        return try {
            val resp = api.chat(
                authHeader = "Bearer $apiKey",
                body = request
            )

            if (!resp.isSuccessful) {
                val errBody = resp.errorBody()?.string()
                Timber.w("Groq HTTP %d: %s", resp.code(), errBody)
                return Result.failure(IllegalStateException(
                    "Groq HTTP ${resp.code()}: ${errBody?.take(200) ?: ""}"
                ))
            }

            val body = resp.body()
                ?: return Result.failure(IllegalStateException("Empty response from Groq"))

            // Surface Groq's own error object if present
            body.error?.let {
                return Result.failure(IllegalStateException(
                    "Groq error: ${it.message ?: it.type ?: "unknown"}"
                ))
            }

            val reply = body.choices.firstOrNull()?.message?.content?.trim()
                ?: return Result.failure(IllegalStateException("No choices returned"))

            val newId = dao.insertChatMessage(
                ChatMessageEntity(
                    role = "model",
                    text = reply,
                    timestamp = System.currentTimeMillis()
                )
            )

            Result.success(
                ChatMessage(
                    id = newId,
                    role = ChatMessage.Role.MODEL,
                    text = reply
                )
            )
        } catch (t: Throwable) {
            Timber.e(t, "Groq call failed")
            Result.failure(t)
        }
    }

    override suspend fun clear() = dao.clearChat()

    companion object {
        private const val SYSTEM_PROMPT = """
You are AEGIS Assistant, the in-app safety companion inside the AEGIS Android app.

Your role:
- Help the user understand AEGIS features: SOS, Auto-Detection, Safe Zones, Safety Timer, Live Journey, Emergency Contacts, Privacy Controls.
- Provide calm, factual, non-alarmist safety guidance.
- If the user says they are in immediate danger, always tell them to press the SOS button or dial 112 (India) / their local emergency number.
- Never claim to be emergency services. Never claim to have contacted anyone on their behalf.
- Never give medical diagnoses. For medical emergencies, tell them to call 108 (India) or 112.
- Keep replies under 120 words unless the user asks for more detail.
- Be warm, direct, and clear. Use plain language — no jargon.

Never:
- Recommend disabling safety features.
- Speculate about the user's identity, gender, or situation.
- Promise a specific response time from police or ambulance.
- Invent AEGIS features that don't exist.
"""
    }
}