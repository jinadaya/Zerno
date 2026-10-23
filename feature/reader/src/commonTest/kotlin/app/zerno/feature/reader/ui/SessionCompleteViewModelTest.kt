package app.zerno.feature.reader.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.data.time.FakeClock
import app.zerno.data.time.FakeTimeZone
import app.zerno.designsystem.component.SjAchievementSet
import app.zerno.entities.achievement.Achievement
import app.zerno.entities.achievement.AchievementCode
import app.zerno.entities.achievement.AchievementSet
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.common.LocalizedText
import app.zerno.entities.plan.PlanDay
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.gamification.api.AchievementTexts
import app.zerno.feature.gamification.api.StreakDay
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.reader.domain.DailyReading
import app.zerno.feature.reader.domain.FakeBible
import app.zerno.feature.reader.domain.FakeEvents
import app.zerno.feature.reader.domain.FakeGamificationState
import app.zerno.feature.reader.domain.FakePlans
import app.zerno.feature.reader.domain.JOHN
import app.zerno.feature.reader.domain.KJV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCompleteViewModelTest {
    private val clock = FakeClock(Instant.parse("2026-10-22T07:00:00Z"))
    private val john = PlanId("gospel-of-john")
    private val plans = FakePlans().apply {
        catalog = listOf(
            ReadingPlan(
                id = john,
                type = PlanType.Sequential,
                title = LocalizedText(mapOf("en" to "Gospel of John", "ru" to "Евангелие от Иоанна")),
                description = LocalizedText(mapOf("en" to "d", "ru" to "о")),
                days = (1..21).map { PlanDay(it, listOf(PassageRef.chapter(JOHN, it)), 5) },
            ),
        )
    }
    private val events = FakeEvents()
    private val state = FakeGamificationState()
    private val daily = DailyReading(FakeBible(), plans, events, state, clock, FakeTimeZone(TimeZone.UTC))
    private val texts = object : AchievementTexts {
        override suspend fun title(achievement: Achievement) = "Title ${achievement.code}"
        override suspend fun description(achievement: Achievement) = "About ${achievement.code}"
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SessionCompleteViewModel(SavedStateHandle(), daily, plans, state, texts)

    @Test
    fun showsThePassageThePlanStepAndNewSeals() = runTest {
        events.onSession = { state.achievements.value = listOf(streakSeal()) }
        daily.begin(john, 4, PassageRef.chapter(JOHN, 4))
        daily.finish(KJV)

        val viewModel = viewModel()
        advanceUntilIdle()

        val summary = assertNotNull(viewModel.state.value.summary)
        assertEquals(PassageRef.chapter(JOHN, 4), summary.passage)
        assertEquals("KJV", summary.translation)
        assertEquals(4, summary.day)
        assertEquals(21, summary.planLength)
        assertEquals(listOf(SealText("Title streak_3", "About streak_3", SjAchievementSet.Bird)), summary.seals)
    }

    @Test
    fun aReflectionTurnsTodayFromReadToDone() = runTest {
        val today = state.streak.value.today
        state.streak.value = state.streak.value.copy(current = 3, week = listOf(StreakDay(today, StreakDayStatus.Read)))
        val viewModel = viewModel()
        assertEquals(StreakDayStatus.Read, viewModel.state.value.today)
        assertEquals(3, viewModel.state.value.streak)

        state.streak.value = state.streak.value.copy(week = listOf(StreakDay(today, StreakDayStatus.Done)))

        assertEquals(StreakDayStatus.Done, viewModel.state.value.today)
    }

    @Test
    fun writingAReflectionOpensItForThePlanSession() = runTest {
        daily.begin(john, 4, PassageRef.chapter(JOHN, 4))
        val session = assertNotNull(daily.finish(KJV)).session
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(SessionCompleteEvent.ReflectClicked)
        advanceUntilIdle()

        assertEquals(
            SessionCompleteEffect.OpenReflection(PassageRef.chapter(JOHN, 4), session.id, afterPlanStep = true),
            viewModel.effects.first(),
        )
    }

    @Test
    fun withNoFinishedSessionOnlyTheWayBackIsLeft() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertNull(viewModel.state.value.summary)

        viewModel.sendEvent(SessionCompleteEvent.DoneClicked)
        advanceUntilIdle()
        assertEquals(SessionCompleteEffect.BackToToday, viewModel.effects.first())
    }

    private fun streakSeal() = Achievement(
        code = AchievementCode("streak_3"),
        set = AchievementSet.Bird,
        titleKey = "t",
        descriptionKey = "d",
        icon = "i",
        unlockedAt = clock.now(),
    )
}
