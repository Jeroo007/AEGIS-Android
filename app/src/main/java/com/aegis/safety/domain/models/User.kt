package com.aegis.safety.domain.models

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val role: UserRole = UserRole.USER,
    val createdAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false
)

enum class UserRole { USER, DISPATCHER, PATROL, ADMIN }