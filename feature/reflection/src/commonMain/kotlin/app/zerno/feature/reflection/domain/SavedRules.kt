package app.zerno.feature.reflection.domain

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.saved.Bookmark
import app.zerno.feature.reflection.api.SavedReflection
import kotlin.time.Instant

enum class SavedFilter { All, Verses, Notes }

enum class SavedSort { Date, Book }

/** A saved verse or a reflection, as one row of the Saved list. */
sealed interface SavedItem {
    /** Unique across both kinds. */
    val key: String
    val passage: PassageRef
    val date: Instant

    data class Verse(val bookmark: Bookmark) : SavedItem {
        override val key: String get() = "verse:${bookmark.id.value}"
        override val passage: PassageRef get() = bookmark.passage
        override val date: Instant get() = bookmark.createdAt
    }

    data class Note(val saved: SavedReflection) : SavedItem {
        override val key: String get() = "note:${saved.reflection.id.value}"
        override val passage: PassageRef get() = saved.reflection.passage
        override val date: Instant get() = saved.reflection.createdAt
    }
}

/** Newest first, or in Bible order (book, chapter, verse) with the newest first within one passage. */
fun savedItems(
    bookmarks: List<Bookmark>,
    reflections: List<SavedReflection>,
    filter: SavedFilter,
    sort: SavedSort,
): List<SavedItem> {
    val verses = if (filter == SavedFilter.Notes) emptyList() else bookmarks.map { SavedItem.Verse(it) }
    val notes = if (filter == SavedFilter.Verses) emptyList() else reflections.map { SavedItem.Note(it) }
    val newestFirst = compareByDescending<SavedItem> { it.date }
    val order = when (sort) {
        SavedSort.Date -> newestFirst

        SavedSort.Book -> compareBy<SavedItem>(
            { it.passage.book.value },
            { it.passage.startChapter },
            { it.passage.startVerse ?: 0 },
        ).then(newestFirst)
    }
    return (verses + notes).sortedWith(order)
}
