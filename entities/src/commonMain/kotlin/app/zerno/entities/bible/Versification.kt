package app.zerno.entities.bible

import kotlinx.serialization.Serializable

/**
 * Chapter and verse numbering a translation follows. A Bible file says which in the
 * `russian_numbering` key of its `info` table (schema v1, section 4.3).
 */
@Serializable
enum class Versification {
    /** The KJV tradition: KJV, ASV. */
    English,

    /**
     * Synodal numbering: RST, NRT. Most of KJV Psalms 10 to 147 are one lower, many psalm titles
     * are verses of their own, and a few chapter breaks sit elsewhere (Job 40 and 41, Daniel 4, Romans 16).
     */
    Russian,
    ;

    companion object {
        /** Reads the `info` value of `russian_numbering`: "true" is [Russian], anything else or a missing key is [English]. */
        fun fromRussianNumbering(value: String?): Versification =
            if (value?.trim().equals("true", ignoreCase = true)) Russian else English
    }
}

/**
 * The same passage in [to] numbering, for a passage written in [from] numbering:
 * KJV Psalm 23 is RST Psalm 22, KJV Malachi 4 stays Malachi 4, KJV Jonah 1:17 is RST Jonah 2:1.
 *
 * Whole chapters stay whole where the target has the same chapter: "Ps 51" in KJV is "Пс 50" in RST,
 * with the title verses included. Where one verse becomes several (a psalm title split from its first
 * verse, 3 John 14), a start maps to the first of them and an end to the last, so KJV Psalm 51:1 is
 * RST Psalm 50:1-3. Where several verses become one, they all map to it.
 *
 * Text the target keeps in another chapter (the closing doxology of Romans, KJV 16:25-27 and RST 14:24-26)
 * moves only when the passage lies inside it; a longer passage stops before it instead, so KJV Romans 16
 * is RST Romans 16 rather than a range running back into chapter 14.
 */
fun PassageRef.mapVersification(from: Versification, to: Versification): PassageRef {
    if (from == to) return this
    val segments = SynodalMapping.segments[book.value] ?: return this
    return VersificationMapper(from, segments).map(this)
}

private class VersificationMapper(private val from: Versification, segments: List<VerseSegment>) {
    private val VerseSegment.source: ChapterSpan get() = if (from == Versification.English) english else russian
    private val VerseSegment.target: ChapterSpan get() = if (from == Versification.English) russian else english

    private val bySource = segments.groupBy { it.source.chapter }.mapValues { (_, list) -> list.sortedBy { it.source.first } }
    private val byTarget = segments.groupBy { it.target.chapter }.mapValues { (_, list) -> list.sortedBy { it.target.first } }

    fun map(ref: PassageRef): PassageRef {
        var start = Place(ref.startChapter, ref.startVerse)
        var end = Place(ref.endChapter, ref.endVerse)
        val first = segmentAt(start, atStart = true)
        val last = segmentAt(end, atStart = false)
        if (first !== last) {
            if (first?.moved == true) start = after(first)
            if (last?.moved == true) end = before(last)
        }
        val toChapterEnd = ref.endVerse == null
        passage(ref.book, mapStart(start), mapEnd(end, toChapterEnd))?.let { return it }
        // Stepping around moved text crossed the ends over; map the passage as written instead.
        val plainStart = mapStart(Place(ref.startChapter, ref.startVerse))
        return passage(ref.book, plainStart, mapEnd(Place(ref.endChapter, ref.endVerse), toChapterEnd))
            ?: PassageRef(ref.book, plainStart.chapter, plainStart.verse, plainStart.chapter, plainStart.verse)
    }

    /** A null verse is the start of the chapter for a start, and its end for an end. */
    private data class Place(val chapter: Int, val verse: Int?)

    private fun segmentAt(place: Place, atStart: Boolean): VerseSegment? {
        val segments = bySource[place.chapter] ?: return null
        val verse = place.verse ?: return if (atStart) segments.first() else segments.last()
        return segments.firstOrNull { verse in it.source } ?: segments.last()
    }

    private fun mapStart(place: Place): Place {
        val segment = segmentAt(place, atStart = true) ?: return place
        val verse = segment.targetVerse(place.verse ?: 1, atStart = true)
        // Kept null when the passage still starts at the top of a chapter in the target.
        return Place(segment.target.chapter, if (place.verse == null && verse == 1) null else verse)
    }

    /** [toChapterEnd]: the passage as written runs to the end of a chapter, so it should in the target too. */
    private fun mapEnd(place: Place, toChapterEnd: Boolean): Place {
        val segment = segmentAt(place, atStart = false) ?: return place
        val target = segment.target
        val verse = if (place.verse == null) target.last else segment.targetVerse(place.verse, atStart = false)
        val atChapterEnd = verse == null || byTarget.getValue(target.chapter).last().target.last == verse
        return Place(target.chapter, if (toChapterEnd && atChapterEnd) null else verse)
    }

    private fun VerseSegment.targetVerse(verse: Int, atStart: Boolean): Int {
        val source = source
        val target = target
        val clamped = source.last?.let { minOf(verse, it) } ?: verse
        return when {
            source.size == target.size -> target.first + (clamped - source.first)
            target.size == 1 -> target.first
            atStart -> target.first
            else -> target.last ?: target.first
        }
    }

    /** First source place after [segment]: the next verse, or the start of the next chapter. */
    private fun after(segment: VerseSegment): Place {
        val source = segment.source
        val next = bySource.getValue(source.chapter).let { it.getOrNull(it.indexOf(segment) + 1) }
        return if (next != null) Place(source.chapter, next.source.first) else Place(source.chapter + 1, null)
    }

    /** Last source place before [segment]: the previous verse, or the end of the previous chapter. */
    private fun before(segment: VerseSegment): Place {
        val source = segment.source
        return if (source.first > 1) Place(source.chapter, source.first - 1) else Place(source.chapter - 1, null)
    }

    private fun passage(book: BookId, start: Place, end: Place): PassageRef? {
        if (start.chapter < 1 || end.chapter < start.chapter) return null
        // "Rom 14:1-23" rather than "from the top of 14 to 14:23" when only the end lost its chapter end.
        val startVerse = start.verse ?: if (end.verse != null) 1 else null
        return runCatching { PassageRef(book, start.chapter, startVerse, end.chapter, end.verse) }.getOrNull()
    }
}
