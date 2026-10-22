package app.zerno.feature.reader.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.entities.bible.Book
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import app.zerno.feature.reader.domain.BibleSource
import app.zerno.feature.reader.domain.ReaderSession
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.UserEvent
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/** [current] is the chapter the reader shows; its book starts expanded. */
data class BookPickerState(
    val books: List<Book> = emptyList(),
    val current: ChapterRef? = null,
    val expanded: BookId? = null,
) : ScreenState

sealed interface BookPickerEvent : UserEvent {
    data class BookClicked(val book: BookId) : BookPickerEvent
    data class ChapterClicked(val chapter: ChapterRef) : BookPickerEvent
    data object BackClicked : BookPickerEvent
}

sealed interface BookPickerInnerEvent : InnerEvent {
    data class Loaded(val books: List<Book>, val current: ChapterRef) : BookPickerInnerEvent
    data class Expanded(val book: BookId?) : BookPickerInnerEvent
}

sealed interface BookPickerEffect : SideEffect {
    data object Close : BookPickerEffect
}

object BookPickerReducer : MviReducer<BookPickerState, BookPickerInnerEvent> {
    override fun reduce(oldState: BookPickerState, event: BookPickerInnerEvent): BookPickerState = when (event) {
        is BookPickerInnerEvent.Loaded -> oldState.copy(
            books = event.books,
            current = event.current,
            expanded = event.current.book,
        )

        is BookPickerInnerEvent.Expanded -> oldState.copy(expanded = event.book)
    }
}

/** Books of the reader's translation; a chapter tapped opens in the reader, which this screen then returns to. */
class BookPickerViewModel(
    savedStateHandle: SavedStateHandle,
    private val session: ReaderSession,
    private val bible: BibleSource,
) : MviViewModel<BookPickerState, BookPickerEvent, BookPickerInnerEvent, BookPickerEffect, ScreenSnapshot.NoSnapshot>(
    savedStateHandle = savedStateHandle,
    initialState = BookPickerState(),
    reducer = BookPickerReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(BookPickerState()),
) {
    init {
        intent(LOAD) {
            val location = session.location.filterNotNull().first()
            emit(BookPickerInnerEvent.Loaded(bible.books(location.translation), location.chapter))
        }
    }

    override fun onEvent(event: BookPickerEvent) {
        when (event) {
            is BookPickerEvent.BookClicked -> intent(EXPAND) {
                emit(BookPickerInnerEvent.Expanded(event.book.takeIf { it != state.value.expanded }))
            }

            is BookPickerEvent.ChapterClicked -> intent(OPEN) {
                session.openChapter(event.chapter)
                postEffect(BookPickerEffect.Close)
            }

            BookPickerEvent.BackClicked -> intent(OPEN) { postEffect(BookPickerEffect.Close) }
        }
    }

    private companion object {
        const val LOAD = "load"
        const val EXPAND = "expand"
        const val OPEN = "open"
    }
}
