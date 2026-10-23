package app.zerno.feature.reflection.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.data.time.FakeClock
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.session.ReadingSession
import app.zerno.feature.gamification.api.GamificationEvents
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class BookmarksServiceTest {
    private val rows = mutableMapOf<String, String>()
    private val dao = object : AppMetaDao {
        override suspend fun get(key: String) = rows[key]
        override suspend fun put(meta: AppMeta) {
            rows[meta.key] = meta.value
        }
    }
    private val clock = FakeClock(Instant.parse("2026-10-22T07:00:00Z"))
    private val events = VerseEvents()
    private val kjv = TranslationId("kjv")

    private fun service() = BookmarksService(BookmarkStore(dao), events, clock)

    @Test
    fun bookmarksAreListedNewestFirstAndSurviveARestart() = runTest {
        val service = service()
        service.save(John3, kjv)
        clock.advanceBy(1.minutes)
        service.save(Psalm23, TranslationId("rst"))

        val listed = service().all.first()
        assertEquals(listOf(Psalm23, John3), listed.map { it.passage })
        assertEquals(TranslationId("rst"), listed[0].translation)
        assertEquals(2, events.verses)
    }

    @Test
    fun savingTheSameVerseAgainKeepsOneAndCountsOnce() = runTest {
        val service = service()
        val first = service.save(John3, kjv)
        clock.advanceBy(1.minutes)
        val second = service.save(John3, kjv)

        assertEquals(first, second)
        assertEquals(1, service.all.first().size)
        assertEquals(1, events.verses)
    }

    @Test
    fun deletingRemovesTheBookmark() = runTest {
        val service = service()
        val bookmark = service.save(John3, kjv)

        service.delete(bookmark.id)

        assertTrue(service().all.first().isEmpty())
    }

    @Test
    fun unreadableStoredValueCountsAsNone() = runTest {
        rows["bookmarks"] = "{not json"
        assertTrue(service().all.first().isEmpty())
    }

    private class VerseEvents : GamificationEvents {
        var verses = 0

        override suspend fun onSessionCompleted(session: ReadingSession) = Unit

        override suspend fun onReflectionSaved(afterPlanStep: Boolean) = Unit

        override suspend fun onVerseSaved() {
            verses++
        }

        override suspend fun onQuestionAnswered() = Unit

        override suspend fun onPlanCompleted(wholeBible: Boolean) = Unit
    }
}
