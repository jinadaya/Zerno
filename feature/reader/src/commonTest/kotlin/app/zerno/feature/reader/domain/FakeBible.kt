package app.zerno.feature.reader.domain

import app.zerno.entities.bible.Book
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Verse
import app.zerno.entities.bible.VerseText
import app.zerno.entities.bible.Versification

val KJV = TranslationId("kjv")
val RST = TranslationId("rst")
val GENESIS = BookId(10)
val PSALMS = BookId(230)
val JONAH = BookId(390)
val JOHN = BookId(500)

/** RST and KJV with a few books; every chapter has 30 verses. */
class FakeBible(
    private val translations: List<ReaderTranslation> = listOf(
        ReaderTranslation(RST, "Синодальный перевод", "ru"),
        ReaderTranslation(KJV, "King James Version", "en"),
    ),
) : BibleSource {
    override suspend fun translations() = translations

    override suspend fun details(translation: TranslationId) = when (translation) {
        RST -> TranslationDetails(Versification.Russian, "Псалом")
        else -> TranslationDetails(Versification.English, "Psalm")
    }

    override suspend fun books(translation: TranslationId): List<Book> = listOf(
        Book(GENESIS, "Gen", "Genesis", 50),
        Book(PSALMS, "Ps", "Psalms", if (translation == RST) 151 else 150),
        Book(JONAH, "Jonah", "Jonah", 4),
        Book(JOHN, "John", "John", 21),
    )

    override suspend fun chapter(translation: TranslationId, chapter: ChapterRef): List<Verse> =
        (1..30).map { Verse(translation, chapter, it, VerseText("Verse $it")) }
}

class FakePrefs : ReaderPrefs {
    var last: ReaderLocation? = null
    val positions = mutableMapOf<Pair<Versification, ChapterRef>, Int?>()
    var size: Int? = null

    override suspend fun lastLocation() = last
    override suspend fun saveLastLocation(location: ReaderLocation) {
        last = location
    }

    override suspend fun position(versification: Versification, chapter: ChapterRef) = positions[versification to chapter]
    override suspend fun savePosition(versification: Versification, chapter: ChapterRef, verse: Int?) {
        positions[versification to chapter] = verse
    }

    override suspend fun fontSize() = size
    override suspend fun saveFontSize(size: Int) {
        this.size = size
    }
}
