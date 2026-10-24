package app.zerno.feature.settings.data

import app.zerno.data.db.bible.TranslationDownloads
import app.zerno.data.net.RemoteTranslation

/** The translations the backend offers, and the ones this device downloaded from it. */
interface TranslationLibrary {
    suspend fun catalog(): List<RemoteTranslation>

    /** The `file_version` downloaded last, or null if [id] is not a download. */
    suspend fun downloadedVersion(id: String): String?

    /** Downloads and installs [translation], reporting the share received so far (0 to 1) to [onProgress]. */
    suspend fun download(translation: RemoteTranslation, onProgress: (Float) -> Unit)

    suspend fun delete(id: String)
}

internal class DownloadsTranslationLibrary(private val downloads: TranslationDownloads) : TranslationLibrary {
    override suspend fun catalog(): List<RemoteTranslation> = downloads.catalog()

    override suspend fun downloadedVersion(id: String): String? = downloads.downloadedVersion(id)

    override suspend fun download(translation: RemoteTranslation, onProgress: (Float) -> Unit) =
        downloads.download(translation) { received, total ->
            onProgress(if (total > 0) received.toFloat() / total else 0f)
        }

    override suspend fun delete(id: String) = downloads.delete(id)
}
