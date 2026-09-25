package com.aegis.safety.data.repository

import com.aegis.safety.core.security.LocalUserStore
import com.aegis.safety.core.security.TokenStore
import com.aegis.safety.domain.models.User
import com.aegis.safety.domain.repository.AuthRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val localUserStore: LocalUserStore,
    private val tokenStore: TokenStore
) : AuthRepositoryInterface {

    override fun login(email: String, password: String): Flow<Result<User>> = flow {
        val result = localUserStore.login(email, password)
        result.onSuccess { user ->
            tokenStore.saveTokens("local-access-${user.id}", "local-refresh-${user.id}")
        }
        emit(result)
    }

    override fun register(
        email: String, password: String, fullName: String, phone: String?
    ): Flow<Result<User>> = flow {
        val result = localUserStore.register(email, password, fullName, phone)
        result.onSuccess { user ->
            tokenStore.saveTokens("local-access-${user.id}", "local-refresh-${user.id}")
        }
        emit(result)
    }

    override suspend fun logout() {
        localUserStore.logout()
        tokenStore.clear()
    }

    override fun isLoggedIn(): Boolean = localUserStore.isLoggedIn()

    fun currentUser(): User? = localUserStore.getCurrentUser()
}