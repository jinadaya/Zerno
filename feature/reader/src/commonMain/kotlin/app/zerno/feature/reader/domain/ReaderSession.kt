package app.zerno.feature.reader.domain

import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.mapVersification
import app.zerno.feature.reader.api.PassageProgress
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
     * Opens where the reader was last, or at the first chapter of [preferredTranslation] (the one chosen on first
     * run), else of the first translation in [preferredLanguage], else of any. Does nothing once a place is open.
     */
    suspend fun start(preferredTranslation: TranslationId?, preferredLanguage: String) = lock.withLock {
        if (_location.value != null) return@withLock
        val translations = bible.translations()
        check(translations.isNotEmpty()) { "No Bible translation is installed" }
        val saved = prefs.lastLocation()?.takeIf { last -> translations.any { it.id == last.translation } }
        _location.value = saved ?: run {
            val translation = translations.firstOrNull { it.id == preferredTranslation }
                ?: translations.firstOrNull { it.language.equals(preferredLanguage, ignoreCase = true) }
                ?: translations.first()
            ReaderLocation(translation.id, ChapterRef(bible.books(translation.id).first().id, 1))
        }
    }

    /**
     * Opens the start of [passage], written in KJV numbering, in the current translation. Must follow [start].
     * A passage starting at a chapter opens at its top. With [resume], opens at the verse its first chapter was left
     * at instead, if any.
     */
    suspend fun openPassage(passage: PassageRef, resume: Boolean = false) = lock.withLock {
        val current = checkNotNull(_location.value) { "The reader has not started" }
        val numbering = bible.details(current.translation).versification
        val mapped = passage.mapVersification(Versification.English, numbering)
        val chapter = ChapterRef(passage.book, mapped.startChapter)
        val saved = if (resume) prefs.position(numbering, chapter) else null
        _location.value = ReaderLocation(current.translation, chapter, saved ?: mapped.startVerse)
    }

    /**
     * How far the first chapter of [passage] (KJV numbering) was read in the current translation, or the last one
     * used before the reader started. Null on a fresh install, or when the saved verse is not past the passage's
     * start.
     */
    suspend fun progressIn(passage: PassageRef): PassageProgress? = lock.withLock {
        val translation = _location.value?.translation ?: prefs.lastLocation()?.translation
        if (translation == null || bible.translations().none { it.id == translation }) return@withLock null
        val numbering = bible.details(translation).versification
        val mapped = passage.mapVersification(Versification.English, numbering)
        val chapter = ChapterRef(passage.book, mapped.startChapter)
        val verse = prefs.position(numbering, chapter)?.takeIf { it > (mapped.startVerse ?: 1) }
        val verses = bible.chapter(translation, chapter).size
        if (verse == null || verses == 0) null else PassageProgress(verse.coerceAtMost(verses), verses)
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
