package app.zerno.feature.settings.domain

import app.zerno.data.db.bible.InstalledTranslation
import app.zerno.data.net.RemoteTranslation
import app.zerno.entities.bible.TranslationId

/**
 * A translation the backend offers for download. [installed] is true once it is on the device; [updateAvailable]
 * when the server's file is newer than the one downloaded.
 */
data class TranslationOffer(val translation: RemoteTranslation, val installed: Boolean, val updateAvailable: Boolean)

/**
 * The catalog's downloadable translations in its order. Bundled ones are left out: the app ships them and updates
 * them with a release. [downloadedVersions] maps an id to the `file_version` downloaded last.
 */
internal fun offersOf(
    catalog: List<RemoteTranslation>,
    installed: List<InstalledTranslation>,
    downloadedVersions: Map<String, String?>,
): List<TranslationOffer> {
    val installedIds = installed.map { it.id.value }.toSet()
    return catalog.filterNot { it.bundled }.map { translation ->
        val onDevice = translation.id in installedIds
        val version = downloadedVersions[translation.id]
        TranslationOffer(
            translation = translation,
            installed = onDevice,
            updateAvailable = onDevice && version != null && version != translation.fileVersion,
        )
    }
}

/**
 * The translation to read after [deleted] is removed while it was [current]: a bundled one in the same language,
 * else any bundled one. Null when [deleted] was not the current translation and nothing needs to change.
 */
internal fun translationAfterDeleting(
    deleted: TranslationId,
    current: TranslationId?,
    installed: List<InstalledTranslation>,
): TranslationId? {
    if (current != deleted) return null
    val language = installed.firstOrNull { it.id == deleted }?.language
    val bundled = installed.filter { it.bundled && it.id != deleted }
    return (bundled.firstOrNull { it.language == language } ?: bundled.firstOrNull())?.id
}
