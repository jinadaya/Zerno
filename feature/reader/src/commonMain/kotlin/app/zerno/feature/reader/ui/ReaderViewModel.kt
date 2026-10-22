package app.zerno.feature.reader.ui

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.SavedStateHandle
import app.zerno.entities.bible.ChapterRef
import app.zerno.feature.reader.domain.BibleSource
import app.zerno.feature.reader.domain.ReaderFontSize
import app.zerno.feature.reader.domain.ReaderLocation
import app.zerno.feature.reader.domain.ReaderPrefs
import app.zerno.feature.reader.domain.ReaderSession
import app.zerno.feature.reader.domain.chapterAfter
import app.zerno.feature.reader.domain.chapterBefore
import app.zerno.feature.reader.domain.chapterTitle
import app.zerno.feature.reader.ui.ReaderInnerEvent.FontSizeChanged
import app.zerno.feature.reader.ui.ReaderInnerEvent.LoadFailed
import app.zerno.feature.reader.ui.ReaderInnerEvent.PageLoaded
import app.zerno.feature.reader.ui.ReaderInnerEvent.SettingsShown
import app.zerno.feature.reader.ui.ReaderInnerEvent.TranslationsLoaded
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

/**
 * The reader page. Follows [ReaderSession.location], so the book picker opens a chapter by telling the session.
 * The verse at the top of the page is written down shortly after scrolling stops.
 */
class ReaderViewModel(
    savedStateHandle: SavedStateHandle,
    private val session: ReaderSession,
    private val bible: BibleSource,
    private val prefs: ReaderPrefs,
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
            session.location.filterNotNull().collectLatest { location ->
                topVerse = location.verse
                emit(load(location))
            }
        }
        intent(START) {
            emit(FontSizeChanged(prefs.fontSize() ?: ReaderFontSize.DEFAULT))
            try {
                emit(TranslationsLoaded(bible.translations()))
                session.start(Locale.current.language)
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
            is ReaderEvent.TopVerseChanged -> {
                topVerse = event.verse
                if (page != null) {
                    intent(SAVE_POSITION) {
                        delay(SAVE_POSITION_DELAY_MS)
                        session.savePosition(page.location.copy(verse = event.verse))
                    }
                }
            }
            ReaderEvent.BookPickerClicked -> intent(NAVIGATE) { postEffect(ReaderEffect.OpenBookPicker) }
            ReaderEvent.PreviousChapterClicked -> page?.previous?.let { open(it.chapter) }
            ReaderEvent.NextChapterClicked -> page?.next?.let { open(it.chapter) }
            ReaderEvent.SettingsClicked -> intent(SETTINGS) { emit(SettingsShown(!state.value.settingsOpen)) }
            ReaderEvent.SettingsDismissed -> intent(SETTINGS) { emit(SettingsShown(false)) }
            is ReaderEvent.FontSizeStepped -> {
                val size = ReaderFontSize.step(state.value.fontSize, event.steps)
                intent(FONT_SIZE) {
                    emit(FontSizeChanged(size))
                    prefs.saveFontSize(size)
                }
            }
            is ReaderEvent.TranslationSelected -> if (page != null) {
                val from = page.location.copy(verse = topVerse)
                intent(OPEN) { session.switchTranslation(from, event.id) }
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
        const val FONT_SIZE = "font-size"
        const val SAVE_POSITION = "save-position"
        const val SAVE_POSITION_DELAY_MS = 400L
    }
}
