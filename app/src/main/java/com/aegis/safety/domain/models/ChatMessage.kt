package com.aegis.safety.domain.models

data class ChatMessage(
    val id: Long = 0,
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
) {
    enum class Role { USER, MODEL }
}