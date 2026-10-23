package app.zerno.feature.onboarding.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.data.prefs.AppPreferences
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.onboarding.domain.DailyMinutes
import app.zerno.feature.onboarding.domain.John
import app.zerno.feature.onboarding.domain.LaunchLike
import app.zerno.feature.onboarding.domain.OnboardingStep
import app.zerno.feature.onboarding.domain.PlanPreference
import app.zerno.feature.onboarding.domain.ReadingGoal
import app.zerno.feature.plans.api.PlanToday
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.reader.api.PassageProgress
import app.zerno.feature.reader.api.ReaderLauncher
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val plans = FakePlans()
    private val reader = FakeReader()
    private val prefs = AppPreferences(MapSettings())

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = OnboardingViewModel(SavedStateHandle(), plans, reader, prefs)

    @Test
    fun finishingStartsThePlanAndOpensTheReaderAtDayOne() = runTest {
        val viewModel = viewModel()
        with(viewModel) {
            sendEvent(OnboardingEvent.ContinueClicked)
            sendEvent(OnboardingEvent.GoalSelected(ReadingGoal.ReadConsistently))
            sendEvent(OnboardingEvent.ContinueClicked)
            sendEvent(OnboardingEvent.MinutesSelected(DailyMinutes.Ten))
            sendEvent(OnboardingEvent.ContinueClicked)
            sendEvent(OnboardingEvent.PreferenceSelected(PlanPreference.Sequential))
            sendEvent(OnboardingEvent.ContinueClicked)
        }
        advanceUntilIdle()
        assertEquals(OnboardingStep.Recommendation, viewModel.state.value.step)
        assertEquals(PlanId("gospel-of-john"), viewModel.state.value.recommended?.id)

        viewModel.sendEvent(OnboardingEvent.ContinueClicked)
        viewModel.sendEvent(OnboardingEvent.ReminderSelected(LocalTime(7, 0)))
        viewModel.sendEvent(OnboardingEvent.ContinueClicked)
        advanceUntilIdle()

        assertEquals(OnboardingEffect.OpenReader, viewModel.effects.first())
        assertEquals(listOf(PlanId("gospel-of-john")), plans.started)
        assertEquals(listOf(PassageRef.chapter(John, 1)), reader.opened)
        assertEquals(LocalTime(7, 0), prefs.prefs.value.reminderTime)
        assertTrue(prefs.prefs.value.onboardingDone)
    }

    @Test
    fun continueWaitsForAnAnswer() = runTest {
        val viewModel = viewModel()
        viewModel.sendEvent(OnboardingEvent.ContinueClicked)
        advanceUntilIdle()
        assertEquals(OnboardingStep.Goal, viewModel.state.value.step)
        assertFalse(viewModel.state.value.canContinue)

        viewModel.sendEvent(OnboardingEvent.ContinueClicked)
        advanceUntilIdle()
        assertEquals(OnboardingStep.Goal, viewModel.state.value.step)
    }

    @Test
    fun backReturnsToThePreviousStepKeepingTheAnswer() = runTest {
        val viewModel = viewModel()
        viewModel.sendEvent(OnboardingEvent.ContinueClicked)
        viewModel.sendEvent(OnboardingEvent.GoalSelected(ReadingGoal.ExploreTheme))
        viewModel.sendEvent(OnboardingEvent.ContinueClicked)
        viewModel.sendEvent(OnboardingEvent.BackClicked)
        advanceUntilIdle()

        assertEquals(OnboardingStep.Goal, viewModel.state.value.step)
        assertEquals(ReadingGoal.ExploreTheme, viewModel.state.value.answers.goal)
    }

    @Test
    fun skippingTheReminderLeavesItOff() = runTest {
        val viewModel = viewModel()
        walkToReminder(viewModel)
        viewModel.sendEvent(OnboardingEvent.ReminderSelected(LocalTime(20, 0)))
        viewModel.sendEvent(OnboardingEvent.SkipReminderClicked)
        advanceUntilIdle()

        assertNull(prefs.prefs.value.reminderTime)
        assertTrue(prefs.prefs.value.onboardingDone)
    }

    @Test
    fun aFailureKeepsOnboardingToRunAgain() = runTest {
        reader.fail = true
        val viewModel = viewModel()
        walkToReminder(viewModel)
        viewModel.sendEvent(OnboardingEvent.SkipReminderClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.failed)
        assertFalse(viewModel.state.value.finishing)
        assertFalse(prefs.prefs.value.onboardingDone)
    }

    private fun walkToReminder(viewModel: OnboardingViewModel) = with(viewModel) {
        sendEvent(OnboardingEvent.ContinueClicked)
        sendEvent(OnboardingEvent.GoalSelected(ReadingGoal.StartReading))
        sendEvent(OnboardingEvent.ContinueClicked)
        sendEvent(OnboardingEvent.MinutesSelected(DailyMinutes.Five))
        sendEvent(OnboardingEvent.ContinueClicked)
        sendEvent(OnboardingEvent.PreferenceSelected(PlanPreference.GuidedIntro))
        sendEvent(OnboardingEvent.ContinueClicked)
        sendEvent(OnboardingEvent.ContinueClicked)
    }

    private class FakePlans : Plans {
        val started = mutableListOf<PlanId>()

        override suspend fun catalog(): List<ReadingPlan> = LaunchLike

        override suspend fun start(planId: PlanId) {
            started += planId
        }

        override suspend fun completeDay(planId: PlanId, day: Int) = Unit

        override fun today(date: LocalDate): Flow<PlanToday?> = flowOf(null)
    }

    private class FakeReader : ReaderLauncher {
        val opened = mutableListOf<PassageRef>()
        var fail = false

        override suspend fun openAt(passage: PassageRef, resume: Boolean) = error("Onboarding opens plan days")

        override suspend fun openPlanDay(planId: PlanId, day: Int, passage: PassageRef, resume: Boolean) {
            check(!fail) { "No Bible installed" }
            check(day == 1) { "Onboarding opens day 1, not $day" }
            opened += passage
        }

        override suspend fun progressIn(passage: PassageRef): PassageProgress? = null
    }
}
