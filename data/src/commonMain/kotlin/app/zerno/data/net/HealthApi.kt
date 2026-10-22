package app.zerno.data.net

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException

/** `GET /health`: the backend answers `OK` when it is up. */
class HealthApi(private val client: HttpClient) {
    suspend fun isUp(): Boolean = try {
        client.get("health").bodyAsText().trim() == "OK"
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }
}
