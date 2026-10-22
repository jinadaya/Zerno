package app.zerno.feature.reader.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Versification
import app.zerno.feature.reader.domain.ReaderLocation
import app.zerno.feature.reader.domain.ReaderPrefs

/**
 * [ReaderPrefs] as `reader.*` rows of the `app_meta` key-value table, so the prototype needs no schema change.
 * Values the app cannot read back (an older format, a hand edit) count as missing.
 *
 * - `reader.location`: `kjv|500|3|16`, translation, book, chapter and verse (empty for the top of the chapter).
 * - `reader.position.english.500.3`: `16`, or empty for the top of the chapter.
 * - `reader.font-size`: `20`.
 */
class AppMetaReaderPrefs(private val meta: AppMetaDao) : ReaderPrefs {

    override suspend fun lastLocation(): ReaderLocation? = meta.get(LOCATION)?.let(::parseLocation)

    override suspend fun saveLastLocation(location: ReaderLocation) {
        val verse = location.verse?.toString().orEmpty()
        meta.put(
            AppMeta(
                LOCATION,
                "${location.translation.value}|${location.chapter.book.value}|${location.chapter.chapter}|$verse",
            ),
        )
    }

    override suspend fun position(versification: Versification, chapter: ChapterRef): Int? =
        meta.get(positionKey(versification, chapter))?.toIntOrNull()?.takeIf { it > 0 }

    override suspend fun savePosition(versification: Versification, chapter: ChapterRef, verse: Int?) {
        meta.put(AppMeta(positionKey(versification, chapter), verse?.toString().orEmpty()))
    }

    override suspend fun fontSize(): Int? = meta.get(FONT_SIZE)?.toIntOrNull()

    override suspend fun saveFontSize(size: Int) {
        meta.put(AppMeta(FONT_SIZE, size.toString()))
    }

    private fun positionKey(versification: Versification, chapter: ChapterRef) =
        "$POSITION.${versification.name.lowercase()}.${chapter.book.value}.${chapter.chapter}"

    private fun parseLocation(value: String): ReaderLocation? = runCatching {
        val (translation, book, chapter, verse) = value.split('|').also { require(it.size == 4) }
        ReaderLocation(
            translation = TranslationId(translation),
            chapter = ChapterRef(BookId(book.toInt()), chapter.toInt()),
            verse = verse.toIntOrNull()?.takeIf { it > 0 },
        )
    }.getOrNull()

    private companion object {
        const val LOCATION = "reader.location"
        const val POSITION = "reader.position"
        const val FONT_SIZE = "reader.font-size"
    }
}
