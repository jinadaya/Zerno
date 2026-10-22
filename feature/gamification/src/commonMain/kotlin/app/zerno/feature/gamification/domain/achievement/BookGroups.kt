package app.zerno.feature.gamification.domain.achievement

import app.zerno.entities.bible.BookId

/** Named groups of books that achievements can ask for, by MyBible book number. */
object BookGroups {
    /** Matthew, Mark, Luke, John. */
    val Gospels: Set<BookId> = setOf(BookId(470), BookId(480), BookId(490), BookId(500))

    val byName: Map<String, Set<BookId>> = mapOf("gospels" to Gospels)
}
