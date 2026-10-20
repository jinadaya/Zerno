package app.zerno.entities.bible

/**
 * Verses of one chapter: [first] to [last], or to the end of the chapter when [last] is null.
 * Written "14:55-56", "6:13" or "13:2-" (verse 2 to the end).
 */
internal class VerseSpan(val chapter: Int, val first: Int, val last: Int?) {
    init {
        require(chapter > 0 && first > 0 && (last == null || last >= first)) { "Bad span $this" }
    }

    /** Number of verses, or null when the span runs to the end of the chapter. */
    val size: Int? get() = last?.let { it - first + 1 }

    operator fun contains(verse: Int): Boolean = verse >= first && (last == null || verse <= last)

    override fun toString(): String = "$chapter:$first-${last ?: ""}"

    companion object {
        fun parse(text: String): VerseSpan {
            val (chapter, verses) = text.split(':')
            val first = verses.substringBefore('-').toInt()
            val last = if ('-' !in verses) first else verses.substringAfter('-').toIntOrNull()
            return VerseSpan(chapter.toInt(), first, last)
        }
    }
}

/**
 * The same text in both numberings. Either both spans have the same size (both may run to the chapter end),
 * or one side is a single verse that the other side splits into several.
 *
 * @property moved the text sits in another place in the other numbering, out of order (the Romans doxology).
 */
internal class VerseSegment(val english: VerseSpan, val russian: VerseSpan, val moved: Boolean = false) {
    init {
        require(english.size == russian.size || (english.size != null && russian.size != null && (english.size == 1 || russian.size == 1))) {
            "Segment $english = $russian must keep its size or split one verse"
        }
    }

    override fun toString(): String = "$english = $russian"
}

/**
 * Where Synodal numbering (RST, NRT) differs from the KJV tradition (KJV, ASV), per book number.
 *
 * Built by aligning every verse of the backend's KJV and RST files by their Strong's numbers
 * (2026-10-19). A chapter listed here is covered from verse 1 to its end on each side it appears on;
 * chapters not listed have the same numbers in both. Joel, Malachi and the other places where the
 * Hebrew Bible numbers differently from KJV keep KJV numbering in RST, so they are not listed.
 *
 * Chapters with the same verse count where a verse boundary moves by part of a verse (Esther 1:6-8,
 * 1 Kings 18:33-34, Ezekiel 1:28-2:1, Acts 24:2-3, Revelation 20:7-9) are left as they are: the same
 * numbers open the right place.
 */
internal object SynodalMapping {
    val segments: Map<Int, List<VerseSegment>> = buildMap {
        book(30) { // Leviticus
            same("14:1-54")
            map("14:55-56", "14:55")
            map("14:57", "14:56")
        }
        book(40) { // Numbers
            same("12:1-15")
            map("12:16", "13:1")
            map("13:1-", "13:2-")
            same("29:1-39")
            map("29:40", "30:1")
            map("30:1-", "30:2-")
        }
        book(60) { // Joshua
            same("5:1-15")
            map("6:1", "5:16")
            map("6:2-", "6:1-")
        }
        book(90) { // 1 Samuel
            same("20:1-41")
            map("20:42", "20:42-43")
            same("23:1-28")
            map("23:29", "24:1")
            map("24:1-", "24:2-")
        }
        book(220) { // Job
            same("39:1-30")
            map("40:1-5", "39:31-35")
            map("40:6-24", "40:1-19")
            map("41:1-8", "40:20-27")
            map("41:9-", "41:1-")
        }
        book(PSALMS) { psalms() }
        book(250) { // Ecclesiastes
            same("4:1-16")
            map("5:1", "4:17")
            map("5:2-", "5:1-")
        }
        book(260) { // Song of Songs
            map("1:1-2", "1:1")
            map("1:3-", "1:2-")
            same("6:1-12")
            map("6:13", "7:1")
            map("7:1-", "7:2-")
        }
        book(290) { // Isaiah
            same("3:1-18")
            map("3:19-20", "3:19")
            map("3:21-", "3:20-")
        }
        book(340) { // Daniel
            same("3:1-30")
            map("4:1-3", "3:31-33")
            map("4:4-", "4:1-")
        }
        book(350) { // Hosea
            same("13:1-15")
            map("13:16", "14:1")
            map("14:1-", "14:2-")
        }
        book(390) { // Jonah
            same("1:1-16")
            map("1:17", "2:1")
            map("2:1-", "2:2-")
        }
        book(510) { // Acts
            same("19:1-39")
            map("19:40-41", "19:40")
        }
        book(520) { // Romans
            same("14:1-23")
            same("16:1-24")
            map("16:25-27", "14:24-26", moved = true)
        }
        book(540) { // 2 Corinthians
            same("11:1-31")
            map("11:32-33", "11:32")
            same("13:1-11")
            map("13:12-13", "13:12")
            map("13:14", "13:13")
        }
        book(640) { // Philemon
            same("1:1-22")
            map("1:23-24", "1:23")
            map("1:25", "1:24")
        }
        book(710) { // 3 John
            same("1:1-13")
            map("1:14", "1:14-15")
        }
    }

