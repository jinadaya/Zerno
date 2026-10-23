package app.zerno.feature.reader.ui

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.SavedStateHandle
import app.zerno.data.prefs.AppPreferences
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.feature.reader.domain.BibleSource
import app.zerno.feature.reader.domain.DailyReading
import app.zerno.feature.reader.domain.ReaderFontSize
import app.zerno.feature.reader.domain.ReaderLocation
import app.zerno.feature.reader.domain.ReaderPrefs
import app.zerno.feature.reader.domain.ReaderSession
import app.zerno.feature.reader.domain.chapterAfter
import app.zerno.feature.reader.domain.chapterBefore
import app.zerno.feature.reader.domain.chapterTitle
import app.zerno.feature.reader.domain.kjvPassage
import app.zerno.feature.reader.domain.shareText
import app.zerno.feature.reader.ui.ReaderInnerEvent.BookmarksLoaded
import app.zerno.feature.reader.ui.ReaderInnerEvent.FinishFailed
import app.zerno.feature.reader.ui.ReaderInnerEvent.Finishing
import app.zerno.feature.reader.ui.ReaderInnerEvent.FontSizeChanged
import app.zerno.feature.reader.ui.ReaderInnerEvent.LoadFailed
import app.zerno.feature.reader.ui.ReaderInnerEvent.PageLoaded
import app.zerno.feature.reader.ui.ReaderInnerEvent.PageThemeChanged
import app.zerno.feature.reader.ui.ReaderInnerEvent.SettingsShown
import app.zerno.feature.reader.ui.ReaderInnerEvent.TranslationsLoaded
import app.zerno.feature.reader.ui.ReaderInnerEvent.VerseSelected
import app.zerno.feature.reflection.api.Bookmarks
import app.zerno.feature.reflection.api.ReflectionRoute
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * The reader page. Follows [ReaderSession.location], so the book picker opens a chapter by telling the session.
 * The verse at the top of the page is written down shortly after scrolling stops.
 */
