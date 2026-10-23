package app.zerno.feature.onboarding.ui

import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.onboarding.domain.DailyMinutes
import app.zerno.feature.onboarding.domain.OnboardingAnswers
import app.zerno.feature.onboarding.domain.OnboardingStep
import app.zerno.feature.onboarding.domain.PlanPreference
import app.zerno.feature.onboarding.domain.ReadingGoal
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent
import kotlinx.datetime.LocalTime

/**
 * [recommended] is filled once the answers lead to the recommendation step. [finishing] covers the short wait while
 * the plan starts and the reader opens; [failed] says that did not work and the user can try again.
 */
data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val plans: List<ReadingPlan> = emptyList(),
    val recommended: ReadingPlan? = null,
    val finishing: Boolean = false,
    val failed: Boolean = false,
) : ScreenState {
    val canContinue: Boolean
        get() = answers.answered(step) && (step != OnboardingStep.Recommendation || recommended != null)
}

sealed interface OnboardingEvent : UserEvent {
    data object ContinueClicked : OnboardingEvent
    data object BackClicked : OnboardingEvent
    data class GoalSelected(val goal: ReadingGoal) : OnboardingEvent
    data class MinutesSelected(val minutes: DailyMinutes) : OnboardingEvent
    data class PreferenceSelected(val preference: PlanPreference) : OnboardingEvent

    /** A time on the reminder step; null clears it. */
    data class ReminderSelected(val time: LocalTime?) : OnboardingEvent

    /** "Skip" on the reminder step: finish with reminders off. */
    data object SkipReminderClicked : OnboardingEvent
}

sealed interface OnboardingInnerEvent : InnerEvent {
    data class PlansLoaded(val plans: List<ReadingPlan>) : OnboardingInnerEvent
    data class StepShown(val step: OnboardingStep, val recommended: ReadingPlan?) : OnboardingInnerEvent
    data class AnswersChanged(val answers: OnboardingAnswers) : OnboardingInnerEvent
    data object Finishing : OnboardingInnerEvent
    data object FinishFailed : OnboardingInnerEvent
}

sealed interface OnboardingEffect : SideEffect {
    /** The plan has started and the reader points at its first day: leave onboarding for the reader. */
    data object OpenReader : OnboardingEffect
}

object OnboardingReducer : MviReducer<OnboardingState, OnboardingInnerEvent> {
    override fun reduce(oldState: OnboardingState, event: OnboardingInnerEvent): OnboardingState = when (event) {
        is OnboardingInnerEvent.PlansLoaded -> oldState.copy(plans = event.plans)
        is OnboardingInnerEvent.StepShown -> oldState.copy(step = event.step, recommended = event.recommended)
        is OnboardingInnerEvent.AnswersChanged -> oldState.copy(answers = event.answers)
        OnboardingInnerEvent.Finishing -> oldState.copy(finishing = true, failed = false)
        OnboardingInnerEvent.FinishFailed -> oldState.copy(finishing = false, failed = true)
    }
}
