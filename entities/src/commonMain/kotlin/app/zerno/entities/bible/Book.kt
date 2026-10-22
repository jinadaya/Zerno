package app.zerno.entities.bible

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Book number in the MyBible format: 10 is Genesis, 470 is Matthew, 730 is Revelation.
 * The same number means the same book in every translation.
 */
@Serializable
@JvmInline
value class BookId(val value: Int) : Comparable<BookId> {
    init {
        require(value in RANGE) { "BookId $value is outside $RANGE" }
    }

    val testament: Testament
        get() = if (value < MATTHEW) Testament.Old else Testament.New

    override fun compareTo(other: BookId): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()

    companion object {
        val RANGE = 10..730
        private const val MATTHEW = 470
    }
}

enum class Testament { Old, New }

/**
 * A book as one translation names it.
 *
 * @property shortName abbreviation from the translation file, e.g. "Ин" or "John".
 * @property name full name, e.g. "От Иоанна" or "John".
 */
@Serializable
data class Book(val id: BookId, val shortName: String, val name: String, val chapterCount: Int) {
    init {
        require(chapterCount > 0) { "Book $id must have at least one chapter" }
    }
}

/** One chapter of a book, independent of translation. */
@Serializable
data class ChapterRef(val book: BookId, val chapter: Int) : Comparable<ChapterRef> {
    init {
        require(chapter > 0) { "Chapter must be positive, was $chapter" }
    }

    override fun compareTo(other: ChapterRef): Int = compareValuesBy(this, other, ChapterRef::book, ChapterRef::chapter)
}

/**
 * One verse of one translation.
 *
 * @property text verse text with MyBible markup turned into plain text and annotations.
 */
@Serializable
data class Verse(val translation: TranslationId, val chapter: ChapterRef, val number: Int, val text: VerseText) {
    init {
        require(number > 0) { "Verse number must be positive, was $number" }
    }
}
