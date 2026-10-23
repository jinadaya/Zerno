package app.zerno.feature.reflection.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.feature.reader.api.ReaderLauncher
import app.zerno.feature.reflection.api.Bookmarks
import app.zerno.feature.reflection.api.Reflections
import app.zerno.feature.reflection.domain.SavedItem
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel

/**
 * Saved verses and reflections in one list, filtered and sorted. A verse opens in the reader, a reflection opens
 * for editing, and deleting either asks once on its row.
 */
class SavedViewModel(
    savedStateHandle: SavedStateHandle,
    private val bookmarks: Bookmarks,
    private val reflections: Reflections,
    private val reader: ReaderLauncher,
) : MviViewModel<SavedState, SavedEvent, SavedInnerEvent, SavedEffect, ScreenSnapshot.NoSnapshot>(
    savedStateHandle = savedStateHandle,
    initialState = SavedState(),
    reducer = SavedReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(SavedState()),
) {
    init {
        subscription(BOOKMARKS) { bookmarks.all.collect { emit(SavedInnerEvent.BookmarksLoaded(it)) } }
        subscription(REFLECTIONS) { reflections.all.collect { emit(SavedInnerEvent.ReflectionsLoaded(it)) } }
    }

    override fun onEvent(event: SavedEvent) {
        when (event) {
            is SavedEvent.FilterChosen -> intent(LIST) { emit(SavedInnerEvent.FilterChanged(event.filter)) }
            is SavedEvent.SortChosen -> intent(LIST) { emit(SavedInnerEvent.SortChanged(event.sort)) }
            is SavedEvent.ItemClicked -> open(event.key)
            is SavedEvent.DeleteClicked -> intent(CONFIRM) { emit(SavedInnerEvent.Confirming(event.key)) }
            SavedEvent.DeleteCancelled -> intent(CONFIRM) { emit(SavedInnerEvent.Confirming(null)) }
            SavedEvent.DeleteConfirmed -> delete()
            SavedEvent.BackClicked -> intent(CLOSE) { postEffect(SavedEffect.Close) }
        }
    }

    private fun open(key: String) {
        when (val item = state.value.item(key)) {
            is SavedItem.Verse -> intent(OPEN) {
                reader.openAt(item.passage)
                postEffect(SavedEffect.OpenReader)
            }

            is SavedItem.Note -> intent(OPEN) { postEffect(SavedEffect.EditReflection(item.route)) }

            null -> Unit
        }
    }

    private fun delete() {
        val item = state.value.confirming?.let { state.value.item(it) } ?: return
        intent(DELETE) {
            emit(SavedInnerEvent.Confirming(null))
            when (item) {
                is SavedItem.Verse -> bookmarks.delete(item.bookmark.id)
                is SavedItem.Note -> reflections.delete(item.saved.reflection.id)
            }
        }
    }

    private companion object {
        const val BOOKMARKS = "bookmarks"
        const val REFLECTIONS = "reflections"
        const val LIST = "list"
        const val CONFIRM = "confirm"
        const val OPEN = "open"
        const val DELETE = "delete"
        const val CLOSE = "close"
    }
}
