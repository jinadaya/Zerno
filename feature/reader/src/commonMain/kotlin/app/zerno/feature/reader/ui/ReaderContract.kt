package app.zerno.feature.reader.ui

import app.zerno.entities.bible.Book
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Verse
import app.zerno.feature.reader.domain.ReaderFontSize
import app.zerno.feature.reader.domain.ReaderLocation
import app.zerno.feature.reader.domain.ReaderTranslation
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
    val settingsOpen: Boolean = false,
    /** "Finish reading" was tapped and the session is being recorded. */
    val finishing: Boolean = false,
    val finishFailed: Boolean = false,
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

sealed interface ReaderInnerEvent : InnerEvent {
    data class PageLoaded(val page: ReaderPage) : ReaderInnerEvent
    data object LoadFailed : ReaderInnerEvent
    data class TranslationsLoaded(val translations: List<ReaderTranslation>) : ReaderInnerEvent
    data class FontSizeChanged(val size: Int) : ReaderInnerEvent
    data class SettingsShown(val open: Boolean) : ReaderInnerEvent
    data object Finishing : ReaderInnerEvent
    data object FinishFailed : ReaderInnerEvent
}

sealed interface ReaderEffect : SideEffect {
    data object OpenBookPicker : ReaderEffect

    /** The plan day was completed; back to Today. */
    data object SessionFinished : ReaderEffect
}

object ReaderReducer : MviReducer<ReaderState, ReaderInnerEvent> {
    override fun reduce(oldState: ReaderState, event: ReaderInnerEvent): ReaderState = when (event) {
        // A finished session clears the plan day and reloads the page, which ends "finishing".
        is ReaderInnerEvent.PageLoaded -> oldState.copy(page = event.page, failed = false, finishing = false)

        ReaderInnerEvent.LoadFailed -> oldState.copy(failed = true)

        is ReaderInnerEvent.TranslationsLoaded -> oldState.copy(translations = event.translations)

        is ReaderInnerEvent.FontSizeChanged -> oldState.copy(fontSize = event.size)

        is ReaderInnerEvent.SettingsShown -> oldState.copy(settingsOpen = event.open)

        ReaderInnerEvent.Finishing -> oldState.copy(finishing = true, finishFailed = false)

        ReaderInnerEvent.FinishFailed -> oldState.copy(finishing = false, finishFailed = true)
    }
}
