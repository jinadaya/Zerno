package app.zerno.feature.today.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.entities.achievement.Achievement
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.gamification.api.GamificationState
import app.zerno.feature.gamification.api.ReadingHistory
import app.zerno.feature.gamification.api.StreakState
import app.zerno.feature.plans.api.PlanOverview
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.reader.api.PassageProgress
import app.zerno.feature.reader.api.ReaderLauncher
import app.zerno.feature.today.domain.John
import app.zerno.feature.today.domain.LighterPassage
import app.zerno.feature.today.domain.TodayAction
import app.zerno.feature.today.domain.testPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val date = LocalDate(2026, 10, 22)
    private val plan = testPlan()
    private val plans = FakePlans()
    private val reader = FakeReader()
    private val gamification = FakeGamification(date)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TodayViewModel(SavedStateHandle(), plans, reader, gamification)

    @Test
    fun noActivePlanShowsNoCard() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.planLoaded)
        assertNull(viewModel.state.value.card)
    }

    @Test
    fun aDayNotStartedReadsStartAndOpensAtTheTop() = runTest {
        plans.show(date, PlanToday(plan, plan.days[0], done = false))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(TodayAction.Start, viewModel.state.value.card?.action)

        viewModel.sendEvent(TodayEvent.ActionClicked)
        advanceUntilIdle()

        assertEquals(TodayEffect.OpenReader, viewModel.effects.first())
        assertEquals(listOf(PassageRef.chapter(John, 1) to false), reader.opened)
    }

    @Test
    fun returningPartWayReadsContinueAndResumes() = runTest {
        plans.show(date, PlanToday(plan, plan.days[0], done = false))
        val viewModel = viewModel()
        advanceUntilIdle()

        reader.progress = PassageProgress(12, 51)
        viewModel.sendEvent(TodayEvent.Shown)
        advanceUntilIdle()
        val card = viewModel.state.value.card
        assertEquals(TodayAction.Continue, card?.action)
        assertEquals(PassageProgress(12, 51), card?.progress)

        viewModel.sendEvent(TodayEvent.ActionClicked)
        advanceUntilIdle()
        assertEquals(listOf(PassageRef.chapter(John, 1) to true), reader.opened)
    }

    @Test
    fun aDayDoneTodayReadsReadAgain() = runTest {
        plans.show(date, PlanToday(plan, plan.days[0], done = false))
        reader.progress = PassageProgress(12, 51)
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(TodayAction.Continue, viewModel.state.value.card?.action)

        plans.show(date, PlanToday(plan, plan.days[0], done = true))
        advanceUntilIdle()

        assertEquals(TodayAction.ReadAgain, viewModel.state.value.card?.action)
        assertNull(viewModel.state.value.card?.progress)
    }

    @Test
    fun aNewDayMovesTheCardOn() = runTest {
        val tomorrow = date.plus(1, DateTimeUnit.DAY)
        plans.show(date, PlanToday(plan, plan.days[0], done = true))
        plans.show(tomorrow, PlanToday(plan, plan.days[1], done = false))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(TodayAction.ReadAgain, viewModel.state.value.card?.action)

        gamification.streak.value = gamification.streak.value.copy(today = tomorrow)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.card?.day?.day)
        assertEquals(TodayAction.Start, viewModel.state.value.card?.action)
    }

    @Test
    fun showingTheScreenRefreshesTheStreak() = runTest {
        val viewModel = viewModel()
        viewModel.sendEvent(TodayEvent.Shown)
        advanceUntilIdle()

        assertEquals(1, gamification.refreshes)
        assertEquals(gamification.streak.value, viewModel.state.value.streak)
    }

    @Test
    fun aReaderThatCannotOpenShowsAMessage() = runTest {
        plans.show(date, PlanToday(plan, plan.days[0], done = false))
        reader.fail = true
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.sendEvent(TodayEvent.ActionClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.openFailed)
    }

    @Test
    fun choosingAPlanOpensThePlansTab() = runTest {
        val viewModel = viewModel()
        viewModel.sendEvent(TodayEvent.ChoosePlanClicked)
        advanceUntilIdle()

        assertEquals(TodayEffect.OpenPlans, viewModel.effects.first())
    }

    @Test
    fun threeDaysAwayWelcomesBackAndResumesThePlanDay() = runTest {
        plans.show(date, PlanToday(plan, plan.days[3], done = false))
        gamification.streak.value =
            gamification.streak.value.copy(lastRead = date.minus(3, DateTimeUnit.DAY))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.welcomeBack)

        viewModel.sendEvent(TodayEvent.ResumeClicked)
        advanceUntilIdle()

        assertEquals(TodayEffect.OpenReader, viewModel.effects.first())
        assertEquals(listOf(PassageRef.chapter(John, 4) to true), reader.opened)
    }

    @Test
    fun somethingLighterOpensAShortPassage() = runTest {
        gamification.streak.value =
            gamification.streak.value.copy(lastRead = date.minus(3, DateTimeUnit.DAY))
        val viewModel = viewModel()
        viewModel.sendEvent(TodayEvent.LighterClicked)
        advanceUntilIdle()

        assertEquals(TodayEffect.OpenReader, viewModel.effects.first())
        assertEquals(listOf(LighterPassage), reader.freeReading)
    }

    @Test
    fun readingYesterdayIsNoWelcomeBack() = runTest {
        gamification.streak.value =
            gamification.streak.value.copy(lastRead = date.minus(1, DateTimeUnit.DAY))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.welcomeBack)
    }

    /** [show] sets what a date shows, and updates every open flow. */
    private class FakePlans : Plans {
        private val state = MutableStateFlow<Map<LocalDate, PlanToday>>(emptyMap())
        fun show(date: LocalDate, today: PlanToday) {
            state.value += date to today
        }

        override suspend fun catalog(): List<ReadingPlan> = emptyList()

        override suspend fun pause() = Unit

        override val overview: Flow<List<PlanOverview>> = emptyFlow()

        override suspend fun start(planId: PlanId) = Unit

        override suspend fun completeDay(planId: PlanId, day: Int) = Unit

        override fun today(date: LocalDate): Flow<PlanToday?> = state.map { it[date] }
    }

    private class FakeReader : ReaderLauncher {
        val opened = mutableListOf<Pair<PassageRef, Boolean>>()
        val freeReading = mutableListOf<PassageRef>()
        var progress: PassageProgress? = null
        var fail = false

        override suspend fun openAt(passage: PassageRef, resume: Boolean) {
            check(!fail) { "No Bible installed" }
            freeReading += passage
        }

        override suspend fun openPlanDay(planId: PlanId, day: Int, passage: PassageRef, resume: Boolean) {
            check(!fail) { "No Bible installed" }
            check(day == passage.startChapter) { "Day $day of the test plan reads chapter $day" }
            opened += passage to resume
        }

        override suspend fun progressIn(passage: PassageRef): PassageProgress? = progress
    }

    private class FakeGamification(date: LocalDate) : GamificationState {
        var refreshes = 0
        override val streak = MutableStateFlow(StreakState(date, 0, 0, emptyList(), 0))
        override val achievements: StateFlow<List<Achievement>> = MutableStateFlow(emptyList())
        override val newlyUnlocked: Flow<List<Achievement>> = emptyFlow()
        override val history: StateFlow<ReadingHistory> = MutableStateFlow(ReadingHistory())

        override suspend fun refresh() {
            refreshes++
        }
    }
}
