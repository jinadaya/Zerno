package app.zerno.data.db.bible

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.data.hash.Sha256
import app.zerno.data.net.ApiConfig
import app.zerno.data.net.RemoteTranslation
import app.zerno.data.net.TranslationsApi
import app.zerno.data.net.zernoHttpClient
import app.zerno.entities.bible.TranslationId
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TranslationDownloadsTest {
    private val fs = SystemFileSystem
    private val directory = Path(SystemTemporaryDirectory, "zerno-downloads-${Random.nextLong().toULong()}")
        .also { fs.createDirectories(it) }
    private val bytes = Random(7).nextBytes(5000)
    private val sha = Sha256().apply { update(bytes) }.hex()
    private val asv = RemoteTranslation(
        id = "asv",
        code = "ASV",
        name = "American Standard Version",
        language = "en",
        licence = "public-domain",
        attribution = "Public domain.",
        schemaVersion = 1,
        fileVersion = sha.take(16),
        size = bytes.size.toLong(),
        sha256 = sha,
        downloadUrl = "v1/translations/asv/file",
        bundled = false,
    )

    private val stored = mutableMapOf<String, String>()
    private val dao = object : AppMetaDao {
        override suspend fun get(key: String): String? = stored[key]

        override suspend fun put(meta: AppMeta) {
            stored[meta.key] = meta.value
        }
    }
    private val store = TranslationStore(directory, dao, bundled = emptyList())
    private val installed = Path(directory, "asv.SQLite3")
    private val part = Path(directory, "asv.SQLite3.part")

    private fun downloads(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Pair<TranslationDownloads, MockEngine> {
        val engine = MockEngine(handler)
        val client = zernoHttpClient(ApiConfig("http://backend.test/", maxRetries = 0), engine = engine)
        return TranslationDownloads(TranslationsApi(client), store, dao) to engine
    }

    private fun MockRequestHandleScope.file(from: Int = 0) = respond(
        content = bytes.copyOfRange(from, bytes.size),
        status = if (from > 0) HttpStatusCode.PartialContent else HttpStatusCode.OK,
    )

    private fun read(path: Path) = fs.source(path).buffered().use { it.readByteArray() }

    private fun writePart(content: ByteArray) = fs.sink(part).buffered().use { it.write(content) }

    @Test
    fun theCatalogListsTheServedTranslations() = runTest {
        val (downloads, _) = downloads { request ->
            assertEquals("http://backend.test/v1/translations", request.url.toString())
            respond(
                """{"version": "v", "translations": [{"id": "asv", "translation_code": "ASV",
                   "display_name": "American Standard Version", "language_id": "en", "licence": "public-domain",
                   "attribution": "Public domain.", "schema_version": 1, "file_version": "${asv.fileVersion}",
                   "size": ${asv.size}, "sha256": "$sha", "download_url": "v1/translations/asv/file",
                   "bundled": false, "added_later": true}]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        assertEquals(listOf(asv), downloads.catalog())
    }

    @Test
    fun aDownloadIsCheckedAndMovedIntoPlace() = runTest {
        val progress = mutableListOf<Long>()
        val (downloads, engine) = downloads { request ->
            assertEquals("http://backend.test/v1/translations/asv/file", request.url.toString())
            assertNull(request.headers[HttpHeaders.Range])
            file()
        }
        downloads.download(asv) { received, total ->
            assertEquals(asv.size, total)
            progress += received
        }
        assertContentEquals(bytes, read(installed))
        assertFalse(fs.exists(part))
        assertEquals(0L, progress.first())
        assertEquals(asv.size, progress.last())
        assertEquals(asv.fileVersion, downloads.downloadedVersion("asv"))
        assertEquals(1, engine.requestHistory.size)
    }

    @Test
    fun aDroppedDownloadGoesOnFromWhatArrived() = runTest {
        writePart(bytes.copyOfRange(0, 1200))
        var calls = 0
        val (downloads, _) = downloads { request ->
            calls++
            if (calls == 1) throw IOException("connection dropped")
            assertEquals("bytes=1200-", request.headers[HttpHeaders.Range])
            assertEquals("\"$sha\"", request.headers[HttpHeaders.IfRange])
            file(from = 1200)
        }
        downloads.download(asv)
        assertEquals(2, calls)
        assertContentEquals(bytes, read(installed))
    }

    @Test
    fun aChangedFileOnTheServerStartsOver() = runTest {
        writePart(ByteArray(1200) { 1 })
        // The server's If-Range check fails and it sends the whole file with 200.
        val (downloads, _) = downloads { file() }
        downloads.download(asv)
        assertContentEquals(bytes, read(installed))
    }

    @Test
    fun aWrongFileIsNeverInstalled() = runTest {
        val (downloads, _) = downloads { file() }
        assertFailsWith<ChecksumMismatchException> { downloads.download(asv.copy(sha256 = "0".repeat(64))) }
        assertFalse(fs.exists(installed))
        assertFalse(fs.exists(part))
        assertNull(downloads.downloadedVersion("asv"))
    }

    @Test
    fun theNetworkErrorReachesTheCallerAfterTheLastAttempt() = runTest {
        var calls = 0
        val (downloads, _) = downloads {
            calls++
            throw IOException("offline")
        }
        assertFailsWith<IOException> { downloads.download(asv) }
        assertEquals(3, calls)
        assertFalse(fs.exists(installed))
    }

    @Test
    fun deletingRemovesTheFileButNotABundledOne() = runTest {
        val (downloads, _) = downloads { file() }
        downloads.download(asv)
        downloads.delete("asv")
        assertFalse(fs.exists(installed))
        assertNull(downloads.downloadedVersion("asv"))

        val kjv = BundledTranslation(TranslationId("kjv"), version = 1)
        val withBundled = TranslationStore(directory, dao, bundled = listOf(kjv))
        assertFailsWith<IllegalArgumentException> { withBundled.delete(TranslationId("kjv")) }
    }
}
