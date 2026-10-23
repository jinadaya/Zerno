package app.zerno.feature.reflection.data

import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.saved.Bookmark
import app.zerno.entities.saved.BookmarkId
import app.zerno.feature.gamification.api.GamificationEvents
import app.zerno.feature.reflection.api.Bookmarks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** [Bookmarks] on the [BookmarkStore], held in memory after the first read. Keep one per app. */
class BookmarksService(
    private val store: BookmarkStore,
    private val gamification: GamificationEvents,
    private val clock: Clock,
) : Bookmarks {
    private val lock = Mutex()

    /** Newest first; null until first read from the store. */
    private val bookmarks = MutableStateFlow<List<Bookmark>?>(null)

    override val all: Flow<List<Bookmark>> = flow {
        lock.withLock { loaded() }
        emitAll(bookmarks.filterNotNull())
    }

    override suspend fun save(passage: PassageRef, translation: TranslationId): Bookmark {
        val (bookmark, isNew) = lock.withLock {
            val current = loaded()
            current.firstOrNull { it.passage == passage }?.let { return@withLock it to false }
            val now = clock.now()
            val bookmark =
                Bookmark(BookmarkId("bookmark-${now.toEpochMilliseconds()}-${current.size}"), passage, translation, now)
            write(listOf(bookmark) + current)
            bookmark to true
        }
        if (isNew) gamification.onVerseSaved()
        return bookmark
    }

    override suspend fun delete(id: BookmarkId) = lock.withLock {
        val current = loaded()
        if (current.any { it.id == id }) write(current.filterNot { it.id == id })
    }

    private suspend fun write(updated: List<Bookmark>) {
        store.save(updated)
        bookmarks.value = updated
    }

    private suspend fun loaded(): List<Bookmark> = bookmarks.value
        ?: store.load().sortedByDescending { it.createdAt }.also { bookmarks.value = it }
}
