package app.zerno.feature.reader.domain

import app.zerno.entities.bible.Book
import app.zerno.entities.bible.ChapterRef

/** The chapter after [chapter] in these books' order, crossing into the next book; null after the last one. */
fun List<Book>.chapterAfter(chapter: ChapterRef): ChapterRef? {
    val index = indexOfFirst { it.id == chapter.book }
    if (index < 0) return null
    val book = this[index]
    if (chapter.chapter < book.chapterCount) return ChapterRef(book.id, chapter.chapter + 1)
    return getOrNull(index + 1)?.let { ChapterRef(it.id, 1) }
}

/** The chapter before [chapter], crossing into the last chapter of the previous book; null before the first one. */
fun List<Book>.chapterBefore(chapter: ChapterRef): ChapterRef? {
    val index = indexOfFirst { it.id == chapter.book }
    if (index < 0) return null
    if (chapter.chapter > 1) return ChapterRef(chapter.book, minOf(chapter.chapter - 1, this[index].chapterCount))
    return getOrNull(index - 1)?.let { ChapterRef(it.id, it.chapterCount) }
}
