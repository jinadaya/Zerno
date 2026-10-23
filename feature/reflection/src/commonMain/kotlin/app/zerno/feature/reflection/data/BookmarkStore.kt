package app.zerno.feature.reflection.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.format
import app.zerno.entities.bible.parse
import app.zerno.entities.saved.Bookmark
import app.zerno.entities.saved.BookmarkId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Bookmarks as JSON in the `bookmarks` row of the `app_meta` key-value table, so the prototype needs no schema
 * change. A value the app cannot read back counts as none; an entry whose passage no longer parses is dropped.
 */
class BookmarkStore(private val meta: AppMetaDao) {
    suspend fun load(): List<Bookmark> = meta.get(KEY)?.let { value ->
        runCatching { json.decodeFromString<List<StoredBookmark>>(value).mapNotNull { it.toBookmark() } }.getOrNull()
    }.orEmpty()

    suspend fun save(bookmarks: List<Bookmark>) {
        meta.put(AppMeta(KEY, json.encodeToString(bookmarks.map(StoredBookmark::from))))
    }

    @Serializable
    private data class StoredBookmark(
        val id: String,
        /** English reference text, which `PassageRef.parse` reads back. */
        val passage: String,
        val translation: String,
        val createdAt: Long,
    ) {
        fun toBookmark(): Bookmark? {
            val ref = PassageRef.parse(passage) ?: return null
            return Bookmark(BookmarkId(id), ref, TranslationId(translation), Instant.fromEpochMilliseconds(createdAt))
        }

        companion object {
            fun from(bookmark: Bookmark) = StoredBookmark(
                id = bookmark.id.value,
                passage = bookmark.passage.format(),
                translation = bookmark.translation.value,
                createdAt = bookmark.createdAt.toEpochMilliseconds(),
            )
        }
    }

    private companion object {
        const val KEY = "bookmarks"
        val json = Json { ignoreUnknownKeys = true }
    }
}
