package app.zerno.data.net

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZernoHttpClientTest {
    private val config = ApiConfig(baseUrl = "http://backend.test/", maxRetries = 1)

    @Test
    fun healthIsUpOnOk() = runTest {
        val engine = MockEngine { request ->
            assertEquals("http://backend.test/health", request.url.toString())
            respond("OK")
        }
        assertTrue(HealthApi(zernoHttpClient(config, engine = engine)).isUp())
    }

    @Test
    fun healthIsDownWhenTheServerKeepsFailing() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }
        assertFalse(HealthApi(zernoHttpClient(config, engine = engine)).isUp())
        // The first try and one retry.
        assertEquals(2, engine.requestHistory.size)
    }

    @Test
    fun serverErrorIsRetried() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            if (calls == 1) respondError(HttpStatusCode.BadGateway) else respond("OK")
        }
        assertEquals("OK", zernoHttpClient(config, engine = engine).get("health").bodyAsText())
        assertEquals(2, calls)
    }

    @Test
    fun noAccountSendsNoAuthorization() = runTest {
        val engine = MockEngine { request ->
            assertNull(request.headers[HttpHeaders.Authorization])
            respond("OK")
        }
        zernoHttpClient(config, engine = engine).get("health")
    }

    @Test
    fun expiredTokenIsRefreshedOnceAndTheRequestRepeated() = runTest {
        val tokens = object : AuthTokenProvider {
            var refreshedFrom: AuthTokens? = null

            override suspend fun tokens() = AuthTokens("old", "r1")

            override suspend fun refresh(expired: AuthTokens?): AuthTokens {
                refreshedFrom = expired
                return AuthTokens("new", "r2")
            }
        }
        val engine = MockEngine { request ->
            when (request.headers[HttpHeaders.Authorization]) {
                "Bearer new" -> respond("OK")
                else -> respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
            }
        }
        assertEquals("OK", zernoHttpClient(config, tokens, engine).get("me").bodyAsText())
        assertEquals(AuthTokens("old", "r1"), tokens.refreshedFrom)
        assertEquals(listOf("Bearer old", "Bearer new"), engine.requestHistory.map { it.headers[HttpHeaders.Authorization] })
    }
}
