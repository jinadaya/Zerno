package app.zerno.feature.reflection.data

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.data.time.FakeClock
import app.zerno.entities.bible.BookId
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.api.GamificationEvents
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

val John3 = PassageRef.chapter(BookId(500), 3)
val Psalm23 = PassageRef.chapter(BookId(230), 23)

class ReflectionsServiceTest {
    private val rows = mutableMapOf<String, String>()
    private val dao = object : AppMetaDao {
        override suspend fun get(key: String) = rows[key]
        override suspend fun put(meta: AppMeta) {
            rows[meta.key] = meta.value
        }
    }
    private val clock = FakeClock(Instant.parse("2026-10-22T07:00:00Z"))
    private val events = FakeEvents()
    private fun service() = ReflectionsService(ReflectionStore(dao), events, clock)

    @Test
    fun savedReflectionsAreListedNewestFirstAndSurviveARestart() = runTest {
        val service = service()
        service.save(John3, "  God so loved  ")
        clock.advanceBy(1.minutes)
        service.save(Psalm23, "The Lord is my shepherd", SessionId("s1"), afterPlanStep = true)

        val listed = service().all.first()
        assertEquals(listOf(Psalm23, John3), listed.map { it.reflection.passage })
        assertEquals("God so loved", listed[1].reflection.text)
        assertEquals(SessionId("s1"), listed[0].sessionId)
        assertEquals(listOf(false, true), events.reflections)
    }

    @Test
    fun aSessionKeepsOneReflection() = runTest {
        val service = service()
        val first = service.save(John3, "First", SessionId("s1"))
        clock.advanceBy(2.minutes)
        val second = service.save(John3, "Second", SessionId("s1"))

        assertEquals(first.reflection.id, second.reflection.id)
        assertEquals("Second", service.forSession(SessionId("s1"))?.reflection?.text)
        assertEquals(first.reflection.createdAt, second.reflection.createdAt)
        assertEquals(Instant.parse("2026-10-22T07:02:00Z"), second.reflection.updatedAt)
        assertEquals(1, service.all.first().size)
        assertEquals(listOf(false), events.reflections, "an edit is not a new reflection")
    }

    @Test
    fun emptyTextIsRefused() = runTest {
        assertFailsWith<IllegalArgumentException> { service().save(John3, "   ") }
        assertTrue(events.reflections.isEmpty())
    }

    @Test
    fun unreadableStoredValueCountsAsNone() = runTest {
        rows["reflections"] = "{not json"
        assertTrue(service().all.first().isEmpty())
        assertNull(service().forSession(SessionId("s1")))
    }

    @Test
    fun editingKeepsTheReflectionAndDeletingRemovesIt() = runTest {
        val service = service()
        val first = service.save(John3, "First")
        clock.advanceBy(1.minutes)
        service.save(Psalm23, "Shepherd")
        clock.advanceBy(1.minutes)

        val edited = service.edit(first.reflection.id, "  Edited ")
        assertEquals("Edited", edited.reflection.text)
        assertEquals(first.reflection.createdAt, edited.reflection.createdAt)
        assertEquals(Instant.parse("2026-10-22T07:02:00Z"), edited.reflection.updatedAt)
        assertEquals("Edited", service().get(first.reflection.id)?.reflection?.text)
        assertEquals(listOf(false, false), events.reflections, "an edit is not a new reflection")

        service.delete(first.reflection.id)
        assertNull(service.get(first.reflection.id))
        assertEquals(listOf(Psalm23), service().all.first().map { it.reflection.passage })
    }

    private class FakeEvents : GamificationEvents {
        val reflections = mutableListOf<Boolean>()

        override suspend fun onSessionCompleted(session: ReadingSession) = Unit

        override suspend fun onReflectionSaved(afterPlanStep: Boolean) {
            reflections += afterPlanStep
        }

        override suspend fun onVerseSaved() = Unit

        override suspend fun onQuestionAnswered() = Unit

        override suspend fun onPlanCompleted(wholeBible: Boolean) = Unit
    }
}
