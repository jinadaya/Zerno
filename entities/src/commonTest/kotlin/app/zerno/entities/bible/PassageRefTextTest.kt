package app.zerno.entities.bible

import app.zerno.entities.bible.BookNameLanguage.English
import app.zerno.entities.bible.BookNameLanguage.Russian
import app.zerno.entities.bible.BookNameStyle.Long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PassageRefTextTest {

    private val genesis = BookId(10)
    private val samuel1 = BookId(90)
    private val kings1 = BookId(110)
    private val psalms = BookId(230)
    private val song = BookId(260)
    private val obadiah = BookId(380)
    private val matthew = BookId(470)
    private val john = BookId(500)
    private val corinthians1 = BookId(530)
    private val philemon = BookId(640)
    private val john1 = BookId(690)
    private val john3 = BookId(710)
    private val jude = BookId(720)
    private val revelation = BookId(730)

    private fun ref(book: BookId, startChapter: Int, startVerse: Int? = null, endChapter: Int = startChapter, endVerse: Int? = null) =
        PassageRef(book, startChapter, startVerse, endChapter, endVerse)

    private fun assertParses(expected: PassageRef, text: String) = assertEquals(expected, PassageRef.parse(text), text)

    // English parsing

    @Test
    fun englishVerseRange() = assertParses(ref(john, 4, 1, 4, 26), "John 4:1-26")

    @Test
    fun englishWholeChapter() = assertParses(ref(psalms, 23), "Ps 23")

    @Test
    fun englishSingleVerse() = assertParses(ref(john, 3, 16, 3, 16), "John 3:16")

    @Test
    fun englishMultiChapterRangeWithVerses() = assertParses(ref(john, 4, 46, 5, 15), "John 4:46-5:15")

    @Test
    fun englishWholeChapterRange() = assertParses(ref(genesis, 1, null, 3, null), "Gen 1-3")

    @Test
    fun englishChapterToVerseInLaterChapter() = assertParses(ref(genesis, 1, null, 2, 3), "Genesis 1-2:3")

    @Test
    fun englishFullName() = assertParses(ref(matthew, 5, 1, 5, 12), "Matthew 5:1-12")

    @Test
    fun englishShortAbbreviations() {
        assertParses(ref(matthew, 5), "Mt 5")
        assertParses(ref(matthew, 5), "Matt 5")
        assertParses(ref(john, 1), "Jn 1")
        assertParses(ref(revelation, 21), "Rev 21")
    }

    @Test
    fun englishNumberedBooks() {
        assertParses(ref(corinthians1, 13, 4, 13, 8), "1 Cor 13:4-8")
        assertParses(ref(corinthians1, 13, 4, 13, 8), "1Cor 13:4-8")
        assertParses(ref(corinthians1, 13), "1 Corinthians 13")
        assertParses(ref(john1, 4, 8, 4, 8), "1 John 4:8")
    }

    @Test
    fun englishRomanAndOrdinalPrefixes() {
        assertParses(ref(corinthians1, 13), "I Cor 13")
        assertParses(ref(kings1, 18), "I Kings 18")
        assertParses(ref(samuel1, 17), "1st Samuel 17")
    }

    @Test
    fun englishMultiWordBookName() = assertParses(ref(song, 2, 1, 2, 1), "Song of Solomon 2:1")

    @Test
    fun englishPsalmSingularAndPlural() {
        assertParses(ref(psalms, 23), "Psalm 23")
        assertParses(ref(psalms, 120, null, 134, null), "Psalms 120-134")
    }

    @Test
    fun caseAndSpacingDoNotMatter() {
        assertParses(ref(john, 3, 16, 3, 16), "  JOHN   3 : 16 ")
        assertParses(ref(john, 3, 16, 3, 18), "john 3:16 - 18")
        assertParses(ref(john, 3, 16, 3, 16), "John3:16")
    }

    @Test
    fun dotAfterAbbreviation() = assertParses(ref(genesis, 1, 1, 1, 1), "Gen. 1:1")

    @Test
    fun enDashAndEmDash() {
        assertParses(ref(john, 4, 1, 4, 26), "John 4:1–26")
        assertParses(ref(john, 4, 1, 4, 26), "John 4:1—26")
    }

    @Test
    fun dotAsChapterVerseSeparator() = assertParses(ref(john, 3, 16, 3, 16), "John 3.16")

    @Test
    fun uniquePrefixOfBookName() {
        assertParses(ref(BookId(50), 6, 4, 6, 4), "Deuter 6:4")
        assertParses(ref(revelation, 1), "Revelations 1")
    }

    @Test
    fun wholeBookWhenNoNumbers() {
        assertParses(ref(BookId(80), 1, null, 4, null), "Ruth")
        assertParses(ref(jude, 1), "Jude")
    }

    // Russian parsing

    @Test
    fun russianVerseRange() = assertParses(ref(john, 4, 1, 4, 26), "Ин 4:1-26")

    @Test
    fun russianWholeChapter() = assertParses(ref(psalms, 22), "Пс 22")

    @Test
    fun russianFullName() = assertParses(ref(john, 3, 16, 3, 16), "От Иоанна 3:16")

    @Test
    fun russianNameWithoutPreposition() = assertParses(ref(john, 3, 16, 3, 16), "Иоанна 3:16")

    @Test
    fun russianGospelAbbreviations() {
        assertParses(ref(matthew, 5, 3, 5, 12), "Мф 5:3-12")
        assertParses(ref(BookId(480), 1), "Мк 1")
        assertParses(ref(BookId(490), 15, 11, 15, 32), "Лк 15:11-32")
    }

    @Test
    fun russianMultiChapterRange() = assertParses(ref(john, 4, 46, 5, 15), "Ин 4:46-5:15")

    @Test
    fun russianChapterRange() = assertParses(ref(genesis, 1, null, 3, null), "Быт 1-3")

    @Test
    fun russianCommaAsChapterVerseSeparator() = assertParses(ref(john, 3, 16, 3, 16), "Ин 3,16")

    @Test
    fun russianDotAfterAbbreviation() = assertParses(ref(corinthians1, 13, 4, 13, 8), "1 Кор. 13:4-8")

    @Test
    fun russianOrdinalPrefix() = assertParses(ref(corinthians1, 13), "1-е Коринфянам 13")

    @Test
    fun russianKingdomsNumbering() {
        assertParses(ref(samuel1, 17), "1 Царств 17")
        assertParses(ref(kings1, 18), "3 Цар 18")
        assertParses(ref(BookId(120), 2), "4Цар 2")
    }

    @Test
    fun russianYoIsReadAsYe() = assertParses(ref(BookId(250), 3, 1, 3, 8), "Екклёсиаст 3:1-8")

    @Test
    fun russianPsalmWords() {
        assertParses(ref(psalms, 50), "Псалом 50")
        assertParses(ref(psalms, 50), "Псалтирь 50")
    }

    @Test
    fun russianEpistlesAndRevelation() {
        assertParses(ref(BookId(660), 1, 2, 1, 4), "Иак 1:2-4")
        assertParses(ref(john1, 4, 8, 4, 8), "1 Ин 4:8")
        assertParses(ref(BookId(570), 4, 13, 4, 13), "Флп 4:13")
        assertParses(ref(revelation, 21, 1, 21, 4), "Откр 21:1-4")
    }

    @Test
    fun russianMultiWordName() = assertParses(ref(BookId(60), 1, 9, 1, 9), "Иисус Навин 1:9")

    // One-chapter books

    @Test
    fun oneChapterBookBareNumberIsVerse() {
        assertParses(ref(jude, 1, 3, 1, 3), "Jude 3")
        assertParses(ref(jude, 1, 3, 1, 3), "Иуд 3")
    }

    @Test
    fun oneChapterBookVerseRange() {
        assertParses(ref(philemon, 1, 4, 1, 7), "Phlm 4-7")
        assertParses(ref(obadiah, 1, 1, 1, 4), "Авд 1-4")
    }

    @Test
    fun oneChapterBookWithExplicitChapter() = assertParses(ref(john3, 1, 2, 1, 2), "3 John 1:2")

    // Open ends

    @Test
    fun followingRunsToEndOfChapter() {
        assertParses(ref(john, 4, 5, 4, null), "John 4:5ff")
        assertParses(ref(john, 4, 5, 5, null), "Ин 4:5-5ff")
    }

    // Rejections

    @Test
    fun unknownBookIsNull() {
        assertNull(PassageRef.parse("Hezekiah 1:1"))
        assertNull(PassageRef.parse("Книга 1"))
    }

    @Test
    fun ambiguousPrefixIsNull() {
        assertNull(PassageRef.parse("Ph 1"))
        assertNull(PassageRef.parse("Ma 1"))
    }

    @Test
    fun emptyAndNumbersOnlyAreNull() {
        assertNull(PassageRef.parse(""))
        assertNull(PassageRef.parse("   "))
        assertNull(PassageRef.parse("3:16"))
    }

    @Test
    fun chapterBeyondBookIsNull() {
        assertNull(PassageRef.parse("Ps 151"))
        assertNull(PassageRef.parse("John 22"))
        assertNull(PassageRef.parse("John 20-22"))
    }

    @Test
    fun zeroIsNull() {
        assertNull(PassageRef.parse("John 0"))
        assertNull(PassageRef.parse("John 3:0"))
    }

    @Test
    fun backwardsRangeIsNull() {
        assertNull(PassageRef.parse("John 4:26-1"))
        assertNull(PassageRef.parse("John 5-4"))
        assertNull(PassageRef.parse("John 5:1-4:2"))
    }

    @Test
    fun malformedNumbersAreNull() {
        assertNull(PassageRef.parse("John 3:"))
        assertNull(PassageRef.parse("John 3:16-"))
        assertNull(PassageRef.parse("John 3:16a"))
        assertNull(PassageRef.parse("John 3::16"))
        assertNull(PassageRef.parse("John 3:16; 4:1"))
        assertNull(PassageRef.parse("John 1234"))
        assertNull(PassageRef.parse("John 3ff"))
    }

    @Test
    fun parseOrThrowNamesTheText() {
        val error = assertFailsWith<IllegalArgumentException> { PassageRef.parseOrThrow("Nowhere 1") }
        assertEquals("Not a Bible reference: \"Nowhere 1\"", error.message)
        assertEquals(ref(psalms, 23), PassageRef.parseOrThrow("Ps 23"))
    }

    // Formatting

    @Test
    fun formatsTaskExamples() {
        assertEquals("John 4:1-26", ref(john, 4, 1, 4, 26).format(English))
        assertEquals("Ин 4:1-26", ref(john, 4, 1, 4, 26).format(Russian))
        assertEquals("Ps 23", ref(psalms, 23).format(English))
        assertEquals("Пс 22", ref(psalms, 22).format(Russian))
    }

    @Test
    fun formatsSingleVerse() {
        assertEquals("John 3:16", PassageRef.verse(john, 3, 16).format())
        assertEquals("Ин 3:16", PassageRef.verse(john, 3, 16).format(Russian))
    }

    @Test
    fun formatsMultiChapterRanges() {
        assertEquals("John 4:46-5:15", ref(john, 4, 46, 5, 15).format())
        assertEquals("Быт 1-3", ref(genesis, 1, null, 3, null).format(Russian))
        assertEquals("Gen 1-2:3", ref(genesis, 1, null, 2, 3).format())
    }

    @Test
    fun formatsLongNames() {
        assertEquals("От Иоанна 3:16", PassageRef.verse(john, 3, 16).format(Russian, Long))
        assertEquals("1 Corinthians 13:4-8", ref(corinthians1, 13, 4, 13, 8).format(English, Long))
        assertEquals("Song of Solomon 2", ref(song, 2).format(English, Long))
    }

    @Test
    fun formatsPsalmSingularOnlyForOneChapter() {
        assertEquals("Psalm 23", ref(psalms, 23).format(English, Long))
        assertEquals("Psalms 120-134", ref(psalms, 120, null, 134, null).format(English, Long))
        assertEquals("Псалом 22", ref(psalms, 22).format(Russian, Long))
        assertEquals("Псалтирь 22-24", ref(psalms, 22, null, 24, null).format(Russian, Long))
    }

    @Test
    fun formatsNumberedBooksWithSpace() {
        assertEquals("1 Cor 13", ref(corinthians1, 13).format())
        assertEquals("3 Цар 18", ref(kings1, 18).format(Russian))
    }

    @Test
    fun formatsOneChapterBooksWithoutChapter() {
        assertEquals("Jude 3", ref(jude, 1, 3, 1, 3).format())
        assertEquals("Флм 4-7", ref(philemon, 1, 4, 1, 7).format(Russian))
        assertEquals("Jude", ref(jude, 1).format())
        assertEquals("Jude 1:5ff", ref(jude, 1, 5, 1, null).format())
    }

    @Test
    fun formatsOpenEnds() {
        assertEquals("John 4:5ff", ref(john, 4, 5, 4, null).format())
        assertEquals("John 4:5-5ff", ref(john, 4, 5, 5, null).format())
    }

    @Test
    fun formatsWholeBookAsChapterRange() = assertEquals("Ruth 1-4", ref(BookId(80), 1, null, 4, null).format())

    // Round trips

    private val samples = listOf(
        ref(john, 4, 1, 4, 26),
        ref(psalms, 23),
        ref(psalms, 120, null, 134, null),
        PassageRef.verse(john, 3, 16),
        ref(john, 4, 46, 5, 15),
        ref(genesis, 1, null, 2, 3),
        ref(john, 4, 5, 4, null),
        ref(john, 4, 5, 5, null),
        ref(jude, 1, 3, 1, 3),
        ref(jude, 1, 3, 1, 10),
        ref(jude, 1),
        ref(jude, 1, 5, 1, null),
        ref(corinthians1, 13, 4, 13, 8),
        ref(BookId(120), 2),
        ref(revelation, 1, null, 22, null),
    )

    @Test
    fun everyStyleAndLanguageRoundTrips() {
        for (passage in samples) {
            for (language in BookNameLanguage.entries) {
                for (style in BookNameStyle.entries) {
                    val text = passage.format(language, style)
                    assertEquals(passage, PassageRef.parse(text), "$language $style: $text")
                }
            }
        }
    }

    @Test
    fun everyBookNameRoundTripsInBothLanguages() {
        for (book in BookNames.all) {
            val passage = PassageRef.chapter(book, 1)
            for (language in BookNameLanguage.entries) {
                for (style in BookNameStyle.entries) {
                    val text = passage.format(language, style)
                    assertEquals(passage, PassageRef.parse(text), text)
                }
            }
        }
    }

    // Book names

    @Test
    fun sixtySixBooks() {
        assertEquals(66, BookNames.all.size)
        assertEquals(1189, BookNames.all.sumOf { BookNames.chapterCount(it) })
    }

    @Test
    fun findIgnoresCaseDotsAndSpaces() {
        assertEquals(john1, BookNames.find("1 ИН."))
        assertEquals(john1, BookNames.find("i jn"))
        assertEquals(BookId(700), BookNames.find("II John"))
        assertEquals(BookId(120), BookNames.find("IV Царств"))
    }

    @Test
    fun jonahJohnAndJudeAreDistinct() {
        assertEquals(BookId(390), BookNames.find("Jon"))
        assertEquals(john, BookNames.find("Joh"))
        assertEquals(jude, BookNames.find("Jud"))
        assertEquals(BookId(70), BookNames.find("Judg"))
        assertEquals(BookId(390), BookNames.find("Ион"))
        assertEquals(BookId(220), BookNames.find("Иов"))
    }

    @Test
    fun languageFromCode() {
        assertEquals(Russian, BookNameLanguage.fromCode("ru"))
        assertEquals(English, BookNameLanguage.fromCode("EN"))
        assertEquals(English, BookNameLanguage.fromCode("de"))
    }
}
