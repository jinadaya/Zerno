package app.zerno.feature.reader.domain

import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.mapVersification

/**
 * A place in the reader: a chapter of one translation, in that translation's numbering, and the verse at the top of
 * the page. A null [verse] is the top of the chapter, above its first verse.
 */
data class ReaderLocation(val translation: TranslationId, val chapter: ChapterRef, val verse: Int? = null)

/**
 * The same place in [target], whose numbering is [to] while this one's is [from]: KJV Psalm 23:4 is RST Psalm 22:4,
 * KJV Jonah 1:17 is RST Jonah 2:1. The top of a chapter stays the top of the chapter it maps to.
 */
fun ReaderLocation.inTranslation(target: TranslationId, from: Versification, to: Versification): ReaderLocation {
    val passage = if (verse == null) {
        PassageRef.chapter(chapter.book, chapter.chapter)
    } else {
        PassageRef.verse(chapter.book, chapter.chapter, verse)
    }
    val mapped = passage.mapVersification(from, to)
    return ReaderLocation(
        translation = target,
        chapter = ChapterRef(chapter.book, mapped.startChapter),
        verse = if (verse == null) null else mapped.startVerse ?: 1,
    )
}
