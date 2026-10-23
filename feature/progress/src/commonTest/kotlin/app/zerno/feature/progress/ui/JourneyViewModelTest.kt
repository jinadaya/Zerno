package app.zerno.feature.progress.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.entities.achievement.Achievement
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.gamification.api.ReadingHistory
import app.zerno.feature.gamification.api.StreakDayStatus
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanRunStatus
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.progress.domain.testPlan
import app.zerno.feature.progress.domain.testSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JourneyViewModelTest {
    private val today = LocalDate(2026, 10, 23)
    private val gamification = FakeGamification(today)
    private val plans = FakePlans()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = JourneyViewModel(SavedStateHandle(), gamification, plans)

    @Test
    fun showsTotalsTheCurrentPlanAndHistory() = runTest {
        val john = testPlan("john")
        plans.overview.value = listOf(
            PlanOverview(testPlan("psalms"), PlanRunStatus.Paused),
            PlanOverview(john, PlanRunStatus.Active, completedDays = setOf(1), nextDay = 2),
        )
        gamification.history.value = ReadingHistory(
            sessions = listOf(testSession("1", "John 1", "2026-10-22T08:00:00Z", plan = "john", day = 1)),
            days = mapOf(LocalDate(2026, 10, 22) to StreakDayStatus.Read),
        )

        val viewModel = viewModel()
        advanceUntilIdle()
        val state = viewModel.state.value

        assertEquals(1, state.numbers.sessions)
        assertEquals(1, state.numbers.chaptersRead)
        assertEquals(john.id, state.currentPlan?.plan?.id)
        assertEquals(john, state.history.single().plan)
        assertEquals(LocalDate(2026, 10, 1), state.calendar?.month)
        assertEquals(StreakDayStatus.Read, state.calendar?.days?.get(21)?.status)
    }

    @Test
    fun noActivePlanMeansNoCurrentPlan() = runTest {
        plans.overview.value = listOf(PlanOverview(testPlan("john"), PlanRunStatus.Paused))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertNull(viewModel.state.value.currentPlan)
    }

    @Test
    fun monthsMoveBackToTheFirstReadingAndNotPastThisMonth() = runTest {
        gamification.history.value = ReadingHistory(days = mapOf(LocalDate(2026, 9, 3) to StreakDayStatus.Done))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(JourneyEvent.NextMonthClicked)
        advanceUntilIdle()

        assertEquals(LocalDate(2026, 10, 1), viewModel.state.value.calendar?.month)

        viewModel.sendEvent(JourneyEvent.PreviousMonthClicked)
        advanceUntilIdle()

        assertEquals(LocalDate(2026, 9, 1), viewModel.state.value.calendar?.month)

        viewModel.sendEvent(JourneyEvent.PreviousMonthClicked)
        advanceUntilIdle()

        assertEquals(LocalDate(2026, 9, 1), viewModel.state.value.calendar?.month)

        viewModel.sendEvent(JourneyEvent.NextMonthClicked)
        advanceUntilIdle()

        assertEquals(LocalDate(2026, 10, 1), viewModel.state.value.calendar?.month)
    }

    @Test
    fun opensSealsAndPlans() = runTest {
        val viewModel = viewModel()

        viewModel.sendEvent(JourneyEvent.AchievementsClicked)
        viewModel.sendEvent(JourneyEvent.CurrentPlanClicked)

        assertEquals(
            listOf(JourneyEffect.OpenAchievements, JourneyEffect.OpenPlans),
            viewModel.effects.take(2).toList(),
        )
    }

    @Test
    fun showingTheTabRecomputes() = runTest {
        val viewModel = viewModel()

        viewModel.sendEvent(JourneyEvent.Shown)

        advanceUntilIdle()

        assertTrue(gamification.refreshes > 0)
    }

    private class FakeGamification(date: LocalDate) : GamificationState {
        var refreshes = 0
        override val streak = MutableStateFlow(StreakState(date, 0, 0, emptyList(), 0))
        override val achievements = MutableStateFlow<List<Achievement>>(emptyList())
        override val newlyUnlocked: Flow<List<Achievement>> = emptyFlow()
        override val history = MutableStateFlow(ReadingHistory())

        override suspend fun refresh() {
            refreshes++
        }
    }

    private class FakePlans : Plans {
        override val overview = MutableStateFlow<List<PlanOverview>>(emptyList())

        override suspend fun catalog(): List<ReadingPlan> = overview.value.map { it.plan }

        override suspend fun start(planId: PlanId) = Unit

        override suspend fun pause() = Unit

        override suspend fun completeDay(planId: PlanId, day: Int) = Unit

        override fun today(date: LocalDate): Flow<PlanToday?> = emptyFlow()
    }
}
