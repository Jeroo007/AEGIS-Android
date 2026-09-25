package com.aegis.safety.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "user" | "model" */
    val role: String,
    val text: String,
    val timestamp: Long,
    val sessionId: String = "default"
)