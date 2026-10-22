package app.zerno.feature.gamification.data

import app.zerno.data.time.FakeClock
import app.zerno.data.time.FakeTimeZone
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.TranslationId
import app.zerno.entities.bible.parseOrThrow
import app.zerno.entities.session.ReadingSession
import app.zerno.entities.session.SessionId
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.gamification.domain.achievement.AchievementCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class GamificationServiceTest {
    private val clock = FakeClock(Instant.parse("2026-03-02T07:00:00Z"))
    private val zone = FakeTimeZone(TimeZone.of("Europe/Moscow"))
    private val dao = FakeGamificationDao()
    private var sessionCount = 0

    private suspend fun TestScope.service(scope: CoroutineScope = this): GamificationService {
        // Loaded up front so the service never waits on real file IO inside virtual time.
        val catalog = BundledAchievementCatalog().load()
        return newService(scope, catalog)
    }

    private fun newService(scope: CoroutineScope, catalog: AchievementCatalog) = GamificationService(
        repository = GamificationRepository(dao),
        loadCatalog = { catalog },
        clock = clock,
        zones = zone,
        scope = scope,
    )

    private fun session(passage: String = "John 1"): ReadingSession {
        val id = SessionId("s${++sessionCount}")
        return ReadingSession(
            id = id,
            passage = PassageRef.parseOrThrow(passage),
            translation = TranslationId("kjv"),
            startedAt = clock.now() - 10.minutes,
            completedAt = clock.now(),
            timeZoneId = zone.zone.id,
        )
    }

    @Test
    fun firstSessionStartsTheStreakAndUnlocksFirstSteps() = runTest {
        val service = service()
        advanceUntilIdle()

        val unlocked = mutableListOf<String>()
        backgroundScope.launchCollect(service) { unlocked += it }
        runCurrent()
        service.onSessionCompleted(session())
        advanceUntilIdle()

        assertEquals(1, service.streak.value.current)
        assertEquals(LocalDate(2026, 3, 2), service.streak.value.today)
        assertEquals(StreakDayStatus.Read, service.streak.value.week.first { it.date == LocalDate(2026, 3, 2) }.status)
        assertEquals(setOf("first_steps", "first_chapter"), unlocked.toSet())
        assertTrue(service.achievements.value.single { it.code.value == "first_steps" }.isUnlocked)
    }

    @Test
    fun nothingUnlocksTwice() = runTest {
        val service = service()
        val unlocked = mutableListOf<String>()
        backgroundScope.launchCollect(service) { unlocked += it }
        advanceUntilIdle()
        service.onSessionCompleted(session())
        clock.now += 1.days
        service.onSessionCompleted(session("John 2"))
        advanceUntilIdle()

        assertEquals(1, unlocked.count { it == "first_steps" })
        assertEquals(2, service.streak.value.current)
        // The first unlock date is kept.
        assertEquals(Instant.parse("2026-03-02T07:00:00Z"), GamificationRepository(dao).unlocked.first().entries.first { it.key.value == "first_steps" }.value)
    }

    @Test
    fun reflectionTheSameDayMarksTheDayDone() = runTest {
        val service = service()
        service.onSessionCompleted(session())
        service.onReflectionSaved(afterPlanStep = true)
        advanceUntilIdle()

        assertEquals(StreakDayStatus.Done, service.streak.value.week.first { it.date == LocalDate(2026, 3, 2) }.status)
        val unlocked = service.achievements.value.filter { it.isUnlocked }.map { it.code.value }.toSet()
        assertTrue("first_light" in unlocked, "a plan-step reflection is a reflection")
        assertTrue("paused_to_think" in unlocked)
    }

    @Test
    fun unfinishedSessionIsIgnored() = runTest {
        val service = service()
        service.onSessionCompleted(session().copy(completedAt = null))
        advanceUntilIdle()

        assertEquals(0, service.streak.value.current)
        assertTrue(service.achievements.value.none { it.isUnlocked })
    }

    @Test
    fun planAndVerseEventsCount() = runTest {
        val service = service()
        service.onPlanCompleted(wholeBible = true)
        service.onVerseSaved()
        service.onQuestionAnswered()
        advanceUntilIdle()

        val unlocked = service.achievements.value.filter { it.isUnlocked }.map { it.code.value }.toSet()
        assertEquals(setOf("plan_finished", "whole_bible", "treasured_verse"), unlocked)
        assertEquals("1 of 5", service.achievements.value.single { it.code.value == "thoughtful_5" }.progress!!.let { "${it.current} of ${it.target}" })
    }

    private fun CoroutineScope.launchCollect(service: GamificationService, onCode: (String) -> Unit) =
        launch { service.newlyUnlocked.collect { list -> list.forEach { onCode(it.code.value) } } }
}
