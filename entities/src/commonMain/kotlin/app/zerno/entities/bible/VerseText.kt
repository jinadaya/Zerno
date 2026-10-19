package app.zerno.entities.bible

import kotlinx.serialization.Serializable

/**
 * Verse text with MyBible markup turned into plain text plus annotations.
 *
 * @property text clean text: Strong's numbers and footnote markers removed, runs of whitespace collapsed to one
 * space, and `\n` where the file breaks a line (`<br/>`, a `<t>` poetry line, or a paragraph inside the verse).
 * @property spans styled ranges of [text], ordered by start.
 * @property paragraphStarts offsets in [text] where a new paragraph begins; 0 means the verse opens a paragraph.
 */
@Serializable
data class VerseText(
    val text: String,
    val spans: List<VerseSpan> = emptyList(),
    val paragraphStarts: List<Int> = emptyList(),
) {
    val startsParagraph: Boolean
        get() = paragraphStarts.firstOrNull() == 0
}

/** [style] applies to `text.substring(start, end)`. */
@Serializable
data class VerseSpan(
    val style: VerseStyle,
    val start: Int,
    val end: Int,
) {
    init {
        require(start in 0 until end) { "Span $start..$end is empty or negative" }
    }
}

enum class VerseStyle {
    /** `<J>`: kept as an annotation, rendered unstyled for now. */
    WordsOfJesus,

    /** `<i>` (KJV supplied words), `<em>` and `<e>`. */
    Italic,

    /** `<n>`: a note inside the text, such as a psalm title in ASV; rendered muted. */
    Note,
}