class ReaderViewModel(
    savedStateHandle: SavedStateHandle,
    private val session: ReaderSession,
    private val bible: BibleSource,
    private val prefs: ReaderPrefs,
    private val appPrefs: AppPreferences,
    private val daily: DailyReading,
    private val bookmarks: Bookmarks,
) : MviViewModel<ReaderState, ReaderEvent, ReaderInnerEvent, ReaderEffect, ScreenSnapshot.NoSnapshot>(
    savedStateHandle = savedStateHandle,
    initialState = ReaderState(),
    reducer = ReaderReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(ReaderState()),
) {
    /** The verse at the top of the page as last reported; the start of a translation switch. */
    private var topVerse: Int? = null

    init {
        subscription(PAGE) {
            // A plan day beginning or ending changes whether the page ends with "Finish reading".
            combine(session.location.filterNotNull(), daily.current) { location, _ ->
                location
            }.collectLatest { location ->
                topVerse = location.verse
                emit(load(location))
            }
        }
        subscription(BOOKMARKS) {
            bookmarks.all.collect { all -> emit(BookmarksLoaded(all.associate { it.passage to it.id })) }
        }
        // Text size and page colour are app settings, so a change in settings shows here at once.
        subscription(LOOK) {
            appPrefs.prefs.collect {
                emit(FontSizeChanged(it.fontSize ?: ReaderFontSize.DEFAULT))
                emit(PageThemeChanged(it.pageTheme))
            }
        }
        // The chosen translation is an app setting too: the page follows it, wherever it was chosen.
        subscription(TRANSLATION) {
            combine(
                session.location.filterNotNull(),
                appPrefs.prefs.map { it.translation }.distinctUntilChanged(),
            ) { location, chosen -> location to chosen }
                .collect { (location, chosen) -> follow(location, chosen) }
        }
        intent(START) {
            // Text size used to be kept by the reader alone.
            if (appPrefs.prefs.value.fontSize == null) prefs.fontSize()?.let(appPrefs::setFontSize)
            try {
                emit(TranslationsLoaded(bible.translations()))
                session.start(appPrefs.prefs.value.translation, Locale.current.language)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(LoadFailed)
            }
        }
    }

    override fun onEvent(event: ReaderEvent) {
        val page = state.value.page
        when (event) {
            is ReaderEvent.TopVerseChanged -> topVerseChanged(event.verse, page)

            ReaderEvent.BookPickerClicked -> intent(NAVIGATE) { postEffect(ReaderEffect.OpenBookPicker) }

            ReaderEvent.PreviousChapterClicked -> page?.previous?.let { open(it.chapter) }

            ReaderEvent.NextChapterClicked -> page?.next?.let { open(it.chapter) }

            ReaderEvent.SettingsClicked -> intent(SETTINGS) { emit(SettingsShown(!state.value.settingsOpen)) }

            ReaderEvent.SettingsDismissed -> intent(SETTINGS) { emit(SettingsShown(false)) }

            is ReaderEvent.FontSizeStepped -> {
                val size = ReaderFontSize.step(state.value.fontSize, event.steps)
                appPrefs.setFontSize(size)
            }

            is ReaderEvent.TranslationSelected -> appPrefs.setTranslation(event.id)

            ReaderEvent.FinishClicked -> finish(page)

            is VerseEvent -> onVerseEvent(event, page)
        }
    }

    private suspend fun follow(location: ReaderLocation, chosen: TranslationId?) {
        if (chosen == null || chosen == location.translation) return
        if (bible.translations().none { it.id == chosen }) return
        val from = if (location == state.value.page?.location) location.copy(verse = topVerse) else location
        session.switchTranslation(from, chosen)
    }

    private fun topVerseChanged(verse: Int?, page: ReaderPage?) {
        topVerse = verse
        if (page == null) return
        intent(SAVE_POSITION) {
            delay(SAVE_POSITION_DELAY_MS)
            session.savePosition(page.location.copy(verse = verse))
        }
    }

    private fun onVerseEvent(event: VerseEvent, page: ReaderPage?) {
        if (event is VerseEvent.VerseClicked) {
            val index = event.index.takeIf { it != state.value.selected }
            intent(VERSE) { emit(VerseSelected(index)) }
            return
        }
        val verse = state.value.selected?.let { page?.verses?.getOrNull(it) }
        intent(VERSE) { emit(VerseSelected(null)) }
        if (page == null || verse == null) return
        val language = BookNameLanguage.fromCode(page.translation.language)
        when (event) {
            VerseEvent.SaveClicked -> toggleBookmark(verse.kjvPassage(page.versification), page.location.translation)

            VerseEvent.NoteClicked -> intent(NAVIGATE) {
                postEffect(ReaderEffect.OpenReflection(ReflectionRoute(verse.kjvPassage(page.versification))))
            }

            VerseEvent.CopyClicked -> intent(NAVIGATE) { postEffect(ReaderEffect.CopyText(verse.shareText(language))) }

            VerseEvent.ShareClicked -> intent(NAVIGATE) {
                postEffect(ReaderEffect.ShareText(verse.shareText(language)))
            }

            VerseEvent.Dismissed, is VerseEvent.VerseClicked -> Unit
        }
    }

    // Storage can fail; the verse simply stays as it was.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun toggleBookmark(passage: PassageRef, translation: TranslationId) {
        val existing = state.value.bookmarks[passage]
        intent(BOOKMARK) {
            try {
                if (existing != null) bookmarks.delete(existing) else bookmarks.save(passage, translation)
            } catch (e: CancellationException) {
                throw e
            } catch (ignored: Exception) {
                // Nothing to show: the save mark follows the stored bookmarks.
            }
        }
    }

    // Recording can fail on storage; the button stays for another try.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun finish(page: ReaderPage?) {
        if (page == null || !page.canFinish || state.value.finishing) return
        intent(FINISH) {
            emit(Finishing)
            try {
                daily.finish(page.location.translation)
                postEffect(ReaderEffect.SessionFinished)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(FinishFailed)
            }
        }
    }

    private fun open(chapter: ChapterRef) = intent(OPEN) { session.openChapter(chapter) }

    private suspend fun load(location: ReaderLocation): ReaderInnerEvent = try {
        val translation = bible.translations().first { it.id == location.translation }
        val details = bible.details(location.translation)
        val books = bible.books(location.translation)
        val book = books.first { it.id == location.chapter.book }
        fun link(chapter: ChapterRef?) = chapter?.let { ref ->
            ChapterLink(ref, chapterTitle(books.first { it.id == ref.book }, ref.chapter, details.psalmName))
        }
        PageLoaded(
            ReaderPage(
                location = location,
                translation = translation,
                book = book,
                title = chapterTitle(book, location.chapter.chapter, details.psalmName),
                verses = bible.chapter(location.translation, location.chapter),
                previous = link(books.chapterBefore(location.chapter)),
                next = link(books.chapterAfter(location.chapter)),
                canFinish = daily.endsAt(location),
                versification = details.versification,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadFailed
    }

    private companion object {
        const val PAGE = "page"
        const val START = "start"
        const val OPEN = "open"
        const val NAVIGATE = "navigate"
        const val SETTINGS = "settings"
        const val LOOK = "look"
        const val TRANSLATION = "translation"
        const val SAVE_POSITION = "save-position"
        const val FINISH = "finish"
        const val VERSE = "verse"
        const val BOOKMARKS = "bookmarks"
        const val BOOKMARK = "bookmark"
        const val SAVE_POSITION_DELAY_MS = 400L
    }
}
