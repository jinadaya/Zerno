package app.zerno.feature.plans.domain

import app.zerno.entities.plan.ReadingPlan
import app.zerno.feature.plans.api.PlanToday
import kotlinx.datetime.LocalDate

/**
 * The plan day to show on [date]: from the active plan, or from a plan finished that date so its last day still
 * reads "Read again". Null when neither exists or the plan is no longer in [catalog].
 */
fun PlanJourney.todayIn(catalog: List<ReadingPlan>, date: LocalDate): PlanToday? {
    val progress = active ?: plans.values.firstOrNull { it.completedOn == date } ?: return null
    val plan = catalog.firstOrNull { it.id == progress.planId } ?: return null
    val step = progress.dayFor(date) ?: return null
    val day = plan.days.firstOrNull { it.day == step.day } ?: return null
    return PlanToday(plan, day, step.done)
}
