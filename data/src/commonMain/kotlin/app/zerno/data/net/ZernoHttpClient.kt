package app.zerno.data.net

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Where the backend is and how loud the client is. The app picks it per build type: a local backend with logging in
 * debug, the production one without in release.
 */
data class ApiConfig(val baseUrl: String, val logging: Boolean = false, val maxRetries: Int = 3)

/** Access and refresh token of the signed-in user. */
data class AuthTokens(val access: String, val refresh: String?)

/**
 * Bearer tokens for the backend, implemented by the account feature (AC-01). [tokens] is read before the first
 * request; [refresh] is called once after a 401 with the tokens that failed and returns new ones, or `null` to give
 * up (the request then fails with 401).
 */
interface AuthTokenProvider {
    suspend fun tokens(): AuthTokens?

    suspend fun refresh(expired: AuthTokens?): AuthTokens?

    /** No account yet: requests go out without `Authorization`. */
    object None : AuthTokenProvider {
        override suspend fun tokens(): AuthTokens? = null

        override suspend fun refresh(expired: AuthTokens?): AuthTokens? = null
    }
}

/** JSON as the backend writes it; unknown fields are ignored so the backend can add them. */
val ZernoJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * The one HttpClient for the backend: [ApiConfig.baseUrl] as the base of relative URLs, JSON, retries with
 * exponential backoff on network errors and 5xx, bearer auth from [tokens], and logging when [ApiConfig.logging]
 * (with `Authorization` redacted). [engine] is for tests; by default Ktor uses OkHttp on Android and Darwin on iOS.
 */
fun zernoHttpClient(
    config: ApiConfig,
    tokens: AuthTokenProvider = AuthTokenProvider.None,
    engine: HttpClientEngine? = null,
): HttpClient {
    val setup: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = true
        defaultRequest { url(config.baseUrl) }
        install(ContentNegotiation) { json(ZernoJson) }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = config.maxRetries)
            exponentialDelay()
        }
        install(Auth) {
            bearer {
                loadTokens { tokens.tokens()?.toBearer() }
                refreshTokens { tokens.refresh(oldTokens?.toAuth())?.toBearer() }
                // Send the token we have up front instead of waiting for a 401 challenge.
                sendWithoutRequest { true }
            }
        }
        if (config.logging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
        }
    }
    return if (engine != null) HttpClient(engine, setup) else HttpClient(setup)
}

private fun AuthTokens.toBearer() = BearerTokens(access, refresh)

private fun BearerTokens.toAuth() = AuthTokens(accessToken, refreshToken)
