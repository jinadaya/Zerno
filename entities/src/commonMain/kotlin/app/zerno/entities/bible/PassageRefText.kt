package app.zerno.entities.bible

/**
 * Parses a reference written in English or Russian: "John 4:1-26", "Ин 4:1-26", "Ps 23", "Пс 22",
 * "John 4:46-5:15", "Gen 1-3", "1 Кор. 13:4-8", "Ин 3,16".
 *
 * Accepted forms after the book name, where a dash may be "-", "–" or "—" and the chapter and
 * verse may be split by ":", "," or ".":
 * - `C` a whole chapter, `C-D` whole chapters
 * - `C:V` one verse, `C:V-W` verses in one chapter, `C:V-D:W` across chapters, `C-D:W` from the start of C
 * - `C:Vff` from verse V to the end of chapter C, `C:V-Dff` to the end of chapter D
 * - nothing: the whole book
 *
 * In one-chapter books (Obadiah, Philemon, 2 and 3 John, Jude) a bare number is a verse, so
 * "Jude 3" is Jude 1:3. Numbers are taken as written, in the numbering of the translation the
 * reference is meant for; "Пс 22" stays Psalm 22 (EN-03 maps between numberings).
 *
 * @return the passage, or null when the book is unknown or the numbers do not make a valid passage.
 */
fun PassageRef.Companion.parse(text: String): PassageRef? {
    val clean = text.trim().trimEnd('.').map { if (it in DASHES) '-' else it }.joinToString("")
    val split = locationStart(clean)
    val book = BookNames.find(clean.substring(0, split)) ?: return null
    val location = clean.substring(split).filterNot { it.isWhitespace() }
    val chapters = BookNames.chapterCount(book)
    if (location.isEmpty()) return PassageRef(book, 1, null, chapters, null)
    val numbers = Location.read(location) ?: return null
    val ref = numbers.toPassage(book, singleChapter = chapters == 1) ?: return null
    return ref.takeIf { it.endChapter <= chapters }
}

/** Like [parse], but throws [IllegalArgumentException] naming the text that failed. */
fun PassageRef.Companion.parseOrThrow(text: String): PassageRef =
    parse(text) ?: throw IllegalArgumentException("Not a Bible reference: \"$text\"")

/**
 * Writes the reference with book names in [language]: "John 4:1-26", "Ин 4:1-26", "Ps 23",
 * "John 4:46-5:15". The result parses back to the same [PassageRef].
 *
 * Chapter and verse numbers are written as stored; to show a KJV reference in RST numbering,
 * map it first (EN-03). One-chapter books drop the chapter ("Jude 3", and "Jude" for the whole book).
 * A passage that starts at a verse and runs to a chapter end is written with "ff": "John 4:5ff".
 */
fun PassageRef.format(
    language: BookNameLanguage = BookNameLanguage.English,
    style: BookNameStyle = BookNameStyle.Short,
): String {
    val name = bookName(language, style)
    val location = location(singleChapter = BookNames.chapterCount(book) == 1)
    return if (location.isEmpty()) name else "$name $location"
}

private val DASHES = setOf('-', '–', '—', '‒', '−', '‐')
private val CHAPTER_VERSE_SEPARATORS = setOf(':', ',', '.')

/** Index where the numbers start: the first digit after the book name has begun. */
private fun locationStart(text: String): Int {
    var seenLetter = false
    for ((index, char) in text.withIndex()) {
        if (char.isLetter()) seenLetter = true
        if (char.isDigit() && seenLetter) return index
    }
    return text.length
}

