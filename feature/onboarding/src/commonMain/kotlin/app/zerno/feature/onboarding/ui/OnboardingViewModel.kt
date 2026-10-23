package app.zerno.feature.onboarding.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.data.prefs.AppPreferences
import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.onboarding.domain.OnboardingAnswers
import app.zerno.feature.onboarding.domain.OnboardingStep
import app.zerno.feature.onboarding.domain.next
import app.zerno.feature.onboarding.domain.previous
import app.zerno.feature.onboarding.domain.recommendPlan
import app.zerno.feature.onboarding.ui.OnboardingInnerEvent.AnswersChanged
import app.zerno.feature.onboarding.ui.OnboardingInnerEvent.StepShown
import app.zerno.feature.plans.api.Plans
import app.zerno.feature.reader.api.ReaderLauncher
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException

/**
 * Purpose, goal, time per day, plan preference, the recommended plan and an optional reminder; no account. Finishing
 * starts the plan, saves the reminder, points the reader at day 1 and marks onboarding done, in that order, so a
 * failure part way leaves onboarding to run again.
 */
class OnboardingViewModel(
    savedStateHandle: SavedStateHandle,
    private val plans: Plans,
    private val reader: ReaderLauncher,
    private val prefs: AppPreferences,
) : MviViewModel<OnboardingState, OnboardingEvent, OnboardingInnerEvent, OnboardingEffect, ScreenSnapshot.NoSnapshot>(
    savedStateHandle = savedStateHandle,
    initialState = OnboardingState(),
    reducer = OnboardingReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(OnboardingState()),
) {
    init {
        intent(LOAD) { emit(OnboardingInnerEvent.PlansLoaded(plans.catalog())) }
    }

    override fun onEvent(event: OnboardingEvent) {
        val state = state.value
        if (state.finishing) return
        val answers = state.answers
        when (event) {
            is OnboardingEvent.GoalSelected -> answer(answers.copy(goal = event.goal))

            is OnboardingEvent.MinutesSelected -> answer(answers.copy(minutes = event.minutes))

            is OnboardingEvent.PreferenceSelected -> answer(answers.copy(preference = event.preference))

            is OnboardingEvent.ReminderSelected -> answer(answers.copy(reminder = event.time))

            OnboardingEvent.ContinueClicked -> when {
                !state.canContinue -> Unit
                state.step == OnboardingStep.Reminder -> finish(state.recommended, answers)
                else -> show(state.step.next(), state)
            }

            OnboardingEvent.SkipReminderClicked -> finish(state.recommended, answers.copy(reminder = null))

            OnboardingEvent.BackClicked -> show(state.step.previous(), state)
        }
    }

    private fun answer(answers: OnboardingAnswers) = intent(ANSWER) { emit(AnswersChanged(answers)) }

    private fun show(step: OnboardingStep?, state: OnboardingState) {
        if (step == null) return
        intent(STEP) { emit(StepShown(step, recommendation(step, state))) }
    }

    /** The plan for the recommendation step and after; worked out again whenever the user comes back to it. */
    private fun recommendation(step: OnboardingStep, state: OnboardingState): ReadingPlan? {
        val answers = state.answers
        return when {
            step < OnboardingStep.Recommendation -> null
            step > OnboardingStep.Recommendation -> state.recommended
            state.plans.isEmpty() -> null
            answers.goal == null || answers.minutes == null || answers.preference == null -> null
            else -> recommendPlan(state.plans, answers.goal, answers.minutes, answers.preference)
        }
    }

    // Any failure to start the plan or open the reader leaves the user on this step with a message to try again.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun finish(plan: ReadingPlan?, answers: OnboardingAnswers) {
        if (plan == null) return
        intent(FINISH) {
            emit(OnboardingInnerEvent.Finishing)
            try {
                plans.start(plan.id)
                prefs.setReminderTime(answers.reminder)
                val first = plan.days.first()
                reader.openPlanDay(plan.id, first.day, first.passages.first())
                prefs.setOnboardingDone(true)
                postEffect(OnboardingEffect.OpenReader)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(OnboardingInnerEvent.FinishFailed)
            }
        }
    }

    private companion object {
        const val LOAD = "load"
        const val ANSWER = "answer"
        const val STEP = "step"
        const val FINISH = "finish"
    }
}
