package app.zerno.feature.reader.domain

import app.zerno.data.time.FakeClock
import app.zerno.data.time.FakeTimeZone
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.entities.session.ReadingSession
import app.zerno.feature.gamification.api.GamificationEvents
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.api.Plans
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class DailyReadingTest {
    private val clock = FakeClock(Instant.parse("2026-10-22T07:00:00Z"))
    private val plans = FakePlans()
    private val gamification = FakeEvents()
    private val daily =
        DailyReading(FakeBible(), plans, gamification, clock, FakeTimeZone(TimeZone.of("Europe/Moscow")))
    private val john = PlanId("gospel-of-john")

    @Test
    fun withoutAPlanDayNothingFinishes() = runTest {
        assertFalse(daily.endsAt(ReaderLocation(KJV, ChapterRef(JOHN, 4))))
        assertNull(daily.finish(KJV))
        assertTrue(gamification.sessions.isEmpty())
    }

    @Test
    fun finishingIsOfferedOnThePassagesLastChapter() = runTest {
        daily.begin(john, 4, PassageRef(JOHN, 4, 46, 5, 15))

        assertFalse(daily.endsAt(ReaderLocation(KJV, ChapterRef(JOHN, 4))))
        assertTrue(daily.endsAt(ReaderLocation(KJV, ChapterRef(JOHN, 5), 3)))
        assertFalse(daily.endsAt(ReaderLocation(KJV, ChapterRef(PSALMS, 5))))
    }

    @Test
    fun theLastChapterFollowsTheTranslationsNumbering() = runTest {
        daily.begin(john, 1, PassageRef.chapter(PSALMS, 23))

        assertTrue(daily.endsAt(ReaderLocation(RST, ChapterRef(PSALMS, 22))))
        assertFalse(daily.endsAt(ReaderLocation(RST, ChapterRef(PSALMS, 23))))
        assertTrue(daily.endsAt(ReaderLocation(KJV, ChapterRef(PSALMS, 23))))
    }

    @Test
    fun finishingRecordsTheSessionAndTicksThePlanDay() = runTest {
        daily.begin(john, 4, PassageRef.chapter(JOHN, 4))
        clock.advanceBy(7.minutes)

        val session = assertNotNull(daily.finish(RST))

        assertEquals(listOf(session), gamification.sessions)
        assertEquals(PassageRef.chapter(JOHN, 4), session.passage)
        assertEquals(RST, session.translation)
        assertEquals(john, session.planId)
        assertEquals(4, session.planDay)
        assertEquals(Instant.parse("2026-10-22T07:00:00Z"), session.startedAt)
        assertEquals(Instant.parse("2026-10-22T07:07:00Z"), session.completedAt)
        assertEquals("Europe/Moscow", session.timeZoneId)
        assertEquals(listOf(john to 4), plans.completed)
        assertNull(daily.current.value)
    }

    @Test
    fun comingBackToTheSameDayKeepsItsStart() = runTest {
        daily.begin(john, 4, PassageRef.chapter(JOHN, 4))
        clock.advanceBy(3.minutes)
        daily.begin(john, 4, PassageRef.chapter(JOHN, 4))
        assertEquals(Instant.parse("2026-10-22T07:00:00Z"), daily.current.value?.startedAt)

        daily.begin(john, 5, PassageRef.chapter(JOHN, 5))
        assertEquals(Instant.parse("2026-10-22T07:03:00Z"), daily.current.value?.startedAt)
    }

    private class FakePlans : Plans {
        val completed = mutableListOf<Pair<PlanId, Int>>()

        override suspend fun catalog(): List<ReadingPlan> = emptyList()

        override suspend fun start(planId: PlanId) = Unit

        override suspend fun completeDay(planId: PlanId, day: Int) {
            completed += planId to day
        }

        override fun today(date: LocalDate): Flow<PlanToday?> = emptyFlow()
    }

    private class FakeEvents : GamificationEvents {
        val sessions = mutableListOf<ReadingSession>()

        override suspend fun onSessionCompleted(session: ReadingSession) {
            sessions += session
        }

        override suspend fun onReflectionSaved(afterPlanStep: Boolean) = Unit

        override suspend fun onVerseSaved() = Unit

        override suspend fun onQuestionAnswered() = Unit

        override suspend fun onPlanCompleted(wholeBible: Boolean) = Unit
    }
}
