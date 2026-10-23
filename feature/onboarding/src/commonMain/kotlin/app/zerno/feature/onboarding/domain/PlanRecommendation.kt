package app.zerno.feature.onboarding.domain

import app.zerno.entities.plan.PlanType
import app.zerno.entities.plan.ReadingPlan

/**
 * The plan to suggest for the answers: the kind the user prefers, else the kind their goal points to, else the first
 * plan. Among plans of that kind, the one whose average day fits [minutes] most closely without going over, else
 * the shortest.
 */
fun recommendPlan(
    plans: List<ReadingPlan>,
    goal: ReadingGoal,
    minutes: DailyMinutes,
    preference: PlanPreference,
): ReadingPlan {
    require(plans.isNotEmpty()) { "No plans to recommend from" }
    val candidates = listOf(preference.planType, goal.planType)
        .map { type -> plans.filter { it.type == type } }
        .firstOrNull { it.isNotEmpty() }
        ?: return plans.first()
    val fitting = candidates.filter { it.averageMinutes <= minutes.minutes }
    return fitting.maxByOrNull { it.averageMinutes } ?: candidates.minBy { it.averageMinutes }
}

/** Average reading time of a day of the plan, in minutes. */
val ReadingPlan.averageMinutes: Int
    get() = (days.sumOf { it.minutes } + length - 1) / length

private val PlanPreference.planType: PlanType
    get() = when (this) {
        PlanPreference.GuidedIntro -> PlanType.Introductory
        PlanPreference.Sequential -> PlanType.Sequential
        PlanPreference.Thematic -> PlanType.Thematic
    }

private val ReadingGoal.planType: PlanType
    get() = when (this) {
        ReadingGoal.StartReading -> PlanType.Introductory
        ReadingGoal.ReadConsistently -> PlanType.Sequential
        ReadingGoal.ExploreTheme -> PlanType.Thematic
        ReadingGoal.WholeBible -> PlanType.WholeBible
    }
