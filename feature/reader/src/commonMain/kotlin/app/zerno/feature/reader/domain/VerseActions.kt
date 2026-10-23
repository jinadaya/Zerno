package app.zerno.feature.reader.domain

import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Verse
import app.zerno.entities.bible.VerseStyle
import app.zerno.entities.bible.VerseText
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.format
import app.zerno.entities.bible.mapVersification

/** The verse as its translation numbers it. */
val Verse.passage: PassageRef get() = PassageRef.verse(chapter.book, chapter.chapter, number)

/** The verse in KJV numbering, as bookmarks and reflections keep it. */
fun Verse.kjvPassage(numbering: Versification): PassageRef = passage.mapVersification(numbering, Versification.English)

/** The words without translators' notes, for copying and sharing. */
fun VerseText.plain(): String {
    val notes = spans.filter { it.style == VerseStyle.Note }.sortedBy { it.start }
    if (notes.isEmpty()) return text.trim()
    val kept = StringBuilder()
    var from = 0
    for (note in notes) {
        if (note.start > from) kept.append(text, from, note.start)
        from = maxOf(from, note.end)
    }
    if (from < text.length) kept.append(text, from, text.length)
    return kept.toString().replace(Spaces, " ").trim()
}

private val Spaces = Regex("\\s{2,}")

/**
 * What copy and share put out: the words in quotes, then the reference as the translation numbers it and the
 * translation's tag, e.g. `"For God so loved the world…" John 3:16 (KJV)`.
 */
fun Verse.shareText(language: BookNameLanguage): String =
    "“${text.plain()}” ${passage.format(language)} (${translation.tag})"

private val TranslationId.tag: String get() = value.uppercase()
