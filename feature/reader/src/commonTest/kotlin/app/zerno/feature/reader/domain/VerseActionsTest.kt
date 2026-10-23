package app.zerno.feature.reader.domain

import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Verse
import app.zerno.entities.bible.VerseSpan
import app.zerno.entities.bible.VerseStyle
import app.zerno.entities.bible.VerseText
import app.zerno.entities.bible.Versification
import kotlin.test.Test
import kotlin.test.assertEquals

class VerseActionsTest {
    private val john316 = Verse(
        TranslationId("kjv"),
        ChapterRef(BookId(500), 3),
        16,
        VerseText("For God so loved the world, that he gave his only begotten Son"),
    )

    @Test
    fun sharedTextCarriesTheReferenceAndTranslation() {
        assertEquals(
            "“For God so loved the world, that he gave his only begotten Son” John 3:16 (KJV)",
            john316.shareText(BookNameLanguage.English),
        )
    }

    @Test
    fun aRussianVerseIsSharedInItsOwnNumberingAndNames() {
        val psalm = Verse(TranslationId("rst"), ChapterRef(BookId(230), 22), 1, VerseText("Господь — Пастырь мой"))

        assertEquals("“Господь — Пастырь мой” Пс 22:1 (RST)", psalm.shareText(BookNameLanguage.Russian))
    }

    @Test
    fun bookmarksKeepTheKjvNumbering() {
        val psalm = Verse(TranslationId("rst"), ChapterRef(BookId(230), 22), 1, VerseText("Господь — Пастырь мой"))

        assertEquals(PassageRef.verse(BookId(230), 23, 1), psalm.kjvPassage(Versification.Russian))
        assertEquals(PassageRef.verse(BookId(500), 3, 16), john316.kjvPassage(Versification.English))
    }

    @Test
    fun translatorsNotesAreLeftOut() {
        val text = VerseText(
            "In the beginning [or, at first] God created",
            spans = listOf(VerseSpan(VerseStyle.Note, 17, 31), VerseSpan(VerseStyle.Italic, 32, 35)),
        )

        assertEquals("In the beginning God created", text.plain())
    }
}
