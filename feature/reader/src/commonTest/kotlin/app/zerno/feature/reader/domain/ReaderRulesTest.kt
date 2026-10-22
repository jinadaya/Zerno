package app.zerno.feature.reader.domain

import app.zerno.entities.bible.Book
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.ChapterRef
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val JUDE = BookId(720)

class ReaderRulesTest {
    private val books = listOf(
        Book(GENESIS, "Gen", "Genesis", 50),
        Book(JOHN, "John", "John", 21),
        Book(JUDE, "Jude", "Jude", 1),
    )

    @Test
    fun nextChapterCrossesIntoTheNextBook() {
        assertEquals(ChapterRef(GENESIS, 2), books.chapterAfter(ChapterRef(GENESIS, 1)))
        assertEquals(ChapterRef(JOHN, 1), books.chapterAfter(ChapterRef(GENESIS, 50)))
        assertNull(books.chapterAfter(ChapterRef(JUDE, 1)))
    }

    @Test
    fun previousChapterCrossesIntoThePreviousBook() {
        assertEquals(ChapterRef(JOHN, 2), books.chapterBefore(ChapterRef(JOHN, 3)))
        assertEquals(ChapterRef(GENESIS, 50), books.chapterBefore(ChapterRef(JOHN, 1)))
        assertNull(books.chapterBefore(ChapterRef(GENESIS, 1)))
    }

    @Test
    fun chapterTitles() {
        val psalms = Book(PSALMS, "Пс", "Псалтирь", 151)
        assertEquals("Псалом 22", chapterTitle(psalms, 22, "Псалом"))
        assertEquals("Псалтирь 22", chapterTitle(psalms, 22, null))
        assertEquals("John 3", chapterTitle(books[1], 3, "Psalm"))
        assertEquals("Jude", chapterTitle(books[2], 1, "Psalm"))
    }

    @Test
    fun fontSizeStaysInRange() {
        assertEquals(22, ReaderFontSize.step(20, 1))
        assertEquals(28, ReaderFontSize.step(28, 1))
        assertEquals(16, ReaderFontSize.step(16, -1))
        assertEquals(18, ReaderFontSize.step(20, -1))
    }

    @Test
    fun locationMapsBetweenNumberings() = runTest {
        val bible = FakeBible()
        val kjv = bible.details(KJV).versification
        val rst = bible.details(RST).versification
        assertEquals(
            ReaderLocation(RST, ChapterRef(PSALMS, 50), 1),
            ReaderLocation(KJV, ChapterRef(PSALMS, 51), 1).inTranslation(RST, kjv, rst),
        )
        assertEquals(
            ReaderLocation(KJV, ChapterRef(JOHN, 3), 16),
            ReaderLocation(RST, ChapterRef(JOHN, 3), 16).inTranslation(KJV, rst, kjv),
        )
    }
}