    /**
     * KJV psalm to RST psalm: from 9 on, RST joins KJV 9 and 10, and 114 and 115, splits 116 and 147,
     * so 11 to 113 and 117 to 146 are one lower.
     */
    private fun SegmentsBuilder.psalms() {
        // Title verses in RST before the verse KJV numbers 1, for psalms that only shift.
        val titleVerses = mapOf(
            1 to listOf(
                3, 4, 5, 6, 7, 8, 12, 18, 19, 20, 21, 22, 30, 31, 34, 36, 38, 39, 40, 41, 42, 44, 45, 46, 47, 48,
                49, 53, 55, 56, 57, 58, 59, 61, 62, 63, 64, 65, 67, 68, 69, 70, 75, 76, 77, 80, 81, 83, 84, 85,
                88, 89, 92, 102, 108, 140,
            ),
            2 to listOf(51, 52, 54, 60),
        ).flatMap { (count, psalms) -> psalms.map { it to count } }.toMap()

        for (kjv in 3..147) {
            when (kjv) {
                9 -> {
                    map("9:1", "9:1-2")
                    map("9:2-20", "9:3-21")
                }
                10 -> map("10:1-", "9:22-")
                // Title split off as in the others, and the last two KJV verses are one in RST.
                13 -> {
                    map("13:1", "12:1-2")
                    map("13:2-4", "12:3-5")
                    map("13:5-6", "12:6")
                }
                90 -> {
                    map("90:1", "89:1-2")
                    map("90:2-4", "89:3-5")
                    map("90:5-6", "89:6")
                    map("90:7-", "89:7-")
                }
                114 -> map("114:1-8", "113:1-8")
                115 -> map("115:1-", "113:9-")
                116 -> {
                    map("116:1-9", "114:1-9")
                    map("116:10-", "115:1-")
                }
                147 -> {
                    map("147:1-11", "146:1-11")
                    map("147:12-", "147:1-")
                }
                else -> {
                    val rst = if (kjv < 9) kjv else kjv - 1
                    val titles = titleVerses[kjv] ?: 0
                    if (titles == 0) {
                        map("$kjv:1-", "$rst:1-")
                    } else {
                        map("$kjv:1", "$rst:1-${1 + titles}")
                        map("$kjv:2-", "$rst:${2 + titles}-")
                    }
                }
            }
        }
    }

    private const val PSALMS = 230

    private class SegmentsBuilder {
        val segments = mutableListOf<VerseSegment>()

        fun map(english: String, russian: String, moved: Boolean = false) {
            segments += VerseSegment(VerseSpan.parse(english), VerseSpan.parse(russian), moved)
        }

        fun same(verses: String) = map(verses, verses)
    }

    private fun MutableMap<Int, List<VerseSegment>>.book(number: Int, build: SegmentsBuilder.() -> Unit) {
        put(number, SegmentsBuilder().apply(build).segments)
    }
}
