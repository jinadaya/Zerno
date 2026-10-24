package app.zerno.data.net

import app.zerno.entities.bible.TranslationId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One entry of the backend's `GET /v1/translations` (BE-04). [fileVersion] changes whenever the file does;
 * [downloadUrl] is relative to the API base URL, and the client resolves it against that.
 */
@Serializable
data class RemoteTranslation(
    @SerialName("id") val id: String,
    @SerialName("translation_code") val code: String,
    @SerialName("display_name") val name: String,
    @SerialName("language_id") val language: String,
    @SerialName("licence") val licence: String,
    @SerialName("attribution") val attribution: String,
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("file_version") val fileVersion: String,
    @SerialName("size") val size: Long,
    @SerialName("sha256") val sha256: String,
    @SerialName("download_url") val downloadUrl: String,
    @SerialName("bundled") val bundled: Boolean,
) {
    val translationId: TranslationId get() = TranslationId(id)
}

@Serializable
private data class TranslationCatalogBody(@SerialName("translations") val translations: List<RemoteTranslation>)

/** The backend's translation catalog and files (BE-04, BE-05). */
class TranslationsApi(private val client: HttpClient) {

    suspend fun catalog(): List<RemoteTranslation> = client.get("v1/translations").body<TranslationCatalogBody>()
        .translations

    /**
     * Streams [translation]'s file from byte [offset] into [read]. A resume asks for the rest only if the file is
     * still the one with [RemoteTranslation.sha256] (`If-Range`), so [read] learns from `resumed` whether the body
     * continues at [offset] (206) or is the whole file again (200) and what came before must be dropped.
     */
    suspend fun <T> download(
        translation: RemoteTranslation,
        offset: Long,
        read: suspend (resumed: Boolean, body: ByteReadChannel) -> T,
    ): T = client.prepareGet(translation.downloadUrl) {
        if (offset > 0) {
            header(HttpHeaders.Range, "bytes=$offset-")
            header(HttpHeaders.IfRange, "\"${translation.sha256}\"")
        }
    }.execute { response ->
        read(offset > 0 && response.status == HttpStatusCode.PartialContent, response.bodyAsChannel())
    }
}
