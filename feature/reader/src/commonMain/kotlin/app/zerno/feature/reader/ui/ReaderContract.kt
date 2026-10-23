package app.zerno.feature.reader.ui

import app.zerno.data.prefs.PageTheme
import app.zerno.entities.bible.Book
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Verse
import app.zerno.entities.bible.Versification
import app.zerno.entities.saved.BookmarkId
import app.zerno.feature.reader.domain.ReaderFontSize
import app.zerno.feature.reader.domain.ReaderLocation
import app.zerno.feature.reader.domain.ReaderTranslation
import app.zerno.feature.reflection.api.ReflectionRoute
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent

data class ReaderState(
    val page: ReaderPage? = null,
    /** The chapter could not be read; the page stays as it was. */
    val failed: Boolean = false,
    val translations: List<ReaderTranslation> = emptyList(),
    val fontSize: Int = ReaderFontSize.DEFAULT,
    val pageTheme: PageTheme = PageTheme.Light,
    val settingsOpen: Boolean = false,
    /** "Finish reading" was tapped and the session is being recorded. */
    val finishing: Boolean = false,
    val finishFailed: Boolean = false,
    /** Index of the tapped verse whose action bar is open. */
    val selected: Int? = null,
    /** Saved verses in KJV numbering, with the bookmark that keeps each. */
    val bookmarks: Map<PassageRef, BookmarkId> = emptyMap(),
) : ScreenState

/**
 * One opened chapter. [location] carries the verse to open at; the screen scrolls there once per page.
 * [previous] and [next] are the neighbouring chapters with their titles, null at either end of the Bible.
 * [canFinish] is true on the last chapter of the plan day being read, which ends with "Finish reading".
 */
data class ReaderPage(
    val location: ReaderLocation,
    val translation: ReaderTranslation,
    val book: Book,
    val title: String,
    val verses: List<Verse>,
    val previous: ChapterLink?,
    val next: ChapterLink?,
    val canFinish: Boolean = false,
    val versification: Versification = Versification.English,
)

data class ChapterLink(val chapter: ChapterRef, val title: String)

sealed interface ReaderEvent : UserEvent {
    /** [verse] is at the top of the page now; null when the page is above the first verse. */
    data class TopVerseChanged(val verse: Int?) : ReaderEvent
    data object BookPickerClicked : ReaderEvent
    data object PreviousChapterClicked : ReaderEvent
    data object NextChapterClicked : ReaderEvent
    data object SettingsClicked : ReaderEvent
    data object SettingsDismissed : ReaderEvent
    data class FontSizeStepped(val steps: Int) : ReaderEvent
    data class TranslationSelected(val id: TranslationId) : ReaderEvent
    data object FinishClicked : ReaderEvent
}

/** The action bar over a tapped verse. */
sealed interface VerseEvent : ReaderEvent {
    data class VerseClicked(val index: Int) : VerseEvent
    data object Dismissed : VerseEvent

    /** Saves the verse, or removes it when already saved. */
    data object SaveClicked : VerseEvent
    data object NoteClicked : VerseEvent
    data object CopyClicked : VerseEvent
    data object ShareClicked : VerseEvent
}

sealed interface ReaderInnerEvent : InnerEvent {
    data class PageLoaded(val page: ReaderPage) : ReaderInnerEvent
    data object LoadFailed : ReaderInnerEvent
    data class TranslationsLoaded(val translations: List<ReaderTranslation>) : ReaderInnerEvent
    data class FontSizeChanged(val size: Int) : ReaderInnerEvent
    data class PageThemeChanged(val theme: PageTheme) : ReaderInnerEvent
    data class SettingsShown(val open: Boolean) : ReaderInnerEvent
    data object Finishing : ReaderInnerEvent
    data object FinishFailed : ReaderInnerEvent
    data class VerseSelected(val index: Int?) : ReaderInnerEvent
    data class BookmarksLoaded(val bookmarks: Map<PassageRef, BookmarkId>) : ReaderInnerEvent
}

sealed interface ReaderEffect : SideEffect {
    data object OpenBookPicker : ReaderEffect

    /** The plan day was completed; on to the session complete screen. */
    data object SessionFinished : ReaderEffect

    data class OpenReflection(val route: ReflectionRoute) : ReaderEffect

    data class CopyText(val text: String) : ReaderEffect

    data class ShareText(val text: String) : ReaderEffect
}

object ReaderReducer : MviReducer<ReaderState, ReaderInnerEvent> {
    override fun reduce(oldState: ReaderState, event: ReaderInnerEvent): ReaderState = when (event) {
        // A finished session clears the plan day and reloads the page, which ends "finishing".
        is ReaderInnerEvent.PageLoaded -> oldState.copy(
            page = event.page,
            failed = false,
            finishing = false,
            selected = oldState.selected.takeIf { event.page.location.chapter == oldState.page?.location?.chapter },
        )

        ReaderInnerEvent.LoadFailed -> oldState.copy(failed = true)

        is ReaderInnerEvent.TranslationsLoaded -> oldState.copy(translations = event.translations)

        is ReaderInnerEvent.FontSizeChanged -> oldState.copy(fontSize = event.size)

        is ReaderInnerEvent.PageThemeChanged -> oldState.copy(pageTheme = event.theme)

        is ReaderInnerEvent.SettingsShown -> oldState.copy(settingsOpen = event.open)

        ReaderInnerEvent.Finishing -> oldState.copy(finishing = true, finishFailed = false)

        ReaderInnerEvent.FinishFailed -> oldState.copy(finishing = false, finishFailed = true)

        is ReaderInnerEvent.VerseSelected -> oldState.copy(selected = event.index)

        is ReaderInnerEvent.BookmarksLoaded -> oldState.copy(bookmarks = event.bookmarks)
    }
}
