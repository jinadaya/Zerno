package app.zerno.entities.saved

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@Serializable
@JvmInline
value class BookmarkId(val value: String) {
    override fun toString(): String = value
}

/**
 * A saved verse or passage (A6).
 *
 * @property translation translation the passage was saved from, so its numbering can be mapped (EN-03).
 */
@Serializable
data class Bookmark(
    val id: BookmarkId,
    val passage: PassageRef,
    val translation: TranslationId,
    val createdAt: Instant,
)
