package app.zerno.entities.bible

import app.zerno.entities.bible.Versification.English
import app.zerno.entities.bible.Versification.Russian
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VersificationTest {

    private fun ref(text: String) = PassageRef.parseOrThrow(text)

    private fun toRussian(text: String) = ref(text).mapVersification(English, Russian)

    private fun toEnglish(text: String) = ref(text).mapVersification(Russian, English)

    /** KJV [english] and RST [russian] are the same text, mapped both ways. */
    private fun assertMatch(english: String, russian: String) {
        assertEquals(ref(russian), toRussian(english), "$english in RST")
        assertEquals(ref(english), toEnglish(russian), "$russian in KJV")
    }

    private fun assertToRussian(english: String, russian: String) = assertEquals(ref(russian), toRussian(english), english)

    private fun assertToEnglish(russian: String, english: String) = assertEquals(ref(english), toEnglish(russian), russian)

    // Reading the flag

    @Test
    fun readsRussianNumberingFlag() {
        assertEquals(Russian, Versification.fromRussianNumbering("true"))
        assertEquals(Russian, Versification.fromRussianNumbering(" TRUE "))
        assertEquals(English, Versification.fromRussianNumbering("false"))
        assertEquals(English, Versification.fromRussianNumbering(null))
        assertEquals(English, Versification.fromRussianNumbering(""))
    }

    @Test
    fun sameNumberingReturnsTheSameReference() {
        val psalm = ref("Ps 23:1")
        assertSame(psalm, psalm.mapVersification(English, English))
        assertSame(psalm, psalm.mapVersification(Russian, Russian))
    }

    // Unchanged places

    @Test
    fun unchangedBooksKeepTheirNumbers() {
        assertMatch("John 3:16", "Ин 3:16")
        assertMatch("Gen 32:1", "Быт 32:1")
        assertMatch("Mal 4:5", "Мал 4:5")
        assertMatch("Joel 2:28", "Иоил 2:28")
        assertMatch("Joel 3", "Иоил 3")
        assertMatch("Rev 22", "Откр 22")
        assertMatch("Matt 5:1-7:29", "Мф 5:1-7:29")
    }

    @Test
    fun unchangedChaptersOfMappedBooks() {
        assertMatch("Lev 1:1", "Лев 1:1")
        assertMatch("Rom 8:28", "Рим 8:28")
        assertMatch("Rom 15", "Рим 15")
        assertMatch("Job 38:4", "Иов 38:4")
    }

    // Psalms

    @Test
    fun psalmsBeforeTheShift() {
        assertMatch("Ps 1", "Пс 1")
        assertMatch("Ps 2:7", "Пс 2:7")
        assertMatch("Ps 150", "Пс 150")
        assertMatch("Ps 148:1-6", "Пс 148:1-6")
    }

    @Test
    fun psalmShiftedByOneChapter() {
        assertMatch("Ps 23", "Пс 22")
        assertMatch("Ps 23:1", "Пс 22:1")
        assertMatch("Ps 23:4", "Пс 22:4")
        assertMatch("Ps 119:105", "Пс 118:105")
        assertMatch("Ps 91", "Пс 90")
        assertMatch("Ps 120-134", "Пс 119-133")
        assertMatch("Ps 146:1-10", "Пс 145:1-10")
    }

    @Test
    fun psalmTitleIsItsOwnVerse() {
        assertToRussian("Ps 3:1", "Пс 3:1-2")
        assertMatch("Ps 3:2", "Пс 3:3")
        assertMatch("Ps 3:8", "Пс 3:9")
        assertMatch("Ps 3", "Пс 3")
        assertToEnglish("Пс 3:1", "Ps 3:1")
        assertToEnglish("Пс 3:2", "Ps 3:1")
        assertToRussian("Ps 22:1", "Пс 21:1-2")
        assertMatch("Ps 22", "Пс 21")
    }

    @Test
    fun psalmTitleOfTwoVerses() {
        assertToRussian("Ps 51:1", "Пс 50:1-3")
        assertMatch("Ps 51:10", "Пс 50:12")
        assertMatch("Ps 51", "Пс 50")
        assertToEnglish("Пс 50:2", "Ps 51:1")
        assertMatch("Ps 52:9", "Пс 51:11")
        assertMatch("Ps 54:7", "Пс 53:9")
        assertMatch("Ps 60:12", "Пс 59:14")
    }

    @Test
    fun psalmsNineAndTenAreOnePsalm() {
        assertToRussian("Ps 9:1", "Пс 9:1-2")
        assertMatch("Ps 9:20", "Пс 9:21")
        assertMatch("Ps 10:1", "Пс 9:22")
        assertMatch("Ps 10:18", "Пс 9:39")
        assertToRussian("Ps 9", "Пс 9:1-21")
        assertToRussian("Ps 10", "Пс 9:22ff")
        assertToEnglish("Пс 9", "Ps 9-10")
        assertMatch("Ps 11", "Пс 10")
    }

    @Test
    fun psalmsWithAMergedVerse() {
        assertMatch("Ps 13:4", "Пс 12:5")
        assertToRussian("Ps 13:5", "Пс 12:6")
        assertToRussian("Ps 13:6", "Пс 12:6")
        assertToEnglish("Пс 12:6", "Ps 13:5-6")
        assertMatch("Ps 13", "Пс 12")
        assertToRussian("Ps 90:5", "Пс 89:6")
        assertToRussian("Ps 90:6", "Пс 89:6")
        assertMatch("Ps 90:12", "Пс 89:12")
        assertMatch("Ps 90", "Пс 89")
    }

    @Test
    fun psalmsOneHundredFourteenToSixteen() {
        assertMatch("Ps 114:8", "Пс 113:8")
        assertMatch("Ps 115:1", "Пс 113:9")
        assertMatch("Ps 115:18", "Пс 113:26")
        assertMatch("Ps 114-115", "Пс 113")
        assertMatch("Ps 116:9", "Пс 114:9")
        assertMatch("Ps 116:10", "Пс 115:1")
        assertMatch("Ps 116", "Пс 114-115")
        assertMatch("Ps 117", "Пс 116")
    }

    @Test
    fun psalmOneHundredFortySeven() {
        assertMatch("Ps 147:11", "Пс 146:11")
        assertMatch("Ps 147:12", "Пс 147:1")
        assertMatch("Ps 147:20", "Пс 147:9")
        assertMatch("Ps 147", "Пс 146-147")
    }

    @Test
    fun everyPsalmMapsBackToItself() {
        // KJV 9 and 10, and 114 and 115, come back as verse ranges, since RST joins each pair into one psalm.
        for (psalm in (1..150) - setOf(9, 10, 114, 115)) {
            val english = PassageRef.chapter(BookId(230), psalm)
            val there = english.mapVersification(English, Russian)
            assertEquals(english, there.mapVersification(Russian, English), "Psalm $psalm via $there")
        }
    }

    // Every other shifted range

    @Test
    fun leviticus14() {
        assertMatch("Lev 14:54", "Лев 14:54")
        assertToRussian("Lev 14:55", "Лев 14:55")
        assertToRussian("Lev 14:56", "Лев 14:55")
        assertToEnglish("Лев 14:55", "Lev 14:55-56")
        assertMatch("Lev 14:57", "Лев 14:56")
        assertMatch("Lev 14", "Лев 14")
    }

    @Test
    fun numbers12And13() {
        assertMatch("Num 12:15", "Чис 12:15")
        assertMatch("Num 12:16", "Чис 13:1")
        assertMatch("Num 13:1", "Чис 13:2")
        assertMatch("Num 13:33", "Чис 13:34")
        assertToRussian("Num 12", "Чис 12:1-13:1")
        assertToRussian("Num 13", "Чис 13:2ff")
        assertToEnglish("Чис 13", "Num 12:16-13ff")
    }

    @Test
    fun numbers29And30() {
        assertMatch("Num 29:39", "Чис 29:39")
        assertMatch("Num 29:40", "Чис 30:1")
        assertMatch("Num 30:1", "Чис 30:2")
        assertMatch("Num 30:16", "Чис 30:17")
    }

    @Test
    fun joshua5And6() {
        assertMatch("Josh 5:15", "Нав 5:15")
        assertMatch("Josh 6:1", "Нав 5:16")
        assertMatch("Josh 6:2", "Нав 6:1")
        assertMatch("Josh 6:27", "Нав 6:26")
    }

    @Test
    fun firstSamuel20() {
        assertMatch("1 Sam 20:41", "1 Цар 20:41")
        assertToRussian("1 Sam 20:42", "1 Цар 20:42-43")
        assertToEnglish("1 Цар 20:43", "1 Sam 20:42")
        assertMatch("1 Sam 20", "1 Цар 20")
    }

    @Test
    fun firstSamuel23And24() {
        assertMatch("1 Sam 23:28", "1 Цар 23:28")
        assertMatch("1 Sam 23:29", "1 Цар 24:1")
        assertMatch("1 Sam 24:1", "1 Цар 24:2")
        assertMatch("1 Sam 24:22", "1 Цар 24:23")
    }

    @Test
    fun job40And41() {
        assertMatch("Job 39:30", "Иов 39:30")
        assertMatch("Job 40:1", "Иов 39:31")
        assertMatch("Job 40:5", "Иов 39:35")
        assertMatch("Job 40:6", "Иов 40:1")
        assertMatch("Job 40:24", "Иов 40:19")
        assertMatch("Job 41:1", "Иов 40:20")
        assertMatch("Job 41:8", "Иов 40:27")
        assertMatch("Job 41:9", "Иов 41:1")
        assertMatch("Job 41:34", "Иов 41:26")
        assertToRussian("Job 41", "Иов 40:20-41ff")
        assertToEnglish("Иов 40", "Job 40:6-41:8")
    }

    @Test
    fun ecclesiastes4And5() {
        assertMatch("Eccl 4:16", "Еккл 4:16")
        assertMatch("Eccl 5:1", "Еккл 4:17")
        assertMatch("Eccl 5:2", "Еккл 5:1")
        assertMatch("Eccl 5:20", "Еккл 5:19")
    }

    @Test
    fun songOfSongs() {
        assertToRussian("Song 1:1", "Песн 1:1")
        assertToRussian("Song 1:2", "Песн 1:1")
        assertToEnglish("Песн 1:1", "Song 1:1-2")
        assertMatch("Song 1:3", "Песн 1:2")
        assertMatch("Song 1:17", "Песн 1:16")
        assertMatch("Song 1", "Песн 1")
        assertMatch("Song 6:12", "Песн 6:12")
        assertMatch("Song 6:13", "Песн 7:1")
        assertMatch("Song 7:1", "Песн 7:2")
        assertMatch("Song 7:13", "Песн 7:14")
    }

    @Test
    fun isaiah3() {
        assertMatch("Isa 3:18", "Ис 3:18")
        assertToRussian("Isa 3:19", "Ис 3:19")
        assertToRussian("Isa 3:20", "Ис 3:19")
        assertToEnglish("Ис 3:19", "Isa 3:19-20")
        assertMatch("Isa 3:21", "Ис 3:20")
        assertMatch("Isa 3:26", "Ис 3:25")
        assertMatch("Isa 3", "Ис 3")
    }

    @Test
    fun daniel3And4() {
        assertMatch("Dan 3:30", "Дан 3:30")
        assertMatch("Dan 4:1", "Дан 3:31")
        assertMatch("Dan 4:3", "Дан 3:33")
        assertMatch("Dan 4:4", "Дан 4:1")
        assertMatch("Dan 4:37", "Дан 4:34")
    }

    @Test
    fun hosea13And14() {
        assertMatch("Hos 13:15", "Ос 13:15")
        assertMatch("Hos 13:16", "Ос 14:1")
        assertMatch("Hos 14:1", "Ос 14:2")
        assertMatch("Hos 14:9", "Ос 14:10")
    }

    @Test
    fun jonah1And2() {
        assertMatch("Jonah 1:16", "Ион 1:16")
        assertMatch("Jonah 1:17", "Ион 2:1")
        assertMatch("Jonah 2:1", "Ион 2:2")
        assertMatch("Jonah 2:10", "Ион 2:11")
        assertToRussian("Jonah 2", "Ион 2:2ff")
    }

    @Test
    fun acts19() {
        assertMatch("Acts 19:39", "Деян 19:39")
        assertToRussian("Acts 19:40", "Деян 19:40")
        assertToRussian("Acts 19:41", "Деян 19:40")
        assertToEnglish("Деян 19:40", "Acts 19:40-41")
        assertMatch("Acts 19", "Деян 19")
    }

    @Test
    fun romansDoxology() {
        assertMatch("Rom 16:24", "Рим 16:24")
        assertMatch("Rom 16:25", "Рим 14:24")
        assertMatch("Rom 16:27", "Рим 14:26")
        assertMatch("Rom 16:25-27", "Рим 14:24-26")
        assertMatch("Rom 14:23", "Рим 14:23")
    }

    @Test
    fun romansPassagesAroundTheDoxology() {
        assertToRussian("Rom 16", "Рим 16")
        assertToEnglish("Рим 16", "Rom 16:1-24")
        assertToRussian("Rom 14", "Рим 14:1-23")
        assertToEnglish("Рим 14", "Rom 14")
        assertToRussian("Rom 16:20-27", "Рим 16:20-24")
        assertToEnglish("Рим 14:20-26", "Rom 14:20-23")
        assertToEnglish("Рим 14:24-15:3", "Rom 15:1-3")
    }

    @Test
    fun secondCorinthians() {
        assertMatch("2 Cor 11:31", "2 Кор 11:31")
        assertToRussian("2 Cor 11:32", "2 Кор 11:32")
        assertToRussian("2 Cor 11:33", "2 Кор 11:32")
        assertToEnglish("2 Кор 11:32", "2 Cor 11:32-33")
        assertMatch("2 Cor 13:11", "2 Кор 13:11")
        assertToRussian("2 Cor 13:12", "2 Кор 13:12")
        assertToRussian("2 Cor 13:13", "2 Кор 13:12")
        assertToEnglish("2 Кор 13:12", "2 Cor 13:12-13")
        assertMatch("2 Cor 13:14", "2 Кор 13:13")
        assertMatch("2 Cor 13", "2 Кор 13")
    }

    @Test
    fun philemon() {
        assertMatch("Phlm 22", "Флм 22")
        assertToRussian("Phlm 23", "Флм 23")
        assertToRussian("Phlm 24", "Флм 23")
        assertToEnglish("Флм 23", "Phlm 23-24")
        assertMatch("Phlm 25", "Флм 24")
        assertMatch("Phlm", "Флм")
    }

    @Test
    fun thirdJohn() {
        assertMatch("3 John 13", "3 Ин 13")
        assertToRussian("3 John 14", "3 Ин 14-15")
        assertToEnglish("3 Ин 14", "3 John 14")
        assertToEnglish("3 Ин 15", "3 John 14")
        assertMatch("3 John", "3 Ин")
    }

    // The table itself

    @Test
    fun everyListedChapterIsCoveredFromVerseOneWithoutGaps() {
        for ((book, segments) in SynodalMapping.segments) {
            for (side in listOf(VerseSegment::english, VerseSegment::russian)) {
                for ((chapter, spans) in segments.map(side).groupBy { it.chapter }) {
                    val sorted = spans.sortedBy { it.first }
                    assertEquals(1, sorted.first().first, "Book $book chapter $chapter starts at verse 1")
                    sorted.zipWithNext { a, b ->
                        assertEquals(a.last?.plus(1), b.first, "Book $book: $b follows $a")
                    }
                }
            }
        }
    }

    @Test
    fun everyVerseInAOneToOneSpanMapsBackToItself() {
        for ((book, segments) in SynodalMapping.segments) {
            for (segment in segments.filter { it.english.size == it.russian.size }) {
                val english = segment.english
                for (verse in english.first..(english.last ?: (english.first + 5))) {
                    val ref = PassageRef.verse(BookId(book), english.chapter, verse)
                    val there = ref.mapVersification(English, Russian)
                    assertEquals(segment.russian.chapter, there.startChapter, "$ref goes to the chapter of $segment")
                    assertEquals(ref, there.mapVersification(Russian, English), "$ref via $there")
                }
            }
        }
    }

    @Test
    fun onlyRomansHasMovedText() {
        val moved = SynodalMapping.segments.filterValues { segments -> segments.any { it.moved } }.keys
        assertEquals(setOf(520), moved)
        assertNull(SynodalMapping.segments[460], "Malachi keeps KJV numbering in RST")
        assertTrue(SynodalMapping.segments.keys.all { BookId(it).value in BookId.RANGE })
    }
}
