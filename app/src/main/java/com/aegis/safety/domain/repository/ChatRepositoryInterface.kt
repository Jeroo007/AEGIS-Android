package com.aegis.safety.domain.repository

import com.aegis.safety.domain.models.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepositoryInterface {
    fun observeHistory(): Flow<List<ChatMessage>>
    suspend fun send(userText: String): Result<ChatMessage>
    suspend fun clear()
}