package app.zerno.feature.reader.domain

import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The place the reader shows, shared by the reader and the book picker. Keep one per app.
 *
 * [location] is the last place asked for, with the verse to open at; the reader follows it. Scrolling does not
 * change it: [savePosition] only writes the place down, so a page is never reloaded under the reader's finger.
 */
class ReaderSession(private val bible: BibleSource, private val prefs: ReaderPrefs) {
    private val lock = Mutex()
    private val _location = MutableStateFlow<ReaderLocation?>(null)
    val location: StateFlow<ReaderLocation?> = _location.asStateFlow()

    /**
     * Opens where the reader was last, or at the first chapter of the first translation in [preferredLanguage]
     * (any translation when none is). Does nothing once a place is open.
     */
    suspend fun start(preferredLanguage: String) = lock.withLock {
        if (_location.value != null) return@withLock
        val translations = bible.translations()
        check(translations.isNotEmpty()) { "No Bible translation is installed" }
        val saved = prefs.lastLocation()?.takeIf { last -> translations.any { it.id == last.translation } }
        _location.value = saved ?: run {
            val translation = translations.firstOrNull { it.language.equals(preferredLanguage, ignoreCase = true) }
                ?: translations.first()
            ReaderLocation(translation.id, ChapterRef(bible.books(translation.id).first().id, 1))
        }
    }

    /** Opens [chapter] of the current translation at the verse it was left at. */
    suspend fun openChapter(chapter: ChapterRef) = lock.withLock {
        val current = checkNotNull(_location.value) { "The reader has not started" }
        val versification = bible.details(current.translation).versification
        _location.value = ReaderLocation(current.translation, chapter, prefs.position(versification, chapter))
    }

    /**
     * Opens [translation] at the place [from] shows, mapped to its numbering. A book the translation does not have
     * opens its first chapter instead.
     */
    suspend fun switchTranslation(from: ReaderLocation, translation: TranslationId) = lock.withLock {
        if (from.translation == translation) return@withLock
        val mapped = from.inTranslation(
            target = translation,
            from = bible.details(from.translation).versification,
            to = bible.details(translation).versification,
        )
        val books = bible.books(translation)
        val book = books.firstOrNull { it.id == mapped.chapter.book }
        _location.value = when {
            book == null -> ReaderLocation(translation, ChapterRef(books.first().id, 1))

            mapped.chapter.chapter > book.chapterCount -> ReaderLocation(
                translation,
                ChapterRef(book.id, book.chapterCount),
            )

            else -> mapped
        }
    }

    /** Writes down that [location]'s verse is at the top of the page, for the chapter and as the last place. */
    suspend fun savePosition(location: ReaderLocation) = lock.withLock {
        prefs.savePosition(bible.details(location.translation).versification, location.chapter, location.verse)
        prefs.saveLastLocation(location)
    }
}
