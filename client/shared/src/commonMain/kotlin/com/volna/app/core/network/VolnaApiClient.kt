package com.volna.app.core.network

import com.volna.app.auth.SessionRepository
import com.volna.app.core.error.AppFailure
import com.volna.app.core.error.AppFailureException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.contentType
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class VolnaApiClient(
    @PublishedApi internal val sessionRepository: SessionRepository,
    @PublishedApi internal val baseUrl: String = DEFAULT_BASE_URL,
    @PublishedApi internal val httpClient: HttpClient = defaultHttpClient(),
) {
    internal suspend inline fun <reified T> send(
        path: String,
        authorized: Boolean = false,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): Result<T> = runCatching {
        val response = httpClient.request(baseUrl + path) {
            contentType(ContentType.Application.Json)
            if (authorized) {
                sessionRepository.token()?.let { bearerAuth(it) }
            }
            block()
        }
        response.toResult<T>()
    }.fold(
        onSuccess = { it },
        onFailure = { Result.failure(it.toAppFailureException()) },
    )

    suspend fun sendUnit(
        path: String,
        authorized: Boolean = false,
        block: HttpRequestBuilder.() -> Unit = {},
    ): Result<Unit> = runCatching {
        val response = httpClient.request(baseUrl + path) {
            contentType(ContentType.Application.Json)
            if (authorized) {
                sessionRepository.token()?.let { bearerAuth(it) }
            }
            block()
        }
        response.toUnitResult()
    }.fold(
        onSuccess = { it },
        onFailure = { Result.failure(it.toAppFailureException()) },
    )

    @PublishedApi
    internal suspend inline fun <reified T> HttpResponse.toResult(): Result<T> {
        if (status == HttpStatusCode.Unauthorized) {
            sessionRepository.clearToken()
            return Result.failure(AppFailureException(AppFailure.Unauthorized))
        }
        if (status.value in 200..299) {
            return Result.success(body())
        }
        return Result.failure(AppFailureException(readFailure()))
    }

    private suspend fun HttpResponse.toUnitResult(): Result<Unit> {
        if (status == HttpStatusCode.Unauthorized) {
            sessionRepository.clearToken()
            return Result.failure(AppFailureException(AppFailure.Unauthorized))
        }
        if (status.value in 200..299) {
            return Result.success(Unit)
        }
        return Result.failure(AppFailureException(readFailure()))
    }

    @PublishedApi
    internal suspend fun HttpResponse.readFailure(): AppFailure {
        val text = bodyAsText()
        val apiFailure = runCatching {
            json.decodeFromString<ApiErrorDto>(text).toFailure()
        }.getOrNull()
        return apiFailure ?: AppFailure.Unknown
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://localhost:8080"

        private val json = Json {
            ignoreUnknownKeys = true
        }

        fun defaultHttpClient(): HttpClient = HttpClient {
            expectSuccess = false
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
            }
        }
    }
}

@PublishedApi
internal fun Throwable.toAppFailureException(): AppFailureException = when (this) {
    is AppFailureException -> this
    is TimeoutCancellationException -> AppFailureException(AppFailure.Timeout)
    is IOException -> AppFailureException(AppFailure.NetworkUnavailable)
    is SerializationException -> AppFailureException(AppFailure.Unknown)
    else -> AppFailureException(AppFailure.Unknown)
}
