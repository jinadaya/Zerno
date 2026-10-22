package app.zerno.feature.reader.domain

import app.zerno.entities.bible.Book
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Verse
import app.zerno.entities.bible.Versification

/** A translation the reader can open. [name] is the file's own, e.g. "King James Version". */
data class ReaderTranslation(val id: TranslationId, val name: String, val language: String)

/**
 * What the reader needs to know about a translation besides its text.
 *
 * @property psalmName the word for one psalm ("Psalm", "Псалом"), used in chapter titles instead of the book name;
 * null when the file does not say.
 */
data class TranslationDetails(val versification: Versification, val psalmName: String?)

/** Bible text on this device, as the reader reads it. */
interface BibleSource {
    /** Installed translations, bundled first. */
    suspend fun translations(): List<ReaderTranslation>

    suspend fun details(translation: TranslationId): TranslationDetails

    /** Books in display order with the translation's own names. */
    suspend fun books(translation: TranslationId): List<Book>

    /** Every verse of [chapter]; empty when the translation does not have it. */
    suspend fun chapter(translation: TranslationId, chapter: ChapterRef): List<Verse>
}
