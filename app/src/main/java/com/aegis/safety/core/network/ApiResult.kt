package com.aegis.safety.core.network

import retrofit2.Response
import java.io.IOException

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> = try {
    val response = call()
    if (response.isSuccessful) {
        val body = response.body()
        if (body == null) Result.failure(IOException("Empty body"))
        else Result.success(body)
    } else {
        Result.failure(ApiException(response.code(), response.errorBody()?.string()))
    }
} catch (e: Exception) {
    Result.failure(e)
}

class ApiException(val code: Int, val messageBody: String?) :
    Exception("HTTP $code: ${messageBody ?: ""}")