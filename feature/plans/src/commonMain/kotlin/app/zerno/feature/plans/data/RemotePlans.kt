package app.zerno.feature.plans.data

import app.zerno.data.net.ZernoJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The plan files the backend serves (`GET /v1/plans`), with the content version it sends as an `ETag`. */
class RemotePlans(private val client: HttpClient) {
    /** One answer: [files] in list order, or null when [version] is the one already held. */
    data class Served(val version: String, val files: List<String>?)

    /** Throws when the backend cannot be reached or answers with an error. */
    suspend fun fetch(knownVersion: String?): Served {
        val response = client.get("v1/plans") {
            expectSuccess = false
            if (knownVersion != null) header(HttpHeaders.IfNoneMatch, "\"$knownVersion\"")
        }
        if (response.status == HttpStatusCode.NotModified && knownVersion != null) return Served(knownVersion, null)
        check(response.status.isSuccess()) { "Plans answered ${response.status}" }
        val body = ZernoJson.parseToJsonElement(response.bodyAsText()).jsonObject
        return Served(
            version = body.getValue("version").jsonPrimitive.content,
            files = body.getValue("plans").jsonArray.map { it.toString() },
        )
    }
}
