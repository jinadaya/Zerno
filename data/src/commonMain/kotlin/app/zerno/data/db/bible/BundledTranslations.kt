package app.zerno.data.db.bible

import app.zerno.data.resources.Res
import app.zerno.entities.bible.TranslationId
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * A Bible file shipped inside the app.
 *
 * @property version bump it when the file in `composeResources/files/bibles` changes, so installed apps copy the
 * new one over the old on their next launch.
 */
data class BundledTranslation(val id: TranslationId, val version: Int) {
    /** Path under `composeResources`, e.g. `files/bibles/kjv.SQLite3`. */
    val resource: String
        get() = "files/bibles/${id.value}$BIBLE_FILE_SUFFIX"

    companion object {
        val all: List<BundledTranslation> = listOf(
            BundledTranslation(TranslationId("rst"), version = 1),
            BundledTranslation(TranslationId("kjv"), version = 1),
        )
    }
}

/** Reads a bundled file's bytes. Tests swap in their own; the app reads Compose resources. */
fun interface BundledBibleSource {
    suspend fun read(resource: String): ByteArray
}

object ResourceBundledBibles : BundledBibleSource {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun read(resource: String): ByteArray = Res.readBytes(resource)
}

/** Bible files are named `{id}.SQLite3` (schema v1, section 1). */
const val BIBLE_FILE_SUFFIX = ".SQLite3"
