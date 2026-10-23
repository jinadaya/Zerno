package app.zerno.feature.onboarding.domain

import kotlinx.datetime.LocalTime

/** Why the user is here (onboarding step 2). */
enum class ReadingGoal { StartReading, ReadConsistently, ExploreTheme, WholeBible }

/** How long a day they want to read (step 3). [TwentyPlus] counts as twenty. */
enum class DailyMinutes(val minutes: Int) { Five(5), Ten(10), Fifteen(15), TwentyPlus(20) }

/** The kind of plan they like (step 4). */
enum class PlanPreference { GuidedIntro, Sequential, Thematic }

/** The screens of onboarding, in order. */
enum class OnboardingStep { Welcome, Goal, Time, Preference, Recommendation, Reminder }

/** Reminder times offered on the last step; skipping it leaves reminders off. */
val ReminderTimes: List<LocalTime> = listOf(LocalTime(7, 0), LocalTime(12, 0), LocalTime(20, 0))

/** What the user answered so far. Nothing is stored until onboarding finishes. */
data class OnboardingAnswers(
    val goal: ReadingGoal? = null,
    val minutes: DailyMinutes? = null,
    val preference: PlanPreference? = null,
    val reminder: LocalTime? = null,
) {
    /** Whether [step] has what it asks for, so Continue is enabled. */
    fun answered(step: OnboardingStep): Boolean = when (step) {
        OnboardingStep.Goal -> goal != null
        OnboardingStep.Time -> minutes != null
        OnboardingStep.Preference -> preference != null
        OnboardingStep.Welcome, OnboardingStep.Recommendation, OnboardingStep.Reminder -> true
    }
}

/** The step after [this], or null on the last one. */
fun OnboardingStep.next(): OnboardingStep? = OnboardingStep.entries.getOrNull(ordinal + 1)

/** The step before [this], or null on the first one. */
fun OnboardingStep.previous(): OnboardingStep? = OnboardingStep.entries.getOrNull(ordinal - 1)
