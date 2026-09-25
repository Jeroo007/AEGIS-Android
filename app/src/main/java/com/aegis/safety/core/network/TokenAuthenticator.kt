package com.aegis.safety.core.network

import com.aegis.safety.core.security.TokenStore
import com.aegis.safety.data.remote.AegisApiService
import com.aegis.safety.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val apiService: Provider<AegisApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 3) return null

        val refreshToken = tokenStore.getRefreshToken() ?: return null
        if (refreshToken.startsWith("local-")) return null

        synchronized(this) {
            val currentToken = tokenStore.getAccessToken()
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")?.trim()
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }
            return try {
                val newTokens = runBlocking {
                    apiService.get().refresh(RefreshRequest(refreshToken))
                }
                val body = newTokens.body()
                if (!newTokens.isSuccessful || body == null) { tokenStore.clear(); return null }
                tokenStore.saveTokens(body.accessToken, body.refreshToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${body.accessToken}")
                    .build()
            } catch (_: Throwable) { tokenStore.clear(); null }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}