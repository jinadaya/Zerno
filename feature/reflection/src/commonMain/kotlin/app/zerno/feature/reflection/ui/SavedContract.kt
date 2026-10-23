package app.zerno.feature.reflection.ui

import app.zerno.entities.saved.Bookmark
import app.zerno.feature.reflection.api.ReflectionRoute
import app.zerno.feature.reflection.api.SavedReflection
import app.zerno.feature.reflection.domain.SavedFilter
import app.zerno.feature.reflection.domain.SavedItem
import app.zerno.feature.reflection.domain.SavedSort
import app.zerno.feature.reflection.domain.savedItems
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent

/** [items] is null until both lists are read. [confirming] is the key of a row asking "Delete?". */
data class SavedState(
    val bookmarks: List<Bookmark>? = null,
    val reflections: List<SavedReflection>? = null,
    val filter: SavedFilter = SavedFilter.All,
    val sort: SavedSort = SavedSort.Date,
    val items: List<SavedItem>? = null,
    val confirming: String? = null,
) : ScreenState

sealed interface SavedEvent : UserEvent {
    data class FilterChosen(val filter: SavedFilter) : SavedEvent

    data class SortChosen(val sort: SavedSort) : SavedEvent

    /** A verse opens in the reader; a reflection opens for editing. */
    data class ItemClicked(val key: String) : SavedEvent

    data class DeleteClicked(val key: String) : SavedEvent

    data object DeleteConfirmed : SavedEvent

    data object DeleteCancelled : SavedEvent

    data object BackClicked : SavedEvent
}

sealed interface SavedInnerEvent : InnerEvent {
    data class BookmarksLoaded(val bookmarks: List<Bookmark>) : SavedInnerEvent

    data class ReflectionsLoaded(val reflections: List<SavedReflection>) : SavedInnerEvent

    data class FilterChanged(val filter: SavedFilter) : SavedInnerEvent

    data class SortChanged(val sort: SavedSort) : SavedInnerEvent

    data class Confirming(val key: String?) : SavedInnerEvent
}

sealed interface SavedEffect : SideEffect {
    data object OpenReader : SavedEffect

    data class EditReflection(val route: ReflectionRoute) : SavedEffect

    data object Close : SavedEffect
}

/** The row behind [key], if it is still listed. */
internal fun SavedState.item(key: String): SavedItem? = items?.firstOrNull { it.key == key }

internal val SavedItem.Note.route: ReflectionRoute
    get() = ReflectionRoute(passage = passage, reflectionId = saved.reflection.id)

object SavedReducer : MviReducer<SavedState, SavedInnerEvent> {
    override fun reduce(oldState: SavedState, event: SavedInnerEvent): SavedState = when (event) {
        is SavedInnerEvent.BookmarksLoaded -> oldState.copy(bookmarks = event.bookmarks).withItems()
        is SavedInnerEvent.ReflectionsLoaded -> oldState.copy(reflections = event.reflections).withItems()
        is SavedInnerEvent.FilterChanged -> oldState.copy(filter = event.filter, confirming = null).withItems()
        is SavedInnerEvent.SortChanged -> oldState.copy(sort = event.sort, confirming = null).withItems()
        is SavedInnerEvent.Confirming -> oldState.copy(confirming = event.key)
    }

    private fun SavedState.withItems(): SavedState {
        val bookmarks = bookmarks ?: return this
        val reflections = reflections ?: return this
        val items = savedItems(bookmarks, reflections, filter, sort)
        return copy(items = items, confirming = confirming?.takeIf { key -> items.any { it.key == key } })
    }
}
