package app.zerno.data.db.bible

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.data.hash.Sha256
import app.zerno.data.net.RemoteTranslation
import app.zerno.data.net.TranslationsApi
import app.zerno.entities.bible.TranslationId
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/** The downloaded file is not the one the catalog describes; nothing was installed and the partial file is gone. */
class ChecksumMismatchException(id: String) : IllegalStateException("Translation $id did not match its checksum")

/**
 * Downloads translations from the backend catalog into the [TranslationStore] (DA-05).
 *
 * Bytes go to `{id}.SQLite3.part` next to the installed files. A dropped connection keeps what arrived and the
 * download goes on from there, up to [maxAttempts] times within one call and again on the next call; the server
 * sends the whole file instead if it changed meanwhile. Only a file whose SHA-256 matches the catalog is moved
 * into place, atomically, so the reader never sees a partial or wrong Bible.
 */
class TranslationDownloads(
    private val api: TranslationsApi,
    private val store: TranslationStore,
    private val meta: AppMetaDao,
    private val fileSystem: FileSystem = SystemFileSystem,
    private val maxAttempts: Int = 3,
) {
    suspend fun catalog(): List<RemoteTranslation> = api.catalog()

    /** The `file_version` of the copy downloaded last, or null if [id] was never downloaded (or was deleted). */
    suspend fun downloadedVersion(id: String): String? = meta.get(versionKey(id))?.takeIf { it.isNotEmpty() }

    /**
     * Downloads and installs [translation], reporting bytes received out of [RemoteTranslation.size] to
     * [onProgress] (called on the IO dispatcher). Throws [ChecksumMismatchException] for a wrong file, and the
     * network's [IOException] once the attempts run out; the partial file then stays for the next call.
     */
    suspend fun download(
        translation: RemoteTranslation,
        onProgress: (received: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        val id = translation.translationId
        val part = store.partFile(id)
        repeat(maxAttempts - 1) {
            try {
                fetch(translation, part, onProgress)
                return install(translation)
            } catch (_: IOException) {
                // A dropped connection: the next attempt goes on from what arrived.
            }
        }
        // The last attempt's failure is the caller's.
        fetch(translation, part, onProgress)
        install(translation)
    }

    private suspend fun install(translation: RemoteTranslation) {
        store.installDownloaded(translation.translationId)
        meta.put(AppMeta(versionKey(translation.id), translation.fileVersion))
    }

    /** Removes a downloaded translation; bundled ones stay. */
    suspend fun delete(id: String) {
        store.delete(TranslationId(id))
        meta.put(AppMeta(versionKey(id), ""))
    }

    /** Fills [part] up to the full size, then checks the whole file against the catalog's checksum. */
    private suspend fun fetch(translation: RemoteTranslation, part: Path, onProgress: (Long, Long) -> Unit) =
        withContext(Dispatchers.IO) {
            var received = fileSystem.metadataOrNull(part)?.size ?: 0L
            if (received > translation.size) {
                fileSystem.delete(part)
                received = 0
            }
            onProgress(received, translation.size)
            if (received < translation.size) {
                api.download(translation, received) { resumed, body ->
                    if (!resumed) received = 0
                    // Closing the sink keeps what arrived before a dropped connection, for the next attempt.
                    fileSystem.sink(part, append = resumed).buffered().use { sink ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = body.readAvailable(buffer)
                            if (read < 0) break
                            sink.write(buffer, 0, read)
                            received += read
                            onProgress(received, translation.size)
                        }
                    }
                }
            }
            if (received != translation.size || sha256(part) != translation.sha256) {
                fileSystem.delete(part, mustExist = false)
                throw ChecksumMismatchException(translation.id)
            }
        }

    private fun sha256(file: Path): String {
        val digest = Sha256()
        fileSystem.source(file).buffered().use { source ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = source.readAtMostTo(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.hex()
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024

        fun versionKey(id: String) = "downloaded.$id"
    }
}
