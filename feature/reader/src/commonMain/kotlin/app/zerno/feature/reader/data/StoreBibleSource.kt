package app.zerno.feature.reader.data

import app.zerno.data.db.bible.BibleFile
import app.zerno.data.db.bible.TranslationStore
import app.zerno.entities.bible.Book
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Verse
import app.zerno.entities.bible.Versification
import app.zerno.feature.reader.domain.BibleSource
import app.zerno.feature.reader.domain.ReaderTranslation
import app.zerno.feature.reader.domain.TranslationDetails
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [BibleSource] over the files in [TranslationStore]. One file stays open, the one read last; asking for another
 * translation closes it. The translation list, details and book lists are read once; translation downloads (DA-05)
 * will need to refresh the list.
 */
class StoreBibleSource(private val store: TranslationStore) : BibleSource {
    private val lock = Mutex()
    private var open: BibleFile? = null
    private var translations: List<ReaderTranslation>? = null
    private val details = mutableMapOf<TranslationId, TranslationDetails>()
    private val books = mutableMapOf<TranslationId, List<Book>>()

    override suspend fun translations(): List<ReaderTranslation> = lock.withLock {
        translations ?: store.list().map { ReaderTranslation(it.id, it.name, it.language) }.also { translations = it }
    }

    override suspend fun details(translation: TranslationId): TranslationDetails = lock.withLock {
        details.getOrPut(translation) {
            val file = file(translation)
            TranslationDetails(
                versification = Versification.fromRussianNumbering(file.infoValue("russian_numbering")),
                psalmName = file.infoValue("chapter_string_ps")?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }

    override suspend fun books(translation: TranslationId): List<Book> = lock.withLock {
        books.getOrPut(translation) { file(translation).books() }
    }

    override suspend fun chapter(translation: TranslationId, chapter: ChapterRef): List<Verse> = lock.withLock {
        file(translation).chapter(chapter)
    }

    /** Call with [lock] held. */
    private suspend fun file(translation: TranslationId): BibleFile {
        open?.let { if (it.translation == translation) return it }
        open?.close()
        open = null
        return store.open(translation).also { open = it }
    }
}
