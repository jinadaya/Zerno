package app.zerno.feature.reader.domain

import app.zerno.data.time.FakeClock
import app.zerno.data.time.FakeTimeZone
import app.zerno.entities.achievement.Achievement
import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.achievement.AchievementSet
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.plan.PlanId
import kotlinx.coroutines.test.runTest
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
    private val state = FakeGamificationState()
    private val daily = DailyReading(
        FakeBible(),
        plans,
        gamification,
        state,
        clock,
        FakeTimeZone(TimeZone.of("Europe/Moscow")),
    )
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

        val session = assertNotNull(daily.finish(RST)).session

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

    @Test
    fun finishingReportsOnlyTheSealsItUnlocked() = runTest {
        state.achievements.value = listOf(seal("first_steps", unlocked = true), seal("streak_3", unlocked = false))
        gamification.onSession = {
            state.achievements.value = listOf(seal("first_steps", unlocked = true), seal("streak_3", unlocked = true))
        }
        daily.begin(john, 4, PassageRef.chapter(JOHN, 4))

        val finished = assertNotNull(daily.finish(KJV))

        assertEquals(listOf(AchievementCode("streak_3")), finished.newSeals.map { it.code })
        assertEquals(finished, daily.lastFinished.value)
    }

    @Test
    fun achievementsNotYetComputedAreComputedFirst() = runTest {
        state.onRefresh = { state.achievements.value = listOf(seal("first_steps", unlocked = true)) }
        daily.begin(john, 4, PassageRef.chapter(JOHN, 4))

        assertTrue(assertNotNull(daily.finish(KJV)).newSeals.isEmpty())
    }

    private fun seal(code: String, unlocked: Boolean) = Achievement(
        code = AchievementCode(code),
        set = AchievementSet.Bird,
        titleKey = "t",
        descriptionKey = "d",
        icon = "i",
        unlockedAt = if (unlocked) clock.now() else null,
    )
}
