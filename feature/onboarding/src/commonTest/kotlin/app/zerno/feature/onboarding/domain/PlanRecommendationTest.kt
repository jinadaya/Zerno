package app.zerno.feature.onboarding.domain

import app.zerno.entities.plan.PlanId
import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlanRecommendationTest {
    private fun recommend(
        goal: ReadingGoal = ReadingGoal.StartReading,
        minutes: DailyMinutes = DailyMinutes.Ten,
        preference: PlanPreference,
        plans: List<ReadingPlan> = LaunchLike,
    ) = recommendPlan(plans, goal, minutes, preference).id

    @Test
    fun preferenceDecidesTheKind() {
        assertEquals(PlanId("first-steps"), recommend(preference = PlanPreference.GuidedIntro))
        assertEquals(PlanId("gospel-of-john"), recommend(preference = PlanPreference.Sequential))
        assertEquals(PlanId("prayer"), recommend(preference = PlanPreference.Thematic))
    }

    @Test
    fun goalDecidesWhenNoPlanOfThePreferredKind() {
        val noThematic = LaunchLike.filter { it.type != PlanType.Thematic }
        assertEquals(
            PlanId("gospel-of-john"),
            recommend(ReadingGoal.ReadConsistently, preference = PlanPreference.Thematic, plans = noThematic),
        )
    }

    @Test
    fun firstPlanWhenNeitherKindExists() {
        val introOnly = LaunchLike.filter { it.type == PlanType.Introductory }
        assertEquals(
            PlanId("first-steps"),
            recommend(ReadingGoal.WholeBible, preference = PlanPreference.Thematic, plans = introOnly),
        )
    }

    @Test
    fun longestPlanThatFitsTheTime() {
        val plans = listOf(
            testPlan("short", PlanType.Sequential, John, minutes = 4),
            testPlan("medium", PlanType.Sequential, John, minutes = 9),
            testPlan("long", PlanType.Sequential, John, minutes = 18),
        )
        assertEquals(
            PlanId("short"),
            recommend(minutes = DailyMinutes.Five, preference = PlanPreference.Sequential, plans = plans),
        )
        assertEquals(
            PlanId("medium"),
            recommend(minutes = DailyMinutes.Fifteen, preference = PlanPreference.Sequential, plans = plans),
        )
        assertEquals(
            PlanId("long"),
            recommend(minutes = DailyMinutes.TwentyPlus, preference = PlanPreference.Sequential, plans = plans),
        )
    }

    @Test
    fun shortestPlanWhenNoneFits() {
        val plans = listOf(
            testPlan("medium", PlanType.Sequential, John, minutes = 9),
            testPlan("long", PlanType.Sequential, John, minutes = 18),
        )
        assertEquals(
            PlanId("medium"),
            recommend(minutes = DailyMinutes.Five, preference = PlanPreference.Sequential, plans = plans),
        )
    }

    @Test
    fun averageMinutesRoundsUp() {
        val plan = testPlan("p", PlanType.Sequential, John, minutes = 5, length = 3)
        assertEquals(5, plan.averageMinutes)
        val uneven = plan.copy(days = plan.days.mapIndexed { i, day -> day.copy(minutes = if (i == 0) 6 else 5) })
        assertEquals(6, uneven.averageMinutes)
    }

    @Test
    fun noPlansFails() {
        assertFailsWith<IllegalArgumentException> {
            recommend(preference = PlanPreference.Sequential, plans = emptyList())
        }
    }

    @Test
    fun stepsRunInOrder() {
        assertEquals(OnboardingStep.Goal, OnboardingStep.Welcome.next())
        assertEquals(null, OnboardingStep.Reminder.next())
        assertEquals(null, OnboardingStep.Welcome.previous())
        assertEquals(OnboardingStep.Recommendation, OnboardingStep.Reminder.previous())
    }

    @Test
    fun questionsNeedAnAnswer() {
        val none = OnboardingAnswers()
        assertEquals(false, none.answered(OnboardingStep.Goal))
        assertEquals(true, none.copy(goal = ReadingGoal.ExploreTheme).answered(OnboardingStep.Goal))
        assertEquals(true, none.answered(OnboardingStep.Reminder))
    }
}
