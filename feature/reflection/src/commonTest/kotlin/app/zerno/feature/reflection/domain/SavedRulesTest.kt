package app.zerno.feature.reflection.domain

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.saved.Bookmark
import app.zerno.entities.saved.BookmarkId
import app.zerno.feature.reflection.data.John3
import app.zerno.feature.reflection.data.Psalm23
import app.zerno.feature.reflection.ui.saved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SavedRulesTest {
    private val genesis1 = PassageRef.verse(BookId(10), 1, 1)
    private val john316 = PassageRef.verse(BookId(500), 3, 16)
    private val bookmarks = listOf(
        Bookmark(BookmarkId("b2"), genesis1, TranslationId("kjv"), Instant.parse("2026-10-22T09:00:00Z")),
        Bookmark(BookmarkId("b1"), john316, TranslationId("kjv"), Instant.parse("2026-10-20T09:00:00Z")),
    )
    private val reflections = listOf(
        saved("r2", Psalm23, "Shepherd", at = "2026-10-21T09:00:00Z"),
        saved("r1", John3, "Loved", at = "2026-10-19T09:00:00Z"),
    )

    @Test
    fun byDateMixesBothKindsNewestFirst() {
        val items = savedItems(bookmarks, reflections, SavedFilter.All, SavedSort.Date)

        assertEquals(listOf("verse:b2", "note:r2", "verse:b1", "note:r1"), items.map { it.key })
    }

    @Test
    fun bibleOrderGoesByBookChapterAndVerse() {
        val items = savedItems(bookmarks, reflections, SavedFilter.All, SavedSort.Book)

        // John 3 (whole chapter) comes before John 3:16; Psalms before John.
        assertEquals(listOf("verse:b2", "note:r2", "note:r1", "verse:b1"), items.map { it.key })
    }

    @Test
    fun filtersKeepOneKind() {
        assertEquals(
            listOf("verse:b2", "verse:b1"),
            savedItems(bookmarks, reflections, SavedFilter.Verses, SavedSort.Date).map { it.key },
        )
        assertEquals(
            listOf("note:r2", "note:r1"),
            savedItems(bookmarks, reflections, SavedFilter.Notes, SavedSort.Date).map { it.key },
        )
    }
}
