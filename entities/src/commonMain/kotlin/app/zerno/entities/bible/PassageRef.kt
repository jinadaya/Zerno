package app.zerno.entities.bible

import kotlinx.serialization.Serializable

/**
 * A continuous passage inside one book, in the numbering of the translation it was taken from.
 *
 * Verses are optional: a null [startVerse] means the passage starts at the beginning of
 * [startChapter], a null [endVerse] means it runs to the end of [endChapter].
 * So "Ps 23" is `PassageRef(Ps, 23, null, 23, null)` and "John 4:1-26" is
 * `PassageRef(John, 4, 1, 4, 26)`. [parse] and [format] in PassageRefText.kt read and write it as text.
 */
@Serializable
data class PassageRef(
    val book: BookId,
    val startChapter: Int,
    val startVerse: Int? = null,
    val endChapter: Int = startChapter,
    val endVerse: Int? = null,
) {
    init {
        require(startChapter > 0) { "Start chapter must be positive, was $startChapter" }
        require(endChapter >= startChapter) { "End chapter $endChapter is before start chapter $startChapter" }
        require(startVerse == null || startVerse > 0) { "Start verse must be positive, was $startVerse" }
        require(endVerse == null || endVerse > 0) { "End verse must be positive, was $endVerse" }
        if (endChapter == startChapter && startVerse != null && endVerse != null) {
            require(endVerse >= startVerse) { "End verse $endVerse is before start verse $startVerse" }
        }
    }

    val isWholeChapters: Boolean
        get() = startVerse == null && endVerse == null

    /** Every chapter the passage touches, in order. */
    val chapters: List<ChapterRef>
        get() = (startChapter..endChapter).map { ChapterRef(book, it) }

    companion object {
        fun chapter(book: BookId, chapter: Int): PassageRef = PassageRef(book, chapter)

        fun verse(book: BookId, chapter: Int, verse: Int): PassageRef = PassageRef(book, chapter, verse, chapter, verse)
    }
}
