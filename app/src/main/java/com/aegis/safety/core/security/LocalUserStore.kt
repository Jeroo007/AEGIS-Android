package com.aegis.safety.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aegis.safety.domain.models.User
import com.aegis.safety.domain.models.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalUserStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "aegis_users",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        if (prefs.getString(KEY_USERS, null) == null) {
            val demo = StoredUser(
                id = UUID.randomUUID().toString(),
                email = DEMO_EMAIL,
                passwordHash = hash(DEMO_PASSWORD),
                fullName = "Demo User",
                phone = null,
                createdAt = System.currentTimeMillis()
            )
            saveUsers(mapOf(DEMO_EMAIL to demo))
        }
    }

    @Serializable
    private data class StoredUser(
        val id: String,
        val email: String,
        val passwordHash: String,
        val fullName: String,
        val phone: String?,
        val createdAt: Long
    )

    private fun hash(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(("aegis-v1::" + password).toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun loadUsers(): Map<String, StoredUser> {
        val raw = prefs.getString(KEY_USERS, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, StoredUser>>(raw)
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private fun saveUsers(users: Map<String, StoredUser>) {
        prefs.edit().putString(KEY_USERS, json.encodeToString(users)).apply()
    }

    fun register(
        email: String, password: String, fullName: String, phone: String?
    ): Result<User> {
        val e = email.trim().lowercase()
        if (!e.contains("@") || e.length < 5) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        if (fullName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your name"))
        }
        val users = loadUsers().toMutableMap()
        if (users.containsKey(e)) {
            return Result.failure(IllegalArgumentException("An account with this email already exists"))
        }
        val stored = StoredUser(
            id = UUID.randomUUID().toString(),
            email = e,
            passwordHash = hash(password),
            fullName = fullName.trim(),
            phone = phone?.trim()?.ifBlank { null },
            createdAt = System.currentTimeMillis()
        )
        users[e] = stored
        saveUsers(users)
        prefs.edit().putString(KEY_CURRENT, json.encodeToString(stored)).apply()
        return Result.success(stored.toDomain())
    }

    fun login(email: String, password: String): Result<User> {
        val e = email.trim().lowercase()
        val user = loadUsers()[e]
            ?: return Result.failure(IllegalArgumentException("No account found for this email"))
        if (user.passwordHash != hash(password)) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }
        prefs.edit().putString(KEY_CURRENT, json.encodeToString(user)).apply()
        return Result.success(user.toDomain())
    }

    fun getCurrentUser(): User? {
        val raw = prefs.getString(KEY_CURRENT, null) ?: return null
        return try {
            json.decodeFromString<StoredUser>(raw).toDomain()
        } catch (_: Throwable) {
            null
        }
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT).apply()
    }

    fun isLoggedIn(): Boolean = getCurrentUser() != null

    private fun StoredUser.toDomain() = User(
        id = id,
        email = email,
        fullName = fullName,
        phoneNumber = phone,
        role = UserRole.USER,
        createdAt = createdAt,
        isVerified = true
    )

    companion object {
        const val DEMO_EMAIL = "demo@aegis.local"
        const val DEMO_PASSWORD = "demo1234"
        private const val KEY_USERS = "users"
        private const val KEY_CURRENT = "current_user"
    }
}