/** Numbers as written: `first[:firstVerse][-second[:secondVerse]]`. */
private class Location(
    val first: Int,
    val firstVerse: Int?,
    val second: Int?,
    val secondVerse: Int?,
    val toChapterEnd: Boolean,
) {
    fun toPassage(book: BookId, singleChapter: Boolean): PassageRef? = runCatching {
        when {
            toChapterEnd -> PassageRef(book, first, firstVerse, second ?: first, null)

            // "Jude 3", "Jude 3-5": numbers without a chapter in a one-chapter book are verses.
            singleChapter && firstVerse == null && secondVerse == null ->
                PassageRef(book, 1, first, 1, second ?: first)

            second == null ->
                if (firstVerse == null) PassageRef(book, first) else PassageRef.verse(book, first, firstVerse)

            // "4:1-26": the number after the dash is a verse in the same chapter.
            firstVerse != null && secondVerse == null -> PassageRef(book, first, firstVerse, first, second)

            else -> PassageRef(book, first, firstVerse, second, secondVerse)
        }
    }.getOrNull()

    companion object {
        fun read(text: String): Location? {
            val reader = NumberReader(text)
            val first = reader.number() ?: return null
            val firstVerse = if (reader.separator()) reader.number() ?: return null else null
            var second: Int? = null
            var secondVerse: Int? = null
            if (reader.dash()) {
                second = reader.number() ?: return null
                if (reader.separator()) secondVerse = reader.number() ?: return null
            }
            val toChapterEnd = reader.following()
            // "ff" needs a start verse and an end without a verse: "4:5ff", "4:5-5ff".
            if (toChapterEnd && (firstVerse == null || secondVerse != null)) return null
            return if (reader.atEnd) Location(first, firstVerse, second, secondVerse, toChapterEnd) else null
        }
    }
}

private class NumberReader(private val text: String) {
    private var position = 0

    val atEnd: Boolean get() = position == text.length

    fun number(): Int? {
        val start = position
        while (position < text.length && text[position].isDigit() && position - start < MAX_DIGITS) position++
        if (position < text.length && text[position].isDigit()) return null
        return text.substring(start, position).toIntOrNull()?.takeIf { it > 0 }
    }

    fun separator(): Boolean = consume { it in CHAPTER_VERSE_SEPARATORS }

    fun dash(): Boolean = consume { it == '-' }

    /** "ff", "and following": the passage runs to the end of the last chapter. */
    fun following(): Boolean {
        if (!text.regionMatches(position, "ff", 0, 2, ignoreCase = true)) return false
        position += 2
        return true
    }

    private inline fun consume(matches: (Char) -> Boolean): Boolean {
        if (position < text.length && matches(text[position])) {
            position++
            return true
        }
        return false
    }

    private companion object {
        const val MAX_DIGITS = 3
    }
}

private fun PassageRef.bookName(language: BookNameLanguage, style: BookNameStyle): String {
    // "Psalm 23" but "Psalms 23-25"; "Псалом 22" but "Псалтирь 22-24".
    if (book.value == PSALMS && style == BookNameStyle.Long && startChapter == endChapter) {
        return if (language == BookNameLanguage.English) "Psalm" else "Псалом"
    }
    return BookNames.name(book, language, style)
}

private fun PassageRef.location(singleChapter: Boolean): String {
    val wholeBook = isWholeChapters && startChapter == 1 && endChapter == BookNames.chapterCount(book)
    if (singleChapter) {
        if (wholeBook) return ""
        val from = startVerse ?: 1
        if (startVerse != null && endVerse == null) return "1:${startVerse}ff"
        return if (endVerse == null || endVerse == from) "$from" else "$from-$endVerse"
    }
    val start = if (startVerse == null) "$startChapter" else "$startChapter:$startVerse"
    return when {
        isWholeChapters && startChapter == endChapter -> start

        isWholeChapters -> "$start-$endChapter"

        startChapter == endChapter && startVerse != null && endVerse != null ->
            if (startVerse == endVerse) start else "$start-$endVerse"

        endVerse == null -> if (startChapter == endChapter) "${start}ff" else "$start-${endChapter}ff"

        else -> "$start-$endChapter:$endVerse"
    }
}

private const val PSALMS = 230
