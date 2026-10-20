package app.zerno.feature.reader.domain

import app.zerno.entities.bible.Book

private const val PSALMS = 230

/**
 * Title of [chapter] of [book] as the reader shows it: "John 3", "Psalm 23" when the translation has a word for one
 * psalm ([psalmName]), and just "Jude" for a book of one chapter.
 */
fun chapterTitle(book: Book, chapter: Int, psalmName: String?): String = when {
    book.id.value == PSALMS && psalmName != null -> "$psalmName $chapter"
    book.chapterCount == 1 -> book.name
    else -> "${book.name} $chapter"
}
